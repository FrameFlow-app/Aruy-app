package ec.edu.puce.aruy.domain.model

/** Datos del formulario de un servicio, listos para enviar. */
data class DatosServicio(
    val titulo: String,
    val descripcion: String,
    val categoria: CategoriaServicio,
    val precio: Double?,
    val disponible: Boolean
)
