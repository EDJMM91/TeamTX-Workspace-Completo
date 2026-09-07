package com.example.reproductor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListaReproduccionDao {
    @Query("SELECT * FROM listas_reproduccion ORDER BY fechaCreacion DESC")
    fun getAllListasFlow(): Flow<List<ListaReproduccionEntity>>

    @Query("SELECT * FROM listas_reproduccion ORDER BY fechaCreacion DESC")
    suspend fun getAllListas(): List<ListaReproduccionEntity>

    @Query("SELECT * FROM listas_reproduccion WHERE id = :id LIMIT 1")
    suspend fun getListaById(id: String): ListaReproduccionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lista: ListaReproduccionEntity)

    @Query("DELETE FROM listas_reproduccion WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM listas_reproduccion")
    suspend fun deleteAll()
}
