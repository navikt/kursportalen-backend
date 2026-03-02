package com.example.config

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

private val preferredHeaders = listOf(
    "x-nav-ident",
    "x-forwarded-email",
    "x-forwarded-user",
    "x-forwarded-preferred-username",
    "x-nav-email",
    "x-user-id"
)

fun ApplicationCall.userIdOrNull(): String? {
    preferredHeaders.forEach { name ->
        val value = request.headers[name]
        if (!value.isNullOrBlank()) return value.trim().lowercase()
    }

    val authorization = request.headers[HttpHeaders.Authorization] ?: return null
    if (!authorization.startsWith("Bearer ", ignoreCase = true)) return null


    val token = authorization.removePrefix("Bearer ").trim()
    return token.takeIf { it.isNotEmpty() }
}
