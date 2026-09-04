package com.example.meshtx

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // Subsistemas internos
    private var buscadorMalla: BuscadorMalla? = null
    private var enrutadorMalla: EnrutadorMalla? = null
    private var transporteUdp: TransporteMallaUdp? = null
    private var audioCasco: AudioCasco? = null
    private var nubeMalla: NubeMalla? = null

    private var idPilotoLocal: Long = 1001L
    private var aliasPilotoLocal: String = "Piloto TX"
    private var estaInicializado = false

    val modoAltavozActivo: StateFlow<Boolean>
        get() = audioCasco?.modoAltavozActivo ?: MutableStateFlow(true)

    /**
     * Inicializar los componentes con el contexto de la aplicación e identidad del piloto.
     */
    fun inicializar(contexto: Context, idPiloto: Long, aliasPiloto: String) {
        if (estaInicializado && this.idPilotoLocal == idPiloto && this.aliasPilotoLocal == aliasPiloto) return
        this.idPilotoLocal = idPiloto
        this.aliasPilotoLocal = aliasPiloto
        val ctx = contexto.applicationContext

        Log.i(ETIQUETA_LOG, "Inicializando GestorMeshTx para: $aliasPiloto (ID: $idPiloto)")

        // 1. Instanciar Transporte Físico UDP de red local offline (Latencia < 15ms)
        transporteUdp = TransporteMallaUdp(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal,
            alRecibirPaquete = { paquete, remitenteId ->
                enrutadorMalla?.procesarPaqueteRecibido(paquete, remitenteId)
                // Registrar o actualizar con el alias real del piloto y dirección IP
                val mapa = buscadorMalla?.nodosDetectados?.value?.toMutableMap() ?: mutableMapOf()
                val previo = mapa[paquete.idEmisor]
                val nodoUdp = NodoMeshPiloto(
                    idMiembro = paquete.idEmisor,
                    aliasPiloto = paquete.aliasEmisor.ifBlank { previo?.aliasPiloto ?: "Piloto TX" },
                    modeloTelefonoHardware = previo?.modeloTelefonoHardware ?: "Dispositivo TX",
                    direccionNodo = "UDP_58200",
                    intensidadSenalDbm = -35,
                    distanciaAproximadaMetros = 5.0,
                    ultimoPingTimestamp = System.currentTimeMillis()
                )
                enrutadorMalla?.actualizarVecinoDirecto(nodoUdp)
                if (previo == null || previo.aliasPiloto != nodoUdp.aliasPiloto) {
                    mapa[paquete.idEmisor] = nodoUdp
                    actualizarListaNodos()
                }
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
                enrutadorMalla?.actualizarVecinoDirecto(nodo)
                actualizarListaNodos()
            },
            alPerderNodo = { idNodo ->
                enrutadorMalla?.removerVecinoDirecto(idNodo)
                actualizarListaNodos()
            }
        )

        // 4. Instanciar Motor de Audio de Casco (PTT, VOX, Filtro Viento, Bluetooth SCO & Altavoz)
        audioCasco = AudioCasco(
            contexto = ctx,
            alGenerarFragmentoVoz = { bytesAudio ->
                // Enviar fragmento de voz capturado a la malla táctica
                enrutadorMalla?.transmitirPaquete(
                    tipo = TipoPaqueteMesh.AUDIO_VOZ_OPUS,
                    payloadAudio = bytesAudio
                )
            }
        )

        // Observar estado del casco Bluetooth
        alcanceGestor.launch {
            audioCasco?.dispositivoBluetoothCascoConectado?.collect { conectado ->
                _cascoBluetoothConectado.value = conectado
            }
        }

        // 5. Instanciar Nube Pasiva (solo para sincronización de telemetría cuando haya internet)
        nubeMalla = NubeMalla(
            contexto = ctx,
            idPilotoLocal = idPilotoLocal,
            aliasPilotoLocal = aliasPilotoLocal
        )

        // 6. Escuchar paquetes entrantes dirigidos a este nodo desde la malla
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
        Log.i(ETIQUETA_LOG, "Deteniendo Malla Táctica TX...")

        buscadorMalla?.detenerExploracion()
        transporteUdp?.detenerTransporte()
        audioCasco?.detenerCaptura()
        _nodosEnRed.value = emptyList()
    }

    /**
     * Cambiar el canal táctico de radiofrecuencia virtual.
     */
    fun cambiarCanal(nuevoCanal: CanalTactico) {
        _canalActual.value = nuevoCanal
        enrutadorMalla?.canalActivo = nuevoCanal
        _ajustes.value = _ajustes.value.copy(canalActivo = nuevoCanal)
        Log.i(ETIQUETA_LOG, "Canal táctico cambiado a: ${nuevoCanal.nombre}")
    }

    /**
     * Pulsar o soltar el botón PTT (Push-to-Talk) en pantalla o manubrio.
     */
    fun setTransmitiendoPtt(transmitiendo: Boolean) {
        _estaTransmitiendoPtt.value = transmitiendo
        audioCasco?.setPttPresionado(transmitiendo)
        if (transmitiendo) {
            _pilotoHablandoAhora.value = "Tú ($aliasPilotoLocal)"
        } else {
            if (_pilotoHablandoAhora.value?.contains(aliasPilotoLocal) == true) {
                _pilotoHablandoAhora.value = null
            }
        }
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
     * Alternar el modo de visualización de nodos (perfil sincronizado vs dispositivo técnico).
     */
    fun alternarVisualizacionPerfil(mostrarPerfil: Boolean) {
        _ajustes.value = _ajustes.value.copy(mostrarPerfilSincronizado = mostrarPerfil)
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
        nubeMalla?.liberar()
        estaInicializado = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS DE GESTIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private fun procesarPaqueteEntrante(paquete: PaqueteDatosMesh) {
        when (paquete.tipo) {
            TipoPaqueteMesh.AUDIO_VOZ_OPUS -> {
                paquete.payloadAudio?.let { audio ->
                    _pilotoHablandoAhora.value = paquete.aliasEmisor
                    _idPilotoHablandoAhora.value = paquete.idEmisor
                    actualizarListaNodos()
                    audioCasco?.encolarAudioEntrante(audio)
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

    private fun actualizarListaNodos() {
        val lista = buscadorMalla?.nodosDetectados?.value?.values?.toList() ?: emptyList()
        val hablandoId = _idPilotoHablandoAhora.value
        val hablandoAlias = _pilotoHablandoAhora.value
        val listaEnriquecida = lista.map { nodo ->
            val perfil = nubeMalla?.obtenerPerfilPiloto(nodo.idMiembro)
            val estaHablando = (hablandoId != null && nodo.idMiembro == hablandoId) ||
                               (!hablandoAlias.isNullOrBlank() && nodo.aliasPiloto.isNotBlank() && nodo.aliasPiloto == hablandoAlias)
            if (perfil != null) {
                nodo.copy(
                    aliasPiloto = perfil.alias.ifBlank { nodo.aliasPiloto },
                    nombreMoto = perfil.modeloMoto.ifBlank { "Keeway TX 200" },
                    fotoUrl = perfil.fotoUrl,
                    fichaMiembro = "TX-${(nodo.idMiembro and 0x3FFL)}",
                    modeloTelefonoHardware = nodo.modeloTelefonoHardware.ifBlank { nodo.aliasPiloto },
                    estaTransmitiendoVoz = estaHablando
                )
            } else {
                nodo.copy(
                    modeloTelefonoHardware = nodo.modeloTelefonoHardware.ifBlank { nodo.aliasPiloto },
                    estaTransmitiendoVoz = estaHablando
                )
            }
        }
        _nodosEnRed.value = listaEnriquecida

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
