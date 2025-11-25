package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Velkommen til Kursportalen!")
        }
        get("/charts") {
            val text = "Bitcoin price: 10000"
            call.respondText(text)
        }
    }
}
