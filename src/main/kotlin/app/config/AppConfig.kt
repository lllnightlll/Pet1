package app.config

import java.util.Properties

class AppConfig(
    val serverHost: String,
    val serverPort: Int,
    val databaseUrl: String,
    val databaseDriver: String,
    val databaseUser: String,
    val databasePassword: String,
    val databasePoolSize: Int
) {
    companion object {
        private const val RESOURCE_NAME = "/application.properties"

        fun load(): AppConfig {
            val properties = loadClasspathProperties()
            return AppConfig(
                serverHost = read(properties, "server.host"),
                serverPort = read(properties, "server.port").toInt(),
                databaseUrl = read(properties, "database.url"),
                databaseDriver = read(properties, "database.driver"),
                databaseUser = read(properties, "database.user"),
                databasePassword = read(properties, "database.password"),
                databasePoolSize = read(properties, "database.poolSize").toInt()
            )
        }

        private fun loadClasspathProperties(): Properties {
            val stream = AppConfig::class.java.getResourceAsStream(RESOURCE_NAME)
                ?: error("Resource $RESOURCE_NAME not found")
            return stream.use { input ->
                Properties().apply { load(input) }
            }
        }

        private fun read(properties: Properties, key: String): String {
            val envName = key.replace('.', '_').uppercase()
            val raw = System.getenv(envName) ?: properties.getProperty(key)
            return raw?.trim() ?: error("Value for key $key (or $envName) is not set")
        }
    }
}
