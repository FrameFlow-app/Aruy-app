package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.validation.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AniosExperienciaTest {

    @Test
    fun vacio_es_valido_porque_es_opcional() {
        val r = Validaciones.aniosExperiencia("   ")
        assertNull(r.valor)
        assertNull(r.error)
    }

    @Test
    fun un_numero_entre_0_y_80_es_valido() {
        assertEquals(0, Validaciones.aniosExperiencia("0").valor)
        assertEquals(3, Validaciones.aniosExperiencia(" 3 ").valor)
        assertEquals(80, Validaciones.aniosExperiencia("80").valor)
        assertNull(Validaciones.aniosExperiencia("80").error)
    }

    @Test
    fun letras_o_numeros_fuera_de_rango_dan_error() {
        for (texto in listOf("81", "-1", "tres", "2,5", "1e3")) {
            val r = Validaciones.aniosExperiencia(texto)
            assertNull(texto, r.valor)
            assertNotNull(texto, r.error)
        }
    }
}
