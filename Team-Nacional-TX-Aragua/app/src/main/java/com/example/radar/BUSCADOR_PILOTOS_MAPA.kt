package com.example.radar

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
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
import net.osmand.plus.OsmandApplication
import java.util.concurrent.TimeUnit

object BuscadorPilotosMapa {

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
                    18 * density, 18 * density,
                    18 * density, 18 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.WHITE)
            }
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (24 * density).toInt())
        }

        val handlePill = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (4 * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 4 * density
                setColor(Color.parseColor("#CCCCCC"))
            }
        }
        rootLayout.addView(handlePill)

        val titleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (4 * density).toInt()
            }
        }

        val titleView = TextView(activity).apply {
            text = "Pilotos en el Mapa"
            setTextColor(Color.parseColor("#1A1A1A"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleRow.addView(titleView)

        val countBadge = TextView(activity).apply {
            text = "0 pilotos"
            setTextColor(Color.parseColor("#FF6D00"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(Color.parseColor("#FFF3E0"))
                setStroke((1 * density).toInt(), Color.parseColor("#FF6D00"))
            }
        }
        titleRow.addView(countBadge)
        rootLayout.addView(titleRow)

        val subtitleView = TextView(activity).apply {
            text = "Pilotos conectados al Radar TX en tiempo real"
            setTextColor(Color.parseColor("#666666"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
        rootLayout.addView(subtitleView)

        val searchBox = EditText(activity).apply {
            hint = "Buscar piloto por nombre o rango..."
            setHintTextColor(Color.parseColor("#999999"))
            setTextColor(Color.parseColor("#1A1A1A"))
            textSize = 14f
            setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#F5F5F5"))
                setStroke((1 * density).toInt(), Color.parseColor("#E0E0E0"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }
        rootLayout.addView(searchBox)

        val listView = ListView(activity).apply {
            divider = null
            dividerHeight = (6 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * density).toInt())
        }
        rootLayout.addView(listView)

        val emptyState = TextView(activity).apply {
            text = "Buscando pilotos conectados..."
            setTextColor(Color.parseColor("#888888"))
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

        val ahora = System.currentTimeMillis()
        val radarActivo = GestorRadar.estaRadarActivo() || TelemetriaGps.estaActivo(activity)
        val pilotosEnMemoria = if (radarActivo) {
            RadarFirebase.obtenerPilotosEnMemoria()
        } else {
            emptyList()
        }

        if (!radarActivo) {
            emptyState.text = "El Radar Táctico está desactivado.\nActívalo tocando el botón de radar en el mapa."
            listView.visibility = View.GONE
            countBadge.text = "Radar OFF"
        } else if (pilotosEnMemoria.isEmpty()) {
            emptyState.text = "No hay otros pilotos conectados al radar en este momento"
            listView.visibility = View.GONE
            countBadge.text = "0 pilotos"
        } else {
            emptyState.visibility = View.GONE
            listView.visibility = View.VISIBLE

            val adapter = PilotoMapaListAdapter(activity, pilotosEnMemoria, ahora)
            listView.adapter = adapter
            countBadge.text = "${adapter.count} pilotos"

            listView.setOnItemClickListener { _, _, position, _ ->
                val piloto = adapter.getItem(position)
                dialog.dismiss()
                navegarAPiloto(app, piloto)
                Toast.makeText(activity, "Centrando en: ${piloto.nombre}", Toast.LENGTH_SHORT).show()
            }

            searchBox.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    adapter.filtrar(s.toString())
                    val filtrados = adapter.count
                    countBadge.text = "$filtrados pilotos"
                    emptyState.visibility = if (filtrados == 0) View.VISIBLE else View.GONE
                    if (filtrados == 0) emptyState.text = "No se encontraron pilotos con '${s}'"
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun navegarAPiloto(app: OsmandApplication, piloto: PilotoRadar) {
        val mapView = app.osmandMap?.mapView ?: return
        mapView.setLatLon(piloto.lat, piloto.lon)
        if (mapView.zoom < 15) {
            mapView.setIntZoom(16)
        }
        mapView.refreshMap(true)
    }

    private class PilotoMapaListAdapter(
        private val context: Activity,
        private val todosLosPilotos: List<PilotoRadar>,
        private val ahora: Long
    ) : BaseAdapter() {

        private var pilotosVisibles: List<PilotoRadar> = ArrayList(todosLosPilotos)

        fun filtrar(texto: String) {
            val q = texto.trim().lowercase()
            pilotosVisibles = if (q.isBlank()) {
                ArrayList(todosLosPilotos)
            } else {
                todosLosPilotos.filter {
                    it.nombre.lowercase().contains(q) ||
                    it.rango.lowercase().contains(q)
                }
            }
            notifyDataSetChanged()
        }

        override fun getCount(): Int = pilotosVisibles.size
        override fun getItem(position: Int): PilotoRadar = pilotosVisibles[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val density = context.resources.displayMetrics.density
            val piloto = getItem(position)

            val minutos = TimeUnit.MILLISECONDS.toMinutes(ahora - piloto.timestamp)
            val tiempoAgo = when {
                minutos < 1 -> "ahora"
                minutos < 60 -> "hace ${minutos}m"
                minutos < 1440 -> "hace ${minutos / 60}h"
                else -> "hace ${minutos / 1440}d"
            }

            val colorRangoHex = when {
                piloto.rango.contains("Capitán", true) || piloto.rango.contains("Capitan", true) -> "#E53935"
                piloto.rango.contains("Mecánico", true) || piloto.rango.contains("Mecanico", true) -> "#1E88E5"
                piloto.rango.contains("Directiva", true) || piloto.rango.contains("Presidente", true) -> "#FDD835"
                piloto.rango.contains("Vocal", true) -> "#43A047"
                else -> "#FF6D00"
            }

            val root = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10 * density
                    setColor(Color.parseColor("#FAFAFA"))
                    setStroke((1 * density).toInt(), Color.parseColor("#E0E0E0"))
                }
            }

            val avatarContainer = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt()).apply {
                    marginEnd = (12 * density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#F0F0F0"))
                    setStroke((2 * density).toInt(), Color.parseColor(colorRangoHex))
                }
            }

            val avatarBitmap = RadarFirebase.obtenerAvatar(piloto.id, piloto.avatarUrl, context)
            val avatarView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt(), Gravity.CENTER)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            if (avatarBitmap != null) {
                avatarView.setImageBitmap(avatarBitmap)
            } else {
                val resId = context.resources.getIdentifier("logoteam", "drawable", context.packageName)
                if (resId != 0) avatarView.setImageResource(resId)
            }
            avatarContainer.addView(avatarView)
            root.addView(avatarContainer)

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvNombre = TextView(context).apply {
                text = piloto.nombre.ifBlank { "Piloto TX" }
                setTextColor(Color.parseColor("#1A1A1A"))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
            }
            textCol.addView(tvNombre)

            val rowInfo = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (2 * density).toInt()
                }
            }

            val tvRango = TextView(context).apply {
                text = piloto.rango.ifBlank { "Miembro" }
                setTextColor(Color.parseColor(colorRangoHex))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            }
            rowInfo.addView(tvRango)

            val tvPunto = TextView(context).apply {
                text = " · "
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 11f
            }
            rowInfo.addView(tvPunto)

            val tvTiempo = TextView(context).apply {
                text = tiempoAgo
                setTextColor(Color.parseColor("#888888"))
                textSize = 11f
            }
            rowInfo.addView(tvTiempo)
            textCol.addView(rowInfo)

            root.addView(textCol)

            val estadoPunto = View(context).apply {
                layoutParams = LinearLayout.LayoutParams((10 * density).toInt(), (10 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (piloto.activo) Color.parseColor("#4CAF50") else Color.parseColor("#AAAAAA"))
                }
            }
            root.addView(estadoPunto)

            return root
        }
    }
}
