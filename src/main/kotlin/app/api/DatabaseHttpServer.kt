package app.api

import app.config.AppConfig
import app.api.dto.ErrorMessage
import app.api.dto.HealthStatus
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.event.Level

class DatabaseHttpServer(
    private val appConfig: AppConfig
) {
    private var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun start(wait: Boolean = true) {
        val server = embeddedServer(
            factory = Netty,
            port = appConfig.serverPort,
            host = appConfig.serverHost
        ) {
            configurePlugins()
            configureRoutes()
        }
        engine = server
        server.start(wait = wait)
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 1_000, timeoutMillis = 2_000)
        engine = null
    }
}

private fun Application.configurePlugins() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorMessage(cause.message ?: "Invalid request")
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorMessage(cause.message ?: "Internal server error")
            )
        }
    }
}

private fun Application.configureRoutes() {
    routing {
        get("/health") {
            val databaseOk = runCatching {
                transaction {
                    exec("SELECT 1") { }
                }
            }.isSuccess
            val status = if (databaseOk) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(
                status,
                HealthStatus(
                    status = if (databaseOk) "UP" else "DOWN",
                    database = databaseOk
                )
            )
        }
    }
}
