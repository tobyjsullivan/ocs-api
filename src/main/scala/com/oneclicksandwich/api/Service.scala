package com.oneclicksandwich.api

import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import com.oneclicksandwich.api.orders.{Driver, Order}
import com.oneclicksandwich.api.orders.records.OrderRecorder
import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn


trait OrderProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val orderFormat = jsonFormat7(Order)
}

object Service extends OrderProtocol {

  private val config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val route =
      path("orders") {
        post {
          entity(as[Order]) { order =>
            val created = createOrder(order)

            onComplete(created) { created =>
              complete(created)
            }
          }

        }
      }

    val bindingFuture = Http().bindAndHandle(route, "::0", config.getInt("api.port"))

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  private def createOrder(order: Order)(implicit executionContext: ExecutionContext): Future[Order] = {
    val withId = order.copy(id = Some(UUID.randomUUID().toString))

    Future.sequence(
      Seq(
        OrderRecorder.saveOrderRecord(withId),
        Driver.notify(withId)
//        notifyCustomer(withId)
      )
    ).map(_ => withId)
  }

  /**
    * Sends an SMS to the phone # on the order notifying of pending delivery.
    * @param order
    * @return
    */
  private def notifyCustomer(order: Order): Future[Done] = ???
}
