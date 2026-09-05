package com.example.meshtx

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * TRANSPORTE FÍSICO DE MALLA UDP (P2P OFFLINE DE ULTRA-BAJA LATENCIA)
 * ═══════════════════════════════════════════════════════════════════════════
 * Responsabilidades:
 * 1. Abrir un socket Datagram UDP en el puerto táctico 58200 con soporte Broadcast.
 * 2. Adquirir MulticastLock para evitar que Android suspenda paquetes de red.
 * 3. Calcular dinámicamente las direcciones de broadcast de subred (Hotspot / LAN).
 * 4. Serializar y deserializar paquetes `PaqueteDatosMesh` a binario compacto.
 * 5. Transmitir paquetes de voz, SOS y telemetría por difusión múltiple
 *    (Broadcast de subred + Unicast redundante a IPs conocidas).
 * 6. Hilo continuo de recepción que alimenta directamente al motor de reproducción.
 *
 * 100% OFFLINE - Cero dependencias de servidores externos o internet.
 */
class TransporteMallaUdp(
    private val contexto: Context,
    private val idPilotoLocal: Long,
    private var aliasPilotoLocal: String,
    private val alRecibirPaquete: (PaqueteDatosMesh, Long) -> Unit
) {

    private val etiquetaLog = "MeshTX_TransporteUDP"
    private val puertoTactico = 58200
    private val encabezadoMagico = 0x54584D53 // "TXMS" (Team TX Mesh System)

    private val alcanceTransporte = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socketUdp: DatagramSocket? = null
    private var tareaRecepcion: Job? = null
    private var tareaBaliza: Job? = null
    private var estaActivo = false

    // Registro de IPs de compañeros detectados para unicast directo redundante
    private val ipsParesConocidos = ConcurrentHashMap<Long, String>()

    fun registrarIpP2p(idNodo: Long, ip: String) {
        if (ip.isNotBlank()) {
            ipsParesConocidos[idNodo] = ip
            Log.i(etiquetaLog, "IP P2P registrada para transporte: nodo=$idNodo, ip=$ip")
        }
    }

    // Multicast lock y WifiLock para evitar que Android suspenda paquetes en Wi-Fi
    private var multicastLock: WifiManager.MulticastLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    // Rate-limiting de logs de audio para no saturar el logcat ni pausar hilos a 25 fps
    private var ultimoLogAudioEmitidoMs = 0L
    private var ultimoLogAudioRecibidoMs = 0L

    /**
     * Inicia la escucha UDP y el canal de difusión física.
     */
    fun iniciarTransporte() {
        if (estaActivo) return
        estaActivo = true

        try {
            val wifiManager = contexto.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("MeshTxMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
            val modoWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager?.createWifiLock(modoWifi, "MeshTxWifiLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.i(etiquetaLog, "MulticastLock y WifiLock adquiridos correctamente (Modo baja latencia activo).")
        } catch (e: Exception) {
            Log.w(etiquetaLog, "No se pudo adquirir locks de Wi-Fi: ${e.message}")
        }

        try {
            socketUdp = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                receiveBufferSize = 65536
                sendBufferSize = 65536
                bind(java.net.InetSocketAddress(puertoTactico))
            }
            Log.i(etiquetaLog, "Socket UDP Táctico abierto con éxito en puerto $puertoTactico")
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Puerto $puertoTactico ocupado, intentando socket dinámico: ${e.message}")
            try {
                socketUdp = DatagramSocket().apply {
                    broadcast = true
                    receiveBufferSize = 65536
                }
            } catch (e2: Exception) {
                Log.e(etiquetaLog, "Fallo crítico abriendo socket alternativo: ${e2.message}")
                return
            }
        }

        iniciarHiloRecepcion()
        iniciarBalizaPresencia()
    }

    /**
     * Detiene el socket y libera recursos de red.
     */
    fun detenerTransporte() {
        estaActivo = false
        tareaRecepcion?.cancel()
        tareaBaliza?.cancel()

        try {
            socketUdp?.close()
        } catch (_: Exception) {}
        socketUdp = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
        wifiLock = null

        ipsParesConocidos.clear()
        Log.i(etiquetaLog, "Transporte UDP detenido y recursos liberados.")
    }

    /**
     * Transmite un paquete por el medio físico hacia los demás pilotos.
     */
    fun transmitirPaquete(paquete: PaqueteDatosMesh, direccionIpDestino: String? = null) {
        if (!estaActivo || socketUdp == null) return

        alcanceTransporte.launch {
            try {
                val bytes = serializarPaquete(paquete)

                if (paquete.tipo == TipoPaqueteMesh.AUDIO_VOZ_OPUS) {
                    val ahora = System.currentTimeMillis()
                    if (ahora - ultimoLogAudioEmitidoMs > 3000L) {
                        ultimoLogAudioEmitidoMs = ahora
                        Log.i(etiquetaLog, "🎙️ Transmitiendo flujo de voz activo (${bytes.size} B) desde $aliasPilotoLocal...")
                    }
                }

                if (!direccionIpDestino.isNullOrBlank() && direccionIpDestino.contains(".")) {
                    // Unicast específico
                    val destino = InetAddress.getByName(direccionIpDestino)
                    val datagrama = DatagramPacket(bytes, bytes.size, destino, puertoTactico)
                    socketUdp?.send(datagrama)
                } else {
                    val esAudio = paquete.tipo == TipoPaqueteMesh.AUDIO_VOZ_OPUS
                    if (esAudio && ipsParesConocidos.isNotEmpty()) {
                        // Flujo de voz en tiempo real: Enviar EXCLUSIVAMENTE por Unicast a cada IP conocida.
                        // Esto viaja a máxima tasa 802.11 (sin throttling de broadcast de 1 Mbps en routers Wi-Fi)
                        for ((_, ip) in ipsParesConocidos) {
                            try {
                                val dest = InetAddress.getByName(ip)
                                val datagrama = DatagramPacket(bytes, bytes.size, dest, puertoTactico)
                                socketUdp?.send(datagrama)
                            } catch (_: Exception) {}
                        }
                    } else {
                        // 1. Enviar prioritariamente por Unicast directo a cada compañero conocido
                        for ((_, ip) in ipsParesConocidos) {
                            try {
                                val dest = InetAddress.getByName(ip)
                                val datagrama = DatagramPacket(bytes, bytes.size, dest, puertoTactico)
                                socketUdp?.send(datagrama)
                            } catch (_: Exception) {}
                        }

                        // 2. Enviar a las direcciones de broadcast de subred para descubrimiento y malla general
                        val broadcasts = obtenerDireccionesBroadcast()
                        for (bcast in broadcasts) {
                            try {
                                val datagrama = DatagramPacket(bytes, bytes.size, bcast, puertoTactico)
                                socketUdp?.send(datagrama)
                            } catch (e: Exception) {
                                Log.w(etiquetaLog, "Fallo enviando a broadcast $bcast: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(etiquetaLog, "Error enviando datagrama ${paquete.tipo}: ${e.message}")
            }
        }
    }

    /**
     * Hilo en segundo plano que escucha continuamente los datagramas entrantes.
     */
    private fun iniciarHiloRecepcion() {
        tareaRecepcion?.cancel()
        tareaRecepcion = alcanceTransporte.launch {
            val buffer = ByteArray(4096)

            while (isActive && estaActivo) {
                try {
                    val socket = socketUdp ?: break
                    val datagrama = DatagramPacket(buffer, buffer.size)
                    socket.receive(datagrama)

                    if (datagrama.length > 0) {
                        val ipRemota = datagrama.address.hostAddress ?: ""
                        val paquete = deserializarPaquete(datagrama.data, datagrama.length)

                        if (paquete != null) {
                            if (paquete.idEmisor == idPilotoLocal) {
                                // Ignorar eco local de nuestro propio broadcast
                                continue
                            }

                            if (ipRemota.isNotBlank()) {
                                ipsParesConocidos[paquete.idEmisor] = ipRemota
                            }

                            if (paquete.tipo == TipoPaqueteMesh.AUDIO_VOZ_OPUS) {
                                val ahora = System.currentTimeMillis()
                                if (ahora - ultimoLogAudioRecibidoMs > 3000L) {
                                    ultimoLogAudioRecibidoMs = ahora
                                    Log.i(etiquetaLog, "🔊 Recibiendo flujo de voz activo de ${paquete.aliasEmisor} (${paquete.payloadAudio?.size} B) desde $ipRemota")
                                }
                            }

                            alRecibirPaquete(paquete, paquete.idEmisor)
                        }
                    }
                } catch (e: SocketException) {
                    if (!estaActivo) break
                    Log.w(etiquetaLog, "SocketException en recepción UDP: ${e.message}")
                    delay(150)
                } catch (e: Exception) {
                    Log.w(etiquetaLog, "Error recibiendo paquete UDP: ${e.message}")
                    delay(30)
                }
            }
        }
    }

    fun actualizarAliasLocal(nuevoAlias: String) {
        if (nuevoAlias.isNotBlank()) {
            this.aliasPilotoLocal = nuevoAlias
        }
    }

    /**
     * Emite una baliza periódica de descubrimiento para que los nodos se detecten
     * automáticamente aún sin conexión a internet ni Bluetooth emparejado.
     */
    private fun iniciarBalizaPresencia() {
        tareaBaliza?.cancel()
        tareaBaliza = alcanceTransporte.launch {
            while (isActive && estaActivo) {
                val fotoACompartir = if (GestorMeshTx.debeCompartirFotoPerfil()) GestorMeshTx.fotoPerfilLocal else ""
                val payloadBaliza = "${android.os.Build.MODEL}|$fotoACompartir|${GestorMeshTx.modeloMotoLocal}|${GestorMeshTx.fichaLocal}"
                val baliza = PaqueteDatosMesh(
                    idPaquete = System.currentTimeMillis(),
                    idEmisor = idPilotoLocal,
                    aliasEmisor = aliasPilotoLocal,
                    payloadTexto = payloadBaliza,
                    tipo = TipoPaqueteMesh.BEACON_DESCUBRIMIENTO,
                    saltosRelay = 0
                )
                transmitirPaquete(baliza)
                delay(2500) // Baliza cada 2.5 segundos
            }
        }
    }

    @Volatile private var cacheBroadcasts = listOf<InetAddress>()
    @Volatile private var tiempoUltimoCalculoBroadcastsMs = 0L

    /**
     * Calcula dinámicamente las direcciones de broadcast de las interfaces de red locales
     * (Hotspot Wi-Fi del líder, red compartida o router) con caché para no saturar CPU ni generar jitter.
     */
    private fun obtenerDireccionesBroadcast(): List<InetAddress> {
        val ahora = System.currentTimeMillis()
        if (cacheBroadcasts.isNotEmpty() && (ahora - tiempoUltimoCalculoBroadcastsMs < 8000L)) {
            return cacheBroadcasts
        }
        val lista = mutableListOf<InetAddress>()
        try {
            lista.add(InetAddress.getByName("255.255.255.255"))
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val red = interfaces.nextElement()
                if (red.isLoopback || !red.isUp) continue
                for (ia in red.interfaceAddresses) {
                    val bcast = ia.broadcast
                    if (bcast != null) {
                        lista.add(bcast)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Error calculando broadcasts locales: ${e.message}")
        }
        val distinct = lista.distinct()
        cacheBroadcasts = distinct
        tiempoUltimoCalculoBroadcastsMs = ahora
        return distinct
    }

    /**
     * Serialización binaria de alta eficiencia.
     */
    private fun serializarPaquete(paquete: PaqueteDatosMesh): ByteArray {
        val stream = ByteArrayOutputStream()
        val salida = DataOutputStream(stream)

        salida.writeInt(encabezadoMagico)
        salida.writeLong(paquete.idPaquete)
        salida.writeLong(paquete.idEmisor)

        val aliasBytes = paquete.aliasEmisor.toByteArray(StandardCharsets.UTF_8)
        salida.writeShort(aliasBytes.size)
        salida.write(aliasBytes)

        salida.writeLong(paquete.destinoPilotoId ?: 0L)
        salida.writeInt(paquete.canal)
        salida.writeByte(paquete.tipo.ordinal)
        salida.writeInt(paquete.saltosRelay)
        salida.writeLong(paquete.timestamp)

        // Payload de audio
        if (paquete.payloadAudio != null && paquete.payloadAudio.isNotEmpty()) {
            salida.writeInt(paquete.payloadAudio.size)
            salida.write(paquete.payloadAudio)
        } else {
            salida.writeInt(0)
        }

        // Payload de texto
        if (!paquete.payloadTexto.isNullOrEmpty()) {
            val textoBytes = paquete.payloadTexto.toByteArray(StandardCharsets.UTF_8)
            salida.writeInt(textoBytes.size)
            salida.write(textoBytes)
        } else {
            salida.writeInt(0)
        }

        salida.flush()
        return stream.toByteArray()
    }

    /**
     * Deserialización binaria segura de datagramas entrantes.
     */
    private fun deserializarPaquete(bytes: ByteArray, longitud: Int): PaqueteDatosMesh? {
        return try {
            val stream = ByteArrayInputStream(bytes, 0, longitud)
            val entrada = DataInputStream(stream)

            val magico = entrada.readInt()
            if (magico != encabezadoMagico) return null

            val idPaquete = entrada.readLong()
            val idEmisor = entrada.readLong()

            val longitudAlias = entrada.readShort().toInt()
            val aliasBytes = ByteArray(longitudAlias)
            entrada.readFully(aliasBytes)
            val aliasEmisor = String(aliasBytes, StandardCharsets.UTF_8)

            val destinoIdRaw = entrada.readLong()
            val destinoId = if (destinoIdRaw == 0L) null else destinoIdRaw

            val canal = entrada.readInt()
            val ordinalTipo = entrada.readByte().toInt()
            val tipo = TipoPaqueteMesh.values().getOrNull(ordinalTipo) ?: TipoPaqueteMesh.AUDIO_VOZ_OPUS

            val saltosRelay = entrada.readInt()
            val timestamp = entrada.readLong()

            // Payload audio
            val longAudio = entrada.readInt()
            val payloadAudio = if (longAudio > 0 && longAudio <= 4096) {
                val audio = ByteArray(longAudio)
                entrada.readFully(audio)
                audio
            } else null

            // Payload texto
            val longTexto = entrada.readInt()
            val payloadTexto = if (longTexto > 0 && longTexto <= 1024) {
                val textoBytes = ByteArray(longTexto)
                entrada.readFully(textoBytes)
                String(textoBytes, StandardCharsets.UTF_8)
            } else null

            PaqueteDatosMesh(
                idPaquete = idPaquete,
                idEmisor = idEmisor,
                aliasEmisor = aliasEmisor,
                destinoPilotoId = destinoId,
                canal = canal,
                tipo = tipo,
                payloadAudio = payloadAudio,
                payloadTexto = payloadTexto,
                timestamp = timestamp,
                saltosRelay = saltosRelay
            )
        } catch (e: Exception) {
            Log.w(etiquetaLog, "Fallo al deserializar datagrama: ${e.message}")
            null
        }
    }
}
