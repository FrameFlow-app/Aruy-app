package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.ContraparteSolicitud
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.repository.SolicitudRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Los casos de uso de las solicitudes, con un repositorio falso en memoria (sin internet ni Android). */
class SolicitudesCasosDeUsoTest {

    private class SolicitudesFalsas : SolicitudRepository {
        val enviadas = mutableListOf<Pair<String, String>>()
        val cambios = mutableListOf<Triple<String, EstadoSolicitud, String?>>()
        val vistas = mutableListOf<TipoSolicitudes>()

        private fun base(estado: EstadoSolicitud, mensaje: String = "Necesito ayuda") = Solicitud(
            "s1", "serv1", "Pintura", estado, mensaje, null, null, null, false, ContraparteSolicitud("Marcos", null, null)
        )

        override suspend fun enviar(servicioId: String, mensaje: String): Solicitud {
            enviadas += servicioId to mensaje
            return base(EstadoSolicitud.PENDIENTE, mensaje)
        }

        override suspend fun listar(tipo: TipoSolicitudes): List<Solicitud> = listOf(base(EstadoSolicitud.PENDIENTE))

        override suspend fun cambiarEstado(id: String, estado: EstadoSolicitud, respuesta: String?): Solicitud {
            cambios += Triple(id, estado, respuesta)
            return base(estado)
        }

        override suspend fun resumen(): ResumenSolicitudes = ResumenSolicitudes(recibidasNuevas = 2)

        override suspend fun marcarVistas(tipo: TipoSolicitudes) {
            vistas += tipo
        }
    }

    private fun solicitud(estado: EstadoSolicitud) = Solicitud(
        "s1", "serv1", "Pintura", estado, "Necesito ayuda", null, null, null, false, ContraparteSolicitud("Marcos", null, null)
    )

    @Test
    fun enviar_una_solicitud_limpia_el_mensaje_y_llega_al_repositorio() = runBlocking {
        val repo = SolicitudesFalsas()
        EnviarSolicitudUseCase(repo)("serv1", "   Necesito pintar mi sala   ")
        assertEquals(listOf("serv1" to "Necesito pintar mi sala"), repo.enviadas)
    }

    @Test
    fun un_mensaje_muy_corto_o_muy_largo_se_rechaza_sin_llegar_al_repositorio() = runBlocking {
        val repo = SolicitudesFalsas()
        for (mensaje in listOf("", "hola", "x".repeat(501))) {
            try {
                EnviarSolicitudUseCase(repo)("serv1", mensaje)
                fail("Debía rechazar un mensaje de ${mensaje.length} letras")
            } catch (e: IllegalArgumentException) {
                assertNotNull(e.message)
            }
        }
        assertTrue(repo.enviadas.isEmpty())
    }

    @Test
    fun el_prestador_acepta_con_una_respuesta_y_llega_limpia() = runBlocking {
        val repo = SolicitudesFalsas()
        CambiarEstadoSolicitudUseCase(repo)(
            solicitud(EstadoSolicitud.PENDIENTE), TipoSolicitudes.RECIBIDAS, EstadoSolicitud.ACEPTADA, "  Puedo el sábado  "
        )
        assertEquals(Triple("s1", EstadoSolicitud.ACEPTADA, "Puedo el sábado"), repo.cambios.single())
    }

    @Test
    fun una_respuesta_en_blanco_no_se_envia() = runBlocking {
        val repo = SolicitudesFalsas()
        CambiarEstadoSolicitudUseCase(repo)(solicitud(EstadoSolicitud.PENDIENTE), TipoSolicitudes.RECIBIDAS, EstadoSolicitud.RECHAZADA, "   ")
        assertNull(repo.cambios.single().third)
    }

    @Test
    fun cada_quien_solo_hace_los_cambios_que_le_tocan() = runBlocking {
        val repo = SolicitudesFalsas()
        val prohibidos = listOf(
            // quien envió no puede aceptar su propia solicitud
            Triple(EstadoSolicitud.PENDIENTE, TipoSolicitudes.ENVIADAS, EstadoSolicitud.ACEPTADA),
            // el prestador no completa lo que no aceptó
            Triple(EstadoSolicitud.PENDIENTE, TipoSolicitudes.RECIBIDAS, EstadoSolicitud.COMPLETADA),
            // una rechazada no se reabre
            Triple(EstadoSolicitud.RECHAZADA, TipoSolicitudes.RECIBIDAS, EstadoSolicitud.ACEPTADA)
        )
        for ((estado, tipo, nuevo) in prohibidos) {
            try {
                CambiarEstadoSolicitudUseCase(repo)(solicitud(estado), tipo, nuevo)
                fail("No debía permitir $estado -> $nuevo en $tipo")
            } catch (e: IllegalArgumentException) {
                assertNotNull(e.message)
            }
        }
        assertTrue(repo.cambios.isEmpty())
    }

    @Test
    fun una_respuesta_demasiado_larga_se_rechaza() = runBlocking {
        val repo = SolicitudesFalsas()
        try {
            CambiarEstadoSolicitudUseCase(repo)(
                solicitud(EstadoSolicitud.PENDIENTE), TipoSolicitudes.RECIBIDAS, EstadoSolicitud.RECHAZADA, "x".repeat(301)
            )
            fail("Debía rechazar la respuesta")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }
        assertTrue(repo.cambios.isEmpty())
    }

    @Test
    fun obtener_solicitudes_resumen_y_marcar_vistas_pasan_por_el_repositorio() = runBlocking {
        val repo = SolicitudesFalsas()
        assertEquals(1, ObtenerSolicitudesUseCase(repo)(TipoSolicitudes.ENVIADAS).size)
        assertEquals(2, ObtenerResumenSolicitudesUseCase(repo)().recibidasNuevas)
        MarcarSolicitudesVistasUseCase(repo)(TipoSolicitudes.RECIBIDAS)
        assertEquals(listOf(TipoSolicitudes.RECIBIDAS), repo.vistas)
    }
}
