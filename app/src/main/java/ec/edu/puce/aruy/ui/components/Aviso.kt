package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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

/** Coloca el aviso en el centro de la ventana, para que nadie lo pase por alto. */
private object CentroDeLaVentana : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(
        (windowSize.width - popupContentSize.width) / 2,
        (windowSize.height - popupContentSize.height) / 2
    )
}

/**
 * Lugar de los avisos de la pantalla: aparecen en el CENTRO, en verde para los éxitos y en rojo para los
 * errores. Se cierran solos (o al tocarlos).
 */
@Composable
fun AvisoHost(estado: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(estado, modifier) { datos -> AvisoCentrado(datos) }
}

@Composable
private fun AvisoCentrado(datos: SnackbarData) {
    val visuales = datos.visuals
    val esError = (visuales as? AvisoVisuals)?.tipo == TipoAviso.ERROR
    val fondo = if (esError) MaterialTheme.colorScheme.error else Jade
    val letra = if (esError) MaterialTheme.colorScheme.onError else Color.White
    Popup(popupPositionProvider = CentroDeLaVentana, properties = PopupProperties(focusable = false)) {
        Surface(
            onClick = { datos.dismiss() },
            shape = RoundedCornerShape(20.dp),
            color = fondo,
            contentColor = letra,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(24.dp).widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (esError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = visuales.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
                visuales.actionLabel?.let { accion ->
                    TextButton(onClick = { datos.performAction() }) { Text(accion, color = letra) }
                }
            }
        }
    }
}
