package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.MarketplaceItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class MarketplaceSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<MarketplaceItem>(
    database = database,
    scope = scope,
    collectionName = "marketplace",
    entityClass = MarketplaceItem::class.java
) {
    override suspend fun dbInsert(item: MarketplaceItem) { database.marketplaceDao().insertItem(item) }
    override suspend fun dbUpsertAll(items: List<MarketplaceItem>) { database.marketplaceDao().upsertItems(items) }
    override suspend fun dbDelete(item: MarketplaceItem) { database.marketplaceDao().deleteItemById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.marketplaceDao().deleteItemById(id) }
    override fun dbGetAll(): Flow<List<MarketplaceItem>> = database.marketplaceDao().getAllItems()
    override fun getId(item: MarketplaceItem): Long = item.id
    override fun setId(item: MarketplaceItem, id: Long): MarketplaceItem = item.copy(id = id)
    override fun getTimestamp(item: MarketplaceItem): Long = item.timestamp
}
