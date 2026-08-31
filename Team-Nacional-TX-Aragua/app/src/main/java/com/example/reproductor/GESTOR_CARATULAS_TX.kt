package com.example.reproductor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// ═══════════════════════════════════════════════════════════════════════════
// GESTOR DE CARÁTULAS - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Descarga automática en segundo plano de portadas en HD y caché local offline.
// ═══════════════════════════════════════════════════════════════════════════

object GESTOR_CARATULAS_TX {

    private const val ETIQUETA = "GESTOR_CARATULAS_TX"
    private val memoriaCache = mutableMapOf<String, String>()

    /**
     * Obtiene la ruta o URL de la carátula de una canción.
     * Si no existe localmente y la configuración lo permite, intenta descargarla en HD.
     */
    suspend fun obtenerCaratula(
        contexto: Context,
        cancion: CancionMotera,
        modoDescarga: ModoDescargaCaratulas
    ): String? = withContext(Dispatchers.IO) {
        // 1. Si ya tiene URI local válida de MediaStore, usarla
        if (!cancion.portadaUriStr.isNullOrBlank()) {
            return@withContext cancion.portadaUriStr
        }

        val clave = generarClave(cancion.artista, cancion.album.ifBlank { cancion.titulo })
        if (memoriaCache.containsKey(clave)) {
            return@withContext memoriaCache[clave]
        }

        // 2. Verificar si ya existe en la caché local del disco
        val carpetaCache = File(contexto.cacheDir, "caratulas_tx").apply { if (!exists()) mkdirs() }
        val archivoCache = File(carpetaCache, "${clave}.jpg")
        if (archivoCache.exists() && archivoCache.length() > 500) {
            val ruta = archivoCache.absolutePath
            memoriaCache[clave] = ruta
            return@withContext ruta
        }

        // 3. Si las descargas están desactivadas, no consultar internet
        if (modoDescarga == ModoDescargaCaratulas.DESACTIVADO) {
            return@withContext null
        }

        // 4. Verificar conectividad según la política (Solo Wi-Fi o Wi-Fi + Datos)
        if (!puedeDescargarSegunRed(contexto, modoDescarga)) {
            return@withContext null
        }

        // 5. Descargar online desde iTunes Search API (API libre, sin API key, devuelve 600x600 px)
        val urlImagenOnline = buscarCaratulaOnline(cancion.artista, cancion.album.ifBlank { cancion.titulo })
        if (urlImagenOnline != null) {
            val descargado = descargarYGuardar(urlImagenOnline, archivoCache)
            if (descargado) {
                val ruta = archivoCache.absolutePath
                memoriaCache[clave] = ruta
                return@withContext ruta
            }
        }

        null
    }

    /**
     * Fuerza la búsqueda y descarga online de la carátula bajo demanda del usuario.
     */
    suspend fun forzarDescargaCaratula(contexto: Context, cancion: CancionMotera): String? = withContext(Dispatchers.IO) {
        val clave = generarClave(cancion.artista, cancion.album.ifBlank { cancion.titulo })
        val carpetaCache = File(contexto.cacheDir, "caratulas_tx").apply { if (!exists()) mkdirs() }
        val archivoCache = File(carpetaCache, "${clave}.jpg")

        val urlImagenOnline = buscarCaratulaOnline(cancion.artista, cancion.album.ifBlank { cancion.titulo })
        if (urlImagenOnline != null) {
            val descargado = descargarYGuardar(urlImagenOnline, archivoCache)
            if (descargado) {
                val ruta = archivoCache.absolutePath
                memoriaCache[clave] = ruta
                return@withContext ruta
            }
        }
        null
    }

    /**
     * Consulta a la API de iTunes para obtener la portada oficial del álbum en alta resolución.
     */
    private suspend fun buscarCaratulaOnline(artista: String, albumOTitulo: String): String? = withContext(Dispatchers.IO) {
        try {
            val terminoLimpio = "$artista $albumOTitulo"
                .replace(Regex("[^a-zA-Z0-9 ]"), " ")
                .trim()

            if (terminoLimpio.isBlank()) return@withContext null

            val terminoCodificado = URLEncoder.encode(terminoLimpio, "UTF-8")
            val urlApi = "https://itunes.apple.com/search?term=$terminoCodificado&entity=song&limit=1"

            val conexion = URL(urlApi).openConnection() as HttpURLConnection
            conexion.connectTimeout = 8000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("User-Agent", "TeamTX-Player/1.0")

            val respuesta = conexion.inputStream.bufferedReader().use { it.readText() }
            conexion.disconnect()

            val json = JSONObject(respuesta)
            val resultados = json.optJSONArray("results")
            if (resultados != null && resultados.length() > 0) {
                val item = resultados.getJSONObject(0)
                // Obtener imagen en 600x600 px reemplazando 100x100bb por 600x600bb
                val url100 = item.optString("artworkUrl100", "")
                if (url100.isNotBlank()) {
                    return@withContext url100.replace("100x100bb", "600x600bb")
                }
            }
            null
        } catch (e: Exception) {
            Log.w(ETIQUETA, "No se pudo obtener carátula online: ${e.message}")
            null
        }
    }

    /**
     * Descarga la imagen y la guarda en la caché local del dispositivo.
     */
    private suspend fun descargarYGuardar(urlStr: String, archivoDestino: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val conexion = URL(urlStr).openConnection() as HttpURLConnection
            conexion.connectTimeout = 10000
            conexion.readTimeout = 10000
            conexion.connect()

            if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                conexion.inputStream.use { input ->
                    FileOutputStream(archivoDestino).use { output ->
                        input.copyTo(output)
                    }
                }
                conexion.disconnect()
                return@withContext archivoDestino.exists() && archivoDestino.length() > 500
            }
            conexion.disconnect()
            false
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error guardando carátula: ${e.message}")
            false
        }
    }

    /**
     * Comprueba si el tipo de red actual cumple con la configuración del usuario.
     */
    private fun puedeDescargarSegunRed(contexto: Context, modoDescarga: ModoDescargaCaratulas): Boolean {
        return try {
            val cm = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val red = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(red) ?: return false

            when (modoDescarga) {
                ModoDescargaCaratulas.SOLO_WIFI -> caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                ModoDescargaCaratulas.WIFI_Y_DATOS -> caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                ModoDescargaCaratulas.DESACTIVADO -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun generarClave(artista: String, album: String): String {
        val raw = "${artista.lowercase().trim()}_${album.lowercase().trim()}"
        return raw.replace(Regex("[^a-z0-9_]"), "_").take(60)
    }
}
