package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.ui.Alignment
import ec.edu.puce.aruy.ui.components.BotonContorno
import androidx.compose.material3.BadgedBox
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.BotonTexto
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import ec.edu.puce.aruy.ui.components.AccionesCuenta
import ec.edu.puce.aruy.ui.components.EncabezadoSaludo
import ec.edu.puce.aruy.ui.components.MenuCuenta
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
    fotoUrl: String? = null,
    puedeOfrecer: Boolean = false,
    onOfrecer: () -> Unit = {},
    solicitudesNuevas: Int = 0,
    onSolicitudes: () -> Unit = {},
    cuenta: AccionesCuenta? = null
) {
    var menuAbierto by rememberSaveable { mutableStateOf(false) }
    val estado by catalogo.catalogo.collectAsStateWithLifecycle()
    val vistosIds by actividad.vistosIds.collectAsStateWithLifecycle()
    val favoritosIds by actividad.favoritosIds.collectAsStateWithLifecycle()

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
                onAvatar = cuenta?.let { { menuAbierto = true } },
                subtitulo = "¿Qué servicio necesitas hoy?",
                accion = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacios.s), verticalAlignment = Alignment.CenterVertically) {
                        if (!invitado) {
                            // La campanita: lleva a Mis solicitudes y muestra cuántas cosas nuevas hay
                            BadgedBox(badge = { if (solicitudesNuevas > 0) Pastilla("$solicitudesNuevas", tipo = TipoPastilla.ERROR) }) {
                                BotonCircular(
                                    icono = Icons.Filled.Notifications,
                                    descripcion = if (solicitudesNuevas > 0) "Mis solicitudes: $solicitudesNuevas sin ver" else "Mis solicitudes",
                                    onClick = onSolicitudes,
                                    variante = VarianteCircular.TONAL
                                )
                            }
                        }
                        BotonCircular(
                            icono = Icons.Filled.Refresh,
                            descripcion = "Actualizar",
                            onClick = catalogo::recargar,
                            variante = VarianteCircular.TONAL
                        )
                    }
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

        if (puedeOfrecer) {
            item {
                AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
                    Text("¿Haces algún oficio?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ofrécelo en Aruy con tu misma cuenta y consigue clientes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    BotonContorno("Ofrecer mis servicios", onOfrecer, modifier = Modifier.padding(top = Espacios.s))
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

    if (menuAbierto && cuenta != null) {
        MenuCuenta(nombreCompleto = nombre.orEmpty().trim(), fotoUrl = fotoUrl, acciones = cuenta, onCerrar = { menuAbierto = false })
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
                    BotonTexto("Ver todos", onClick = { onVerTodos(null) })
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
            BotonTexto("Ver todos", onClick = { onVerTodos(OrdenCatalogo.MEJOR_CALIFICACION) })
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
