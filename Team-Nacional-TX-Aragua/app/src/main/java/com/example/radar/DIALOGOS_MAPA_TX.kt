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
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import com.aistudio.teamtxvzla.R
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.osmand.plus.OsmandApplication
import java.io.File

object DialogosMapaTx {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Muestra el detalle interactivo al tocar cualquier disco de avatar o icono de emergencia en el mapa.
     * Si el piloto tiene una alerta SOS vial activa, abre la tarjeta de emergencia
     * con su icono alusivo personalizado y opciones tácticas de resolución.
     */
    @JvmStatic
    fun mostrarPiloto(activity: Activity, piloto: PilotoRadar) {
        if (!piloto.alertaSos.isNullOrBlank()) {
            mostrarDetalleEmergenciaSos(activity, piloto)
        } else {
            mostrarCarnetPiloto(activity, piloto)
        }
    }

    /**
     * Muestra el Carnet TX táctico al tocar el disco de avatar de piloto en el mapa.
     */
    @JvmStatic
    fun mostrarCarnetPiloto(activity: Activity, piloto: PilotoRadar) {
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
                val prefs = activity.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
                prefs.edit().putString("target_carnet_pilot_id", piloto.id).apply()
                activity.finish() // Regresa a la app para ver el Carnet TX en modo lectura
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
     * Despliega el detalle completo de una emergencia SOS activa al tocar el avatar o su icono tocándolo.
     * Incluye:
     * - Icono alusivo personalizado según el tipo de emergencia (sos_gasolina, sos_mecanico, sos_caida, sos_choque, etc.)
     * - Nivel de severidad y protocolo de acción
     * - Datos del piloto y moto
     * - Coordenadas GPS con botón interactivo para copiar al portapapeles
     * - Botones de acción directa: Llamar, WhatsApp SOS, Centrar en Mapa
     * - Botón para Ver Carnet completo del piloto
     * - Botón para que el usuario o directiva marque la emergencia como RESUELTA
     */
    @JvmStatic
    fun mostrarDetalleEmergenciaSos(activity: Activity, piloto: PilotoRadar) {
        val app = activity.application as? OsmandApplication ?: return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = activity.resources.displayMetrics.density
        val config = RadarMapLayer.obtenerConfiguracionSos(piloto.alertaSos)
        val colorAlertaInt = Color.parseColor(config.colorHex)

        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    20 * density, 20 * density,
                    20 * density, 20 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#121212"))
            }
            setPadding((18 * density).toInt(), (12 * density).toInt(), (18 * density).toInt(), (20 * density).toInt())
        }

        // Handle superior
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

        // Cabecera: Título SOS y botón cerrar
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val tvHeader = TextView(activity).apply {
            text = "🚨 ALERTA SOS VIAL EN CURSO"
            setTextColor(colorAlertaInt)
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

        // ScrollView para soportar todo el contenido cómodamente en cualquier pantalla
        val scrollView = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        val contentContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        // TARJETA PRINCIPAL: ICONO ALUSIVO + TIPO DE ALERTA + NIVEL
        val emergencyCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(Color.parseColor("#1C1C1C"))
                setStroke((2 * density).toInt(), colorAlertaInt)
            }
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        // Contenedor del Icono Alusivo Personalizado
        val iconContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams((60 * density).toInt(), (60 * density).toInt()).apply {
                marginEnd = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#261414"))
                setStroke((2 * density).toInt(), colorAlertaInt)
            }
        }

        val ivSosIcon = ImageView(activity).apply {
            layoutParams = FrameLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt(), Gravity.CENTER)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val resId = activity.resources.getIdentifier(config.nombreDrawable, "drawable", activity.packageName)
            if (resId != 0) {
                setImageResource(resId)
            } else {
                val fallbackRes = if (config.nombreDrawable == "sos_choque" || config.nombreDrawable == "sos_caida" || config.nombreDrawable == "sos_medico") {
                    R.drawable.emergencia
                } else {
                    R.drawable.precaucion
                }
                setImageResource(fallbackRes)
            }
        }
        iconContainer.addView(ivSosIcon)
        emergencyCard.addView(iconContainer)

        val emergencyTextCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val badgeNivel = TextView(activity).apply {
            text = config.nivelTag
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), (2 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6 * density
                setColor(colorAlertaInt)
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        emergencyTextCol.addView(badgeNivel)

        val tvTituloEmergencia = TextView(activity).apply {
            text = config.titulo
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        emergencyTextCol.addView(tvTituloEmergencia)

        val tvDetalleSos = TextView(activity).apply {
            text = "Detalle: ${piloto.alertaSos ?: "Auxilio vial reportado"}"
            setTextColor(Color.parseColor("#FFA726"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (2 * density).toInt()
            }
        }
        emergencyTextCol.addView(tvDetalleSos)

        val tvDrawableName = TextView(activity).apply {
            text = "🏷️ Icono: ${config.nombreDrawable}"
            setTextColor(Color.parseColor("#888888"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (2 * density).toInt()
            }
        }
        emergencyTextCol.addView(tvDrawableName)

        emergencyCard.addView(emergencyTextCol)
        contentContainer.addView(emergencyCard)

        // TARJETA DE PILOTO AFECTADO
        val pilotCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(Color.parseColor("#1A1A1A"))
                setStroke((1 * density).toInt(), Color.parseColor("#333333"))
            }
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val tvPilotoNombre = TextView(activity).apply {
            text = "👤 Piloto: ${piloto.nombre.ifBlank { "Piloto Team TX" }} (${piloto.rango})"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        pilotCard.addView(tvPilotoNombre)

        val tvMotoInfo = TextView(activity).apply {
            text = "🏍️ Moto: Keeway TX 200 • Consultando registro..."
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        pilotCard.addView(tvMotoInfo)

        var telefonoPiloto = ""
        val tvContactoInfo = TextView(activity).apply {
            text = "📞 Teléfono: Consultando carnet..."
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (2 * density).toInt()
            }
        }
        pilotCard.addView(tvContactoInfo)
        contentContainer.addView(pilotCard)

        // TARJETA DE COORDENADAS GPS + BOTÓN COPIAR
        val gpsCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#182026"))
                setStroke((1 * density).toInt(), Color.parseColor("#2680C2"))
            }
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val tvGps = TextView(activity).apply {
            text = "📍 GPS: %.5f, %.5f".format(piloto.lat, piloto.lon)
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        gpsCard.addView(tvGps)

        val btnCopiarGps = Button(activity).apply {
            text = "📋 Copiar GPS"
            setTextColor(Color.BLACK)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6 * density
                setColor(Color.parseColor("#64B5F6"))
            }
            layoutParams = LinearLayout.LayoutParams((100 * density).toInt(), (34 * density).toInt())
            setOnClickListener {
                val coordsText = "${piloto.lat}, ${piloto.lon}"
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Coordenadas SOS", coordsText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(activity, "📍 Coordenadas copiadas: $coordsText", Toast.LENGTH_SHORT).show()
            }
        }
        gpsCard.addView(btnCopiarGps)
        contentContainer.addView(gpsCard)

        // TARJETA DE PROTOCOLO DE ACCIÓN
        val protocolCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#1B1A22"))
                setStroke((1 * density).toInt(), Color.parseColor("#673AB7"))
            }
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * density).toInt()
            }
        }

        val tvProtocolTitle = TextView(activity).apply {
            text = "🛡️ PROTOCOLO DE ACCIÓN INMEDIATO"
            setTextColor(Color.parseColor("#B388FF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        protocolCard.addView(tvProtocolTitle)

        val tvProtocolContent = TextView(activity).apply {
            text = config.protocoloAccion
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        protocolCard.addView(tvProtocolContent)

        val tvSpecialist = TextView(activity).apply {
            text = "👥 Apoyo sugerido: ${config.especialistaAsignado}"
            setTextColor(Color.parseColor("#CE93D8"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        protocolCard.addView(tvSpecialist)
        contentContainer.addView(protocolCard)

        scrollView.addView(contentContainer)
        rootLayout.addView(scrollView)

        // SECCIÓN INFERIOR: BOTONES DE ACCIÓN
        val buttonsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * density).toInt()
            }
        }

        // Fila de Comunicación Rápida: Llamar y WhatsApp SOS
        val commRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }

        val btnLlamar = Button(activity).apply {
            text = "📞 Llamar"
            setTextColor(Color.WHITE)
            textSize = 12f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#1565C0"))
            }
            layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply {
                marginEnd = (4 * density).toInt()
            }
            setOnClickListener {
                val num = telefonoPiloto.replace(Regex("[^0-9+]"), "")
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(activity, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
                }
            }
        }
        commRow.addView(btnLlamar)

        val btnWa = Button(activity).apply {
            text = "💬 WhatsApp SOS"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#2E7D32"))
            }
            layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply {
                marginStart = (4 * density).toInt()
            }
            setOnClickListener {
                val num = telefonoPiloto.replace(Regex("[^0-9]"), "")
                val mensaje = Uri.encode("🚨 Hola ${piloto.nombre}, vi tu alerta SOS vial [${config.titulo}] en el Mapa TX. Voy en camino a apoyarte / ¿cómo estás?")
                try {
                    val uri = Uri.parse("https://wa.me/$num?text=$mensaje")
                    activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {
                    Toast.makeText(activity, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                }
            }
        }
        commRow.addView(btnWa)
        buttonsContainer.addView(commRow)

        // Fila 2: Centrar en Mapa + Ver Carnet
        val navRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val btnCentrar = Button(activity).apply {
            text = "📍 Centrar Radar"
            setTextColor(Color.WHITE)
            textSize = 12f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#2C2C2C"))
                setStroke((1 * density).toInt(), Color.parseColor("#444444"))
            }
            layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply {
                marginEnd = (4 * density).toInt()
            }
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
        navRow.addView(btnCentrar)

        val btnVerCarnet = Button(activity).apply {
            text = "🏍️ Carnet Piloto"
            setTextColor(Color.BLACK)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#FF9800"))
            }
            layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply {
                marginStart = (4 * density).toInt()
            }
            setOnClickListener {
                dialog.dismiss()
                mostrarCarnetPiloto(activity, piloto)
            }
        }
        navRow.addView(btnVerCarnet)
        buttonsContainer.addView(navRow)

        // BOTÓN PRINCIPAL: MARCAR EMERGENCIA COMO RESUELTA
        val btnResolver = Button(activity).apply {
            text = "✅ MARCAR EMERGENCIA COMO RESUELTA"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#10B981")) // Verde Éxito
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (46 * density).toInt())
            setOnClickListener {
                resolverEmergenciaSos(
                    activity = activity,
                    pilotId = piloto.id,
                    pilotName = piloto.nombre,
                    alertaSosTexto = piloto.alertaSos ?: "",
                    lat = piloto.lat,
                    lon = piloto.lon,
                    onComplete = {
                        dialog.dismiss()
                    }
                )
            }
        }
        buttonsContainer.addView(btnResolver)
        rootLayout.addView(buttonsContainer)

        // Configuración final del Diálogo
        dialog.setContentView(rootLayout)
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (520 * density).toInt())
            w.setGravity(Gravity.BOTTOM)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()

        // Carga asíncrona de datos de contacto y moto desde la base de datos local
        scope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(activity, scope)
                val members = db.memberDao().getAllMembers().first()
                val member = members.firstOrNull { it.id.toString() == piloto.id || it.fullName.equals(piloto.nombre, ignoreCase = true) }

                withContext(Dispatchers.Main) {
                    if (member != null) {
                        tvPilotoNombre.text = "👤 Piloto: ${member.fullName} (${member.role.displayName})"
                        val motoInfo = "${member.bikeBrand} ${member.bikeModel} (${member.bikeColor})".trim()
                        tvMotoInfo.text = "🏍️ Moto: ${if (motoInfo.isNotBlank()) motoInfo else "Keeway TX 200"} • Placa: ${member.bikePlate.ifBlank { "Sin placa" }}"
                        telefonoPiloto = member.phone
                        tvContactoInfo.text = "📞 Teléfono: ${if (telefonoPiloto.isNotBlank()) telefonoPiloto else "No registrado"}"
                    } else {
                        tvMotoInfo.text = "🏍️ Moto: Keeway TX 200 (Oficial)"
                        tvContactoInfo.text = "📞 Teléfono: Disponible por radio / chat"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Muestra el detalle interactivo al tocar un marcador SOS en la capa de eventos del mapa.
     */
    @JvmStatic
    fun mostrarDetalleEmergenciaEvento(activity: Activity, evento: EventosMapLayer.EventoMarcador) {
        val app = activity.application as? OsmandApplication ?: return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = activity.resources.displayMetrics.density
        val config = RadarMapLayer.obtenerConfiguracionSos("${evento.titulo} ${evento.descripcion}")
        val colorAlertaInt = Color.parseColor(config.colorHex)

        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    20 * density, 20 * density,
                    20 * density, 20 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#121212"))
            }
            setPadding((18 * density).toInt(), (12 * density).toInt(), (18 * density).toInt(), (20 * density).toInt())
        }

        // Handle superior
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

        // Cabecera: Título SOS y botón cerrar
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val tvHeader = TextView(activity).apply {
            text = "🚨 PUNTO DE AUXILIO VIAL EN MAPA"
            setTextColor(colorAlertaInt)
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

        // Tarjeta Principal con Icono Alusivo
        val emergencyCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(Color.parseColor("#1C1C1C"))
                setStroke((2 * density).toInt(), colorAlertaInt)
            }
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val iconContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams((56 * density).toInt(), (56 * density).toInt()).apply {
                marginEnd = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#261414"))
                setStroke((2 * density).toInt(), colorAlertaInt)
            }
        }

        val ivSosIcon = ImageView(activity).apply {
            layoutParams = FrameLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt(), Gravity.CENTER)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val resId = activity.resources.getIdentifier(config.nombreDrawable, "drawable", activity.packageName)
            if (resId != 0) {
                setImageResource(resId)
            } else {
                val fallbackRes = if (config.nombreDrawable == "sos_choque" || config.nombreDrawable == "sos_caida" || config.nombreDrawable == "sos_medico") {
                    R.drawable.emergencia
                } else {
                    R.drawable.precaucion
                }
                setImageResource(fallbackRes)
            }
        }
        iconContainer.addView(ivSosIcon)
        emergencyCard.addView(iconContainer)

        val emergencyTextCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val badgeNivel = TextView(activity).apply {
            text = config.nivelTag
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), (2 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6 * density
                setColor(colorAlertaInt)
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        emergencyTextCol.addView(badgeNivel)

        val tvTitulo = TextView(activity).apply {
            text = evento.titulo
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        emergencyTextCol.addView(tvTitulo)

        val tvDrawableName = TextView(activity).apply {
            text = "🏷️ Icono: ${config.nombreDrawable}"
            setTextColor(Color.parseColor("#888888"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (2 * density).toInt()
            }
        }
        emergencyTextCol.addView(tvDrawableName)

        emergencyCard.addView(emergencyTextCol)
        rootLayout.addView(emergencyCard)

        // Tarjeta de Descripción
        val descCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#1A1A1A"))
                setStroke((1 * density).toInt(), Color.parseColor("#333333"))
            }
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val tvDesc = TextView(activity).apply {
            text = evento.descripcion.ifBlank { "Sin detalles adicionales" }
            setTextColor(Color.parseColor("#DDDDDD"))
            textSize = 12f
        }
        descCard.addView(tvDesc)
        rootLayout.addView(descCard)

        // Tarjeta GPS + Copiar
        val gpsCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#182026"))
                setStroke((1 * density).toInt(), Color.parseColor("#2680C2"))
            }
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val tvGps = TextView(activity).apply {
            text = "📍 GPS: %.5f, %.5f".format(evento.lat, evento.lon)
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        gpsCard.addView(tvGps)

        val btnCopiarGps = Button(activity).apply {
            text = "📋 Copiar"
            setTextColor(Color.BLACK)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6 * density
                setColor(Color.parseColor("#64B5F6"))
            }
            layoutParams = LinearLayout.LayoutParams((84 * density).toInt(), (34 * density).toInt())
            setOnClickListener {
                val coordsText = "${evento.lat}, ${evento.lon}"
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Coordenadas SOS", coordsText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(activity, "📍 Coordenadas copiadas: $coordsText", Toast.LENGTH_SHORT).show()
            }
        }
        gpsCard.addView(btnCopiarGps)
        rootLayout.addView(gpsCard)

        // Botón Navegar / Centrar
        val btnNavegar = Button(activity).apply {
            text = "🏍️ Centrar en este Sitio de Emergencia"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#2C2C2C"))
                setStroke((1 * density).toInt(), Color.parseColor("#555555"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (42 * density).toInt()).apply {
                bottomMargin = (8 * density).toInt()
            }
            setOnClickListener {
                dialog.dismiss()
                val mapView = app.osmandMap?.mapView
                mapView?.setLatLon(evento.lat, evento.lon)
                if ((mapView?.zoom ?: 0) < 16) {
                    mapView?.setIntZoom(16)
                }
                mapView?.refreshMap(true)
            }
        }
        rootLayout.addView(btnNavegar)

        // Extraer nombre del piloto del título si está disponible
        val nombreExtraido = evento.titulo.substringAfter(":", "").trim()

        // Botón Resolver Alerta
        val btnResolver = Button(activity).apply {
            text = "✅ MARCAR EMERGENCIA COMO RESUELTA"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#10B981"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (46 * density).toInt())
            setOnClickListener {
                resolverEmergenciaSos(
                    activity = activity,
                    pilotId = "",
                    pilotName = nombreExtraido,
                    alertaSosTexto = "${evento.titulo} ${evento.descripcion}",
                    lat = evento.lat,
                    lon = evento.lon,
                    onComplete = {
                        dialog.dismiss()
                    }
                )
            }
        }
        rootLayout.addView(btnResolver)

        dialog.setContentView(rootLayout)
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.BOTTOM)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    /**
     * Resuelve la emergencia en todos los módulos sincronizados:
     * 1. Limpia la telemetría GPS del piloto (TelemetriaGps.limpiarAlertaSos).
     * 2. Actualiza la alerta en la base de datos local a EmergencyStatus.RESUELTA.
     * 3. Desancla avisos SOS previos del Muro/Feed.
     * 4. Si es emergencia crítica (choque, caída, accidente), publica en el feed aviso de solventado.
     * 5. Envía mensaje de resolución a Chat General, Auxilio y Directiva.
     * 6. Re-sincroniza el mapa y retira el punto/icono inmediatamente.
     */
    @JvmStatic
    fun resolverEmergenciaSos(
        activity: Activity,
        pilotId: String,
        pilotName: String,
        alertaSosTexto: String,
        lat: Double,
        lon: Double,
        onComplete: () -> Unit
    ) {
        val app = activity.application as? OsmandApplication ?: return
        TelemetriaGps.limpiarAlertaSos(activity, pilotId)

        scope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(activity, scope)
                val allAlerts = db.emergencyDao().getAllAlerts().first()
                val alert = allAlerts.firstOrNull {
                    (pilotId.isNotBlank() && it.memberNumber == pilotId) ||
                    (pilotName.isNotBlank() && it.reporterName.contains(pilotName, ignoreCase = true)) &&
                    it.status != EmergencyStatus.RESUELTA
                }

                val config = RadarMapLayer.obtenerConfiguracionSos(alertaSosTexto)
                val tipoNombre = alert?.emergencyType?.label ?: config.titulo
                val gradoTag = alert?.emergencyType?.levelTag ?: config.nivelTag
                val nombreFinal = if (pilotName.isNotBlank()) pilotName else (alert?.reporterName ?: "Piloto TX")

                if (alert != null) {
                    val updated = alert.copy(
                        status = EmergencyStatus.RESUELTA,
                        respondersNotes = "Resuelta desde Mapa TX táctico por directiva / piloto"
                    )
                    db.emergencyDao().updateAlert(updated)
                }

                // 3. Desanclar avisos de emergencia del Muro si existieran
                try {
                    val pubs = db.publicationDao().getAllPublications().first()
                    pubs.filter { it.isPinned && it.title.contains("SOS") && (it.title.contains(nombreFinal) || it.content.contains(nombreFinal)) }
                        .forEach { p ->
                            db.publicationDao().updatePublication(p.copy(isPinned = false))
                        }
                } catch (_: Exception) {}

                // 4. Publicar en feed solo si es choque, caída, accidente grave (sePublicaEnMuro)
                val esGrave = alert?.emergencyType?.sePublicaEnMuro == true ||
                        config.nombreDrawable in listOf("sos_choque", "sos_caida", "sos_medico")
                if (esGrave) {
                    try {
                        val pubRes = Publication(
                            title = "✅ SOS VIAL SOLVENTADO: $nombreFinal a salvo",
                            content = "Se informa a la comunidad motera que la alerta vial [$gradoTag - $tipoNombre] en Lat: ${"%.4f".format(lat)}, Lon: ${"%.4f".format(lon)} ha sido totalmente SOLVENTADA.\n\n" +
                                    "👤 Piloto: $nombreFinal\n" +
                                    "🏍️ Bitácora: Situación atendida y fuera de peligro.\n" +
                                    "El punto de auxilio ha sido retirado del Mapa TX. ¡Gracias a todos por acudir!",
                            category = NoticeCategory.COMUNICADO,
                            priority = NoticePriority.NORMAL,
                            authorName = "Team TX Directiva",
                            authorRole = "Directiva Central",
                            isPinned = false
                        )
                        db.publicationDao().insertPublication(pubRes)
                    } catch (_: Exception) {}
                }

                // 5. Enviar mensajes de resolución a Chat General, Auxilio y Directiva
                val chatMsg = "✅ EMERGENCIA SOLVENTADA [$gradoTag - $tipoNombre]:\n" +
                        "La alerta vial de $nombreFinal ha sido marcada como RESUELTA.\n" +
                        "📍 Coordenadas: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}\n" +
                        "🏍️ Situación: Piloto seguro y asistido.\n" +
                        "🤝 El aviso ha sido retirado del Mapa TX."

                val now = System.currentTimeMillis()
                listOf("GENERAL", "MECANICA_AUXILIO", "DIRECTIVA").forEach { canal ->
                    try {
                        db.chatDao().insertMessage(
                            ChatMessage(
                                channelId = canal,
                                senderName = "Sistema SOS TX",
                                senderNickname = "Alerta Vial",
                                senderRole = MemberRole.DIRECTIVA,
                                senderInitials = "SOS",
                                messageText = chatMsg,
                                timestamp = now
                            )
                        )
                    } catch (_: Exception) {}
                }

                // 6. Re-sincronizar capa de mapa para retirar inmediatamente el marcador
                try {
                    val pubs = db.publicationDao().getAllPublications().first()
                    val events = db.calendarDao().getAllEvents().first()
                    val activeAlerts = db.emergencyDao().getAllAlerts().first().filter { it.status != EmergencyStatus.RESUELTA }
                    GestorRadar.sincronizarEventosEnMapa(pubs, events, activeAlerts)
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    app.osmandMap?.mapView?.refreshMap(true)
                    Toast.makeText(activity, "✅ Emergencia marcada como RESUELTA. Aviso retirado del mapa.", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Alerta actualizada: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
        }
    }

    /**
     * Muestra una lista de pilotos agrupados en la misma ubicación.
     */
    @JvmStatic
    fun mostrarListaPilotos(activity: Activity, grupo: List<PilotoRadar>) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = activity.resources.displayMetrics.density
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * density
                setColor(Color.parseColor("#161616"))
            }
            setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
        }

        val tvTitle = TextView(activity).apply {
            text = "👥 PILOTOS AGRUPADOS (${grupo.size})"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        root.addView(tvTitle)

        val scrollView = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (300 * density).toInt())
        }
        
        val listContainer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

        grupo.forEach { piloto ->
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((8 * density).toInt(), (10 * density).toInt(), (8 * density).toInt(), (10 * density).toInt())
                isClickable = true
                isFocusable = true
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10 * density
                }
                setOnClickListener {
                    dialog.dismiss()
                    mostrarPiloto(activity, piloto)
                }
            }

            val icon = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
                val bmp = RadarFirebase.obtenerAvatar(piloto.id, piloto.avatarUrl, activity)
                if (bmp != null) setImageBitmap(bmp) else {
                    val resId = activity.resources.getIdentifier("logoteam", "drawable", activity.packageName)
                    if (resId != 0) setImageResource(resId)
                }
            }
            row.addView(icon)

            val nameCol = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((12 * density).toInt(), 0, 0, 0)
            }
            
            val tvName = TextView(activity).apply {
                text = piloto.nombre
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            nameCol.addView(tvName)

            val tvRange = TextView(activity).apply {
                text = piloto.rango
                setTextColor(Color.parseColor("#FF9800"))
                textSize = 11f
            }
            nameCol.addView(tvRange)
            row.addView(nameCol)

            listContainer.addView(row)
            
            // Separador
            val sep = View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (1 * density).toInt())
                setBackgroundColor(Color.parseColor("#333333"))
            }
            listContainer.addView(sep)
        }

        scrollView.addView(listContainer)
        root.addView(scrollView)

        val btnClose = Button(activity).apply {
            text = "CERRAR"
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * density).toInt()
            }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()
    }

    /**
     * Muestra el detalle interactivo al tocar cualquier marcador de aviso o evento en el mapa.
     */
    @JvmStatic
    fun mostrarEvento(activity: Activity, evento: EventosMapLayer.EventoMarcador) {
        if (evento.titulo.startsWith("🚨 SOS")) {
            mostrarDetalleEmergenciaEvento(activity, evento)
            return
        }
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

    /**
     * Muestra el detalle táctico al tocar cualquier marcador de taller o repuestos en el mapa.
     */
    @JvmStatic
    fun mostrarDirectorio(activity: Activity, item: DirectorioMapLayer.DirectorioMarcador) {
        val app = activity.application as? OsmandApplication ?: return
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = activity.resources.displayMetrics.density
        val tieneCashea = item.tieneCashea || item.plataformasCredito.contains("Cashea", ignoreCase = true)
        val colorTemaHex = if (tieneCashea) "#00E676" else "#FF9800"
        val colorTemaInt = Color.parseColor(colorTemaHex)

        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    20 * density, 20 * density,
                    20 * density, 20 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#141822"))
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
                bottomMargin = (10 * density).toInt()
            }
        }

        val tvHeader = TextView(activity).apply {
            text = "🏪 COMERCIO RECOMENDADO TEAM TX"
            setTextColor(colorTemaInt)
            textSize = 12f
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

        // Fila de Título con Logo de Bandera o Team TX
        val titleRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val logoContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt()).apply {
                marginEnd = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1C2331"))
                setStroke((2 * density).toInt(), colorTemaInt)
            }
        }

        val logoImg = ImageView(activity).apply {
            layoutParams = FrameLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt(), Gravity.CENTER)
            val resId = activity.resources.getIdentifier("bandera", "drawable", activity.packageName)
            if (resId != 0) {
                setImageResource(resId)
            } else {
                val altRes = activity.resources.getIdentifier("logoteam", "drawable", activity.packageName)
                if (altRes != 0) setImageResource(altRes)
            }
        }
        logoContainer.addView(logoImg)
        titleRow.addView(logoContainer)

        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvNombre = TextView(activity).apply {
            text = item.nombre
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
        }
        titleCol.addView(tvNombre)

        val tvTipo = TextView(activity).apply {
            text = "🏷️ ${item.tipo}"
            setTextColor(Color.parseColor("#FFB74D"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (2 * density).toInt()
            }
        }
        titleCol.addView(tvTipo)
        titleRow.addView(titleCol)
        rootLayout.addView(titleRow)

        // Tarjeta de Dirección y Ubicación
        val infoCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#1A2130"))
                setStroke((1 * density).toInt(), Color.parseColor("#2C3B50"))
            }
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val tvDir = TextView(activity).apply {
            text = "📍 ${item.direccion}, ${item.ciudad}, Edo. ${item.estado}"
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 12f
        }
        infoCard.addView(tvDir)

        if (item.notas.isNotBlank()) {
            val tvNotas = TextView(activity).apply {
                text = "🔧 ${item.notas}"
                setTextColor(Color.parseColor("#90CAF9"))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (6 * density).toInt()
                }
            }
            infoCard.addView(tvNotas)
        }
        rootLayout.addView(infoCard)

        // Tarjeta de Calificación y Recomendación Táctica
        val esTop = item.rating >= 4.5
        val ratingCard = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(if (esTop) Color.parseColor("#261E08") else Color.parseColor("#1A2130"))
                setStroke((1 * density).toInt(), if (esTop) Color.parseColor("#FFD700") else Color.parseColor("#2C3B50"))
            }
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }

        val tvStar = TextView(activity).apply {
            text = if (esTop) "🏆" else "⭐"
            textSize = 18f
            setPadding(0, 0, (8 * density).toInt(), 0)
        }
        ratingCard.addView(tvStar)

        val ratingTextCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvRatingScore = TextView(activity).apply {
            text = "Calificación: ${String.format(java.util.Locale.US, "%.1f", item.rating)} / 5.0 ⭐"
            setTextColor(if (esTop) Color.parseColor("#FFD700") else Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        ratingTextCol.addView(tvRatingScore)

        val tvRatingLabel = TextView(activity).apply {
            text = if (esTop) "🌟 Comercio Top Recomendado por el Team TX" else "Verificado por la Comunidad Motera"
            setTextColor(if (esTop) Color.parseColor("#FFE082") else Color.parseColor("#90A4AE"))
            textSize = 10f
        }
        ratingTextCol.addView(tvRatingLabel)
        ratingCard.addView(ratingTextCol)
        rootLayout.addView(ratingCard)

        // SECCIÓN DESTACADA: CASHEA / FINANCIAMIENTO
        if (tieneCashea || item.plataformasCredito.isNotBlank()) {
            val casheaCard = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10 * density
                    setColor(Color.parseColor("#0F2B1D"))
                    setStroke((1 * density).toInt(), Color.parseColor("#00E676"))
                }
                setPadding((10 * density).toInt(), (8 * density).toInt(), (10 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (12 * density).toInt()
                }
            }

            val casheaImg = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                    marginEnd = (10 * density).toInt()
                }
                val resCashea = activity.resources.getIdentifier("logocashea", "drawable", activity.packageName)
                if (resCashea != 0) setImageResource(resCashea)
            }
            casheaCard.addView(casheaImg)

            val casheaTextCol = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvCasheaTitle = TextView(activity).apply {
                text = "💳 ACEPTA FINANCIAMIENTO"
                setTextColor(Color.parseColor("#69F0AE"))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            casheaTextCol.addView(tvCasheaTitle)

            val tvCasheaPlat = TextView(activity).apply {
                text = item.plataformasCredito.ifBlank { "Cashea / Rapikom / Convenio" }
                setTextColor(Color.parseColor("#B9F6CA"))
                textSize = 11f
            }
            casheaTextCol.addView(tvCasheaPlat)
            casheaCard.addView(casheaTextCol)
            rootLayout.addView(casheaCard)
        }

        // Fila de Botones: Llamar y WhatsApp
        val commRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }

        if (item.telefono.isNotBlank()) {
            val btnLlamar = Button(activity).apply {
                text = "📞 Llamar"
                setTextColor(Color.WHITE)
                textSize = 12f
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#1565C0"))
                }
                layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply {
                    marginEnd = if (item.whatsapp.isNotBlank()) (6 * density).toInt() else 0
                }
                setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:${item.telefono.replace(Regex("[^0-9+]"), "")}"))
                        activity.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(activity, "No se pudo abrir el marcador telefónico", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            commRow.addView(btnLlamar)
        }

        if (item.whatsapp.isNotBlank()) {
            val btnWa = Button(activity).apply {
                text = "💬 WhatsApp"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#2E7D32"))
                }
                layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply {
                    marginStart = if (item.telefono.isNotBlank()) (6 * density).toInt() else 0
                }
                setOnClickListener {
                    try {
                        val num = item.whatsapp.replace(Regex("[^0-9]"), "")
                        val uri = android.net.Uri.parse("https://wa.me/$num")
                        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        Toast.makeText(activity, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            commRow.addView(btnWa)
        }
        if (item.telefono.isNotBlank() || item.whatsapp.isNotBlank()) {
            rootLayout.addView(commRow)
        }

        // Botón: Ir a Guía de Servicios y Repuestos
        val btnDirectorio = Button(activity).apply {
            text = "🏍️ Abrir en Guía de Servicios y Repuestos"
            setTextColor(Color.BLACK)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#FF9800"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (42 * density).toInt()).apply {
                bottomMargin = (6 * density).toInt()
            }
            setOnClickListener {
                dialog.dismiss()
                val prefs = activity.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
                prefs.edit().putString("target_workshop_name", item.nombre).apply()
                activity.finish()
            }
        }
        rootLayout.addView(btnDirectorio)

        dialog.setContentView(rootLayout)
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.BOTTOM)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    /**
     * Muestra el diálogo para emitir una alerta SOS Vial directamente desde el Mapa Táctico de OsmAnd.
     */
    @JvmStatic
    fun mostrarDialogoEmitirSos(activity: Activity) {
        val density = activity.resources.displayMetrics.density
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * density
                setColor(Color.parseColor("#1E1E24"))
                setStroke((1 * density).toInt(), Color.parseColor("#E53935"))
            }
        }

        // Header: Titulo
        val headerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val tvTitulo = TextView(activity).apply {
            text = "🚨 EMITIR ALERTA SOS VIAL"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerLayout.addView(tvTitulo)

        val btnCerrar = TextView(activity).apply {
            text = "✕"
            setTextColor(Color.parseColor("#9E9E9E"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            setOnClickListener { dialog.dismiss() }
        }
        headerLayout.addView(btnCerrar)
        rootLayout.addView(headerLayout)

        val tvSubtitulo = TextView(activity).apply {
            text = "Selecciona el tipo de auxilio requerida en la ruta:"
            setTextColor(Color.parseColor("#B0BEC5"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * density).toInt()
            }
        }
        rootLayout.addView(tvSubtitulo)

        // Opciones de tipo de emergencia
        val tipos = listOf(
            Triple(EmergencyType.ACCIDENTADO_GASOLINA, "⛽ Sin Gasolina", "#FF9800"),
            Triple(EmergencyType.ACCIDENTADO_MECANICO, "🔧 Falla Mecánica", "#2196F3"),
            Triple(EmergencyType.CAIDA, "🚑 Caída en Ruta", "#E53935"),
            Triple(EmergencyType.CHOQUE, "🚗 Choque Grave", "#B71C1C"),
            Triple(EmergencyType.EMERGENCIA_MEDICA, "🏥 Auxilio Médico", "#D32F2F"),
            Triple(EmergencyType.APOYO_SEGURIDAD, "🛡️ Peligro en Vía", "#7B1FA2"),
            Triple(EmergencyType.ALCABALA_RETEN, "👮 Alcabala / Retén", "#00897B")
        )

        var tipoSeleccionado = EmergencyType.ACCIDENTADO_GASOLINA
        val containerOpciones = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * density).toInt()
            }
        }

        val optionViews = mutableListOf<View>()
        tipos.forEach { (type, label, hexColor) ->
            val itemLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (6 * density).toInt()
                }
            }

            itemLayout.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor(if (type == tipoSeleccionado) "#2C2C38" else "#121216"))
                if (type == tipoSeleccionado) setStroke((1.5 * density).toInt(), Color.parseColor(hexColor))
            }

            val tvLabel = TextView(activity).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = if (type == tipoSeleccionado) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            itemLayout.addView(tvLabel)

            itemLayout.setOnClickListener {
                tipoSeleccionado = type
                optionViews.forEachIndexed { idx, v ->
                    val (t, _, colorHex) = tipos[idx]
                    val isSel = t == tipoSeleccionado
                    v.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 8 * density
                        setColor(Color.parseColor(if (isSel) "#2C2C38" else "#121216"))
                        if (isSel) setStroke((1.5 * density).toInt(), Color.parseColor(colorHex))
                    }
                }
            }

            optionViews.add(itemLayout)
            containerOpciones.addView(itemLayout)
        }
        rootLayout.addView(containerOpciones)

        // Campo de texto para ubicación o detalles
        val etUbicacion = EditText(activity).apply {
            hint = "Escribe referencia vial o punto de apoyo..."
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#121216"))
                setStroke((1 * density).toInt(), Color.parseColor("#37474F"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * density).toInt()
            }
        }
        rootLayout.addView(etUbicacion)

        // Botón Emitir Alerta
        val btnEmitir = Button(activity).apply {
            text = "🚨 EMITIR ALERTA SOS AHORA"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10 * density
                setColor(Color.parseColor("#E53935"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (46 * density).toInt())
            setOnClickListener {
                val refText = etUbicacion.text.toString().trim().ifBlank { "Ubicación reportada por GPS en ruta" }
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonimo"
                val alertaSosTexto = "🚨 ${tipoSeleccionado.label}: $refText"

                // 1. Guardar en SharedPreferences y Firestore Telemetria
                val prefs = activity.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
                val nombre = prefs.getString("radar_nombre", "Piloto TX") ?: "Piloto TX"
                prefs.edit().putString("radar_alerta_sos", alertaSosTexto).apply()

                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("radar_en_vivo")
                        .document(uid)
                        .update("alertaSos", alertaSosTexto)
                } catch (_: Exception) {}

                // 2. Insertar en Room Local DB
                scope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(activity, scope)
                        val newAlert = EmergencyAlert(
                            id = System.currentTimeMillis(),
                            reporterName = nombre,
                            reporterPhone = "+58 412 000 0000",
                            memberNumber = "TX-MAP",
                            emergencyType = tipoSeleccionado,
                            locationDescription = refText,
                            coordinateLat = 0.0,
                            coordinateLng = 0.0,
                            bikeDetails = "Unidad TX",
                            details = refText,
                            status = EmergencyStatus.ACTIVA,
                            respondersNotes = "Alerta emitida directamente desde el Mapa TX"
                        )
                        db.emergencyDao().insertAlert(newAlert)
                    } catch (_: Exception) {}
                }

                // 3. Activar Mesh Radio SOS si aplica
                try {
                    if (tipoSeleccionado == EmergencyType.ALCABALA_RETEN) {
                        com.example.meshtx.GestorMeshTx.activarModoAlcabalaSos("Ubicación: $refText")
                    } else {
                        com.example.meshtx.GestorMeshTx.emitirAlertaSos("🚨 SOS ${tipoSeleccionado.name}: $refText", null)
                    }
                } catch (_: Exception) {}

                Toast.makeText(activity, "🚨 Alerta SOS Emitida. Visible inmediatamente en el mapa.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }
        rootLayout.addView(btnEmitir)

        dialog.setContentView(rootLayout)
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.CENTER)
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
