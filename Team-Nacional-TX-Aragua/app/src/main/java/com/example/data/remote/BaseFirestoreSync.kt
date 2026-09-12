package com.example.data.remote

import android.util.Log
import com.aistudio.teamtxvzla.nube.AutenticacionNube
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

abstract class BaseFirestoreSync<T : Any>(
    protected val database: AppDatabase,
    protected val scope: CoroutineScope,
    protected val collectionName: String,
    protected val entityClass: Class<T>
) {
    protected val db = FirebaseFirestore.getInstance()
    protected val listeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
    private val pendingWrites = Channel<T>(Channel.BUFFERED)
    private val isInitialized = AtomicBoolean(false)

    // 🛡️ Blindaje: canales activos y reconexión cuando la sesión de Auth aparece/cambia
    protected val canalesActivos = mutableSetOf<String>()
    private var ultimoUidVisto: String? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        synchronized(this@BaseFirestoreSync) {
            if (uid != null && uid != ultimoUidVisto) {
                ultimoUidVisto = uid
                if (canalesActivos.isNotEmpty() && listeners.isEmpty()) {
                    // Sesión lista tras un arranque/logout: adjuntar listeners que faltan
                    Log.i("FIREBASE_SYNC", "🔄 Auth lista ($uid): adjuntando listeners de $collectionName")
                    canalesActivos.toList().forEach { setupChannelListener(it) }
                } else if (canalesActivos.isNotEmpty()) {
                    // Cambio de usuario: reconectar todo con la nueva identidad
                    Log.i("FIREBASE_SYNC", "🔄 Cambio de sesión ($uid): re-adjuntando listeners de $collectionName")
                    listeners.values.forEach { it.remove() }
                    listeners.clear()
                    canalesActivos.toList().forEach { setupChannelListener(it) }
                }
            } else if (uid == null) {
                ultimoUidVisto = null
            }
        }
    }

    init {
        try { FirebaseAuth.getInstance().addAuthStateListener(authStateListener) } catch (_: Exception) {}
        scope.launch(Dispatchers.IO) { setupChannelListener() }
        scope.launch(Dispatchers.IO) { processPendingWrites() }
    }

    protected abstract suspend fun dbInsert(item: T)
    protected abstract suspend fun dbUpsertAll(items: List<T>)
    protected abstract suspend fun dbDelete(item: T)
    open suspend fun dbDeleteById(id: Long) {}
    protected abstract fun dbGetAll(): Flow<List<T>>
    abstract fun getId(item: T): Long
    abstract fun setId(item: T, id: Long): T
    abstract fun getTimestamp(item: T): Long
    protected open fun enriquecerCamposImagenes(doc: com.google.firebase.firestore.DocumentSnapshot, item: T): T = item

    protected open fun setupChannelListener(channelId: String = collectionName) {
        canalesActivos.add(channelId)
        scope.launch(Dispatchers.IO) {
            // 🛡️ Garantiza sesión activa ANTES de adjuntar el listener (evita PERMISSION_DENIED permanente)
            if (FirebaseAuth.getInstance().currentUser == null) {
                AutenticacionNube.esperarAuthLista()
            }
            Log.d("FIREBASE_SYNC", "📡 Iniciando listener en tiempo real para: $collectionName")
            val registration = db.collection(collectionName)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FIREBASE_SYNC", "❌ Error en listener $collectionName: code=${error.code} ${error.message}")
                        return@addSnapshotListener
                    }
                
                if (snapshot == null) return@addSnapshotListener

                Log.i("FIREBASE_SYNC", "📥 Recibida actualización de $collectionName (${snapshot.size()} documentos)")
                
                scope.launch(Dispatchers.IO) {
                    // 1. 🗑️ Procesar eliminaciones remotas en tiempo real
                    for (change in snapshot.documentChanges) {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            val docId = change.document.id.toLongOrNull() ?: change.document.getLong("id")
                            if (docId != null) {
                                Log.i("FIREBASE_SYNC", "🗑️ Eliminando localmente item remoto de $collectionName: ID=$docId")
                                dbDeleteById(docId)
                            }
                        }
                    }

                    // 2. 📥 Procesar inserciones y actualizaciones
                    val toUpsert = mutableListOf<T>()
                    for (doc in snapshot.documents) {
                        try {
                            val item = doc.toObject(entityClass)
                            item?.let { unparsed ->
                                val itemEnriquecido = enriquecerCamposImagenes(doc, unparsed)
                                val itemWithId = setId(itemEnriquecido, doc.id.toLongOrNull() ?: getId(itemEnriquecido))
                                toUpsert.add(itemWithId)
                            }
                        } catch (e: Exception) {
                            Log.e("FIREBASE_SYNC", "❌ Error parseando documento ${doc.id} en $collectionName: ${e.message}")
                        }
                    }
                    
                    if (toUpsert.isNotEmpty()) {
                        Log.d("FIREBASE_SYNC", "💾 Guardando ${toUpsert.size} items de $collectionName en base de datos local")
                        dbUpsertAll(toUpsert)
                    }

                    // 3. 🛡️ Reconciliación de snapshot: si no hay escrituras pendientes y hay documentos remotos, limpiar elementos locales que no existen en Firestore
                    if (!snapshot.metadata.hasPendingWrites() && snapshot.documents.isNotEmpty()) {
                        try {
                            val remoteIds = snapshot.documents.mapNotNull { it.id.toLongOrNull() ?: it.getLong("id") }.toSet()
                            val localItems = dbGetAll().first()
                            for (local in localItems) {
                                val localId = getId(local)
                                if (localId > 0 && localId !in remoteIds) {
                                    Log.i("FIREBASE_SYNC", "🗑️ Reconciliación: eliminando elemento obsoleto en $collectionName: ID=$localId")
                                    dbDelete(local)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("FIREBASE_SYNC", "Advertencia en reconciliación de $collectionName: ${e.message}")
                        }
                    }
                }
            }
            listeners[channelId] = registration
        }
    }

    private fun processPendingWrites() = scope.launch(Dispatchers.IO) {
        for (item in pendingWrites) {
            writeToFirestore(item)
        }
    }

    protected suspend fun writeToFirestore(item: T) = withContext(Dispatchers.IO) {
        try {
            val localId = getId(item)
            // 🛡️ REGLA DE ORO: Si el ID es 0, no subimos. El ViewModel DEBE asignar System.currentTimeMillis()
            if (localId <= 0) {
                Log.w("FIREBASE_SYNC", "⚠️ Omitiendo subida a $collectionName: ID inválido ($localId)")
                return@withContext
            }

            val docRef = db.collection(collectionName).document(localId.toString())
            
            Log.d("FIREBASE_SYNC", "📤 Subiendo a $collectionName con ID FIJO: $localId")
            docRef.set(item).await()
            Log.i("FIREBASE_SYNC", "✅ Sincronización EXITOSA en $collectionName: $localId")
        } catch (e: Exception) {
            Log.e("FIREBASE_SYNC", "❌ ERROR CRÍTICO subiendo a $collectionName: ${e.message}", e)
        }
    }

    suspend fun insertOrUpdate(item: T) {
        dbInsert(item)
        scope.launch(Dispatchers.IO) {
            pendingWrites.send(item)
        }
    }

    suspend fun delete(item: T) {
        dbDelete(item)
        scope.launch(Dispatchers.IO) {
            try {
                val localId = getId(item)
                db.collection(collectionName).document(localId.toString()).delete().await()
                val querySnapshot = db.collection(collectionName).whereEqualTo("id", localId).get().await()
                for (doc in querySnapshot.documents) {
                    if (doc.id != localId.toString()) {
                        doc.reference.delete().await()
                    }
                }
            } catch (e: Exception) {
                Log.e("FIREBASE_SYNC", "Error eliminando en delete: ${e.message}")
            }
        }
    }

    open suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        try {
            dbDeleteById(id)
        } catch (e: Exception) {
            Log.w("FIREBASE_SYNC", "Aviso borrando localmente $collectionName ($id): ${e.message}")
        }
        try {
            Log.i("FIREBASE_SYNC", "🗑️ Eliminando de Firestore $collectionName: ID=$id")
            db.collection(collectionName).document(id.toString()).delete().await()
            val querySnapshot = db.collection(collectionName).whereEqualTo("id", id).get().await()
            for (doc in querySnapshot.documents) {
                if (doc.id != id.toString()) {
                    doc.reference.delete().await()
                }
            }
            Log.i("FIREBASE_SYNC", "✅ Eliminación exitosa en Firestore $collectionName: ID=$id")
        } catch (e: Exception) {
            Log.e("FIREBASE_SYNC", "❌ Error eliminando en Firestore $collectionName ($id): ${e.message}", e)
        }
    }

    open suspend fun refreshFromFirestore() = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(collectionName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .await()
            
            val remoteItems = snapshot.documents.mapNotNull { doc ->
                doc.toObject(entityClass)?.let { item ->
                    setId(item, doc.id.toLongOrNull() ?: getId(item))
                }
            }
            if (remoteItems.isNotEmpty()) {
                dbUpsertAll(remoteItems)
            }
            
            // Sincronización inteligente de eliminaciones (Safe Deletion)
            val remoteIds = remoteItems.map { getId(it) }.toSet()
            val localItems = dbGetAll().first()
            
            // Determinar el timestamp más antiguo en el snapshot de los últimos 500
            val oldestRemoteTimestamp = if (remoteItems.size >= 500) {
                remoteItems.minOfOrNull { getTimestamp(it) } ?: 0L
            } else {
                0L // Si hay menos de 500, el snapshot es "virtualmente" completo para el pasado
            }

            for (local in localItems) {
                val localId = getId(local)
                val localTimestamp = getTimestamp(local)
                
                if (localId !in remoteIds) {
                    // Si el item local es más reciente que el más antiguo del servidor
                    // pero no está en la lista, significa que fue eliminado.
                    if (localTimestamp >= oldestRemoteTimestamp) {
                        dbDelete(local)
                        Log.d("BaseFirestoreSync", "Eliminado localmente (no hallado en Firestore): $localId")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BaseFirestoreSync", "Error en refreshFromFirestore $collectionName: ${e.message}")
        }
    }

    fun getAll(): Flow<List<T>> = dbGetAll()

    protected interface BaseDao<T> {
        suspend fun insert(item: T)
        suspend fun upsertAll(items: List<T>)
        suspend fun delete(item: T)
        fun getAll(): Flow<List<T>>
    }

    open suspend fun shutdown() {
        try { FirebaseAuth.getInstance().removeAuthStateListener(authStateListener) } catch (_: Exception) {}
        listeners.values.forEach { it.remove() }
        listeners.clear()
        pendingWrites.close()
    }
}