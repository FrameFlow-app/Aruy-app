package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.catalogo.EtiquetaFoto
import ec.edu.puce.aruy.domain.catalogo.ResultadoFoto
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.DatosPrestador
import ec.edu.puce.aruy.domain.model.DatosRegistro
import ec.edu.puce.aruy.domain.model.DatosServicio
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.PrestadorPublico
import ec.edu.puce.aruy.domain.model.ResumenOpiniones
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.domain.repository.AuthRepository
import ec.edu.puce.aruy.domain.repository.CatalogoRepository
import ec.edu.puce.aruy.domain.repository.FotoPerfilRepository
import ec.edu.puce.aruy.domain.repository.PrestadorRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

/**
 * Los casos de uso se prueban con repositorios falsos que viven en memoria: sin internet, sin Android y sin
 * base de datos. Es justo lo que permite separar el dominio (interfaces) de los datos (implementaciones).
 */
class CasosDeUsoTest {

    private val sesionDePrueba = Sesion("token", Usuario("u1", "Ana", "0991111111", RolUsuario.CONTRATANTE))

    private fun foto(vararg pares: Pair<String, Float>) =
        pares.map { (original, confianza) -> EtiquetaFoto(original, confianza, original) }

    // ---------------- repositorios falsos ----------------

    private inner class AuthFalso : AuthRepository {
        var correoRecibido: String? = null
        var datosRecibidos: DatosRegistro? = null
        var prestadorRecibido: DatosPrestador? = null
        override suspend fun iniciarSesion(correo: String, password: String): Sesion {
            correoRecibido = correo
            return sesionDePrueba
        }

        override suspend fun registrar(datos: DatosRegistro): Sesion {
            datosRecibidos = datos
            return sesionDePrueba
        }

        override suspend fun activarPerfilPrestador(datos: DatosPrestador): Sesion {
            prestadorRecibido = datos
            return sesionDePrueba.copy(usuario = sesionDePrueba.usuario.copy(rol = RolUsuario.PRESTADOR), perfilPrestadorId = "perfil-nuevo")
        }

        override suspend fun cerrarSesion() = Unit
    }

    private class CatalogoFalso(private val servicios: List<ServicioConPrestador>) : CatalogoRepository {
        override suspend fun cargarCatalogo(): List<ServicioConPrestador> = servicios
        override suspend fun prestador(perfilId: String): PrestadorPublico? = null
        override suspend fun servicio(id: String): Servicio = throw NotImplementedError()
        override suspend fun serviciosDe(perfilId: String): List<Servicio> = emptyList()
        override suspend fun fotos(servicioId: String, forzar: Boolean): List<FotoPortafolio> = emptyList()
        override suspend fun opiniones(perfilId: String): ResumenOpiniones = throw NotImplementedError()
        override suspend fun calificar(perfilId: String, puntuacion: Int, comentario: String?) = Unit
    }

    private class PrestadorFalso : PrestadorRepository {
        val creados = mutableListOf<DatosServicio>()
        val editados = mutableListOf<Pair<String, DatosServicio>>()
        val fotosSubidas = mutableListOf<List<String>>()

        private fun servicioDe(id: String, datos: DatosServicio) =
            Servicio(id, "p1", datos.titulo, datos.descripcion, datos.categoria, datos.precio, datos.disponible)

        override suspend fun disponibilidad(): Boolean = true
        override suspend fun cambiarDisponibilidad(disponible: Boolean): Boolean = disponible
        override suspend fun misServicios(perfilId: String): List<Servicio> = emptyList()
        override suspend fun servicio(id: String): Servicio = throw NotImplementedError()
        override suspend fun fotosDe(servicioId: String): List<FotoPortafolio> = emptyList()
        override suspend fun crear(datos: DatosServicio): Servicio {
            creados += datos
            return servicioDe("nuevo", datos)
        }

        override suspend fun editar(id: String, datos: DatosServicio): Servicio {
            editados += id to datos
            return servicioDe(id, datos)
        }

        override suspend fun cambiarDisponibilidadDelServicio(id: String, disponible: Boolean): Servicio = throw NotImplementedError()
        override suspend fun eliminar(id: String) = Unit
        override suspend fun subirFoto(servicioId: String, webp: ByteArray, etiquetas: List<String>, descripcion: String?): FotoPortafolio {
            fotosSubidas += etiquetas
            return FotoPortafolio("f1", servicioId, "$servicioId/f1.webp")
        }

        override suspend fun eliminarFoto(fotoId: String) = Unit
    }

    private class FotoPerfilFalsa : FotoPerfilRepository {
        var bytesRecibidos: Int? = null
        var quitada = false
        private val base = Usuario("u1", "Ana", "0991111111", RolUsuario.CONTRATANTE)
        override suspend fun subirFoto(webp: ByteArray): Usuario {
            bytesRecibidos = webp.size
            return base.copy(fotoPerfil = "https://fotos.test/perfiles/u1/f.webp")
        }

        override suspend fun quitarFoto(): Usuario {
            quitada = true
            return base.copy(fotoPerfil = null)
        }
    }

    private val datos = DatosServicio("LIMPIEZA DE CASAS", "Limpio casas completas con productos propios", CategoriaServicio.OTROS, null, true)

    // ---------------- cuenta ----------------

    @Test
    fun iniciar_sesion_usa_el_repositorio_y_devuelve_la_sesion() = runBlocking {
        val auth = AuthFalso()
        val sesion = IniciarSesionUseCase(auth)("ana@correo.com", "clave")
        assertEquals(sesionDePrueba, sesion)
        assertEquals("ana@correo.com", auth.correoRecibido)
    }

    @Test
    fun registrar_cuenta_entrega_los_datos_al_repositorio() = runBlocking {
        val auth = AuthFalso()
        val registro = DatosRegistro(
            "Ana", "ana@correo.com", "0991111111", "Aruy2026x", RolUsuario.CONTRATANTE, fechaNacimiento = LocalDate.of(1995, 6, 15)
        )
        RegistrarCuentaUseCase(auth)(registro)
        assertEquals(registro, auth.datosRecibidos)
    }

    @Test
    fun registrar_cuenta_rechaza_a_un_menor_de_edad_y_no_llega_al_repositorio() = runBlocking {
        val auth = AuthFalso()
        val menor = DatosRegistro(
            "Ana", "ana@correo.com", "0991111111", "Aruy2026x", RolUsuario.CONTRATANTE, fechaNacimiento = LocalDate.now().minusYears(17)
        )
        try {
            RegistrarCuentaUseCase(auth)(menor)
            fail("Debía rechazar a un menor de edad")
        } catch (e: EdadInvalidaException) {
            assertTrue(e.message.orEmpty().contains("18"))
        }
        assertNull(auth.datosRecibidos)
    }

    @Test
    fun registrar_cuenta_acepta_a_quien_cumple_18_hoy() = runBlocking {
        val auth = AuthFalso()
        val justo = DatosRegistro(
            "Ana", "ana@correo.com", "0991111111", "Aruy2026x", RolUsuario.CONTRATANTE, fechaNacimiento = LocalDate.now().minusYears(18)
        )
        RegistrarCuentaUseCase(auth)(justo)
        assertEquals(justo, auth.datosRecibidos)
    }

    @Test
    fun registrar_cuenta_sin_fecha_de_nacimiento_se_rechaza() = runBlocking {
        val auth = AuthFalso()
        try {
            RegistrarCuentaUseCase(auth)(DatosRegistro("Ana", "ana@correo.com", "0991111111", "Aruy2026x", RolUsuario.CONTRATANTE))
            fail("Debía pedir la fecha de nacimiento")
        } catch (e: EdadInvalidaException) {
            assertNotNull(e.message)
        }
        assertNull(auth.datosRecibidos)
    }

    // ---------------- "Quiero ofrecer servicios" ----------------

    @Test
    fun activar_el_perfil_de_prestador_usa_la_misma_cuenta_y_devuelve_la_sesion_de_prestador() = runBlocking {
        val auth = AuthFalso()
        val sesion = ActivarPerfilPrestadorUseCase(auth)(DatosPrestador("Manicure", "Norte", aniosExperiencia = 3))
        assertEquals(RolUsuario.PRESTADOR, sesion.usuario.rol)
        assertEquals("perfil-nuevo", sesion.perfilPrestadorId)
        assertEquals("u1", sesion.usuario.id)
        assertEquals("Manicure", auth.prestadorRecibido?.oficio)
    }

    @Test
    fun activar_el_perfil_sin_oficio_o_sin_zona_se_rechaza_y_no_llega_al_repositorio() = runBlocking {
        val auth = AuthFalso()
        for (datos in listOf(DatosPrestador("", "Norte"), DatosPrestador("Manicure", "   "))) {
            try {
                ActivarPerfilPrestadorUseCase(auth)(datos)
                fail("Debía rechazar $datos")
            } catch (e: IllegalArgumentException) {
                assertNotNull(e.message)
            }
        }
        assertNull(auth.prestadorRecibido)
    }

    // ---------------- foto de perfil ----------------

    @Test
    fun subir_la_foto_de_perfil_devuelve_el_usuario_con_su_foto() = runBlocking {
        val fotos = FotoPerfilFalsa()
        val usuario = SubirFotoPerfilUseCase(fotos)(ByteArray(1000))
        assertEquals("https://fotos.test/perfiles/u1/f.webp", usuario.fotoPerfil)
        assertEquals(1000, fotos.bytesRecibidos)
    }

    @Test
    fun una_foto_vacia_o_demasiado_grande_se_rechaza_sin_llegar_al_repositorio() = runBlocking {
        val fotos = FotoPerfilFalsa()
        for (bytes in listOf(ByteArray(0), ByteArray(SubirFotoPerfilUseCase.TAMANO_MAXIMO + 1))) {
            try {
                SubirFotoPerfilUseCase(fotos)(bytes)
                fail("Debía rechazar una foto de ${bytes.size} bytes")
            } catch (e: IllegalArgumentException) {
                assertNotNull(e.message)
            }
        }
        assertNull(fotos.bytesRecibidos)
    }

    @Test
    fun una_foto_justo_en_el_limite_se_acepta() = runBlocking {
        val fotos = FotoPerfilFalsa()
        SubirFotoPerfilUseCase(fotos)(ByteArray(SubirFotoPerfilUseCase.TAMANO_MAXIMO))
        assertEquals(SubirFotoPerfilUseCase.TAMANO_MAXIMO, fotos.bytesRecibidos)
    }

    @Test
    fun quitar_la_foto_de_perfil_vuelve_a_las_iniciales() = runBlocking {
        val fotos = FotoPerfilFalsa()
        val usuario = QuitarFotoPerfilUseCase(fotos)()
        assertNull(usuario.fotoPerfil)
        assertTrue(fotos.quitada)
    }

    // ---------------- catálogo ----------------

    @Test
    fun obtener_catalogo_devuelve_lo_que_da_el_repositorio() = runBlocking {
        val item = ServicioConPrestador(Servicio("s1", "p1", "Pintura", "Pinto paredes y techos", CategoriaServicio.PINTURA), null)
        val resultado = ObtenerCatalogoUseCase(CatalogoFalso(listOf(item)))()
        assertEquals(listOf(item), resultado)
    }

    // ---------------- publicar un servicio ----------------

    @Test
    fun publicar_un_servicio_nuevo_lo_crea_con_el_titulo_bien_escrito() = runBlocking {
        val prestador = PrestadorFalso()
        val servicio = PublicarServicioUseCase(prestador)(null, datos, totalFotos = 1)
        assertEquals("nuevo", servicio.id)
        assertEquals(1, prestador.creados.size)
        assertEquals("Limpieza de casas", prestador.creados.first().titulo)
        assertTrue(prestador.editados.isEmpty())
    }

    @Test
    fun publicar_un_servicio_existente_lo_edita_y_no_crea_otro() = runBlocking {
        val prestador = PrestadorFalso()
        PublicarServicioUseCase(prestador)("s9", datos, totalFotos = 3)
        assertTrue(prestador.creados.isEmpty())
        assertEquals("s9", prestador.editados.single().first)
        assertEquals("Limpieza de casas", prestador.editados.single().second.titulo)
    }

    @Test
    fun publicar_sin_fotos_se_rechaza_y_no_llega_al_repositorio() = runBlocking {
        val prestador = PrestadorFalso()
        try {
            PublicarServicioUseCase(prestador)(null, datos, totalFotos = 0)
            fail("Debía rechazar un servicio sin fotos")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }
        assertTrue(prestador.creados.isEmpty())
    }

    // ---------------- fotos ----------------

    @Test
    fun una_foto_ajena_al_servicio_se_rechaza_sin_subir_nada() = runBlocking {
        val prestador = PrestadorFalso()
        val resultado = SubirFotoPortafolioUseCase(prestador)(
            "s1", ByteArray(3), foto("Road" to 0.9f, "Asphalt" to 0.9f), CategoriaServicio.OTROS, "Limpieza de casas", ""
        )
        assertTrue(resultado is ResultadoSubidaFoto.Rechazada)
        assertTrue((resultado as ResultadoSubidaFoto.Rechazada).mensaje.contains("Intenta con otra imagen"))
        assertTrue(prestador.fotosSubidas.isEmpty())
    }

    @Test
    fun una_foto_del_servicio_se_sube_con_sus_etiquetas() = runBlocking {
        val prestador = PrestadorFalso()
        val resultado = SubirFotoPortafolioUseCase(prestador)(
            "s1", ByteArray(3), foto("Room" to 0.9f, "Bedroom" to 0.8f), CategoriaServicio.OTROS, "Limpieza de casas", ""
        )
        assertTrue(resultado is ResultadoSubidaFoto.Subida)
        assertEquals("s1", (resultado as ResultadoSubidaFoto.Subida).foto.servicioId)
        assertEquals(listOf(listOf("Room", "Bedroom")), prestador.fotosSubidas)
    }

    @Test
    fun validar_foto_usa_las_reglas_del_dominio() {
        val validar = ValidarFotoServicioUseCase()
        assertEquals(ResultadoFoto.Aceptada, validar(foto("Kitchen" to 0.9f), CategoriaServicio.OTROS, "Limpieza de casas", ""))
        assertTrue(validar(foto("Cat" to 0.9f), CategoriaServicio.OTROS, "Limpieza de casas", "") is ResultadoFoto.Rechazada)
    }
}
