package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementDto(
    val id: String,
    val recordedAt: String,
    val sys: Int,
    val dia: Int,
    val pulse: Int
)

@Serializable
data class CreateMeasurementRequest(
    val sys: Int,
    val dia: Int,
    val pulse: Int
)

@Serializable
data class OcrResponse(
    val sys: Int,
    val dia: Int,
    val pulse: Int
)
