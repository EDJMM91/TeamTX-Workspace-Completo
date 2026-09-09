package com.example

import android.content.Context
import android.util.Log
import com.example.ui.preferences.PreferenciasApp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

/**
 * Módulo de seguridad para gestión de identidades, identificador único de dispositivo
 * y cierre forzoso de sesión ("Hard Logout") para evitar trampas en gamificación.
 */
object SEGURIDAD_CUENTAS {

    private const val ETIQUETA_LOG = "SEGURIDAD_CUENTAS"
    private const val NOMBRE_PREFERENCIAS = "preferencias_seguridad_tx"
    private const val CLAVE_ID_DISPOSITIVO = "id_dispositivo_activo_unico"

    /**
     * Obtiene el identificador único persistente para este dispositivo móvil.
     * Si ya existe en SharedPreferences lo devuelve; si no, lo genera con UUID y lo almacena.
     */
    fun obtenerIdDispositivo(contexto: Context): String {
        val preferencias = contexto.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE)
        var idGuardado = preferencias.getString(CLAVE_ID_DISPOSITIVO, null)
        
        if (idGuardado.isNullOrBlank()) {
            idGuardado = UUID.randomUUID().toString()
            preferencias.edit().putString(CLAVE_ID_DISPOSITIVO, idGuardado).apply()
            Log.i(ETIQUETA_LOG, "🔑 Nuevo identificador único generado para dispositivo: $idGuardado")
        }
        return idGuardado
    }

    /**
     * Ejecuta un cierre forzoso total de sesión ("Hard Logout"):
     * 1. Cierra sesión en FirebaseAuth.
     * 2. Desconecta y revoca acceso en GoogleSignInClient para evitar inicio silencioso automático.
     * 3. Limpia cachés locales de SharedPreferences (prefs_radar_tx, preferencias de carnet).
     * 4. Purga la memoria de imágenes de Coil.
     * 5. Ejecuta la acción de terminación [alTerminar].
     */
    fun forzarCierreSesion(contexto: Context, alTerminar: () -> Unit = {}) {
        Log.w(ETIQUETA_LOG, "🚨 Ejecutando Hard Logout forzoso por seguridad de cuentas...")

        // 1. Cerrar sesión en Firebase Auth
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error al cerrar sesión en FirebaseAuth: ${error.message}")
        }

        // 2. Limpiar cachés privadas locales
        try {
            contexto.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE).edit().clear().apply()
            PreferenciasApp.carnetGooglePhotoUrl = null
            PreferenciasApp.carnetTipoFoto = "LOCAL"
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error al limpiar cachés de preferencias: ${error.message}")
        }

        // 3. Purgar caché de imágenes en memoria (evita que persista foto del carnet anterior)
        try {
            coil.Coil.imageLoader(contexto).memoryCache?.clear()
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error al purgar caché de Coil: ${error.message}")
        }

        // 4. Revocar credenciales en Google Sign-In
        try {
            val opcionesGoogle = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val clienteGoogle = GoogleSignIn.getClient(contexto, opcionesGoogle)
            
            clienteGoogle.signOut().addOnCompleteListener {
                clienteGoogle.revokeAccess().addOnCompleteListener {
                    Log.i(ETIQUETA_LOG, "✅ Credenciales de Google revocadas y sesión cerrada.")
                    alTerminar()
                }
            }
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error al revocar acceso en Google Sign-In: ${error.message}")
            alTerminar()
        }
    }
}
