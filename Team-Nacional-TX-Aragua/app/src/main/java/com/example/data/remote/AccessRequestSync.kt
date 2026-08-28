package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.AccessRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class AccessRequestSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<AccessRequest>(
    database = database,
    scope = scope,
    collectionName = "access_requests",
    entityClass = AccessRequest::class.java
) {
    override fun getId(item: AccessRequest): Long = item.id
    override fun setId(item: AccessRequest, id: Long): AccessRequest = item.copy(id = id)
    override fun getTimestamp(item: AccessRequest): Long = item.timestamp
    
    override suspend fun dbInsert(item: AccessRequest) { database.accessRequestDao().insertAccessRequest(item) }
    override suspend fun dbUpsertAll(items: List<AccessRequest>) = database.accessRequestDao().upsertAccessRequests(items)
    override suspend fun dbDelete(item: AccessRequest) = database.accessRequestDao().deleteAccessRequest(item.id)
    override fun dbGetAll(): Flow<List<AccessRequest>> = database.accessRequestDao().getAllAccessRequests()
}