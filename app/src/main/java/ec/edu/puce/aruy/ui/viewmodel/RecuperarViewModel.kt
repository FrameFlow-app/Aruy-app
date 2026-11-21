package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.MensajesSolicitud
import ec.edu.puce.aruy.domain.usecase.PedirCodigoUseCase
import ec.edu.puce.aruy.domain.usecase.RestablecerContrasenaUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PasoRecuperar { CORREO, CODIGO }

data class RecuperarUi(
    val paso: PasoRecuperar = PasoRecuperar.CORREO,
    val correo: String = "",
    val cargando: Boolean = false,
    /** Lo que dice el servidor (o la validación) cuando algo falla. */
    val error: String? = null,
    /** Mensaje del servidor al pedir el código ("Si el correo existe, te enviamos un código"). */
    val info: String? = null,
    /** Segundos que faltan para poder pedir otro código. */
    val esperaReenvio: Int = 0
)

/** "Olvidé mi contraseña": primero el correo, luego el código de 6 dígitos y la contraseña nueva. */
class RecuperarViewModel(
    private val casoPedir: PedirCodigoUseCase,
    private val casoRestablecer: RestablecerContrasenaUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(RecuperarUi())
    val ui: StateFlow<RecuperarUi> = _ui.asStateFlow()

    private var cuentaAtras: Job? = null

    /** Paso 1, y también "Reenviar código" (solo si ya pasó la espera). */
    fun pedirCodigo(correo: String) {
        val actual = _ui.value
        if (actual.cargando || actual.esperaReenvio > 0) return
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val mensaje = casoPedir(correo)
                _ui.update {
                    it.copy(paso = PasoRecuperar.CODIGO, correo = correo.trim(), cargando = false, info = mensaje.ifBlank { null })
                }
                iniciarEspera()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = MensajesSolicitud.alRecuperar(e)) }
            }
        }
    }

    fun restablecer(codigo: String, password: String, confirmar: String, alExito: () -> Unit) {
        if (_ui.value.cargando) return
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                casoRestablecer(_ui.value.correo, codigo, password, confirmar)
                _ui.update { it.copy(cargando = false) }
                alExito()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = MensajesSolicitud.alRecuperar(e)) }
            }
        }
    }

    /** Volver al paso 1 para corregir el correo. */
    fun cambiarCorreo() {
        cuentaAtras?.cancel()
        _ui.update { RecuperarUi(correo = it.correo) }
    }

    private fun iniciarEspera() {
        cuentaAtras?.cancel()
        _ui.update { it.copy(esperaReenvio = ESPERA_REENVIO_SEGUNDOS) }
        cuentaAtras = viewModelScope.launch {
            while (_ui.value.esperaReenvio > 0) {
                delay(1_000)
                _ui.update { it.copy(esperaReenvio = (it.esperaReenvio - 1).coerceAtLeast(0)) }
            }
        }
    }

    companion object {
        const val ESPERA_REENVIO_SEGUNDOS = 60
    }
}
