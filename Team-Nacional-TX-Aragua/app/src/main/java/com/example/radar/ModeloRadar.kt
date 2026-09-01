package com.example.radar

data class PilotoRadar(
    val id: String,
    val nombre: String,
    val rango: String,
    val lat: Double,
    val lon: Double,
    val avatarUrl: String,
    val timestamp: Long,
    val activo: Boolean
)

enum class EstadoRadar {
    DESACTIVO,
    ACTIVO,
    BUSCANDO
}
