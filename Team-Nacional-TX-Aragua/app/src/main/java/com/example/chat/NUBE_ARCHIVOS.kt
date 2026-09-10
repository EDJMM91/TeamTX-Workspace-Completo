package com.example.chat

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ARCHIVO: NUBE_ARCHIVOS.kt
 *
 * Módulo de transporte para Firebase Storage.
 * Responsabilidad ÚNICA: Subir archivos y devolver URL de descarga.
 * No contiene lógica de negocio, solo transporte.
 */
object NubeArchivos {

    private const val CARPETA_STICKERS = "stickers_chat"
    private const val CARPETA_IMAGENES = "imagenes_chat"
    private const val CARPETA_AUDIOS = "audios_chat"
    private const val CARPETA_AVISOS = "avisos"

    enum class TipoArchivo {
        STICKER,
        IMAGEN,
        AUDIO,
        AVISO
    }

    /**
     * Sube un archivo a Firebase Storage y retorna la URL de descarga.
     * @param uriArchivo URI local del archivo a subir
     * @param tipoArchivo Tipo para organizar en carpetas
     * @param nombrePersonalizado Nombre opcional, si es null se genera automático
     * @return URL de descarga pública o null si falla
     */
    suspend fun subirArchivo(
        uriArchivo: Uri,
        tipoArchivo: TipoArchivo = TipoArchivo.IMAGEN,
        nombrePersonalizado: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<String?>()

        try {
            val almacenamiento: com.google.firebase.storage.FirebaseStorage = FirebaseStorage.getInstance()
            val carpeta = when (tipoArchivo) {
                TipoArchivo.STICKER -> CARPETA_STICKERS
                TipoArchivo.IMAGEN -> CARPETA_IMAGENES
                TipoArchivo.AUDIO -> CARPETA_AUDIOS
                TipoArchivo.AVISO -> CARPETA_AVISOS
            }

            val extension = when (tipoArchivo) {
                TipoArchivo.STICKER -> ".webp"
                TipoArchivo.IMAGEN -> ".jpg"
                TipoArchivo.AUDIO -> ".ogg"
                TipoArchivo.AVISO -> ".jpg"
            }

            val rutaCompleta = if (!nombrePersonalizado.isNullOrBlank() && nombrePersonalizado.contains("/")) {
                nombrePersonalizado
            } else {
                val nombreArchivo = nombrePersonalizado ?: "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}$extension"
                "$carpeta/$nombreArchivo"
            }
            val referencia: StorageReference = almacenamiento.reference.child(rutaCompleta)

            val tareaSubida: UploadTask = referencia.putFile(uriArchivo)
            manejarTareaSubida(tareaSubida, referencia, deferred)
        } catch (e: Exception) {
            Log.e("NubeArchivos", "Excepción al subir: ${e.message}")
            deferred.complete(null)
        }

        deferred.await()
    }

    /**
     * Sube un ByteArray a Firebase Storage y retorna la URL de descarga.
     */
    suspend fun subirBytes(
        bytes: ByteArray,
        tipoArchivo: TipoArchivo = TipoArchivo.IMAGEN,
        nombrePersonalizado: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<String?>()

        try {
            val almacenamiento = FirebaseStorage.getInstance()
            val carpeta = when (tipoArchivo) {
                TipoArchivo.STICKER -> CARPETA_STICKERS
                TipoArchivo.IMAGEN -> CARPETA_IMAGENES
                TipoArchivo.AUDIO -> CARPETA_AUDIOS
                TipoArchivo.AVISO -> CARPETA_AVISOS
            }
            val extension = when (tipoArchivo) {
                TipoArchivo.STICKER -> ".webp"
                TipoArchivo.IMAGEN -> ".jpg"
                TipoArchivo.AUDIO -> ".ogg"
                TipoArchivo.AVISO -> ".jpg"
            }

            val rutaCompleta = if (!nombrePersonalizado.isNullOrBlank() && nombrePersonalizado.contains("/")) {
                nombrePersonalizado
            } else {
                val nombreArchivo = nombrePersonalizado ?: "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}$extension"
                "$carpeta/$nombreArchivo"
            }
            val referencia = almacenamiento.reference.child(rutaCompleta)

            val tareaSubida = referencia.putBytes(bytes)
            manejarTareaSubida(tareaSubida, referencia, deferred)
        } catch (e: Exception) {
            Log.e("NubeArchivos", "Excepción al subir bytes: ${e.message}")
            deferred.complete(null)
        }

        deferred.await()
    }

    private fun manejarTareaSubida(
        tarea: UploadTask,
        referencia: StorageReference,
        deferred: CompletableDeferred<String?>
    ) {
        tarea.addOnSuccessListener {
            referencia.downloadUrl.addOnSuccessListener { uri ->
                Log.i("TEAM_TX_IMAGES", "✅ Imagen subida a Firebase Storage: ${referencia.path} -> $uri")
                Log.i("NubeArchivos", "✅ URL de descarga obtenida: $uri")
                deferred.complete(uri.toString())
            }.addOnFailureListener { error ->
                Log.e("TEAM_TX_IMAGES", "❌ Error al obtener URL de descarga para ${referencia.path}: ${error.message}")
                Log.e("NubeArchivos", "Error al obtener URL descarga: ${error.message}")
                deferred.complete(null)
            }
        }.addOnFailureListener { error ->
            Log.e("TEAM_TX_IMAGES", "❌ Error al subir a Firebase Storage (${referencia.path}): ${error.message}")
            Log.e("NubeArchivos", "Error al subir: ${error.message}")
            deferred.complete(null)
        }
    }

    /**
     * Elimina un archivo de Firebase Storage usando su URL de descarga.
     */
    suspend fun eliminarArchivo(urlDescarga: String): Boolean = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<Boolean>()

        try {
            val almacenamiento: com.google.firebase.storage.FirebaseStorage = FirebaseStorage.getInstance()
            val referencia = almacenamiento.getReferenceFromUrl(urlDescarga)
            referencia.delete().addOnSuccessListener {
                deferred.complete(true)
            }.addOnFailureListener { error ->
                Log.e("NubeArchivos", "Error al eliminar: ${error.message}")
                deferred.complete(false)
            }
        } catch (e: Exception) {
            Log.e("NubeArchivos", "Excepción al eliminar: ${e.message}")
            deferred.complete(false)
        }

        deferred.await()
    }
}