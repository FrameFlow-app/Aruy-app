package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.model.accionesPara
import ec.edu.puce.aruy.domain.repository.SolicitudRepository
import ec.edu.puce.aruy.domain.validation.ValidacionesSolicitud

/**
 * Una intención de la persona: aceptar, rechazar, completar o cancelar una solicitud.
 * Regla de negocio: solo los cambios que le tocan según su bandeja (el prestador responde; quien envió, cancela).
 */
class CambiarEstadoSolicitudUseCase(private val solicitudes: SolicitudRepository) {
    suspend operator fun invoke(
        solicitud: Solicitud,
        tipo: TipoSolicitudes,
        nuevo: EstadoSolicitud,
        respuesta: String? = null
    ): Solicitud {
        require(nuevo in solicitud.accionesPara(tipo)) {
            "Esa acción no es posible con una solicitud ${solicitud.estado.etiqueta.lowercase()}."
        }
        respuesta?.let { ValidacionesSolicitud.respuesta(it)?.let { error -> throw IllegalArgumentException(error) } }
        return solicitudes.cambiarEstado(solicitud.id, nuevo, respuesta?.trim()?.takeIf { it.isNotEmpty() })
    }
}
