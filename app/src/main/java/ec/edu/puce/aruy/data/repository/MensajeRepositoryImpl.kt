package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.remote.api.SolicitudApi
import ec.edu.puce.aruy.data.remote.dto.EnviarMensajeRequest
import ec.edu.puce.aruy.data.remote.dto.MensajeDto
import ec.edu.puce.aruy.domain.model.Mensaje
import ec.edu.puce.aruy.domain.model.TipoMensaje
import ec.edu.puce.aruy.domain.repository.MensajeRepository

class MensajeRepositoryImpl(private val api: SolicitudApi) : MensajeRepository {

    override suspend fun mensajes(solicitudId: String, desde: String?): List<Mensaje> =
        api.mensajes(solicitudId, desde).map { it.toDomain() }

    override suspend fun enviar(solicitudId: String, texto: String): Mensaje =
        api.enviarMensaje(solicitudId, EnviarMensajeRequest(texto)).toDomain()

    override suspend fun compartirWhatsapp(solicitudId: String): Mensaje =
        api.compartirWhatsapp(solicitudId).toDomain()

    private fun MensajeDto.toDomain() = Mensaje(
        id = id,
        esMio = esMio,
        texto = texto.orEmpty(),
        tipo = TipoMensaje.desde(tipo),
        fecha = fechaCreacion
    )
}
