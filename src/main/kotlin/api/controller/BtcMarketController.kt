package com.example.api.controller

import com.example.service.BinanceMarketService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class BtcMarketController(
    private val binanceMarketService: BinanceMarketService
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getBtcCandles(call: ApplicationCall) {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 365
        val candles = binanceMarketService.getBtcDailyCandles(limit)
        call.respond(HttpStatusCode.OK, candles)
    }

    suspend fun streamBtcDaily(session: DefaultWebSocketServerSession) = with(session) {
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
