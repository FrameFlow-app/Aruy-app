package ec.edu.puce.aruy.data.remote.util

import retrofit2.HttpException

/** Mensajes en español para los errores de editar el perfil, darse de baja y Aruy Pro. */
object MensajesPerfil {

    /** `PATCH /usuarios/yo` y `PATCH /perfiles/yo`. */
    fun alGuardarPerfil(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa los datos: hay algún campo incorrecto."
                404 -> return "Todavía no tienes perfil de prestador."
                409 -> return "Ese celular ya está registrado en otra cuenta."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    /** `POST /usuarios/yo/foto` y quitar la foto con `PATCH /usuarios/yo`. */
    fun alSubirFoto(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400 -> return "No pudimos leer esa imagen. Prueba con otra foto."
                413 -> return "La foto pesa demasiado. Elige una de menos de 5 MB."
                415 -> return "Ese formato no es válido. Usa una foto JPG, PNG o WebP."
                422 -> return "No pudimos reducir la foto. Prueba con otra más pequeña."
                502, 503 -> return "El almacenamiento de fotos no está disponible ahora. Inténtalo en unos minutos."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    /** `POST /suscripciones` y `PATCH /suscripciones/{id}/cancelar`. */
    fun enSuscripciones(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Elige uno de los planes."
                403 -> return "Aruy Pro es solo para prestadores."
                404 -> return "No encontramos tu suscripción."
                409 -> return "Ya tienes una suscripción pendiente o activa."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }
}
