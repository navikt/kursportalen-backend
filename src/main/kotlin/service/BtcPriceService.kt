package com.example.service

import com.example.model.Crypto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class BtcPriceService {

    private val apiKey = System.getenv("FREECRYPTO_API_KEY") ?: ""

    private val client = HttpClient(Apache) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Serializable
    private data class ApiResponse(
        val symbols: List<ApiSymbol>
    )

    @Serializable
    private data class ApiSymbol(
        val symbol: String,
        val last: String
    )

    suspend fun getBtcPrice(): Crypto {
        val response: ApiResponse =
            client.get("https://api.freecryptoapi.com/v1/getData") {
                url { parameters.append("symbol", "BTC") }
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }.body()

        val btc = response.symbols.first()

        return Crypto(symbol = btc.symbol, last = btc.last)
    }
}
