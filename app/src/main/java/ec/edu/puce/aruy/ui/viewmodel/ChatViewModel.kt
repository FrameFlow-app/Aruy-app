package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.MensajesSolicitud
import ec.edu.puce.aruy.domain.model.Mensaje
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoMensaje
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.usecase.CompartirWhatsappUseCase
import ec.edu.puce.aruy.domain.usecase.EnviarMensajeUseCase
import ec.edu.puce.aruy.domain.usecase.ObtenerMensajesUseCase
import ec.edu.puce.aruy.domain.usecase.ObtenerSolicitudesUseCase
import ec.edu.puce.aruy.domain.usecase.combinarMensajes
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

data class ChatUi(
    val cargando: Boolean = true,
    /** Error que impide mostrar el chat (no se pudo abrir). */
    val error: String? = null,
    val solicitud: Solicitud? = null,
    /** true si la solicitud es de la bandeja "recibidas": yo soy quien ofrece el servicio. */
    val soyPrestador: Boolean = false,
    val mensajes: List<Mensaje> = emptyList(),
    val enviando: Boolean = false,
    val compartiendo: Boolean = false,
    /** El servidor dijo que la solicitud ya se cerró (409) aunque la lista aún no lo sabía. */
    val cerradaPorServidor: Boolean = false
) {
    val chatAbierto: Boolean get() = !cerradaPorServidor && (solicitud?.chatAbierto ?: true)

    val whatsappCompartido: Boolean
        get() = solicitud?.whatsappCompartido == true || mensajes.any { it.tipo == TipoMensaje.WHATSAPP }

    /** Fecha del último mensaje que ya se tiene: el refresco pide solo lo posterior. */
    val ultimaFecha: String? get() = mensajes.lastOrNull { it.fecha != null }?.fecha
}

/** El chat de una solicitud: leer, escribir, refrescar y (si ofreces el servicio) compartir tu WhatsApp. */
class ChatViewModel(
    private val solicitudId: String,
    private val obtenerSolicitudes: ObtenerSolicitudesUseCase,
    private val obtenerMensajes: ObtenerMensajesUseCase,
    private val enviarMensaje: EnviarMensajeUseCase,
    private val compartirWhatsapp: CompartirWhatsappUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(ChatUi())
    val ui: StateFlow<ChatUi> = _ui.asStateFlow()

    private val _avisos = MutableSharedFlow<Aviso>(extraBufferCapacity = 4)
    val avisos: SharedFlow<Aviso> = _avisos.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val (solicitud, tipo) = buscarSolicitud()
                    ?: throw IllegalStateException("No encontramos esta solicitud. Puede que ya no exista.")
                val mensajes = obtenerMensajes(solicitudId)
                _ui.update {
                    it.copy(
                        cargando = false,
                        solicitud = solicitud,
                        soyPrestador = tipo == TipoSolicitudes.RECIBIDAS,
                        mensajes = mensajes
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                _ui.update { it.copy(cargando = false, error = e.message) }
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = MensajesSolicitud.alAbrirChat(e)) }
            }
        }
    }

    /** Busca la solicitud en las dos bandejas (el servidor no tiene "una sola"). Una que falle no impide la otra. */
    private suspend fun buscarSolicitud(): Pair<Solicitud, TipoSolicitudes>? {
        var fallo: Exception? = null
        for (tipo in listOf(TipoSolicitudes.ENVIADAS, TipoSolicitudes.RECIBIDAS)) {
            try {
                obtenerSolicitudes(tipo).firstOrNull { it.id == solicitudId }?.let { return it to tipo }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fallo = e
            }
        }
        // Si las dos fallaron por la red, se muestra ese error y no un falso "no existe"
        fallo?.let { if (_ui.value.solicitud == null) throw it }
        return null
    }

    /** Pide solo los mensajes posteriores al último que se tiene. Falla en silencio: el siguiente intento lo corrige. */
    fun refrescar() {
        val actual = _ui.value
        if (actual.cargando || actual.error != null || actual.solicitud == null) return
        viewModelScope.launch {
            try {
                val nuevos = obtenerMensajes(solicitudId, actual.ultimaFecha)
                if (nuevos.isNotEmpty()) _ui.update { it.copy(mensajes = combinarMensajes(it.mensajes, nuevos)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // sin internet un momento: no se molesta a la persona
            }
        }
    }

    /** [alExito] se llama cuando el mensaje ya salió (para vaciar el campo). */
    fun enviar(texto: String, alExito: () -> Unit) {
        if (_ui.value.enviando) return
        _ui.update { it.copy(enviando = true) }
        viewModelScope.launch {
            try {
                val mensaje = enviarMensaje(solicitudId, texto)
                _ui.update { it.copy(enviando = false, mensajes = combinarMensajes(it.mensajes, listOf(mensaje))) }
                alExito()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cerrada = (e as? retrofit2.HttpException)?.code() == 409
                _ui.update { it.copy(enviando = false, cerradaPorServidor = it.cerradaPorServidor || cerrada) }
                _avisos.tryEmit(Aviso.error(MensajesSolicitud.alEscribir(e)))
            }
        }
    }

    fun compartirMiWhatsapp() {
        if (_ui.value.compartiendo) return
        _ui.update { it.copy(compartiendo = true) }
        viewModelScope.launch {
            try {
                val mensaje = compartirWhatsapp(solicitudId)
                _ui.update {
                    it.copy(
                        compartiendo = false,
                        mensajes = combinarMensajes(it.mensajes, listOf(mensaje)),
                        solicitud = it.solicitud?.copy(whatsappCompartido = true)
                    )
                }
                _avisos.tryEmit(Aviso.exito("Compartiste tu WhatsApp. Ahora te pueden escribir por ahí."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(compartiendo = false) }
                _avisos.tryEmit(Aviso.error(MensajesSolicitud.alCompartirWhatsapp(e)))
            }
        }
    }
}
