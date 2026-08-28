package com.example.chat

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * ARCHIVO: GESTOR_STICKERS.kt
 *
 * Módulo independiente para gestión de stickers WebP (formato WhatsApp).
 * Responsabilidades:
 * - Lanzar selector de archivos del sistema filtrando .webp
 * - Escanear WhatsApp automáticamente en Android 11+ (Android/media/com.whatsapp/...)
 *   y Android 10 y anteriores (WhatsApp/Media/...)
 * - Selector de carpeta personalizada (SAF)
 * - Copiar archivos a almacenamiento interno privado (filesDir/stickers_guardados)
 * - Guardar stickers del chat en Favoritos
 */
object GestorStickers {

    private const val CARPETA_STICKERS = "stickers_guardados"
    private const val EXTENSION_WEBP = ".webp"
    private const val MIME_WEBP = "image/webp"

    // Estado reactivo de stickers guardados
    private val _stickersGuardados = MutableStateFlow<List<File>>(emptyList())
    val stickersGuardados: StateFlow<List<File>> = _stickersGuardados.asStateFlow()

    // Referencias a launchers
    private var launcherSelectorRef: AtomicReference<ActivityResultLauncher<Intent>?> = AtomicReference(null)
    private var launcherCarpetaRef: AtomicReference<ActivityResultLauncher<Intent>?> = AtomicReference(null)

    /**
     * Inicializa los launchers de selección de archivos y carpetas.
     * DEBE llamarse desde la Activity principal (MainActivity) en onCreate.
     */
    fun inicializarLauncher(activity: ComponentActivity) {
        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->
            if (resultado.resultCode == Activity.RESULT_OK) {
                val data = resultado.data
                data?.data?.let { uri ->
                    copiarStickerDesdeUri(activity, uri)
                }
                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { uri ->
                            copiarStickerDesdeUri(activity, uri)
                        }
                    }
                }
            }
        }
        launcherSelectorRef.set(launcher)

        val launcherCarpeta = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->
            if (resultado.resultCode == Activity.RESULT_OK) {
                resultado.data?.data?.let { treeUri ->
                    try {
                        activity.contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}
                    importarDesdeCarpetaTreeUri(activity, treeUri)
                }
            }
        }
        launcherCarpetaRef.set(launcherCarpeta)
    }

    /**
     * Abre el selector de archivos del sistema filtrando solo imágenes .webp.
     */
    fun abrirSelectorStickers(activity: Activity) {
        val launcher = launcherSelectorRef.get()
            ?: throw IllegalStateException("GestorStickers no inicializado. Llama a inicializarLauncher() en MainActivity.onCreate()")

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_WEBP
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(MIME_WEBP))
        }
        launcher.launch(intent)
    }

    /**
     * Abre el selector para elegir directamente la carpeta de WhatsApp / Stickers del teléfono.
     */
    fun abrirSelectorCarpetaStickers(activity: Activity) {
        val launcher = launcherCarpetaRef.get()
        if (launcher != null) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            launcher.launch(intent)
        } else {
            abrirSelectorStickers(activity)
        }
    }

    /**
     * Importa todos los archivos .webp de una carpeta seleccionada mediante DocumentTree (SAF).
     */
    fun importarDesdeCarpetaTreeUri(context: Context, treeUri: Uri) = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        var importados = 0
        try {
            val carpetaDestino = obtenerCarpetaStickers(context)
            val existentes = carpetaDestino.listFiles()?.map { it.length() }?.toSet() ?: emptySet()

            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: "wa_doc_${System.currentTimeMillis()}$EXTENSION_WEBP"
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)

                    if ((name.lowercase().endsWith(EXTENSION_WEBP) || mime == MIME_WEBP) && !existentes.contains(size)) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        val destino = File(carpetaDestino, name)
                        val inputStream = context.contentResolver.openInputStream(docUri)
                        if (inputStream != null) {
                            inputStream.use { input ->
                                FileOutputStream(destino).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            importados++
                        }
                    }
                }
            }

            if (importados > 0) {
                actualizarListaStickers(context)
            }

            withContext(Dispatchers.Main) {
                val msg = if (importados > 0) "✅ Se importaron $importados stickers de la carpeta" else "No se encontraron stickers .webp en esa carpeta"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("GestorStickers", "Error importando desde treeUri: ${e.message}")
        }
    }

    /**
     * Obtiene una URI segura para un archivo local (FileProvider o Uri.fromFile).
     */
    fun obtenerUriArchivo(context: Context, archivo: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )
        } catch (_: Exception) {
            Uri.fromFile(archivo)
        }
    }

    /**
     * Copia un archivo .webp desde URI externo al almacenamiento interno privado.
     */
    private fun copiarStickerDesdeUri(context: Context, uri: Uri) = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val carpetaDestino = obtenerCarpetaStickers(context)
            val nombreArchivo = obtenerNombreArchivo(context, uri) ?: "sticker_${System.currentTimeMillis()}$EXTENSION_WEBP"
            var archivoDestino = File(carpetaDestino, nombreArchivo)

            var contador = 1
            var nombreFinal = nombreArchivo
            while (archivoDestino.exists()) {
                val nombreBase = nombreArchivo.removeSuffix(EXTENSION_WEBP)
                nombreFinal = "${nombreBase}_$contador$EXTENSION_WEBP"
                archivoDestino = File(carpetaDestino, nombreFinal)
                contador++
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(archivoDestino).use { output ->
                    input.copyTo(output)
                }
            }

            actualizarListaStickers(context)
            Log.d("GestorStickers", "Sticker importado: ${archivoDestino.name}")
        } catch (e: Exception) {
            Log.e("GestorStickers", "Error al copiar sticker: ${e.message}")
        }
    }

    /**
     * Elimina un sticker específico del almacenamiento interno.
     */
    fun eliminarSticker(context: Context, archivo: File) = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            if (archivo.exists() && archivo.delete()) {
                actualizarListaStickers(context)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Sticker eliminado", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("GestorStickers", "Error eliminando sticker: ${e.message}")
        }
    }

    /**
     * Lee el directorio interno y emite la lista de archivos .webp ordenados por fecha.
     */
    fun cargarStickersGuardados(context: Context) {
        val carpeta = obtenerCarpetaStickers(context)
        val archivos = carpeta.listFiles { _, name ->
            name.lowercase().endsWith(EXTENSION_WEBP)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        _stickersGuardados.value = archivos
    }

    /**
     * Actualiza la lista en segundo plano y emite el nuevo estado.
     */
    private suspend fun actualizarListaStickers(context: Context) = withContext(Dispatchers.IO) {
        val carpeta = obtenerCarpetaStickers(context)
        val archivos = carpeta.listFiles { _, name ->
            name.lowercase().endsWith(EXTENSION_WEBP)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        _stickersGuardados.value = archivos
    }

    /**
     * Obtiene (y crea si no existe) la carpeta interna para stickers.
     */
    private fun obtenerCarpetaStickers(context: Context): File {
        val carpeta = File(context.filesDir, CARPETA_STICKERS)
        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }
        return carpeta
    }

    /**
     * Extrae el nombre original del archivo desde el ContentResolver.
     */
    private fun obtenerNombreArchivo(context: Context, uri: Uri): String? {
        var nombre: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val indiceNombre = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (indiceNombre != -1) {
                        nombre = it.getString(indiceNombre)
                    }
                }
            }
        }
        if (nombre == null) {
            nombre = uri.path?.let { path ->
                val corte = path.lastIndexOf('/')
                if (corte != -1) path.substring(corte + 1) else path
            }
        }
        return nombre
    }

    /**
     * Guarda cualquier sticker del chat como FAVORITO localmente.
     */
    fun guardarStickerComoFavorito(context: Context, stickerSource: String, onResult: (Boolean) -> Unit = {}) = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val carpetaDestino = obtenerCarpetaStickers(context)
            val nombreArchivo = "fav_${System.currentTimeMillis()}$EXTENSION_WEBP"
            val archivoDestino = File(carpetaDestino, nombreArchivo)

            if (stickerSource.startsWith("http://") || stickerSource.startsWith("https://")) {
                val url = java.net.URL(stickerSource)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.inputStream.use { input ->
                    FileOutputStream(archivoDestino).use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (stickerSource.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(stickerSource))?.use { input ->
                    FileOutputStream(archivoDestino).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                val file = File(stickerSource)
                if (file.exists()) {
                    file.copyTo(archivoDestino, overwrite = true)
                }
            }

            actualizarListaStickers(context)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "⭐ Sticker añadido a tus Favoritos", android.widget.Toast.LENGTH_SHORT).show()
                onResult(true)
            }
        } catch (e: Exception) {
            Log.e("GestorStickers", "Error guardando sticker en favoritos: ${e.message}")
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error al guardar el sticker", android.widget.Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }

    fun descargarSticker(context: Context, url: String) {
        guardarStickerComoFavorito(context, url)
    }

    /**
     * Escanea automáticamente las carpetas conocidas de WhatsApp en el dispositivo
     * tanto en Android 11+ (Scoped Storage en Android/media) como en Android 10 y anteriores (WhatsApp/Media).
     */
    fun escanearStickersWhatsApp(context: Context, onComplete: (Int) -> Unit = {}) = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        var importados = 0
        try {
            val carpetaDestino = obtenerCarpetaStickers(context)
            val archivosExistentes = carpetaDestino.listFiles()?.map { it.length() }?.toSet() ?: emptySet()

            // 1. Escaneo en todas las rutas directas de WhatsApp (Android 11+ y Android 10-)
            val posiblesRutasWhatsApp = listOf(
                // 📱 Android 11+ (Scoped Storage en Android/media)
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers"),
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers/Sent"),
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Stickers"),
                File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers"),
                File("/sdcard/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers"),

                // 📱 Android 10 y versiones anteriores
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Stickers"),
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Stickers/Sent"),
                File(Environment.getExternalStorageDirectory(), "WhatsApp Business/Media/WhatsApp Business Stickers"),
                File("/storage/emulated/0/WhatsApp/Media/WhatsApp Stickers"),
                File("/sdcard/WhatsApp/Media/WhatsApp Stickers"),

                // 📁 Rutas complementarias de imágenes y descargas
                File(Environment.getExternalStorageDirectory(), "Pictures/WhatsApp Stickers"),
                File(Environment.getExternalStorageDirectory(), "Pictures/WhatsApp/WhatsApp Stickers"),
                File(Environment.getExternalStorageDirectory(), "Download/WhatsApp Stickers"),
                File(Environment.getExternalStorageDirectory(), "Download")
            )

            for (directorio in posiblesRutasWhatsApp) {
                if (directorio.exists() && directorio.isDirectory) {
                    try {
                        val archivos = directorio.listFiles()
                        archivos?.forEach { archivoSticker ->
                            if (archivoSticker.isFile && archivoSticker.length() in 1024..1048576) {
                                val isWebp = archivoSticker.name.lowercase().endsWith(EXTENSION_WEBP) || esArchivoWebp(archivoSticker)
                                if (isWebp && !archivosExistentes.contains(archivoSticker.length())) {
                                    val nombreSeguro = if (archivoSticker.name.lowercase().endsWith(EXTENSION_WEBP)) {
                                        archivoSticker.name
                                    } else {
                                        "${archivoSticker.name}$EXTENSION_WEBP"
                                    }
                                    val destino = File(carpetaDestino, "wa_${nombreSeguro}")
                                    archivoSticker.copyTo(destino, overwrite = true)
                                    importados++
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("GestorStickers", "No se pudo leer directorio ${directorio.absolutePath}: ${e.message}")
                    }
                }
            }

            // 2. Escaneo complementario vía MediaStore (Compatible con Android 10 a 15)
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE
                )
                val selection = "${MediaStore.Images.Media.MIME_TYPE} = ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf(MIME_WEBP, "%$EXTENSION_WEBP")

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "wa_sticker_$id$EXTENSION_WEBP"
                        val size = cursor.getLong(sizeCol)

                        if (size in 1024..1048576 && !archivosExistentes.contains(size)) {
                            val contentUri = android.content.ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            val destino = File(carpetaDestino, "ms_${id}_$name")
                            val inputStream = context.contentResolver.openInputStream(contentUri)
                            if (inputStream != null) {
                                inputStream.use { input ->
                                    FileOutputStream(destino).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                importados++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GestorStickers", "Error en query MediaStore: ${e.message}")
            }

            if (importados > 0) {
                actualizarListaStickers(context)
            }

            withContext(Dispatchers.Main) {
                val msg = if (importados > 0) {
                    "✅ Se encontraron e importaron $importados stickers de WhatsApp"
                } else {
                    "ℹ️ No se detectaron nuevos stickers. Usa el botón '📁 Carpeta' para elegir la carpeta directamente."
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                onComplete(importados)
            }
        } catch (e: Exception) {
            Log.e("GestorStickers", "Error escaneando stickers de WhatsApp: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(importados)
            }
        }
    }

    /**
     * Comprueba si la cabecera de un archivo corresponde al formato RIFF....WEBP
     */
    private fun esArchivoWebp(file: File): Boolean {
        return try {
            if (file.length() < 12) return false
            val bytes = ByteArray(12)
            file.inputStream().use { it.read(bytes, 0, 12) }
            val riff = String(bytes, 0, 4, Charsets.US_ASCII)
            val webp = String(bytes, 8, 4, Charsets.US_ASCII)
            riff == "RIFF" && webp == "WEBP"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Verifica si el launcher está inicializado.
     */
    fun estaInicializado(): Boolean = launcherSelectorRef.get() != null
}