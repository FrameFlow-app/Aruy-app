package ec.edu.puce.aruy.notificaciones

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.almacenAvisos: DataStore<Preferences> by preferencesDataStore(name = "avisos_solicitudes")

/** Cuántas novedades había la última vez que se avisó, para no repetir la misma notificación cada 15 minutos. */
class AvisosSolicitudesStore(private val contexto: Context) {

    private val claveRecibidas = intPreferencesKey("recibidas")
    private val claveEnviadas = intPreferencesKey("enviadas")

    private val claveMensajes = intPreferencesKey("mensajes")

    /** (recibidas nuevas, respuestas nuevas, mensajes sin leer) de la última vez. */
    suspend fun leer(): Triple<Int, Int, Int> {
        val p = contexto.almacenAvisos.data.first()
        return Triple(p[claveRecibidas] ?: 0, p[claveEnviadas] ?: 0, p[claveMensajes] ?: 0)
    }

    suspend fun guardar(recibidas: Int, enviadas: Int, mensajes: Int = 0) {
        contexto.almacenAvisos.edit {
            it[claveRecibidas] = recibidas
            it[claveEnviadas] = enviadas
            it[claveMensajes] = mensajes
        }
    }

    suspend fun borrar() {
        contexto.almacenAvisos.edit { it.clear() }
    }
}
