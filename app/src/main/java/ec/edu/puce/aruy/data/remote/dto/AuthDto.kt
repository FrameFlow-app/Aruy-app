package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * Cuerpo de POST /auth/registro. Los campos de prestador van solo si el rol es PRESTADOR;
 * los nulos no se envían (Gson los omite).
 */
data class RegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    @SerializedName("oficio_principal") val oficioPrincipal: String? = null,
    @SerializedName("zona_cobertura") val zonaCobertura: String? = null,
    @SerializedName("descripcion_profesional") val descripcionProfesional: String? = null,
    @SerializedName("anios_experiencia") val aniosExperiencia: Int? = null
)

/** Cuerpo de `POST /perfiles/activar`: "Quiero ofrecer servicios" con la misma cuenta. */
data class ActivarPerfilRequest(
    @SerializedName("oficio_principal") val oficioPrincipal: String,
    @SerializedName("zona_cobertura") val zonaCobertura: String,
    @SerializedName("descripcion_profesional") val descripcionProfesional: String? = null,
    @SerializedName("anios_experiencia") val aniosExperiencia: Int? = null
)

/** Cuerpo de `POST /auth/olvide`: el servidor siempre responde 200 con un mensaje, exista o no el correo. */
data class OlvideRequest(@SerializedName("email") val email: String)

/** Cuerpo de `POST /auth/restablecer`: el código de 6 dígitos que llegó al correo y la contraseña nueva. */
data class RestablecerRequest(
    @SerializedName("email") val email: String,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("password") val password: String
)

data class MensajeServidorDto(@SerializedName("mensaje") val mensaje: String? = null)

/** Respuesta de /auth/login, /auth/registro y /perfiles/activar. */
data class AuthRespuestaDto(
    @SerializedName("token") val token: String,
    @SerializedName("usuario") val usuario: UsuarioDto,
    @SerializedName("perfil_prestador_id") val perfilPrestadorId: String? = null
)
