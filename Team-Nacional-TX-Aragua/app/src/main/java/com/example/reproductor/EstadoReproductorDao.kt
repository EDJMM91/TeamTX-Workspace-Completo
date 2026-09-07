package com.example.reproductor

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EstadoReproductorDao {
    @Query("SELECT * FROM estado_reproductor WHERE clave = :clave LIMIT 1")
    fun getEstadoFlow(clave: String = "global"): Flow<EstadoReproductorEntity?>

    @Query("SELECT * FROM estado_reproductor WHERE clave = :clave LIMIT 1")
    suspend fun getEstado(clave: String = "global"): EstadoReproductorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(estado: EstadoReproductorEntity)

    @Query("DELETE FROM estado_reproductor WHERE clave = :clave")
    suspend fun delete(clave: String = "global")
}
