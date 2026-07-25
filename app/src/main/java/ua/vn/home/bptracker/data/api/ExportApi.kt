package ua.vn.home.bptracker.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ua.vn.home.bptracker.data.dto.ExportRequest
import ua.vn.home.bptracker.data.dto.ExportResponse

interface ExportApi {
    @POST("export/csv")
    suspend fun exportCsv(@Body body: ExportRequest): Response<ExportResponse>
}
