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
}
