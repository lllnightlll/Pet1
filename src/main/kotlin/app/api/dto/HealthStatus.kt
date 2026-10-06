package app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    val status: String,
    val database: Boolean
)