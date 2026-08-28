package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.BikerCalendarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class CalendarSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<BikerCalendarEvent>(
    database = database,
    scope = scope,
    collectionName = "biker_calendar_events",
    entityClass = BikerCalendarEvent::class.java
) {
    override suspend fun dbInsert(item: BikerCalendarEvent) { database.calendarDao().insertEvent(item) }
    override suspend fun dbUpsertAll(items: List<BikerCalendarEvent>) { database.calendarDao().upsertEvents(items) }
    override suspend fun dbDelete(item: BikerCalendarEvent) { database.calendarDao().deleteEventById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.calendarDao().deleteEventById(id) }
    override fun dbGetAll(): Flow<List<BikerCalendarEvent>> = database.calendarDao().getAllEvents()
    override fun getId(item: BikerCalendarEvent): Long = item.id
    override fun setId(item: BikerCalendarEvent, id: Long): BikerCalendarEvent = item.copy(id = id)
    override fun getTimestamp(item: BikerCalendarEvent): Long = item.timestamp

    fun getOfficialEvents(): Flow<List<BikerCalendarEvent>> = database.calendarDao().getOfficialEvents()
    fun getEventsByDate(date: String): Flow<List<BikerCalendarEvent>> = database.calendarDao().getEventsByDate(date)
}
