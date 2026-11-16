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

    /** (recibidas nuevas, respuestas nuevas) de la última vez. */
    suspend fun leer(): Pair<Int, Int> {
        val p = contexto.almacenAvisos.data.first()
        return (p[claveRecibidas] ?: 0) to (p[claveEnviadas] ?: 0)
    }

    suspend fun guardar(recibidas: Int, enviadas: Int) {
        contexto.almacenAvisos.edit {
            it[claveRecibidas] = recibidas
            it[claveEnviadas] = enviadas
        }
    }

    suspend fun borrar() {
        contexto.almacenAvisos.edit { it.clear() }
    }
}
