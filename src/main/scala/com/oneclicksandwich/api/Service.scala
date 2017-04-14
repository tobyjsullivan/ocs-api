package com.oneclicksandwich.api

import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import com.oneclicksandwich.api.orders.{AcceptedOrder, Driver, Order}
import com.oneclicksandwich.api.orders.records.OrderRecorder
import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

trait OrderProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val orderFormat: RootJsonFormat[Order] = jsonFormat6(Order)
//  implicit val acceptedOrderFormat: RootJsonFormat[AcceptedOrder] = jsonFormat2(AcceptedOrder)

  implicit object acceptedOrderFormat extends RootJsonFormat[AcceptedOrder] {
    override def write(obj: AcceptedOrder): JsValue = {
      val order = obj.order.toJson.asJsObject

      order.copy(fields = order.fields + ("id" -> JsString(obj.id)))
    }

    // Parsing isn't needed
    override def read(json: JsValue): AcceptedOrder = ???
  }
}

object Service extends OrderProtocol {
  private val config = ConfigFactory.load()
  private val allowedOrigin = HttpOrigin(config.getString("api.cors.allowedOrigin"))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val route =
      headerValueByType[Origin]() { origin =>
        withCorsHeaders(origin) {
          preflightRequestHandler ~
            path("orders") {
              post {
                entity(as[Order]) { order =>
                  val fAccepted = acceptOrder(order)

                  onComplete(fAccepted) { accepted =>
                    complete(accepted)
                  }
                }
              }
            }
        }
      }

    val hostname = config.getString("api.host")
    val port = config.getInt("api.port")
    val bindingFuture = Http().bindAndHandle(route, interface = hostname, port = port)

    println(s"Server listening to $hostname on port $port")

    sys.addShutdownHook({
      println("Shutting down...")

      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete{_ =>
          system.terminate()
          println("Done.")
        } // and shutdown when done
    })
  }

  private def withCorsHeaders(origin: Origin): Directive0 = {
    respondWithHeaders(
      allowOrigin(origin),
      `Access-Control-Allow-Headers`("Content-Type")
    )
  }

  private def allowOrigin(origin: Origin): `Access-Control-Allow-Origin` = origin.origins match {
    case Seq(head) if head.host == allowedOrigin.host => `Access-Control-Allow-Origin`(head)
    case _ => `Access-Control-Allow-Origin`.`null`
  }

  //this handles preflight OPTIONS requests.
  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }

  private def acceptOrder(order: Order)(implicit executionContext: ExecutionContext): Future[AcceptedOrder] = {
    order.accept().flatMap { accepted =>
      Future.sequence(
        Seq(
          OrderRecorder.saveOrderRecord(accepted),
          Driver.notify(order)
        )
      ).map(_ => accepted)
    }
  }
}
