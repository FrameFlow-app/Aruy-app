package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.Avisos

interface AvisosRepository {
    /** Avisos del servidor, o null si no se pudo consultar. */
    suspend fun obtener(): Avisos?

    suspend fun mantenimientoVisto(): String?
    suspend fun guardarMantenimientoVisto(huella: String)
    suspend fun versionOmitida(): Int
    suspend fun guardarVersionOmitida(version: Int)
}
