package ec.edu.puce.aruy.domain.model

/** En qué va una solicitud: la persona la envía (PENDIENTE) y el prestador la acepta o rechaza. */
enum class EstadoSolicitud(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada"),
    COMPLETADA("Completada");

    companion object {
        fun desde(texto: String?): EstadoSolicitud = entries.firstOrNull { it.name == texto } ?: PENDIENTE
    }
}

/** Las dos bandejas: las que yo envié y las que me enviaron a mí como prestador. */
enum class TipoSolicitudes(val valor: String) {
    ENVIADAS("enviadas"),
    RECIBIDAS("recibidas")
}

/** La otra persona de la solicitud: el prestador en las enviadas, el cliente en las recibidas. */
data class ContraparteSolicitud(
    val nombre: String,
    val fotoPerfil: String?,
    /** Solo llega cuando la solicitud está ACEPTADA. */
    val telefono: String?
)

data class Solicitud(
    val id: String,
    val servicioId: String?,
    /** null si el servicio ya se eliminó. */
    val servicioTitulo: String?,
    val estado: EstadoSolicitud,
    val mensaje: String,
    val respuesta: String?,
    /** Fecha en formato ISO (`2026-11-15T20:15:00.000Z`). */
    val fechaCreacion: String?,
    val fechaActualizacion: String?,
    /** true si todavía no la viste. */
    val sinVer: Boolean,
    val contraparte: ContraparteSolicitud
)

/** Lo que cambió desde la última vez que se miró: sirve para el numerito y las notificaciones. */
data class ResumenSolicitudes(
    val recibidasNuevas: Int = 0,
    val recibidasPendientes: Int = 0,
    val enviadasNovedades: Int = 0
) {
    /** Cuántas cosas nuevas hay en total. */
    val nuevas: Int get() = recibidasNuevas + enviadasNovedades

    companion object {
        val VACIO = ResumenSolicitudes()
    }
}

/**
 * Qué cambios de estado puede hacer la persona, según de qué bandeja es la solicitud. Son las mismas reglas del
 * servidor (que las vuelve a comprobar): el prestador responde; quien envió la solicitud solo puede cancelarla.
 */
fun Solicitud.accionesPara(tipo: TipoSolicitudes): List<EstadoSolicitud> = when (tipo) {
    TipoSolicitudes.RECIBIDAS -> when (estado) {
        EstadoSolicitud.PENDIENTE -> listOf(EstadoSolicitud.ACEPTADA, EstadoSolicitud.RECHAZADA)
        EstadoSolicitud.ACEPTADA -> listOf(EstadoSolicitud.COMPLETADA, EstadoSolicitud.CANCELADA)
        else -> emptyList()
    }
    TipoSolicitudes.ENVIADAS -> when (estado) {
        EstadoSolicitud.PENDIENTE, EstadoSolicitud.ACEPTADA -> listOf(EstadoSolicitud.CANCELADA)
        else -> emptyList()
    }
}
