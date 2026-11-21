package ec.edu.puce.aruy.domain.solicitudes

import ec.edu.puce.aruy.domain.model.ResumenSolicitudes

/** Un aviso del sistema listo para mostrar. [clave] distingue el tipo: un aviso nuevo del mismo tipo reemplaza al anterior. */
data class TextoAviso(val clave: Int, val titulo: String, val texto: String) {
    companion object {
        const val RECIBIDAS = 1
        const val ENVIADAS = 2
        const val MENSAJES = 3
    }
}

/** "Tienes 1 mensaje nuevo" / "Tienes N mensajes nuevos". */
fun textoMensajesNuevos(cantidad: Int): String =
    if (cantidad == 1) "Tienes 1 mensaje nuevo" else "Tienes $cantidad mensajes nuevos"

/**
 * Decide cuándo avisar con una notificación. Solo se avisa cuando hay MÁS novedades que la última vez que se avisó:
 * así no se repite el mismo aviso cada 15 minutos mientras la persona no abre la app.
 */
object PoliticaAvisos {
    fun textos(
        actual: ResumenSolicitudes,
        ultimasRecibidas: Int,
        ultimasEnviadas: Int,
        ultimosMensajes: Int = 0
    ): List<TextoAviso> = buildList {
        if (actual.mensajesSinLeer > ultimosMensajes) {
            add(TextoAviso(TextoAviso.MENSAJES, "Mensajes nuevos", textoMensajesNuevos(actual.mensajesSinLeer) + ". Respóndelos en Aruy."))
        }
        if (actual.recibidasNuevas > ultimasRecibidas) {
            add(
                if (actual.recibidasNuevas == 1) {
                    TextoAviso(TextoAviso.RECIBIDAS, "Nueva solicitud", "Alguien te pidió uno de tus servicios. Respóndele en Aruy.")
                } else {
                    TextoAviso(TextoAviso.RECIBIDAS, "Nuevas solicitudes", "Tienes ${actual.recibidasNuevas} solicitudes nuevas. Respóndelas en Aruy.")
                }
            )
        }
        if (actual.enviadasNovedades > ultimasEnviadas) {
            add(
                if (actual.enviadasNovedades == 1) {
                    TextoAviso(TextoAviso.ENVIADAS, "Respondieron tu solicitud", "Mira qué te contestó el prestador.")
                } else {
                    TextoAviso(TextoAviso.ENVIADAS, "Respondieron tus solicitudes", "Tienes ${actual.enviadasNovedades} respuestas nuevas.")
                }
            )
        }
    }
}
