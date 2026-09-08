package com.example.rutas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Gestor principal de tracking de rutas, cálculo de estadísticas en tiempo real y Seguro de Vida por batería.
 * Nomenclatura en español estricta.
 */
object RUTA {

    private const val TAG = "TEAM_TX_RUTA"
    private const val ARCHIVO_RUTA_LOCAL = "ruta_activa_tx.json"
    private const val PREFS_RUTA = "preferencias_ruta_tx"

    // Estado observable de la ruta en curso
    private val _resumenRutaState = MutableStateFlow(ResumenRutaTX())
    val resumenRutaState: StateFlow<ResumenRutaTX> = _resumenRutaState.asStateFlow()

    // Estado del Seguro de Vida por Batería
    private val _bateriaNivelState = MutableStateFlow(100)
    val bateriaNivelState: StateFlow<Int> = _bateriaNivelState.asStateFlow()

    private val _respaldoCriticoEjecutadoState = MutableStateFlow(false)
    val respaldoCriticoEjecutadoState: StateFlow<Boolean> = _respaldoCriticoEjecutadoState.asStateFlow()

    private var receptorBateriaRegistrado = false

    /**
     * BroadcastReceiver interno ("Seguro de Vida - Batería") que vigila el porcentaje de carga.
     * Si la batería baja de o es igual al 5%, empaqueta los datos de la ruta actual
     * y los envía a Firebase mediante NUBE.kt automáticamente.
     */
    val receptorBateria = object : BroadcastReceiver() {
        override fun onReceive(contexto: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val nivel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val escala = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val porcentaje = if (nivel >= 0 && escala > 0) {
                    (nivel * 100) / escala
                } else 100

                _bateriaNivelState.value = porcentaje

                // Si la batería baja del 5% y la ruta está activa (GRABANDO o PAUSADA)
                if (porcentaje <= 5 && !_respaldoCriticoEjecutadoState.value) {
                    val rutaActual = _resumenRutaState.value
                    if (rutaActual.estadoRuta == EstadoRutaEnum.GRABANDO || rutaActual.estadoRuta == EstadoRutaEnum.PAUSADA) {
                        Log.w(TAG, "⚡ SEGURO DE VIDA ACTIVADO: Batería crítica en $porcentaje%. Iniciando respaldo en Firebase...")
                        contexto?.let { respaldarPorBateriaBaja(it, porcentaje) }
                    }
                }
            }
        }
    }

    /**
     * Inicia el monitoreo dinámico del receptor de batería en el contexto proporcionado.
     */
    fun registrarReceptorBateria(contexto: Context) {
        if (!receptorBateriaRegistrado) {
            try {
                val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                contexto.registerReceiver(receptorBateria, intentFilter)
                receptorBateriaRegistrado = true
                Log.d(TAG, "🟢 Receptor de Batería (Seguro de Vida) registrado correctamente.")
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando receptor de batería: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Detiene el monitoreo del receptor de batería.
     */
    fun desregistrarReceptorBateria(contexto: Context) {
        if (receptorBateriaRegistrado) {
            try {
                contexto.unregisterReceiver(receptorBateria)
                receptorBateriaRegistrado = false
                Log.d(TAG, "🔴 Receptor de Batería desregistrado.")
            } catch (e: Exception) {
                Log.e(TAG, "Error desregistrando receptor de batería: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Inicia una nueva ruta de navegación o tracking motero.
     *
     * @param titulo Nombre asignado a la ruta.
     * @param idPiloto UID o ficha del piloto.
     * @param nombrePiloto Nombre o apodo del piloto.
     */
    fun iniciarRuta(titulo: String, idPiloto: String, nombrePiloto: String) {
        val idNuevaRuta = "RUTA_${System.currentTimeMillis()}"
        val nuevaRuta = ResumenRutaTX(
            idRuta = idNuevaRuta,
            tituloRuta = titulo.ifBlank { "Ruta TX Aragua" },
            idPiloto = idPiloto,
            nombrePiloto = nombrePiloto.ifBlank { "Piloto TX" },
            fechaInicioMs = System.currentTimeMillis(),
            estadoRuta = EstadoRutaEnum.GRABANDO
        )
        _resumenRutaState.value = nuevaRuta
        _respaldoCriticoEjecutadoState.value = false
        Log.d(TAG, "🟢 Nueva ruta iniciada: $idNuevaRuta - $titulo")
    }

    /**
     * Agrega un nuevo punto GPS (latitud, longitud, altitud, velocidad, tiempo) al arreglo de la ruta actual
     * y recalcula las estadísticas en tiempo real.
     *
     * @param latitud Latitud GPS.
     * @param longitud Longitud GPS.
     * @param altitudMts Altitud en metros.
     * @param velocidadKmh Velocidad actual en km/h.
     */
    fun agregarPunto(latitud: Double, longitud: Double, altitudMts: Double, velocidadKmh: Float) {
        val rutaActual = _resumenRutaState.value
        if (rutaActual.estadoRuta != EstadoRutaEnum.GRABANDO) return

        val nuevoPunto = PuntoRutaGPS(
            latitud = latitud,
            longitud = longitud,
            altitudMts = altitudMts,
            velocidadKmh = velocidadKmh,
            timestamp = System.currentTimeMillis()
        )

        val listaPuntos = rutaActual.puntos.toMutableList()

        // Calcular incremento de distancia si hay un punto previo
        var distanciaAdicional = 0.0
        if (listaPuntos.isNotEmpty()) {
            val ultimoPunto = listaPuntos.last()
            distanciaAdicional = calcularDistanciaKm(
                ultimoPunto.latitud, ultimoPunto.longitud,
                latitud, longitud
            )
        }

        listaPuntos.add(nuevoPunto)

        // Estadísticas en tiempo real
        val nuevaDistanciaTotal = rutaActual.distanciaTotalKm + distanciaAdicional
        val nuevaVelocidadMax = maxOf(rutaActual.velocidadMaximaKmh, velocidadKmh)

        // Velocidad promedio
        val sumaVelocidades = listaPuntos.fold(0f) { acc, p -> acc + p.velocidadKmh }
        val nuevaVelocidadPromedio = if (listaPuntos.isNotEmpty()) sumaVelocidades / listaPuntos.size else 0f

        val rutaActualizada = rutaActual.copy(
            puntos = listaPuntos,
            distanciaTotalKm = nuevaDistanciaTotal,
            velocidadMaximaKmh = nuevaVelocidadMax,
            velocidadPromedioKmh = nuevaVelocidadPromedio,
            fechaFinMs = System.currentTimeMillis()
        )

        _resumenRutaState.value = rutaActualizada
    }

    /**
     * Pausa temporalmente la grabación de la ruta.
     */
    fun pausarRuta() {
        val rutaActual = _resumenRutaState.value
        if (rutaActual.estadoRuta == EstadoRutaEnum.GRABANDO) {
            _resumenRutaState.value = rutaActual.copy(estadoRuta = EstadoRutaEnum.PAUSADA)
            Log.d(TAG, "⏸️ Ruta pausada")
        }
    }

    /**
     * Reanuda la grabación de la ruta tras una pausa.
     */
    fun reanudarRuta() {
        val rutaActual = _resumenRutaState.value
        if (rutaActual.estadoRuta == EstadoRutaEnum.PAUSADA) {
            _resumenRutaState.value = rutaActual.copy(estadoRuta = EstadoRutaEnum.GRABANDO)
            Log.d(TAG, "▶️ Ruta reanudada")
        }
    }

    /**
     * Finaliza la ruta actual, la guarda localmente y la sube a Firebase (NUBE.kt).
     *
     * @param alTerminar Callback al concluir el guardado.
     */
    fun detenerRuta(contexto: Context, alTerminar: (ResumenRutaTX) -> Unit = {}) {
        val rutaActual = _resumenRutaState.value
        val rutaFinalizada = rutaActual.copy(
            estadoRuta = EstadoRutaEnum.FINALIZADA,
            fechaFinMs = System.currentTimeMillis()
        )
        _resumenRutaState.value = rutaFinalizada

        // 1. Guardado en la Nube (NUBE.kt)
        NUBE.guardarRutaNube(rutaFinalizada) { exitoso ->
            Log.d(TAG, "🏁 Ruta finalizada. Subida a Nube: $exitoso")
        }

        // 2. Guardado en almacenamiento local de respaldo
        guardarRutaLocal(contexto, rutaFinalizada)

        alTerminar(rutaFinalizada)
    }

    /**
     * Ejecuta el respaldo automático por batería crítica (<= 5%).
     */
    fun respaldarPorBateriaBaja(contexto: Context, nivelBateria: Int) {
        val rutaActual = _resumenRutaState.value
        _respaldoCriticoEjecutadoState.value = true

        val rutaRespaldada = rutaActual.copy(
            estadoRuta = EstadoRutaEnum.RESPALDADA_BATERIA,
            nivelBateriaRespaldo = nivelBateria,
            fechaFinMs = System.currentTimeMillis()
        )
        _resumenRutaState.value = rutaRespaldada

        val respaldoObj = RespaldoBateriaRuta(
            idRespaldo = "RESPALDO_BAT_${System.currentTimeMillis()}",
            idRuta = rutaActual.idRuta,
            porcentajeBateria = nivelBateria,
            timestamp = System.currentTimeMillis(),
            resumenRuta = rutaRespaldada
        )

        // Enviar a la nube mediante NUBE.kt
        NUBE.respaldarBateriaCritica(respaldoObj) { exitoso ->
            Log.d(TAG, "⚡ Respaldo de batería enviado a NUBE.kt: $exitoso")
        }

        // Guardado local de emergencia
        guardarRutaLocal(contexto, rutaRespaldada)
    }

    /**
     * Guarda la estructura de la ruta en un archivo JSON en la memoria interna del teléfono.
     */
    fun guardarRutaLocal(contexto: Context, ruta: ResumenRutaTX) {
        try {
            val jsonObj = JSONObject().apply {
                put("idRuta", ruta.idRuta)
                put("tituloRuta", ruta.tituloRuta)
                put("idPiloto", ruta.idPiloto)
                put("nombrePiloto", ruta.nombrePiloto)
                put("distanciaTotalKm", ruta.distanciaTotalKm)
                put("velocidadMaximaKmh", ruta.velocidadMaximaKmh.toDouble())
                put("velocidadPromedioKmh", ruta.velocidadPromedioKmh.toDouble())
                put("estadoRuta", ruta.estadoRuta.name)

                val puntosArr = JSONArray()
                ruta.puntos.forEach { p ->
                    val pJson = JSONObject().apply {
                        put("lat", p.latitud)
                        put("lng", p.longitud)
                        put("alt", p.altitudMts)
                        put("vel", p.velocidadKmh.toDouble())
                        put("time", p.timestamp)
                    }
                    puntosArr.put(pJson)
                }
                put("puntos", puntosArr)
            }

            val archivo = File(contexto.filesDir, ARCHIVO_RUTA_LOCAL)
            archivo.writeText(jsonObj.toString())
            Log.d(TAG, "💾 Ruta guardada localmente en ${archivo.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando ruta localmente: ${e.localizedMessage}")
        }
    }

    /**
     * Calcula la distancia Haversine en kilómetros entre dos coordenadas GPS.
     */
    fun calcularDistanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radio de la Tierra en kilómetros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
