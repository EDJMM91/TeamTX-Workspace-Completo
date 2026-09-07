package com.example.reproductor

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.teamtxvzla.R
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

// ═══════════════════════════════════════════════════════════════════════════
// FOREGROUND SERVICE - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Mantiene viva la reproducción, controles lockscreen y enrutamiento Bluetooth.
// ═══════════════════════════════════════════════════════════════════════════

class ReproductorForegroundService : Service() {

    companion object {
        const val ETIQUETA = "TEAM_TX_REPRODUCTOR"
        const val CANAL_ID = "canal_reproductor_tx_pro"
        const val NOTIFICACION_ID = 2001

        const val ACCION_INICIAR = "com.example.reproductor.INICIAR"
        const val ACCION_PLAY_PAUSA = "com.example.reproductor.PLAY_PAUSA"
        const val ACCION_SIGUIENTE = "com.example.reproductor.SIGUIENTE"
        const val ACCION_ANTERIOR = "com.example.reproductor.ANTERIOR"
        const val ACCION_DETENER = "com.example.reproductor.DETENER"

        fun iniciarServicio(context: Context) {
            try {
                val intent = Intent(context, ReproductorForegroundService::class.java).apply {
                    action = ACCION_INICIAR
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(ETIQUETA, "Error al iniciar ReproductorForegroundService: ${e.message}")
            }
        }

        fun detenerServicio(context: Context) {
            try {
                val intent = Intent(context, ReproductorForegroundService::class.java).apply {
                    action = ACCION_DETENER
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(ETIQUETA, "Error al detener ReproductorForegroundService: ${e.message}")
            }
        }
    }

    private val scopeServicio = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSessionHelper: MediaSessionHelper? = null
    private var becomingNoisyReceiver: BroadcastReceiver? = null
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        crearCanalNotificacion()

        mediaSessionHelper = MediaSessionHelper(this, object : MediaSessionHelper.MediaSessionCallback {
            override fun onPlay() {
                GESTOR_AUDIO_TX.alternarPlayPausa()
            }

            override fun onPause() {
                GESTOR_AUDIO_TX.alternarPlayPausa()
            }

            override fun onSkipToNext() {
                GESTOR_AUDIO_TX.siguienteCancion()
            }

            override fun onSkipToPrevious() {
                GESTOR_AUDIO_TX.anteriorCancion()
            }

            override fun onSeekTo(posMs: Long) {
                GESTOR_AUDIO_TX.buscarPosicion(posMs)
            }

            override fun onStop() {
                GESTOR_AUDIO_TX.detener()
                detenerForegroundYFinalizar()
            }
        })

        registrarBecomingNoisy()
        observarEstadoReproductor()

        Log.d(ETIQUETA, "ReproductorForegroundService creado con éxito")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val accion = intent?.action
        Log.d(ETIQUETA, "ReproductorForegroundService onStartCommand: $accion")

        when (accion) {
            ACCION_PLAY_PAUSA -> GESTOR_AUDIO_TX.alternarPlayPausa()
            ACCION_SIGUIENTE -> GESTOR_AUDIO_TX.siguienteCancion()
            ACCION_ANTERIOR -> GESTOR_AUDIO_TX.anteriorCancion()
            ACCION_DETENER -> {
                GESTOR_AUDIO_TX.detener()
                detenerForegroundYFinalizar()
                return START_NOT_STICKY
            }
        }

        // Mostrar notificación inicial para cumplir el contrato Foreground
        actualizarNotificacion(
            GESTOR_AUDIO_TX.cancionActual.value,
            GESTOR_AUDIO_TX.estado.value,
            GESTOR_AUDIO_TX.posicionActualMs.value
        )

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observarEstadoReproductor() {
        // Observar canción actual
        scopeServicio.launch {
            GESTOR_AUDIO_TX.cancionActual.collectLatest { cancion ->
                val estado = GESTOR_AUDIO_TX.estado.value
                val pos = GESTOR_AUDIO_TX.posicionActualMs.value
                actualizarNotificacion(cancion, estado, pos)
            }
        }

        // Observar estado de reproducción
        scopeServicio.launch {
            GESTOR_AUDIO_TX.estado.collectLatest { estado ->
                val cancion = GESTOR_AUDIO_TX.cancionActual.value
                val pos = GESTOR_AUDIO_TX.posicionActualMs.value
                actualizarNotificacion(cancion, estado, pos)

                if (estado == EstadoReproductor.DETENIDO) {
                    // Si se detiene y no hay nada sonando, permitir remover foreground
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }
    }

    private fun actualizarNotificacion(
        cancion: CancionMotera?,
        estado: EstadoReproductor,
        posicionMs: Long
    ) {
        val caratulaBitmap = cancion?.let { cargarCaratula(it) }

        // Actualizar MediaSession
        mediaSessionHelper?.actualizarMetadatos(cancion, caratulaBitmap)
        mediaSessionHelper?.actualizarEstado(estado, posicionMs)

        val notificacion = construirNotificacion(cancion, estado, caratulaBitmap)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICACION_ID,
                    notificacion,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICACION_ID, notificacion)
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error al llamar startForeground: ${e.message}")
            notificationManager?.notify(NOTIFICACION_ID, notificacion)
        }
    }

    private fun construirNotificacion(
        cancion: CancionMotera?,
        estado: EstadoReproductor,
        caratulaBitmap: Bitmap?
    ): Notification {
        val titulo = cancion?.titulo ?: "Reproductor TX Pro"
        val artista = cancion?.artista ?: "Team TX Aragua"
        val album = cancion?.album ?: "Música Biker"
        val esPlaying = estado == EstadoReproductor.REPRODUCIENDO

        // Intent para abrir la aplicación al presionar la notificación
        val intentApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentApp = PendingIntent.getActivity(
            this,
            0,
            intentApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntents de acciones
        val piPrev = crearPendingIntentAccion(ACCION_ANTERIOR, 1)
        val piPlayPausa = crearPendingIntentAccion(ACCION_PLAY_PAUSA, 2)
        val piNext = crearPendingIntentAccion(ACCION_SIGUIENTE, 3)
        val piStop = crearPendingIntentAccion(ACCION_DETENER, 4)

        val iconoPlayPausa = if (esPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val textoPlayPausa = if (esPlaying) "Pausar" else "Reproducir"

        val builder = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.logoteam)
            .setContentTitle(titulo)
            .setContentText(artista)
            .setSubText(album)
            .setContentIntent(pendingIntentApp)
            .setOngoing(esPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "Anterior", piPrev)
            .addAction(iconoPlayPausa, textoPlayPausa, piPlayPausa)
            .addAction(android.R.drawable.ic_media_next, "Siguiente", piNext)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cerrar", piStop)

        caratulaBitmap?.let {
            builder.setLargeIcon(it)
        }

        return builder.build()
    }

    private fun crearPendingIntentAccion(accion: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ReproductorForegroundService::class.java).apply {
            action = accion
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cargarCaratula(cancion: CancionMotera): Bitmap? {
        return try {
            cancion.portadaUriStr?.let { uriStr ->
                val archivo = File(uriStr)
                if (archivo.exists()) {
                    BitmapFactory.decodeFile(archivo.absolutePath)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Reproductor TX Pro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción continua en segundo plano y pantalla de bloqueo"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(canal)
        }
    }

    private fun registrarBecomingNoisy() {
        becomingNoisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    Log.d(ETIQUETA, "Auriculares desconectados: pausando reproducción")
                    if (GESTOR_AUDIO_TX.estado.value == EstadoReproductor.REPRODUCIENDO) {
                        GESTOR_AUDIO_TX.alternarPlayPausa()
                    }
                }
            }
        }
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, filter)
    }

    private fun detenerForegroundYFinalizar() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scopeServicio.cancel()
        becomingNoisyReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        mediaSessionHelper?.liberar()
        mediaSessionHelper = null
        Log.d(ETIQUETA, "ReproductorForegroundService destruido limpiamente")
    }
}
