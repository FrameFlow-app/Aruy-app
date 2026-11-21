package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.catalogo.EtiquetaFoto
import ec.edu.puce.aruy.domain.catalogo.ResultadoFoto
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.repository.PrestadorRepository

/** Qué pasó al intentar subir una foto al portafolio. */
sealed interface ResultadoSubidaFoto {
    data class Subida(val foto: FotoPortafolio) : ResultadoSubidaFoto

    /** La foto no corresponde al servicio: no se subió nada. [mensaje] se muestra tal cual. */
    data class Rechazada(val mensaje: String) : ResultadoSubidaFoto
}

/**
 * Una intención del prestador: subir una foto a su portafolio.
 * Regla de negocio: una foto que claramente no es del servicio no se sube.
 */
class SubirFotoPortafolioUseCase(
    private val prestador: PrestadorRepository,
    private val validarFoto: ValidarFotoServicioUseCase = ValidarFotoServicioUseCase()
) {
    suspend operator fun invoke(
        servicioId: String,
        webp: ByteArray,
        etiquetas: List<EtiquetaFoto>,
        categoria: CategoriaServicio?,
        titulo: String,
        descripcion: String
    ): ResultadoSubidaFoto {
        val revision = validarFoto(etiquetas, categoria, titulo, descripcion)
        if (revision is ResultadoFoto.Rechazada) return ResultadoSubidaFoto.Rechazada(revision.mensaje)
        val foto = prestador.subirFoto(servicioId, webp, etiquetas.map { it.texto })
        return ResultadoSubidaFoto.Subida(foto)
    }
}
