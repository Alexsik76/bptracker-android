package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExportRequest(
    val tz: String
)

@Serializable
data class ExportResponse(
    val message: String,
    val email: String
)
