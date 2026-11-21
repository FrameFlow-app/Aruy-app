package ec.edu.puce.aruy.domain.model

/** TEXTO es un mensaje normal; WHATSAPP es el aviso de que el prestador compartió su número (el texto es el número). */
enum class TipoMensaje {
    TEXTO, WHATSAPP;

    companion object {
        fun desde(texto: String?): TipoMensaje = if (texto == "WHATSAPP") WHATSAPP else TEXTO
    }
}

/** Un mensaje del chat de una solicitud. [fecha] viene en formato ISO. */
data class Mensaje(
    val id: String,
    val esMio: Boolean,
    val texto: String,
    val tipo: TipoMensaje,
    val fecha: String?
)
