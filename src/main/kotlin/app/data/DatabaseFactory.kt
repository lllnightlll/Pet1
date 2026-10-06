package app.data

import app.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory

class DatabaseFactory(private val appConfig: AppConfig) {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private var dataSource: HikariDataSource? = null

    fun connect() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = appConfig.databaseUrl
            driverClassName = appConfig.databaseDriver
            username = appConfig.databaseUser
            password = appConfig.databasePassword
            maximumPoolSize = appConfig.databasePoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val hikariDataSource = HikariDataSource(hikariConfig)
        dataSource = hikariDataSource
        Database.connect(hikariDataSource)
        logger.info("PostgreSQL connected: {}", appConfig.databaseUrl)
    }

    fun close() {
        dataSource?.close()
        dataSource = null
    }
}
