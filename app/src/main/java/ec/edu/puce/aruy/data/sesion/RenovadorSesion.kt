package ec.edu.puce.aruy.data.sesion

import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.AuthApi
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.Sesion
import kotlinx.coroutines.CancellationException

/**
 * Sesión que se renueva sola: al abrir la app, si al token le quedan menos de 20 de sus 30 días, se pide uno nuevo
 * (`POST /auth/renovar`). Así quien usa Aruy seguido nunca tiene que volver a escribir su contraseña.
 * Si no hay internet o el servidor falla no pasa nada: la sesión actual sigue valiendo.
 */
class RenovadorSesion(
    private val api: AuthApi,
    private val sesionManager: SesionManager,
    private val ahoraMs: () -> Long = System::currentTimeMillis
) {
    suspend fun renovarSiHaceFalta() {
        val actual = (sesionManager.estado.value as? EstadoSesion.Autenticada)?.sesion ?: return
        val vence = Jwt.vencimientoSegundos(actual.token) ?: return
        if (vence * 1000 - ahoraMs() > UMBRAL_MS) return
        try {
            val respuesta = api.renovar()
            sesionManager.iniciar(Sesion(respuesta.token, respuesta.usuario.toDomain(), respuesta.perfilPrestadorId))
            RegistroSesion.info("Sesion renovada por 30 dias mas")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RegistroSesion.aviso("No se pudo renovar la sesion (${e.javaClass.simpleName}); sigue valiendo la actual")
        }
    }

    companion object {
        /** Se renueva cuando faltan menos de 20 días para que venza. */
        const val UMBRAL_MS = 20L * 24 * 3600 * 1000
    }
}
