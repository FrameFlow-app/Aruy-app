package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.DatosRegistro
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.repository.AuthRepository
import ec.edu.puce.aruy.domain.validation.Validaciones

/** La fecha de nacimiento falta o la persona es menor de edad: [message] se muestra tal cual. */
class EdadInvalidaException(mensaje: String) : IllegalArgumentException(mensaje)

/**
 * Una intención de la persona: crear su cuenta.
 * Regla de negocio: Aruy es solo para mayores de 18 años.
 */
class RegistrarCuentaUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(datos: DatosRegistro): Sesion {
        val nacimiento = datos.fechaNacimiento ?: throw EdadInvalidaException("Escribe tu fecha de nacimiento.")
        if (Validaciones.edadEn(nacimiento) < Validaciones.EDAD_MINIMA) {
            throw EdadInvalidaException("Aruy es solo para mayores de ${Validaciones.EDAD_MINIMA} años.")
        }
        return auth.registrar(datos)
    }
}
