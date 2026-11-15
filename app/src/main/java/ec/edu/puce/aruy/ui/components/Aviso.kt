package ec.edu.puce.aruy.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ec.edu.puce.aruy.ui.theme.Jade

/** Si algo salió bien (verde) o hubo un problema (rojo). */
enum class TipoAviso { EXITO, ERROR }

/** Un mensaje para la persona: verde si lo logró, rojo si no. */
data class Aviso(val texto: String, val tipo: TipoAviso) {
    companion object {
        fun exito(texto: String) = Aviso(texto, TipoAviso.EXITO)
        fun error(texto: String) = Aviso(texto, TipoAviso.ERROR)
    }
}

private class AvisoVisuals(
    override val message: String,
    val tipo: TipoAviso,
    override val actionLabel: String?,
    override val duration: SnackbarDuration
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
}

/**
 * Muestra un [Aviso] con acción opcional ("Deshacer"). Los errores duran más, porque suelen ser más largos.
 * Devuelve true si el usuario tocó la acción.
 */
suspend fun SnackbarHostState.avisar(aviso: Aviso, accion: String? = null): Boolean =
    showSnackbar(
        AvisoVisuals(
            message = aviso.texto,
            tipo = aviso.tipo,
            actionLabel = accion,
            duration = if (accion != null || aviso.tipo == TipoAviso.ERROR) SnackbarDuration.Long else SnackbarDuration.Short
        )
    ) == SnackbarResult.ActionPerformed

/** Barra de avisos de la pantalla: verde para los éxitos y roja para los errores. */
@Composable
fun AvisoHost(estado: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(estado, modifier) { datos ->
        val esError = (datos.visuals as? AvisoVisuals)?.tipo == TipoAviso.ERROR
        Snackbar(
            snackbarData = datos,
            containerColor = if (esError) MaterialTheme.colorScheme.error else Jade,
            contentColor = if (esError) MaterialTheme.colorScheme.onError else Color.White,
            actionColor = if (esError) MaterialTheme.colorScheme.onError else Color.White
        )
    }
}
