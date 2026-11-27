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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** "Quiero ofrecer servicios": una persona con cuenta empieza a ofrecer los suyos, con el mismo correo. */
class ActivarPrestadorViewModel(
    private val activar: ActivarPerfilPrestadorUseCase
) : ViewModel() {

    // Definición de la data class del estado (inmutable: todos son val)
    data class ScreenState(
        val titleText: String = "Ofrecer mis servicios",
        val isProcessing: Boolean = false,
        val isReady: Boolean = false
    )
    //ViewModel puede cambiar el estado.
    private val _uiState = MutableStateFlow(ScreenState())

    // solo LECTURA
    // cumple el flujo unidireccional
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _mensajes = MutableSharedFlow<Aviso>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<Aviso> = _mensajes.asSharedFlow()

    fun activarPerfil(datos: DatosPrestador) {
        if (_uiState.value.isProcessing) return
        // copy() crea un estado nuevo; el anterior nunca se modifica (inmutabilidad)
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                activar(datos)
                _uiState.update { it.copy(isProcessing = false, isReady = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, isReady = false) }
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
