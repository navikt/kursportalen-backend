package com.example

import com.example.config.ApplicationState
import com.example.config.commonConfig
import com.example.config.internalNaisRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.kotlinx.json.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val applicationState = ApplicationState()

    commonConfig()

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true

        // For development
        anyHost()

        // For production, use specific hosts instead of anyHost():
        // allowHost("kursportalen.ansatt.dev.nav.no", schemes = listOf("https"))
        // allowHost("localhost:5173")
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        internalNaisRoutes(applicationState)
        configureRouting()
    }
}
