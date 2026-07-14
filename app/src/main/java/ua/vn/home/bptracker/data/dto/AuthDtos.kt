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

@Serializable
data class MagicLinkRequest(val email: String)

@Serializable
data class MagicLinkConfirmRequest(val token: String)

@Serializable
data class WebAuthnCredentialDto(
    val id: String,
    val label: String? = null,
    val transports: List<String>? = null,
    val createdAt: String,
    val lastUsedAt: String? = null,
)
