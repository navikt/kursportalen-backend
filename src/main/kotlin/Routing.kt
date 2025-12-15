package com.example

import com.example.service.BtcPriceService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val btcPriceService = BtcPriceService()

    routing {
        get("/bitcoinprice") {
            call.respond(btcPriceService.getBtcPrice())
        }
    }
}
