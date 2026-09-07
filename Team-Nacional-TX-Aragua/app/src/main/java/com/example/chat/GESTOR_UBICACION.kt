package com.example.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * ARCHIVO: GESTOR_UBICACION.kt
 * 
 * Módulo táctico multi-fuente para capturar con máxima fidelidad la coordenada
 * GPS exacta del piloto en carretera y emergencias SOS.
 * Incorpora cascada de 5 fuentes:
 * 1. Fused Location Provider (Última ubicación conocida)
 * 2. Fused Location Provider (Petición en vivo alta precisión con timeout seguro)
 * 3. Android LocationManager nativo (GPS_PROVIDER / NETWORK_PROVIDER / PASSIVE_PROVIDER)
 * 4. Proveedor de OsmAnd en vivo (lastKnownLocation)
 * 5. Caché de telemetría previa en SharedPreferences
 * 6. Geocodificación inversa para traducir automáticamente coordenadas a dirección vial humana.
 */
class GestorUbicacion(private val contexto: Context) {

    companion object {
        private const val TAG = "GESTOR_UBICACION"
    }

    private val proveedorUbicacion = LocationServices.getFusedLocationProviderClient(contexto)

    /**
     * Verifica si el usuario concedió permisos de localización.
     */
    fun tienePermisos(): Boolean {
        val permisoFino = ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
        val permisoGrueso = ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION)
        return permisoFino == PackageManager.PERMISSION_GRANTED || permisoGrueso == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Captura el objeto [Location] nativo más preciso y actualizado del dispositivo.
     */
    suspend fun capturarLocationObjeto(): Location? {
        if (!tienePermisos()) {
            Log.w(TAG, "Permisos de ubicación no otorgados")
            return null
        }

        // 1. Fused Location Client: última ubicación conocida
        try {
            val lastLoc: Location? = proveedorUbicacion.lastLocation.await()
            if (lastLoc != null && esCoordenadaValida(lastLoc.latitude, lastLoc.longitude)) {
                // Si la ubicación tiene menos de 10 minutos, es excelente
                if (System.currentTimeMillis() - lastLoc.time < 600_000) {
                    Log.d(TAG, "Ubicación Fused en caché obtenida: ${lastLoc.latitude}, ${lastLoc.longitude}")
                    return lastLoc
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en lastLocation: ${e.message}")
        }

        // 2. Fused Location Client: captura fresca en vivo (máximo 4 segundos de espera)
        try {
            val freshLoc = withTimeoutOrNull(4000L) {
                proveedorUbicacion.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            }
            if (freshLoc != null && esCoordenadaValida(freshLoc.latitude, freshLoc.longitude)) {
                Log.d(TAG, "Ubicación Fused fresca obtenida: ${freshLoc.latitude}, ${freshLoc.longitude}")
                return freshLoc
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en getCurrentLocation: ${e.message}")
        }

        // 3. LocationManager nativo del sistema Android
        try {
            val lm = contexto.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                var mejorLoc: Location? = null
                for (prov in providers) {
                    try {
                        if (lm.isProviderEnabled(prov)) {
                            val l = lm.getLastKnownLocation(prov)
                            if (l != null && esCoordenadaValida(l.latitude, l.longitude)) {
                                if (mejorLoc == null || l.time > mejorLoc.time) {
                                    mejorLoc = l
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (mejorLoc != null) {
                    Log.d(TAG, "Ubicación LocationManager obtenida: ${mejorLoc.latitude}, ${mejorLoc.longitude}")
                    return mejorLoc
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en LocationManager: ${e.message}")
        }

        // 4. Proveedor interno de OsmAnd (si el mapa estuvo abierto)
        try {
            val app = contexto.applicationContext as? net.osmand.plus.OsmandApplication
            val osmandLoc = app?.locationProvider?.lastKnownLocation
            if (osmandLoc != null && esCoordenadaValida(osmandLoc.latitude, osmandLoc.longitude)) {
                val loc = Location("OsmAnd").apply {
                    latitude = osmandLoc.latitude
                    longitude = osmandLoc.longitude
                    time = osmandLoc.time
                }
                Log.d(TAG, "Ubicación OsmAnd obtenida: ${loc.latitude}, ${loc.longitude}")
                return loc
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando OsmAnd location: ${e.message}")
        }

        // 5. SharedPreferences del radar táctico
        try {
            val prefs = contexto.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
            val sLat = prefs.getString("last_lat", null)?.toDoubleOrNull()
            val sLon = prefs.getString("last_lon", null)?.toDoubleOrNull()
            if (sLat != null && sLon != null && esCoordenadaValida(sLat, sLon)) {
                val loc = Location("RadarPrefs").apply {
                    latitude = sLat
                    longitude = sLon
                    time = System.currentTimeMillis()
                }
                Log.d(TAG, "Ubicación de SharedPreferences recuperada: $sLat, $sLon")
                return loc
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando SharedPreferences: ${e.message}")
        }

        return null
    }

    /**
     * Captura la coordenada en formato "latitud,longitud".
     */
    suspend fun capturarCoordenadaActual(): String? {
        val loc = capturarLocationObjeto() ?: return null
        return "${loc.latitude},${loc.longitude}"
    }

    /**
     * Traduce coordenadas geográficas en un nombre de dirección vial legible.
     * Ejemplo: "Av. Las Delicias, Maracay, Aragua" o "Autopista Regional del Centro, Aragua".
     */
    fun obtenerNombreUbicacion(lat: Double, lon: Double): String {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(contexto, Locale("es", "VE"))
                val resultados = geocoder.getFromLocation(lat, lon, 1)
                if (!resultados.isNullOrEmpty()) {
                    val addr = resultados[0]
                    val calle = addr.thoroughfare ?: addr.featureName ?: ""
                    val sector = addr.subLocality ?: addr.locality ?: ""
                    val estado = addr.adminArea ?: ""
                    val partes = listOf(calle, sector, estado).filter { it.isNotBlank() }
                    if (partes.isNotEmpty()) {
                        return partes.joinToString(", ")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en Geocoder: ${e.message}")
        }
        return "Coordenadas GPS: ${String.format(Locale.US, "%.5f, %.5f", lat, lon)}"
    }

    /**
     * Verifica que la coordenada no sea nula, cero o el valor de descarte de Caracas por defecto.
     */
    private fun esCoordenadaValida(lat: Double, lon: Double): Boolean {
        if (lat == 0.0 && lon == 0.0) return false
        if (lat.isNaN() || lon.isNaN()) return false
        // Descarta valores fuera de rango geográfico terrestre
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return false
        return true
    }
}
