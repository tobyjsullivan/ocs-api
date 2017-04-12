package com.oneclicksandwich.api.orders.records

import akka.Done
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document._
import com.oneclicksandwich.api.orders.Order
import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat

import scala.concurrent.{ExecutionContext, Future}

object OrderRecorder {
  private val ordersTable = "ocs-order"
  private lazy val client = AmazonDynamoDBClientBuilder
    .standard()
    .withCredentials(new ProfileCredentialsProvider())
    .withRegion(Regions.US_WEST_2)
    .build()

  private val dateFmt = ISODateTimeFormat.dateTimeNoMillis()

  /**
    * Saves a record of the order to our DynamoDB table
    * @param order
    * @return
    */
  def saveOrderRecord(order: Order)(implicit executionContext: ExecutionContext): Future[Done] = order match {
    case Order(None, _, _, _, _, _, _) => Future.failed(new IllegalArgumentException("Order must have an ID before saving."))
    case _ => Future {
      val dynamoDB = new DynamoDB(client)

      val table = dynamoDB.getTable(ordersTable)

      table.putItem(buildItem(order))

      Done
    }
  }

  private def buildItem(order: Order): Item = {
    val baseItem = new Item()
      .withPrimaryKey("ID", order.id.get)

    Map[String, String](
      "Name" -> order.name,
      "Phone" -> order.phone,
      "Address 1" -> order.address1,
      "Address 2" -> order.address2,
      "Postal Code" -> order.postalCode,
      "Additional Instructions" -> order.additionalInstructions,
      "Timestamp" -> dateFmt.print(new Instant())
    ).foldLeft(baseItem) {
      case (item, (key, value)) if !value.isEmpty => item.withString(key, value)
      case (item, _) => item
    }
  }
}
