package com.example.service

import com.example.model.Crypto
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class BtcPriceService {

    private val apiKey = System.getenv("COINMARKETCAP_APIKEY") ?: ""

    private val client = HttpClient(Apache) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Serializable
    private data class ApiResponse(val data: Map<String, Coin>)

    @Serializable
    private data class Coin(val symbol: String, val quote: Quote)

    @Serializable
    private data class Quote(val USD: Usd)

    @Serializable
    private data class Usd(val price: Double)

    suspend fun getBtcPrice(): Crypto {
        if (apiKey.isBlank()) throw IllegalStateException("Missing COINMARKETCAP_APIKEY")

        val resp: HttpResponse =
            client.get("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest") {
                url { parameters.append("symbol", "BTC") }
                header("X-CMC_PRO_API_KEY", apiKey)
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }

        if (!resp.status.isSuccess()) {
            throw IllegalStateException("CoinMarketCap ${resp.status.value}: ${resp.bodyAsText()}")
        }

        val parsed: ApiResponse = resp.body()
        val btc = parsed.data["BTC"] ?: throw IllegalStateException("BTC missing in response")

        return Crypto(symbol = btc.symbol, last = btc.quote.USD.price)
    }
}
