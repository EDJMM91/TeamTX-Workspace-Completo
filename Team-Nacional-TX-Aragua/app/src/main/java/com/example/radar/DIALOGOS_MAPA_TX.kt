package com.example.radar

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.example.data.local.AppDatabase
import com.example.data.model.MemberProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.osmand.plus.OsmandApplication
import java.io.File

object DialogosMapaTx {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Muestra el Carnet TX táctico al tocar cualquier disco de avatar de piloto en el mapa.
     */
    @JvmStatic
    fun mostrarPiloto(activity: Activity, piloto: PilotoRadar) {
        val app = activity.application as? OsmandApplication ?: return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = activity.resources.displayMetrics.density
        val colorRangoHex = obtenerColorRangoHex(piloto.rango)
        val colorRangoInt = Color.parseColor(colorRangoHex)

        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    20 * density, 20 * density,
                    20 * density, 20 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#161616"))
            }
            setPadding((18 * density).toInt(), (12 * density).toInt(), (18 * density).toInt(), (24 * density).toInt())
        }

        // Handle superior
        val handlePill = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (4 * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (14 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 4 * density
                setColor(Color.parseColor("#555555"))
            }
        }
        rootLayout.addView(handlePill)

        // Cabecera: Título y botón cerrar
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val tvHeader = TextView(activity).apply {
            text = "CARNET TX - PILOTO EN VIVO"
            setTextColor(colorRangoInt)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(tvHeader)

        val btnCerrar = TextView(activity).apply {
            text = "✕"
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(btnCerrar)
        rootLayout.addView(headerRow)

        // Fila principal del Perfil: Foto + Info básica
        val profileRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
        }

        // Contenedor Foto de Perfil Circular
        val avatarContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams((72 * density).toInt(), (72 * density).toInt()).apply {
                marginEnd = (14 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#222222"))
                setStroke((3 * density).toInt(), colorRangoInt)
            }
        }

        val avatarImg = ImageView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        // Cargar foto local o por defecto
        val bmpAvatar = RadarFirebase.obtenerAvatar(piloto.id, piloto.avatarUrl, activity)
        if (bmpAvatar != null) {
            avatarImg.setImageBitmap(bmpAvatar)
        } else {
            val resId = activity.resources.getIdentifier("logoteam", "drawable", activity.packageName)
            if (resId != 0) avatarImg.setImageResource(resId)
        }
        avatarContainer.addView(avatarImg)
        profileRow.addView(avatarContainer)

        // Info: Nombre, Apodo, Rango
        val infoCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvNombre = TextView(activity).apply {
            text = piloto.nombre.ifBlank { "Piloto Team TX" }
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        infoCol.addView(tvNombre)

        val badgeRango = TextView(activity).apply {
            text = "🛡️ ${piloto.rango.ifBlank { "Miembro Oficial" }}"
            setTextColor(colorRangoInt)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), (2 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#242424"))
                setStroke((1 * density).toInt(), colorRangoInt)
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        infoCol.addView(badgeRango)

        val tvCoord = TextView(activity).apply {
            text = "📍 Lat: %.4f, Lon: %.4f".format(piloto.lat, piloto.lon)
            setTextColor(Color.parseColor("#777777"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        infoCol.addView(tvCoord)

        profileRow.addView(infoCol)
        rootLayout.addView(profileRow)

        // Tarjeta de Datos Detallados (Moto, Placa, Contacto)
        val detailsCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(Color.parseColor("#1F1F1F"))
                setStroke((1 * density).toInt(), Color.parseColor("#333333"))
            }
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
        }

        val tvMoto = TextView(activity).apply {
            text = "🏍️ Cargando datos de la moto y socio..."
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 13f
        }
        detailsCard.addView(tvMoto)

        val tvPlaca = TextView(activity).apply {
            text = ""
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        detailsCard.addView(tvPlaca)
        rootLayout.addView(detailsCard)

        // Botones de Acción
        val buttonsCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Botón: Acceder a Carnet TX
        val btnCarnet = Button(activity).apply {
            text = "🏍️ Acceder a Carnet TX Completo"
            setTextColor(Color.BLACK)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#FF9800"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt()).apply {
                bottomMargin = (8 * density).toInt()
            }
            setOnClickListener {
                dialog.dismiss()
                activity.finish() // Regresa a la app para ver el Carnet TX
            }
        }
        buttonsCol.addView(btnCarnet)

        // Botón: Centrar Mapa
        val btnCentrar = Button(activity).apply {
            text = "📍 Centrar Piloto en el Mapa"
            setTextColor(Color.WHITE)
            textSize = 14f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#262626"))
                setStroke((1 * density).toInt(), Color.parseColor("#444444"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt())
            setOnClickListener {
                dialog.dismiss()
                val mapView = app.osmandMap?.mapView
                mapView?.setLatLon(piloto.lat, piloto.lon)
                if ((mapView?.zoom ?: 0) < 16) {
                    mapView?.setIntZoom(16)
                }
                mapView?.refreshMap(true)
            }
        }
        buttonsCol.addView(btnCentrar)
        rootLayout.addView(buttonsCol)

        dialog.setContentView(rootLayout)
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.BOTTOM)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()

        // Carga asíncrona de datos adicionales desde la base de datos local / nube
        scope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(activity, CoroutineScope(Dispatchers.IO))
                val members = db.memberDao().getAllMembers().first()
                val member = members.firstOrNull { it.id.toString() == piloto.id || it.fullName.equals(piloto.nombre, ignoreCase = true) }

                withContext(Dispatchers.Main) {
                    if (member != null) {
                        tvNombre.text = member.fullName
                        val motoInfo = "${member.bikeBrand} ${member.bikeModel} (${member.bikeColor})".trim()
                        tvMoto.text = "🏍️ Moto: ${if (motoInfo.isNotBlank()) motoInfo else "Keeway TX 200"}"
                        tvPlaca.text = "🏷️ Placa: ${member.bikePlate.ifBlank { "Sin placa registrada" }}  •  N° Socio: ${member.memberNumber.ifBlank { "-" }}"
                        tvPlaca.visibility = View.VISIBLE
                    } else {
                        tvMoto.text = "🏍️ Moto: Keeway TX 200 (Oficial)"
                        tvPlaca.text = "🏷️ Estado: Conectado en Vivo al Radar"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvMoto.text = "🏍️ Moto: Keeway TX 200"
                    tvPlaca.text = "🏷️ Estado: Conectado en Vivo al Radar"
                }
            }
        }
    }

    /**
     * Muestra el detalle interactivo al tocar cualquier marcador de aviso o evento en el mapa.
     */
    @JvmStatic
    fun mostrarEvento(activity: Activity, evento: EventosMapLayer.EventoMarcador) {
        val app = activity.application as? OsmandApplication ?: return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = activity.resources.displayMetrics.density
        val esCalendario = evento.esCalendario
        val colorTemaHex = if (esCalendario) "#E53935" else "#FF9800"
        val colorTemaInt = Color.parseColor(colorTemaHex)
        val etiquetaTipo = if (esCalendario) "🗓️ EVENTO DE CALENDARIO" else "📢 AVISO DEL MURO"

        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    20 * density, 20 * density,
                    20 * density, 20 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#161616"))
            }
            setPadding((18 * density).toInt(), (12 * density).toInt(), (18 * density).toInt(), (24 * density).toInt())
        }

        // Handle superior
        val handlePill = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (4 * density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (14 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 4 * density
                setColor(Color.parseColor("#555555"))
            }
        }
        rootLayout.addView(handlePill)

        // Cabecera: Tipo e icono cerrar
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val tvHeader = TextView(activity).apply {
            text = etiquetaTipo
            setTextColor(colorTemaInt)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(tvHeader)

        val btnCerrar = TextView(activity).apply {
            text = "✕"
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(btnCerrar)
        rootLayout.addView(headerRow)

        // Fila de Título con Logo Team TX
        val titleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * density).toInt()
            }
        }

        val logoContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt()).apply {
                marginEnd = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#222222"))
                setStroke((2 * density).toInt(), colorTemaInt)
            }
        }

        val logoImg = ImageView(activity).apply {
            layoutParams = FrameLayout.LayoutParams((38 * density).toInt(), (38 * density).toInt(), Gravity.CENTER)
            val resId = activity.resources.getIdentifier("logoteam", "drawable", activity.packageName)
            if (resId != 0) setImageResource(resId)
        }
        logoContainer.addView(logoImg)
        titleRow.addView(logoContainer)

        val tvTitulo = TextView(activity).apply {
            text = evento.titulo
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleRow.addView(tvTitulo)
        rootLayout.addView(titleRow)

        // Tarjeta de Descripción y Coordenadas
        val descCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(Color.parseColor("#1F1F1F"))
                setStroke((1 * density).toInt(), Color.parseColor("#333333"))
            }
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * density).toInt()
            }
        }

        val tvDesc = TextView(activity).apply {
            text = evento.descripcion.ifBlank { "Sin descripción adicional publicada." }
            setTextColor(Color.parseColor("#DDDDDD"))
            textSize = 13f
        }
        descCard.addView(tvDesc)

        val tvCoord = TextView(activity).apply {
            text = "📍 Ubicación: Lat %.5f, Lon %.5f".format(evento.lat, evento.lon)
            setTextColor(Color.parseColor("#888888"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * density).toInt()
            }
        }
        descCard.addView(tvCoord)
        rootLayout.addView(descCard)

        // Botón: Navegar / Centrar aquí
        val btnNavegar = Button(activity).apply {
            text = "🏍️ Navegar / Centrar en este Sitio"
            setTextColor(Color.BLACK)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(colorTemaInt)
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt())
            setOnClickListener {
                dialog.dismiss()
                val mapView = app.osmandMap?.mapView
                mapView?.setLatLon(evento.lat, evento.lon)
                if ((mapView?.zoom ?: 0) < 16) {
                    mapView?.setIntZoom(16)
                }
                mapView?.refreshMap(true)
                Toast.makeText(activity, "🏍️ Destino enfocado: ${evento.titulo}", Toast.LENGTH_SHORT).show()
            }
        }
        rootLayout.addView(btnNavegar)

        dialog.setContentView(rootLayout)
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.BOTTOM)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun obtenerColorRangoHex(rango: String): String {
        return when {
            rango.contains("Capitán", true) || rango.contains("Capitan", true) -> "#E53935"
            rango.contains("Mecánico", true) || rango.contains("Mecanico", true) -> "#1E88E5"
            rango.contains("Directiva", true) || rango.contains("Presidente", true) -> "#FDD835"
            rango.contains("Vocal", true) -> "#43A047"
            else -> "#FF9800"
        }
    }
}
