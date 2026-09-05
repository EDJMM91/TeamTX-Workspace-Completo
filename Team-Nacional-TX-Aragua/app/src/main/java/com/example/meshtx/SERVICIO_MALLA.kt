package com.example.meshtx

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.aistudio.teamtxvzla.R

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SERVICIO PERSISTENTE DE MALLA TÁCTICA (ANTI DOZE MODE) - PASO 3
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidades:
 * 1. Anclar el intercomunicador como Foreground Service en Android.
 * 2. Prevenir que Android Doze Mode y Battery Saver suspendan los sockets UDP
 *    y los hilos de audio al apagar la pantalla o guardar el teléfono en el bolsillo.
 * 3. Mantener un Partial WakeLock activo mientras la malla esté en uso.
 * 4. Mostrar una notificación persistente de estado con acceso rápido al PTT.
 *
 * 100% OFFLINE - Cero dependencias externas.
 */
class ServicioMallaTx : Service() {

    private val etiquetaLog = "MeshTX_Servicio"
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CANAL_NOTIFICACION_ID = "canal_mesh_tactico_tx"
        const val ID_NOTIFICACION = 58200

        const val ACCION_INICIAR = "com.example.meshtx.ACCION_INICIAR"
        const val ACCION_DETENER = "com.example.meshtx.ACCION_DETENER"
        const val ACCION_ACTUALIZAR_ESTADO = "com.example.meshtx.ACCION_ACTUALIZAR_ESTADO"
        const val EXTRA_TEXTO_ESTADO = "extra_texto_estado"

        @Volatile
        var estaEnEjecucion: Boolean = false
            private set

        fun iniciar(contexto: Context) {
            val intent = Intent(contexto, ServicioMallaTx::class.java).apply {
                action = ACCION_INICIAR
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    contexto.startForegroundService(intent)
                } else {
                    contexto.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("MeshTX_Servicio", "Error al iniciar ServicioMallaTx: ${e.message}")
            }
        }

        fun detener(contexto: Context) {
            val intent = Intent(contexto, ServicioMallaTx::class.java).apply {
                action = ACCION_DETENER
            }
            try {
                contexto.startService(intent)
            } catch (e: Exception) {
                Log.e("MeshTX_Servicio", "Error al detener ServicioMallaTx: ${e.message}")
            }
        }

        fun actualizarEstado(contexto: Context, estado: String) {
            if (!estaEnEjecucion) return
            val intent = Intent(contexto, ServicioMallaTx::class.java).apply {
                action = ACCION_ACTUALIZAR_ESTADO
                putExtra(EXTRA_TEXTO_ESTADO, estado)
            }
            try {
                contexto.startService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        adquirirWakeLock()
        estaEnEjecucion = true
        Log.i(etiquetaLog, "ServicioMallaTx creado y WakeLock adquirido.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACCION_DETENER -> {
                Log.i(etiquetaLog, "Orden de detención recibida en ServicioMallaTx.")
                detenerForegroundYFinalizar()
                return START_NOT_STICKY
            }
            ACCION_ACTUALIZAR_ESTADO -> {
                val nuevoTexto = intent.getStringExtra(EXTRA_TEXTO_ESTADO) ?: "Enlazado a la malla"
                actualizarNotificacionVisual(nuevoTexto)
            }
            else -> {
                val notificacion = construirNotificacion("Intercomunicador Activo • Escuchando Malla")
                iniciarEnPrimerPlano(notificacion)
            }
        }
        return START_STICKY
    }

    private fun iniciarEnPrimerPlano(notificacion: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requiere tipos específicos declarados en manifest
                startForeground(
                    ID_NOTIFICACION,
                    notificacion,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    ID_NOTIFICACION,
                    notificacion,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                )
            } else {
                startForeground(ID_NOTIFICACION, notificacion)
            }
            Log.i(etiquetaLog, "Servicio anclado a primer plano exitosamente (Anti-Doze ON).")
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Excepción en startForeground: ${e.message}")
        }
    }

    private fun actualizarNotificacionVisual(textoEstado: String) {
        val notificacion = construirNotificacion(textoEstado)
        val gestor = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        gestor?.notify(ID_NOTIFICACION, notificacion)
    }

    private fun construirNotificacion(textoEstado: String): Notification {
        val intentApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flagsPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intentApp, flagsPending)

        return NotificationCompat.Builder(this, CANAL_NOTIFICACION_ID)
            .setSmallIcon(R.drawable.logoteam)
            .setContentTitle("Team TX • Malla Táctica Biker")
            .setContentText(textoEstado)
            .setSubText("Intercomunicador Mesh")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_NOTIFICACION_ID,
                "Intercomunicador Malla TX",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el intercomunicador táctico y audio activo en segundo plano y con pantalla apagada"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val gestor = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            gestor?.createNotificationChannel(canal)
        }
    }

    private fun adquirirWakeLock() {
        try {
            val gestorPoder = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = gestorPoder?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TeamTX:MeshAntiDozeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 60 * 1000L) // 10 horas máximo por sesión de rodada
            }
            Log.d(etiquetaLog, "Partial WakeLock adquirido para supervivencia de corrutinas.")
        } catch (e: Exception) {
            Log.w(etiquetaLog, "No se pudo adquirir WakeLock: ${e.message}")
        }
    }

    private fun liberarWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun detenerForegroundYFinalizar() {
        estaEnEjecucion = false
        liberarWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        Log.i(etiquetaLog, "ServicioMallaTx detenido y recursos liberados.")
    }

    override fun onDestroy() {
        detenerForegroundYFinalizar()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
