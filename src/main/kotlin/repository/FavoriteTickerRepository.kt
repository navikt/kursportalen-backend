package com.example.repository

import javax.sql.DataSource

class FavoriteTickerRepository(
    private val dataSource: DataSource
) {
    fun findByUserId(userId: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT ticker
                FROM user_favorite_ticker
                WHERE user_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, userId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.getString("ticker") else null
                }
            }
        }

    fun upsert(userId: String, ticker: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO user_favorite_ticker (user_id, ticker)
                VALUES (?, ?)
                ON CONFLICT (user_id)
                DO UPDATE SET
                    ticker = EXCLUDED.ticker,
                    updated_at = NOW()
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, ticker)
                statement.executeUpdate()
            }
        }
    }

    fun delete(userId: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                DELETE FROM user_favorite_ticker
                WHERE user_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, userId)
                statement.executeUpdate()
            }
        }
    }
}
