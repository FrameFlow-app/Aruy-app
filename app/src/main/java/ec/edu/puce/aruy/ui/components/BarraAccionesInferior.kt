package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.theme.Espacios

/**
 * Barra de acciones fija al pie de una pantalla: un botón secundario (tonal) y uno primario (relleno)
 * lado a lado. Pasa botones con `ancho = true` para que llenen su mitad.
 */
@Composable
fun BarraAccionesInferior(
    secundario: @Composable () -> Unit,
    primario: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = Espacios.l, vertical = Espacios.m),
            horizontalArrangement = Arrangement.spacedBy(Espacios.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { secundario() }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { primario() }
        }
    }
}
