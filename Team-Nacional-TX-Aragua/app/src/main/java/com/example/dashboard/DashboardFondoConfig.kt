package com.example.dashboard

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Tipo de fondo configurado para el Dashboard Principal.
 */
enum class TipoFondoDashboard {
    TEMA_CLARO_ESTANDAR,
    DEGRADADO_TACTICO,
    IMAGEN_PERSONALIZADA,
    IMAGEN_DRAWABLE
}

/**
 * Gestor y configuración persistente del fondo del Dashboard.
 * Permite cambiar el fondo dinámicamente por imagen, degradado o color sólido.
 */
object DashboardFondoConfig {

    private const val PREFS_NAME = "prefs_dashboard_tx"
    private const val KEY_TIPO = "dash_bg_tipo"
    private const val KEY_URI = "dash_bg_uri"
    private const val KEY_OPACIDAD = "dash_bg_opacidad"
    private const val KEY_DRAWABLE = "dash_bg_drawable"

    var tipoFondo by mutableStateOf(TipoFondoDashboard.TEMA_CLARO_ESTANDAR)
    var imagenUri by mutableStateOf<String?>(null)
    var drawableResId by mutableStateOf<Int?>(null)
    var opacidadSuperposicion by mutableFloatStateOf(0.94f)

    // Paleta oficial para el tema claro del Dashboard
    val ColorFondoClaro = Color(0xFFF6F8FA)
    val ColorTarjetaClara = Color(0xFFFFFFFF)
    val ColorBordeClaro = Color(0xFFE3E6EB)
    val ColorTextoPrimario = Color(0xFF16181D)
    val ColorTextoSecundario = Color(0xFF64748B)
    val ColorRojoCarrera = Color(0xFFE52323)
    val ColorDoradoOro = Color(0xFFFFB300)
    val ColorContenedorRojo = Color(0xFFFFEBEE)
    val ColorContenedorAzul = Color(0xFFE3F2FD)
    val ColorContenedorDorado = Color(0xFFFFF8E1)
    val ColorContenedorVerde = Color(0xFFE8F5E9)

    fun inicializar(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tipoName = prefs.getString(KEY_TIPO, TipoFondoDashboard.TEMA_CLARO_ESTANDAR.name)
        tipoFondo = try {
            TipoFondoDashboard.valueOf(tipoName ?: TipoFondoDashboard.TEMA_CLARO_ESTANDAR.name)
        } catch (_: Exception) {
            TipoFondoDashboard.TEMA_CLARO_ESTANDAR
        }
        imagenUri = prefs.getString(KEY_URI, null)
        opacidadSuperposicion = prefs.getFloat(KEY_OPACIDAD, 0.94f)
        val res = prefs.getInt(KEY_DRAWABLE, 0)
        drawableResId = if (res != 0) res else null
    }

    fun configurarImagen(context: Context, uri: String?, opacidad: Float = 0.90f) {
        imagenUri = uri
        tipoFondo = if (!uri.isNullOrBlank()) TipoFondoDashboard.IMAGEN_PERSONALIZADA else TipoFondoDashboard.TEMA_CLARO_ESTANDAR
        opacidadSuperposicion = opacidad
        guardar(context)
    }

    fun configurarDrawable(context: Context, resId: Int, opacidad: Float = 0.90f) {
        drawableResId = resId
        tipoFondo = TipoFondoDashboard.IMAGEN_DRAWABLE
        opacidadSuperposicion = opacidad
        guardar(context)
    }

    fun restaurarTemaClaro(context: Context) {
        tipoFondo = TipoFondoDashboard.TEMA_CLARO_ESTANDAR
        imagenUri = null
        drawableResId = null
        opacidadSuperposicion = 0.94f
        guardar(context)
    }

    private fun guardar(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TIPO, tipoFondo.name)
            .putString(KEY_URI, imagenUri)
            .putFloat(KEY_OPACIDAD, opacidadSuperposicion)
            .putInt(KEY_DRAWABLE, drawableResId ?: 0)
            .apply()
    }
}
