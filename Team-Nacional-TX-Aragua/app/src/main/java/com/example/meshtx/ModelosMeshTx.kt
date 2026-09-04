package com.example.meshtx

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PAQUETE INDEPENDIENTE: MESH TX - INTERCOMUNICADOR TÁCTICO
 * ═══════════════════════════════════════════════════════════════════════════
 * Estructura de modelos de datos y estados base para la red de intercomunicación
 * táctica de corto/medio alcance (Mesh P2P / WiFi Direct / BLE / Audio Streaming)
 * para el convoy motero Team TX Venezuela.
 */

/**
 * Estado general de la conexión al enmallado Mesh TX.
 */
enum class MeshEstadoConexion(val descripcion: String) {
    DESCONECTADO("Desconectado"),
    ESCANEANDO("Buscando nodos cercanos..."),
    CONECTANDO("Estableciendo enlace táctico..."),
    ENLACE_DIRECTO("Enlace Directo Activo"),
    ENMALLADO_RELAY("Enmallado Mesh Multisalton (Relay)"),
    ERROR("Error de Conexión")
}

/**
 * Canales tácticos de frecuencia virtual en el intercomunicador.
 */
enum class CanalTactico(val idCanal: Int, val nombre: String, val descripcion: String) {
    GENERAL_TX(1, "Canal General TX", "Canal abierto para toda la comunidad y rodada"),
    CARAVANA_CONVOY(2, "Caravana & Convoy", "Comunicación para Capitanes, Punteros y Barredoras"),
    EMERGENCIA_SOS(3, "Emergencia SOS", "Prioridad máxima de auxilio vial y mecánicos"),
    DIRECTIVA(4, "Directiva Oficial", "Canal cifrado exclusivo para líderes"),
    PERSONALIZADO(5, "Canal Privado", "Enlace cerrado entre pilotos emparejados")
}

/**
 * Representación de un piloto/nodo activo en la red Mesh TX.
 */
data class NodoMeshPiloto(
    val idMiembro: Long,
    val aliasPiloto: String,
    val nombreMoto: String = "",
    val fotoUrl: String = "",
    val fichaMiembro: String = "",
    val modeloTelefonoHardware: String = "", // Modelo técnico ej: "Samsung Galaxy A16", "Honor ALI-NX3"
    val direccionNodo: String = "", // IP local o dirección MAC BLE/WiFi
    val intensidadSenalDbm: Int = -60, // RSSI
    val estaTransmitiendoVoz: Boolean = false, // Modo PTT activo
    val estaSilenciado: Boolean = false,
    val esCapitanOBarredora: Boolean = false,
    val distanciaAproximadaMetros: Double = 0.0,
    val ultimoPingTimestamp: Long = System.currentTimeMillis()
)

/**
 * Paquete de datos que viaja a través de la red de intercomunicación.
 */
enum class TipoPaqueteMesh {
    AUDIO_VOZ_OPUS,       // Paquete comprimido de voz PTT en tiempo real
    PAQUETE_SOS,          // Paquete de alerta de emergencia con máxima prioridad
    TELEMETRIA_POSICION,  // Coordenadas GPS en caravana
    BEACON_DESCUBRIMIENTO,// Ping periódico para mantener la topología de la malla
    TEXTO_RAPIDO          // Mensajes tácticos pregrabados o código morse sonoro
}

data class PaqueteDatosMesh(
    val idPaquete: Long = System.currentTimeMillis(),
    val idEmisor: Long,
    val aliasEmisor: String,
    val destinoPilotoId: Long? = null, // null para difusión grupal, id específico para chat privado 1 a 1
    val canal: Int = CanalTactico.GENERAL_TX.idCanal,
    val tipo: TipoPaqueteMesh = TipoPaqueteMesh.AUDIO_VOZ_OPUS,
    val payloadAudio: ByteArray? = null,
    val payloadTexto: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val saltosRelay: Int = 0 // Contador de retransmisión (TTL de la malla)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PaqueteDatosMesh
        return idPaquete == other.idPaquete
    }

    override fun hashCode(): Int = idPaquete.hashCode()
}

/**
 * Ajustes de configuración del hardware de audio e intercomunicador.
 */
data class AjustesIntercomunicadorTactico(
    val modoPtt: Boolean = true, // Push-to-Talk (pulsar para hablar) vs VOX (manos libres por detección de voz)
    val umbralVoxSensibilidad: Float = 0.65f, // Sensibilidad del micrófono para activación por voz
    val cancelacionRuidoViento: Boolean = true,
    val supresionEcoAcustico: Boolean = true,
    val gananciaMicrofonoCasco: Float = 1.2f,
    val codecSeleccionado: String = "OPUS_LOW_LATENCY",
    val canalActivo: CanalTactico = CanalTactico.GENERAL_TX,
    val mostrarPerfilSincronizado: Boolean = true, // true: ver foto, moto TX y nombre real; false: ver modelo teléfono hardware y MAC
    val modoAltavozActivo: Boolean = true // true: altavoz exterior potente (manos libres); false: auricular privado/casco
)
