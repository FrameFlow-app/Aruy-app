package ec.edu.puce.aruy.ui.screen.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.BuildConfig
import ec.edu.puce.aruy.domain.model.EstadoVerificacion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.ui.components.AruyAvatar
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.FilaAjuste
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.theme.Espacios

/** Perfil de la persona con sesión: cuenta, privacidad y cierre de sesión (`perfil.js`). */
@Composable
fun PerfilScreen(
    usuario: Usuario,
    recibeSolicitudes: Boolean,
    onCambiarRecepcion: (Boolean) -> Unit,
    alAbrir: () -> Unit,
    onEditar: () -> Unit,
    onAjustes: () -> Unit,
    onPro: () -> Unit,
    onPermisos: () -> Unit,
    onPrivacidad: () -> Unit,
    onBloqueados: () -> Unit,
    onReportes: () -> Unit,
    onNormas: () -> Unit,
    onBaja: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    val prestador = usuario.rol == RolUsuario.PRESTADOR

    LaunchedEffect(Unit) { alAbrir() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Espacios.l, vertical = Espacios.s),
        verticalArrangement = Arrangement.spacedBy(Espacios.l)
    ) {
        AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Espacios.s)
            ) {
                AruyAvatar(usuario.nombre, tamano = 104.dp, fotoUrl = usuario.fotoPerfil)
                Text(
                    usuario.nombre,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                EtiquetasPerfil(prestador, usuario)
            }
        }

        if (prestador) {
            AruyTarjeta(
                modifier = Modifier.fillMaxWidth(),
                tipo = if (recibeSolicitudes) TipoTarjeta.ACENTO else TipoTarjeta.NORMAL
            ) {
                AruyInterruptor(
                    titulo = if (recibeSolicitudes) "Recibiendo solicitudes" else "No recibes solicitudes",
                    activado = recibeSolicitudes,
                    onCambio = onCambiarRecepcion,
                    descripcion = if (recibeSolicitudes) "Apareces como «Disponible ahora» en las búsquedas."
                    else "Los clientes verán que no estás disponible por ahora."
                )
            }
        }

        GrupoFilas("Cuenta") {
            FilaAjuste(Icons.Filled.Edit, "Editar perfil", onEditar, texto = if (prestador) "Nombre, celular, oficio y descripción" else "Nombre y celular")
            if (prestador) {
                SeparadorFilas()
                FilaAjuste(Icons.Filled.Star, "Aruy Pro", onPro, texto = "Suscripción simulada: sin cobro real")
            }
            SeparadorFilas()
            FilaAjuste(Icons.Filled.Settings, "Ajustes", onAjustes, texto = "Tema, tamaño de texto y animaciones")
        }

        GrupoFilas("Privacidad y seguridad") {
            FilaAjuste(Icons.Filled.Lock, "Permisos", onPermisos, texto = "Cámara y galería")
            SeparadorFilas()
            FilaAjuste(Icons.Filled.PrivacyTip, "Mis datos y privacidad", onPrivacidad)
            if (!prestador) {
                SeparadorFilas()
                FilaAjuste(Icons.Filled.Block, "Personas bloqueadas", onBloqueados)
            }
            SeparadorFilas()
            FilaAjuste(Icons.Filled.Flag, "Mis reportes", onReportes)
            SeparadorFilas()
            FilaAjuste(Icons.Filled.Gavel, "Normas de la comunidad", onNormas)
        }

        GrupoFilas("Sesión") {
            FilaAjuste(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", { confirmarSalida = true })
            SeparadorFilas()
            FilaAjuste(Icons.Filled.DeleteForever, "Eliminar mi cuenta", onBaja, texto = "Borra tu cuenta y tus datos", peligro = true)
        }

        Text(
            "Aruy ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = Espacios.s, bottom = Espacios.xxxl)
        )
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            texto = "Tus favoritos y tu historial se quedan guardados en este teléfono.",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = { confirmarSalida = false; onCerrarSesion() },
            onCancelar = { confirmarSalida = false }
        )
    }
}

@Composable
private fun EtiquetasPerfil(prestador: Boolean, usuario: Usuario) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espacios.s)) {
        Pastilla(if (prestador) "Prestador" else "Contratante", tipo = TipoPastilla.INFO)
        if (usuario.estadoVerificacion == EstadoVerificacion.VERIFICADA) Pastilla("Verificada", tipo = TipoPastilla.OK)
    }
}

/** Grupo de filas: título suave y una sola tarjeta redondeada con todas las filas dentro. */
@Composable
internal fun GrupoFilas(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacios.s)) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = Espacios.l)
                .semantics { heading() }
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(vertical = Espacios.xs), content = content)
        }
    }
}

/** Divisor con sangría para separar filas dentro de un grupo. */
@Composable
internal fun SeparadorFilas() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = Espacios.l),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
