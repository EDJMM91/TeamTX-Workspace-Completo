package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.MaintenanceLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class MaintenanceSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<MaintenanceLog>(
    database = database,
    scope = scope,
    collectionName = "maintenance_logs",
    entityClass = MaintenanceLog::class.java
) {
    override suspend fun dbInsert(item: MaintenanceLog) { database.maintenanceDao().insertLog(item) }
    override suspend fun dbUpsertAll(items: List<MaintenanceLog>) { database.maintenanceDao().upsertLogs(items) }
    override suspend fun dbDelete(item: MaintenanceLog) { database.maintenanceDao().deleteLogById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.maintenanceDao().deleteLogById(id) }
    override fun dbGetAll(): Flow<List<MaintenanceLog>> = database.maintenanceDao().getAllLogs()
    override fun getId(item: MaintenanceLog): Long = item.id
    override fun setId(item: MaintenanceLog, id: Long): MaintenanceLog = item.copy(id = id)
    override fun getTimestamp(item: MaintenanceLog): Long = item.timestamp
}
