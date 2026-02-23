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
import java.util.concurrent.atomic.AtomicReference

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val applicationState = ApplicationState()
    val favoriteTickerRepositoryRef = AtomicReference<FavoriteTickerRepository?>(null)

    Thread {
        try {
            val dataSource = DatabaseFactory.initDataSourceWithRetry()
            favoriteTickerRepositoryRef.set(FavoriteTickerRepository(dataSource))
            log.info("Favorite ticker repository initialized")
        } catch (error: Throwable) {
            log.error("Failed to initialize favorite ticker repository", error)
        }
    }.apply {
        name = "favorite-ticker-db-init"
        isDaemon = true
        start()
    }

    install(WebSockets)

    commonConfig()
    configureCors()

    routing {
        internalNaisRoutes(applicationState)
        configureRouting { favoriteTickerRepositoryRef.get() }
    }
}
