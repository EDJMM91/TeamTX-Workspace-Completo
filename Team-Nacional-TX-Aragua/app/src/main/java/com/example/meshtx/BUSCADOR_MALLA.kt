package com.example.meshtx

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.aware.*
import android.net.wifi.p2p.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BUSCADOR DE MALLA TÁCTICA (WIFI DIRECT P2P + WIFI AWARE / NAN + BLUETOOTH LE)
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidades:
 * 1. Anunciar pasivamente la presencia del piloto mediante balizas BLE (Beacons)
 *    en formatos duales: binario compacto little-endian y compatibilidad UTF-8.
 * 2. Escanear el entorno por BLE con control de cadencia (ScanRateLimiter) para
 *    evitar penalizaciones de throttling de batería en Android.
 * 3. Descubrir y conectar pares mediante Wi-Fi Direct (P2P) autónomo sin requerir router.
 * 4. Descubrir y publicar servicios Wi-Fi Aware (NAN) para enlaces de alta velocidad.
 * 5. Auditoría periódica y purga automática de nodos fuera de rango.
 *
 * 100% OFFLINE - Cero dependencias de servidores externos o internet.
 */
class BuscadorMalla(
    private val contexto: Context,
    private val idPilotoLocal: Long,
    private val aliasPilotoLocal: String,
    private val alDetectarNodo: (NodoMeshPiloto) -> Unit,
    private val alPerderNodo: (Long) -> Unit
) {

    private val etiquetaLog = "MeshTX_Buscador"
    private val uuidServicioMesh = UUID.fromString("0000fe50-0000-1000-8000-00805f9b34fb")
    private val idFabricanteTactico = 0x07AF // Identificador de fabricante táctico Team TX
    private val alcanceBuscador = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Estado observable de nodos detectados
    private val _nodosDetectados = MutableStateFlow<Map<Long, NodoMeshPiloto>>(emptyMap())
    val nodosDetectados: StateFlow<Map<Long, NodoMeshPiloto>> = _nodosDetectados.asStateFlow()

    private val _estaBuscando = MutableStateFlow(false)
    val estaBuscando: StateFlow<Boolean> = _estaBuscando.asStateFlow()

    // Adaptadores de hardware BLE
    private var adaptadorBluetooth: BluetoothAdapter? = null
    private var anunciadorBle: BluetoothLeAdvertiser? = null
    private var escanerBle: BluetoothLeScanner? = null

    // Limitador de cadencia de escaneo BLE
    private val limitadorEscaneo = LimitadorTasaEscaneo()

    // Wi-Fi Aware (NAN)
    private var gestorWifiAware: WifiAwareManager? = null
    private var sesionWifiAware: WifiAwareSession? = null
    private var sesionPublicacionAware: PublishDiscoverySession? = null
    private var sesionSuscripcionAware: SubscribeDiscoverySession? = null

    // Wi-Fi Direct (P2P)
    private var gestorWifiP2p: WifiP2pManager? = null
    private var canalWifiP2p: WifiP2pManager.Channel? = null
    private var receptorWifiDirect: ReceptorWifiDirectMesh? = null
    private var estaWifiDirectRegistrado = false

    // Tareas periódicas de limpieza y escaneo
    private var tareaLimpiezaNodos: Job? = null
    private var tareaCicloEscaneoBle: Job? = null

    init {
        inicializarAdaptadores()
    }

    private fun inicializarAdaptadores() {
        try {
            // Inicializar Bluetooth LE
            val gestorBluetooth = contexto.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            adaptadorBluetooth = gestorBluetooth?.adapter
            escanerBle = adaptadorBluetooth?.bluetoothLeScanner
            anunciadorBle = adaptadorBluetooth?.bluetoothLeAdvertiser

            // Inicializar Wi-Fi Aware (NAN) disponible desde Android 8.0 (API 26)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                gestorWifiAware = contexto.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
            }

            // Inicializar Wi-Fi Direct (P2P)
            gestorWifiP2p = contexto.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            canalWifiP2p = gestorWifiP2p?.initialize(contexto, Looper.getMainLooper(), null)

        } catch (e: Exception) {
            Log.e(etiquetaLog, "Error al inicializar adaptadores de red local: ${e.message}")
        }
    }

    /**
     * Iniciar el ciclo de emisión de presencia y rastreo de nodos compañeros.
     */
    fun iniciarExploracion() {
        if (_estaBuscando.value) return
        _estaBuscando.value = true
        Log.d(etiquetaLog, "Iniciando búsqueda táctica de malla (BLE + Wi-Fi Aware + Wi-Fi Direct)...")

        iniciarAnuncioBle()
        iniciarEscaneoBle()
        iniciarWifiAware()
        iniciarWifiDirect()
        iniciarLimpiezaPeriodica()
    }

    /**
     * Detener escaneo y emisión para ahorrar batería al desmontar la moto o cerrar el enmallado.
     */
    fun detenerExploracion() {
        _estaBuscando.value = false
        Log.d(etiquetaLog, "Deteniendo búsqueda táctica de malla...")

        detenerAnuncioBle()
        detenerEscaneoBle()
        detenerWifiAware()
        detenerWifiDirect()
        tareaLimpiezaNodos?.cancel()
        tareaCicloEscaneoBle?.cancel()
        _nodosDetectados.value = emptyMap()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE 1: BLUETOOTH LOW ENERGY (BEACONS & ESCANEO PASIVO CON RATE LIMITER)
    // ─────────────────────────────────────────────────────────────────────────

    private val callbackAnuncioBle = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(etiquetaLog, "Baliza BLE táctica emitida con éxito para: $aliasPilotoLocal")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(etiquetaLog, "Fallo al emitir baliza BLE. Código error: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarAnuncioBle() {
        try {
            if (adaptadorBluetooth?.isEnabled != true) return
            if (anunciadorBle == null) anunciadorBle = adaptadorBluetooth?.bluetoothLeAdvertiser
            if (anunciadorBle == null) return

            val ajustes = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0)
                .build()

            // 1. Datos de servicio: Formato binario compacto little-endian (10 bytes)
            // [0..3]: idPilotoLocal (UInt32 / Int)
            // [4..7]: idRed / canal (UInt32 / Int)
            // [8]: saltos iniciales (0)
            // [9]: bandera de disponibilidad (1 = activo)
            val bufferServicio = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
            bufferServicio.putInt((idPilotoLocal and 0xFFFFFFFFL).toInt())
            bufferServicio.putInt(CanalTactico.GENERAL_TX.idCanal)
            bufferServicio.put(0.toByte()) // 0 saltos
            bufferServicio.put(1.toByte()) // Disponible

            // 2. Datos de fabricante: Grupo táctico + Alias en bytes UTF-8
            val aliasBytes = aliasPilotoLocal.toByteArray(StandardCharsets.UTF_8).take(12).toByteArray()
            val bufferFabricante = ByteBuffer.allocate(4 + aliasBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            bufferFabricante.putInt(CanalTactico.GENERAL_TX.idCanal)
            bufferFabricante.put(aliasBytes)

            val datos = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(uuidServicioMesh))
                .addServiceData(ParcelUuid(uuidServicioMesh), bufferServicio.array())
                .addManufacturerData(idFabricanteTactico, bufferFabricante.array())
                .setIncludeDeviceName(false)
                .build()

            anunciadorBle?.startAdvertising(ajustes, datos, callbackAnuncioBle)
        } catch (e: SecurityException) {
            Log.w(etiquetaLog, "Permiso de Bluetooth no concedido para anunciar: ${e.message}")
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Excepción en anuncio BLE: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun detenerAnuncioBle() {
        try {
            anunciadorBle?.stopAdvertising(callbackAnuncioBle)
        } catch (_: Exception) {}
    }

    private val callbackEscaneoBle = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            procesarResultadoBle(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { procesarResultadoBle(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(etiquetaLog, "Escaneo BLE fallido. Código: $errorCode")
        }
    }

    private fun procesarResultadoBle(resultado: ScanResult?) {
        if (resultado == null) return
        val record = resultado.scanRecord ?: return
        val datosServicio = record.getServiceData(ParcelUuid(uuidServicioMesh)) ?: return

        try {
            var idRemoto: Long? = null
            var aliasRemoto: String? = null

            // Intentar primero decodificación binaria compacta (10 bytes little-endian)
            if (datosServicio.size >= 10) {
                val buffer = ByteBuffer.wrap(datosServicio).order(ByteOrder.LITTLE_ENDIAN)
                val idEntero = buffer.int.toLong() and 0xFFFFFFFFL
                if (idEntero != 0L) {
                    idRemoto = idEntero
                }

                // Extraer nombre desde fabricante si existe
                val datosFabricante = record.getManufacturerSpecificData(idFabricanteTactico)
                if (datosFabricante != null && datosFabricante.size > 4) {
                    val tamanoNombre = datosFabricante.size - 4
                    val bytesNombre = ByteArray(tamanoNombre)
                    System.arraycopy(datosFabricante, 4, bytesNombre, 0, tamanoNombre)
                    aliasRemoto = String(bytesNombre, StandardCharsets.UTF_8).trim()
                }
            }

            // Fallback de compatibilidad: decodificación por texto delimitado "id|alias"
            if (idRemoto == null || aliasRemoto == null) {
                val contenido = String(datosServicio, StandardCharsets.UTF_8)
                val partes = contenido.split("|")
                if (partes.size >= 2) {
                    idRemoto = partes[0].toLongOrNull()
                    aliasRemoto = partes[1]
                }
            }

            if (idRemoto == null || idRemoto == idPilotoLocal) return // Ignorar propio eco
            val aliasFinal = aliasRemoto ?: "Piloto TX $idRemoto"

            val rssi = resultado.rssi
            val direccionFisica = try { resultado.device?.address ?: "" } catch (_: SecurityException) { "" }
            val distanciaEst = calcularDistanciaAproximada(rssi)

            val nodoActualizado = NodoMeshPiloto(
                idMiembro = idRemoto,
                aliasPiloto = aliasFinal,
                direccionNodo = direccionFisica,
                intensidadSenalDbm = rssi,
                distanciaAproximadaMetros = distanciaEst,
                ultimoPingTimestamp = System.currentTimeMillis()
            )

            val mapaActual = _nodosDetectados.value.toMutableMap()
            val eraNuevo = !mapaActual.containsKey(idRemoto)
            mapaActual[idRemoto] = nodoActualizado
            _nodosDetectados.value = mapaActual

            alDetectarNodo(nodoActualizado)
            if (eraNuevo) {
                Log.i(etiquetaLog, "🏍️ Nuevo compañero en rango Mesh BLE: $aliasFinal ($distanciaEst m)")
            }
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Error al procesar paquete beacon BLE: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarEscaneoBle() {
        tareaCicloEscaneoBle?.cancel()
        tareaCicloEscaneoBle = alcanceBuscador.launch {
            while (isActive && _estaBuscando.value) {
                if (adaptadorBluetooth?.isEnabled == true && limitadorEscaneo.puedeEscanear()) {
                    ejecutarVentanaEscaneoBle()
                }
                delay(8000) // Pausa entre ciclos de escaneo para proteger batería
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun ejecutarVentanaEscaneoBle() {
        try {
            if (escanerBle == null) escanerBle = adaptadorBluetooth?.bluetoothLeScanner
            val escaner = escanerBle ?: return

            val filtro = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(uuidServicioMesh))
                .build()

            val ajustesEscaneo = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()

            escaner.startScan(listOf(filtro), ajustesEscaneo, callbackEscaneoBle)
            limitadorEscaneo.registrarEscaneo()

            // Detener la ventana después de 4 segundos de escucha activa
            alcanceBuscador.launch {
                delay(4000)
                try {
                    escaner.stopScan(callbackEscaneoBle)
                } catch (_: Exception) {}
            }
        } catch (e: SecurityException) {
            Log.w(etiquetaLog, "Permiso de Bluetooth scan no concedido: ${e.message}")
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Excepción en escaneo BLE: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun detenerEscaneoBle() {
        tareaCicloEscaneoBle?.cancel()
        try {
            escanerBle?.stopScan(callbackEscaneoBle)
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE 2: WI-FI AWARE (NAN - NEIGHBOR AWARENESS NETWORKING)
    // ─────────────────────────────────────────────────────────────────────────

    private fun iniciarWifiAware() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestor = gestorWifiAware ?: return

        try {
            if (!contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
                Log.d(etiquetaLog, "Dispositivo no soporta Wi-Fi Aware de hardware. Se usará BLE y Wi-Fi Direct.")
                return
            }

            if (!gestor.isAvailable) {
                Log.d(etiquetaLog, "Wi-Fi Aware no disponible temporalmente (Wi-Fi apagado).")
                return
            }

            gestor.attach(object : AttachCallback() {
                override fun onAttached(session: WifiAwareSession?) {
                    sesionWifiAware = session
                    publicarServicioAware()
                    suscribirServicioAware()
                }

                override fun onAttachFailed() {
                    Log.w(etiquetaLog, "Fallo al conectar con Wi-Fi Aware.")
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            Log.w(etiquetaLog, "Permisos insuficientes para Wi-Fi Aware: ${e.message}")
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Excepción al iniciar Wi-Fi Aware: ${e.message}")
        }
    }

    private fun publicarServicioAware() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sesion = sesionWifiAware ?: return

        val configPublicacion = PublishConfig.Builder()
            .setServiceName("TeamTX_MeshTactico")
            .setServiceSpecificInfo("$idPilotoLocal|$aliasPilotoLocal".toByteArray(StandardCharsets.UTF_8))
            .build()

        sesion.publish(configPublicacion, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                sesionPublicacionAware = session
                Log.d(etiquetaLog, "Servicio Wi-Fi Aware publicado con éxito para Team TX.")
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                Log.d(etiquetaLog, "Mensaje Wi-Fi Aware recibido de peer.")
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun suscribirServicioAware() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sesion = sesionWifiAware ?: return

        val configSuscripcion = SubscribeConfig.Builder()
            .setServiceName("TeamTX_MeshTactico")
            .build()

        sesion.subscribe(configSuscripcion, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                sesionSuscripcionAware = session
                Log.d(etiquetaLog, "Suscripción a servicio Wi-Fi Aware iniciada.")
            }

            override fun onServiceDiscovered(peerHandle: PeerHandle, serviceSpecificInfo: ByteArray?, matchFilter: MutableList<ByteArray>?) {
                if (serviceSpecificInfo != null) {
                    try {
                        val info = String(serviceSpecificInfo, StandardCharsets.UTF_8)
                        val partes = info.split("|")
                        if (partes.size >= 2) {
                            val idRemoto = partes[0].toLongOrNull() ?: return
                            if (idRemoto == idPilotoLocal) return

                            val aliasRemoto = partes[1]
                            val nodoWifi = NodoMeshPiloto(
                                idMiembro = idRemoto,
                                aliasPiloto = aliasRemoto,
                                direccionNodo = "WIFI_AWARE_${peerHandle.hashCode()}",
                                intensidadSenalDbm = -45,
                                distanciaAproximadaMetros = 15.0,
                                ultimoPingTimestamp = System.currentTimeMillis()
                            )

                            val mapaActual = _nodosDetectados.value.toMutableMap()
                            mapaActual[idRemoto] = nodoWifi
                            _nodosDetectados.value = mapaActual
                            alDetectarNodo(nodoWifi)
                        }
                    } catch (e: Exception) {
                        Log.e(etiquetaLog, "Error al descodificar info Wi-Fi Aware: ${e.message}")
                    }
                }
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun detenerWifiAware() {
        try {
            sesionPublicacionAware?.close()
            sesionSuscripcionAware?.close()
            sesionWifiAware?.close()
        } catch (_: Exception) {}
        sesionPublicacionAware = null
        sesionSuscripcionAware = null
        sesionWifiAware = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE 3: WI-FI DIRECT (P2P SIN HOST NI ROUTER)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun iniciarWifiDirect() {
        val gestor = gestorWifiP2p ?: return
        val canal = canalWifiP2p ?: return

        try {
            if (receptorWifiDirect == null) {
                receptorWifiDirect = ReceptorWifiDirectMesh()
            }

            if (!estaWifiDirectRegistrado) {
                val filtro = IntentFilter().apply {
                    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                }
                contexto.registerReceiver(receptorWifiDirect, filtro)
                estaWifiDirectRegistrado = true
            }

            // Iniciar descubrimiento de pares Wi-Fi Direct
            gestor.discoverPeers(canal, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(etiquetaLog, "Descubrimiento de pares Wi-Fi Direct P2P iniciado.")
                }

                override fun onFailure(reasonCode: Int) {
                    Log.w(etiquetaLog, "Fallo al iniciar descubrimiento Wi-Fi Direct. Código: $reasonCode")
                }
            })
        } catch (e: SecurityException) {
            Log.w(etiquetaLog, "Permiso de Wi-Fi P2P no concedido: ${e.message}")
        } catch (e: Exception) {
            Log.e(etiquetaLog, "Error en iniciarWifiDirect: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun detenerWifiDirect() {
        try {
            if (estaWifiDirectRegistrado && receptorWifiDirect != null) {
                contexto.unregisterReceiver(receptorWifiDirect)
                estaWifiDirectRegistrado = false
            }
            gestorWifiP2p?.stopPeerDiscovery(canalWifiP2p, null)
        } catch (_: Exception) {}
    }

    /**
     * BroadcastReceiver interno para gestionar eventos de conexión y descubrimiento Wi-Fi Direct P2P.
     */
    private inner class ReceptorWifiDirectMesh : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val accion = intent.action ?: return
            val gestor = gestorWifiP2p ?: return
            val canal = canalWifiP2p ?: return

            when (accion) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val estado = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val habilitado = estado == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(etiquetaLog, "Wi-Fi Direct estado cambiado: habilitado=$habilitado")
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    gestor.requestPeers(canal) { listaPares ->
                        val pares = listaPares.deviceList
                        for (dispositivo in pares) {
                            val idVirtual = (dispositivo.deviceAddress.hashCode().toLong() and 0x7FFFFFFF)
                                val previo = _nodosDetectados.value[idVirtual]
                                val aliasAsignado = if (previo != null && previo.aliasPiloto.isNotBlank() && previo.aliasPiloto != dispositivo.deviceName) {
                                    previo.aliasPiloto
                                } else {
                                    "Piloto TX"
                                }
                                val nodoP2p = NodoMeshPiloto(
                                    idMiembro = idVirtual,
                                    aliasPiloto = aliasAsignado,
                                    modeloTelefonoHardware = dispositivo.deviceName.ifBlank { "Dispositivo Android" },
                                    direccionNodo = dispositivo.deviceAddress,
                                    intensidadSenalDbm = -50,
                                    distanciaAproximadaMetros = 12.0,
                                    ultimoPingTimestamp = System.currentTimeMillis()
                                )

                                val mapaActual = _nodosDetectados.value.toMutableMap()
                                mapaActual[idVirtual] = nodoP2p
                                _nodosDetectados.value = mapaActual
                                alDetectarNodo(nodoP2p)
                            }
                        }
                    }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val infoRed = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (infoRed?.isConnected == true) {
                        gestor.requestConnectionInfo(canal) { infoConexion ->
                            val esPropietarioGrupo = infoConexion.isGroupOwner
                            val ipPropietario = infoConexion.groupOwnerAddress?.hostAddress ?: ""
                            Log.i(etiquetaLog, "Wi-Fi Direct conectado. EsPropietario=$esPropietarioGrupo, IP=$ipPropietario")
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE 4: AUDITORÍA DE SALUD Y CADUCIDAD DE NODOS (CLEANUP)
    // ─────────────────────────────────────────────────────────────────────────

    private fun iniciarLimpiezaPeriodica() {
        tareaLimpiezaNodos?.cancel()
        tareaLimpiezaNodos = alcanceBuscador.launch {
            while (isActive && _estaBuscando.value) {
                delay(12000) // Auditar cada 12 segundos
                val ahora = System.currentTimeMillis()
                val mapa = _nodosDetectados.value.toMutableMap()
                val iterador = mapa.entries.iterator()
                var huboCambios = false

                while (iterador.hasNext()) {
                    val entrada = iterador.next()
                    // Si pasaron más de 120 segundos sin señal, considerar desconectado
                    if (ahora - entrada.value.ultimoPingTimestamp > 120000) {
                        Log.d(etiquetaLog, "Nodo fuera de rango: ${entrada.value.aliasPiloto}")
                        alPerderNodo(entrada.key)
                        iterador.remove()
                        huboCambios = true
                    }
                }

                if (huboCambios) {
                    _nodosDetectados.value = mapa
                }
            }
        }
    }

    /**
     * Estimación de distancia basada en el modelo de atenuación Log-Distance Path Loss.
     */
    private fun calcularDistanciaAproximada(rssi: Int, txPowerCalibrado: Int = -59): Double {
        if (rssi == 0) return -1.0
        val ratio = rssi * 1.0 / txPowerCalibrado
        return if (ratio < 1.0) {
            Math.pow(ratio, 10.0)
        } else {
            val dist = (0.89976) * Math.pow(ratio, 7.7095) + 0.111
            Math.round(dist * 10.0) / 10.0
        }
    }

    /**
     * Limitador de frecuencia de escaneo BLE para evitar que Android bloquee el escáner por batería.
     */
    private class LimitadorTasaEscaneo(
        private val maximosEscaneosPorVentana: Int = 4,
        private val ventanaTiempoMs: Long = 30000L
    ) {
        private val timestampsEscaneos = ArrayDeque<Long>()

        @Synchronized
        fun puedeEscanear(): Boolean {
            val ahora = System.currentTimeMillis()
            while (timestampsEscaneos.isNotEmpty() && (ahora - timestampsEscaneos.first()) > ventanaTiempoMs) {
                timestampsEscaneos.removeFirst()
            }
            return timestampsEscaneos.size < maximosEscaneosPorVentana
        }

        @Synchronized
        fun registrarEscaneo() {
            timestampsEscaneos.addLast(System.currentTimeMillis())
        }
    }
}
