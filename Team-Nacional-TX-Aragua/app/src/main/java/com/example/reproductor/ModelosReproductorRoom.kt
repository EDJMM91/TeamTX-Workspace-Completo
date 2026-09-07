package com.example.reproductor

import androidx.room.Entity
import androidx.room.PrimaryKey

// ═══════════════════════════════════════════════════════════════════════════
// ENTIDADES ROOM - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Persistencia indestructible de biblioteca, listas, favoritas y estado.
// ═══════════════════════════════════════════════════════════════════════════

@Entity(tableName = "canciones_locales")
data class CancionLocalEntity(
    @PrimaryKey val id: Long,
    val titulo: String,
    val artista: String,
    val album: String,
    val duracionMs: Long,
    val rutaArchivo: String,
    val uriStr: String,
    val portadaUriStr: String?,
    val fechaAgregada: Long,
    val esFavorita: Boolean,
    val tamanoBytes: Long,
    val carpetaContenedora: String,
    val metadatosPersonalizadosJson: String? = null
) {
    fun toCancionMotera(): CancionMotera {
        return CancionMotera(
            id = id,
            titulo = titulo,
            artista = artista,
            album = album,
            duracionMs = duracionMs,
            rutaArchivo = rutaArchivo,
            uriStr = uriStr,
            portadaUriStr = portadaUriStr,
            fechaAgregada = fechaAgregada,
            esFavorita = esFavorita,
            tamanoBytes = tamanoBytes,
            carpetaContenedora = carpetaContenedora
        )
    }

    companion object {
        fun fromCancionMotera(cancion: CancionMotera): CancionLocalEntity {
            return CancionLocalEntity(
                id = cancion.id,
                titulo = cancion.titulo,
                artista = cancion.artista,
                album = cancion.album,
                duracionMs = cancion.duracionMs,
                rutaArchivo = cancion.rutaArchivo,
                uriStr = cancion.uriStr,
                portadaUriStr = cancion.portadaUriStr,
                fechaAgregada = cancion.fechaAgregada,
                esFavorita = cancion.esFavorita,
                tamanoBytes = cancion.tamanoBytes,
                carpetaContenedora = cancion.carpetaContenedora
            )
        }
    }
}

@Entity(tableName = "listas_reproduccion")
data class ListaReproduccionEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val fechaCreacion: Long,
    val icono: String,
    val cancionIdsJson: String
)

@Entity(tableName = "estado_reproductor")
data class EstadoReproductorEntity(
    @PrimaryKey val clave: String = "global",
    val ultimaCancionId: Long?,
    val colaIdsJson: String,
    val indiceColaActual: Int,
    val modoBucle: Int,
    val modoAleatorio: Boolean,
    val posicionMs: Long,
    val timestamp: Long
)
