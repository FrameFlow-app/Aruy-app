package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.repository.RecuperarRepository
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.domain.validation.ValidacionesRecuperar

/** Paso 1: pedir el código por correo. */
class PedirCodigoUseCase(private val recuperar: RecuperarRepository) {
    suspend operator fun invoke(correo: String): String {
        Validaciones.correo(correo)?.let { throw IllegalArgumentException(it) }
        return recuperar.pedirCodigo(correo)
    }
}

/** Paso 2: el código y la contraseña nueva, con las mismas reglas que el registro. */
class RestablecerContrasenaUseCase(private val recuperar: RecuperarRepository) {
    suspend operator fun invoke(correo: String, codigo: String, password: String, confirmar: String): String {
        ValidacionesRecuperar.codigo(codigo)?.let { throw IllegalArgumentException(it) }
        Validaciones.password(password)?.let { throw IllegalArgumentException(it) }
        ValidacionesRecuperar.confirmacion(password, confirmar)?.let { throw IllegalArgumentException(it) }
        return recuperar.restablecer(correo, codigo, password)
    }
}
