package com.example.meshtx

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GESTOR CENTRAL DEL PAQUETE MESH TX - INTERCOMUNICADOR TÁCTICO
 * ═══════════════════════════════════════════════════════════════════════════
 * Este es el ÚNICO archivo del paquete que se comunica con la interfaz de usuario,
 * MainActivity o ViewModels. Coordina de forma totalmente desacoplada los subsistemas:
 * 1. BuscadorMalla: Rastreo Wi-Fi Direct P2P, Wi-Fi Aware (NAN) y BLE.
 * 2. EnrutadorMalla: Multi-hop descentralizado (Modelo Actor + CacheExpiracionTemporal).
 * 3. AudioCasco: Captura PTT/VOX, filtros de viento, Bluetooth SCO y reproducción.
 * 4. NubeMalla: Sincronización pasiva en segundo plano (cuando haya red).
 *
 * 100% OFFLINE para audio y malla tácticα - Cero dependencias externas.
 */
object GestorMeshTx {

    private const val ETIQUETA_LOG = "MeshTX_GestorCentral"
    private val alcanceGestor = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Estados observables expuestos a la interfaz
    private val _estadoConexion = MutableStateFlow(MeshEstadoConexion.DESCONECTADO)
    val estadoConexion: StateFlow<MeshEstadoConexion> = _estadoConexion.asStateFlow()

    private val _canalActual = MutableStateFlow(CanalTactico.GENERAL_TX)
    val canalActual: StateFlow<CanalTactico> = _canalActual.asStateFlow()

    private val _nodosEnRed = MutableStateFlow<List<NodoMeshPiloto>>(emptyList())
    val nodosEnRed: StateFlow<List<NodoMeshPiloto>> = _nodosEnRed.asStateFlow()

    private val _estaTransmitiendoPtt = MutableStateFlow(false)
    val estaTransmitiendoPtt: StateFlow<Boolean> = _estaTransmitiendoPtt.asStateFlow()

    private val _pilotoHablandoAhora = MutableStateFlow<String?>(null)
    val pilotoHablandoAhora: StateFlow<String?> = _pilotoHablandoAhora.asStateFlow()

    private val _idPilotoHablandoAhora = MutableStateFlow<Long?>(null)
    val idPilotoHablandoAhora: StateFlow<Long?> = _idPilotoHablandoAhora.asStateFlow()

    private val _ajustes = MutableStateFlow(AjustesIntercomunicadorTactico())
    val ajustes: StateFlow<AjustesIntercomunicadorTactico> = _ajustes.asStateFlow()

    private val _totalSaltosRelay = MutableStateFlow(0)
    val totalSaltosRelay: StateFlow<Int> = _totalSaltosRelay.asStateFlow()

    // Estado de casco Bluetooth táctico
    private val _cascoBluetoothConectado = MutableStateFlow(false)
    val cascoBluetoothConectado: StateFlow<Boolean> = _cascoBluetoothConectado.asStateFlow()

    // Nodos descubiertos activamente por transporte UDP en red local
    private val nodosDetectadosUdp = ConcurrentHashMap<Long, NodoMeshPiloto>()

    // Nodos remotos descubiertos en la misma sala/canal vía Firebase (fuera de rango offline con WiFi/Datos)
    private val nodosDetectadosNube = ConcurrentHashMap<Long, NodoMeshPiloto>()

    // Sala privada dinámica opcional
    private val _salaPrivadaActiva = MutableStateFlow<String?>(null)
    val salaPrivadaActiva: StateFlow<String?> = _salaPrivadaActiva.asStateFlow()

    // Modo Alcabala / Retén Policial: Transmisión en vivo continua encubierta/seguridad
    private val _modoAlcabalaEnVivoActivo = MutableStateFlow(false)
    val modoAlcabalaEnVivoActivo: StateFlow<Boolean> = _modoAlcabalaEnVivoActivo.asStateFlow()

    // Subsistemas internos
    private var buscadorMalla: BuscadorMalla? = null
    private var enrutadorMalla: EnrutadorMalla? = null
    private var transporteUdp: TransporteMallaUdp? = null
    private var audioCasco: AudioCasco? = null
    private var nubeMalla: NubeMalla? = null
    private var buzonTactico: BuzonTacticoMalla? = null
    private var puenteCelular: PuenteCelularMalla? = null
    private var enlaceCopiloto: EnlaceCopiloto? = null

    private var idPilotoLocal: Long = 1001L
    private var aliasPilotoLocal: String = "Piloto TX"
    private var estaInicializado = false
    private var contextoApp: Context? = null

    var fotoPerfilLocal: String = ""
        private set
    var modeloMotoLocal: String = "Keeway TX 200"
        private set
    var fichaLocal: String = ""
        private set

    val modoAltavozActivo: StateFlow<Boolean>
        get() = audioCasco?.modoAltavozActivo ?: MutableStateFlow(true)

    // Estados observables del Intercom Intramoto (Piloto-Copiloto)
    val estadoEnlaceCopiloto: StateFlow<EstadoEnlaceCopiloto>
        get() = enlaceCopiloto?.estadoEnlace ?: MutableStateFlow(EstadoEnlaceCopiloto.DESCONECTADO).asStateFlow()

    val rolEnMoto: StateFlow<RolEnMoto>
        get() = enlaceCopiloto?.rolActual ?: MutableStateFlow(RolEnMoto.SOLO_PILOTO_INDIVIDUAL).asStateFlow()

    val nombreCopilotoConectado: StateFlow<String?>
        get() = enlaceCopiloto?.dispositivoConectadoNombre ?: MutableStateFlow(null).asStateFlow()

    val estaHablandoCopiloto: StateFlow<Boolean>
        get() = enlaceCopiloto?.estaHablandoCopiloto ?: MutableStateFlow(false).asStateFlow()

    val modoEnlaceIntramoto: StateFlow<ModoEnlaceIntramoto>
        get() = enlaceCopiloto?.modoEnlace ?: MutableStateFlow(ModoEnlaceIntramoto.HIBRIDO_TRIMODAL).asStateFlow()

    /**
     * Inicializar los componentes con el contexto de la aplicación e identidad del piloto.
     */
    fun inicializar(
        contexto: Context,
        idPiloto: Long,
        aliasPiloto: String,
        fotoPerfil: String = "",
        modeloMoto: String = "Keeway TX 200",
        ficha: String = ""
    ) {
        val ctx = contexto.applicationContext
        this.contextoApp = ctx
        this.fotoPerfilLocal = fotoPerfil
        if (modeloMoto.isNotBlank()) this.modeloMotoLocal = modeloMoto
        if (ficha.isNotBlank()) this.fichaLocal = ficha

        val prefs = ctx.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
        val guardadoMostrarFoto = prefs.getBoolean("mostrar_foto_perfil", true)
        val guardadoAyudaDatos = prefs.getBoolean("ayuda_con_datos", false)
        val guardadoCng = prefs.getBoolean("cng_ruido_confort", true)
        val guardadoFec = prefs.getBoolean("fec_redundancia", true)
        val guardadoRetardoPtt = prefs.getLong("retardo_fin_ptt", 800L)
        val guardadoBufferJitter = prefs.getInt("tamano_buffer_jitter", 200)
        val guardadoFidelidadAlta = prefs.getBoolean("fidelidad_audio_alta", true)
        val guardadoRogerBeep = prefs.getBoolean("tono_roger_beep", true)
        val guardadoCanalId = prefs.getInt("canal_activo_id", CanalTactico.GENERAL_TX.idCanal)
        val canalRestaurado = CanalTactico.desdeId(guardadoCanalId)

        val guardadoModoEnlace = prefs.getString("modo_enlace_intramoto", ModoEnlaceIntramoto.HIBRIDO_TRIMODAL.name) ?: ModoEnlaceIntramoto.HIBRIDO_TRIMODAL.name
        val modoEnlaceParsed = try { ModoEnlaceIntramoto.valueOf(guardadoModoEnlace) } catch (_: Exception) { ModoEnlaceIntramoto.HIBRIDO_TRIMODAL }
        val guardadoRol = prefs.getString("rol_en_moto", RolEnMoto.SOLO_PILOTO_INDIVIDUAL.name) ?: RolEnMoto.SOLO_PILOTO_INDIVIDUAL.name
        val rolParsed = try { RolEnMoto.valueOf(guardadoRol) } catch (_: Exception) { RolEnMoto.SOLO_PILOTO_INDIVIDUAL }
        val guardadoRetransmitir = prefs.getBoolean("retransmitir_copiloto_caravana", false)
        val guardadoMacCopiloto = prefs.getString("mac_copiloto", "") ?: ""
        val guardadoNombreCopiloto = prefs.getString("nombre_copiloto", "") ?: ""

        _canalActual.value = canalRestaurado

        _ajustes.value = _ajustes.value.copy(
            mostrarFotoPerfil = guardadoMostrarFoto,
            ayudaConDatosFirebase = guardadoAyudaDatos,
            cngRuidoConfort = guardadoCng,
            fecRedundanciaActiva = guardadoFec,
            retardoFinPttMs = guardadoRetardoPtt,
            tamanoBufferJitterMs = guardadoBufferJitter,
            fidelidadAudioAlta = guardadoFidelidadAlta,
            tonoRogerBeep = guardadoRogerBeep,
            canalActivo = canalRestaurado,
            modoEnlacePrivado = modoEnlaceParsed,
            rolEnMoto = rolParsed,
            retransmitirCopilotoACaravana = guardadoRetransmitir,
            macDispositivoCopiloto = guardadoMacCopiloto,
            nombreDispositivoCopiloto = guardadoNombreCopiloto
        )

        if (estaInicializado) {
            if (this.idPilotoLocal == idPiloto && this.aliasPilotoLocal == aliasPiloto) {
                audioCasco?.idPilotoLocal = idPiloto
                enlaceCopiloto?.actualizarIdentidadLocal(idPiloto, aliasPiloto)
                return
            }
            // Limpieza profunda de instancias anteriores para evitar puertos bloqueados o ecos residuales
            transporteUdp?.detenerTransporte()
            audioCasco?.detenerCaptura()
            audioCasco?.purgarColasYDetener()
            enlaceCopiloto?.detenerEnlace()
            buscadorMalla?.detenerExploracion()
            puenteCelular?.desactivarPuenteVoip()
            nodosDetectadosUdp.clear()
            _nodosEnRed.value = emptyList()
        }

        this.idPilotoLocal = idPiloto
        this.aliasPilotoLocal = aliasPiloto

        Log.i(ETIQUETA_LOG, "Inicializando GestorMeshTx para: $aliasPiloto (ID: $idPiloto)")

        // 1. Instanciar Transporte Físico UDP de red local offline (Latencia < 15ms)
        transporteUdp = TransporteMallaUdp(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal,
            alRecibirPaquete = { paquete: PaqueteDatosMesh, remitenteId: Long ->
                // BUG 1 & 2 FIX: Descartar estrictamente cualquier paquete emitido por este mismo dispositivo
                if (paquete.idEmisor == idPilotoLocal || paquete.aliasEmisor.equals(aliasPilotoLocal, ignoreCase = true)) {
                    return@TransporteMallaUdp
                }
                enrutadorMalla?.procesarPaqueteRecibido(paquete, remitenteId)
                // Registrar o actualizar con el alias real del piloto, foto y hardware
                val aliasRecibido = paquete.aliasEmisor.ifBlank { "Piloto TX" }
                var hardwareRecibido = nodosDetectadosUdp[paquete.idEmisor]?.modeloTelefonoHardware ?: "Dispositivo Android"
                var fotoRecibida = nodosDetectadosUdp[paquete.idEmisor]?.fotoUrl ?: ""
                var motoRecibida = nodosDetectadosUdp[paquete.idEmisor]?.nombreMoto ?: "Keeway TX 200"
                var fichaRecibida = nodosDetectadosUdp[paquete.idEmisor]?.fichaMiembro ?: "TX-${(paquete.idEmisor and 0x3FFL)}"
                var canalIdRecibido = paquete.canal
                var nombreCanalRecibido = CanalTactico.desdeId(paquete.canal).nombre
                var salaPrivadaRecibida: String? = null

                if (paquete.tipo == TipoPaqueteMesh.BEACON_DESCUBRIMIENTO && !paquete.payloadTexto.isNullOrBlank()) {
                    val partes = paquete.payloadTexto.split("|")
                    if (partes.isNotEmpty() && partes[0].isNotBlank()) hardwareRecibido = partes[0]
                    if (partes.size > 1) fotoRecibida = partes[1]
                    if (partes.size > 2 && partes[2].isNotBlank()) motoRecibida = partes[2]
                    if (partes.size > 3 && partes[3].isNotBlank()) fichaRecibida = partes[3]
                    if (partes.size > 4 && partes[4].isNotBlank()) canalIdRecibido = partes[4].toIntOrNull() ?: paquete.canal
                    if (partes.size > 5 && partes[5].isNotBlank()) nombreCanalRecibido = partes[5]
                    if (partes.size > 6 && partes[6].isNotBlank()) salaPrivadaRecibida = partes[6]
                }

                val nodoUdp = NodoMeshPiloto(
                    idMiembro = paquete.idEmisor,
                    aliasPiloto = aliasRecibido,
                    modeloTelefonoHardware = hardwareRecibido,
                    nombreMoto = motoRecibida,
                    fichaMiembro = fichaRecibida,
                    fotoUrl = fotoRecibida,
                    direccionNodo = "UDP Red Local",
                    intensidadSenalDbm = -35,
                    distanciaAproximadaMetros = 4.0,
                    ultimoPingTimestamp = System.currentTimeMillis(),
                    idCanalActual = canalIdRecibido,
                    nombreCanalActual = nombreCanalRecibido,
                    salaPrivada = salaPrivadaRecibida
                )
                nodosDetectadosUdp[paquete.idEmisor] = nodoUdp
                enrutadorMalla?.actualizarVecinoDirecto(nodoUdp)
                actualizarListaNodos()
            }
        )

        // 2. Instanciar Enrutador Multisalto basado en Modelo Actor
        enrutadorMalla = EnrutadorMalla(
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal,
            alEnviarPaqueteFisico = { paquete, nodoDestino ->
                val destinoStr = nodoDestino?.aliasPiloto ?: "BROADCAST_UNIVERSAL"
                if (paquete.tipo == TipoPaqueteMesh.AUDIO_VOZ_OPUS) {
                    Log.i(ETIQUETA_LOG, "Emitiendo paquete AUDIO hacia $destinoStr por UDP")
                }
                transporteUdp?.transmitirPaquete(
                    paquete,
                    if (nodoDestino != null && nodoDestino.direccionNodo.contains(".")) nodoDestino.direccionNodo else null
                )
            }
        )

        // 3. Instanciar Buscador Híbrido (Wi-Fi Direct P2P + Wi-Fi Aware + BLE)
        buscadorMalla = BuscadorMalla(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal,
            alDetectarNodo = { nodo ->
                if (!esMismoDispositivo(nodo)) {
                    enrutadorMalla?.actualizarVecinoDirecto(nodo)
                    actualizarListaNodos()
                    // Despacho asíncrono táctico al detectar compañeros en rango físico
                    buzonTactico?.notificarNuevoCompaneroEnRango { paquetePendiente ->
                        transporteUdp?.transmitirPaquete(paquetePendiente)
                    }
                }
            },
            alPerderNodo = { idNodo ->
                enrutadorMalla?.removerVecinoDirecto(idNodo)
                actualizarListaNodos()
            }
        ).apply {
            alActualizarConexionP2p = { esPropietario, ipPropietario ->
                if (ipPropietario.isNotBlank()) {
                    transporteUdp?.registrarIpP2p(0L, ipPropietario)
                }
            }
        }

        // 4. Instanciar Motor de Audio de Casco (PTT, VOX, Filtro Viento, Bluetooth SCO & Altavoz)
        audioCasco = AudioCasco(
            contexto = ctx,
            alGenerarFragmentoVoz = { bytesAudio ->
                // Transmitir al copiloto si está conectado por Bluetooth clásico (Intercom Intramoto)
                enlaceCopiloto?.enviarAudioCopiloto(bytesAudio)

                // Si el modo intramoto es SOLO_BLUETOOTH, la comunicación es privada y ahorra batería (no va a la malla exterior)
                if (_ajustes.value.modoEnlacePrivado == ModoEnlaceIntramoto.SOLO_BLUETOOTH) {
                    return@AudioCasco
                }

                // BUG 3 FIX: Transmitir a la malla únicamente si está activa (botón Play encendido)
                if (_estadoConexion.value == MeshEstadoConexion.DESCONECTADO) return@AudioCasco
                val canalEfectivo = obtenerCanalIdEfectivo()
                // Enviar fragmento de voz capturado a la malla táctica local (UDP)
                enrutadorMalla?.transmitirPaquete(
                    tipo = TipoPaqueteMesh.AUDIO_VOZ_OPUS,
                    payloadAudio = bytesAudio
                )
                // Si la Ayuda con Datos (Firebase) está activa, retransmitir por el puente celular
                if (_ajustes.value.ayudaConDatosFirebase) {
                    puenteCelular?.retransmitirAudioPorPuente(
                        canal = canalEfectivo,
                        payloadAudio = bytesAudio
                    )
                }
            }
        ).apply {
            idPilotoLocal = idPiloto
            tamanoBufferJitterMs = _ajustes.value.tamanoBufferJitterMs
            estaEnlaceActivo = (_estadoConexion.value != MeshEstadoConexion.DESCONECTADO)
            supresionEcoActivada = _ajustes.value.supresionEcoAcustico
            bufferAntiEntrecorteActivado = _ajustes.value.bufferAntiEntrecorte
            cngRuidoConfortHabilitado = _ajustes.value.cngRuidoConfort
            fecHabilitado = _ajustes.value.fecRedundanciaActiva
        }

        // Observar estado del casco Bluetooth
        alcanceGestor.launch {
            audioCasco?.dispositivoBluetoothCascoConectado?.collect { conectado ->
                _cascoBluetoothConectado.value = conectado
            }
        }

        // 5. Instanciar Nube Pasiva (telemetría) y Buzón Táctico Asíncrono (Store & Forward Híbrido)
        nubeMalla = NubeMalla(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal
        )

        buzonTactico = BuzonTacticoMalla(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal,
            alDescargarMensajeVozRemoto = { payloadAudio, idEmisor, aliasEmisor ->
                if (_estadoConexion.value != MeshEstadoConexion.DESCONECTADO && idEmisor != idPilotoLocal) {
                    _pilotoHablandoAhora.value = "$aliasEmisor (Buzón Táctico)"
                    _idPilotoHablandoAhora.value = idEmisor
                    actualizarListaNodos()
                    audioCasco?.encolarAudioEntrante(payloadAudio, idEmisor)
                }
            }
        )

        // 6. Instanciar Puente Celular VoIP Fallback en Firebase
        puenteCelular = PuenteCelularMalla(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal,
            modeloMotoLocal = modeloMotoLocal,
            fichaLocal = fichaLocal,
            fotoUrlLocal = fotoPerfilLocal,
            alRecibirAudioDesdePuente = { audioBytes, emisorId, emisorAlias, canalId ->
                // Solo recibir si la malla está activa, no es eco propio y coincide el canal
                if (_estadoConexion.value != MeshEstadoConexion.DESCONECTADO && emisorId != idPilotoLocal) {
                    val canalEsperado = obtenerCanalIdEfectivo()
                    if (canalId == canalEsperado) {
                        _pilotoHablandoAhora.value = "$emisorAlias (Puente 4G)"
                        _idPilotoHablandoAhora.value = emisorId
                        actualizarListaNodos()
                        audioCasco?.encolarAudioEntrante(audioBytes, emisorId)
                    }
                }
            },
            alDetectarPilotoNube = { nodo ->
                if (!esMismoDispositivo(nodo)) {
                    nodosDetectadosNube[nodo.idMiembro] = nodo
                    actualizarListaNodos()
                }
            },
            alPerderPilotoNube = { idPiloto ->
                nodosDetectadosNube.remove(idPiloto)
                actualizarListaNodos()
            }
        ).apply {
            actualizarNombreCanal(_canalActual.value.nombre)
            actualizarSalaPrivada(_salaPrivadaActiva.value)
        }
        // BUG 3 FIX: Inicialmente desactivado. Solo se activa al encender la malla con el botón Play
        puenteCelular?.setMallaActiva(_estadoConexion.value != MeshEstadoConexion.DESCONECTADO)
        if (_ajustes.value.ayudaConDatosFirebase && _estadoConexion.value != MeshEstadoConexion.DESCONECTADO) {
            puenteCelular?.activarPuenteVoip(obtenerCanalIdEfectivo())
        }

        // 7. Instanciar Enlace Copiloto Bluetooth Clásico (Intercom Intramoto)
        enlaceCopiloto = EnlaceCopiloto(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal,
            alRecibirAudioCopiloto = { payloadAudio, idEmisor, aliasEmisor ->
                _pilotoHablandoAhora.value = "$aliasEmisor (Copiloto BT)"
                _idPilotoHablandoAhora.value = idEmisor
                actualizarListaNodos()
                audioCasco?.encolarAudioEntrante(payloadAudio, idEmisor)

                // El Piloto como Gateway: Si está habilitada la retransmisión a la caravana,
                // reenviar la voz del copiloto por Wi-Fi Direct y 4G
                if (_ajustes.value.retransmitirCopilotoACaravana &&
                    _ajustes.value.rolEnMoto == RolEnMoto.PILOTO_GATEWAY &&
                    _estadoConexion.value != MeshEstadoConexion.DESCONECTADO
                ) {
                    enrutadorMalla?.transmitirPaquete(
                        tipo = TipoPaqueteMesh.AUDIO_VOZ_OPUS,
                        payloadAudio = payloadAudio
                    )
                    if (_ajustes.value.ayudaConDatosFirebase) {
                        puenteCelular?.retransmitirAudioPorPuente(
                            canal = obtenerCanalIdEfectivo(),
                            payloadAudio = payloadAudio
                        )
                    }
                }
            }
        ).apply {
            cambiarModoEnlace(_ajustes.value.modoEnlacePrivado)
            alternarRetransmitirACaravana(_ajustes.value.retransmitirCopilotoACaravana)
        }

        if (_ajustes.value.rolEnMoto == RolEnMoto.PILOTO_GATEWAY) {
            enlaceCopiloto?.iniciarComoPilotoServidor()
        } else if (_ajustes.value.rolEnMoto == RolEnMoto.COPILOTO_ENLACE && _ajustes.value.macDispositivoCopiloto.isNotBlank()) {
            enlaceCopiloto?.iniciarComoCopilotoCliente(_ajustes.value.macDispositivoCopiloto)
        }

        // 8. Escuchar paquetes entrantes dirigidos a este nodo desde la malla
        alcanceGestor.launch {
            enrutadorMalla?.paquetesEntrantes?.collect { paquete ->
                procesarPaqueteEntrante(paquete)
            }
        }

        estaInicializado = true
    }

    /**
     * Inicia la conexión táctica a la malla: activa búsqueda de nodos y captura de audio.
     */
    fun iniciarMallaTactico() {
        if (_estadoConexion.value != MeshEstadoConexion.DESCONECTADO) return
        _estadoConexion.value = MeshEstadoConexion.ESCANEANDO
        Log.i(ETIQUETA_LOG, "Iniciando Malla Táctica TX...")

        audioCasco?.estaEnlaceActivo = true
        puenteCelular?.setMallaActiva(true)
        if (_ajustes.value.ayudaConDatosFirebase) {
            puenteCelular?.activarPuenteVoip(obtenerCanalIdEfectivo())
        }

        contextoApp?.let { ServicioMallaTx.iniciar(it) }
        buscadorMalla?.iniciarExploracion()
        transporteUdp?.iniciarTransporte()
        audioCasco?.iniciarCaptura()
    }

    /**
     * Detiene la transmisión y recepción de la malla táctica para ahorro de batería.
     */
    fun detenerMallaTactico() {
        _estadoConexion.value = MeshEstadoConexion.DESCONECTADO
        _estaTransmitiendoPtt.value = false
        _pilotoHablandoAhora.value = null
        _idPilotoHablandoAhora.value = null
        trabajoCierrePtt?.cancel()
        trabajoCierrePtt = null
        Log.i(ETIQUETA_LOG, "Deteniendo Malla Táctica TX...")

        audioCasco?.estaEnlaceActivo = false
        audioCasco?.purgarColasYDetener()
        audioCasco?.detenerCaptura()
        puenteCelular?.setMallaActiva(false)
        puenteCelular?.desactivarPuenteVoip()
        buscadorMalla?.detenerExploracion()
        transporteUdp?.detenerTransporte()
        nodosDetectadosUdp.clear()
        _nodosEnRed.value = emptyList()
        contextoApp?.let { ServicioMallaTx.detener(it) }
    }

    /**
     * Cambiar el canal táctico de radiofrecuencia virtual.
     */
    fun cambiarCanal(nuevoCanal: CanalTactico) {
        _canalActual.value = nuevoCanal
        enrutadorMalla?.canalActivo = nuevoCanal
        _ajustes.value = _ajustes.value.copy(canalActivo = nuevoCanal)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putInt("canal_activo_id", nuevoCanal.idCanal)?.apply()
        puenteCelular?.actualizarNombreCanal(nuevoCanal.nombre)
        if (_estadoConexion.value != MeshEstadoConexion.DESCONECTADO && _ajustes.value.ayudaConDatosFirebase) {
            puenteCelular?.activarPuenteVoip(nuevoCanal.idCanal)
        }
        Log.i(ETIQUETA_LOG, "Canal táctico cambiado a: ${nuevoCanal.nombre}")
    }

    private var trabajoCierrePtt: Job? = null

    /**
     * Pulsar o soltar el botón PTT (Push-to-Talk) en pantalla o manubrio con algoritmo antirrebote táctil
     * y retardo de cola (hang-time) para no cortar las últimas palabras.
     */
    fun setTransmitiendoPtt(transmitiendo: Boolean) {
        if (_estadoConexion.value == MeshEstadoConexion.DESCONECTADO) {
            Log.w(ETIQUETA_LOG, "PTT bloqueado: El intercomunicador está desconectado. Presiona Play primero.")
            return
        }
        if (transmitiendo) {
            trabajoCierrePtt?.cancel()
            trabajoCierrePtt = null
            if (!_estaTransmitiendoPtt.value) {
                _estaTransmitiendoPtt.value = true
                audioCasco?.setPttPresionado(true)
                _pilotoHablandoAhora.value = "Tú ($aliasPilotoLocal)"
                Log.d(ETIQUETA_LOG, "🎙️ Transmisión PTT iniciada (Push-To-Talk ACTIVO)")
            }
        } else {
            trabajoCierrePtt?.cancel()
            trabajoCierrePtt = alcanceGestor.launch {
                // BUG 6 FIX: Retardo de fin de PTT (hang-time) para terminar de vaciar el buffer del micrófono
                val retardo = _ajustes.value.retardoFinPttMs.coerceAtLeast(300L)
                delay(retardo)
                _estaTransmitiendoPtt.value = false
                audioCasco?.setPttPresionado(false)
                if (_ajustes.value.tonoRogerBeep) {
                    audioCasco?.reproducirRogerBeepFin()
                }
                if (_pilotoHablandoAhora.value?.contains(aliasPilotoLocal) == true) {
                    _pilotoHablandoAhora.value = null
                }
                Log.d(ETIQUETA_LOG, "🛑 Transmisión PTT finalizada tras retardo de cola de $retardo ms")
            }
        }
    }

    /**
     * Entrar a una sala privada cifrada virtual por nombre o PIN de 4 dígitos.
     */
    fun entrarASalaPrivada(nombreOPin: String) {
        val claveLimpia = nombreOPin.trim()
        if (claveLimpia.isNotBlank()) {
            _salaPrivadaActiva.value = claveLimpia
            val hashId = Math.abs(claveLimpia.hashCode()) % 60000 + 100
            enrutadorMalla?.canalActivoIdPersonalizado = hashId
            puenteCelular?.actualizarSalaPrivada(claveLimpia)
            puenteCelular?.actualizarNombreCanal("Sala Privada: $claveLimpia")
            if (_estadoConexion.value != MeshEstadoConexion.DESCONECTADO && _ajustes.value.ayudaConDatosFirebase) {
                puenteCelular?.activarPuenteVoip(hashId)
            }
            Log.i(ETIQUETA_LOG, "Entrando a Sala Privada: $claveLimpia (ID Virtual: $hashId)")
        }
    }

    /**
     * Salir de la sala privada y regresar al Canal General TX.
     */
    fun salirASalaGeneral() {
        _salaPrivadaActiva.value = null
        enrutadorMalla?.canalActivoIdPersonalizado = null
        puenteCelular?.actualizarSalaPrivada(null)
        puenteCelular?.actualizarNombreCanal(CanalTactico.GENERAL_TX.nombre)
        cambiarCanal(CanalTactico.GENERAL_TX)
        Log.i(ETIQUETA_LOG, "Salida de sala privada. Regresando a Canal General TX")
    }

    /**
     * Activar sala de Rodadas & Convoy (Canal 2).
     */
    fun activarCanalRodadas() {
        salirASalaGeneral()
        cambiarCanal(CanalTactico.CARAVANA_CONVOY)
        Log.i(ETIQUETA_LOG, "Modo Rodadas activado: Canal Caravana & Convoy")
    }

    /**
     * Cambiar modo entre PTT manual y VOX manos libres.
     */
    fun alternarModoPtt(activarPtt: Boolean) {
        _ajustes.value = _ajustes.value.copy(modoPtt = activarPtt)
        audioCasco?.modoPttActivo = activarPtt
    }

    /**
     * Calibrar sensibilidad del detector VOX.
     */
    fun ajustarSensibilidadVox(sensibilidad: Float) {
        _ajustes.value = _ajustes.value.copy(umbralVoxSensibilidad = sensibilidad)
        audioCasco?.sensibilidadUmbralVox = sensibilidad
    }

    /**
     * Alternar filtro digital de ruido de viento.
     */
    fun alternarFiltroViento(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(cancelacionRuidoViento = activar)
        audioCasco?.cancelacionVientoActivada = activar
    }

    /**
     * Alternar cancelación activa de eco acústico (AEC por hardware + compuerta software).
     */
    fun alternarSupresionEco(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(supresionEcoAcustico = activar)
        audioCasco?.supresionEcoActivada = activar
        Log.i(ETIQUETA_LOG, "Supresión de eco acústico cambiada a: $activar")
    }

    /**
     * Alternar búfer adaptativo anti-entrecorte (Jitter Buffer + Pre-roll + PLC).
     */
    fun alternarBufferAntiEntrecorte(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(bufferAntiEntrecorte = activar)
        audioCasco?.bufferAntiEntrecorteActivado = activar
        Log.i(ETIQUETA_LOG, "Búfer adaptativo anti-entrecorte cambiado a: $activar")
    }

    /**
     * Alternar el modo de visualización de nodos (perfil sincronizado vs dispositivo técnico).
     */
    fun alternarVisualizacionPerfil(mostrarPerfil: Boolean) {
        _ajustes.value = _ajustes.value.copy(mostrarPerfilSincronizado = mostrarPerfil)
    }

    /**
     * Actualiza los datos del perfil local (foto carnet/sincronizada, alias, modelo moto, ficha).
     */
    fun actualizarPerfilLocal(
        alias: String,
        fotoPerfil: String,
        modeloMoto: String = "Keeway TX 200",
        ficha: String = ""
    ) {
        if (alias.isNotBlank()) this.aliasPilotoLocal = alias
        this.fotoPerfilLocal = fotoPerfil
        if (modeloMoto.isNotBlank()) this.modeloMotoLocal = modeloMoto
        if (ficha.isNotBlank()) this.fichaLocal = ficha
        transporteUdp?.actualizarAliasLocal(this.aliasPilotoLocal)
        puenteCelular?.actualizarPerfil(this.modeloMotoLocal, this.fichaLocal, this.fotoPerfilLocal)
        actualizarListaNodos()
    }

    /**
     * Alternar visibilidad de foto de perfil en la malla y persistir preferencia.
     */
    fun alternarMostrarFotoPerfil(mostrar: Boolean) {
        _ajustes.value = _ajustes.value.copy(mostrarFotoPerfil = mostrar)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("mostrar_foto_perfil", mostrar)?.apply()
        actualizarListaNodos()
    }

    /**
     * Retorna si este nodo debe compartir su foto de perfil con los demás pilotos.
     */
    fun debeCompartirFotoPerfil(): Boolean = _ajustes.value.mostrarFotoPerfil

    /**
     * Alternar Ayuda con Datos (Firebase Sync) para enlazar grupos y cobertura extendida.
     */
    fun alternarAyudaConDatos(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(ayudaConDatosFirebase = activar)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("ayuda_con_datos", activar)?.apply()
        if (activar) {
            puenteCelular?.activarPuenteVoip(_ajustes.value.canalActivo.idCanal)
        } else {
            puenteCelular?.desactivarPuenteVoip()
        }
        Log.i(ETIQUETA_LOG, "Ayuda con Datos (Firebase Sync) cambiada a: $activar")
    }

    /**
     * Alternar Generador de Ruido de Confort térmico analógico (-48 dBFS).
     */
    fun alternarCngRuidoConfort(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(cngRuidoConfort = activar)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("cng_ruido_confort", activar)?.apply()
        audioCasco?.cngRuidoConfortHabilitado = activar
        Log.i(ETIQUETA_LOG, "Ruido de confort CNG cambiado a: $activar")
    }

    /**
     * Alternar redundancia FEC intrapaquete (N + N-1).
     */
    fun alternarFecRedundancia(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(fecRedundanciaActiva = activar)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("fec_redundancia", activar)?.apply()
        audioCasco?.fecHabilitado = activar
        Log.i(ETIQUETA_LOG, "Redundancia FEC cambiada a: $activar")
    }

    /**
     * Ajustar retardo de cola (hang-time) al soltar PTT para no cortar las últimas palabras.
     */
    fun ajustarRetardoFinPtt(retardoMs: Long) {
        val valorSeguro = retardoMs.coerceIn(300L, 2500L)
        _ajustes.value = _ajustes.value.copy(retardoFinPttMs = valorSeguro)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putLong("retardo_fin_ptt", valorSeguro)?.apply()
        Log.i(ETIQUETA_LOG, "Retardo fin PTT ajustado a: $valorSeguro ms")
    }

    /**
     * Ajustar tamaño del búfer Jitter anti-entrecorte (120ms rápido, 200ms recomendado, 320ms anti-pérdida).
     */
    fun ajustarBufferJitter(bufferMs: Int) {
        val valorSeguro = bufferMs.coerceIn(80, 500)
        _ajustes.value = _ajustes.value.copy(tamanoBufferJitterMs = valorSeguro)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putInt("tamano_buffer_jitter", valorSeguro)?.apply()
        audioCasco?.tamanoBufferJitterMs = valorSeguro
        Log.i(ETIQUETA_LOG, "Búfer Jitter ajustado a: $valorSeguro ms")
    }

    /**
     * Alternar fidelidad de audio entre Opus HD (16 kHz) y Ultra-comprimido (8 kHz).
     */
    fun alternarFidelidadAudio(altaFidelidad: Boolean) {
        _ajustes.value = _ajustes.value.copy(fidelidadAudioAlta = altaFidelidad)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("fidelidad_audio_alta", altaFidelidad)?.apply()
        Log.i(ETIQUETA_LOG, "Fidelidad de audio alta cambiada a: $altaFidelidad")
    }

    /**
     * Alternar tono táctico Roger Beep de fin de transmisión.
     */
    fun alternarTonoRogerBeep(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(tonoRogerBeep = activar)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("tono_roger_beep", activar)?.apply()
        Log.i(ETIQUETA_LOG, "Tono Roger Beep cambiado a: $activar")
    }

    /**
     * Devuelve el ID de canal efectivo (si hay sala privada activa usa su hash, sino el canal táctico).
     */
    fun obtenerCanalIdEfectivo(): Int {
        return _salaPrivadaActiva.value?.let { sala ->
            Math.abs(sala.hashCode()) % 60000 + 100
        } ?: _canalActual.value.idCanal
    }

    /**
     * Alternar el modo de enlace intramoto (Solo Bluetooth, Solo Wi-Fi, o Híbrido Trimodal).
     */
    fun alternarModoEnlaceIntramoto(modo: ModoEnlaceIntramoto) {
        _ajustes.value = _ajustes.value.copy(modoEnlacePrivado = modo)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putString("modo_enlace_intramoto", modo.name)?.apply()
        enlaceCopiloto?.cambiarModoEnlace(modo)
        Log.i(ETIQUETA_LOG, "Modo de enlace intramoto cambiado a: ${modo.titulo}")
    }

    /**
     * Conmutar el rol en la moto (Piloto Gateway, Copiloto Acompañante, o Piloto Individual).
     */
    fun alternarRolEnMoto(rol: RolEnMoto) {
        _ajustes.value = _ajustes.value.copy(rolEnMoto = rol)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putString("rol_en_moto", rol.name)?.apply()
        when (rol) {
            RolEnMoto.PILOTO_GATEWAY -> iniciarModoPilotoGateway()
            RolEnMoto.COPILOTO_ENLACE -> {
                if (_ajustes.value.macDispositivoCopiloto.isNotBlank()) {
                    iniciarModoCopiloto(_ajustes.value.macDispositivoCopiloto)
                }
            }
            RolEnMoto.SOLO_PILOTO_INDIVIDUAL -> desconectarEnlaceCopiloto()
        }
    }

    /**
     * Alternar si la voz del copiloto se retransmite a la caravana por Wi-Fi Direct y Firebase.
     */
    fun alternarRetransmitirCopilotoCaravana(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(retransmitirCopilotoACaravana = activar)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putBoolean("retransmitir_copiloto_caravana", activar)?.apply()
        enlaceCopiloto?.alternarRetransmitirACaravana(activar)
    }

    /**
     * Guarda el dispositivo del copiloto/piloto emparejado seleccionado.
     */
    fun configurarDispositivoCopiloto(nombre: String, mac: String) {
        _ajustes.value = _ajustes.value.copy(nombreDispositivoCopiloto = nombre, macDispositivoCopiloto = mac)
        contextoApp?.getSharedPreferences("meshtx_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putString("nombre_copiloto", nombre)?.putString("mac_copiloto", mac)?.apply()
        if (_ajustes.value.rolEnMoto == RolEnMoto.COPILOTO_ENLACE && mac.isNotBlank()) {
            iniciarModoCopiloto(mac)
        }
    }

    /**
     * Inicia el modo Piloto Gateway para esperar al copiloto por Bluetooth.
     */
    fun iniciarModoPilotoGateway() {
        enlaceCopiloto?.iniciarComoPilotoServidor()
    }

    /**
     * Inicia la conexión Bluetooth cliente hacia el teléfono del piloto.
     */
    fun iniciarModoCopiloto(macPiloto: String) {
        enlaceCopiloto?.iniciarComoCopilotoCliente(macPiloto)
    }

    /**
     * Detiene el enlace Bluetooth intramoto.
     */
    fun desconectarEnlaceCopiloto() {
        enlaceCopiloto?.detenerEnlace()
    }

    /**
     * Obtiene la lista de dispositivos Bluetooth emparejados en el sistema.
     */
    fun obtenerDispositivosBluetoothEmparejados(): List<Pair<String, String>> {
        return enlaceCopiloto?.obtenerDispositivosEmparejados() ?: emptyList()
    }

    /**
     * Emitir alerta de auxilio vial inmediato (SOS) a toda la caravana con máxima prioridad.
     */
    fun emitirAlertaSosMalla(mensajeSos: String) {
        emitirAlertaSos(mensajeSos, null)
    }

    /**
     * Emisión de alerta SOS con soporte opcional de coordenadas GPS para el Gateway de nube.
     */
    fun emitirAlertaSos(mensajeSos: String, coordenadasGps: String? = null) {
        val paqueteSos = PaqueteDatosMesh(
            idPaquete = (System.currentTimeMillis() shl 16) or (idPilotoLocal and 0xFFFFL),
            idEmisor = idPilotoLocal,
            aliasEmisor = aliasPilotoLocal,
            canal = CanalTactico.EMERGENCIA_SOS.idCanal,
            tipo = TipoPaqueteMesh.PAQUETE_SOS,
            payloadTexto = mensajeSos
        )
        enrutadorMalla?.transmitirPaquete(
            tipo = TipoPaqueteMesh.PAQUETE_SOS,
            payloadTexto = mensajeSos
        )
        // Store & Forward: despachar a la nube si hay internet o encolar para cuando aparezca señal
        nubeMalla?.procesarAlertaSosMalla(paqueteSos, coordenadasGps)
        nubeMalla?.registrarEventoTelemetria(
            canalUsado = CanalTactico.EMERGENCIA_SOS.idCanal,
            totalVecinosAlcanzados = _nodosEnRed.value.size,
            huboAlertaSos = true
        )
        Log.w(ETIQUETA_LOG, "🚨 ALERTA SOS EMITIDA EN MALLA: $mensajeSos (${coordenadasGps ?: "Sin GPS"})")
    }

    /**
     * Alternar modo altavoz potente para escuchar por el altavoz exterior sin auriculares.
     */
    fun alternarModoAltavoz(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(modoAltavozActivo = activar)
        audioCasco?.configurarModoAltavoz(activar)
        Log.i(ETIQUETA_LOG, "Modo Altavoz Táctico cambiado a: $activar")
    }

    /**
     * Alternar el botón flotante PTT movible en pantalla (Overlay fuera de Mesh TX).
     */
    fun alternarBotonFlotantePtt(activar: Boolean) {
        _ajustes.value = _ajustes.value.copy(botonFlotantePttActivo = activar)
        Log.i(ETIQUETA_LOG, "Botón Flotante PTT cambiado a: $activar")
    }

    /**
     * Activa el Protocolo de Seguridad en Alcabala / Retén Policial:
     * 1. Enciende la malla Mesh TX si estaba apagada.
     * 2. Conmuta automáticamente al Canal de Emergencia SOS (o Caravana).
     * 3. Difunde alerta prioritaria en la malla y en la nube Firebase (si hay datos móviles).
     * 4. Activa la transmisión continua de audio en vivo del micrófono sin soltar para que
     *    los compañeros y la directiva escuchen todo el procedimiento policial en tiempo real.
     */
    fun activarModoAlcabalaSos(detalles: String = "Alcabala Policial / Retén en Vía") {
        _modoAlcabalaEnVivoActivo.value = true
        iniciarMallaTactico()
        cambiarCanal(CanalTactico.EMERGENCIA_SOS)
        emitirAlertaSosMalla("🚨 PROTOCOLO ALCABALA POLICIAL - TRANSMISIÓN DE AUDIO EN VIVO: $detalles")
        // Iniciar transmisión continua de audio
        trabajoCierrePtt?.cancel()
        _estaTransmitiendoPtt.value = true
        audioCasco?.iniciarCapturaAudio()
        Log.w(ETIQUETA_LOG, "🚨 MODO ALCABALA ACTIVADO: Transmitiendo audio en vivo continuo por Mesh y Datos")
    }

    /**
     * Desactiva la transmisión continua del modo alcabala.
     */
    fun desactivarModoAlcabalaSos() {
        _modoAlcabalaEnVivoActivo.value = false
        _estaTransmitiendoPtt.value = false
        audioCasco?.detenerCapturaAudio()
        cambiarCanal(CanalTactico.GENERAL_TX)
        Log.i(ETIQUETA_LOG, "Modo Alcabala desactivado. Retornando a canal general.")
    }

    /**
     * Resuelve el perfil enriquecido de un compañero (moto, foto) precargado en la nube.
     */
    fun resolverPerfilPiloto(idPiloto: Long): NubeMalla.InfoPilotoCache? {
        return nubeMalla?.obtenerPerfilPiloto(idPiloto)
    }

    /**
     * Liberar recursos y cerrar hardware de audio al cerrar la sesión táctica.
     */
    fun liberar() {
        detenerMallaTactico()
        transporteUdp?.detenerTransporte()
        audioCasco?.liberar()
        enlaceCopiloto?.liberar()
        nubeMalla?.liberar()
        buzonTactico?.liberar()
        puenteCelular?.liberar()
        estaInicializado = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS DE GESTIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private fun procesarPaqueteEntrante(paquete: PaqueteDatosMesh) {
        // BUG 1 & 3 FIX: Descartar si el intercomunicador está apagado o si es eco de este mismo dispositivo
        if (_estadoConexion.value == MeshEstadoConexion.DESCONECTADO) return
        if (paquete.idEmisor == idPilotoLocal || paquete.aliasEmisor.equals(aliasPilotoLocal, ignoreCase = true)) return
        if (_estaTransmitiendoPtt.value) return // Half-duplex: silenciar recepción durante la propia transmisión PTT

        when (paquete.tipo) {
            TipoPaqueteMesh.AUDIO_VOZ_OPUS -> {
                // BUG 5 FIX: Aislamiento estricto de canales
                val canalEsperado = obtenerCanalIdEfectivo()
                if (paquete.canal != canalEsperado) {
                    Log.d(ETIQUETA_LOG, "Paquete de voz descartado por canal diferente (Paquete: ${paquete.canal}, Esperado: $canalEsperado)")
                    return
                }
                paquete.payloadAudio?.let { audio ->
                    _pilotoHablandoAhora.value = paquete.aliasEmisor
                    _idPilotoHablandoAhora.value = paquete.idEmisor
                    actualizarListaNodos()
                    audioCasco?.encolarAudioEntrante(audio, paquete.idEmisor)
                    _totalSaltosRelay.value = paquete.saltosRelay

                    // Restablecer indicador de orador después de 1.5 segundos sin paquetes
                    alcanceGestor.launch {
                        delay(1500)
                        if (_idPilotoHablandoAhora.value == paquete.idEmisor) {
                            _idPilotoHablandoAhora.value = null
                            _pilotoHablandoAhora.value = null
                            actualizarListaNodos()
                        }
                    }
                }
            }
            TipoPaqueteMesh.PAQUETE_SOS -> {
                Log.w(ETIQUETA_LOG, "🚨 ALERTA SOS RECIBIDA de ${paquete.aliasEmisor}: ${paquete.payloadTexto}")
                // Actuar como Gateway híbrido: si este teléfono tiene señal, dispara la alerta a Firebase
                nubeMalla?.procesarAlertaSosMalla(paquete)
            }
            TipoPaqueteMesh.TELEMETRIA_POSICION,
            TipoPaqueteMesh.BEACON_DESCUBRIMIENTO,
            TipoPaqueteMesh.TEXTO_RAPIDO -> {
                // Manejo de telemetría y mensajes rápidos
            }
        }
    }

    private fun esMismoDispositivo(nodo: NodoMeshPiloto): Boolean {
        if (nodo.idMiembro == idPilotoLocal) return true
        if (aliasPilotoLocal.isNotBlank() && nodo.aliasPiloto.equals(aliasPilotoLocal, ignoreCase = true)) return true
        if (fichaLocal.isNotBlank() && nodo.fichaMiembro.equals(fichaLocal, ignoreCase = true)) return true
        return false
    }

    private fun actualizarListaNodos() {
        val ahora = System.currentTimeMillis()
        // Purgar nodos UDP viejos (> 90 segundos) o si corresponden a este mismo dispositivo
        val iterador = nodosDetectadosUdp.entries.iterator()
        while (iterador.hasNext()) {
            val entrada = iterador.next()
            if (ahora - entrada.value.ultimoPingTimestamp > 90_000L || esMismoDispositivo(entrada.value)) {
                iterador.remove()
            }
        }

        val mapaCombinado = mutableMapOf<Long, NodoMeshPiloto>()

        // 1. Nodos de BLE / WiFi Direct (BUG 2 FIX: Excluir al propio usuario del radar)
        buscadorMalla?.nodosDetectados?.value?.values?.forEach { nodo ->
            if (!esMismoDispositivo(nodo)) {
                mapaCombinado[nodo.idMiembro] = nodo
            }
        }

        // 2. Nodos de UDP / Broadcast / Malla Local (BUG 2 FIX: Excluir al propio usuario del radar)
        nodosDetectadosUdp.values.forEach { nodo ->
            if (!esMismoDispositivo(nodo)) {
                val existente = mapaCombinado[nodo.idMiembro]
                if (existente == null || nodo.ultimoPingTimestamp >= existente.ultimoPingTimestamp) {
                    mapaCombinado[nodo.idMiembro] = nodo
                }
            }
        }

        // 3. Nodos remotos en la misma sala/canal detectados vía Firebase (Fuera de rango offline con datos/WiFi)
        val iterNube = nodosDetectadosNube.entries.iterator()
        while (iterNube.hasNext()) {
            val entrada = iterNube.next()
            if (ahora - entrada.value.ultimoPingTimestamp > 60_000L || esMismoDispositivo(entrada.value)) {
                iterNube.remove()
            }
        }
        nodosDetectadosNube.values.forEach { nodo ->
            if (!esMismoDispositivo(nodo)) {
                val existente = mapaCombinado[nodo.idMiembro]
                if (existente == null) {
                    mapaCombinado[nodo.idMiembro] = nodo
                }
            }
        }

        val lista = mapaCombinado.values.toList()
        val hablandoId = _idPilotoHablandoAhora.value
        val hablandoAlias = _pilotoHablandoAhora.value
        val listaEnriquecida = lista.map { nodo ->
            val perfil = nubeMalla?.obtenerPerfilPiloto(nodo.idMiembro)
            val estaHablando = (hablandoId != null && nodo.idMiembro == hablandoId) ||
                               (!hablandoAlias.isNullOrBlank() && nodo.aliasPiloto.isNotBlank() && nodo.aliasPiloto == hablandoAlias)
            val fotoFinal = when {
                perfil != null && perfil.fotoUrl.isNotBlank() -> perfil.fotoUrl
                nodo.fotoUrl.isNotBlank() -> nodo.fotoUrl
                else -> ""
            }
            val aliasFinal = when {
                perfil != null && perfil.alias.isNotBlank() -> perfil.alias
                nodo.aliasPiloto.isNotBlank() -> nodo.aliasPiloto
                else -> "Piloto TX"
            }
            val motoFinal = when {
                perfil != null && perfil.modeloMoto.isNotBlank() -> perfil.modeloMoto
                nodo.nombreMoto.isNotBlank() -> nodo.nombreMoto
                else -> "Keeway TX 200"
            }
            val fichaFinal = when {
                nodo.fichaMiembro.isNotBlank() -> nodo.fichaMiembro
                else -> "TX-${(nodo.idMiembro and 0x3FFL)}"
            }

            nodo.copy(
                aliasPiloto = aliasFinal,
                nombreMoto = motoFinal,
                fotoUrl = fotoFinal,
                fichaMiembro = fichaFinal,
                modeloTelefonoHardware = nodo.modeloTelefonoHardware.ifBlank { aliasFinal },
                estaTransmitiendoVoz = estaHablando,
                idCanalActual = nodo.idCanalActual,
                nombreCanalActual = nodo.nombreCanalActual,
                salaPrivada = nodo.salaPrivada
            )
        }
        _nodosEnRed.value = listaEnriquecida
        audioCasco?.tieneEnlaceFisicoActivo = listaEnriquecida.isNotEmpty()

        // Actualizar estado general de la malla
        if (_estadoConexion.value != MeshEstadoConexion.DESCONECTADO) {
            _estadoConexion.value = when {
                listaEnriquecida.isEmpty() -> MeshEstadoConexion.ESCANEANDO
                listaEnriquecida.size == 1 -> MeshEstadoConexion.ENLACE_DIRECTO
                else -> MeshEstadoConexion.ENMALLADO_RELAY
            }
        }
    }
}
