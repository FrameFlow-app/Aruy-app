package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.repository.SolicitudRepository

/** Una intención de la persona: ver las solicitudes que envió o las que recibió como prestador. */
class ObtenerSolicitudesUseCase(private val solicitudes: SolicitudRepository) {
    suspend operator fun invoke(tipo: TipoSolicitudes): List<Solicitud> = solicitudes.listar(tipo)
}
