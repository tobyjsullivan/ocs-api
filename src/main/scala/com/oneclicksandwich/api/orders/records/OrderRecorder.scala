package com.oneclicksandwich.api.orders.records

import akka.Done
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document._
import com.oneclicksandwich.api.orders.{AcceptedOrder, Order}
import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat

import scala.concurrent.{ExecutionContext, Future}

object OrderRecorder {
  private val ordersTable = "ocs-order"
  private lazy val client = AmazonDynamoDBClientBuilder
    .standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withRegion(Regions.US_WEST_2)
    .build()

  private val dateFmt = ISODateTimeFormat.dateTimeNoMillis()

  /**
    * Saves a record of the order to our DynamoDB table
    * @param accepted
    * @return
    */
  def saveOrderRecord(accepted: AcceptedOrder)(implicit executionContext: ExecutionContext): Future[Done] = Future {
      val dynamoDB = new DynamoDB(client)

      val table = dynamoDB.getTable(ordersTable)

      table.putItem(buildItem(accepted))

      Done
    }

  private def buildItem(accepted: AcceptedOrder): Item = {
    val baseItem = new Item()
      .withPrimaryKey("ID", accepted.id)

    Map[String, String](
      "Name" -> accepted.order.name,
      "Phone" -> accepted.order.phone,
      "Address 1" -> accepted.order.address1,
      "Address 2" -> accepted.order.address2,
      "Postal Code" -> accepted.order.postalCode,
      "Additional Instructions" -> accepted.order.additionalInstructions,
      "Timestamp" -> dateFmt.print(new Instant())
    ).foldLeft(baseItem) {
      case (item, (key, value)) if !value.isEmpty => item.withString(key, value)
      case (item, _) => item
    }
  }
}
