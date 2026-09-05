package com.example.mapa

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ARCHIVO: GESTOR_SELECCION_MAPA.kt
 *
 * Coordinador de selección asistida de ubicación para eventos, avisos y rodadas.
 * Cuando el usuario está creando o editando una publicación o evento y pulsa "Seleccionar en Mapa",
 * este gestor activa el modo de captura. Al detectar que se copió una coordenada válida en el mapa,
 * notifica al usuario, cierra el mapa automáticamente y autopega la coordenada en el formulario.
 */
object GestorSeleccionMapa {

    /** Indica si actualmente hay un formulario esperando coordenadas del mapa */
    var modoBuscandoCoordenada by mutableStateOf(false)

    /** Identificador del formulario que solicitó la coordenada: "FEED_CREATE", "FEED_EDIT", "CALENDAR", "RIDES" */
    var origenSolicitud by mutableStateOf<String?>(null)

    /** Almacena la última coordenada capturada */
    var coordenadaSeleccionada by mutableStateOf<String?>(null)

    /**
     * Inicia el modo de captura y abre el Mapa TX centrado en una coordenada base.
     */
    fun iniciarSeleccion(
        contexto: Context,
        origen: String,
        coordenadasIniciales: String = "10.228,-67.475",
        titulo: String = "Seleccionar Ubicación"
    ) {
        modoBuscandoCoordenada = true
        origenSolicitud = origen
        coordenadaSeleccionada = null
        PuenteMapa.mostrarUbicacionEnMapa(contexto, coordenadasIniciales, titulo)
    }

    /**
     * Se invoca cuando se detecta una coordenada copiada en el portapapeles mientras el mapa está abierto.
     * Guarda la coordenada, desactiva la búsqueda y ejecuta el cierre del mapa.
     */
    fun registrarCoordenadaDetectada(
        contexto: Context,
        coordenadas: String,
        onCerrarMapa: (() -> Unit)? = null
    ) {
        if (modoBuscandoCoordenada) {
            coordenadaSeleccionada = coordenadas
            modoBuscandoCoordenada = false
            Toast.makeText(
                contexto,
                "📍 ¡Coordenada capturada! ($coordenadas)\nRegresando a la edición...",
                Toast.LENGTH_SHORT
            ).show()
            onCerrarMapa?.invoke()
        }
    }

    /**
     * Consume la coordenada seleccionada (la devuelve y limpia el estado).
     */
    fun consumirCoordenada(origenEsperado: String? = null): String? {
        if (origenEsperado != null && origenSolicitud != origenEsperado) {
            return null
        }
        val coords = coordenadaSeleccionada
        if (coords != null) {
            coordenadaSeleccionada = null
            modoBuscandoCoordenada = false
            origenSolicitud = null
        }
        return coords
    }

    /**
     * Cancela la búsqueda de coordenada activa.
     */
    fun cancelarSeleccion() {
        modoBuscandoCoordenada = false
        origenSolicitud = null
        coordenadaSeleccionada = null
    }
}
