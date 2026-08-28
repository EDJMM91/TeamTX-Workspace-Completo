package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.RideRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class RideRegistrationSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<RideRegistration>(
    database = database,
    scope = scope,
    collectionName = "ride_registrations",
    entityClass = RideRegistration::class.java
) {
    override suspend fun dbInsert(item: RideRegistration) { database.rideDao().insertRegistration(item) }
    override suspend fun dbUpsertAll(items: List<RideRegistration>) { database.rideDao().upsertRegistrations(items) }
    override suspend fun dbDelete(item: RideRegistration) { database.rideDao().deleteRegistration(item) }
    override fun dbGetAll(): Flow<List<RideRegistration>> = database.rideDao().getAllRegistrations()
    override fun getId(item: RideRegistration): Long = item.id
    override fun setId(item: RideRegistration, id: Long): RideRegistration = item.copy(id = id)
    override fun getTimestamp(item: RideRegistration): Long = item.registeredAt
}