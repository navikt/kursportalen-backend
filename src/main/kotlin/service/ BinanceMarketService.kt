package com.example.service

import com.example.model.Candle
import com.example.model.CandleUpdate
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BinanceMarketService(
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(Apache) {
        expectSuccess = true
    }

    private val wsClient = HttpClient(Apache) {
        install(WebSockets)
    }

    private val _btcDailyFlow = MutableSharedFlow<CandleUpdate>(replay = 1, extraBufferCapacity = 256)
    val btcDailyFlow = _btcDailyFlow.asSharedFlow()

    private var btcJob: Job? = null

    private fun content(obj: Map<String, JsonElement>, key: String): String? =
        obj[key]?.jsonPrimitive?.content

    private fun long(obj: Map<String, JsonElement>, key: String): Long? =
        content(obj, key)?.toLongOrNull()

    private fun bool(obj: Map<String, JsonElement>, key: String): Boolean? =
        content(obj, key)?.let { it == "true" || it == "1" }

    fun startBtcDailyStream() {
        if (btcJob?.isActive == true) return

        btcJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    wsClient.webSocket(
                        method = HttpMethod.Get,
                        host = "stream.binance.com",
                        port = 9443,
                        path = "/ws/btcusdt@kline_1d"
                    ) {
                        for (frame in incoming) {
                            val text = (frame as? Frame.Text)?.readText() ?: continue
                            val root = json.parseToJsonElement(text).jsonObject
                            if (content(root, "e") != "kline") continue

                            val eventTime = long(root, "E") ?: System.currentTimeMillis()
                            val symbol = content(root, "s") ?: "BTCUSDT"
                            val k = root["k"]?.jsonObject ?: continue

                            val startTime = long(k, "t") ?: continue
                            val closeTime = long(k, "T") ?: continue
                            val interval = content(k, "i") ?: "1d"
                            val open = content(k, "o") ?: continue
                            val high = content(k, "h") ?: continue
                            val low = content(k, "l") ?: continue
                            val close = content(k, "c") ?: continue
                            val volume = content(k, "v") ?: "0"
                            val isClosed = bool(k, "x") ?: false

                            _btcDailyFlow.tryEmit(
                                CandleUpdate(
                                    symbol = symbol,
                                    interval = interval,
                                    eventTime = eventTime,
                                    startTime = startTime,
                                    closeTime = closeTime,
                                    open = open,
                                    high = high,
                                    low = low,
                                    close = close,
                                    volume = volume,
                                    isClosed = isClosed
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                }

                delay(1000)
            }
        }
    }

    suspend fun getBtcDailyCandles(limit: Int): List<Candle> {
        val capped = limit.coerceIn(1, 1000)

        val bodyText: String = httpClient.get("https://api.binance.com/api/v3/klines") {
            url {
                parameters.append("symbol", "BTCUSDT")
                parameters.append("interval", "1d")
                parameters.append("limit", capped.toString())
            }
        }.body()

        val arr = json.parseToJsonElement(bodyText).jsonArray

        return arr.mapNotNull { row ->
            val r = row.jsonArray

            val time = r.getOrNull(0)?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
            val open = r.getOrNull(1)?.jsonPrimitive?.content ?: return@mapNotNull null
            val high = r.getOrNull(2)?.jsonPrimitive?.content ?: return@mapNotNull null
            val low = r.getOrNull(3)?.jsonPrimitive?.content ?: return@mapNotNull null
            val close = r.getOrNull(4)?.jsonPrimitive?.content ?: return@mapNotNull null
            val volume = r.getOrNull(5)?.jsonPrimitive?.content ?: "0"

            Candle(
                time = time,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume
            )
        }
    }
}
