package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import ec.edu.puce.aruy.domain.repository.CatalogoRepository

/** Una intención de la persona: ver los servicios disponibles, con los datos de quien los ofrece. */
class ObtenerCatalogoUseCase(private val catalogo: CatalogoRepository) {
    suspend operator fun invoke(): List<ServicioConPrestador> = catalogo.cargarCatalogo()
}
