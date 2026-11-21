package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.domain.repository.FotoPerfilRepository

/**
 * Una intención de la persona: cambiar su foto de perfil.
 * Regla de negocio: la foto no puede venir vacía ni pasar de 5 MB (el servidor la vuelve a comprobar).
 */
class SubirFotoPerfilUseCase(private val fotos: FotoPerfilRepository) {
    suspend operator fun invoke(webp: ByteArray): Usuario {
        require(webp.isNotEmpty()) { "La foto está vacía. Elige otra." }
        require(webp.size <= TAMANO_MAXIMO) { "La foto es demasiado grande. Elige otra." }
        return fotos.subirFoto(webp)
    }

    companion object {
        const val TAMANO_MAXIMO = 5 * 1024 * 1024
    }
}
