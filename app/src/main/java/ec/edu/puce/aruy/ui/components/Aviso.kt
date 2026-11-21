package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import ec.edu.puce.aruy.ui.theme.AruyTheme
import ec.edu.puce.aruy.ui.theme.Espacios
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

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

/**
 * Lugar de los avisos de la pantalla: una tarjeta con los colores del tema (verde para éxitos, rojo para errores)
 * que se apoya sobre la barra flotante, sin tapar botones. Se cierra sola (o al tocarla).
 */
@Composable
fun AvisoHost(estado: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(estado, modifier) { datos -> AvisoTarjeta(datos) }
}

@Composable
private fun AvisoTarjeta(datos: SnackbarData) {
    val visuales = datos.visuals
    val esError = (visuales as? AvisoVisuals)?.tipo == TipoAviso.ERROR
    val colores = AruyTheme.colores
    val esquema = MaterialTheme.colorScheme
    val fondo = if (esError) esquema.errorContainer else colores.okContainer
    val letra = if (esError) esquema.onErrorContainer else colores.onOkContainer
    Box(Modifier.fillMaxWidth().padding(horizontal = Espacios.l, vertical = Espacios.s), contentAlignment = Alignment.Center) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = fondo,
            contentColor = letra,
            shadowElevation = Espacios.s,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = "Cerrar aviso", role = Role.Button) { datos.dismiss() }
        ) {
            Row(
                modifier = Modifier.padding(start = Espacios.l, end = Espacios.s, top = Espacios.s, bottom = Espacios.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacios.m)
            ) {
                Icon(
                    imageVector = if (esError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(Espacios.xxl)
                )
                Text(
                    text = visuales.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = Espacios.s)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
                visuales.actionLabel?.let { accion ->
                    BotonTexto(texto = accion, onClick = { datos.performAction() })
                }
            }
        }
    }
}
