package ec.edu.puce.aruy.notificaciones

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Enciende y apaga la consulta periódica de solicitudes: corre mientras haya una sesión iniciada. */
class ProgramadorAvisos(private val contexto: Context) {

    fun activar() {
        val peticion = PeriodicWorkRequestBuilder<SolicitudesWorker>(INTERVALO_MINUTOS, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // KEEP: si ya está programada, no se reinicia el reloj cada vez que se abre la app
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(NOMBRE, ExistingPeriodicWorkPolicy.KEEP, peticion)
    }

    fun desactivar() {
        WorkManager.getInstance(contexto).cancelUniqueWork(NOMBRE)
    }

    private companion object {
        const val NOMBRE = "solicitudes_aruy"
        const val INTERVALO_MINUTOS = 15L // el mínimo que permite Android
    }
}
