package ec.edu.puce.aruy.domain.repository

/** Contrato de "Olvidé mi contraseña". Ambos pasos devuelven el mensaje que dice el servidor. */
interface RecuperarRepository {
    suspend fun pedirCodigo(correo: String): String

    suspend fun restablecer(correo: String, codigo: String, password: String): String
}
