package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.ui.theme.FormaPildora

/**
 * Campo de búsqueda en píldora con botón oscuro "Filtrar".
 *
 * - Modo falso: pasa [onClick] y el campo es un botón que abre la búsqueda (Inicio).
 * - Modo editable: pasa [value] y [onValueChange] (Buscar). [onBuscar] se dispara con la tecla de búsqueda.
 *
 * El botón "Filtrar" aparece si hay [onFiltrar]; [filtrosActivos] > 0 muestra la cuenta de filtros.
 */
@Composable
fun BarraBusqueda(
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar servicios",
    onClick: (() -> Unit)? = null,
    value: String = "",
    onValueChange: ((String) -> Unit)? = null,
    onBuscar: (() -> Unit)? = null,
    onFiltrar: (() -> Unit)? = null,
    filtrosActivos: Int = 0,
    focusRequester: FocusRequester? = null
) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = FormaPildora,
        color = esquema.surfaceContainerHigh
    ) {
        Row(
            Modifier.padding(start = Espacios.l, end = Espacios.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacios.m)
        ) {
            val editable = onClick == null && onValueChange != null
            val zonaCampo = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .then(
                    if (onClick != null) {
                        Modifier
                            .clickable(onClick = onClick, role = Role.Button)
                            .semantics { contentDescription = placeholder }
                    } else Modifier
                )
            Row(
                zonaCampo,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacios.m)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = esquema.onSurfaceVariant)
                if (editable) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = esquema.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange!!,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = esquema.onSurface),
                            cursorBrush = SolidColor(esquema.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onBuscar?.invoke() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                                .semantics { contentDescription = placeholder }
                        )
                    }
                } else {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = esquema.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (onFiltrar != null) BotonFiltrar(onFiltrar, filtrosActivos)
        }
    }
}

@Composable
private fun BotonFiltrar(onClick: () -> Unit, cuenta: Int) {
    val esquema = MaterialTheme.colorScheme
    val descripcion = if (cuenta > 0) "Filtrar, $cuenta filtros activos" else "Filtrar"
    Surface(
        onClick = onClick,
        shape = FormaPildora,
        color = esquema.inverseSurface,
        contentColor = esquema.inverseOnSurface,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = descripcion }
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = Espacios.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacios.s)
        ) {
            Text("Filtrar", style = MaterialTheme.typography.labelLarge)
            if (cuenta > 0) {
                Surface(
                    shape = CircleShape,
                    color = esquema.inverseOnSurface,
                    contentColor = esquema.inverseSurface,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(cuenta.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
