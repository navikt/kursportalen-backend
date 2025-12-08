package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true

        // For development
        anyHost()

        // For production, use specific hosts:
        // allowHost("kursportalen.ansatt.dev.nav.no", schemes = listOf("https"))
        // allowHost("localhost:5173")
    }
}
