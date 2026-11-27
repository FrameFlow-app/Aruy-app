package ec.edu.puce.aruy.ui.screen.perfil

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import ec.edu.puce.aruy.ui.components.Aviso
import ec.edu.puce.aruy.ui.components.AvisoHost
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.AruySegmentado
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonPeligro
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.ui.theme.ModoTema
import ec.edu.puce.aruy.ui.theme.TamanoTexto
import ec.edu.puce.aruy.ui.viewmodel.AjustesViewModel
import kotlinx.coroutines.launch

@Composable
private fun Subtitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

/** Ajustes: tema, tamaño del texto, animaciones y caché de fotos (`ajustes` en `ajustes.js`). */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun AjustesScreen(viewModel: AjustesViewModel, onVolver: () -> Unit) {
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior("Ajustes", onVolver)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(Espacios.l),
                verticalArrangement = Arrangement.spacedBy(Espacios.l)
            ) {
                AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                    Subtitulo("Tema")
                    AruySegmentado(
                        opciones = listOf("Sistema", "Claro", "Oscuro"),
                        seleccionada = preferencias.modoTema.ordinal,
                        onSeleccion = { viewModel.elegirTema(ModoTema.entries[it]) },
                        modifier = Modifier.padding(top = Espacios.m)
                    )
                    Text(
                        "«Sistema» sigue el modo claro u oscuro de tu teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Espacios.m)
                    )
                }

                AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                    Subtitulo("Tamaño del texto")
                    AruySegmentado(
                        opciones = listOf("Normal", "Grande", "Muy grande"),
                        seleccionada = preferencias.tamanoTexto.ordinal,
                        onSeleccion = { viewModel.elegirTamanoTexto(TamanoTexto.entries[it]) },
                        modifier = Modifier.padding(top = Espacios.m)
                    )
                    Text(
                        "Así se verá el texto en Aruy.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(top = Espacios.m)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
                            .padding(Espacios.l)
                    )
                }

                AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                    Subtitulo("Movimiento")
                    AruyInterruptor(
                        titulo = "Reducir animaciones",
                        activado = preferencias.reducirAnimaciones,
                        onCambio = viewModel::reducirAnimaciones,
                        descripcion = "Quita los brillos y movimientos de las pantallas de carga."
                    )
                }

                AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                    Subtitulo("Datos guardados")
                    BotonContorno(
                        "Borrar fotos en caché",
                        onClick = {
                            contexto.imageLoader.memoryCache?.clear()
                            contexto.imageLoader.diskCache?.clear()
                            alcance.launch { snackbar.avisar(Aviso.exito("Listo. La próxima vez se descargan de nuevo.")) }
                        },
                        modifier = Modifier.padding(top = Espacios.m),
                        ancho = true
                    )
                }
            }
        }
        AvisoHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Permisos. Aruy solo usa el de notificaciones (opcional): la cámara se abre con la app de cámara del teléfono y
 * las fotos se eligen con el selector del sistema. Esta pantalla lo explica y lleva a los ajustes del teléfono.
 */
@Composable
fun PermisosScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    var notificacionesActivas by remember { mutableStateOf(NotificationManagerCompat.from(contexto).areNotificationsEnabled()) }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                notificacionesActivas = NotificationManagerCompat.from(contexto).areNotificationsEnabled()
            }
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Permisos", onVolver)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(Espacios.l),
            verticalArrangement = Arrangement.spacedBy(Espacios.m)
        ) {
            Text("Aruy solo usa el permiso de notificaciones, y es opcional.", style = MaterialTheme.typography.bodyLarge)
            AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notificaciones", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pastilla(
                        if (notificacionesActivas) "Activadas" else "Desactivadas",
                        tipo = if (notificacionesActivas) TipoPastilla.OK else TipoPastilla.NEUTRA
                    )
                }
                Text(
                    "Sirven para avisarte cuando llega una solicitud o responden la tuya. Son opcionales: sin ellas, Aruy funciona igual y ves todo dentro de la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Espacios.xs)
                )
                BotonContorno(
                    "Abrir ajustes de notificaciones",
                    onClick = {
                        val intento = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { contexto.startActivity(intento) }
                    },
                    modifier = Modifier.padding(top = Espacios.m),
                    ancho = true
                )
            }
            TarjetaPermiso("Cámara", "Para la foto de un trabajo se abre la app de cámara de tu teléfono. Aruy no tiene acceso a la cámara por su cuenta.")
            TarjetaPermiso("Galería", "Eliges las fotos con el selector del sistema: Aruy solo ve las que tú escoges, no toda tu galería.")
            TarjetaPermiso("Ubicación", "Aruy no usa tu ubicación.")
            BotonContorno(
                "Abrir ajustes del teléfono",
                onClick = {
                    contexto.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                ancho = true
            )
        }
    }
}

@Composable
private fun TarjetaPermiso(titulo: String, texto: String) {
    AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Espacios.xs))
    }
}

/** Mis datos y privacidad: qué se guarda y dónde. */
@Composable
fun PrivacidadScreen(onVolver: () -> Unit, onPermisos: () -> Unit, onBaja: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Mis datos y privacidad", onVolver)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(Espacios.l),
            verticalArrangement = Arrangement.spacedBy(Espacios.m)
        ) {
            AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                Text("Tu información es tuya", style = MaterialTheme.typography.titleMedium)
                Text("Aquí ves qué guarda Aruy y dónde.", style = MaterialTheme.typography.bodyMedium)
            }
            Dato(
                "Tu cuenta",
                "Tu nombre, celular y correo se guardan en el servidor de Aruy. La contraseña se guarda cifrada y nunca se vuelve a mostrar."
            )
            Dato(
                "En este teléfono",
                "Tus favoritos, personas bloqueadas, historial de contactos, reportes y el borrador de un servicio. " +
                    "La sesión se guarda cifrada y no se copia a respaldos."
            )
            Dato(
                "Tus fotos",
                "Las fotos de tus servicios se reducen y se convierten a WebP en tu teléfono, y se guardan en el almacenamiento de Aruy " +
                    "sin la ubicación ni los datos de la cámara. El análisis de etiquetas ocurre en tu teléfono, sin internet."
            )
            Dato("Tu ubicación", "Aruy no la usa ni la guarda.")
            Dato("Notificaciones", "Son opcionales: solo sirven para avisarte de solicitudes. Puedes activarlas o desactivarlas cuando quieras en Permisos.")
            BotonContorno("Revisar permisos", onPermisos, ancho = true)
            BotonPeligro("Eliminar mi cuenta", onBaja, modifier = Modifier.fillMaxWidth(), soloTexto = true)
        }
    }
}

@Composable
private fun Dato(titulo: String, texto: String) {
    AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Espacios.xs))
    }
}

// EXAMEN (Parte 1): pantalla "Acerca de". Lleva el texto institucional exacto que pide el profesor,
// el aviso de versión beta y el correo de contacto del equipo (no el institucional).
@Composable
fun AcercaScreen(onVolver: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Acerca de", onVolver)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(Espacios.l),
            verticalArrangement = Arrangement.spacedBy(Espacios.m)
        ) {
            AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                Text("Aruy · Versión beta", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Estás probando una versión beta. Puede tener errores y está pensada para pruebas con datos inventados.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Dato(
                "Proyecto académico",
                "Proyecto académico estudiantil de la carrera de Ingeniería de Sistemas de Información de la " +
                    "Pontificia Universidad Católica del Ecuador (PUCE), asignatura Emprendimiento Tecnológico, " +
                    "desarrollado con metodología de aprendizaje-servicio y con el apoyo de la Coordinación de " +
                    "Aprendizaje-Servicio. No es un producto oficial de la PUCE. " +
                    "Profesor tutor: Ing. Francisco Rodríguez, MsC · frodriguez@puce.edu.ec"
            )
            Dato("Contacto del equipo", "aruy.ec@gmail.com")
        }
    }
}
