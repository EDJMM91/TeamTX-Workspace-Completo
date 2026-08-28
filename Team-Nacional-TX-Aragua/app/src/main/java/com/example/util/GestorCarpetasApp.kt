package com.example.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * ARCHIVO: GestorCarpetasApp.kt
 *
 * Módulo centralizado para la gestión y creación de la jerarquía de carpetas
 * locales de la aplicación con NOMBRES 100% EN ESPAÑOL y fácil acceso para el usuario.
 *
 * Estructura organizada:
 * 📁 Team Nacional TX/
 *    ├── 📁 Audios de Voz/
 *    ├── 📁 Stickers y Memes/
 *    ├── 📁 Fotos del Club/
 *    ├── 📁 Documentos y Reportes/
 *    ├── 📁 Rutas y Mapas/
 *    └── 📁 Copias de Seguridad/
 */
object GestorCarpetasApp {

    private const val TAG = "GestorCarpetasApp"

    // Nombre de la carpeta raíz pública en el almacenamiento del teléfono
    const val CARPETA_RAIZ_PUBLIC = "Team Nacional TX"

    // Subcarpetas en español entendible
    const val SUB_AUDIOS = "Audios de Voz"
    const val SUB_STICKERS = "Stickers y Memes"
    const val SUB_FOTOS = "Fotos del Club"
    const val SUB_DOCUMENTOS = "Documentos y Reportes"
    const val SUB_RUTAS = "Rutas y Mapas"
    const val SUB_RESPALDOS = "Copias de Seguridad"

    /**
     * Inicializa y asegura que todas las carpetas existan en el dispositivo.
     * Se llama al inicio en MainActivity o TeamTxApplication.
     */
    fun inicializarCarpetas(context: Context) {
        try {
            val subcarpetas = listOf(SUB_AUDIOS, SUB_STICKERS, SUB_FOTOS, SUB_DOCUMENTOS, SUB_RUTAS, SUB_RESPALDOS)

            // 1. Carpetas internas privadas de la app
            subcarpetas.forEach { sub ->
                obtenerCarpetaInterna(context, sub).mkdirs()
            }

            // 2. Carpetas en Almacenamiento Externo de la App (sdcard/Android/data/.../files/Team Nacional TX/...)
            try {
                val extFilesDir = context.getExternalFilesDir(null)
                if (extFilesDir != null) {
                    val rootExt = File(extFilesDir, CARPETA_RAIZ_PUBLIC)
                    subcarpetas.forEach { sub ->
                        File(rootExt, sub).mkdirs()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo crear en externalFilesDir: ${e.message}")
            }

            // 3. Carpetas públicas en la raíz del almacenamiento (/sdcard/Team Nacional TX/...)
            try {
                val baseDir = Environment.getExternalStorageDirectory()
                val root = File(baseDir, CARPETA_RAIZ_PUBLIC)
                subcarpetas.forEach { sub ->
                    File(root, sub).mkdirs()
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo crear en almacenamiento raíz: ${e.message}")
            }

            // 4. Carpetas públicas en Documentos (/sdcard/Documents/Team Nacional TX/...)
            try {
                val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val rootDocs = File(docDir, CARPETA_RAIZ_PUBLIC)
                subcarpetas.forEach { sub ->
                    File(rootDocs, sub).mkdirs()
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo crear en Documents: ${e.message}")
            }

            Log.d(TAG, "✅ Sistema de carpetas en español inicializado en todos los niveles")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando sistema de carpetas: ${e.message}")
        }
    }

    /**
     * Obtiene la carpeta interna privada de la aplicación para una categoría.
     */
    fun obtenerCarpetaInterna(context: Context, subcarpeta: String): File {
        val root = File(context.filesDir, CARPETA_RAIZ_PUBLIC)
        val dir = File(root, subcarpeta)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Obtiene la carpeta pública en la memoria principal del teléfono (sdcard/Team Nacional TX/...).
     */
    fun obtenerCarpetaPublica(subcarpeta: String): File? {
        return try {
            val baseDir = Environment.getExternalStorageDirectory()
            val root = File(baseDir, CARPETA_RAIZ_PUBLIC)
            val dir = File(root, subcarpeta)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (_: Exception) {
            null
        }
    }

    // Métodos directos para cada tipo de contenido

    fun getCarpetaAudios(context: Context): File = obtenerCarpetaInterna(context, SUB_AUDIOS)
    fun getCarpetaStickers(context: Context): File = obtenerCarpetaInterna(context, SUB_STICKERS)
    fun getCarpetaFotos(context: Context): File = obtenerCarpetaInterna(context, SUB_FOTOS)
    fun getCarpetaDocumentos(context: Context): File = obtenerCarpetaInterna(context, SUB_DOCUMENTOS)
    fun getCarpetaRutas(context: Context): File = obtenerCarpetaInterna(context, SUB_RUTAS)
    fun getCarpetaRespaldos(context: Context): File = obtenerCarpetaInterna(context, SUB_RESPALDOS)

    /**
     * Devuelve una descripción legible de las carpetas creadas para mostrarla en la pantalla de Info / Ajustes.
     */
    fun obtenerResumenCarpetas(context: Context): List<Pair<String, String>> {
        val rootPublic = try {
            File(Environment.getExternalStorageDirectory(), CARPETA_RAIZ_PUBLIC).absolutePath
        } catch (_: Exception) {
            "Almacenamiento del Dispositivo"
        }

        return listOf(
            "📁 Audios de Voz" to "$rootPublic/$SUB_AUDIOS (Mensajes y notas grabadas)",
            "📁 Stickers y Memes" to "$rootPublic/$SUB_STICKERS (Colección WebP y WhatsApp)",
            "📁 Fotos del Club" to "$rootPublic/$SUB_FOTOS (Fotos de eventos, carnet y perfiles)",
            "📁 Documentos y Reportes" to "$rootPublic/$SUB_DOCUMENTOS (PDFs, normativas y finanzas)",
            "📁 Rutas y Mapas" to "$rootPublic/$SUB_RUTAS (Trazados GPX y waypoints)",
            "📁 Copias de Seguridad" to "$rootPublic/$SUB_RESPALDOS (Respaldos locales de datos)"
        )
    }
}
