package ec.edu.puce.aruy.domain.solicitudes

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** "hace 5 min", "hace 3 h", "ayer" o la fecha, a partir de un instante ISO del servidor. Vacío si no se entiende. */
object TiempoRelativo {
    fun desde(iso: String?, ahora: Instant = Instant.now(), zona: ZoneId = ZoneId.systemDefault()): String {
        if (iso.isNullOrBlank()) return ""
        val instante = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        val minutos = Duration.between(instante, ahora).toMinutes()
        return when {
            minutos < 1 -> "ahora"
            minutos < 60 -> "hace $minutos min"
            minutos < 24 * 60 -> "hace ${minutos / 60} h"
            minutos < 48 * 60 -> "ayer"
            else -> DateTimeFormatter.ofPattern("dd/MM/yyyy").format(instante.atZone(zona))
        }
    }
}
