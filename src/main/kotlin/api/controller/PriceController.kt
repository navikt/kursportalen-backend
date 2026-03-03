package com.example.api.controller

import com.example.model.PriceRequest
import com.example.service.CryptoService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond

class PriceController(
    private val cryptoService: CryptoService
) {
    suspend fun getPrice(call: ApplicationCall) {
        val request = call.receive<PriceRequest>()
        val response = cryptoService.getPrice(request)
        call.respond(HttpStatusCode.OK, response)
    }
}
