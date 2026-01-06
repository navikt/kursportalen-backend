package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Crypto(
    val symbol: String,
    val last: Double,
    @SerialName("last_updated")
    val lastUpdated: String,
    @SerialName("percent_change_1h")
    val percentChange1h: Double
)
