package com.example.api

import com.example.service.CryptoService
import com.example.model.PriceRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val cryptoService = CryptoService()

    routing {
        route("/api/v1") {
            post("/price") {
                val request = call.receive<PriceRequest>()
                val response = cryptoService.getPrice(request)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
