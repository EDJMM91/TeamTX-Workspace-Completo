package com.example.rutas

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio en segundo plano (Foreground Service) para el tracking continuo de rutas moteras
 * y monitoreo ininterrumpido de batería (Seguro de Vida).
 *
 * Previene que el sistema Android mate el proceso al minimizar la aplicación o apagar la pantalla.
 * Nomenclatura en español estricta.
 */
class SERVICIO : Service(), LocationListener {

    companion object {
        private const val TAG = "TEAM_TX_SERVICIO_RUTA"
        private const val CANAL_ID = "canal_servicio_rutas_tx"
        private const val NOTIFICACION_ID = 9912

        private val _servicioActivoState = MutableStateFlow(false)
        val servicioActivoState: StateFlow<Boolean> = _servicioActivoState.asStateFlow()

        /**
         * Inicia el servicio en segundo plano de la ruta.
         *
         * @param contexto Contexto de la aplicación o actividad.
         * @param tituloRuta Nombre de la ruta iniciada.
         * @param idPiloto Identificador del piloto.
         * @param nombrePiloto Nombre o apodo del piloto.
         */
        fun iniciarServicioRuta(contexto: Context, tituloRuta: String, idPiloto: String, nombrePiloto: String) {
            val intent = Intent(contexto, SERVICIO::class.java).apply {
                action = "INICIAR"
                putExtra("tituloRuta", tituloRuta)
                putExtra("idPiloto", idPiloto)
                putExtra("nombrePiloto", nombrePiloto)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                contexto.startForegroundService(intent)
            } else {
                contexto.startService(intent)
            }
        }

        /**
         * Detiene el servicio en segundo plano.
         *
         * @param contexto Contexto de la aplicación.
         */
        fun detenerServicioRuta(contexto: Context) {
            val intent = Intent(contexto, SERVICIO::class.java).apply {
                action = "DETENER"
            }
            contexto.startService(intent)
        }
    }

    private var locationManager: LocationManager? = null

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        RUTA.registrarReceptorBateria(applicationContext)
        Log.d(TAG, "🟢 SERVICIO de ruta creado correctamente.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val accion = intent?.action
        if (accion == "DETENER") {
            detenerServicioLocal()
            return START_NOT_STICKY
        }

        val titulo = intent?.getStringExtra("tituloRuta") ?: "Ruta TX Aragua"
        val idPiloto = intent?.getStringExtra("idPiloto") ?: ""
        val nombrePiloto = intent?.getStringExtra("nombrePiloto") ?: "Piloto TX"

        // Iniciar la ruta en la lógica central RUTA.kt si aún no está activa
        if (RUTA.resumenRutaState.value.estadoRuta == EstadoRutaEnum.INACTIVA) {
            RUTA.iniciarRuta(titulo, idPiloto, nombrePiloto)
        }

        val notificacion = construirNotificacion("Grabando Ruta: $titulo", "Tracking continuo y Seguro de Vida (Batería) activo")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICACION_ID, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICACION_ID, notificacion)
        }

        iniciarLecturaGPS()
        _servicioActivoState.value = true

        return START_STICKY
    }

    private fun iniciarLecturaGPS() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, // Cada 2 segundos
                2.0f,  // O cada 2 metros
                this
            )
            Log.d(TAG, "🛰️ Lectura GPS en segundo plano iniciada")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permiso de ubicación denegado en servicio: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando lectura GPS: ${e.localizedMessage}")
        }
    }

    override fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        RUTA.agregarPunto(
            latitud = location.latitude,
            longitud = location.longitude,
            altitudMts = location.altitude,
            velocidadKmh = speedKmh
        )
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun detenerServicioLocal() {
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}

        RUTA.desregistrarReceptorBateria(applicationContext)
        _servicioActivoState.value = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        Log.d(TAG, "🔴 SERVICIO de ruta detenido completamente")
    }

    override fun onDestroy() {
        detenerServicioLocal()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Servicio de Rutas TX",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente para la grabación de rutas y seguro de batería"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(titulo: String, mensaje: String): Notification {
        val builder = NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }
}
