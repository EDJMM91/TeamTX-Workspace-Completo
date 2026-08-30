package com.example.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/**
 * ARCHIVO: GESTOR_UBICACION.kt
 * 
 * Módulo independiente para obtener la ubicación actual del dispositivo.
 * Actúa como un sensor silencioso: no abre mapas ni UI, solo retorna texto.
 * Optimizado para minimizar el consumo de batería y datos en carretera.
 */
class GestorUbicacion(private val contexto: Context) {
    
    // Cliente nativo de Android optimizado para ubicación
    private val proveedorUbicacion = LocationServices.getFusedLocationProviderClient(contexto)

    /**
     * Captura la latitud y longitud actual del piloto.
     * Retorna un String con formato "latitud,longitud" listo para Firebase y Room.
     * Si no hay permisos o GPS deshabilitado, retorna nulo.
     */
    suspend fun capturarCoordenadaActual(): String? {
        // Validación de seguridad de permisos
        val permisoFino = ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
        val permisoGrueso = ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (permisoFino != PackageManager.PERMISSION_GRANTED && permisoGrueso != PackageManager.PERMISSION_GRANTED) {
            return null // Los permisos se deben solicitar en la Vista antes de llamar a esta función
        }

        return try {
            // 1. Intentar lectura rápida de la última posición conocida (cero consumo extra)
            val ubicacion: Location? = proveedorUbicacion.lastLocation.await()
            if (ubicacion != null && (System.currentTimeMillis() - ubicacion.time) < 120_000) {
                "${ubicacion.latitude},${ubicacion.longitude}"
            } else {
                // 2. Si no hay posición reciente en caché, solicitar posición puntual de alta precisión
                val ubicacionFresca: Location? = proveedorUbicacion.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
                if (ubicacionFresca != null) {
                    "${ubicacionFresca.latitude},${ubicacionFresca.longitude}"
                } else if (ubicacion != null) {
                    "${ubicacion.latitude},${ubicacion.longitude}"
                } else {
                    null
                }
            }
        } catch (error: Exception) {
            null
        }
    }
}
