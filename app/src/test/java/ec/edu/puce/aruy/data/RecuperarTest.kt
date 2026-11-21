package ec.edu.puce.aruy.data

import com.google.gson.JsonParser
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.remote.util.MensajesSolicitud
import ec.edu.puce.aruy.data.repository.RecuperarRepositoryImpl
import ec.edu.puce.aruy.domain.usecase.PedirCodigoUseCase
import ec.edu.puce.aruy.domain.usecase.RestablecerContrasenaUseCase
import ec.edu.puce.aruy.ui.viewmodel.PasoRecuperar
import ec.edu.puce.aruy.ui.viewmodel.RecuperarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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

/** "Olvidé mi contraseña" contra un servidor de mentira, y el ViewModel que lleva los dos pasos. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecuperarTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repo: RecuperarRepositoryImpl

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        servidor = MockWebServer().apply { start() }
        val cliente = RetrofitClient({ null }, {}, servidor.url("/").toString())
        repo = RecuperarRepositoryImpl(cliente.authApi)
    }

    @After
    fun terminar() {
        servidor.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun olvide_manda_el_correo_en_minusculas_y_devuelve_el_mensaje() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"mensaje":"Si el correo existe, te enviamos un código"}"""))

        val mensaje = repo.pedirCodigo("  Lucia@Correo.com ")

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/auth/olvide", peticion.path)
        assertEquals("lucia@correo.com", JsonParser.parseString(peticion.body.readUtf8()).asJsonObject.get("email").asString)
        assertEquals("Si el correo existe, te enviamos un código", mensaje)
    }

    @Test
    fun restablecer_manda_correo_codigo_y_contrasena() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"mensaje":"Contraseña actualizada"}"""))

        val mensaje = repo.restablecer("lucia@correo.com", "123456", "Clave1234")

        val peticion = servidor.takeRequest()
        assertEquals("/auth/restablecer", peticion.path)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("lucia@correo.com", cuerpo.get("email").asString)
        assertEquals("123456", cuerpo.get("codigo").asString)
        assertEquals("Clave1234", cuerpo.get("password").asString)
        assertEquals("Contraseña actualizada", mensaje)
    }

    // ---- ViewModel, con un repositorio falso para que todo ocurra sin esperar a la red ----

    private class Falso : ec.edu.puce.aruy.domain.repository.RecuperarRepository {
        var falla: Exception? = null
        var pedidos = 0
        override suspend fun pedirCodigo(correo: String): String {
            pedidos++
            falla?.let { throw it }
            return "Si el correo existe, te enviamos un código"
        }
        override suspend fun restablecer(correo: String, codigo: String, password: String): String {
            falla?.let { throw it }
            return "Contraseña actualizada"
        }
    }

    private fun httpError(codigo: Int, cuerpo: String) = retrofit2.HttpException(
        retrofit2.Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType()))
    )

    private fun crear(falso: Falso) = RecuperarViewModel(PedirCodigoUseCase(falso), RestablecerContrasenaUseCase(falso))

    @Test
    fun el_paso_1_pasa_al_paso_2_y_empieza_la_espera_de_60_segundos() {
        val vm = crear(Falso())

        vm.pedirCodigo("lucia@correo.com")

        val ui = vm.ui.value
        assertEquals(PasoRecuperar.CODIGO, ui.paso)
        assertEquals("lucia@correo.com", ui.correo)
        assertEquals("Si el correo existe, te enviamos un código", ui.info)
        assertNull(ui.error)
        assertFalse(ui.cargando)
        assertEquals(60, ui.esperaReenvio)
    }

    @Test
    fun no_se_puede_reenviar_mientras_corre_la_espera() {
        val falso = Falso()
        val vm = crear(falso)
        vm.pedirCodigo("lucia@correo.com")

        vm.pedirCodigo("lucia@correo.com") // ignorado: sigue la espera

        assertEquals(1, falso.pedidos)
    }

    @Test
    fun un_correo_mal_escrito_se_queda_en_el_paso_1_sin_llamar_al_servidor() {
        val falso = Falso()
        val vm = crear(falso)

        vm.pedirCodigo("no es correo")

        assertEquals(PasoRecuperar.CORREO, vm.ui.value.paso)
        assertTrue(vm.ui.value.error!!.contains("correo"))
        assertEquals(0, falso.pedidos)
    }

    @Test
    fun un_error_400_muestra_el_mensaje_del_servidor_tal_cual() {
        val falso = Falso()
        val vm = crear(falso)
        vm.pedirCodigo("lucia@correo.com")
        falso.falla = httpError(400, """{"error":"El código es incorrecto o ya venció"}""")
        var listo = false

        vm.restablecer("123456", "Clave1234", "Clave1234") { listo = true }

        assertFalse(listo)
        assertEquals("El código es incorrecto o ya venció", vm.ui.value.error)
        assertFalse(vm.ui.value.cargando)
        assertEquals(PasoRecuperar.CODIGO, vm.ui.value.paso)
    }

    @Test
    fun restablecer_con_exito_avisa_para_volver_al_login() {
        val vm = crear(Falso())
        vm.pedirCodigo("lucia@correo.com")
        var listo = false

        vm.restablecer("123456", "Clave1234", "Clave1234") { listo = true }

        assertTrue(listo)
        assertNull(vm.ui.value.error)
    }

    @Test
    fun el_429_se_explica_en_espanol() {
        assertEquals(
            "Hiciste muchos intentos. Espera unos minutos e inténtalo de nuevo.",
            MensajesSolicitud.alRecuperar(httpError(429, "{}"))
        )
    }
}
