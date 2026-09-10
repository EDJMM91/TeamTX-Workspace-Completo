package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.SpotReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class SpotReportSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<SpotReport>(
    database = database,
    scope = scope,
    collectionName = "denuncias_puntos_tx",
    entityClass = SpotReport::class.java
) {
    override suspend fun dbInsert(item: SpotReport) { database.spotReportDao().insertSpotReport(item) }
    override suspend fun dbUpsertAll(items: List<SpotReport>) { database.spotReportDao().upsertSpotReports(items) }
    override suspend fun dbDelete(item: SpotReport) { database.spotReportDao().deleteSpotReportById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.spotReportDao().deleteSpotReportById(id) }
    override fun dbGetAll(): Flow<List<SpotReport>> = database.spotReportDao().getAllSpotReports()
    override fun getId(item: SpotReport): Long = item.id
    override fun setId(item: SpotReport, id: Long): SpotReport = item.copy(id = id)
    override fun getTimestamp(item: SpotReport): Long = item.timestamp
}
