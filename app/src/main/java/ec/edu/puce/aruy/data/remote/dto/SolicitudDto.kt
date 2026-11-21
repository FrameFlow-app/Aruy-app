package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ContraparteDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("foto_perfil") val fotoPerfil: String? = null,
    @SerializedName("telefono") val telefono: String? = null
)

/** Una solicitud, como la entrega el servidor (`/solicitudes`). */
data class SolicitudDto(
    @SerializedName("id") val id: String,
    @SerializedName("servicio_id") val servicioId: String? = null,
    @SerializedName("servicio_titulo") val servicioTitulo: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("mensaje") val mensaje: String? = null,
    @SerializedName("respuesta") val respuesta: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null,
    @SerializedName("fecha_actualizacion") val fechaActualizacion: String? = null,
    @SerializedName("sin_ver") val sinVer: Boolean = false,
    @SerializedName("contraparte") val contraparte: ContraparteDto? = null,
    @SerializedName("whatsapp_compartido") val whatsappCompartido: Boolean = false,
    @SerializedName("mensajes_sin_leer") val mensajesSinLeer: Int = 0,
    @SerializedName("ultimo_mensaje") val ultimoMensaje: UltimoMensajeDto? = null
)

data class UltimoMensajeDto(
    @SerializedName("texto") val texto: String? = null,
    @SerializedName("fecha") val fecha: String? = null
)

/** Un mensaje del chat de una solicitud. `tipo`: "TEXTO" o "WHATSAPP". */
data class MensajeDto(
    @SerializedName("id") val id: String,
    @SerializedName("autor_id") val autorId: String? = null,
    @SerializedName("es_mio") val esMio: Boolean = false,
    @SerializedName("texto") val texto: String? = null,
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null
)

data class EnviarMensajeRequest(@SerializedName("texto") val texto: String)

data class CrearSolicitudRequest(
    @SerializedName("servicio_id") val servicioId: String,
    @SerializedName("mensaje") val mensaje: String
)

data class CambiarEstadoSolicitudRequest(
    @SerializedName("estado") val estado: String,
    @SerializedName("respuesta") val respuesta: String? = null
)

data class ResumenSolicitudesDto(
    @SerializedName("recibidas_nuevas") val recibidasNuevas: Int = 0,
    @SerializedName("recibidas_pendientes") val recibidasPendientes: Int = 0,
    @SerializedName("enviadas_novedades") val enviadasNovedades: Int = 0,
    @SerializedName("mensajes_sin_leer") val mensajesSinLeer: Int = 0
)

data class MarcarVistasRequest(@SerializedName("tipo") val tipo: String)

data class MarcadasDto(@SerializedName("marcadas") val marcadas: Int = 0)
