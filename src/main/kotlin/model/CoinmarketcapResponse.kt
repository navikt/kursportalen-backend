package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ApiResponse(
    val data: Map<String, Coin>
)

@Serializable
data class Coin(
    val symbol: String,
    @SerialName("last_updated")
    val lastUpdated: String,
    val quote: Quote
)

@Serializable
data class Quote(
    val USD: Usd
)

@Serializable
data class Usd(
    val price: Double,
    @SerialName("percent_change_1h")
    val percentChange1h: Double
)