package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.repository.SolicitudRepository

/** Una intención de la app: saber cuántas solicitudes nuevas hay (para el numerito y las notificaciones). */
class ObtenerResumenSolicitudesUseCase(private val solicitudes: SolicitudRepository) {
    suspend operator fun invoke(): ResumenSolicitudes = solicitudes.resumen()
}
