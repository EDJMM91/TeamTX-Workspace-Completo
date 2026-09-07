package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.teamtxvzla.R
import com.example.data.local.AppDatabase
import com.example.data.model.NotificacionApp
import com.example.ui.preferences.PreferenciasApp
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
    private const val CANAL_SISTEMA_ID = "canal_team_tx_general"
    private const val CANAL_SISTEMA_NOMBRE = "Notificaciones Team TX"
    private var db: AppDatabase? = null
    private var appContext: Context? = null

    fun inicializar(database: AppDatabase, context: Context? = null) {
        db = database
        if (context != null) {
            appContext = context.applicationContext
        }
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
     * Notificación de actualización OTA disponible sincronizada con Avisos.
     */
    fun notificarActualizacionDisponible(versionName: String, titulo: String, novedades: String = "") {
        crearNotificacion(
            tipo = "ACTUALIZACION",
            titulo = "🚀 Nueva versión v$versionName disponible",
            mensaje = if (novedades.isNotBlank()) novedades.take(140) else "Nueva actualización oficial lista en el módulo Info: $titulo",
            referenciaId = "OTA_$versionName",
            icono = "update"
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
     * Notificación de nueva Rodada programada.
     */
    fun notificarNuevaRodada(titulo: String, fecha: String, lugar: String, rodadaId: String = "") {
        crearNotificacion(
            tipo = "RODADA",
            titulo = "🏍️ Nueva Rodada Programada: $titulo",
            mensaje = "Fecha: $fecha | Salida: $lugar. ¡Prepárate para la ruta con el equipo!",
            referenciaId = rodadaId,
            icono = "two_wheeler"
        )
    }

    /**
     * Notificación de nuevo Reto o Desafío.
     */
    fun notificarNuevoReto(titulo: String, descripcion: String, retoId: String = "") {
        crearNotificacion(
            tipo = "RETOS",
            titulo = "🏆 Nuevo Reto Biker: $titulo",
            mensaje = descripcion.take(140),
            referenciaId = retoId,
            icono = "emoji_events"
        )
    }

    /**
     * Notificación de nuevo producto o moto en Mercado Bikero.
     */
    fun notificarPublicacionMercado(titulo: String, precio: String, vendedor: String, productoId: String = "") {
        crearNotificacion(
            tipo = "MERCADO",
            titulo = "🛒 Mercado Biker: $titulo",
            mensaje = "Precio: $precio | Vendedor: $vendedor",
            referenciaId = productoId,
            icono = "storefront"
        )
    }

    /**
     * Notificación de Tesorería o Rifa Oficial del Club.
     */
    fun notificarTesoreriaRifa(titulo: String, monto: String, detalle: String = "") {
        crearNotificacion(
            tipo = "TESORERIA",
            titulo = "💰 Tesorería & Rifa Oficial: $titulo",
            mensaje = "Valor: $monto. ${detalle.take(100)}",
            icono = "account_balance_wallet"
        )
    }

    /**
     * Notificación de nuevo establecimiento en Directorio Comercial.
     */
    fun notificarNuevoComercio(nombre: String, categoria: String, comercioId: String = "") {
        crearNotificacion(
            tipo = "COMERCIO",
            titulo = "🛠️ Nuevo Aliado Comercial: $nombre",
            mensaje = "Categoría: $categoria. Revisa los beneficios y servicios para el club.",
            referenciaId = comercioId,
            icono = "storefront"
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
            mostrarEnBarraSistema(tipo, titulo, mensaje)
        }
    }

    private fun mostrarEnBarraSistema(tipo: String, titulo: String, mensaje: String) {
        val ctx = appContext ?: return
        if (!PreferenciasApp.notificacionesActivas) {
            Log.d(ETIQUETA, "🔕 Notificaciones en barra del sistema desactivadas en Ajustes")
            return
        }

        try {
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CANAL_SISTEMA_ID,
                    CANAL_SISTEMA_NOMBRE,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas, eventos y mensajes del Club Team TX"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("tipo_notificacion", tipo)
            }
            val pendingIntent = PendingIntent.getActivity(
                ctx,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(ctx, CANAL_SISTEMA_ID)
                .setSmallIcon(R.drawable.logoteam)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            Log.i(ETIQUETA, "🔔 Notificación mostrada en barra del sistema: $titulo")
        } catch (e: Exception) {
            Log.e(ETIQUETA, "❌ Error mostrando notificación en barra del sistema: ${e.message}")
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

    fun eliminarLeidas() {
        CoroutineScope(Dispatchers.IO).launch {
            db?.notificacionDao()?.deleteLeidas()
            Log.i(ETIQUETA, "🧹 Notificaciones leídas eliminadas")
        }
    }

    fun eliminarTodas() {
        CoroutineScope(Dispatchers.IO).launch {
            db?.notificacionDao()?.deleteAll()
            Log.i(ETIQUETA, "🧹 Todas las notificaciones eliminadas")
        }
    }

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
