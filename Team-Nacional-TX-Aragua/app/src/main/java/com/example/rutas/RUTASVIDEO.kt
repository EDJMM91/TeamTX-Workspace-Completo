package com.example.rutas

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Módulo para la generación de reportes detallados y animación/video 2D acelerado (estilo Relive)
 * de las rutas registradas por el piloto. Utiliza la API nativa MediaProjection para grabar
 * la animación del recorrido en el mapa y guardarla directamente en formato MP4 en la galería del teléfono.
 *
 * Nomenclatura en español estricta.
 */
object RUTASVIDEO {

    private const val TAG = "TEAM_TX_RUTAS_VIDEO"

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private val _grabandoVideoState = MutableStateFlow(false)
    val grabandoVideoState: StateFlow<Boolean> = _grabandoVideoState.asStateFlow()

    private val _progresoAnimacionState = MutableStateFlow(0f)
    val progresoAnimacionState: StateFlow<Float> = _progresoAnimacionState.asStateFlow()

    private var archivoVideoSalida: File? = null

    /**
     * Genera un reporte detallado en formato texto estructurado con las estadísticas clave
     * de la ruta recorrida.
     *
     * @param ruta Objeto con los datos acumulados de la ruta.
     * @return Cadena formateada en español con el reporte técnico motero.
     */
    fun generarReporteTexto(ruta: ResumenRutaTX): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaInicio = sdf.format(Date(ruta.fechaInicioMs))
        val fechaFin = sdf.format(Date(ruta.fechaFinMs))
        val tiempoTotalMs = ruta.fechaFinMs - ruta.fechaInicioMs

        val horas = tiempoTotalMs / (1000 * 60 * 60)
        val minutos = (tiempoTotalMs % (1000 * 60 * 60)) / (1000 * 60)
        val segundos = (tiempoTotalMs % (1000 * 60)) / 1000

        val tiempoFormateado = String.format(Locale.getDefault(), "%02dh %02dm %02ds", horas, minutos, segundos)

        return """
            ===========================================
            🚩 REPORTE OFICIAL DE RUTA - TEAM TX ARAGUA
            ===========================================
            📌 Título: ${ruta.tituloRuta}
            👤 Piloto: ${ruta.nombrePiloto}
            📅 Fecha Inicio: $fechaInicio
            🏁 Fecha Final: $fechaFin
            ⏱️ Tiempo en Ruta: $tiempoFormateado
            -------------------------------------------
            📊 ESTADÍSTICAS EN TIEMPO REAL:
            🔹 Distancia Recorrida: %.2f Km
            🚀 Velocidad Máxima: %.1f Km/h
            🛵 Velocidad Promedio: %.1f Km/h
            📍 Puntos GPS Registrados: ${ruta.puntos.size}
            ⚡ Seguro Batería: ${ruta.nivelBateriaRespaldo}%%
            ===========================================
        """.trimIndent().format(
            ruta.distanciaTotalKm,
            ruta.velocidadMaximaKmh,
            ruta.velocidadPromedioKmh
        )
    }

    /**
     * Inicia la captura y animación 2D acelerada de la ruta, utilizando la API nativa de Android
     * MediaProjection para compilar el video MP4 en la galería.
     *
     * @param contexto Contexto de la aplicación.
     * @param mediaProjectionIntent Intent obtenido del launcher de MediaProjection.
     * @param resultCode Código de resultado otorgado por el usuario.
     * @param ruta Objeto de la ruta a animar y procesar.
     * @param alTerminar Callback que entrega la Uri o ruta del archivo MP4 resultante.
     */
    fun crearVideo(
        contexto: Context,
        mediaProjectionIntent: Intent,
        resultCode: Int,
        ruta: ResumenRutaTX,
        alTerminar: (String) -> Unit
    ) {
        try {
            val projectionManager = contexto.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, mediaProjectionIntent)

            val windowManager = contexto.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val screenDensity = metrics.densityDpi

            // Crear archivo de salida MP4 local
            val nombreArchivo = "RUTA_TX_${System.currentTimeMillis()}.mp4"
            archivoVideoSalida = File(contexto.cacheDir, nombreArchivo)

            // Configurar MediaRecorder nativo
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(contexto)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(archivoVideoSalida!!.absolutePath)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(5 * 1024 * 1024)
                setVideoFrameRate(30)
                setVideoSize(screenWidth, screenHeight)
                prepare()
            }

            // Crear pantalla virtual para grabar la animación
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "GrabarRutaTX",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface, null, null
            )

            mediaRecorder?.start()
            _grabandoVideoState.value = true

            Log.d(TAG, "📹 Grabación de video MP4 (MediaProjection) iniciada exitosamente.")

            // Notificar que la grabación comenzó
            alTerminar(archivoVideoSalida!!.absolutePath)

        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error al crear el video de la ruta: ${e.localizedMessage}", e)
            alTerminar("")
        }
    }

    /**
     * Detiene la grabación del video MP4, guarda el archivo en la Galería del teléfono (MediaStore)
     * y libera los recursos del sistema.
     *
     * @param contexto Contexto de la aplicación.
     * @param alCompletar Callback que devuelve la URI del video guardado en la galería.
     */
    fun detenerYGuardarVideoGaleria(contexto: Context, alCompletar: (Uri?) -> Unit) {
        try {
            if (_grabandoVideoState.value) {
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
                mediaRecorder = null

                virtualDisplay?.release()
                virtualDisplay = null

                mediaProjection?.stop()
                mediaProjection = null

                _grabandoVideoState.value = false

                archivoVideoSalida?.let { archivo ->
                    val uriGuardada = insertarVideoEnGaleria(contexto, archivo)
                    Log.d(TAG, "🎬 Video MP4 guardado exitosamente en Galería: $uriGuardada")
                    alCompletar(uriGuardada)
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener grabación de video: ${e.localizedMessage}", e)
        }
        alCompletar(null)
    }

    /**
     * Inserta el archivo MP4 en la Galería pública de Android usando MediaStore.
     */
    private fun insertarVideoEnGaleria(contexto: Context, archivoVideo: File): Uri? {
        val resolver = contexto.contentResolver
        val valores = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, archivoVideo.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TeamTXRutas")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uriColeccion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(uriColeccion, valores)
        if (itemUri != null) {
            resolver.openOutputStream(itemUri)?.use { outputStream ->
                archivoVideo.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                valores.clear()
                valores.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, valores, null, null)
            }
        }
        return itemUri
    }
}
