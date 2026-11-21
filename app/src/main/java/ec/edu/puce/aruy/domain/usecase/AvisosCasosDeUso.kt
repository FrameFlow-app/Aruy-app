package ec.edu.puce.aruy.domain.usecase

import ec.edu.puce.aruy.domain.model.Actualizacion
import ec.edu.puce.aruy.domain.model.Avisos
import ec.edu.puce.aruy.domain.model.DecisionAvisos
import ec.edu.puce.aruy.domain.repository.AvisosRepository

/**
 * Qué avisos mostrar. Obligatoria si la app es anterior a la mínima (y mínima > 0); opcional si es anterior
 * a la reciente y esa misma reciente no se dejó para después; mantenimiento si está activo y no se leyó ya.
 */
fun decidirAvisos(
    avisos: Avisos,
    versionCode: Int,
    mantenimientoVistoHash: String?,
    versionVistaOmitida: Int
): DecisionAvisos {
    val v = avisos.version
    val actualizacion = when {
        v.minima > 0 && versionCode < v.minima -> Actualizacion.OBLIGATORIA
        versionCode < v.reciente && versionVistaOmitida != v.reciente -> Actualizacion.OPCIONAL
        else -> Actualizacion.NINGUNA
    }
    val m = avisos.mantenimiento
    val mostrarMantenimiento = m != null && m.activo && m.huella != mantenimientoVistoHash
    return DecisionAvisos(mostrarMantenimiento, actualizacion)
}

/** Resultado listo para pintar: los avisos y qué mostrar. */
data class AvisosParaMostrar(val avisos: Avisos, val decision: DecisionAvisos)

class ObtenerAvisosUseCase(private val repositorio: AvisosRepository) {
    /** null si no se pudo consultar el servidor. */
    suspend operator fun invoke(versionCode: Int): AvisosParaMostrar? {
        val avisos = repositorio.obtener() ?: return null
        val decision = decidirAvisos(
            avisos, versionCode, repositorio.mantenimientoVisto(), repositorio.versionOmitida()
        )
        return AvisosParaMostrar(avisos, decision)
    }
}

class MarcarMantenimientoVistoUseCase(private val repositorio: AvisosRepository) {
    suspend operator fun invoke(huella: String) = repositorio.guardarMantenimientoVisto(huella)
}

class OmitirVersionUseCase(private val repositorio: AvisosRepository) {
    suspend operator fun invoke(version: Int) = repositorio.guardarVersionOmitida(version)
}
