package com.example.reproductor

import android.net.Uri

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS DE DATOS - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Modelos independientes en español básico para canciones, listas y estados.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Modelo representativo de una canción local en el dispositivo.
 */
data class CancionMotera(
    val id: Long = 0L,
    val titulo: String = "Pista Desconocida",
    val artista: String = "Artista Desconocido",
    val album: String = "Álbum Desconocido",
    val duracionMs: Long = 0L,
    val rutaArchivo: String = "",
    val uriStr: String = "",
    val portadaUriStr: String? = null,
    val urlCaratulaOnline: String? = null,
    val fechaAgregada: Long = System.currentTimeMillis(),
    val esFavorita: Boolean = false,
    val tamanoBytes: Long = 0L,
    val carpetaContenedora: String = ""
) {
    val duracionFormateada: String
        get() {
            val totalSegundos = duracionMs / 1000
            val minutos = totalSegundos / 60
            val segundos = totalSegundos % 60
            return String.format("%02d:%02d", minutos, segundos)
        }

    val formato: String
        get() = rutaArchivo.substringAfterLast('.', "MP3").uppercase()

    val tamanoLegible: String
        get() {
            val bytes = if (tamanoBytes > 0) tamanoBytes else {
                try { java.io.File(rutaArchivo).length() } catch (_: Exception) { 0L }
            }
            return if (bytes > 0) {
                val mb = bytes.toDouble() / (1024.0 * 1024.0)
                String.format(java.util.Locale.US, "%.2f MB", mb)
            } else "N/D"
        }
}

/**
 * Lista de reproducción personalizada del club.
 */
data class ListaReproduccionMotera(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nombre: String = "Nueva Lista TX",
    val descripcion: String = "Selección para rodadas",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val cancionIds: List<Long> = emptyList(),
    val icono: String = "🎵"
)

/**
 * Estados del motor de reproducción.
 */
enum class EstadoReproductor {
    DETENIDO,
    REPRODUCIENDO,
    PAUSADO,
    CARGANDO
}

/**
 * Modos de repetición de cola.
 */
enum class ModoBucle {
    SIN_BUCLE,       // Se detiene al terminar la lista
    BUCLE_TODAS,     // Vuelve al inicio al terminar la lista
    BUCLE_UNA        // Repite indefinidamente la canción actual
}

/**
 * Modos de descarga automática de carátulas.
 */
enum class ModoDescargaCaratulas(val titulo: String, val descripcion: String) {
    SOLO_WIFI("Solo Wi-Fi", "Descarga carátulas únicamente cuando estés conectado a Wi-Fi"),
    WIFI_Y_DATOS("Wi-Fi y Datos Móviles", "Descarga carátulas con cualquier conexión a Internet"),
    DESACTIVADO("Desactivado", "No descargar carátulas online automáticamente")
}

/**
 * Pestañas de navegación interna del reproductor.
 */
enum class PestanaReproductor(val titulo: String, val icono: String) {
    CANCIONES("Canciones", "🎵"),
    LISTAS("Listas", "📂"),
    FAVORITAS("Favoritas", "❤️"),
    ALBUMES("Álbumes", "💿"),
    ARTISTAS("Artistas", "🎙️"),
    CARPETAS("Carpetas", "📁"),
    DESCARGAS_YT("Descargas YT", "⬇️"),
    AJUSTES("Ajustes", "⚙️")
}

/**
 * Preset predefinido del ecualizador.
 */
data class PresetEcualizador(
    val nombre: String,
    val gananciasDb: FloatArray, // 5 bandas: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val superBass: Float = 0.0f,
    val ultraVolumen: Float = 1.0f,
    val espacialidad: Float = 0.0f
)

/**
 * Configuración general y persistente del Reproductor TX.
 */
data class ConfiguracionReproductor(
    val nubeFlotanteActiva: Boolean = true,
    val nubeBloqueada: Boolean = false,
    val posicionNubeX: Float = 40f,
    val posicionNubeY: Float = 250f,
    val autoOcultarNube: Boolean = true,
    val duracionMinimaSegundos: Int = 30, // Excluir notas de voz cortas
    val ultraVolumenNivel: Float = 1.0f,   // 1.0f (Normal 100%) a 3.0f (300%)
    val superBassNivel: Float = 0.4f,      // 0.0f a 1.0f
    val espacialidadNivel: Float = 0.2f,   // 0.0f a 1.0f
    val bandasEcualizador: FloatArray = floatArrayOf(2.0f, 1.0f, 0.0f, 2.0f, 3.0f),
    val presetActual: String = "Rock Motero",
    val excluirCarpetasWhatsApp: Boolean = true,
    val excluirAudiosCortos: Boolean = true,
    val descargaCaratulasModo: ModoDescargaCaratulas = ModoDescargaCaratulas.WIFI_Y_DATOS
)
