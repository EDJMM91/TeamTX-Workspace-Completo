package com.example.mapa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * ARCHIVO: GESTOR_PORTAPAPELES.kt
 * 
 * El Lector Silencioso del Portapapeles.
 * Lee el texto actual del portapapeles del sistema, lo pasa por el analizador
 * y extrae coordenadas válidas automáticamente para la creación de Avisos y Publicaciones.
 */
object GestorPortapapeles {

    /**
     * Lee el texto plano del portapapeles de Android si existe.
     */
    fun leerTexto(contexto: Context): String? {
        return try {
            val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val item = clipboard.primaryClip?.getItemAt(0)
                item?.text?.toString() ?: item?.uri?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lee el portapapeles y, si contiene una coordenada válida, la retorna en formato "lat,lon".
     */
    fun leerCoordenadaValida(contexto: Context): String? {
        val texto = leerTexto(contexto)
        return AnalizadorCoordenadas.extraerCoordenadas(texto)
    }

    /**
     * Limpia el portapapeles o copia texto nuevo si es necesario.
     */
    fun copiarTexto(contexto: Context, etiqueta: String, texto: String) {
        try {
            val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(etiqueta, texto)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {}
    }
}
