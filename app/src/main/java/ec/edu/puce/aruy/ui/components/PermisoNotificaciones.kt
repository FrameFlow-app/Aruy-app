package ec.edu.puce.aruy.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Devuelve una función que pide el permiso para mostrar notificaciones (Android 13 o más). Se llama en el momento en
 * que sirve de algo ("Te avisaremos cuando responda"), no al abrir la app. Si ya está concedido, no hace nada.
 */
@Composable
fun rememberPedirNotificaciones(): () -> Unit {
    val contexto = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
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
