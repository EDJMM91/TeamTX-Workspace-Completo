package com.example.radar

import android.util.Log
import com.example.data.model.MemberProfile
import com.example.data.model.BikerCalendarEvent
import com.example.data.model.Publication
import com.example.data.remote.PerfilNube
import kotlinx.coroutines.*
import net.osmand.plus.OsmandApplication

object GestorRadar {

    private const val ETIQUETA = "GESTOR_RADAR"
    private const val Z_RADAR = 7.4f
    private const val Z_EVENTOS = 7.0f

    private var application: OsmandApplication? = null
    private var mapaLayer: RadarMapLayer? = null
    private var eventosLayer: EventosMapLayer? = null
    private val alcance = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var escuchando = false
    private var miUserId: String = ""
    private var miNombre: String = ""
    private var miRango: String = ""
    private var miAvatarUrl: String = ""

    var pilotoSeleccionadoPerfil: MemberProfile? = null
        private set
    var pilotoSeleccionadoPiloto: PilotoRadar? = null
        private set
    var onPerfilCargado: ((MemberProfile?) -> Unit)? = null
    var onAccionPiloto: ((String, MemberProfile) -> Unit)? = null

    fun iniciar(
        app: OsmandApplication,
        userId: String,
        nombre: String = "",
        rango: String = "",
        avatarUrl: String = ""
    ) {
        if (escuchando) return
        application = app
        miUserId = userId
        miNombre = nombre
        miRango = rango
        miAvatarUrl = avatarUrl

        RadarFirebase.init(app)
        RadarFirebase.obtenerAvatarLocal(app)

        if (miAvatarUrl.isNotBlank()) {
            alcance.launch {
                RadarFirebase.descargarAvatar(miAvatarUrl, app)
            }
        }

        val layer = RadarMapLayer(app)
        mapaLayer = layer
        layer.setMiUserId(miUserId)

        layer.setOnPilotoSeleccionado { piloto ->
            Log.d(ETIQUETA, "Piloto seleccionado: ${piloto.nombre}")
            pilotoSeleccionadoPiloto = piloto
            alcance.launch {
                val perfil = try {
                    PerfilNube.descargarPerfil(piloto.id)
                } catch (e: Exception) {
                    Log.w(ETIQUETA, "Error descargando perfil de ${piloto.nombre}: ${e.message}")
                    null
                }
                pilotoSeleccionadoPerfil = perfil
                withContext(Dispatchers.Main) {
                    onPerfilCargado?.invoke(perfil)
                }
            }
        }

        try {
            val mapView = app.osmandMap.mapView
            mapView?.addLayer(layer, Z_RADAR)
            Log.d(ETIQUETA, "Capa RadarMapLayer registrada en z=$Z_RADAR")
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Mapa no disponible aún, se registrará al abrir Mapa TX")
        }

        RadarFirebase.escucharPilotos { pilotos ->
            val todosLosPilotos = mutableListOf<PilotoRadar>()

            if (miUserId.isNotBlank() && miNombre.isNotBlank()) {
                val loc = try { app.locationProvider?.lastKnownLocation } catch (_: Exception) { null }
                val prefs = application?.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
                val latLocal = loc?.latitude ?: prefs?.getString("last_lat", null)?.toDoubleOrNull()
                val lonLocal = loc?.longitude ?: prefs?.getString("last_lon", null)?.toDoubleOrNull()

                if (latLocal != null && lonLocal != null && latLocal != 0.0 && lonLocal != 0.0) {
                    todosLosPilotos.add(PilotoRadar(
                        id = miUserId,
                        nombre = miNombre,
                        rango = miRango,
                        lat = latLocal,
                        lon = lonLocal,
                        avatarUrl = miAvatarUrl,
                        timestamp = System.currentTimeMillis(),
                        activo = true
                    ))
                }
            }

            todosLosPilotos.addAll(pilotos.filter { it.id != miUserId })
            mapaLayer?.actualizarPilotos(todosLosPilotos)

            todosLosPilotos.forEach { piloto ->
                if (RadarFirebase.obtenerAvatarCacheado(piloto.avatarUrl) == null && piloto.avatarUrl.isNotBlank()) {
                    alcance.launch {
                        val bitmap = RadarFirebase.descargarAvatar(piloto.avatarUrl, app)
                        if (bitmap != null) {
                            mapaLayer?.cachearAvatar(piloto.avatarUrl, bitmap)
                            mapaLayer?.actualizarPilotos(todosLosPilotos)
                        }
                    }
                }
            }
        }
        escuchando = true
        Log.d(ETIQUETA, "Radar activado, escuchando pilotos")
    }

    fun intentarRegistrarCapa() {
        val app = application ?: return
        val layer = mapaLayer ?: return
        try {
            val mapView = app.osmandMap.mapView ?: return
            mapView.addLayer(layer, Z_RADAR)
            Log.d(ETIQUETA, "Capa radar registrada (post-map init)")
        } catch (_: Exception) {}
    }

    fun intentarRegistrarCapaEventos() {
        val app = application ?: return
        val layer = eventosLayer ?: return
        try {
            val mapView = app.osmandMap.mapView ?: return
            mapView.addLayer(layer, Z_EVENTOS)
            Log.d(ETIQUETA, "Capa eventos registrada (post-map init)")
        } catch (_: Exception) {}
    }

    fun detener() {
        RadarFirebase.detenerEscucha()
        mapaLayer?.limpiarPilotos()
        escuchando = false
        Log.d(ETIQUETA, "Radar desactivado")
    }

    fun sincronizarEventosEnMapa(
        publications: List<Publication>,
        calendarEvents: List<BikerCalendarEvent>
    ) {
        val app = application

        val layer = eventosLayer ?: if (app != null) {
            EventosMapLayer(app).also {
                eventosLayer = it
                try {
                    val mapView = app.osmandMap.mapView
                    mapView?.addLayer(it, Z_EVENTOS)
                } catch (_: Exception) {}
            }
        } else return

        val marcadores = mutableListOf<EventosMapLayer.EventoMarcador>()

        for (pub in publications) {
            if (pub.locationCoordinates.isNullOrBlank()) continue
            if (pub.isEventFinished) continue

            val partes = pub.locationCoordinates!!.split(",")
            if (partes.size < 2) continue

            val lat = partes[0].trim().toDoubleOrNull() ?: continue
            val lon = partes[1].trim().toDoubleOrNull() ?: continue
            if (lat == 0.0 && lon == 0.0) continue

            marcadores.add(EventosMapLayer.EventoMarcador(
                lat = lat,
                lon = lon,
                titulo = pub.title.ifBlank { "Evento TX" },
                descripcion = pub.content.take(100),
                esCalendario = false
            ))
        }

        for (ev in calendarEvents) {
            if (ev.isEventFinished) continue

            val lat = if (ev.originLatitude != 0.0) ev.originLatitude else ev.destinationLatitude
            val lon = if (ev.originLongitude != 0.0) ev.originLongitude else ev.destinationLongitude
            if (lat == 0.0 && lon == 0.0) continue

            marcadores.add(EventosMapLayer.EventoMarcador(
                lat = lat,
                lon = lon,
                titulo = ev.title.ifBlank { "Evento Moto" },
                descripcion = ev.description.take(100),
                esCalendario = true
            ))
        }

        layer.actualizarEventos(marcadores)
        Log.d(ETIQUETA, "Eventos sincronizados en mapa: ${marcadores.size}")
    }

    fun obtenerEventos(): List<EventosMapLayer.EventoMarcador> {
        return eventosLayer?.getEventos() ?: emptyList()
    }

    fun limpiarEventosDelMapa() {
        eventosLayer?.limpiarEventos()
        Log.d(ETIQUETA, "Eventos limpiados del mapa")
    }

    fun liberar() {
        detener()
        alcance.cancel()
        application = null
        mapaLayer = null
        eventosLayer = null
    }
}
