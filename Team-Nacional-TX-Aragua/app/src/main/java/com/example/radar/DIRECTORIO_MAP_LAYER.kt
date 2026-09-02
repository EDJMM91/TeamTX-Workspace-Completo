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

class DirectorioMapLayer(context: Context) : OsmandMapLayer(context),
    IContextMenuProvider {

    companion object {
        private const val ETIQUETA = "DIRECTORIO_MAP_LAYER"
        private const val ZOOM_MINIMO = 3
        private const val TOUCH_RADIUS_MULTIPLIER = 1.5f
        private const val OFFSET_CASHEA_X_DP = 34
        private const val OFFSET_CASHEA_Y_DP = -28
    }

    data class DirectorioMarcador(
        val id: Long,
        val lat: Double,
        val lon: Double,
        val nombre: String,
        val tipo: String,
        val direccion: String,
        val ciudad: String,
        val estado: String,
        val telefono: String,
        val whatsapp: String,
        val tieneCashea: Boolean,
        val plataformasCredito: String,
        val notas: String,
        val googleMapsUrl: String
    )

    private var directorios: List<DirectorioMarcador> = emptyList()
    private var directorioSeleccionado: ((DirectorioMarcador) -> Unit)? = null

    private val paintBitmap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val paintBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#FF9800")
    }
    private val paintLineaCashea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#00E676")
        pathEffect = DashPathEffect(floatArrayOf(8f, 5f), 0f)
    }
    private val paintFondoCashea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#122A1A")
        style = Paint.Style.FILL
    }
    private val paintBordeCashea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#00E676")
    }
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
        isFakeBoldText = true
    }
    private val paintFondoTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 15, 20, 30)
        style = Paint.Style.FILL
    }

    private var iconoBandera: Bitmap? = null
    private var iconoCashea: Bitmap? = null
    private val rectTemporal = RectF()

    override fun initLayer(view: OsmandMapTileView) {
        super.initLayer(view)
        iconoBandera = cargarDrawable("bandera")
        iconoCashea = cargarDrawable("logocashea")
        Log.d(ETIQUETA, "DirectorioMapLayer inicializado")
    }

    private fun cargarDrawable(nombre: String): Bitmap? {
        return try {
            val resId = context.resources.getIdentifier(nombre, "drawable", context.packageName)
            if (resId != 0) {
                val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
                    val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
                    val targetW = if (nombre == "logocashea") 64 else 64
                    val targetH = (targetW * (h.toFloat() / w.toFloat())).toInt()
                    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, targetW, targetH)
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
        iconoBandera?.recycle()
        iconoCashea?.recycle()
        directorios = emptyList()
    }

    override fun drawInScreenPixels(): Boolean = false

    fun setOnDirectorioSeleccionado(callback: (DirectorioMarcador) -> Unit) {
        directorioSeleccionado = callback
    }

    fun actualizarDirectorios(nuevosDirectorios: List<DirectorioMarcador>) {
        directorios = nuevosDirectorios
        getTileView()?.refreshMap()
    }

    fun limpiarDirectorios() {
        directorios = emptyList()
        getTileView()?.refreshMap()
    }

    fun getDirectorios(): List<DirectorioMarcador> = directorios

    override fun onPrepareBufferImage(
        canvas: Canvas,
        tileBox: RotatedTileBox,
        settings: DrawSettings?
    ) {
        super.onPrepareBufferImage(canvas, tileBox, settings)
        if (tileBox.zoom < ZOOM_MINIMO || directorios.isEmpty()) return

        val density = tileBox.density
        val tamBandera = (38 * density).toInt()
        val tamCashea = (24 * density).toInt()

        canvas.save()
        canvas.rotate(
            -tileBox.rotate,
            tileBox.centerPixelX.toFloat(),
            tileBox.centerPixelY.toFloat()
        )

        for (item in directorios) {
            val x = tileBox.getPixXFromLatLon(item.lat, item.lon)
            val y = tileBox.getPixYFromLatLon(item.lat, item.lon)

            // 1. DIBUJAR ÍCONO DE BANDERA EN EL PUNTO PRINCIPAL
            val bandera = iconoBandera
            if (bandera != null) {
                rectTemporal.set(
                    x - tamBandera / 2f,
                    y - tamBandera.toFloat(),
                    x + tamBandera / 2f,
                    y
                )
                canvas.drawBitmap(bandera, null, rectTemporal, paintBitmap)
            } else {
                paintBorde.color = Color.parseColor("#FF9800")
                paintBorde.style = Paint.Style.FILL
                canvas.drawCircle(x, y - tamBandera / 2f, 14f * density, paintBorde)
            }

            // 2. SI TIENE CASHEA: DIBUJAR LÍNEA CONECTORA Y BADGE CON LOGOCASHEA
            if (item.tieneCashea) {
                val cxCashea = x + OFFSET_CASHEA_X_DP * density
                val cyCashea = y + OFFSET_CASHEA_Y_DP * density

                // Línea conectora
                paintLineaCashea.strokeWidth = 2f * density
                canvas.drawLine(x, y - tamBandera / 2f, cxCashea, cyCashea, paintLineaCashea)

                // Contenedor / Disco del logo Cashea
                val radioCashea = tamCashea / 1.4f
                canvas.drawCircle(cxCashea, cyCashea, radioCashea, paintFondoCashea)
                paintBordeCashea.strokeWidth = 1.8f * density
                canvas.drawCircle(cxCashea, cyCashea, radioCashea, paintBordeCashea)

                val cashea = iconoCashea
                if (cashea != null) {
                    rectTemporal.set(
                        cxCashea - tamCashea / 2f,
                        cyCashea - tamCashea / 2f,
                        cxCashea + tamCashea / 2f,
                        cyCashea + tamCashea / 2f
                    )
                    canvas.drawBitmap(cashea, null, rectTemporal, paintBitmap)
                }
            }

            // 3. ETIQUETA INFORMATIVA CON NOMBRE
            val textoCorto = if (item.nombre.length > 15) item.nombre.take(15) + ".." else item.nombre
            val labelY = y + 12 * density
            val anchoTexto = paintTexto.measureText(textoCorto)
            val altoTexto = (paintTexto.descent() - paintTexto.ascent()).toInt()
            val padding = 5f * density
            val radioFondo = 6f * density

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
        val radius = (getScaledTouchRadius(app, tileBox.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER * 1.6f).toFloat()

        for (item in directorios) {
            val isNearBase = tileBox.isLatLonNearPixel(item.lat, item.lon, point.x, point.y, radius)
            val isNearCashea = if (item.tieneCashea) {
                val density = tileBox.density
                val cxCashea = tileBox.getPixXFromLatLon(item.lat, item.lon) + OFFSET_CASHEA_X_DP * density
                val cyCashea = tileBox.getPixYFromLatLon(item.lat, item.lon) + OFFSET_CASHEA_Y_DP * density
                val dx = point.x - cxCashea
                val dy = point.y - cyCashea
                (dx * dx + dy * dy) <= (radius * radius)
            } else false

            if (isNearBase || isNearCashea) {
                directorioSeleccionado?.invoke(item)
                result.collect(item, this)
            }
        }
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        if (o is DirectorioMarcador) {
            val act = (mapActivity as? android.app.Activity)
                ?: (application?.osmandMap?.mapView?.context as? android.app.Activity)
                ?: return false
            DialogosMapaTx.mostrarDirectorio(act, o)
            return true
        }
        return false
    }

    override fun getObjectLocation(o: Any?): LatLon? {
        if (o is DirectorioMarcador) {
            return LatLon(o.lat, o.lon)
        }
        return null
    }

    override fun getObjectName(o: Any?): PointDescription? {
        if (o is DirectorioMarcador) {
            return PointDescription(PointDescription.POINT_TYPE_MARKER, o.nombre)
        }
        return null
    }
}
