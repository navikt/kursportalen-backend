package com.example

import com.example.service.BtcPriceService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class BtcPriceResponse(val price: String)

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    val btcPriceService = BtcPriceService()

    routing {
        get("/") {
            call.respondText("Velkommen til Kursportalen!")
        }
        get("/bitcoinprice") {
            val price = btcPriceService.getBtcPrice()
            call.respond(BtcPriceResponse(price = price))
        }
    }
}
