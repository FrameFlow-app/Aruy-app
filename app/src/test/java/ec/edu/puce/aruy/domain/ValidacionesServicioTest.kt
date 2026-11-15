package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.validation.ValidacionesServicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionesServicioTest {

    @Test
    fun titulo_necesita_4_letras() {
        assertNull(ValidacionesServicio.titulo("Pintura"))
        assertNotNull(ValidacionesServicio.titulo("a1 2"))
        assertNotNull(ValidacionesServicio.titulo(""))
    }

    @Test
    fun descripcion_necesita_20_letras() {
        assertNull(ValidacionesServicio.descripcion("Instalo breakers y luminarias en casas"))
        assertNotNull(ValidacionesServicio.descripcion("Muy corta"))
        assertNotNull(ValidacionesServicio.descripcion("1234567890 1234567890 1234567890"))
    }

    @Test
    fun categoria_es_obligatoria() {
        assertNotNull(ValidacionesServicio.categoria(null))
        assertNull(ValidacionesServicio.categoria(CategoriaServicio.OTROS))
    }

    @Test
    fun precio_es_opcional_y_acepta_coma_decimal() {
        assertEquals(ValidacionesServicio.Precio(null, null), ValidacionesServicio.precio("  "))
        assertEquals(15.0, ValidacionesServicio.precio("15").valor!!, 0.0)
        assertEquals(12.5, ValidacionesServicio.precio("12,50").valor!!, 0.0)
    }

    @Test
    fun precio_rechaza_cero_negativos_texto_y_diez_mil() {
        listOf("0", "-5", "abc", "10000", "1e9x").forEach { texto ->
            assertNotNull("$texto debía ser inválido", ValidacionesServicio.precio(texto).error)
        }
    }

    @Test
    fun el_titulo_queda_con_inicial_mayuscula_y_el_resto_en_minuscula() {
        assertEquals("Limpieza de casas", ValidacionesServicio.normalizarTitulo("LIMPIEZA DE CASAS"))
        assertEquals("Limpieza de casas", ValidacionesServicio.normalizarTitulo("limpieza de casas"))
        assertEquals("Limpieza de casas", ValidacionesServicio.normalizarTitulo("  LiMpIeZa   dE  cAsAs  "))
        assertEquals("Árbol de navidad", ValidacionesServicio.normalizarTitulo("ÁRBOL DE NAVIDAD"))
        assertEquals("Ñandú y más", ValidacionesServicio.normalizarTitulo("ñandú Y MÁS"))
        assertEquals("", ValidacionesServicio.normalizarTitulo("   "))
    }

    @Test
    fun un_servicio_necesita_al_menos_una_foto() {
        assertNotNull(ValidacionesServicio.fotos(0))
        assertNull(ValidacionesServicio.fotos(1))
        assertNull(ValidacionesServicio.fotos(10))
    }
}
