package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Mensaje
import ec.edu.puce.aruy.domain.repository.MensajeRepository
import ec.edu.puce.aruy.domain.validation.ValidacionesMensaje

/** Leer el chat: todo, o solo lo posterior a [desde] cuando se está refrescando. */
class ObtenerMensajesUseCase(private val mensajes: MensajeRepository) {
    suspend operator fun invoke(solicitudId: String, desde: String? = null): List<Mensaje> = mensajes.mensajes(solicitudId, desde)
}

/** Escribir en el chat: el texto se limpia y se valida antes de salir del teléfono. */
class EnviarMensajeUseCase(private val mensajes: MensajeRepository) {
    suspend operator fun invoke(solicitudId: String, texto: String): Mensaje {
        ValidacionesMensaje.texto(texto)?.let { throw IllegalArgumentException(it) }
        return mensajes.enviar(solicitudId, texto.trim())
    }
}

/** El prestador comparte su WhatsApp para seguir la conversación fuera de Aruy. */
class CompartirWhatsappUseCase(private val mensajes: MensajeRepository) {
    suspend operator fun invoke(solicitudId: String): Mensaje = mensajes.compartirWhatsapp(solicitudId)
}

/** Junta los mensajes nuevos con los que ya había, sin repetir ninguno y en orden de llegada. */
fun combinarMensajes(actuales: List<Mensaje>, nuevos: List<Mensaje>): List<Mensaje> {
    if (nuevos.isEmpty()) return actuales
    val conocidos = actuales.mapTo(HashSet()) { it.id }
    return actuales + nuevos.filter { conocidos.add(it.id) }
}
