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
    private var idPilotoLocal: Long,
    private var aliasPilotoLocal: String,
    private var modeloMotoLocal: String = "Keeway TX 200",
    private var fichaLocal: String = "",
    private var fotoUrlLocal: String = "",
    private val alRecibirAudioDesdePuente: (ByteArray, Long, String, Int) -> Unit,
    private val alDetectarPilotoNube: (NodoMeshPiloto) -> Unit = {},
    private val alPerderPilotoNube: (Long) -> Unit = {}
) {

    private val etiquetaLog = "MeshTX_PuenteCelular"
    private val alcancePuente = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var firestore: FirebaseFirestore? = null
    private var registroListenerCanal: ListenerRegistration? = null
    private var registroListenerPresencia: ListenerRegistration? = null
    private var canalActualId: Int = 1
    private var nombreCanalActual: String = "General TX"
    private var salaPrivadaActiva: String? = null
    private var tareaPresencia: Job? = null

    var ayudaConDatosHabilitada: Boolean = false
        private set

    var estaMallaActiva: Boolean = false
        private set

    private var contadorSecuencia = 0L
    private var ultimoTimestampProcesado = 0L

    // Búfer de ráfaga para agrupar 3 fragmentos (~120ms) y no sobrecargar el límite de 1 write/seg de Firestore
    private val bufferRafaga = mutableListOf<ByteArray>()
    private var tareaFlushRafaga: Job? = null

    fun actualizarIdentidad(id: Long, alias: String) {
        this.idPilotoLocal = id
        if (alias.isNotBlank()) this.aliasPilotoLocal = alias
    }

    fun actualizarPerfil(modeloMoto: String, ficha: String, fotoUrl: String) {
        if (modeloMoto.isNotBlank()) this.modeloMotoLocal = modeloMoto
        if (ficha.isNotBlank()) this.fichaLocal = ficha
        this.fotoUrlLocal = fotoUrl
    }

    fun actualizarSalaPrivada(sala: String?) {
        this.salaPrivadaActiva = sala
    }

    fun actualizarNombreCanal(nombre: String) {
        this.nombreCanalActual = nombre
    }

    fun setMallaActiva(activa: Boolean) {
        this.estaMallaActiva = activa
        if (activa && ayudaConDatosHabilitada) {
            conectarPuente()
        } else {
            desconectarPuente()
        }
    }

    fun activarPuenteVoip(canalId: Int) {
        ayudaConDatosHabilitada = true
        canalActualId = canalId
        if (estaMallaActiva) {
            conectarPuente()
        }
    }

    fun desactivarPuenteVoip() {
        ayudaConDatosHabilitada = false
        desconectarPuente()
    }

    fun sintonizarCanal(nuevoCanalId: Int) {
        if (canalActualId != nuevoCanalId) {
            canalActualId = nuevoCanalId
            if (ayudaConDatosHabilitada && estaMallaActiva) {
                conectarPuente()
            }
        }
    }

    /**
     * Publica fragmentos de voz en ráfagas estructuradas hacia Firestore para enlace VoIP
     * sin exceder los límites de escritura por segundo de Firebase.
     */
    fun retransmitirAudioPorPuente(
        canal: Int,
        payloadAudio: ByteArray
    ) {
        if (!ayudaConDatosHabilitada || !estaMallaActiva || !tieneConexionInternet()) return

        synchronized(bufferRafaga) {
            bufferRafaga.add(payloadAudio)
            if (bufferRafaga.size >= 3) {
                val fragmentosAEnviar = bufferRafaga.toList()
                bufferRafaga.clear()
                tareaFlushRafaga?.cancel()
                despacharRafagaAFirebase(canal, fragmentosAEnviar)
            } else if (tareaFlushRafaga == null || tareaFlushRafaga?.isCompleted == true) {
                tareaFlushRafaga = alcancePuente.launch {
                    delay(120) // Enviar lo acumulado tras 120ms
                    val fragmentosPendientes = synchronized(bufferRafaga) {
                        val copia = bufferRafaga.toList()
                        bufferRafaga.clear()
                        copia
                    }
                    if (fragmentosPendientes.isNotEmpty()) {
                        despacharRafagaAFirebase(canal, fragmentosPendientes)
                    }
                }
            }
        }
    }

    private fun despacharRafagaAFirebase(canal: Int, fragmentos: List<ByteArray>) {
        alcancePuente.launch {
            try {
                val db = firestore ?: FirebaseFirestore.getInstance().also { firestore = it }
                // Concatenar fragmentos con delimitador de tamaño: [Short longitud][Bytes]
                val stream = java.io.ByteArrayOutputStream()
                val dos = java.io.DataOutputStream(stream)
                dos.writeInt(fragmentos.size)
                for (frag in fragmentos) {
                    dos.writeInt(frag.size)
                    dos.write(frag)
                }
                dos.flush()
                val rafagaBytes = stream.toByteArray()
                val base64 = Base64.encodeToString(rafagaBytes, Base64.NO_WRAP)

                val payload = mapOf(
                    "emisorId" to idPilotoLocal,
                    "emisorAlias" to aliasPilotoLocal,
                    "canalId" to canal,
                    "secuencia" to ++contadorSecuencia,
                    "timestamp" to System.currentTimeMillis(),
                    "audioRafagaBase64" to base64
                )

                db.collection("mesh_tx_puente")
                    .document("canal_$canal")
                    .set(payload, SetOptions.merge())
            } catch (e: Exception) {
                Log.w(etiquetaLog, "Error transmitiendo ráfaga por Puente Celular: ${e.message}")
            }
        }
    }

    private fun conectarPuente() {
        desconectarPuente()
        if (!ayudaConDatosHabilitada || !estaMallaActiva || !tieneConexionInternet()) return

        try {
            val db = firestore ?: FirebaseFirestore.getInstance().also { firestore = it }
            val docRef = db.collection("mesh_tx_puente").document("canal_$canalActualId")

            registroListenerCanal = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(etiquetaLog, "Error en listener de Puente Celular: ${error.message}")
                    return@addSnapshotListener
                }

                // Si la malla no está activa, no procesar
                if (!estaMallaActiva) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val emisorId = snapshot.getLong("emisorId") ?: return@addSnapshotListener
                        if (emisorId == idPilotoLocal) return@addSnapshotListener // Evitar eco propio absoluto

                        val canalRemoto = snapshot.getLong("canalId")?.toInt() ?: canalActualId
                        if (canalRemoto != canalActualId) return@addSnapshotListener // Aislamiento estricto de canal

                        val emisorAlias = snapshot.getString("emisorAlias") ?: "Piloto Celular"
                        val timestamp = snapshot.getLong("timestamp") ?: 0L

                        // Descartar paquetes de más de 3 segundos de antigüedad o ya procesados
                        val ahora = System.currentTimeMillis()
                        if (ahora - timestamp > 3500L || timestamp <= ultimoTimestampProcesado) return@addSnapshotListener
                        ultimoTimestampProcesado = timestamp

                        // Procesar ráfaga concatenada
                        val rafagaB64 = snapshot.getString("audioRafagaBase64")
                        if (!rafagaB64.isNullOrBlank()) {
                            val rafagaBytes = Base64.decode(rafagaB64, Base64.NO_WRAP)
                            val dis = java.io.DataInputStream(java.io.ByteArrayInputStream(rafagaBytes))
                            val totalFrags = dis.readInt()
                            for (f in 0 until totalFrags) {
                                val tam = dis.readInt()
                                if (tam in 1..4096) {
                                    val fragBytes = ByteArray(tam)
                                    dis.readFully(fragBytes)
                                    alRecibirAudioDesdePuente(fragBytes, emisorId, emisorAlias, canalActualId)
                                }
                            }
                        } else {
                            // Compatibilidad con formato simple
                            val audioB64 = snapshot.getString("audioBase64") ?: return@addSnapshotListener
                            val audioBytes = Base64.decode(audioB64, Base64.NO_WRAP)
                            if (audioBytes.isNotEmpty()) {
                                alRecibirAudioDesdePuente(audioBytes, emisorId, emisorAlias, canalActualId)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(etiquetaLog, "Fallo al procesar audio de Puente Celular: ${e.message}")
                    }
                }
            }

            // 🌐 2. Publicar presencia periódica (Heartbeat) en la sala/canal de Firebase
            val docIdPresencia = "canal_${canalActualId}_${idPilotoLocal}"
            tareaPresencia?.cancel()
            tareaPresencia = alcancePuente.launch {
                while (isActive && estaMallaActiva && ayudaConDatosHabilitada) {
                    try {
                        val payload = mapOf(
                            "idPiloto" to idPilotoLocal,
                            "aliasPiloto" to aliasPilotoLocal,
                            "modeloMoto" to modeloMotoLocal,
                            "ficha" to fichaLocal,
                            "fotoUrl" to fotoUrlLocal,
                            "canalId" to canalActualId,
                            "nombreCanal" to nombreCanalActual,
                            "salaPrivada" to (salaPrivadaActiva ?: ""),
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("mesh_tx_presencia").document(docIdPresencia).set(payload, SetOptions.merge())
                    } catch (e: Exception) {
                        Log.w(etiquetaLog, "Error emitiendo presencia en Firebase: ${e.message}")
                    }
                    delay(20_000)
                }
            }

            // 🌐 3. Escuchar presencia de otros pilotos en la misma sala / canal de Firebase (Fuera de rango offline)
            registroListenerPresencia?.remove()
            registroListenerPresencia = db.collection("mesh_tx_presencia")
                .whereEqualTo("canalId", canalActualId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !estaMallaActiva) return@addSnapshotListener
                    val ahora = System.currentTimeMillis()
                    for (doc in snapshot.documents) {
                        try {
                            val idPiloto = doc.getLong("idPiloto") ?: continue
                            if (idPiloto == idPilotoLocal) continue

                            val ts = doc.getLong("timestamp") ?: 0L
                            if (ahora - ts > 60_000L) {
                                alPerderPilotoNube(idPiloto)
                                continue
                            }

                            val alias = doc.getString("aliasPiloto") ?: "Piloto Nube"
                            val moto = doc.getString("modeloMoto") ?: "Keeway TX 200"
                            val ficha = doc.getString("ficha") ?: ""
                            val foto = doc.getString("fotoUrl") ?: ""
                            val nombreCanal = doc.getString("nombreCanal") ?: "Canal $canalActualId"
                            val sala = doc.getString("salaPrivada")?.ifBlank { null }

                            val nodo = NodoMeshPiloto(
                                idMiembro = idPiloto,
                                aliasPiloto = alias,
                                nombreMoto = moto,
                                fotoUrl = foto,
                                fichaMiembro = ficha,
                                modeloTelefonoHardware = "Enlace Firebase (4G/Wi-Fi)",
                                direccionNodo = "Nube / Firebase",
                                intensidadSenalDbm = -50,
                                estaTransmitiendoVoz = false,
                                distanciaAproximadaMetros = 0.0,
                                ultimoPingTimestamp = ts,
                                idCanalActual = canalActualId,
                                nombreCanalActual = nombreCanal,
                                salaPrivada = sala
                            )
                            alDetectarPilotoNube(nodo)
                        } catch (e: Exception) {
                            Log.w(etiquetaLog, "Error procesando presencia de nodo nube: ${e.message}")
                        }
                    }
                }

            Log.i(etiquetaLog, "🌐 Puente Celular Firebase Firestore CONECTADO en Canal $canalActualId (Audio + Presencia en Vivo).")
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Error conectando Puente Celular: ${e.message}")
        }
    }

    private fun desconectarPuente() {
        try {
            registroListenerCanal?.remove()
            registroListenerCanal = null
            registroListenerPresencia?.remove()
            registroListenerPresencia = null
        } catch (_: Exception) {}

        tareaPresencia?.cancel()
        tareaPresencia = null

        // Eliminar rastro de presencia en Firebase de forma asíncrona
        val idAEliminar = idPilotoLocal
        val canalAEliminar = canalActualId
        alcancePuente.launch {
            try {
                val db = firestore ?: FirebaseFirestore.getInstance()
                db.collection("mesh_tx_presencia").document("canal_${canalAEliminar}_${idAEliminar}").delete()
            } catch (_: Exception) {}
        }

        synchronized(bufferRafaga) {
            bufferRafaga.clear()
        }
        tareaFlushRafaga?.cancel()
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
