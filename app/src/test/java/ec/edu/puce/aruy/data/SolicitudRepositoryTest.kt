package ec.edu.puce.aruy.data

import com.google.gson.JsonParser
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.remote.util.MensajesSolicitud
import ec.edu.puce.aruy.data.repository.SolicitudRepositoryImpl
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SolicitudRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: SolicitudRepositoryImpl

    private val solicitudJson = """
        {"id":"s1","servicio_id":"serv1","servicio_titulo":"Arreglo de tuberias","estado":"PENDIENTE",
         "mensaje":"Necesito arreglar una tuberia","respuesta":null,
         "fecha_creacion":"2026-11-15T20:00:00.000Z","fecha_actualizacion":"2026-11-15T20:00:00.000Z","sin_ver":true,
         "contraparte":{"nombre":"Marcos Prestador","foto_perfil":null,"telefono":null}}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val cliente = RetrofitClient({ "token" }, {}, servidor.url("/").toString())
        repositorio = SolicitudRepositoryImpl(cliente.solicitudApi)
    }

    @After
    fun terminar() = servidor.shutdown()

    private fun httpError(codigo: Int, cuerpo: String = "{}") =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun enviar_manda_el_servicio_y_el_mensaje_y_entiende_la_respuesta() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(solicitudJson))

        val s = repositorio.enviar("serv1", "Necesito arreglar una tuberia")

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/solicitudes", peticion.path)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("serv1", cuerpo.get("servicio_id").asString)
        assertEquals("Necesito arreglar una tuberia", cuerpo.get("mensaje").asString)
        assertEquals("s1", s.id)
        assertEquals(EstadoSolicitud.PENDIENTE, s.estado)
        assertEquals("Marcos Prestador", s.contraparte.nombre)
        assertNull(s.contraparte.telefono)
        assertNull(s.respuesta)
        assertTrue(s.sinVer)
    }

    @Test
    fun listar_pide_el_tipo_y_convierte_cada_solicitud() = runBlocking {
        servidor.enqueue(MockResponse().setBody("[$solicitudJson]"))

        val lista = repositorio.listar(TipoSolicitudes.RECIBIDAS)

        assertEquals("/solicitudes?tipo=recibidas", servidor.takeRequest().path)
        assertEquals(1, lista.size)
        assertEquals("Arreglo de tuberias", lista.single().servicioTitulo)
    }

    @Test
    fun una_solicitud_sin_contraparte_o_con_estado_nuevo_no_rompe_la_app() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""[{"id":"s2","estado":"ALGO_NUEVO","mensaje":null}]"""))

        val s = repositorio.listar(TipoSolicitudes.ENVIADAS).single()

        assertEquals(EstadoSolicitud.PENDIENTE, s.estado)
        assertEquals("", s.mensaje)
        assertEquals("Cuenta eliminada", s.contraparte.nombre)
        assertNull(s.servicioTitulo)
        assertFalse(s.sinVer)
    }

    @Test
    fun cambiar_el_estado_manda_el_nuevo_estado_y_la_respuesta() = runBlocking {
        servidor.enqueue(MockResponse().setBody(solicitudJson.replace("PENDIENTE", "ACEPTADA").replace("\"respuesta\":null", "\"respuesta\":\"Puedo el sabado\"")))

        val s = repositorio.cambiarEstado("s1", EstadoSolicitud.ACEPTADA, "Puedo el sabado")

        val peticion = servidor.takeRequest()
        assertEquals("PATCH", peticion.method)
        assertEquals("/solicitudes/s1", peticion.path)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("ACEPTADA", cuerpo.get("estado").asString)
        assertEquals("Puedo el sabado", cuerpo.get("respuesta").asString)
        assertEquals(EstadoSolicitud.ACEPTADA, s.estado)
        assertEquals("Puedo el sabado", s.respuesta)
    }

    @Test
    fun sin_respuesta_el_campo_no_viaja() = runBlocking {
        servidor.enqueue(MockResponse().setBody(solicitudJson))
        repositorio.cambiarEstado("s1", EstadoSolicitud.CANCELADA)
        val cuerpo = JsonParser.parseString(servidor.takeRequest().body.readUtf8()).asJsonObject
        assertFalse(cuerpo.has("respuesta"))
    }

    @Test
    fun el_resumen_se_convierte() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"recibidas_nuevas":2,"recibidas_pendientes":5,"enviadas_novedades":1}"""))

        val r = repositorio.resumen()

        assertEquals("/solicitudes/resumen", servidor.takeRequest().path)
        assertEquals(2, r.recibidasNuevas)
        assertEquals(5, r.recibidasPendientes)
        assertEquals(1, r.enviadasNovedades)
        assertEquals(3, r.nuevas)
    }

    @Test
    fun marcar_vistas_manda_el_tipo() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"marcadas":3}"""))

        repositorio.marcarVistas(TipoSolicitudes.ENVIADAS)

        val peticion = servidor.takeRequest()
        assertEquals("/solicitudes/vistas", peticion.path)
        assertEquals("enviadas", JsonParser.parseString(peticion.body.readUtf8()).asJsonObject.get("tipo").asString)
    }

    @Test
    fun los_errores_se_explican_en_espanol() {
        assertEquals(
            "Ya enviaste una solicitud pendiente para este servicio",
            MensajesSolicitud.alEnviar(httpError(409, """{"error":"Ya enviaste una solicitud pendiente para este servicio"}"""))
        )
        assertEquals("No puedes enviar una solicitud a tu propio servicio.", MensajesSolicitud.alEnviar(httpError(403)))
        assertEquals("Este servicio ya no está disponible.", MensajesSolicitud.alEnviar(httpError(404)))
        assertEquals("Esta solicitud ya no existe.", MensajesSolicitud.alCambiar(httpError(404)))
        assertEquals(
            "Esta solicitud cambió mientras la mirabas. Actualiza la lista.",
            MensajesSolicitud.alCambiar(httpError(409, """{"error":"Una solicitud RECHAZADA no puede pasar a ACEPTADA"}"""))
        )
        assertEquals("Cuéntale qué necesitas (mínimo 5 letras).", MensajesSolicitud.alEnviar(IllegalArgumentException("Cuéntale qué necesitas (mínimo 5 letras).")))
    }
}
