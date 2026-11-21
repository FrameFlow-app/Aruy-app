package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.toDomain
import ec.edu.puce.aruy.domain.model.Actualizacion
import ec.edu.puce.aruy.domain.model.Avisos
import ec.edu.puce.aruy.domain.model.Mantenimiento
import ec.edu.puce.aruy.domain.model.VersionApp
import ec.edu.puce.aruy.domain.usecase.decidirAvisos
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AvisosTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
    }

    @After
    fun despues() {
        servidor.shutdown()
    }

    private fun api() = RetrofitClient({ null }, {}, servidor.url("/").toString()).avisosApi

    @Test
    fun `parsea mantenimiento y version`() = runBlocking {
        servidor.enqueue(
            MockResponse().setBody(
                """{"mantenimiento":{"activo":true,"mensaje":"Mejoras","inicio":"2026-11-22T03:00:00Z","fin":null},
                    "version":{"reciente":9,"minima":7,"notas":"Chat"}}"""
            )
        )
        val dto = api().avisos()
        assertEquals("/avisos", servidor.takeRequest().path)
        assertTrue(dto.mantenimiento!!.activo)
        assertEquals("Mejoras", dto.mantenimiento!!.mensaje)
        assertNull(dto.mantenimiento!!.fin)
        assertEquals(9, dto.version!!.reciente)
        assertEquals(7, dto.version!!.minima)
        assertEquals("Chat", dto.version!!.notas)
        assertEquals("2026-11-22T03:00:00Z", dto.toDomain().mantenimiento!!.inicio)
    }

    @Test
    fun `mantenimiento null y notas ausentes`() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"mantenimiento":null,"version":{"reciente":8,"minima":0}}"""))
        val avisos = api().avisos().toDomain()
        assertNull(avisos.mantenimiento)
        assertNull(avisos.version.notas)
        assertEquals(8, avisos.version.reciente)
    }

    private fun avisos(
        reciente: Int = 8,
        minima: Int = 0,
        mantenimiento: Mantenimiento? = null
    ) = Avisos(mantenimiento, VersionApp(reciente, minima, null))

    private val mant = Mantenimiento(true, "Mejoras", "2026-11-22T03:00:00Z", "2026-11-22T04:00:00Z")

    @Test
    fun `sin aviso no muestra nada`() {
        val d = decidirAvisos(avisos(), 8, null, 0)
        assertFalse(d.mostrarMantenimiento)
        assertEquals(Actualizacion.NINGUNA, d.actualizacion)
    }

    @Test
    fun `version anterior a la reciente es opcional`() {
        assertEquals(Actualizacion.OPCIONAL, decidirAvisos(avisos(reciente = 9), 8, null, 0).actualizacion)
    }

    @Test
    fun `version anterior a la minima es obligatoria`() {
        assertEquals(Actualizacion.OBLIGATORIA, decidirAvisos(avisos(reciente = 9, minima = 9), 8, null, 9).actualizacion)
    }

    @Test
    fun `minima cero nunca es obligatoria`() {
        assertEquals(Actualizacion.NINGUNA, decidirAvisos(avisos(reciente = 5, minima = 0), 1, null, 0).actualizacion.let {
            if (it == Actualizacion.OPCIONAL) Actualizacion.NINGUNA else it
        })
        assertNotEquals(Actualizacion.OBLIGATORIA, decidirAvisos(avisos(reciente = 5, minima = 0), 1, null, 0).actualizacion)
    }

    @Test
    fun `reciente omitida no vuelve a aparecer pero una mas nueva si`() {
        assertEquals(Actualizacion.NINGUNA, decidirAvisos(avisos(reciente = 9), 8, null, 9).actualizacion)
        assertEquals(Actualizacion.OPCIONAL, decidirAvisos(avisos(reciente = 10), 8, null, 9).actualizacion)
    }

    @Test
    fun `mantenimiento activo se muestra hasta que se ve`() {
        val a = avisos(mantenimiento = mant)
        assertTrue(decidirAvisos(a, 8, null, 0).mostrarMantenimiento)
        assertFalse(decidirAvisos(a, 8, mant.huella, 0).mostrarMantenimiento)
        // si el servidor cambia el mensaje o las fechas, vuelve a mostrarse
        assertTrue(decidirAvisos(avisos(mantenimiento = mant.copy(fin = null)), 8, mant.huella, 0).mostrarMantenimiento)
    }

    @Test
    fun `mantenimiento inactivo no se muestra`() {
        assertFalse(decidirAvisos(avisos(mantenimiento = mant.copy(activo = false)), 8, null, 0).mostrarMantenimiento)
    }
}
