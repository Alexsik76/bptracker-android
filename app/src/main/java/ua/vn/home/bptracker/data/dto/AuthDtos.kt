package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class MeResponse(
    val id: String,
    val email: String,
    val createdAt: String,
)
