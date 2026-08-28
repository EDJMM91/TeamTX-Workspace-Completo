package com.example

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.NotificacionApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NOTIFICACIONES_APP — Módulo independiente de notificaciones internas de la app.
 * Gestiona notificaciones de: Chat, Muro, SOS, Perfil, Vinculación Google.
 * No confundir con FCM (push notifications remotas).
 */
object GestorNotificacionesApp {

    private const val ETIQUETA = "NOTIFICACIONES_APP"
    private var db: AppDatabase? = null

    fun inicializar(database: AppDatabase) {
        db = database
        Log.i(ETIQUETA, "✅ Gestor de notificaciones inicializado")
    }

    // ==========================================
    // CREAR NOTIFICACIONES
    // ==========================================

    /**
     * Notificación de mensaje nuevo en chat.
     */
    fun notificarMensajeChat(
        remitente: String,
        mensaje: String,
        canal: String,
        referenciaId: String = ""
    ) {
        crearNotificacion(
            tipo = "CHAT",
            titulo = "Mensaje nuevo en #$canal",
            mensaje = "$remitente: $mensaje",
            referenciaId = referenciaId,
            canalOrigen = canal,
            icono = "chat"
        )
    }

    /**
     * Notificación de aviso nuevo en el Muro.
     */
    fun notificarAvisoMuro(titulo: String, contenido: String, referenciaId: String = "") {
        crearNotificacion(
            tipo = "MURO",
            titulo = "Nuevo aviso: $titulo",
            mensaje = contenido.take(120),
            referenciaId = referenciaId,
            icono = "campaign"
        )
    }

    /**
     * Notificación de alerta SOS activa.
     */
    fun notificarAlertaSOS(remitente: String, ubicacion: String) {
        crearNotificacion(
            tipo = "SOS",
            titulo = "Alerta de emergencia activa",
            mensaje = "$remitente reportó emergencia en $ubicacion",
            icono = "emergency"
        )
    }

    /**
     * Notificación para completar perfil del Carnet TX.
     */
    fun notificarPerfilIncompleto() {
        crearNotificacion(
            tipo = "PERFIL",
            titulo = "Completa tu Carnet TX",
            mensaje = "Ingresa a la sección de Perfil y completa tus datos para sincronizarlos en la nube.",
            icono = "badge"
        )
    }

    /**
     * Notificación para vincular cuenta Google.
     */
    fun notificarVincularGoogle() {
        crearNotificacion(
            tipo = "VINCULAR_GOOGLE",
            titulo = "Vincula tu cuenta Google",
            mensaje = "Vincula tu cuenta Google desde el Carnet TX para guardar tus datos en la nube y acceder desde cualquier dispositivo.",
            icono = "account_circle"
        )
    }

    /**
     * Notificación de invitación a grupo privado.
     */
    fun notificarInvitacionGrupoPrivado(nombreGrupo: String, creador: String, grupoId: String) {
        crearNotificacion(
            tipo = "CHAT",
            titulo = "Invitación a Grupo Privado",
            mensaje = "$creador te ha invitado a unirte a la sala \"$nombreGrupo\".",
            referenciaId = grupoId,
            canalOrigen = grupoId,
            icono = "groups"
        )
    }

    /**
     * Notificación a la Directiva sobre creación de grupo privado.
     */
    fun notificarGrupoPrivadoDirectiva(nombreGrupo: String, creador: String, grupoId: String) {
        crearNotificacion(
            tipo = "DIRECTIVA",
            titulo = "Nuevo Grupo Privado Creado",
            mensaje = "$creador creó la sala \"$nombreGrupo\". Supervisión disponible en el Panel de Directiva.",
            referenciaId = grupoId,
            canalOrigen = "DIRECTIVA",
            icono = "shield"
        )
    }

    /**
     * Notificación de recordatorio de evento del Calendario Motero.
     */
    fun notificarEventoCalendario(titulo: String, fecha: String, hora: String, lugar: String, horasAntes: Int = 24) {
        val tiempoTexto = if (horasAntes >= 24) "${horasAntes / 24} día(s) antes" else "$horasAntes hora(s) antes"
        crearNotificacion(
            tipo = "CALENDARIO",
            titulo = "📅 Recordatorio: $titulo",
            mensaje = "El evento se realizará el $fecha a las $hora ($tiempoTexto). Punto de salida: $lugar",
            icono = "event"
        )
    }

    /**
     * Notificación de mantenimiento preventivo o vencimiento de trámites.
     */
    fun notificarMantenimientoCalendario(titulo: String, fecha: String, detalle: String) {
        crearNotificacion(
            tipo = "MANTENIMIENTO",
            titulo = "🔧 Alerta de Garaje: $titulo",
            mensaje = "Programado para el $fecha. $detalle",
            icono = "build"
        )
    }

    /**
     * Notificación genérica.
     */
    fun notificarGeneral(titulo: String, mensaje: String) {
        crearNotificacion(
            tipo = "GENERAL",
            titulo = titulo,
            mensaje = mensaje,
            icono = "notifications"
        )
    }

    // ==========================================
    // CREAR Y ALMACENAR
    // ==========================================

    private fun crearNotificacion(
        tipo: String,
        titulo: String,
        mensaje: String,
        referenciaId: String = "",
        canalOrigen: String = "",
        icono: String = "notifications"
    ) {
        val dao = db?.notificacionDao()
        if (dao == null) {
            Log.w(ETIQUETA, "⚠️ Base de datos no inicializada")
            return
        }

        val notificacion = NotificacionApp(
            id = System.currentTimeMillis(),
            tipo = tipo,
            titulo = titulo,
            mensaje = mensaje,
            referenciaId = referenciaId,
            canalOrigen = canalOrigen,
            leida = false,
            timestamp = System.currentTimeMillis(),
            icono = icono
        )

        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(notificacion)
            Log.i(ETIQUETA, "📩 Notificación creada: [$tipo] $titulo")
        }
    }

    // ==========================================
    // CONSULTAR
    // ==========================================

    fun obtenerTodas(): Flow<List<NotificacionApp>>? {
        return db?.notificacionDao()?.getAll()
    }

    fun obtenerNoLeidas(): Flow<List<NotificacionApp>>? {
        return db?.notificacionDao()?.getNoLeidas()
    }

    fun obtenerCantidadNoLeidas(): Flow<Int>? {
        return db?.notificacionDao()?.getCantidadNoLeidas()
    }

    fun obtenerPorTipo(tipo: String): Flow<List<NotificacionApp>>? {
        return db?.notificacionDao()?.getByTipo(tipo)
    }

    // ==========================================
    // MARCAR LEÍDAS
    // ==========================================

    fun marcarLeida(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            db?.notificacionDao()?.marcarLeida(id)
        }
    }

    fun marcarTodasLeidas() {
        CoroutineScope(Dispatchers.IO).launch {
            db?.notificacionDao()?.marcarTodasLeidas()
            Log.i(ETIQUETA, "✅ Todas las notificaciones marcadas como leídas")
        }
    }

    // ==========================================
    // LIMPIEZA
    // ==========================================

    /**
     * Elimina notificaciones mayores a 30 días.
     */
    fun limpiarNotificacionesAntiguas() {
        CoroutineScope(Dispatchers.IO).launch {
            val treintaDiasMs = 30L * 24 * 60 * 60 * 1000
            val limite = System.currentTimeMillis() - treintaDiasMs
            db?.notificacionDao()?.eliminarAnteriores(limite)
            Log.i(ETIQUETA, "🧹 Notificaciones antiguas eliminadas")
        }
    }
}
