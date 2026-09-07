package com.example.reproductor

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════
// GESTOR DE DESCARGAS - REPRODUCTOR TX PRO
// Descarga directa a 320kbps, registro en MediaStore y biblioteca de la app.
// ═══════════════════════════════════════════════════════════════════════════

object GESTOR_DESCARGAS_YT {

    private const val ETIQUETA = "GESTOR_DESCARGAS_YT"
    private const val NOMBRE_LISTA_DESCARGAS = "Descargas TX"

    private val scopeCoroutine = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var contextoApp: Context? = null

    // Estado del panel de búsqueda
    private val _estadoPanel = MutableStateFlow<EstadoPanelYT>(EstadoPanelYT.Inactivo)
    val estadoPanel: StateFlow<EstadoPanelYT> = _estadoPanel.asStateFlow()

    // Cola de descargas activas
    private val _descargasActivas = MutableStateFlow<List<EstadoDescarga>>(emptyList())
    val descargasActivas: StateFlow<List<EstadoDescarga>> = _descargasActivas.asStateFlow()

    // Historial de descargas completadas
    private val _descargasCompletadas = MutableStateFlow<List<InfoDescargaCompleta>>(emptyList())
    val descargasCompletadas: StateFlow<List<InfoDescargaCompleta>> = _descargasCompletadas.asStateFlow()

    val totalDescargasEnCurso: Int
        get() = _descargasActivas.value.count {
            it.estado == TipoEstadoDescarga.DESCARGANDO ||
            it.estado == TipoEstadoDescarga.CONVIRTIENDO ||
            it.estado == TipoEstadoDescarga.GUARDANDO
        }

    fun inicializar(contexto: Context) {
        if (contextoApp != null) return
        contextoApp = contexto.applicationContext
        obtenerCarpetaDescargas()
        logDescarga("Gestor de descargas inicializado")
    }

    private fun logDescarga(mensaje: String) {
        Log.d(ETIQUETA, mensaje)
        try {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            val ts = sdf.format(Date())
            val file = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "tx_reproductor_log.txt"
            )
            file.appendText("[$ts DESCARGA] $mensaje\n")
        } catch (_: Exception) {}
    }

    private fun obtenerCarpetaDescargas(): File {
        val carpetaMusica = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val carpetaTX = File(carpetaMusica, "TX_PRO_Descargas")
        if (!carpetaTX.exists()) {
            carpetaTX.mkdirs()
        }
        return carpetaTX
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BÚSQUEDA
    // ─────────────────────────────────────────────────────────────────────────

    fun iniciarBusqueda(termino: String) {
        if (termino.isBlank()) return

        _estadoPanel.value = EstadoPanelYT.Buscando(termino)

        scopeCoroutine.launch {
            try {
                val resultados = BUSCADOR_YT.buscar(termino)
                if (resultados.isNotEmpty()) {
                    _estadoPanel.value = EstadoPanelYT.ListaResultados(resultados, termino)
                } else {
                    _estadoPanel.value = EstadoPanelYT.ErrorBusqueda("No se encontraron resultados para '$termino'")
                }
            } catch (e: Exception) {
                Log.e(ETIQUETA, "Error en búsqueda: ${e.message}")
                _estadoPanel.value = EstadoPanelYT.ErrorBusqueda("Error de conexión: ${e.message}")
            }
        }
    }

    fun limpiarBusqueda() {
        _estadoPanel.value = EstadoPanelYT.Inactivo
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DESCARGA
    // ─────────────────────────────────────────────────────────────────────────

    fun iniciarDescarga(resultado: ResultadoBusquedaYT) {
        if (_descargasActivas.value.any { it.idVideo == resultado.idVideo && it.estado != TipoEstadoDescarga.ERROR }) {
            logDescarga("Ya se está descargando: ${resultado.titulo}")
            return
        }

        logDescarga("=== INICIO DESCARGA: ${resultado.titulo} (${resultado.duracionFormateada}) ===")
        logDescarga("URL directa: ${resultado.urlDescargaDirecta ?: "null (se resolverá)"}")

        val nuevaDescarga = EstadoDescarga(
            idVideo = resultado.idVideo,
            titulo = resultado.titulo,
            estado = TipoEstadoDescarga.EN_COLA
        )
        _descargasActivas.value = _descargasActivas.value + nuevaDescarga

        scopeCoroutine.launch {
            try {
                actualizarEstado(resultado.idVideo, TipoEstadoDescarga.DESCARGANDO, 0f)

                // 1. Obtener URL de stream directo
                val urlDirecta = resultado.urlDescargaDirecta ?: resolverUrlDirecta(resultado)
                if (urlDirecta.isNullOrBlank()) {
                    actualizarEstado(resultado.idVideo, TipoEstadoDescarga.ERROR, mensaje = "No se pudo obtener la URL de descarga directa")
                    return@launch
                }

                // 2. Descargar archivo
                val nombreLimpio = limpiarNombreArchivo("${resultado.autor} - ${resultado.titulo}")
                val archivoDestino = File(obtenerCarpetaDescargas(), "$nombreLimpio.mp3")

                val descargaExitosa = descargarStream(urlDirecta, archivoDestino, resultado.idVideo) { progreso ->
                    actualizarEstado(resultado.idVideo, TipoEstadoDescarga.DESCARGANDO, progreso)
                }

                if (!descargaExitosa || !archivoDestino.exists() || archivoDestino.length() == 0L) {
                    actualizarEstado(resultado.idVideo, TipoEstadoDescarga.ERROR, mensaje = "Error al descargar el flujo de audio")
                    return@launch
                }

                // 3. Registrar en MediaStore y escanear
                actualizarEstado(resultado.idVideo, TipoEstadoDescarga.GUARDANDO, 1f)
                val rutaFinal = archivoDestino.absolutePath
                notificarMediaStore(archivoDestino, resultado)

                logDescarga("Iniciando escáneo de biblioteca...")
                GESTOR_AUDIO_TX.escanearMusicaLocal()

                // Esperar a que el escáneo termine
                delay(3000)
                logDescarga("Buscando canción en biblioteca: rutaFinal=$rutaFinal")

                var cancionEncontrada = GESTOR_AUDIO_TX.todasLasCanciones.value.find {
                    it.rutaArchivo == rutaFinal
                }

                if (cancionEncontrada == null) {
                    logDescarga("No encontrada por ruta exacta. Buscando por título...")
                    cancionEncontrada = GESTOR_AUDIO_TX.todasLasCanciones.value.find {
                        it.titulo.contains(resultado.titulo, ignoreCase = true) ||
                        resultado.titulo.contains(it.titulo, ignoreCase = true)
                    }
                }

                if (cancionEncontrada == null) {
                    logDescarga("No encontrada en biblioteca. Reintentando escáneo...")
                    GESTOR_AUDIO_TX.escanearMusicaLocal()
                    delay(3000)
                    cancionEncontrada = GESTOR_AUDIO_TX.todasLasCanciones.value.find {
                        it.rutaArchivo == rutaFinal
                    }
                }

                if (cancionEncontrada != null) {
                    logDescarga("✅ Canción encontrada: '${cancionEncontrada.titulo}' ID=${cancionEncontrada.id}")
                    GESTOR_AUDIO_TX.agregarDescargaAListaDescargas(cancionEncontrada.id)
                    logDescarga("✅ Agregada a lista 'Descargas TX'")
                } else {
                    logDescarga("❌ Canción NO encontrada tras 2 intentos. Archivo: $rutaFinal")
                    logDescarga("   Biblioteca tiene ${GESTOR_AUDIO_TX.todasLasCanciones.value.size} canciones")
                    // Último recurso: re-escanear con config relajada
                    GESTOR_AUDIO_TX.escanearMusicaLocal()
                    delay(5000)
                    cancionEncontrada = GESTOR_AUDIO_TX.todasLasCanciones.value.find {
                        it.rutaArchivo == rutaFinal
                    }
                    if (cancionEncontrada != null) {
                        GESTOR_AUDIO_TX.agregarDescargaAListaDescargas(cancionEncontrada.id)
                        logDescarga("✅ Encontrada en 3er intento: '${cancionEncontrada.titulo}'")
                    } else {
                        logDescarga("❌ FALLO TOTAL. Verificar duración mínima en ajustes.")
                    }
                }

                actualizarEstado(resultado.idVideo, TipoEstadoDescarga.COMPLETADO, 1f, rutaGuardado = rutaFinal)
                val infoCompleta = InfoDescargaCompleta(
                    idVideo = resultado.idVideo,
                    titulo = resultado.titulo,
                    autor = resultado.autor,
                    duracionSegundos = resultado.duracionSegundos,
                    rutaArchivo = rutaFinal,
                    uriMediaStore = rutaFinal
                )
                _descargasCompletadas.value = _descargasCompletadas.value + infoCompleta

                logDescarga("✅ Descarga finalizada: $rutaFinal")

            } catch (e: Exception) {
                Log.e(ETIQUETA, "Error en descarga: ${e.message}")
                actualizarEstado(resultado.idVideo, TipoEstadoDescarga.ERROR, mensaje = "Error: ${e.message}")
            }
        }
    }

    private fun actualizarEstado(
        idVideo: String,
        estado: TipoEstadoDescarga,
        progreso: Float = 0f,
        mensaje: String? = null,
        rutaGuardado: String? = null
    ) {
        _descargasActivas.value = _descargasActivas.value.map { descarga ->
            if (descarga.idVideo == idVideo) {
                descarga.copy(
                    estado = estado,
                    progreso = progreso,
                    porcentaje = (progreso * 100).toInt(),
                    mensajeError = mensaje,
                    rutaArchivoGuardado = rutaGuardado
                )
            } else descarga
        }
    }

    /**
     * Resuelve la URL directa en caso de no venir precalculada.
     */
    private suspend fun resolverUrlDirecta(resultado: ResultadoBusquedaYT): String? = withContext(Dispatchers.IO) {
        // Intento 1: Buscar en JioSaavn por Título + Artista
        try {
            val q = URLEncoder.encode("${resultado.titulo} ${resultado.autor}", "UTF-8")
            val urlApi = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=5&p=1&q=$q"
            val conexion = URL(urlApi).openConnection() as HttpURLConnection
            conexion.connectTimeout = 6000
            conexion.readTimeout = 6000
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conexion.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val results = obj.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val more = first.optJSONObject("more_info")
                    val encUrl = more?.optString("encrypted_media_url", "") ?: ""
                    if (encUrl.isNotBlank()) {
                        val decrypted = BUSCADOR_YT.desencriptarUrlJioSaavn(encUrl)
                        if (!decrypted.isNullOrBlank()) {
                            Log.d(ETIQUETA, "✅ Stream resuelto vía Music API para: ${resultado.titulo}")
                            return@withContext decrypted
                        }
                    }
                }
            }
            conexion.disconnect()
        } catch (_: Exception) {}

        // Intento 2: Invidious API
        val videoId = resultado.idVideo
        val instancias = listOf(
            "https://invidious.privacydev.net",
            "https://invidious.fdn.fr",
            "https://inv.zzls.xyz",
            "https://inv.tux.pizza",
            "https://yewtu.be"
        )
        for (instancia in instancias) {
            try {
                val urlApi = "$instancia/api/v1/videos/$videoId"
                val conexion = URL(urlApi).openConnection() as HttpURLConnection
                conexion.connectTimeout = 5000
                conexion.readTimeout = 5000
                conexion.setRequestProperty("User-Agent", "Mozilla/5.0")

                if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonStr = conexion.inputStream.bufferedReader().use { it.readText() }
                    conexion.disconnect()
                    val obj = JSONObject(jsonStr)
                    val formats = obj.optJSONArray("adaptiveFormats")
                    if (formats != null) {
                        for (i in 0 until formats.length()) {
                            val f = formats.optJSONObject(i) ?: continue
                            val tipo = f.optString("type", "")
                            if (tipo.startsWith("audio/")) {
                                val urlDirecta = f.optString("url", "")
                                if (urlDirecta.isNotBlank()) {
                                    return@withContext urlDirecta
                                }
                            }
                        }
                    }
                } else {
                    conexion.disconnect()
                }
            } catch (_: Exception) {}
        }

        null
    }

    /**
     * Descarga el flujo de audio directamente hacia el archivo final.
     */
    private suspend fun descargarStream(
        urlDirecta: String,
        archivoDestino: File,
        idVideo: String,
        onProgreso: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val urlObj = URL(urlDirecta)
            val conexion = urlObj.openConnection() as HttpURLConnection
            conexion.connectTimeout = 12000
            conexion.readTimeout = 15000
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conexion.setRequestProperty("Accept", "*/*")
            conexion.setRequestProperty("Connection", "Keep-Alive")

            if (conexion.responseCode !in 200..299) {
                Log.e(ETIQUETA, "Error HTTP al descargar: ${conexion.responseCode}")
                conexion.disconnect()
                return@withContext false
            }

            val tamanhoTotal = conexion.contentLength.toLong()
            val inputStream: InputStream = conexion.inputStream
            val outputStream = FileOutputStream(archivoDestino)

            val buffer = ByteArray(16384)
            var bytesLeidos: Int
            var bytesTotales = 0L

            while (inputStream.read(buffer).also { bytesLeidos = it } != -1) {
                outputStream.write(buffer, 0, bytesLeidos)
                bytesTotales += bytesLeidos
                if (tamanhoTotal > 0) {
                    withContext(Dispatchers.Main) {
                        onProgreso((bytesTotales.toFloat() / tamanhoTotal.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            conexion.disconnect()

            true
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error descargando stream: ${e.message}")
            false
        }
    }

    /**
     * Registra el archivo descargado en MediaStore y el escáner del sistema.
     */
    private fun notificarMediaStore(archivo: File, resultado: ResultadoBusquedaYT) {
        val ctx = contextoApp ?: return
        try {
            MediaScannerConnection.scanFile(
                ctx,
                arrayOf(archivo.absolutePath),
                arrayOf("audio/mpeg")
            ) { path, uri ->
                Log.d(ETIQUETA, "MediaScanner indexó: $path -> $uri")
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error notificando MediaStore: ${e.message}")
        }
    }

    private fun limpiarNombreArchivo(nombre: String): String {
        return nombre.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}
