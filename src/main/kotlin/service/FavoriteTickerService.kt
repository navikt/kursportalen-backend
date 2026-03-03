package com.example.service

import com.example.repository.FavoriteTickerRepository

class FavoriteTickerService(
    private val favoriteTickerRepositoryProvider: () -> FavoriteTickerRepository?
) {
    fun findByUserId(userId: String): String? =
        favoriteTickerRepositoryProvider()?.findByUserId(userId)

    fun isStorageReady(): Boolean = favoriteTickerRepositoryProvider() != null

    fun save(userId: String, tickerInput: String): SaveFavoriteTickerResult {
        val repository = favoriteTickerRepositoryProvider() ?: return SaveFavoriteTickerResult.StorageUnavailable
        val ticker = tickerInput.trim().uppercase()

        if (ticker.isBlank()) return SaveFavoriteTickerResult.InvalidTicker

        repository.upsert(userId, ticker)
        return SaveFavoriteTickerResult.Saved
    }

    fun delete(userId: String): DeleteFavoriteTickerResult {
        val repository = favoriteTickerRepositoryProvider() ?: return DeleteFavoriteTickerResult.StorageUnavailable
        repository.delete(userId)
        return DeleteFavoriteTickerResult.Deleted
    }
}

enum class SaveFavoriteTickerResult {
    StorageUnavailable,
    InvalidTicker,
    Saved,
}

enum class DeleteFavoriteTickerResult {
    StorageUnavailable,
    Deleted,
}
