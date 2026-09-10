package com.example.radar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest

object RadarFirebase {

    private const val ETIQUETA = "RADAR_FIREBASE"
    private const val COLECCION = "radar_en_vivo"
    private const val TIMEOUT_MS = 12 * 3600_000L
    private const val TIMEOUT_LIMPIEZA_MS = 24 * 3600_000L
    private const val CARPETA_AVATARS = "radar_avatars"
    private const val ARCHIVO_AVATAR_LOCAL = "avatar_local_permanente.jpg"
    private const val TAM_MAX_AVATAR_PX = 200

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private var appContext: Context? = null
    private var escuchaActual: ListenerRegistration? = null
    private val pilotosEnMemoria = mutableMapOf<String, PilotoRadar>()
    private val avataresCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()
    private var carpetaAvatars: File? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val dir = File(context.filesDir, CARPETA_AVATARS)
        if (!dir.exists()) dir.mkdirs()
        carpetaAvatars = dir
        Log.d(ETIQUETA, "Caché persistente de avatares inicializado en: ${dir.absolutePath}")

        // Pre-cargar avatar local si existe
        obtenerAvatarLocal(context)
    }

    private fun hashUrl(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun archivoAvatar(url: String): File? {
        val dir = carpetaAvatars ?: appContext?.let { File(it.filesDir, CARPETA_AVATARS) } ?: return null
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${hashUrl(url)}.jpg")
    }

    private fun archivoAvatarLocal(context: Context?): File? {
        val ctx = context ?: appContext ?: return null
        val dir = carpetaAvatars ?: File(ctx.filesDir, CARPETA_AVATARS)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, ARCHIVO_AVATAR_LOCAL)
    }

    fun guardarAvatarLocal(context: Context, bitmap: Bitmap) {
        try {
            val escalado = escalarBitmap(bitmap, TAM_MAX_AVATAR_PX)
            avataresCache["__local_avatar__"] = escalado
            val archivo = archivoAvatarLocal(context) ?: return
            archivo.outputStream().use { out ->
                escalado.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d(ETIQUETA, "Avatar local guardado permanentemente en: ${archivo.absolutePath}")
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error guardando avatar local: ${e.message}")
        }
    }

    fun obtenerAvatarLocal(context: Context?): Bitmap? {
        avataresCache["__local_avatar__"]?.let { return it }
        return try {
            val archivo = archivoAvatarLocal(context) ?: return null
            if (!archivo.exists()) return null
            val bitmap = BitmapFactory.decodeFile(archivo.absolutePath)
            if (bitmap != null) {
                avataresCache["__local_avatar__"] = bitmap
                Log.d(ETIQUETA, "Avatar local cargado desde disco")
            }
            bitmap
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error cargando avatar local de disco: ${e.message}")
            null
        }
    }

    fun actualizarAvatarLocalDesdeUri(context: Context, uriString: String): Bitmap? {
        if (uriString.isBlank()) return null
        val bitmap = decodificarDesdeOrigen(context, uriString)
        if (bitmap != null) {
            guardarAvatarLocal(context, bitmap)
            guardarAvatarEnDisco(uriString, bitmap)
        }
        return bitmap
    }

    private fun guardarAvatarEnDisco(url: String, bitmap: Bitmap) {
        try {
            val archivo = archivoAvatar(url) ?: return
            val escalado = escalarBitmap(bitmap, TAM_MAX_AVATAR_PX)
            archivo.outputStream().use { out ->
                escalado.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d(ETIQUETA, "Avatar remoto guardado en disco: ${archivo.name}")
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error guardando avatar en disco: ${e.message}")
        }
    }

    private fun cargarAvatarDeDisco(url: String): Bitmap? {
        return try {
            val archivo = archivoAvatar(url) ?: return null
            if (!archivo.exists()) return null
            BitmapFactory.decodeFile(archivo.absolutePath)
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error cargando avatar de disco: ${e.message}")
            null
        }
    }

    private fun escalarBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        if (bitmap.width <= maxDim && bitmap.height <= maxDim) return bitmap
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val ancho = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
        val alto = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
        return Bitmap.createScaledBitmap(bitmap, ancho.coerceAtLeast(1), alto.coerceAtLeast(1), true)
    }

    private fun decodificarDesdeOrigen(context: Context?, uriString: String): Bitmap? {
        if (uriString.isBlank()) return null
        val ctx = context ?: appContext
        return try {
            when {
                uriString.startsWith("data:image/") || uriString.startsWith("data:application/") -> {
                    val base64Data = uriString.substringAfter("base64,")
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                uriString.startsWith("content://") && ctx != null -> {
                    ctx.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                uriString.startsWith("file://") -> {
                    val path = Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                    BitmapFactory.decodeFile(path)
                }
                uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                    val url = URL(uriString)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.instanceFollowRedirects = true
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) TeamTX/1.0")
                    conn.connect()
                    if (conn.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        conn.inputStream.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    } else {
                        Log.w(ETIQUETA, "HTTP ${conn.responseCode} descargando avatar desde $uriString")
                        null
                    }
                }
                else -> {
                    if (uriString.length > 100 && !uriString.contains("/")) {
                        try {
                            val bytes = android.util.Base64.decode(uriString, android.util.Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) {
                            val f = File(uriString)
                            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                        }
                    } else {
                        val f = File(uriString)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error decodificando imagen desde ${uriString.take(40)}...: ${e.message}")
            null
        }
    }

    fun escucharPilotos(
        onPilotosActualizados: (List<PilotoRadar>) -> Unit
    ) {
        escuchaActual?.remove()

        escuchaActual = db.collection(COLECCION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(ETIQUETA, "Error en escucha de radar", error)
                    return@addSnapshotListener
                }

                val ahora = System.currentTimeMillis()
                val pilotos = mutableListOf<PilotoRadar>()
                val idsEnFirebase = mutableSetOf<String>()

                snapshot?.documents?.forEach { doc ->
                    val id = doc.getString("id") ?: return@forEach
                    val lat = doc.getDouble("lat") ?: return@forEach
                    val lon = doc.getDouble("lon") ?: return@forEach
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val activo = doc.getBoolean("activo") ?: true
                    val nombre = doc.getString("nombre") ?: "Piloto"
                    val rango = doc.getString("rango") ?: ""
                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    val alertaSos = doc.getString("alertaSos")

                    idsEnFirebase.add(id)

                    val antiguedad = ahora - timestamp
                    val esFantasma = antiguedad > TIMEOUT_MS && !activo
                    val esUbicacionMuerta = antiguedad > TIMEOUT_LIMPIEZA_MS

                    if (esFantasma || esUbicacionMuerta) {
                        pilotosEnMemoria.remove(id)
                        return@forEach
                    }

                    val piloto = PilotoRadar(
                        id = id,
                        nombre = nombre,
                        rango = rango,
                        lat = lat,
                        lon = lon,
                        avatarUrl = avatarUrl,
                        timestamp = timestamp,
                        activo = activo && antiguedad < TIMEOUT_MS,
                        alertaSos = if (alertaSos.isNullOrBlank()) null else alertaSos
                    )

                    pilotosEnMemoria[id] = piloto
                    pilotos.add(piloto)
                }

                pilotosEnMemoria.keys.retainAll(idsEnFirebase)

                val listaFinal = pilotosEnMemoria.values.sortedByDescending { it.timestamp }
                onPilotosActualizados(listaFinal)

                Log.d(ETIQUETA, "Pilotos actualizados en radar: ${listaFinal.size}")
            }
    }

    fun detenerEscucha() {
        escuchaActual?.remove()
        escuchaActual = null
        pilotosEnMemoria.clear()
        Log.d(ETIQUETA, "Escucha detenida")
    }

    fun obtenerPilotosEnMemoria(): List<PilotoRadar> {
        return pilotosEnMemoria.values.sortedByDescending { it.timestamp }
    }

    suspend fun descargarAvatar(url: String, context: Context? = null): Bitmap? {
        if (url.isBlank()) return null

        avataresCache[url]?.let { return it }

        val disco = cargarAvatarDeDisco(url)
        if (disco != null) {
            val escalado = escalarBitmap(disco, TAM_MAX_AVATAR_PX)
            avataresCache[url] = escalado
            return escalado
        }

        return withContext(Dispatchers.IO) {
            val bitmap = decodificarDesdeOrigen(context ?: appContext, url)
            if (bitmap != null) {
                val escalado = escalarBitmap(bitmap, TAM_MAX_AVATAR_PX)
                avataresCache[url] = escalado
                guardarAvatarEnDisco(url, escalado)
                escalado
            } else {
                null
            }
        }
    }

    fun obtenerAvatarCacheado(url: String): Bitmap? = avataresCache[url]

    fun obtenerAvatarComoBase64(context: Context?, avatarUrlStr: String): String {
        if (avatarUrlStr.startsWith("http://") || avatarUrlStr.startsWith("https://") || avatarUrlStr.startsWith("data:image/")) {
            return avatarUrlStr
        }
        val bitmap = if (avatarUrlStr.isNotBlank()) {
            cargarAvatarDeDisco(avatarUrlStr) ?: obtenerAvatarLocal(context)
        } else {
            obtenerAvatarLocal(context)
        } ?: return avatarUrlStr

        return try {
            val escalado = escalarBitmap(bitmap, 150)
            val baos = java.io.ByteArrayOutputStream()
            escalado.compress(Bitmap.CompressFormat.JPEG, 65, baos)
            val bytes = baos.toByteArray()
            val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64Str"
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error convirtiendo avatar a Base64: ${e.message}")
            avatarUrlStr
        }
    }

    fun obtenerAvatar(id: String, url: String, context: Context?): Bitmap? {
        if (url.isNotBlank()) {
            avataresCache[url]?.let { return it }
            if (url.startsWith("data:image/")) {
                try {
                    val base64Data = url.substringAfter(",")
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        avataresCache[url] = bmp
                        return bmp
                    }
                } catch (_: Exception) {}
            }
            val disco = cargarAvatarDeDisco(url)
            if (disco != null) return disco
        }
        val local = archivoAvatarLocal(context ?: appContext)
        if (local != null && local.exists()) {
            try {
                return BitmapFactory.decodeFile(local.absolutePath)
            } catch (_: Exception) {}
        }
        return null
    }

    fun limpiarCacheUsuario() {
        avataresCache.clear()
        val local = archivoAvatarLocal(appContext)
        try {
            local?.delete()
        } catch (_: Exception) {}
        Log.d(ETIQUETA, "Caché de usuario reseteado")
    }

    fun eliminarPilotoRemoto(id: String) {
        if (id.isBlank()) return
        try {
            db.collection(COLECCION).document(id).delete()
            pilotosEnMemoria.remove(id)
            Log.d(ETIQUETA, "Piloto $id eliminado de radar_en_vivo")
        } catch (e: Exception) {
            Log.e(ETIQUETA, "Error eliminando piloto $id de radar: ${e.message}")
        }
    }
}

