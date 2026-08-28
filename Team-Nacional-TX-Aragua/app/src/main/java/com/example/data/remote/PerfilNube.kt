package com.example.data.remote

import android.util.Log
import com.example.data.model.MemberProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Sincronizacion del perfil del usuario autenticado, anclado al UID de Firebase Auth.
 *
 * Ruta en Firestore: usuarios/{uid}
 * - Al iniciar sesion: si el documento existe se descarga y alimenta la UI;
 *   si no existe, la app crea uno nuevo con los datos locales por defecto.
 * - Escucha en tiempo real: cualquier cambio en la nube llega a todos los dispositivos.
 */
object PerfilNube {

    private const val TAG = "PERFIL_NUBE"
    private const val COLECCION = "usuarios"

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** Descarga el perfil remoto por uid. Devuelve null si no existe o si falla (error detallado en Logcat). */
    suspend fun descargarPerfil(uid: String): MemberProfile? {
        return try {
            val doc = db.collection(COLECCION).document(uid).get().await()
            if (doc.exists()) {
                doc.toObject(MemberProfile::class.java)?.copy(id = 0L)
            } else {
                Log.i(TAG, "Sin perfil previo en nube para uid=${uid.take(6)}… (primera vez)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando perfil usuarios/$uid", e)
            null
        }
    }

    /** Sube (crea o actualiza) el documento usuarios/{uid} con el perfil completo. */
    suspend fun subirPerfil(uid: String, perfil: MemberProfile): Boolean {
        return try {
            db.collection(COLECCION).document(uid).set(mapaDelPerfil(perfil)).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo perfil usuarios/$uid", e)
            false
        }
    }

    /** Crea el documento inicial solo si no existe (no sobrescribe datos ya guardados). */
    suspend fun crearSiNoExiste(uid: String, perfil: MemberProfile): Boolean {
        return try {
            val ref = db.collection(COLECCION).document(uid)
            val existe = ref.get().await().exists()
            if (!existe) {
                ref.set(mapaDelPerfil(perfil)).await()
                Log.i(TAG, "Perfil creado en nube para uid=${uid.take(6)}…")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando perfil inicial usuarios/$uid", e)
            false
        }
    }

    /** Escucha en tiempo real los cambios del perfil en la nube. */
    fun escucharPerfil(uid: String, alCambiar: (MemberProfile) -> Unit): ListenerRegistration? {
        return try {
            db.collection(COLECCION).document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Escucha en vivo usuarios/$uid falló", error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    // Comentado: snapshot.metadata.hasPendingWrites() // Quitado para permitir sync en tiempo real entre dispositivos
                    snapshot.toObject(MemberProfile::class.java)?.let { alCambiar(it.copy(id = 0L)) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo registrar escucha de perfil para uid=$uid", e)
            null
        }
    }

    /** Serializacion explicita de todos los campos persistidos (sin el id autogenerado de Room ni campos @Ignore). */
    private fun mapaDelPerfil(p: MemberProfile): Map<String, Any?> = mapOf(
        "fullName" to p.fullName,
        "nickname" to p.nickname,
        "memberNumber" to p.memberNumber,
        "cedulaDni" to p.cedulaDni,
        "phone" to p.phone,
        "role" to p.role.name,
        "chapterState" to p.chapterState,
        "birthDate" to p.birthDate,
        "bikeBrand" to p.bikeBrand,
        "bikeModel" to p.bikeModel,
        "bikeColor" to p.bikeColor,
        "bikeDisplacementCc" to p.bikeDisplacementCc,
        "bikeTankCapacityLiters" to p.bikeTankCapacityLiters,
        "bikeYear" to p.bikeYear,
        "bikePlate" to p.bikePlate,
        "bloodType" to p.bloodType,
        "medicalNotes" to p.medicalNotes,
        "emergencyContactName" to p.emergencyContactName,
        "emergencyContactPhone" to p.emergencyContactPhone,
        "emergencyContactRelation" to p.emergencyContactRelation,
        "isDirectiva" to p.isDirectiva,
        "solvencyStatus" to p.solvencyStatus,
        "joinYear" to p.joinYear,
        "avatarInitials" to p.avatarInitials,
        "isSuspended" to p.isSuspended,
        "suspensionReason" to p.suspensionReason,
        "suspensionDurationDays" to p.suspensionDurationDays,
        "suspendedBy" to p.suspendedBy,
        "suspensionStartTimestamp" to p.suspensionStartTimestamp,
        "suspensionEndTimestamp" to p.suspensionEndTimestamp,
        "isOnline" to p.isOnline,
        "lastActiveTimestamp" to System.currentTimeMillis(),
        "licenseImageUri" to p.licenseImageUri,
        "medicalCertImageUri" to p.medicalCertImageUri,
        "bikeRegImageUri" to p.bikeRegImageUri,
        "insuranceImageUri" to p.insuranceImageUri,
        "copilotName" to p.copilotName,
        "copilotRelation" to p.copilotRelation,
        "attendanceCount" to p.attendanceCount,
        "longRidesCount" to p.longRidesCount,
        "bigEventsCount" to p.bigEventsCount,
        "prospectStartDate" to p.prospectStartDate,
        "profilePhotoUri" to p.profilePhotoUri,
        "bikePhotoUri" to p.bikePhotoUri,
        "firebaseUid" to p.firebaseUid,
        "email" to p.email
    )
}
