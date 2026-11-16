package ec.edu.puce.aruy.data.remote.api

import retrofit2.http.POST
import okhttp3.RequestBody
import ec.edu.puce.aruy.data.remote.dto.EditarUsuarioRequest
import ec.edu.puce.aruy.data.remote.dto.UsuarioDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UsuarioApi {

    @GET("usuarios/{id}")
    suspend fun obtenerUsuario(
        @Path("id") id: String
    ): UsuarioDto

    /** Mis datos; es la única consulta que incluye mi correo. */
    @GET("usuarios/yo")
    suspend fun miUsuario(): UsuarioDto

    @PATCH("usuarios/yo")
    suspend fun editarMiUsuario(@Body cuerpo: EditarUsuarioRequest): UsuarioDto

    /** Lo mismo con un JSON que conserva los `null` (para quitar la foto: `{"foto_perfil": null}`). */
    @PATCH("usuarios/yo")
    suspend fun editarMiUsuarioJson(@Body cuerpo: RequestBody): UsuarioDto

    /** Mi foto de perfil: el cuerpo es la imagen (WebP); el servidor responde con mi usuario ya actualizado. */
    @POST("usuarios/yo/foto")
    suspend fun subirMiFoto(@Body imagen: RequestBody): UsuarioDto

    /** Eliminar mi cuenta: el servidor borra la cuenta, el perfil, los servicios, las fotos y las opiniones. */
    @DELETE("usuarios/yo")
    suspend fun eliminarMiCuenta()
}
