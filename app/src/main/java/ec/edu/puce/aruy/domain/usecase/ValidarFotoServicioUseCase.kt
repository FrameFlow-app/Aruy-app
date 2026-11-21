package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.catalogo.EtiquetaFoto
import ec.edu.puce.aruy.domain.catalogo.ResultadoFoto
import ec.edu.puce.aruy.domain.catalogo.ValidadorFotoOficio
import ec.edu.puce.aruy.domain.model.CategoriaServicio

/** Una intención del prestador: comprobar que una foto corresponde a su servicio, antes de agregarla. */
class ValidarFotoServicioUseCase {
    operator fun invoke(
        etiquetas: List<EtiquetaFoto>,
        categoria: CategoriaServicio?,
        titulo: String,
        descripcion: String
    ): ResultadoFoto = ValidadorFotoOficio.evaluar(etiquetas, categoria, titulo, descripcion)
}
