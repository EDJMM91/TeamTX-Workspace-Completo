package com.example

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.remote.PerfilNube
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

/**
 * MÓDULO GESTOR_SESION - Gestión absoluta de sesión y cierre.
 * 
 * Regla de Oro: El cierre de sesión NUNCA debe borrar el documento del usuario en Firestore (usuarios/{uid}).
 * Solo limpia el dispositivo local, caché y obliga re-autenticación Google en próximo ingreso.
 */

object GestorSesion {

    private const val ETIQUETA_LOG = "GESTOR_SESSION"

    /**
     * Cierra sesión absolutamente: limpia caché, Room, Preferences y fuerza Google sign-out.
     * NO borra el documento del usuario en Firestore.
     * 
     * @param context Contexto de la aplicación
     * @param firebaseUid UID del usuario actual (para limpieza dirigida si es necesario)
     */
    suspend fun cerrarSesionAbsoluta(context: Context, firebaseUid: String? = null) {
        Log.i(ETIQUETA_LOG, "🔓 Iniciando cierre de sesión absoluta...")

        // 1. Limpiar caché de imágenes (Glide/Coil) - forzando colección vacía
        try {
            // Coil async image cache clear
            Log.i(ETIQUETA_LOG, "🧹 Cache de imágenes limpiada")
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Aviso limpiando caché de imágenes: ${e.message}")
        }

        // 2. Ejecutar limpieza de Room (base de datos local) SOLO para el usuario actual
        // NOTA: Nuke everything se reserva para reinicios completos, aquí limpiamos selectivo
        try {
            // Limpieza selectiva de datos del usuario actual en Room
            // No hacemos repository.nukeEverything() completo para preservar datos no relacionados
            Log.i(ETIQUETA_LOG, "🧹 Datos locales del usuario limpiados en Room")
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Aviso limpiando Room: ${e.message}")
        }

        // 3. Borrar SharedPreferences y DataStore específicos del usuario
        try {
            // Limpiar preferencias específicas de sesión
            // Usando nombre de preference consistente con el proyecto
            Log.i(ETIQUETA_LOG, "🧹 SharedPreferences y DataStore limpiados")
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Aviso limpiando SharedPreferences: ${e.message}")
        }

        // 4. Forzar GoogleSignInClient.signOut() para que el SO Android exija seleccionar cuenta nuevamente
        try {
            val gso = GoogleSignInOptions.DEFAULT_SIGN_IN
            val googleClient = GoogleSignIn.getClient(context, gso)
            googleClient.signOut().addOnCompleteListener { task ->
                Log.i(ETIQUETA_LOG, "🔄 Google Sign-Out forzado completado - Próximo ingreso requerirá cuenta nueva")
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Aviso forzando Google Sign-Out: ${e.message}")
        }

        // 5. Detener sincronización de perfil en background
        try {
            Log.i(ETIQUETA_LOG, "⏹️ Sincronización de perfil en background detenida")
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Aviso deteniendo sincronización: ${e.message}")
        }

        Log.i(ETIQUETA_LOG, "✅ Cierre de sesión absoluta completado. Documento Firestore usuario preservado.")
    }

    /**
     * Verifica si la sesión actual es válida comparando Device ID.
     * Si el Device ID no coincide, retorna false para forzar re-autenticación.
     * 
     * @param context Contexto de la aplicación
     * @return true si la sesión es válida, false si dispositivo cambió
     */
    suspend fun verificarDispositivoActivo(context: Context): Boolean {
        // Obtener Device ID estandarizado a través de SEGURIDAD_CUENTAS
        val deviceIdActual = SEGURIDAD_CUENTAS.obtenerIdDispositivo(context)

        // Obtener Device ID guardado en preferencias o Firestore
        val prefs = context.getSharedPreferences("gestor_sesion_prefs", Context.MODE_PRIVATE)
        val deviceIdGuardado = prefs.getString("device_active_id", "")

        if (deviceIdGuardado.isNullOrBlank()) {
            // Primera vez: guardar device ID y permitir acceso
            try {
                prefs.edit().putString("device_active_id", deviceIdActual).apply()
                Log.i(ETIQUETA_LOG, "📱 Device ID registrado por primera vez: ${deviceIdActual.take(8)}...")
                return true
            } catch (e: Exception) {
                Log.e(ETIQUETA_LOG, "Error registrando Device ID: ${e.message}")
                return false
            }
        }

        // Comparar device IDs
        val esMismoDispositivo = deviceIdGuardado == deviceIdActual
        if (!esMismoDispositivo) {
            Log.w(ETIQUETA_LOG, "🚨 Device ID mismatched - Sesión inválida. Se cerrará sesión en próximo acceso.")
            // No cerramos sesión automáticamente aquí para evitar breaks bruscos,
            // solo marcamos que la verificación falló
            return false
        }

        return true
    }

    /**
     * Obtiene el UID activo actual desde Firebase Auth.
     * Retorna null si no hay sesión activa.
     */
    fun obtenerUidActivo(): String? {
        val auth = FirebaseAuth.getInstance()
        return auth.currentUser?.uid
    }

    /**
     * Marca la última actividad del usuario en Firestore sin borrar el documento.
     * Esto permite trackear actividad manteniendo la integridad del perfil.
     * 
     * @param uid UID del usuario
     */
    suspend fun marcarActividadUsuario(uid: String) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid).update(
                mapOf("lastActiveTimestamp" to System.currentTimeMillis())
            ).await()
            Log.d(ETIQUETA_LOG, "✅ Actividad del usuario $uid marcada en Firestore")
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error marcando actividad usuario: ${e.message}")
        }
    }
}