package com.example.radar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.MainActivity

class TelemetriaGps : Service() {

    companion object {
        private const val ETIQUETA = "RADAR_TELEMETRIA"
        private const val CHANNEL_ID = "radar_telemetria_tx"
        private const val NOTIFICATION_ID = 999
        private const val INTERVALO_MS = 10_000L
        private const val PREFS_NOMBRE = "prefs_radar_tx"
        private const val PREFS_ACTIVO = "radar_activo"
        private const val PREFS_USER_ID = "radar_user_id"
        private const val PREFS_NOMBRE_PILOTO = "radar_nombre"
        private const val PREFS_RANGO = "radar_rango"
        private const val PREFS_AVATAR_URL = "radar_avatar"
        private const val PREFS_ALERTA_SOS = "radar_alerta_sos"
        private const val COLECCION = "radar_en_vivo"

        fun activar(contexto: Context, userId: String, nombre: String = "", rango: String = "", avatarUrl: String = "", alertaSos: String = "") {
            val prefs = contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(PREFS_ACTIVO, true)
                .putString(PREFS_USER_ID, userId)
                .putString(PREFS_NOMBRE_PILOTO, nombre)
                .putString(PREFS_RANGO, rango)
                .putString(PREFS_AVATAR_URL, avatarUrl)
                .putString(PREFS_ALERTA_SOS, alertaSos)
                .apply()
            val intent = Intent(contexto, TelemetriaGps::class.java)
            contexto.startForegroundService(intent)
            Log.d(ETIQUETA, "Servicio de telemetría solicitado para: $nombre (SOS: $alertaSos)")
        }

        fun desactivar(contexto: Context) {
            val prefs = contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
            val userId = prefs.getString(PREFS_USER_ID, "") ?: ""
            prefs.edit().putBoolean(PREFS_ACTIVO, false).apply()
            contexto.stopService(Intent(contexto, TelemetriaGps::class.java))
            if (userId.isNotBlank()) {
                borrarUbicacionDelServidor(userId)
            }
            Log.d(ETIQUETA, "Servicio de telemetría detenido")
        }

        fun estaActivo(contexto: Context): Boolean {
            return contexto.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
                .getBoolean(PREFS_ACTIVO, false)
        }

        private fun borrarUbicacionDelServidor(userId: String) {
            FirebaseFirestore.getInstance().collection(COLECCION).document(userId)
                .delete()
                .addOnSuccessListener { Log.d(ETIQUETA, "Ubicación borrada del servidor") }
                .addOnFailureListener { Log.w(ETIQUETA, "Error al borrar: ${it.message}") }
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private lateinit var prefs: SharedPreferences
    private var userId: String = ""
    private var nombrePiloto: String = ""
    private var rangoPiloto: String = ""
    private var avatarUrlPiloto: String = ""
    private var locationCallback: LocationCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        firestore = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)
        userId = prefs.getString(PREFS_USER_ID, "") ?: ""
        nombrePiloto = prefs.getString(PREFS_NOMBRE_PILOTO, "") ?: ""
        rangoPiloto = prefs.getString(PREFS_RANGO, "") ?: ""
        avatarUrlPiloto = prefs.getString(PREFS_AVATAR_URL, "") ?: ""
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.getBoolean(PREFS_ACTIVO, false)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            crearNotificacion(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        iniciarCaptura()
        Log.d(ETIQUETA, "Servicio foreground iniciado para piloto: $userId")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerCaptura()
        Log.d(ETIQUETA, "Servicio destruido")
    }

    private fun iniciarCaptura() {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, INTERVALO_MS)
            .setMinUpdateIntervalMillis(INTERVALO_MS / 2)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                subirAUbicacion(loc.latitude, loc.longitude)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback!!, null)
        } catch (e: SecurityException) {
            Log.e(ETIQUETA, "Sin permisos de ubicación: ${e.message}")
            desactivar(this)
        }
    }

    private fun detenerCaptura() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun subirAUbicacion(lat: Double, lon: Double) {
        if (userId.isBlank()) return

        val alertaSos = prefs.getString(PREFS_ALERTA_SOS, "") ?: ""
        val datos = hashMapOf(
            "id" to userId,
            "lat" to lat,
            "lon" to lon,
            "timestamp" to System.currentTimeMillis(),
            "activo" to true,
            "nombre" to nombrePiloto,
            "rango" to rangoPiloto,
            "avatarUrl" to avatarUrlPiloto,
            "alertaSos" to alertaSos
        )

        prefs.edit()
            .putString("last_lat", lat.toString())
            .putString("last_lon", lon.toString())
            .apply()

        firestore.collection(COLECCION).document(userId)
            .set(datos as Map<String, Any>, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(ETIQUETA, "Ubicación subida: $lat, $lon")
            }
            .addOnFailureListener { e ->
                Log.w(ETIQUETA, "Error al subir ubicación: ${e.message}")
            }
    }

    private fun crearCanalNotificacion() {
        val canal = NotificationChannel(
            CHANNEL_ID,
            "Radar TX",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene el radar táctico activo"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(canal)
    }

    private fun crearNotificacion(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Radar TX Activo")
            .setContentText("Compartiendo ubicación con la flota")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
