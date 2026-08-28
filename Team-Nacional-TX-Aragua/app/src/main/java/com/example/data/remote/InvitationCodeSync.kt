package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.InvitationCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope

class InvitationCodeSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<InvitationCode>(
    database = database,
    scope = scope,
    collectionName = "invitations",
    entityClass = InvitationCode::class.java
) {
        override suspend fun dbInsert(item: InvitationCode) { database.invitationCodeDao().insertInvitationCode(item) }
    override suspend fun dbUpsertAll(items: List<InvitationCode>) { database.invitationCodeDao().upsertInvitationCodes(items) }
    override suspend fun dbDelete(item: InvitationCode) { database.invitationCodeDao().deleteInvitationCodeById(item.id) }
    override fun dbGetAll(): Flow<List<InvitationCode>> = database.invitationCodeDao().getAllInvitationCodes()
    override fun getId(item: InvitationCode): Long = item.id
    override fun setId(item: InvitationCode, id: Long): InvitationCode = item.copy(id = id)
    override fun getTimestamp(item: InvitationCode): Long = item.createdAt
}