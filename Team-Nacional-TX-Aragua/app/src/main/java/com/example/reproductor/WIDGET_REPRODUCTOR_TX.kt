package com.example.reproductor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

            // Si no hay layout XML de widget disponible, creamos un RemoteViews básico seguro
            try {
                val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_2)
                views.setTextViewText(android.R.id.text1, cancion?.titulo ?: "Reproductor TX Pro")
                views.setTextViewText(android.R.id.text2, if (cancion != null) "${cancion.artista} • ${if (esPlaying) "▶ Reproduciendo" else "⏸ En pausa"}" else "Toca para abrir música")
                views.setOnClickPendingIntent(android.R.id.text1, pendingIntentApp)
                views.setOnClickPendingIntent(android.R.id.text2, pIntentPlay)
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (_: Exception) {}
        }

        fun actualizarTodosLosWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, WIDGET_REPRODUCTOR_TX::class.java))
            for (id in ids) {
                actualizarWidget(context, appWidgetManager, id)
            }
        }
    }
}
