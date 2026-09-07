package com.example.meshtx

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ENRUTADOR DE MALLA TÁCTICA (CEREBRO MESH 3.0 MULTI-HOP)
 * ═══════════════════════════════════════════════════════════════════════════
 * Arquitectura basada en Modelo Actor (Actor Model):
 * 1. Bucle de eventos secuencial alimentado por un Channel sin bloqueo para
 *    garantizar consistencia y cero carreras de hilos entre audio y red.
 * 2. Deduplicación O(1) de paquetes mediante Caché de Expiración Temporal
 *    (TimeEvictingCache) que suprime tormentas de difusión (Broadcast Storms).
 * 3. Enrutamiento multisalto inteligente: Flooding para SOS/telemetría y
 *    retransmisión de baja latencia para paquetes de voz.
 * 4. Aprendizaje de ruta inversa (Reverse Path Learning) y autorreparación dinámica.
 * 5. Monitoreo de presencia de nodos (Peer Liveness Tracker) en tiempo real.
 *
 * 100% OFFLINE - Cero dependencia de internet o servidores externos.
 */
class EnrutadorMalla(
    private var idPilotoLocal: Long,
    private var aliasPilotoLocal: String,
    private val alEnviarPaqueteFisico: (PaqueteDatosMesh, NodoMeshPiloto?) -> Unit
) {

    fun actualizarIdLocal(nuevoId: Long) {
        this.idPilotoLocal = nuevoId
    }

    fun actualizarAliasLocal(nuevoAlias: String) {
        if (nuevoAlias.isNotBlank()) this.aliasPilotoLocal = nuevoAlias
    }

    private val etiquetaLog = "MeshTX_Enrutador"
    private val limiteMaximoSaltosTtl = 5 // Máximo 5 saltos para paquetes de control y SOS
    private val limiteSaltosAudio = 3 // Máximo 3 saltos para audio para preservar baja latencia
    private val alcanceEnrutador = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Flujo de paquetes recibidos dirigidos a este piloto o al canal grupal suscrito
    private val _paquetesEntrantes = MutableSharedFlow<PaqueteDatosMesh>(extraBufferCapacity = 64)
    val paquetesEntrantes: SharedFlow<PaqueteDatosMesh> = _paquetesEntrantes.asSharedFlow()

    // Canal del Modelo Actor para procesar eventos secuencialmente sin bloqueos
    private val canalAcciones = Channel<AccionEnrutador>(Channel.UNLIMITED)

    // Caché de desalojo temporal O(1) para suprimir ecos y paquetes duplicados (20 segundos)
    private val cacheDeduplicacion = CacheExpiracionTemporal<Long>(tiempoExpiracionMs = 20000L)

    // Rastreador de presencia y salud de nodos (IdPiloto -> UltimoTimestampEscuchado)
    private val registroPresenciaNodos = mutableMapOf<Long, Long>()

    // Tabla de enrutamiento: [IdPilotoDestino -> EntradaRuta(SiguienteSalto, SaltosTotales, Timestamp)]
    data class EntradaRuta(
        val idSiguienteSalto: Long,
        val saltos: Int,
        val ultimoTimestamp: Long = System.currentTimeMillis()
    )
    private val tablaEnrutamiento = ConcurrentHashMap<Long, EntradaRuta>()

    // Nodos vecinos directos alcanzables en este momento
    private val vecinosDirectos = ConcurrentHashMap<Long, NodoMeshPiloto>()

    // Canal activo seleccionado por el usuario local
    var canalActivo: CanalTactico = CanalTactico.GENERAL_TX
    var canalActivoIdPersonalizado: Int? = null

    init {
        // Iniciar el bucle de procesamiento del Modelo Actor
        alcanceEnrutador.launch {
            bucleProcesamientoActor()
        }

        // Tarea de mantenimiento periódico de la caché y presencia de pares
        alcanceEnrutador.launch {
            while (isActive) {
                delay(5000)
                canalAcciones.trySend(AccionEnrutador.TickMantenimiento(System.currentTimeMillis()))
            }
        }
    }

    /**
     * Acciones procesadas exclusivamente en el bucle secuencial del Actor.
     */
    private sealed interface AccionEnrutador {
        data class ActualizarVecino(val nodo: NodoMeshPiloto) : AccionEnrutador
        data class RemoverVecino(val idNodo: Long) : AccionEnrutador
        data class TransmitirPaquete(
            val tipo: TipoPaqueteMesh,
            val payloadAudio: ByteArray?,
            val payloadTexto: String?,
            val destinoPilotoId: Long?
        ) : AccionEnrutador
        data class PaqueteRecibidoAntena(
            val paquete: PaqueteDatosMesh,
            val remitenteFisicoId: Long
        ) : AccionEnrutador
        data class TickMantenimiento(val tiempoActualMs: Long) : AccionEnrutador
    }

    /**
     * Bucle central del Modelo Actor: Procesa eventos uno a uno sin bloqueos de hilos.
     */
    private suspend fun bucleProcesamientoActor() {
        for (accion in canalAcciones) {
            try {
                when (accion) {
                    is AccionEnrutador.ActualizarVecino -> {
                        ejecutarActualizarVecino(accion.nodo)
                    }
                    is AccionEnrutador.RemoverVecino -> {
                        ejecutarRemoverVecino(accion.idNodo)
                    }
                    is AccionEnrutador.TransmitirPaquete -> {
                        ejecutarTransmisionPaquete(
                            tipo = accion.tipo,
                            payloadAudio = accion.payloadAudio,
                            payloadTexto = accion.payloadTexto,
                            destinoPilotoId = accion.destinoPilotoId
                        )
                    }
                    is AccionEnrutador.PaqueteRecibidoAntena -> {
                        ejecutarProcesamientoPaqueteRecibido(
                            paquete = accion.paquete,
                            remitenteFisicoId = accion.remitenteFisicoId
                        )
                    }
                    is AccionEnrutador.TickMantenimiento -> {
                        cacheDeduplicacion.limpiar(accion.tiempoActualMs)
                        auditarPresenciaNodos(accion.tiempoActualMs)
                    }
                }
            } catch (e: Exception) {
                Log.e(etiquetaLog, "Error en bucle del actor de enrutamiento: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERFAZ PÚBLICA (THREAD-SAFE / NON-BLOCKING)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registrar o actualizar un nodo vecino detectado directamente por antena.
     */
    fun actualizarVecinoDirecto(nodo: NodoMeshPiloto) {
        canalAcciones.trySend(AccionEnrutador.ActualizarVecino(nodo))
    }

    /**
     * Remover un nodo vecino que se salió del alcance directo (autorreparación).
     */
    fun removerVecinoDirecto(idNodo: Long) {
        canalAcciones.trySend(AccionEnrutador.RemoverVecino(idNodo))
    }

    /**
     * Emitir un paquete generado por el piloto local hacia la malla.
     */
    fun transmitirPaquete(
        tipo: TipoPaqueteMesh,
        payloadAudio: ByteArray? = null,
        payloadTexto: String? = null,
        destinoPilotoId: Long? = null
    ) {
        canalAcciones.trySend(
            AccionEnrutador.TransmitirPaquete(
                tipo = tipo,
                payloadAudio = payloadAudio,
                payloadTexto = payloadTexto,
                destinoPilotoId = destinoPilotoId
            )
        )
    }

    /**
     * Procesar un paquete que llegó de la antena física (enviado por otro nodo).
     */
    fun procesarPaqueteRecibido(paquete: PaqueteDatosMesh, remitenteFisicoId: Long) {
        canalAcciones.trySend(
            AccionEnrutador.PaqueteRecibidoAntena(
                paquete = paquete,
                remitenteFisicoId = remitenteFisicoId
            )
        )
    }

    fun obtenerTotalVecinosDirectos(): Int = vecinosDirectos.size
    fun obtenerTotalRutasConocidas(): Int = tablaEnrutamiento.size

    // ─────────────────────────────────────────────────────────────────────────
    // LÓGICA INTERNA SECUENCIAL (CONFINADA AL ACTOR)
    // ─────────────────────────────────────────────────────────────────────────

    private fun ejecutarActualizarVecino(nodo: NodoMeshPiloto) {
        vecinosDirectos[nodo.idMiembro] = nodo
        registroPresenciaNodos[nodo.idMiembro] = System.currentTimeMillis()

        // Ruta de 1 salto hacia el vecino directo
        tablaEnrutamiento[nodo.idMiembro] = EntradaRuta(
            idSiguienteSalto = nodo.idMiembro,
            saltos = 1,
            ultimoTimestamp = System.currentTimeMillis()
        )
        Log.d(etiquetaLog, "Tabla de enrutamiento: Vecino directo ${nodo.aliasPiloto} (1 salto)")
    }

    private fun ejecutarRemoverVecino(idNodo: Long) {
        vecinosDirectos.remove(idNodo)
        registroPresenciaNodos.remove(idNodo)

        // Purgar rutas que dependían de este enlace caído
        val iterador = tablaEnrutamiento.entries.iterator()
        while (iterador.hasNext()) {
            val entrada = iterador.next()
            if (entrada.value.idSiguienteSalto == idNodo) {
                iterador.remove()
                Log.w(etiquetaLog, "Ruta perdida hacia ${entrada.key}. Red autorreparándose...")
            }
        }
    }

    private fun ejecutarTransmisionPaquete(
        tipo: TipoPaqueteMesh,
        payloadAudio: ByteArray?,
        payloadTexto: String?,
        destinoPilotoId: Long?
    ) {
        val idPaquete = generarIdUnicoPaquete()
        val paquete = PaqueteDatosMesh(
            idPaquete = idPaquete,
            idEmisor = idPilotoLocal,
            aliasEmisor = aliasPilotoLocal,
            destinoPilotoId = destinoPilotoId,
            canal = canalActivoIdPersonalizado ?: canalActivo.idCanal,
            tipo = tipo,
            payloadAudio = payloadAudio,
            payloadTexto = payloadTexto,
            timestamp = System.currentTimeMillis(),
            saltosRelay = 0
        )

        // Marcar en caché propia para no procesar eco
        cacheDeduplicacion.poner(idPaquete, System.currentTimeMillis())

        if (destinoPilotoId != null) {
            enrutarPaqueteUnicast(paquete, destinoPilotoId)
        } else {
            difundirPaqueteAVecinos(paquete)
        }
    }

    private fun ejecutarProcesamientoPaqueteRecibido(paquete: PaqueteDatosMesh, remitenteFisicoId: Long) {
        val ahora = System.currentTimeMillis()

        // 1. Supresión absoluta de eco propio: NUNCA procesar ni emitir tramas originadas por este nodo local
        if (paquete.idEmisor == idPilotoLocal) {
            return
        }

        // 2. Descartar si ya fue procesado o emitido anteriormente (evita bucle infinito y tormenta de difusión)
        if (cacheDeduplicacion.contiene(paquete.idPaquete)) {
            return
        }
        cacheDeduplicacion.poner(paquete.idPaquete, ahora)

        // 3. Registrar presencia del emisor y del remitente físico
        registroPresenciaNodos[remitenteFisicoId] = ahora
        registroPresenciaNodos[paquete.idEmisor] = ahora

        // 4. Reverse Path Learning: Aprender ruta de retorno hacia el emisor original
        aprenderRutaInversa(paquete.idEmisor, remitenteFisicoId, paquete.saltosRelay + 1)

        // 5. AISLAMIENTO ESTRICTO DE CANALES:
        // Si es audio, SOLO se procesa si coincide EXACTAMENTE con el canal o sala activa
        val canalEsperado = canalActivoIdPersonalizado ?: canalActivo.idCanal
        if (paquete.tipo == TipoPaqueteMesh.AUDIO_VOZ_OPUS && paquete.canal != canalEsperado) {
            // El paquete de audio pertenece a un canal o sala diferente: NO mezclar ni reproducir
            return
        }

        // 6. Evaluar si el paquete nos corresponde por destino o por canal
        val esParaMi = paquete.destinoPilotoId == null || paquete.destinoPilotoId == idPilotoLocal
        val perteneceACanal = paquete.tipo == TipoPaqueteMesh.PAQUETE_SOS ||
                paquete.tipo == TipoPaqueteMesh.BEACON_DESCUBRIMIENTO ||
                paquete.destinoPilotoId != null ||
                paquete.canal == canalEsperado

        if (esParaMi && perteneceACanal) {
            _paquetesEntrantes.tryEmit(paquete)
        }

        // 5. Retransmisión Multisalto (Relay) según tipo de paquete y límite TTL
        if (paquete.tipo == TipoPaqueteMesh.AUDIO_VOZ_OPUS && paquete.destinoPilotoId == null) {
            // Audio de difusión general de canal abierto: ya fue reproducido en este receptor.
            // Para evitar tormentas de difusión en routers Wi-Fi compartidos, no re-inyectar al mismo medio.
            return
        }

        val ttlMaximo = if (paquete.tipo == TipoPaqueteMesh.AUDIO_VOZ_OPUS) limiteSaltosAudio else limiteMaximoSaltosTtl
        val nuevoSalto = paquete.saltosRelay + 1

        if (nuevoSalto < ttlMaximo) {
            val paqueteAReexpedir = paquete.copy(saltosRelay = nuevoSalto)

            if (paquete.destinoPilotoId != null) {
                // Mensaje privado no dirigido a nosotros: reenviar hacia el destino
                if (paquete.destinoPilotoId != idPilotoLocal) {
                    enrutarPaqueteUnicast(paqueteAReexpedir, paquete.destinoPilotoId, remitenteFisicoId)
                }
            } else {
                // Difusión grupal: retransmitir a todos excepto al remitente físico
                difundirPaqueteAVecinos(paqueteAReexpedir, nodoAExcluir = remitenteFisicoId)
            }
        } else {
            Log.d(etiquetaLog, "Paquete ${paquete.idPaquete} (${paquete.tipo}) expiró por límite TTL ($nuevoSalto).")
        }
    }

    private fun difundirPaqueteAVecinos(paquete: PaqueteDatosMesh, nodoAExcluir: Long? = null) {
        // 1. Difusión física universal inmediata (al aire/broadcast)
        alEnviarPaqueteFisico(paquete, null)

        // 2. Transmisión dirigida a vecinos conocidos
        vecinosDirectos.values.forEach { vecino ->
            if (vecino.idMiembro != nodoAExcluir && vecino.idMiembro != paquete.idEmisor) {
                alEnviarPaqueteFisico(paquete, vecino)
            }
        }
    }

    private fun enrutarPaqueteUnicast(paquete: PaqueteDatosMesh, destinoId: Long, nodoAExcluir: Long? = null) {
        val ruta = tablaEnrutamiento[destinoId]
        if (ruta != null) {
            val siguienteNodo = vecinosDirectos[ruta.idSiguienteSalto]
            if (siguienteNodo != null && siguienteNodo.idMiembro != nodoAExcluir) {
                Log.d(etiquetaLog, "Enrutando 1 a 1 hacia $destinoId vía siguiente salto ${siguienteNodo.aliasPiloto}")
                alEnviarPaqueteFisico(paquete, siguienteNodo)
                return
            }
        }

        // Si no tenemos ruta conocida, inundación controlada de búsqueda
        Log.d(etiquetaLog, "Sin ruta conocida hacia $destinoId. Flooding controlado de malla...")
        difundirPaqueteAVecinos(paquete, nodoAExcluir)
    }

    private fun aprenderRutaInversa(idEmisor: Long, idSiguienteSalto: Long, saltos: Int) {
        val rutaExistente = tablaEnrutamiento[idEmisor]
        if (rutaExistente == null || saltos <= rutaExistente.saltos) {
            tablaEnrutamiento[idEmisor] = EntradaRuta(
                idSiguienteSalto = idSiguienteSalto,
                saltos = saltos,
                ultimoTimestamp = System.currentTimeMillis()
            )
        }
    }

    private fun auditarPresenciaNodos(ahora: Long) {
        val iterador = registroPresenciaNodos.entries.iterator()
        while (iterador.hasNext()) {
            val entrada = iterador.next()
            if (ahora - entrada.value > 120000L) { // 120 segundos sin señal para evitar desconexiones falsas
                Log.d(etiquetaLog, "Silencio prolongado del nodo ${entrada.key}. Purgando presencia...")
                iterador.remove()
            }
        }
    }

    private fun generarIdUnicoPaquete(): Long {
        return (System.currentTimeMillis() shl 16) or (idPilotoLocal and 0xFFFFL)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CACHÉ TEMPORAL DE DESALOJO O(1) (TIME-EVICTING CACHE CON LAZY DELETION)
    // ─────────────────────────────────────────────────────────────────────────

    private class CacheExpiracionTemporal<K>(private val tiempoExpiracionMs: Long) {
        private data class Entrada<K>(val clave: K, val timestamp: Long)

        private val mapa = HashMap<K, Long>()
        private val colaCronologica = ArrayDeque<Entrada<K>>()

        @Synchronized
        fun poner(clave: K, tiempoMs: Long) {
            mapa[clave] = tiempoMs
            colaCronologica.addLast(Entrada(clave, tiempoMs))
        }

        @Synchronized
        fun contiene(clave: K): Boolean {
            return mapa.containsKey(clave)
        }

        @Synchronized
        fun limpiar(tiempoActualMs: Long) {
            while (colaCronologica.isNotEmpty()) {
                val masViejo = colaCronologica.first()
                if (tiempoActualMs - masViejo.timestamp <= tiempoExpiracionMs) {
                    break
                }

                colaCronologica.removeFirst()

                // Verificación de borrado perezoso (Lazy Deletion)
                val tiempoRegistrado = mapa[masViejo.clave]
                if (tiempoRegistrado == masViejo.timestamp) {
                    mapa.remove(masViejo.clave)
                }
            }
        }

        @Synchronized
        fun limpiarTodo() {
            mapa.clear()
            colaCronologica.clear()
        }
    }
}
