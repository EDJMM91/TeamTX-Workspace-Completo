package com.example.meshtx

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ENLACE COPILOTO - MÓDULO INTRAMOTO POR BLUETOOTH CLÁSICO (RFCOMM)
 * ═══════════════════════════════════════════════════════════════════════════
 * Implementa la Micro-Malla y Nodo Puente para la comunicación Piloto-Copiloto
 * en la misma moto (Keeway TX 200).
 *
 * Características clave:
 * 1. Ahorro extremo de batería: En modo SOLO_BLUETOOTH, el copiloto apaga el
 *    escáner Wi-Fi y se enlaza por BluetoothSocket directo.
 * 2. El Piloto como Gateway: El teléfono del piloto recibe el audio del copiloto
 *    por Bluetooth, lo reproduce en su casco y, opcionalmente, lo retransmite hacia
 *    la caravana por Wi-Fi Direct y Firebase 4G.
 * 3. Latencia ultrabaja (<40ms): Transmisión continua punto a punto sin contención.
 * 4. Canal privado intramoto: Conversación privada entre piloto y copiloto sin
 *    saturar la frecuencia general de la caravana.
 * 5. Advertencia de alcance físico: Bluetooth clásico está acotado a ~10-15 metros.
 */
class EnlaceCopiloto(
    private val contexto: Context,
    private var idPilotoLocal: Long,
    private var aliasPilotoLocal: String,
    private val alRecibirAudioCopiloto: (payloadAudio: ByteArray, idEmisor: Long, aliasEmisor: String) -> Unit
) {

    companion object {
        private const val ETIQUETA_LOG = "MeshTX_EnlaceCopiloto"
        const val UUID_COPILOTO_STR = "fa87c0d0-afac-11de-8a39-0800200c9a66"
        val UUID_COPILOTO: UUID = UUID.fromString(UUID_COPILOTO_STR)
        private const val NOMBRE_SERVICIO = "TeamTX_Intercom_Copiloto"
        private const val CABECERA_MAGICA: Short = 0x5458 // 'T', 'X'
        const val ALCANCE_MAXIMO_METROS = 15
    }

    private val alcanceEnlace = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Estados observables expuestos a la UI y GestorMeshTx
    private val _estadoEnlace = MutableStateFlow(EstadoEnlaceCopiloto.DESCONECTADO)
    val estadoEnlace: StateFlow<EstadoEnlaceCopiloto> = _estadoEnlace.asStateFlow()

    private val _rolActual = MutableStateFlow(RolEnMoto.SOLO_PILOTO_INDIVIDUAL)
    val rolActual: StateFlow<RolEnMoto> = _rolActual.asStateFlow()

    private val _dispositivoConectadoNombre = MutableStateFlow<String?>(null)
    val dispositivoConectadoNombre: StateFlow<String?> = _dispositivoConectadoNombre.asStateFlow()

    private val _dispositivoConectadoMac = MutableStateFlow<String?>(null)
    val dispositivoConectadoMac: StateFlow<String?> = _dispositivoConectadoMac.asStateFlow()

    private val _estaHablandoCopiloto = MutableStateFlow(false)
    val estaHablandoCopiloto: StateFlow<Boolean> = _estaHablandoCopiloto.asStateFlow()

    private val _retransmitirACaravana = MutableStateFlow(false)
    val retransmitirACaravana: StateFlow<Boolean> = _retransmitirACaravana.asStateFlow()

    private val _modoEnlace = MutableStateFlow(ModoEnlaceIntramoto.HIBRIDO_TRIMODAL)
    val modoEnlace: StateFlow<ModoEnlaceIntramoto> = _modoEnlace.asStateFlow()

    @Volatile private var socketActivo: BluetoothSocket? = null
    @Volatile private var serverSocket: BluetoothServerSocket? = null
    @Volatile private var flujoSalida: DataOutputStream? = null
    private val estaActivo = AtomicBoolean(false)
    private var tareaServidor: Job? = null
    private var tareaCliente: Job? = null
    private var tareaReceptor: Job? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = contexto.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    /**
     * Verifica si se cuentan con los permisos necesarios de Bluetooth según la versión de Android.
     */
    fun tienePermisosBluetooth(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(contexto, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val scan = ContextCompat.checkSelfPermission(contexto, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            return connect && scan
        }
        return true
    }

    /**
     * Lista los dispositivos Bluetooth emparejados en el teléfono para facilitar la selección del copiloto.
     */
    @SuppressLint("MissingPermission")
    fun obtenerDispositivosEmparejados(): List<Pair<String, String>> {
        if (!tienePermisosBluetooth() || bluetoothAdapter == null) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.map { it.name to it.address } ?: emptyList()
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Error al listar emparejados: ${e.message}")
            emptyList()
        }
    }

    /**
     * Inicia el modo Piloto (Servidor Gateway): Espera pasivamente la conexión Bluetooth del copiloto.
     */
    fun iniciarComoPilotoServidor() {
        detenerEnlace()
        _rolActual.value = RolEnMoto.PILOTO_GATEWAY
        estaActivo.set(true)
        _estadoEnlace.value = EstadoEnlaceCopiloto.ESPERANDO_COPILOTO

        tareaServidor = alcanceEnlace.launch {
            ejecutarBucleServidor()
        }
        Log.i(ETIQUETA_LOG, "🏍️ Modo Piloto Gateway iniciado. Esperando conexión Bluetooth del Copiloto...")
    }

    /**
     * Inicia el modo Copiloto (Cliente): Se conecta por Bluetooth al teléfono del piloto.
     */
    fun iniciarComoCopilotoCliente(macPiloto: String) {
        detenerEnlace()
        _rolActual.value = RolEnMoto.COPILOTO_ENLACE
        estaActivo.set(true)
        _estadoEnlace.value = EstadoEnlaceCopiloto.CONECTANDO

        tareaCliente = alcanceEnlace.launch {
            ejecutarConexionCliente(macPiloto)
        }
        Log.i(ETIQUETA_LOG, "🎒 Modo Copiloto iniciado. Conectando con Piloto en MAC: $macPiloto...")
    }

    /**
     * Bucle del servidor Bluetooth RFCOMM (Piloto Gateway).
     */
    @SuppressLint("MissingPermission")
    private suspend fun ejecutarBucleServidor() = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled || !tienePermisosBluetooth()) {
            _estadoEnlace.value = EstadoEnlaceCopiloto.ERROR
            Log.e(ETIQUETA_LOG, "Bluetooth apagado o sin permisos para servidor")
            return@withContext
        }

        while (estaActivo.get() && isActive) {
            try {
                Log.d(ETIQUETA_LOG, "Abriendo BluetoothServerSocket RFCOMM...")
                val server = adapter.listenUsingInsecureRfcommWithServiceRecord(NOMBRE_SERVICIO, UUID_COPILOTO)
                serverSocket = server

                val socket = server.accept() // Bloqueante hasta que el copiloto se conecta
                server.close() // Aceptamos 1 solo enlace intramoto (Piloto-Copiloto)
                serverSocket = null

                if (socket != null && estaActivo.get()) {
                    gestionarSocketConectado(socket, socket.remoteDevice.name ?: "Copiloto TX", socket.remoteDevice.address)
                }
            } catch (e: IOException) {
                if (estaActivo.get()) {
                    Log.w(ETIQUETA_LOG, "Reintentando escucha servidor RFCOMM: ${e.message}")
                    delay(3000)
                }
            }
        }
    }

    /**
     * Intento de conexión del cliente Bluetooth RFCOMM (Copiloto).
     */
    @SuppressLint("MissingPermission")
    private suspend fun ejecutarConexionCliente(macPiloto: String) = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled || !tienePermisosBluetooth() || macPiloto.isBlank()) {
            _estadoEnlace.value = EstadoEnlaceCopiloto.ERROR
            Log.e(ETIQUETA_LOG, "Bluetooth inactivo, sin permisos o MAC del piloto vacía ($macPiloto)")
            return@withContext
        }

        while (estaActivo.get() && isActive && _estadoEnlace.value != EstadoEnlaceCopiloto.CONECTADO) {
            try {
                val dispositivoPiloto = adapter.getRemoteDevice(macPiloto)
                Log.d(ETIQUETA_LOG, "Intentando enlazar con Piloto: ${dispositivoPiloto.name} ($macPiloto)...")
                
                // Cancelar descubrimiento para máxima velocidad y latencia baja
                adapter.cancelDiscovery()

                val socket = dispositivoPiloto.createInsecureRfcommSocketToServiceRecord(UUID_COPILOTO)
                socket.connect()

                if (socket.isConnected && estaActivo.get()) {
                    gestionarSocketConectado(socket, dispositivoPiloto.name ?: "Piloto TX", macPiloto)
                    break
                }
            } catch (e: IOException) {
                if (estaActivo.get()) {
                    Log.w(ETIQUETA_LOG, "Fallo al conectar con Piloto (${e.message}). Reintentando en 4s...")
                    _estadoEnlace.value = EstadoEnlaceCopiloto.CONECTANDO
                    delay(4000)
                }
            }
        }
    }

    /**
     * Configura los flujos de lectura y escritura una vez que el socket Bluetooth está establecido.
     */
    private fun gestionarSocketConectado(socket: BluetoothSocket, nombreRemoto: String, macRemota: String) {
        socketActivo = socket
        _dispositivoConectadoNombre.value = nombreRemoto
        _dispositivoConectadoMac.value = macRemota
        _estadoEnlace.value = EstadoEnlaceCopiloto.CONECTADO

        try {
            flujoSalida = DataOutputStream(socket.outputStream)
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error al abrir stream de salida Bluetooth: ${e.message}")
            return
        }

        tareaReceptor?.cancel()
        tareaReceptor = alcanceEnlace.launch {
            bucleLecturaAudio(socket)
        }
        Log.i(ETIQUETA_LOG, "✅ ¡Enlace Bluetooth Intramoto ESTABLECIDO con $nombreRemoto ($macRemota)!")
    }

    /**
     * Bucle de lectura de tramas de voz recibidas por Bluetooth desde el otro asiento de la moto.
     */
    private suspend fun bucleLecturaAudio(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        val entrada = try {
            DataInputStream(socket.inputStream)
        } catch (e: Exception) {
            cerrarConexionActual()
            return@withContext
        }

        var silencioTimerJob: Job? = null

        try {
            while (estaActivo.get() && socket.isConnected && isActive) {
                // 1. Leer cabecera mágica (2 bytes)
                val magica = entrada.readShort()
                if (magica != CABECERA_MAGICA) {
                    continue
                }

                // 2. Metadatos de la trama
                val emisorId = entrada.readLong()
                val aliasEmisor = entrada.readUTF()
                val longitud = entrada.readInt()

                if (longitud in 1..4096) {
                    val bufferAudio = ByteArray(longitud)
                    entrada.readFully(bufferAudio)

                    _estaHablandoCopiloto.value = true
                    silencioTimerJob?.cancel()
                    silencioTimerJob = alcanceEnlace.launch {
                        delay(600)
                        _estaHablandoCopiloto.value = false
                    }

                    // Entregar audio directamente al gestor central / AudioCasco
                    alRecibirAudioCopiloto(bufferAudio, emisorId, aliasEmisor)
                }
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Enlace Bluetooth Intramoto desconectado: ${e.message}")
        } finally {
            cerrarConexionActual()
            // Auto-reconexión si aún se desea mantener el rol
            if (estaActivo.get()) {
                val rol = _rolActual.value
                val mac = _dispositivoConectadoMac.value
                delay(2000)
                if (rol == RolEnMoto.PILOTO_GATEWAY) {
                    iniciarComoPilotoServidor()
                } else if (rol == RolEnMoto.COPILOTO_ENLACE && !mac.isNullOrBlank()) {
                    iniciarComoCopilotoCliente(mac)
                }
            }
        }
    }

    /**
     * Transmite un paquete de audio comprimido por el enlace Bluetooth hacia el otro teléfono.
     */
    fun enviarAudioCopiloto(payloadAudio: ByteArray) {
        if (_estadoEnlace.value != EstadoEnlaceCopiloto.CONECTADO) return
        val salida = flujoSalida ?: return

        alcanceEnlace.launch {
            try {
                synchronized(salida) {
                    salida.writeShort(CABECERA_MAGICA.toInt())
                    salida.writeLong(idPilotoLocal)
                    salida.writeUTF(aliasPilotoLocal)
                    salida.writeInt(payloadAudio.size)
                    salida.write(payloadAudio)
                    salida.flush()
                }
            } catch (e: Exception) {
                Log.w(ETIQUETA_LOG, "Fallo al enviar trama por Bluetooth: ${e.message}")
            }
        }
    }

    /**
     * Cierra el socket activo y limpia streams sin desarmar la configuración de roles.
     */
    private fun cerrarConexionActual() {
        try {
            flujoSalida?.close()
        } catch (_: Exception) {}
        flujoSalida = null

        try {
            socketActivo?.close()
        } catch (_: Exception) {}
        socketActivo = null

        _estaHablandoCopiloto.value = false
        _dispositivoConectadoNombre.value = null
        if (estaActivo.get()) {
            _estadoEnlace.value = if (_rolActual.value == RolEnMoto.PILOTO_GATEWAY)
                EstadoEnlaceCopiloto.ESPERANDO_COPILOTO
            else
                EstadoEnlaceCopiloto.CONECTANDO
        } else {
            _estadoEnlace.value = EstadoEnlaceCopiloto.DESCONECTADO
        }
    }

    /**
     * Configura si la voz del copiloto se debe retransmitir a la caravana Wi-Fi/4G.
     */
    fun alternarRetransmitirACaravana(activar: Boolean) {
        _retransmitirACaravana.value = activar
        Log.i(ETIQUETA_LOG, "Retransmitir copiloto a caravana: $activar")
    }

    /**
     * Cambia el modo de enlace intramoto (Solo Bluetooth, Solo Wi-Fi, o Híbrido Trimodal).
     */
    fun cambiarModoEnlace(modo: ModoEnlaceIntramoto) {
        _modoEnlace.value = modo
        Log.i(ETIQUETA_LOG, "Modo de enlace intramoto cambiado a: ${modo.titulo}")
    }

    /**
     * Actualiza la identidad del piloto local.
     */
    fun actualizarIdentidadLocal(id: Long, alias: String) {
        this.idPilotoLocal = id
        this.aliasPilotoLocal = alias
    }

    /**
     * Detiene el enlace Bluetooth y cancela tareas de fondo.
     */
    fun detenerEnlace() {
        estaActivo.set(false)
        tareaServidor?.cancel()
        tareaCliente?.cancel()
        tareaReceptor?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        cerrarConexionActual()
        _estadoEnlace.value = EstadoEnlaceCopiloto.DESCONECTADO
    }

    /**
     * Libera recursos al destruir el servicio o ViewModel.
     */
    fun liberar() {
        detenerEnlace()
        alcanceEnlace.cancel()
    }
}
