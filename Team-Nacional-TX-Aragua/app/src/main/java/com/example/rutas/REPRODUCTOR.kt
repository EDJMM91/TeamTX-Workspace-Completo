package com.example.rutas

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.plus.views.OsmandMapTileView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Motor Cinemático de Reproducción 2D de Rutas (Estudio de Grabación / Estilo Relive).
 *
 * Funcionalidades clave:
 * 1. Bucle cinemático en Corrutinas con interpolación de tiempo proporcional al viaje real.
 *    - Acelera cinematográficamente en rectas despejadas.
 *    - Desacelera y suaviza la cámara en curvas cerradas.
 * 2. Seguimiento dinámico en OsmAnd:
 *    - Desplaza la cámara continuamente con `mapView.setLatLon(lat, lon)`.
 *    - Calcula el azimuth/rumbo entre puntos consecutivos para girar el mapa (`mapView.setRotate(-azimuth, true)`),
 *      generando la perspectiva subjetiva de pilotaje en moto.
 * 3. Inyección de Fotografías en Ruta:
 *    - Detecta si una foto tomada en la ruta coincide con el timestamp o proximidad del punto actual.
 *    - Detiene la animación temporalmente (2.5 segundos), emite el overlay de foto para Jetpack Compose,
 *      y reanuda automáticamente el recorrido.
 * 4. Telemetría de reproducción en vivo observable mediante StateFlow.
 *
 * Nomenclatura en español estricta, desacoplada en com.example.rutas.
 */
object REPRODUCTOR {

    private const val TAG = "TEAM_TX_REPRODUCTOR"

    private val alcanceCorrutinas = CoroutineScope(Dispatchers.Default)
    private var trabajoReproduccion: Job? = null

    // Estados observables
    private val _estadoReproductor = MutableStateFlow(EstadoReproductorRuta.DETENIDO)
    val estadoReproductor: StateFlow<EstadoReproductorRuta> = _estadoReproductor.asStateFlow()

    private val _telemetria = MutableStateFlow(TelemetriaReproduccion())
    val telemetria: StateFlow<TelemetriaReproduccion> = _telemetria.asStateFlow()

    private val _fotoEnPantalla = MutableStateFlow<FotoRuta?>(null)
    val fotoEnPantalla: StateFlow<FotoRuta?> = _fotoEnPantalla.asStateFlow()

    // Control de pausa manual
    private var estaEnPausaManual = false

    /**
     * Inicia la reproducción virtual acelerada en 2D de la ruta proporcionada.
     *
     * @param ruta Resumen de la ruta con matriz de puntos GPS y fotos asociadas.
     * @param mapView Instancia activa del MapView de OsmAnd (opcional si solo se simula telemetría).
     * @param zoomNivel Nivel de zoom de la cámara táctica (por defecto 16).
     * @param multiplicadorVelocidad Factor de aceleración de la animación (1.0x a 4.0x).
     * @param alFinalizar Callback invocado al completar el recorrido cinematográfico.
     */
    fun iniciarReproduccion2D(
        ruta: ResumenRutaTX,
        mapView: OsmandMapTileView? = null,
        zoomNivel: Int = 16,
        multiplicadorVelocidad: Float = 2.0f,
        alFinalizar: () -> Unit = {}
    ) {
        detenerReproduccion()

        val puntos = ruta.puntos
        if (puntos.isEmpty()) {
            Log.w(TAG, "No hay puntos registrados en la ruta para reproducir.")
            return
        }

        estaEnPausaManual = false
        _estadoReproductor.value = EstadoReproductorRuta.REPRODUCIENDO

        trabajoReproduccion = alcanceCorrutinas.launch {
            try {
                Log.d(TAG, "🎬 Iniciando estudio de grabación y reproducción 2D para: ${ruta.tituloRuta} (${puntos.size} puntos)")

                val fotosPendientes = ruta.fotos.toMutableList()
                val fotosYaMostradas = mutableSetOf<String>()
                var distanciaAcumuladaKm = 0.0

                // Posicionar cámara inicialmente en el punto de salida
                withContext(Dispatchers.Main) {
                    try {
                        mapView?.setLatLon(puntos.first().latitud, puntos.first().longitud)
                        mapView?.setIntZoom(zoomNivel)
                        mapView?.setRotate(0f, true)
                        mapView?.refreshMap(true)
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo inicializar MapView: ${e.message}")
                    }
                }

                delay(600) // Pausa inicial para estabilizar la vista

                for (i in 0 until puntos.size) {
                    val puntoActual = puntos[i]
                    val puntoSiguiente = if (i < puntos.size - 1) puntos[i + 1] else null

                    // Manejo de pausa manual
                    while (estaEnPausaManual) {
                        delay(200)
                    }

                    // Calcular rumbo/azimuth hacia el siguiente punto
                    val azimuth = if (puntoSiguiente != null) {
                        calcularRumbo(
                            puntoActual.latitud, puntoActual.longitud,
                            puntoSiguiente.latitud, puntoSiguiente.longitud
                        )
                    } else {
                        _telemetria.value.rumboAzimuth
                    }

                    // Acumular distancia recorrida
                    if (puntoSiguiente != null) {
                        val tramoMetros = calcularDistanciaMetros(
                            puntoActual.latitud, puntoActual.longitud,
                            puntoSiguiente.latitud, puntoSiguiente.longitud
                        )
                        distanciaAcumuladaKm += (tramoMetros / 1000.0)
                    }

                    val progreso = if (puntos.size > 1) (i.toFloat() / (puntos.size - 1)) else 1.0f

                    // Emitir telemetría instantánea
                    _telemetria.value = TelemetriaReproduccion(
                        indicePuntoActual = i,
                        totalPuntos = puntos.size,
                        latitudActual = puntoActual.latitud,
                        longitudActual = puntoActual.longitud,
                        rumboAzimuth = azimuth,
                        velocidadSimuladaKmh = puntoActual.velocidadKmh,
                        distanciaRecorridaKm = distanciaAcumuladaKm,
                        porcentajeProgreso = progreso,
                        fotoVisible = _fotoEnPantalla.value
                    )

                    // Actualizar MapView en el hilo principal
                    withContext(Dispatchers.Main) {
                        try {
                            mapView?.setLatLon(puntoActual.latitud, puntoActual.longitud)
                            mapView?.setRotate(-azimuth, true)
                            mapView?.refreshMap(false)
                        } catch (_: Exception) {}
                    }

                    // Comprobar inyección de fotos por timestamp o proximidad geográfica
                    val fotoParaInyectar = fotosPendientes.find { foto ->
                        if (foto.idFoto in fotosYaMostradas) return@find false
                        val deltaTiempoMs = kotlin.math.abs(foto.timestamp - puntoActual.timestamp)
                        val deltaDistanciaMts = calcularDistanciaMetros(
                            foto.latitud, foto.longitud,
                            puntoActual.latitud, puntoActual.longitud
                        )
                        (deltaTiempoMs <= 20000L) || (deltaDistanciaMts <= 80.0)
                    }

                    if (fotoParaInyectar != null) {
                        fotosYaMostradas.add(fotoParaInyectar.idFoto)
                        fotosPendientes.remove(fotoParaInyectar)

                        Log.d(TAG, "📸 Inyectando foto en ruta: ${fotoParaInyectar.idFoto} en (${puntoActual.latitud}, ${puntoActual.longitud})")
                        _estadoReproductor.value = EstadoReproductorRuta.PAUSADO_FOTO
                        _fotoEnPantalla.value = fotoParaInyectar

                        // Pausa cinematográfica de 2.5 segundos para apreciar la foto
                        delay(2500)

                        _fotoEnPantalla.value = null
                        _estadoReproductor.value = EstadoReproductorRuta.REPRODUCIENDO
                    }

                    // Cálculo matemático del delay para simulación de conducción:
                    // En rectas (curva pequeña < 15°): avanza más rápido.
                    // En curvas cerradas (> 45°): desacelera suavemente para apreciarlas.
                    val tiempoBaseMs = if (puntoSiguiente != null) {
                        val rumboSig = if (i + 2 < puntos.size) {
                            calcularRumbo(puntoSiguiente.latitud, puntoSiguiente.longitud, puntos[i + 2].latitud, puntos[i + 2].longitud)
                        } else azimuth
                        val deltaAngulo = kotlin.math.abs(azimuth - rumboSig)
                        if (deltaAngulo > 40f) 120L else 50L
                    } else 80L

                    val delayCalculado = (tiempoBaseMs / multiplicadorVelocidad).toLong().coerceIn(25L, 300L)
                    delay(delayCalculado)
                }

                _estadoReproductor.value = EstadoReproductorRuta.FINALIZADO
                Log.d(TAG, "🏁 Reproducción 2D completada exitosamente.")
                withContext(Dispatchers.Main) {
                    alFinalizar()
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Reproducción cancelada por el usuario.")
                _estadoReproductor.value = EstadoReproductorRuta.DETENIDO
            } catch (e: Exception) {
                Log.e(TAG, "Error en bucle de reproducción: ${e.localizedMessage}", e)
                _estadoReproductor.value = EstadoReproductorRuta.DETENIDO
            } finally {
                _fotoEnPantalla.value = null
            }
        }
    }

    /**
     * Pausa o reanuda la reproducción cinemática en curso.
     */
    fun alternarPausa() {
        if (_estadoReproductor.value == EstadoReproductorRuta.REPRODUCIENDO) {
            estaEnPausaManual = true
            _estadoReproductor.value = EstadoReproductorRuta.PAUSADO_MANUAL
        } else if (_estadoReproductor.value == EstadoReproductorRuta.PAUSADO_MANUAL) {
            estaEnPausaManual = false
            _estadoReproductor.value = EstadoReproductorRuta.REPRODUCIENDO
        }
    }

    /**
     * Detiene por completo la reproducción y restablece los estados.
     */
    fun detenerReproduccion() {
        trabajoReproduccion?.cancel()
        trabajoReproduccion = null
        estaEnPausaManual = false
        _estadoReproductor.value = EstadoReproductorRuta.DETENIDO
        _fotoEnPantalla.value = null
        _telemetria.value = TelemetriaReproduccion()
    }

    /**
     * Calcula el rumbo o ángulo azimutal en grados (0° a 360°) entre dos coordenadas GPS.
     */
    fun calcularRumbo(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        var brng = Math.toDegrees(atan2(y, x)).toFloat()
        if (brng < 0) {
            brng += 360f
        }
        return brng
    }

    /**
     * Calcula la distancia en metros entre dos coordenadas geográficas utilizando la fórmula de Haversine.
     */
    fun calcularDistanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierraM = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radioTierraM * c
    }
}
