package com.example.api

import com.example.service.BtcPriceService
import com.example.model.PriceRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val btcPriceService = BtcPriceService()

    routing {
        route("/api/v1/") {

            get("/bitcoin") {
                call.respond(btcPriceService.getBtcPrice())
            }
            post("/price") {
                val request = call.receive<PriceRequest>()
                val response = btcPriceService.getPrice(request)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
