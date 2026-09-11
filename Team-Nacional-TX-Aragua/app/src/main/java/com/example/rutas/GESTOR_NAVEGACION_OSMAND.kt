package com.example.rutas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.radar.GestorRadar
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.search.ShowQuickSearchMode

object GestorNavegacionOsmand {

    /**
     * Inicia una ruta guiada en OsmAnd inyectando las coordenadas en el TargetPointsHelper.
     * No modifica el core de OsmAnd.
     */
    fun iniciarRutaGuiada(context: Context, origenLat: Double, origenLon: Double, destinoLat: Double, destinoLon: Double) {
        val app = context.applicationContext as? OsmandApplication
        if (app == null) {
            Toast.makeText(context, "El motor del Mapa TX no está inicializado.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val targetPointsHelper = app.targetPointsHelper
            
            // Limpiar puntos previos
            targetPointsHelper.clearPointToNavigate(false)
            targetPointsHelper.clearStartPoint(false)

            // Inyectar el origen (como si el usuario lo hubiera seleccionado en el mapa)
            val pOrigen = LatLon(origenLat, origenLon)
            targetPointsHelper.setStartPoint(pOrigen, false, PointDescription(PointDescription.POINT_TYPE_LOCATION, "Origen Ruta TX"))

            // Inyectar el destino y activar la ruta
            val pDestino = LatLon(destinoLat, destinoLon)
            targetPointsHelper.navigateToPoint(pDestino, true, -1, PointDescription(PointDescription.POINT_TYPE_LOCATION, "Destino Ruta TX"))

            // Abrir el mapa para que OsmAnd renderice la ruta automáticamente
            val intent = Intent(context, MapActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Error al inyectar coordenadas en OsmAnd: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inicia inmediatamente la navegación GPS guiada paso a paso por voz en OsmAnd hacia el destino.
     */
    fun navegarADestino(context: Context, destinoLat: Double, destinoLon: Double, nombreDestino: String = "Destino Ruta TX") {
        val app = context.applicationContext as? OsmandApplication
        val loc = try { app?.locationProvider?.lastKnownLocation } catch (_: Exception) { null }
        val origLat = loc?.latitude ?: 0.0
        val origLon = loc?.longitude ?: 0.0
        iniciarRutaGuiada(context, origLat, origLon, destinoLat, destinoLon)
    }

    /**
     * Llama al buscador nativo de OsmAnd (QuickSearch).
     */
    fun invocarBuscadorNativo(context: Context, query: String = "") {
        // Intentar obtener la MapActivity actual a través de GestorRadar
        val mapActivity = GestorRadar.obtenerMapActivity()
        
        if (mapActivity != null) {
            // Si el mapa ya está en memoria y en pantalla, abrimos el buscador de fragmentos nativo
            if (query.isNotEmpty()) {
                mapActivity.fragmentsHelper.showQuickSearch(query)
            } else {
                mapActivity.fragmentsHelper.showQuickSearch(ShowQuickSearchMode.NEW, true)
            }
            
            // Volver al mapa si estábamos en otra pantalla
            val intent = Intent(context, MapActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            context.startActivity(intent)
        } else {
            // Si el mapa no está corriendo, podemos abrirlo con un intent geo: si hay query, 
            // o arrancar MapActivity y enviar el intent geo.
            val uri = if (query.isNotEmpty()) Uri.parse("geo:0,0?q=${Uri.encode(query)}") else Uri.parse("geo:0,0")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage(context.packageName) // Asegurar que lo abre nuestro propio OsmAnd
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback
                val fallbackIntent = Intent(context, MapActivity::class.java)
                fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * Revisa el portapapeles y extrae un LatLon si encuentra coordenadas válidas.
     * Formatos soportados: "Lat, Lon" o enlaces con coordenadas.
     */
    fun leerCoordenadasPortapapeles(context: Context): LatLon? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) return null
        
        val clip: ClipData = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        
        val texto = clip.getItemAt(0).text?.toString() ?: return null
        
        // Regex para buscar "lat, lon"
        val regex = Regex("(-?\\d{1,2}\\.\\d+)\\s*,\\s*(-?\\d{1,3}\\.\\d+)")
        val match = regex.find(texto)
        
        if (match != null) {
            val (latStr, lonStr) = match.destructured
            try {
                val lat = latStr.toDouble()
                val lon = lonStr.toDouble()
                if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                    return LatLon(lat, lon)
                }
            } catch (e: Exception) {
                // Ignorar parse error
            }
        }
        
        // Regex para urls geo:lat,lon
        val regexGeo = Regex("geo:(-?\\d{1,2}\\.\\d+),(-?\\d{1,3}\\.\\d+)")
        val matchGeo = regexGeo.find(texto)
        if (matchGeo != null) {
            val (latStr, lonStr) = matchGeo.destructured
            try {
                val lat = latStr.toDouble()
                val lon = lonStr.toDouble()
                if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                    return LatLon(lat, lon)
                }
            } catch (e: Exception) {
                // Ignorar
            }
        }
        
        return null
    }
}
