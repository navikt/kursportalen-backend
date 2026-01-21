package com.example.api

import com.example.model.PriceRequest
import com.example.service.BinanceMarketService
import com.example.service.CryptoService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    val cryptoService = CryptoService()

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val binanceMarketService = BinanceMarketService(appScope)
    binanceMarketService.startBtcDailyStream()

    val json = Json { ignoreUnknownKeys = true }

    routing {
        route("/api/v1") {
            post("/price") {
                val request = call.receive<PriceRequest>()
                val response = cryptoService.getPrice(request)
                call.respond(HttpStatusCode.OK, response)
            }

            get("/candles/btc") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 365
                val candles = binanceMarketService.getBtcDailyCandles(limit)
                call.respond(HttpStatusCode.OK, candles)
            }

            webSocket("/live/btc") {
                val job = launch {
                    binanceMarketService.btcDailyFlow.collectLatest { update ->
                        send(Frame.Text(json.encodeToString(update)))
                    }
                }

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Close) break
                    }
                } finally {
                    job.cancel()
                }
            }
        }
    }
}
