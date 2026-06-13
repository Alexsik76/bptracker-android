package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Mirrors backend records (System.Text.Json camelCase).

@Serializable
data class NativeLoginBeginResponse(
    val challengeId: String,
    val options: JsonElement
)

@Serializable
data class NativeLoginCompleteRequest(
    val challengeId: String,
    val assertion: JsonElement
)

@Serializable
data class NativeLoginResponse(
    val token: String,
    val userId: String,
    val email: String,
    val expiresAt: String
)

@Serializable
data class MeResponse(
    val id: String,
    val email: String
)
