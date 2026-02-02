package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Candle(
    @SerialName("time")
    val time: Long,
    @SerialName("open")
    val open: String,
    @SerialName("high")
    val high: String,
    @SerialName("low")
    val low: String,
    @SerialName("close")
    val close: String,
    @SerialName("volume")
    val volume: String
)

@Serializable
data class CandleUpdate(
    val symbol: String,
    val interval: String,
    val eventTime: Long,
    @SerialName("startTime")
    val startTime: Long,
    val closeTime: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val isClosed: Boolean
)
