package com.example.api

import com.example.api.controller.BtcMarketController
import com.example.api.controller.FavoriteTickerController
import com.example.api.controller.PriceController
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

fun Application.configureRouting(
    priceController: PriceController,
    favoriteTickerController: FavoriteTickerController,
    btcMarketController: BtcMarketController,
) {
    routing {
        route("/api/v1") {
            post("/price") {
                priceController.getPrice(call)
            }

            get("/user/favorite-ticker") {
                favoriteTickerController.getFavoriteTicker(call)
            }

            put("/user/favorite-ticker") {
                favoriteTickerController.putFavoriteTicker(call)
            }

            delete("/user/favorite-ticker") {
                favoriteTickerController.deleteFavoriteTicker(call)
            }

            get("/candles/btc") {
                btcMarketController.getBtcCandles(call)
            }

            webSocket("/live/btc") {
                btcMarketController.streamBtcDaily(this)
            }
        }
    }
}
