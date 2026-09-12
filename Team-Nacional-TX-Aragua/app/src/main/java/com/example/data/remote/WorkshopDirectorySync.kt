package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.WorkshopDirectoryItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WorkshopDirectorySync(
    database: AppDatabase,
    scope: CoroutineScope
) : BaseFirestoreSync<WorkshopDirectoryItem>(
    database = database,
    scope = scope,
    collectionName = "workshops_directory",
    entityClass = WorkshopDirectoryItem::class.java
) {
    override suspend fun dbInsert(item: WorkshopDirectoryItem) { database.workshopDirectoryDao().insertWorkshop(item) }
    override suspend fun dbUpsertAll(items: List<WorkshopDirectoryItem>) { database.workshopDirectoryDao().upsertWorkshops(items) }
    override suspend fun dbDelete(item: WorkshopDirectoryItem) { database.workshopDirectoryDao().deleteWorkshopById(item.id) }
    override suspend fun dbDeleteById(id: Long) { database.workshopDirectoryDao().deleteWorkshopById(id) }
    override fun dbGetAll(): Flow<List<WorkshopDirectoryItem>> = database.workshopDirectoryDao().getAllWorkshops()
    override fun getId(item: WorkshopDirectoryItem): Long = item.id
    override fun setId(item: WorkshopDirectoryItem, id: Long): WorkshopDirectoryItem = item.copy(id = id)
    override fun getTimestamp(item: WorkshopDirectoryItem): Long = item.timestamp

    init {
        scope.launch(Dispatchers.IO) {
            verificarYSembrarDirectorio()
        }
    }

    private suspend fun verificarYSembrarDirectorio() {
        try {
            val prefs = com.example.TeamTxApplication.instance?.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
            val yaSembrado = prefs?.getBoolean("workshops_sembrados_v5", false) ?: false

            if (!yaSembrado) {
                // 1. Siembra o actualización en Room localmente
                database.workshopDirectoryDao().upsertWorkshops(AppDatabase.INITIAL_WORKSHOPS)

                // 2. Sembrar en Firestore si no existen
                val snapshot = db.collection(collectionName).get().await()
                val existingIds = snapshot.documents.mapNotNull { it.id.toLongOrNull() ?: it.getLong("id") }.toSet()
                for (item in AppDatabase.INITIAL_WORKSHOPS) {
                    if (item.id !in existingIds) {
                        insertOrUpdate(item)
                    }
                }
                prefs?.let { p ->
                    val editor = p.edit()
                    editor.putBoolean("workshops_sembrados_v5", true)
                    editor.apply()
                }
                Log.d("FIREBASE_SYNC", "🌱 Siembra de directorio v5 (concesionarios Aragua/Valencia/Caracas) completada con ${AppDatabase.INITIAL_WORKSHOPS.size} registros.")
            } else {
                // Si ya fue sembrado, descargar actualizaciones remotas sin forzar re-inserción de borrados
                val snapshot = db.collection(collectionName).get().await()
                val remotos = snapshot.documents.mapNotNull { it.toObject(WorkshopDirectoryItem::class.java) }
                if (remotos.isNotEmpty()) {
                    database.workshopDirectoryDao().upsertWorkshops(remotos)
                }
            }
        } catch (e: Exception) {
            Log.w("FIREBASE_SYNC", "Aviso en sync de directorio: ${e.message}")
        }
    }

    override fun setupChannelListener(channelId: String) {
        canalesActivos.add(channelId)
        scope.launch(Dispatchers.IO) {
            if (FirebaseAuth.getInstance().currentUser == null) {
                com.aistudio.teamtxvzla.nube.AutenticacionNube.esperarAuthLista()
            }
            Log.d("FIREBASE_SYNC", "📡 Iniciando listener robusto y persistente para: $collectionName")
            val registration = db.collection(collectionName)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FIREBASE_SYNC", "❌ Error en listener $collectionName: code=${error.code} ${error.message}")
                        return@addSnapshotListener
                    }
                
                if (snapshot == null) return@addSnapshotListener

                Log.i("FIREBASE_SYNC", "📥 Recibida actualización robusta de $collectionName (${snapshot.size()} documentos)")
                
                scope.launch(Dispatchers.IO) {
                    // 1. Procesar únicamente eliminaciones explícitas del administrador (REMOVED)
                    for (change in snapshot.documentChanges) {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            val docId = change.document.id.toLongOrNull() ?: change.document.getLong("id")
                            if (docId != null) {
                                Log.i("FIREBASE_SYNC", "🗑️ Administrador eliminó de Firestore $collectionName: ID=$docId. Borrando localmente.")
                                dbDeleteById(docId)
                            }
                        }
                    }

                    // 2. Guardar y persistir todos los documentos remotos recibidos
                    val toUpsert = mutableListOf<WorkshopDirectoryItem>()
                    for (doc in snapshot.documents) {
                        try {
                            val item = doc.toObject(WorkshopDirectoryItem::class.java)
                            item?.let {
                                val itemWithId = it.copy(id = doc.id.toLongOrNull() ?: it.id)
                                toUpsert.add(itemWithId)
                            }
                        } catch (e: Exception) {
                            Log.e("FIREBASE_SYNC", "❌ Error parseando documento ${doc.id} en $collectionName: ${e.message}")
                        }
                    }
                    
                    if (toUpsert.isNotEmpty()) {
                        database.workshopDirectoryDao().upsertWorkshops(toUpsert)
                    }
                }
            }
            listeners[channelId] = registration
        }
    }
}
