package com.example.chat

/**
 * ARCHIVO: MODELO_MENSAJE.kt
 * 
 * Clase de datos simple e independiente que representa la estructura
 * de cada mensaje dentro del chat de la comunidad.
 * 
 * Contiene el texto enviado, el emisor, la marca de tiempo (hora)
 * y campos auxiliares para la identificación de los miembros.
 * Soporta tipos: texto, imagen, audio, sticker (WebP), ubicación.
 * Compatible con Room entity ChatMessage.
 */
enum class TipoMensaje(val etiqueta: String) {
    TEXTO("texto"),
    IMAGEN("imagen"),
    AUDIO("audio"),
    STICKER("sticker"),
    UBICACION("ubicacion")
}

data class Mensaje(
    val id: String = "",
    val texto: String = "",
    val emisor: String = "Piloto TX",
    val apodoEmisor: String = "",
    val hora: Long = System.currentTimeMillis(),
    val tipo: TipoMensaje = TipoMensaje.TEXTO,
    val urlMultimedia: String = "",
    val nombreArchivo: String = "",
    val channelId: String = "GENERAL",
    val senderMemberId: Long = 0,
    val senderMemberNumber: String = "",
    val senderRole: String = "MIEMBRO_ACTIVO",
    val senderCustomRoleTitle: String? = null,
    val senderInitials: String = "TX",
    val isRadioCallout: Boolean = false
) {
    val isSticker: Boolean
        get() = tipo == TipoMensaje.STICKER && (nombreArchivo.isNotBlank() || urlMultimedia.isNotBlank())
}
