package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.validation.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FechaNacimientoTest {

    private val hoy = LocalDate.of(2026, 11, 15)

    @Test
    fun mientras_se_escribe_pone_las_barras_y_solo_deja_numeros() {
        assertEquals("", Validaciones.formatearFecha(""))
        assertEquals("2", Validaciones.formatearFecha("2"))
        assertEquals("25", Validaciones.formatearFecha("25"))
        assertEquals("25/0", Validaciones.formatearFecha("250"))
        assertEquals("25/03", Validaciones.formatearFecha("2503"))
        assertEquals("25/03/2", Validaciones.formatearFecha("25032"))
        assertEquals("25/03/2000", Validaciones.formatearFecha("25032000"))
        assertEquals("25/03/2000", Validaciones.formatearFecha("25/03/2000"))
        assertEquals("25/03/2000", Validaciones.formatearFecha("2503200099999"))
        assertEquals("12/05", Validaciones.formatearFecha("12ab05"))
    }

    @Test
    fun una_fecha_de_una_persona_mayor_de_edad_es_valida() {
        val r = Validaciones.fechaNacimiento("25/03/2000", hoy)
        assertEquals(LocalDate.of(2000, 3, 25), r.fecha)
        assertNull(r.error)
    }

    @Test
    fun cumplir_18_hoy_ya_cuenta_y_cumplirlos_manana_no() {
        assertNull(Validaciones.fechaNacimiento("15/11/2008", hoy).error)
        val manana = Validaciones.fechaNacimiento("16/11/2008", hoy)
        assertNull(manana.fecha)
        assertEquals("Aruy es solo para mayores de 18 años.", manana.error)
    }

    @Test
    fun un_menor_de_edad_no_pasa() {
        val r = Validaciones.fechaNacimiento("01/01/2015", hoy)
        assertNull(r.fecha)
        assertTrue(r.error.orEmpty().contains("mayores de 18"))
    }

    @Test
    fun una_fecha_que_no_existe_o_mal_escrita_da_un_error_claro() {
        assertNotNull(Validaciones.fechaNacimiento("", hoy).error)
        assertNotNull(Validaciones.fechaNacimiento("25/03", hoy).error)
        assertNotNull(Validaciones.fechaNacimiento("25/03/00", hoy).error)
        assertNotNull(Validaciones.fechaNacimiento("ab/cd/efgh", hoy).error)
        assertEquals("Esa fecha no existe. Revisa el día, el mes y el año.", Validaciones.fechaNacimiento("30/02/2000", hoy).error)
        assertEquals("Esa fecha no existe. Revisa el día, el mes y el año.", Validaciones.fechaNacimiento("31/04/2000", hoy).error)
        assertEquals("Esa fecha no existe. Revisa el día, el mes y el año.", Validaciones.fechaNacimiento("10/13/2000", hoy).error)
    }

    @Test
    fun una_fecha_futura_o_de_hace_mas_de_120_anios_se_rechaza() {
        assertEquals("Revisa el año de tu fecha de nacimiento.", Validaciones.fechaNacimiento("01/01/2030", hoy).error)
        assertEquals("Revisa el año de tu fecha de nacimiento.", Validaciones.fechaNacimiento("01/01/1850", hoy).error)
    }

    @Test
    fun la_edad_se_cuenta_en_anios_cumplidos() {
        assertEquals(26, Validaciones.edadEn(LocalDate.of(2000, 11, 15), hoy)) // cumple hoy
        assertEquals(25, Validaciones.edadEn(LocalDate.of(2000, 11, 16), hoy)) // cumple mañana
        assertEquals(0, Validaciones.edadEn(LocalDate.of(2026, 1, 1), hoy))
    }
}
