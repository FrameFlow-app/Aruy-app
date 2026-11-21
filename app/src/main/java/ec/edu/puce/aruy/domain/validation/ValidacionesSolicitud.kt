package ec.edu.puce.aruy.domain.validation

/** Reglas de los textos de una solicitud (el servidor las vuelve a comprobar). */
object ValidacionesSolicitud {
    const val MENSAJE_MIN = 5
    const val MENSAJE_MAX = 500
    const val RESPUESTA_MAX = 300

    /** Mensaje de error, o null si el texto sirve. */
    fun mensaje(texto: String): String? {
        val largo = texto.trim().length
        return when {
            largo < MENSAJE_MIN -> "Cuéntale qué necesitas (mínimo $MENSAJE_MIN letras)."
            largo > MENSAJE_MAX -> "Es muy largo: máximo $MENSAJE_MAX caracteres."
            else -> null
        }
    }

    fun respuesta(texto: String): String? =
        if (texto.trim().length > RESPUESTA_MAX) "La respuesta es muy larga: máximo $RESPUESTA_MAX caracteres." else null
}
