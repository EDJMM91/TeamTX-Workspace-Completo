package com.example.radar

import android.content.Context
import android.graphics.*
import android.graphics.BitmapFactory
import android.util.Log
import com.aistudio.teamtxvzla.R
import net.osmand.data.LatLon
import android.graphics.PointF
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.MapSelectionResult
import net.osmand.plus.views.layers.MapSelectionRules
import net.osmand.plus.views.layers.base.OsmandMapLayer

class RadarMapLayer(context: Context) : OsmandMapLayer(context),
    IContextMenuProvider {

    companion object {
        private const val ETIQUETA = "RADAR_MAP_LAYER"
        private const val ZOOM_MINIMO = 3
        private const val RADIO_ICONO_PX = 40
        private const val TOUCH_RADIUS_MULTIPLIER = 1.5f
        private const val OFFSET_LOCAL_X_DP = 80
        private const val OFFSET_LOCAL_Y_DP = -50

        data class SosVisualConfig(
            val nombreDrawable: String,
            val colorHex: String,
            val titulo: String,
            val nivelTag: String,
            val protocoloAccion: String = "",
            val especialistaAsignado: String = ""
        )

        /**
         * Mapea el tipo de emergencia a nombres cortos en español para drawables personalizados:
         * - sos_gasolina: Falta de combustible (Nivel 1)
         * - sos_mecanico: Falla mecanica o averia (Nivel 2)
         * - sos_caida: Caida en ruta / deslizamiento (Nivel 3)
         * - sos_choque: Choque / colision vial grave (Nivel 4)
         * - sos_medico: Emergencia medica / triage (Nivel 4)
         * - sos_alcabala: Reten policial / alcabala audio vivo (Nivel 3)
         * - sos_seguridad: Apoyo de seguridad vial (Nivel 3)
         * - sos_alerta: Fallback generico
         */
        fun obtenerConfiguracionSos(alertaSos: String?): SosVisualConfig {
            val t = alertaSos?.uppercase()?.trim() ?: ""
            return when {
                t.contains("GASOLINA") -> SosVisualConfig(
                    nombreDrawable = "sos_gasolina",
                    colorHex = "#D97706",
                    titulo = "Falta de Gasolina / Combustible",
                    nivelTag = "NIVEL 1 - ASISTENCIA RÁPIDA",
                    protocoloAccion = "Llevar pimpina de gasolina 91/95 octanos o manguera de trasvase. Asistir en ruta.",
                    especialistaAsignado = "Pilotos cercanos en ruta / Grupo de Apoyo"
                )
                t.contains("MECANIC") || t.contains("FALLA") || t.contains("AVERIA") -> SosVisualConfig(
                    nombreDrawable = "sos_mecanico",
                    colorHex = "#EA580C",
                    titulo = "Falla Mecánica / Avería en Ruta",
                    nivelTag = "NIVEL 2 - URGENCIA TÁCTICA",
                    protocoloAccion = "Llevar kit de herramientas, guayas, fusibles, bujías o tripas. Remolque si no enciende.",
                    especialistaAsignado = "Mecánicos Oficiales Team TX / Capitán de Ruta"
                )
                t.contains("CAIDA") || t.contains("DESLIZ") -> SosVisualConfig(
                    nombreDrawable = "sos_caida",
                    colorHex = "#E11D48",
                    titulo = "Caída en Ruta / Deslizamiento",
                    nivelTag = "NIVEL 3 - PRIORIDAD ALTA",
                    protocoloAccion = "Verificar integridad física del piloto, asegurar perímetro vial contra tráfico y levantar moto con precaución.",
                    especialistaAsignado = "Comité de Seguridad / Primeros Auxilios / Capitán"
                )
                t.contains("CHOQUE") || t.contains("COLISION") || t.contains("ACCIDENTE") -> SosVisualConfig(
                    nombreDrawable = "sos_choque",
                    colorHex = "#DC2626",
                    titulo = "Choque / Colisión Grave",
                    nivelTag = "NIVEL 4 - CRÍTICO VIAL",
                    protocoloAccion = "NO mover al piloto lesionado sin personal médico. Contactar 911 / Paramédicos y acordonar la zona vial.",
                    especialistaAsignado = "Paramédicos / 911 / Directiva Central"
                )
                t.contains("MEDIC") || t.contains("SALUD") || t.contains("TRIAGE") -> SosVisualConfig(
                    nombreDrawable = "sos_medico",
                    colorHex = "#7C3AED",
                    titulo = "Emergencia Médica / Triage",
                    nivelTag = "NIVEL 4 - CRÍTICO MÉDICO",
                    protocoloAccion = "Atención médica prioritaria. Suministrar botiquín de primeros auxilios y activar traslado urgente.",
                    especialistaAsignado = "Comité Médico / Paramédicos Motorizados"
                )
                t.contains("ALCABALA") || t.contains("RETEN") || t.contains("POLICIA") -> SosVisualConfig(
                    nombreDrawable = "sos_alcabala",
                    colorHex = "#6D28D9",
                    titulo = "Retén Policial / Alcabala",
                    nivelTag = "NIVEL 3 - MONITOREO DIRECTO",
                    protocoloAccion = "Transmisión de audio en vivo activa. Mantener la calma, portar documentos vigentes y no confrontar.",
                    especialistaAsignado = "Consultoría Jurídica / Directiva TX"
                )
                t.contains("SEGURIDAD") || t.contains("VIA") || t.contains("OBSTACULO") -> SosVisualConfig(
                    nombreDrawable = "sos_seguridad",
                    colorHex = "#0284C7",
                    titulo = "Situación Vial / Seguridad",
                    nivelTag = "NIVEL 3 - ALERTA VIAL",
                    protocoloAccion = "Reducir velocidad, señalizar peligro a la caravana y advertir a pilotos en retaguardia.",
                    especialistaAsignado = "Líderes de Escuadrón / Guardia Biker"
                )
                else -> SosVisualConfig(
                    nombreDrawable = "sos_alerta",
                    colorHex = "#DC2626",
                    titulo = "Alerta de Auxilio SOS",
                    nivelTag = "ALERTA SOS",
                    protocoloAccion = "Piloto del Team TX solicita asistencia inmediata en coordenadas registradas.",
                    especialistaAsignado = "Hermandad Team TX / Directiva"
                )
            }
        }
    }

    var radarHabilitado: Boolean = false

    private var pilotos: List<PilotoRadar> = emptyList()
    private var pilotoSeleccionado: ((PilotoRadar) -> Unit)? = null
    private var miUserId: String = ""

    private val paintBitmap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val paintAvatarBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
        isFakeBoldText = true
    }
    private val paintFondoTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val paintCirculo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintLinea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
        pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }

    private val cacheIconosSos = mutableMapOf<String, Bitmap>()
    private val rectIconoEmergencia = RectF()
    private val paintPuntoAccidente = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val avataresCache = mutableMapOf<String, Bitmap>()
    private val limitarRect = RectF()

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        Log.d(ETIQUETA, "RadarMapLayer inicializado")
    }

    override fun destroyLayer() {
        super.destroyLayer()
        avataresCache.clear()
        pilotos = emptyList()
        cacheIconosSos.values.forEach { if (!it.isRecycled) it.recycle() }
        cacheIconosSos.clear()
    }

    override fun drawInScreenPixels(): Boolean = false

    fun setMiUserId(id: String) {
        miUserId = id
    }

    fun setOnPilotoSeleccionado(callback: (PilotoRadar) -> Unit) {
        pilotoSeleccionado = callback
    }

    fun actualizarPilotos(nuevosPilotos: List<PilotoRadar>) {
        if (!radarHabilitado) {
            pilotos = emptyList()
            getTileView()?.refreshMap()
            return
        }
        pilotos = nuevosPilotos
        getTileView()?.refreshMap()
    }

    fun limpiarPilotos() {
        pilotos = emptyList()
        getTileView()?.refreshMap()
    }

    fun cachearAvatar(url: String, bitmap: Bitmap) {
        avataresCache[url] = bitmap
    }

    fun obtenerPilotosEnPosicion(lat: Double, lon: Double, zoom: Int): List<PilotoRadar> {
        val radioGrados = 0.0001 * (20 - zoom).coerceAtLeast(1)
        return pilotos.filter { p ->
            val dist = Math.sqrt(Math.pow(p.lat - lat, 2.0) + Math.pow(p.lon - lon, 2.0))
            dist < radioGrados
        }
    }

    private fun obtenerBitmapSos(nombreDrawable: String): Bitmap? {
        cacheIconosSos[nombreDrawable]?.let { if (!it.isRecycled) return it }
        return try {
            val resId = context.resources.getIdentifier(nombreDrawable, "drawable", context.packageName)
            if (resId != 0) {
                val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val density = context.resources.displayMetrics.density
                    val sizePx = (48 * density).toInt()
                    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, sizePx, sizePx)
                    drawable.draw(canvas)
                    cacheIconosSos[nombreDrawable] = bmp
                    bmp
                } else null
            } else {
                // Fallbacks seguros si no existen los drawables
                val fallbackRes = if (nombreDrawable == "sos_choque" || nombreDrawable == "sos_caida" || nombreDrawable == "sos_medico") {
                    R.drawable.emergencia
                } else {
                    R.drawable.precaucion
                }
                val bmp = BitmapFactory.decodeResource(context.resources, fallbackRes)
                if (bmp != null) cacheIconosSos[nombreDrawable] = bmp
                bmp
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error cargando icono SOS $nombreDrawable: ${e.message}")
            null
        }
    }

    /**
     * Dibuja el icono de emergencia anclado y tocando directamente el disco del usuario (arriba a la derecha).
     */
    private fun dibujarIconoEmergencia(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radio: Float,
        density: Float,
        piloto: PilotoRadar
    ) {
        val config = obtenerConfiguracionSos(piloto.alertaSos)
        val bitmapIcono = obtenerBitmapSos(config.nombreDrawable)

        // Centro y radio del icono de emergencia tocando el perímetro del disco del piloto
        val badgeRadio = radio * 0.54f
        val badgeCx = cx + (radio * 0.72f)
        val badgeCy = cy - (radio * 0.72f)

        // 1. Halo exterior de advertencia pulsante
        val paintHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            color = Color.parseColor(config.colorHex)
            alpha = 190
        }
        canvas.drawCircle(badgeCx, badgeCy, badgeRadio + (3f * density), paintHalo)

        // 2. Fondo circular del badge con color del nivel de emergencia
        val paintFondoBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor(config.colorHex)
        }
        canvas.drawCircle(badgeCx, badgeCy, badgeRadio, paintFondoBadge)

        // 3. Borde blanco de alto contraste que toca el disco del avatar
        val paintBordeBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            color = Color.WHITE
        }
        canvas.drawCircle(badgeCx, badgeCy, badgeRadio, paintBordeBadge)

        // 4. Dibujar icono alusivo centrado dentro del badge
        if (bitmapIcono != null) {
            val tamIcono = badgeRadio * 1.55f
            rectIconoEmergencia.set(
                badgeCx - tamIcono / 2f,
                badgeCy - tamIcono / 2f,
                badgeCx + tamIcono / 2f,
                badgeCy + tamIcono / 2f
            )
            canvas.drawBitmap(bitmapIcono, null, rectIconoEmergencia, paintBitmap)
        }
    }

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings?
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        if (!radarHabilitado || tileBox.zoom < ZOOM_MINIMO || pilotos.isEmpty()) return

        val density = tileBox.density
        val radio = (RADIO_ICONO_PX * density).toInt()

        canvas.save()
        canvas.rotate(
            -tileBox.rotate,
            tileBox.centerPixelX.toFloat(),
            tileBox.centerPixelY.toFloat()
        )

        // Agrupar pilotos por proximidad visual para manejar solapamiento (~35 px)
        val pilotosAgrupados = mutableMapOf<String, MutableList<PilotoRadar>>()
        for (p in pilotos) {
            val pixX = tileBox.getPixXFromLatLon(p.lat, p.lon)
            val pixY = tileBox.getPixYFromLatLon(p.lat, p.lon)
            if (pixX < 0 || pixX > tileBox.pixWidth || pixY < 0 || pixY > tileBox.pixHeight) continue
            
            val key = "${(pixX / 35).toInt()},${(pixY / 35).toInt()}"
            pilotosAgrupados.getOrPut(key) { mutableListOf() }.add(p)
        }

        for (grupo in pilotosAgrupados.values) {
            val count = grupo.size
            val centroX = tileBox.getPixXFromLatLon(grupo.first().lat, grupo.first().lon)
            val centroY = tileBox.getPixYFromLatLon(grupo.first().lat, grupo.first().lon)

            if (count == 1) {
                val piloto = grupo.first()
                val esLocal = piloto.id == miUserId && miUserId.isNotBlank()
                val esSos = !piloto.alertaSos.isNullOrBlank()

                val avatar = if (esLocal) {
                    RadarFirebase.obtenerAvatarLocal(context) 
                        ?: (if (piloto.avatarUrl.isNotBlank()) avataresCache[piloto.avatarUrl] ?: RadarFirebase.obtenerAvatarCacheado(piloto.avatarUrl) else null)
                } else {
                    if (piloto.avatarUrl.isNotBlank()) avataresCache[piloto.avatarUrl] ?: RadarFirebase.obtenerAvatarCacheado(piloto.avatarUrl) else null
                }

                val hasOffset = esLocal || esSos
                val avatarCx = if (hasOffset) centroX + (OFFSET_LOCAL_X_DP * density) else centroX
                val avatarCy = if (hasOffset) centroY + (OFFSET_LOCAL_Y_DP * density) else centroY

                if (hasOffset) {
                    val colorLinea = if (esSos) Color.parseColor("#FF1744") else colorPorRango(piloto.rango)
                    paintLinea.color = colorLinea
                    paintLinea.strokeWidth = if (esSos) 3.5f * density else 2.5f * density
                    canvas.drawLine(centroX, centroY, avatarCx, avatarCy, paintLinea)
                }

                if (avatar != null) {
                    dibujarAvatar(canvas, avatarCx, avatarCy, avatar, radio.toFloat(), piloto)
                } else {
                    dibujarPlaceholder(canvas, avatarCx, avatarCy, radio.toFloat(), piloto)
                }

                val labelTexto = if (esLocal) "Tú" else piloto.nombre
                dibujarLabel(canvas, avatarCx, avatarCy + radio + 16 * density, labelTexto, esSos = esSos)

                if (esSos) {
                    dibujarIconoEmergencia(canvas, avatarCx, avatarCy, radio.toFloat(), density, piloto)
                }
            } else {
                // 🌀 ALGORITMO ORBITAL TÁCTICO (SPIDERIFIER): Dispersar los discos en un círculo ordenado para no solaparse
                val radioOrbita = (36f * density) + (count * 4f * density)

                // Dibujar punto central GPS del grupo
                paintPuntoAccidente.color = Color.parseColor("#FF9800")
                canvas.drawCircle(centroX, centroY, 6f * density, paintPuntoAccidente)

                for (i in 0 until count) {
                    val piloto = grupo[i]
                    val angulo = (2.0 * Math.PI * i) / count
                    val avatarCx = (centroX + radioOrbita * Math.cos(angulo)).toFloat()
                    val avatarCy = (centroY + radioOrbita * Math.sin(angulo)).toFloat()

                    val esLocal = piloto.id == miUserId && miUserId.isNotBlank()
                    val esSos = !piloto.alertaSos.isNullOrBlank()

                    val avatar = if (esLocal) {
                        RadarFirebase.obtenerAvatarLocal(context)
                            ?: (if (piloto.avatarUrl.isNotBlank()) avataresCache[piloto.avatarUrl] ?: RadarFirebase.obtenerAvatarCacheado(piloto.avatarUrl) else null)
                    } else {
                        if (piloto.avatarUrl.isNotBlank()) avataresCache[piloto.avatarUrl] ?: RadarFirebase.obtenerAvatarCacheado(piloto.avatarUrl) else null
                    }

                    // Línea táctica desde el centro GPS hasta la posición orbital del piloto
                    paintLinea.color = if (esSos) Color.parseColor("#FF1744") else colorPorRango(piloto.rango)
                    paintLinea.strokeWidth = 2.5f * density
                    canvas.drawLine(centroX, centroY, avatarCx, avatarCy, paintLinea)

                    if (avatar != null) {
                        dibujarAvatar(canvas, avatarCx, avatarCy, avatar, radio.toFloat(), piloto)
                    } else {
                        dibujarPlaceholder(canvas, avatarCx, avatarCy, radio.toFloat(), piloto)
                    }

                    val labelTexto = if (esLocal) "Tú (${piloto.nombre})" else piloto.nombre
                    dibujarLabel(canvas, avatarCx, avatarCy + radio + 14 * density, labelTexto, esSos = esSos)

                    if (esSos) {
                        dibujarIconoEmergencia(canvas, avatarCx, avatarCy, radio.toFloat(), density, piloto)
                    }
                }
            }
        }

        canvas.restore()
    }

    private fun dibujarAvatar(
        canvas: Canvas, cx: Float, cy: Float,
        avatar: Bitmap, radio: Float, piloto: PilotoRadar
    ) {
        limitarRect.set(cx - radio, cy - radio, cx + radio, cy + radio)

        canvas.save()
        val path = Path().apply {
            addCircle(cx, cy, radio, Path.Direction.CW)
        }
        canvas.clipPath(path)
        canvas.drawBitmap(avatar, null, limitarRect, paintBitmap)
        canvas.restore()

        val esSos = !piloto.alertaSos.isNullOrBlank()
        paintAvatarBorde.color = if (esSos) Color.parseColor("#FF1744") else colorPorRango(piloto.rango)
        paintAvatarBorde.strokeWidth = (if (esSos) 5f else 3f) * getContext()!!.resources.displayMetrics.density
        canvas.drawCircle(cx, cy, radio, paintAvatarBorde)
    }

    private fun dibujarPlaceholder(
        canvas: Canvas, cx: Float, cy: Float,
        radio: Float, piloto: PilotoRadar
    ) {
        val esSos = !piloto.alertaSos.isNullOrBlank()
        paintCirculo.color = if (esSos) Color.parseColor("#FF1744") else colorPorRango(piloto.rango)
        canvas.drawCircle(cx, cy, radio, paintCirculo)

        val inicial = piloto.nombre.take(1).uppercase()
        val textoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = radio * 1.2f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yPos = cy - (textoPaint.descent() + textoPaint.ascent()) / 2
        canvas.drawText(inicial, cx, yPos, textoPaint)
    }

    private fun dibujarLabel(canvas: Canvas, cx: Float, cy: Float, texto: String, esSos: Boolean = false) {
        val textoCorto = if (texto.length > 16) texto.take(16) + "." else texto
        val anchoTexto = paintTexto.measureText(textoCorto)
        val altoTexto = (paintTexto.descent() - paintTexto.ascent()).toInt()
        val density = getContext()!!.resources.displayMetrics.density
        val padding = 6f * density
        val radioFondo = 8f * density

        val left = cx - anchoTexto / 2 - padding
        val top = cy - altoTexto / 2 - padding
        val right = cx + anchoTexto / 2 + padding
        val bottom = cy + altoTexto / 2 + padding
        paintFondoTexto.color = if (esSos) Color.parseColor("#D50000") else Color.parseColor("#CC111827")
        canvas.drawRoundRect(left, top, right, bottom, radioFondo, radioFondo, paintFondoTexto)
        canvas.drawText(textoCorto, cx, cy + altoTexto / 4, paintTexto)
    }

    private fun colorPorRango(rango: String): Int {
        return when {
            rango.contains("Capitán", true) || rango.contains("Capitan", true) -> Color.parseColor("#E53935")
            rango.contains("Mecánico", true) || rango.contains("Mecanico", true) -> Color.parseColor("#1E88E5")
            rango.contains("Directiva", true) || rango.contains("Presidente", true) -> Color.parseColor("#FDD835")
            rango.contains("Vocal", true) -> Color.parseColor("#43A047")
            else -> Color.parseColor("#FF9800")
        }
    }

    override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings?) {
    }

    override fun collectObjectsFromPoint(
        result: MapSelectionResult,
        rules: MapSelectionRules
    ) {
        if (!radarHabilitado || pilotos.isEmpty()) return
        val app = application ?: return
        val point = result.point
        val tileBox = result.tileBox
        val density = tileBox.density
        val radius = (getScaledTouchRadius(app, tileBox.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER * 2.2f).toFloat()

        // Agrupar para detectar toques en grupos
        val pilotosAgrupados = mutableMapOf<String, MutableList<PilotoRadar>>()
        for (p in pilotos) {
            val pixX = tileBox.getPixXFromLatLon(p.lat, p.lon)
            val pixY = tileBox.getPixYFromLatLon(p.lat, p.lon)
            val key = "${(pixX / 40).toInt()},${(pixY / 40).toInt()}"
            pilotosAgrupados.getOrPut(key) { mutableListOf() }.add(p)
        }

        for (grupo in pilotosAgrupados.values) {
            val count = grupo.size
            val centroX = tileBox.getPixXFromLatLon(grupo.first().lat, grupo.first().lon)
            val centroY = tileBox.getPixYFromLatLon(grupo.first().lat, grupo.first().lon)

            if (count == 1) {
                val p = grupo.first()
                val esLocal = p.id == miUserId
                val hasOffset = esLocal || !p.alertaSos.isNullOrBlank()

                val avatarCx = if (hasOffset) centroX + OFFSET_LOCAL_X_DP * density else centroX
                val avatarCy = if (hasOffset) centroY + OFFSET_LOCAL_Y_DP * density else centroY

                val dx = point.x - avatarCx
                val dy = point.y - avatarCy
                val touchDistSq = dx * dx + dy * dy

                val isNearAvatar = touchDistSq <= (radius * radius * 1.8f)
                val isNearBase = tileBox.isLatLonNearPixel(p.lat, p.lon, point.x, point.y, radius)

                if (isNearAvatar || isNearBase) {
                    result.collect(p, this)
                }
            } else {
                // Detección de toque en posiciones orbitales individuales de cada piloto
                val radioOrbita = (36f * density) + (count * 4f * density)
                var pilotoTocado: PilotoRadar? = null

                for (i in 0 until count) {
                    val p = grupo[i]
                    val angulo = (2.0 * Math.PI * i) / count
                    val avatarCx = (centroX + radioOrbita * Math.cos(angulo)).toFloat()
                    val avatarCy = (centroY + radioOrbita * Math.sin(angulo)).toFloat()

                    val dx = point.x - avatarCx
                    val dy = point.y - avatarCy
                    if ((dx * dx + dy * dy) <= (radius * radius * 1.8f)) {
                        pilotoTocado = p
                        break
                    }
                }

                // Detección de toque en el punto central del grupo GPS
                val dxCentro = point.x - centroX
                val dyCentro = point.y - centroY
                val isNearCenter = (dxCentro * dxCentro + dyCentro * dyCentro) <= (radius * radius * 1.8f)

                if (pilotoTocado != null) {
                    result.collect(pilotoTocado, this)
                } else if (isNearCenter) {
                    result.collect(grupo, this) // Enviar lista completa del grupo
                }
            }
        }
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        val act: android.app.Activity = (mapActivity as? android.app.Activity)
            ?: (GestorRadar.obtenerMapActivity() as? android.app.Activity)
            ?: (application?.osmandMap?.mapView?.context as? android.app.Activity)
            ?: return false

        if (o is List<*>) {
            val grupo = o.filterIsInstance<PilotoRadar>()
            if (grupo.isNotEmpty()) {
                act.runOnUiThread {
                    DialogosMapaTx.mostrarListaPilotos(act, grupo)
                }
                return true
            }
        } else if (o is PilotoRadar) {
            act.runOnUiThread {
                DialogosMapaTx.mostrarPiloto(act, o)
            }
            return true
        }
        return false
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        if (o is PilotoRadar) {
            return LatLon(o.lat, o.lon)
        }
        return null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        if (o is PilotoRadar) {
            return PointDescription(PointDescription.POINT_TYPE_MARKER, o.nombre)
        }
        return null
    }
}
