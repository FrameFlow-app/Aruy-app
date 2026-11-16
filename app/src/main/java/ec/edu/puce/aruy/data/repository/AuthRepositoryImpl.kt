package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.AuthApi
import ec.edu.puce.aruy.data.remote.dto.ActivarPerfilRequest
import ec.edu.puce.aruy.data.remote.dto.AuthRespuestaDto
import ec.edu.puce.aruy.data.remote.dto.LoginRequest
import ec.edu.puce.aruy.data.remote.dto.RegistroRequest
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.DatosPrestador
import ec.edu.puce.aruy.domain.model.DatosRegistro
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.repository.AuthRepository
import ec.edu.puce.aruy.domain.validation.Validaciones

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sesionManager: SesionManager
) : AuthRepository {

    override suspend fun iniciarSesion(correo: String, password: String): Sesion {
        val respuesta = authApi.login(LoginRequest(correo.trim().lowercase(), password))
        return guardar(respuesta)
    }

    override suspend fun registrar(datos: DatosRegistro): Sesion {
        val esPrestador = datos.rol == RolUsuario.PRESTADOR
        val respuesta = authApi.registrar(
            RegistroRequest(
                nombre = datos.nombre.trim(),
                telefono = Validaciones.normalizarCelular(datos.celular),
                email = datos.correo.trim().lowercase(),
                password = datos.password,
                rol = datos.rol.name,
                fechaNacimiento = datos.fechaNacimiento?.toString(),
                oficioPrincipal = if (esPrestador) datos.oficio?.trim() else null,
                zonaCobertura = if (esPrestador) datos.zona?.trim() else null
            )
        )
        return guardar(respuesta)
    }

    override suspend fun activarPerfilPrestador(datos: DatosPrestador): Sesion {
        val respuesta = authApi.activarPerfil(
            ActivarPerfilRequest(
                oficioPrincipal = datos.oficio.trim(),
                zonaCobertura = datos.zona.trim(),
                descripcionProfesional = datos.descripcion?.trim()?.takeIf { it.isNotEmpty() },
                aniosExperiencia = datos.aniosExperiencia
            )
        )
        return guardar(respuesta) // el token nuevo y el rol PRESTADOR reemplazan a los anteriores
    }

    private suspend fun guardar(respuesta: AuthRespuestaDto): Sesion {
        val sesion = Sesion(
            token = respuesta.token,
            usuario = respuesta.usuario.toDomain(),
            perfilPrestadorId = respuesta.perfilPrestadorId
        )
        sesionManager.iniciar(sesion)
        return sesion
    }

    override suspend fun cerrarSesion() {
        sesionManager.cerrar()
    }
}
