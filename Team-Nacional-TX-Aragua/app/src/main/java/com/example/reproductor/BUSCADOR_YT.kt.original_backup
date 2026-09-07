package com.example.reproductor

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// ═══════════════════════════════════════════════════════════════════════════
// BUSCADOR MULTI-FUENTE - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Motor híbrido: JioSaavn Global Music (320kbps) + Invidious + Piped + YT Scraper
// ═══════════════════════════════════════════════════════════════════════════

object BUSCADOR_YT {

    private const val ETIQUETA = "BUSCADOR_YT"
    private const val MAX_RESULTADOS = 20
    private val CLAVE_DES_SAAVN = "38346591".toByteArray(Charsets.UTF_8)

    // Instancias Invidious y Piped de respaldo
    private val INSTANCIAS_INVIDIOUS = listOf(
        "https://invidious.privacydev.net",
        "https://invidious.fdn.fr",
        "https://inv.zzls.xyz",
        "https://invidious.perennialte.ch",
        "https://inv.tux.pizza",
        "https://yewtu.be"
    )

    private val INSTANCIAS_PIPED = listOf(
        "https://pipedapi.kavin.rocks",
        "https://piped-api.lunar.icu",
        "https://api.piped.projectsegfau.lt",
        "https://piped.video/api"
    )

    /**
     * Busca canciones utilizando el motor global con enlaces directos 320kbps y respaldos.
     */
    suspend fun buscar(termino: String): List<ResultadoBusquedaYT> = withContext(Dispatchers.IO) {
        val query = termino.trim()
        if (query.isBlank()) return@withContext emptyList()

        val resultados = mutableListOf<ResultadoBusquedaYT>()

        // 1. Fuente Primaria: JioSaavn Global Music API (Descargas 320kbps directas, sin bloqueos)
        try {
            val resSaavn = buscarEnJioSaavn(query)
            if (resSaavn.isNotEmpty()) {
                Log.d(ETIQUETA, "✅ Búsqueda exitosa en Music Global API: ${resSaavn.size} canciones")
                resultados.addAll(resSaavn)
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Music Global API falló: ${e.message}")
        }

        // 2. Si hay pocos resultados o búsqueda específica de YouTube, complementar con Invidious / Piped / Scraper
        if (resultados.size < 5) {
            try {
                for (instancia in INSTANCIAS_INVIDIOUS) {
                    val res = buscarEnInvidious(instancia, query)
                    if (res.isNotEmpty()) {
                        Log.d(ETIQUETA, "✅ Complementado con Invidious ($instancia): ${res.size} resultados")
                        res.forEach { if (resultados.none { r -> r.idVideo == it.idVideo }) resultados.add(it) }
                        break
                    }
                }
            } catch (_: Exception) {}

            try {
                if (resultados.size < 5) {
                    for (instancia in INSTANCIAS_PIPED) {
                        val res = buscarEnPiped(instancia, query)
                        if (res.isNotEmpty()) {
                            Log.d(ETIQUETA, "✅ Complementado con Piped ($instancia): ${res.size} resultados")
                            res.forEach { if (resultados.none { r -> r.idVideo == it.idVideo }) resultados.add(it) }
                            break
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                if (resultados.size < 5) {
                    val resWeb = buscarEnYouTubeWeb(query)
                    if (resWeb.isNotEmpty()) {
                        Log.d(ETIQUETA, "✅ Complementado con Web Scraper: ${resWeb.size} resultados")
                        resWeb.forEach { if (resultados.none { r -> r.idVideo == it.idVideo }) resultados.add(it) }
                    }
                }
            } catch (_: Exception) {}
        }

        resultados.take(MAX_RESULTADOS)
    }

    /**
     * Búsqueda en JioSaavn Global Music API con desencriptación de stream directo a 320kbps.
     */
    private fun buscarEnJioSaavn(termino: String): List<ResultadoBusquedaYT> {
        val lista = mutableListOf<ResultadoBusquedaYT>()
        try {
            val q = URLEncoder.encode(termino, "UTF-8")
            val urlApi = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=20&p=1&q=$q"
            val conexion = URL(urlApi).openConnection() as HttpURLConnection
            conexion.connectTimeout = 7000
            conexion.readTimeout = 7000
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conexion.setRequestProperty("Accept", "application/json")

            if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conexion.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(jsonStr)
                val resultsArray = jsonObj.optJSONArray("results") ?: JSONArray()

                for (i in 0 until resultsArray.length()) {
                    val songObj = resultsArray.optJSONObject(i) ?: continue
                    val id = songObj.optString("id", "")
                    val rawTitle = songObj.optString("title", "Canción")
                    val titulo = rawTitle.replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")
                    val moreInfo = songObj.optJSONObject("more_info")

                    val rawArtist = moreInfo?.optString("music", "") ?: songObj.optString("subtitle", "Artista")
                    val artista = rawArtist.replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")
                    val rawAlbum = moreInfo?.optString("album", "Música TX") ?: "Música TX"
                    val album = rawAlbum.replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")

                    val duracionSeg = moreInfo?.optLong("duration", 210L) ?: 210L
                    val rawImg = songObj.optString("image", "")
                    val imgHd = rawImg.replace("150x150", "500x500").replace("50x50", "500x500")

                    val encryptedUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                    val directUrl = if (encryptedUrl.isNotBlank()) desencriptarUrlJioSaavn(encryptedUrl) else null

                    lista.add(
                        ResultadoBusquedaYT(
                            titulo = titulo,
                            autor = artista,
                            duracionSegundos = duracionSeg,
                            urlVideo = songObj.optString("perma_url", "https://www.jiosaavn.com/song/$id"),
                            urlThumbnail = imgHd.ifBlank { "https://via.placeholder.com/500?text=TX+Music" },
                            idVideo = id.ifBlank { "song_$i" },
                            urlDescargaDirecta = directUrl,
                            album = album,
                            fuente = "Music HD 320kbps"
                        )
                    )
                }
            }
            conexion.disconnect()
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error en JioSaavn Search: ${e.message}")
        }
        return lista
    }

    /**
     * Desencripta el stream directo de JioSaavn con DES/ECB/PKCS5Padding.
     */
    fun desencriptarUrlJioSaavn(encUrl: String): String? {
        return try {
            val keySpec = SecretKeySpec(CLAVE_DES_SAAVN, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encUrl.trim(), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val url = String(decryptedBytes, Charsets.UTF_8)
            // Solicitar la mejor calidad disponible (320kbps o 160kbps)
            url.replace("_96.mp4", "_320.mp4")
               .replace("_160.mp4", "_320.mp4")
               .replace("_96.m4a", "_320.m4a")
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error desencriptando stream: ${e.message}")
            null
        }
    }

    /**
     * Resuelve la URL de stream via Piped /streams/{videoId}.
     */
    private fun resolverViaPiped(instancia: String, videoId: String): String? {
        return try {
            val urlApi = "$instancia/streams/$videoId"
            val conexion = URL(urlApi).openConnection() as HttpURLConnection
            conexion.connectTimeout = 5000
            conexion.readTimeout = 5000
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conexion.inputStream.bufferedReader().use { it.readText() }
                conexion.disconnect()
                val obj = JSONObject(jsonStr)
                val audioStreams = obj.optJSONArray("audioStreams")
                if (audioStreams != null && audioStreams.length() > 0) {
                    audioStreams.optJSONObject(0)?.optString("url", "")?.takeIf { it.isNotBlank() }
                } else null
            } else { conexion.disconnect(); null }
        } catch (_: Exception) { null }
    }

    /**
     * Búsqueda en instancias Invidious (retorna JSON nativo).
     */
    private fun buscarEnInvidious(instancia: String, termino: String): List<ResultadoBusquedaYT> {
        val lista = mutableListOf<ResultadoBusquedaYT>()
        try {
            val q = URLEncoder.encode(termino, "UTF-8")
            val urlApi = "$instancia/api/v1/search?q=$q&type=video"
            val conexion = URL(urlApi).openConnection() as HttpURLConnection
            conexion.connectTimeout = 6000
            conexion.readTimeout = 6000
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")

            if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conexion.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)

                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val tipo = obj.optString("type", "video")
                    if (tipo != "video") continue

                    val videoId = obj.optString("videoId", "")
                    if (videoId.isBlank()) continue

                    val titulo = obj.optString("title", "Canción")
                    val autor = obj.optString("author", "Artista")
                    val duracionSeg = obj.optLong("lengthSeconds", 0L)
                    val thumbnails = obj.optJSONArray("videoThumbnails")
                    val thumbUrl = thumbnails?.optJSONObject(0)?.optString("url", "")
                        ?: "https://img.youtube.com/vi/$videoId/mqdefault.jpg"

                    lista.add(
                        ResultadoBusquedaYT(
                            titulo = titulo,
                            autor = autor,
                            duracionSegundos = duracionSeg,
                            urlVideo = "https://www.youtube.com/watch?v=$videoId",
                            urlThumbnail = thumbUrl,
                            idVideo = videoId,
                            fuente = "YouTube"
                        )
                    )
                }
            }
            conexion.disconnect()
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error en Invidious $instancia: ${e.message}")
        }
        return lista
    }

    /**
     * Búsqueda en instancias Piped (retorna JSON estructurado).
     */
    private fun buscarEnPiped(instancia: String, termino: String): List<ResultadoBusquedaYT> {
        val lista = mutableListOf<ResultadoBusquedaYT>()
        try {
            val q = URLEncoder.encode(termino, "UTF-8")
            val urlApi = "$instancia/search?q=$q&filter=videos"
            val conexion = URL(urlApi).openConnection() as HttpURLConnection
            conexion.connectTimeout = 6000
            conexion.readTimeout = 6000
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conexion.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val items = obj.optJSONArray("items") ?: JSONArray()

                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val tipo = item.optString("type", "stream")
                    if (tipo != "stream") continue

                    val urlRelativa = item.optString("url", "")
                    val videoId = urlRelativa.substringAfter("/watch?v=").substringBefore("&")
                    if (videoId.isBlank()) continue

                    val titulo = item.optString("title", "Canción")
                    val autor = item.optString("uploaderName", "Artista")
                    val duracionSeg = item.optLong("duration", 0L)
                    val thumbUrl = item.optString("thumbnail", "https://img.youtube.com/vi/$videoId/mqdefault.jpg")

                    lista.add(
                        ResultadoBusquedaYT(
                            titulo = titulo,
                            autor = autor,
                            duracionSegundos = duracionSeg,
                            urlVideo = "https://www.youtube.com/watch?v=$videoId",
                            urlThumbnail = thumbUrl,
                            idVideo = videoId,
                            fuente = "YouTube"
                        )
                    )
                }
            }
            conexion.disconnect()
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error en Piped $instancia: ${e.message}")
        }
        return lista
    }

    /**
     * Scraper web directo de YouTube.
     */
    private fun buscarEnYouTubeWeb(termino: String): List<ResultadoBusquedaYT> {
        val lista = mutableListOf<ResultadoBusquedaYT>()
        try {
            val terminoCodificado = URLEncoder.encode(termino, "UTF-8")
            val urlBusqueda = "https://www.youtube.com/results?search_query=$terminoCodificado"
            val conexion = URL(urlBusqueda).openConnection() as HttpURLConnection
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            conexion.setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            conexion.connectTimeout = 8000
            conexion.readTimeout = 8000

            val html = conexion.inputStream.bufferedReader().use { it.readText() }
            conexion.disconnect()

            val patronVideo = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\",\"thumbnail\":\\{\"thumbnails\":\\[\\{\"url\":\"([^\"]+)\"")
            val patronTitulo = Regex("\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"")

            val matchesVideo = patronVideo.findAll(html).toList()
            val matchesTitulo = patronTitulo.findAll(html).toList()

            for (i in 0 until minOf(matchesVideo.size, matchesTitulo.size, MAX_RESULTADOS)) {
                val videoId = matchesVideo[i].groupValues[1]
                val thumb = matchesVideo[i].groupValues[2].replace("\\u0026", "&")
                val titulo = matchesTitulo[i].groupValues[1]

                if (lista.none { it.idVideo == videoId }) {
                    lista.add(
                        ResultadoBusquedaYT(
                            titulo = titulo,
                            autor = termino.substringBefore("-").trim().ifBlank { "YouTube Music" },
                            duracionSegundos = 210L,
                            urlVideo = "https://www.youtube.com/watch?v=$videoId",
                            urlThumbnail = thumb,
                            idVideo = videoId,
                            fuente = "YouTube"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error en YouTube Web Scraper: ${e.message}")
        }
        return lista
    }
}
