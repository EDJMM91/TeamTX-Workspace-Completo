package com.example.meshtx

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * MOTOR DE AUDIO PARA CASCO (MICRÓFONO, PARLANTES, PTT, VOX, BLUETOOTH SCO & JITTER BUFFER)
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidades:
 * 1. Capturar audio del micrófono de casco con AudioRecord a 16 kHz Mono 16-bit.
 * 2. Soporte para intercomunicadores Bluetooth de casco (Sena, Cardo, Ejeas)
 *    mediante Bluetooth SCO y detección automática de dispositivos de audio.
 * 3. Gestión de Foco de Audio (AudioFocusRequest) con Ducking para atenuar
 *    música o GPS de fondo al hablar por la malla.
 * 4. Filtro digital de viento (High-pass IIR a 300 Hz) para suprimir turbulencias de carretera.
 * 5. Detección de actividad vocal (VOX) con umbral RMS dinámico.
 * 6. Búfer anti-jitter para reproducción suave y sin saltos por AudioTrack.
 * 7. Compresión/descompresión de baja latencia DPCM 8-bit para transporte en malla.
 *
 * 100% OFFLINE - Cero dependencias de servidores externos o internet.
 */
class AudioCasco(
    private val contexto: Context,
    private val alGenerarFragmentoVoz: (ByteArray) -> Unit
) {

    private val etiquetaLog = "MeshTX_AudioCasco"

    // Parámetros de audio táctico
    private val frecuenciaMuestreo = 16000 // 16 kHz Mono
    private val canalEntrada = AudioFormat.CHANNEL_IN_MONO
    private val canalSalida = AudioFormat.CHANNEL_OUT_MONO
    private val formatoAudio = AudioFormat.ENCODING_PCM_16BIT

    private val tamanoBufferGrabacion: Int
    private val tamanoBufferReproduccion: Int

    private var grabadorAudio: AudioRecord? = null
    private var reproductorAudio: AudioTrack? = null
    private val gestorAudio = contexto.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val alcanceAudio = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tareaGrabacion: Job? = null
    private var tareaReproduccion: Job? = null

    // Búfer anti-jitter para suavizar reproducción de voz entrante
    private val colaReproduccion = ConcurrentLinkedQueue<ByteArray>()

    // Estados observables
    private val _estaGrabando = MutableStateFlow(false)
    val estaGrabando: StateFlow<Boolean> = _estaGrabando.asStateFlow()

    private val _estaHablandoVox = MutableStateFlow(false)
    val estaHablandoVox: StateFlow<Boolean> = _estaHablandoVox.asStateFlow()

    private val _dispositivoBluetoothCascoConectado = MutableStateFlow(false)
    val dispositivoBluetoothCascoConectado: StateFlow<Boolean> = _dispositivoBluetoothCascoConectado.asStateFlow()

    private val _modoAltavozActivo = MutableStateFlow(true)
    val modoAltavozActivo: StateFlow<Boolean> = _modoAltavozActivo.asStateFlow()

    // Configuraciones tácticas
    var modoPttActivo: Boolean = true // True = pulsar para hablar; False = manos libres VOX
    var pulsadorPttPresionado: Boolean = false
    var sensibilidadUmbralVox: Float = 0.08f // Umbral RMS para activar transmisión
    var cancelacionVientoActivada: Boolean = true
    var factorGananciaMicrofono: Float = 1.8f // Ganancia amplificada para captar voz sin audífonos
    var factorVolumenSalida: Float = 1.5f // Ganancia de amplificación en altavoz

    // Estado del filtro paso-alto de viento (IIR a ~300 Hz)
    private var ultimoMuestreoEntrada = 0f
    private var ultimoMuestreoFiltrado = 0f

    // Gestión de Foco de Audio (Ducking para navegación/música)
    private var peticionFocoAudio: AudioFocusRequest? = null
    private var tieneFocoAudio = false

    // Callback de hardware para audífonos y cascos Bluetooth
    private var callbackDispositivosAudio: AudioDeviceCallback? = null

    init {
        val minBufferEntrada = AudioRecord.getMinBufferSize(frecuenciaMuestreo, canalEntrada, formatoAudio)
        tamanoBufferGrabacion = maxOf(minBufferEntrada, 2048)

        val minBufferSalida = AudioTrack.getMinBufferSize(frecuenciaMuestreo, canalSalida, formatoAudio)
        tamanoBufferReproduccion = maxOf(minBufferSalida, 2048)

        inicializarReproductor()
        configurarDispositivosHardware()
        configurarModoAltavoz(true) // Activar altavoz exterior potente por defecto
    }

    private fun inicializarReproductor() {
        try {
            // Usar USAGE_MEDIA para garantizar que el audio salga con volumen completo
            // por los altavoces exteriores del teléfono si no hay auriculares conectados.
            val atributos = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val formato = AudioFormat.Builder()
                .setSampleRate(frecuenciaMuestreo)
                .setEncoding(formatoAudio)
                .setChannelMask(canalSalida)
                .build()

            reproductorAudio = AudioTrack.Builder()
                .setAudioAttributes(atributos)
                .setAudioFormat(formato)
                .setBufferSizeInBytes(tamanoBufferReproduccion)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            reproductorAudio?.play()
            iniciarHiloReproduccion()
            Log.i(etiquetaLog, "AudioTrack inicializado en USAGE_MEDIA para salida de altavoz nítida.")
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Error al inicializar AudioTrack: ${e.message}")
        }
    }

    /**
     * Alternar el modo altavoz potente (manos libres) o modo auricular privado.
     */
    fun configurarModoAltavoz(activar: Boolean) {
        _modoAltavozActivo.value = activar
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (activar) {
                    val dispositivos = gestorAudio.availableCommunicationDevices
                    val altavoz = dispositivos.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (altavoz != null) {
                        gestorAudio.setCommunicationDevice(altavoz)
                    }
                } else {
                    gestorAudio.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                gestorAudio.isSpeakerphoneOn = activar
            }
            Log.d(etiquetaLog, "Modo Altavoz Táctico configurado a: $activar")
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Error al configurar modo altavoz: ${e.message}")
        }
    }

    /**
     * Detectar intercomunicadores Bluetooth de casco (Sena, Cardo, etc.) y enrutar audio vía SCO.
     */
    private fun configurarDispositivosHardware() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            callbackDispositivosAudio = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    evaluarDispositivosAudio()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    evaluarDispositivosAudio()
                }
            }
            gestorAudio.registerAudioDeviceCallback(callbackDispositivosAudio, null)
            evaluarDispositivosAudio()
        }
    }

    private fun evaluarDispositivosAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val dispositivos = gestorAudio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hayBluetooth = false
            for (dispositivo in dispositivos) {
                if (dispositivo.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    dispositivo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    dispositivo.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                ) {
                    hayBluetooth = true
                    break
                }
            }
            _dispositivoBluetoothCascoConectado.value = hayBluetooth
            if (hayBluetooth) {
                activarModoCascoBluetooth()
            }
        }
    }

    /**
     * Habilitar el canal bidireccional Bluetooth SCO para el micrófono y parlantes del casco.
     */
    fun activarModoCascoBluetooth() {
        try {
            gestorAudio.mode = AudioManager.MODE_IN_COMMUNICATION
            gestorAudio.startBluetoothSco()
            gestorAudio.isBluetoothScoOn = true
            Log.d(etiquetaLog, "Modo Bluetooth SCO de casco activado con éxito.")
        } catch (e: Exception) {
            Log.w(etiquetaLog, "No se pudo activar Bluetooth SCO: ${e.message}")
        }
    }

    fun desactivarModoCascoBluetooth() {
        try {
            gestorAudio.stopBluetoothSco()
            gestorAudio.isBluetoothScoOn = false
            gestorAudio.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}
    }

    /**
     * Inicia el ciclo de captura de audio del micrófono del casco.
     */
    @SuppressLint("MissingPermission")
    fun iniciarCaptura() {
        if (_estaGrabando.value) return

        try {
            grabadorAudio = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                frecuenciaMuestreo,
                canalEntrada,
                formatoAudio,
                tamanoBufferGrabacion
            )

            if (grabadorAudio?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(etiquetaLog, "No se pudo inicializar el AudioRecord.")
                return
            }

            grabadorAudio?.startRecording()
            _estaGrabando.value = true
            Log.d(etiquetaLog, "Captura de micrófono de casco iniciada con éxito.")

            iniciarHiloGrabacion()
        } catch (e: SecurityException) {
            Log.e(etiquetaLog, "Permiso RECORD_AUDIO no concedido: ${e.message}")
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Fallo al iniciar captura de audio: ${e.message}")
        }
    }

    /**
     * Detiene la captura del micrófono de casco.
     */
    fun detenerCaptura() {
        _estaGrabando.value = false
        tareaGrabacion?.cancel()
        try {
            grabadorAudio?.stop()
            grabadorAudio?.release()
        } catch (_: Exception) {}
        grabadorAudio = null
        _estaHablandoVox.value = false
        liberarFocoAudio()
    }

    /**
     * Presionar / Soltar el botón Push-to-Talk (PTT).
     */
    fun setPttPresionado(presionado: Boolean) {
        pulsadorPttPresionado = presionado
        if (presionado) {
            solicitarFocoAudio()
        } else {
            liberarFocoAudio()
        }
        Log.d(etiquetaLog, "Estado PTT cambiado: presionado=$presionado")
    }

    /**
     * Encolar un paquete de voz recibido por la malla para reproducirlo en los parlantes/auriculares.
     */
    fun encolarAudioEntrante(datosComprimidos: ByteArray) {
        val audioPcm = descomprimirAudioSimple(datosComprimidos)
        colaReproduccion.add(audioPcm)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE FOCO DE AUDIO TÁCTICO (DUCKING)
    // ─────────────────────────────────────────────────────────────────────────

    private fun solicitarFocoAudio() {
        if (tieneFocoAudio) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val atributos = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                peticionFocoAudio = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(atributos)
                    .setOnAudioFocusChangeListener { }
                    .build()

                val resultado = gestorAudio.requestAudioFocus(peticionFocoAudio!!)
                tieneFocoAudio = (resultado == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            } else {
                @Suppress("DEPRECATION")
                val resultado = gestorAudio.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                tieneFocoAudio = (resultado == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            }
        } catch (_: Exception) {}
    }

    private fun liberarFocoAudio() {
        if (!tieneFocoAudio) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                peticionFocoAudio?.let { gestorAudio.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                gestorAudio.abandonAudioFocus(null)
            }
            tieneFocoAudio = false
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HILOS DE CAPTURA, DETECCIÓN VOX Y PROCESAMIENTO
    // ─────────────────────────────────────────────────────────────────────────

    private fun iniciarHiloGrabacion() {
        tareaGrabacion?.cancel()
        tareaGrabacion = alcanceAudio.launch {
            val bufferLectura = ShortArray(640) // Bloques de 40 ms a 16 kHz

            while (isActive && _estaGrabando.value) {
                val leidos = grabadorAudio?.read(bufferLectura, 0, bufferLectura.size) ?: 0
                if (leidos > 0) {
                    procesarBloqueAudio(bufferLectura, leidos)
                }
            }
        }
    }

    private fun procesarBloqueAudio(buffer: ShortArray, longitud: Int) {
        // 1. Calcular energía RMS para detector VOX
        var sumaCuadrados = 0.0
        for (i in 0 until longitud) {
            val normalizado = buffer[i] / 32768.0
            sumaCuadrados += normalizado * normalizado
        }
        val energiaRms = Math.sqrt(sumaCuadrados / longitud).toFloat()

        // 2. Determinar si se debe transmitir (PTT o VOX)
        val debeTransmitir = if (modoPttActivo) {
            pulsadorPttPresionado
        } else {
            val sobrepasaUmbral = energiaRms > sensibilidadUmbralVox
            if (sobrepasaUmbral && !_estaHablandoVox.value) {
                solicitarFocoAudio()
            } else if (!sobrepasaUmbral && _estaHablandoVox.value) {
                liberarFocoAudio()
            }
            _estaHablandoVox.value = sobrepasaUmbral
            sobrepasaUmbral
        }

        if (!debeTransmitir) return

        // 3. Aplicar Filtro de Viento (High-Pass IIR a ~300 Hz) y Ganancia
        val bufferFiltrado = ShortArray(longitud)
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * 300.0f)
        val dt = 1.0f / frecuenciaMuestreo
        val alfa = rc / (rc + dt)

        for (i in 0 until longitud) {
            var muestra = buffer[i].toFloat() * factorGananciaMicrofono

            if (cancelacionVientoActivada) {
                val filtrada = alfa * (ultimoMuestreoFiltrado + muestra - ultimoMuestreoEntrada)
                ultimoMuestreoEntrada = muestra
                ultimoMuestreoFiltrado = filtrada
                muestra = filtrada
            }

            // Clamping limpio a 16-bit
            val muestraClamped = muestra.coerceIn(-32768f, 32767f).toInt().toShort()
            bufferFiltrado[i] = muestraClamped
        }

        // 4. Comprimir a formato ligero para envío inmediato por la malla
        val datosComprimidos = comprimirAudioSimple(bufferFiltrado)
        alGenerarFragmentoVoz(datosComprimidos)
    }

    private fun iniciarHiloReproduccion() {
        tareaReproduccion?.cancel()
        tareaReproduccion = alcanceAudio.launch {
            while (isActive) {
                val audio = colaReproduccion.poll()
                if (audio != null && audio.isNotEmpty()) {
                    try {
                        if (reproductorAudio?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            reproductorAudio?.play()
                        }
                        reproductorAudio?.write(audio, 0, audio.size, AudioTrack.WRITE_BLOCKING)
                    } catch (e: Exception) {
                        Log.w(etiquetaLog, "Error al escribir en AudioTrack: ${e.message}")
                    }
                } else {
                    delay(8) // Búfer anti-jitter de espera suave
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPRESIÓN Y DESCOMPRESIÓN LIGERA DE BAJA LATENCIA (DPCM 8-BIT)
    // ─────────────────────────────────────────────────────────────────────────

    private fun comprimirAudioSimple(pcm: ShortArray): ByteArray {
        val stream = ByteArrayOutputStream(pcm.size)
        var previo = 0

        for (muestra in pcm) {
            val delta = (muestra - previo) shr 8
            val delta8 = delta.coerceIn(-128, 127).toByte()
            stream.write(delta8.toInt())
            previo = muestra.toInt()
        }

        return stream.toByteArray()
    }

    private fun descomprimirAudioSimple(comprimido: ByteArray): ByteArray {
        val pcmBytes = ByteArray(comprimido.size * 2)
        var previo = 0

        for (i in comprimido.indices) {
            val delta = comprimido[i].toInt() shl 8
            val escalado = ((previo + delta) * factorVolumenSalida).toInt().coerceIn(-32768, 32767)
            previo = escalado

            pcmBytes[i * 2] = (escalado and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((escalado shr 8) and 0xFF).toByte()
        }

        return pcmBytes
    }

    /**
     * Liberar recursos de hardware al destruir el servicio.
     */
    fun liberar() {
        detenerCaptura()
        desactivarModoCascoBluetooth()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callbackDispositivosAudio != null) {
            gestorAudio.unregisterAudioDeviceCallback(callbackDispositivosAudio)
        }
        tareaReproduccion?.cancel()
        try {
            reproductorAudio?.stop()
            reproductorAudio?.release()
        } catch (_: Exception) {}
        reproductorAudio = null
        colaReproduccion.clear()
    }
}
