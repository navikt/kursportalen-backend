package com.example.config

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json

fun Application.commonConfig() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }
}

fun Routing.internalNaisRoutes(
    applicationState: ApplicationState,
    readynessCheck: () -> Boolean = { applicationState.ready },
    alivenessCheck: () -> Boolean = { applicationState.alive }
) {
    route("internal") {
        get("isAlive") {
            if (alivenessCheck()) {
                call.respondText("I'm alive :)")
            } else {
                call.respondText(
                    text = "I'm dead x_x",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
        get("isReady") {
            if (readynessCheck()) {
                call.respondText("I'm ready! :)")
            } else {
                call.respondText(
                    text = "Wait! I'm not ready yet! :O",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}
