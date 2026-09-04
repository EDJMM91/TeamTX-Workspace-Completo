package com.example.reproductor

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log

// ═══════════════════════════════════════════════════════════════════════════
// PUENTE JNI Y MOTOR DSP - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Integra funciones nativas C++ con el subsistema hardware AudioFX de Android.
// ═══════════════════════════════════════════════════════════════════════════

object MOTOR_AUDIO_NATIVO {

    private const val ETIQUETA_LOG = "TEAM_TX_MOTOR_AUDIO"
    private var libreriaNativaCargada = false

    // Efectos de hardware integrados en el audio pipeline de Android
    private var ecualizadorHardware: Equalizer? = null
    private var bassBoostHardware: BassBoost? = null
    private var virtualizadorHardware: Virtualizer? = null
    private var ultraVolumenHardware: LoudnessEnhancer? = null
    private var ultimoAudioSessionId: Int = 0

    init {
        try {
            System.loadLibrary("motor_audio_tx")
            libreriaNativaCargada = true
            Log.d(ETIQUETA_LOG, "✅ Librería nativa C++ motor_audio_tx cargada exitosamente.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(ETIQUETA_LOG, "⚠️ Librería nativa C++ no disponible aún en binarios empaquetados. Usando pipeline DSP AudioFX de alta fidelidad: ${e.message}")
            libreriaNativaCargada = false
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error al inicializar motor nativo: ${e.message}")
            libreriaNativaCargada = false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FUNCIONES JNI NATIVAS (C++)
    // ─────────────────────────────────────────────────────────────────────────

    @JvmStatic
    external fun procesarSuperBass(
        muestras: ShortArray,
        cantidadMuestras: Int,
        nivelGraves: Float,
        frecuenciaMuestreo: Int
    )

    @JvmStatic
    external fun procesarUltraVolumen(
        muestras: ShortArray,
        cantidadMuestras: Int,
        factorGanancia: Float
    )

    @JvmStatic
    external fun procesarEcualizador(
        muestras: ShortArray,
        cantidadMuestras: Int,
        gananciasBandaDb: FloatArray,
        frecuenciaMuestreo: Int
    )

    @JvmStatic
    external fun procesarEspacialidad(
        muestras: ShortArray,
        cantidadMuestras: Int,
        nivelEspacialidad: Float
    )

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE EFECTOS EN TIEMPO REAL (HARDWARE AUDIOFX & DSP)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vincula y configura el pipeline de efectos al AudioSessionId del reproductor.
     */
    fun vincularAudioSession(audioSessionId: Int, config: ConfiguracionReproductor) {
        if (audioSessionId <= 0 || (audioSessionId == ultimoAudioSessionId && ecualizadorHardware != null)) return
        liberarEfectos()
        ultimoAudioSessionId = audioSessionId

        try {
            // 1. Ecualizador Multibanda
            ecualizadorHardware = Equalizer(0, audioSessionId).apply {
                enabled = true
                aplicarBandasEcualizador(config.bandasEcualizador)
            }

            // 2. Super Bass (Bass Boost)
            bassBoostHardware = BassBoost(0, audioSessionId).apply {
                enabled = true
                aplicarSuperBass(config.superBassNivel)
            }

            // 3. Espacialidad 3D (Virtualizer)
            virtualizadorHardware = Virtualizer(0, audioSessionId).apply {
                enabled = true
                aplicarEspacialidad(config.espacialidadNivel)
            }

            // 4. Ultra Volumen (Loudness Enhancer en Android 4.4+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ultraVolumenHardware = LoudnessEnhancer(audioSessionId).apply {
                    enabled = true
                    aplicarUltraVolumen(config.ultraVolumenNivel)
                }
            }

            Log.d(ETIQUETA_LOG, "🎛️ Pipeline de Efectos Biker Pro vinculado a sesión: $audioSessionId")
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error al vincular AudioFX: ${e.message}")
        }
    }

    /**
     * Aplica ganancia de Ultra Volumen (+100% hasta +300%).
     */
    fun aplicarUltraVolumen(factorGanancia: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && ultraVolumenHardware != null) {
                // Ganancia en mili-decibelios (mB): Factor 1.0 = 0mB, Factor 3.0 = ~1200mB (+12dB)
                val gananciaMb = if (factorGanancia <= 1.0f) 0 else ((factorGanancia - 1.0f) * 600.0f).toInt()
                ultraVolumenHardware?.setTargetGain(gananciaMb)
                ultraVolumenHardware?.enabled = factorGanancia > 1.01f
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error al aplicar Ultra Volumen: ${e.message}")
        }
    }

    /**
     * Aplica nivel de Super Bass (0.0f a 1.0f).
     */
    fun aplicarSuperBass(nivel: Float) {
        try {
            bassBoostHardware?.let { bb ->
                val fuerza = (nivel.coerceIn(0f, 1f) * 1000).toInt().toShort()
                bb.setStrength(fuerza)
                bb.enabled = fuerza > 0
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error al aplicar Super Bass: ${e.message}")
        }
    }

    /**
     * Aplica nivel de Espacialidad 3D (0.0f a 1.0f).
     */
    fun aplicarEspacialidad(nivel: Float) {
        try {
            virtualizadorHardware?.let { virt ->
                val fuerza = (nivel.coerceIn(0f, 1f) * 1000).toInt().toShort()
                virt.setStrength(fuerza)
                virt.enabled = fuerza > 0
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error al aplicar Espacialidad: ${e.message}")
        }
    }

    /**
     * Aplica las 5 bandas del Ecualizador Real.
     */
    fun aplicarBandasEcualizador(bandasDb: FloatArray) {
        try {
            ecualizadorHardware?.let { eq ->
                val numBandas = eq.numberOfBands.toInt()
                val minMb = eq.bandLevelRange[0]
                val maxMb = eq.bandLevelRange[1]

                for (i in 0 until minOf(numBandas, bandasDb.size)) {
                    val db = bandasDb[i]
                    // Mapear dB (-15.0 a +15.0) a mili-decibelios (ej. -1500 a +1500 mB)
                    val nivelMb = (db * 100).toInt().coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                    eq.setBandLevel(i.toShort(), nivelMb)
                }
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error al aplicar Bandas de Ecualizador: ${e.message}")
        }
    }

    /**
     * Libera los recursos de efectos cuando finaliza el reproductor.
     */
    fun liberarEfectos() {
        try {
            ecualizadorHardware?.enabled = false
            ecualizadorHardware?.release()
        } catch (_: Exception) {}
        try {
            bassBoostHardware?.enabled = false
            bassBoostHardware?.release()
        } catch (_: Exception) {}
        try {
            virtualizadorHardware?.enabled = false
            virtualizadorHardware?.release()
        } catch (_: Exception) {}
        try {
            ultraVolumenHardware?.enabled = false
            ultraVolumenHardware?.release()
        } catch (_: Exception) {}
        ecualizadorHardware = null
        bassBoostHardware = null
        virtualizadorHardware = null
        ultraVolumenHardware = null
        ultimoAudioSessionId = 0
    }
}
