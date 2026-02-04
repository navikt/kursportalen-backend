package com.example

import com.example.api.configureRouting
import com.example.config.ApplicationState
import com.example.config.commonConfig
import com.example.config.internalNaisRoutes
import com.example.config.configureCors
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module(
    priceProvider: suspend (com.example.model.PriceRequest) -> com.example.model.Crypto = com.example.service.CryptoService()::getPrice,
    binanceMarketService: com.example.service.BinanceMarketService? = null,
    startBinanceStream: Boolean = true
) {
    val applicationState = ApplicationState()

    install(WebSockets)

    commonConfig()
    configureCors()

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val activeBinanceService = binanceMarketService ?: com.example.service.BinanceMarketService(appScope)
    if (startBinanceStream) {
        activeBinanceService.startBtcDailyStream()
    }

    routing {
        internalNaisRoutes(applicationState)
        configureRouting(
            priceProvider = priceProvider,
            binanceMarketService = activeBinanceService
        )
    }
}
