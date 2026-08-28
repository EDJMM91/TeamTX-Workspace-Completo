package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.InventoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class InventoryItemSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<InventoryItem>(
    database = database,
    scope = scope,
    collectionName = "inventory",
    entityClass = InventoryItem::class.java
) {
        override suspend fun dbInsert(item: InventoryItem) { database.inventoryDao().insertItem(item) }
    override suspend fun dbUpsertAll(items: List<InventoryItem>) { database.inventoryDao().upsertItems(items) }
    override suspend fun dbDelete(item: InventoryItem) { }
    override fun dbGetAll(): Flow<List<InventoryItem>> = database.inventoryDao().getAllItems()
    override fun getId(item: InventoryItem): Long = item.id
    override fun setId(item: InventoryItem, id: Long): InventoryItem = item.copy(id = id)
    override fun getTimestamp(item: InventoryItem): Long = item.lastMaintenanceTimestamp
}