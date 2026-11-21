package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.repository.SolicitudRepository
import ec.edu.puce.aruy.domain.validation.ValidacionesSolicitud

/** Una intención de la persona: pedirle un servicio a un prestador. El mensaje debe contar qué necesita. */
class EnviarSolicitudUseCase(private val solicitudes: SolicitudRepository) {
    suspend operator fun invoke(servicioId: String, mensaje: String): Solicitud {
        ValidacionesSolicitud.mensaje(mensaje)?.let { throw IllegalArgumentException(it) }
        return solicitudes.enviar(servicioId, mensaje.trim())
    }
}
