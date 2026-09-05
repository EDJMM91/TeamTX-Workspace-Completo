package com.example.meshtx

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PUENTE DE MALLA CELULAR (VOIP FALLBACK MEDIANTE FIREBASE FIRESTORE SYNC)
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidad:
 * Interconectar sub-grupos de la caravana separados por kilómetros donde el
 * radio Wi-Fi Direct / UDP local no alcanza (ej. Grupo Puntero vs Barredora).
 *
 * Funcionamiento:
 * 1. Se activa únicamente si el usuario activa "Ayuda con Datos" en Ajustes.
 * 2. Si este nodo cuenta con datos móviles (LTE/4G) o Wi-Fi:
 *    - Al transmitir voz, retransmite el fragmento hacia Firestore (`mesh_tx_puente/canal_X`).
 *    - Escucha en tiempo real (SnapshotListener) los paquetes que otros puentes
 *      suben y los inyecta al audio local para que la caravana los escuche.
 */
class PuenteCelularMalla(
    private val contexto: Context,
    private val idPilotoLocal: Long,
    private val aliasPilotoLocal: String,
    private val alRecibirAudioDesdePuente: (ByteArray, Long, String, Int) -> Unit
) {

    private val etiquetaLog = "MeshTX_PuenteCelular"
    private val alcancePuente = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var firestore: FirebaseFirestore? = null
    private var registroListenerCanal: ListenerRegistration? = null
    private var canalActualId: Int = 1

    var ayudaConDatosHabilitada: Boolean = false
        private set

    fun activarPuenteVoip(canalId: Int) {
        ayudaConDatosHabilitada = true
        canalActualId = canalId
        conectarPuente()
    }

    fun desactivarPuenteVoip() {
        ayudaConDatosHabilitada = false
        desconectarPuente()
    }

    fun sintonizarCanal(nuevoCanalId: Int) {
        if (canalActualId != nuevoCanalId) {
            canalActualId = nuevoCanalId
            if (ayudaConDatosHabilitada) {
                conectarPuente()
            }
        }
    }

    /**
     * Publica un fragmento de voz local en el canal de Firestore para enlace VoIP.
     */
    fun retransmitirAudioPorPuente(
        canal: Int,
        payloadAudio: ByteArray
    ) {
        if (!ayudaConDatosHabilitada || !tieneConexionInternet()) return

        alcancePuente.launch {
            try {
                val db = firestore ?: FirebaseFirestore.getInstance().also { firestore = it }
                val base64 = Base64.encodeToString(payloadAudio, Base64.NO_WRAP)

                val payload = mapOf(
                    "emisorId" to idPilotoLocal,
                    "emisorAlias" to aliasPilotoLocal,
                    "timestamp" to System.currentTimeMillis(),
                    "audioBase64" to base64
                )

                db.collection("mesh_tx_puente")
                    .document("canal_$canal")
                    .set(payload, SetOptions.merge())
            } catch (e: Exception) {
                Log.w(etiquetaLog, "Error transmitiendo por Puente Celular: ${e.message}")
            }
        }
    }

    private fun conectarPuente() {
        desconectarPuente()
        if (!ayudaConDatosHabilitada || !tieneConexionInternet()) return

        try {
            val db = firestore ?: FirebaseFirestore.getInstance().also { firestore = it }
            val docRef = db.collection("mesh_tx_puente").document("canal_$canalActualId")

            registroListenerCanal = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(etiquetaLog, "Error en listener de Puente Celular: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val emisorId = snapshot.getLong("emisorId") ?: return@addSnapshotListener
                        if (emisorId == idPilotoLocal) return@addSnapshotListener // Evitar eco propio

                        val emisorAlias = snapshot.getString("emisorAlias") ?: "Piloto Celular"
                        val timestamp = snapshot.getLong("timestamp") ?: 0L

                        // Descartar paquetes de más de 3 segundos de antigüedad
                        if (System.currentTimeMillis() - timestamp > 3000L) return@addSnapshotListener

                        val audioB64 = snapshot.getString("audioBase64") ?: return@addSnapshotListener
                        val audioBytes = Base64.decode(audioB64, Base64.NO_WRAP)

                        if (audioBytes.isNotEmpty()) {
                            alRecibirAudioDesdePuente(audioBytes, emisorId, emisorAlias, canalActualId)
                        }
                    } catch (e: Exception) {
                        Log.w(etiquetaLog, "Fallo al procesar audio de Puente Celular: ${e.message}")
                    }
                }
            }

            Log.i(etiquetaLog, "🌐 Puente Celular Firebase Firestore CONECTADO en Canal $canalActualId.")
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Error conectando Puente Celular: ${e.message}")
        }
    }

    private fun desconectarPuente() {
        try {
            registroListenerCanal?.remove()
            registroListenerCanal = null
        } catch (_: Exception) {}
    }

    private fun tieneConexionInternet(): Boolean {
        return try {
            val cm = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val red = cm.activeNetwork ?: return false
            val capacidades = cm.getNetworkCapabilities(red) ?: return false
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun liberar() {
        desconectarPuente()
        alcancePuente.cancel()
    }
}
