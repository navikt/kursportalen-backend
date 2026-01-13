package com.example.service

import com.example.model.Crypto
import com.example.model.ApiResponse
import com.example.model.PriceRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class BtcPriceService {

    private val apiKey = System.getenv("COINMARKETCAP_APIKEY") ?: ""

    private val client = HttpClient(Apache) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }


    suspend fun getBtcPrice(): Crypto {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Missing COINMARKETCAP_APIKEY")
        }

        val response: ApiResponse = client.get("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest") {
            url { parameters.append("symbol", "BTC") }
            header("X-CMC_PRO_API_KEY", apiKey)
        }.body()

        val btc = response.data["BTC"] ?: throw IllegalStateException("BTC not found in response")

        return Crypto(
            symbol = btc.symbol,
            last = btc.quote.USD.price,
            lastUpdated = btc.lastUpdated,
            percentChange1h = btc.quote.USD.percentChange1h
        )
    }


    suspend fun getPrice(request: PriceRequest): Crypto {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Missing COINMARKETCAP_APIKEY")
        }

        val response: ApiResponse = client.get("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest") {
            url { parameters.append("symbol", { request.ticker }.toString()) }
            header("X-CMC_PRO_API_KEY", apiKey)
        }.body()

        val price = response.data[""] ?: throw IllegalStateException("Price not found")

        return Crypto(
            symbol = price.symbol,
            last = price.quote.USD.price,
            lastUpdated = price.lastUpdated,
            percentChange1h = price.quote.USD.percentChange1h
        )
    }
}

