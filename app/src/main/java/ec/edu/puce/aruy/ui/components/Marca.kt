package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import ec.edu.puce.aruy.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.puce.aruy.ui.theme.Anil
import ec.edu.puce.aruy.ui.theme.FuenteTitulos
import ec.edu.puce.aruy.ui.theme.Maiz

/** Logotipo de Aruy: el mismo ícono de la app (fondo anil y "A" con barra de maíz), recortado en círculo. */
@Composable
fun MarcaAruy(modifier: Modifier = Modifier, tamano: Dp = 96.dp) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(Anil),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        // El primer plano del ícono deja margen de seguridad: se amplía para que se lea bien dentro del círculo
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Logotipo de Aruy",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.3f)
        )
    }
}

/** Barra superior con botón de volver, para pantallas fuera de la barra inferior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    onVolver: (() -> Unit)?,
    modifier: Modifier = Modifier,
    // EXAMEN (Caso 2): espacio para "acciones" de la TopAppBar (aqui va el Switch del formulario)
    acciones: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier.fillMaxWidth(),
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
