package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.CambiarEstadoSolicitudRequest
import ec.edu.puce.aruy.data.remote.dto.CrearSolicitudRequest
import ec.edu.puce.aruy.data.remote.dto.MarcadasDto
import ec.edu.puce.aruy.data.remote.dto.MarcarVistasRequest
import ec.edu.puce.aruy.data.remote.dto.ResumenSolicitudesDto
import ec.edu.puce.aruy.data.remote.dto.SolicitudDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Solicitudes dentro de la app (el servidor las crea y las guarda). */
interface SolicitudApi {

    @POST("solicitudes")
    suspend fun crear(@Body cuerpo: CrearSolicitudRequest): SolicitudDto

    /** `tipo`: "enviadas" o "recibidas". */
    @GET("solicitudes")
    suspend fun listar(@Query("tipo") tipo: String): List<SolicitudDto>

    @PATCH("solicitudes/{id}")
    suspend fun cambiarEstado(@Path("id") id: String, @Body cuerpo: CambiarEstadoSolicitudRequest): SolicitudDto

    @GET("solicitudes/resumen")
    suspend fun resumen(): ResumenSolicitudesDto

    @POST("solicitudes/vistas")
    suspend fun marcarVistas(@Body cuerpo: MarcarVistasRequest): MarcadasDto
}
