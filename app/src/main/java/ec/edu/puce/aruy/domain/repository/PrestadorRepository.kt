package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.DatosServicio
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.Servicio

/** Contrato de los servicios y las fotos del propio prestador. */
interface PrestadorRepository {
    /** Disponibilidad del perfil: `true` = "Recibiendo solicitudes". */
    suspend fun disponibilidad(): Boolean

    suspend fun cambiarDisponibilidad(disponible: Boolean): Boolean

    suspend fun misServicios(perfilId: String): List<Servicio>

    suspend fun servicio(id: String): Servicio

    suspend fun fotosDe(servicioId: String): List<FotoPortafolio>

    suspend fun crear(datos: DatosServicio): Servicio

    suspend fun editar(id: String, datos: DatosServicio): Servicio

    suspend fun cambiarDisponibilidadDelServicio(id: String, disponible: Boolean): Servicio

    suspend fun eliminar(id: String)

    /** Sube una foto ya convertida a WebP. */
    suspend fun subirFoto(
        servicioId: String,
        webp: ByteArray,
        etiquetas: List<String> = emptyList(),
        descripcion: String? = null
    ): FotoPortafolio

    suspend fun eliminarFoto(fotoId: String)

    companion object {
        const val MAX_FOTOS_POR_SERVICIO = 10
    }
}
