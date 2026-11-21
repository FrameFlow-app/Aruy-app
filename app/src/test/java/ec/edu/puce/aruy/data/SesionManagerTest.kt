package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.sesion.AlmacenSesion
import ec.edu.puce.aruy.data.sesion.Jwt
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SesionManagerTest {

    private val ahora = 1_800_000_000_000L // ms
    private val tokenVigente = tokenDePrueba(ahora / 1000 + 3600)
    private val tokenVencido = tokenDePrueba(ahora / 1000 - 60)

    private fun sesion(token: String) = Sesion(
        token = token,
        usuario = Usuario(id = "u1", nombre = "Camila Andrade", telefono = "0998765432", rol = RolUsuario.PRESTADOR),
        perfilPrestadorId = "p1"
    )

    private fun manager(almacen: AlmacenSesion) =
        SesionManager(almacen, CoroutineScope(Dispatchers.Unconfined), ahoraMs = { ahora })

    @Test
    fun jwt_lee_el_vencimiento_y_detecta_tokens_vencidos() {
        assertEquals(ahora / 1000 + 3600, Jwt.vencimientoSegundos(tokenVigente))
        assertFalse(Jwt.estaVencido(tokenVigente, ahora))
        assertTrue(Jwt.estaVencido(tokenVencido, ahora))
        // un token ilegible NO se da por vencido: lo decide el servidor (así un error de lectura no saca a nadie de su cuenta)
        assertFalse(Jwt.estaVencido("no-es-un-jwt", ahora))
    }

    @Test
    fun el_vencimiento_se_muestra_con_dia_y_hora() {
        val texto = Jwt.vencimientoLegible(tokenVigente)
        assertTrue(texto.orEmpty(), Regex("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}").matches(texto.orEmpty()))
        assertNull(Jwt.vencimientoLegible("no-es-un-jwt"))
    }

    @Test
    fun un_token_ilegible_guardado_no_cierra_la_sesion() = runBlocking {
        val m = manager(AlmacenEnMemoria(sesion("no-es-un-jwt")))
        m.cargar()
        assertTrue(m.estado.value is EstadoSesion.Autenticada)
        assertNull(m.aviso.value)
    }

    @Test
    fun si_la_sesion_guardada_no_se_puede_abrir_se_dice_el_motivo() = runBlocking {
        val m = manager(AlmacenIlegible("la clave de seguridad del teléfono no pudo abrir tu sesión guardada"))
        m.cargar()
        assertEquals(EstadoSesion.SinSesion, m.estado.value)
        assertEquals(
            "No pudimos recuperar tu sesión: la clave de seguridad del teléfono no pudo abrir tu sesión guardada. Inicia sesión de nuevo.",
            m.aviso.value
        )
    }

    @Test
    fun si_leer_el_almacenamiento_falla_tambien_se_dice() = runBlocking {
        val m = manager(AlmacenQueFalla())
        m.cargar()
        assertEquals(EstadoSesion.SinSesion, m.estado.value)
        assertTrue(m.aviso.value.orEmpty().startsWith("No pudimos recuperar tu sesión: no se pudo leer el almacenamiento"))
    }

    @Test
    fun sin_nada_guardado_no_hay_aviso() = runBlocking {
        val m = manager(AlmacenEnMemoria())
        m.cargar()
        assertNull(m.aviso.value)
    }

    @Test
    fun sin_sesion_guardada_queda_sin_sesion() = runBlocking {
        val m = manager(AlmacenEnMemoria())
        assertEquals(EstadoSesion.Cargando, m.estado.value)
        m.cargar()
        assertEquals(EstadoSesion.SinSesion, m.estado.value)
        assertNull(m.tokenActual())
    }

    @Test
    fun sesion_guardada_vigente_se_restaura() = runBlocking {
        val m = manager(AlmacenEnMemoria(sesion(tokenVigente)))
        m.cargar()
        assertTrue(m.estado.value is EstadoSesion.Autenticada)
        assertEquals(tokenVigente, m.tokenActual())
    }

    @Test
    fun sesion_guardada_vencida_se_descarta_y_avisa() = runBlocking {
        val almacen = AlmacenEnMemoria(sesion(tokenVencido))
        val m = manager(almacen)
        m.cargar()
        assertEquals(EstadoSesion.SinSesion, m.estado.value)
        assertNull(almacen.guardada)
        // dice CUÁNDO venció, para que se pueda saber si fue por fecha
        assertTrue(m.aviso.value.orEmpty(), m.aviso.value.orEmpty().startsWith("Tu sesión venció el "))
    }

    @Test
    fun cerrar_sesion_borra_el_token() = runBlocking {
        val almacen = AlmacenEnMemoria()
        val m = manager(almacen)
        m.iniciar(sesion(tokenVigente))
        assertEquals(tokenVigente, m.tokenActual())
        m.cerrar()
        assertEquals(EstadoSesion.SinSesion, m.estado.value)
        assertNull(m.tokenActual())
        assertNull(almacen.guardada)
    }

    @Test
    fun expirar_cierra_la_sesion_y_deja_un_aviso() = runBlocking {
        val almacen = AlmacenEnMemoria()
        val m = manager(almacen)
        m.iniciar(sesion(tokenVigente))
        m.expirar()
        assertEquals(EstadoSesion.SinSesion, m.estado.value)
        assertEquals(SesionManager.MENSAJE_RECHAZADA, m.aviso.value)
        assertNull(almacen.guardada)
        m.avisoMostrado()
        assertNull(m.aviso.value)
    }

    @Test
    fun invitado_no_tiene_token() = runBlocking {
        val m = manager(AlmacenEnMemoria())
        m.entrarComoInvitado()
        assertEquals(EstadoSesion.Invitado, m.estado.value)
        assertNull(m.tokenActual())
    }
}
