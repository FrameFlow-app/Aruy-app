package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.sesion.AlmacenSesion
import ec.edu.puce.aruy.domain.model.Sesion
import java.util.Base64

/** Almacén en memoria para probar sin Android. */
class AlmacenEnMemoria(var guardada: Sesion? = null) : AlmacenSesion {
    override suspend fun leer(): Sesion? = guardada
    override suspend fun guardar(sesion: Sesion) { guardada = sesion }
    override suspend fun borrar() { guardada = null }
}

/** Un almacén donde SÍ había una sesión pero no se pudo abrir (por ejemplo, la clave del Keystore). */
class AlmacenIlegible(private val motivo: String) : AlmacenSesion {
    override suspend fun leer(): Sesion? = null
    override suspend fun guardar(sesion: Sesion) = Unit
    override suspend fun borrar() = Unit
    override fun motivoDelUltimoFallo(): String? = motivo
}

/** Un almacén cuya lectura lanza un error (disco dañado, por ejemplo). */
class AlmacenQueFalla : AlmacenSesion {
    override suspend fun leer(): Sesion? = throw java.io.IOException("disco dañado")
    override suspend fun guardar(sesion: Sesion) = Unit
    override suspend fun borrar() = Unit
}

/** Token con la forma de un JWT (la firma no importa: la app no la valida). */
fun tokenDePrueba(vencimientoSegundos: Long): String {
    val codificador = Base64.getUrlEncoder().withoutPadding()
    val cabecera = codificador.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
    val carga = codificador.encodeToString(
        """{"rol":"PRESTADOR","sub":"u1","iat":1,"exp":$vencimientoSegundos}""".toByteArray()
    )
    return "$cabecera.$carga.firma"
}
