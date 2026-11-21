package ec.edu.puce.aruy.ui.viewmodel

import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import ec.edu.puce.aruy.util.ErrorFoto
import ec.edu.puce.aruy.util.ConversorWebp
import ec.edu.puce.aruy.ui.components.Aviso
import ec.edu.puce.aruy.domain.usecase.SubirFotoPerfilUseCase
import ec.edu.puce.aruy.data.remote.util.MensajesPerfil
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.domain.repository.AuthRepository
import ec.edu.puce.aruy.domain.usecase.EdadInvalidaException
import ec.edu.puce.aruy.domain.usecase.IniciarSesionUseCase
import ec.edu.puce.aruy.domain.usecase.RegistrarCuentaUseCase
import ec.edu.puce.aruy.domain.model.DatosRegistro
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.Sesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Estado de un formulario que habla con el servidor (login o registro). */
data class EstadoFormularioAuth(
    val cargando: Boolean = false,
    /** Mensaje general, se anuncia con `role="alert"` en el prototipo. */
    val error: String? = null,
    /** Error que pertenece al campo del correo (por ejemplo, "Ya existe una cuenta..."). */
    val errorCorreo: String? = null,
    /** Error que pertenece al campo del celular (por ejemplo, "Ese celular ya está registrado..."). */
    val errorCelular: String? = null,
    /** Error de la fecha de nacimiento (por ejemplo, "Aruy es solo para mayores de 18 años."). */
    val errorFechaNacimiento: String? = null
)

/** La foto que la persona eligió al crear su cuenta; se sube justo después de registrarla. */
class FotoElegida(val uri: Uri, val desdeCamara: Boolean)

class AuthViewModel(
    private val repositorio: AuthRepository,
    private val iniciarSesionCaso: IniciarSesionUseCase,
    private val registrarCuentaCaso: RegistrarCuentaUseCase,
    private val conversor: ConversorWebp,
    private val subirFotoPerfil: SubirFotoPerfilUseCase,
    private val sesionManager: SesionManager
) : ViewModel() {

    private val _mensajes = MutableSharedFlow<Aviso>(extraBufferCapacity = 4)

    /** Avisos que pasan después de salir del formulario (por ejemplo, si la foto de perfil no se pudo subir). */
    val mensajes: SharedFlow<Aviso> = _mensajes.asSharedFlow()

    val estadoSesion = sesionManager.estado
    val aviso: StateFlow<String?> = sesionManager.aviso

    private val _formulario = MutableStateFlow(EstadoFormularioAuth())
    val formulario: StateFlow<EstadoFormularioAuth> = _formulario.asStateFlow()

    fun iniciarSesion(correo: String, password: String, alExito: (Sesion) -> Unit) {
        ejecutar(::mensajeDeLogin) {
            val sesion = iniciarSesionCaso(correo, password)
            alExito(sesion)
        }
    }

    fun registrar(datos: DatosRegistro, foto: FotoElegida? = null, alExito: (Sesion) -> Unit) {
        ejecutar(::mensajeDeRegistro) {
            val sesion = registrarCuentaCaso(datos)
            alExito(sesion)
            // La cuenta ya existe: si la foto falla no se deshace nada, solo se avisa
            if (foto != null) viewModelScope.launch { subirFotoDeRegistro(foto) }
        }
    }

    private suspend fun subirFotoDeRegistro(foto: FotoElegida) {
        try {
            val imagen = conversor.convertir(foto.uri)
            val usuario = subirFotoPerfil(imagen.webp)
            sesionManager.actualizarUsuario(usuario)
            _mensajes.tryEmit(Aviso.exito("¡Cuenta creada! Tu foto de perfil ya está lista."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val motivo = if (e is ErrorFoto) e.message.orEmpty() else MensajesPerfil.alSubirFoto(e)
            _mensajes.tryEmit(
                Aviso.error("Tu cuenta se creó, pero la foto no se pudo subir. $motivo Puedes agregarla desde Editar perfil.")
            )
        } finally {
            if (foto.desdeCamara) conversor.borrarTemporal(foto.uri)
        }
    }

    fun limpiarError() {
        _formulario.value = EstadoFormularioAuth()
    }

    fun avisoMostrado() = sesionManager.avisoMostrado()

    fun entrarComoInvitado() = sesionManager.entrarComoInvitado()

    fun cerrarSesion() {
        viewModelScope.launch { repositorio.cerrarSesion() }
    }

    private fun ejecutar(
        mensaje: (Exception) -> EstadoFormularioAuth,
        bloque: suspend () -> Unit
    ) {
        if (_formulario.value.cargando) return
        _formulario.value = EstadoFormularioAuth(cargando = true)
        viewModelScope.launch {
            try {
                bloque()
                _formulario.value = EstadoFormularioAuth()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _formulario.value = mensaje(e)
            }
        }
    }

    companion object {

        fun mensajeDeLogin(error: Exception): EstadoFormularioAuth {
            if (error is HttpException) {
                when (error.code()) {
                    400 -> return EstadoFormularioAuth(error = "Escribe tu correo y tu contraseña.")
                    401 -> return EstadoFormularioAuth(error = "El correo o la contraseña no son correctos.")
                }
            }
            return EstadoFormularioAuth(error = ApiErrorHandler.obtenerMensaje(error))
        }

        fun mensajeDeRegistro(error: Exception): EstadoFormularioAuth {
            if (error is EdadInvalidaException) {
                return EstadoFormularioAuth(errorFechaNacimiento = error.message)
            }
            if (error is HttpException) {
                when (error.code()) {
                    409 -> return mensajeDeDuplicado(ApiErrorHandler.detalleDelServidor(error))
                    400 -> return mensajeDeDatosInvalidos(ApiErrorHandler.detalleDelServidor(error))
                }
            }
            return EstadoFormularioAuth(error = ApiErrorHandler.obtenerMensaje(error))
        }

        /** El servidor dice cuál dato está repetido: el celular o el correo. */
        private fun mensajeDeDuplicado(detalle: ApiErrorHandler.DetalleError): EstadoFormularioAuth =
            when (detalle.campo) {
                "telefono" -> EstadoFormularioAuth(
                    errorCelular = "Ese celular ya está registrado en otra cuenta. Usa otro número o inicia sesión."
                )
                "email" -> EstadoFormularioAuth(errorCorreo = "Ya existe una cuenta con este correo. Prueba iniciar sesión.")
                else -> EstadoFormularioAuth(
                    errorCorreo = "Ya existe una cuenta con este correo o celular. Prueba iniciar sesión."
                )
            }

        private fun mensajeDeDatosInvalidos(detalle: ApiErrorHandler.DetalleError): EstadoFormularioAuth {
            if (detalle.campo == "fecha_nacimiento") {
                val esMenor = detalle.mensaje.orEmpty().contains("18")
                return EstadoFormularioAuth(
                    errorFechaNacimiento = if (esMenor) "Aruy es solo para mayores de 18 años." else "Revisa tu fecha de nacimiento."
                )
            }
            return EstadoFormularioAuth(error = detalle.mensaje ?: "Revisa los datos: hay algún campo incorrecto.")
        }

        fun fabrica(
            repositorio: AuthRepository,
            iniciarSesion: IniciarSesionUseCase,
            registrarCuenta: RegistrarCuentaUseCase,
            conversor: ConversorWebp,
            subirFotoPerfil: SubirFotoPerfilUseCase,
            sesionManager: SesionManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(repositorio, iniciarSesion, registrarCuenta, conversor, subirFotoPerfil, sesionManager) as T
            }
    }
}
