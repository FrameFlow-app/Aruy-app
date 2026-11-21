package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.local.AvisosVistosStore
import ec.edu.puce.aruy.data.remote.api.AvisosApi
import ec.edu.puce.aruy.data.remote.dto.AvisosDto
import ec.edu.puce.aruy.domain.model.Avisos
import ec.edu.puce.aruy.domain.model.Mantenimiento
import ec.edu.puce.aruy.domain.model.VersionApp
import ec.edu.puce.aruy.domain.repository.AvisosRepository
import kotlinx.coroutines.CancellationException

class AvisosRepositoryImpl(
    private val api: AvisosApi,
    private val vistos: AvisosVistosStore
) : AvisosRepository {

    override suspend fun obtener(): Avisos? = try {
        api.avisos().toDomain()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    override suspend fun mantenimientoVisto() = vistos.mantenimientoVisto()
    override suspend fun guardarMantenimientoVisto(huella: String) = vistos.guardarMantenimientoVisto(huella)
    override suspend fun versionOmitida() = vistos.versionOmitida()
    override suspend fun guardarVersionOmitida(version: Int) = vistos.guardarVersionOmitida(version)
}

internal fun AvisosDto.toDomain() = Avisos(
    mantenimiento = mantenimiento?.let {
        Mantenimiento(
            activo = it.activo,
            mensaje = it.mensaje.orEmpty(),
            inicio = it.inicio?.takeIf { s -> s.isNotBlank() },
            fin = it.fin?.takeIf { s -> s.isNotBlank() }
        )
    },
    version = VersionApp(
        reciente = version?.reciente ?: 0,
        minima = version?.minima ?: 0,
        notas = version?.notas?.takeIf { it.isNotBlank() }
    )
)
