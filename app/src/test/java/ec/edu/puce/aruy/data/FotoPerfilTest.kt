package ec.edu.puce.aruy.data

import com.google.gson.JsonParser
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.remote.util.MensajesPerfil
import ec.edu.puce.aruy.data.repository.PerfilRepository
import ec.edu.puce.aruy.domain.repository.FotoPerfilRepository
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class FotoPerfilTest {

    private lateinit var servidor: MockWebServer
    private lateinit var perfiles: PerfilRepository

    private val conFoto = """
        {"id":"u1","nombre":"Camila Andrade","telefono":"0998765432","rol":"CONTRATANTE",
         "estado_verificacion":"NO_SOLICITADA","activo":true,
         "foto_perfil":"https://fotos.test/storage/v1/object/public/aruy-portafolio/perfiles/u1/a.webp",
         "formato":"webp","ancho":512,"alto":512,"bytes":18000,"calidad":80}
    """.trimIndent()

    private val sinFoto = """
        {"id":"u1","nombre":"Camila Andrade","telefono":"0998765432","rol":"CONTRATANTE",
         "estado_verificacion":"NO_SOLICITADA","foto_perfil":null,"activo":true}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val cliente = RetrofitClient({ "token" }, {}, servidor.url("/").toString())
        perfiles = PerfilRepository(cliente.usuarioApi, cliente.prestadorApi)
    }

    @After
    fun terminar() = servidor.shutdown()

    @Test
    fun subir_la_foto_manda_la_imagen_tal_cual_como_webp_y_devuelve_la_direccion() = runBlocking {
        servidor.enqueue(MockResponse().setBody(conFoto))
        val imagen = ByteArray(300) { (it % 251).toByte() }

        val usuario = (perfiles as FotoPerfilRepository).subirFoto(imagen)

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/usuarios/yo/foto", peticion.path)
        assertEquals("image/webp", peticion.getHeader("Content-Type"))
        assertArrayEquals(imagen, peticion.body.readByteArray())
        assertEquals("https://fotos.test/storage/v1/object/public/aruy-portafolio/perfiles/u1/a.webp", usuario.fotoPerfil)
    }

    @Test
    fun quitar_la_foto_manda_foto_perfil_null_y_no_borra_el_campo() = runBlocking {
        servidor.enqueue(MockResponse().setBody(sinFoto))

        val usuario = (perfiles as FotoPerfilRepository).quitarFoto()

        val peticion = servidor.takeRequest()
        assertEquals("PATCH", peticion.method)
        assertEquals("/usuarios/yo", peticion.path)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals(true, cuerpo.has("foto_perfil"))
        assertEquals(true, cuerpo.get("foto_perfil").isJsonNull)
        assertNull(usuario.fotoPerfil)
    }

    private fun httpError(codigo: Int, cuerpo: String) =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun los_errores_de_la_foto_se_explican_en_espanol() {
        assertEquals("No pudimos leer esa imagen. Prueba con otra foto.", MensajesPerfil.alSubirFoto(httpError(400, "{}")))
        assertEquals("La foto pesa demasiado. Elige una de menos de 5 MB.", MensajesPerfil.alSubirFoto(httpError(413, "{}")))
        assertEquals("Ese formato no es válido. Usa una foto JPG, PNG o WebP.", MensajesPerfil.alSubirFoto(httpError(415, "{}")))
        assertEquals("No pudimos reducir la foto. Prueba con otra más pequeña.", MensajesPerfil.alSubirFoto(httpError(422, "{}")))
        assertEquals(
            "El almacenamiento de fotos no está disponible ahora. Inténtalo en unos minutos.",
            MensajesPerfil.alSubirFoto(httpError(502, "{}"))
        )
    }
}
