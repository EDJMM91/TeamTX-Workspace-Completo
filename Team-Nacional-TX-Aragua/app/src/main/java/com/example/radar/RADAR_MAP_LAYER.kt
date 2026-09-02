package com.example.radar

import android.content.Context
import android.graphics.*
import android.util.Log
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
    }

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
    }

    override fun drawInScreenPixels(): Boolean = false

    fun setMiUserId(id: String) {
        miUserId = id
    }

    fun setOnPilotoSeleccionado(callback: (PilotoRadar) -> Unit) {
        pilotoSeleccionado = callback
    }

    fun actualizarPilotos(nuevosPilotos: List<PilotoRadar>) {
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

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings?
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        if (tileBox.zoom < ZOOM_MINIMO || pilotos.isEmpty()) return

        val density = tileBox.density
        val radio = (RADIO_ICONO_PX * density).toInt()

        canvas.save()
        canvas.rotate(
            -tileBox.rotate,
            tileBox.centerPixelX.toFloat(),
            tileBox.centerPixelY.toFloat()
        )

        for (piloto in pilotos) {
            val x = tileBox.getPixXFromLatLon(piloto.lat, piloto.lon)
            val y = tileBox.getPixYFromLatLon(piloto.lat, piloto.lon)
            val esLocal = piloto.id == miUserId && miUserId.isNotBlank()

            val avatar = if (esLocal) {
                RadarFirebase.obtenerAvatarLocal(context) 
                    ?: (if (piloto.avatarUrl.isNotBlank()) avataresCache[piloto.avatarUrl] ?: RadarFirebase.obtenerAvatarCacheado(piloto.avatarUrl) else null)
            } else {
                if (piloto.avatarUrl.isNotBlank()) avataresCache[piloto.avatarUrl] ?: RadarFirebase.obtenerAvatarCacheado(piloto.avatarUrl) else null
            }

            if (esLocal) {
                val offsetX = OFFSET_LOCAL_X_DP * density
                val offsetY = OFFSET_LOCAL_Y_DP * density
                val avatarCx = x + offsetX
                val avatarCy = y + offsetY

                paintLinea.color = colorPorRango(piloto.rango)
                paintLinea.strokeWidth = 2.5f * density
                canvas.drawLine(x, y, avatarCx, avatarCy, paintLinea)

                if (avatar != null) {
                    dibujarAvatar(canvas, avatarCx, avatarCy, avatar, radio.toFloat(), piloto)
                } else {
                    dibujarPlaceholder(canvas, avatarCx, avatarCy, radio.toFloat(), piloto)
                }
                dibujarLabel(canvas, avatarCx, avatarCy + radio + 16 * density, piloto.nombre)
            } else {
                if (avatar != null) {
                    dibujarAvatar(canvas, x, y, avatar, radio.toFloat(), piloto)
                } else {
                    dibujarPlaceholder(canvas, x, y, radio.toFloat(), piloto)
                }
                dibujarLabel(canvas, x, y + radio + 16 * density, piloto.nombre)
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

        paintAvatarBorde.color = colorPorRango(piloto.rango)
        paintAvatarBorde.strokeWidth = 3f * getContext()!!.resources.displayMetrics.density
        canvas.drawCircle(cx, cy, radio, paintAvatarBorde)
    }

    private fun dibujarPlaceholder(
        canvas: Canvas, cx: Float, cy: Float,
        radio: Float, piloto: PilotoRadar
    ) {
        paintCirculo.color = colorPorRango(piloto.rango)
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

    private fun dibujarLabel(canvas: Canvas, cx: Float, cy: Float, texto: String) {
        val textoCorto = if (texto.length > 12) texto.take(12) + "." else texto
        val anchoTexto = paintTexto.measureText(textoCorto)
        val altoTexto = (paintTexto.descent() - paintTexto.ascent()).toInt()
        val density = getContext()!!.resources.displayMetrics.density
        val padding = 6f * density
        val radioFondo = 8f * density

        val left = cx - anchoTexto / 2 - padding
        val top = cy - altoTexto / 2 - padding
        val right = cx + anchoTexto / 2 + padding
        val bottom = cy + altoTexto / 2 + padding
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
        val app = application ?: return
        val point = result.point
        val tileBox = result.tileBox
        val density = tileBox.density
        val radius = (getScaledTouchRadius(app, tileBox.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER * 2.2f).toFloat()

        for (piloto in pilotos) {
            val esLocal = piloto.id == miUserId && miUserId.isNotBlank()
            val px = tileBox.getPixXFromLatLon(piloto.lat, piloto.lon)
            val py = tileBox.getPixYFromLatLon(piloto.lat, piloto.lon)

            val avatarCx = if (esLocal) px + OFFSET_LOCAL_X_DP * density else px
            val avatarCy = if (esLocal) py + OFFSET_LOCAL_Y_DP * density else py

            val dx = point.x - avatarCx
            val dy = point.y - avatarCy
            val touchDistSq = dx * dx + dy * dy
            val isNearAvatar = touchDistSq <= (radius * radius * 1.5f)
            val isNearBase = tileBox.isLatLonNearPixel(piloto.lat, piloto.lon, point.x, point.y, radius)

            if (isNearAvatar || isNearBase) {
                pilotoSeleccionado?.invoke(piloto)
                result.collect(piloto, this)
            }
        }
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        if (o is PilotoRadar) {
            val act: android.app.Activity = (mapActivity as? android.app.Activity)
                ?: (GestorRadar.obtenerMapActivity() as? android.app.Activity)
                ?: (application?.osmandMap?.mapView?.context as? android.app.Activity)
                ?: return false
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
