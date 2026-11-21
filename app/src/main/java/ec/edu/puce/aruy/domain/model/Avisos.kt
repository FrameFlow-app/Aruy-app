package ec.edu.puce.aruy.domain.model

data class Mantenimiento(
    val activo: Boolean,
    val mensaje: String,
    val inicio: String?,
    val fin: String?
) {
    /** Identifica este aviso: si el servidor cambia mensaje o fechas, vuelve a mostrarse. */
    val huella: String get() = "$mensaje|${inicio.orEmpty()}|${fin.orEmpty()}".hashCode().toString()
}

data class VersionApp(val reciente: Int, val minima: Int, val notas: String?)

data class Avisos(val mantenimiento: Mantenimiento?, val version: VersionApp)

enum class Actualizacion { NINGUNA, OPCIONAL, OBLIGATORIA }

data class DecisionAvisos(
    val mostrarMantenimiento: Boolean,
    val actualizacion: Actualizacion
)
