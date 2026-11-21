package ec.edu.puce.aruy.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ec.edu.puce.aruy.domain.model.Mantenimiento
import java.time.OffsetDateTime
import java.time.ZoneId
import ec.edu.puce.aruy.domain.avisos.textoFechasMantenimiento as textoFechas

private const val URL_APK = "https://github.com/FrameFlow-app/Aruy-app/releases/latest/download/aruy.apk"
private const val PLAY_STORE = "com.android.vending"

/** Aviso de mantenimiento programado: se lee y se cierra con "Entendido". */
@Composable
fun DialogoMantenimiento(mantenimiento: Mantenimiento, onEntendido: () -> Unit) {
    val fechas = remember(mantenimiento) { textoFechasMantenimiento(mantenimiento.inicio, mantenimiento.fin) }
    AlertDialog(
        onDismissRequest = onEntendido,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = { Icon(Icons.Filled.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Mantenimiento programado") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (mantenimiento.mensaje.isNotBlank()) Text(mantenimiento.mensaje)
                if (fechas != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(fechas, style = MaterialTheme.typography.titleSmall)
                }
            }
        },
        confirmButton = { BotonTexto("Entendido", onEntendido) }
    )
}

/** Aviso de versión nueva. Si es obligatoria no se puede cerrar ni dejar para después. */
@Composable
fun DialogoActualizacion(
    obligatoria: Boolean,
    notas: String?,
    onActualizar: () -> Unit,
    onMasTarde: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!obligatoria) onMasTarde() },
        properties = if (obligatoria) {
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        } else DialogProperties(),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(if (obligatoria) "Actualiza Aruy para continuar" else "Hay una versión nueva de Aruy") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (obligatoria) Text("Esta versión ya no funciona. Instala la nueva para seguir usando Aruy.")
                if (!notas.isNullOrBlank()) {
                    if (obligatoria) Spacer(Modifier.height(8.dp))
                    Text(notas)
                }
            }
        },
        confirmButton = { BotonRelleno("Actualizar", onActualizar) },
        dismissButton = if (obligatoria) null else ({ BotonTexto("Más tarde", onMasTarde) })
    )
}

/** Abre Play Store si la app se instaló desde ahí; si no, descarga el APK más reciente de GitHub. */
fun abrirActualizacion(contexto: Context) {
    val paquete = contexto.packageName
    if (origenDeInstalacion(contexto) == PLAY_STORE) {
        val mercado = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$paquete"))
        try {
            contexto.startActivity(mercado.conNuevaTarea(contexto))
        } catch (e: ActivityNotFoundException) {
            abrirEnlace(contexto, "https://play.google.com/store/apps/details?id=$paquete")
        }
    } else {
        abrirEnlace(contexto, URL_APK)
    }
}

private fun abrirEnlace(contexto: Context, url: String) {
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).conNuevaTarea(contexto))
    } catch (e: ActivityNotFoundException) {
        // Sin navegador instalado: no hay a dónde ir
    }
}

private fun Intent.conNuevaTarea(contexto: Context): Intent =
    if (contexto is android.app.Activity) this else addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Suppress("DEPRECATION")
private fun origenDeInstalacion(contexto: Context): String? = try {
    val pm = contexto.packageManager
    if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(contexto.packageName).installingPackageName
    else pm.getInstallerPackageName(contexto.packageName)
} catch (e: Exception) {
    null
}

/** "sábado 22 de nov., 22:00 a 23:00" en la hora del teléfono; null si no hay fechas válidas. */
internal fun textoFechasMantenimiento(
    inicio: String?,
    fin: String?,
    zona: ZoneId = ZoneId.systemDefault()
): String? {
    return textoFechas(parsear(inicio, zona), parsear(fin, zona), java.time.ZonedDateTime.now(zona))
}

private fun parsear(texto: String?, zona: ZoneId) = texto?.takeIf { it.isNotBlank() }?.let {
    try {
        OffsetDateTime.parse(it).atZoneSameInstant(zona)
    } catch (e: Exception) {
        null
    }
}
