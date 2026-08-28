package com.aistudio.teamtxvzla.nube

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.chat.NubeArchivos
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

// ========================================================================
// NUBE_MULTIMEDIA - Módulo robusto para Supabase Storage + Firebase Storage
// ========================================================================

@Serializable
data class RespuestaSubida(
    val id: String? = null,
    val key: String? = null,
    val fullPath: String? = null,
    val error: String? = null
)

@Serializable
data class UrlPublica(
    val publicUrl: String? = null,
    val signedUrl: String? = null,
    val error: String? = null
)

@Serializable
data class ResultadoOperacion(
    val exito: Boolean,
    val mensaje: String,
    val url: String? = null,
    val ruta: String? = null
)

data class SubidaResultado(
    val url: String?,
    val origen: OrigenSubida,
    val error: String? = null
)

enum class OrigenSubida {
    SUPABASE,
    FIREBASE,
    NINGUNO
}

object NubeMultimedia {

    private const val ETIQUETA_LOG = "NUBE_MULTIMEDIA"
    private const val URL_SUPABASE = "https://lwbvpvxwteebybipqgje.supabase.co"
    private const val CLAVE_ANONIMA = "sb_publishable_lRf0dvnLyGilFayM9l_IHQ_Ju1xCPOX"
    private const val NOMBRE_BUCKET = "multimedia"
    private const val MAX_INTENTOS = 3
    private const val TIEMPO_ESPERA_INTENTO_MS = 20000L

    private var clienteHttp: HttpClient? = null
    private var estaInicializado = false
    private var contextoAplicacion: Context? = null
    private var firebaseVerificado = false

    fun inicializar(contexto: Context) {
        if (estaInicializado) return
        
        contextoAplicacion = contexto.applicationContext
        
        clienteHttp = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.NONE
            }
            install(HttpRequestRetry) {
                maxRetries = 2
                retryOnExceptionIf { _, causa ->
                    causa is UnknownHostException || causa is TimeoutException || causa is java.io.IOException
                }
                constantDelay(millis = 1000)
            }
            expectSuccess = false
        }
        
        verificarFirebaseEntornos()
        estaInicializado = true
        Log.i(ETIQUETA_LOG, "✅ Nube Multimedia inicializada correctamente")
    }

    private fun verificarConectividad(contexto: Context): Boolean {
        val cm = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun verificarFirebaseEntornos() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            Log.i(ETIQUETA_LOG, "✅ Firebase Storage verificado: Usuario autenticado (${user.uid})")
            firebaseVerificado = true
        } else {
            Log.w(ETIQUETA_LOG, "⚠️ Firebase Storage: Usuario NO autenticado. Intentando login anónimo...")
            com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener {
                    Log.i(ETIQUETA_LOG, "✅ Login anónimo exitoso para Storage")
                    firebaseVerificado = true
                }
                .addOnFailureListener { e ->
                    Log.e(ETIQUETA_LOG, "❌ Fallo login anónimo para Storage: ${e.message}")
                    firebaseVerificado = false
                }
        }
    }

    /**
     * Sube una imagen optimizada (Flyer, Aviso o Perfil) usando solo Firebase Storage.
     * Confirma que Firebase Storage está activo. Sin fallback a Supabase.
     * Retorna resultado detallado con URL, origen y error si falla.
     */
    suspend fun subirImagenAvisoAsync(
        contexto: Context,
        uri: Uri,
        carpeta: String = "avisos"
    ): SubidaResultado = withContext(Dispatchers.IO) {
        if (!verificarConectividad(contexto)) {
            Log.w(ETIQUETA_LOG, "⚠️ Sin conexión a internet validada")
            return@withContext SubidaResultado(null, OrigenSubida.NINGUNO, "Sin conexión a internet")
        }

        // Garantizar sesión de Auth activa antes de subir
        com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()

        var ultimoError: String? = null
        
        for (intento in 1..MAX_INTENTOS) {
            try {
                Log.i(ETIQUETA_LOG, "🔄 Intento $intento/$MAX_INTENTOS subiendo imagen a '$carpeta' vía Firebase...")
                val bytesOptimizados = optimizarImagen(contexto, uri)
                val nombreArchivo = "${carpeta}_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
                
                // 1. Único intento: Firebase Storage (solo Firebase, sin Supabase)
                Log.d(ETIQUETA_LOG, "📤 Subiendo a Firebase Storage (carpeta: $carpeta, intento $intento)...")
                val urlFirebase = withTimeoutOrNull(TIEMPO_ESPERA_INTENTO_MS) {
                    if (bytesOptimizados != null) {
                        NubeArchivos.subirBytes(bytesOptimizados, NubeArchivos.TipoArchivo.AVISO, nombreArchivo)
                    } else {
                        NubeArchivos.subirArchivo(uri, NubeArchivos.TipoArchivo.AVISO, nombreArchivo)
                    }
                }
                
                if (!urlFirebase.isNullOrBlank()) {
                    Log.i(ETIQUETA_LOG, "✅ Subida a Firebase exitosa (intento $intento): $urlFirebase")
                    return@withContext SubidaResultado(urlFirebase, OrigenSubida.FIREBASE, null)
                }
                Log.w(ETIQUETA_LOG, "⚠️ Firebase Storage falló o timeout (intento $intento)")
                
                ultimoError = "Firebase Storage falló en intento $intento"
                
            } catch (e: TimeoutException) {
                ultimoError = "Timeout en intento $intento (${TIEMPO_ESPERA_INTENTO_MS/1000}s)"
                Log.e(ETIQUETA_LOG, "⏱️ $ultimoError", e)
            } catch (e: Exception) {
                ultimoError = "Excepción en intento $intento: ${e.message}"
                Log.e(ETIQUETA_LOG, "❌ $ultimoError", e)
            }
            
            if (intento < MAX_INTENTOS) {
                val espera = 2000L * intento
                Log.d(ETIQUETA_LOG, "⏳ Esperando ${espera}ms antes de reintento...")
                delay(espera)
            }
        }
        
        Log.e(ETIQUETA_LOG, "❌ Todos los intentos fallaron: $ultimoError")
        SubidaResultado(null, OrigenSubida.NINGUNO, ultimoError ?: "Error desconocido tras $MAX_INTENTOS intentos a Firebase")
    }

    /** Versión callback para compatibilidad */
    fun subirImagen(
        archivoUri: Uri,
        carpeta: String,
        nombreArchivo: String? = null,
        callback: (ResultadoOperacion) -> Unit
    ) {
        val ctx = contextoAplicacion
        if (ctx == null) {
            callback(ResultadoOperacion(false, "Contexto no inicializado"))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val resultado = subirImagenAvisoAsync(ctx, archivoUri, carpeta)
            if (resultado.url != null) {
                callback(ResultadoOperacion(true, "Subida exitosa (${resultado.origen})", resultado.url, resultado.url))
            } else {
                callback(ResultadoOperacion(false, resultado.error ?: "Error al subir archivo a los servidores"))
            }
        }
    }

    fun subirBytes(
        bytes: ByteArray,
        carpeta: String,
        nombreArchivo: String,
        extension: String = "webp",
        callback: (ResultadoOperacion) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rutaCompleta = "$carpeta/$nombreArchivo"
                val mime = if (extension == "webp") "image/webp" else "image/jpeg"
                val url = subirBytesSupabase(bytes, rutaCompleta, mime)
                if (url != null) {
                    callback(ResultadoOperacion(true, "Subida exitosa", url, rutaCompleta))
                } else {
                    callback(ResultadoOperacion(false, "Error al subir bytes a Supabase"))
                }
            } catch (e: Exception) {
                callback(ResultadoOperacion(false, "Excepción: ${e.message}"))
            }
        }
    }

    fun obtenerUrlPublica(rutaEnBucket: String): String {
        return "$URL_SUPABASE/storage/v1/object/public/$NOMBRE_BUCKET/$rutaEnBucket"
    }

    private suspend fun subirBytesSupabase(bytes: ByteArray, rutaCompleta: String, mimeType: String): String? {
        val client = clienteHttp ?: HttpClient(OkHttp)
        return try {
            val response = client.post("$URL_SUPABASE/storage/v1/object/$NOMBRE_BUCKET/$rutaCompleta") {
                header("apikey", CLAVE_ANONIMA)
                header("Authorization", "Bearer $CLAVE_ANONIMA")
                header("Content-Type", mimeType)
                header("x-upsert", "true")
                setBody(bytes)
            }
            if (response.status.value in 200..299) {
                obtenerUrlPublica(rutaCompleta)
            } else {
                val body = response.bodyAsText()
                Log.w(ETIQUETA_LOG, "Respuesta Supabase HTTP ${response.status.value}: $body")
                null
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Excepción conectando con Supabase: ${e.message}", e)
            null
        }
    }

    private fun optimizarImagen(contexto: Context, uri: Uri): ByteArray? {
        return try {
            // 1. Detectar orientación EXIF para evitar que las fotos grandes se volteen
            var rotationDegrees = 0f
            try {
                contexto.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }
            } catch (e: Exception) {
                Log.w(ETIQUETA_LOG, "No se pudo leer EXIF de imagen: ${e.message}")
            }

            val input: InputStream? = contexto.contentResolver.openInputStream(uri)
            val bitmapDecoded = BitmapFactory.decodeStream(input) ?: return null
            input?.close()

            // 2. Corregir rotación física si el sensor de la cámara guardó orientación
            val bitmapOriginal = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmapDecoded, 0, 0, bitmapDecoded.width, bitmapDecoded.height, matrix, true)
            } else {
                bitmapDecoded
            }

            // 3. Redimensionar preservando proporción (máximo 1280px para no consumir datos ni almacenamiento)
            val maxDimension = 1280
            val width = bitmapOriginal.width
            val height = bitmapOriginal.height
            val bitmapRedimensionado = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (ratio > 1) {
                    targetW = maxDimension
                    targetH = (maxDimension / ratio).toInt()
                } else {
                    targetH = maxDimension
                    targetW = (maxDimension * ratio).toInt()
                }
                Bitmap.createScaledBitmap(bitmapOriginal, targetW, targetH, true)
            } else {
                bitmapOriginal
            }

            val baos = ByteArrayOutputStream()
            bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error optimizando imagen", e)
            null
        }
    }

    fun cerrar() {
        clienteHttp?.close()
        estaInicializado = false
    }
}