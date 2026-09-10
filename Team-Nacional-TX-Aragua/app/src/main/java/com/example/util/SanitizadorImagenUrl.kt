package com.example.util

import android.util.Log

/**
 * Sanitizador y formateador universal de URLs de imagen para Coil / Firebase / Supabase.
 * Convierte cualquier esquema (gs://, rutas relativas, URLs sin token/alt=media, rutas locales)
 * en una URL de descarga directa totalmente compatible con Coil (AsyncImage / SubcomposeAsyncImage).
 */
object SanitizadorImagenUrl {

    private const val TAG = "SanitizadorImagenUrl"
    private const val FIREBASE_STORAGE_BUCKET = "teamnacionaltx.firebasestorage.app"
    private const val FIREBASE_STORAGE_BASE_URL = "https://firebasestorage.googleapis.com/v0/b/$FIREBASE_STORAGE_BUCKET/o/"

    /**
     * Sanitiza y devuelve la URL o Uri efectiva para Coil.
     * Devuelve null si la entrada es nula o está vacía.
     */
    fun obtenerUrlEfectiva(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val urlLimpia = rawUrl.trim()

        return try {
            when {
                // 1. Esquema gs:// (Ej: gs://teamnacionaltx.firebasestorage.app/avisos/foto.jpg)
                urlLimpia.startsWith("gs://") -> {
                    val pathSinGs = urlLimpia.substringAfter("gs://").substringAfter("/")
                    val encodedPath = java.net.URLEncoder.encode(pathSinGs, "UTF-8").replace("+", "%20")
                    "$FIREBASE_STORAGE_BASE_URL$encodedPath?alt=media"
                }

                // 2. URL de Firebase Storage (Verificar y asegurar ?alt=media)
                urlLimpia.contains("firebasestorage.googleapis.com") -> {
                    if (!urlLimpia.contains("alt=media")) {
                        if (urlLimpia.contains("?")) "$urlLimpia&alt=media" else "$urlLimpia?alt=media"
                    } else {
                        urlLimpia
                    }
                }

                // 3. Rutas relativas de carpetas de Firebase Storage
                urlLimpia.startsWith("avisos/") ||
                urlLimpia.startsWith("imagenes_chat/") ||
                urlLimpia.startsWith("stickers_chat/") ||
                urlLimpia.startsWith("perfiles/") ||
                urlLimpia.startsWith("motos/") ||
                urlLimpia.startsWith("audios_chat/") ||
                urlLimpia.startsWith("updates/") ||
                urlLimpia.startsWith("flyers/") -> {
                    val encodedPath = java.net.URLEncoder.encode(urlLimpia, "UTF-8").replace("+", "%20")
                    "$FIREBASE_STORAGE_BASE_URL$encodedPath?alt=media"
                }

                // 4. Data URIs de imágenes comprimidas en Base64 (Soporte directo de Coil)
                urlLimpia.startsWith("data:image/") -> {
                    urlLimpia
                }

                // 5. URL de Supabase Storage u otros servidores HTTP/HTTPS
                urlLimpia.contains("supabase.co") || urlLimpia.startsWith("http://") || urlLimpia.startsWith("https://") -> {
                    urlLimpia
                }

                // 6. URI local de Android
                urlLimpia.startsWith("content://") || urlLimpia.startsWith("file://") -> {
                    urlLimpia
                }

                // 6. Ruta de archivo física local
                urlLimpia.startsWith("/") -> {
                    "file://$urlLimpia"
                }

                else -> urlLimpia
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error sanitizando URL de imagen '$rawUrl': ${e.message}")
            rawUrl
        }
    }
}
