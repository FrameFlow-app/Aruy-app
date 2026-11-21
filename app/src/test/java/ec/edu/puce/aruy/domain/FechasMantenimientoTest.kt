package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.avisos.textoFechasMantenimiento
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FechasMantenimientoTest {
    private val zona = ZoneId.of("America/Guayaquil")
    private fun f(a: Int, m: Int, d: Int, h: Int, min: Int = 0) = ZonedDateTime.of(a, m, d, h, min, 0, 0, zona)
    private val ahora = f(2026, 11, 20, 10)

    @Test
    fun mismoDiaMuestraSoloHoraEnElFin() {
        assertEquals(
            "domingo 22 de nov., 22:00 a 23:00",
            textoFechasMantenimiento(f(2026, 11, 22, 22), f(2026, 11, 22, 23), ahora)
        )
    }

    @Test
    fun diasDistintosMuestraDiaEnAmbos() {
        assertEquals(
            "domingo 22 de nov., 22:00 a lunes 23 de nov., 04:00",
            textoFechasMantenimiento(f(2026, 11, 22, 22), f(2026, 11, 23, 4), ahora)
        )
    }

    @Test
    fun otroAnioAnadeElAnio() {
        assertEquals(
            "lunes 22 de nov. de 2027, 22:00 a 23:00",
            textoFechasMantenimiento(f(2027, 11, 22, 22), f(2027, 11, 22, 23), ahora)
        )
    }

    @Test
    fun siUnaFechaEsDeOtroAnioAmbasLlevanAnio() {
        assertEquals(
            "jueves 31 de dic. de 2026, 22:00 a viernes 1 de ene. de 2027, 02:00",
            textoFechasMantenimiento(f(2026, 12, 31, 22), f(2027, 1, 1, 2), f(2026, 12, 1, 10))
        )
    }

    @Test
    fun soloInicioYSoloFin() {
        assertEquals("Desde el domingo 22 de nov., 22:00", textoFechasMantenimiento(f(2026, 11, 22, 22), null, ahora))
        assertEquals("Hasta el domingo 22 de nov., 23:00", textoFechasMantenimiento(null, f(2026, 11, 22, 23), ahora))
    }

    @Test
    fun sinFechasEsNulo() {
        assertNull(textoFechasMantenimiento(null, null, ahora))
    }
}
