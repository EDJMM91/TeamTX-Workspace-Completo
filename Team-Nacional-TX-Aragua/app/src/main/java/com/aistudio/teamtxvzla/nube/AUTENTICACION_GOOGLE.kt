package com.aistudio.teamtxvzla.nube

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Datos del usuario autenticado con Google + Firebase.
 * Se devuelve al módulo del Carnet para mostrar en la UI.
 */
data class DatosUsuarioGoogle(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val fotoUrl: String? = null,
    val esNuevoRegistro: Boolean = false
)

/**
 * Resultado de la operación de login con Google.
 */
sealed class ResultadoGoogleLogin {
    data class Exito(val datos: DatosUsuarioGoogle) : ResultadoGoogleLogin()
    data class Error(val mensaje: String) : ResultadoGoogleLogin()
    data class RequiereVinculacion(val uid: String, val email: String, val fotoUrl: String?) : ResultadoGoogleLogin()
}

/**
 * ═══════════════════════════════════════════════════════════════
 * MÓDULO AUTENTICACION_GOOGLE
 * ═══════════════════════════════════════════════════════════════
 *
 * Módulo centralizado para login con Google en la app Team TX.
 * Toda la lógica de Google Sign-In vive aquí. Las pantallas
 * SOLO consumen el resultado a través de los callbacks.
 *
 * FLUJO COMPLETO:
 * 1. Usuario toca "Vincular con Google" en el Carnet
 * 2. Se abre el selector de cuentas de Google
 * 3. Google devuelve un idToken
 * 4. Se llama a FirebaseAuth.signInWithCredential(token)
 * 5. Firebase registra al usuario en la consola (Authentication → Users)
 * 6. Se devuelven los datos del usuario al Carnet
 *
 * Web Client ID: 554561749183-q637440n43im5mjfg5pp9lsb4uu0a441.apps.googleusercontent.com
 * Firebase Console: Authentication → Sign-in method → Google = HABILITADO
 * ═══════════════════════════════════════════════════════════════
 */
object AutenticacionGoogle {

    private const val ETIQUETA = "AUTENTICACION_GOOGLE"

    /**
     * Web Client ID registrado en Firebase Console para esta app.
     * OBLIGATORIO: debe coincidir con el que aparece en
     * Firebase Console → Project Settings → Your apps → Web client ID
     */
    private const val WEB_CLIENT_ID =
        "554561749183-q637440n43im5mjfg5pp9lsb4uu0a441.apps.googleusercontent.com"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // ═══════════════════════════════════════════════
    // CREAR CLIENTE DE GOOGLE SIGN-IN
    // ═══════════════════════════════════════════════

    /**
     * Crea el GoogleSignInClient configurado con el Web Client ID de Firebase.
     * Debe llamarse una vez por pantalla (usar `remember` en Composable).
     */
    fun crearCliente(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Lanza el selector de cuentas de Google.
     * La pantalla debe implementar un ActivityResultLauncher con esta función.
     */
    fun lanzarSelector(cliente: GoogleSignInClient, launcher: ActivityResultLauncher<Intent>) {
        Log.i(ETIQUETA, "Abriendo selector de cuentas de Google...")
        launcher.launch(cliente.signInIntent)
    }

    // ═══════════════════════════════════════════════
    // PROCESAR RESULTADO DEL SELECTOR
    // ═══════════════════════════════════════════════

    /**
     * Procesa el resultado del selector de cuentas de Google.
     * Extrae el idToken y lo envía a Firebase para registrar al usuario.
     *
     * @param resultCode Código de resultado de la Activity
     * @param data Intent con los datos del selector
     * @param alExito Callback cuando el login es exitoso → devuelve DatosUsuarioGoogle
     * @param alError Callback cuando falla → devuelve mensaje de error
     */
    fun procesarResultado(
        resultCode: Int,
        data: Intent?,
        alExito: (DatosUsuarioGoogle) -> Unit,
        alError: (String) -> Unit
    ) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            val task = data?.let { GoogleSignIn.getSignedInAccountFromIntent(it) }
            if (task != null) {
                try {
                    task.getResult(ApiException::class.java)
                } catch (e: ApiException) {
                    if (e.statusCode != 12501 && e.statusCode != 12502) {
                        Log.e(ETIQUETA, "ApiException: ${e.statusCode}", e)
                        alError("Error de Google (${e.statusCode})")
                    } else {
                        // 12501 = Cancelled by user, 12502 = Already signed in
                        Log.i(ETIQUETA, "Selector cancelado por el usuario")
                    }
                }
            }
            return
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val email = account?.email ?: ""
            val photoUrl = account?.photoUrl?.toString()

            if (idToken == null) {
                alError("No se pudo obtener el token de Google")
                return
            }

            // ═══════════════════════════════════════════════
            // PASO CRÍTICO: Inyectar token en Firebase Auth
            // Esto registra al usuario en la consola de Firebase
            // ═══════════════════════════════════════════════
            inyectarEnFirebase(idToken, email, photoUrl, alExito, alError)

        } catch (e: ApiException) {
            if (e.statusCode != 12501 && e.statusCode != 12502) {
                Log.e(ETIQUETA, "ApiException: ${e.statusCode}", e)
                alError("Error al procesar cuenta de Google (${e.statusCode})")
            }
        }
    }

    /**
     * Inyecta el idToken de Google en Firebase Auth.
     * Esto es OBLIGATORIO para que el usuario aparezca en
     * Firebase Console → Authentication → Users.
     *
     * FLUJO:
     * 1. GoogleAuthProvider.getCredential(idToken) → credencial Firebase
     * 2. auth.signInWithCredential(credencial) → Firebase registra al usuario
     * 3. Si el usuario ya existía, Firebase lo reconoce (no crea duplicado)
     * 4. Se devuelven los datos del usuario al Carnet
     */
    private fun inyectarEnFirebase(
        idToken: String,
        emailGoogle: String,
        fotoGoogle: String?,
        alExito: (DatosUsuarioGoogle) -> Unit,
        alError: (String) -> Unit
    ) {
        Log.i(ETIQUETA, "Inyectando token de Google en Firebase Auth...")

        val credencial = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credencial)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user
                if (usuario != null) {
                    Log.i(ETIQUETA, "═══════════════════════════════════════")
                    Log.i(ETIQUETA, "  GOOGLE + FIREBASE EXITOSO")
                    Log.i(ETIQUETA, "  UID: ${usuario.uid}")
                    Log.i(ETIQUETA, "  Nombre: ${usuario.displayName}")
                    Log.i(ETIQUETA, "  Email: ${usuario.email}")
                    Log.i(ETIQUETA, "  Foto: ${usuario.photoUrl}")
                    Log.i(ETIQUETA, "═══════════════════════════════════════")

                    val datos = DatosUsuarioGoogle(
                        uid = usuario.uid,
                        nombre = usuario.displayName ?: "",
                        correo = usuario.email ?: emailGoogle,
                        fotoUrl = usuario.photoUrl?.toString() ?: fotoGoogle,
                        esNuevoRegistro = resultado.additionalUserInfo?.isNewUser ?: false
                    )
                    alExito(datos)
                } else {
                    alError("Firebase devolvió usuario null tras signInWithCredential")
                }
            }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "FALLO en signInWithCredential", error)
                val msg = when {
                    error.message?.contains("INVALID_ID_TOKEN") == true ->
                        "Token de Google inválido. Reintentá."
                    error.message?.contains("CREDENTIAL_ALREADY_IN_USE") == true ->
                        "Esta cuenta Google ya está vinculada a otro usuario."
                    error.message?.contains("OPERATION_NOT_ALLOWED") == true ->
                        "Google Auth no está habilitado.\n\nPasos para habilitar:\n1. Firebase Console → Authentication → Sign-in method\n2. Habilitar 'Google' como proveedor\n3. Agregar SHA-1 en Project Settings → Your apps"
                    error.message?.contains("INVALID_CREDENTIAL") == true ->
                        "Credenciales de Google inválidas.\n\nVerifica en Firebase Console:\n1. Authentication → Sign-in method → Google = Habilitado\n2. Project Settings → SHA-1 certificate fingerprints registrado"
                    else -> "Error de Firebase: ${error.message}\n\nSi es error de configuración, verifica:\n1. Firebase Console → Authentication → Sign-in method → Google habilitado\n2. Project Settings → SHA-1 registrado"
                }
                alError(msg)
            }
    }

    // ═══════════════════════════════════════════════
    // VERSIÓN SUSPEND (para coroutines)
    // ═══════════════════════════════════════════════

    /**
     * Versión suspend de inyectarEnFirebase.
     * Útil cuando se llama desde un coroutine scope.
     */
    suspend fun inyectarEnFirebaseAwait(
        idToken: String,
        emailGoogle: String,
        fotoGoogle: String?
    ): ResultadoGoogleLogin {
        return try {
            val credencial = GoogleAuthProvider.getCredential(idToken, null)
            val resultado = auth.signInWithCredential(credencial).await()
            val usuario = resultado.user

            if (usuario != null) {
                Log.i(ETIQUETA, "✅ Firebase Auth exitoso: UID=${usuario.uid}")
                ResultadoGoogleLogin.Exito(
                    DatosUsuarioGoogle(
                        uid = usuario.uid,
                        nombre = usuario.displayName ?: "",
                        correo = usuario.email ?: emailGoogle,
                        fotoUrl = usuario.photoUrl?.toString() ?: fotoGoogle,
                        esNuevoRegistro = resultado.additionalUserInfo?.isNewUser ?: false
                    )
                )
            } else {
                ResultadoGoogleLogin.Error("Firebase devolvió usuario null")
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA, "FALLO en inyectarEnFirebaseAwait", e)
            ResultadoGoogleLogin.Error("Error de Firebase: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    // CERRAR SESIÓN
    // ═══════════════════════════════════════════════

    /**
     * Cierra la sesión de Google (no de Firebase).
     * Útil para forzar selector de cuentas la próxima vez.
     */
    fun cerrarSesionGoogle(context: Context) {
        crearCliente(context).signOut()
        Log.i(ETIQUETA, "Sesión de Google cerrada")
    }

    /**
     * Revoca el acceso de la app a la cuenta de Google.
     * El usuario deberá re-autorizar la app la próxima vez.
     */
    fun revocarAcceso(context: Context) {
        crearCliente(context).revokeAccess()
        Log.i(ETIQUETA, "Acceso de Google revocado")
    }

    // ═══════════════════════════════════════════════
    // COMPOSABLE HELPER (para pantallas Compose)
    // ═══════════════════════════════════════════════

    /**
     * Recuerda el GoogleSignInClient y el launcher listos para usar.
     * Devuelve un objeto que la pantalla puede usar directamente.
     */
    @Composable
    fun recordarLauncherGoogle(
        onExito: (DatosUsuarioGoogle) -> Unit,
        onError: (String) -> Unit
    ): GoogleLoginHelper {
        val context = androidx.compose.ui.platform.LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val cliente = remember { crearCliente(context) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            procesarResultado(
                resultCode = result.resultCode,
                data = result.data,
                alExito = { datos ->
                    coroutineScope.launch { onExito(datos) }
                },
                alError = { mensaje ->
                    coroutineScope.launch { onError(mensaje) }
                }
            )
        }

        return remember {
            GoogleLoginHelper(cliente, launcher)
        }
    }

    /**
     * Helper que encapsula el cliente y launcher listos para usar.
     */
    class GoogleLoginHelper(
        private val cliente: GoogleSignInClient,
        private val launcher: ActivityResultLauncher<Intent>
    ) {
        /**
         * Abre el selector de cuentas de Google (forzando la aparición del selector y evitando login automático silencioso).
         * Llamar desde el onClick del botón "Vincular con Google".
         */
        fun abrirSelector() {
            cliente.signOut().addOnCompleteListener {
                cliente.revokeAccess().addOnCompleteListener {
                    lanzarSelector(cliente, launcher)
                }
            }
        }
    }
}
