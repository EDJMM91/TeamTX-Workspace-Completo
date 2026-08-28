package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.Publication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import android.util.Log

class PublicationSync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<Publication>(
    database = database,
    scope = scope,
    collectionName = "feed",
    entityClass = Publication::class.java
) {
    init {
        scope.launch(Dispatchers.IO) {
            com.aistudio.teamtxvzla.nube.AutenticacionNube.esperarAuthLista()
            cleanGhostAndEmptyPublications()
        }
    }

    private fun isValidPublication(item: Publication): Boolean {
        val t = item.title.trim()
        val c = item.content.trim()
        val isDefaultTitleOnly = t.equals("Aviso Oficial", ignoreCase = true) && c.isEmpty()
        val isDefaultContentOnly = c.equals("Aviso Oficial", ignoreCase = true) && t.isEmpty()
        val isCompletelyEmpty = t.isEmpty() && c.isEmpty()
        return !isDefaultTitleOnly && !isDefaultContentOnly && !isCompletelyEmpty && item.id != 0L
    }

    override suspend fun dbInsert(item: Publication) {
        if (isValidPublication(item)) {
            database.publicationDao().insertPublication(item)
        }
    }

    override suspend fun dbUpsertAll(items: List<Publication>) {
        val validItems = items.filter { isValidPublication(it) }
        database.publicationDao().deleteEmptyPublications()
        if (validItems.isNotEmpty()) {
            database.publicationDao().upsertPublications(validItems)
        }
    }

    override suspend fun dbDelete(item: Publication) {
        database.publicationDao().deletePublicationById(item.id)
        deleteById(item.id)
    }

    override suspend fun dbDeleteById(id: Long) {
        database.publicationDao().deletePublicationById(id)
        database.publicationDao().deleteEmptyPublications()
    }

    override fun dbGetAll(): Flow<List<Publication>> = database.publicationDao().getAllPublications()
    override fun getId(item: Publication): Long = item.id
    override fun setId(item: Publication, id: Long): Publication = item.copy(id = id)
    override fun getTimestamp(item: Publication): Long = item.timestamp

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        try {
            database.publicationDao().deletePublicationById(id)
            database.publicationDao().deleteEmptyPublications()
            
            // 1. Eliminar por docId exacto numérico
            if (id > 0) {
                db.collection("feed").document(id.toString()).delete().await()
                val q = db.collection("feed").whereEqualTo("id", id).get().await()
                for (doc in q.documents) {
                    doc.reference.delete().await()
                }
            }

            // 2. Purgar cualquier documento fantasma / vacío / admin residual en Firestore
            cleanGhostAndEmptyPublications()
            Log.i("PublicationSync", "✅ Aviso $id eliminado completamente de Firestore y Room")
        } catch (e: Exception) {
            Log.e("PublicationSync", "❌ Error eliminando aviso $id de Firestore: ${e.message}", e)
        }
    }

    override suspend fun refreshFromFirestore() = withContext(Dispatchers.IO) {
        cleanGhostAndEmptyPublications()
        super.refreshFromFirestore()
        database.publicationDao().deleteEmptyPublications()
    }

    suspend fun cleanGhostAndEmptyPublications() = withContext(Dispatchers.IO) {
        try {
            database.publicationDao().deleteEmptyPublications()
            val allSnapshot = db.collection("feed").get().await()
            for (doc in allSnapshot.documents) {
                val title = doc.getString("title")?.trim() ?: ""
                val content = doc.getString("content")?.trim() ?: ""
                val docId = doc.id
                val docNumId = docId.toLongOrNull() ?: doc.getLong("id") ?: 0L

                val isGhost = (title.isEmpty() && content.isEmpty()) ||
                        (title.equals("Aviso Oficial", ignoreCase = true) && content.isEmpty()) ||
                        (content.equals("Aviso Oficial", ignoreCase = true) && title.isEmpty()) ||
                        docId.equals("admin", ignoreCase = true) ||
                        docId.equals("null", ignoreCase = true) ||
                        docId.equals("0") ||
                        docNumId == 0L

                if (isGhost) {
                    doc.reference.delete().await()
                    Log.i("PublicationSync", "🧹 Documento fantasma / vacío purgado en Firestore ($docId)")
                }
            }
        } catch (e: Exception) {
            Log.w("PublicationSync", "No se pudo limpiar documentos fantasma: ${e.message}")
        }
    }
}
