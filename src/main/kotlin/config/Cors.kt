package com.example.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors() {
    install(CORS) {
        allowHost("localhost:5173", schemes = listOf("http"))
        allowHost("kursportalen.ansatt.dev.nav.no", schemes = listOf("https"))
        allowHost("kursportalen.intern.dev.nav.no", schemes = listOf("https"))

        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
    }
}
