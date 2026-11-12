package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.EditarUsuarioRequest
import ec.edu.puce.aruy.data.remote.dto.UsuarioDto
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    /** Foto de perfil: cuerpo crudo de la imagen (WebP); devuelve mi usuario con la URL pública. */
    @POST("usuarios/yo/foto")
    suspend fun subirMiFoto(@Body imagen: RequestBody): UsuarioDto

    /** Quitar la foto: el cuerpo es `{"foto_perfil": null}` (se envía como JSON crudo para conservar el null). */
    @PATCH("usuarios/yo")
    suspend fun quitarMiFoto(@Body cuerpo: RequestBody): UsuarioDto

    /** Eliminar mi cuenta: el servidor borra la cuenta, el perfil, los servicios, las fotos y las opiniones. */
    @DELETE("usuarios/yo")
    suspend fun eliminarMiCuenta()
}
