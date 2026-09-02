package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.WorkshopDirectoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WorkshopDirectorySync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<WorkshopDirectoryItem>(
    database = database,
    scope = scope,
    collectionName = "workshops_directory",
    entityClass = WorkshopDirectoryItem::class.java
) {
    override suspend fun dbInsert(item: WorkshopDirectoryItem) { database.workshopDirectoryDao().insertWorkshop(item) }
    override suspend fun dbUpsertAll(items: List<WorkshopDirectoryItem>) { database.workshopDirectoryDao().upsertWorkshops(items) }
    override suspend fun dbDelete(item: WorkshopDirectoryItem) { database.workshopDirectoryDao().deleteWorkshopById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.workshopDirectoryDao().deleteWorkshopById(id) }
    override fun dbGetAll(): Flow<List<WorkshopDirectoryItem>> = database.workshopDirectoryDao().getAllWorkshops()
    override fun getId(item: WorkshopDirectoryItem): Long = item.id
    override fun setId(item: WorkshopDirectoryItem, id: Long): WorkshopDirectoryItem = item.copy(id = id)
    override fun getTimestamp(item: WorkshopDirectoryItem): Long = item.timestamp

    init {
        scope.launch(Dispatchers.IO) {
            verificarYSembrarDirectorio()
        }
    }

    private suspend fun verificarYSembrarDirectorio() {
        try {
            // 1. Asegurar primero que las 22 tiendas de Aragua estén en Room localmente
            database.workshopDirectoryDao().upsertWorkshops(AppDatabase.INITIAL_WORKSHOPS)

            // 2. Consultar Firestore para sembrar en la nube cualquier tienda inicial que falte
            val snapshot = db.collection(collectionName).get().await()
            val existingIds = snapshot.documents.mapNotNull { it.id.toLongOrNull() ?: it.getLong("id") }.toSet()
            for (item in AppDatabase.INITIAL_WORKSHOPS) {
                if (item.id !in existingIds) {
                    insertOrUpdate(item)
                }
            }
            val remotos = snapshot.documents.mapNotNull { it.toObject(WorkshopDirectoryItem::class.java) }
            if (remotos.isNotEmpty()) {
                database.workshopDirectoryDao().upsertWorkshops(remotos)
            }
        } catch (e: Exception) {
            Log.w("FIREBASE_SYNC", "Aviso en sync de directorio: ${e.message}")
            try {
                database.workshopDirectoryDao().upsertWorkshops(AppDatabase.INITIAL_WORKSHOPS)
            } catch (_: Exception) {}
        }
    }
}

