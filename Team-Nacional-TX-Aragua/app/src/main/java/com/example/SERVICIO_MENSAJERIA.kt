package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.teamtxvzla.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SERVICIO_MENSAJERIA : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_SERVICIO"
        private const val CANAL_ID = "team_tx_channel"
        private const val CANAL_NOMBRE = "Canal Principal Team TX"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "  NUEVO TOKEN FCM GENERADO")
        Log.d(TAG, "  Token: $token")
        Log.d(TAG, "═══════════════════════════════════════")
        guardarTokenEnFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje recibido desde: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Carga útil de datos: ${remoteMessage.data}")
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Notificación push: ${it.title} - ${it.body}")
            mostrarNotificacionPush(
                it.title ?: "Team TX Aragua",
                it.body ?: "",
                remoteMessage.data
            )
        }
    }

    private fun guardarTokenEnFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w(TAG, "No hay usuario autenticado, token no guardado")
            return
        }

        val datos = mapOf(
            "fcmToken" to token,
            "fcmTokenTimestamp" to System.currentTimeMillis(),
            "deviceId" to Build.MODEL,
            "deviceBrand" to Build.BRAND,
            "androidVersion" to Build.VERSION.RELEASE
        )

        FirebaseFirestore.getInstance()
            .collection("fcm_tokens")
            .document(uid)
            .set(datos)
            .addOnSuccessListener {
                Log.i(TAG, "✅ Token FCM guardado en Firestore: fcm_tokens/$uid")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error guardando token FCM", e)
            }
    }

    private fun mostrarNotificacionPush(titulo: String, cuerpo: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val constructorNotificacion = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.logoteam)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val gestorNotificaciones = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones principales del Team TX"
                enableLights(true)
                enableVibration(true)
            }
            gestorNotificaciones.createNotificationChannel(canal)
        }

        gestorNotificaciones.notify(System.currentTimeMillis().toInt(), constructorNotificacion.build())
    }
}
