package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.BikerChallenge
import com.example.data.model.UserChallengeProgress
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChallengeSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<BikerChallenge>(
    database = database,
    scope = scope,
    collectionName = "biker_challenges",
    entityClass = BikerChallenge::class.java
) {
    private var progressListener: ListenerRegistration? = null

    init {
        scope.launch(Dispatchers.IO) {
            setupProgressListener()
        }
    }

    override suspend fun dbInsert(item: BikerChallenge) { database.challengeDao().insertChallenge(item) }
    override suspend fun dbUpsertAll(items: List<BikerChallenge>) { database.challengeDao().upsertChallenges(items) }
    override suspend fun dbDelete(item: BikerChallenge) { database.challengeDao().deleteChallengeById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.challengeDao().deleteChallengeById(id) }
    override fun dbGetAll(): Flow<List<BikerChallenge>> = database.challengeDao().getAllChallenges()
    override fun getId(item: BikerChallenge): Long = item.id
    override fun setId(item: BikerChallenge, id: Long): BikerChallenge = item.copy(id = id)
    override fun getTimestamp(item: BikerChallenge): Long = item.timestamp

    fun getOfficialChallenges(): Flow<List<BikerChallenge>> = database.challengeDao().getOfficialChallenges()
    fun getProgressForMember(memberId: Long): Flow<List<UserChallengeProgress>> = database.challengeDao().getProgressForMember(memberId)
    fun getProgressForChallenge(challengeId: Long): Flow<List<UserChallengeProgress>> = database.challengeDao().getProgressForChallenge(challengeId)
    fun getAllProgress(): Flow<List<UserChallengeProgress>> = database.challengeDao().getAllProgress()

    suspend fun insertOrUpdateProgress(progress: UserChallengeProgress) = withContext(Dispatchers.IO) {
        try {
            val progressWithId = if (progress.id == 0L) progress.copy(id = System.currentTimeMillis()) else progress
            database.challengeDao().insertProgress(progressWithId)
            db.collection("user_challenge_progress").document(progressWithId.id.toString()).set(progressWithId).await()
            Log.i("ChallengeSync", "✅ Progreso de reto guardado en Firestore: ${progressWithId.challengeTitle}")
        } catch (e: Exception) {
            Log.e("ChallengeSync", "❌ Error guardando progreso: ${e.message}", e)
        }
    }

    suspend fun deleteProgress(id: Long) = withContext(Dispatchers.IO) {
        try {
            database.challengeDao().deleteProgressById(id)
            db.collection("user_challenge_progress").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.e("ChallengeSync", "❌ Error eliminando progreso: ${e.message}", e)
        }
    }

    private fun setupProgressListener() {
        try {
            progressListener?.remove()
            progressListener = db.collection("user_challenge_progress")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        val progressList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(UserChallengeProgress::class.java)?.copy(
                                id = doc.id.toLongOrNull() ?: doc.getLong("id") ?: 0L
                            )
                        }.filter { it.id > 0 }
                        if (progressList.isNotEmpty()) {
                            database.challengeDao().upsertProgressList(progressList)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("ChallengeSync", "Error en listener de progreso: ${e.message}")
        }
    }

    override suspend fun shutdown() {
        super.shutdown()
        progressListener?.remove()
    }
}
