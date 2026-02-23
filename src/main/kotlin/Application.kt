package com.example

import com.example.api.configureRouting
import com.example.config.ApplicationState
import com.example.config.DatabaseFactory
import com.example.config.commonConfig
import com.example.config.internalNaisRoutes
import com.example.config.configureCors
import com.example.repository.FavoriteTickerRepository
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val applicationState = ApplicationState()
    val dataSource = DatabaseFactory.createDataSourceOrThrow()
    DatabaseFactory.runMigrations(dataSource)
    val favoriteTickerRepository = FavoriteTickerRepository(dataSource)

    install(WebSockets)

    commonConfig()
    configureCors()

    routing {
        internalNaisRoutes(applicationState)
        configureRouting(favoriteTickerRepository)
    }
}
