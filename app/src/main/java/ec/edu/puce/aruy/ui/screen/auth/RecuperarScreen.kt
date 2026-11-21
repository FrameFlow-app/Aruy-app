package ec.edu.puce.aruy.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.domain.validation.ValidacionesRecuperar
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.viewmodel.PasoRecuperar
import ec.edu.puce.aruy.ui.viewmodel.RecuperarViewModel

/**
 * "Olvidé mi contraseña" en dos pasos: el correo y, cuando llega el código de 6 dígitos,
 * el código con la contraseña nueva. Al terminar vuelve al inicio de sesión ([onListo]).
 */
@Composable
fun RecuperarScreen(
    viewModel: RecuperarViewModel,
    onVolver: () -> Unit,
    onListo: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var correo by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmar by rememberSaveable { mutableStateOf("") }
    var verPassword by rememberSaveable { mutableStateOf(false) }
    var errorCorreo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCodigo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorPassword by rememberSaveable { mutableStateOf<String?>(null) }
    var errorConfirmar by rememberSaveable { mutableStateOf<String?>(null) }

    fun pedir() {
        errorCorreo = Validaciones.correo(correo)
        if (errorCorreo == null) viewModel.pedirCodigo(correo)
    }

    fun restablecer() {
        errorCodigo = ValidacionesRecuperar.codigo(codigo)
        errorPassword = Validaciones.password(password)
        errorConfirmar = ValidacionesRecuperar.confirmacion(password, confirmar)
        if (errorCodigo == null && errorPassword == null && errorConfirmar == null) {
            viewModel.restablecer(codigo, password, confirmar, onListo)
        }
    }

    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Recuperar contraseña", onVolver)
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (ui.paso) {
                PasoRecuperar.CORREO -> {
                    Text("¿Olvidaste tu contraseña?", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Escribe el correo de tu cuenta y te enviamos un código de 6 dígitos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AruyCampo(
                        valor = correo,
                        onValor = { correo = it; errorCorreo = null },
                        etiqueta = "Correo",
                        placeholder = "nombre@correo.com",
                        error = errorCorreo,
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        acciones = KeyboardActions(onDone = { pedir() }),
                        habilitado = !ui.cargando
                    )
                    MensajeError(ui.error)
                    BotonRelleno(
                        texto = if (ui.cargando) "Enviando…" else "Enviar código",
                        onClick = ::pedir,
                        grande = true,
                        ancho = true,
                        cargando = ui.cargando
                    )
                }

                PasoRecuperar.CODIGO -> {
                    Text("Revisa tu correo", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        ui.info ?: "Si ${ui.correo} tiene una cuenta, te enviamos un código de 6 dígitos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Escribe el código y elige tu contraseña nueva. Puede tardar un minuto en llegar; revisa también el spam.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    AruyCampo(
                        valor = codigo,
                        onValor = { codigo = it.filter(Char::isDigit).take(ValidacionesRecuperar.LARGO_CODIGO); errorCodigo = null },
                        etiqueta = "Código de ${ValidacionesRecuperar.LARGO_CODIGO} dígitos",
                        placeholder = "123456",
                        error = errorCodigo,
                        teclado = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                        habilitado = !ui.cargando
                    )
                    AruyCampo(
                        valor = password,
                        onValor = { password = it; errorPassword = null },
                        etiqueta = "Contraseña nueva",
                        ayuda = "Mínimo ${Validaciones.PASSWORD_MIN} caracteres, con letras y números.",
                        error = errorPassword,
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        transformacion = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        iconoFinal = {
                            IconButton(onClick = { verPassword = !verPassword }) {
                                Icon(
                                    if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        habilitado = !ui.cargando
                    )
                    AruyCampo(
                        valor = confirmar,
                        onValor = { confirmar = it; errorConfirmar = null },
                        etiqueta = "Repite la contraseña",
                        error = errorConfirmar,
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        acciones = KeyboardActions(onDone = { restablecer() }),
                        transformacion = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        habilitado = !ui.cargando
                    )
                    MensajeError(ui.error)
                    BotonRelleno(
                        texto = if (ui.cargando) "Guardando…" else "Cambiar contraseña",
                        onClick = ::restablecer,
                        grande = true,
                        ancho = true,
                        cargando = ui.cargando
                    )
                    BotonTexto(
                        texto = if (ui.esperaReenvio > 0) "Reenviar código en ${ui.esperaReenvio} s" else "Reenviar código",
                        onClick = { viewModel.pedirCodigo(ui.correo) },
                        habilitado = ui.esperaReenvio == 0 && !ui.cargando,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BotonTexto(
                        texto = "Usar otro correo",
                        onClick = {
                            codigo = ""; password = ""; confirmar = ""
                            viewModel.cambiarCorreo()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** El mensaje del servidor tal cual (código vencido, incorrecto...), anunciado a quien usa lector de pantalla. */
@Composable
private fun MensajeError(mensaje: String?) {
    mensaje?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
        )
    }
}
