package ec.edu.puce.aruy.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre WhatsApp con [numero] (formato internacional sin "+") y un [mensaje] opcional. false si no se pudo abrir. */
fun abrirWhatsApp(contexto: Context, numero: String, mensaje: String = ""): Boolean = try {
    val enlace = if (mensaje.isBlank()) "https://wa.me/$numero" else "https://wa.me/$numero?text=${Uri.encode(mensaje)}"
    contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
}
