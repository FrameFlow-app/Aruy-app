package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.PrestadorPublico
import ec.edu.puce.aruy.domain.model.ResumenOpiniones
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.domain.model.ServicioConPrestador

/** Contrato del catálogo que ve quien busca un servicio. */
interface CatalogoRepository {
    suspend fun cargarCatalogo(): List<ServicioConPrestador>

    /** null si el prestador no existe o el servidor no responde. */
    suspend fun prestador(perfilId: String): PrestadorPublico?

    suspend fun servicio(id: String): Servicio

    suspend fun serviciosDe(perfilId: String): List<Servicio>

    suspend fun fotos(servicioId: String, forzar: Boolean = false): List<FotoPortafolio>

    suspend fun opiniones(perfilId: String): ResumenOpiniones

    suspend fun calificar(perfilId: String, puntuacion: Int, comentario: String?)
}
