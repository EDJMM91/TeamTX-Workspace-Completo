package com.example.mapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * ARCHIVO: PUENTE_MAPA.kt
 * 
 * Puente de comunicación aislado.
 * Toma una coordenada del chat y despierta el motor nativo de OsmAnd / Mapa TX
 * centrado directamente en dicha coordenada.
 */
object PuenteMapa {

    /**
     * Dispara la coordenada hacia el Mapa TX interno.
     */
    fun mostrarUbicacionEnMapa(contexto: Context, coordenadas: String, tituloEtiqueta: String = "Ubicación Compartida") {
        try {
            // Firebase guarda "latitud,longitud". Las separamos limpiamente.
            val partes = coordenadas.split(",")
            if (partes.size >= 2) {
                val latitud = partes[0].trim()
                val longitud = partes[1].trim()

                // Creamos un estándar geo: que el motor OsmAnd lee de forma nativa
                val uriEstandar = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud($tituloEtiqueta)")
                val intentoAbrirMapa = Intent(Intent.ACTION_VIEW, uriEstandar)
                
                // TRUCO ARQUITECTÓNICO: Obligamos a que el sistema busque resolver el intent 
                // estrictamente dentro de nuestra propia aplicación, esquivando Google Maps.
                intentoAbrirMapa.setPackage(contexto.packageName)
                if (contexto !is android.app.Activity) {
                    intentoAbrirMapa.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                contexto.startActivity(intentoAbrirMapa)
            } else {
                Toast.makeText(contexto, "Coordenadas no válidas", Toast.LENGTH_SHORT).show()
            }
        } catch (error: Exception) {
            // Manejo de errores por si hay fallos en la conversión o el motor está suspendido
            Toast.makeText(contexto, "No se pudo abrir el Mapa TX", Toast.LENGTH_SHORT).show()
        }
    }
}
