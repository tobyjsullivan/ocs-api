package com.oneclicksandwich.api.orders

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object Service extends DefaultJsonProtocol {
  private val config = ConfigFactory.load()
  private val ordersSvcHost = config.getString("ordersSvc.host")
  private val ordersSvcPort = config.getInt("ordersSvc.port")

  private implicit val orderFmt = jsonFormat6(Order)
  private implicit object acceptedOrderFmt extends RootJsonReader[AcceptedOrder] {
    override def read(json: JsValue): AcceptedOrder = {
      println("Parsing:", json)
      val order = json.convertTo[Order]

      json.asJsObject.getFields("id") match {
        case Seq(JsString(id)) => AcceptedOrder(id, order)
        case _ => deserializationError("Expected an order object")
      }
    }
  }

  def accept(order: Order)(implicit executionContext: ExecutionContext, system: ActorSystem, fm: Materializer): Future[AcceptedOrder] = {
    // Serialize order into request
    Marshal(order.toJson.compactPrint).to[MessageEntity].flatMap { entity =>
      // Forward request to Orders svc
      val uri = Uri.from(scheme = "http", host = ordersSvcHost, port = ordersSvcPort, path = "/orders")
      Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity))
    }.map {
      case HttpResponse(StatusCodes.OK, _, HttpEntity.Strict(_, content), _) =>
        println("Content: ", content.utf8String)
        // Deserialize Order
        content.utf8String.parseJson.convertTo[AcceptedOrder]
      case HttpResponse(StatusCodes.OK, _, HttpEntity.Strict(contentType, _), _) =>
        throw new Exception(String.format("Unexpected content-type: %s", contentType))
      case HttpResponse(_, _, entity, _) =>
        throw new Exception(String.format("Unexpected error: %s", entity))
    }
  }
}
