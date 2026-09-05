package com.example.meshtx

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * CORRECTOR TÁCTICO FEC (FORWARD ERROR CORRECTION) INTRAPAQUETE + CÓDEC G.711 μ-LAW
 * ═══════════════════════════════════════════════════════════════════════════
 * Diseñado específicamente para intercomunicadores de caravana motera:
 * 1. CÓDEC G.711 μ-LAW PURO EN KOTLIN:
 *    - Cada muestra PCM de 16-bit a 16 kHz se comprime de forma independiente a 8-bit.
 *    - CERO dependencias de MediaCodec o binarios C/C++ propensos a fallar entre marcas.
 *    - Si un paquete se pierde por interferencia de montañas o distancia, la voz se
 *      recupera al milisegundo sin emitir ruido blanco ensordecedor.
 * 2. REDUNDANCIA FEC ENTRELAZADA (N + N-1):
 *    - Cada datagrama de voz enviado transporta:
 *      a) El fragmento actual de voz (20 ms / 320 muestras).
 *      b) Una copia comprimida de respaldo del fragmento anterior (N-1).
 *    - Si el paquete N choca y se pierde en el aire a 120 km/h, el receptor detecta
 *      el salto en la secuencia al recibir N+1, extrae el respaldo de N y lo reconstruye
 *      al vuelo en el búfer de reproducción, eliminando micro-cortes y tartamudeo.
 */
object FecAudioTactico {

    const val ID_CODEC_G711U_FEC: Byte = 0x05 // G.711 μ-law con redundancia FEC intrapaquete
    const val ID_CODEC_G711U_SIMPLE: Byte = 0x03 // G.711 μ-law estándar sin FEC

    private const val BIAS_MULAW = 0x84
    private const val CLIP_MULAW = 32635

    // Tablas de descompresión rápida en RAM (LUT de 256 entradas para 0% latencia de CPU)
    private val tablaDescompresionMuLaw = ShortArray(256) { indiceByte ->
        descomprimirMuestraMuLawCalculado(indiceByte.toByte())
    }

    // Estado del transmisor local (para entrelazar N-1)
    private var contadorSecuenciaTransmision: Short = 0
    @Volatile private var respaldoFrameAnterior: ByteArray? = null

    // Seguimiento del receptor por ID de piloto remoto
    private val secuenciasReceptorPorPiloto = java.util.concurrent.ConcurrentHashMap<Long, Short>()

    /**
     * Comprime un bloque de audio PCM 16-bit (ej. 320 o 640 muestras) a G.711 μ-law 8-bit.
     */
    fun comprimirPcmAMuLaw(pcm: ShortArray, offset: Int = 0, longitud: Int = pcm.size): ByteArray {
        val salida = ByteArray(longitud)
        for (i in 0 until longitud) {
            salida[i] = comprimirMuestraMuLaw(pcm[offset + i])
        }
        return salida
    }

    /**
     * Descomprime un búfer G.711 μ-law 8-bit a muestras PCM de 16-bit.
     */
    fun descomprimirMuLawAPcm(muLaw: ByteArray, offset: Int = 0, longitud: Int = muLaw.size): ShortArray {
        val salida = ShortArray(longitud)
        for (i in 0 until longitud) {
            val uByte = muLaw[offset + i].toInt() and 0xFF
            salida[i] = tablaDescompresionMuLaw[uByte]
        }
        return salida
    }

    /**
     * Empaqueta el bloque de audio actual con redundancia entrelazada FEC.
     * Estructura del paquete:
     * [Byte 0]: ID_CODEC_G711U_FEC (0x05)
     * [Bytes 1..2]: Número de secuencia (Short)
     * [Bytes 3..4]: Tamaño del frame actual (Short)
     * [Bytes 5..5+L]: Frame de audio actual en μ-law
     * [Bytes X..X+1]: Tamaño del frame anterior de respaldo (Short)
     * [Bytes X+2..fin]: Frame anterior de respaldo en μ-law
     */
    @Synchronized
    fun empaquetarConFec(muLawActual: ByteArray): ByteArray {
        val secuencia = ++contadorSecuenciaTransmision
        val copiaAnterior = respaldoFrameAnterior
        val tamanoActual = muLawActual.size
        val tamanoAnterior = copiaAnterior?.size ?: 0

        val totalBytes = 1 + 2 + 2 + tamanoActual + 2 + tamanoAnterior
        val buffer = ByteBuffer.allocate(totalBytes).order(ByteOrder.BIG_ENDIAN)

        buffer.put(ID_CODEC_G711U_FEC)
        buffer.putShort(secuencia)
        buffer.putShort(tamanoActual.toShort())
        buffer.put(muLawActual)
        buffer.putShort(tamanoAnterior.toShort())
        if (copiaAnterior != null && tamanoAnterior > 0) {
            buffer.put(copiaAnterior)
        }

        // Guardar el actual para ser el respaldo del próximo paquete
        respaldoFrameAnterior = muLawActual.copyOf()

        return buffer.array()
    }

    /**
     * Reinicia el estado FEC del transmisor al soltar el botón PTT.
     */
    fun reiniciarTransmisorFec() {
        respaldoFrameAnterior = null
    }

    /**
     * Resultado del procesamiento de recepción FEC:
     * entrega los fragmentos PCM listos para encolar en el AudioTrack.
     */
    data class ResultadoFec(
        val framesPcmAEncolar: List<ShortArray>,
        val huboPaqueteRecuperado: Boolean
    )

    /**
     * Desempaqueta y reconstruye datagramas entrantes.
     * Si detecta pérdida de paquete (salto de secuencia == 2),
     * recupera el paquete perdido utilizando el bloque FEC intrapaquete.
     */
    fun procesarRecepcionConFec(datosEntrantes: ByteArray, idEmisor: Long): ResultadoFec {
        if (datosEntrantes.isEmpty()) return ResultadoFec(emptyList(), false)

        val codecId = datosEntrantes[0]
        if (codecId != ID_CODEC_G711U_FEC) {
            // Compatibilidad hacia atrás: audio sin FEC
            val pcm = descomprimirMuLawAPcm(datosEntrantes, offset = 1, longitud = datosEntrantes.size - 1)
            return ResultadoFec(listOf(pcm), false)
        }

        if (datosEntrantes.size < 7) {
            return ResultadoFec(emptyList(), false)
        }

        val buffer = ByteBuffer.wrap(datosEntrantes).order(ByteOrder.BIG_ENDIAN)
        buffer.get() // Saltar codecId (0x05)

        val secuencia = buffer.short
        val tamanoActual = buffer.short.toInt() and 0xFFFF
        if (buffer.remaining() < tamanoActual) {
            return ResultadoFec(emptyList(), false)
        }

        val bytesActual = ByteArray(tamanoActual)
        buffer.get(bytesActual)

        var bytesAnterior: ByteArray? = null
        if (buffer.remaining() >= 2) {
            val tamanoAnterior = buffer.short.toInt() and 0xFFFF
            if (tamanoAnterior > 0 && buffer.remaining() >= tamanoAnterior) {
                val temp = ByteArray(tamanoAnterior)
                buffer.get(temp)
                bytesAnterior = temp
            }
        }

        // Evaluar salto de secuencia para recuperación
        val ultimaSecuencia = secuenciasReceptorPorPiloto[idEmisor]
        secuenciasReceptorPorPiloto[idEmisor] = secuencia

        val listaSalida = mutableListOf<ShortArray>()
        var recuperado = false

        if (ultimaSecuencia != null) {
            val diferencia = (secuencia - ultimaSecuencia).toShort()
            if (diferencia == 2.toShort() && bytesAnterior != null && bytesAnterior.isNotEmpty()) {
                // ¡PAQUETE PERDIDO EN EL AIRE DETECTADO!
                // El paquete anterior no llegó, pero este paquete trajo su respaldo FEC.
                val pcmRecuperado = descomprimirMuLawAPcm(bytesAnterior)
                listaSalida.add(pcmRecuperado)
                recuperado = true
            }
        }

        // Añadir el frame actual recibido
        val pcmActual = descomprimirMuLawAPcm(bytesActual)
        listaSalida.add(pcmActual)

        return ResultadoFec(listaSalida, recuperado)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FÓRMULAS MATEMÁTICAS ESTÁNDAR ITU-T G.711 μ-LAW
    // ─────────────────────────────────────────────────────────────────────────

    private fun comprimirMuestraMuLaw(pcmMuestra: Short): Byte {
        var muestra = pcmMuestra.toInt()
        val signo = if (muestra < 0) 0x80 else 0
        if (signo != 0) muestra = -muestra
        if (muestra > CLIP_MULAW) muestra = CLIP_MULAW
        muestra += BIAS_MULAW

        var exponente = 7
        var mascara = 0x4000
        while ((muestra and mascara) == 0 && exponente > 0) {
            exponente--
            mascara = mascara shr 1
        }

        val mantisa = (muestra shr (exponente + 3)) and 0x0F
        val muLawByte = (signo or (exponente shl 4) or mantisa).inv()
        return muLawByte.toByte()
    }

    private fun descomprimirMuestraMuLawCalculado(muLawByte: Byte): Short {
        val u = muLawByte.toInt().inv() and 0xFF
        val signo = u and 0x80
        val exponente = (u shr 4) and 0x07
        val mantisa = u and 0x0F

        var muestra = ((mantisa shl 3) + BIAS_MULAW) shl exponente
        muestra -= BIAS_MULAW

        return if (signo != 0) (-muestra).toShort() else muestra.toShort()
    }
}
