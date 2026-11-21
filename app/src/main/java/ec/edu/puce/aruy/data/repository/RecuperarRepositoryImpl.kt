package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.remote.api.AuthApi
import ec.edu.puce.aruy.data.remote.dto.OlvideRequest
import ec.edu.puce.aruy.data.remote.dto.RestablecerRequest
import ec.edu.puce.aruy.domain.repository.RecuperarRepository

class RecuperarRepositoryImpl(private val api: AuthApi) : RecuperarRepository {

    override suspend fun pedirCodigo(correo: String): String =
        api.olvide(OlvideRequest(correo.trim().lowercase())).mensaje.orEmpty()

    override suspend fun restablecer(correo: String, codigo: String, password: String): String =
        api.restablecer(RestablecerRequest(correo.trim().lowercase(), codigo.trim(), password)).mensaje.orEmpty()
}
