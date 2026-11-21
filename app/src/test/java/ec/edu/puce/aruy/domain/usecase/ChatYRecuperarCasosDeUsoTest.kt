package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Mensaje
import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.model.TipoMensaje
import ec.edu.puce.aruy.domain.repository.MensajeRepository
import ec.edu.puce.aruy.domain.repository.RecuperarRepository
import ec.edu.puce.aruy.domain.solicitudes.HoraChat
import ec.edu.puce.aruy.domain.solicitudes.PoliticaAvisos
import ec.edu.puce.aruy.domain.solicitudes.textoMensajesNuevos
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** Los casos de uso del chat y de recuperar la contraseña, con repositorios falsos en memoria. */
class ChatYRecuperarCasosDeUsoTest {

    private class ChatFalso : MensajeRepository {
        val pedidos = mutableListOf<Pair<String, String?>>()
        val enviados = mutableListOf<Pair<String, String>>()
        val compartidos = mutableListOf<String>()
        var respuesta: List<Mensaje> = emptyList()

        override suspend fun mensajes(solicitudId: String, desde: String?): List<Mensaje> {
            pedidos += solicitudId to desde
            return respuesta
        }

        override suspend fun enviar(solicitudId: String, texto: String): Mensaje {
            enviados += solicitudId to texto
            return Mensaje("nuevo", true, texto, TipoMensaje.TEXTO, null)
        }

        override suspend fun compartirWhatsapp(solicitudId: String): Mensaje {
            compartidos += solicitudId
            return Mensaje("wa", true, "593990000001", TipoMensaje.WHATSAPP, null)
        }
    }

    private class RecuperarFalso(private val falla: Exception? = null) : RecuperarRepository {
        val codigos = mutableListOf<String>()
        val cambios = mutableListOf<Triple<String, String, String>>()

        override suspend fun pedirCodigo(correo: String): String {
            falla?.let { throw it }
            codigos += correo
            return "Si el correo existe, te enviamos un código"
        }

        override suspend fun restablecer(correo: String, codigo: String, password: String): String {
            falla?.let { throw it }
            cambios += Triple(correo, codigo, password)
            return "Contraseña actualizada"
        }
    }

    private fun msg(id: String, fecha: String? = null) = Mensaje(id, false, "texto $id", TipoMensaje.TEXTO, fecha)

    // ---- chat ----

    @Test
    fun enviar_un_mensaje_lo_limpia_y_llega_al_repositorio() = runBlocking {
        val repo = ChatFalso()
        val enviado = EnviarMensajeUseCase(repo)("s1", "   Hola, ¿a qué hora?   ")
        assertEquals(listOf("s1" to "Hola, ¿a qué hora?"), repo.enviados)
        assertTrue(enviado.esMio)
    }

    @Test
    fun un_mensaje_vacio_o_de_mas_de_1000_letras_no_sale() = runBlocking {
        val repo = ChatFalso()
        for (texto in listOf("", "   ", "x".repeat(1001))) {
            try {
                EnviarMensajeUseCase(repo)("s1", texto)
                fail("Debía rechazar un mensaje de ${texto.length} letras")
            } catch (e: IllegalArgumentException) {
                assertNotNull(e.message)
            }
        }
        assertTrue(repo.enviados.isEmpty())
        EnviarMensajeUseCase(repo)("s1", "x".repeat(1000)) // justo el máximo sí sale
        assertEquals(1, repo.enviados.size)
    }

    @Test
    fun compartir_whatsapp_pasa_por_el_repositorio_y_devuelve_el_numero() = runBlocking {
        val repo = ChatFalso()
        val m = CompartirWhatsappUseCase(repo)("s1")
        assertEquals(listOf("s1"), repo.compartidos)
        assertEquals(TipoMensaje.WHATSAPP, m.tipo)
        assertEquals("593990000001", m.texto)
    }

    @Test
    fun el_refresco_pide_desde_el_ultimo_y_la_carga_inicial_pide_todo() = runBlocking {
        val repo = ChatFalso().apply { respuesta = listOf(msg("a")) }
        ObtenerMensajesUseCase(repo)("s1")
        ObtenerMensajesUseCase(repo)("s1", "2026-11-20T15:05:00.000Z")
        assertEquals(listOf("s1" to null, "s1" to "2026-11-20T15:05:00.000Z"), repo.pedidos)
    }

    @Test
    fun los_mensajes_nuevos_se_suman_sin_repetir_los_que_ya_estaban() {
        val actuales = listOf(msg("a"), msg("b"))
        val combinados = combinarMensajes(actuales, listOf(msg("b"), msg("c")))
        assertEquals(listOf("a", "b", "c"), combinados.map { it.id })
        assertTrue(combinarMensajes(actuales, emptyList()) === actuales)
    }

    @Test
    fun la_hora_corta_distingue_hoy_ayer_y_antes() {
        val zona = ZoneId.of("UTC")
        val ahora = Instant.parse("2026-11-20T18:00:00Z")
        assertEquals("14:05", HoraChat.corta("2026-11-20T14:05:00.000Z", ahora, zona))
        assertEquals("ayer 09:30", HoraChat.corta("2026-11-19T09:30:00.000Z", ahora, zona))
        assertEquals("15/11 08:00", HoraChat.corta("2026-11-15T08:00:00.000Z", ahora, zona))
        assertEquals("", HoraChat.corta(null, ahora, zona))
        assertEquals("", HoraChat.corta("no es fecha", ahora, zona))
    }

    @Test
    fun los_mensajes_sin_leer_generan_su_aviso_y_su_texto() {
        assertEquals("Tienes 1 mensaje nuevo", textoMensajesNuevos(1))
        assertEquals("Tienes 3 mensajes nuevos", textoMensajesNuevos(3))
        val avisos = PoliticaAvisos.textos(ResumenSolicitudes(mensajesSinLeer = 2), 0, 0, ultimosMensajes = 0)
        assertEquals(1, avisos.size)
        assertTrue(avisos.single().texto.startsWith("Tienes 2 mensajes nuevos"))
        // sin más mensajes que la última vez: no se repite el aviso
        assertTrue(PoliticaAvisos.textos(ResumenSolicitudes(mensajesSinLeer = 2), 0, 0, ultimosMensajes = 2).isEmpty())
    }

    // ---- recuperar contraseña ----

    @Test
    fun pedir_el_codigo_valida_el_correo_y_llega_al_repositorio() = runBlocking {
        val repo = RecuperarFalso()
        val mensaje = PedirCodigoUseCase(repo)("lucia@correo.com")
        assertEquals(listOf("lucia@correo.com"), repo.codigos)
        assertEquals("Si el correo existe, te enviamos un código", mensaje)
        try {
            PedirCodigoUseCase(repo)("esto no es un correo")
            fail("Debía rechazar el correo")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }
        assertEquals(1, repo.codigos.size)
    }

    @Test
    fun restablecer_con_codigo_y_contrasena_validos_llega_al_repositorio() = runBlocking {
        val repo = RecuperarFalso()
        val mensaje = RestablecerContrasenaUseCase(repo)("lucia@correo.com", "123456", "Clave1234", "Clave1234")
        assertEquals(listOf(Triple("lucia@correo.com", "123456", "Clave1234")), repo.cambios)
        assertEquals("Contraseña actualizada", mensaje)
    }

    @Test
    fun restablecer_rechaza_codigo_contrasena_debil_o_que_no_coincide_sin_llamar_al_servidor() = runBlocking {
        val repo = RecuperarFalso()
        val casos = listOf(
            listOf("12345", "Clave1234", "Clave1234"),     // código corto
            listOf("12ab56", "Clave1234", "Clave1234"),    // código con letras
            listOf("123456", "corta1", "corta1"),          // menos de 8
            listOf("123456", "soloLetrasAqui", "soloLetrasAqui"), // sin números
            listOf("123456", "Clave1234", "Clave12345")    // no coincide
        )
        for ((codigo, password, confirmar) in casos) {
            try {
                RestablecerContrasenaUseCase(repo)("lucia@correo.com", codigo, password, confirmar)
                fail("Debía rechazar $codigo / $password / $confirmar")
            } catch (e: IllegalArgumentException) {
                assertNotNull(e.message)
            }
        }
        assertTrue(repo.cambios.isEmpty())
        assertNull(null)
    }
}
