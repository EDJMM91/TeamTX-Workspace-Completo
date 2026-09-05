package com.example.meshtx

import java.util.Random

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GENERADOR DE RUIDO DE CONFORT (CNG) + FILTRO PASA-BANDA VAD ESTRICTO
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidades:
 * 1. FILTRO PASA-BANDA ESTRICTO (300 Hz a 3400 Hz):
 *    - Filtro digital IIR en cascada (Paso-Alto 300 Hz + Paso-Bajo 3400 Hz).
 *    - Corta físicamente las resonancias y escapes graves de las motos (< 300 Hz)
 *      y la turbulencia estridente del viento en el casco (> 3400 Hz).
 * 2. CNG (COMFORT NOISE GENERATOR):
 *    - Genera una sutil estática analógica de línea suave (-48 dBFS)
 *      en los auriculares mientras exista enlace táctico con otros compañeros.
 *    - Si el ruido de confort desaparece, el piloto sabe de forma subliminal
 *      e inmediata que se salió del rango del convoy sin mirar el teléfono.
 */
class GeneradorConfortAudio(
    private val frecuenciaMuestreo: Int = 16000
) {

    // ─────────────────────────────────────────────────────────────────────────
    // FILTRO PASA-BANDA: 300 Hz - 3400 Hz
    // ─────────────────────────────────────────────────────────────────────────
    private val fcPasoAlto = 300.0f
    private val fcPasoBajo = 3400.0f

    // Coeficientes IIR de 1er orden
    private val dt = 1.0f / frecuenciaMuestreo

    // Paso-Alto (300 Hz)
    private val rcHigh = 1.0f / (2.0f * Math.PI.toFloat() * fcPasoAlto)
    private val alfaHigh = rcHigh / (rcHigh + dt)
    private var hpEntradaAnterior = 0f
    private var hpSalidaAnterior = 0f

    // Paso-Bajo (3400 Hz)
    private val rcLow = 1.0f / (2.0f * Math.PI.toFloat() * fcPasoBajo)
    private val alfaLow = dt / (rcLow + dt)
    private var lpSalidaAnterior = 0f

    /**
     * Aplica el filtro pasa-banda estricto a un arreglo de muestras PCM 16-bit.
     */
    fun filtrarVozBandaEstricta(pcm: ShortArray, longitud: Int = pcm.size): ShortArray {
        val salida = ShortArray(longitud)
        for (i in 0 until longitud) {
            val entrada = pcm[i].toFloat()

            // 1. Filtro Paso-Alto a 300 Hz (Corta escapes de moto y rumble de asfalto)
            val pasoAlto = alfaHigh * (hpSalidaAnterior + entrada - hpEntradaAnterior)
            hpEntradaAnterior = entrada
            hpSalidaAnterior = pasoAlto

            // 2. Filtro Paso-Bajo a 3400 Hz (Corta silbido de viento y fricción aerodinámica)
            val pasoBajo = lpSalidaAnterior + alfaLow * (pasoAlto - lpSalidaAnterior)
            lpSalidaAnterior = pasoBajo

            salida[i] = pasoBajo.coerceIn(-32768f, 32767f).toInt().toShort()
        }
        return salida
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERADOR DE RUIDO DE CONFORT (CNG)
    // ─────────────────────────────────────────────────────────────────────────
    private val aleatorio = Random()
    private val amplitudConfort = 160 // Amplitud muy baja (~ -46 dBFS) para no molestar

    /**
     * Genera un bloque de ruido de confort térmico analógico filtrado.
     */
    fun generarMuestraRuidoConfort(tamanoMuestras: Int): ShortArray {
        val bloque = ShortArray(tamanoMuestras)
        for (i in 0 until tamanoMuestras) {
            // Ruido blanco con paso suave para emular ruido térmico analógico de radio
            val ruido = (aleatorio.nextGaussian() * amplitudConfort).toInt()
            bloque[i] = ruido.coerceIn(-1000, 1000).toShort()
        }
        return bloque
    }

    fun reiniciarFiltros() {
        hpEntradaAnterior = 0f
        hpSalidaAnterior = 0f
        lpSalidaAnterior = 0f
    }
}
