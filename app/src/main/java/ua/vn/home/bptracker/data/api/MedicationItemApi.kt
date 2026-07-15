package ua.vn.home.bptracker.data.api

import retrofit2.http.*
import ua.vn.home.bptracker.data.dto.MedicationItemCreateDto
import ua.vn.home.bptracker.data.dto.MedicationItemPatchDto
import ua.vn.home.bptracker.data.dto.MedicationItemReadDto

interface MedicationItemApi {
    @POST("prescriptions/{prescription_id}/items")
    suspend fun createItem(
        @Path("prescription_id") prescriptionId: String,
        @Body body: MedicationItemCreateDto
    ): MedicationItemReadDto

    @GET("prescriptions/{prescription_id}/items")
    suspend fun getItems(@Path("prescription_id") prescriptionId: String): List<MedicationItemReadDto>

    @GET("prescriptions/{prescription_id}/items/{item_id}")
    suspend fun getItem(
        @Path("prescription_id") prescriptionId: String,
        @Path("item_id") itemId: String
    ): MedicationItemReadDto

    @PATCH("prescriptions/{prescription_id}/items/{item_id}")
    suspend fun updateItem(
        @Path("prescription_id") prescriptionId: String,
        @Path("item_id") itemId: String,
        @Body body: MedicationItemPatchDto
    ): MedicationItemReadDto

    @DELETE("prescriptions/{prescription_id}/items/{item_id}")
    suspend fun deleteItem(
        @Path("prescription_id") prescriptionId: String,
        @Path("item_id") itemId: String
    )
}
