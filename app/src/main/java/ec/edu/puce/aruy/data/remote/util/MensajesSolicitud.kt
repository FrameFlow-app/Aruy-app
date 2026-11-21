package ec.edu.puce.aruy.data.remote.util

import retrofit2.HttpException

/** Mensajes en español para los errores de las solicitudes. */
object MensajesSolicitud {

    fun alEnviar(error: Exception): String {
        if (error is IllegalArgumentException) return error.message ?: "Revisa tu mensaje."
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa tu mensaje e inténtalo de nuevo."
                403 -> return "No puedes enviar una solicitud a tu propio servicio."
                404 -> return "Este servicio ya no está disponible."
                409 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Ya tienes una solicitud pendiente para este servicio."
                429 -> return "Enviaste muchas solicitudes seguidas. Intenta de nuevo en un rato."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    fun alCambiar(error: Exception): String {
        if (error is IllegalArgumentException) return error.message ?: "Esa acción no es posible."
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa los datos e inténtalo de nuevo."
                404 -> return "Esta solicitud ya no existe."
                409 -> return "Esta solicitud cambió mientras la mirabas. Actualiza la lista."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    fun alListar(error: Exception): String = ApiErrorHandler.obtenerMensaje(error)

    /** Leer el chat. */
    fun alAbrirChat(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                403 -> return "No tienes acceso a esta conversación."
                404 -> return "Esta conversación ya no existe."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    /** Enviar un mensaje. */
    fun alEscribir(error: Exception): String {
        if (error is IllegalArgumentException) return error.message ?: "Revisa tu mensaje."
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa tu mensaje e inténtalo de nuevo."
                403 -> return "No tienes acceso a esta conversación."
                404 -> return "Esta conversación ya no existe."
                409 -> return "Esta solicitud ya está cerrada: no se pueden enviar más mensajes."
                429 -> return "Estás escribiendo muy rápido. Espera un momento e inténtalo de nuevo."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    /** Compartir el WhatsApp del prestador. */
    fun alCompartirWhatsapp(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error)
                    ?: "Tu perfil no tiene un teléfono válido. Agrégalo en Editar perfil."
                403 -> return "Solo quien ofrece el servicio puede compartir su WhatsApp."
                409 -> return "Esta solicitud ya está cerrada."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    /** Olvidé mi contraseña: el servidor explica qué falló (código vencido, incorrecto...). */
    fun alRecuperar(error: Exception): String {
        if (error is IllegalArgumentException) return error.message ?: "Revisa los datos."
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa el código y la contraseña."
                429 -> return "Hiciste muchos intentos. Espera unos minutos e inténtalo de nuevo."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }
}
