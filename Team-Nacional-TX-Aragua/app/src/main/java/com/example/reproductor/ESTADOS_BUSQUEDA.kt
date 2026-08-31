package com.example.reproductor

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS DE DATOS Y ESTADOS - BUSCADOR Y DESCARGAS YT (REPRODUCTOR TX PRO)
// Define los estados del flujo de búsqueda, resultados y descargas.
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Resultado individual de una búsqueda en YouTube.
 * Contiene toda la información necesaria para mostrar y descargar el video.
 */
data class ResultadoBusquedaYT(
    val titulo: String,
    val autor: String,
    val duracionSegundos: Long,
    val urlVideo: String,
    val urlThumbnail: String,
    val idVideo: String,
    val urlDescargaDirecta: String? = null,
    val album: String = "Descargas TX",
    val fuente: String = "Online"
) {
    /**
     * Duración formateada como mm:ss o h:mm:ss
     */
    val duracionFormateada: String
        get() {
            val horas = duracionSegundos / 3600
            val minutos = (duracionSegundos % 3600) / 60
            val segundos = duracionSegundos % 60
            return if (horas > 0) {
                String.format("%d:%02d:%02d", horas, minutos, segundos)
            } else {
                String.format("%02d:%02d", minutos, segundos)
            }
        }
}

/**
 * Estado de una descarga individual en progreso.
 */
data class EstadoDescarga(
    val idVideo: String,
    val titulo: String,
    val progreso: Float = 0f,
    val porcentaje: Int = 0,
    val estado: TipoEstadoDescarga = TipoEstadoDescarga.EN_COLA,
    val rutaArchivoGuardado: String? = null,
    val mensajeError: String? = null
)

/**
 * Tipos de estado posibles para una descarga.
 */
enum class TipoEstadoDescarga {
    EN_COLA,            // Esperando para iniciar
    DESCARGANDO,        // Descargando audio de YouTube
    CONVIRTIENDO,       // Convirtiendo a MP3 320kbps con FFmpeg
    GUARDANDO,          // Guardando en MediaStore y carpeta pública
    COMPLETADO,         // Descarga finalizada exitosamente
    ERROR               // Error durante la descarga
}

/**
 * Estado global del panel de búsqueda y descargas.
 */
sealed class EstadoPanelYT {
    /**
     * Estado inicial: sin búsquedas realizadas.
     */
    object Inactivo : EstadoPanelYT()

    /**
     * Búsqueda en progreso con el término dado.
     */
    data class Buscando(val termino: String) : EstadoPanelYT()

    /**
     * Resultados de búsqueda disponibles.
     */
    data class ListaResultados(val resultados: List<ResultadoBusquedaYT>, val termino: String) : EstadoPanelYT()

    /**
     * Error durante la búsqueda.
     */
    data class ErrorBusqueda(val mensaje: String) : EstadoPanelYT()
}

/**
 * Información de una descarga en cola para el MediaStore.
 */
data class InfoDescargaCompleta(
    val idVideo: String,
    val titulo: String,
    val autor: String,
    val duracionSegundos: Long,
    val rutaArchivo: String,
    val uriMediaStore: String,
    val fechaDescarga: Long = System.currentTimeMillis()
)
