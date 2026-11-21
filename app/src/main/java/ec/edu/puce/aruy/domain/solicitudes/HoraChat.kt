package ec.edu.puce.aruy.domain.solicitudes

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Fecha corta de un mensaje: "14:05" si es de hoy, "ayer 14:05" o "18/11 14:05". Vacío si la fecha no se entiende. */
object HoraChat {
    private val HORA = DateTimeFormatter.ofPattern("HH:mm")
    private val DIA_Y_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

    fun corta(iso: String?, ahora: Instant = Instant.now(), zona: ZoneId = ZoneId.systemDefault()): String {
        if (iso.isNullOrBlank()) return ""
        val instante = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        val fecha = instante.atZone(zona)
        val hoy = ahora.atZone(zona).toLocalDate()
        return when (fecha.toLocalDate()) {
            hoy -> HORA.format(fecha)
            hoy.minusDays(1) -> "ayer " + HORA.format(fecha)
            else -> DIA_Y_HORA.format(fecha)
        }
    }
}
