package ec.edu.puce.aruy.data

import com.google.gson.JsonParser
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.remote.util.MensajesSolicitud
import ec.edu.puce.aruy.data.repository.MensajeRepositoryImpl
import ec.edu.puce.aruy.data.repository.SolicitudRepositoryImpl
import ec.edu.puce.aruy.domain.model.TipoMensaje
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** El chat contra un servidor de mentira: rutas, cuerpos y cómo se entienden los DTO nuevos. */
class ChatRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var chat: MensajeRepositoryImpl
    private lateinit var solicitudes: SolicitudRepositoryImpl

    private val mensajeJson = """
        {"id":"m1","autor_id":"u1","es_mio":true,"texto":"Hola, ¿puedes el sábado?","tipo":"TEXTO","fecha_creacion":"2026-11-20T15:00:00.000Z"}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val cliente = RetrofitClient({ "token" }, {}, servidor.url("/").toString())
        chat = MensajeRepositoryImpl(cliente.solicitudApi)
        solicitudes = SolicitudRepositoryImpl(cliente.solicitudApi)
    }

    @After
    fun terminar() = servidor.shutdown()

    private fun httpError(codigo: Int, cuerpo: String = "{}") =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun los_mensajes_se_piden_sin_desde_la_primera_vez_y_se_convierten() = runBlocking {
        servidor.enqueue(
            MockResponse().setBody(
                "[$mensajeJson,{\"id\":\"m2\",\"autor_id\":\"u2\",\"es_mio\":false,\"texto\":\"593990000001\",\"tipo\":\"WHATSAPP\",\"fecha_creacion\":\"2026-11-20T15:05:00.000Z\"}]"
            )
        )

        val lista = chat.mensajes("s1")

        assertEquals("/solicitudes/s1/mensajes", servidor.takeRequest().path)
        assertEquals(2, lista.size)
        assertTrue(lista[0].esMio)
        assertEquals(TipoMensaje.TEXTO, lista[0].tipo)
        assertEquals("Hola, ¿puedes el sábado?", lista[0].texto)
        assertFalse(lista[1].esMio)
        assertEquals(TipoMensaje.WHATSAPP, lista[1].tipo)
        assertEquals("593990000001", lista[1].texto)
    }

    @Test
    fun el_refresco_manda_desde_con_la_fecha_del_ultimo_mensaje() = runBlocking {
        servidor.enqueue(MockResponse().setBody("[]"))

        val lista = chat.mensajes("s1", desde = "2026-11-20T15:05:00.000Z")

        val ruta = servidor.takeRequest().requestUrl!!
        assertEquals("/solicitudes/s1/mensajes", ruta.encodedPath)
        assertEquals("2026-11-20T15:05:00.000Z", ruta.queryParameter("desde"))
        assertTrue(lista.isEmpty())
    }

    @Test
    fun enviar_manda_el_texto_y_entiende_el_mensaje_creado() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(mensajeJson))

        val m = chat.enviar("s1", "Hola, ¿puedes el sábado?")

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/solicitudes/s1/mensajes", peticion.path)
        assertEquals("Hola, ¿puedes el sábado?", JsonParser.parseString(peticion.body.readUtf8()).asJsonObject.get("texto").asString)
        assertEquals("m1", m.id)
        assertTrue(m.esMio)
    }

    @Test
    fun compartir_whatsapp_es_un_post_sin_cuerpo_y_devuelve_el_numero() = runBlocking {
        servidor.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":"m3","autor_id":"u1","es_mio":true,"texto":"593990000001","tipo":"WHATSAPP","fecha_creacion":"2026-11-20T15:10:00.000Z"}"""
            )
        )

        val m = chat.compartirWhatsapp("s1")

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/solicitudes/s1/whatsapp", peticion.path)
        assertEquals(TipoMensaje.WHATSAPP, m.tipo)
        assertEquals("593990000001", m.texto)
    }

    @Test
    fun un_tipo_desconocido_se_trata_como_texto_y_los_campos_que_faltan_no_rompen() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""[{"id":"m9","tipo":"OTRO"}]"""))

        val m = chat.mensajes("s1").single()

        assertEquals(TipoMensaje.TEXTO, m.tipo)
        assertEquals("", m.texto)
        assertFalse(m.esMio)
        assertNull(m.fecha)
    }

    @Test
    fun la_solicitud_trae_whatsapp_compartido_mensajes_sin_leer_y_ultimo_mensaje() = runBlocking {
        servidor.enqueue(
            MockResponse().setBody(
                """[{"id":"s1","estado":"ACEPTADA","mensaje":"Necesito ayuda","whatsapp_compartido":true,"mensajes_sin_leer":3,
                    "ultimo_mensaje":{"texto":"Voy en camino","fecha":"2026-11-20T16:00:00.000Z"},
                    "contraparte":{"nombre":"Marcos","telefono":"0990000001"}}]"""
            )
        )

        val s = solicitudes.listar(TipoSolicitudes.ENVIADAS).single()

        assertTrue(s.whatsappCompartido)
        assertEquals(3, s.mensajesSinLeer)
        assertEquals("Voy en camino", s.ultimoMensaje?.texto)
        assertEquals("2026-11-20T16:00:00.000Z", s.ultimoMensaje?.fecha)
        assertTrue(s.activa)
        assertTrue(s.chatAbierto)
    }

    @Test
    fun una_solicitud_sin_los_campos_nuevos_queda_sin_chat_ni_pendientes() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""[{"id":"s2","estado":"RECHAZADA","ultimo_mensaje":null}]"""))

        val s = solicitudes.listar(TipoSolicitudes.RECIBIDAS).single()

        assertFalse(s.whatsappCompartido)
        assertEquals(0, s.mensajesSinLeer)
        assertNull(s.ultimoMensaje)
        assertFalse(s.chatAbierto)
        assertFalse(s.activa)
    }

    @Test
    fun el_resumen_incluye_los_mensajes_sin_leer_en_el_numerito() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"recibidas_nuevas":1,"recibidas_pendientes":2,"enviadas_novedades":1,"mensajes_sin_leer":4}"""))

        val r = solicitudes.resumen()

        assertEquals(4, r.mensajesSinLeer)
        assertEquals(6, r.nuevas)
    }

    @Test
    fun los_errores_del_chat_se_explican_en_espanol() {
        assertEquals("Esta solicitud ya está cerrada: no se pueden enviar más mensajes.", MensajesSolicitud.alEscribir(httpError(409)))
        assertEquals("Solo quien ofrece el servicio puede compartir su WhatsApp.", MensajesSolicitud.alCompartirWhatsapp(httpError(403)))
        assertEquals(
            "Tu teléfono no es válido",
            MensajesSolicitud.alCompartirWhatsapp(httpError(400, """{"error":"Tu teléfono no es válido"}"""))
        )
        assertNotNull(MensajesSolicitud.alEscribir(httpError(429)))
        assertEquals("No tienes acceso a esta conversación.", MensajesSolicitud.alAbrirChat(httpError(403)))
    }
}
