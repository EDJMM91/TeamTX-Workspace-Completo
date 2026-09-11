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
        val rating: Double = 5.0,
        val notas: String,
        val googleMapsUrl: String,
        val imageUrl: String? = null,
        val iconoDrawableName: String = "ic_action_repair"
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

    // Estilos para comercios destacados / bien calificados (Rating >= 4.5)
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#45FFD700")
        style = Paint.Style.FILL
    }
    private val paintGlowBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#FFD700")
    }
    private val paintStarFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }
    private val paintStarBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        color = Color.parseColor("#FFD700")
    }
    private val paintStarEmoji = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
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
    private val paintBordeTextoRecomendado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#FFD700")
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
            val esRecomendado = item.rating >= 4.5

            // 0. SI ESTÁ BIEN CALIFICADO (>=4.5): RESALTAR CON HALO DORADO Y ESTRELLA TOP
            if (esRecomendado) {
                // Halo brillante dorado de fondo
                canvas.drawCircle(x, y - tamBandera / 2f, tamBandera / 1.4f, paintGlow)
                canvas.drawCircle(x, y - tamBandera / 2f, tamBandera / 1.6f, paintGlowBorder)

                // Insignia de Estrella Dorada ⭐ flotando encima de la bandera
                val starY = y - tamBandera - 7 * density
                val starRadius = 8.5f * density
                canvas.drawCircle(x, starY, starRadius, paintStarFondo)
                canvas.drawCircle(x, starY, starRadius, paintStarBorde)
                canvas.drawText("⭐", x, starY + 4f * density, paintStarEmoji)
            }

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
                paintBorde.color = if (esRecomendado) Color.parseColor("#FFD700") else Color.parseColor("#FF9800")
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
            val prefijo = if (esRecomendado) "⭐ " else ""
            val baseNombre = if (item.nombre.length > 13) item.nombre.take(13) + ".." else item.nombre
            val textoCorto = prefijo + baseNombre
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
            if (esRecomendado) {
                canvas.drawRoundRect(left, top, right, bottom, radioFondo, radioFondo, paintBordeTextoRecomendado)
            }
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
        val density = tileBox.density
        val radius = (getScaledTouchRadius(app, tileBox.defaultRadiusPoi) * TOUCH_RADIUS_MULTIPLIER * 2.2f).toFloat()
        val tamBandera = (38 * density).toInt()

        for (item in directorios) {
            val px = tileBox.getPixXFromLatLon(item.lat, item.lon)
            val py = tileBox.getPixYFromLatLon(item.lat, item.lon)

            val flagCx = px
            val flagCy = py - tamBandera / 2f
            val dxFlag = point.x - flagCx
            val dyFlag = point.y - flagCy
            val isNearFlag = (dxFlag * dxFlag + dyFlag * dyFlag) <= (radius * radius * 1.5f)

            val isNearBase = tileBox.isLatLonNearPixel(item.lat, item.lon, point.x, point.y, radius)

            val isNearCashea = if (item.tieneCashea) {
                val cxCashea = px + OFFSET_CASHEA_X_DP * density
                val cyCashea = py + OFFSET_CASHEA_Y_DP * density
                val dx = point.x - cxCashea
                val dy = point.y - cyCashea
                (dx * dx + dy * dy) <= (radius * radius * 1.5f)
            } else false

            if (isNearFlag || isNearBase || isNearCashea) {
                directorioSeleccionado?.invoke(item)
                result.collect(item, this)
            }
        }
    }

    override fun runExclusiveAction(o: Any?, unknownLocation: Boolean): Boolean {
        if (o is DirectorioMarcador) {
            val act: android.app.Activity = (mapActivity as? android.app.Activity)
                ?: (GestorRadar.obtenerMapActivity() as? android.app.Activity)
                ?: (application?.osmandMap?.mapView?.context as? android.app.Activity)
                ?: return false
            act.runOnUiThread {
                DialogosMapaTx.mostrarDirectorio(act, o)
            }
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
