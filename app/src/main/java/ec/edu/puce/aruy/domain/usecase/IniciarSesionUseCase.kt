package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.repository.AuthRepository

/** Una intención de la persona: entrar con su correo y su contraseña. */
class IniciarSesionUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(correo: String, password: String): Sesion = auth.iniciarSesion(correo, password)
}
