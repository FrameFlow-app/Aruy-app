package ec.edu.puce.aruy.data.sesion

import android.util.Log

/**
 * Deja constancia en Logcat (etiqueta "AruySesion") de POR QUÉ no se recuperó o se cerró la sesión.
 * Nunca escribe el token ni la contraseña. Si Logcat no está disponible (pruebas en la JVM), no hace nada.
 */
internal object RegistroSesion {
    private const val ETIQUETA = "AruySesion"

    fun info(mensaje: String) {
        runCatching { Log.i(ETIQUETA, mensaje) }
    }

    fun aviso(mensaje: String) {
        runCatching { Log.w(ETIQUETA, mensaje) }
    }
}
