package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Notificación interna de la app (no es push de FCM).
 * Se genera localmente cuando hay eventos nuevos: chat, muro, SOS, perfil, vinculación Google.
 */
@Entity(tableName = "app_notifications")
data class NotificacionApp(
    @PrimaryKey var id: Long = System.currentTimeMillis(),
    var tipo: String = "GENERAL",       // CHAT, MURO, SOS, PERFIL, VINCULAR_GOOGLE, GENERAL
    var titulo: String = "",
    var mensaje: String = "",
    var referenciaId: String = "",      // ID del documento relacionado (mensaje, aviso, etc.)
    var canalOrigen: String = "",       // GENERAL, RODADAS, DIRECTIVA, etc. (para chat)
    var leida: Boolean = false,
    var timestamp: Long = System.currentTimeMillis(),
    var icono: String = "notifications" // nombre del ícono Material
)
