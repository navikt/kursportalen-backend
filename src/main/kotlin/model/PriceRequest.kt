package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class PriceRequest(
    val ticker: String
)
