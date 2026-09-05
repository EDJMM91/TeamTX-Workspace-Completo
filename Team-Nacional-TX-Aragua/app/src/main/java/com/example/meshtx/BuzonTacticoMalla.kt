package com.example.meshtx

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BUZÓN TÁCTICO ASÍNCRONO (STORE & FORWARD HÍBRIDO CON FIREBASE & OFFLINE)
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidad:
 * Garantizar que ningún mensaje crítico (desvío, aviso, auxilio) se pierda
 * si el piloto habla cuando nadie está en el rango de radio local.
 *
 * Funcionamiento:
 * 1. Si se transmite audio con 0 compañeros en rango, se encola en el buzón.
 * 2. Vía Celular / Firebase (Si "Ayuda con Datos" está activa y hay 4G):
 *    - Despacha los paquetes a Firebase Firestore (/mesh_tx_buzon/)
 *      para que los compañeros con internet lo descarguen al instante.
 * 3. Vía Malla Offline (Si no hay internet):
 *    - El audio queda "cargado en el cañón" en disco/memoria.
 *    - En cuanto el escáner BLE / Wi-Fi Direct detecta que una moto entra en rango,
 *      dispara inmediatamente los paquetes de voz acumulados por UDP a máxima tasa.
 */
class BuzonTacticoMalla(
    private val contexto: Context,
    private val idPilotoLocal: Long,
    private val aliasPilotoLocal: String,
    private val alDescargarMensajeVozRemoto: ((ByteArray, Long, String) -> Unit)? = null
) {

    private val etiquetaLog = "MeshTX_BuzonTactico"
    private val alcanceBuzon = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class MensajeEncolado(
        val idMensaje: String,
        val timestamp: Long,
        val canal: Int,
        val fragmentosAudio: List<ByteArray>,
        val remitenteAlias: String
    )

    private val colaMensajesLocales = ConcurrentLinkedQueue<MensajeEncolado>()

    /**
     * Encola una transmisión de voz emitida cuando no había compañeros en rango directo.
     */
    fun almacenarMensajeVoz(
        canal: Int,
        fragmentosAudio: List<ByteArray>,
        ayudaConDatosActiva: Boolean
    ) {
        if (fragmentosAudio.isEmpty()) return

        val idMsg = "BUZON_${System.currentTimeMillis()}_${idPilotoLocal}"
        val mensaje = MensajeEncolado(
            idMensaje = idMsg,
            timestamp = System.currentTimeMillis(),
            canal = canal,
            fragmentosAudio = fragmentosAudio,
            remitenteAlias = aliasPilotoLocal
        )

        colaMensajesLocales.add(mensaje)
        Log.i(etiquetaLog, "📥 Mensaje de voz guardado en Buzón Táctico ($idMsg, ${fragmentosAudio.size} fragmentos).")

        alcanceBuzon.launch {
            if (ayudaConDatosActiva && tieneConexionInternet()) {
                subirMensajeAFirebase(mensaje)
            }
        }
    }

    /**
     * Se ejecuta cuando el escáner BLE / Wi-Fi Direct detecta que un compañero entró en rango.
     * Dispara inmediatamente los audios pendientes cargados en el cañón.
     */
    fun notificarNuevoCompaneroEnRango(alEnviarPaquete: (PaqueteDatosMesh) -> Unit) {
        if (colaMensajesLocales.isEmpty()) return

        Log.i(etiquetaLog, "⚡ Nuevo compañero en rango. Disparando mensajes pendientes del buzón...")

        alcanceBuzon.launch {
            while (colaMensajesLocales.isNotEmpty()) {
                val mensaje = colaMensajesLocales.poll() ?: break
                for (fragmento in mensaje.fragmentosAudio) {
                    val paquete = PaqueteDatosMesh(
                        idEmisor = idPilotoLocal,
                        aliasEmisor = aliasPilotoLocal,
                        canal = mensaje.canal,
                        tipo = TipoPaqueteMesh.AUDIO_VOZ_OPUS,
                        payloadAudio = fragmento
                    )
                    alEnviarPaquete(paquete)
                    delay(15) // Ritmo controlado para no saturar el buffer UDP receptor
                }
                Log.d(etiquetaLog, "🚀 Mensaje ${mensaje.idMensaje} entregado vía radio offline.")
            }
        }
    }

    private fun subirMensajeAFirebase(mensaje: MensajeEncolado) {
        try {
            val db = FirebaseFirestore.getInstance()
            val totalBytes = mensaje.fragmentosAudio.sumOf { it.size }
            val audioCompleto = ByteArray(totalBytes)
            var offset = 0
            for (frag in mensaje.fragmentosAudio) {
                System.arraycopy(frag, 0, audioCompleto, offset, frag.size)
                offset += frag.size
            }

            val payloadBase64 = Base64.encodeToString(audioCompleto, Base64.NO_WRAP)

            val datos = mapOf(
                "id" to mensaje.idMensaje,
                "timestamp" to mensaje.timestamp,
                "emisorId" to idPilotoLocal,
                "emisorAlias" to aliasPilotoLocal,
                "canal" to mensaje.canal,
                "audioBase64" to payloadBase64
            )

            db.collection("mesh_tx_buzon")
                .document(mensaje.idMensaje)
                .set(datos, SetOptions.merge())
                .addOnSuccessListener {
                    Log.i(etiquetaLog, "☁️ Mensaje del buzón sincronizado con Firebase con éxito (${audioCompleto.size} B).")
                }.addOnFailureListener { e ->
                    Log.w(etiquetaLog, "Fallo al subir mensaje a Firebase: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Error operando Firebase en Buzón Táctico: ${e.message}")
        }
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
        alcanceBuzon.cancel()
        colaMensajesLocales.clear()
    }
}
