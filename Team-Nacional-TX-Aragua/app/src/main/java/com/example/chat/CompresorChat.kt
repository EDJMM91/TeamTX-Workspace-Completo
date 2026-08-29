package com.example.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * ARCHIVO: CompresorChat.kt
 *
 * Módulo de compresión agresiva en el cliente para imágenes y multimedia del chat.
 *
 * Características:
 * - Reducción y recompresión en el dispositivo local antes de la subida (< 200–300 KB).
 * - Rotación automática según metadatos EXIF.
 * - Redimensionamiento inteligente manteniendo la relación de aspecto (máximo 1280px).
 * - Optimizado para conexiones lentas (2G / 3G / túneles).
 */
object CompresorChat {

    private const val TAG = "CompresorChat"
    private const val MAX_DIMENSION_PX = 1280
    private const val CALIDAD_INICIAL = 80
    private const val PESO_MAXIMO_BYTES = 280 * 1024L // 280 KB máximo

    /**
     * Comprime una imagen desde una URI local y devuelve un archivo optimizado en caché.
     */
    suspend fun comprimirImagen(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION_PX,
        calidadInicial: Int = CALIDAD_INICIAL,
        maxBytes: Long = PESO_MAXIMO_BYTES
    ): File? = withContext(Dispatchers.IO) {
        try {
            // 1. Obtener orientación EXIF
            var rotacion = 0
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    rotacion = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo leer EXIF: ${e.message}")
            }

            // 2. Decodificar dimensiones sin cargar todo el bitmap en memoria
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val anchoOriginal = options.outWidth
            val altoOriginal = options.outHeight
            if (anchoOriginal <= 0 || altoOriginal <= 0) return@withContext null

            // 3. Calcular factor de escala inSampleSize
            var sampleSize = 1
            val dimensionMayor = maxOf(anchoOriginal, altoOriginal)
            while (dimensionMayor / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            // 4. Decodificar bitmap escalado
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // 50% menos RAM que ARGB_8888
            }

            var bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return@withContext null

            // 5. Aplicar rotación y redimensionamiento final exacto si es necesario
            val anchoActual = bitmap.width
            val altoActual = bitmap.height
            val escala = minOf(1.0f, maxDimension.toFloat() / maxOf(anchoActual, altoActual))

            val matrix = Matrix()
            if (rotacion != 0) matrix.postRotate(rotacion.toFloat())
            if (escala < 1.0f) matrix.postScale(escala, escala)

            if (rotacion != 0 || escala < 1.0f) {
                val bitmapTransformado = Bitmap.createBitmap(bitmap, 0, 0, anchoActual, altoActual, matrix, true)
                if (bitmapTransformado != bitmap) {
                    bitmap.recycle()
                    bitmap = bitmapTransformado
                }
            }

            // 6. Recompresión progresiva para garantizar peso < maxBytes
            var calidad = calidadInicial
            var stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, stream)

            while (stream.size() > maxBytes && calidad > 35) {
                stream.reset()
                calidad -= 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, stream)
            }

            // 7. Guardar en archivo temporal
            val carpetaDestino = File(context.cacheDir, "chat_media_comp").apply {
                if (!exists()) mkdirs()
            }
            val archivoSalida = File(carpetaDestino, "img_tx_${System.currentTimeMillis()}.jpg")
            FileOutputStream(archivoSalida).use { fos ->
                fos.write(stream.toByteArray())
            }

            bitmap.recycle()
            Log.i(TAG, "📸 Imagen comprimida con éxito: ${archivoSalida.length() / 1024} KB (calidad: $calidad%)")
            archivoSalida
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error comprimiendo imagen: ${e.message}", e)
            null
        }
    }
}
