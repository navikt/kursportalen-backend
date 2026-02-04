package com.example

import com.example.model.Crypto
import com.example.model.PriceRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceRouteTest {
    @Test
    fun `POST api v1 price returns crypto payload`() = testApplication {
        application {
            module(
                priceProvider = { request ->
                    Crypto(
                        symbol = request.ticker.uppercase(),
                        last = 123.45,
                        lastUpdated = "2026-02-04T12:34:56Z",
                        percentChange1h = 1.23
                    )
                },
                startBinanceStream = false
            )
        }

        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/v1/price") {
            contentType(ContentType.Application.Json)
            setBody(PriceRequest("btc"))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<Crypto>()
        assertEquals("BTC", body.symbol)
        assertEquals(123.45, body.last)
        assertEquals("2026-02-04T12:34:56Z", body.lastUpdated)
        assertEquals(1.23, body.percentChange1h)
    }
}
