package com.example.reproductor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.teamtxvzla.R
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════
// GESTOR CENTRAL DE AUDIO - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Controlador singleton: Escáner local, Cola, Playback continuo y Persistencia.
// ═══════════════════════════════════════════════════════════════════════════

object GESTOR_AUDIO_TX {

    private const val ETIQUETA = "TEAM_TX_REPRODUCTOR"
    private const val CANAL_NOTIFICACION_ID = "canal_reproductor_tx_pro"
    private const val NOTIFICACION_ID = 777
    private const val PREFS_NOMBRE = "prefs_reproductor_tx"

    const val ACCION_PLAY_PAUSA = "com.example.reproductor.ACCION_PLAY_PAUSA"
    const val ACCION_SIGUIENTE = "com.example.reproductor.ACCION_SIGUIENTE"
    const val ACCION_ANTERIOR = "com.example.reproductor.ACCION_ANTERIOR"

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var notificationManager: NotificationManager? = null
    private var contextoApp: Context? = null
    private var prefs: SharedPreferences? = null

    private val scopeCoroutine = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var jobProgreso: Job? = null
    private var jobDebounceEscaneo: Job? = null
    private var jobGuardarConfig: Job? = null
    private var receptorRegistrado = false

    // Carpetas y prefijos de audios no deseados (WhatsApp, notas de voz, etc.)
    private val carpetasExcluidas = setOf(
        "WhatsApp Audio", "WhatsApp Voice Notes", "WhatsApp Images",
        "Recordings", "Voice Recorder", "Audio Recorder",
        "Telegram", "Telegram Audio", "Telegram Voice",
        "Download", "Downloads", "UCDownloads",
        "Gboard", "speech"
    )
    private val prefijosExcluidos = setOf(
        "AUD-", "PTT-", "LT-", "LONG-", "VOICENOTE-",
        "PTT-", "IMG-", "VID-", "STK-",
        "com.whatsapp", "com.telegram"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // ESTADOS REACTIVOS
    // ─────────────────────────────────────────────────────────────────────────

    private val _cancionActual = MutableStateFlow<CancionMotera?>(null)
    val cancionActual: StateFlow<CancionMotera?> = _cancionActual.asStateFlow()

    private val _estado = MutableStateFlow(EstadoReproductor.DETENIDO)
    val estado: StateFlow<EstadoReproductor> = _estado.asStateFlow()

    private val _posicionActualMs = MutableStateFlow(0L)
    val posicionActualMs: StateFlow<Long> = _posicionActualMs.asStateFlow()

    private val _duracionTotalMs = MutableStateFlow(0L)
    val duracionTotalMs: StateFlow<Long> = _duracionTotalMs.asStateFlow()

    private val _modoBucle = MutableStateFlow(ModoBucle.BUCLE_TODAS)
    val modoBucle: StateFlow<ModoBucle> = _modoBucle.asStateFlow()

    private val _modoAleatorio = MutableStateFlow(false)
    val modoAleatorio: StateFlow<Boolean> = _modoAleatorio.asStateFlow()

    private val _todasLasCanciones = MutableStateFlow<List<CancionMotera>>(emptyList())
    val todasLasCanciones: StateFlow<List<CancionMotera>> = _todasLasCanciones.asStateFlow()

    private val _colaReproduccion = MutableStateFlow<List<CancionMotera>>(emptyList())
    val colaReproduccion: StateFlow<List<CancionMotera>> = _colaReproduccion.asStateFlow()

    private val _listasPersonalizadas = MutableStateFlow<List<ListaReproduccionMotera>>(emptyList())
    val listasPersonalizadas: StateFlow<List<ListaReproduccionMotera>> = _listasPersonalizadas.asStateFlow()

    private val _favoritasIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoritasIds: StateFlow<Set<Long>> = _favoritasIds.asStateFlow()

    private val _configuracion = MutableStateFlow(ConfiguracionReproductor())
    val configuracion: StateFlow<ConfiguracionReproductor> = _configuracion.asStateFlow()

    private val _caratulaActualRuta = MutableStateFlow<String?>(null)
    val caratulaActualRuta: StateFlow<String?> = _caratulaActualRuta.asStateFlow()

    private var indiceColaActual: Int = -1
    private var intentosErrorConsecutivos: Int = 0
    private const val MAX_INTENTOS_ERROR = 3
    private var autoSkipsConsecutivos: Int = 0
    private var tiempoInicioReproduccionMs: Long = 0L
    private const val MAX_AUTO_SKIPS = 5
    private const val MIN_DURACION_VALIDA_MS = 2000L
    private var errorEnCurso = false
    private val isTransitioning = java.util.concurrent.atomic.AtomicBoolean(false)
    private var logFile: File? = null
    private var timestampsSkips = mutableListOf<Long>()

    // Receptor para los controles de la notificación multimedia
    private val receptorControlesMultimedia = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACCION_PLAY_PAUSA -> alternarPlayPausa()
                ACCION_SIGUIENTE -> siguienteCancion()
                ACCION_ANTERIOR -> anteriorCancion()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    fun inicializar(contexto: Context) {
        if (contextoApp != null) return
        val appCtx = contexto.applicationContext
        contextoApp = appCtx
        prefs = appCtx.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
        audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        notificationManager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        // Archivo de diagnóstico para Honor (sin logcat)
        try {
            val carpetaDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!carpetaDocs.exists()) carpetaDocs.mkdirs()
            logFile = File(carpetaDocs, "tx_reproductor_log.txt")
            logFile?.writeText("=== LOG INICIO ${System.currentTimeMillis()} ===\n")
        } catch (_: Exception) {}

        registrarReceptorNotificacion(appCtx)
        crearCanalNotificacion()
        cargarConfiguracionGuardada()
        cargarFavoritasGuardadas()
        cargarListasGuardadas()

        escanearMusicaLocal()
    }

    fun logDiagnostico(mensaje: String) {
        Log.d(ETIQUETA, mensaje)
        try {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            val timestamp = sdf.format(Date())
            logFile?.appendText("[$timestamp] $mensaje\n")
        } catch (_: Exception) {}
    }

    private fun registrarReceptorNotificacion(ctx: Context) {
        if (!receptorRegistrado) {
            val filtro = IntentFilter().apply {
                addAction(ACCION_PLAY_PAUSA)
                addAction(ACCION_SIGUIENTE)
                addAction(ACCION_ANTERIOR)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(receptorControlesMultimedia, filtro, Context.RECEIVER_NOT_EXPORTED)
            } else {
                ctx.registerReceiver(receptorControlesMultimedia, filtro)
            }
            receptorRegistrado = true
            Log.d(ETIQUETA, "BroadcastReceiver de notificación registrado")
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_NOTIFICACION_ID,
                "Reproductor TX Pro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de música en segundo plano y pantalla de bloqueo"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(canal)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESCÁNER DE MÚSICA LOCAL
    // ─────────────────────────────────────────────────────────────────────────

    fun escanearMusicaLocal() {
        val ctx = contextoApp ?: return
        scopeCoroutine.launch(Dispatchers.IO) {
            val lista = mutableListOf<CancionMotera>()
            val duracionMinMs = _configuracion.value.duracionMinimaSegundos * 1000L
            val favs = _favoritasIds.value
            val cfg = _configuracion.value

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(duracionMinMs.toString())
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val prefsMeta = ctx.getSharedPreferences("prefs_metadatos_canciones_tx", Context.MODE_PRIVATE)

            try {
                ctx.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        var title = cursor.getString(titleCol) ?: "Pista $id"
                        var artist = cursor.getString(artistCol) ?: "Artista Desconocido"
                        var album = cursor.getString(albumCol) ?: "Álbum Desconocido"
                        val dur = cursor.getLong(durCol)
                        val path = cursor.getString(dataCol) ?: ""
                        val size = cursor.getLong(sizeCol)
                        val date = cursor.getLong(dateCol) * 1000L
                        val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L

                        // Aplicar sobreescrituras personalizadas guardadas por el usuario si existen
                        val customMetaStr = prefsMeta.getString("meta_$id", null)
                        if (!customMetaStr.isNullOrBlank()) {
                            try {
                                val obj = org.json.JSONObject(customMetaStr)
                                title = obj.optString("titulo", title).ifBlank { title }
                                artist = obj.optString("artista", artist).ifBlank { artist }
                                album = obj.optString("album", album).ifBlank { album }
                            } catch (_: Exception) {}
                        }

                        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        val parentFolder = if (path.isNotBlank()) File(path).parentFile?.name ?: "Música" else "Música"
                        val fileName = if (path.isNotBlank()) File(path).nameWithoutExtension else ""

                        val albumArtUri = if (albumId > 0) {
                            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                        } else null

                        // Filtro 1: Excluir carpetas de WhatsApp y mensajería
                        if (cfg.excluirCarpetasWhatsApp && carpetasExcluidas.any { carpeta ->
                            parentFolder.contains(carpeta, ignoreCase = true)
                        }) continue

                        // Filtro 2: Excluir archivos por prefijo (AUD-, PTT-, etc.)
                        if (cfg.excluirAudiosCortos && prefijosExcluidos.any { prefijo ->
                            fileName.startsWith(prefijo, ignoreCase = true)
                        }) continue

                        lista.add(
                            CancionMotera(
                                id = id,
                                titulo = title.trim(),
                                artista = if (artist.contains("<unknown>", true)) "Artista Biker" else artist.trim(),
                                album = if (album.contains("<unknown>", true)) "Álbum TX" else album.trim(),
                                duracionMs = dur,
                                rutaArchivo = path,
                                uriStr = uri.toString(),
                                portadaUriStr = albumArtUri,
                                fechaAgregada = date,
                                esFavorita = favs.contains(id),
                                tamanoBytes = size,
                                carpetaContenedora = parentFolder
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(ETIQUETA, "Error escaneando MediaStore: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                _todasLasCanciones.value = lista
                if (_colaReproduccion.value.isEmpty() && lista.isNotEmpty()) {
                    _colaReproduccion.value = lista
                }
                restaurarUltimaCancionSiExiste()
                Log.d(ETIQUETA, "Escaneo completado: ${lista.size} canciones encontradas.")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROLES DE REPRODUCCIÓN (PLAY, PAUSE, NEXT, PREV, SEEK)
    // ─────────────────────────────────────────────────────────────────────────

    fun reproducirCancion(cancion: CancionMotera, nuevaCola: List<CancionMotera>? = null) {
        val ctx = contextoApp ?: return

        nuevaCola?.let {
            _colaReproduccion.value = it
        }

        val colaActual = _colaReproduccion.value
        indiceColaActual = colaActual.indexOfFirst { it.id == cancion.id }.takeIf { it >= 0 } ?: 0

        _cancionActual.value = cancion
        _posicionActualMs.value = 0L
        _duracionTotalMs.value = cancion.duracionMs
        _estado.value = EstadoReproductor.CARGANDO

        guardarUltimaCancion(cancion.id)
        cargarCaratulaParaCancion(cancion)
        liberarMediaPlayer()
        solicitarFocoAudio()

        val uriAudio = if (cancion.uriStr.isNotBlank()) Uri.parse(cancion.uriStr) else Uri.fromFile(File(cancion.rutaArchivo))
        val rutaFisica = cancion.rutaArchivo

        errorEnCurso = false
        logDiagnostico("▶ reproducirCancion [error=$intentosErrorConsecutivos auto=$autoSkipsConsecutivos]: ${cancion.titulo} | $uriAudio")

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // Intento 1: URI content:// (estándar)
                // Intento 2 (fallback Honor/EMUI): ruta física del archivo
                try {
                    setDataSource(ctx, uriAudio)
                    logDiagnostico("setDataSource OK vía URI: $uriAudio")
                } catch (e: Exception) {
                    logDiagnostico("setDataSource URI falló (${e.message}) — reintentando con ruta física: $rutaFisica")
                    if (rutaFisica.isNotBlank() && File(rutaFisica).exists()) {
                        reset()
                        setDataSource(rutaFisica)
                        logDiagnostico("setDataSource OK vía ruta física")
                    } else {
                        throw e // No hay fallback disponible
                    }
                }
                setOnPreparedListener { mp ->
                    logDiagnostico("ON_PREPARED: ${cancion.titulo} dur=${mp.duration}ms session=${mp.audioSessionId} errCount=$intentosErrorConsecutivos autoCount=$autoSkipsConsecutivos")
                    if (errorEnCurso) {
                        logDiagnostico("BLOQUEADO por errorEnCurso")
                        return@setOnPreparedListener
                    }
                    try {
                        mp.start()
                        logDiagnostico("start() OK: ${cancion.titulo}")
                    } catch (e: Exception) {
                        logDiagnostico("FALLO start(): ${e.message}")
                        intentosErrorConsecutivos++
                        autoSkipsConsecutivos++
                        liberarMediaPlayer()
                        _estado.value = EstadoReproductor.DETENIDO
                        jobProgreso?.cancel()
                        if (intentosErrorConsecutivos >= MAX_INTENTOS_ERROR || autoSkipsConsecutivos >= MAX_AUTO_SKIPS) {
                            logDiagnostico("STOP por max en start()")
                            intentosErrorConsecutivos = 0
                            autoSkipsConsecutivos = 0
                            ocultarNotificacion()
                            abandonarFocoAudio()
                        } else {
                            scopeCoroutine.launch {
                                delay(500)
                                siguienteCancion()
                            }
                        }
                        return@setOnPreparedListener
                    }
                    // start() exitoso — AHORA sí resetear contadores
                    intentosErrorConsecutivos = 0
                    autoSkipsConsecutivos = 0
                    tiempoInicioReproduccionMs = System.currentTimeMillis()
                    _estado.value = EstadoReproductor.REPRODUCIENDO
                    _duracionTotalMs.value = mp.duration.toLong()
                    iniciarMonitoreoProgreso()
                    mostrarNotificacion()
                    try {
                        MOTOR_AUDIO_NATIVO.vincularAudioSession(mp.audioSessionId, _configuracion.value)
                    } catch (e: Exception) {
                        logDiagnostico("WARN AudioFX: ${e.message}")
                    }
                    logDiagnostico("REPRODUCIENDO: ${cancion.titulo} dur=${mp.duration}ms")
                }
                setOnCompletionListener {
                    logDiagnostico("🔄 ON_COMPLETION (errorEnCurso=$errorEnCurso): ${cancion.titulo}")
                    if (!errorEnCurso) {
                        alCompletarCancion()
                    } else {
                        logDiagnostico("⚠️ ON_COMPLETION BLOQUEADO por error en curso")
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    errorEnCurso = true
                    intentosErrorConsecutivos++
                    autoSkipsConsecutivos++
                    logDiagnostico("❌ ON_ERROR [error=$intentosErrorConsecutivos auto=$autoSkipsConsecutivos]: what=$what extra=$extra cancion=${cancion.titulo}")

                    try { mp.reset() } catch (_: Exception) {}
                    try { mp.release() } catch (_: Exception) {}
                    mediaPlayer = null
                    _estado.value = EstadoReproductor.DETENIDO
                    jobProgreso?.cancel()

                    if (intentosErrorConsecutivos < MAX_INTENTOS_ERROR && autoSkipsConsecutivos < MAX_AUTO_SKIPS) {
                        logDiagnostico("Saltando en 500ms...")
                        scopeCoroutine.launch {
                            delay(500)
                            logDiagnostico("Ejecutando siguienteCancion desde error handler")
                            siguienteCancion()
                        }
                    } else {
                        logDiagnostico("🛑 DETENIENDO: max errores alcanzado")
                        intentosErrorConsecutivos = 0
                        autoSkipsConsecutivos = 0
                        ocultarNotificacion()
                        abandonarFocoAudio()
                    }
                    true
                }
                logDiagnostico("🔧 prepareAsync iniciado")
                prepareAsync()
            }
        } catch (e: Exception) {
            logDiagnostico("💥 EXCEPCIÓN MediaPlayer: ${e.message}")
            _estado.value = EstadoReproductor.DETENIDO
        }
    }

    /**
     * Libera el MediaPlayer de forma segura.
     */
    private fun liberarMediaPlayer() {
        try {
            mediaPlayer?.reset()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun cargarCaratulaParaCancion(cancion: CancionMotera) {
        val ctx = contextoApp ?: return
        scopeCoroutine.launch(Dispatchers.IO) {
            val ruta = GESTOR_CARATULAS_TX.obtenerCaratula(ctx, cancion, _configuracion.value.descargaCaratulasModo)
            withContext(Dispatchers.Main) {
                if (_cancionActual.value?.id == cancion.id) {
                    _caratulaActualRuta.value = ruta
                    mostrarNotificacion()
                }
            }
        }
    }

    fun alternarPlayPausa() {
        val mp = mediaPlayer
        if (mp == null) {
            val actual = _cancionActual.value ?: _todasLasCanciones.value.firstOrNull()
            if (actual != null) {
                intentosErrorConsecutivos = 0
                autoSkipsConsecutivos = 0
                reproducirCancion(actual)
            }
            return
        }

        if (mp.isPlaying) {
            mp.pause()
            _estado.value = EstadoReproductor.PAUSADO
            mostrarNotificacion()
            abandonarFocoAudio()
        } else {
            solicitarFocoAudio()
            mp.start()
            _estado.value = EstadoReproductor.REPRODUCIENDO
            iniciarMonitoreoProgreso()
            mostrarNotificacion()
        }
    }

    fun siguienteCancion() {
        val cola = _colaReproduccion.value
        if (cola.isEmpty()) {
            logDiagnostico("⏭ siguienteCancion: cola vacía")
            return
        }

        intentosErrorConsecutivos = 0
        autoSkipsConsecutivos = 0
        errorEnCurso = false
        isTransitioning.set(false)

        logDiagnostico("⏭ siguienteCancion: indiceActual=$indiceColaActual tamañoCola=${cola.size}")

        if (_modoAleatorio.value && cola.size > 1) {
            var nuevoIndice = (cola.indices).random()
            if (nuevoIndice == indiceColaActual) {
                nuevoIndice = (nuevoIndice + 1) % cola.size
            }
            indiceColaActual = nuevoIndice
            logDiagnostico("🎲 Aleatorio: nuevoIndice=$nuevoIndice -> ${cola[nuevoIndice].titulo}")
            reproducirCancion(cola[indiceColaActual])
            return
        }

        var siguienteIndice = indiceColaActual + 1
        if (siguienteIndice >= cola.size) {
            if (_modoBucle.value == ModoBucle.SIN_BUCLE) {
                logDiagnostico("⏹ Fin de cola, deteniendo")
                detener()
                return
            }
            siguienteIndice = 0
        }

        indiceColaActual = siguienteIndice
        logDiagnostico("▶ Siguiente: indice=$siguienteIndice -> ${cola[siguienteIndice].titulo}")
        reproducirCancion(cola[indiceColaActual])
    }

    fun anteriorCancion() {
        val mp = mediaPlayer
        if (mp != null && mp.currentPosition > 3000) {
            // Si ya pasaron más de 3 segundos, reiniciar la pista actual
            buscarPosicion(0L)
            return
        }

        val cola = _colaReproduccion.value
        if (cola.isEmpty()) return

        intentosErrorConsecutivos = 0
        autoSkipsConsecutivos = 0
        errorEnCurso = false
        isTransitioning.set(false)

        var anteriorIndice = indiceColaActual - 1
        if (anteriorIndice < 0) {
            anteriorIndice = cola.size - 1
        }

        indiceColaActual = anteriorIndice
        reproducirCancion(cola[indiceColaActual])
    }

    /**
     * Búsqueda segura en la barra de progreso (sin bloquear decoder ni congelar audio).
     */
    fun buscarPosicion(posicionMs: Long) {
        try {
            val mp = mediaPlayer ?: return
            val dur = _duracionTotalMs.value
            val posSegura = posicionMs.coerceIn(0L, if (dur > 0) dur else Long.MAX_VALUE)
            _posicionActualMs.value = posSegura
            mp.seekTo(posSegura.toInt())
            if (_estado.value == EstadoReproductor.REPRODUCIENDO && !mp.isPlaying) {
                mp.start()
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error en seek: ${e.message}")
        }
    }

    fun detener() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _estado.value = EstadoReproductor.DETENIDO
        _posicionActualMs.value = 0L
        jobProgreso?.cancel()
        MOTOR_AUDIO_NATIVO.liberarEfectos()
        ocultarNotificacion()
        abandonarFocoAudio()
    }

    private fun alCompletarCancion() {
        val tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioReproduccionMs
        logDiagnostico("🎵 alCompletarCancion: '${_cancionActual.value?.titulo}' tiempo=${tiempoTranscurrido}ms modoBucle=${_modoBucle.value}")

        val esCompletionInmediato = tiempoTranscurrido < MIN_DURACION_VALIDA_MS

        if (esCompletionInmediato && tiempoInicioReproduccionMs > 0) {
            autoSkipsConsecutivos++
            logDiagnostico("⚠️ COMPLETION INMEDIATO [auto=$autoSkipsConsecutivos/$MAX_AUTO_SKIPS]")

            if (autoSkipsConsecutivos >= MAX_AUTO_SKIPS) {
                logDiagnostico("🛑 DEMASIADOS AUTO-SKIPS. Deteniendo.")
                autoSkipsConsecutivos = 0
                detener()
                return
            }
        }

        when (_modoBucle.value) {
            ModoBucle.BUCLE_UNA -> {
                val actual = _cancionActual.value
                if (actual != null) reproducirCancion(actual)
            }
            ModoBucle.BUCLE_TODAS, ModoBucle.SIN_BUCLE -> {
                siguienteCancion()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODOS BUCLE Y ALEATORIO
    // ─────────────────────────────────────────────────────────────────────────

    fun alternarModoBucle() {
        _modoBucle.value = when (_modoBucle.value) {
            ModoBucle.SIN_BUCLE -> ModoBucle.BUCLE_TODAS
            ModoBucle.BUCLE_TODAS -> ModoBucle.BUCLE_UNA
            ModoBucle.BUCLE_UNA -> ModoBucle.SIN_BUCLE
        }
    }

    fun alternarModoAleatorio() {
        _modoAleatorio.value = !_modoAleatorio.value
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE FAVORITAS Y LISTAS DE REPRODUCCIÓN
    // ─────────────────────────────────────────────────────────────────────────

    fun alternarFavorita(cancionId: Long) {
        val actual = _favoritasIds.value.toMutableSet()
        if (actual.contains(cancionId)) {
            actual.remove(cancionId)
        } else {
            actual.add(cancionId)
        }
        _favoritasIds.value = actual
        guardarFavoritas()

        _todasLasCanciones.value = _todasLasCanciones.value.map {
            if (it.id == cancionId) it.copy(esFavorita = actual.contains(cancionId)) else it
        }
        if (_cancionActual.value?.id == cancionId) {
            _cancionActual.value = _cancionActual.value?.copy(esFavorita = actual.contains(cancionId))
        }
    }

    fun marcarFavoritasPorLote(cancionIds: List<Long>, agregar: Boolean) {
        val actual = _favoritasIds.value.toMutableSet()
        if (agregar) {
            actual.addAll(cancionIds)
        } else {
            actual.removeAll(cancionIds.toSet())
        }
        _favoritasIds.value = actual
        guardarFavoritas()

        _todasLasCanciones.value = _todasLasCanciones.value.map {
            if (cancionIds.contains(it.id)) it.copy(esFavorita = actual.contains(it.id)) else it
        }
    }

    /**
     * Actualiza y persiste el título, artista y álbum personalizados de una canción.
     */
    fun actualizarMetadatosCancion(
        cancionId: Long,
        nuevoTitulo: String,
        nuevoArtista: String,
        nuevoAlbum: String
    ) {
        val tit = nuevoTitulo.trim()
        val art = nuevoArtista.trim()
        val alb = nuevoAlbum.trim()

        _todasLasCanciones.value = _todasLasCanciones.value.map { cancion ->
            if (cancion.id == cancionId) {
                cancion.copy(
                    titulo = if (tit.isNotBlank()) tit else cancion.titulo,
                    artista = if (art.isNotBlank()) art else cancion.artista,
                    album = if (alb.isNotBlank()) alb else cancion.album
                )
            } else cancion
        }

        _colaReproduccion.value = _colaReproduccion.value.map { cancion ->
            if (cancion.id == cancionId) {
                cancion.copy(
                    titulo = if (tit.isNotBlank()) tit else cancion.titulo,
                    artista = if (art.isNotBlank()) art else cancion.artista,
                    album = if (alb.isNotBlank()) alb else cancion.album
                )
            } else cancion
        }

        if (_cancionActual.value?.id == cancionId) {
            _cancionActual.value = _cancionActual.value?.copy(
                titulo = if (tit.isNotBlank()) tit else _cancionActual.value!!.titulo,
                artista = if (art.isNotBlank()) art else _cancionActual.value!!.artista,
                album = if (alb.isNotBlank()) alb else _cancionActual.value!!.album
            )
            mostrarNotificacion()
        }

        val ctx = contextoApp ?: return
        scopeCoroutine.launch(Dispatchers.IO) {
            val p = ctx.getSharedPreferences("prefs_metadatos_canciones_tx", Context.MODE_PRIVATE)
            val jsonObj = org.json.JSONObject().apply {
                put("titulo", tit)
                put("artista", art)
                put("album", alb)
            }
            p.edit().putString("meta_$cancionId", jsonObj.toString()).apply()
            Log.d(ETIQUETA, "Metadatos actualizados para canción $cancionId: $tit - $art")
        }
    }

    fun crearLista(nombre: String, descripcion: String = "", icono: String = "🎵", cancionesIniciales: List<Long> = emptyList()) {
        val nueva = ListaReproduccionMotera(
            nombre = nombre.trim().ifBlank { "Lista TX" },
            descripcion = descripcion.trim(),
            icono = icono,
            cancionIds = cancionesIniciales
        )
        _listasPersonalizadas.value = _listasPersonalizadas.value + nueva
        guardarListas()
    }

    fun agregarCancionesALista(listaId: String, cancionesIds: List<Long>) {
        _listasPersonalizadas.value = _listasPersonalizadas.value.map { lista ->
            if (lista.id == listaId) {
                val combinadas = (lista.cancionIds + cancionesIds).distinct()
                lista.copy(cancionIds = combinadas)
            } else lista
        }
        guardarListas()
    }

    fun alternarCancionEnLista(listaId: String, cancionId: Long) {
        _listasPersonalizadas.value = _listasPersonalizadas.value.map { lista ->
            if (lista.id == listaId) {
                val nuevosIds = if (lista.cancionIds.contains(cancionId)) {
                    lista.cancionIds - cancionId
                } else {
                    lista.cancionIds + cancionId
                }
                lista.copy(cancionIds = nuevosIds)
            } else lista
        }
        guardarListas()
    }

    fun eliminarCancionDeLista(listaId: String, cancionId: Long) {
        _listasPersonalizadas.value = _listasPersonalizadas.value.map { lista ->
            if (lista.id == listaId) {
                lista.copy(cancionIds = lista.cancionIds.filter { it != cancionId })
            } else lista
        }
        guardarListas()
    }

    fun eliminarLista(listaId: String) {
        _listasPersonalizadas.value = _listasPersonalizadas.value.filter { it.id != listaId }
        guardarListas()
    }

    /**
     * Obtiene o crea la lista "Descargas YT" y agrega una canción a ella.
     * Retorna el ID de la lista.
     */
    fun agregarDescargaAListaDescargas(cancionId: Long): String {
        val listaExistente = _listasPersonalizadas.value.find { it.nombre.equals("Descargas TX", ignoreCase = true) }
        return if (listaExistente != null) {
            if (!listaExistente.cancionIds.contains(cancionId)) {
                agregarCancionesALista(listaExistente.id, listOf(cancionId))
            }
            listaExistente.id
        } else {
            crearLista("Descargas TX", "Música descargada de YouTube", "⬇️", listOf(cancionId))
            _listasPersonalizadas.value.last().id
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN DE CONFIGURACIÓN Y EFECTOS DSP
    // ─────────────────────────────────────────────────────────────────────────

    fun actualizarConfiguracion(nueva: ConfiguracionReproductor) {
        val previa = _configuracion.value
        _configuracion.value = nueva

        // Guardado asíncrono con debounce en disco
        jobGuardarConfig?.cancel()
        jobGuardarConfig = scopeCoroutine.launch(Dispatchers.IO) {
            delay(300)
            guardarConfiguracion(nueva)
        }

        // Si cambió la duración mínima o los filtros de WhatsApp, re-escanear con debounce
        if (previa.duracionMinimaSegundos != nueva.duracionMinimaSegundos ||
            previa.excluirCarpetasWhatsApp != nueva.excluirCarpetasWhatsApp ||
            previa.excluirAudiosCortos != nueva.excluirAudiosCortos
        ) {
            jobDebounceEscaneo?.cancel()
            jobDebounceEscaneo = scopeCoroutine.launch {
                delay(500)
                escanearMusicaLocal()
            }
        }

        // Aplicar efectos en tiempo real al motor de audio
        MOTOR_AUDIO_NATIVO.aplicarUltraVolumen(nueva.ultraVolumenNivel)
        MOTOR_AUDIO_NATIVO.aplicarSuperBass(nueva.superBassNivel)
        MOTOR_AUDIO_NATIVO.aplicarEspacialidad(nueva.espacialidadNivel)
        MOTOR_AUDIO_NATIVO.aplicarBandasEcualizador(nueva.bandasEcualizador)
    }

    fun actualizarPosicionNube(x: Float, y: Float) {
        val nueva = _configuracion.value.copy(posicionNubeX = x, posicionNubeY = y)
        _configuracion.value = nueva
        scopeCoroutine.launch(Dispatchers.IO) {
            guardarConfiguracion(nueva)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PERSISTENCIA (PREFERENCIAS)
    // ─────────────────────────────────────────────────────────────────────────

    private fun guardarFavoritas() {
        scopeCoroutine.launch(Dispatchers.IO) {
            val arr = JSONArray(_favoritasIds.value)
            prefs?.edit()?.putString("favoritas_ids", arr.toString())?.apply()
        }
    }

    private fun cargarFavoritasGuardadas() {
        val str = prefs?.getString("favoritas_ids", null) ?: return
        try {
            val arr = JSONArray(str)
            val set = mutableSetOf<Long>()
            for (i in 0 until arr.length()) set.add(arr.getLong(i))
            _favoritasIds.value = set
        } catch (_: Exception) {}
    }

    private fun guardarListas() {
        scopeCoroutine.launch(Dispatchers.IO) {
            val arr = JSONArray()
            _listasPersonalizadas.value.forEach { lista ->
                val obj = JSONObject().apply {
                    put("id", lista.id)
                    put("nombre", lista.nombre)
                    put("descripcion", lista.descripcion)
                    put("fechaCreacion", lista.fechaCreacion)
                    put("icono", lista.icono)
                    put("cancionIds", JSONArray(lista.cancionIds))
                }
                arr.put(obj)
            }
            prefs?.edit()?.putString("listas_guardadas", arr.toString())?.apply()
        }
    }

    private fun cargarListasGuardadas() {
        val str = prefs?.getString("listas_guardadas", null) ?: return
        try {
            val arr = JSONArray(str)
            val lista = mutableListOf<ListaReproduccionMotera>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val nombre = obj.getString("nombre")
                val desc = obj.optString("descripcion", "")
                val fecha = obj.optLong("fechaCreacion", System.currentTimeMillis())
                val icono = obj.optString("icono", "🎵")
                val idsArr = obj.optJSONArray("cancionIds")
                val cancionIds = mutableListOf<Long>()
                if (idsArr != null) {
                    for (j in 0 until idsArr.length()) cancionIds.add(idsArr.getLong(j))
                }
                lista.add(ListaReproduccionMotera(id, nombre, desc, fecha, cancionIds, icono))
            }
            _listasPersonalizadas.value = lista
        } catch (_: Exception) {}
    }

    private fun guardarConfiguracion(cfg: ConfiguracionReproductor) {
        val p = prefs ?: return
        val arr = JSONArray()
        cfg.bandasEcualizador.forEach { arr.put(it.toDouble()) }

        p.edit()
            .putBoolean("nube_activa", cfg.nubeFlotanteActiva)
            .putBoolean("nube_bloqueada", cfg.nubeBloqueada)
            .putBoolean("nube_auto_ocultar", cfg.autoOcultarNube)
            .putFloat("nube_x", cfg.posicionNubeX)
            .putFloat("nube_y", cfg.posicionNubeY)
            .putInt("duracion_min_seg", cfg.duracionMinimaSegundos)
            .putFloat("ultra_vol", cfg.ultraVolumenNivel)
            .putFloat("super_bass", cfg.superBassNivel)
            .putFloat("espacialidad", cfg.espacialidadNivel)
            .putString("bandas_eq", arr.toString())
            .putString("preset_actual", cfg.presetActual)
            .putBoolean("excluir_whatsapp", cfg.excluirCarpetasWhatsApp)
            .putBoolean("excluir_audios_cortos", cfg.excluirAudiosCortos)
            .putString("descarga_caratulas_modo", cfg.descargaCaratulasModo.name)
            .apply()
    }

    private fun guardarUltimaCancion(cancionId: Long) {
        scopeCoroutine.launch(Dispatchers.IO) {
            val colaIds = _colaReproduccion.value.map { it.id }
            prefs?.edit()
                ?.putLong("ultima_cancion_id", cancionId)
                ?.putString("cola_reproduccion_ids", JSONArray(colaIds).toString())
                ?.apply()
        }
    }

    private fun restaurarUltimaCancionSiExiste() {
        if (_cancionActual.value != null) return
        val p = prefs ?: return
        val ultimaId = p.getLong("ultima_cancion_id", -1L)
        if (ultimaId > 0) {
            val cancion = _todasLasCanciones.value.find { it.id == ultimaId }
            if (cancion != null) {
                _cancionActual.value = cancion
                _duracionTotalMs.value = cancion.duracionMs
                cargarCaratulaParaCancion(cancion)

                val strCola = p.getString("cola_reproduccion_ids", null)
                if (strCola != null) {
                    try {
                        val arr = JSONArray(strCola)
                        val ids = mutableListOf<Long>()
                        for (i in 0 until arr.length()) ids.add(arr.getLong(i))
                        val colaRestaurada = ids.mapNotNull { id -> _todasLasCanciones.value.find { it.id == id } }
                        if (colaRestaurada.isNotEmpty()) {
                            _colaReproduccion.value = colaRestaurada
                            indiceColaActual = colaRestaurada.indexOfFirst { it.id == ultimaId }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun cargarConfiguracionGuardada() {
        val p = prefs ?: return
        val arrEqStr = p.getString("bandas_eq", null)
        val bandas = if (arrEqStr != null) {
            try {
                val arr = JSONArray(arrEqStr)
                FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
            } catch (_: Exception) { floatArrayOf(2f, 1f, 0f, 2f, 3f) }
        } else floatArrayOf(2f, 1f, 0f, 2f, 3f)

        val modoCaratulasStr = p.getString("descarga_caratulas_modo", ModoDescargaCaratulas.WIFI_Y_DATOS.name)
        val modoCaratulas = try {
            ModoDescargaCaratulas.valueOf(modoCaratulasStr ?: ModoDescargaCaratulas.WIFI_Y_DATOS.name)
        } catch (_: Exception) { ModoDescargaCaratulas.WIFI_Y_DATOS }

        _configuracion.value = ConfiguracionReproductor(
            nubeFlotanteActiva = p.getBoolean("nube_activa", true),
            nubeBloqueada = p.getBoolean("nube_bloqueada", false),
            posicionNubeX = p.getFloat("nube_x", 40f),
            posicionNubeY = p.getFloat("nube_y", 250f),
            autoOcultarNube = p.getBoolean("nube_auto_ocultar", true),
            duracionMinimaSegundos = p.getInt("duracion_min_seg", 30),
            ultraVolumenNivel = p.getFloat("ultra_vol", 1.0f),
            superBassNivel = p.getFloat("super_bass", 0.4f),
            espacialidadNivel = p.getFloat("espacialidad", 0.2f),
            bandasEcualizador = bandas,
            presetActual = p.getString("preset_actual", "Rock Motero") ?: "Rock Motero",
            excluirCarpetasWhatsApp = p.getBoolean("excluir_whatsapp", true),
            excluirAudiosCortos = p.getBoolean("excluir_audios_cortos", true),
            descargaCaratulasModo = modoCaratulas
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NOTIFICACIÓN DEL SISTEMA CON CONTROLES MULTIMEDIA
    // ─────────────────────────────────────────────────────────────────────────

    private fun mostrarNotificacion() {
        val ctx = contextoApp ?: return
        val cancion = _cancionActual.value ?: return
        val esPlaying = _estado.value == EstadoReproductor.REPRODUCIENDO

        val intentApp = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentApp = PendingIntent.getActivity(
            ctx,
            0,
            intentApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val intentPrev = Intent(ACCION_ANTERIOR).apply { setPackage(ctx.packageName) }
        val pIntentPrev = PendingIntent.getBroadcast(ctx, 10, intentPrev, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val intentPlay = Intent(ACCION_PLAY_PAUSA).apply { setPackage(ctx.packageName) }
        val pIntentPlay = PendingIntent.getBroadcast(ctx, 11, intentPlay, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val intentNext = Intent(ACCION_SIGUIENTE).apply { setPackage(ctx.packageName) }
        val pIntentNext = PendingIntent.getBroadcast(ctx, 12, intentNext, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(ctx, CANAL_NOTIFICACION_ID)
            .setSmallIcon(R.drawable.logoteam)
            .setContentTitle(cancion.titulo)
            .setContentText("${cancion.artista} • ${cancion.album}")
            .setSubText("Música TX")
            .setContentIntent(pendingIntentApp)
            .setOngoing(esPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Anterior", pIntentPrev)
            .addAction(if (esPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (esPlaying) "Pausa" else "Play", pIntentPlay)
            .addAction(android.R.drawable.ic_media_next, "Siguiente", pIntentNext)

        // Cargar carátula en miniatura si existe
        val rutaCaratula = _caratulaActualRuta.value
        if (!rutaCaratula.isNullOrBlank()) {
            try {
                val bitmap = BitmapFactory.decodeFile(rutaCaratula)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                }
            } catch (_: Exception) {}
        }

        notificationManager?.notify(NOTIFICACION_ID, builder.build())
    }

    private fun ocultarNotificacion() {
        notificationManager?.cancel(NOTIFICACION_ID)
    }

    private fun iniciarMonitoreoProgreso() {
        jobProgreso?.cancel()
        jobProgreso = scopeCoroutine.launch {
            while (_estado.value == EstadoReproductor.REPRODUCIENDO) {
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            _posicionActualMs.value = mp.currentPosition.toLong()
                        }
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }

    private var audioFocusRequest: AudioFocusRequest? = null

    private fun solicitarFocoAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { foco ->
                    when (foco) {
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS -> {
                            mediaPlayer?.pause()
                            _estado.value = EstadoReproductor.PAUSADO
                            mostrarNotificacion()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            mediaPlayer?.setVolume(0.3f, 0.3f)
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            mediaPlayer?.setVolume(1.0f, 1.0f)
                        }
                    }
                }
                .build()
            audioFocusRequest = request
            audioManager?.requestAudioFocus(request)
        }
    }

    private fun abandonarFocoAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager?.abandonAudioFocusRequest(it)
                audioFocusRequest = null
            }
        }
    }

    fun estaActivoOEnPausa(): Boolean {
        return _estado.value == EstadoReproductor.REPRODUCIENDO || _estado.value == EstadoReproductor.PAUSADO
    }
}
