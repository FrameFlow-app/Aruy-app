package ec.edu.puce.aruy.domain.avisos

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE = Locale.forLanguageTag("es-EC")

/**
 * "sábado 22 de nov., 22:00 a 23:00" (mismo día) o "sábado 22 de nov., 22:00 a domingo 23 de nov., 04:00".
 * Si alguna fecha no es del año de [ahora], todas llevan año ("22 de nov. de 2027").
 * Las fechas deben venir ya en la zona horaria a mostrar. Null si no hay ninguna.
 */
fun textoFechasMantenimiento(inicio: ZonedDateTime?, fin: ZonedDateTime?, ahora: ZonedDateTime): String? {
    val conAnio = listOfNotNull(inicio, fin).any { it.year != ahora.year }
    return when {
        inicio != null && fin != null ->
            if (inicio.toLocalDate() == fin.toLocalDate()) "${dia(inicio, conAnio)}, ${hora(inicio)} a ${hora(fin)}"
            else "${dia(inicio, conAnio)}, ${hora(inicio)} a ${dia(fin, conAnio)}, ${hora(fin)}"
        inicio != null -> "Desde el ${dia(inicio, conAnio)}, ${hora(inicio)}"
        fin != null -> "Hasta el ${dia(fin, conAnio)}, ${hora(fin)}"
        else -> null
    }
}

private fun dia(f: ZonedDateTime, conAnio: Boolean): String {
    val base = DateTimeFormatter.ofPattern("EEEE d 'de' MMM", LOCALE).format(f).let { if (it.endsWith(".")) it else "$it." }
    return if (conAnio) "$base de ${f.year}" else base
}

private fun hora(f: ZonedDateTime): String = DateTimeFormatter.ofPattern("HH:mm", LOCALE).format(f)
