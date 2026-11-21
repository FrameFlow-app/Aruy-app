package ec.edu.puce.aruy.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.almacenAvisosVistos: DataStore<Preferences> by preferencesDataStore(name = "avisos_vistos")

/** Recuerda qué aviso de mantenimiento ya se leyó y qué versión nueva se dejó para después. */
class AvisosVistosStore(private val contexto: Context) {
    private val claveMantenimiento = stringPreferencesKey("mantenimiento_visto")
    private val claveVersion = intPreferencesKey("version_omitida")

    suspend fun mantenimientoVisto(): String? = contexto.almacenAvisosVistos.data.first()[claveMantenimiento]

    suspend fun guardarMantenimientoVisto(huella: String) {
        contexto.almacenAvisosVistos.edit { it[claveMantenimiento] = huella }
    }

    suspend fun versionOmitida(): Int = contexto.almacenAvisosVistos.data.first()[claveVersion] ?: 0

    suspend fun guardarVersionOmitida(version: Int) {
        contexto.almacenAvisosVistos.edit { it[claveVersion] = version }
    }
}
