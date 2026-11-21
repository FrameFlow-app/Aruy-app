package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.remote.AuthInterceptor
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.sesion.RenovadorSesion
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** La sesión se renueva sola y un 401 tardío de un token viejo no cierra la sesión nueva. */
class SesionRenovacionTest {

    private lateinit var servidor: MockWebServer
    private val ahora = 1_800_000_000_000L
    private val dia = 24 * 3600L

    private fun sesion(token: String) = Sesion(
        token, Usuario("u1", "Camila Andrade", "0998765432", RolUsuario.CONTRATANTE), perfilPrestadorId = null
    )

    private fun respuesta(token: String) = """
        {"token":"$token",
         "usuario":{"id":"u1","nombre":"Camila Andrade","telefono":"0998765432","rol":"CONTRATANTE",
                    "estado_verificacion":"NO_SOLICITADA","foto_perfil":null,"activo":true},
         "perfil_prestador_id":null}
    """.trimIndent()

    @Before
    fun preparar() { servidor = MockWebServer().apply { start() } }

    @After
    fun terminar() = servidor.shutdown()

    private fun armar(sesion: Sesion): Pair<SesionManager, RenovadorSesion> = runBlocking {
        val manager = SesionManager(AlmacenEnMemoria(), CoroutineScope(Dispatchers.Unconfined), ahoraMs = { ahora })
        manager.iniciar(sesion)
        val cliente = RetrofitClient(manager::tokenActual, manager::expirar, servidor.url("/").toString())
        manager to RenovadorSesion(cliente.authApi, manager, ahoraMs = { ahora })
    }

    @Test
    fun a_un_token_con_pocos_dias_se_le_pide_uno_nuevo_de_30() = runBlocking {
        val viejo = tokenDePrueba(ahora / 1000 + 5 * dia)
        val nuevo = tokenDePrueba(ahora / 1000 + 30 * dia)
        val (manager, renovador) = armar(sesion(viejo))
        servidor.enqueue(MockResponse().setBody(respuesta(nuevo)))

        renovador.renovarSiHaceFalta()

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/auth/renovar", peticion.path)
        assertEquals("Bearer $viejo", peticion.getHeader("Authorization"))
        assertEquals(nuevo, manager.tokenActual())
        assertTrue(manager.estado.value is EstadoSesion.Autenticada)
    }

    @Test
    fun a_un_token_con_muchos_dias_no_se_le_pide_nada() = runBlocking {
        val (manager, renovador) = armar(sesion(tokenDePrueba(ahora / 1000 + 25 * dia)))

        renovador.renovarSiHaceFalta()

        assertEquals(0, servidor.requestCount)
        assertTrue(manager.estado.value is EstadoSesion.Autenticada)
    }

    @Test
    fun si_el_servidor_falla_la_sesion_actual_sigue_valiendo() = runBlocking {
        val viejo = tokenDePrueba(ahora / 1000 + 2 * dia)
        val (manager, renovador) = armar(sesion(viejo))
        servidor.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        renovador.renovarSiHaceFalta()

        assertEquals(viejo, manager.tokenActual())
        assertTrue(manager.estado.value is EstadoSesion.Autenticada)
    }

    @Test
    fun si_no_hay_sesion_no_hace_nada() = runBlocking {
        val manager = SesionManager(AlmacenEnMemoria(), CoroutineScope(Dispatchers.Unconfined), ahoraMs = { ahora })
        val cliente = RetrofitClient(manager::tokenActual, manager::expirar, servidor.url("/").toString())
        RenovadorSesion(cliente.authApi, manager, ahoraMs = { ahora }).renovarSiHaceFalta()
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun un_401_del_token_actual_cierra_la_sesion() {
        var token: String? = "A"
        var expiro = 0
        servidor.enqueue(MockResponse().setResponseCode(401))
        val http = OkHttpClient.Builder().addInterceptor(AuthInterceptor({ token }, { expiro++ })).build()

        http.newCall(Request.Builder().url(servidor.url("/x")).build()).execute().close()

        assertEquals(1, expiro)
    }

    @Test
    fun un_401_tardio_de_un_token_viejo_no_cierra_la_sesion_nueva() {
        // El token con que salió la petición era "A", pero cuando llegó la respuesta ya se había iniciado sesión de nuevo ("B")
        val tokens = ArrayDeque(listOf("A", "B"))
        var ultimo: String? = null
        var expiro = 0
        servidor.enqueue(MockResponse().setResponseCode(401))
        val http = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor({ tokens.removeFirstOrNull()?.also { ultimo = it } ?: ultimo }, { expiro++ }))
            .build()

        http.newCall(Request.Builder().url(servidor.url("/x")).build()).execute().close()

        assertEquals(0, expiro)
    }

    @Test
    fun un_401_sin_token_no_es_una_sesion_vencida() {
        var expiro = 0
        servidor.enqueue(MockResponse().setResponseCode(401))
        val http = OkHttpClient.Builder().addInterceptor(AuthInterceptor({ null }, { expiro++ })).build()

        http.newCall(Request.Builder().url(servidor.url("/auth/login")).build()).execute().close()

        assertEquals(0, expiro)
    }
}
