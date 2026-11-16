package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.repository.SolicitudRepository

/** Una intención de la app: dar por vistas las solicitudes de una bandeja cuando la persona la abre. */
class MarcarSolicitudesVistasUseCase(private val solicitudes: SolicitudRepository) {
    suspend operator fun invoke(tipo: TipoSolicitudes) = solicitudes.marcarVistas(tipo)
}
