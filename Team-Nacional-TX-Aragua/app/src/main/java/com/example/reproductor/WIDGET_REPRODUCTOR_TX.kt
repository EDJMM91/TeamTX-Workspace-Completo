package com.example.reproductor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import com.aistudio.teamtxvzla.R
import com.example.MainActivity

// ═══════════════════════════════════════════════════════════════════════════
// WIDGET DE ESCRITORIO ANDROID - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Control nativo de música desde la pantalla de inicio del teléfono.
// ═══════════════════════════════════════════════════════════════════════════

class WIDGET_REPRODUCTOR_TX : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            actualizarWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACCION_PLAY_PAUSA -> {
                GESTOR_AUDIO_TX.alternarPlayPausa()
                actualizarTodosLosWidgets(context)
            }
            ACCION_SIGUIENTE -> {
                GESTOR_AUDIO_TX.siguienteCancion()
                actualizarTodosLosWidgets(context)
            }
            ACCION_ANTERIOR -> {
                GESTOR_AUDIO_TX.anteriorCancion()
                actualizarTodosLosWidgets(context)
            }
        }
    }

    companion object {
        const val ACCION_PLAY_PAUSA = "com.example.reproductor.ACCION_PLAY_PAUSA"
        const val ACCION_SIGUIENTE = "com.example.reproductor.ACCION_SIGUIENTE"
        const val ACCION_ANTERIOR = "com.example.reproductor.ACCION_ANTERIOR"

        fun actualizarWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cancion = GESTOR_AUDIO_TX.cancionActual.value
            val esPlaying = GESTOR_AUDIO_TX.estado.value == EstadoReproductor.REPRODUCIENDO
            val posMs = GESTOR_AUDIO_TX.posicionActualMs.value
            val durMs = GESTOR_AUDIO_TX.duracionTotalMs.value
            val rutaCaratula = GESTOR_AUDIO_TX.caratulaActualRuta.value

            // PendingIntent para abrir la App al tocar el widget
            val intentApp = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntentApp = PendingIntent.getActivity(
                context,
                0,
                intentApp,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // PendingIntents para botones de control
            val intentPlay = Intent(context, WIDGET_REPRODUCTOR_TX::class.java).apply { action = ACCION_PLAY_PAUSA }
            val pIntentPlay = PendingIntent.getBroadcast(context, 1, intentPlay, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val intentSig = Intent(context, WIDGET_REPRODUCTOR_TX::class.java).apply { action = ACCION_SIGUIENTE }
            val pIntentSig = PendingIntent.getBroadcast(context, 2, intentSig, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val intentAnt = Intent(context, WIDGET_REPRODUCTOR_TX::class.java).apply { action = ACCION_ANTERIOR }
            val pIntentAnt = PendingIntent.getBroadcast(context, 3, intentAnt, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            try {
                val views = RemoteViews(context.packageName, R.layout.widget_reproductor_tx)

                // Textos
                views.setTextViewText(R.id.widget_song_title, cancion?.titulo ?: "Reproductor TX Pro")
                views.setTextViewText(R.id.widget_song_artist, cancion?.artista ?: "Team Nacional TX Aragua")

                // Icono Play / Pausa Dinámico
                views.setImageViewResource(
                    R.id.widget_btn_play_pause,
                    if (esPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                )

                // Carátula
                if (!rutaCaratula.isNullOrBlank()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(rutaCaratula)
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_play)
                        }
                    } catch (_: Exception) {
                        views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_play)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_play)
                }

                // Progreso
                val progreso = if (durMs > 0) ((posMs.toFloat() / durMs.toFloat()) * 1000).toInt() else 0
                views.setProgressBar(R.id.widget_progress_bar, 1000, progreso.coerceIn(0, 1000), false)

                // Clics
                views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntentApp)
                views.setOnClickPendingIntent(R.id.widget_info_container, pendingIntentApp)
                views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntentApp)
                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, pIntentPlay)
                views.setOnClickPendingIntent(R.id.widget_btn_next, pIntentSig)
                views.setOnClickPendingIntent(R.id.widget_btn_prev, pIntentAnt)

                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Exception) {
                Log.e("WIDGET_REPRODUCTOR_TX", "Error actualizando widget", e)
            }
        }

        fun actualizarTodosLosWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, WIDGET_REPRODUCTOR_TX::class.java))
                for (id in ids) {
                    actualizarWidget(context, appWidgetManager, id)
                }
            } catch (e: Exception) {
                Log.e("WIDGET_REPRODUCTOR_TX", "Error actualizando todos los widgets", e)
            }
        }
    }
}
