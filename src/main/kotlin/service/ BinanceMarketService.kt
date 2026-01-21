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

    private fun str(obj: Map<String, kotlinx.serialization.json.JsonElement>, key: String): String? =
        obj[key]?.jsonPrimitive?.content

    private fun long(obj: Map<String, kotlinx.serialization.json.JsonElement>, key: String): Long? =
        str(obj, key)?.toLongOrNull()

    private fun bool(obj: Map<String, kotlinx.serialization.json.JsonElement>, key: String): Boolean? =
        str(obj, key)?.let { it == "true" || it == "1" }

    fun startBtcDailyStream() {
        if (btcJob?.isActive == true) return

        btcJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    wsClient.webSocket(
                        method = HttpMethod.Get, host = "stream.binance.com", port = 9443, path = "/ws/btcusdt@kline_1d"
                    ) {
                        for (frame in incoming) {
                            val text = (frame as? Frame.Text)?.readText() ?: continue
                            val root = json.parseToJsonElement(text).jsonObject

                            if (str(root, "e") != "kline") continue

                            val eventTime = long(root, "E") ?: System.currentTimeMillis()
                            val symbol = str(root, "s") ?: "BTCUSDT"
                            val k = root["k"]?.jsonObject ?: continue

                            val t = long(k, "t") ?: continue
                            val T = long(k, "T") ?: continue
                            val interval = str(k, "i") ?: "1d"
                            val o = str(k, "o") ?: continue
                            val h = str(k, "h") ?: continue
                            val l = str(k, "l") ?: continue
                            val c = str(k, "c") ?: continue
                            val v = str(k, "v") ?: "0"
                            val isClosed = bool(k, "x") ?: false

                            _btcDailyFlow.tryEmit(
                                CandleUpdate(
                                    symbol = symbol,
                                    interval = interval,
                                    eventTime = eventTime,
                                    t = t,
                                    closeTime = T,
                                    o = o,
                                    h = h,
                                    l = l,
                                    c = c,
                                    v = v,
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
            val t = r.getOrNull(0)?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
            val o = r.getOrNull(1)?.jsonPrimitive?.content ?: return@mapNotNull null
            val h = r.getOrNull(2)?.jsonPrimitive?.content ?: return@mapNotNull null
            val l = r.getOrNull(3)?.jsonPrimitive?.content ?: return@mapNotNull null
            val c = r.getOrNull(4)?.jsonPrimitive?.content ?: return@mapNotNull null
            val v = r.getOrNull(5)?.jsonPrimitive?.content ?: "0"

            Candle(
                t = t, o = o, h = h, l = l, c = c, v = v
            )
        }
    }
}
