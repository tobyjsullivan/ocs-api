package com.oneclicksandwich.api.orders

case class Order(
                name: String,
                phone: String,
                address1: String,
                address2: String,
                postalCode: String,
                additionalInstructions: String
              )

case class AcceptedOrder(id: String, order: Order)
