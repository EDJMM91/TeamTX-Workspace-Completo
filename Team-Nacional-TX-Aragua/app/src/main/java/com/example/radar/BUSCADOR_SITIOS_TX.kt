package com.example.radar

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.example.data.local.AppDatabase
import com.example.data.model.BikerCalendarEvent
import com.example.data.model.Publication
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.osmand.plus.OsmandApplication

enum class TipoSitioTx(val etiqueta: String, val colorHex: String, val iconoEmoji: String) {
    AVISO_MURO("Aviso Muro", "#FF9800", "📢"),
    CALENDARIO("Calendario", "#E53935", "🗓️"),
    FAVORITO("Favorito", "#FDD835", "⭐"),
    MARCADOR_MAPA("Punto Mapa", "#4CAF50", "📍")
}

data class SitioMapaTx(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val direccion: String,
    val tipo: TipoSitioTx,
    val lat: Double,
    val lon: Double,
    val timestamp: Long = 0L
)

object BuscadorSitiosTx {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @JvmStatic
    fun mostrar(activity: Activity) {
        val app = activity.application as? OsmandApplication ?: return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = activity.resources.displayMetrics.density

        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    18 * density, 18 * density, // Top-left
                    18 * density, 18 * density, // Top-right
                    0f, 0f, 0f, 0f             // Bottom
                )
                setColor(Color.parseColor("#161616"))
            }
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (24 * density).toInt())
        }

        // 1. Barra superior / Handle
        val handlePill = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (4 * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 4 * density
                setColor(Color.parseColor("#555555"))
            }
        }
        rootLayout.addView(handlePill)

        // 2. Fila de Título y Contador
        val titleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (4 * density).toInt()
            }
        }

        val titleView = TextView(activity).apply {
            text = "Direcciones y Sitios Team TX"
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleRow.addView(titleView)

        val countBadge = TextView(activity).apply {
            text = "0 sitios"
            setTextColor(Color.parseColor("#FF9800"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(Color.parseColor("#262626"))
                setStroke((1 * density).toInt(), Color.parseColor("#FF9800"))
            }
        }
        titleRow.addView(countBadge)
        rootLayout.addView(titleRow)

        val subtitleView = TextView(activity).apply {
            text = "Muro de avisos, Calendario motero y Favoritos"
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
        rootLayout.addView(subtitleView)

        // 3. Campo de Búsqueda
        val searchBox = EditText(activity).apply {
            hint = "Buscar dirección, evento, calle o aviso..."
            setHintTextColor(Color.parseColor("#777777"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#222222"))
                setStroke((1 * density).toInt(), Color.parseColor("#383838"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
        rootLayout.addView(searchBox)

        // 4. Lista de Sitios
        val listView = ListView(activity).apply {
            divider = null
            dividerHeight = (8 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * density).toInt())
        }
        rootLayout.addView(listView)

        // 5. Estado de carga / vacío
        val emptyState = TextView(activity).apply {
            text = "Cargando sitios del mapa..."
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, (30 * density).toInt(), 0, (30 * density).toInt())
            visibility = View.VISIBLE
        }
        rootLayout.addView(emptyState)

        dialog.setContentView(rootLayout)
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.BOTTOM)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()

        // 6. Carga asíncrona de sitios
        scope.launch(Dispatchers.IO) {
            val listaSitios = mutableListOf<SitioMapaTx>()

            // A) Desde GestorRadar.obtenerEventos()
            val eventosRadar = GestorRadar.obtenerEventos()
            for (ev in eventosRadar) {
                listaSitios.add(
                    SitioMapaTx(
                        id = "radar_${ev.lat}_${ev.lon}",
                        titulo = ev.titulo,
                        descripcion = ev.descripcion,
                        direccion = if (ev.esCalendario) "Evento del Calendario Motero" else "Aviso publicado en el Muro",
                        tipo = if (ev.esCalendario) TipoSitioTx.CALENDARIO else TipoSitioTx.AVISO_MURO,
                        lat = ev.lat,
                        lon = ev.lon,
                        timestamp = 0L
                    )
                )
            }

            // B) Desde Base de Datos Room (Publicaciones con ubicación y Eventos de Calendario)
            try {
                val db = AppDatabase.getDatabase(activity, CoroutineScope(Dispatchers.IO))
                val pubs = db.publicationDao().getAllPublications().first()
                for (p in pubs) {
                    if (p.locationCoordinates.isNullOrBlank()) continue
                    val partes = p.locationCoordinates!!.split(",")
                    if (partes.size >= 2) {
                        val lat = partes[0].trim().toDoubleOrNull()
                        val lon = partes[1].trim().toDoubleOrNull()
                        if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                            val existe = listaSitios.any { it.lat == lat && it.lon == lon }
                            if (!existe) {
                                listaSitios.add(
                                    SitioMapaTx(
                                        id = "pub_${p.id}",
                                        titulo = if (p.title.isNotBlank()) p.title else (p.locationName ?: "Aviso en Mapa"),
                                        descripcion = p.content.take(100),
                                        direccion = p.locationName ?: "Ubicación en el Muro",
                                        tipo = TipoSitioTx.AVISO_MURO,
                                        lat = lat,
                                        lon = lon,
                                        timestamp = p.timestamp
                                    )
                                )
                            }
                        }
                    }
                }

                val calendarEvents = db.calendarDao().getAllEvents().first()
                for (c in calendarEvents) {
                    val lat = if (c.originLatitude != 0.0) c.originLatitude else c.destinationLatitude
                    val lon = if (c.originLongitude != 0.0) c.originLongitude else c.destinationLongitude
                    if (lat != 0.0 || lon != 0.0) {
                        val existe = listaSitios.any { it.lat == lat && it.lon == lon }
                        if (!existe) {
                            val dir = if (c.originAddress.isNotBlank()) c.originAddress else (if (c.destinationAddress.isNotBlank()) c.destinationAddress else "Calendario Motero")
                            listaSitios.add(
                                SitioMapaTx(
                                    id = "cal_${c.id}",
                                    titulo = c.title,
                                    descripcion = c.description.take(100),
                                    direccion = dir,
                                    tipo = TipoSitioTx.CALENDARIO,
                                    lat = lat,
                                    lon = lon,
                                    timestamp = 0L
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("BUSCADOR_TX", "Error leyendo base de datos: ${e.message}")
            }

            // C) Desde Favoritos de OsmAnd
            try {
                val favHelper = app.javaClass.getMethod("getFavorites").invoke(app)
                if (favHelper != null) {
                    val points = favHelper.javaClass.getMethod("getFavoritePoints").invoke(favHelper) as? List<*>
                    if (points != null) {
                        for (fav in points) {
                            if (fav == null) continue
                            val name = (fav.javaClass.getMethod("getName").invoke(fav) as? String) ?: "Favorito"
                            val desc = (fav.javaClass.getMethod("getDescription").invoke(fav) as? String) ?: "Punto guardado en favoritos"
                            val cat = (fav.javaClass.getMethod("getCategory").invoke(fav) as? String) ?: "Favoritos"
                            val lat = (fav.javaClass.getMethod("getLatitude").invoke(fav) as? Double) ?: 0.0
                            val lon = (fav.javaClass.getMethod("getLongitude").invoke(fav) as? Double) ?: 0.0
                            if (lat != 0.0 || lon != 0.0) {
                                listaSitios.add(
                                    SitioMapaTx(
                                        id = "fav_${name}_${lat}",
                                        titulo = name,
                                        descripcion = desc,
                                        direccion = cat,
                                        tipo = TipoSitioTx.FAVORITO,
                                        lat = lat,
                                        lon = lon,
                                        timestamp = 0L
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("BUSCADOR_TX", "Error leyendo favoritos: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                countBadge.text = "${listaSitios.size} sitios"
                if (listaSitios.isEmpty()) {
                    emptyState.text = "No hay direcciones o avisos guardados aún en el mapa"
                    emptyState.visibility = View.VISIBLE
                    listView.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    listView.visibility = View.VISIBLE

                    val adapter = SitioListAdapter(activity, listaSitios)
                    listView.adapter = adapter

                    listView.setOnItemClickListener { _, _, position, _ ->
                        val sitioSeleccionado = adapter.getItem(position)
                        dialog.dismiss()
                        navegarASitio(app, sitioSeleccionado)
                        Toast.makeText(activity, "🏍️ Destino: ${sitioSeleccionado.titulo}", Toast.LENGTH_SHORT).show()
                    }

                    searchBox.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            adapter.filtrar(s.toString())
                            val filtrados = adapter.count
                            countBadge.text = "$filtrados sitios"
                            emptyState.visibility = if (filtrados == 0) View.VISIBLE else View.GONE
                            if (filtrados == 0) emptyState.text = "No se encontraron sitios con '$s'"
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    })
                }
            }
        }
    }

    private fun navegarASitio(app: OsmandApplication, sitio: SitioMapaTx) {
        val mapView = app.osmandMap?.mapView ?: return
        mapView.setLatLon(sitio.lat, sitio.lon)
        if (mapView.zoom < 15) {
            mapView.setIntZoom(16)
        }
        mapView.refreshMap(true)
    }

    private class SitioListAdapter(
        private val context: Context,
        private val todosLosSitios: List<SitioMapaTx>
    ) : BaseAdapter() {

        private var sitiosVisibles: List<SitioMapaTx> = ArrayList(todosLosSitios)

        fun filtrar(texto: String) {
            val q = texto.trim().lowercase()
            sitiosVisibles = if (q.isBlank()) {
                ArrayList(todosLosSitios)
            } else {
                todosLosSitios.filter {
                    it.titulo.lowercase().contains(q) ||
                    it.descripcion.lowercase().contains(q) ||
                    it.direccion.lowercase().contains(q) ||
                    it.tipo.etiqueta.lowercase().contains(q)
                }
            }
            notifyDataSetChanged()
        }

        override fun getCount(): Int = sitiosVisibles.size
        override fun getItem(position: Int): SitioMapaTx = sitiosVisibles[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val density = context.resources.displayMetrics.density
            val sitio = getItem(position)

            val root = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10 * density
                    setColor(Color.parseColor("#1F1F1F"))
                    setStroke((1 * density).toInt(), Color.parseColor("#333333"))
                }
            }

            // Icono Logo Team circular
            val iconContainer = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt()).apply {
                    marginEnd = (12 * density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#222222"))
                    setStroke((2 * density).toInt(), Color.parseColor(sitio.tipo.colorHex))
                }
            }

            val iconLogo = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt(), Gravity.CENTER)
                val resId = context.resources.getIdentifier("logoteam", "drawable", context.packageName)
                if (resId != 0) setImageResource(resId)
            }
            iconContainer.addView(iconLogo)
            root.addView(iconContainer)

            // Columna de información
            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val rowTop = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvTitulo = TextView(context).apply {
                text = sitio.titulo
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            rowTop.addView(tvTitulo)

            val tvBadge = TextView(context).apply {
                text = "${sitio.tipo.iconoEmoji} ${sitio.tipo.etiqueta}"
                setTextColor(Color.parseColor(sitio.tipo.colorHex))
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#2A2A2A"))
                    setStroke((1 * density).toInt(), Color.parseColor(sitio.tipo.colorHex))
                }
            }
            rowTop.addView(tvBadge)
            textCol.addView(rowTop)

            val tvDesc = TextView(context).apply {
                text = if (sitio.descripcion.isNotBlank()) sitio.descripcion else "Sin descripción adicional"
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 12f
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (2 * density).toInt()
                }
            }
            textCol.addView(tvDesc)

            val tvDir = TextView(context).apply {
                text = "📍 ${sitio.direccion}"
                setTextColor(Color.parseColor("#777777"))
                textSize = 11f
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (2 * density).toInt()
                }
            }
            textCol.addView(tvDir)

            root.addView(textCol)

            // Icono de flecha a la derecha
            val navIcon = TextView(context).apply {
                text = "➔"
                setTextColor(Color.parseColor("#FF9800"))
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding((8 * density).toInt(), 0, (4 * density).toInt(), 0)
            }
            root.addView(navIcon)

            return root
        }
    }
}
