package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.SEGURIDAD_CUENTAS
import com.example.data.model.MemberProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Sincronización del perfil del usuario autenticado, anclado al UID de Firebase Auth.
 *
 * Ruta en Firestore: usuarios/{uid}
 * - Al iniciar sesión: si el documento existe se descarga y alimenta la UI;
 *   si no existe, la app crea uno nuevo con los datos locales por defecto.
 * - Escucha en tiempo real: cualquier cambio en la nube llega a todos los dispositivos.
 * - Anti-trampa / Sesión Única: verifica 'id_dispositivo_activo' en vivo.
 */
data class VinculacionGoogle(
    var email: String = "",
    var firebaseUid: String = "",
    var memberNumber: String = "",
    var memberName: String = "",
    var memberId: Long = 0L,
    var accessCode: String = "",
    var activeDeviceId: String = "",
    var linkedAt: Long = 0L,
    var lastActiveTimestamp: Long = 0L
)

object PerfilNube {

    private const val ETIQUETA_LOG = "PERFIL_NUBE"
    private const val COLECCION_USUARIOS = "usuarios"
    private const val COLECCION_VINCULOS = "vinculos_google"

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** Sanitiza el correo para usarlo con seguridad como Document ID en Firestore */
    fun sanitizarEmailDocId(correo: String): String {
        return correo.trim().lowercase().replace(".", "_").replace("@", "_at_")
    }

    /** Consulta si un correo ya está vinculado a algún perfil en Firestore */
    suspend fun consultarVinculacionPorEmail(correo: String): VinculacionGoogle? {
        return try {
            val idDocumento = sanitizarEmailDocId(correo)
            val documento = db.collection(COLECCION_VINCULOS).document(idDocumento).get().await()
            if (documento.exists()) {
                documento.toObject(VinculacionGoogle::class.java)
            } else {
                null
            }
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error consultando vinculación de $correo: ${error.message}", error)
            null
        }
    }

    /** Registra o actualiza la vinculación 1-a-1 de un correo con un miembro */
    suspend fun registrarVinculacion(vinculacion: VinculacionGoogle): Boolean {
        return try {
            val idDocumento = sanitizarEmailDocId(vinculacion.email)
            db.collection(COLECCION_VINCULOS).document(idDocumento).set(vinculacion).await()
            Log.i(ETIQUETA_LOG, "✅ Vinculación 1-a-1 registrada en Firestore: ${vinculacion.email} -> ${vinculacion.memberNumber}")
            true
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error registrando vinculación para ${vinculacion.email}: ${error.message}", error)
            false
        }
    }

    /** Libera la vinculación de un correo en Firestore para que pueda ser utilizado en otro perfil */
    suspend fun liberarVinculacion(correo: String, uid: String): Boolean {
        return try {
            val idDocumento = sanitizarEmailDocId(correo)
            val correoLimpio = correo.trim().lowercase()
            db.collection(COLECCION_VINCULOS).document(idDocumento).delete().await()
            if (idDocumento != correoLimpio) {
                try {
                    db.collection(COLECCION_VINCULOS).document(correoLimpio).delete().await()
                } catch (_: Exception) {}
            }
            try {
                if (uid.isNotBlank()) {
                    db.collection(COLECCION_USUARIOS).document(uid).delete().await()
                }
            } catch (_: Exception) {}
            Log.i(ETIQUETA_LOG, "✅ Vinculación liberada totalmente para $correo (UID: $uid)")
            true
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error liberando vinculación para $correo: ${error.message}", error)
            false
        }
    }

    /** Actualiza el dispositivo activo en Firestore para control anti-trampas */
    suspend fun actualizarDispositivoActivo(correo: String, uid: String, idDispositivo: String): Boolean {
        return try {
            val idDocumento = sanitizarEmailDocId(correo)
            val correoLimpio = correo.trim().lowercase()
            val mapaActualizacion = mapOf(
                "id_dispositivo_activo" to idDispositivo,
                "activeDeviceId" to idDispositivo,
                "lastActiveTimestamp" to System.currentTimeMillis()
            )
            db.collection(COLECCION_VINCULOS).document(idDocumento).update(mapaActualizacion)
            if (idDocumento != correoLimpio) {
                try {
                    db.collection(COLECCION_VINCULOS).document(correoLimpio).update(mapaActualizacion)
                } catch (_: Exception) {}
            }
            if (uid.isNotBlank()) {
                db.collection(COLECCION_USUARIOS).document(uid).update(mapaActualizacion)
            }
            true
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error actualizando dispositivo activo para $correo: ${error.message}", error)
            false
        }
    }

    /** Descarga el perfil remoto por uid. Devuelve null si no existe o si falla */
    suspend fun descargarPerfil(uid: String): MemberProfile? {
        return try {
            val documento = db.collection(COLECCION_USUARIOS).document(uid).get().await()
            if (documento.exists()) {
                documento.toObject(MemberProfile::class.java)?.copy(id = 0L)
            } else {
                Log.i(ETIQUETA_LOG, "Sin perfil previo en nube para uid=${uid.take(6)}… (primera vez)")
                null
            }
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error descargando perfil usuarios/$uid: ${error.message}", error)
            null
        }
    }

    /** Sube (crea o actualiza) el documento usuarios/{uid} con el perfil completo */
    suspend fun subirPerfil(uid: String, perfil: MemberProfile, contexto: Context? = null): Boolean {
        return try {
            db.collection(COLECCION_USUARIOS).document(uid).set(mapaDelPerfil(perfil, contexto)).await()
            true
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error subiendo perfil usuarios/$uid: ${error.message}", error)
            false
        }
    }

    /** Crea el documento inicial solo si no existe (no sobrescribe datos ya guardados) */
    suspend fun crearSiNoExiste(uid: String, perfil: MemberProfile, contexto: Context? = null): Boolean {
        return try {
            val referencia = db.collection(COLECCION_USUARIOS).document(uid)
            val existe = referencia.get().await().exists()
            if (!existe) {
                referencia.set(mapaDelPerfil(perfil, contexto)).await()
                Log.i(ETIQUETA_LOG, "Perfil creado en nube para uid=${uid.take(6)}…")
                true
            } else {
                false
            }
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "Error creando perfil inicial usuarios/$uid: ${error.message}", error)
            false
        }
    }

    /**
     * Escucha en tiempo real los cambios del perfil en la nube ('usuarios/{uid}')
     * con validación Anti-Trampa (Sesión Única):
     * 1. Extrae 'id_dispositivo_activo' (o 'activeDeviceId') de Firestore.
     * 2. Llama a SEGURIDAD_CUENTAS.obtenerIdDispositivo(contexto) para el ID local.
     * 3. Si el ID remoto existe y es diferente al local, se detiene la sincronización local y se emite evento a la UI.
     * 4. El usuario en la UI decide si reclamar la sesión aquí o cerrar la sesión.
     */
    fun escucharPerfil(
        contexto: Context,
        uid: String,
        alDetectarDispositivoDistinto: (mensaje: String) -> Unit = {},
        alSerBloqueado: () -> Unit = {},
        alCambiar: (perfil: MemberProfile, idDispositivoActivo: String?) -> Unit
    ): ListenerRegistration? {
        return try {
            var listenerRegistration: ListenerRegistration? = null
            listenerRegistration = db.collection(COLECCION_USUARIOS).document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(ETIQUETA_LOG, "Escucha en vivo usuarios/$uid falló: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot == null || !snapshot.exists()) {
                        Log.w(ETIQUETA_LOG, "🚨 Documento de perfil usuarios/$uid no existe (posible borrado)")
                        alSerBloqueado()
                        return@addSnapshotListener
                    }

                    val perfil = snapshot.toObject(MemberProfile::class.java)
                    if (perfil == null) {
                        alSerBloqueado()
                        return@addSnapshotListener
                    }

                    // Verificar suspensión en tiempo real
                    if (perfil.isSuspended) {
                        Log.w(ETIQUETA_LOG, "🚨 Piloto suspendido detectado en vivo")
                        alSerBloqueado()
                        return@addSnapshotListener
                    }

                    // Las sesiones anónimas (invitados/testers) no aplican control de desplazamiento
                    val esAnonimo = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                    if (esAnonimo) {
                        alCambiar(perfil.copy(id = 0L), null)
                        return@addSnapshotListener
                    }

                    // 1. Extraer campo 'id_dispositivo_activo' (o 'activeDeviceId') desde Firestore
                    val idDispositivoRemoto = snapshot.getString("id_dispositivo_activo")
                        ?: snapshot.getString("activeDeviceId")

                    // 2. Obtener el ID único local del dispositivo
                    val idDispositivoLocal = SEGURIDAD_CUENTAS.obtenerIdDispositivo(contexto)

                    // 3. Comparar si el remoto existe y difiere del local (sesión iniciada en otro celular)
                    if (!idDispositivoRemoto.isNullOrBlank() && idDispositivoRemoto != idDispositivoLocal) {
                        Log.w(ETIQUETA_LOG, "🚨 Sesión iniciada en otro celular detectada (Remoto: $idDispositivoRemoto vs Local: $idDispositivoLocal)")

                        // 4. Detener escucha activa para evitar bucles
                        listenerRegistration?.remove()
                        listenerRegistration = null

                        // 5. Notificar a la UI (muestra diálogo para Reclamar o Cerrar Sesión)
                        val mensajeAviso = "Sesión pausada. Se detectó actividad en otro celular."
                        alDetectarDispositivoDistinto(mensajeAviso)
                        return@addSnapshotListener
                    }

                    // Mapeo continuo si es el mismo dispositivo
                    alCambiar(perfil.copy(id = 0L), idDispositivoRemoto)
                }
            listenerRegistration
        } catch (error: Exception) {
            Log.e(ETIQUETA_LOG, "No se pudo registrar escucha de perfil para uid=$uid: ${error.message}", error)
            null
        }
    }

    /** Serialización explícita de todos los campos persistidos incluyendo el id del dispositivo activo */
    private fun mapaDelPerfil(p: MemberProfile, contexto: Context? = null): Map<String, Any?> {
        val idDispositivo = if (contexto != null) {
            SEGURIDAD_CUENTAS.obtenerIdDispositivo(contexto)
        } else {
            com.example.ui.preferences.PreferenciasApp.obtenerDeviceId()
        }

        return mapOf(
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
            "email" to p.email,
            "id_dispositivo_activo" to idDispositivo,
            "activeDeviceId" to idDispositivo
        )
    }
}
