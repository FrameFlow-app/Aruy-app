package ec.edu.puce.aruy.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ec.edu.puce.aruy.AruyApplication
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.solicitudes.PoliticaAvisos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cada ~15 minutos (aunque la app esté cerrada) pregunta al servidor si hay solicitudes nuevas y, si las hay,
 * muestra una notificación. No usa Firebase: es una consulta ligera que solo corre con internet.
 */
class SolicitudesWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as AruyApplication).contenedor
        // Si el proceso arrancó solo para esta tarea, la sesión guardada todavía se está leyendo
        val estado = withTimeoutOrNull(ESPERA_SESION_MS) { c.sesionManager.estado.first { it !is EstadoSesion.Cargando } }
            ?: return Result.retry()
        if (estado !is EstadoSesion.Autenticada) return Result.success()

        val resumen = try {
            c.obtenerResumenSolicitudesCaso()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.retry()
        }

        val (ultimasRecibidas, ultimasEnviadas) = c.avisosSolicitudesStore.leer()
        if (!c.notificadorSolicitudes.enPrimerPlano()) {
            PoliticaAvisos.textos(resumen, ultimasRecibidas, ultimasEnviadas).forEach(c.notificadorSolicitudes::mostrar)
        }
        c.avisosSolicitudesStore.guardar(resumen.recibidasNuevas, resumen.enviadasNovedades)
        return Result.success()
    }

    private companion object {
        const val ESPERA_SESION_MS = 15_000L
    }
}
