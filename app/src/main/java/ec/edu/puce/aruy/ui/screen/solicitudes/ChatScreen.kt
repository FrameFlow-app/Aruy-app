package ec.edu.puce.aruy.ui.screen.solicitudes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import ec.edu.puce.aruy.domain.catalogo.Contacto
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.Mensaje
import ec.edu.puce.aruy.domain.model.TipoMensaje
import ec.edu.puce.aruy.domain.solicitudes.HoraChat
import ec.edu.puce.aruy.domain.validation.ValidacionesMensaje
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.Aviso
import ec.edu.puce.aruy.ui.components.AvisoHost
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonTonal
import ec.edu.puce.aruy.ui.components.BotonWhatsApp
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.ui.viewmodel.ChatViewModel
import ec.edu.puce.aruy.util.abrirWhatsApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Cada cuánto se piden mensajes nuevos mientras el chat está a la vista. */
private const val INTERVALO_CHAT_MS = 8_000L

/**
 * El chat de una solicitud. Las burbujas mías van a la derecha en el color principal; las de la otra persona,
 * a la izquierda en una superficie suave. Si ofreces el servicio puedes compartir tu WhatsApp con un toque.
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel, onVolver: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val lista = rememberLazyListState()
    val alcance = rememberCoroutineScope()
    var texto by rememberSaveable { mutableStateOf("") }
    var confirmarWhatsapp by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.avisos.collect { snackbar.avisar(it) } }
    // Solo mientras la pantalla está a la vista: en segundo plano no se gasta batería ni datos
    LaunchedEffect(viewModel) {
        ciclo.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refrescar()
                delay(INTERVALO_CHAT_MS)
            }
        }
    }
    LaunchedEffect(ui.mensajes.size) {
        if (ui.mensajes.isNotEmpty()) lista.scrollToItem(ui.mensajes.lastIndex)
    }

    val solicitud = ui.solicitud
    val nombre = solicitud?.contraparte?.nombre ?: "Chat"

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            BarraSuperior(nombre, onVolver)

            when {
                ui.cargando && solicitud == null -> EstadoCargando("Abriendo la conversación…", Modifier.weight(1f))
                solicitud == null -> EstadoVacio(
                    titulo = "No pudimos abrir el chat",
                    texto = ui.error ?: "Inténtalo de nuevo.",
                    icono = Icons.Filled.ErrorOutline,
                    modifier = Modifier.weight(1f),
                    textoBoton = "Reintentar",
                    onBoton = viewModel::cargar
                )
                else -> {
                    EncabezadoChat(
                        servicio = solicitud.servicioTitulo ?: "Servicio eliminado",
                        estado = solicitud.estado,
                        mostrarCompartir = ui.soyPrestador && !ui.whatsappCompartido && ui.chatAbierto,
                        compartiendo = ui.compartiendo,
                        onCompartir = { confirmarWhatsapp = true }
                    )
                    if (ui.mensajes.isEmpty()) {
                        EstadoVacio(
                            titulo = "Aún no hay mensajes",
                            texto = if (ui.chatAbierto) {
                                "Escribe el primero: cuéntale a $nombre los detalles o resuelve tus dudas."
                            } else {
                                "Esta solicitud está cerrada, así que no hay mensajes."
                            },
                            icono = Icons.Filled.ChatBubbleOutline,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            state = lista,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = Espacios.l, vertical = Espacios.m),
                            verticalArrangement = Arrangement.spacedBy(Espacios.s)
                        ) {
                            items(ui.mensajes, key = { it.id }) { mensaje ->
                                if (mensaje.tipo == TipoMensaje.WHATSAPP) {
                                    TarjetaWhatsapp(
                                        mensaje = mensaje,
                                        nombre = nombre,
                                        onAbrir = {
                                            val numero = Contacto.numeroWhatsApp(mensaje.texto)
                                            val saludo = "Hola $nombre, te escribo por «${solicitud.servicioTitulo ?: "tu servicio"}» en Aruy."
                                            when {
                                                numero == null -> alcance.launch { snackbar.avisar(Aviso.error("No tenemos un número de WhatsApp válido para esta persona.")) }
                                                !abrirWhatsApp(contexto, numero, saludo) -> alcance.launch { snackbar.avisar(Aviso.error("No pudimos abrir WhatsApp en este teléfono.")) }
                                            }
                                        }
                                    )
                                } else {
                                    Burbuja(mensaje)
                                }
                            }
                        }
                    }
                    BarraEscribir(
                        texto = texto,
                        onTexto = { texto = it },
                        abierto = ui.chatAbierto,
                        enviando = ui.enviando,
                        onEnviar = { viewModel.enviar(texto) { texto = "" } }
                    )
                }
            }
        }
        AvisoHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmarWhatsapp) {
        DialogoConfirmacion(
            titulo = "¿Compartir tu WhatsApp?",
            texto = "La otra persona verá tu número y podrá escribirte por WhatsApp.",
            textoConfirmar = "Compartir",
            textoCancelar = "Ahora no",
            onConfirmar = {
                confirmarWhatsapp = false
                viewModel.compartirMiWhatsapp()
            },
            onCancelar = { confirmarWhatsapp = false }
        )
    }
}

@Composable
private fun EncabezadoChat(
    servicio: String,
    estado: EstadoSolicitud,
    mostrarCompartir: Boolean,
    compartiendo: Boolean,
    onCompartir: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Espacios.l, vertical = Espacios.s),
        verticalArrangement = Arrangement.spacedBy(Espacios.s)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacios.m)) {
            Text(
                servicio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Pastilla(estado.etiqueta, tipo = tipoDeEstado(estado))
        }
        if (mostrarCompartir) {
            BotonTonal(
                texto = "Compartir mi WhatsApp",
                onClick = onCompartir,
                icono = Icons.AutoMirrored.Filled.Chat,
                ancho = true,
                habilitado = !compartiendo
            )
        }
    }
}

private fun tipoDeEstado(estado: EstadoSolicitud): TipoPastilla = when (estado) {
    EstadoSolicitud.PENDIENTE -> TipoPastilla.ADVERTENCIA
    EstadoSolicitud.ACEPTADA -> TipoPastilla.OK
    EstadoSolicitud.RECHAZADA -> TipoPastilla.ERROR
    EstadoSolicitud.CANCELADA -> TipoPastilla.NEUTRA
    EstadoSolicitud.COMPLETADA -> TipoPastilla.INFO
}

@Composable
private fun Burbuja(mensaje: Mensaje) {
    val esquema = MaterialTheme.colorScheme
    val mio = mensaje.esMio
    val forma = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (mio) 20.dp else 4.dp,
        bottomEnd = if (mio) 4.dp else 20.dp
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mio) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = forma,
            color = if (mio) esquema.primary else esquema.surfaceContainer,
            contentColor = if (mio) esquema.onPrimary else esquema.onSurface,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = Espacios.m, vertical = Espacios.s)) {
                Text(mensaje.texto, style = MaterialTheme.typography.bodyLarge)
                val hora = HoraChat.corta(mensaje.fecha)
                if (hora.isNotEmpty()) {
                    Text(
                        hora,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = Espacios.xs)
                    )
                }
            }
        }
    }
}

/** El aviso de que el prestador compartió su número; quien lo recibe puede abrir WhatsApp desde aquí. */
@Composable
private fun TarjetaWhatsapp(mensaje: Mensaje, nombre: String, onAbrir: () -> Unit) {
    AruyTarjeta(tipo = TipoTarjeta.OK, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacios.m)) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(Espacios.xxl))
            Column(Modifier.weight(1f)) {
                Text(
                    if (mensaje.esMio) "Compartiste tu WhatsApp" else "$nombre te compartió su WhatsApp",
                    style = MaterialTheme.typography.titleMedium
                )
                val hora = HoraChat.corta(mensaje.fecha)
                if (hora.isNotEmpty()) Text(hora, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (!mensaje.esMio) {
            BotonWhatsApp("Abrir WhatsApp", onAbrir, modifier = Modifier.padding(top = Espacios.m), icono = Icons.AutoMirrored.Filled.Chat)
        }
    }
}

@Composable
private fun BarraEscribir(
    texto: String,
    onTexto: (String) -> Unit,
    abierto: Boolean,
    enviando: Boolean,
    onEnviar: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = Espacios.l, vertical = Espacios.s),
            horizontalArrangement = Arrangement.spacedBy(Espacios.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AruyCampo(
                valor = texto,
                onValor = onTexto,
                etiqueta = if (abierto) "Escribe un mensaje" else "Chat cerrado",
                ayuda = if (abierto) null else "Esta solicitud se rechazó o se canceló, así que ya no se puede escribir.",
                unaLinea = false,
                maxCaracteres = ValidacionesMensaje.MAX,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                habilitado = abierto,
                modifier = Modifier.weight(1f)
            )
            FilledIconButton(
                onClick = onEnviar,
                enabled = abierto && !enviando && texto.isNotBlank(),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar mensaje")
            }
        }
    }
}
