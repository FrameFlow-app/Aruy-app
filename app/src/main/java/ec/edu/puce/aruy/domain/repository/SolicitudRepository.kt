package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes

/** Contrato de las solicitudes: pedirle un servicio a un prestador y responder. */
interface SolicitudRepository {
    suspend fun enviar(servicioId: String, mensaje: String): Solicitud

    suspend fun listar(tipo: TipoSolicitudes): List<Solicitud>

    suspend fun cambiarEstado(id: String, estado: EstadoSolicitud, respuesta: String? = null): Solicitud

    suspend fun resumen(): ResumenSolicitudes

    /** Marca como vistas las solicitudes de una bandeja (al abrirla). */
    suspend fun marcarVistas(tipo: TipoSolicitudes)
}
