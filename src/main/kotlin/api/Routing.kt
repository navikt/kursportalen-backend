package com.example.api

import com.example.config.userIdOrNull
import com.example.model.FavoriteTickerRequest
import com.example.model.FavoriteTickerResponse
import com.example.repository.FavoriteTickerRepository
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

fun Application.configureRouting(
    favoriteTickerRepositoryProvider: () -> FavoriteTickerRepository?
) {
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

            get("/user/favorite-ticker") {
                val favoriteTickerRepository = favoriteTickerRepositoryProvider()
                if (favoriteTickerRepository == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")
                    return@get
                }

                val userId = call.userIdOrNull()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val ticker = favoriteTickerRepository.findByUserId(userId)
                if (ticker == null) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.OK, FavoriteTickerResponse(ticker))
                }
            }

            put("/user/favorite-ticker") {
                val favoriteTickerRepository = favoriteTickerRepositoryProvider()
                if (favoriteTickerRepository == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")
                    return@put
                }

                val userId = call.userIdOrNull()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }

                val request = call.receive<FavoriteTickerRequest>()
                val ticker = request.ticker.trim().uppercase()
                if (ticker.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Ticker cannot be blank")
                    return@put
                }

                favoriteTickerRepository.upsert(userId, ticker)
                call.respond(HttpStatusCode.NoContent)
            }

            delete("/user/favorite-ticker") {
                val favoriteTickerRepository = favoriteTickerRepositoryProvider()
                if (favoriteTickerRepository == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")
                    return@delete
                }

                val userId = call.userIdOrNull()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@delete
                }

                favoriteTickerRepository.delete(userId)
                call.respond(HttpStatusCode.NoContent)
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
