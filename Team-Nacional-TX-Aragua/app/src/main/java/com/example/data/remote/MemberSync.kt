package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.MemberProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MemberSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<MemberProfile>(
    database = database,
    scope = scope,
    collectionName = "members",
    entityClass = MemberProfile::class.java
) {
    override suspend fun dbInsert(item: MemberProfile) { database.memberDao().insertMember(item) }
    override suspend fun dbUpsertAll(items: List<MemberProfile>) { database.memberDao().upsertMembers(items) }
    override suspend fun dbDelete(item: MemberProfile) { database.memberDao().deleteMember(item) }
    override suspend fun dbDeleteById(id: Long) { database.memberDao().deleteMemberById(id) }
    override fun dbGetAll(): Flow<List<MemberProfile>> = database.memberDao().getAllMembers()
    override fun getId(item: MemberProfile): Long = item.id
    override fun setId(item: MemberProfile, id: Long): MemberProfile = item.copy(id = id)
    override fun getTimestamp(item: MemberProfile): Long = item.lastActiveTimestamp
}
