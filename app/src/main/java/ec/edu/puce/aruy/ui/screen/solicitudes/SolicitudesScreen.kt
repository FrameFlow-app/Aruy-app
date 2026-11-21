package ec.edu.puce.aruy.ui.screen.solicitudes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import ec.edu.puce.aruy.ui.theme.Espacios
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ec.edu.puce.aruy.domain.catalogo.Contacto
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.model.accionesPara
import ec.edu.puce.aruy.domain.solicitudes.TiempoRelativo
import ec.edu.puce.aruy.domain.validation.ValidacionesSolicitud
import ec.edu.puce.aruy.ui.components.AruyAvatar
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyHoja
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.Aviso
import ec.edu.puce.aruy.ui.components.AvisoHost
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTonal
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.components.rememberPedirNotificaciones
import ec.edu.puce.aruy.ui.viewmodel.SolicitudesViewModel
import ec.edu.puce.aruy.util.abrirWhatsApp
import kotlinx.coroutines.launch

/** Un cambio de estado que está esperando confirmación o un mensaje para la otra persona. */
private data class CambioPendiente(val solicitud: Solicitud, val nuevo: EstadoSolicitud)

/**
 * "Mis solicitudes": lo que pediste y, si ofreces servicios, lo que te pidieron. El prestador acepta o rechaza;
 * quien pidió puede cancelar. Cada tarjeta abre el chat; el WhatsApp solo se comparte si el prestador lo decide.
 */
@Composable
fun SolicitudesScreen(
    viewModel: SolicitudesViewModel,
    onVolver: () -> Unit,
    onChat: (String) -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val contexto = LocalContext.current
    val pedirNotificaciones = rememberPedirNotificaciones(snackbar)
    var cambio by remember { mutableStateOf<CambioPendiente?>(null) }

    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }
    // Aquí es donde las notificaciones sirven: se pide el permiso al abrir la bandeja
    LaunchedEffect(Unit) { pedirNotificaciones() }

    fun escribir(solicitud: Solicitud) {
        val numero = Contacto.numeroWhatsApp(solicitud.contraparte.telefono)
        val texto = "Hola ${solicitud.contraparte.nombre}, te escribo por la solicitud de «${solicitud.servicioTitulo ?: "tu servicio"}» en Aruy."
        alcance.launch {
            when {
                numero == null -> snackbar.avisar(Aviso.error("No tenemos un número de WhatsApp válido para esta persona."))
                !abrirWhatsApp(contexto, numero, texto) -> snackbar.avisar(Aviso.error("No pudimos abrir WhatsApp en este teléfono."))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior("Mis solicitudes", onVolver)
            if (ui.puedeRecibir) {
                TabRow(selectedTabIndex = if (ui.tipo == TipoSolicitudes.RECIBIDAS) 0 else 1) {
                    Tab(
                        selected = ui.tipo == TipoSolicitudes.RECIBIDAS,
                        onClick = { viewModel.elegirPestana(TipoSolicitudes.RECIBIDAS) },
                        text = { Text("Recibidas") }
                    )
                    Tab(
                        selected = ui.tipo == TipoSolicitudes.ENVIADAS,
                        onClick = { viewModel.elegirPestana(TipoSolicitudes.ENVIADAS) },
                        text = { Text("Enviadas") }
                    )
                }
            }
            when {
                ui.cargando && ui.items.isEmpty() -> EstadoCargando("Cargando tus solicitudes…")
                ui.error != null && ui.items.isEmpty() -> EstadoVacio(
                    titulo = "No pudimos cargar tus solicitudes",
                    texto = ui.error.orEmpty(),
                    icono = Icons.Filled.Inbox,
                    textoBoton = "Reintentar",
                    onBoton = viewModel::cargar
                )
                ui.items.isEmpty() -> EstadoVacio(
                    titulo = if (ui.tipo == TipoSolicitudes.RECIBIDAS) "Todavía no te han pedido nada" else "Todavía no has enviado solicitudes",
                    texto = if (ui.tipo == TipoSolicitudes.RECIBIDAS) {
                        "Cuando alguien pida uno de tus servicios, aparece aquí y te avisamos."
                    } else {
                        "Entra a un servicio y toca «Solicitar y chatear». Aquí verás qué te responden."
                    },
                    icono = Icons.Filled.Inbox,
                    textoBoton = "Actualizar",
                    onBoton = viewModel::cargar
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(Espacios.l),
                    verticalArrangement = Arrangement.spacedBy(Espacios.m)
                ) {
                    items(ui.items, key = { it.id }) { solicitud ->
                        TarjetaSolicitud(
                            solicitud = solicitud,
                            tipo = ui.tipo,
                            trabajando = ui.trabajandoId == solicitud.id,
                            ocupado = ui.trabajandoId != null,
                            onAbrirChat = { onChat(solicitud.id) },
                            onCambiar = { nuevo -> cambio = CambioPendiente(solicitud, nuevo) },
                            onEscribir = { escribir(solicitud) }
                        )
                    }
                    item { BotonTexto("Actualizar", viewModel::cargar, modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
        AvisoHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    cambio?.let { pendiente ->
        val s = pendiente.solicitud
        when (pendiente.nuevo) {
            EstadoSolicitud.ACEPTADA, EstadoSolicitud.RECHAZADA -> HojaRespuesta(
                aceptar = pendiente.nuevo == EstadoSolicitud.ACEPTADA,
                nombre = s.contraparte.nombre,
                onConfirmar = { respuesta ->
                    cambio = null
                    viewModel.cambiarEstado(s, pendiente.nuevo, respuesta)
                },
                onCerrar = { cambio = null }
            )
            else -> DialogoConfirmacion(
                titulo = if (pendiente.nuevo == EstadoSolicitud.CANCELADA) "¿Cancelar la solicitud?" else "¿Marcar como completada?",
                texto = if (pendiente.nuevo == EstadoSolicitud.CANCELADA) {
                    "${s.contraparte.nombre} verá que se canceló."
                } else {
                    "Confirma que el trabajo con ${s.contraparte.nombre} ya se hizo."
                },
                textoConfirmar = if (pendiente.nuevo == EstadoSolicitud.CANCELADA) "Cancelar solicitud" else "Sí, completada",
                textoCancelar = "Volver",
                peligroso = pendiente.nuevo == EstadoSolicitud.CANCELADA,
                onConfirmar = {
                    cambio = null
                    viewModel.cambiarEstado(s, pendiente.nuevo)
                },
                onCancelar = { cambio = null }
            )
        }
    }
}

@Composable
private fun TarjetaSolicitud(
    solicitud: Solicitud,
    tipo: TipoSolicitudes,
    trabajando: Boolean,
    ocupado: Boolean,
    onAbrirChat: () -> Unit,
    onCambiar: (EstadoSolicitud) -> Unit,
    onEscribir: () -> Unit
) {
    val acciones = solicitud.accionesPara(tipo)
    AruyTarjeta(modifier = Modifier.fillMaxWidth(), onClick = onAbrirChat) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacios.m)) {
            AruyAvatar(solicitud.contraparte.nombre, fotoUrl = solicitud.contraparte.fotoPerfil)
            Column(Modifier.weight(1f)) {
                Text(solicitud.contraparte.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    solicitud.servicioTitulo ?: "Servicio eliminado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Espacios.xs)) {
                Pastilla(solicitud.estado.etiqueta, tipo = tipoDePastilla(solicitud.estado))
                if (solicitud.sinVer) Pastilla("Nuevo", tipo = TipoPastilla.ROSA)
                if (solicitud.mensajesSinLeer > 0) {
                    Pastilla(
                        if (solicitud.mensajesSinLeer == 1) "1 mensaje" else "${solicitud.mensajesSinLeer} mensajes",
                        tipo = TipoPastilla.ERROR
                    )
                }
            }
        }
        Text(solicitud.mensaje, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Espacios.s))
        solicitud.respuesta?.let {
            Text(
                "Respuesta: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Espacios.xs)
            )
        }
        Text(
            TiempoRelativo.desde(solicitud.fechaCreacion),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Espacios.xs)
        )

        solicitud.ultimoMensaje?.let { ultimo ->
            Text(
                "Último mensaje: ${ultimo.texto}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (solicitud.mensajesSinLeer > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Espacios.xs)
            )
        }

        BotonTonal(
            "Abrir chat", onAbrirChat,
            icono = Icons.AutoMirrored.Filled.Send, ancho = true, modifier = Modifier.padding(top = Espacios.s)
        )
        if (solicitud.whatsappCompartido && Contacto.numeroWhatsApp(solicitud.contraparte.telefono) != null) {
            BotonContorno(
                "Escribir por WhatsApp", onEscribir,
                icono = Icons.AutoMirrored.Filled.Chat, ancho = true, modifier = Modifier.padding(top = Espacios.s)
            )
        }
        if (acciones.isNotEmpty()) {
            Row(Modifier.padding(top = Espacios.s), horizontalArrangement = Arrangement.spacedBy(Espacios.s)) {
                acciones.forEach { accion ->
                    val principal = accion == EstadoSolicitud.ACEPTADA || accion == EstadoSolicitud.COMPLETADA
                    val modificador = Modifier.weight(1f)
                    if (principal) {
                        BotonRelleno(textoDeAccion(accion), { onCambiar(accion) }, modifier = modificador, cargando = trabajando, habilitado = !ocupado)
                    } else {
                        BotonContorno(textoDeAccion(accion), { onCambiar(accion) }, modifier = modificador, habilitado = !ocupado)
                    }
                }
            }
        }
    }
}

/** Para aceptar o rechazar: un mensaje opcional para la otra persona. */
@Composable
private fun HojaRespuesta(aceptar: Boolean, nombre: String, onConfirmar: (String?) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    AruyHoja(if (aceptar) "Aceptar la solicitud" else "Rechazar la solicitud", onCerrar) {
        Text(
            if (aceptar) {
                "$nombre verá tu respuesta y podrán seguir hablando por el chat. Tu WhatsApp solo se comparte si tú lo decides."
            } else {
                "$nombre verá que no puedes ahora. Puedes explicarle por qué (es opcional)."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AruyCampo(
            valor = texto,
            onValor = { texto = it.take(ValidacionesSolicitud.RESPUESTA_MAX) },
            etiqueta = "Mensaje (opcional)",
            placeholder = if (aceptar) "Ejemplo: puedo el sábado a las 9." else "Ejemplo: ese día no tengo cupo.",
            unaLinea = false,
            lineasMin = 3,
            maxCaracteres = ValidacionesSolicitud.RESPUESTA_MAX
        )
        if (aceptar) {
            BotonRelleno("Aceptar solicitud", { onConfirmar(texto.trim().ifEmpty { null }) }, grande = true, ancho = true)
        } else {
            BotonContorno("Rechazar solicitud", { onConfirmar(texto.trim().ifEmpty { null }) }, grande = true, ancho = true)
        }
        BotonTexto("Volver", onCerrar)
    }
}

private fun tipoDePastilla(estado: EstadoSolicitud): TipoPastilla = when (estado) {
    EstadoSolicitud.PENDIENTE -> TipoPastilla.ADVERTENCIA
    EstadoSolicitud.ACEPTADA -> TipoPastilla.OK
    EstadoSolicitud.RECHAZADA -> TipoPastilla.ERROR
    EstadoSolicitud.CANCELADA -> TipoPastilla.NEUTRA
    EstadoSolicitud.COMPLETADA -> TipoPastilla.INFO
}

private fun textoDeAccion(estado: EstadoSolicitud): String = when (estado) {
    EstadoSolicitud.ACEPTADA -> "Aceptar"
    EstadoSolicitud.RECHAZADA -> "Rechazar"
    EstadoSolicitud.COMPLETADA -> "Completada"
    EstadoSolicitud.CANCELADA -> "Cancelar"
    EstadoSolicitud.PENDIENTE -> "Pendiente"
}
