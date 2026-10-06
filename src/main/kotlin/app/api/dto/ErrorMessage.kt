package app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorMessage(
    val error: String
)
