package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.model.ContraparteSolicitud
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.model.accionesPara
import ec.edu.puce.aruy.domain.solicitudes.PoliticaAvisos
import ec.edu.puce.aruy.domain.solicitudes.TextoAviso
import ec.edu.puce.aruy.domain.solicitudes.TiempoRelativo
import ec.edu.puce.aruy.domain.validation.ValidacionesSolicitud
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class SolicitudesDominioTest {

    private fun solicitud(estado: EstadoSolicitud) = Solicitud(
        id = "s1", servicioId = "serv1", servicioTitulo = "Arreglo de tuberías", estado = estado, mensaje = "Necesito ayuda",
        respuesta = null, fechaCreacion = null, fechaActualizacion = null, sinVer = false,
        contraparte = ContraparteSolicitud("Marcos", null, null)
    )

    // ---------------- qué puede hacer cada quien ----------------

    @Test
    fun el_prestador_responde_y_completa() {
        val r = TipoSolicitudes.RECIBIDAS
        assertEquals(listOf(EstadoSolicitud.ACEPTADA, EstadoSolicitud.RECHAZADA), solicitud(EstadoSolicitud.PENDIENTE).accionesPara(r))
        assertEquals(listOf(EstadoSolicitud.COMPLETADA, EstadoSolicitud.CANCELADA), solicitud(EstadoSolicitud.ACEPTADA).accionesPara(r))
        for (cerrada in listOf(EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA, EstadoSolicitud.COMPLETADA)) {
            assertTrue("$cerrada no se reabre", solicitud(cerrada).accionesPara(r).isEmpty())
        }
    }

    @Test
    fun quien_envio_la_solicitud_solo_puede_cancelarla() {
        val e = TipoSolicitudes.ENVIADAS
        assertEquals(listOf(EstadoSolicitud.CANCELADA), solicitud(EstadoSolicitud.PENDIENTE).accionesPara(e))
        assertEquals(listOf(EstadoSolicitud.CANCELADA), solicitud(EstadoSolicitud.ACEPTADA).accionesPara(e))
        for (cerrada in listOf(EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA, EstadoSolicitud.COMPLETADA)) {
            assertTrue(solicitud(cerrada).accionesPara(e).isEmpty())
        }
    }

    @Test
    fun un_estado_desconocido_del_servidor_no_rompe_la_app() {
        assertEquals(EstadoSolicitud.ACEPTADA, EstadoSolicitud.desde("ACEPTADA"))
        assertEquals(EstadoSolicitud.PENDIENTE, EstadoSolicitud.desde("ALGO_NUEVO"))
        assertEquals(EstadoSolicitud.PENDIENTE, EstadoSolicitud.desde(null))
    }

    // ---------------- textos ----------------

    @Test
    fun el_mensaje_de_una_solicitud_debe_tener_entre_5_y_500_letras() {
        assertNotNull(ValidacionesSolicitud.mensaje(""))
        assertNotNull(ValidacionesSolicitud.mensaje("   hola  "))
        assertNull(ValidacionesSolicitud.mensaje("Hola, necesito ayuda"))
        assertNull(ValidacionesSolicitud.mensaje("x".repeat(500)))
        assertNotNull(ValidacionesSolicitud.mensaje("x".repeat(501)))
    }

    @Test
    fun la_respuesta_puede_ser_vacia_pero_no_pasar_de_300() {
        assertNull(ValidacionesSolicitud.respuesta(""))
        assertNull(ValidacionesSolicitud.respuesta("x".repeat(300)))
        assertNotNull(ValidacionesSolicitud.respuesta("x".repeat(301)))
    }

    // ---------------- el numerito y los avisos ----------------

    @Test
    fun el_numerito_suma_lo_recibido_sin_ver_y_las_respuestas_sin_ver() {
        assertEquals(0, ResumenSolicitudes.VACIO.nuevas)
        assertEquals(5, ResumenSolicitudes(recibidasNuevas = 2, recibidasPendientes = 7, enviadasNovedades = 3).nuevas)
    }

    @Test
    fun solo_se_avisa_cuando_hay_mas_novedades_que_la_ultima_vez() {
        val hoy = ResumenSolicitudes(recibidasNuevas = 2, enviadasNovedades = 1)
        assertEquals(2, PoliticaAvisos.textos(hoy, ultimasRecibidas = 0, ultimasEnviadas = 0).size)
        assertTrue("mismo resumen: no se repite", PoliticaAvisos.textos(hoy, 2, 1).isEmpty())
        assertTrue("si bajó (ya las vio): no avisa", PoliticaAvisos.textos(ResumenSolicitudes(recibidasNuevas = 1), 2, 0).isEmpty())
        val soloNueva = PoliticaAvisos.textos(ResumenSolicitudes(recibidasNuevas = 3), ultimasRecibidas = 2, ultimasEnviadas = 0)
        assertEquals(1, soloNueva.size)
        assertEquals(TextoAviso.RECIBIDAS, soloNueva.single().clave)
    }

    @Test
    fun el_texto_del_aviso_cambia_entre_una_y_varias() {
        val una = PoliticaAvisos.textos(ResumenSolicitudes(recibidasNuevas = 1), 0, 0).single()
        assertEquals("Nueva solicitud", una.titulo)
        val varias = PoliticaAvisos.textos(ResumenSolicitudes(recibidasNuevas = 4), 0, 0).single()
        assertEquals("Nuevas solicitudes", varias.titulo)
        assertTrue(varias.texto.contains("4"))
        val respuesta = PoliticaAvisos.textos(ResumenSolicitudes(enviadasNovedades = 1), 0, 0).single()
        assertEquals(TextoAviso.ENVIADAS, respuesta.clave)
        assertEquals("Respondieron tu solicitud", respuesta.titulo)
    }

    // ---------------- hace cuánto ----------------

    @Test
    fun el_tiempo_se_dice_en_palabras() {
        val ahora = Instant.parse("2026-11-15T20:00:00Z")
        fun hace(iso: String?) = TiempoRelativo.desde(iso, ahora, ZoneOffset.UTC)
        assertEquals("ahora", hace("2026-11-15T19:59:40Z"))
        assertEquals("hace 5 min", hace("2026-11-15T19:55:00Z"))
        assertEquals("hace 3 h", hace("2026-11-15T17:00:00.000Z"))
        assertEquals("ayer", hace("2026-11-14T15:00:00Z"))
        assertEquals("10/11/2026", hace("2026-11-10T08:30:00Z"))
        assertEquals("", hace(null))
        assertEquals("", hace(""))
        assertEquals("", hace("no es una fecha"))
    }
}
