package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Candle(
    val t: Long,
    val o: String,
    val h: String,
    val l: String,
    val c: String,
    val v: String
)

@Serializable
data class CandleUpdate(
    val symbol: String,
    val interval: String,
    @SerialName("eventTime")
    val eventTime: Long,
    val t: Long,
    @SerialName("T")
    val closeTime: Long,
    val o: String,
    val h: String,
    val l: String,
    val c: String,
    val v: String,
    @SerialName("isClosed")
    val isClosed: Boolean
)
