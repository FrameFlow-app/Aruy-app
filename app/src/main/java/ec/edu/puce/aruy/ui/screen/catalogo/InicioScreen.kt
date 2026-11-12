package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.FiltroCatalogo
import ec.edu.puce.aruy.domain.catalogo.OrdenCatalogo
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraBusqueda
import ec.edu.puce.aruy.ui.components.BotonCircular
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.EncabezadoSaludo
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.Esqueleto
import ec.edu.puce.aruy.ui.components.FilaServicio
import ec.edu.puce.aruy.ui.components.TarjetaServicio
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.TituloSeccion
import ec.edu.puce.aruy.ui.components.VarianteCircular
import ec.edu.puce.aruy.ui.components.icono
import ec.edu.puce.aruy.ui.components.tituloDeError
import ec.edu.puce.aruy.ui.theme.Espacios
import ec.edu.puce.aruy.ui.theme.FormaPildora
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel

/** Pantalla de inicio del contratante o del invitado (`home.js` del prototipo, sin distancia ni anuncios). */
@Composable
fun InicioScreen(
    catalogo: CatalogoViewModel,
    actividad: ActividadLocalViewModel,
    nombre: String?,
    invitado: Boolean,
    onBuscar: () -> Unit,
    onCategoria: (CategoriaServicio) -> Unit,
    onVerTodos: (OrdenCatalogo?) -> Unit,
    onServicio: (String) -> Unit,
    onCrearCuenta: () -> Unit,
    fotoUrl: String? = null
) {
    val estado by catalogo.catalogo.collectAsState()
    val vistosIds by actividad.vistosIds.collectAsState()
    val favoritosIds by actividad.favoritosIds.collectAsState()

    LaunchedEffect(Unit) { catalogo.cargarSiHaceFalta() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Espacios.l, end = Espacios.l, top = Espacios.s, bottom = Espacios.xxl),
        verticalArrangement = Arrangement.spacedBy(Espacios.xl)
    ) {
        item {
            EncabezadoSaludo(
                nombre = nombre?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() } ?: "invitado",
                fotoUrl = fotoUrl,
                subtitulo = "¿Qué servicio necesitas hoy?",
                accion = {
                    BotonCircular(
                        icono = Icons.Filled.Refresh,
                        descripcion = "Actualizar",
                        onClick = catalogo::recargar,
                        variante = VarianteCircular.TONAL
                    )
                }
            )
        }

        if (invitado) {
            item {
                AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                    Text("Estás explorando sin cuenta.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Crea una para ver precios, calificaciones y contactar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    BotonRelleno("Crear cuenta", onCrearCuenta, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        item {
            BarraBusqueda(
                placeholder = "Busca plomero, manicure, pintura…",
                onClick = onBuscar,
                onFiltrar = onBuscar
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Espacios.s)) {
                TituloSeccion("Categorías")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacios.s)) {
                    item { ChipCategoria("Todos", Icons.Filled.Apps, seleccionado = true) { } }
                    items(CategoriaServicio.entries) { categoria ->
                        ChipCategoria(categoria.etiqueta, categoria.icono, seleccionado = false) { onCategoria(categoria) }
                    }
                }
            }
        }

        when (val e = estado) {
            EstadoApi.Inactivo, EstadoApi.Cargando -> item { CargandoLista() }
            is EstadoApi.Error -> item {
                EstadoVacio(
                    titulo = tituloDeError(e.mensaje),
                    texto = e.mensaje,
                    icono = Icons.Filled.SearchOff,
                    textoBoton = "Reintentar",
                    onBoton = catalogo::recargar
                )
            }
            is EstadoApi.Exito -> {
                val servicios = e.datos
                if (servicios.isEmpty()) {
                    item {
                        EstadoVacio(
                            titulo = "Aún no hay servicios",
                            texto = "Cuando se publiquen, aparecerán aquí.",
                            icono = Icons.Filled.SearchOff
                        )
                    }
                } else {
                    contenido(servicios, vistosIds, favoritosIds, invitado, catalogo, actividad, onVerTodos, onServicio)
                }
            }
        }
    }
}

private fun LazyListScope.contenido(
    servicios: List<ServicioConPrestador>,
    vistosIds: List<String>,
    favoritosIds: List<String>,
    invitado: Boolean,
    catalogo: CatalogoViewModel,
    actividad: ActividadLocalViewModel,
    onVerTodos: (OrdenCatalogo?) -> Unit,
    onServicio: (String) -> Unit
) {
    val disponibles = servicios.filter(FiltroCatalogo::disponibleAhora).take(8)
    val mejores = FiltroCatalogo.ordenar(servicios, OrdenCatalogo.MEJOR_CALIFICACION).take(6)
    val vistos = vistosIds.mapNotNull { id -> servicios.firstOrNull { it.id == id } }

    if (disponibles.isNotEmpty()) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Espacios.s)) {
                TituloSeccion("Disponibles ahora", accion = {
                    TextButton(onClick = { onVerTodos(null) }) { Text("Ver todos") }
                })
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacios.m)) {
                    items(disponibles, key = { it.id }) { item ->
                        val esFavorito = item.id in favoritosIds
                        TarjetaServicio(
                            item, invitado, catalogo::portada, onClick = { onServicio(item.id) },
                            esFavorito = esFavorito,
                            onFavorito = if (invitado) null else ({ actividad.alternarFavorito(item.id, esFavorito) })
                        )
                    }
                }
            }
        }
    }

    item {
        TituloSeccion("Mejor valorados", accion = {
            TextButton(onClick = { onVerTodos(OrdenCatalogo.MEJOR_CALIFICACION) }) { Text("Ver todos") }
        })
    }
    items(mejores.chunked(2), key = { par -> "mejor-" + par.joinToString("-") { it.id } }) { par ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Espacios.m)) {
            par.forEach { item ->
                val esFavorito = item.id in favoritosIds
                TarjetaServicio(
                    item, invitado, catalogo::portada, onClick = { onServicio(item.id) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    esFavorito = esFavorito,
                    onFavorito = if (invitado) null else ({ actividad.alternarFavorito(item.id, esFavorito) })
                )
            }
            if (par.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    if (vistos.isNotEmpty() && !invitado) {
        item { TituloSeccion("Vistos recientemente") }
        items(vistos, key = { "visto-${it.id}" }) { item ->
            FilaServicio(item, invitado, catalogo::portada, onClick = { onServicio(item.id) })
        }
    }
}

/** Chip de categoría con icono; el activo se rellena con el color primario. */
@Composable
private fun ChipCategoria(texto: String, icono: ImageVector, seleccionado: Boolean, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = { Icon(icono, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
        modifier = Modifier.heightIn(min = 48.dp),
        shape = FormaPildora,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = esquema.surfaceContainerHigh,
            labelColor = esquema.onSurface,
            iconColor = esquema.onSurfaceVariant,
            selectedContainerColor = esquema.primary,
            selectedLabelColor = esquema.onPrimary,
            selectedLeadingIconColor = esquema.onPrimary
        ),
        border = null
    )
}

@Composable
private fun CargandoLista() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { Esqueleto(Modifier.fillMaxWidth(), alto = 96.dp) }
        Text(
            "Cargando servicios…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
