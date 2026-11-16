package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.DatosServicio
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.domain.repository.PrestadorRepository
import ec.edu.puce.aruy.domain.validation.ValidacionesServicio

/**
 * Una intención del prestador: publicar un servicio nuevo, o guardar los cambios de uno existente.
 * Reglas de negocio: el título queda con inicial en mayúscula y el servicio necesita al menos una foto.
 */
class PublicarServicioUseCase(private val prestador: PrestadorRepository) {

    /** [servicioId] es null para un servicio nuevo. [totalFotos] cuenta las guardadas y las que se van a subir. */
    suspend operator fun invoke(servicioId: String?, datos: DatosServicio, totalFotos: Int): Servicio {
        ValidacionesServicio.fotos(totalFotos)?.let { throw IllegalArgumentException(it) }
        val limpios = datos.copy(titulo = ValidacionesServicio.normalizarTitulo(datos.titulo))
        return if (servicioId == null) prestador.crear(limpios) else prestador.editar(servicioId, limpios)
    }
}
