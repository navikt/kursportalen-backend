package com.example

import com.example.config.ApplicationState
import com.example.config.commonConfig
import com.example.config.internalNaisRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.routing

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val applicationState = ApplicationState()

    commonConfig()
    routing {
        internalNaisRoutes(applicationState)
        configureRouting()
    }
}
