package ec.edu.puce.aruy.data.sesion

import com.google.gson.JsonParser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

/** Lee la fecha de vencimiento (`exp`) del token JWT, sin validar la firma (eso lo hace el servidor). */
object Jwt {

    /** Segundos desde 1970 en que vence el token, o null si no se puede leer. */
    fun vencimientoSegundos(token: String): Long? = runCatching {
        val carga = token.split(".")[1]
        val json = String(Base64.getUrlDecoder().decode(carga), Charsets.UTF_8)
        JsonParser.parseString(json).asJsonObject.get("exp").asLong
    }.getOrNull()

    /**
     * Solo se da por vencido un token cuya fecha SE PUDO leer y ya pasó. Si no se puede leer, no se descarta:
     * lo decide el servidor en la primera petición (así un error de lectura nunca saca a nadie de su cuenta).
     */
    fun estaVencido(token: String, ahoraMs: Long = System.currentTimeMillis()): Boolean {
        val exp = vencimientoSegundos(token) ?: return false
        return exp * 1000 <= ahoraMs
    }

    /** "12/11/2026 03:40" en la hora del teléfono, o null si el token no trae fecha. */
    fun vencimientoLegible(token: String): String? = vencimientoSegundos(token)?.let {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()))
    }
}
