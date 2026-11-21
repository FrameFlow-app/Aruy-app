package ec.edu.puce.aruy.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Devuelve una función que pide el permiso para mostrar notificaciones (Android 13 o más). Se llama en el momento en
 * que sirve de algo ("Te avisaremos cuando responda"), no al abrir la app. Si ya está concedido, no hace nada.
 * Si la persona lo niega y se pasa [avisos], se le muestra un aviso corto con acceso a los Ajustes del sistema.
 */
@Composable
fun rememberPedirNotificaciones(avisos: SnackbarHostState? = null): () -> Unit {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val avisosActuales by rememberUpdatedState(avisos)
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        val host = avisosActuales
        if (!concedido && host != null) {
            alcance.launch {
                val irAAjustes = host.avisar(
                    Aviso.error("Sin notificaciones no sabrás cuándo responden tu solicitud."),
                    accion = "Ajustes"
                )
                if (irAAjustes) {
                    val intento = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { contexto.startActivity(intento) }
                }
            }
        }
    }
    return remember(lanzador, contexto) {
        {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
