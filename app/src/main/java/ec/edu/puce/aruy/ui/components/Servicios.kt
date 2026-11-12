package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Foundation
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import ec.edu.puce.aruy.domain.catalogo.FiltroCatalogo
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

val CategoriaServicio.icono: ImageVector
    get() = when (this) {
        CategoriaServicio.PLOMERIA -> Icons.Filled.Plumbing
        CategoriaServicio.ELECTRICIDAD -> Icons.Filled.ElectricalServices
        CategoriaServicio.ALBANILERIA -> Icons.Filled.Foundation
        CategoriaServicio.PINTURA -> Icons.Filled.FormatPaint
        CategoriaServicio.CARPINTERIA -> Icons.Filled.Carpenter
        CategoriaServicio.BELLEZA -> Icons.Filled.Spa
        CategoriaServicio.OTROS -> Icons.Filled.MiscellaneousServices
    }

private val SIMBOLOS_EC = DecimalFormatSymbols(Locale.forLanguageTag("es-EC"))

/** "$20" o "$12,50"; sin precio: "Precio a convenir". */
fun formatoDinero(valor: Double): String =
    "$" + DecimalFormat(if (valor % 1.0 == 0.0) "#,##0" else "#,##0.00", SIMBOLOS_EC).format(valor)

fun textoPrecio(precio: Double?): String =
    if (precio == null) "Precio a convenir" else "Desde ${formatoDinero(precio)}"

/** Foto de portada del servicio; si no tiene fotos, un fondo con el ícono de su categoría. */
@Composable
fun PortadaServicio(
    servicioId: String,
    categoria: CategoriaServicio,
    cargarPortada: suspend (String) -> String?,
    modifier: Modifier = Modifier,
    forma: Shape = MaterialTheme.shapes.medium
) {
    var url by remember(servicioId) { mutableStateOf<String?>(null) }
    LaunchedEffect(servicioId) { url = cargarPortada(servicioId) }
    Box(
        // Primero se recorta y después se pinta el fondo: al revés, el fondo asoma por las esquinas
        modifier = modifier
            .clip(forma)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                categoria.icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/**
 * Datos que una persona invitada no puede ver: se muestra texto de relleno desenfocado, como en
 * `components.js` del prototipo, para no dejar salir información real.
 */
@Composable
private fun DatosOcultos(modifier: Modifier = Modifier) {
    Column(modifier.clearAndSetSemantics { contentDescription = "Disponible al crear tu cuenta" }) {
        Text("Nombre Apellido", Modifier.blur(5.dp), style = MaterialTheme.typography.bodyMedium)
        Text("4,8 (30) · Desde \$00", Modifier.blur(5.dp), style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Tarjeta grande del carrusel de Inicio: foto con esquinas muy redondeadas, título y precio en una
 * fila, y debajo el prestador con su calificación. El corazón flotante solo aparece si hay [onFavorito].
 */
@Composable
fun TarjetaServicio(
    item: ServicioConPrestador,
    invitado: Boolean,
    cargarPortada: suspend (String) -> String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    esFavorito: Boolean = false,
    onFavorito: (() -> Unit)? = null
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.width(240.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.padding(Espacios.s)) {
            PortadaServicio(
                item.id, item.servicio.categoria, cargarPortada,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                forma = MaterialTheme.shapes.large
            )
            if (onFavorito != null) {
                BotonCircular(
                    icono = if (esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    descripcion = if (esFavorito) "Quitar de guardados" else "Guardar servicio",
                    onClick = onFavorito,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Espacios.s),
                    variante = VarianteCircular.SOBRE_FOTO,
                    seleccionado = esFavorito
                )
            }
        }
        Column(
            Modifier.padding(start = Espacios.l, end = Espacios.l, top = Espacios.xs, bottom = Espacios.l),
            verticalArrangement = Arrangement.spacedBy(Espacios.xs)
        ) {
            if (invitado) {
                Text(
                    item.servicio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                DatosOcultos()
            } else {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Espacios.s)
                ) {
                    Text(
                        item.servicio.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        textoPrecio(item.servicio.precioReferencial),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    item.nombrePrestador,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Calificacion(item.prestador?.promedio, item.prestador?.totalCalificaciones ?: 0)
            }
        }
    }
}

/** Fila de las listas (Buscar, Mejor valorados, Vistos, Guardados). */
@Composable
fun FilaServicio(
    item: ServicioConPrestador,
    invitado: Boolean,
    cargarPortada: suspend (String) -> String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(Espacios.m),
            horizontalArrangement = Arrangement.spacedBy(Espacios.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortadaServicio(
                item.id, item.servicio.categoria, cargarPortada, Modifier.size(88.dp),
                forma = MaterialTheme.shapes.medium
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.servicio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (invitado) {
                    DatosOcultos()
                } else {
                    Text(
                        item.nombrePrestador,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Calificacion(item.prestador?.promedio, item.prestador?.totalCalificaciones ?: 0)
                        Text(textoPrecio(item.servicio.precioReferencial), style = MaterialTheme.typography.labelLarge)
                    }
                    if (!FiltroCatalogo.disponibleAhora(item)) Pastilla("No disponible", tipo = TipoPastilla.ADVERTENCIA)
                }
            }
            accion?.invoke()
        }
    }
}
