package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.DatosRegistro
import ec.edu.puce.aruy.domain.model.Sesion

/** Contrato de la cuenta: entrar, registrarse y salir. No sabe si los datos vienen de internet o de otro lado. */
interface AuthRepository {
    suspend fun iniciarSesion(correo: String, password: String): Sesion

    suspend fun registrar(datos: DatosRegistro): Sesion

    suspend fun cerrarSesion()
}
