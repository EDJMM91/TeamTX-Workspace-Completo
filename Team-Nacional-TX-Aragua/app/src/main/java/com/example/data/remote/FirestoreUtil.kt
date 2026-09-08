package com.example.data.remote

import android.util.Log
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * UTILS DE FIRESTORE - Utilidades para consultas a Firebase Firestore.
 * Usado por los nuevos módulos de gobernanza y sesión.
 */
object FirestoreUtil {

    /**
     * Obtiene un Flow reactivo en tiempo real de miembros filtrados por estado ("PENDIENTE", "ACTIVO", "RECHAZADO").
     * Escucha en vivo las colecciones 'usuarios' y 'users'.
     */
    fun obtenerUsuariosConEstado(estado: String): Flow<List<MemberProfile>> = callbackFlow {
        val db = FirebaseFirestore.getInstance()
        val listenerUsuarios = db.collection("usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreUtil", "Error escuchando 'usuarios': ${error.message}")
                    return@addSnapshotListener
                }
                val lista = mutableListOf<MemberProfile>()
                if (snapshot != null) {
                    for (document in snapshot.documents) {
                        try {
                            val perfil = document.toObject(MemberProfile::class.java)
                            val docEstado = document.getString("usuarioEstado")
                                ?: document.getString("estado")
                                ?: "PENDIENTE"
                            val uid = document.id
                            if (perfil != null && docEstado.equals(estado, ignoreCase = true)) {
                                val conUid = if (perfil.firebaseUid.isNullOrBlank()) perfil.copy(firebaseUid = uid) else perfil
                                lista.add(conUid)
                            }
                        } catch (e: Exception) {
                            Log.w("FirestoreUtil", "Error parseando documento usuarios: ${e.message}")
                        }
                    }
                }
                trySend(lista)
            }

        awaitClose {
            listenerUsuarios.remove()
        }
    }

    /**
     * Obtiene el perfil de usuario específico por UID desde Firestore.
     */
    suspend fun obtenerPerfilPorUid(uid: String): MemberProfile? {
        return try {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                snapshot.toObject(MemberProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirestoreUtil", "Error obteniendo perfil por UID $uid: ${e.message}")
            null
        }
    }

    /**
     * Verifica si un usuario tiene permiso de Gobernanza (PRESIDENTE o DESARROLLADOR).
     */
    suspend fun verificarPermisoGobernanza(uid: String): Boolean {
        val perfil = obtenerPerfilPorUid(uid) ?: return false
        return perfil.role == MemberRole.PRESIDENTE || perfil.role == MemberRole.DESARROLLADOR
    }

    /**
     * Marca la actividad de un usuario en Firestore actualizando timestamp.
     */
    suspend fun marcarActividad(uid: String) {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).update(
                mapOf("lastActiveTimestamp" to System.currentTimeMillis())
            ).await()
        } catch (e: Exception) {
            Log.e("FirestoreUtil", "Error marcando actividad de $uid: ${e.message}")
        }
    }

    /**
     * Obtiene el UID actual desde Firebase Auth de manera segura.
     */
    fun obtenerUidActual(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
}
