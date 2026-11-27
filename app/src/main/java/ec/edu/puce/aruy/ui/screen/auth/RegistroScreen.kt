package ec.edu.puce.aruy.ui.screen.auth

import ec.edu.puce.aruy.ui.viewmodel.FotoElegida
import ec.edu.puce.aruy.ui.components.rememberAccionesFoto
import ec.edu.puce.aruy.ui.components.HojaAgregarFoto
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.AruyAvatar
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.model.DatosRegistro
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyChip
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel

/**
 * EXAMEN (Caso 1): codigo estatico de auditoria generado en la pantalla de origen (el formulario de registro).
 * Se envia como argumento tipado `auditHash` al destino del catalogo (Destino.Inicio).
 */
const val AUDIT_HASH_REGISTRO = "HASH-2026-RDA3"

private val ZONAS = listOf("Norte", "Centro", "Sur", "Valles")

/** Registro en 2 pasos (`auth.js`): 1) cómo usarás Aruy, 2) tus datos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroScreen(
    viewModel: AuthViewModel,
    onVolver: () -> Unit,
    onIniciarSesion: () -> Unit
) {
    val formulario by viewModel.formulario.collectAsStateWithLifecycle()

    var paso by rememberSaveable { mutableStateOf(1) }
    var rol by rememberSaveable { mutableStateOf(RolUsuario.CONTRATANTE) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var celular by rememberSaveable { mutableStateOf("") }
    var fechaNacimiento by rememberSaveable { mutableStateOf("") }
    var fotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var fotoDesdeCamara by rememberSaveable { mutableStateOf(false) }
    var hojaFoto by rememberSaveable { mutableStateOf(false) }
    val accionesFoto = rememberAccionesFoto { uri, desdeCamara -> fotoUri = uri; fotoDesdeCamara = desdeCamara }
    var password by rememberSaveable { mutableStateOf("") }
    var verPassword by rememberSaveable { mutableStateOf(false) }
    var oficio by rememberSaveable { mutableStateOf<CategoriaServicio?>(null) }
    var zona by rememberSaveable { mutableStateOf(ZONAS.first()) }
    var acepta by rememberSaveable { mutableStateOf(false) }
    // EXAMEN (Caso 2): State Hoisting. El estado vive en el padre (stateful) y sobrevive a recomposiciones y
    // rotacion porque usa rememberSaveable (RDA-1 Tema 4: "Inputs de texto" -> rememberSaveable).
    var isFormEditable by rememberSaveable { mutableStateOf(true) }
    // EXAMEN (Caso 5): texto del campo numerico y ultimo valor ya procesado de forma segura
    var anios by rememberSaveable { mutableStateOf("") }
    var resultadoAnios by rememberSaveable { mutableStateOf<Int?>(null) }
    // EXAMEN (Parte 1): controla el cuadro que muestra la Política de Privacidad
    var verPolitica by rememberSaveable { mutableStateOf(false) }

    var errorNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCorreo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCelular by rememberSaveable { mutableStateOf<String?>(null) }
    var errorFecha by rememberSaveable { mutableStateOf<String?>(null) }
    var errorPassword by rememberSaveable { mutableStateOf<String?>(null) }
    var errorOficio by rememberSaveable { mutableStateOf<String?>(null) }
    var errorAcepta by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { viewModel.limpiarError() } }

    // Atrás en el paso 2 vuelve al paso 1, no sale del registro
    BackHandler(enabled = paso == 2) { paso = 1 }

    fun enviar() {
        // EXAMEN (Caso 5): Null Safety. toIntOrNull() devuelve null (no lanza excepcion) si el texto no es un
        // numero; el operador Elvis (?:) asigna 0 como valor seguro (RDA-1 Semana 2, "Manejo seguro de null").
        val aniosSeguros: Int = anios.trim().toIntOrNull() ?: 0
        resultadoAnios = aniosSeguros

        errorNombre = Validaciones.nombre(nombre)
        errorCorreo = Validaciones.correo(correo)
        errorCelular = Validaciones.celular(celular)
        val nacimiento = Validaciones.fechaNacimiento(fechaNacimiento)
        errorFecha = nacimiento.error
        errorPassword = Validaciones.password(password)
        errorOficio = if (rol == RolUsuario.PRESTADOR && oficio == null) "Elige qué servicio ofreces." else null
        errorAcepta = if (!acepta) "Para crear tu cuenta acepta la Política de Privacidad." else null
        val hayError = listOf(errorNombre, errorCorreo, errorCelular, errorFecha, errorPassword, errorOficio, errorAcepta)
            .any { it != null }
        if (!hayError) {
            viewModel.registrar(
                DatosRegistro(
                    nombre = nombre,
                    correo = correo,
                    celular = celular,
                    password = password,
                    rol = rol,
                    oficio = oficio?.etiqueta,
                    zona = zona,
                    fechaNacimiento = nacimiento.fecha
                ),
                fotoUri?.let { FotoElegida(it, fotoDesdeCamara) }
            ) {}
        }
    }

    if (hojaFoto) {
        HojaAgregarFoto(
            acciones = accionesFoto,
            onCerrar = { hojaFoto = false },
            titulo = "Foto de perfil",
            texto = "Es opcional. Las personas verán esta foto en tu perfil; puedes cambiarla cuando quieras. Aruy la reduce y le quita la ubicación."
        )
    }

    // EXAMEN (Caso 2): Scaffold padre; el Switch va en las acciones de la TopAppBar y cambia isFormEditable
    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = if (paso == 1) "Crear cuenta" else if (rol == RolUsuario.CONTRATANTE) "Crea tu cuenta" else "Crea tu perfil",
                onVolver = { if (paso == 2) paso = 1 else onVolver() },
                acciones = {
                    Switch(
                        checked = isFormEditable,
                        onCheckedChange = { isFormEditable = !isFormEditable },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .semantics { contentDescription = "Formulario editable" }
                    )
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { relleno ->
    Column(Modifier.fillMaxSize().padding(relleno)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Indicador(paso)

            if (paso == 1) {
                Text("¿Cómo usarás Aruy?", style = MaterialTheme.typography.headlineMedium)
                TarjetaRol(
                    icono = Icons.Filled.Search,
                    titulo = "Quiero contratar servicios",
                    texto = "Busca por categoría y zona, mira trabajos reales y escribe por WhatsApp.",
                    seleccionada = rol == RolUsuario.CONTRATANTE,
                    onClick = { rol = RolUsuario.CONTRATANTE }
                )
                TarjetaRol(
                    icono = Icons.Filled.Handyman,
                    titulo = "Quiero ofrecer mis servicios",
                    texto = "Publica lo que haces, sube fotos de tus trabajos y recibe clientes. Con la misma cuenta también puedes buscar y contratar.",
                    seleccionada = rol == RolUsuario.PRESTADOR,
                    onClick = { rol = RolUsuario.PRESTADOR }
                )
                BotonRelleno("Continuar", { paso = 2 }, grande = true, ancho = true)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                    BotonTexto("Iniciar sesión", onIniciarSesion)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (fotoUri != null) {
                        AsyncImage(
                            model = fotoUri,
                            contentDescription = "Tu foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                        )
                    } else {
                        AruyAvatar(nombre.ifBlank { "Tú" }, tamano = 72.dp)
                    }
                    Column {
                        BotonContorno(
                            texto = if (fotoUri == null) "Agregar foto (opcional)" else "Cambiar foto",
                            onClick = { hojaFoto = true },
                            icono = Icons.Filled.PhotoCamera
                        )
                        if (fotoUri != null) BotonTexto("Quitar foto", { fotoUri = null })
                    }
                }
                // EXAMEN (Caso 2): el padre (stateful) pasa isFormEditable al hijo (stateless) y recibe sus eventos
                FormularioRegistro(
                    isFormEditable = isFormEditable,
                    nombre = nombre,
                    onNombre = { nombre = it; errorNombre = null },
                    errorNombre = errorNombre,
                    correo = correo,
                    onCorreo = { correo = it; errorCorreo = null },
                    errorCorreo = errorCorreo ?: formulario.errorCorreo,
                    celular = celular,
                    onCelular = { celular = it; errorCelular = null },
                    errorCelular = errorCelular ?: formulario.errorCelular,
                    fechaNacimiento = fechaNacimiento,
                    onFecha = { fechaNacimiento = Validaciones.formatearFecha(it); errorFecha = null },
                    errorFecha = errorFecha ?: formulario.errorFechaNacimiento,
                    password = password,
                    onPassword = { password = it; errorPassword = null },
                    errorPassword = errorPassword,
                    verPassword = verPassword,
                    onVerPassword = { verPassword = !verPassword },
                    anios = anios,
                    onAnios = { anios = it },
                    resultadoAnios = resultadoAnios
                )

                if (rol == RolUsuario.PRESTADOR) {
                    Text("¿Qué servicio ofreces?", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoriaServicio.entries.forEach { categoria ->
                            AruyChip(
                                texto = categoria.etiqueta,
                                seleccionado = oficio == categoria,
                                onClick = { oficio = categoria; errorOficio = null }
                            )
                        }
                    }
                    errorOficio?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    Text("Tu zona en Quito", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZONAS.forEach { opcion ->
                            AruyChip(opcion, seleccionado = zona == opcion, onClick = { zona = opcion })
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = acepta, role = Role.Checkbox, onClick = { acepta = !acepta; errorAcepta = null }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = acepta, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    // EXAMEN (Parte 1): texto exacto pedido; la casilla (acepta) empieza SIN marcar (false)
                    Text(
                        "Leí y acepto la Política de Privacidad",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // EXAMEN (Parte 1): enlace que abre la política dentro de la app
                TextButton(onClick = { verPolitica = true }) { Text("Leer la Política de Privacidad") }
                if (verPolitica) {
                    AlertDialog(
                        onDismissRequest = { verPolitica = false },
                        confirmButton = { TextButton(onClick = { verPolitica = false }) { Text("Cerrar") } },
                        title = { Text("Política de Privacidad") },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Responsable: equipo FrameFlow, proyecto académico de la PUCE. Contacto: aruy.ec@gmail.com.")
                                Text("Datos que se guardan: nombre, correo, celular, fecha de nacimiento y contraseña (cifrada). Si ofreces servicios: oficio, descripción, zona y fotos de tus trabajos.")
                                Text("Para qué: crear tu cuenta, mostrar tu perfil y servicios, y permitir que te contacten por WhatsApp. No vendemos tus datos ni los usamos para publicidad.")
                                Text("Dónde: base de datos en Neon y fotos en Supabase. No usamos tu ubicación GPS.")
                                Text("Cuánto tiempo: mientras tu cuenta esté activa, hasta que la elimines.")
                                Text("Borrar tus datos: Perfil, Eliminar mi cuenta. Se borra al instante y para siempre.")
                                Text("Texto completo: docs/privacidad.html del repositorio del proyecto.")
                            }
                        }
                    )
                }
                errorAcepta?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                formulario.error?.let { mensaje ->
                    Text(
                        mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                    )
                }

                BotonRelleno(
                    texto = if (formulario.cargando) "Creando tu cuenta…" else "Crear cuenta",
                    onClick = ::enviar,
                    grande = true,
                    ancho = true,
                    cargando = formulario.cargando,
                    habilitado = isFormEditable // EXAMEN (Caso 2): el boton tambien obedece al estado elevado
                )
            }
        }
    }
    }
}

/**
 * EXAMEN (Caso 2): formulario STATELESS (sin estado propio). Solo recibe valores y devuelve eventos hacia arriba.
 * Concepto del curso: State Hoisting (RDA-2 Tema 7y8) y "la UI como funcion del estado" (RDA-3 Tema 9y10):
 * la interfaz solo proyecta lo que recibe; aqui `isFormEditable` decide si cada campo esta habilitado.
 */
@Composable
private fun FormularioRegistro(
    isFormEditable: Boolean,
    nombre: String,
    onNombre: (String) -> Unit,
    errorNombre: String?,
    correo: String,
    onCorreo: (String) -> Unit,
    errorCorreo: String?,
    celular: String,
    onCelular: (String) -> Unit,
    errorCelular: String?,
    fechaNacimiento: String,
    onFecha: (String) -> Unit,
    errorFecha: String?,
    password: String,
    onPassword: (String) -> Unit,
    errorPassword: String?,
    verPassword: Boolean,
    onVerPassword: () -> Unit,
    anios: String,
    onAnios: (String) -> Unit,
    resultadoAnios: Int?
) {
    AruyCampo(
        valor = nombre,
        onValor = onNombre,
        etiqueta = "Nombre completo",
        placeholder = "Ejemplo: Lucía Cevallos",
        error = errorNombre,
        maxCaracteres = Validaciones.NOMBRE_MAX,
        teclado = KeyboardOptions(imeAction = ImeAction.Next),
        habilitado = isFormEditable
    )
    AruyCampo(
        valor = correo,
        onValor = onCorreo,
        etiqueta = "Correo",
        placeholder = "nombre@correo.com",
        error = errorCorreo,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        habilitado = isFormEditable
    )
    AruyCampo(
        valor = celular,
        onValor = onCelular,
        etiqueta = "Celular",
        placeholder = "0998765432",
        ayuda = "Las personas te contactan por este número en WhatsApp.",
        error = errorCelular,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        habilitado = isFormEditable
    )
    AruyCampo(
        valor = fechaNacimiento,
        onValor = onFecha,
        etiqueta = "Fecha de nacimiento",
        placeholder = "25/03/2000",
        ayuda = "Aruy es solo para mayores de 18 años.",
        error = errorFecha,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        habilitado = isFormEditable
    )
    AruyCampo(
        valor = password,
        onValor = onPassword,
        etiqueta = "Contraseña",
        placeholder = "Mínimo 8 caracteres",
        error = errorPassword,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        transformacion = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
        iconoFinal = {
            IconButton(onClick = onVerPassword, enabled = isFormEditable) {
                Icon(
                    if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        habilitado = isFormEditable
    )
    MedidorPassword(password)
    // EXAMEN (Caso 5): campo numerico critico. Se usa teclado de texto A PROPOSITO para poder probar letras
    // ("abc"); la defensa real es toIntOrNull() + Elvis en enviar(), no el tipo de teclado.
    AruyCampo(
        valor = anios,
        onValor = onAnios,
        etiqueta = "Años de experiencia (opcional)",
        placeholder = "Ejemplo: 3",
        ayuda = "Escribe solo números. Si escribes letras o lo dejas vacío, se usa 0.",
        maxCaracteres = 3,
        teclado = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        habilitado = isFormEditable
    )
    resultadoAnios?.let {
        Text(
            "Valor procesado: $it",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** "Paso N de 2": dos segmentos en píldora de 8 dp. */
@Composable
private fun Indicador(paso: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Paso $paso de 2" },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(2) { indice ->
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (indice < paso) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
            )
        }
    }
}

@Composable
private fun TarjetaRol(icono: ImageVector, titulo: String, texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) esquema.primaryContainer else esquema.surfaceContainerLow
        ),
        border = BorderStroke(2.dp, if (seleccionada) esquema.primary else Color.Transparent)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (seleccionada) esquema.primary else esquema.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icono, null,
                    tint = if (seleccionada) esquema.onPrimary else esquema.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (seleccionada) esquema.onPrimaryContainer else esquema.onSurfaceVariant
                )
            }
            RadioButton(selected = seleccionada, onClick = null)
        }
    }
}

/** Medidor de 4 barras con el texto de [Validaciones.textoFuerzaPassword]. */
@Composable
private fun MedidorPassword(password: String) {
    val puntaje = Validaciones.fuerzaPassword(password)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { indice ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (password.isNotEmpty() && indice < puntaje) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                )
            }
        }
        Text(
            Validaciones.textoFuerzaPassword(if (password.isEmpty()) 0 else puntaje),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
