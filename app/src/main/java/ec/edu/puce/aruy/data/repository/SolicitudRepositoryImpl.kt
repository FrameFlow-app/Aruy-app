package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.remote.api.SolicitudApi
import ec.edu.puce.aruy.data.remote.dto.CambiarEstadoSolicitudRequest
import ec.edu.puce.aruy.data.remote.dto.CrearSolicitudRequest
import ec.edu.puce.aruy.data.remote.dto.MarcarVistasRequest
import ec.edu.puce.aruy.data.remote.dto.SolicitudDto
import ec.edu.puce.aruy.domain.model.ContraparteSolicitud
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.repository.SolicitudRepository

class SolicitudRepositoryImpl(private val api: SolicitudApi) : SolicitudRepository {

    override suspend fun enviar(servicioId: String, mensaje: String): Solicitud =
        api.crear(CrearSolicitudRequest(servicioId, mensaje)).toDomain()

    override suspend fun listar(tipo: TipoSolicitudes): List<Solicitud> =
        api.listar(tipo.valor).map { it.toDomain() }

    override suspend fun cambiarEstado(id: String, estado: EstadoSolicitud, respuesta: String?): Solicitud =
        api.cambiarEstado(id, CambiarEstadoSolicitudRequest(estado.name, respuesta)).toDomain()

    override suspend fun resumen(): ResumenSolicitudes =
        api.resumen().let { ResumenSolicitudes(it.recibidasNuevas, it.recibidasPendientes, it.enviadasNovedades) }

    override suspend fun marcarVistas(tipo: TipoSolicitudes) {
        api.marcarVistas(MarcarVistasRequest(tipo.valor))
    }

    private fun SolicitudDto.toDomain() = Solicitud(
        id = id,
        servicioId = servicioId,
        servicioTitulo = servicioTitulo,
        estado = EstadoSolicitud.desde(estado),
        mensaje = mensaje.orEmpty(),
        respuesta = respuesta?.takeIf { it.isNotBlank() },
        fechaCreacion = fechaCreacion,
        fechaActualizacion = fechaActualizacion,
        sinVer = sinVer,
        contraparte = ContraparteSolicitud(
            nombre = contraparte?.nombre ?: "Cuenta eliminada",
            fotoPerfil = contraparte?.fotoPerfil,
            telefono = contraparte?.telefono
        )
    )
}
