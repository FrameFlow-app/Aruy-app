package ec.edu.puce.aruy.data.solicitudes

import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.usecase.ObtenerResumenSolicitudesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lo que hay de nuevo en las solicitudes, compartido por toda la app (el numerito del Perfil, del Inicio y del Panel).
 * [llegaron] avisa cuando aparece algo que antes no estaba.
 */
class ResumenSolicitudesStore(private val obtener: ObtenerResumenSolicitudesUseCase) {

    private val _resumen = MutableStateFlow(ResumenSolicitudes.VACIO)
    val resumen: StateFlow<ResumenSolicitudes> = _resumen.asStateFlow()

    private val _llegaron = MutableSharedFlow<ResumenSolicitudes>(extraBufferCapacity = 1)
    val llegaron: SharedFlow<ResumenSolicitudes> = _llegaron.asSharedFlow()

    /** Pregunta al servidor. Si no hay internet o falla, se queda con lo que había (el numerito no desaparece). */
    suspend fun refrescar() {
        val nuevo = try {
            obtener()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
        val antes = _resumen.value
        _resumen.value = nuevo
        if (nuevo.recibidasNuevas > antes.recibidasNuevas || nuevo.enviadasNovedades > antes.enviadasNovedades) {
            _llegaron.tryEmit(nuevo)
        }
    }

    /** Al cerrar sesión: la siguiente persona no debe ver los numeritos de la anterior. */
    fun limpiar() {
        _resumen.value = ResumenSolicitudes.VACIO
    }
}
