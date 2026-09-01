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

class EventosMapLayer(context: Context) : OsmandMapLayer(context),
    IContextMenuProvider {

    companion object {
        private const val ETIQUETA = "EVENTOS_MAP_LAYER"
        private const val ZOOM_MINIMO = 3
        private const val TOUCH_RADIUS_MULTIPLIER = 1.5f
    }

    data class EventoMarcador(
        val lat: Double,
        val lon: Double,
        val titulo: String,
        val descripcion: String,
        val esCalendario: Boolean
    )

    private var eventos: List<EventoMarcador> = emptyList()
    private var eventoSeleccionado: ((EventoMarcador) -> Unit)? = null

    private val paintIcono = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val paintBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
        isFakeBoldText = true
    }
    private val paintFondoTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private var iconoPosition: Bitmap? = null
    private var iconoTeam: Bitmap? = null
    private val limitarRect = RectF()

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        iconoPosition = cargarDrawable("iconoposicion")
        iconoTeam = cargarDrawable("logoteam")
        Log.d(ETIQUETA, "EventosMapLayer inicializado")
    }

    private fun cargarDrawable(nombre: String): Bitmap? {
        return try {
            val resId = context.resources.getIdentifier(nombre, "drawable", context.packageName)
            if (resId != 0) {
                val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val bmp = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, 72, 72)
                    drawable.draw(canvas)
                    bmp
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Error cargando drawable $nombre: ${e.message}")
            null
        }
    }

    override fun destroyLayer() {
        super.destroyLayer()
        iconoPosition?.recycle()
        iconoTeam?.recycle()
        eventos = emptyList()
    }

    override fun drawInScreenPixels(): Boolean = false

    fun setOnEventoSeleccionado(callback: (EventoMarcador) -> Unit) {
        eventoSeleccionado = callback
    }

    fun actualizarEventos(nuevosEventos: List<EventoMarcador>) {
        eventos = nuevosEventos
        getTileView()?.refreshMap()
    }

    fun limpiarEventos() {
        eventos = emptyList()
        getTileView()?.refreshMap()
    }

    fun getEventos(): List<EventoMarcador> = eventos

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings?
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        if (tileBox.zoom < ZOOM_MINIMO || eventos.isEmpty()) return

        val density = tileBox.density
        val tamIcono = (50 * density).toInt()

        canvas.save()
        canvas.rotate(
            -tileBox.rotate,
            tileBox.centerPixelX.toFloat(),
            tileBox.centerPixelY.toFloat()
        )

        for (evento in eventos) {
            val x = tileBox.getPixXFromLatLon(evento.lat, evento.lon)
            val y = tileBox.getPixYFromLatLon(evento.lat, evento.lon)

            val icono = iconoTeam ?: iconoPosition

            if (icono != null) {
                limitarRect.set(
                    x - tamIcono / 2f,
                    y - tamIcono.toFloat(),
                    x + tamIcono / 2f,
                    y
                )
                canvas.drawBitmap(icono, null, limitarRect, paintIcono)

                paintBorde.color = Color.parseColor("#E53935")
                paintBorde.strokeWidth = 2f * density
                canvas.drawCircle(x, y - tamIcono / 2f, tamIcono / 2f + 2 * density, paintBorde)
            } else {
                paintIcono.color = Color.parseColor("#E53935")
                canvas.drawCircle(x, y, 20f * density, paintIcono)
            }

            val textoCorto = if (evento.titulo.length > 16) evento.titulo.take(16) + "." else evento.titulo
            val labelY = y + 14 * density
            val anchoTexto = paintTexto.measureText(textoCorto)
            val altoTexto = (paintTexto.descent() - paintTexto.ascent()).toInt()
            val padding = 6f * density
            val radioFondo = 8f * density

            val left = x - anchoTexto / 2 - padding
            val top = labelY - altoTexto / 2 - padding
            val right = x + anchoTexto / 2 + padding
            val bottom = labelY + altoTexto / 2 + padding
            canvas.drawRoundRect(left, top, right, bottom, radioFondo, radioFondo, paintFondoTexto)
            canvas.drawText(textoCorto, x, labelY + altoTexto / 4, paintTexto)
        }

        canvas.restore()
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
        val radius = (getScaledTouchRadius(app, tileBox.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER * 1.5f).toFloat()

        for (evento in eventos) {
            if (tileBox.isLatLonNearPixel(evento.lat, evento.lon, point.x, point.y, radius)) {
                eventoSeleccionado?.invoke(evento)
                result.collect(evento, this)
            }
        }
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        if (o is EventoMarcador) {
            val act = (mapActivity as? android.app.Activity)
                ?: (application?.osmandMap?.mapView?.context as? android.app.Activity)
                ?: return false
            DialogosMapaTx.mostrarEvento(act, o)
            return true
        }
        return false
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        if (o is EventoMarcador) {
            return LatLon(o.lat, o.lon)
        }
        return null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        if (o is EventoMarcador) {
            return PointDescription(PointDescription.POINT_TYPE_MARKER, o.titulo)
        }
        return null
    }
}
