package com.example.ui.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * Singleton para gestionar las preferencias persistentes de la app Team TX.
 * Usa SharedPreferences para guardar configuraciones de UI/UX inmediatamente.
 * Estas preferencias NO se borran al cerrar sesión, solo al desinstalar la app.
 */
object PreferenciasApp {

    private const val PREFS_NAME = "team_tx_app_preferences"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _modoOscuroState.value = prefs.getBoolean("modo_oscuro", false)
        }
    }

    // ─── GLOBAL ───────────────────────────────────────────────────────────────

    /** Estado reactivo para que el Theme composable observe cambios de inmediato */
    private val _modoOscuroState = mutableStateOf(false)
    val modoOscuroState get() = _modoOscuroState

    /** Modo oscuro: fijado en false para tema claro unificado */
    var modoOscuro: Boolean
        get() = prefs.getBoolean("modo_oscuro", false)
        set(value) {
            prefs.edit().putBoolean("modo_oscuro", value).apply()
            _modoOscuroState.value = value
        }

    /** Notificaciones push globales */
    var notificacionesActivas: Boolean
        get() = prefs.getBoolean("notificaciones_activas", true)
        set(value) = prefs.edit().putBoolean("notificaciones_activas", value).apply()

    /** Sonido al recibir mensajes de chat */
    var sonidoChat: Boolean
        get() = prefs.getBoolean("sonido_chat", true)
        set(value) = prefs.edit().putBoolean("sonido_chat", value).apply()

    /** Tamaño de texto: "normal" o "grande" */
    var tamanoTexto: String
        get() = prefs.getString("tamano_texto", "normal") ?: "normal"
        set(value) = prefs.edit().putString("tamano_texto", value).apply()

    // ─── MÓDULO 1: MURO ───────────────────────────────────────────────────────

    /** Mostrar publicaciones fijadas siempre al tope */
    var muroFijadosPrimero: Boolean
        get() = prefs.getBoolean("muro_fijados_primero", true)
        set(value) = prefs.edit().putBoolean("muro_fijados_primero", value).apply()

    /** Auto-colapsar comentarios al abrir el feed */
    var muroAutoColapsarComentarios: Boolean
        get() = prefs.getBoolean("muro_auto_colapsar_comentarios", false)
        set(value) = prefs.edit().putBoolean("muro_auto_colapsar_comentarios", value).apply()

    // ─── MÓDULO 2: CHAT ───────────────────────────────────────────────────────

    /** Mostrar avatar/foto de perfil en el chat */
    var chatMostrarAvatar: Boolean
        get() = prefs.getBoolean("chat_mostrar_avatar", true)
        set(value) = prefs.edit().putBoolean("chat_mostrar_avatar", value).apply()

    /** Vibrar al recibir mensaje en chat */
    var chatVibrar: Boolean
        get() = prefs.getBoolean("chat_vibrar", true)
        set(value) = prefs.edit().putBoolean("chat_vibrar", value).apply()

    // ─── MÓDULO 3: RODADAS ────────────────────────────────────────────────────

    /** Unidad de distancia preferida */
    var rodasUnidadKm: Boolean
        get() = prefs.getBoolean("rodadas_unidad_km", true)
        set(value) = prefs.edit().putBoolean("rodadas_unidad_km", value).apply()

    /** Recordar próxima rodada programada con notificación */
    var rodadasRecordatorio: Boolean
        get() = prefs.getBoolean("rodadas_recordatorio", true)
        set(value) = prefs.edit().putBoolean("rodadas_recordatorio", value).apply()

    // ─── MÓDULO 4: MIEMBROS ───────────────────────────────────────────────────

    /** Vista de miembros: true = lista, false = tarjetas */
    var miembrosVistaLista: Boolean
        get() = prefs.getBoolean("miembros_vista_lista", false)
        set(value) = prefs.edit().putBoolean("miembros_vista_lista", value).apply()

    /** Mostrar solo miembros activos por defecto */
    var miembrosSoloActivos: Boolean
        get() = prefs.getBoolean("miembros_solo_activos", true)
        set(value) = prefs.edit().putBoolean("miembros_solo_activos", value).apply()

    // ─── MÓDULO 5: TESORERÍA ──────────────────────────────────────────────────

    /** Moneda preferida: "USD" o "BS" */
    var tesoreraMoneda: String
        get() = prefs.getString("tesoreria_moneda", "USD") ?: "USD"
        set(value) = prefs.edit().putString("tesoreria_moneda", value).apply()

    /** Actualizar tasa BCV automáticamente al abrir finanzas */
    var tesoreriaBcvAuto: Boolean
        get() = prefs.getBoolean("tesoreria_bcv_auto", true)
        set(value) = prefs.edit().putBoolean("tesoreria_bcv_auto", value).apply()

    // ─── MÓDULO 6: INVENTARIO ─────────────────────────────────────────────────

    /** Alertar cuando un ítem tenga stock bajo (≤2) */
    var inventarioAlertaStock: Boolean
        get() = prefs.getBoolean("inventario_alerta_stock", true)
        set(value) = prefs.edit().putBoolean("inventario_alerta_stock", value).apply()

    /** Mostrar solo ítems disponibles por defecto */
    var inventarioSoloDisponibles: Boolean
        get() = prefs.getBoolean("inventario_solo_disponibles", false)
        set(value) = prefs.edit().putBoolean("inventario_solo_disponibles", value).apply()

    // ─── MÓDULO 7: SOS VIAL ───────────────────────────────────────────────────

    /** Compartir ubicación GPS al emitir una alerta SOS */
    var sosCompartirUbicacion: Boolean
        get() = prefs.getBoolean("sos_compartir_ubicacion", true)
        set(value) = prefs.edit().putBoolean("sos_compartir_ubicacion", value).apply()

    /** Consentimiento transparente para acceso a GPS en emergencias */
    var sosConsentimientoUbicacionAceptado: Boolean
        get() = prefs.getBoolean("sos_consentimiento_gps_aceptado", false)
        set(value) = prefs.edit().putBoolean("sos_consentimiento_gps_aceptado", value).apply()

    /** Reproducir sonido de alerta al recibir un SOS activo */
    var sosSonidoAlerta: Boolean
        get() = prefs.getBoolean("sos_sonido_alerta", true)
        set(value) = prefs.edit().putBoolean("sos_sonido_alerta", value).apply()

    /** Último tipo de alerta SOS seleccionado para emisión rápida desde el Mapa TX (nombre del enum EmergencyType) */
    var sosMapaTipoRapido: String?
        get() = prefs.getString("sos_mapa_tipo_rapido", null)
        set(value) = if (value != null) prefs.edit().putString("sos_mapa_tipo_rapido", value).apply()
                     else prefs.edit().remove("sos_mapa_tipo_rapido").apply()


    // ─── MÓDULO 8: NORMATIVAS ─────────────────────────────────────────────────

    /** Tamaño de texto en las normativas: "normal" o "grande" */
    var normativasTamanoTexto: String
        get() = prefs.getString("normativas_tamano_texto", "normal") ?: "normal"
        set(value) = prefs.edit().putString("normativas_tamano_texto", value).apply()

    // ─── MÓDULO 9: DIRECTIVA ──────────────────────────────────────────────────

    /** Auto-ocultar barra de navegación inferior al entrar al panel directiva */
    var directivaAutoOcultarNav: Boolean
        get() = prefs.getBoolean("directiva_auto_ocultar_nav", true)
        set(value) = prefs.edit().putBoolean("directiva_auto_ocultar_nav", value).apply()

    // ─── MÓDULO 10: CARNET TX ─────────────────────────────────────────────────

    /** Mostrar código QR en el carnet */
    var carnetMostrarQr: Boolean
        get() = prefs.getBoolean("carnet_mostrar_qr", true)
        set(value) = prefs.edit().putBoolean("carnet_mostrar_qr", value).apply()

    /** Tipo de imagen a mostrar en Carnet TX: "PERFIL" (foto subida de perfil) o "CORREO" (foto cuenta Google) */
    var carnetTipoFoto: String
        get() = prefs.getString("carnet_tipo_foto", "PERFIL") ?: "PERFIL"
        set(value) = prefs.edit().putString("carnet_tipo_foto", value).apply()

    /** URL de la foto de la cuenta Google vinculada */
    var carnetGooglePhotoUrl: String?
        get() = prefs.getString("carnet_google_photo_url", null)
        set(value) = prefs.edit().putString("carnet_google_photo_url", value).apply()

    // ─── MÓDULO 11: VELOCÍMETRO & TELEMETRÍA ──────────────────────────────────

    /** Récord de velocidad máxima histórica alcanzada (en KM/H) */
    var topSpeedRecordKmh: Float
        get() = prefs.getFloat("top_speed_record_kmh", 0f)
        set(value) = prefs.edit().putFloat("top_speed_record_kmh", value).apply()

    /** Unidad de velocidad seleccionada: false = KM/H, true = MPH */
    var velocidadEnMph: Boolean
        get() = prefs.getBoolean("velocidad_en_mph", false)
        set(value) = prefs.edit().putBoolean("velocidad_en_mph", value).apply()

    /** Odómetro total acumulado de la moto (en KM) */
    var odometroTotalKm: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong("odometro_total_km_bits", java.lang.Double.doubleToLongBits(0.0)))
        set(value) = prefs.edit().putLong("odometro_total_km_bits", java.lang.Double.doubleToLongBits(value)).apply()

    /** Registrar Odómetro continuo en toda la app (GPS continuo en primer plano) */
    var odometroGlobalActivo: Boolean
        get() = prefs.getBoolean("odometro_global_activo", false)
        set(value) = prefs.edit().putBoolean("odometro_global_activo", value).apply()

    /** Radar Táctico / Compartir ubicación persistente */
    var radarActivo: Boolean
        get() = prefs.getBoolean("radar_activo_persistente", false)
        set(value) = prefs.edit().putBoolean("radar_activo_persistente", value).apply()

    // ─── ACCESOS RÁPIDOS BARRA INFERIOR (PERSISTENCIA TOTAL) ─────────────────
    private const val KEY_BOTTOM_TABS = "accesos_rapidos_bottom_tabs_config"

    fun guardarBottomTabs(tabs: List<String>) {
        prefs.edit().putString(KEY_BOTTOM_TABS, tabs.joinToString(",")).apply()
    }

    fun obtenerBottomTabs(): List<String>? {
        if (!::prefs.isInitialized) return null
        val guardado = prefs.getString(KEY_BOTTOM_TABS, null) ?: return null
        val items = guardado.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return if (items.size == 5) items else null
    }

    // ─── REGISTRO Y CONTEO DE USO DE MÓDULOS (MÁS USADOS) ──────────────────

    /** Incrementar el contador de uso de un módulo para ordenar los más usados */
    fun registrarUsoModulo(tabName: String) {
        if (!::prefs.isInitialized) return
        val currentCount = prefs.getInt("uso_modulo_$tabName", 0)
        prefs.edit().putInt("uso_modulo_$tabName", currentCount + 1).apply()
    }

    /** Obtener el mapa de conteos de uso para ordenar módulos en el Dashboard */
    fun obtenerUsoModulosMap(): Map<String, Int> {
        if (!::prefs.isInitialized) return emptyMap()
        val allMap = prefs.all
        val map = mutableMapOf<String, Int>()
        for ((key, value) in allMap) {
            if (key.startsWith("uso_modulo_") && value is Int) {
                map[key.removePrefix("uso_modulo_")] = value
            }
        }
        return map
    }

    // ─── GOBERNANZA & DIRECTIVA INSTITUCIONAL ──────────────────────────────────
    /** Lema oficial o comunicado de cabecera del Capítulo */
    var lemaClub: String
        get() = if (::prefs.isInitialized) prefs.getString("directiva_lema_club", "Team Nacional TX Aragua • Hermandad y Asfalto") ?: "Team Nacional TX Aragua • Hermandad y Asfalto" else "Team Nacional TX Aragua • Hermandad y Asfalto"
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("directiva_lema_club", value).apply() }

    /** Monto referencial mensual de membresía */
    var cuotaMembresiaRef: String
        get() = if (::prefs.isInitialized) prefs.getString("directiva_cuota_ref", "5 $") ?: "5 $" else "5 $"
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("directiva_cuota_ref", value).apply() }

    /** Política del Feed: solo directiva puede publicar avisos */
    var soloDirectivaPublicaFeed: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("directiva_solo_publica_feed", false) else false
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean("directiva_solo_publica_feed", value).apply() }

    // ─── PACTO DE HONOR BIKER ──────────────────────────────────────────────────

    /** Verifica si el miembro ya aceptó el compromiso de honor biker y buen uso */
    fun haAceptadoCompromisoBiker(memberId: Long): Boolean {
        if (!::prefs.isInitialized) return false
        val clave = if (memberId > 0) "compromiso_biker_aceptado_$memberId" else "compromiso_biker_aceptado_global"
        return prefs.getBoolean(clave, false)
    }

    /** Marca como aceptado el compromiso de honor biker */
    fun setCompromisoBikerAceptado(memberId: Long, aceptado: Boolean) {
        if (!::prefs.isInitialized) return
        val clave = if (memberId > 0) "compromiso_biker_aceptado_$memberId" else "compromiso_biker_aceptado_global"
        prefs.edit().putBoolean(clave, aceptado).apply()
    }

    // ─── CONTROL DE SESIÓN Y DISPOSITIVO ÚNICO (ANTI-TRAMPAS) ───────────────────

    /** Identificador único persistente para este dispositivo */
    fun obtenerDeviceId(): String {
        if (!::prefs.isInitialized) return "DEV_DEFAULT"
        var id = prefs.getString("dispositivo_unico_uuid", null)
        if (id.isNullOrBlank()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("dispositivo_unico_uuid", id).apply()
        }
        return id
    }
}
