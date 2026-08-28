package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.PassportDestination
import com.example.data.model.PassportStamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PassportSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<PassportDestination>(
    database = database,
    scope = scope,
    collectionName = "passport_destinations",
    entityClass = PassportDestination::class.java
) {
    private var stampsListener: ListenerRegistration? = null

    init {
        scope.launch(Dispatchers.IO) {
            setupStampsListener()
        }
    }

    override suspend fun dbInsert(item: PassportDestination) { database.passportDao().insertDestination(item) }
    override suspend fun dbUpsertAll(items: List<PassportDestination>) { database.passportDao().upsertDestinations(items) }
    override suspend fun dbDelete(item: PassportDestination) { database.passportDao().deleteDestinationById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.passportDao().deleteDestinationById(id) }
    override fun dbGetAll(): Flow<List<PassportDestination>> = database.passportDao().getAllDestinations()
    override fun getId(item: PassportDestination): Long = item.id
    override fun setId(item: PassportDestination, id: Long): PassportDestination = item.copy(id = id)
    override fun getTimestamp(item: PassportDestination): Long = item.timestamp

    fun getStampsForMember(memberId: Long): Flow<List<PassportStamp>> = database.passportDao().getStampsForMember(memberId)
    fun getAllStamps(): Flow<List<PassportStamp>> = database.passportDao().getAllStamps()

    suspend fun insertStamp(stamp: PassportStamp) = withContext(Dispatchers.IO) {
        try {
            val stampWithId = if (stamp.id == 0L) stamp.copy(id = System.currentTimeMillis()) else stamp
            database.passportDao().insertStamp(stampWithId)
            db.collection("passport_stamps").document(stampWithId.id.toString()).set(stampWithId).await()
            Log.i("PassportSync", "✅ Sello de pasaporte guardado en Firestore: ${stampWithId.destinationTitle}")
        } catch (e: Exception) {
            Log.e("PassportSync", "❌ Error guardando sello: ${e.message}", e)
        }
    }

    suspend fun deleteStamp(id: Long) = withContext(Dispatchers.IO) {
        try {
            database.passportDao().deleteStampById(id)
            db.collection("passport_stamps").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.e("PassportSync", "❌ Error eliminando sello: ${e.message}", e)
        }
    }

    private fun setupStampsListener() {
        try {
            stampsListener?.remove()
            stampsListener = db.collection("passport_stamps")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        val stamps = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(PassportStamp::class.java)?.copy(
                                id = doc.id.toLongOrNull() ?: doc.getLong("id") ?: 0L
                            )
                        }.filter { it.id > 0 }
                        if (stamps.isNotEmpty()) {
                            database.passportDao().upsertStamps(stamps)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("PassportSync", "Error en listener de sellos: ${e.message}")
        }
    }

    override suspend fun shutdown() {
        super.shutdown()
        stampsListener?.remove()
    }
}
