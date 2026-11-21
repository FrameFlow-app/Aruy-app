package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ec.edu.puce.aruy.ui.theme.Espacios

/** Destinos del menú de la cuenta; los arma AruyApp con el mismo helper de navegación de las demás pantallas. */
class AccionesCuenta(
    val esInvitado: Boolean,
    val esPrestador: Boolean,
    val onSolicitudes: () -> Unit,
    val onEditarPerfil: () -> Unit,
    val onPro: () -> Unit,
    val onAjustes: () -> Unit,
    val onPermisos: () -> Unit,
    val onCerrarSesion: () -> Unit,
    val onIniciarSesion: () -> Unit,
    val onCrearCuenta: () -> Unit
)

/** Hoja "Tu cuenta": foto, nombre, rol y atajos a las pantallas de la cuenta. */
@Composable
fun MenuCuenta(
    nombreCompleto: String,
    fotoUrl: String?,
    acciones: AccionesCuenta,
    onCerrar: () -> Unit
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    // Se cierra la hoja y luego se navega: así no queda abierta al volver
    val ir: (() -> Unit) -> () -> Unit = { destino -> { onCerrar(); destino() } }

    AruyHoja(titulo = "Tu cuenta", onCerrar = onCerrar) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacios.s)
        ) {
            val nombre = if (acciones.esInvitado) "Invitado" else nombreCompleto
            AruyAvatar(nombre, tamano = 88.dp, fotoUrl = fotoUrl)
            Text(
                nombre,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            if (acciones.esInvitado) {
                Text(
                    "Crea una cuenta para ver precios y contactar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Pastilla(if (acciones.esPrestador) "Prestador" else "Contratante", tipo = TipoPastilla.INFO)
            }
        }
        Column(Modifier.padding(bottom = Espacios.s)) {
            if (acciones.esInvitado) {
                FilaAjuste(Icons.AutoMirrored.Filled.Login, "Iniciar sesión", ir(acciones.onIniciarSesion))
                FilaAjuste(Icons.Filled.PersonAdd, "Crear cuenta", ir(acciones.onCrearCuenta))
            } else {
                FilaAjuste(Icons.Filled.Inbox, "Mis solicitudes", ir(acciones.onSolicitudes))
                FilaAjuste(Icons.Filled.Edit, "Editar perfil", ir(acciones.onEditarPerfil))
                if (acciones.esPrestador) FilaAjuste(Icons.Filled.Star, "Aruy Pro", ir(acciones.onPro))
                FilaAjuste(Icons.Filled.Settings, "Ajustes", ir(acciones.onAjustes))
                FilaAjuste(Icons.Filled.Lock, "Permisos", ir(acciones.onPermisos))
                FilaAjuste(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", { confirmarSalida = true })
            }
        }
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            texto = "Tus favoritos y tu historial se quedan guardados en este teléfono.",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = { confirmarSalida = false; onCerrar(); acciones.onCerrarSesion() },
            onCancelar = { confirmarSalida = false }
        )
    }
}
