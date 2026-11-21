package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.domain.repository.FotoPerfilRepository

/** Una intención de la persona: quitar su foto de perfil y volver a las iniciales. */
class QuitarFotoPerfilUseCase(private val fotos: FotoPerfilRepository) {
    suspend operator fun invoke(): Usuario = fotos.quitarFoto()
}
