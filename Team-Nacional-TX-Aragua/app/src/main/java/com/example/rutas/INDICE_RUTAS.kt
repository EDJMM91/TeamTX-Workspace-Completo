package com.example.rutas

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.osmand.plus.views.OsmandMapTileView

/**
 * Índice y Fachada Central del Módulo de Rutas TX (Team Nacional TX Aragua).
 *
 * Facilita el acceso limpio, modular y desacoplado a todos los subsistemas de rutas:
 * - Tracking GPS y Seguro de Vida (Batería): [RUTA]
 * - Servicio en segundo plano persistente: [SERVICIO]
 * - Sincronización en la nube con Firebase: [NUBE]
 * - Estudio de reproducción cinemática 2D (Relive): [REPRODUCTOR]
 * - Compilación y guardado de video MP4 en Galería: [RUTASVIDEO]
 *
 * Nomenclatura 100% en español. Cumple con MODULOS_GESTOR.md.
 */
object INDICE_RUTAS {

    // Fachadas directas a los submódulos
    val gestorTracking = RUTA
    val gestorReproductor = REPRODUCTOR
    val gestorVideo = RUTASVIDEO
    val gestorNube = NUBE
    val gestorServicio = SERVICIO

    /**
     * Inicia una ruta motera activando el servicio en segundo plano y el seguro de batería.
     */
    fun iniciarTracking(contexto: Context, tituloRuta: String, idPiloto: String, nombrePiloto: String) {
        SERVICIO.iniciarServicioRuta(contexto, tituloRuta, idPiloto, nombrePiloto)
    }

    /**
     * Detiene el tracking activo y el servicio en segundo plano.
     */
    fun detenerTracking(contexto: Context) {
        SERVICIO.detenerServicioRuta(contexto)
        RUTA.detenerRuta(contexto)
    }

    /**
     * Inicia la reproducción cinemática 2D de la ruta activa o seleccionada.
     */
    fun reproducirRuta(
        ruta: ResumenRutaTX,
        mapView: OsmandMapTileView? = null,
        multiplicadorVelocidad: Float = 2.0f,
        alFinalizar: () -> Unit = {}
    ) {
        REPRODUCTOR.iniciarReproduccion2D(
            ruta = ruta,
            mapView = mapView,
            multiplicadorVelocidad = multiplicadorVelocidad,
            alFinalizar = alFinalizar
        )
    }

    /**
     * Inicia la grabación en video MP4 de la animación en pantalla.
     */
    fun iniciarGrabacionVideo(
        contexto: Context,
        mediaProjectionIntent: Intent,
        resultCode: Int,
        ruta: ResumenRutaTX,
        alTerminar: (String) -> Unit
    ) {
        RUTASVIDEO.crearVideo(contexto, mediaProjectionIntent, resultCode, ruta, alTerminar)
    }

    /**
     * Finaliza la grabación y almacena el archivo MP4 en la Galería del teléfono.
     */
    fun finalizarYGuardarVideo(contexto: Context, alCompletar: (Uri?) -> Unit) {
        RUTASVIDEO.detenerYGuardarVideoGaleria(contexto, alCompletar)
    }

    /**
     * Genera el reporte textual estructurado para compartir por WhatsApp o redes.
     */
    fun obtenerReporteTexto(ruta: ResumenRutaTX): String {
        return RUTASVIDEO.generarReporteTexto(ruta)
    }
}
