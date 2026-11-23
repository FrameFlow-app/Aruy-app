package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.data.repository.ActividadLocalRepository
import ec.edu.puce.aruy.domain.repository.CatalogoRepository
import ec.edu.puce.aruy.domain.usecase.ObtenerCatalogoUseCase
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.catalogo.FiltroCatalogo
import ec.edu.puce.aruy.domain.catalogo.FiltrosCatalogo
import ec.edu.puce.aruy.domain.catalogo.OrdenCatalogo
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.PrestadorPublico
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Id del usuario con sesión, o null si es invitado o no hay sesión. */
internal fun SesionManager.usuarioIdFlow() =
    estado.map { (it as? EstadoSesion.Autenticada)?.sesion?.usuario?.id }.distinctUntilChanged()

/**
 * Catálogo compartido por Inicio, Buscar, Detalle y Guardados: se descarga una vez y se
 * reutiliza. Oculta los servicios de prestadores que la persona bloqueó.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogoViewModel(
    private val repositorio: CatalogoRepository,
    private val obtenerCatalogo: ObtenerCatalogoUseCase,
    local: ActividadLocalRepository,
    sesion: SesionManager
) : ViewModel() {

    private val _catalogo = MutableStateFlow<EstadoApi<List<ServicioConPrestador>>>(EstadoApi.Inactivo)

    private val bloqueados: StateFlow<Set<String>> = sesion.usuarioIdFlow()
        .flatMapLatest { id ->
            if (id == null) flowOf(emptySet()) else local.bloqueados(id).map { lista -> lista.map { it.prestadorId }.toSet() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Catálogo sin los prestadores bloqueados. */
    val catalogo: StateFlow<EstadoApi<List<ServicioConPrestador>>> =
        combine(_catalogo, bloqueados) { estado, ocultos ->
            if (estado is EstadoApi.Exito) {
                EstadoApi.Exito(estado.datos.filter { it.servicio.prestadorId !in ocultos })
            } else {
                estado
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApi.Inactivo)

    fun cargarSiHaceFalta() {
        if (_catalogo.value is EstadoApi.Inactivo) recargar()
    }

    /** Si la carga falló por falta de internet, vuelve a intentar sola cuando regresa la conexión. */
    fun recargarSiFallo() {
        if (_catalogo.value is EstadoApi.Error) recargar()
    }

    fun recargar() {
        if (_catalogo.value is EstadoApi.Cargando) return
        _catalogo.value = EstadoApi.Cargando
        viewModelScope.launch {
            try {
                _catalogo.value = EstadoApi.Exito(obtenerCatalogo())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _catalogo.value = EstadoApi.Error(ApiErrorHandler.obtenerMensaje(e))
            }
        }
    }

    fun servicioPorId(id: String): ServicioConPrestador? =
        (_catalogo.value as? EstadoApi.Exito)?.datos?.firstOrNull { it.id == id }

    /** Datos públicos del prestador ya descargados con el catálogo (`perfilId` = `servicios.prestador_id`). */
    fun prestadorPorId(perfilId: String): PrestadorPublico? =
        (_catalogo.value as? EstadoApi.Exito)?.datos
            ?.firstOrNull { it.servicio.prestadorId == perfilId && it.prestador != null }?.prestador

    /** Primera foto del servicio, para la portada de las tarjetas. null si no tiene o no se pudo leer. */
    suspend fun portada(servicioId: String): String? = try {
        repositorio.fotos(servicioId).firstOrNull()?.url
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

/**
 * Filtros de Buscar. Viven en un ViewModel de la actividad para que se conserven al cambiar
 * de pestaña. Inicio manda la categoría elegida con [aplicarCategoria].
 */
class BuscarViewModel(
    /** Guarda los filtros (texto, números y booleanos) para que sobrevivan a la muerte del proceso. */
    private val estado: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _filtros = MutableStateFlow(leerFiltros())
    val filtros: StateFlow<FiltrosCatalogo> = _filtros.asStateFlow()

    init {
        viewModelScope.launch { _filtros.collect { guardarFiltros(it) } }
    }

    private fun leerFiltros() = FiltrosCatalogo(
        texto = estado.get<String>(K_TEXTO).orEmpty(),
        categoria = estado.get<String>(K_CATEGORIA)?.let { n -> CategoriaServicio.entries.firstOrNull { it.name == n } },
        zona = estado.get<String>(K_ZONA),
        calificacionMinima = estado.get<Double>(K_CALIFICACION) ?: 0.0,
        soloDisponibles = estado.get<Boolean>(K_DISPONIBLES) ?: false,
        soloVerificados = estado.get<Boolean>(K_VERIFICADOS) ?: false,
        precioMaximo = estado.get<Double>(K_PRECIO),
        orden = estado.get<String>(K_ORDEN)?.let { n -> OrdenCatalogo.entries.firstOrNull { it.name == n } }
            ?: OrdenCatalogo.MEJOR_CALIFICACION
    )

    private fun guardarFiltros(f: FiltrosCatalogo) {
        estado[K_TEXTO] = f.texto
        estado[K_CATEGORIA] = f.categoria?.name
        estado[K_ZONA] = f.zona
        estado[K_CALIFICACION] = f.calificacionMinima
        estado[K_DISPONIBLES] = f.soloDisponibles
        estado[K_VERIFICADOS] = f.soloVerificados
        estado[K_PRECIO] = f.precioMaximo
        estado[K_ORDEN] = f.orden.name
    }

    fun actualizar(cambio: (FiltrosCatalogo) -> FiltrosCatalogo) {
        _filtros.value = cambio(_filtros.value)
    }

    fun aplicarCategoria(categoria: CategoriaServicio?) {
        _filtros.value = FiltrosCatalogo(categoria = categoria)
    }

    fun limpiar() {
        _filtros.value = FiltrosCatalogo(texto = _filtros.value.texto)
    }

    fun limpiarTodo() {
        _filtros.value = FiltrosCatalogo()
    }

    fun resultados(items: List<ServicioConPrestador>): List<ServicioConPrestador> =
        FiltroCatalogo.aplicar(items, _filtros.value)

    private companion object {
        const val K_TEXTO = "buscar_texto"
        const val K_CATEGORIA = "buscar_categoria"
        const val K_ZONA = "buscar_zona"
        const val K_CALIFICACION = "buscar_calificacion"
        const val K_DISPONIBLES = "buscar_disponibles"
        const val K_VERIFICADOS = "buscar_verificados"
        const val K_PRECIO = "buscar_precio"
        const val K_ORDEN = "buscar_orden"
    }
}
