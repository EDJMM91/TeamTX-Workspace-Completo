package com.example.data.remote

import android.util.Log
import com.aistudio.teamtxvzla.nube.AutenticacionNube
import com.example.data.local.AppDatabase
import com.example.data.model.PrivateGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sincronización de Grupos Privados con Firebase Firestore.
 * TAG de logcat: GRUPO_PRIVADO
 */
class FirebasePrivateGroupSync(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "GRUPO_PRIVADO"
    private var listenerRegistration: ListenerRegistration? = null
    private val isInitialized = AtomicBoolean(false)

    init {
        scope.launch { inicializar() }
    }

    private suspend fun inicializar() {
        if (isInitialized.getAndSet(true)) return

        Log.i(TAG, "⏳ Comprobando Auth para sincronizar Grupos Privados...")
        if (FirebaseAuth.getInstance().currentUser == null) {
            AutenticacionNube.esperarAuthLista()
        }

        Log.i(TAG, "✅ Auth lista. Iniciando listener de Grupos Privados...")
        setupGroupsListener()
    }

    private fun setupGroupsListener() {
        if (listenerRegistration != null) return

        scope.launch(Dispatchers.IO) {
            try {
                listenerRegistration = db.collection("private_groups")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "❌ Error en listener de private_groups: ${error.message}")
                            return@addSnapshotListener
                        }

                        if (snapshot == null) return@addSnapshotListener

                        Log.d(TAG, "📥 Snapshot private_groups recibido: ${snapshot.size()} documentos")

                        scope.launch(Dispatchers.IO) {
                            // 1. Manejar eliminaciones
                            for (change in snapshot.documentChanges) {
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    val groupId = change.document.id
                                    database.privateGroupDao().deleteGroupById(groupId)
                                    Log.i(TAG, "🗑️ Grupo privado eliminado localmente: $groupId")
                                }
                            }

                            // 2. Parsear y actualizar grupos
                            val groups = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(PrivateGroup::class.java)?.let { group ->
                                        group.copy(id = doc.id)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error parseando grupo ${doc.id}: ${e.message}")
                                    null
                                }
                            }

                            if (groups.isNotEmpty()) {
                                database.privateGroupDao().upsertGroups(groups)
                                Log.i(TAG, "✅ ${groups.size} grupos privados sincronizados en Room")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al configurar listener de private_groups: ${e.message}", e)
            }
        }
    }

    suspend fun saveGroup(group: PrivateGroup) = withContext(Dispatchers.IO) {
        try {
            // Guardar local primero (offline-first)
            database.privateGroupDao().insertGroup(group)

            if (group.id.isNotBlank()) {
                db.collection("private_groups")
                    .document(group.id)
                    .set(group, SetOptions.merge())
                    .await()
                Log.i(TAG, "✅ Grupo privado ${group.id} subido a Firestore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando grupo privado en Firestore: ${e.message}", e)
        }
    }

    suspend fun deleteGroup(groupId: String) = withContext(Dispatchers.IO) {
        try {
            database.privateGroupDao().deleteGroupById(groupId)
            db.collection("private_groups")
                .document(groupId)
                .delete()
                .await()
            Log.i(TAG, "🗑️ Grupo privado $groupId eliminado de Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando grupo privado de Firestore: ${e.message}", e)
        }
    }

    suspend fun toggleBlockGroup(
        groupId: String,
        isBlocked: Boolean,
        reason: String,
        blockedBy: String
    ) = withContext(Dispatchers.IO) {
        try {
            val existing = database.privateGroupDao().getGroupById(groupId)
            if (existing != null) {
                val updated = existing.copy(
                    isBlockedByDirectiva = isBlocked,
                    blockedReason = if (isBlocked) reason else "",
                    blockedBy = if (isBlocked) blockedBy else ""
                )
                database.privateGroupDao().updateGroup(updated)
            }

            db.collection("private_groups")
                .document(groupId)
                .update(
                    mapOf(
                        "isBlockedByDirectiva" to isBlocked,
                        "blockedReason" to if (isBlocked) reason else "",
                        "blockedBy" to if (isBlocked) blockedBy else ""
                    )
                )
                .await()
            Log.i(TAG, "🔒 Estado de bloqueo actualizado para grupo $groupId: isBlocked=$isBlocked")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando bloqueo de grupo: ${e.message}", e)
        }
    }

    suspend fun updateLastMessage(groupId: String, text: String, timestamp: Long) = withContext(Dispatchers.IO) {
        try {
            val existing = database.privateGroupDao().getGroupById(groupId)
            if (existing != null) {
                database.privateGroupDao().updateGroup(
                    existing.copy(
                        lastMessageText = text,
                        lastMessageTimestamp = timestamp
                    )
                )
            }
            db.collection("private_groups")
                .document(groupId)
                .update(
                    mapOf(
                        "lastMessageText" to text,
                        "lastMessageTimestamp" to timestamp
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ No se pudo actualizar último mensaje en grupo $groupId: ${e.message}")
        }
    }

    fun shutdown() {
        listenerRegistration?.remove()
        listenerRegistration = null
        isInitialized.set(false)
    }
}
