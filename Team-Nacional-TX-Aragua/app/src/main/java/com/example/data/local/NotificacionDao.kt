package com.example.data.local

import androidx.room.*
import com.example.data.model.NotificacionApp
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NotificacionApp>>

    @Query("SELECT * FROM app_notifications WHERE leida = 0 ORDER BY timestamp DESC")
    fun getNoLeidas(): Flow<List<NotificacionApp>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE leida = 0")
    fun getCantidadNoLeidas(): Flow<Int>

    @Query("SELECT * FROM app_notifications WHERE tipo = :tipo ORDER BY timestamp DESC")
    fun getByTipo(tipo: String): Flow<List<NotificacionApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notificacion: NotificacionApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notificaciones: List<NotificacionApp>)

    @Update
    suspend fun update(notificacion: NotificacionApp)

    @Query("UPDATE app_notifications SET leida = 1 WHERE id = :id")
    suspend fun marcarLeida(id: Long)

    @Query("UPDATE app_notifications SET leida = 1")
    suspend fun marcarTodasLeidas()

    @Delete
    suspend fun delete(notificacion: NotificacionApp)

    @Query("DELETE FROM app_notifications WHERE timestamp < :timestampLimite")
    suspend fun eliminarAnteriores(timestampLimite: Long)
}
