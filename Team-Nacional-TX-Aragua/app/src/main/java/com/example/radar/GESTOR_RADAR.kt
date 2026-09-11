package com.example.radar

import android.content.Context
import android.content.ClipboardManager
import android.util.Log
import com.example.data.model.MemberProfile
import com.example.data.model.BikerCalendarEvent
import com.example.data.model.Publication
import com.example.data.model.WorkshopDirectoryItem
import com.example.data.model.EmergencyAlert
import com.example.data.model.EmergencyStatus
import com.example.data.remote.PerfilNube
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.osmand.plus.OsmandApplication

object GestorRadar {

    private const val ETIQUETA = "GESTOR_RADAR"
    private const val Z_RADAR = 7.4f
    private const val Z_EVENTOS = 7.0f
    private const val Z_DIRECTORIO = 7.1f

    private var application: OsmandApplication? = null
    private var mapaLayer: RadarMapLayer? = null
    private var eventosLayer: EventosMapLayer? = null
    private var directorioLayer: DirectorioMapLayer? = null
    private var mapaActivityRef: java.lang.ref.WeakReference<net.osmand.plus.activities.MapActivity>? = null
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val alcance = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var escuchando = false
    private var miUserId: String = ""
    private var miNombre: String = ""
    private var miRango: String = ""
    private var miAvatarUrl: String = ""

    @JvmStatic
    fun registrarMapActivity(activity: net.osmand.plus.activities.MapActivity?) {
        val currentActivity = (activity as Any?) as? android.app.Activity
        // Remover listener previo si existía
        try {
            val oldAct = (mapaActivityRef?.get() as Any?) as? android.app.Activity
            if (oldAct != null && clipListener != null) {
                val clipManager = oldAct.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipManager?.removePrimaryClipChangedListener(clipListener)
            }
        } catch (_: Exception) {}

        mapaActivityRef = if (activity != null) java.lang.ref.WeakReference(activity) else null
        mapaLayer?.setMapActivity(activity)
        eventosLayer?.setMapActivity(activity)
        directorioLayer?.setMapActivity(activity)
        Log.d(ETIQUETA, "MapActivity registrada en GestorRadar: ${activity != null}")

        // Si el usuario está buscando una dirección para un aviso o evento, escuchar copias de coordenadas
        if (currentActivity != null) {
            try {
                val clipboard = currentActivity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipListener = ClipboardManager.OnPrimaryClipChangedListener {
                    if (com.example.mapa.GestorSeleccionMapa.modoBuscandoCoordenada) {
                        val coords = com.example.mapa.GestorPortapapeles.leerCoordenadaValida(currentActivity)
                        if (coords != null) {
                            com.example.mapa.GestorSeleccionMapa.registrarCoordenadaDetectada(currentActivity, coords) {
                                currentActivity.finish()
                            }
                        }
                    }
                }
                clipboard?.addPrimaryClipChangedListener(clipListener)
            } catch (e: Exception) {
                Log.e(ETIQUETA, "Error registrando escucha de portapapeles en mapa", e)
            }
        }
    }

    @JvmStatic
    fun obtenerMapActivity(): net.osmand.plus.activities.MapActivity? {
        return mapaActivityRef?.get()
    }

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
        miAvatarUrl = RadarFirebase.obtenerAvatarComoBase64(app, avatarUrl)

        RadarFirebase.init(app)
        RadarFirebase.obtenerAvatarLocal(app)

        if (miAvatarUrl.isNotBlank()) {
            alcance.launch {
                RadarFirebase.descargarAvatar(miAvatarUrl, app)
            }
        }

        val layer = RadarMapLayer(app)
        mapaActivityRef?.get()?.let { layer.setMapActivity(it) }
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

    fun intentarRegistrarCapaDirectorio() {
        val app = application ?: return
        val layer = directorioLayer ?: return
        try {
            val mapView = app.osmandMap.mapView ?: return
            mapView.addLayer(layer, Z_DIRECTORIO)
            Log.d(ETIQUETA, "Capa directorio registrada (post-map init)")
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
        calendarEvents: List<BikerCalendarEvent>,
        emergencyAlerts: List<EmergencyAlert> = emptyList()
    ) {
        val app = application

        val layer = eventosLayer ?: if (app != null) {
            EventosMapLayer(app).also {
                mapaActivityRef?.get()?.let { act -> it.setMapActivity(act) }
                eventosLayer = it
                try {
                    val mapView = app.osmandMap.mapView
                    mapView?.addLayer(it, Z_EVENTOS)
                } catch (_: Exception) {}
            }
        } else return

        val marcadores = mutableListOf<EventosMapLayer.EventoMarcador>()

        // 1. Alertas SOS viales activas (🚨 Desaparecen inmediatamente al resolverse)
        for (alert in emergencyAlerts) {
            if (alert.status == EmergencyStatus.RESUELTA) continue
            if (alert.coordinateLat == 0.0 && alert.coordinateLng == 0.0) continue

            val tituloSos = "🚨 SOS [${alert.emergencyType.levelTag}]: ${alert.reporterName}"
            val descSos = buildString {
                append("Tipo: ${alert.emergencyType.label} (${alert.emergencyType.levelName})")
                if (alert.bikeDetails.isNotBlank()) append(" • Moto: ${alert.bikeDetails}")
                if (alert.details.isNotBlank()) append(" • ${alert.details}")
                if (alert.reporterPhone.isNotBlank()) append(" • Tlf: ${alert.reporterPhone}")
            }

            marcadores.add(EventosMapLayer.EventoMarcador(
                lat = alert.coordinateLat,
                lon = alert.coordinateLng,
                titulo = tituloSos,
                descripcion = descSos.take(150),
                esCalendario = false
            ))
        }

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
        Log.d(ETIQUETA, "Eventos y SOS sincronizados en mapa: ${marcadores.size} (SOS activos: ${emergencyAlerts.count { it.status != EmergencyStatus.RESUELTA }})")
    }

    fun obtenerEventos(): List<EventosMapLayer.EventoMarcador> {
        return eventosLayer?.getEventos() ?: emptyList()
    }

    fun limpiarEventosDelMapa() {
        eventosLayer?.limpiarEventos()
        Log.d(ETIQUETA, "Eventos limpiados del mapa")
    }

    fun sincronizarDirectorioEnMapa(
        workshops: List<WorkshopDirectoryItem>,
        mostrar: Boolean = true
    ) {
        val app = application

        val layer = directorioLayer ?: if (app != null) {
            DirectorioMapLayer(app).also {
                mapaActivityRef?.get()?.let { act -> it.setMapActivity(act) }
                directorioLayer = it
                try {
                    val mapView = app.osmandMap.mapView
                    mapView?.addLayer(it, Z_DIRECTORIO)
                } catch (_: Exception) {}
            }
        } else return

        if (!mostrar) {
            layer.limpiarDirectorios()
            Log.d(ETIQUETA, "Capa directorio ocultada")
            return
        }

        val marcadores = mutableListOf<DirectorioMapLayer.DirectorioMarcador>()
        for (w in workshops) {
            if (w.latitude == 0.0 && w.longitude == 0.0) continue
            val hasCashea = (w.hasCredit || w.creditPlatforms.isNotBlank()) && w.creditPlatforms.contains("Cashea", ignoreCase = true)
            marcadores.add(
                DirectorioMapLayer.DirectorioMarcador(
                    id = w.id,
                    lat = w.latitude,
                    lon = w.longitude,
                    nombre = w.name,
                    tipo = w.type,
                    direccion = w.address,
                    ciudad = w.city,
                    estado = w.state,
                    telefono = w.phone,
                    whatsapp = w.whatsapp,
                    tieneCashea = hasCashea,
                    plataformasCredito = w.creditPlatforms,
                    rating = w.rating,
                    notas = w.notes,
                    googleMapsUrl = w.googleMapsUrl
                )
            )
        }

        layer.actualizarDirectorios(marcadores)
        Log.d(ETIQUETA, "Directorios sincronizados en mapa: ${marcadores.size}")
    }

    fun limpiarDirectorioDelMapa() {
        directorioLayer?.limpiarDirectorios()
        Log.d(ETIQUETA, "Directorios limpiados del mapa")
    }

    @JvmStatic
    fun alternarDirectorio(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
        val actual = prefs.getBoolean("mostrar_directorio_en_mapa", true)
        val nuevo = !actual
        prefs.edit().putBoolean("mostrar_directorio_en_mapa", nuevo).apply()

        alcance.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
                val workshops = db.workshopDirectoryDao().getAllWorkshops().first()
                val list = if (workshops.isEmpty()) com.example.data.local.AppDatabase.INITIAL_WORKSHOPS else workshops
                withContext(Dispatchers.Main) {
                    sincronizarDirectorioEnMapa(list, nuevo)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    sincronizarDirectorioEnMapa(com.example.data.local.AppDatabase.INITIAL_WORKSHOPS, nuevo)
                }
            }
        }
        return nuevo
    }

    @JvmStatic
    fun estaDirectorioVisible(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean("mostrar_directorio_en_mapa", true)
    }

    private const val Z_SITIOS = 7.2f
    private var sitiosLayer: SitiosInteresMapLayer? = null

    @JvmStatic
    fun sincronizarSitiosInteresEnMapa(sitios: List<com.example.data.model.BikerInterestPoint>, mostrar: Boolean) {
        val app = application ?: return
        val mapView = app.osmandMap?.mapView ?: return

        if (!mostrar) {
            sitiosLayer?.limpiarSitios()
            return
        }

        if (sitiosLayer == null) {
            val layer = SitiosInteresMapLayer(app)
            mapaActivityRef?.get()?.let { layer.setMapActivity(it) }
            layer.setOnSitioSeleccionado { marcador ->
                val act = (mapaActivityRef?.get() as Any?) as? android.app.Activity
                if (act != null) {
                    DialogosMapaTx.mostrarSitioInteres(act, marcador)
                }
            }
            try {
                mapView.addLayer(layer, Z_SITIOS)
                sitiosLayer = layer
            } catch (e: Exception) {
                Log.w(ETIQUETA, "Error registrando SitiosInteresMapLayer: ${e.message}")
            }
        }

        val marcadores = sitios.map { s ->
            SitiosInteresMapLayer.SitioInteresMarcador(
                id = s.id,
                lat = s.latitude,
                lon = s.longitude,
                nombre = s.name,
                categoria = s.category,
                descripcion = s.description,
                direccion = s.address,
                telefono = s.phone,
                imageUrl = s.imageUrl,
                iconoDrawableName = s.iconDrawableName,
                likesCount = s.likesCount,
                dislikesCount = s.dislikesCount
            )
        }
        sitiosLayer?.actualizarSitios(marcadores)
        Log.d(ETIQUETA, "Sitios de interés sincronizados en mapa: ${marcadores.size}")
    }

    fun liberar() {
        detener()
        alcance.cancel()
        application = null
        mapaLayer = null
        eventosLayer = null
        directorioLayer = null
        sitiosLayer = null
    }
}
