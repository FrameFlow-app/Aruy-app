package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.catalogo.EtiquetaFoto
import ec.edu.puce.aruy.domain.catalogo.ResultadoFoto
import ec.edu.puce.aruy.domain.catalogo.SugerenciaCategoria
import ec.edu.puce.aruy.domain.catalogo.ValidadorFotoOficio
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las etiquetas son las que de verdad existen en el modelo base de ML Kit (label map oficial). */
class ValidadorFotoOficioTest {

    private fun foto(vararg pares: Pair<String, Float>) =
        pares.map { (original, confianza) -> EtiquetaFoto(SugerenciaCategoria.traducir(original), confianza, original) }

    private fun rechazada(r: ResultadoFoto): ResultadoFoto.Rechazada {
        assertTrue("se esperaba rechazar la foto, pero fue $r", r is ResultadoFoto.Rechazada)
        return r as ResultadoFoto.Rechazada
    }

    private fun evaluar(etiquetas: List<EtiquetaFoto>, categoria: CategoriaServicio?, titulo: String = "") =
        ValidadorFotoOficio.evaluar(etiquetas, categoria, titulo, "")

    // ---- El título del servicio manda: "Limpieza de casas" ----

    @Test
    fun limpieza_de_casas_acepta_una_casa_por_dentro() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Room" to 0.9f, "Person" to 0.9f), CategoriaServicio.OTROS, "Limpieza de casas"))
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Bedroom" to 0.9f, "Cushion" to 0.8f), CategoriaServicio.OTROS, "Limpieza de casas"))
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Kitchen" to 0.9f, "Sink" to 0.8f), CategoriaServicio.OTROS, "Limpieza y aseo del hogar"))
    }

    @Test
    fun limpieza_de_casas_rechaza_una_persona_sola_y_explica_que_no_se_reconoce_el_trabajo() {
        val r = rechazada(evaluar(foto("Person" to 0.9f, "Smile" to 0.9f), CategoriaServicio.OTROS, "Limpieza de casas"))
        assertTrue(r.mensaje.startsWith("No pudimos reconocer en esta foto un trabajo de «Limpieza de casas»."))
        assertTrue(r.mensaje.contains("Intenta con otra imagen"))
    }

    @Test
    fun si_no_se_reconoce_nada_en_un_servicio_conocido_se_rechaza_con_su_mensaje() {
        val r = rechazada(evaluar(emptyList(), CategoriaServicio.OTROS, "Limpieza de casas"))
        assertTrue(r.mensaje.startsWith("No pudimos reconocer qué muestra la foto."))
    }

    @Test
    fun una_selfie_en_un_cuarto_se_rechaza() {
        rechazada(evaluar(foto("Selfie" to 0.9f, "Room" to 0.85f), CategoriaServicio.OTROS, "Limpieza de casas"))
    }

    // ---- Lo que pasó en el celular: libro, calle y página web en "arreglar casas" ----

    @Test
    fun un_libro_sobre_un_escritorio_se_rechaza_en_arreglar_casas() {
        val etiquetas = foto("Desk" to 0.9f, "Tableware" to 0.8f, "Paper" to 0.8f, "Fiction" to 0.75f, "Product" to 0.7f)
        val r = rechazada(evaluar(etiquetas, CategoriaServicio.OTROS, "arreglar casas"))
        assertTrue(r.mensaje.contains("Detectamos"))
    }

    @Test
    fun una_calle_se_rechaza_aunque_aparezca_una_pared() {
        val etiquetas = foto("Road" to 0.9f, "Asphalt" to 0.9f, "Wall" to 0.8f, "Sky" to 0.9f)
        val r = rechazada(evaluar(etiquetas, CategoriaServicio.OTROS, "arreglar casas"))
        assertTrue(r.detectado.contains("Calle"))
    }

    @Test
    fun una_obra_junto_a_la_calle_se_acepta() {
        val etiquetas = foto("Construction" to 0.9f, "Brick" to 0.85f, "Road" to 0.8f)
        assertEquals(ResultadoFoto.Aceptada, evaluar(etiquetas, CategoriaServicio.OTROS, "arreglar casas"))
    }

    @Test
    fun la_foto_de_una_pagina_web_se_rechaza_en_arreglar_casas() {
        val r = rechazada(evaluar(foto("Web page" to 0.9f, "Computer" to 0.8f, "Screenshot" to 0.8f), CategoriaServicio.OTROS, "arreglar casas"))
        assertTrue(r.detectado.contains("Página web"))
    }

    // ---- Tecnología: las pantallas son el trabajo ----

    @Test
    fun tecnologia_acepta_pantallas_y_rechaza_mascotas() {
        val pantallas = foto("Computer" to 0.9f, "Screenshot" to 0.8f)
        assertEquals(ResultadoFoto.Aceptada, evaluar(pantallas, CategoriaServicio.OTROS, "Desarrollo de Apps"))
        assertEquals(ResultadoFoto.Aceptada, evaluar(pantallas, CategoriaServicio.OTROS, "Reparación de computadoras"))
        assertEquals(
            ResultadoFoto.Aceptada,
            evaluar(foto("Musical instrument" to 0.8f, "Computer" to 0.8f), CategoriaServicio.OTROS, "Programadora Junior")
        )
        rechazada(evaluar(foto("Dog" to 0.9f, "Cat" to 0.9f), CategoriaServicio.OTROS, "Desarrollo de Apps"))
    }

    // ---- Categorías concretas ----

    @Test
    fun plomeria_acepta_lavabo_y_rechaza_comida_con_el_oficio_en_el_mensaje() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Sink" to 0.9f, "Bathroom" to 0.8f), CategoriaServicio.PLOMERIA))
        val r = rechazada(evaluar(foto("Cheeseburger" to 0.9f, "Eating" to 0.8f), CategoriaServicio.PLOMERIA))
        assertTrue(r.mensaje.contains("de plomería"))
    }

    @Test
    fun carpinteria_acepta_escritorio_y_silla_pero_no_un_libro() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Desk" to 0.9f, "Chair" to 0.8f, "Cabinetry" to 0.8f), CategoriaServicio.CARPINTERIA))
        rechazada(evaluar(foto("Paper" to 0.9f, "Fiction" to 0.8f), CategoriaServicio.CARPINTERIA))
    }

    @Test
    fun belleza_acepta_cabello_y_retratos() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Hair" to 0.9f, "Selfie" to 0.9f, "Smile" to 0.8f), CategoriaServicio.BELLEZA))
        rechazada(evaluar(foto("Dog" to 0.9f), CategoriaServicio.BELLEZA))
    }

    // ---- Otros tipos de servicio que se reconocen por el título ----

    @Test
    fun jardineria_acepta_plantas_y_rechaza_un_bano() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Garden" to 0.9f, "Plant" to 0.8f, "Flower" to 0.8f), CategoriaServicio.OTROS, "Jardinería y poda"))
        rechazada(evaluar(foto("Bathroom" to 0.9f), CategoriaServicio.OTROS, "Jardinería y poda"))
    }

    @Test
    fun reposteria_acepta_pasteles_y_rechaza_una_calle() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Cake" to 0.9f, "Icing" to 0.8f), CategoriaServicio.OTROS, "Repostería"))
        rechazada(evaluar(foto("Road" to 0.9f), CategoriaServicio.OTROS, "Repostería"))
    }

    @Test
    fun los_eventos_permiten_selfis_y_multitudes() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Party" to 0.9f, "Crowd" to 0.9f, "Selfie" to 0.8f), CategoriaServicio.OTROS, "DJ para fiestas"))
    }

    @Test
    fun la_mecanica_permite_la_calle_porque_el_trabajo_puede_ser_en_la_via() {
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Car" to 0.9f, "Road" to 0.9f, "Metal" to 0.7f), CategoriaServicio.OTROS, "Mecánica automotriz"))
    }

    // ---- Título que no corresponde a ningún tipo conocido: solo se rechaza lo claramente ajeno ----

    @Test
    fun un_servicio_desconocido_solo_rechaza_lo_claramente_ajeno() {
        rechazada(evaluar(foto("Cat" to 0.9f, "Dog" to 0.9f), CategoriaServicio.OTROS, "Servicio especial"))
        rechazada(evaluar(foto("Selfie" to 0.9f), CategoriaServicio.OTROS, "Servicio especial"))
        assertEquals(ResultadoFoto.Aceptada, evaluar(foto("Mountain" to 0.9f), CategoriaServicio.OTROS, "Servicio especial"))
        assertEquals(ResultadoFoto.Aceptada, evaluar(emptyList(), CategoriaServicio.OTROS, "Servicio especial"))
    }

    @Test
    fun el_mensaje_muestra_hasta_tres_etiquetas_sin_repetir() {
        val etiquetas = foto("Cat" to 0.99f, "Dog" to 0.95f, "Pet" to 0.9f, "Bird" to 0.8f, "Fur" to 0.75f)
        val r = rechazada(evaluar(etiquetas, CategoriaServicio.OTROS, "Limpieza de casas"))
        assertEquals(listOf("Gato", "Perro", "Mascota"), r.detectado)
    }

    @Test
    fun arreglar_casas_tambien_acepta_el_jardin_y_las_plantas_de_la_casa() {
        val etiquetas = foto("Person" to 0.9f, "Smile" to 0.9f, "Plant" to 0.85f)
        assertEquals(ResultadoFoto.Aceptada, evaluar(etiquetas, CategoriaServicio.OTROS, "arreglar casas"))
        rechazada(evaluar(etiquetas, CategoriaServicio.OTROS, "Limpieza de casas"))
    }

    @Test
    fun nombrePara_traduce_lo_que_vio_la_ia() {
        assertEquals("Cuarto", ValidadorFotoOficio.nombrePara(foto("Room" to 0.9f).first()))
    }
}
