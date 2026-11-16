package ec.edu.puce.aruy.ui.screen.perfil

import ec.edu.puce.aruy.ui.components.rememberAccionesFoto
import ec.edu.puce.aruy.ui.components.HojaAgregarFoto
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.AruyAvatar
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.components.AvisoHost
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.domain.validation.ValidacionesPerfil
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.viewmodel.EditarPerfilViewModel

/** Editar nombre y celular de la cuenta y, si eres prestador, tu perfil público (`editarPerfil` en `perfil.js`). */
@Composable
fun EditarPerfilScreen(viewModel: EditarPerfilViewModel, onVolver: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var hojaFoto by rememberSaveable { mutableStateOf(false) }
    val acciones = rememberAccionesFoto { uri, desdeCamara -> viewModel.cambiarFoto(uri, desdeCamara) }

    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }
    LaunchedEffect(ui.finalizado) { if (ui.finalizado) onVolver() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior("Editar perfil", onVolver)
            if (ui.cargando) {
                EstadoCargando("Cargando tus datos…")
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AruyAvatar(ui.nombre.ifBlank { "Tú" }, tamano = 80.dp, fotoUrl = ui.fotoUrl)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BotonContorno(
                                texto = when {
                                    ui.subiendoFoto -> "Subiendo…"
                                    ui.fotoUrl == null -> "Agregar foto"
                                    else -> "Cambiar foto"
                                },
                                onClick = { hojaFoto = true },
                                icono = Icons.Filled.PhotoCamera,
                                habilitado = !ui.subiendoFoto
                            )
                            if (ui.fotoUrl != null) {
                                BotonTexto("Quitar foto", viewModel::quitarFoto, habilitado = !ui.subiendoFoto)
                            }
                        }
                    }
                    AruyCampo(
                        valor = ui.nombre, onValor = viewModel::cambiarNombre, etiqueta = "Nombre completo",
                        error = ui.errorNombre, maxCaracteres = Validaciones.NOMBRE_MAX,
                        teclado = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    AruyCampo(
                        valor = ui.celular, onValor = viewModel::cambiarCelular, etiqueta = "Celular",
                        placeholder = "0998765432", error = ui.errorCelular,
                        ayuda = "Las personas te contactan por este número en WhatsApp.",
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                    )
                    ui.email?.let {
                        AruyCampo(
                            valor = it, onValor = {}, etiqueta = "Correo",
                            ayuda = "El correo no se puede cambiar desde la app.", habilitado = false
                        )
                    }

                    if (ui.esPrestador) {
                        AruyCampo(
                            valor = ui.oficio, onValor = viewModel::cambiarOficio, etiqueta = "Oficio principal",
                            placeholder = "Ejemplo: Plomero", error = ui.errorOficio,
                            teclado = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        AruyCampo(
                            valor = ui.zona, onValor = viewModel::cambiarZona, etiqueta = "Dónde atiendes",
                            placeholder = "Ejemplo: Norte de Quito", error = ui.errorZona,
                            teclado = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        AruyCampo(
                            valor = ui.descripcion, onValor = viewModel::cambiarDescripcion, etiqueta = "Sobre ti (opcional)",
                            placeholder = "Cuéntales a los clientes cómo trabajas.",
                            ayuda = "${ui.descripcion.length} / ${ValidacionesPerfil.DESCRIPCION_MAX}",
                            unaLinea = false, lineasMin = 4, maxCaracteres = ValidacionesPerfil.DESCRIPCION_MAX
                        )
                        AruyCampo(
                            valor = ui.anios, onValor = viewModel::cambiarAnios, etiqueta = "Años de experiencia (opcional)",
                            error = ui.errorAnios,
                            teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                    }

                    BotonRelleno(
                        texto = if (ui.guardando) "Guardando…" else "Guardar cambios",
                        onClick = viewModel::guardar, grande = true, ancho = true, cargando = ui.guardando
                    )
                }
            }
        }
        AvisoHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (hojaFoto) {
        HojaAgregarFoto(
            acciones = acciones,
            onCerrar = { hojaFoto = false },
            titulo = "Foto de perfil",
            texto = "Es la foto que verán las personas en tu perfil. Aruy la reduce y le quita la ubicación antes de subirla."
        )
    }
}
