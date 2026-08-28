package com.aistudio.teamtxvzla.nube

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Estados posibles de la autenticación Firebase.
 */
enum class EstadoAuth {
    NO_AUTENTICADO,
    AUTENTICANDO,
    AUTENTICADO,
    ERROR
}

/**
 * Módulo de Autenticación Centralizado para Team TX.
 *
 * ARQUITECTURA:
 * - Expone un StateFlow<EstadoAuth> que cualquier módulo puede observar.
 * - Garantiza sesión anónima al iniciar (respaldo para Firestore/Storage).
 * - La sesión de Google sobreescribe la anónima cuando completa.
 * - Los módulos que dependan de Firestore DEBEN esperar EstadoAuth.AUTENTICADO
 *   antes de adjuntar listeners o escribir documentos.
 */
object AutenticacionNube {

    private const val ETIQUETA = "AUTENTICACION_NUBE"
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // ═══════════════════════════════════════════════
    // ESTADO REACTIVO DE AUTENTICACIÓN
    // ═══════════════════════════════════════════════

    private val _estado = MutableStateFlow(EstadoAuth.NO_AUTENTICADO)
    val estado: StateFlow<EstadoAuth> = _estado.asStateFlow()

    private val _usuario = MutableStateFlow<FirebaseUser?>(null)
    val usuario: StateFlow<FirebaseUser?> = _usuario.asStateFlow()

    /**
     * Evento que se emite UNA VEZ cuando la autenticación se completa exitosamente.
     * Los módulos deben hacer .first{} en este flow para bloquearse hasta que auth esté lista.
     */
    private val authLista = CompletableDeferred<Unit>()

    /**
     * Flow compartido que notifica cada cambio de estado.
     * Útil para escuchar múltiples veces (ej: reconnect listeners).
     */
    private val _eventosAuth = MutableSharedFlow<EstadoAuth>(extraBufferCapacity = 5)
    val eventosAuth: SharedFlow<EstadoAuth> = _eventosAuth.asSharedFlow()

    // ═══════════════════════════════════════════════
    // INICIALIZACIÓN: Sesión anónima obligatoria
    // ═══════════════════════════════════════════════

    /**
     * DEBE llamarse una sola vez al arrancar la app (Application o ViewModel init).
     * Intentarestore sesión anterior → si no hay, crea sesión anónima → emite AUTENTICADO.
     *
     * FLUJO:
     * 1. Si ya hay un usuario logueado (Google o anónimo previo), lo reutiliza.
     * 2. Si no hay, ejecuta signInAnonymously() y espera resultado.
     * 3. Si falla, reintent una vez más. Si falla de nuevo → EstadoAuth.ERROR.
     * 4. Si todo ok → emite AUTENTICADO y completa authLista.
     */
    suspend fun inicializar() {
        if (_estado.value == EstadoAuth.AUTENTICADO) {
            Log.i(ETIQUETA, "✅ Auth ya inicializada, sesión activa: ${auth.currentUser?.uid}")
            return
        }

        _estado.value = EstadoAuth.AUTENTICANDO
        _eventosAuth.emit(EstadoAuth.AUTENTICANDO)

        // Caso 1: Ya hay un usuario (restaurado de sesión anterior)
        val usuarioExistente = auth.currentUser
        if (usuarioExistente != null) {
            Log.i(ETIQUETA, "✅ Sesión restaurada: UID=${usuarioExistente.uid}")
            confirmarAutenticacion(usuarioExistente)
            return
        }

        // Caso 2: No hay sesión → crear anónima
        Log.i(ETIQUETA, "🔄 No hay sesión activa, intentando sesión anónima...")
        val exito = intentarSesionAnonima()

        if (exito) {
            confirmarAutenticacion(auth.currentUser!!)
        } else {
            // Intento fallido: reportar error
            Log.e(ETIQUETA, "❌ FALLO: No se pudo establecer ninguna sesión de Firebase Auth.")
            Log.e(ETIQUETA, "   Verificar en Firebase Console → Authentication → Sign-in method → Anonymous = HABILITADO")
            _estado.value = EstadoAuth.ERROR
            _eventosAuth.emit(EstadoAuth.ERROR)
        }
    }

    /**
     * Intenta crear sesión anónima. Devuelve true si fue exitosa.
     */
    private suspend fun intentarSesionAnonima(): Boolean {
        return try {
            val resultado = auth.signInAnonymously().await()
            val usuario = resultado.user
            if (usuario != null) {
                Log.i(ETIQUETA, "✅ Sesión anónima creada: UID=${usuario.uid}")
                true
            } else {
                Log.e(ETIQUETA, "❌ signInAnonymously devolvió null")
                false
            }
        } catch (e: Exception) {
            val codigo = (e as? FirebaseAuthException)?.errorCode ?: "DESCONOCIDO"
            Log.e(ETIQUETA, "❌ Error en signInAnonymously: $codigo - ${e.message}")

            when (codigo) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    Log.e(ETIQUETA, "   → SOLUCIÓN: Firebase Console → Authentication → Sign-in method → Habilitar 'Anonymous'")
                "ERROR_NETWORK_REQUEST_FAILED" ->
                    Log.e(ETIQUETA, "   → Sin conexión a internet. Reintentará al recuperar red.")
                else ->
                    Log.e(ETIQUETA, "   → Error inesperado: ${e.message}")
            }
            false
        }
    }

    /**
     * Confirma que la autenticación fue exitosa y actualiza todos los flujos.
     */
    private suspend fun confirmarAutenticacion(usuario: FirebaseUser) {
        _usuario.value = usuario
        _estado.value = EstadoAuth.AUTENTICADO
        _eventosAuth.emit(EstadoAuth.AUTENTICADO)
        if (!authLista.isCompleted) {
            authLista.complete(Unit)
        }
        Log.i(ETIQUETA, "═══════════════════════════════════════")
        Log.i(ETIQUETA, "  AUTH CONFIRMADA - UID: ${usuario.uid}")
        Log.i(ETIQUETA, "  Todos los módulos pueden usar Firestore")
        Log.i(ETIQUETA, "═══════════════════════════════════════")
    }

    // ═══════════════════════════════════════════════
    // ESPERAR HASTA QUE AUTH ESTÉ LISTA
    // ═══════════════════════════════════════════════

    /**
     * Bloquea al coroutine caller hasta que la autenticación esté completa o haya sesión.
     * Si ya hay usuario en Firebase, completa y avanza de inmediato sin trabas.
     */
    suspend fun esperarAuthLista() {
        if (_estado.value == EstadoAuth.AUTENTICADO) return
        val usuarioActual = auth.currentUser
        if (usuarioActual != null) {
            confirmarAutenticacion(usuarioActual)
            return
        }
        try {
            kotlinx.coroutines.withTimeoutOrNull(8000L) {
                authLista.await()
            }
        } catch (_: Exception) {}
    }

    /**
     * Garantiza que exista al menos una sesión de Firebase Auth activa (Google o Anónima).
     */
    suspend fun garantizarSesionActiva(): Boolean {
        if (auth.currentUser != null) {
            _usuario.value = auth.currentUser
            _estado.value = EstadoAuth.AUTENTICADO
            if (!authLista.isCompleted) authLista.complete(Unit)
            return true
        }
        return intentarSesionAnonima().also { exito ->
            if (exito && auth.currentUser != null) {
                confirmarAutenticacion(auth.currentUser!!)
            }
        }
    }

    // ═══════════════════════════════════════════════
    // LOGIN CON GOOGLE (sobreescribe sesión anónima)
    // ═══════════════════════════════════════════════

    /**
     * Inicia sesión con Google. La sesión anónima previa se descarta automáticamente
     * cuando Firebase recibe la credencial de Google.
     */
    fun iniciarSesionConGoogle(tokenId: String, alTerminar: (FirebaseUser?) -> Unit) {
        val credencial = GoogleAuthProvider.getCredential(tokenId, null)
        Log.i(ETIQUETA, "Intentando vincular credencial de Google con Firebase...")

        auth.signInWithCredential(credencial)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user
                if (usuario != null) {
                    Log.i(ETIQUETA, "✅ Login Google exitoso. UID: ${usuario.uid}")
                    _usuario.value = usuario
                    _estado.value = EstadoAuth.AUTENTICADO
                    if (!authLista.isCompleted) {
                        authLista.complete(Unit)
                    }
                    alTerminar(usuario)
                } else {
                    Log.e(ETIQUETA, "❌ Google login: usuario null")
                    alTerminar(null)
                }
            }
            .addOnFailureListener { error ->
                Log.e(ETIQUETA, "❌ FALLO en signInWithCredential", error)
                Log.e(ETIQUETA, "🔎 DIAGNÓSTICO → ${diagnosticarError(error)}")
                alTerminar(null)
            }
    }

    /**
     * Traduce el código técnico de Firebase a una causa accionable para Logcat.
     */
    fun diagnosticarError(error: Throwable): String {
        val codigo = (error as? FirebaseAuthException)?.errorCode ?: ""
        return when {
            codigo == "ERROR_INVALID_CREDENTIAL" || codigo == "ERROR_INVALID_ID_TOKEN" ->
                "Token de Google rechazado por Firebase. SHA-1 no coincide o token expirado."
            codigo == "ERROR_OPERATION_NOT_ALLOWED" ->
                "El proveedor está deshabilitado en Firebase Console → Authentication → Sign-in method."
            codigo == "ERROR_NETWORK_REQUEST_FAILED" ->
                "Sin conexión estable con los servidores de Google/Firebase."
            codigo == "ERROR_USER_DISABLED" ->
                "La cuenta fue deshabilitada en Firebase Console → Authentication → Users."
            codigo == "ERROR_TOO_MANY_REQUESTS" ->
                "Demasiados intentos desde este dispositivo. Espera unos minutos."
            else -> "Código=$codigo Mensaje=${error.message}"
        }
    }

    // ═══════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════

    fun obtenerUsuarioActual(): FirebaseUser? = auth.currentUser

    fun estaAutenticado(): Boolean = _estado.value == EstadoAuth.AUTENTICADO

    fun cerrarSesionNube() {
        Log.i(ETIQUETA, "Cerrando sesión en Firebase Authentication.")
        auth.signOut()
        _usuario.value = null
        _estado.value = EstadoAuth.NO_AUTENTICADO
    }
}
