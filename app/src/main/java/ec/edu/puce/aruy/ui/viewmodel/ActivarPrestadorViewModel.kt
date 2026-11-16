package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.domain.model.DatosPrestador
import ec.edu.puce.aruy.domain.usecase.ActivarPerfilPrestadorUseCase
import ec.edu.puce.aruy.ui.components.Aviso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Estado de la pantalla "Quiero ofrecer servicios". [listo] pasa a true cuando la cuenta ya ofrece servicios. */
data class EstadoActivarPrestador(
    val cargando: Boolean = false,
    val listo: Boolean = false
)

/** "Quiero ofrecer servicios": una persona con cuenta empieza a ofrecer los suyos, con el mismo correo. */
class ActivarPrestadorViewModel(
    private val activar: ActivarPerfilPrestadorUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(EstadoActivarPrestador())
    val ui: StateFlow<EstadoActivarPrestador> = _ui.asStateFlow()

    private val _mensajes = MutableSharedFlow<Aviso>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<Aviso> = _mensajes.asSharedFlow()

    fun activarPerfil(datos: DatosPrestador) {
        if (_ui.value.cargando) return
        _ui.value = EstadoActivarPrestador(cargando = true)
        viewModelScope.launch {
            try {
                activar(datos)
                _ui.value = EstadoActivarPrestador(listo = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.value = EstadoActivarPrestador()
                _mensajes.tryEmit(Aviso.error(mensajeDeActivar(e)))
            }
        }
    }

    companion object {
        fun mensajeDeActivar(error: Exception): String {
            if (error is IllegalArgumentException) return error.message ?: "Revisa los datos."
            if (error is HttpException) {
                when (error.code()) {
                    409 -> return "Ya ofreces servicios con esta cuenta."
                    400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa los datos: hay algún campo incorrecto."
                }
            }
            return ApiErrorHandler.obtenerMensaje(error)
        }
    }
}
