package app

import app.config.AppConfig
import app.db.DatabaseFactory
import app.server.DatabaseHttpServer

fun main() {
    val config = AppConfig.load()
    val databaseFactory = DatabaseFactory(config)
    databaseFactory.connect()
    Runtime.getRuntime().addShutdownHook(
        Thread {
            databaseFactory.close()
        }
    )

    DatabaseHttpServer(config).start(wait = true)
}
