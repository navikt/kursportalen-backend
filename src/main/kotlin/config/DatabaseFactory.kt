package com.example.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.sql.DataSource

object DatabaseFactory {
    private const val expectedDatabaseName = "kursportalen-db"

    private val naisKey = Regex(
        "^NAIS_DATABASE_(.+)_(JDBC_URL|URL|HOST|PORT|DATABASE|USER|USERNAME|PASSWORD)$"
    )

    fun createDataSourceOrThrow(env: Map<String, String> = System.getenv()): DataSource {
        val config = resolveConfig(env) ?: error(
            "No database configuration found. Expected DATABASE_URL/JDBC_DATABASE_URL or NAIS_DATABASE_* variables."
        )

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 2
            minimumIdle = 0
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
                rootCause(t)?.let { cause ->
                    System.err.println("Root cause: ${cause.javaClass.simpleName}: ${cause.message}")
                }
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

        val ordered = grouped.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, MutableMap<String, String>>> { entry ->
                    entry.value["DATABASE"] == expectedDatabaseName
                }.thenByDescending { entry ->
                    entry.key.contains("KURSPORTALEN", ignoreCase = true)
                }
            )
            .map { it.value }

        return ordered.firstNotNullOfOrNull { v ->
            val jdbcUrl = v["JDBC_URL"]
                ?.let(::normalizeJdbcUrl)
                ?: v["URL"]?.let(::normalizeJdbcUrl)
                ?: buildJdbcUrl(v)
                ?: return@firstNotNullOfOrNull null

            val username = v["USERNAME"] ?: v["USER"]
            val password = v["PASSWORD"]
            if (username.isNullOrBlank() || password.isNullOrBlank()) return@firstNotNullOfOrNull null

            System.err.println(
                "Selected NAIS DB config for database='${v["DATABASE"] ?: "unknown"}' host='${v["HOST"] ?: "n/a"}'"
            )

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
        if (url.startsWith("jdbc:postgresql://")) return sanitizeJdbcQuery(url)
        if (url.startsWith("jdbc:")) return url

        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            val uri = URI(url)
            val host = uri.host ?: error("Could not parse database host from URL")
            val port = if (uri.port == -1) 5432 else uri.port
            val dbName = (uri.path ?: "").removePrefix("/").ifBlank {
                error("Could not parse database name from URL")
            }
            val query = sanitizeQueryString(uri.query)
            return "jdbc:postgresql://$host:$port/$dbName$query"
        }

        return "jdbc:$url"
    }

    private fun sanitizeJdbcQuery(jdbcUrl: String): String {
        val prefix = "jdbc:postgresql://"
        if (!jdbcUrl.startsWith(prefix)) return jdbcUrl

        val withoutPrefix = jdbcUrl.removePrefix(prefix)
        val queryStart = withoutPrefix.indexOf('?')
        if (queryStart == -1) return jdbcUrl

        val base = withoutPrefix.substring(0, queryStart)
        val query = withoutPrefix.substring(queryStart + 1)
        val sanitized = sanitizeQueryString(query)
        return "$prefix$base$sanitized"
    }

    private fun sanitizeQueryString(query: String?): String {
        if (query.isNullOrBlank()) return ""

        val rewritten = query
            .split("&")
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                val rawKey = if (idx >= 0) part.substring(0, idx) else part
                val key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8).lowercase()

                if (idx >= 0 && key == "sslkey") {
                    val value = part.substring(idx + 1)
                    val decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8)
                    val pk8Candidate = decodedValue.replace("key.pem", "key.pk8")
                    if (pk8Candidate != decodedValue && File(pk8Candidate).exists()) {
                        return@mapNotNull "${part.substring(0, idx + 1)}$pk8Candidate"
                    }
                }
                part
            }

        if (rewritten.isEmpty()) return ""
        return "?${rewritten.joinToString("&")}"
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

private fun rootCause(error: Throwable): Throwable? {
    var current: Throwable? = error
    var last: Throwable? = null
    while (current != null) {
        last = current
        current = current.cause
    }
    return last
}
