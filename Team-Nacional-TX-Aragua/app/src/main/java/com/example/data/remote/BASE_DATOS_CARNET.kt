package com.example.data.remote

import android.util.Log
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * ═══════════════════════════════════════════════════════════════
 * MÓDULO BASE_DATOS_CARNET
 * ═══════════════════════════════════════════════════════════════
 *
 * Base de datos centralizada para el Carnet TX en Firestore.
 * Colección: carnet_tx_usuarios
 * Documento: UID de Firebase Auth (evita duplicados)
 *
 * FLUJO:
 * 1. Al vincular Google → guardarDatosCarnet() crea/actualiza el documento
 * 2. Al abrir Carnet TX → obtenerDatosCarnet() inyecta datos en la UI
 * 3. Escucha en tiempo real para sincronización entre dispositivos
 *
 * TODOS los campos visuales del carnet están mapeados aquí.
 * ═══════════════════════════════════════════════════════════════
 */

/**
 * Data class en español con TODA la información del Carnet TX.
 * Mapeo exacto de los campos visuales en la UI del carnet.
 */
data class DatosCarnet(
    // ─── DATOS PERSONALES ──────────────────────────────────────
    val nombreCompleto: String = "",
    val alias: String = "",
    val numeroMiembro: String = "",
    val cedulaDni: String = "",
    val telefono: String = "",
    val correo: String = "",
    val fechaNacimiento: String = "",
    val estadoCapitulo: String = "",
    val anioIngreso: String = "",
    val inicialesAvatar: String = "TX",
    val firebaseUid: String = "",

    // ─── ROL Y MEMBRESÍA ──────────────────────────────────────
    val rol: String = "ASPIRANTE",
    val esDirectiva: Boolean = false,
    val estadoSolvencia: Boolean = true,
    val estaSuspendido: Boolean = false,
    val razonSuspension: String = "",
    val suspendidoPor: String = "",
    val diasSuspension: Int = 0,
    val inicioSuspensionTimestamp: Long = 0,
    val finSuspensionTimestamp: Long = 0,

    // ─── FICHA TÉCNICA DE LA MOTO ──────────────────────────────
    val marcaMoto: String = "Keeway",
    val modeloMoto: String = "TX 200 SM",
    val colorMoto: String = "Negro / Naranja",
    val cilindradaCc: String = "200 cc",
    val capacidadTanqueLitros: String = "11.5 L",
    val anioMoto: String = "2023",
    val placaMoto: String = "",
    val fotoMotoUri: String? = null,

    // ─── FICHA MÉDICA Y SOS ────────────────────────────────────
    val tipoSangre: String = "O+",
    val notasMedicas: String = "Sin alergias reportadas",
    val nombreContactoEmergencia: String = "",
    val telefonoContactoEmergencia: String = "",
    val parentescoContactoEmergencia: String = "Familiar",

    // ─── GUANTERA DIGITAL (DOCUMENTOS) ─────────────────────────
    val uriLicencia: String? = null,
    val uriCertificadoMedico: String? = null,
    val uriRegistroMoto: String? = null,
    val uriSeguroVehicular: String? = null,

    // ─── COPILOTO OFICIAL ──────────────────────────────────────
    val nombreCopiloto: String? = null,
    val parentescoCopiloto: String? = null,

    // ─── ESTADÍSTICAS ──────────────────────────────────────────
    val asistencias: Int = 0,
    val rodadasLargas: Int = 0,
    val eventosGrandes: Int = 0,
    val inicioProspectoTimestamp: Long? = null,

    // ─── FOTOS DE PERFIL ───────────────────────────────────────
    val fotoPerfilUri: String? = null,

    // ─── ESTADO EN VIVO ────────────────────────────────────────
    val estaEnLinea: Boolean = true,
    val ultimoActivoTimestamp: Long = System.currentTimeMillis()
)

/**
 * Resultado de la operación de guardar/obtener datos del carnet.
 */
sealed class ResultadoCarnet {
    data class Exito(val datos: DatosCarnet) : ResultadoCarnet()
    data class Error(val mensaje: String) : ResultadoCarnet()
    object SinDatos : ResultadoCarnet()
}

/**
 * ═══════════════════════════════════════════════════════════════
 * OBJETO PRINCIPAL: BASE_DATOS_CARNET
 * ═══════════════════════════════════════════════════════════════
 */
object BaseDatosCarnet {

    private const val TAG = "BASE_DATOS_CARNET"
    private const val COLECCION = "carnet_tx_usuarios"

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // ═══════════════════════════════════════════════
    // GUARDAR DATOS DEL CARNET
    // ═══════════════════════════════════════════════

    /**
     * Guarda o actualiza los datos del carnet en Firestore.
     * Usa el UID como nombre del documento para evitar duplicados.
     *
     * @param uid UID de Firebase Auth del usuario
     * @param datos DatosCarnet con toda la información del carnet
     * @return true si se guardó correctamente, false si falló
     */
    suspend fun guardarDatosCarnet(uid: String, datos: DatosCarnet): Boolean {
        return try {
            db.collection(COLECCION).document(uid).set(serializar(datos)).await()
            Log.i(TAG, "✅ Datos del carnet guardados para uid=${uid.take(8)}…")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando carnet para uid=${uid.take(8)}…", e)
            false
        }
    }

    // ═══════════════════════════════════════════════
    // OBTENER DATOS DEL CARNET
    // ═══════════════════════════════════════════════

    /**
     * Descarga los datos del carnet desde Firestore.
     *
     * @param uid UID de Firebase Auth del usuario
     * @return ResultadoCarnet con los datos o error
     */
    suspend fun obtenerDatosCarnet(uid: String): ResultadoCarnet {
        return try {
            val doc = db.collection(COLECCION).document(uid).get().await()
            if (doc.exists()) {
                val datos = doc.toObject(DatosCarnet::class.java)
                if (datos != null) {
                    Log.i(TAG, "📥 Datos del carnet descargados para uid=${uid.take(8)}…")
                    ResultadoCarnet.Exito(datos)
                } else {
                    Log.w(TAG, "⚠️ Documento existe pero no se pudo deserializar")
                    ResultadoCarnet.SinDatos
                }
            } else {
                Log.i(TAG, "📭 Sin datos previos del carnet para uid=${uid.take(8)}…")
                ResultadoCarnet.SinDatos
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando carnet para uid=${uid.take(8)}…", e)
            ResultadoCarnet.Error("Error de conexión: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    // CONVERTIR MemberProfile → DatosCarnet
    // ═══════════════════════════════════════════════

    /**
     * Convierte un MemberProfile (Room) a DatosCarnet (Firestore).
     * Útil para sincronizar desde la UI hacia la nube.
     */
    fun desdeMemberProfile(profile: MemberProfile): DatosCarnet {
        return DatosCarnet(
            nombreCompleto = profile.fullName,
            alias = profile.nickname,
            numeroMiembro = profile.memberNumber,
            cedulaDni = profile.cedulaDni,
            telefono = profile.phone,
            correo = profile.email ?: "",
            fechaNacimiento = profile.birthDate,
            estadoCapitulo = profile.chapterState,
            anioIngreso = profile.joinYear,
            inicialesAvatar = profile.avatarInitials,
            firebaseUid = profile.firebaseUid ?: "",
            rol = profile.role.name,
            esDirectiva = profile.isDirectiva,
            estadoSolvencia = profile.solvencyStatus,
            estaSuspendido = profile.isSuspended,
            razonSuspension = profile.suspensionReason,
            suspendidoPor = profile.suspendedBy,
            diasSuspension = profile.suspensionDurationDays,
            inicioSuspensionTimestamp = profile.suspensionStartTimestamp,
            finSuspensionTimestamp = profile.suspensionEndTimestamp,
            marcaMoto = profile.bikeBrand,
            modeloMoto = profile.bikeModel,
            colorMoto = profile.bikeColor,
            cilindradaCc = profile.bikeDisplacementCc,
            capacidadTanqueLitros = profile.bikeTankCapacityLiters,
            anioMoto = profile.bikeYear,
            placaMoto = profile.bikePlate,
            fotoMotoUri = profile.bikePhotoUri,
            tipoSangre = profile.bloodType,
            notasMedicas = profile.medicalNotes,
            nombreContactoEmergencia = profile.emergencyContactName,
            telefonoContactoEmergencia = profile.emergencyContactPhone,
            parentescoContactoEmergencia = profile.emergencyContactRelation,
            uriLicencia = profile.licenseImageUri,
            uriCertificadoMedico = profile.medicalCertImageUri,
            uriRegistroMoto = profile.bikeRegImageUri,
            uriSeguroVehicular = profile.insuranceImageUri,
            nombreCopiloto = profile.copilotName,
            parentescoCopiloto = profile.copilotRelation,
            asistencias = profile.attendanceCount,
            rodadasLargas = profile.longRidesCount,
            eventosGrandes = profile.bigEventsCount,
            inicioProspectoTimestamp = profile.prospectStartDate,
            fotoPerfilUri = profile.profilePhotoUri,
            estaEnLinea = profile.isOnline,
            ultimoActivoTimestamp = profile.lastActiveTimestamp
        )
    }

    // ═══════════════════════════════════════════════
    // CONVERTIR DatosCarnet → MemberProfile
    // ═══════════════════════════════════════════════

    /**
     * Convierte DatosCarnet (Firestore) a MemberProfile (Room).
     * Útil para sincronizar desde la nube hacia la UI local.
     */
    fun aMemberProfile(datos: DatosCarnet, idLocal: Long = 0L): MemberProfile {
        return MemberProfile(
            id = idLocal,
            fullName = datos.nombreCompleto,
            nickname = datos.alias,
            memberNumber = datos.numeroMiembro,
            cedulaDni = datos.cedulaDni,
            phone = datos.telefono,
            email = datos.correo,
            birthDate = datos.fechaNacimiento,
            chapterState = datos.estadoCapitulo,
            joinYear = datos.anioIngreso,
            avatarInitials = datos.inicialesAvatar,
            firebaseUid = datos.firebaseUid.ifBlank { null },
            role = try { MemberRole.valueOf(datos.rol) } catch (_: Exception) { MemberRole.ASPIRANTE },
            isDirectiva = datos.esDirectiva,
            solvencyStatus = datos.estadoSolvencia,
            isSuspended = datos.estaSuspendido,
            suspensionReason = datos.razonSuspension,
            suspendedBy = datos.suspendidoPor,
            suspensionDurationDays = datos.diasSuspension,
            suspensionStartTimestamp = datos.inicioSuspensionTimestamp,
            suspensionEndTimestamp = datos.finSuspensionTimestamp,
            bikeBrand = datos.marcaMoto,
            bikeModel = datos.modeloMoto,
            bikeColor = datos.colorMoto,
            bikeDisplacementCc = datos.cilindradaCc,
            bikeTankCapacityLiters = datos.capacidadTanqueLitros,
            bikeYear = datos.anioMoto,
            bikePlate = datos.placaMoto,
            bikePhotoUri = datos.fotoMotoUri,
            bloodType = datos.tipoSangre,
            medicalNotes = datos.notasMedicas,
            emergencyContactName = datos.nombreContactoEmergencia,
            emergencyContactPhone = datos.telefonoContactoEmergencia,
            emergencyContactRelation = datos.parentescoContactoEmergencia,
            licenseImageUri = datos.uriLicencia,
            medicalCertImageUri = datos.uriCertificadoMedico,
            bikeRegImageUri = datos.uriRegistroMoto,
            insuranceImageUri = datos.uriSeguroVehicular,
            copilotName = datos.nombreCopiloto,
            copilotRelation = datos.parentescoCopiloto,
            attendanceCount = datos.asistencias,
            longRidesCount = datos.rodadasLargas,
            bigEventsCount = datos.eventosGrandes,
            prospectStartDate = datos.inicioProspectoTimestamp,
            profilePhotoUri = datos.fotoPerfilUri,
            isOnline = datos.estaEnLinea,
            lastActiveTimestamp = datos.ultimoActivoTimestamp
        )
    }

    // ═══════════════════════════════════════════════
    // ESCUCHA EN TIEMPO REAL
    // ═══════════════════════════════════════════════

    /**
     * Escucha en tiempo real los cambios del carnet en la nube.
     * Devuelve una ListenerRegistration para cancelar la escucha.
     *
     * @param uid UID del usuario
     * @param alCambiar Callback con los nuevos datos
     */
    fun escucharCarnet(uid: String, alCambiar: (DatosCarnet) -> Unit): ListenerRegistration? {
        return try {
            db.collection(COLECCION).document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Escucha de carnet falló para uid=${uid.take(8)}…", error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    snapshot.toObject(DatosCarnet::class.java)?.let { alCambiar(it) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo registrar escucha de carnet", e)
            null
        }
    }

    // ═══════════════════════════════════════════════
    // SINCRONIZACIÓN AUTOMÁTICA (MemberProfile ↔ Firestore)
    // ═══════════════════════════════════════════════

    /**
     * Sincroniza un MemberProfile local hacia Firestore (carnet_tx_usuarios).
     * Convierte automáticamente usando los campos en español.
     */
    suspend fun sincronizarHaciaNube(uid: String, profile: MemberProfile): Boolean {
        val datos = desdeMemberProfile(profile)
        return guardarDatosCarnet(uid, datos)
    }

    /**
     * Descarga desde Firestore y convierte a MemberProfile local.
     */
    suspend fun sincronizarDesdeNube(uid: String): MemberProfile? {
        val resultado = obtenerDatosCarnet(uid)
        return when (resultado) {
            is ResultadoCarnet.Exito -> aMemberProfile(resultado.datos)
            else -> null
        }
    }

    // ═══════════════════════════════════════════════
    // ELIMINAR DATOS
    // ═══════════════════════════════════════════════

    /**
     * Elimina los datos del carnet de Firestore.
     * Usar solo en casos extremos (baja del miembro).
     */
    suspend fun eliminarDatosCarnet(uid: String): Boolean {
        return try {
            db.collection(COLECCION).document(uid).delete().await()
            Log.i(TAG, "🗑️ Datos del carnet eliminados para uid=${uid.take(8)}…")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando carnet", e)
            false
        }
    }

    // ═══════════════════════════════════════════════
    // SERIALIZACIÓN
    // ═══════════════════════════════════════════════

    /**
     * Serializa DatosCarnet a un Map para Firestore.
     * Excluye campos nulos/vacíos para mantener la base de datos limpia.
     */
    private fun serializar(datos: DatosCarnet): Map<String, Any?> = mapOf(
        // Personales
        "nombreCompleto" to datos.nombreCompleto,
        "alias" to datos.alias,
        "numeroMiembro" to datos.numeroMiembro,
        "cedulaDni" to datos.cedulaDni,
        "telefono" to datos.telefono,
        "correo" to datos.correo,
        "fechaNacimiento" to datos.fechaNacimiento,
        "estadoCapitulo" to datos.estadoCapitulo,
        "anioIngreso" to datos.anioIngreso,
        "inicialesAvatar" to datos.inicialesAvatar,
        "firebaseUid" to datos.firebaseUid,
        // Rol
        "rol" to datos.rol,
        "esDirectiva" to datos.esDirectiva,
        "estadoSolvencia" to datos.estadoSolvencia,
        "estaSuspendido" to datos.estaSuspendido,
        "razonSuspension" to datos.razonSuspension,
        "suspendidoPor" to datos.suspendidoPor,
        "diasSuspension" to datos.diasSuspension,
        "inicioSuspensionTimestamp" to datos.inicioSuspensionTimestamp,
        "finSuspensionTimestamp" to datos.finSuspensionTimestamp,
        // Moto
        "marcaMoto" to datos.marcaMoto,
        "modeloMoto" to datos.modeloMoto,
        "colorMoto" to datos.colorMoto,
        "cilindradaCc" to datos.cilindradaCc,
        "capacidadTanqueLitros" to datos.capacidadTanqueLitros,
        "anioMoto" to datos.anioMoto,
        "placaMoto" to datos.placaMoto,
        "fotoMotoUri" to datos.fotoMotoUri,
        // Médico
        "tipoSangre" to datos.tipoSangre,
        "notasMedicas" to datos.notasMedicas,
        "nombreContactoEmergencia" to datos.nombreContactoEmergencia,
        "telefonoContactoEmergencia" to datos.telefonoContactoEmergencia,
        "parentescoContactoEmergencia" to datos.parentescoContactoEmergencia,
        // Documentos
        "uriLicencia" to datos.uriLicencia,
        "uriCertificadoMedico" to datos.uriCertificadoMedico,
        "uriRegistroMoto" to datos.uriRegistroMoto,
        "uriSeguroVehicular" to datos.uriSeguroVehicular,
        // Copiloto
        "nombreCopiloto" to datos.nombreCopiloto,
        "parentescoCopiloto" to datos.parentescoCopiloto,
        // Estadísticas
        "asistencias" to datos.asistencias,
        "rodadasLargas" to datos.rodadasLargas,
        "eventosGrandes" to datos.eventosGrandes,
        "inicioProspectoTimestamp" to datos.inicioProspectoTimestamp,
        // Fotos
        "fotoPerfilUri" to datos.fotoPerfilUri,
        // Estado
        "estaEnLinea" to datos.estaEnLinea,
        "ultimoActivoTimestamp" to System.currentTimeMillis()
    )
}
