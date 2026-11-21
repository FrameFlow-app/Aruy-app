package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.Mensaje

/** Contrato del chat de una solicitud. */
interface MensajeRepository {
    /** Todos los mensajes, o solo los posteriores a [desde] (fecha ISO del último que ya se tiene). */
    suspend fun mensajes(solicitudId: String, desde: String? = null): List<Mensaje>

    suspend fun enviar(solicitudId: String, texto: String): Mensaje

    /** Solo el prestador: comparte su WhatsApp en el chat. */
    suspend fun compartirWhatsapp(solicitudId: String): Mensaje
}
