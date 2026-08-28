package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.WorkshopDirectoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

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
}
