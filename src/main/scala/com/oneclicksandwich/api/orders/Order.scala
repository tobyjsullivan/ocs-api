package com.oneclicksandwich.api.orders

final case class Order(
                        id: Option[String],
                        name: String,
                        phone: String,
                        address1: String,
                        address2: String,
                        postalCode: String,
                        additionalInstructions: String
                      )
