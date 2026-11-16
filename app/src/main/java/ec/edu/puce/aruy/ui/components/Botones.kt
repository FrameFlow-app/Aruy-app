package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.layout.Spacer
import ec.edu.puce.aruy.ui.theme.AruyTheme
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.ui.theme.FormaPildora
import ec.edu.puce.aruy.ui.theme.Superposiciones

// Botones del rediseño: píldora de 52 dp, "grande" de 56 dp

private val AlturaNormal = 52.dp
private val AlturaGrande = 56.dp
private val AlturaMinimaToque = 48.dp
private val RellenoBoton = PaddingValues(horizontal = Espacios.xxl, vertical = 0.dp)

@Composable
private fun ContenidoBoton(
    texto: String,
    icono: ImageVector?,
    cargando: Boolean,
    colorProgreso: Color
) {
    if (cargando) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = colorProgreso
        )
    } else if (icono != null) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
    }
    if (cargando || icono != null) Spacer(Modifier.size(Espacios.s))
    Text(texto, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun BotonRelleno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    grande: Boolean = false,
    ancho: Boolean = false,
    cargando: Boolean = false,
    habilitado: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = modifier.botonMedidas(grande, ancho),
        contentPadding = RellenoBoton
    ) { ContenidoBoton(texto, icono, cargando, MaterialTheme.colorScheme.onPrimary) }
}

@Composable
fun BotonTonal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    grande: Boolean = false,
    ancho: Boolean = false,
    habilitado: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.botonMedidas(grande, ancho),
        shape = FormaPildora,
        contentPadding = RellenoBoton
    ) { ContenidoBoton(texto, icono, false, MaterialTheme.colorScheme.onSecondaryContainer) }
}

@Composable
fun BotonSuave(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    grande: Boolean = false,
    ancho: Boolean = false,
    habilitado: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.botonMedidas(grande, ancho),
        shape = FormaPildora,
        contentPadding = RellenoBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) { ContenidoBoton(texto, icono, false, MaterialTheme.colorScheme.onPrimaryContainer) }
}

@Composable
fun BotonContorno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    grande: Boolean = false,
    ancho: Boolean = false,
    habilitado: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.botonMedidas(grande, ancho),
        shape = FormaPildora,
        contentPadding = RellenoBoton,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) { ContenidoBoton(texto, icono, false, MaterialTheme.colorScheme.primary) }
}

@Composable
fun BotonTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.heightIn(min = AlturaMinimaToque),
        shape = FormaPildora
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

/** Botón verde de "Contactar por WhatsApp". */
@Composable
fun BotonWhatsApp(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.botonMedidas(grande = true, ancho = true),
        shape = FormaPildora,
        contentPadding = RellenoBoton,
        colors = ButtonDefaults.buttonColors(
            containerColor = AruyTheme.colores.whatsapp,
            contentColor = Superposiciones.tintaSobreVelo
        )
    ) { ContenidoBoton(texto, icono, false, Superposiciones.tintaSobreVelo) }
}

/** Acciones destructivas: "Eliminar servicio", "Eliminar mi cuenta". */
@Composable
fun BotonPeligro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    soloTexto: Boolean = false,
    ancho: Boolean = false,
    cargando: Boolean = false
) {
    if (soloTexto) {
        TextButton(
            onClick = onClick,
            enabled = !cargando,
            modifier = modifier.heightIn(min = AlturaMinimaToque),
            shape = FormaPildora,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
    } else {
        Button(
            onClick = onClick,
            enabled = !cargando,
            modifier = modifier.botonMedidas(grande = false, ancho = ancho),
            shape = FormaPildora,
        contentPadding = RellenoBoton,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) { ContenidoBoton(texto, null, cargando, MaterialTheme.colorScheme.onError) }
    }
}

private fun Modifier.botonMedidas(grande: Boolean, ancho: Boolean): Modifier =
    this
        .heightIn(min = if (grande) AlturaGrande else AlturaNormal)
        .then(if (ancho) Modifier.fillMaxWidth() else Modifier)

/** Fila de botones separados 12 dp, para pies de hoja y diálogos. */
@Composable
fun FilaBotones(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacios.m),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Aspecto del [BotonCircular]. */
enum class VarianteCircular { SOBRE_FOTO, TONAL, RELLENO }

/**
 * Botón circular de 48 dp (corazón sobre una foto, atrás, compartir...). [seleccionado] marca un
 * estado activo, por ejemplo "guardado": cambia el color del ícono.
 */
@Composable
fun BotonCircular(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteCircular = VarianteCircular.SOBRE_FOTO,
    seleccionado: Boolean = false
) {
    val esquema = MaterialTheme.colorScheme
    val colores = when (variante) {
        VarianteCircular.SOBRE_FOTO -> IconButtonDefaults.filledIconButtonColors(
            containerColor = Superposiciones.sobreFoto,
            contentColor = if (seleccionado) esquema.error else Superposiciones.sobreFotoTinta
        )
        VarianteCircular.TONAL -> IconButtonDefaults.filledIconButtonColors(
            containerColor = esquema.surfaceContainerHigh,
            contentColor = if (seleccionado) esquema.error else esquema.onSurface
        )
        VarianteCircular.RELLENO -> IconButtonDefaults.filledIconButtonColors(
            containerColor = esquema.primary,
            contentColor = esquema.onPrimary
        )
    }
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(AlturaMinimaToque),
        colors = colores
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(22.dp))
    }
}
