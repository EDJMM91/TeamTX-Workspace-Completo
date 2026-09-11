package com.example.radar

import android.content.Context
import android.graphics.*
import android.util.Log
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.RotatedTileBox
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider
import net.osmand.plus.views.layers.MapSelectionResult
import net.osmand.plus.views.layers.MapSelectionRules
import net.osmand.plus.views.layers.base.OsmandMapLayer

class SitiosInteresMapLayer(context: Context) : OsmandMapLayer(context), IContextMenuProvider {

    companion object {
        private const val ETIQUETA = "SITIOS_MAP_LAYER"
        private const val ZOOM_MINIMO = 3
        private const val TOUCH_RADIUS_MULTIPLIER = 1.6f
    }

    data class SitioInteresMarcador(
        val id: Long,
        val lat: Double,
        val lon: Double,
        val nombre: String,
        val categoria: String,
        val descripcion: String,
        val direccion: String,
        val telefono: String,
        val imageUrl: String?,
        val iconoDrawableName: String,
        val likesCount: Int = 0,
        val dislikesCount: Int = 0
    )

    private var sitios: List<SitioInteresMarcador> = emptyList()
    private var sitioSeleccionado: ((SitioInteresMarcador) -> Unit)? = null

    private val paintBitmap = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val paintFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#181818")
        style = Paint.Style.FILL
    }
    private val paintBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#00E5FF")
    }
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val paintFondoLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60F172A")
        style = Paint.Style.FILL
    }

    private var iconoTeam: Bitmap? = null
    private val rectTemporal = RectF()

    init {
        iconoTeam = cargarDrawable("logoteam")
    }

    private fun cargarDrawable(nombre: String): Bitmap? {
        val resId = context.resources.getIdentifier(nombre, "drawable", context.packageName)
        if (resId == 0) return null
        return try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (_: Exception) { null }
    }

    fun setOnSitioSeleccionado(callback: (SitioInteresMarcador) -> Unit) {
        sitioSeleccionado = callback
    }

    fun actualizarSitios(nuevosSitios: List<SitioInteresMarcador>) {
        sitios = nuevosSitios
        getTileView()?.refreshMap()
    }

    fun limpiarSitios() {
        sitios = emptyList()
        getTileView()?.refreshMap()
    }

    override fun onPrepareBufferImage(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings?) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        if (tileBox.zoom < ZOOM_MINIMO || sitios.isEmpty()) return

        val density = tileBox.density

        canvas.save()
        canvas.rotate(-tileBox.rotate, tileBox.centerPixelX.toFloat(), tileBox.centerPixelY.toFloat())

        for (item in sitios) {
            val x = tileBox.getPixXFromLatLon(item.lat, item.lon)
            val y = tileBox.getPixYFromLatLon(item.lat, item.lon)

            val radio = 16f * density
            canvas.drawCircle(x, y, radio, paintFondo)
            canvas.drawCircle(x, y, radio, paintBorde)

            val bmpIcono = cargarDrawable(item.iconoDrawableName) ?: iconoTeam
            if (bmpIcono != null) {
                rectTemporal.set(x - radio * 0.7f, y - radio * 0.7f, x + radio * 0.7f, y + radio * 0.7f)
                canvas.drawBitmap(bmpIcono, null, rectTemporal, paintBitmap)
            }

            val textoCorto = if (item.nombre.length > 14) item.nombre.take(14) + ".." else item.nombre
            val labelY = y + radio + 14 * density
            val anchoTexto = paintTexto.measureText(textoCorto)
            rectTemporal.set(x - anchoTexto / 2f - 6 * density, labelY - 14 * density, x + anchoTexto / 2f + 6 * density, labelY + 6 * density)
            canvas.drawRoundRect(rectTemporal, 6 * density, 6 * density, paintFondoLabel)
            canvas.drawText(textoCorto, x, labelY, paintTexto)
        }

        canvas.restore()
    }

    override fun drawInScreenPixels(): Boolean = false

    override fun onDraw(canvas: Canvas?, tileBox: RotatedTileBox?, settings: DrawSettings?) {
        if (canvas != null && tileBox != null) {
            onPrepareBufferImage(canvas, tileBox, settings)
        }
    }

    override fun collectObjectsFromPoint(result: MapSelectionResult, rules: MapSelectionRules) {
        if (sitios.isEmpty()) return
        val point = result.point
        val tileBox = result.tileBox
        val density = tileBox.density
        val osmandApp = context.applicationContext as? net.osmand.plus.OsmandApplication ?: return
        val radius = (getScaledTouchRadius(osmandApp, tileBox.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER * 2f).toFloat()

        for (item in sitios) {
            val px = tileBox.getPixXFromLatLon(item.lat, item.lon)
            val py = tileBox.getPixYFromLatLon(item.lat, item.lon)
            val dx = point.x - px
            val dy = point.y - py
            if ((dx * dx + dy * dy) <= (radius * radius)) {
                result.collect(item, this)
            }
        }
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        if (o is SitioInteresMarcador) {
            val act = mapActivity as? android.app.Activity
            if (act != null) {
                act.runOnUiThread {
                    sitioSeleccionado?.invoke(o)
                }
                return true
            }
        }
        return false
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        if (o is SitioInteresMarcador) return LatLon(o.lat, o.lon)
        return null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        if (o is SitioInteresMarcador) return PointDescription(PointDescription.POINT_TYPE_MARKER, o.nombre)
        return null
    }
}
