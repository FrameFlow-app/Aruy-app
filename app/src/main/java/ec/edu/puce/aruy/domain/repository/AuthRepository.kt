package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.DatosPrestador
import ec.edu.puce.aruy.domain.model.DatosRegistro
import ec.edu.puce.aruy.domain.model.Sesion

/** Contrato de la cuenta: entrar, registrarse y salir. No sabe si los datos vienen de internet o de otro lado. */
interface AuthRepository {
    suspend fun iniciarSesion(correo: String, password: String): Sesion

    suspend fun registrar(datos: DatosRegistro): Sesion

    /**
     * "Quiero ofrecer servicios": quien ya tiene cuenta empieza a ofrecer los suyos con la MISMA cuenta (sin otro correo).
     * Devuelve la sesión nueva, porque el rol viaja dentro del token.
     */
    suspend fun activarPerfilPrestador(datos: DatosPrestador): Sesion

    suspend fun cerrarSesion()
}
