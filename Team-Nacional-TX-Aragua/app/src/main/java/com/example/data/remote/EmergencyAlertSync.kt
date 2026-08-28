package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.EmergencyAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class EmergencyAlertSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<EmergencyAlert>(
    database = database,
    scope = scope,
    collectionName = "emergencies",
    entityClass = EmergencyAlert::class.java
) {
    override fun getId(item: EmergencyAlert): Long = item.id
    override fun setId(item: EmergencyAlert, id: Long): EmergencyAlert = item.copy(id = id)
    override fun getTimestamp(item: EmergencyAlert): Long = item.timestamp
    
    override suspend fun dbInsert(item: EmergencyAlert) { database.emergencyDao().insertAlert(item) }
    override suspend fun dbUpsertAll(items: List<EmergencyAlert>) = database.emergencyDao().upsertAlerts(items)
    override suspend fun dbDelete(item: EmergencyAlert) = database.emergencyDao().deleteAlert(item.id)
    override fun dbGetAll(): Flow<List<EmergencyAlert>> = database.emergencyDao().getAllAlerts()
}