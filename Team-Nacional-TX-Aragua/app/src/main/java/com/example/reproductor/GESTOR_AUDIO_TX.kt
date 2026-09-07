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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private var activoAfd: android.content.res.AssetFileDescriptor? = null
    private var activoPfd: android.os.ParcelFileDescriptor? = null
    private var activoFis: java.io.FileInputStream? = null

    private val scopeCoroutine = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var jobProgreso: Job? = null
    private var jobDebounceEscaneo: Job? = null
    private var jobGuardarConfig: Job? = null
    private var receptorRegistrado = false

    // Carpetas y prefijos de audios no deseados (notas de voz de mensajería, grabaciones de llamadas, etc.)
    // NOTA: Se eliminaron "Download" y "Downloads" para permitir leer canciones descargadas por el usuario
    private val carpetasExcluidas = setOf(
        "WhatsApp Voice Notes", "WhatsApp Audio",
        "Recordings", "Voice Recorder", "Audio Recorder",
        "Telegram Voice", "Gboard", "speech", "call_rec"
    )
    private val prefijosExcluidos = setOf(
        "PTT-", "VOICENOTE-", "STK-", "call_rec_"
    )
    private val extensionesAudioSoportadas = setOf(
        "mp3", "m4a", "aac", "ogg", "opus", "flac", "wav", "mid", "xmf", "amr", "wma"
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
    private const val MAX_AUTO_SKIPS = 3
    private const val MIN_DURACION_VALIDA_MS = 500L
    private var errorEnCurso = false
    private val playbackMutex = Mutex()
    private var jobAutoSkip: Job? = null
    private var contadorSkipsConsecutivosUsuario = 0
    private var logFile: File? = null
    private var timestampsSkips = mutableListOf<Long>()

    // DAOs de persistencia Room (Versión 31)
    private var cancionDao: CancionLocalDao? = null
    private var listaDao: ListaReproduccionDao? = null
    private var estadoDao: EstadoReproductorDao? = null

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

        // Inicializar Room Database
        try {
            val db = com.example.data.local.AppDatabase.getDatabase(appCtx, scopeCoroutine)
            cancionDao = db.cancionLocalDao()
            listaDao = db.listaReproduccionDao()
            estadoDao = db.estadoReproductorDao()
            Log.d(ETIQUETA, "DAOs Room vinculados exitosamente")
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error inicializando DAOs Room: ${e.message}")
        }

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
            val rutasExistentes = mutableSetOf<String>()
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

            // Consulta universal: no requerir estrictamente IS_MUSIC != 0 porque muchos formatos (M4A, OPUS, FLAC) no lo traen activo
            val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' OR ${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR ${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR ${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.wav' OR ${MediaStore.Audio.Media.DATA} LIKE '%.aac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.ogg' OR ${MediaStore.Audio.Media.DATA} LIKE '%.opus')"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val prefsMeta = ctx.getSharedPreferences("prefs_metadatos_canciones_tx", Context.MODE_PRIVATE)

            try {
                ctx.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
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

                        // Si ya procesamos esta ruta física, evitar duplicar
                        if (path.isNotBlank() && !rutasExistentes.add(path)) continue

                        // Filtro de duración: solo si la duración ya fue calculada y el usuario configuró una duración mínima mayor a 0
                        if (cfg.excluirAudiosCortos && duracionMinMs > 0 && dur in 1 until duracionMinMs) {
                            continue
                        }

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

                        // Filtro 1: Excluir carpetas de notas de voz de mensajería exclusivamente
                        if (cfg.excluirCarpetasWhatsApp && carpetasExcluidas.any { carpeta ->
                            parentFolder.contains(carpeta, ignoreCase = true)
                        }) continue

                        // Filtro 2: Excluir archivos por prefijo de notas de voz
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

        // Escaneo complementario directo de carpetas estándar (Music y Download) para pistas no indexadas
        val directoriosPublicos = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File("/storage/emulated/0/Music"),
                File("/storage/emulated/0/Download")
            ).filter { it.exists() && it.canRead() }.distinctBy { it.absolutePath }

            fun escanearDirectorio(dir: File, nivel: Int = 0) {
                if (nivel > 2) return
                try {
                    val archivos = dir.listFiles() ?: return
                    for (f in archivos) {
                        if (f.isDirectory) {
                            val nom = f.name
                            if (!nom.startsWith(".") && !carpetasExcluidas.any { nom.contains(it, true) }) {
                                escanearDirectorio(f, nivel + 1)
                            }
                        } else if (f.isFile && f.length() > 64 * 1024) { // más de 64KB
                            val ext = f.extension.lowercase()
                            if (extensionesAudioSoportadas.contains(ext)) {
                                val ruta = f.absolutePath
                                if (rutasExistentes.add(ruta)) {
                                    val idLocal = -kotlin.math.abs(ruta.hashCode().toLong())
                                    val nombreLimpio = f.nameWithoutExtension.trim()
                                    val carpetaPadre = f.parentFile?.name ?: "Descargas"
                                    lista.add(
                                        CancionMotera(
                                            id = idLocal,
                                            titulo = nombreLimpio,
                                            artista = "Pista Local",
                                            album = carpetaPadre,
                                            duracionMs = 0L,
                                            rutaArchivo = ruta,
                                            uriStr = Uri.fromFile(f).toString(),
                                            portadaUriStr = null,
                                            fechaAgregada = f.lastModified(),
                                            esFavorita = favs.contains(idLocal),
                                            tamanoBytes = f.length(),
                                            carpetaContenedora = carpetaPadre
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            for (dir in directoriosPublicos) {
                escanearDirectorio(dir)
            }

            withContext(Dispatchers.Main) {
                _todasLasCanciones.value = lista
                if (_colaReproduccion.value.isEmpty() && lista.isNotEmpty()) {
                    _colaReproduccion.value = lista
                }
                restaurarUltimaCancionSiExiste()
                Log.d(ETIQUETA, "Escaneo completado: ${lista.size} canciones encontradas.")
            }

            // Persistir biblioteca en Room
            try {
                cancionDao?.upsertAll(lista.map { CancionLocalEntity.fromCancionMotera(it) })
            } catch (e: Exception) {
                Log.w(ETIQUETA, "Error guardando en Room: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROLES DE REPRODUCCIÓN (PLAY, PAUSE, NEXT, PREV, SEEK)
    // ─────────────────────────────────────────────────────────────────────────

    fun reproducirCancion(cancion: CancionMotera, nuevaCola: List<CancionMotera>? = null) {
        val ctx = contextoApp ?: return

        jobAutoSkip?.cancel()
        jobAutoSkip = null
        jobProgreso?.cancel()
        jobProgreso = null

        nuevaCola?.let {
            _colaReproduccion.value = it
        }

        val colaActual = _colaReproduccion.value
        indiceColaActual = colaActual.indexOfFirst { it.id == cancion.id }.takeIf { it >= 0 } ?: 0

        _cancionActual.value = cancion
        _posicionActualMs.value = 0L
        _duracionTotalMs.value = cancion.duracionMs
        _estado.value = EstadoReproductor.CARGANDO

        // Resetear marca de tiempo ANTES de cualquier operación async para evitar herencia de canción anterior
        tiempoInicioReproduccionMs = 0L

        guardarUltimaCancion(cancion.id)
        cargarCaratulaParaCancion(cancion)

        // Limpieza previa de descriptores abiertos
        try { activoAfd?.close() } catch (_: Exception) {}
        activoAfd = null
        try { activoPfd?.close() } catch (_: Exception) {}
        activoPfd = null
        try { activoFis?.close() } catch (_: Exception) {}
        activoFis = null

        errorEnCurso = false
        solicitarFocoAudio()

        // Ejecutar reproducción bajo mutex para serializar transiciones
        scopeCoroutine.launch(Dispatchers.IO) {
            playbackMutex.withLock {
                reproducirCancionInternal(ctx, cancion)
            }
        }
    }

    /**
     * Lógica interna de reproducción protegida por mutex.
     * Debe llamarse desde coroutine en Dispatchers.IO con playbackMutex held.
     */
    private suspend fun reproducirCancionInternal(
        ctx: Context,
        cancion: CancionMotera
    ) {
        val uriAudio = if (cancion.uriStr.isNotBlank()) Uri.parse(cancion.uriStr) else Uri.fromFile(File(cancion.rutaArchivo))
        val rutaFisica = cancion.rutaArchivo

        logDiagnostico("▶ reproducirCancionInternal: ${cancion.titulo} | $uriAudio")

        // Liberar SÍNCRONAMENTE antes de crear nuevo MediaPlayer
        awaitLiberarMediaPlayer()

        val mp = MediaPlayer().also { mediaPlayer = it }

        try {
            mp.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // ═══════════════════════════════════════════════════════════════════
                // PIPELINE ROBUSTO DE ORIGEN DE DATOS CON ASSETFILEDESCRIPTOR
                // ═══════════════════════════════════════════════════════════════════
                var fuenteConfigurada = false

                // Etapa 1: Apertura oficial AssetFileDescriptor con offset y declaredLength
                if (cancion.uriStr.isNotBlank() && cancion.uriStr.startsWith("content://")) {
                    try {
                        val afd = ctx.contentResolver.openAssetFileDescriptor(Uri.parse(cancion.uriStr), "r")
                        if (afd != null) {
                            activoAfd = afd
                            if (afd.declaredLength < 0) {
                                setDataSource(afd.fileDescriptor)
                            } else {
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
                            }
                            fuenteConfigurada = true
                            logDiagnostico("setDataSource OK vía AssetFileDescriptor (${afd.declaredLength} bytes)")
                        }
                    } catch (e: Exception) {
                        logDiagnostico("Etapa 1 AFD falló (${e.message}) — probando etapa 2...")
                    }
                }

                // Etapa 2: FileDescriptor persistente vía ContentResolver
                if (!fuenteConfigurada && cancion.uriStr.isNotBlank() && cancion.uriStr.startsWith("content://")) {
                    try {
                        val pfd = ctx.contentResolver.openFileDescriptor(Uri.parse(cancion.uriStr), "r")
                        if (pfd != null) {
                            activoPfd = pfd
                            setDataSource(pfd.fileDescriptor)
                            fuenteConfigurada = true
                            logDiagnostico("setDataSource OK vía ContentResolver PFD persistente")
                        }
                    } catch (e: Exception) {
                        logDiagnostico("Etapa 2 PFD falló (${e.message}) — probando etapa 3...")
                    }
                }

                // Etapa 3: Apertura oficial por Context + Uri
                if (!fuenteConfigurada && cancion.uriStr.isNotBlank()) {
                    try {
                        setDataSource(ctx, uriAudio)
                        fuenteConfigurada = true
                        logDiagnostico("setDataSource OK vía Context + URI")
                    } catch (e: Exception) {
                        logDiagnostico("Etapa 3 Context URI falló (${e.message}) — probando etapa 4...")
                    }
                }

                // Etapa 4: FileDescriptor persistente vía FileInputStream
                if (!fuenteConfigurada && rutaFisica.isNotBlank()) {
                    val f = File(rutaFisica)
                    if (f.exists() && f.canRead() && f.length() > 0) {
                        try {
                            val fis = java.io.FileInputStream(f)
                            activoFis = fis
                            setDataSource(fis.fd, 0, f.length())
                            fuenteConfigurada = true
                            logDiagnostico("setDataSource OK vía FileInputStream FD persistente: $rutaFisica")
                        } catch (e: Exception) {
                            logDiagnostico("Etapa 4 FIS falló (${e.message}) — probando etapa 5...")
                        }
                    }
                }

                // Etapa 5: Ruta física directa (fallback)
                if (!fuenteConfigurada && rutaFisica.isNotBlank() && File(rutaFisica).exists()) {
                    try {
                        setDataSource(rutaFisica)
                        fuenteConfigurada = true
                        logDiagnostico("setDataSource OK vía ruta física directa")
                    } catch (e: Exception) {
                        logDiagnostico("Etapa 5 Ruta directa falló (${e.message})")
                    }
                }

                if (!fuenteConfigurada) {
                    throw java.io.IOException("No se pudo abrir descriptor para pista: ${cancion.titulo}")
                }

                setOnPreparedListener { mpPrepared ->
                    val durLeida = mpPrepared.duration.toLong()
                    val durReal = if (durLeida > 0) durLeida else cancion.duracionMs
                    logDiagnostico("ON_PREPARED: ${cancion.titulo} dur=${durLeida}ms (durReal=${durReal}ms) session=${mpPrepared.audioSessionId}")
                    if (errorEnCurso) {
                        logDiagnostico("BLOQUEADO por errorEnCurso")
                        return@setOnPreparedListener
                    }
                    try {
                        mpPrepared.start()
                        logDiagnostico("start() OK: ${cancion.titulo}")
                    } catch (e: Exception) {
                        logDiagnostico("FALLO start(): ${e.message}")
                        scopeCoroutine.launch(Dispatchers.IO) { awaitLiberarMediaPlayer() }
                        scopeCoroutine.launch(Dispatchers.Main) { _estado.value = EstadoReproductor.DETENIDO }
                        return@setOnPreparedListener
                    }

                    intentosErrorConsecutivos = 0
                    autoSkipsConsecutivos = 0
                    errorEnCurso = false
                    tiempoInicioReproduccionMs = System.currentTimeMillis()
                    _estado.value = EstadoReproductor.REPRODUCIENDO
                    _duracionTotalMs.value = durReal
                    iniciarMonitoreoProgreso()
                    mostrarNotificacion()
                    try {
                        MOTOR_AUDIO_NATIVO.vincularAudioSession(mpPrepared.audioSessionId, _configuracion.value)
                    } catch (e: Exception) {
                        logDiagnostico("WARN AudioFX: ${e.message}")
                    }
                    logDiagnostico("REPRODUCIENDO EXITOSAMENTE: ${cancion.titulo} dur=${durReal}ms")
                }

                setOnCompletionListener {
                    logDiagnostico("🔄 ON_COMPLETION (errorEnCurso=$errorEnCurso, estado=${_estado.value}): ${cancion.titulo}")
                    if (!errorEnCurso && _estado.value == EstadoReproductor.REPRODUCIENDO) {
                        scopeCoroutine.launch(Dispatchers.Main) { alCompletarCancion() }
                    } else {
                        logDiagnostico("⚠️ ON_COMPLETION BLOQUEADO por error en curso o estado no reproduciendo")
                    }
                }

                setOnErrorListener { mpError, what, extra ->
                    if (what == -38 || extra == -38) {
                        logDiagnostico("⚠️ ON_ERROR transitorio ignorado (what=$what extra=$extra): ${cancion.titulo}")
                        return@setOnErrorListener true
                    }

                    logDiagnostico("❌ ON_ERROR [what=$what extra=$extra]: ${cancion.titulo}")

                    // Si es un error transitorio al cambiar de canción, reintentar UNA sola vez
                    // de forma limpia y transparente sin bloquear ni caer en bucle
                    if (intentosErrorConsecutivos < 1) {
                        intentosErrorConsecutivos++
                        logDiagnostico("🔄 Reintentando reproducción limpia de '${cancion.titulo}' (intento 1)...")
                        scopeCoroutine.launch(Dispatchers.IO) { awaitLiberarMediaPlayer() }
                        scopeCoroutine.launch(Dispatchers.Main) {
                            delay(120)
                            reproducirCancion(cancion)
                        }
                        return@setOnErrorListener true
                    }

                    scopeCoroutine.launch(Dispatchers.IO) { awaitLiberarMediaPlayer() }
                    scopeCoroutine.launch(Dispatchers.Main) {
                        _estado.value = EstadoReproductor.DETENIDO
                        jobProgreso?.cancel()
                        intentosErrorConsecutivos = 0
                        autoSkipsConsecutivos = 0
                        errorEnCurso = false
                        ocultarNotificacion()
                        abandonarFocoAudio()
                        android.widget.Toast.makeText(ctx, "No se pudo reproducir '${cancion.titulo}'.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                setOnSeekCompleteListener {
                    logDiagnostico("SEEK_COMPLETE: posición=${it.currentPosition}ms")
                }

                logDiagnostico("🔧 prepareAsync iniciado")
                prepareAsync()
            }
        } catch (e: Exception) {
            logDiagnostico("💥 EXCEPCIÓN MediaPlayer: ${e.message}")
            awaitLiberarMediaPlayer()
            scopeCoroutine.launch(Dispatchers.Main) {
                _estado.value = EstadoReproductor.DETENIDO
                autoSkipsConsecutivos = 0
                intentosErrorConsecutivos = 0
                errorEnCurso = false
                ocultarNotificacion()
                abandonarFocoAudio()
                android.widget.Toast.makeText(ctx, "No se pudo reproducir el audio. Formato no compatible.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Libera el MediaPlayer de forma segura, cerrando descriptores y desvinculando efectos de hardware.
     * Función SÍNCRONA/BLOQUEANTE: garantiza que release() nativo complete antes de retornar.
     */
    private suspend fun liberarMediaPlayer() {
        errorEnCurso = true
        jobProgreso?.cancel()
        jobProgreso = null
        val mp = mediaPlayer
        mediaPlayer = null
        if (mp != null) {
            try {
                // PRIMERO desvincular todos los listeners para evitar callbacks asíncronos residuales
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
                mp.setOnInfoListener(null)
                mp.setOnSeekCompleteListener(null)
                if (mp.isPlaying) {
                    try { mp.stop() } catch (_: Exception) {}
                }
                mp.reset()
                mp.release()
                // Pequeña pausa para asegurar liberación nativa completa
                try { Thread.sleep(50) } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
        try { activoAfd?.close() } catch (_: Exception) {}
        activoAfd = null
        try { activoPfd?.close() } catch (_: Exception) {}
        activoPfd = null
        try { activoFis?.close() } catch (_: Exception) {}
        activoFis = null
        try {
            MOTOR_AUDIO_NATIVO.liberarEfectos()
        } catch (_: Exception) {}
        errorEnCurso = false
    }

    /**
     * Wrapper seguro para llamar liberarMediaPlayer() desde callbacks no-suspend.
     */
    private suspend fun awaitLiberarMediaPlayer() = liberarMediaPlayer()

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

    fun siguienteCancion(esAutoSkip: Boolean = false) {
        scopeCoroutine.launch(Dispatchers.IO) {
            playbackMutex.withLock {
                val cola = _colaReproduccion.value
                if (cola.isEmpty()) {
                    logDiagnostico("⏭ siguienteCancion: cola vacía")
                    return@withLock
                }

                if (!esAutoSkip) {
                    // Acción manual del usuario: cancelar auto-skips y resetear contadores de error
                    jobAutoSkip?.cancel()
                    jobAutoSkip = null
                    intentosErrorConsecutivos = 0
                    autoSkipsConsecutivos = 0
                }
                errorEnCurso = false

                logDiagnostico("⏭ siguienteCancion (auto=$esAutoSkip): indiceActual=$indiceColaActual tamañoCola=${cola.size}")

                val cancionSiguiente: CancionMotera
                if (_modoAleatorio.value && cola.size > 1) {
                    var nuevoIndice = (cola.indices).random()
                    if (nuevoIndice == indiceColaActual) {
                        nuevoIndice = (nuevoIndice + 1) % cola.size
                    }
                    indiceColaActual = nuevoIndice
                    cancionSiguiente = cola[indiceColaActual]
                    logDiagnostico("🎲 Aleatorio: nuevoIndice=$nuevoIndice -> ${cancionSiguiente.titulo}")
                } else {
                    var siguienteIndice = indiceColaActual + 1
                    if (siguienteIndice >= cola.size) {
                        if (_modoBucle.value == ModoBucle.SIN_BUCLE) {
                            logDiagnostico("⏹ Fin de cola, deteniendo")
                            scopeCoroutine.launch(Dispatchers.Main) { detener() }
                            return@withLock
                        }
                        siguienteIndice = 0
                    }
                    indiceColaActual = siguienteIndice
                    cancionSiguiente = cola[indiceColaActual]
                    logDiagnostico("▶ Siguiente: indice=$siguienteIndice -> ${cancionSiguiente.titulo}")
                }

                // Actualizar estado UI y lanzar reproducción interna (ya estamos bajo mutex)
                scopeCoroutine.launch(Dispatchers.Main) {
                    _cancionActual.value = cancionSiguiente
                    _posicionActualMs.value = 0L
                    _duracionTotalMs.value = cancionSiguiente.duracionMs
                    _estado.value = EstadoReproductor.CARGANDO
                    tiempoInicioReproduccionMs = 0L
                    guardarUltimaCancion(cancionSiguiente.id)
                    cargarCaratulaParaCancion(cancionSiguiente)
                    // Limpieza previa de descriptores
                    try { activoAfd?.close() } catch (_: Exception) {}
                    activoAfd = null
                    try { activoPfd?.close() } catch (_: Exception) {}
                    activoPfd = null
                    try { activoFis?.close() } catch (_: Exception) {}
                    activoFis = null
                    errorEnCurso = false
                    solicitarFocoAudio()
                }

                // Llamar reproducción interna directamente (ya bajo mutex)
                val ctx = contextoApp ?: return@withLock
                reproducirCancionInternal(ctx, cancionSiguiente)
            }
        }
    }

    fun anteriorCancion() {
        scopeCoroutine.launch(Dispatchers.IO) {
            playbackMutex.withLock {
                val mp = mediaPlayer
                if (mp != null && try { mp.currentPosition > 3000 } catch (_: Exception) { false }) {
                    // Si ya pasaron más de 3 segundos, reiniciar la pista actual
                    scopeCoroutine.launch(Dispatchers.Main) { buscarPosicion(0L) }
                    return@withLock
                }

                val cola = _colaReproduccion.value
                if (cola.isEmpty()) return@withLock

                jobAutoSkip?.cancel()
                jobAutoSkip = null
                intentosErrorConsecutivos = 0
                autoSkipsConsecutivos = 0
                errorEnCurso = false

                var anteriorIndice = indiceColaActual - 1
                if (anteriorIndice < 0) {
                    anteriorIndice = cola.size - 1
                }

                indiceColaActual = anteriorIndice
                val cancionAnterior = cola[indiceColaActual]

                // Actualizar estado UI y lanzar reproducción interna (ya bajo mutex)
                scopeCoroutine.launch(Dispatchers.Main) {
                    _cancionActual.value = cancionAnterior
                    _posicionActualMs.value = 0L
                    _duracionTotalMs.value = cancionAnterior.duracionMs
                    _estado.value = EstadoReproductor.CARGANDO
                    tiempoInicioReproduccionMs = 0L
                    guardarUltimaCancion(cancionAnterior.id)
                    cargarCaratulaParaCancion(cancionAnterior)
                    // Limpieza previa de descriptores
                    try { activoAfd?.close() } catch (_: Exception) {}
                    activoAfd = null
                    try { activoPfd?.close() } catch (_: Exception) {}
                    activoPfd = null
                    try { activoFis?.close() } catch (_: Exception) {}
                    activoFis = null
                    errorEnCurso = false
                    solicitarFocoAudio()
                }

                val ctx = contextoApp ?: return@withLock
                reproducirCancionInternal(ctx, cancionAnterior)
            }
        }
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
        scopeCoroutine.launch(Dispatchers.IO) {
            playbackMutex.withLock {
                awaitLiberarMediaPlayer()
                scopeCoroutine.launch(Dispatchers.Main) {
                    _estado.value = EstadoReproductor.DETENIDO
                    _posicionActualMs.value = 0L
                    jobProgreso?.cancel()
                    ocultarNotificacion()
                    abandonarFocoAudio()
                }
            }
        }
    }

    private fun alCompletarCancion() {
        // Protección 1: Si el reproductor no está reproduciendo activamente, ignorar callbacks espurios
        if (_estado.value != EstadoReproductor.REPRODUCIENDO) {
            logDiagnostico("⚠️ alCompletarCancion ignorado: estado actual es ${_estado.value} (no REPRODUCIENDO)")
            return
        }

        // Protección 2: Si nunca se inició el playback real, ignorar evento espurio
        if (tiempoInicioReproduccionMs <= 0L) {
            logDiagnostico("⚠️ alCompletarCancion ignorado: tiempoInicioReproduccionMs no válido ($tiempoInicioReproduccionMs)")
            return
        }

        val tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioReproduccionMs
        logDiagnostico("🎵 alCompletarCancion: '${_cancionActual.value?.titulo}' tiempo=${tiempoTranscurrido}ms modoBucle=${_modoBucle.value}")

        // Si terminó en menos de 2000ms, es un fallo prematuro o descriptor cerrado
        val esFalloPrematuro = tiempoTranscurrido < MIN_DURACION_VALIDA_MS

        if (esFalloPrematuro) {
            autoSkipsConsecutivos++
            logDiagnostico("⚠️ FALLO PREMATURO / COMPLETION INMEDIATO [auto=$autoSkipsConsecutivos/$MAX_AUTO_SKIPS]")

            val cola = _colaReproduccion.value
            val maxPermitido = kotlin.math.min(MAX_AUTO_SKIPS, if (cola.isNotEmpty()) cola.size else MAX_AUTO_SKIPS)

            if (autoSkipsConsecutivos >= maxPermitido) {
                logDiagnostico("🛑 DEMASIADOS AUTO-SKIPS por fallo prematuro. Deteniendo reproducción.")
                autoSkipsConsecutivos = 0
                detener()
                val ctx = contextoApp
                if (ctx != null) {
                    scopeCoroutine.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(ctx, "Reproducción detenida: varios audios consecutivos no pudieron reproducirse.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                return
            } else {
                jobAutoSkip?.cancel()
                jobAutoSkip = scopeCoroutine.launch {
                    delay(350)
                    siguienteCancion(esAutoSkip = true)
                }
                return
            }
        } else {
            autoSkipsConsecutivos = 0
        }

        when (_modoBucle.value) {
            ModoBucle.BUCLE_UNA -> {
                val actual = _cancionActual.value
                if (actual != null) reproducirCancion(actual)
            }
            ModoBucle.BUCLE_TODAS, ModoBucle.SIN_BUCLE -> {
                siguienteCancion(esAutoSkip = false)
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
            val favs = _favoritasIds.value
            cancionDao?.let { dao ->
                try {
                    dao.getAllCanciones().forEach { c ->
                        val esFav = favs.contains(c.id)
                        if (c.esFavorita != esFav) {
                            dao.updateFavorita(c.id, esFav)
                        }
                    }
                } catch (_: Exception) {}
            }
            val arr = JSONArray(favs)
            prefs?.edit()?.putString("favoritas_ids", arr.toString())?.apply()
        }
    }

    private fun cargarFavoritasGuardadas() {
        scopeCoroutine.launch(Dispatchers.IO) {
            val favsDb = try {
                cancionDao?.getAllCanciones()?.filter { it.esFavorita }?.map { it.id }?.toSet()
            } catch (_: Exception) { null }

            if (!favsDb.isNullOrEmpty()) {
                _favoritasIds.value = favsDb
            } else {
                val str = prefs?.getString("favoritas_ids", null) ?: return@launch
                try {
                    val arr = JSONArray(str)
                    val set = mutableSetOf<Long>()
                    for (i in 0 until arr.length()) set.add(arr.getLong(i))
                    _favoritasIds.value = set
                } catch (_: Exception) {}
            }
        }
    }

    private fun guardarListas() {
        scopeCoroutine.launch(Dispatchers.IO) {
            val dao = listaDao
            _listasPersonalizadas.value.forEach { lista ->
                try {
                    dao?.upsert(
                        ListaReproduccionEntity(
                            id = lista.id,
                            nombre = lista.nombre,
                            descripcion = lista.descripcion,
                            fechaCreacion = lista.fechaCreacion,
                            icono = lista.icono,
                            cancionIdsJson = JSONArray(lista.cancionIds).toString()
                        )
                    )
                } catch (_: Exception) {}
            }

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
        scopeCoroutine.launch(Dispatchers.IO) {
            val listasDb = try { listaDao?.getAllListas() } catch (_: Exception) { null }
            if (!listasDb.isNullOrEmpty()) {
                val lista = listasDb.map { entidad ->
                    val ids = mutableListOf<Long>()
                    try {
                        val arr = JSONArray(entidad.cancionIdsJson)
                        for (j in 0 until arr.length()) ids.add(arr.getLong(j))
                    } catch (_: Exception) {}
                    ListaReproduccionMotera(
                        id = entidad.id,
                        nombre = entidad.nombre,
                        descripcion = entidad.descripcion,
                        fechaCreacion = entidad.fechaCreacion,
                        cancionIds = ids,
                        icono = entidad.icono
                    )
                }
                _listasPersonalizadas.value = lista
            } else {
                val str = prefs?.getString("listas_guardadas", null) ?: return@launch
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
        }
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
            val pos = _posicionActualMs.value
            val bucle = _modoBucle.value.ordinal
            val aleatorio = _modoAleatorio.value

            try {
                estadoDao?.upsert(
                    EstadoReproductorEntity(
                        clave = "global",
                        ultimaCancionId = cancionId,
                        colaIdsJson = JSONArray(colaIds).toString(),
                        indiceColaActual = indiceColaActual,
                        modoBucle = bucle,
                        modoAleatorio = aleatorio,
                        posicionMs = pos,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}

            prefs?.edit()
                ?.putLong("ultima_cancion_id", cancionId)
                ?.putString("cola_reproduccion_ids", JSONArray(colaIds).toString())
                ?.apply()
        }
    }

    private fun restaurarUltimaCancionSiExiste() {
        if (_cancionActual.value != null) return
        scopeCoroutine.launch(Dispatchers.IO) {
            val estadoDb = try { estadoDao?.getEstado("global") } catch (_: Exception) { null }
            val ultimaId = estadoDb?.ultimaCancionId ?: prefs?.getLong("ultima_cancion_id", -1L) ?: -1L

            if (ultimaId > 0) {
                val cancion = _todasLasCanciones.value.find { it.id == ultimaId }
                if (cancion != null) {
                    withContext(Dispatchers.Main) {
                        _cancionActual.value = cancion
                        _duracionTotalMs.value = cancion.duracionMs
                        cargarCaratulaParaCancion(cancion)
                    }

                    val strCola = estadoDb?.colaIdsJson ?: prefs?.getString("cola_reproduccion_ids", null)
                    if (strCola != null) {
                        try {
                            val arr = JSONArray(strCola)
                            val ids = mutableListOf<Long>()
                            for (i in 0 until arr.length()) ids.add(arr.getLong(i))
                            val colaRestaurada = ids.mapNotNull { id -> _todasLasCanciones.value.find { it.id == id } }
                            if (colaRestaurada.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    _colaReproduccion.value = colaRestaurada
                                    indiceColaActual = colaRestaurada.indexOfFirst { it.id == ultimaId }
                                }
                            }
                        } catch (_: Exception) {}
                    }
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
            duracionMinimaSegundos = p.getInt("duracion_min_seg", 0),
            ultraVolumenNivel = p.getFloat("ultra_vol", 1.0f),
            superBassNivel = p.getFloat("super_bass", 0.4f),
            espacialidadNivel = p.getFloat("espacialidad", 0.2f),
            bandasEcualizador = bandas,
            presetActual = p.getString("preset_actual", "Rock Motero") ?: "Rock Motero",
            excluirCarpetasWhatsApp = p.getBoolean("excluir_whatsapp", false),
            excluirAudiosCortos = p.getBoolean("excluir_audios_cortos", false),
            descargaCaratulasModo = modoCaratulas
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NOTIFICACIÓN DEL SISTEMA CON CONTROLES MULTIMEDIA
    // ─────────────────────────────────────────────────────────────────────────

    private fun mostrarNotificacion() {
        val ctx = contextoApp ?: return
        ReproductorForegroundService.iniciarServicio(ctx)
        WIDGET_REPRODUCTOR_TX.actualizarTodosLosWidgets(ctx)
    }

    private fun ocultarNotificacion() {
        val ctx = contextoApp ?: return
        ReproductorForegroundService.detenerServicio(ctx)
        WIDGET_REPRODUCTOR_TX.actualizarTodosLosWidgets(ctx)
    }

    private fun iniciarMonitoreoProgreso() {
        jobProgreso?.cancel()
        jobProgreso = scopeCoroutine.launch {
            delay(250) // Pausa de estabilización para evitar colisión de estado nativo
            while (_estado.value == EstadoReproductor.REPRODUCIENDO) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && _estado.value == EstadoReproductor.REPRODUCIENDO) {
                        try {
                            if (mp.isPlaying) {
                                _posicionActualMs.value = mp.currentPosition.toLong()
                            }
                        } catch (_: Exception) {}
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
