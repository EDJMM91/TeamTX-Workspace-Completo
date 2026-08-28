package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.RideEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class RideSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<RideEvent>(
    database = database,
    scope = scope,
    collectionName = "rides",
    entityClass = RideEvent::class.java
) {
        override suspend fun dbInsert(item: RideEvent) { database.rideDao().insertRide(item) }
    override suspend fun dbUpsertAll(items: List<RideEvent>) { database.rideDao().upsertRides(items) }
    override suspend fun dbDelete(item: RideEvent) { database.rideDao().updateRide(item.copy(status = com.example.data.model.RideStatus.CANCELADA)) }
    override fun dbGetAll(): Flow<List<RideEvent>> = database.rideDao().getAllRides()
    override fun getId(item: RideEvent): Long = item.id
    override fun setId(item: RideEvent, id: Long): RideEvent = item.copy(id = id)
    override fun getTimestamp(item: RideEvent): Long = item.departureTimestamp
}