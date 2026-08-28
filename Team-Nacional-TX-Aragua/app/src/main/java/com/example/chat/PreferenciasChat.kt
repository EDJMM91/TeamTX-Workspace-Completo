package com.example.chat

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Preferencias del Chat — Guarda configuraciones de sonido, notificaciones, tema, etc.
 * Persiste entre sesiones usando SharedPreferences.
 */
object PreferenciasChat {

    private const val TAG = "PREFERENCIAS_CHAT"
    private const val PREFS_NAME = "team_tx_chat_preferences"

    private var prefs: SharedPreferences? = null

    // Keys
    private const val KEY_SONIDO_MENSAJES = "sonido_mensajes"
    private const val KEY_VIBRACION = "vibracion"
    private const val KEY_NOTIFICACIONES_CANAL = "notificaciones_canal"
    private const val KEY_TEMA_CLARO = "tema_claro"
    private const val KEY_MOSTRAR_AVATARES = "mostrar_avatares"
    private const val KEY_ENVIAR_ENTER = "enviar_con_enter"
    private const val KEY_TAMANO_FUENTE = "tamano_fuente"

    fun inicializar(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.i(TAG, "✅ Preferencias del chat inicializadas")
    }

    // ═══════════════════════════════════════════════
    // SONIDOS
    // ═══════════════════════════════════════════════

    var sonidoMensajes: Boolean
        get() = prefs?.getBoolean(KEY_SONIDO_MENSAJES, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_SONIDO_MENSAJES, value)?.apply() }

    var vibracion: Boolean
        get() = prefs?.getBoolean(KEY_VIBRACION, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_VIBRACION, value)?.apply() }

    // ═══════════════════════════════════════════════
    // NOTIFICACIONES
    // ═══════════════════════════════════════════════

    var notificacionesCanal: Boolean
        get() = prefs?.getBoolean(KEY_NOTIFICACIONES_CANAL, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_NOTIFICACIONES_CANAL, value)?.apply() }

    // ═══════════════════════════════════════════════
    // APARIENCIA
    // ═══════════════════════════════════════════════

    var temaClaro: Boolean
        get() = prefs?.getBoolean(KEY_TEMA_CLARO, false) ?: false
        set(value) { prefs?.edit()?.putBoolean(KEY_TEMA_CLARO, value)?.apply() }

    var mostrarAvatares: Boolean
        get() = prefs?.getBoolean(KEY_MOSTRAR_AVATARES, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_MOSTRAR_AVATARES, value)?.apply() }

    // ═══════════════════════════════════════════════
    // ENVÍO
    // ═══════════════════════════════════════════════

    var enviarConEnter: Boolean
        get() = prefs?.getBoolean(KEY_ENVIAR_ENTER, false) ?: false
        set(value) { prefs?.edit()?.putBoolean(KEY_ENVIAR_ENTER, value)?.apply() }

    var tamanoFuente: Float
        get() = prefs?.getFloat(KEY_TAMANO_FUENTE, 14f) ?: 14f
        set(value) { prefs?.edit()?.putFloat(KEY_TAMANO_FUENTE, value)?.apply() }

    // ═══════════════════════════════════════════════
    // REPRODUCIR SONIDOS
    // ═══════════════════════════════════════════════

    private var toneGenerator: ToneGenerator? = null

    private fun getToneGenerator(): ToneGenerator {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        }
        return toneGenerator!!
    }

    /**
     * Sonido de mensaje enviado (tono corto y agudo)
     */
    fun sonidoMensajeEnviado(context: Context) {
        if (!sonidoMensajes) return
        try {
            val tg = getToneGenerator()
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 60)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo sonido enviado: ${e.message}")
        }
    }

    /**
     * Sonido de mensaje recibido (tono suave)
     */
    fun sonidoMensajeRecibido(context: Context) {
        if (!sonidoMensajes) return
        try {
            val tg = getToneGenerator()
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo sonido recibido: ${e.message}")
        }
    }

    /**
     * Sonido de notificación de radio
     */
    fun sonidoRadio(context: Context) {
        if (!sonidoMensajes) return
        try {
            val tg = getToneGenerator()
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo sonido radio: ${e.message}")
        }
    }

    /**
     * Sonido de sticker enviado
     */
    fun sonidoSticker(context: Context) {
        if (!sonidoMensajes) return
        try {
            val tg = getToneGenerator()
            tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 50)
        } catch (e: Exception) {
            Log.w(TAG, "Error reproduciendo sonido sticker: ${e.message}")
        }
    }

    /**
     * Liberar recursos del ToneGenerator
     */
    fun liberar() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando ToneGenerator: ${e.message}")
        }
    }
}
