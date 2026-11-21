package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.AvisosDto
import retrofit2.http.GET

/** Avisos públicos del servidor (no necesitan token). */
interface AvisosApi {
    @GET("avisos")
    suspend fun avisos(): AvisosDto
}
