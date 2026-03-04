package com.example.api.controller

import com.example.config.userIdOrNull
import com.example.model.FavoriteTickerRequest
import com.example.model.FavoriteTickerResponse
import com.example.service.DeleteFavoriteTickerResult
import com.example.service.FavoriteTickerService
import com.example.service.SaveFavoriteTickerResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond

class FavoriteTickerController(
    private val favoriteTickerService: FavoriteTickerService
) {
    suspend fun getFavoriteTicker(call: ApplicationCall) {
        if (!favoriteTickerService.isStorageReady()) {
            call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")
            return
        }

        val userId = call.userIdOrNull()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        val ticker = favoriteTickerService.findByUserId(userId)
        if (ticker == null) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.OK, FavoriteTickerResponse(ticker))
        }
    }

    suspend fun putFavoriteTicker(call: ApplicationCall) {
        if (!favoriteTickerService.isStorageReady()) {
            call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")
            return
        }

        val userId = call.userIdOrNull()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        val request = call.receive<FavoriteTickerRequest>()
        when (favoriteTickerService.save(userId, request.ticker)) {
            SaveFavoriteTickerResult.StorageUnavailable ->
                call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")

            SaveFavoriteTickerResult.InvalidTicker ->
                call.respond(HttpStatusCode.BadRequest, "Ticker cannot be blank")

            SaveFavoriteTickerResult.Saved ->
                call.respond(HttpStatusCode.NoContent)
        }
    }

    suspend fun deleteFavoriteTicker(call: ApplicationCall) {
        if (!favoriteTickerService.isStorageReady()) {
            call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")
            return
        }

        val userId = call.userIdOrNull()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        when (favoriteTickerService.delete(userId)) {
            DeleteFavoriteTickerResult.StorageUnavailable ->
                call.respond(HttpStatusCode.ServiceUnavailable, "Favorite ticker storage is not ready")

            DeleteFavoriteTickerResult.Deleted ->
                call.respond(HttpStatusCode.NoContent)
        }
    }
}
