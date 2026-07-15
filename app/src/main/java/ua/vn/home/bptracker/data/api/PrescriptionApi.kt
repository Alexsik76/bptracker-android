package ua.vn.home.bptracker.data.api

import retrofit2.http.*
import ua.vn.home.bptracker.data.dto.PrescriptionCreateDto
import ua.vn.home.bptracker.data.dto.PrescriptionPatchDto
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto

interface PrescriptionApi {
    @POST("prescriptions")
    suspend fun createPrescription(@Body body: PrescriptionCreateDto): PrescriptionReadDto

    @GET("prescriptions")
    suspend fun getPrescriptions(): List<PrescriptionReadDto>

    @GET("prescriptions/{id}")
    suspend fun getPrescription(@Path("id") id: String): PrescriptionReadDto

    @PATCH("prescriptions/{id}")
    suspend fun updatePrescription(@Path("id") id: String, @Body body: PrescriptionPatchDto): PrescriptionReadDto

    @DELETE("prescriptions/{id}")
    suspend fun deletePrescription(@Path("id") id: String)
}
