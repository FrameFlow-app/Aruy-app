package ec.edu.puce.aruy.ui.screen.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.theme.Espacios
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.DatosPrestador
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyChip
import ec.edu.puce.aruy.ui.components.AvisoHost
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.viewmodel.ActivarPrestadorViewModel

private val ZONAS = listOf("Norte", "Centro", "Sur", "Valles")

/**
 * "Quiero ofrecer servicios": quien ya tiene cuenta cuenta qué oficio hace y en qué zona trabaja.
 * Sigue siendo la misma cuenta, con el mismo correo y la misma contraseña.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivarPrestadorScreen(
    viewModel: ActivarPrestadorViewModel,
    onVolver: () -> Unit,
    onListo: () -> Unit
) {
    // se lee el flujo que se detiene cuando la app no se ve.
    // solo LEE el estado, jamas se modifica
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var oficio by rememberSaveable { mutableStateOf<CategoriaServicio?>(null) }
    var zona by rememberSaveable { mutableStateOf(ZONAS.first()) }
    var anios by rememberSaveable { mutableStateOf("") }
    var errorOficio by rememberSaveable { mutableStateOf<String?>(null) }
    var errorAnios by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }
    LaunchedEffect(state.isReady) { if (state.isReady) onListo() }

    fun enviar() {
        errorOficio = if (oficio == null) "Elige qué servicio ofreces." else null
        val experiencia = Validaciones.aniosExperiencia(anios)
        errorAnios = experiencia.error
        val elegido = oficio
        if (elegido != null && experiencia.error == null) {
            viewModel.activarPerfil(DatosPrestador(oficio = elegido.etiqueta, zona = zona, aniosExperiencia = experiencia.valor))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // EXAMEN (Caso 3, paso 3): el título sale del estado (state.titleText), ya no es un texto escrito a mano
            BarraSuperior(titulo = state.titleText, onVolver = onVolver)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Espacios.l, vertical = Espacios.s),
                verticalArrangement = Arrangement.spacedBy(Espacios.l)
            ) {
                Text(
                    "Con esta misma cuenta",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "No necesitas otro correo ni otra contraseña. Cuéntanos qué haces y empieza a publicar tus servicios.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("¿Qué servicio ofreces?", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacios.s), verticalArrangement = Arrangement.spacedBy(Espacios.s)) {
                    CategoriaServicio.entries.forEach { categoria ->
                        AruyChip(
                            categoria.etiqueta,
                            seleccionado = oficio == categoria,
                            onClick = { oficio = categoria; errorOficio = null }
                        )
                    }
                }
                errorOficio?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error) }

                Text("Tu zona en Quito", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacios.s), verticalArrangement = Arrangement.spacedBy(Espacios.s)) {
                    ZONAS.forEach { opcion ->
                        AruyChip(opcion, seleccionado = zona == opcion, onClick = { zona = opcion })
                    }
                }

                AruyCampo(
                    valor = anios,
                    onValor = { anios = it.filter(Char::isDigit).take(2); errorAnios = null },
                    etiqueta = "Años de experiencia (opcional)",
                    placeholder = "3",
                    error = errorAnios,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    acciones = KeyboardActions(onDone = { enviar() })
                )

                BotonRelleno(
                    texto = if (state.isProcessing) "Activando…" else "Activar mi perfil de prestador",
                    onClick = ::enviar,
                    grande = true,
                    ancho = true,
                    cargando = state.isProcessing
                )
            }
        }
        AvisoHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
