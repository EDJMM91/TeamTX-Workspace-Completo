package com.example.mapa

import java.util.regex.Pattern

/**
 * ARCHIVO: ANALIZADOR_COORDENADAS.kt
 * 
 * El Cerebro Extractor de Coordenadas.
 * Recibe cualquier texto copiado del portapapeles (incluso con texto sucio o URLs de OsmAnd/Google Maps)
 * y extrae limpiamente la latitud y longitud ("lat,lon") validando los rangos geográficos.
 */
object AnalizadorCoordenadas {

    // Regex para URL de OsmAnd: https://osmand.net/go?lat=10.228&lon=-67.475 o variantes
    private val PATRON_OSMAND_URL = Pattern.compile(
        """https?://(?:www\.)?osmand\.net/go(?:\.html)?\?(?:.*&)?lat=(-?\d+(?:\.\d+)?)&(?:.*&)?lon=(-?\d+(?:\.\d+)?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex para URI geo: geo:10.228,-67.475 o geo:0,0?q=10.228,-67.475
    private val PATRON_GEO_URI = Pattern.compile(
        """geo:(?:(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)|(?:0,0\?q=(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)))""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex para Google Maps / OpenStreetMap URLs
    private val PATRON_MAPS_URL = Pattern.compile(
        """(?:https?://)?(?:www\.)?(?:google\.com/maps|maps\.google\.com|openstreetmap\.org).*?[?&@/](-?\d{1,2}\.\d+)[,/](-?\d{1,3}\.\d+)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex para texto de coordenadas estándar: "10.228, -67.475" o "Lat: 10.228 Lon: -67.475"
    private val PATRON_COORDENADAS_TEXTO = Pattern.compile(
        """(-?\d{1,2}(?:\.\d+)?)[,\s]+(-?\d{1,3}(?:\.\d+)?)"""
    )

    // Regex para coordenadas con cardinales: "10.228 N, 67.475 W" o "10.228° N, 67.475° O"
    private val PATRON_CARDINALES = Pattern.compile(
        """(\d{1,2}(?:\.\d+)?)[°\s]*([NSns])[,\s]+(\d{1,3}(?:\.\d+)?)[°\s]*([EWOWewow])"""
    )

    /**
     * Analiza el texto recibido y extrae la coordenada en formato estándar "latitud,longitud".
     * Retorna null si el texto no contiene coordenadas válidas.
     */
    fun extraerCoordenadas(textoBruto: String?): String? {
        if (textoBruto.isNullOrBlank()) return null
        val texto = textoBruto.trim()

        // 1. Probar URL de OsmAnd
        val matcherOsmAnd = PATRON_OSMAND_URL.matcher(texto)
        if (matcherOsmAnd.find()) {
            val lat = matcherOsmAnd.group(1)?.toDoubleOrNull()
            val lon = matcherOsmAnd.group(2)?.toDoubleOrNull()
            if (esCoordenadaValida(lat, lon)) {
                return "$lat,$lon"
            }
        }

        // 2. Probar URI geo:
        val matcherGeo = PATRON_GEO_URI.matcher(texto)
        if (matcherGeo.find()) {
            val lat = (matcherGeo.group(1) ?: matcherGeo.group(3))?.toDoubleOrNull()
            val lon = (matcherGeo.group(2) ?: matcherGeo.group(4))?.toDoubleOrNull()
            if (esCoordenadaValida(lat, lon)) {
                return "$lat,$lon"
            }
        }

        // 3. Probar URLs de Mapas (Google Maps, OSM)
        val matcherMaps = PATRON_MAPS_URL.matcher(texto)
        if (matcherMaps.find()) {
            val lat = matcherMaps.group(1)?.toDoubleOrNull()
            val lon = matcherMaps.group(2)?.toDoubleOrNull()
            if (esCoordenadaValida(lat, lon)) {
                return "$lat,$lon"
            }
        }

        // 4. Probar formato con direcciones cardinales (N/S, E/W/O)
        val matcherCardinal = PATRON_CARDINALES.matcher(texto)
        if (matcherCardinal.find()) {
            var lat = matcherCardinal.group(1)?.toDoubleOrNull()
            val dirLat = matcherCardinal.group(2)?.uppercase()
            var lon = matcherCardinal.group(3)?.toDoubleOrNull()
            val dirLon = matcherCardinal.group(4)?.uppercase()

            if (lat != null && lon != null) {
                if (dirLat == "S") lat = -lat
                if (dirLon == "W" || dirLon == "O") lon = -lon
                if (esCoordenadaValida(lat, lon)) {
                    return "$lat,$lon"
                }
            }
        }

        // 5. Probar formato texto numérico directo ("10.228, -67.475")
        // Limpiamos palabras de prefijo comunes ("Ubicación:", "Coordenadas:", "📍", etc.)
        val textoLimpio = texto
            .replace("📍", "")
            .replace("Ubicación:", "", ignoreCase = true)
            .replace("Ubicacion:", "", ignoreCase = true)
            .replace("Coordenadas:", "", ignoreCase = true)
            .replace("Lat:", "", ignoreCase = true)
            .replace("Lon:", "", ignoreCase = true)
            .replace("Lng:", "", ignoreCase = true)
            .trim()

        val matcherTexto = PATRON_COORDENADAS_TEXTO.matcher(textoLimpio)
        if (matcherTexto.find()) {
            val lat = matcherTexto.group(1)?.toDoubleOrNull()
            val lon = matcherTexto.group(2)?.toDoubleOrNull()
            if (esCoordenadaValida(lat, lon)) {
                return "$lat,$lon"
            }
        }

        return null
    }

    /**
     * Valida que los valores numéricos correspondan a latitudes (-90 a 90) y longitudes (-180 a 180).
     */
    fun esCoordenadaValida(latitud: Double?, longitud: Double?): Boolean {
        if (latitud == null || longitud == null) return false
        return latitud >= -90.0 && latitud <= 90.0 && longitud >= -180.0 && longitud <= 180.0 &&
                (latitud != 0.0 || longitud != 0.0) // Evitar 0.0, 0.0 como coordenada nula de GPS
    }
}
