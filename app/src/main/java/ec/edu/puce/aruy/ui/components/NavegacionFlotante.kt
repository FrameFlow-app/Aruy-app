package ec.edu.puce.aruy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.theme.AruyTheme
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.ui.theme.FormaPildora

/** Un destino de la [NavegacionFlotante]; [ruta] identifica el destino al seleccionarlo. */
data class ItemNavegacionFlotante(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

/**
 * Barra inferior flotante en píldora: contenedor redondeado con margen respecto a los bordes de la
 * pantalla y el destino activo resaltado con un indicador relleno. Íconos con etiqueta.
 */
@Composable
fun NavegacionFlotante(
    items: List<ItemNavegacionFlotante>,
    rutaActual: String?,
    onSeleccionar: (ItemNavegacionFlotante) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = Espacios.l, vertical = Espacios.m)
            .fillMaxWidth(),
        shape = FormaPildora,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            Modifier.padding(Espacios.xs),
            horizontalArrangement = Arrangement.spacedBy(Espacios.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                ElementoFlotante(
                    item = item,
                    seleccionado = rutaActual == item.ruta,
                    onClick = { onSeleccionar(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ElementoFlotante(
    item: ItemNavegacionFlotante,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val esquema = MaterialTheme.colorScheme
    val animacion = if (AruyTheme.reducirAnimaciones) snap<Color>() else tween(200)
    val fondo by animateColorAsState(
        if (seleccionado) esquema.primary else Color.Transparent,
        animationSpec = animacion, label = "fondoItem"
    )
    val tinta by animateColorAsState(
        if (seleccionado) esquema.onPrimary else esquema.onSurfaceVariant,
        animationSpec = animacion, label = "tintaItem"
    )
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(FormaPildora)
            .drawBehind { drawRect(fondo) }
            .selectable(selected = seleccionado, onClick = onClick, role = Role.Tab)
            .padding(horizontal = Espacios.xs, vertical = Espacios.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(item.icono, contentDescription = null, tint = tinta, modifier = Modifier.size(24.dp))
        Text(
            item.titulo,
            style = MaterialTheme.typography.labelMedium,
            color = tinta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
