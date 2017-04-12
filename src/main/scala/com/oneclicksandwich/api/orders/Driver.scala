package com.oneclicksandwich.api.orders

import akka.Done
import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}

case object Driver {
  private val conf = ConfigFactory.load()

  private val twilioSid = conf.getString("twilio.accountSid")

  private val fromNumber = conf.getString("api.sourceNumber")
  private val phoneNumber = conf.getString("api.driver.number")

  Twilio.init(
    twilioSid,
    conf.getString("twilio.authToken")
  )

  /**
    * Sends an SMS to the driver with the delivery address
    * @param order
    * @return
    */
  def notify(order: Order)(implicit executionContext: ExecutionContext): Future[Done] = Future {
    val message = Message.creator(
      new PhoneNumber(phoneNumber),
      new PhoneNumber(fromNumber),
      formatSMS(order)
    ).create()

    println("Driver notification sent: " + message.getSid)

    Done
  }

  private def formatSMS(order: Order): String = {
    s"""
       |New Order!
       |${order.name} (${order.phone})
       |${order.address1}
       |${order.address2}
       |${order.postalCode}
       |
       |${order.additionalInstructions}
       |
       |waze://?q=${java.net.URLEncoder.encode(order.address1, "utf-8")}
       |""".stripMargin.trim
  }
}
