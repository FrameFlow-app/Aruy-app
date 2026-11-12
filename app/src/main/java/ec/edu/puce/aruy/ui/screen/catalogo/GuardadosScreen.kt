package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.TiempoRelativo
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonPeligro
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.tituloDeError
import ec.edu.puce.aruy.ui.components.TarjetaServicio
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.ui.theme.FormaPildora
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel

/** Guardados: favoritos y prestadores contactados (`guardados.js`). Todo vive en el teléfono. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GuardadosScreen(
    catalogo: CatalogoViewModel,
    actividad: ActividadLocalViewModel,
    onServicio: (String) -> Unit,
    onBuscar: () -> Unit
) {
    val estado by catalogo.catalogo.collectAsState()
    val favoritosIds by actividad.favoritosIds.collectAsState()
    val contactos by actividad.contactos.collectAsState()
    var pestana by rememberSaveable { mutableStateOf(0) }
    var confirmarBorrado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { catalogo.cargarSiHaceFalta() }

    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacios.l, vertical = Espacios.s)
        ) {
            listOf("Favoritos", "Contactados").forEachIndexed { i, texto ->
                SegmentedButton(
                    selected = pestana == i,
                    onClick = { pestana = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 2, FormaPildora),
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        inactiveBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    icon = {},
                    label = { Text(texto, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
        if (pestana == 0) {
            when (val e = estado) {
                EstadoApi.Inactivo, EstadoApi.Cargando -> EstadoCargando("Cargando tus favoritos…")
                is EstadoApi.Error -> EstadoVacio(
                    titulo = tituloDeError(e.mensaje), texto = e.mensaje, icono = Icons.Filled.FavoriteBorder,
                    textoBoton = "Reintentar", onBoton = catalogo::recargar
                )
                is EstadoApi.Exito -> {
                    val items = favoritosIds.mapNotNull { id -> e.datos.firstOrNull { it.id == id } }
                    if (items.isEmpty()) {
                        EstadoVacio(
                            titulo = "Aún no guardas a nadie",
                            texto = "Toca el corazón en un servicio para encontrarlo rápido más tarde.",
                            icono = Icons.Filled.FavoriteBorder,
                            textoBoton = "Buscar servicios",
                            onBoton = onBuscar
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = Espacios.l, end = Espacios.l, top = Espacios.s, bottom = Espacios.xxl),
                            verticalArrangement = Arrangement.spacedBy(Espacios.m)
                        ) {
                            items(items.chunked(2), key = { par -> par.joinToString("-") { it.id } }) { par ->
                                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Espacios.m)) {
                                    par.forEach { item ->
                                        TarjetaServicio(
                                            item, invitado = false, cargarPortada = catalogo::portada,
                                            onClick = { onServicio(item.id) },
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            esFavorito = true,
                                            onFavorito = { actividad.alternarFavorito(item.id, true) }
                                        )
                                    }
                                    if (par.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (contactos.isEmpty()) {
                EstadoVacio(
                    titulo = "Todavía no contactaste a nadie",
                    texto = "Cuando escribas por WhatsApp a un prestador, aparecerá aquí.",
                    icono = Icons.AutoMirrored.Filled.Chat
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = Espacios.l, end = Espacios.l, top = Espacios.s, bottom = Espacios.xxl),
                    verticalArrangement = Arrangement.spacedBy(Espacios.m)
                ) {
                    item {
                        Text(
                            "Los últimos prestadores que contactaste desde Aruy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(contactos, key = { it.id }) { contacto ->
                        AruyTarjeta(tipo = TipoTarjeta.NORMAL, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(contacto.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(TiempoRelativo.texto(contacto.fecha), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("«${contacto.mensaje}»", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                            contacto.servicioId?.let { id ->
                                BotonContorno("Ver servicio", { onServicio(id) })
                            }
                        }
                    }
                    item { BotonPeligro("Borrar historial de contactos", { confirmarBorrado = true }, soloTexto = true) }
                }
            }
        }
    }

    if (confirmarBorrado) {
        DialogoConfirmacion(
            titulo = "¿Borrar el historial?",
            texto = "Se borra la lista de prestadores que contactaste desde este teléfono. Tus opiniones no cambian.",
            textoConfirmar = "Borrar historial",
            peligroso = true,
            onConfirmar = { confirmarBorrado = false; actividad.borrarHistorialDeContactos() },
            onCancelar = { confirmarBorrado = false }
        )
    }
}
