package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Crypto(
    val symbol: String,
    val last: Double,
    val lastUpdated: String,
    val percentChange1h: Double
)
