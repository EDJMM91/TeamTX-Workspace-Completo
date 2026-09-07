package com.example.reproductor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CancionLocalDao {
    @Query("SELECT * FROM canciones_locales ORDER BY titulo ASC")
    fun getAllCancionesFlow(): Flow<List<CancionLocalEntity>>

    @Query("SELECT * FROM canciones_locales ORDER BY titulo ASC")
    suspend fun getAllCanciones(): List<CancionLocalEntity>

    @Query("SELECT * FROM canciones_locales WHERE id = :id LIMIT 1")
    suspend fun getCancionById(id: Long): CancionLocalEntity?

    @Query("SELECT * FROM canciones_locales WHERE esFavorita = 1 ORDER BY titulo ASC")
    fun getFavoritasFlow(): Flow<List<CancionLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(canciones: List<CancionLocalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cancion: CancionLocalEntity)

    @Query("UPDATE canciones_locales SET esFavorita = :esFavorita WHERE id = :id")
    suspend fun updateFavorita(id: Long, esFavorita: Boolean)

    @Query("UPDATE canciones_locales SET titulo = :titulo, artista = :artista, album = :album WHERE id = :id")
    suspend fun updateMetadatos(id: Long, titulo: String, artista: String, album: String)

    @Query("DELETE FROM canciones_locales WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM canciones_locales")
    suspend fun deleteAll()
}
