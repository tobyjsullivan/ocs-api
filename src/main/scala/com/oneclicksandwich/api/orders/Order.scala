package com.oneclicksandwich.api.orders

import java.util.UUID

import akka.Done
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

case class Order(
                        name: String,
                        phone: String,
                        address1: String,
                        address2: String,
                        postalCode: String,
                        additionalInstructions: String
                      ) {

  def accept()(implicit executionContext: ExecutionContext): Future[AcceptedOrder] = {
    val accepted = AcceptedOrder(id = UUID.randomUUID().toString, this)

    OrderAcceptedEvent.fire(accepted).map(_ => accepted)
  }


}

object OrderAcceptedEvent extends DefaultJsonProtocol {
  private implicit val orderFmt = jsonFormat6(Order)
  implicit object acceptedOrderFormat extends RootJsonFormat[AcceptedOrder] {
    override def write(obj: AcceptedOrder): JsValue = {
      val order = obj.order.toJson.asJsObject

      order.copy(fields = order.fields + ("id" -> JsString(obj.id)))
    }

    // Parsing isn't needed
    override def read(json: JsValue): AcceptedOrder = ???
  }

  def fire(acceptedOrder: AcceptedOrder)(implicit executionContext: ExecutionContext): Future[Done] = {
    val content = acceptedOrder.toJson.prettyPrint

    publish(content)
  }

  private val snsTopicARN = "arn:aws:sns:us-west-2:110303772622:ocs-order_accepted"
  private lazy val snsClient = AmazonSNSClientBuilder
    .standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withRegion(Regions.US_WEST_2)
    .build()

  private def publish(content: String)(implicit executionContext: ExecutionContext): Future[Done] = Future {
    val result = snsClient.publish(snsTopicARN, content)
    println(s"Published event: ${result.getMessageId}")

    Done
  }
}

case class AcceptedOrder(id: String, order: Order)
