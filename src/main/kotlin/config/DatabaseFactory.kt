package com.example.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object DatabaseFactory {
    private val naisDatabaseKey = Regex(
        pattern = "^NAIS_DATABASE_(.+)_(JDBC_URL|URL|HOST|PORT|DATABASE|USERNAME|PASSWORD)$"
    )

    fun createDataSourceOrThrow(env: Map<String, String> = System.getenv()): DataSource {
        val config = resolveConfig(env)
            ?: error(
                "No database configuration found. Expected DATABASE_URL/JDBC_DATABASE_URL or NAIS_DATABASE_* variables."
            )

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            driverClassName = "org.postgresql.Driver"
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
            try {
                val dataSource = createDataSourceOrThrow(env)
                runMigrations(dataSource)
                return dataSource
            } catch (error: Throwable) {
                lastError = error
                System.err.println(
                    "Database init attempt $attempt/$attempts failed: ${error.message}"
                )
                if (attempt < attempts) {
                    Thread.sleep(delaySeconds * 1000)
                }
            }
        }

        error("Database initialization failed after $attempts attempts: ${lastError?.message}")
    }

    private fun resolveConfig(env: Map<String, String>): DbConfig? {
        val directUrl = env["JDBC_DATABASE_URL"] ?: env["DATABASE_URL"]
        if (directUrl != null) {
            val jdbcUrl = if (directUrl.startsWith("jdbc:")) directUrl else "jdbc:$directUrl"
            return DbConfig(
                jdbcUrl = jdbcUrl,
                username = env["JDBC_DATABASE_USERNAME"] ?: env["DATABASE_USERNAME"] ?: env["DB_USERNAME"],
                password = env["JDBC_DATABASE_PASSWORD"] ?: env["DATABASE_PASSWORD"] ?: env["DB_PASSWORD"]
            )
        }

        val grouped = mutableMapOf<String, MutableMap<String, String>>()
        env.forEach { (key, value) ->
            val match = naisDatabaseKey.matchEntire(key) ?: return@forEach
            val group = match.groupValues[1]
            val property = match.groupValues[2]
            grouped.getOrPut(group) { mutableMapOf() }[property] = value
        }

        val preferred = grouped["KURSPORTALEN_DB"]
        val ordered = buildList {
            if (preferred != null) add(preferred)
            addAll(grouped.values.filterNot { it === preferred })
        }

        return ordered.firstNotNullOfOrNull { values ->
            val jdbcUrl = values["JDBC_URL"]
                ?: values["URL"]?.let { if (it.startsWith("jdbc:")) it else "jdbc:$it" }
                ?: buildJdbcUrl(values)
            if (jdbcUrl == null) return@firstNotNullOfOrNull null

            DbConfig(
                jdbcUrl = jdbcUrl,
                username = values["USERNAME"],
                password = values["PASSWORD"]
            )
        }
    }

    private fun buildJdbcUrl(values: Map<String, String>): String? {
        val host = values["HOST"] ?: return null
        val port = values["PORT"] ?: "5432"
        val database = values["DATABASE"] ?: return null
        return "jdbc:postgresql://$host:$port/$database"
    }
}

private data class DbConfig(
    val jdbcUrl: String,
    val username: String?,
    val password: String?
)
