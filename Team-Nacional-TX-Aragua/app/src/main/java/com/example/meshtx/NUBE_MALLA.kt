package com.example.meshtx

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SINCRONIZADOR PASIVO CON LA NUBE (PUENTE HÍBRIDO SOS, CACHÉ CARAVANA Y TELEMETRÍA)
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidades:
 * 1. PUENTE DE RESCATE SOS (Store & Forward):
 *    Si un piloto en zona montañosa sin señal emite SOS, la alerta viaja de moto
 *    en moto por la malla. Cuando CUALQUIER piloto del convoy consiga datos celulares
 *    (3G/4G/Wi-Fi), este componente dispara la alerta a Firebase con coordenadas GPS
 *    para avisar a la central, familiares y servicios de auxilio.
 * 2. PRE-REGISTRO Y CACHÉ DE CARAVANA:
 *    Descarga y almacena en memoria los nombres, modelos de moto (ej. TX 200) y avatares
 *    mientras hay señal en el punto de encuentro, para que en plena ruta offline la
 *    pantalla muestre la identidad real de cada motero en lugar de una dirección MAC fría.
 * 3. TELEMETRÍA DIFERIDA:
 *    Acumula métricas de ruta en silencio y las sube a Firebase al recuperar conexión.
 * 4. AISLAMIENTO TOTAL: Cero impacto o dependencia en el flujo de voz offline.
 */
class NubeMalla(
    private val contexto: Context,
    private val idPilotoLocal: Long,
    private val aliasPilotoLocal: String
) {

    private val etiquetaLog = "MeshTX_NubePasiva"
    private val alcanceNube = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Estado observable de disponibilidad de internet
    private val _hayInternet = MutableStateFlow(false)
    val hayInternet: StateFlow<Boolean> = _hayInternet.asStateFlow()

    // Cola de eventos de telemetría y alertas SOS pendientes de envío (Store & Forward)
    private val colaTelemetriaPendiente = ConcurrentLinkedQueue<Map<String, Any>>()
    private val colaAlertasSosPendiente = ConcurrentLinkedQueue<Map<String, Any>>()

    // Caché local de perfiles de la caravana: [IdPiloto -> InfoPilotoCache]
    data class InfoPilotoCache(
        val alias: String,
        val modeloMoto: String = "Keeway TX 200",
        val fotoUrl: String = "",
        val rol: String = "Piloto"
    )
    private val cacheCaravana = ConcurrentHashMap<Long, InfoPilotoCache>()

    // Conexión a Firebase Firestore
    private var baseDatosFirebase: FirebaseFirestore? = null
    private var callbackRed: ConnectivityManager.NetworkCallback? = null

    init {
        inicializarMonitoreoRed()
        inicializarFirebase()
        cargarCacheCaravanaLocal()
    }

    private fun inicializarFirebase() {
        try {
            baseDatosFirebase = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Firebase Firestore no disponible o sin inicializar en este entorno: ${e.message}")
        }
    }

    private fun inicializarMonitoreoRed() {
        try {
            val gestorConectividad = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (gestorConectividad == null) return

            val solicitudRed = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            callbackRed = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _hayInternet.value = true
                    Log.d(etiquetaLog, "📡 Conexión a Internet detectada. Vaciando colas híbridas a Firebase...")
                    procesarAlertasSosPendientes()
                    procesarColaPendiente()
                    sincronizarPerfilesCaravana()
                }

                override fun onLost(network: Network) {
                    _hayInternet.value = false
                    Log.d(etiquetaLog, "Modo 100% Offline (Sin cobertura celular).")
                }
            }

            gestorConectividad.registerNetworkCallback(solicitudRed, callbackRed!!)
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Error al configurar detector de conectividad: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. PUENTE DE RESCATE SOS HÍBRIDO (STORE & FORWARD)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encola o despacha una alerta SOS recibida de la malla offline hacia Firebase.
     */
    fun procesarAlertaSosMalla(
        paqueteSos: PaqueteDatosMesh,
        coordenadasGps: String? = null
    ) {
        val alertaMap = hashMapOf<String, Any>(
            "idEmisorOriginal" to paqueteSos.idEmisor,
            "aliasEmisorOriginal" to paqueteSos.aliasEmisor,
            "mensaje" to (paqueteSos.payloadTexto ?: "Auxilio vial en ruta"),
            "saltosRelay" to paqueteSos.saltosRelay,
            "idNodoReceptorGateway" to idPilotoLocal,
            "aliasNodoReceptorGateway" to aliasPilotoLocal,
            "timestampOriginal" to paqueteSos.timestamp,
            "timestampDespachoGateway" to System.currentTimeMillis(),
            "estado" to "ACTIVA_EN_NUBE"
        )
        if (coordenadasGps != null) {
            alertaMap["coordenadasGps"] = coordenadasGps
        }

        colaAlertasSosPendiente.add(alertaMap)

        // Conexión con el Módulo Avisos / Notificaciones (Regla 71-72 MODULOS_GESTOR.md)
        try {
            val alias = paqueteSos.aliasEmisor
            val canal = paqueteSos.canal
            val texto = paqueteSos.payloadTexto ?: "Auxilio vial en ruta"
            com.example.GestorNotificacionesApp.notificarAvisoMuro(
                titulo = "🚨 SOS Malla TX: $alias",
                contenido = "Alerta de socorro recibida por red offline en canal $canal. $texto",
                referenciaId = paqueteSos.idEmisor.toString()
            )
        } catch (e: Exception) {
            Log.w(etiquetaLog, "No se pudo emitir aviso local de SOS: ${e.message}")
        }

        if (_hayInternet.value) {
            procesarAlertasSosPendientes()
        } else {
            Log.i(etiquetaLog, "🚨 Alerta SOS guardada en cola local (Store & Forward). Se enviará al detectar señal.")
        }
    }

    private fun procesarAlertasSosPendientes() {
        alcanceNube.launch {
            val db = baseDatosFirebase ?: return@launch
            while (_hayInternet.value && colaAlertasSosPendiente.isNotEmpty()) {
                val alerta = colaAlertasSosPendiente.poll() ?: break
                try {
                    db.collection("alertas_sos_mesh")
                        .add(alerta)
                        .addOnSuccessListener {
                            Log.i(etiquetaLog, "🚨 ¡ALERTA SOS DISPARADA A LA NUBE FIREBASE CON ÉXITO! ($alerta)")
                        }
                        .addOnFailureListener {
                            colaAlertasSosPendiente.add(alerta)
                        }
                } catch (e: Exception) {
                    colaAlertasSosPendiente.add(alerta)
                    break
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. PRE-REGISTRO Y CACHÉ DE CARAVANA (IDENTIDADES REALES VS DIRECCIONES MAC)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registrar perfiles de caravana conocidos para resolver alias y motos sin conexión.
     */
    fun registrarPerfilCaravana(
        idPiloto: Long,
        alias: String,
        modeloMoto: String = "TX 200",
        fotoUrl: String = "",
        rol: String = "Piloto"
    ) {
        cacheCaravana[idPiloto] = InfoPilotoCache(alias, modeloMoto, fotoUrl, rol)
    }

    /**
     * Obtener el perfil precargado de un compañero por su ID de nodo.
     */
    fun obtenerPerfilPiloto(idPiloto: Long): InfoPilotoCache? {
        return cacheCaravana[idPiloto]
    }

    private fun cargarCacheCaravanaLocal() {
        // Precargar perfil local propio
        cacheCaravana[idPilotoLocal] = InfoPilotoCache(aliasPilotoLocal, "Keeway TX 200", "", "Piloto")
    }

    private fun sincronizarPerfilesCaravana() {
        if (!_hayInternet.value) return
        alcanceNube.launch {
            try {
                val db = baseDatosFirebase ?: return@launch
                db.collection("members")
                    .limit(100)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        try {
                            for (doc in snapshot.documents) {
                                try {
                                    val numStr = doc.getString("memberNumber") ?: doc.id
                                    val id = numStr.filter { it.isDigit() }.toLongOrNull() ?: doc.id.hashCode().toLong()
                                    val alias = doc.getString("nickname") ?: doc.getString("alias") ?: doc.getString("fullName") ?: "Piloto TX"
                                    val moto = doc.getString("motorcycleModel") ?: doc.getString("bikeModel") ?: "TX 200"
                                    val foto = doc.getString("photoUrl") ?: ""
                                    val rol = doc.getString("role") ?: "Piloto"
                                    cacheCaravana[id] = InfoPilotoCache(alias, moto, foto, rol)
                                } catch (docError: Exception) {
                                    Log.w(etiquetaLog, "Error parseando perfil individual: ${docError.message}")
                                }
                            }
                            Log.d(etiquetaLog, "Caché de caravana precargada: ${cacheCaravana.size} pilotos registrados.")
                        } catch (listenerError: Exception) {
                            Log.w(etiquetaLog, "Error procesando snapshot de caravana: ${listenerError.message}")
                        }
                    }
                    .addOnFailureListener { err ->
                        Log.w(etiquetaLog, "Fallo al consultar perfiles en Firebase: ${err.message}")
                    }
            } catch (e: Exception) {
                Log.w(etiquetaLog, "Error al sincronizar perfiles de caravana: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TELEMETRÍA DIFERIDA Y RESPALDO
    // ─────────────────────────────────────────────────────────────────────────

    fun registrarEventoTelemetria(
        canalUsado: Int,
        totalVecinosAlcanzados: Int,
        huboAlertaSos: Boolean = false,
        coordenadasGps: String? = null
    ) {
        val mapaDatos = hashMapOf<String, Any>(
            "idPiloto" to idPilotoLocal,
            "aliasPiloto" to aliasPilotoLocal,
            "canalUsado" to canalUsado,
            "totalVecinos" to totalVecinosAlcanzados,
            "alertaSos" to huboAlertaSos,
            "timestamp" to System.currentTimeMillis()
        )
        if (coordenadasGps != null) {
            mapaDatos["coordenadas"] = coordenadasGps
        }

        colaTelemetriaPendiente.add(mapaDatos)

        if (_hayInternet.value) {
            procesarColaPendiente()
        }
    }

    fun respaldarAjustesUsuario(ajustes: AjustesIntercomunicadorTactico) {
        if (!_hayInternet.value) return

        alcanceNube.launch {
            try {
                val db = baseDatosFirebase ?: return@launch
                val datosAjustes = hashMapOf(
                    "modoPtt" to ajustes.modoPtt,
                    "sensibilidadVox" to ajustes.umbralVoxSensibilidad,
                    "cancelacionViento" to ajustes.cancelacionRuidoViento,
                    "supresionEco" to ajustes.supresionEcoAcustico,
                    "canalFavorito" to ajustes.canalActivo.name,
                    "ultimaActualizacion" to System.currentTimeMillis()
                )

                db.collection("meshtx_ajustes_piloto")
                    .document(idPilotoLocal.toString())
                    .set(datosAjustes, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(etiquetaLog, "Ajustes de Mesh TX respaldados en la nube con éxito.")
                    }
            } catch (e: Exception) {
                Log.w(etiquetaLog, "No se pudo sincronizar ajustes con Firebase: ${e.message}")
            }
        }
    }

    private fun procesarColaPendiente() {
        alcanceNube.launch {
            val db = baseDatosFirebase ?: return@launch

            while (_hayInternet.value && colaTelemetriaPendiente.isNotEmpty()) {
                val elemento = colaTelemetriaPendiente.poll() ?: break
                try {
                    db.collection("meshtx_telemetria_convoy")
                        .add(elemento)
                        .addOnFailureListener {
                            colaTelemetriaPendiente.add(elemento)
                        }
                } catch (e: Exception) {
                    colaTelemetriaPendiente.add(elemento)
                    break
                }
            }
        }
    }

    fun liberar() {
        try {
            val gestorConectividad = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (callbackRed != null && gestorConectividad != null) {
                gestorConectividad.unregisterNetworkCallback(callbackRed!!)
            }
        } catch (_: Exception) {}
        callbackRed = null
    }
}
