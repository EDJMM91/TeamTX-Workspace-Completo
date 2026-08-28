package com.example.chat

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ARCHIVO: GestorAudio.kt
 *
 * Módulo de Grabación, Reproducción y Ciclo de Vida de Notas de Voz para el Chat del Club.
 *
 * Características:
 * - Grabación comprimida en formato MPEG-4 / AAC a 32 kbps (peso ultra bajo: ~240 KB por minuto).
 * - Soporte de audios largos (mínimo 1 minuto, hasta 5 minutos de grabación continua).
 * - Reproductor integrado con MediaPlayer para notas de voz en el chat.
 * - Limpieza automática de audios antiguos (> 30 días) para proteger el plan gratuito de 5 GB de Firebase.
 */
object GestorAudio {

    private const val TAG = "GestorAudio"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var archivoGrabacionActual: File? = null
    private var inicioGrabacionMillis: Long = 0L

    // Estados reactivos de grabación
    private val _estaGrabando = MutableStateFlow(false)
    val estaGrabando: StateFlow<Boolean> = _estaGrabando.asStateFlow()

    private val _segundosGrabados = MutableStateFlow(0)
    val segundosGrabados: StateFlow<Int> = _segundosGrabados.asStateFlow()

    private var grabacionJob: Job? = null

    // Estados reactivos de reproducción
    private val _audioEnReproduccionUrl = MutableStateFlow<String?>(null)
    val audioEnReproduccionUrl: StateFlow<String?> = _audioEnReproduccionUrl.asStateFlow()

    private val _progresoReproduccion = MutableStateFlow(0f) // 0.0 a 1.0
    val progresoReproduccion: StateFlow<Float> = _progresoReproduccion.asStateFlow()

    private val _estaPausado = MutableStateFlow(false)
    val estaPausado: StateFlow<Boolean> = _estaPausado.asStateFlow()

    private var reproduccionJob: Job? = null

    /**
     * Inicia la grabación de una nota de voz.
     */
    fun iniciarGrabacion(context: Context): Boolean {
        return try {
            detenerGrabacion(descartar = true)

            val carpetaAudios = File(context.cacheDir, "audios_temp")
            if (!carpetaAudios.exists()) carpetaAudios.mkdirs()

            val archivoDestino = File(carpetaAudios, "audio_tx_${System.currentTimeMillis()}.m4a")
            archivoGrabacionActual = archivoDestino

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(22050)
                setAudioEncodingBitRate(32000) // 32 kbps ultra eficiente
                setOutputFile(archivoDestino.absolutePath)
                setMaxDuration(300000) // 5 minutos máximo
                prepare()
                start()
            }

            mediaRecorder = recorder
            inicioGrabacionMillis = System.currentTimeMillis()
            _estaGrabando.value = true
            _segundosGrabados.value = 0

            grabacionJob?.cancel()
            grabacionJob = CoroutineScope(Dispatchers.Main).launch {
                while (_estaGrabando.value) {
                    delay(1000L)
                    _segundosGrabados.value = ((System.currentTimeMillis() - inicioGrabacionMillis) / 1000).toInt()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando grabación de audio: ${e.message}")
            detenerGrabacion(descartar = true)
            false
        }
    }

    /**
     * Detiene la grabación actual y retorna el archivo y su duración en segundos.
     */
    fun detenerGrabacion(descartar: Boolean = false): Pair<File, Int>? {
        grabacionJob?.cancel()
        grabacionJob = null

        val duracion = if (inicioGrabacionMillis > 0) {
            ((System.currentTimeMillis() - inicioGrabacionMillis) / 1000).toInt().coerceAtLeast(1)
        } else 0

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener MediaRecorder: ${e.message}")
        } finally {
            mediaRecorder = null
            _estaGrabando.value = false
            _segundosGrabados.value = 0
            inicioGrabacionMillis = 0L
        }

        val archivo = archivoGrabacionActual
        archivoGrabacionActual = null

        if (descartar || archivo == null || !archivo.exists() || archivo.length() == 0L) {
            archivo?.delete()
            return null
        }

        return Pair(archivo, duracion)
    }

    /**
     * Reproduce o pausa una nota de voz.
     */
    fun toggleReproducirAudio(urlOPath: String) {
        if (_audioEnReproduccionUrl.value == urlOPath) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _estaPausado.value = true
            } else {
                mediaPlayer?.start()
                _estaPausado.value = false
                iniciarLoopProgreso()
            }
            return
        }

        detenerReproduccion()

        try {
            val player = MediaPlayer().apply {
                setDataSource(urlOPath)
                setOnPreparedListener { mp ->
                    mp.start()
                    _audioEnReproduccionUrl.value = urlOPath
                    _estaPausado.value = false
                    iniciarLoopProgreso()
                }
                setOnCompletionListener {
                    detenerReproduccion()
                }
                setOnErrorListener { _, _, _ ->
                    detenerReproduccion()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error reproduciendo audio: ${e.message}")
            detenerReproduccion()
        }
    }

    private fun iniciarLoopProgreso() {
        reproduccionJob?.cancel()
        reproduccionJob = CoroutineScope(Dispatchers.Main).launch {
            while (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition?.toFloat() ?: 0f
                val total = mediaPlayer?.duration?.toFloat() ?: 1f
                if (total > 0f) {
                    _progresoReproduccion.value = (current / total).coerceIn(0f, 1f)
                }
                delay(100L)
            }
        }
    }

    fun detenerReproduccion() {
        reproduccionJob?.cancel()
        reproduccionJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _audioEnReproduccionUrl.value = null
        _progresoReproduccion.value = 0f
        _estaPausado.value = false
    }

    /**
     * 🛡️ Estrategia de Ciclo de Vida y Limpieza Automática para Proteger el Plan Gratuito de Firebase:
     * - Elimina audios de más de 30 días de antigüedad tanto en Firebase Storage como en la memoria local.
     */
    fun ejecutarLimpiezaAudiosAntiguos(context: Context, diasRetencion: Int = 30) = CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1. Limpieza de caché local
            val carpetaAudios = File(context.cacheDir, "audios_temp")
            if (carpetaAudios.exists()) {
                val limiteMillis = System.currentTimeMillis() - (diasRetencion * 24L * 60L * 60L * 1000L)
                carpetaAudios.listFiles()?.forEach { archivo ->
                    if (archivo.lastModified() < limiteMillis) {
                        archivo.delete()
                    }
                }
            }

            // 2. Limpieza en Firebase Storage (carpeta audios_chat)
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("audios_chat")
            storageRef.listAll().addOnSuccessListener { listResult ->
                val limiteMillis = System.currentTimeMillis() - (diasRetencion * 24L * 60L * 60L * 1000L)
                listResult.items.forEach { item ->
                    item.metadata.addOnSuccessListener { metadata ->
                        if (metadata.creationTimeMillis < limiteMillis) {
                            item.delete().addOnSuccessListener {
                                Log.d(TAG, "Audio antiguo purgado de Firebase Storage: ${item.name}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en rutina de limpieza de audios: ${e.message}")
        }
    }
}
