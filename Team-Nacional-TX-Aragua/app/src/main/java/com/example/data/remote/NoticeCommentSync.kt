package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.NoticeComment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await

class NoticeCommentSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<NoticeComment>(
    database = database,
    scope = scope,
    collectionName = "notice_comments",
    entityClass = NoticeComment::class.java
) {
        override suspend fun dbInsert(item: NoticeComment) { database.noticeCommentDao().insertComment(item) }
    override suspend fun dbUpsertAll(items: List<NoticeComment>) { database.noticeCommentDao().upsertComments(items) }
    override suspend fun dbDelete(item: NoticeComment) { database.noticeCommentDao().deleteComment(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.noticeCommentDao().deleteComment(id) }
    override fun dbGetAll(): Flow<List<NoticeComment>> = database.noticeCommentDao().getAllComments()
    override fun getId(item: NoticeComment): Long = item.id
    override fun setId(item: NoticeComment, id: Long): NoticeComment = item.copy(id = id)
    override fun getTimestamp(item: NoticeComment): Long = item.timestamp

    suspend fun deleteCommentsForPublication(publicationId: Long) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            database.noticeCommentDao().deleteCommentsByPublicationId(publicationId)
            val snapshot = db.collection("notice_comments")
                .whereEqualTo("publicationId", publicationId)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                android.util.Log.i("NoticeCommentSync", "✅ Comentarios de aviso $publicationId eliminados de Firestore (${snapshot.size()} comentarios)")
            }
        } catch (e: Exception) {
            android.util.Log.e("NoticeCommentSync", "❌ Error eliminando comentarios de aviso $publicationId: ${e.message}", e)
        }
    }
}