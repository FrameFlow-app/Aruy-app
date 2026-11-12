package ec.edu.puce.aruy.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.BotonTonal
import ec.edu.puce.aruy.ui.components.MarcaAruy
import ec.edu.puce.aruy.ui.components.Tejido
import ec.edu.puce.aruy.ui.theme.Espacios

/** Bienvenida: crear cuenta, iniciar sesión o explorar sin cuenta (`auth.js`). */
@Composable
fun BienvenidaScreen(
    onCrearCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
    onExplorar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = Espacios.xxl)
            .padding(top = Espacios.l, bottom = Espacios.l)
    ) {
        // Zona superior: se desplaza si el contenido no cabe (fuente grande).
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Espacios.m)
        ) {
            Tejido(Modifier.fillMaxWidth())
            Spacer(Modifier.height(Espacios.l))
            Box(
                Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(Espacios.l)
            ) { MarcaAruy(tamano = 52.dp) }
            Text(
                "Aruy",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "Encuentra a quien necesitas, cerca de ti.",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Conecta a personas que ofrecen un servicio con quienes lo necesitan en Quito: " +
                    "plomería, electricidad, belleza y más. Ves su trabajo y los contactas directo por WhatsApp.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Acciones ancladas abajo.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Espacios.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacios.s)
        ) {
            Text(
                "¿Quieres crear una cuenta o ya tienes una?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BotonRelleno("Crear cuenta", onCrearCuenta, grande = true, ancho = true)
            BotonTonal("Iniciar sesión", onIniciarSesion, grande = true, ancho = true)
            BotonTexto("Explorar sin cuenta", onExplorar)
        }
    }
}
