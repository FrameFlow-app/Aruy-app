package ec.edu.puce.aruy.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ec.edu.puce.aruy.MainActivity
import ec.edu.puce.aruy.R
import ec.edu.puce.aruy.domain.solicitudes.TextoAviso
import ec.edu.puce.aruy.ui.navigation.Rutas

/** Avisos del sistema: "Nueva solicitud" y "Respondieron tu solicitud". Al tocarlos se abre Mis solicitudes. */
class NotificadorSolicitudes(private val contexto: Context) {

    init {
        crearCanal()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= 26) {
            val canal = NotificationChannel(CANAL, "Solicitudes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos cuando te piden un servicio o responden tu solicitud"
            }
            contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    /** En Android 13 o más hace falta el permiso; además, la persona puede haber apagado las notificaciones de Aruy. */
    fun puedeNotificar(): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(contexto).areNotificationsEnabled()

    /** Si la persona está usando la app, no hace falta una notificación: ya ve el aviso dentro de la app. */
    fun enPrimerPlano(): Boolean {
        val administrador = contexto.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val yo = administrador.runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() } ?: return false
        return yo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    @SuppressLint("MissingPermission") // se comprueba en puedeNotificar()
    fun mostrar(aviso: TextoAviso) {
        if (!puedeNotificar()) return
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ABRIR, Rutas.SOLICITUDES)
        }
        val pendiente = PendingIntent.getActivity(
            contexto, ID_BASE + aviso.clave, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.texto))
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(contexto).notify(ID_BASE + aviso.clave, notificacion)
        } catch (e: SecurityException) {
            // el permiso se retiró justo ahora: no pasa nada
        }
    }

    private companion object {
        const val CANAL = "solicitudes"
        const val ID_BASE = 7100
    }
}
