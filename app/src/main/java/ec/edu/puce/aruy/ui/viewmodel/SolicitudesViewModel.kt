package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.MensajesSolicitud
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.data.solicitudes.ResumenSolicitudesStore
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.usecase.CambiarEstadoSolicitudUseCase
import ec.edu.puce.aruy.domain.usecase.MarcarSolicitudesVistasUseCase
import ec.edu.puce.aruy.domain.usecase.ObtenerSolicitudesUseCase
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

/** La bandeja que se está viendo ([tipo]) y lo que contiene. */
data class SolicitudesUi(
    val tipo: TipoSolicitudes = TipoSolicitudes.ENVIADAS,
    /** Solo quien ofrece servicios tiene la bandeja de recibidas. */
    val puedeRecibir: Boolean = false,
    val cargando: Boolean = true,
    val error: String? = null,
    val items: List<Solicitud> = emptyList(),
    /** La solicitud que se está cambiando ahora mismo (para no tocar dos a la vez). */
    val trabajandoId: String? = null
)

/** Solicitudes enviadas y recibidas: verlas, aceptarlas, rechazarlas, completarlas o cancelarlas. */
class SolicitudesViewModel(
    private val obtener: ObtenerSolicitudesUseCase,
    private val cambiar: CambiarEstadoSolicitudUseCase,
    private val marcarVistas: MarcarSolicitudesVistasUseCase,
    private val resumen: ResumenSolicitudesStore,
    sesion: SesionManager
) : ViewModel() {

    private val recibe = (sesion.estado.value as? EstadoSesion.Autenticada)?.sesion?.perfilPrestadorId != null

    private val _ui = MutableStateFlow(
        SolicitudesUi(tipo = if (recibe) TipoSolicitudes.RECIBIDAS else TipoSolicitudes.ENVIADAS, puedeRecibir = recibe)
    )
    val ui: StateFlow<SolicitudesUi> = _ui.asStateFlow()

    private val _mensajes = MutableSharedFlow<Aviso>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<Aviso> = _mensajes.asSharedFlow()

    init {
        cargar()
    }

    fun elegirPestana(tipo: TipoSolicitudes) {
        if (tipo == _ui.value.tipo) return
        _ui.update { it.copy(tipo = tipo, items = emptyList(), cargando = true, error = null) }
        cargar()
    }

    fun cargar() {
        val tipo = _ui.value.tipo
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val lista = obtener(tipo)
                _ui.update { if (it.tipo == tipo) it.copy(cargando = false, items = lista) else it }
                // Abrir la bandeja las da por vistas; los "Nuevo" siguen a la vista en esta pantalla
                try {
                    marcarVistas(tipo)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // si no se pudo marcar, el numerito se corrige la próxima vez
                }
                resumen.refrescar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { if (it.tipo == tipo) it.copy(cargando = false, error = MensajesSolicitud.alListar(e)) else it }
            }
        }
    }

    fun cambiarEstado(solicitud: Solicitud, nuevo: EstadoSolicitud, respuesta: String? = null) {
        if (_ui.value.trabajandoId != null) return
        val tipo = _ui.value.tipo
        _ui.update { it.copy(trabajandoId = solicitud.id) }
        viewModelScope.launch {
            try {
                val actualizada = cambiar(solicitud, tipo, nuevo, respuesta)
                _ui.update { estado ->
                    estado.copy(
                        trabajandoId = null,
                        items = estado.items.map {
                            // El servidor solo devuelve el estado nuevo: el resto (nombre, servicio, mensaje) se conserva
                            if (it.id == solicitud.id) {
                                it.copy(
                                    estado = actualizada.estado,
                                    respuesta = actualizada.respuesta ?: it.respuesta,
                                    fechaActualizacion = actualizada.fechaActualizacion ?: it.fechaActualizacion,
                                    sinVer = false
                                )
                            } else {
                                it
                            }
                        }
                    )
                }
                _mensajes.tryEmit(Aviso.exito(mensajeDeExito(nuevo)))
                resumen.refrescar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(trabajandoId = null) }
                _mensajes.tryEmit(Aviso.error(MensajesSolicitud.alCambiar(e)))
            }
        }
    }

    companion object {
        fun mensajeDeExito(estado: EstadoSolicitud): String = when (estado) {
            EstadoSolicitud.ACEPTADA -> "Aceptaste la solicitud. Ya puedes escribirle por WhatsApp."
            EstadoSolicitud.RECHAZADA -> "Rechazaste la solicitud."
            EstadoSolicitud.COMPLETADA -> "Marcaste el trabajo como completado."
            EstadoSolicitud.CANCELADA -> "Cancelaste la solicitud."
            EstadoSolicitud.PENDIENTE -> "Solicitud actualizada."
        }
    }
}
