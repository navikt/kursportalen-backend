package com.example.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.net.URI
import javax.sql.DataSource

object DatabaseFactory {
    private val naisKey = Regex("^NAIS_DATABASE_(.+)_(JDBC_URL|URL|HOST|PORT|DATABASE|USER|USERNAME|PASSWORD)$")

    fun createDataSourceOrThrow(env: Map<String, String> = System.getenv()): DataSource {
        val config = resolveConfig(env) ?: error(
            "No database configuration found. Expected DATABASE_URL/JDBC_DATABASE_URL or NAIS_DATABASE_* variables."
        )

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            maximumPoolSize = 5
            minimumIdle = 1
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        }

        return HikariDataSource(hikariConfig)
    }

    fun runMigrations(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate()
    }

    fun initDataSourceWithRetry(
        env: Map<String, String> = System.getenv(),
        attempts: Int = 20,
        delaySeconds: Long = 3
    ): DataSource {
        var lastError: Throwable? = null

        repeat(attempts) { index ->
            val attempt = index + 1
            var ds: DataSource? = null
            try {
                ds = createDataSourceOrThrow(env)
                runMigrations(ds)
                return ds
            } catch (t: Throwable) {
                lastError = t
                System.err.println("Database init attempt $attempt/$attempts failed: ${t.message}")
                (ds as? HikariDataSource)?.close()
                if (attempt < attempts) Thread.sleep(delaySeconds * 1000)
            }
        }

        error("Database initialization failed after $attempts attempts: ${lastError?.message}")
    }

    private fun resolveConfig(env: Map<String, String>): DbConfig? {
        val directUrl = env["JDBC_DATABASE_URL"] ?: env["DATABASE_URL"]
        if (directUrl != null) {
            val jdbcUrl = normalizeJdbcUrl(directUrl)
            val (uFromUrl, pFromUrl) = extractUserPassFromUrl(directUrl)
            val username = env["JDBC_DATABASE_USERNAME"]
                ?: env["DATABASE_USERNAME"]
                ?: env["DB_USERNAME"]
                ?: uFromUrl
            val password = env["JDBC_DATABASE_PASSWORD"]
                ?: env["DATABASE_PASSWORD"]
                ?: env["DB_PASSWORD"]
                ?: pFromUrl

            if (username.isNullOrBlank() || password.isNullOrBlank()) return null
            return DbConfig(jdbcUrl, username, password)
        }

        val grouped = mutableMapOf<String, MutableMap<String, String>>()
        env.forEach { (key, value) ->
            val m = naisKey.matchEntire(key) ?: return@forEach
            val group = m.groupValues[1]
            val prop = m.groupValues[2]
            grouped.getOrPut(group) { mutableMapOf() }[prop] = value
        }

        return grouped.values.firstNotNullOfOrNull { v ->
            val jdbcUrl = v["JDBC_URL"]
                ?.let(::normalizeJdbcUrl)
                ?: v["URL"]?.let(::normalizeJdbcUrl)
                ?: buildJdbcUrl(v)
                ?: return@firstNotNullOfOrNull null

            val username = v["USERNAME"] ?: v["USER"]
            val password = v["PASSWORD"]
            if (username.isNullOrBlank() || password.isNullOrBlank()) return@firstNotNullOfOrNull null

            DbConfig(jdbcUrl, username, password)
        }
    }

    private fun buildJdbcUrl(values: Map<String, String>): String? {
        val host = values["HOST"] ?: return null
        val port = values["PORT"] ?: "5432"
        val database = values["DATABASE"] ?: return null
        return "jdbc:postgresql://$host:$port/$database"
    }

    private fun normalizeJdbcUrl(url: String): String {
        if (url.startsWith("jdbc:postgresql://")) return url
        if (url.startsWith("jdbc:")) return url

        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            val uri = URI(url)
            val host = uri.host ?: error("Could not parse database host from URL")
            val port = if (uri.port == -1) 5432 else uri.port
            val dbName = (uri.path ?: "").removePrefix("/").ifBlank {
                error("Could not parse database name from URL")
            }
            val query = uri.query?.takeIf { it.isNotBlank() }?.let { "?$it" } ?: ""
            return "jdbc:postgresql://$host:$port/$dbName$query"
        }

        return "jdbc:$url"
    }

    private fun extractUserPassFromUrl(url: String): Pair<String?, String?> =
        runCatching {
            if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
                val userInfo = URI(url).userInfo
                val user = userInfo?.substringBefore(':')
                val pass = userInfo?.substringAfter(':', missingDelimiterValue = "")
                    ?.takeIf { it.isNotBlank() }
                user to pass
            } else {
                null to null
            }
        }.getOrElse { null to null }

    private data class DbConfig(
        val jdbcUrl: String,
        val username: String,
        val password: String
    )
}