package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteTickerResponse(
    @SerialName("ticker")
    val ticker: String
)

@Serializable
data class FavoriteTickerRequest(
    @SerialName("ticker")
    val ticker: String
)
