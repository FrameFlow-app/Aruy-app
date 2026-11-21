package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.DatosPrestador
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.repository.AuthRepository

/**
 * Una intención de la persona: empezar a ofrecer sus servicios con la cuenta que ya tiene.
 * Regla de negocio: hay que decir qué oficio se ofrece y en qué zona.
 */
class ActivarPerfilPrestadorUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(datos: DatosPrestador): Sesion {
        require(datos.oficio.isNotBlank()) { "Elige qué servicio ofreces." }
        require(datos.zona.isNotBlank()) { "Elige tu zona." }
        return auth.activarPerfilPrestador(datos)
    }
}
