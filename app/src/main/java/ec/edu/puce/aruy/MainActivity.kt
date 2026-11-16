package ec.edu.puce.aruy

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import ec.edu.puce.aruy.data.local.PreferenciasUi
import ec.edu.puce.aruy.ui.navigation.AruyApp
import ec.edu.puce.aruy.ui.theme.AruyTheme
import ec.edu.puce.aruy.ui.theme.ModoTema

class MainActivity : ComponentActivity() {

    /** La pantalla que pidió abrir una notificación (por ejemplo, Mis solicitudes). */
    private var abrirRuta by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        abrirRuta = intent?.getStringExtra(EXTRA_ABRIR)

        enableEdgeToEdge()

        val contenedor = (application as AruyApplication).contenedor

        setContent {
            val preferencias by contenedor.preferencias.preferencias.collectAsStateWithLifecycle(initialValue = PreferenciasUi())

            // Los íconos de las barras del sistema deben verse sobre el tema elegido en Ajustes,
            // no solo sobre el del teléfono (si no, "Oscuro" con el sistema en claro queda ilegible)
            val oscuro = when (preferencias.modoTema) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }
            LaunchedEffect(oscuro) {
                val estilo = if (oscuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }

            AruyTheme(
                modo = preferencias.modoTema,
                tamanoTexto = preferencias.tamanoTexto,
                reducirAnimaciones = preferencias.reducirAnimaciones
            ) {
                AruyApp(contenedor = contenedor, abrirRuta = abrirRuta, alAbrirRuta = { abrirRuta = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        abrirRuta = intent.getStringExtra(EXTRA_ABRIR)
    }

    companion object {
        const val EXTRA_ABRIR = "abrir_ruta"
    }
}
