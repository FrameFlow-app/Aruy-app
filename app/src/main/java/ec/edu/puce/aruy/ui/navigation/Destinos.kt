package ec.edu.puce.aruy.ui.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

/**
 * Destinos de la app con navegación tipada: cada pantalla es una clase `@Serializable`.
 * Si un argumento tiene el tipo equivocado, falla al compilar y no al ejecutar la app.
 * Las pantallas sin argumentos son `data object`; las que reciben datos son `data class`.
 */
sealed interface Destino {
    @Serializable data object Splash : Destino
    @Serializable data object Bienvenida : Destino
    @Serializable data object Login : Destino
    @Serializable data object Registro : Destino
    @Serializable data object Recuperar : Destino

    @Serializable data object Inicio : Destino
    @Serializable data object Guardados : Destino
    @Serializable data object Perfil : Destino

    @Serializable data object Panel : Destino
    @Serializable data object MisServicios : Destino
    @Serializable data object Portafolio : Destino

    @Serializable data object Bloqueados : Destino
    @Serializable data object Reportes : Destino
    @Serializable data object Normas : Destino
    @Serializable data object EditarPerfil : Destino
    @Serializable data object Ajustes : Destino
    @Serializable data object Permisos : Destino
    @Serializable data object Privacidad : Destino
    @Serializable data object Baja : Destino
    @Serializable data object Pro : Destino
    @Serializable data object Ofrecer : Destino
    @Serializable data object Solicitudes : Destino

    /** [cat] es el nombre de una `CategoriaServicio` (o null); [foco] pide enfocar el campo de búsqueda. */
    @Serializable data class Buscar(val cat: String? = null, val foco: Boolean = false) : Destino

    @Serializable data class Servicio(val id: String) : Destino

    @Serializable data class PrestadorPublico(val id: String) : Destino

    @Serializable data class Chat(val solicitudId: String) : Destino

    /** Sin [id] abre el formulario de un servicio nuevo. */
    @Serializable data class Editor(val id: String? = null) : Destino
}

/** Primera pantalla de cada rol, como destino tipado (equivale a [rutaInicial]). */
fun destinoInicial(rol: RolNavegacion): Destino =
    if (rol == RolNavegacion.PRESTADOR) Destino.Panel else Destino.Inicio

/**
 * Traduce una clave de [Rutas] a su destino tipado. Sirve para la barra inferior y para las notificaciones,
 * que guardan la pantalla a abrir como texto dentro de un Intent. Devuelve null si la clave tiene argumentos
 * o no existe.
 */
fun destinoDeClave(clave: String?): Destino? = when (clave) {
    Rutas.SPLASH -> Destino.Splash
    Rutas.BIENVENIDA -> Destino.Bienvenida
    Rutas.LOGIN -> Destino.Login
    Rutas.REGISTRO -> Destino.Registro
    Rutas.RECUPERAR -> Destino.Recuperar
    Rutas.INICIO -> Destino.Inicio
    Rutas.BUSCAR -> Destino.Buscar()
    Rutas.GUARDADOS -> Destino.Guardados
    Rutas.PERFIL -> Destino.Perfil
    Rutas.PANEL -> Destino.Panel
    Rutas.MIS_SERVICIOS -> Destino.MisServicios
    Rutas.PORTAFOLIO -> Destino.Portafolio
    Rutas.BLOQUEADOS -> Destino.Bloqueados
    Rutas.REPORTES -> Destino.Reportes
    Rutas.NORMAS -> Destino.Normas
    Rutas.EDITAR_PERFIL -> Destino.EditarPerfil
    Rutas.AJUSTES -> Destino.Ajustes
    Rutas.PERMISOS -> Destino.Permisos
    Rutas.PRIVACIDAD -> Destino.Privacidad
    Rutas.BAJA -> Destino.Baja
    Rutas.PRO -> Destino.Pro
    Rutas.OFRECER -> Destino.Ofrecer
    Rutas.SOLICITUDES -> Destino.Solicitudes
    else -> null
}

/** Clave de [Rutas] del destino que se está viendo (para la barra inferior y las guardas por rol). */
fun claveDe(destino: NavDestination?): String? = when {
    destino == null -> null
    destino.hasRoute<Destino.Splash>() -> Rutas.SPLASH
    destino.hasRoute<Destino.Bienvenida>() -> Rutas.BIENVENIDA
    destino.hasRoute<Destino.Login>() -> Rutas.LOGIN
    destino.hasRoute<Destino.Registro>() -> Rutas.REGISTRO
    destino.hasRoute<Destino.Recuperar>() -> Rutas.RECUPERAR
    destino.hasRoute<Destino.Inicio>() -> Rutas.INICIO
    destino.hasRoute<Destino.Buscar>() -> Rutas.BUSCAR
    destino.hasRoute<Destino.Guardados>() -> Rutas.GUARDADOS
    destino.hasRoute<Destino.Perfil>() -> Rutas.PERFIL
    destino.hasRoute<Destino.Panel>() -> Rutas.PANEL
    destino.hasRoute<Destino.MisServicios>() -> Rutas.MIS_SERVICIOS
    destino.hasRoute<Destino.Portafolio>() -> Rutas.PORTAFOLIO
    destino.hasRoute<Destino.Bloqueados>() -> Rutas.BLOQUEADOS
    destino.hasRoute<Destino.Reportes>() -> Rutas.REPORTES
    destino.hasRoute<Destino.Normas>() -> Rutas.NORMAS
    destino.hasRoute<Destino.EditarPerfil>() -> Rutas.EDITAR_PERFIL
    destino.hasRoute<Destino.Ajustes>() -> Rutas.AJUSTES
    destino.hasRoute<Destino.Permisos>() -> Rutas.PERMISOS
    destino.hasRoute<Destino.Privacidad>() -> Rutas.PRIVACIDAD
    destino.hasRoute<Destino.Baja>() -> Rutas.BAJA
    destino.hasRoute<Destino.Pro>() -> Rutas.PRO
    destino.hasRoute<Destino.Ofrecer>() -> Rutas.OFRECER
    destino.hasRoute<Destino.Solicitudes>() -> Rutas.SOLICITUDES
    destino.hasRoute<Destino.Servicio>() -> Rutas.SERVICIO
    destino.hasRoute<Destino.PrestadorPublico>() -> Rutas.PRESTADOR_PUBLICO
    destino.hasRoute<Destino.Chat>() -> Rutas.CHAT
    destino.hasRoute<Destino.Editor>() -> Rutas.EDITOR
    else -> null
}
