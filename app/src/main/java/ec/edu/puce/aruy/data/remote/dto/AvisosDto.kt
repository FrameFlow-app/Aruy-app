package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Respuesta de `GET /avisos` (público): mantenimiento programado y versiones de la app. */
data class AvisosDto(
    @SerializedName("mantenimiento") val mantenimiento: MantenimientoDto? = null,
    @SerializedName("version") val version: VersionAppDto? = null
)

data class MantenimientoDto(
    @SerializedName("activo") val activo: Boolean = false,
    @SerializedName("mensaje") val mensaje: String? = null,
    @SerializedName("inicio") val inicio: String? = null,
    @SerializedName("fin") val fin: String? = null
)

data class VersionAppDto(
    @SerializedName("reciente") val reciente: Int = 0,
    @SerializedName("minima") val minima: Int = 0,
    @SerializedName("notas") val notas: String? = null
)
