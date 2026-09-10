package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.teamtxvzla.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.InvitationCodeCleanupJob
import com.example.data.remote.FirebaseChatSync
import com.example.data.remote.PerfilNube
import com.example.data.remote.VinculacionGoogle
import com.example.data.repository.TeamTxRepository
import com.example.GestorNotificacionesApp
import com.example.SEGURIDAD_CUENTAS
import com.example.VINCULACION
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

data class FinancialSummary(
    val totalIncomeUsd: Double = 0.0,
    val totalExpenseUsd: Double = 0.0,
    val netBalanceUsd: Double = 0.0,
    val emergencyFundUsd: Double = 0.0,
    val monthlyDuesUsd: Double = 0.0,
    val eventPotsUsd: Double = 0.0
)

class TeamTxViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG_FEED = "TEAM_TX_FEED"
        const val TAG_RODADAS = "TEAM_TX_RODADAS"
        const val TAG_SOS = "TEAM_TX_SOS"
        const val TAG_MEMBERS = "TEAM_TX_MEMBERS"
        const val TAG_TREASURY = "TEAM_TX_TREASURY"
        const val TAG_INVENTORY = "TEAM_TX_INVENTORY"
        const val TAG_DIRECTIVA = "TEAM_TX_DIRECTIVA"
    }

    private val repository: TeamTxRepository
    private val prefs = application.getSharedPreferences("team_tx_session", Context.MODE_PRIVATE)

    private var perfilSyncJob: Job? = null
    private var perfilListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var ultimoHashPerfilSubido = 0

    // Global Chat Killswitch
    private val _isChatEnabled = MutableStateFlow(true)
    val isChatEnabled: StateFlow<Boolean> = _isChatEnabled.asStateFlow()

    // Dismissed Notices / Local Feed Cleanup State
    private val _dismissedNoticeIds = MutableStateFlow<Set<Long>>(emptySet())
    val dismissedNoticeIds: StateFlow<Set<Long>> = _dismissedNoticeIds.asStateFlow()

    // ---------------- AUTHENTICATION & GATEKEEPER (Inicializados antes de init) ---------------- //
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // 🔐 NUEVA PROPIEDAD: Estado del usuario desde Firestore
    // Valores posibles: "ACTIVO", "PENDIENTE", "RECHAZADO"
    private val _usuarioEstado = MutableStateFlow("PENDIENTE")
    val usuarioEstado: StateFlow<String> = _usuarioEstado.asStateFlow()

    private val _isLeaderSuperAdmin = MutableStateFlow(false)
    val isLeaderSuperAdmin: StateFlow<Boolean> = _isLeaderSuperAdmin.asStateFlow()

    private val _isDirectivaMode = MutableStateFlow(false)
    val isDirectivaMode: StateFlow<Boolean> = _isDirectivaMode.asStateFlow()

    private val _currentMemberId = MutableStateFlow<Long>(1)

    private val _requestAttemptsLeft = MutableStateFlow(prefs.getInt("PREF_REQUEST_ATTEMPTS_LEFT", 3))
    val requestAttemptsLeft: StateFlow<Int> = _requestAttemptsLeft.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = TeamTxRepository(db, viewModelScope)
        GestorNotificacionesApp.inicializar(db, application)
        // Initialize background jobs
        InvitationCodeCleanupJob(db, viewModelScope)

        // Cargar avisos descartados/ocultados localmente
        val savedDismissed = prefs.getStringSet("dismissed_notices_set", emptySet()) ?: emptySet()
        _dismissedNoticeIds.value = savedDismissed.mapNotNull { it.toLongOrNull() }.toSet()
        Log.i(TAG_FEED, "📂 Avisos ocultados restaurados localmente: ${_dismissedNoticeIds.value.size}")

        FirebaseFirestore.getInstance().collection("app_settings").document("global")
            .addSnapshotListener { snapshot, _ ->
                val enabled = snapshot?.getBoolean("chatEnabled") ?: true
                _isChatEnabled.value = enabled
            }
            
        viewModelScope.launch {
            repository.refreshPublications()
        }

        // Verificación y sincronización en segundo plano de actualizaciones oficiales con Avisos
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(5000)
            try {
                val ota = com.example.GestorActualizaciones.verificarActualizacion()
                if (ota != null && ota.versionCode > com.aistudio.teamtxvzla.BuildConfig.VERSION_CODE) {
                    sincronizarAvisoActualizacionOta(ota, forzar = false)
                }
            } catch (e: Exception) {
                Log.e("OTA_AVISOS", "Error en verificación inicial de actualización: ${e.message}")
            }
        }

        checkAndRestoreSession()
    }

    fun setChatEnabled(enabled: Boolean) {
        FirebaseFirestore.getInstance().collection("app_settings").document("global")
            .set(mapOf("chatEnabled" to enabled), SetOptions.merge())
    }

    // Directiva mode switch (declarado arriba de init)

    fun toggleDirectivaMode() {
        val member = currentMember.value
        val hasPermission = member?.isDirectiva == true || member?.role?.canManageApp == true || _isLeaderSuperAdmin.value
        if (hasPermission) {
            _isDirectivaMode.value = !_isDirectivaMode.value
        }
    }

    // Control de Sesión Única por Dispositivo (Anti-trampas en gamificación)
    private val _sesionDesplazadaPorOtroDispositivo = MutableStateFlow(false)
    val sesionDesplazadaPorOtroDispositivo: StateFlow<Boolean> = _sesionDesplazadaPorOtroDispositivo.asStateFlow()

    // Exchange rate USD -> VES
    private val _bcvRate = MutableStateFlow(36.00)
    val bcvRate: StateFlow<Double> = _bcvRate.asStateFlow()

    fun setBcvRate(rate: Double) {
        if (rate > 0) _bcvRate.value = rate
    }

    // All Members
    val allMembers: StateFlow<List<MemberProfile>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active User Profile (Defaults to first or selected, _currentMemberId declarado arriba de init)
    val currentMember: StateFlow<MemberProfile?> = combine(allMembers, _currentMemberId) { members, id ->
        members.find { it.id == id } ?: members.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectMember(memberId: Long) {
        val current = currentMember.value
        val isDev = current?.role == MemberRole.DESARROLLADOR ||
                   current?.role == MemberRole.PRESIDENTE ||
                   current?.memberNumber == "TX-001" ||
                   current?.memberNumber?.startsWith("TX-DEV-") == true ||
                   _isLeaderSuperAdmin.value
        if (isDev) {
            _currentMemberId.value = memberId
            val memberForSession = allMembers.value.find { it.id == memberId }
            saveSession(memberId, memberForSession?.email, memberForSession?.firebaseUid)
            Log.i("TeamTxViewModel", "👨‍💻 Modo Desarrollador: Conmutado a perfil $memberId (${memberForSession?.fullName})")
        } else {
            Log.w("TeamTxViewModel", "⛔ Intento no autorizado de conmutar usuario ignorado. Solo Desarrollador.")
        }
    }

    fun updateProfile(updated: MemberProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMember(updated)
            saveSession(updated.id, updated.email, updated.firebaseUid)
        }
    }

    fun accumulateMemberKilometers(addedKm: Double) {
        val member = currentMember.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newTotalKm = member.totalKmRidden + addedKm
            val newMeritPoints = (newTotalKm / 10.0).toInt() +
                    (member.attendanceCount * 50) +
                    (member.longRidesCount * 100) +
                    (member.bigEventsCount * 200) +
                    (member.sosAssistanceCount * 150) +
                    (member.challengesCompletedCount * 75)
            val newRankTitle = com.example.ui.screens.getMemberHonorRank(newMeritPoints).title
            val updated = member.copy(
                totalKmRidden = newTotalKm,
                meritPoints = newMeritPoints,
                rankingTitle = newRankTitle
            )
            repository.updateMember(updated)
            Log.d("TEAM_TX_RANKING", "📈 Odómetro sincronizado para ${member.fullName}: +$addedKm km (Total: $newTotalKm km, $newMeritPoints pts)")
        }
    }

    fun updateMemberTopSpeed(speed: Float) {
        val member = currentMember.value ?: return
        if (speed <= member.topSpeedRecordKmh) return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = member.copy(topSpeedRecordKmh = speed)
            repository.updateMember(updated)
            Log.d("TEAM_TX_VELOCIMETRO", "⚡ Nuevo récord de velocidad guardado para ${member.fullName}: $speed km/h")
        }
    }

    fun incrementMemberSosAssistance(memberId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val member = allMembers.value.find { it.id == memberId } ?: return@launch
            val newCount = member.sosAssistanceCount + 1
            val newMeritPoints = (member.totalKmRidden / 10.0).toInt() +
                    (member.attendanceCount * 50) +
                    (member.longRidesCount * 100) +
                    (member.bigEventsCount * 200) +
                    (newCount * 150) +
                    (member.challengesCompletedCount * 75)
            val newRankTitle = com.example.ui.screens.getMemberHonorRank(newMeritPoints).title
            val updated = member.copy(
                sosAssistanceCount = newCount,
                meritPoints = newMeritPoints,
                rankingTitle = newRankTitle
            )
            repository.updateMember(updated)
            Log.d("TEAM_TX_RANKING", "🆘 Auxilio SOS sumado a ${member.fullName}: $newCount auxilios ($newMeritPoints pts)")
        }
    }

    /**
     * Califica el desempeño de un piloto otorgando likes positivos o manitos abajo
     * que suman o restan puntos de mérito en la gamificación del Ranking.
     */
    fun ratePilotMember(
        targetMemberId: Long,
        isPositive: Boolean,
        category: String,
        pointsDelta: Int,
        comment: String = "",
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val reviewer = currentMember.value
        if (reviewer == null) {
            onComplete(false, "Debes iniciar sesión para calificar a un compañero.")
            return
        }
        if (reviewer.id == targetMemberId) {
            onComplete(false, "No puedes calificar tu propio perfil de piloto.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val target = allMembers.value.find { it.id == targetMemberId }
            if (target == null) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Piloto no encontrado.")
                }
                return@launch
            }

            val newPositive = if (isPositive) target.positiveRatingsCount + 1 else target.positiveRatingsCount
            val newNegative = if (!isPositive) target.negativeRatingsCount + 1 else target.negativeRatingsCount
            val newReputationPoints = target.reputationPoints + pointsDelta

            val totalMerit = (
                (target.totalKmRidden / 10.0).toInt() +
                (target.attendanceCount * 50) +
                (target.longRidesCount * 100) +
                (target.bigEventsCount * 200) +
                (target.sosAssistanceCount * 150) +
                (target.challengesCompletedCount * 75) +
                (newPositive * 15) -
                (newNegative * 15) +
                newReputationPoints
            ).coerceAtLeast(0)

            val newRankTitle = com.example.ui.screens.getMemberHonorRank(totalMerit).title

            val updated = target.copy(
                positiveRatingsCount = newPositive,
                negativeRatingsCount = newNegative,
                reputationPoints = newReputationPoints,
                meritPoints = totalMerit,
                rankingTitle = newRankTitle
            )

            repository.updateMember(updated)

            // Sincronizar hacia Firestore
            val targetUid = target.firebaseUid
            if (!targetUid.isNullOrBlank()) {
                try {
                    com.example.data.remote.BaseDatosCarnet.sincronizarHaciaNube(targetUid, updated)
                } catch (e: Exception) {
                    Log.e("TEAM_TX_RANKING", "Error sincronizando carnet en nube: ${e.message}")
                }
            }

            Log.d(
                "TEAM_TX_RANKING",
                "⭐ Calificación de ${reviewer.fullName} a ${target.fullName}: " +
                "${if (isPositive) "👍 POSITIVO" else "👎 NEGATIVO"} ($category, $pointsDelta pts) -> " +
                "Likes: $newPositive, Dislikes: $newNegative, Total: $totalMerit pts ($newRankTitle)"
            )

            withContext(Dispatchers.Main) {
                onComplete(
                    true,
                    if (isPositive) "¡Calificación positiva registrada para ${target.fullName} (+${pointsDelta} pts)!"
                    else "Calificación registrada para ${target.fullName} (${pointsDelta} pts)."
                )
            }
        }
    }

    fun registerMember(
        fullName: String,
        nickname: String,
        memberNumber: String,
        cedulaDni: String,
        phone: String,
        role: MemberRole,
        chapterState: String,
        bikeBrand: String = "Keeway",
        bikeModel: String = "TX 200 SM",
        bikeColor: String = "Negro / Naranja",
        bikeDisplacementCc: String = "200 cc",
        bikeTankCapacityLiters: String = "11.5 L",
        bikeYear: String = "2023",
        bikePlate: String,
        bloodType: String = "O+",
        medicalNotes: String = "Sin alergias reportadas",
        emergencyName: String,
        emergencyPhone: String,
        emergencyRelation: String = "Familiar",
        solvencyStatus: Boolean = true
    ) {
        viewModelScope.launch {
            val initials = if (nickname.isNotBlank()) {
                nickname.take(2).uppercase()
            } else {
                fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "TX" }
            }
            val newMember = MemberProfile(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                fullName = fullName,
                nickname = nickname,
                memberNumber = memberNumber,
                cedulaDni = cedulaDni,
                phone = phone,
                role = role,
                chapterState = chapterState,
                bikeBrand = bikeBrand,
                bikeModel = bikeModel,
                bikeColor = bikeColor,
                bikeDisplacementCc = bikeDisplacementCc,
                bikeTankCapacityLiters = bikeTankCapacityLiters,
                bikeYear = bikeYear,
                bikePlate = bikePlate,
                bloodType = bloodType,
                medicalNotes = medicalNotes,
                emergencyContactName = emergencyName,
                emergencyContactPhone = emergencyPhone,
                emergencyContactRelation = emergencyRelation,
                isDirectiva = role.canManageApp || role == MemberRole.PRESIDENTE || role == MemberRole.CAPITAN_RUTA,
                solvencyStatus = solvencyStatus,
                joinYear = "2026",
                avatarInitials = initials
            )
            repository.insertMember(newMember)
        }
    }

    fun toggleMemberSolvency(member: MemberProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = member.copy(solvencyStatus = !member.solvencyStatus)
            repository.updateMember(updated)
            val targetUid = member.firebaseUid
            if (!targetUid.isNullOrBlank()) {
                try {
                    com.example.data.remote.BaseDatosCarnet.sincronizarHaciaNube(targetUid, updated)
                } catch (e: Exception) {
                    Log.e(TAG_DIRECTIVA, "Error al sincronizar solvencia en nube: ${e.message}")
                }
            }
        }
    }

    fun setMemberSolvency(member: MemberProfile, isSolvent: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = member.copy(solvencyStatus = isSolvent)
            repository.updateMember(updated)
            val targetUid = member.firebaseUid
            if (!targetUid.isNullOrBlank()) {
                try {
                    com.example.data.remote.BaseDatosCarnet.sincronizarHaciaNube(targetUid, updated)
                } catch (e: Exception) {
                    Log.e(TAG_DIRECTIVA, "Error al fijar solvencia en nube: ${e.message}")
                }
            }
        }
    }

    fun suspendMember(
        member: MemberProfile,
        reason: String,
        durationDays: Int,
        suspendedBy: String = "Directiva Nacional"
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = member.copy(
                isSuspended = true,
                suspensionReason = reason,
                suspensionDurationDays = durationDays,
                suspendedBy = suspendedBy,
                suspensionStartTimestamp = now,
                suspensionEndTimestamp = if (durationDays > 0) now + (durationDays * 24L * 60L * 60L * 1000L) else 0L
            )
            repository.updateMember(updated)
        }
    }

    fun reactivateMember(member: MemberProfile) {
        viewModelScope.launch {
            val updated = member.copy(
                isSuspended = false,
                suspensionReason = "",
                suspensionDurationDays = 0,
                suspendedBy = "",
                suspensionStartTimestamp = 0L,
                suspensionEndTimestamp = 0L
            )
            repository.updateMember(updated)
        }
    }

    fun toggleMemberChatMute(member: MemberProfile, isMuted: Boolean, reason: String = "") {
        viewModelScope.launch {
            val updated = member.copy(
                isChatMuted = isMuted,
                muteReason = if (isMuted) reason.ifBlank { "Silenciado por Directiva" } else ""
            )
            repository.updateMember(updated)

            val actionText = if (isMuted) "SILENCIADO DEL CHAT GENERAL" else "DESILENCIADO EN EL CHAT"
            val notice = "🔇 MEDIDA DISCIPLINARIA: ${member.fullName} (${member.nickname}) ha sido $actionText por la Directiva.${if (isMuted && reason.isNotBlank()) " Motivo: $reason" else ""}"
            repository.insertChatMessage(
                ChatMessage(
                    id = System.currentTimeMillis(),
                    channelId = "DIRECTIVA",
                    senderMemberId = currentMember.value?.id ?: 2,
                    senderName = "Tribunal & Disciplina",
                    senderNickname = "Disciplina",
                    senderMemberNumber = "TX-DIR",
                    senderRole = MemberRole.DISCIPLINARIO,
                    senderCustomRoleTitle = "Oficial Disciplinario",
                    senderInitials = "TX",
                    messageText = notice,
                    isRadioCallout = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * El miembro solicita darse de baja del Team voluntariamente.
     * Su perfil queda DESHABILITADO (isSuspended=true, duración indefinida = 0).
     * Solo un directivo puede reactivarlo con reactivateMember().
     * Se registra el motivo de la baja en suspensionReason.
     * Al terminar, se cierra la sesión local completamente.
     */
    fun darseDeBAja(motivo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val member = currentMember.value ?: return@launch
            val razon = "BAJA VOLUNTARIA: ${motivo.ifBlank { "Sin motivo especificado" }}"
            val updated = member.copy(
                isSuspended = true,
                suspensionReason = razon,
                suspensionDurationDays = 0, // 0 = indefinido, solo directivo puede reactivar
                suspendedBy = member.fullName, // se dio de baja él mismo
                suspensionStartTimestamp = System.currentTimeMillis(),
                suspensionEndTimestamp = 0L   // sin fecha de fin (indefinido)
            )
            repository.updateMember(updated)
            // También notificar en Firestore para que directivos lo vean
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("members")
                    .document(member.id.toString())
                    .update(
                        mapOf(
                            "isSuspended" to true,
                            "suspensionReason" to razon,
                            "suspensionDurationDays" to 0,
                            "suspendedBy" to member.fullName,
                            "suspensionStartTimestamp" to System.currentTimeMillis()
                        )
                    ).await()
            } catch (_: Exception) { /* Sin conexión, se sincroniza después */ }
            notifyDirectivaChannel(
                titulo = "BAJA VOLUNTARIA DE MIEMBRO",
                detalle = "⚠️ ${member.fullName} (${member.memberNumber}) se ha dado de baja del Team TX.\nMotivo: ${motivo.ifBlank { "Sin motivo especificado" }}",
                tipo = "BAJA"
            )
            // Cerrar sesión local
            try { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
            clearSession()
            _isAuthenticated.value = false
            _isLeaderSuperAdmin.value = false
            _isDirectivaMode.value = false
        }
    }

    /**
     * Expulsa definitivamente a un miembro del club por decisión de Directiva.
     * 1. Registra la baja en el Registro Histórico de Miembros Pasados (DisciplinaryRecord).
     * 2. Revoca y elimina códigos de invitación asociados para impedir que vuelva a entrar con su código.
     * 3. Elimina su perfil de Room y Firebase.
     * 4. Elimina su ubicación en vivo del radar.
     * 5. Cierra sesión local si es el usuario actual.
     * 6. Notifica en el canal de Gobernanza de Directiva.
     */
    fun expelMember(
        member: MemberProfile,
        reason: String,
        expelledBy: String = currentMember.value?.let { "${it.fullName} (${it.role.displayName})" } ?: "Directiva Nacional"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 🛡️ REGLA SUPREMA: Solo Presidente (o con código de presidente activo) y Desarrollador pueden eliminar personas
            val caller = currentMember.value
            val isPresCaller = caller?.role == MemberRole.PRESIDENTE || _isLeaderSuperAdmin.value
            val isDevCaller = caller?.role == MemberRole.DESARROLLADOR || caller?.memberNumber?.startsWith("TX-DEV-") == true
            if (!isPresCaller && !isDevCaller) {
                Log.w("TeamTxViewModel", "Intento no autorizado de expulsión por ${caller?.fullName} (${caller?.role?.displayName})")
                return@launch
            }

            // 🛡️ REGLA SUPREMA: El Desarrollador y el Presidente están blindados y son inmunes a la expulsión
            if (member.role == MemberRole.DESARROLLADOR || member.memberNumber.startsWith("TX-DEV-") || member.role == MemberRole.PRESIDENTE) {
                Log.w("TeamTxViewModel", "Intento de expulsar al Presidente o Desarrollador bloqueado por jerarquía suprema.")
                return@launch
            }

            val now = System.currentTimeMillis()
            val motivoFinal = reason.trim().ifBlank { "Expulsión definitiva acordada por la Directiva del Club TX" }

            // 1. Crear registro histórico detallado de Miembro Pasado / Expulsado
            val record = DisciplinaryRecord(
                id = now,
                memberId = member.id,
                memberName = "${member.fullName} (${member.nickname}) [${member.memberNumber}]",
                reason = motivoFinal,
                penaltyType = "EXPULSIÓN DEFINITIVA",
                issuedBy = expelledBy,
                timestamp = now
            )
            repository.insertDisciplinaryRecord(record)

            // 2. Revocar / Eliminar cualquier código de invitación activo asociado al miembro
            try {
                val codes = repository.allInvitationCodes.first()
                codes.filter { code ->
                    !code.isMaster && (
                        code.usedByName?.equals(member.fullName, ignoreCase = true) == true ||
                        (member.phone.isNotBlank() && code.usedByPhone?.endsWith(member.phone.takeLast(7)) == true) ||
                        code.note.contains(member.memberNumber, ignoreCase = true) ||
                        code.note.contains(member.fullName, ignoreCase = true)
                    )
                }.forEach { c ->
                    repository.deleteInvitationCode(c.id)
                }
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error revocando códigos de invitación para ${member.memberNumber}: ${e.message}")
            }

            // 3. Eliminar de radar en vivo (Firebase)
            try {
                com.example.radar.RadarFirebase.eliminarPilotoRemoto(member.id.toString())
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error eliminando del radar: ${e.message}")
            }

            // 4. Desvincular en Firestore y eliminar documento en usuarios/{uid}
            try {
                if (!member.email.isNullOrBlank()) {
                    VINCULACION.desvincularCuentaGoogle(member.email ?: "", member.firebaseUid ?: "", member.memberNumber)
                }
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error desvinculando en Firestore durante expulsión: ${e.message}")
            }

            // 5. Eliminar el perfil del miembro de Room y Firebase
            repository.deleteMember(member)

            // 6. Si el usuario expulsado es el que tiene la app abierta, cerrar su sesión
            if (currentMember.value?.id == member.id) {
                _currentMemberId.value = -1L
                _isDirectivaMode.value = false
                _isLeaderSuperAdmin.value = false
                _isAuthenticated.value = false
                try { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
                clearSession()
            }

            // 7. Notificar al canal privado de gobernanza de directiva
            notifyDirectivaChannel(
                titulo = "EXPULSIÓN DEFINITIVA DE MIEMBRO",
                detalle = "🚨 RESOLUCIÓN DISCIPLINARIA:\n" +
                        "👤 Miembro: ${member.fullName} (${member.nickname})\n" +
                        "🔢 Ficha: ${member.memberNumber} | Placa: ${member.bikePlate}\n" +
                        "📝 Motivo de Expulsión: $motivoFinal\n" +
                        "⚖️ Autoridad: $expelledBy\n" +
                        "🚫 Estado: Eliminado de toda la app. Códigos revocados. Para reingresar deberá solicitar un nuevo código de acceso formal.",
                tipo = "EXPULSION"
            )
        }
    }

    fun updateMemberRole(member: MemberProfile, newRole: MemberRole) {
        viewModelScope.launch(Dispatchers.IO) {
            val operator = currentMember.value
            val isOperatorDev = operator?.role == MemberRole.DESARROLLADOR || _isLeaderSuperAdmin.value
            
            // Regla Jerárquica: Solamente el Desarrollador Máster puede asignar o modificar el rol de Presidente
            if (newRole == MemberRole.PRESIDENTE && !isOperatorDev) {
                Log.w("TeamTxViewModel", "⚠️ Intento no autorizado de asignar cargo de Presidente por un operador no Desarrollador Máster.")
                return@launch
            }

            val isDir = newRole.canManageApp || newRole == MemberRole.DIRECTIVA || newRole == MemberRole.PRESIDENTE || newRole == MemberRole.CAPITAN_RUTA || newRole == MemberRole.DEPARTAMENTO_REDES
            val updated = member.copy(role = newRole, isDirectiva = isDir)
            
            repository.updateMember(updated)

            // Sincronizar en Firestore
            val uid = updated.firebaseUid
            if (!uid.isNullOrBlank()) {
                val updateData = mapOf(
                    "role" to newRole.name,
                    "isDirectiva" to isDir,
                    "lastActiveTimestamp" to System.currentTimeMillis()
                )
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("usuarios").document(uid).set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                    db.collection("users").document(uid).set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                    db.collection("members").document(updated.id.toString()).set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (e: Exception) {
                    Log.e("TeamTxViewModel", "Error sincronizando nuevo rol en Firestore: ${e.message}")
                }
            }
        }
    }

    /** Purga completa y definitiva de un usuario en Firestore y Room */
    fun eliminarUsuarioDefinitivamente(member: MemberProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = member.firebaseUid ?: ""
            val email = member.email ?: ""
            val id = member.id

            // 1. Purga total en Firestore (usuarios, users, members, vinculos_google)
            PerfilNube.eliminarUsuarioTotalmente(uid, email, id)

            // 2. Borrado local en Room DB
            repository.deleteMember(member)

            Log.i("TeamTxViewModel", "🗑️ Usuario ${member.fullName} (${member.memberNumber}) purgado definitivamente por la Directiva.")
        }
    }

    /** Inhabilita/Habilita individualmente módulos por piloto (Killswitch Modular) */
    fun toggleModuloPiloto(member: MemberProfile, moduloTag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDisabled = member.disabledModulesList.toMutableList()
            if (currentDisabled.contains(moduloTag)) {
                currentDisabled.remove(moduloTag)
            } else {
                currentDisabled.add(moduloTag)
            }
            val newJson = currentDisabled.joinToString(",")
            val updated = member.copy(disabledModulesJson = newJson)

            repository.updateMember(updated)

            val uid = updated.firebaseUid
            if (!uid.isNullOrBlank()) {
                try {
                    val updateMap = mapOf("disabledModulesJson" to newJson)
                    val db = FirebaseFirestore.getInstance()
                    db.collection("usuarios").document(uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                    db.collection("users").document(uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                    db.collection("members").document(updated.id.toString()).set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (e: Exception) {
                    Log.e("TeamTxViewModel", "Error actualizando módulos deshabilitados en Firestore: ${e.message}")
                }
            }
        }
    }

    // Feed / Publications
    val publications: StateFlow<List<Publication>> = repository.allPublications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshingFeed = MutableStateFlow(false)
    val isRefreshingFeed: StateFlow<Boolean> = _isRefreshingFeed.asStateFlow()

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshingFeed.value = true
            try {
                repository.refreshPublications()
            } finally {
                _isRefreshingFeed.value = false
            }
        }
    }

    private val _isUploadingPublication = MutableStateFlow(false)
    val isUploadingPublication: StateFlow<Boolean> = _isUploadingPublication.asStateFlow()

    private val _publicationUploadError = MutableStateFlow<String?>(null)
    val publicationUploadError: StateFlow<String?> = _publicationUploadError.asStateFlow()

    private val _publicationUploadSuccess = MutableStateFlow(false)
    val publicationUploadSuccess: StateFlow<Boolean> = _publicationUploadSuccess.asStateFlow()

    fun createPublication(
        title: String,
        content: String,
        category: NoticeCategory,
        priority: NoticePriority,
        isPinned: Boolean = false,
        challengeKm: Int = 0,
        challengeBadge: String? = null,
        telegramUrl: String? = null,
        imageUri: Uri? = null,
        allowComments: Boolean = true,
        locationCoordinates: String? = null,
        locationName: String? = null,
        eventDate: String? = null,
        eventTime: String? = null,
        syncWithCalendar: Boolean = false
    ) {
        viewModelScope.launch {
            _isUploadingPublication.value = true
            _publicationUploadError.value = null
            _publicationUploadSuccess.value = false
            try {
                var uploadedImageUrl: String? = null
                var uploadOrigen = "ninguno"
                
                if (imageUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val resultado = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(),
                        imageUri,
                        "avisos"
                    )
                    
                    if (resultado.url != null) {
                        uploadedImageUrl = resultado.url
                        uploadOrigen = resultado.origen.name
                        Log.i("TeamTxViewModel", "✅ Imagen subida vía ${resultado.origen}: $uploadedImageUrl")
                    } else {
                        val errorMsg = resultado.error ?: "Error desconocido subiendo imagen"
                        Log.e("TeamTxViewModel", "❌ Fallo subida imagen: $errorMsg")
                        _publicationUploadError.value = "No se pudo subir la imagen: $errorMsg. El aviso se publicará sin imagen."
                    }
                }

                val author = currentMember.value?.fullName ?: "Directiva Team TX"
                val role = if (_isDirectivaMode.value) "Directiva Nacional" else "Miembro TX"
                val pubId = System.currentTimeMillis()
                var linkedCalId: Long? = null

                // 📅 Sincronización automática con el Calendario Motero si tiene fecha
                val cleanedEventDate = eventDate?.trim()?.ifBlank { null }
                val cleanedEventTime = eventTime?.trim()?.ifBlank { "08:00 AM" } ?: "08:00 AM"
                if (syncWithCalendar && cleanedEventDate != null) {
                    val calId = pubId + 1
                    linkedCalId = calId
                    val calEvent = BikerCalendarEvent(
                        id = calId,
                        title = title,
                        description = content,
                        category = when (category) {
                            NoticeCategory.RETO_MOTERO -> "Reto Motero"
                            NoticeCategory.EMERGENCIA -> "Emergencia / Auxilio"
                            NoticeCategory.COMUNICADO -> "Comunicado Oficial"
                            else -> "Ruta Oficial"
                        },
                        visibility = "PUBLICO_CLUB",
                        eventDate = cleanedEventDate,
                        eventTime = cleanedEventTime,
                        departureTime = "08:30 AM",
                        originAddress = locationName?.trim()?.ifBlank { "Punto de concentración oficial" } ?: "Punto de concentración oficial",
                        destinationAddress = locationName?.trim()?.ifBlank { "" } ?: "",
                        isOfficialClubEvent = true,
                        creatorMemberId = currentMember.value?.id ?: 1L,
                        creatorName = author,
                        flyerUrl = uploadedImageUrl,
                        linkedPublicationId = pubId,
                        isEventFinished = false,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertCalendarEvent(calEvent)
                    Log.i("TeamTxViewModel", "📅 Evento sincronizado en Calendario Motero (ID: $calId, Fecha: $cleanedEventDate)")
                }

                val pub = Publication(
                    id = pubId,
                    title = title,
                    content = content,
                    category = category,
                    priority = priority,
                    authorName = author,
                    authorRole = role,
                    isPinned = isPinned,
                    targetChallengeDistanceKm = challengeKm,
                    challengeBadgeText = challengeBadge,
                    telegramPostUrl = telegramUrl,
                    imageUrl = uploadedImageUrl,
                    allowComments = allowComments,
                    locationCoordinates = locationCoordinates?.trim()?.ifBlank { null },
                    locationName = locationName?.trim()?.ifBlank { null },
                    eventDate = cleanedEventDate,
                    eventTime = if (cleanedEventDate != null) cleanedEventTime else null,
                    isEventFinished = false,
                    linkedCalendarEventId = linkedCalId,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPublication(pub)
                _publicationUploadSuccess.value = true
                Log.i("TeamTxViewModel", "✅ Publicación creada con fecha: ${pub.eventDate}, calId: ${pub.linkedCalendarEventId}")
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error creando publicación", e)
                _publicationUploadError.value = "Error al publicar: ${e.message}"
            } finally {
                _isUploadingPublication.value = false
            }
        }
    }

    fun updatePublication(
        publication: Publication,
        newImageUri: Uri? = null,
        syncWithCalendar: Boolean = true,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var finalImageUrl = publication.imageUrl
                if (newImageUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), newImageUri, "flyers"
                    )
                    finalImageUrl = res.url
                }

                var updatedPub = publication.copy(imageUrl = finalImageUrl)
                val cleanedEventDate = updatedPub.eventDate?.trim()?.ifBlank { null }
                val cleanedEventTime = updatedPub.eventTime?.trim()?.ifBlank { "08:00 AM" } ?: "08:00 AM"

                // Actualizar o crear evento vinculado en el Calendario Motero
                if (cleanedEventDate != null && syncWithCalendar) {
                    val calId = updatedPub.linkedCalendarEventId ?: (System.currentTimeMillis() + 1)
                    val calEvent = BikerCalendarEvent(
                        id = calId,
                        title = updatedPub.title,
                        description = updatedPub.content,
                        category = when (updatedPub.category) {
                            NoticeCategory.RETO_MOTERO -> "Reto Motero"
                            NoticeCategory.EMERGENCIA -> "Emergencia / Auxilio"
                            NoticeCategory.COMUNICADO -> "Comunicado Oficial"
                            else -> "Ruta Oficial"
                        },
                        visibility = "PUBLICO_CLUB",
                        eventDate = cleanedEventDate,
                        eventTime = cleanedEventTime,
                        departureTime = "08:30 AM",
                        originAddress = updatedPub.locationName?.trim()?.ifBlank { "Punto de concentración oficial" } ?: "Punto de concentración oficial",
                        destinationAddress = updatedPub.locationName?.trim()?.ifBlank { "" } ?: "",
                        isOfficialClubEvent = true,
                        creatorMemberId = currentMember.value?.id ?: 1L,
                        creatorName = updatedPub.authorName,
                        flyerUrl = finalImageUrl,
                        linkedPublicationId = updatedPub.id,
                        isEventFinished = updatedPub.isEventFinished,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.updateCalendarEvent(calEvent)
                    updatedPub = updatedPub.copy(linkedCalendarEventId = calId)
                }

                repository.updatePublication(updatedPub)
                _publicationUploadSuccess.value = true
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error actualizando publicación: ${e.message}", e)
                _publicationUploadError.value = "Error actualizando aviso: ${e.message}"
                onComplete(false)
            }
        }
    }

    fun togglePublicationEventFinished(pub: Publication, isFinished: Boolean) {
        viewModelScope.launch {
            try {
                val updatedPub = pub.copy(isEventFinished = isFinished)
                repository.updatePublication(updatedPub)

                // Si tiene evento de calendario vinculado, actualizarlo también
                if (pub.linkedCalendarEventId != null) {
                    val allCalEvents = calendarEvents.value
                    val calEvent = allCalEvents.find { it.id == pub.linkedCalendarEventId || it.linkedPublicationId == pub.id }
                    if (calEvent != null) {
                        repository.updateCalendarEvent(calEvent.copy(isEventFinished = isFinished))
                    }
                }
                Log.i("TeamTxViewModel", "✅ Estado de evento en Muro cambiado: isFinished = $isFinished")
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error al cambiar estado de finalización: ${e.message}")
            }
        }
    }

    fun clearPublicationUploadStatus() {
        _publicationUploadError.value = null
        _publicationUploadSuccess.value = false
    }

    fun likePublication(pub: Publication) {
        val memberId = currentMember.value?.id ?: 1L
        viewModelScope.launch {
            val memberIdStr = memberId.toString()
            val currentList = pub.likedByMemberIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            val isAlreadyLiked = currentList.contains(memberIdStr)
            val newCount: Int
            if (isAlreadyLiked) {
                currentList.remove(memberIdStr)
                val calculated = maxOf(0, pub.likesCount - 1)
                newCount = if (currentList.size > calculated) currentList.size else calculated
            } else {
                currentList.add(memberIdStr)
                val calculated = pub.likesCount + 1
                newCount = if (currentList.size > calculated) currentList.size else calculated
            }
            val updatedPub = pub.copy(
                likesCount = newCount,
                likedByMemberIds = currentList.joinToString(",")
            )
            repository.updatePublication(updatedPub)
        }
    }

    fun sharePublication(pub: Publication) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedPub = pub.copy(sharesCount = pub.sharesCount + 1)
                repository.updatePublication(updatedPub)
                Log.i("TeamTxViewModel", "📊 Publicación ${pub.id} compartida (Total: ${updatedPub.sharesCount})")
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error incrementando sharesCount: ${e.message}")
            }
        }
    }

    fun savePublication(pub: Publication) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedPub = pub.copy(savesCount = pub.savesCount + 1)
                repository.updatePublication(updatedPub)
                Log.i("TeamTxViewModel", "📊 Flyer de publicación ${pub.id} guardado/descargado (Total: ${updatedPub.savesCount})")
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error incrementando savesCount: ${e.message}")
            }
        }
    }

    fun dismissNotice(publicationId: Long) {
        val updated = _dismissedNoticeIds.value.toMutableSet().apply { add(publicationId) }
        _dismissedNoticeIds.value = updated
        prefs.edit().putStringSet("dismissed_notices_set", updated.map { it.toString() }.toSet()).apply()
        Log.i(TAG_FEED, "🧹 Aviso $publicationId ocultado de la pantalla localmente")
    }

    fun clearAllNoticesFromScreen(currentIds: List<Long>) {
        val updated = _dismissedNoticeIds.value.toMutableSet().apply { addAll(currentIds) }
        _dismissedNoticeIds.value = updated
        prefs.edit().putStringSet("dismissed_notices_set", updated.map { it.toString() }.toSet()).apply()
        Log.i(TAG_FEED, "🧹 ${currentIds.size} avisos ocultados de la pantalla (Limpieza total)")
    }

    fun restoreDismissedNotices() {
        _dismissedNoticeIds.value = emptySet()
        prefs.edit().remove("dismissed_notices_set").apply()
        Log.i(TAG_FEED, "👁️ Todos los avisos ocultos fueron restaurados en la pantalla")
    }

    fun postSystemNotice(
        title: String,
        content: String,
        category: NoticeCategory = NoticeCategory.COMUNICADO,
        priority: NoticePriority = NoticePriority.NORMAL,
        isPinned: Boolean = false,
        targetChallengeDistanceKm: Int = 0,
        challengeBadgeText: String? = null,
        telegramPostUrl: String? = null,
        imageUrl: String? = null,
        allowComments: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val publication = Publication(
                    id = System.currentTimeMillis(),
                    title = title,
                    content = content,
                    category = category,
                    priority = priority,
                    isPinned = isPinned,
                    targetChallengeDistanceKm = targetChallengeDistanceKm,
                    challengeBadgeText = challengeBadgeText,
                    telegramPostUrl = telegramPostUrl,
                    imageUrl = imageUrl,
                    allowComments = allowComments,
                    authorName = "Sistema Team TX",
                    authorRole = "SISTEMA",
                    timestamp = System.currentTimeMillis(),
                    likesCount = 0,
                    sharesCount = 0,
                    savesCount = 0,
                    likedByMemberIds = ""
                )
                repository.insertPublication(publication)
                Log.i(TAG_FEED, "📢 Aviso de sistema publicado en Muro: $title [$category]")
            } catch (e: Exception) {
                Log.e(TAG_FEED, "❌ Error publicando aviso de sistema: ${e.message}", e)
            }
        }
    }

    fun deletePublication(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deletePublication(id)
                Log.i(TAG_FEED, "✅ Publicación $id eliminada exitosamente de Room y Firestore")
            } catch (e: Exception) {
                Log.e(TAG_FEED, "❌ Error eliminando publicación $id", e)
            }
        }
    }

    /**
     * Sincroniza automáticamente la disponibilidad de una actualización OTA con el Muro de Avisos
     * y el Centro de Notificaciones, garantizando que todos los pilotos estén informados con el changelog.
     */
    fun sincronizarAvisoActualizacionOta(
        infoOta: com.example.GestorActualizaciones.InformacionOta,
        forzar: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val versionTag = "v${infoOta.versionName.trim()}"

                // Evitar duplicados a menos que se fuerce la sincronización manual
                if (!forzar) {
                    val yaExiste = publications.value.any { pub: Publication ->
                        pub.title.contains(versionTag, ignoreCase = true) ||
                        (pub.category == NoticeCategory.AVISO_OFICIAL && pub.content.contains(versionTag, ignoreCase = true))
                    }
                    if (yaExiste) {
                        Log.d("OTA_AVISOS", "El aviso para $versionTag ya se encuentra publicado en el Muro.")
                        return@launch
                    }
                }

                val tituloAviso = "🚀 ¡Nueva Versión Oficial $versionTag! (${infoOta.titulo})"
                val cuerpoAviso = buildString {
                    appendLine("🏍️ ATENCIÓN HERMANDAD MOTERA - TEAM NACIONAL TX:")
                    appendLine()
                    appendLine("Se encuentra disponible la actualización oficial $versionTag en el módulo INFO de la aplicación.")
                    appendLine()
                    if (infoOta.notas.isNotBlank()) {
                        appendLine("📝 Resumen del Release:")
                        appendLine(infoOta.notas)
                        appendLine()
                    }
                    if (infoOta.novedades.isNotEmpty()) {
                        appendLine("✨ Novedades Principales:")
                        infoOta.novedades.forEach { appendLine("• $it") }
                        appendLine()
                    }
                    if (infoOta.correcciones.isNotEmpty()) {
                        appendLine("🛠️ Correcciones y Mejoras:")
                        infoOta.correcciones.forEach { appendLine("✓ $it") }
                        appendLine()
                    }
                    appendLine("📲 Abre el módulo INFO (icono ℹ️) para descargar e instalar la actualización oficial.")
                }

                // 1. Publicar en el Muro (Avisos Oficiales)
                postSystemNotice(
                    title = tituloAviso,
                    content = cuerpoAviso,
                    category = NoticeCategory.AVISO_OFICIAL,
                    priority = NoticePriority.IMPORTANTE,
                    isPinned = true
                )

                // 2. Notificación en Centro de Avisos
                GestorNotificacionesApp.notificarActualizacionDisponible(
                    versionName = infoOta.versionName,
                    titulo = infoOta.titulo,
                    novedades = if (infoOta.novedades.isNotEmpty()) infoOta.novedades.first() else infoOta.notas
                )

                Log.i("OTA_AVISOS", "📢 Aviso de actualización $versionTag sincronizado exitosamente con el Muro y Notificaciones.")
            } catch (e: Exception) {
                Log.e("OTA_AVISOS", "❌ Error sincronizando aviso de actualización: ${e.message}", e)
            }
        }
    }

    // Rides & Registrations
    val rides: StateFlow<List<RideEvent>> = repository.allRides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registrations: StateFlow<List<RideRegistration>> = repository.allRegistrations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createRide(
        title: String,
        description: String,
        origin: String,
        destination: String,
        departureDate: String,
        meetingTime: String,
        distanceKm: Int,
        terrainType: String,
        convoyLeader: String,
        secondLeader: String = "Segundo Guía / Sub-Capitán",
        roadSafetyOfficer: String = "Oficial de Seguridad",
        tailRider: String,
        medicOfficer: String = "Médico / Paramédico de Ruta",
        mechanicOfficer: String = "Mecánico Oficial TX",
        gasStops: String,
        requiredGear: String,
        costUsd: Double,
        maxParticipants: Int,
        whatsappLink: String? = null
    ) {
        viewModelScope.launch {
            val costVes = costUsd * _bcvRate.value
            val ride = RideEvent(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                title = title,
                description = description,
                originCity = origin,
                destinationCity = destination,
                departureDate = departureDate,
                meetingTime = meetingTime,
                distanceKm = distanceKm,
                terrainType = terrainType,
                convoyLeader = convoyLeader,
                secondLeader = secondLeader,
                roadSafetyOfficer = roadSafetyOfficer,
                tailRider = tailRider,
                medicOfficer = medicOfficer,
                mechanicOfficer = mechanicOfficer,
                gasStops = gasStops,
                requiredGear = requiredGear,
                costUsdCents = (costUsd * 100).toLong(),
                costVesCents = (costVes * 100).toLong(),
                maxParticipants = maxParticipants,
                whatsappGroupUrl = whatsappLink
            )
            repository.insertRide(ride)
            Log.i(TAG_RODADAS, "🏍️ Rodada creada: $title ($origin -> $destination)")

            // Auto-publicación en el Muro (Avisos de Rodadas)
            postSystemNotice(
                title = "🏍️ NUEVA RODADA: $title",
                content = "Ruta: $origin ➡️ $destination\n" +
                        "📅 Fecha: $departureDate • ⏰ Hora: $meetingTime\n" +
                        "📍 Distancia: $distanceKm KM • Terreno: $terrainType\n" +
                        "👑 Capitán de Ruta: $convoyLeader\n" +
                        (if (tailRider.isNotBlank()) "🛡️ Barrendero: $tailRider\n" else "") +
                        "\n$description",
                category = NoticeCategory.NOTICIA_RUTA,
                priority = NoticePriority.NORMAL,
                isPinned = true,
                targetChallengeDistanceKm = distanceKm,
                challengeBadgeText = "RODADA OFICIAL TX",
                telegramPostUrl = whatsappLink
            )
        }
    }

    fun updateRideStatus(ride: RideEvent, newStatus: RideStatus) {
        viewModelScope.launch {
            repository.updateRide(ride.copy(status = newStatus))
        }
    }

    fun updateRide(ride: RideEvent) {
        viewModelScope.launch {
            repository.updateRide(ride)
        }
    }

    fun deleteRide(id: Long) {
        viewModelScope.launch {
            repository.deleteRide(id)
        }
    }

    fun finalizeRideAndRegisterAttendance(
        ride: RideEvent,
        attendedMemberIds: List<Long>
    ) {
        viewModelScope.launch {
            // Update ride status to FINALIZADA
            repository.updateRide(ride.copy(status = RideStatus.FINALIZADA))

            // Update registration checkedIn status and increment attendance for attended members
            val allRegs = repository.allRegistrations.first().filter { it.rideId == ride.id }
            for (reg in allRegs) {
                val didAttend = attendedMemberIds.contains(reg.memberId)
                if (reg.checkedIn != didAttend) {
                    repository.updateRegistration(reg.copy(checkedIn = didAttend))
                }
            }

            val allM = repository.allMembers.first()
            for (memberId in attendedMemberIds) {
                val member = allM.find { it.id == memberId }
                if (member != null) {
                    val updated = member.copy(
                        attendanceCount = member.attendanceCount + 1,
                        longRidesCount = if (ride.distanceKm >= 100) member.longRidesCount + 1 else member.longRidesCount
                    )
                    repository.updateMember(updated)
                }
            }
        }
    }

    fun updateRegistrationConvoyRole(registrationId: Long, newRole: ConvoyRoleType) {
        viewModelScope.launch {
            val list = registrations.value
            val reg = list.find { it.id == registrationId } ?: return@launch
            repository.updateRegistration(reg.copy(convoyRole = newRole.label))
        }
    }

    fun updateRegistrationConvoyRole(registration: RideRegistration, newRole: String) {
        viewModelScope.launch {
            repository.updateRegistration(registration.copy(convoyRole = newRole))
        }
    }

    fun updateRegistrationConvoyRole(registrationId: Long, newRole: String) {
        viewModelScope.launch {
            val list = registrations.value
            val reg = list.find { it.id == registrationId } ?: return@launch
            repository.updateRegistration(reg.copy(convoyRole = newRole))
        }
    }

    fun joinRide(
        rideId: Long,
        hasPillion: Boolean,
        pillionName: String,
        convoyRole: String = "Piloto Caravana"
    ) {
        viewModelScope.launch {
            val member = currentMember.value ?: return@launch
            if (member.isSuspended) return@launch // Suspended members cannot join
            val reg = RideRegistration(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                rideId = rideId,
                memberId = member.id,
                memberName = member.fullName,
                memberAlias = member.nickname,
                bikePlate = member.bikePlate,
                hasPillion = hasPillion,
                pillionName = pillionName,
                convoyRole = convoyRole,
                paymentConfirmed = true,
                checkedIn = false
            )
            repository.registerForRide(reg)
        }
    }

    fun cancelRideJoin(rideId: Long) {
        viewModelScope.launch {
            val member = currentMember.value ?: return@launch
            repository.cancelRegistration(rideId, member.id)
        }
    }

    // Finances
    val transactions: StateFlow<List<FinancialTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialSummary: StateFlow<FinancialSummary> = transactions.map { list ->
        var income = 0.0
        var expense = 0.0
        var sosFund = 0.0
        var dues = 0.0
        var pots = 0.0

        for (tx in list) {
            if (tx.status == PaymentStatus.VERIFICADO) {
                if (tx.type == TransactionType.INGRESO) {
                    income += tx.amountUsd
                    when (tx.category) {
                        PaymentCategory.FONDO_EMERGENCIA -> sosFund += tx.amountUsd
                        PaymentCategory.MEMBRESIA_MENSUAL -> dues += tx.amountUsd
                        PaymentCategory.POTE_EVENTO -> pots += tx.amountUsd
                        else -> {}
                    }
                } else {
                    expense += tx.amountUsd
                }
            }
        }
        FinancialSummary(
            totalIncomeUsd = income,
            totalExpenseUsd = expense,
            netBalanceUsd = income - expense,
            emergencyFundUsd = sosFund,
            monthlyDuesUsd = dues,
            eventPotsUsd = pots
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    fun submitPayment(
        concept: String,
        description: String,
        category: PaymentCategory,
        amountUsd: Double,
        paymentMethod: PaymentMethod,
        referenceCode: String
    ) {
        viewModelScope.launch {
            val member = currentMember.value
            val memberName = member?.fullName ?: "Miembro TX"
            val memberNum = member?.memberNumber ?: "TX-000"
            val amountVes = amountUsd * _bcvRate.value

            val tx = FinancialTransaction(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                concept = concept,
                description = description,
                category = category,
                type = TransactionType.INGRESO,
                amountUsdCents = (amountUsd * 100).toLong(),
                amountVesCents = (amountVes * 100).toLong(),
                paymentMethod = paymentMethod,
                referenceCode = referenceCode,
                memberName = "$memberName ($memberNum)",
                memberNumber = memberNum,
                status = PaymentStatus.VERIFICADO
            )
            repository.insertTransaction(tx)
        }
    }

    fun submitExpense(
        concept: String,
        description: String,
        category: PaymentCategory,
        amountUsd: Double,
        paymentMethod: PaymentMethod,
        referenceCode: String
    ) {
        viewModelScope.launch {
            val amountVes = amountUsd * _bcvRate.value
            val tx = FinancialTransaction(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                concept = concept,
                description = description,
                category = category,
                type = TransactionType.GASTO,
                amountUsdCents = (amountUsd * 100).toLong(),
                amountVesCents = (amountVes * 100).toLong(),
                paymentMethod = paymentMethod,
                referenceCode = referenceCode,
                memberName = "Directiva / Tesorería Nacional",
                memberNumber = "TX-DIRECTIVA",
                status = PaymentStatus.VERIFICADO
            )
            repository.insertTransaction(tx)
        }
    }

    fun updateTransactionStatus(tx: FinancialTransaction, newStatus: PaymentStatus) {
        viewModelScope.launch {
            repository.updateTransaction(tx.copy(status = newStatus))
        }
    }

    // Inventory & Loans
    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipmentLoans: StateFlow<List<EquipmentLoan>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createInventoryItem(
        code: String,
        name: String,
        category: ItemCategory,
        description: String,
        totalStock: Int,
        condition: ItemCondition,
        location: String,
        custodianName: String
    ) {
        viewModelScope.launch {
            val item = InventoryItem(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                code = code,
                name = name,
                category = category,
                description = description,
                totalStock = totalStock,
                availableStock = totalStock,
                condition = condition,
                location = location,
                lastMaintenanceTimestamp = System.currentTimeMillis(),
                custodianName = custodianName
            )
            repository.insertItem(item)
        }
    }

    fun requestEquipmentLoan(
        item: InventoryItem,
        expectedReturnDate: String,
        purpose: String
    ) {
        viewModelScope.launch {
            val member = currentMember.value ?: return@launch
            if (item.availableStock > 0) {
                val loan = EquipmentLoan(
                    id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                    itemId = item.id,
                    itemName = item.name,
                    itemCode = item.code,
                    borrowerName = member.fullName,
                    borrowerPhone = member.phone,
                    borrowerMemberNumber = member.memberNumber,
                    loanTimestamp = System.currentTimeMillis(),
                    expectedReturnTimestamp = System.currentTimeMillis() + 86400000L,
                    status = LoanStatus.ACTIVO,
                    purpose = purpose,
                    authorizedBy = "Directiva / Registro Automático"
                )
                repository.insertLoan(loan)
                repository.updateItem(item.copy(availableStock = item.availableStock - 1))
                notifyDirectivaChannel(
                    titulo = "SOLICITUD DE PRÉSTAMO DE EQUIPO",
                    detalle = "📦 ${member.fullName} (${member.memberNumber}) solicitó préstamo de '${item.name}' (${item.code}).\nMotivo: $purpose\nRetorno estimado: $expectedReturnDate",
                    tipo = "PRESTAMO"
                )
            }
        }
    }

    fun returnEquipmentLoan(loan: EquipmentLoan, items: List<InventoryItem>) {
        viewModelScope.launch {
            repository.updateLoan(loan.copy(status = LoanStatus.DEVUELTO, actualReturnTimestamp = System.currentTimeMillis()))
            val item = items.find { it.id == loan.itemId }
            if (item != null && item.availableStock < item.totalStock) {
                repository.updateItem(item.copy(availableStock = item.availableStock + 1))
            }
        }
    }

    // Emergency Alerts / SOS
    val emergencyAlerts: StateFlow<List<EmergencyAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun broadcastSosEmergency(
        emergencyType: EmergencyType,
        locationDesc: String,
        details: String,
        bloodTypeNeeded: String? = null,
        lat: Double = 0.0,
        lng: Double = 0.0
    ) {
        viewModelScope.launch {
            var finalLat = lat
            var finalLng = lng
            var finalDesc = locationDesc

            // Si las coordenadas son 0.0 o el valor legado de Caracas (10.4806, -66.9036), capturar las coordenadas reales del piloto
            if ((finalLat == 0.0 || finalLat == 10.4806) && (finalLng == 0.0 || finalLng == -66.9036)) {
                try {
                    val gestor = com.example.chat.GestorUbicacion(getApplication())
                    val loc = gestor.capturarLocationObjeto()
                    if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                        finalLat = loc.latitude
                        finalLng = loc.longitude
                        if (finalDesc.isBlank() || finalDesc.contains("cerca de Tazón", ignoreCase = true) || finalDesc.contains("Caracas", ignoreCase = true)) {
                            finalDesc = gestor.obtenerNombreUbicacion(loc.latitude, loc.longitude)
                        }
                    } else {
                        val rPrefs = getApplication<android.app.Application>().getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
                        val sLat = rPrefs.getString("last_lat", null)?.toDoubleOrNull()
                        val sLon = rPrefs.getString("last_lon", null)?.toDoubleOrNull()
                        if (sLat != null && sLon != null && sLat != 0.0 && sLat != 10.4806) {
                            finalLat = sLat
                            finalLng = sLon
                            if (finalDesc.isBlank() || finalDesc.contains("cerca de Tazón", ignoreCase = true) || finalDesc.contains("Caracas", ignoreCase = true)) {
                                finalDesc = gestor.obtenerNombreUbicacion(sLat, sLon)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG_SOS, "Error resolviendo GPS real en broadcastSosEmergency: ${e.message}")
                }
            }

            if (finalDesc.isBlank()) {
                finalDesc = if (finalLat != 0.0 && finalLng != 0.0) "Coordenadas GPS: %.5f, %.5f".format(finalLat, finalLng) else "Ubicación del Piloto"
            }

            // Guardar destino en radar para centrar mapa en el punto del SOS
            if (finalLat != 0.0 && finalLng != 0.0) {
                try {
                    val rPrefs = getApplication<android.app.Application>().getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
                    rPrefs.edit()
                        .putString("target_dest_lat", finalLat.toString())
                        .putString("target_dest_lon", finalLng.toString())
                        .putString("target_dest_name", "🚨 SOS: ${emergencyType.label}")
                        .apply()
                } catch (_: Exception) {}
            }

            val member = currentMember.value
            val alert = EmergencyAlert(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                reporterName = member?.fullName ?: "Hermano Motero TX",
                reporterPhone = member?.phone ?: "+58 412 000 0000",
                memberNumber = member?.memberNumber ?: "TX-SOS",
                emergencyType = emergencyType,
                locationDescription = finalDesc,
                coordinateLat = finalLat,
                coordinateLng = finalLng,
                bikeDetails = "${member?.bikeModel ?: "Keeway TX 200"} - Placa: ${member?.bikePlate ?: "N/A"}",
                bloodTypeNeeded = bloodTypeNeeded ?: member?.bloodType,
                details = details,
                status = EmergencyStatus.ACTIVA,
                respondersNotes = "Alerta emitida. Grupo de apoyo y Directiva notificados."
            )
            repository.insertAlert(alert)
            Log.i(TAG_SOS, "🚨 Alerta SOS emitida: ${alert.reporterName} en $finalDesc ($finalLat, $finalLng)")

            val esCopiloto = member?.role == MemberRole.COPILOTO
            val rolTexto = if (esCopiloto) "El copiloto" else "El piloto"
            val gradoInfo = "[${emergencyType.levelTag} - ${emergencyType.levelName}]"

            notifyDirectivaChannel(
                titulo = "EMERGENCIA SOS $gradoInfo",
                detalle = "🚨 $rolTexto ${alert.reporterName} (${alert.memberNumber}) ha emitido una alerta de ${emergencyType.label} (Nivel ${emergencyType.levelNumber}).\n📍 Ubicación: $finalDesc\n🗺️ Coordenadas GPS: $finalLat, $finalLng\n🏍️ Moto: ${alert.bikeDetails}\n📋 Protocolo: ${emergencyType.actionProtocol}\n👨‍⚕️ Especialista: ${emergencyType.recommendedSpecialist}\n📝 Detalles: $details",
                tipo = "SOS"
            )

            // Auto-publicación en el Chat Táctico General, Auxilio y Directiva
            val chatMsg = "🚨 ALERTA SOS VIAL $gradoInfo: $rolTexto ${alert.reporterName} (${alert.memberNumber}) reporta ${emergencyType.label.uppercase()} en: $finalDesc.\n📍 GPS: $finalLat, $finalLng\n🏍️ Vehículo: ${alert.bikeDetails}\n📋 Protocolo: ${emergencyType.actionProtocol}\n👨‍⚕️ Especialista asignado: ${emergencyType.recommendedSpecialist}\n📝 Detalles: $details"
            sendChatMessage("GENERAL", chatMsg, isRadioCallout = true)
            sendChatMessage("AUXILIO", chatMsg, isRadioCallout = true)
            sendChatMessage("DIRECTIVA", "🚨 [DIRECTIVA SOS] $gradoInfo: $rolTexto ${alert.reporterName} (${alert.memberNumber}) reporta ${emergencyType.label} en $finalDesc (GPS: $finalLat, $finalLng). Requerido: ${emergencyType.recommendedSpecialist}.", isRadioCallout = true)

            // Auto-publicación en el Muro: SOLO SI ES CHOQUE, CAÍDA, ACCIDENTE O NIVEL CRÍTICO (Gasolina o Mecánico NO van al feed)
            if (emergencyType.sePublicaEnMuro) {
                postSystemNotice(
                    title = "🚨 [${emergencyType.levelTag}] SOS VIAL: $rolTexto ${alert.reporterName} (${emergencyType.label})",
                    content = "⚠️ *Emergencia Crítica:* ${emergencyType.label} (${emergencyType.levelName})\n" +
                            "👤 *Afectado:* $rolTexto ${alert.reporterName} (${alert.memberNumber})\n" +
                            "📍 *Ubicación:* $finalDesc\n" +
                            "🗺️ *Coordenadas GPS:* $finalLat, $finalLng\n" +
                            "🏍️ *Vehículo:* ${alert.bikeDetails}\n" +
                            (if (!alert.bloodTypeNeeded.isNullOrBlank()) "🩸 *Tipo de Sangre:* ${alert.bloodTypeNeeded}\n" else "") +
                            "📋 *Protocolo activado:* ${emergencyType.actionProtocol}\n" +
                            "👨‍⚕️ *Especialista:* ${emergencyType.recommendedSpecialist}\n" +
                            "📝 *Detalles:* $details\n\n" +
                            "📲 Contacto: ${alert.reporterPhone}. Asistencia y despeje vial prioritario.",
                    category = NoticeCategory.AVISO_OFICIAL,
                    priority = NoticePriority.URGENTE,
                    isPinned = true
                )
            }

            // Emitir Notificación del Sistema con sonido y alerta en barra
            GestorNotificacionesApp.notificarAlertaSOS(
                remitente = "$rolTexto ${alert.reporterName}",
                ubicacion = "$finalDesc (${emergencyType.label})"
            )

            // Activar Telemetría GPS en el Radar del Mapa marcando estado SOS
            try {
                val appCtx = getApplication<android.app.Application>()
                val myUid = member?.id?.toString() ?: "sos_${System.currentTimeMillis()}"
                com.example.radar.TelemetriaGps.activar(
                    contexto = appCtx,
                    userId = myUid,
                    nombre = alert.reporterName,
                    rango = member?.role?.displayName ?: (if (esCopiloto) "Copiloto" else "Piloto"),
                    avatarUrl = member?.avatarInitials ?: "",
                    alertaSos = emergencyType.name
                )
            } catch (e: Exception) {
                Log.w(TAG_SOS, "No se pudo activar telemetría SOS: ${e.message}")
            }

            // Sincronizar Mapa TX inmediatamente con el nuevo punto SOS
            try {
                val appCtx = getApplication<android.app.Application>()
                val db = com.example.data.local.AppDatabase.getDatabase(appCtx, viewModelScope)
                val pubs = db.publicationDao().getAllPublications().first()
                val events = db.calendarDao().getAllEvents().first()
                val allAlerts = db.emergencyDao().getAllAlerts().first()
                val activeAlerts = (allAlerts + alert).filter { it.status != EmergencyStatus.RESUELTA }
                com.example.radar.GestorRadar.sincronizarEventosEnMapa(pubs, events, activeAlerts)
            } catch (e: Exception) {
                Log.w(TAG_SOS, "Error actualizando mapa tras SOS: ${e.message}")
            }
        }
    }

    fun updateAlertStatus(alert: EmergencyAlert, newStatus: EmergencyStatus, notes: String) {
        viewModelScope.launch {
            val updatedAlert = alert.copy(status = newStatus, respondersNotes = notes)
            repository.updateAlert(updatedAlert)
            Log.i(TAG_SOS, "🚨 Alerta SOS ${alert.id} actualizada a $newStatus")

            val gradoInfo = "[${alert.emergencyType.levelTag} - ${alert.emergencyType.label}]"

            if (newStatus == EmergencyStatus.RESUELTA) {
                val appCtx = getApplication<android.app.Application>()

                // 1. Limpiar estado SOS de telemetría GPS
                try {
                    com.example.radar.TelemetriaGps.limpiarAlertaSos(appCtx, alert.memberNumber)
                    currentMember.value?.id?.toString()?.let {
                        com.example.radar.TelemetriaGps.limpiarAlertaSos(appCtx, it)
                    }
                } catch (e: Exception) {
                    Log.w(TAG_SOS, "Error limpiando telemetría SOS: ${e.message}")
                }

                // 2. Desanclar avisos SOS previos del Muro/Feed
                try {
                    val pubs = repository.allPublications.first()
                    pubs.filter { it.isPinned && it.title.contains("SOS") && (it.title.contains(alert.reporterName) || it.content.contains(alert.memberNumber)) }
                        .forEach { p ->
                            repository.updatePublication(p.copy(isPinned = false))
                        }
                } catch (e: Exception) {
                    Log.w(TAG_SOS, "Error desanclando aviso de emergencia previo: ${e.message}")
                }

                // 3. Si era emergencia crítica de Muro, publicar aviso de situación solventada
                if (alert.emergencyType.sePublicaEnMuro) {
                    postSystemNotice(
                        title = "✅ SOS VIAL SOLVENTADO: ${alert.reporterName} a salvo",
                        content = "Se informa a la comunidad motera que la alerta vial $gradoInfo en ${alert.locationDescription} ha sido totalmente SOLVENTADA.\n\n" +
                                "👤 Piloto: ${alert.reporterName} (${alert.memberNumber})\n" +
                                "🏍️ Vehículo: ${alert.bikeDetails}\n" +
                                "📋 Bitácora: ${notes.ifBlank { "Hermano motero asistido en sitio y fuera de peligro." }}\n\n" +
                                "El punto de auxilio ha sido retirado del Mapa TX. ¡Gracias a todos los que acudieron y colaboraron!",
                        category = NoticeCategory.COMUNICADO,
                        priority = NoticePriority.NORMAL,
                        isPinned = false
                    )
                }

                // 4. Enviar reporte de resolución a Chat General, Auxilio y Directiva
                val resMsg = "✅ EMERGENCIA SOLVENTADA $gradoInfo:\n" +
                        "La alerta de ${alert.emergencyType.label} emitida por ${alert.reporterName} (${alert.memberNumber}) en ${alert.locationDescription} ha sido marcada como RESUELTA.\n" +
                        "📋 Nivel: ${alert.emergencyType.levelNumber} (${alert.emergencyType.levelName})\n" +
                        "🏍️ Vehículo: ${alert.bikeDetails}\n" +
                        "📝 Bitácora: ${notes.ifBlank { "Hermano asistido y fuera de peligro." }}\n" +
                        "🤝 El punto de auxilio ha sido retirado del Mapa TX. ¡Gracias a todos por la hermandad!"

                sendChatMessage("GENERAL", resMsg, isRadioCallout = false)
                sendChatMessage("AUXILIO", resMsg, isRadioCallout = false)

                val directivaResMsg = "✅ [SOS RESUELTO] $gradoInfo: Emergencia de ${alert.reporterName} (${alert.memberNumber}) en ${alert.locationDescription} cerrada en el sistema.\nBitácora: ${notes.ifBlank { "Atendido satisfactoriamente." }}"
                sendChatMessage("DIRECTIVA", directivaResMsg, isRadioCallout = false)
                notifyDirectivaChannel(
                    titulo = "EMERGENCIA RESUELTA $gradoInfo",
                    detalle = "✅ Alerta de ${alert.reporterName} en ${alert.locationDescription} cerrada.\nBitácora: ${notes.ifBlank { "Atendido satisfactoriamente." }}",
                    tipo = "SOS_RESUELTO"
                )

                // 5. Actualizar Mapa TX para remover inmediatamente el punto de emergencia
                try {
                    val db = com.example.data.local.AppDatabase.getDatabase(appCtx, viewModelScope)
                    val pubs = db.publicationDao().getAllPublications().first()
                    val events = db.calendarDao().getAllEvents().first()
                    val allAlerts = db.emergencyDao().getAllAlerts().first()
                    val activeAlerts = allAlerts.filter { it.id != alert.id && it.status != EmergencyStatus.RESUELTA }
                    com.example.radar.GestorRadar.sincronizarEventosEnMapa(pubs, events, activeAlerts)
                } catch (e: Exception) {
                    Log.w(TAG_SOS, "Error actualizando capa mapa tras resolver SOS: ${e.message}")
                }
            } else if (newStatus == EmergencyStatus.ATENDIDA || newStatus == EmergencyStatus.EN_CAMINO) {
                // Notificar cambio de estado a brigadas y directiva
                val avisoEstado = "ℹ️ [ESTADO SOS] $gradoInfo: La emergencia de ${alert.reporterName} ahora está: ${newStatus.label}.\nNotas: ${notes.ifBlank { "En proceso de auxilio." }}"
                sendChatMessage("AUXILIO", avisoEstado, isRadioCallout = false)
                sendChatMessage("DIRECTIVA", avisoEstado, isRadioCallout = false)
            }
        }
    }

    // Role Configs (Editable by President)
    val roleConfigs: StateFlow<List<RoleConfig>> = repository.allRoleConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateRoleConfig(roleKey: String, customTitle: String, roleDuties: String) {
        viewModelScope.launch {
            val existing = roleConfigs.value.find { it.roleKey == roleKey }
            val updater = currentMember.value?.fullName ?: "Presidente Nacional"
            val config = (existing?.copy(
                customTitle = customTitle,
                roleDuties = roleDuties,
                lastUpdatedBy = updater,
                lastUpdatedAt = System.currentTimeMillis()
            )) ?: RoleConfig(
                roleKey = roleKey,
                customTitle = customTitle,
                roleDuties = roleDuties,
                lastUpdatedBy = updater,
                lastUpdatedAt = System.currentTimeMillis()
            )
            repository.insertRoleConfig(config)
        }
    }

    // Notice Comments (Pilots replying to official announcements)
    val allNoticeComments: StateFlow<List<NoticeComment>> = repository.allComments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNoticeComment(publicationId: Long, content: String) {
        viewModelScope.launch {
            val member = currentMember.value ?: return@launch
            val comment = NoticeComment(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                publicationId = publicationId,
                memberId = member.id,
                authorName = member.fullName,
                authorNickname = member.nickname,
                authorMemberNumber = member.memberNumber,
                authorRole = member.role,
                authorInitials = member.avatarInitials,
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )
            repository.insertComment(comment)
        }
    }

    fun deleteNoticeComment(commentId: Long) {
        viewModelScope.launch {
            repository.deleteComment(commentId)
        }
    }

    // ═══════════════════════════════════════════════
    // MÓDULO DE GRUPOS PRIVADOS (Sincronizado con Firestore y Room)
    // ═══════════════════════════════════════════════

    val allPrivateGroups: StateFlow<List<PrivateGroup>> = repository.allPrivateGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myPrivateGroups: StateFlow<List<PrivateGroup>> = combine(allPrivateGroups, currentMember) { groups, member ->
        val memberId = member?.id ?: 0L
        if (memberId <= 0L) emptyList()
        else groups.filter { it.creatorMemberId == memberId || it.memberIds.contains(memberId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myCreatedGroupsCount: StateFlow<Int> = combine(allPrivateGroups, currentMember) { groups, member ->
        val memberId = member?.id ?: 0L
        if (memberId <= 0L) 0
        else groups.count { it.creatorMemberId == memberId && !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun createPrivateGroup(
        name: String,
        description: String,
        selectedMemberIds: List<Long>,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val member = currentMember.value
        if (member == null) {
            onResult(false, "Debes iniciar sesión para crear un grupo")
            return
        }

        if (name.isBlank()) {
            onResult(false, "El nombre del grupo no puede estar vacío")
            return
        }

        val createdCount = allPrivateGroups.value.count { it.creatorMemberId == member.id && !it.isDeleted }
        if (createdCount >= 2) {
            onResult(false, "Límite alcanzado: cada usuario puede crear máximo 2 salas privadas.")
            return
        }

        val now = System.currentTimeMillis()
        val groupId = "GRP_${now}_${member.id}"
        val finalMembers = (selectedMemberIds + member.id).distinct()

        val group = PrivateGroup(
            id = groupId,
            name = name.trim(),
            description = description.trim(),
            creatorMemberId = member.id,
            creatorName = member.fullName,
            creatorNickname = member.nickname.ifBlank { member.fullName },
            memberIds = finalMembers,
            adminIds = listOf(member.id),
            createdAt = now,
            lastMessageText = "Sala creada por ${member.nickname.ifBlank { member.fullName }}",
            lastMessageTimestamp = now
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertPrivateGroup(group)
                repository.ensureChatChannelListener(groupId)

                // Mensaje de bienvenida
                val roleCfg = roleConfigs.value.find { it.roleKey == member.role.name }
                val welcomeMsg = ChatMessage(
                    id = System.currentTimeMillis(),
                    channelId = groupId,
                    senderMemberId = member.id,
                    senderName = member.fullName,
                    senderNickname = member.nickname.ifBlank { member.fullName },
                    senderMemberNumber = member.memberNumber,
                    senderRole = member.role,
                    senderCustomRoleTitle = roleCfg?.customTitle ?: member.role.displayName,
                    senderInitials = member.avatarInitials,
                    senderPhotoUrl = member.profilePhotoUri,
                    messageText = "🏁 Sala privada \"$name\" creada por ${member.nickname.ifBlank { member.fullName }}. ¡Bienvenidos hermanos moteros!",
                    timestamp = System.currentTimeMillis()
                )
                repository.insertChatMessage(welcomeMsg)

                // Notificar a invitados
                for (invitedId in selectedMemberIds) {
                    if (invitedId != member.id) {
                        GestorNotificacionesApp.notificarInvitacionGrupoPrivado(name, member.nickname.ifBlank { member.fullName }, groupId)
                    }
                }

                // Notificar a la directiva
                GestorNotificacionesApp.notificarGrupoPrivadoDirectiva(name, member.fullName, groupId)

                withContext(Dispatchers.Main) {
                    selectChatChannel(groupId)
                    onResult(true, "Grupo privado creado exitosamente")
                }
            } catch (e: Exception) {
                Log.e("GRUPO_PRIVADO", "Error creando grupo privado: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Error al crear grupo: ${e.message}")
                }
            }
        }
    }

    fun addMembersToPrivateGroup(groupId: String, newMemberIds: List<Long>) {
        if (newMemberIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val group = allPrivateGroups.value.find { it.id == groupId } ?: return@launch
            val updatedMembers = (group.memberIds + newMemberIds).distinct()
            val updated = group.copy(memberIds = updatedMembers)
            repository.updatePrivateGroup(updated)

            val member = currentMember.value
            for (invitedId in newMemberIds) {
                GestorNotificacionesApp.notificarInvitacionGrupoPrivado(group.name, member?.nickname ?: "Un administrador", groupId)
            }
        }
    }

    fun removeMemberFromPrivateGroup(groupId: String, memberIdToRemove: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val group = allPrivateGroups.value.find { it.id == groupId } ?: return@launch
            val updatedMembers = group.memberIds.filter { it != memberIdToRemove }
            val updatedAdmins = group.adminIds.filter { it != memberIdToRemove }
            val updated = group.copy(memberIds = updatedMembers, adminIds = updatedAdmins)
            repository.updatePrivateGroup(updated)
        }
    }

    fun leavePrivateGroup(groupId: String) {
        val memberId = currentMember.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val group = allPrivateGroups.value.find { it.id == groupId } ?: return@launch
            val updatedMembers = group.memberIds.filter { it != memberId }
            val updatedAdmins = group.adminIds.filter { it != memberId }
            val updated = group.copy(memberIds = updatedMembers, adminIds = updatedAdmins)
            repository.updatePrivateGroup(updated)
            if (_selectedChatChannel.value == groupId) {
                withContext(Dispatchers.Main) {
                    _selectedChatChannel.value = "GENERAL"
                }
            }
        }
    }

    fun deletePrivateGroup(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePrivateGroup(groupId)
            repository.clearChatChannel(groupId)
            if (_selectedChatChannel.value == groupId) {
                withContext(Dispatchers.Main) {
                    _selectedChatChannel.value = "GENERAL"
                }
            }
        }
    }

    fun toggleBlockPrivateGroup(groupId: String, isBlocked: Boolean, reason: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val member = currentMember.value
            val blockedByName = member?.fullName ?: "Directiva Nacional"
            repository.toggleBlockPrivateGroup(groupId, isBlocked, reason, blockedByName)

            val actionText = if (isBlocked) "BLOQUEADA POR LA DIRECTIVA" else "DESBLOQUEADA"
            val systemMsg = "🛡️ ATENCIÓN: Esta sala privada ha sido $actionText por $blockedByName.${if (isBlocked && reason.isNotBlank()) " Motivo: $reason" else ""}"
            val chatMsg = ChatMessage(
                id = System.currentTimeMillis(),
                channelId = groupId,
                senderMemberId = member?.id ?: 2,
                senderName = "Directiva & Gobernanza",
                senderNickname = "Directiva",
                senderMemberNumber = "TX-DIR",
                senderRole = MemberRole.DIRECTIVA,
                senderCustomRoleTitle = "Supervisión Directiva",
                senderInitials = "TX",
                messageText = systemMsg,
                isRadioCallout = true,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(chatMsg)
        }
    }

    // Club Chat
    val allChatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🛡️ Canal Directiva Estable (Evita parpadeos y re-suscripciones cíclicas)
    val directivaChatMessages: StateFlow<List<ChatMessage>> = repository.getMessagesForChannel("DIRECTIVA")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMessagesForChannel(channelId: String) = repository.getMessagesForChannel(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Envía automáticamente una alerta del sistema al canal DIRECTIVA
     * cada vez que un usuario normal solicita algo (acceso, auxilio, préstamo, etc.).
     */
    fun notifyDirectivaChannel(titulo: String, detalle: String, tipo: String = "INFO") {
        viewModelScope.launch {
            val emoji = when (tipo) {
                "SOLICITUD" -> "📥"
                "SOS" -> "🚨"
                "MERCADO" -> "🛒"
                "PRESTAMO" -> "📦"
                "BAJA" -> "⚠️"
                "RODADA" -> "🏍️"
                else -> "📢"
            }
            val mensaje = "$emoji [SISTEMA - $titulo]\n$detalle"
            val chatMsg = ChatMessage(
                id = System.currentTimeMillis(),
                channelId = "DIRECTIVA",
                senderMemberId = 0,
                senderName = "SISTEMA TX",
                senderNickname = "Bot Directiva",
                senderMemberNumber = "TX-BOT",
                senderRole = MemberRole.DIRECTIVA,
                senderCustomRoleTitle = "Alerta de Gobernanza",
                senderInitials = "TX",
                messageText = mensaje,
                isRadioCallout = true,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(chatMsg)
        }
    }

    private val _selectedChatChannel = MutableStateFlow("GENERAL")
    val selectedChatChannel: StateFlow<String> = _selectedChatChannel.asStateFlow()

    fun selectChatChannel(channelId: String) {
        _selectedChatChannel.value = channelId
        repository.ensureChatChannelListener(channelId)
        markChannelMessagesAsRead(channelId)
    }

    fun sendChatMessage(
        channelId: String,
        text: String,
        isRadioCallout: Boolean = false,
        replyToMessageId: Long? = null,
        replyToSenderName: String? = null,
        replyToText: String? = null
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val member = currentMember.value
            val roleCfg = roleConfigs.value.find { it.roleKey == (member?.role?.name ?: "MIEMBRO_ACTIVO") }
            val photoToUse = member?.profilePhotoUri?.takeIf { it.isNotBlank() }
                ?: com.example.ui.preferences.PreferenciasApp.carnetFotoPerfil.takeIf { it.isNotBlank() }
                ?: com.example.ui.preferences.PreferenciasApp.carnetGooglePhotoUrl

            val message = ChatMessage(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                channelId = channelId,
                senderMemberId = member?.id ?: 1,
                senderName = member?.fullName ?: "Piloto TX",
                senderNickname = member?.nickname ?: "Piloto",
                senderMemberNumber = member?.memberNumber ?: "TX-000",
                senderRole = member?.role ?: MemberRole.MIEMBRO_ACTIVO,
                senderCustomRoleTitle = roleCfg?.customTitle ?: member?.role?.displayName,
                senderInitials = member?.avatarInitials ?: "TX",
                senderPhotoUrl = photoToUse, // 📸 Foto configurada (Google o Manual)
                messageText = text.trim(),
                isRadioCallout = isRadioCallout,
                replyToMessageId = replyToMessageId,
                replyToSenderName = replyToSenderName,
                replyToText = replyToText,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(message)
        }
    }

    fun sendLocationMessage(channelId: String, coordinates: String) {
        if (coordinates.isBlank()) return
        viewModelScope.launch {
            val member = currentMember.value
            val roleCfg = roleConfigs.value.find { it.roleKey == (member?.role?.name ?: "MIEMBRO_ACTIVO") }
            val message = ChatMessage(
                id = System.currentTimeMillis(),
                channelId = channelId,
                senderMemberId = member?.id ?: 1,
                senderName = member?.fullName ?: "Piloto TX",
                senderNickname = member?.nickname ?: "Piloto",
                senderMemberNumber = member?.memberNumber ?: "TX-000",
                senderRole = member?.role ?: MemberRole.MIEMBRO_ACTIVO,
                senderCustomRoleTitle = roleCfg?.customTitle ?: member?.role?.displayName,
                senderInitials = member?.avatarInitials ?: "TX",
                senderPhotoUrl = member?.profilePhotoUri,
                messageText = coordinates.trim(),
                messageType = MessageType.LOCATION,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(message)
        }
    }

    fun sendStickerMessage(channelId: String, stickerFile: java.io.File) {
        viewModelScope.launch {
            val member = currentMember.value
            val roleCfg = roleConfigs.value.find { it.roleKey == (member?.role?.name ?: "MIEMBRO_ACTIVO") }
            val messageId = System.currentTimeMillis()
            
            // 1. Crear e insertar mensaje local de inmediato
            val localMessage = ChatMessage(
                id = messageId,
                channelId = channelId,
                senderMemberId = member?.id ?: 1,
                senderName = member?.fullName ?: "Piloto TX",
                senderNickname = member?.nickname ?: "Piloto",
                senderMemberNumber = member?.memberNumber ?: "TX-000",
                senderRole = member?.role ?: MemberRole.MIEMBRO_ACTIVO,
                senderCustomRoleTitle = roleCfg?.customTitle ?: member?.role?.displayName,
                senderInitials = member?.avatarInitials ?: "TX",
                senderPhotoUrl = member?.profilePhotoUri,
                messageText = "Sticker",
                messageType = MessageType.STICKER,
                stickerFileName = stickerFile.name,
                stickerFilePath = stickerFile.absolutePath,
                localMediaPath = stickerFile.absolutePath,
                syncStatus = "PENDING",
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(localMessage)

            try {
                // 2. Intentar subida a Firebase Storage
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                val fileUri = android.net.Uri.fromFile(stickerFile)
                val stickerRef = storageRef.child("chat_stickers/${java.util.UUID.randomUUID()}_${stickerFile.name}")
                
                stickerRef.putFile(fileUri).await()
                val downloadUrl = stickerRef.downloadUrl.await().toString()
                
                val updatedMessage = localMessage.copy(
                    stickerFilePath = downloadUrl,
                    syncStatus = "SENT"
                )
                repository.insertChatMessage(updatedMessage)
            } catch (e: Exception) {
                Log.w("TeamTxViewModel", "Subida de sticker pendiente para sincronización diferida: ${e.message}")
            }
        }
    }

    fun sendAudioMessage(channelId: String, audioFile: java.io.File, durationSeconds: Int, transcriptionText: String? = null) {
        viewModelScope.launch {
            val member = currentMember.value
            val roleCfg = roleConfigs.value.find { it.roleKey == (member?.role?.name ?: "MIEMBRO_ACTIVO") }
            val messageId = System.currentTimeMillis()

            val textoMensaje = if (!transcriptionText.isNullOrBlank()) transcriptionText else "🎤 Nota de voz (${durationSeconds}s)"

            // 1. Crear e insertar mensaje local de inmediato
            val localMessage = ChatMessage(
                id = messageId,
                channelId = channelId,
                senderMemberId = member?.id ?: 1,
                senderName = member?.fullName ?: "Piloto TX",
                senderNickname = member?.nickname ?: "Piloto",
                senderMemberNumber = member?.memberNumber ?: "TX-000",
                senderRole = member?.role ?: MemberRole.MIEMBRO_ACTIVO,
                senderCustomRoleTitle = roleCfg?.customTitle ?: member?.role?.displayName,
                senderInitials = member?.avatarInitials ?: "TX",
                senderPhotoUrl = member?.profilePhotoUri,
                messageText = textoMensaje,
                messageType = MessageType.AUDIO,
                audioUrl = audioFile.absolutePath,
                localMediaPath = audioFile.absolutePath,
                audioDurationSeconds = durationSeconds,
                syncStatus = "PENDING",
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(localMessage)

            try {
                // 2. Intentar subida a Firebase Storage
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                val fileUri = android.net.Uri.fromFile(audioFile)
                val audioRef = storageRef.child("audios_chat/${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.m4a")

                audioRef.putFile(fileUri).await()
                val downloadUrl = audioRef.downloadUrl.await().toString()

                val updatedMessage = localMessage.copy(
                    audioUrl = downloadUrl,
                    syncStatus = "SENT"
                )
                repository.insertChatMessage(updatedMessage)
            } catch (e: Exception) {
                Log.w("TeamTxViewModel", "Subida de audio pendiente para sincronización diferida: ${e.message}")
            }
        }
    }

    fun toggleMessageReaction(messageId: Long, emoji: String) {
        val memberId = currentMember.value?.id ?: return
        viewModelScope.launch {
            val allMsgs = repository.allChatMessages.first()
            val msg = allMsgs.find { it.id == messageId } ?: return@launch
            val updatedReactions = msg.toggleReaction(emoji, memberId)
            val updatedMsg = msg.copy(reactions = updatedReactions)
            repository.updateChatMessage(updatedMsg)
            repository.updateMessageReactions(messageId, msg.channelId, updatedReactions)
        }
    }

    fun deleteChatMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteChatMessage(messageId)
        }
    }

    fun deleteChatMessageForMe(messageId: Long) {
        val memberId = currentMember.value?.id ?: return
        viewModelScope.launch {
            val allMsgs = repository.allChatMessages.first()
            val msg = allMsgs.find { it.id == messageId } ?: return@launch
            val currentDeleted = msg.deletedForMemberIds.split(",").filter { it.isNotBlank() }.toMutableList()
            if (!currentDeleted.contains(memberId.toString())) {
                currentDeleted.add(memberId.toString())
                val updated = msg.copy(deletedForMemberIds = currentDeleted.joinToString(","))
                repository.updateChatMessage(updated)
            }
        }
    }

    fun clearGeneralChat() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearChatChannel("GENERAL")
        }
    }

    fun toggleChatEnabled(enabled: Boolean) {
        _isChatEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("app_settings")
                    .document("global")
                    .set(mapOf("chatEnabled" to enabled), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                android.util.Log.i("CHAT_CONTROL", "Estado chat global actualizado: enabled=$enabled")
            } catch (e: Exception) {
                android.util.Log.e("CHAT_CONTROL", "Error actualizando chatEnabled en Firestore", e)
            }
        }
    }

    fun markMessageAsRead(message: ChatMessage) {
        val memberId = currentMember.value?.id ?: return
        if (message.senderMemberId != memberId && !message.readBy.contains(memberId)) {
            val updatedReadBy = message.readBy.toMutableList().apply { add(memberId) }
            val updatedMessage = message.copy(readBy = updatedReadBy)
            viewModelScope.launch {
                repository.insertChatMessage(updatedMessage)
            }
        }
    }

    // ═══════════════════════════════════════════════
    // CONTEO DE MENSAJES NO LEÍDOS POR CANAL Y GLOBALES
    // ═══════════════════════════════════════════════

    val unreadChatCountsByChannel: StateFlow<Map<String, Int>> = combine(
        allChatMessages,
        currentMember
    ) { messages, member ->
        val memberId = member?.id ?: 0L
        if (memberId <= 0L) {
            emptyMap()
        } else {
            messages.filter { it.senderMemberId != memberId && !it.readBy.contains(memberId) }
                .groupBy { it.channelId }
                .mapValues { it.value.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val unreadGeneralChatCount: StateFlow<Int> = unreadChatCountsByChannel
        .map { it["GENERAL"] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadPublicChatCount: StateFlow<Int> = unreadChatCountsByChannel
        .map { counts ->
            (counts["GENERAL"] ?: 0) + (counts["RODADAS"] ?: 0) + (counts["MECANICA_AUXILIO"] ?: 0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadDirectivaChatCount: StateFlow<Int> = unreadChatCountsByChannel
        .map { it["DIRECTIVA"] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markChannelMessagesAsRead(channelId: String) {
        val memberId = currentMember.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val messages = allChatMessages.value
            val unreadInChannel = messages.filter {
                it.channelId == channelId && it.senderMemberId != memberId && !it.readBy.contains(memberId)
            }
            if (unreadInChannel.isNotEmpty()) {
                val updatedMessages = unreadInChannel.map { msg ->
                    msg.copy(readBy = msg.readBy + memberId)
                }
                repository.markChatMessagesAsRead(updatedMessages)
            }
        }
    }

    // ---------------- AUTHENTICATION & GATEKEEPER (Declaraciones movidas al encabezado de la clase) ---------------- //

    private fun saveSession(memberId: Long, email: String?, firebaseUid: String?) {
        prefs.edit()
            .putLong("PREF_LOGGED_IN_MEMBER_ID", memberId)
            .putString("PREF_LOGGED_IN_EMAIL", email)
            .putString("PREF_LOGGED_IN_FIREBASE_UID", firebaseUid)
            .apply()
    }

    /**
     * Sincronizacion bidireccional en tiempo real del perfil del usuario logueado.
     * Nube -> Room -> UI (escucha) y Room -> Nube (subida automatica al editar).
     */
    private fun iniciarSincronizacionDePerfil(uid: String?) {
        if (uid == null) return
        perfilListener?.remove()
        perfilSyncJob?.cancel()

        val app = getApplication<Application>()
        perfilListener = PerfilNube.escucharPerfil(
            contexto = app,
            uid = uid,
            alDetectarDispositivoDistinto = { mensaje ->
                viewModelScope.launch(Dispatchers.Main) {
                    _sesionDesplazadaPorOtroDispositivo.value = false
                    Log.w("PERFIL_SYNC", "🚨 $mensaje: Sesión iniciada/reclamada en otro dispositivo. Desvinculando y cerrando sesión local automáticamente.")
                    logout()
                }
            },
            alSerBloqueado = {
                viewModelScope.launch(Dispatchers.Main) {
                    Log.w("PERFIL_SYNC", "🚨 Bloqueo detectado: Cerrando sesión forzosamente.")
                    logout()
                }
            },
            alCambiar = { remoto, _ ->
                viewModelScope.launch(Dispatchers.IO) {
                    val mid = _currentMemberId.value
                    val local = repository.allMembers.first().find { it.id == mid }
                    if (local != null && local.firebaseUid == uid) {
                        val fusionado = mezclarRemotoEnLocal(local, remoto)
                        if (fusionado != local) {
                            repository.updateMember(fusionado.copy(id = local.id, firebaseUid = uid))
                            Log.d("PERFIL_SYNC", "Perfil fusionado desde la nube en vivo")
                        }
                    }
                }
            }
        )

        perfilSyncJob = viewModelScope.launch(Dispatchers.IO) {
            combine(repository.allMembers, _currentMemberId) { miembros, id ->
                miembros.find { it.id == id }
            }.distinctUntilChanged().collect { yo ->
                if (yo != null && yo.firebaseUid == uid) {
                    val hash = yo.copy(id = 0L, lastActiveTimestamp = 0L).hashCode()
                    if (hash != ultimoHashPerfilSubido) {
                        ultimoHashPerfilSubido = hash
                        if (PerfilNube.subirPerfil(uid, yo)) {
                            Log.d("PERFIL_SYNC", "Perfil subido a la nube (usuarios/$uid)")
                        }
                    }
                }
            }
        }
    }

    private fun detenerSincronizacionDePerfil() {
        perfilListener?.remove()
        perfilListener = null
        perfilSyncJob?.cancel()
        perfilSyncJob = null
        ultimoHashPerfilSubido = 0
    }

    private fun clearSession() {
        prefs.edit()
            .remove("PREF_LOGGED_IN_MEMBER_ID")
            .remove("PREF_LOGGED_IN_EMAIL")
            .remove("PREF_LOGGED_IN_FIREBASE_UID")
            .apply()
    }

    private fun checkAndRestoreSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val savedMemberId = prefs.getLong("PREF_LOGGED_IN_MEMBER_ID", -1L)
            val savedEmail = prefs.getString("PREF_LOGGED_IN_EMAIL", null) ?: firebaseUser?.email
            val savedUid = prefs.getString("PREF_LOGGED_IN_FIREBASE_UID", null) ?: firebaseUser?.uid

            // 1. Check local DB first
            val localMembers = repository.allMembers.first()
            var matched: MemberProfile? = null

            if (savedUid != null) {
                matched = localMembers.find { it.firebaseUid == savedUid }
            }
            if (matched == null && !savedEmail.isNullOrBlank()) {
                val cleanSaved = savedEmail.trim().lowercase()
                matched = localMembers.find { it.email.equals(cleanSaved, ignoreCase = true) }
            }
            if (matched == null && savedMemberId > 0) {
                matched = localMembers.find { it.id == savedMemberId }
            }

            // 1.5 Si no esta en Room, intentar descargarlo de la nube por UID (usuarios/{uid})
            if (matched == null && savedUid != null) {
                val remoto = PerfilNube.descargarPerfil(savedUid)
                if (remoto != null) {
                    val nuevoId = repository.insertMemberAndGetId(
                        remoto.copy(firebaseUid = savedUid, email = remoto.email ?: savedEmail)
                    )
                    matched = remoto.copy(id = nuevoId)
                    Log.d("PERFIL_SYNC", "Perfil restaurado desde la nube por UID")
                }
            }

            // 2. If not found locally, query remote Firestore
            if (matched == null && (!savedEmail.isNullOrBlank() || savedUid != null)) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val cleanQuery = savedEmail?.trim()?.lowercase()
                    if (cleanQuery != null) {
                        val snapshot = firestore.collection("members")
                            .whereEqualTo("email", cleanQuery)
                            .get()
                            .await()
                        if (!snapshot.isEmpty) {
                            val remoteDoc = snapshot.documents.first()
                            val profile = remoteDoc.toObject(MemberProfile::class.java)?.copy(
                                id = remoteDoc.id.toLongOrNull() ?: System.currentTimeMillis()
                            )
                            if (profile != null) {
                                repository.insertMember(profile)
                                matched = profile
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TeamTxViewModel", "Error restoring session from Firestore: ${e.message}")
                }
            }

            // 3. If matched member found and session data exists, auto-login!
            if (matched != null) {
                var finalMember = matched

                // Validar que la vinculación del correo siga existiendo en la nube (evita recordar vínculos del pasado)
                val emailLimpio = (finalMember.email ?: "").trim().lowercase()
                val esDevBypass = emailLimpio == "eduardo.androide.em@gmail.com" || emailLimpio == "eduardo.jose.marquez.matos@gmail.com"

                if (!esDevBypass && !finalMember.email.isNullOrBlank() && finalMember.email?.endsWith("@teamtx.com") != true) {
                    val vinculo = PerfilNube.consultarVinculacionPorEmail(finalMember.email ?: "")
                    if (vinculo == null || !vinculo.memberNumber.equals(finalMember.memberNumber, ignoreCase = true)) {
                        Log.i("RESTORE_SESSION", "El vínculo del correo ${finalMember.email} ya no existe en Firestore. Limpiando perfil local desde cero...")
                        finalMember = finalMember.copy(
                            email = "", 
                            firebaseUid = null,
                            profilePhotoUri = null,
                            bikePhotoUri = null,
                            licenseImageUri = null,
                            medicalCertImageUri = null,
                            bikeRegImageUri = null,
                            insuranceImageUri = null,
                            phone = "",
                            cedulaDni = "",
                            bikePlate = "",
                            emergencyContactName = "",
                            emergencyContactPhone = "",
                            medicalNotes = "",
                            copilotName = null,
                            copilotRelation = null
                        )
                        com.example.ui.preferences.PreferenciasApp.carnetGooglePhotoUrl = null
                        repository.updateMember(finalMember)
                        clearSession()
                    }
                } else if (!esDevBypass && finalMember.email?.endsWith("@teamtx.com") == true) {
                    finalMember = finalMember.copy(email = "")
                    repository.updateMember(finalMember)
                }

                val googlePhoto = firebaseUser?.photoUrl?.toString()
                if (finalMember.profilePhotoUri.isNullOrBlank() && !googlePhoto.isNullOrBlank()) {
                    finalMember = finalMember.copy(profilePhotoUri = googlePhoto)
                    repository.updateMember(finalMember)
                }
                _currentMemberId.value = finalMember.id
                _isAuthenticated.value = true

                // 🔐 Consultar usuarioEstado actualizado desde Firestore (usuarios/{uid})
                val uidVerificar = finalMember.firebaseUid
                if (!uidVerificar.isNullOrBlank()) {
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        var docVerif = firestore.collection("usuarios").document(uidVerificar).get().await()
                        if (!docVerif.exists()) {
                            docVerif = firestore.collection("users").document(uidVerificar).get().await()
                        }
                        if (docVerif.exists()) {
                            val estadoRemoto = docVerif.getString("usuarioEstado") ?: docVerif.getString("estado") ?: "ACTIVO"
                            _usuarioEstado.value = estadoRemoto
                        } else {
                            val esAspirante = finalMember.role == MemberRole.ASPIRANTE || finalMember.role == MemberRole.INVITADO
                            _usuarioEstado.value = if (esDevBypass || !esAspirante) "ACTIVO" else "PENDIENTE"
                        }
                    } catch (e: Exception) {
                        _usuarioEstado.value = "ACTIVO"
                    }
                } else {
                    _usuarioEstado.value = "ACTIVO"
                }

                if (finalMember.role == MemberRole.PRESIDENTE || finalMember.role.canManageApp || finalMember.isDirectiva) {
                    _isDirectivaMode.value = true
                    if (finalMember.role == MemberRole.PRESIDENTE) _isLeaderSuperAdmin.value = true
                }
                saveSession(finalMember.id, finalMember.email?.ifBlank { null }, finalMember.firebaseUid)
                if (!finalMember.firebaseUid.isNullOrBlank()) {
                    iniciarSincronizacionDePerfil(finalMember.firebaseUid)
                }
                
                // Asegurar sesión en Firebase si se restauró localmente pero Auth no está activo
                if (firebaseUser == null) {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                        .addOnSuccessListener { Log.d("TeamTxViewModel", "Sesión restaurada: Auth anónima activada") }
                        .addOnFailureListener { e -> Log.e("TeamTxViewModel", "Error en Auth anónima al restaurar sesión", e) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            // 🛡️ PASO 0: RESPALDO FINAL OBLIGATORIO EN FIREBASE ANTES DE CERRAR SESIÓN
            val current = currentMember.value
            if (current != null) {
                try {
                    // Respaldo en colección members de Firestore
                    FirebaseFirestore.getInstance().collection("members")
                        .document(current.id.toString())
                        .set(current)
                        .await()

                    // Respaldo en usuarios/{uid} si está vinculado con Google
                    val uid = current.firebaseUid
                    if (!uid.isNullOrBlank()) {
                        PerfilNube.subirPerfil(uid, current)
                    }
                    Log.i("LOGOUT_TX", "💾 Respaldo seguro en Firebase completado para: ${current.memberNumber} (${current.fullName})")
                } catch (e: Exception) {
                    Log.e("LOGOUT_TX", "Error en respaldo preventivo previo al logout: ${e.message}")
                }
            }

            val app = getApplication<Application>()
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                com.aistudio.teamtxvzla.nube.AutenticacionGoogle.cerrarSesionGoogle(app)
                com.aistudio.teamtxvzla.nube.AutenticacionGoogle.revocarAcceso(app)
            } catch (_: Exception) {}
            detenerSincronizacionDePerfil()
            clearSession()

            // 🛡️ LIMPIEZA PROFUNDA DE DATOS LOCALES: No dejar rastros para el siguiente usuario
            repository.nukeEverything()

            // 1. Limpiar fotos y preferencias de carnet
            com.example.ui.preferences.PreferenciasApp.carnetGooglePhotoUrl = null
            com.example.ui.preferences.PreferenciasApp.carnetTipoFoto = "LOCAL"

            // 2. Limpiar preferencias de radar (avatar y alias)
            try {
                app.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE).edit().clear().apply()
                app.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {}

            // 3. Resetear perfil local en Mesh TX
            try {
                com.example.meshtx.GestorMeshTx.actualizarPerfilLocal("Piloto TX", "")
            } catch (_: Exception) {}

            // 4. Purgar caché en memoria de Coil para que no persista la foto anterior
            try {
                coil.Coil.imageLoader(app).memoryCache?.clear()
                coil.Coil.imageLoader(app).diskCache?.clear()
            } catch (_: Exception) {}

            // 5. Resetear estados de sesión reactivos
            _currentMemberId.value = -1L
            _isAuthenticated.value = false
            _usuarioEstado.value = "PENDIENTE"
            _isLeaderSuperAdmin.value = false
            _isDirectivaMode.value = false
            _sesionDesplazadaPorOtroDispositivo.value = false
        }
    }

    fun loginWithPhone(inputPhone: String): Boolean {
        _sesionDesplazadaPorOtroDispositivo.value = false
        val cleanInput = inputPhone.replace(Regex("[^0-9]"), "")
        val matched = allMembers.value.find { m ->
            val cleanMemPhone = m.phone.replace(Regex("[^0-9]"), "")
            cleanMemPhone.endsWith(cleanInput) || cleanInput.endsWith(cleanMemPhone) || cleanMemPhone == cleanInput
        }
        return if (matched != null) {
            _currentMemberId.value = matched.id
            _isAuthenticated.value = true
            _usuarioEstado.value = "ACTIVO"
            if (matched.role == MemberRole.PRESIDENTE || matched.role.canManageApp || matched.isDirectiva) {
                _isDirectivaMode.value = true
                if (matched.role == MemberRole.PRESIDENTE) _isLeaderSuperAdmin.value = true
            }
            saveSession(matched.id, matched.email, matched.firebaseUid)
            
            // Asegurar autenticación en Firebase para permitir subida de archivos
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid
                        Log.d("TeamTxViewModel", "Autenticación anónima exitosa: $uid")
                        if (matched.firebaseUid == null && uid != null) {
                            updateProfile(matched.copy(firebaseUid = uid))
                        }
                    }
            }
            
            true
        } else {
            false
        }
    }

    fun procesarIngresoGoogle(uid: String, email: String, googlePhotoUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanEmail = email.trim().lowercase()
            val idDispositivoLocal = SEGURIDAD_CUENTAS.obtenerIdDispositivo(getApplication())

            // 🛡️ REGLA 4: Privilegio Nivel Dios (Bypass del Creador)
            val isDev = cleanEmail == "eduardo.androide.em@gmail.com" || cleanEmail == "eduardo.jose.marquez.matos@gmail.com"

            if (isDev) {
                Log.i("TeamTxViewModel", "👑 Bypass Creador/Super Admin detectado para: $cleanEmail")

                loginWithGoogle(email, uid, googlePhotoUrl)

                val perfilActual = currentMember.value
                val idMember = perfilActual?.id ?: System.currentTimeMillis()
                val numMember = perfilActual?.memberNumber?.ifBlank { "TX-001" } ?: "TX-001"
                val nombreDev = perfilActual?.fullName?.ifBlank { "Eduardo Marquez (EM)" } ?: "Eduardo Marquez (EM)"

                val datosDev = mapOf(
                    "id" to idMember,
                    "fullName" to nombreDev,
                    "nickname" to "Dev TX",
                    "memberNumber" to numMember,
                    "cedulaDni" to "V-19554402",
                    "phone" to "04120000000",
                    "email" to cleanEmail,
                    "firebaseUid" to uid,
                    "role" to MemberRole.DESARROLLADOR.name,
                    "isDirectiva" to true,
                    "isSuspended" to false,
                    "suspensionReason" to "",
                    "estado" to "ACTIVO",
                    "usuarioEstado" to "ACTIVO",
                    "solvencyStatus" to true,
                    "id_dispositivo_activo" to idDispositivoLocal,
                    "activeDeviceId" to idDispositivoLocal,
                    "timestamp" to System.currentTimeMillis(),
                    "lastActiveTimestamp" to System.currentTimeMillis()
                )

                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("usuarios").document(uid).set(datosDev, com.google.firebase.firestore.SetOptions.merge()).await()
                    db.collection("users").document(uid).set(datosDev, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (e: Exception) {
                    Log.e("TeamTxViewModel", "Error guardando datos Super Admin en Firestore: ${e.message}")
                }

                // Registrar vinculación 1-a-1 en vinculos_google
                val vinculacionDev = com.example.data.remote.VinculacionGoogle(
                    email = cleanEmail,
                    correo = cleanEmail,
                    firebaseUid = uid,
                    uid_firebase = uid,
                    memberNumber = numMember,
                    numero_miembro = numMember,
                    memberName = nombreDev,
                    nombre_piloto = nombreDev,
                    memberId = idMember,
                    activeDeviceId = idDispositivoLocal,
                    id_dispositivo_activo = idDispositivoLocal,
                    linkedAt = System.currentTimeMillis(),
                    lastActiveTimestamp = System.currentTimeMillis()
                )
                PerfilNube.registrarVinculacion(vinculacionDev)

                _usuarioEstado.value = "ACTIVO"
                _isAuthenticated.value = true
                _isDirectivaMode.value = true
                _isLeaderSuperAdmin.value = true
                iniciarSincronizacionDePerfil(uid)
                return@launch
            }

            try {
                val db = FirebaseFirestore.getInstance()
                var doc = db.collection("usuarios").document(uid).get().await()
                if (!doc.exists()) {
                    doc = db.collection("users").document(uid).get().await()
                }

                if (doc.exists()) {
                    val estadoRemote = doc.getString("usuarioEstado") ?: doc.getString("estado") ?: "PENDIENTE"
                    val deviceIdRemoto = doc.getString("id_dispositivo_activo") ?: doc.getString("activeDeviceId")

                    // 🛡️ REGLA 5: Bloqueo Multi-cuenta por Device ID
                    if (!deviceIdRemoto.isNullOrBlank() && deviceIdRemoto != idDispositivoLocal) {
                        Log.w("TeamTxViewModel", "🚨 Dispositivo en Firestore ($deviceIdRemoto) no coincide con teléfono ($idDispositivoLocal). Acceso bloqueado.")
                        _usuarioEstado.value = "RECHAZADO"
                        _isAuthenticated.value = true
                        return@launch
                    }

                    // Actualizar ID del teléfono actual en Firestore
                    PerfilNube.actualizarDispositivoActivo(cleanEmail, uid, idDispositivoLocal)

                    _usuarioEstado.value = estadoRemote
                    _isAuthenticated.value = true

                    if (estadoRemote == "ACTIVO") {
                        loginWithGoogle(email, uid, googlePhotoUrl)
                        iniciarSincronizacionDePerfil(uid)
                    }
                } else {
                    // 🛡️ REGLA 2: Si el UID NO existe en Firestore -> Dirigir a Formulario de Registro
                    _usuarioEstado.value = "NUEVO_REGISTRO"
                    _isAuthenticated.value = true
                }
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error procesando ingreso Google: ${e.message}")
                _usuarioEstado.value = "NUEVO_REGISTRO"
                _isAuthenticated.value = true
            }
        }
    }

    fun guardarNuevoPerfilFirestore(nuevoPerfil: MemberProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = nuevoPerfil.firebaseUid?.ifBlank { null }
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val idDispositivoLocal = SEGURIDAD_CUENTAS.obtenerIdDispositivo(getApplication())
            val cleanEmail = (nuevoPerfil.email ?: "").trim().lowercase()

            val isDev = cleanEmail == "eduardo.androide.em@gmail.com" || cleanEmail == "eduardo.jose.marquez.matos@gmail.com"
            val estadoInicial = if (isDev) "ACTIVO" else "PENDIENTE"

            val perfilFinal = nuevoPerfil.copy(
                firebaseUid = uid,
                email = cleanEmail,
                solvencyStatus = if (isDev) true else false,
                role = if (isDev) MemberRole.DESARROLLADOR else nuevoPerfil.role,
                isDirectiva = if (isDev) true else nuevoPerfil.isDirectiva
            )

            val datosMap = mapOf(
                "id" to perfilFinal.id,
                "fullName" to perfilFinal.fullName,
                "nickname" to perfilFinal.nickname,
                "memberNumber" to perfilFinal.memberNumber,
                "email" to perfilFinal.email,
                "firebaseUid" to uid,
                "bikeModel" to perfilFinal.bikeModel,
                "bikePlate" to perfilFinal.bikePlate,
                "bloodType" to perfilFinal.bloodType,
                "emergencyContactPhone" to perfilFinal.emergencyContactPhone,
                "role" to perfilFinal.role.name,
                "isDirectiva" to perfilFinal.isDirectiva,
                "estado" to estadoInicial,
                "usuarioEstado" to estadoInicial,
                "id_dispositivo_activo" to idDispositivoLocal,
                "activeDeviceId" to idDispositivoLocal,
                "timestamp" to System.currentTimeMillis(),
                "lastActiveTimestamp" to System.currentTimeMillis()
            )

            try {
                val db = FirebaseFirestore.getInstance()
                if (uid.isNotBlank()) {
                    db.collection("usuarios").document(uid).set(datosMap, com.google.firebase.firestore.SetOptions.merge()).await()
                    db.collection("users").document(uid).set(datosMap, com.google.firebase.firestore.SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error guardando nuevo perfil en Firestore: ${e.message}")
            }

            repository.insertMember(perfilFinal)
            _currentMemberId.value = perfilFinal.id
            _usuarioEstado.value = estadoInicial
            _isAuthenticated.value = true
            if (isDev) {
                _isDirectivaMode.value = true
                _isLeaderSuperAdmin.value = true
            }
            saveSession(perfilFinal.id, perfilFinal.email, uid)
            if (uid.isNotBlank()) {
                iniciarSincronizacionDePerfil(uid)
            }
        }
    }

    fun verificarEstadoFirestore() {
        val current = currentMember.value
        val isApprovedLocal = current != null && current.role != MemberRole.ASPIRANTE && current.role != MemberRole.INVITADO
        val uid = current?.firebaseUid?.ifBlank { null }
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        if (isApprovedLocal) {
            _usuarioEstado.value = "ACTIVO"
        }

        if (uid.isNullOrBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                var doc = db.collection("usuarios").document(uid).get().await()
                if (!doc.exists()) {
                    doc = db.collection("users").document(uid).get().await()
                }

                if (doc.exists()) {
                    val nuevoEstado = doc.getString("usuarioEstado") ?: doc.getString("estado") ?: "ACTIVO"
                    _usuarioEstado.value = if (isApprovedLocal) "ACTIVO" else nuevoEstado
                    if (nuevoEstado == "ACTIVO") {
                        currentMember.value?.let { member ->
                            val updated = member.copy(solvencyStatus = true)
                            repository.updateMember(updated)
                        }
                    }
                } else if (isApprovedLocal) {
                    _usuarioEstado.value = "ACTIVO"
                }
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error verificando estado Firestore: ${e.message}")
                if (isApprovedLocal) {
                    _usuarioEstado.value = "ACTIVO"
                }
            }
        }
    }

    suspend fun loginWithGoogle(email: String, firebaseUid: String? = null, googlePhotoUrl: String? = null): Pair<Boolean, String> {
        _sesionDesplazadaPorOtroDispositivo.value = false
        val cleanEmail = email.trim().lowercase()
        val authUid = firebaseUid ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        // 1. Search locally
        val localMembers = repository.allMembers.first()
        var matched = localMembers.find { 
            (authUid != null && it.firebaseUid == authUid) || it.email.equals(cleanEmail, ignoreCase = true) 
        }

        // 2. If not found locally, query remote Firestore "members"
        if (matched == null) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = firestore.collection("members")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val remoteDoc = snapshot.documents.first()
                    val profile = remoteDoc.toObject(MemberProfile::class.java)?.copy(
                        id = remoteDoc.id.toLongOrNull() ?: System.currentTimeMillis()
                    )
                    if (profile != null) {
                        repository.insertMember(profile)
                        matched = profile
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TeamTxViewModel", "Error fetching member from Firestore: ${e.message}")
            }
        }

        // 2.5 Ultimo intento: perfil anclado al UID en la nube (usuarios/{uid})
        if (matched == null && authUid != null) {
            val remoto = PerfilNube.descargarPerfil(authUid)
            if (remoto != null) {
                val nuevoId = remoto.id.takeIf { it > 0 } ?: System.currentTimeMillis()
                val profileToInsert = remoto.copy(id = nuevoId, email = remoto.email ?: cleanEmail, firebaseUid = authUid)
                repository.insertMember(profileToInsert)
                matched = profileToInsert
                Log.d("PERFIL_SYNC", "Perfil descargado de la nube por UID en login")
            }
        }

        // 3. Developer / Master account special handling
        val isDevEmail = cleanEmail == "eduardo.androide.em@gmail.com" || cleanEmail == "eduardo.jose.marquez.matos@gmail.com"
        if (isDevEmail) {
            if (matched != null) {
                matched = matched!!.copy(
                    email = cleanEmail,
                    firebaseUid = authUid ?: matched!!.firebaseUid,
                    role = MemberRole.DESARROLLADOR,
                    isDirectiva = true,
                    isSuspended = false,
                    suspensionReason = "",
                    solvencyStatus = true
                )
            } else {
                val devMaster = localMembers.find { it.role == MemberRole.DESARROLLADOR }
                if (devMaster != null) {
                    matched = devMaster.copy(
                        email = cleanEmail,
                        firebaseUid = authUid,
                        role = MemberRole.DESARROLLADOR,
                        isDirectiva = true,
                        isSuspended = false,
                        suspensionReason = "",
                        solvencyStatus = true
                    )
                } else {
                    matched = MemberProfile(
                        id = System.currentTimeMillis(),
                        fullName = "Eduardo Marquez (EM)",
                        nickname = "Dev TX",
                        memberNumber = "TX-001",
                        role = MemberRole.DESARROLLADOR,
                        isDirectiva = true,
                        isSuspended = false,
                        suspensionReason = "",
                        solvencyStatus = true,
                        email = cleanEmail,
                        firebaseUid = authUid,
                        avatarInitials = "EA",
                        cedulaDni = "V-19554402",
                        phone = "04120000000",
                        bikePlate = "TX-001",
                        emergencyContactName = "Directiva TX",
                        emergencyContactPhone = "04120000000"
                    )
                }
            }
        }

        if (matched != null) {
            val conFoto = if (matched!!.profilePhotoUri.isNullOrBlank() && !googlePhotoUrl.isNullOrBlank())
                matched!!.copy(profilePhotoUri = googlePhotoUrl) else matched!!
            var updated = conFoto.copy(
                email = cleanEmail,
                firebaseUid = authUid ?: conFoto.firebaseUid
            )
            if (isDevEmail) {
                updated = updated.copy(
                    role = MemberRole.DESARROLLADOR,
                    isDirectiva = true,
                    isSuspended = false,
                    suspensionReason = "",
                    solvencyStatus = true
                )
            }
            repository.updateMember(updated)
            _currentMemberId.value = updated.id
            _isAuthenticated.value = true
            _usuarioEstado.value = "ACTIVO"
            if (updated.role == MemberRole.PRESIDENTE || updated.role == MemberRole.DESARROLLADOR || updated.role.canManageApp || updated.isDirectiva) {
                _isDirectivaMode.value = true
                if (updated.role == MemberRole.PRESIDENTE || updated.role == MemberRole.DESARROLLADOR) _isLeaderSuperAdmin.value = true
            }
            saveSession(updated.id, updated.email, updated.firebaseUid)
            iniciarSincronizacionDePerfil(updated.firebaseUid ?: authUid)
            return Pair(true, "¡Bienvenido de vuelta, ${updated.nickname.ifBlank { updated.fullName }}!")
        } else {
            // 🚀 USUARIO NUEVO CON GOOGLE: Crear perfil automático para permitir el acceso
            val nombreExtraido = cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
            val iniciales = nombreExtraido.take(2).uppercase().ifBlank { "TX" }
            val nuevoMiembro = MemberProfile(
                id = System.currentTimeMillis(),
                fullName = nombreExtraido,
                nickname = nombreExtraido,
                memberNumber = "TX-${(100..999).random()}",
                email = cleanEmail,
                firebaseUid = authUid,
                profilePhotoUri = googlePhotoUrl,
                role = MemberRole.MIEMBRO_ACTIVO,
                isDirectiva = false,
                solvencyStatus = true,
                avatarInitials = iniciales,
                phone = "",
                bikePlate = "PENDIENTE",
                chapterState = "Venezuela"
            )
            repository.insertMember(nuevoMiembro)
            if (authUid != null) {
                PerfilNube.crearSiNoExiste(authUid, nuevoMiembro)
            }
            _currentMemberId.value = nuevoMiembro.id
            _isAuthenticated.value = true
            _usuarioEstado.value = "ACTIVO"
            saveSession(nuevoMiembro.id, nuevoMiembro.email, nuevoMiembro.firebaseUid)
            iniciarSincronizacionDePerfil(nuevoMiembro.firebaseUid ?: authUid)
            Log.i("TeamTxViewModel", "✅ Nuevo piloto registrado con Google: $cleanEmail (UID: $authUid)")
            return Pair(true, "¡Bienvenido a Team Nacional TX Aragua, ${nuevoMiembro.nickname}!")
        }
    }

    suspend fun loginWithCode(
        inputCode: String,
        fullName: String = "",
        phone: String = "",
        bikeModel: String = "",
        bikePlate: String = "",
        birthDate: String = "",
        role: String = "Piloto"
    ): Pair<Boolean, String> {
        val codeTrim = inputCode.trim().uppercase()

        // 🛡️ REINICIO ABSOLUTO: Al cambiar de cuenta, limpiamos TODO rastro de la anterior
        detenerSincronizacionDePerfil()
        _sesionDesplazadaPorOtroDispositivo.value = false
        
        // Limpieza profunda preventiva de Room y Prefs locales
        repository.nukeEverything()
        clearSession()

        val isDev1 = codeTrim == "DESARROLLO1" || codeTrim == "TX19554402SB"
        val isDev2 = codeTrim == "DESARROLLO2" || codeTrim == "19554402SB"
        val isPresident = codeTrim == "PRESIDENTE" || codeTrim == "TX19554402"
        val isPilot1 = codeTrim == "PILOTO1"
        val isPilot2 = codeTrim == "PILOTO2"

        if (!isDev1 && !isDev2 && !isPresident && !isPilot1 && !isPilot2) {
            // Intentar buscar en códigos de invitación generados dinámicamente si no es uno de los fijos
            try {
                val dbFirestore = FirebaseFirestore.getInstance()
                val snapshot = dbFirestore.collection("invitation_codes")
                    .whereEqualTo("code", codeTrim)
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .await()
                
                if (snapshot.isEmpty) {
                    return Pair(false, "Código de credencial inválido o caducado. Contacta a la Directiva.")
                }
                
                // Si existe código dinámico, crear perfil local temporal
                val doc = snapshot.documents.first()
                val newProfile = MemberProfile(
                    id = System.currentTimeMillis(),
                    fullName = doc.getString("fullName") ?: "Piloto TX",
                    memberNumber = doc.getString("memberNumber") ?: "TX-NUEVO",
                    role = MemberRole.MIEMBRO_ACTIVO,
                    solvencyStatus = true
                )
                repository.insertMember(newProfile)
                _currentMemberId.value = newProfile.id
                _usuarioEstado.value = "PENDIENTE"
                saveSession(newProfile.id, null, null)
                return Pair(true, "¡Acceso concedido! Por favor vincula tu cuenta Google en el perfil.")
            } catch (e: Exception) {
                return Pair(false, "Error de conexión: ${e.message}")
            }
        }

        // --- MANEJO DE CUENTAS FIJAS ---
        val newProfile = when {
            isDev1 -> MemberProfile(
                id = 1001L,
                fullName = "Eduardo Marquez (EM)",
                nickname = "Dev EM",
                memberNumber = "TX-DEV-001",
                role = MemberRole.DESARROLLADOR,
                isDirectiva = true,
                solvencyStatus = true,
                avatarInitials = "EM",
                email = "eduardo.androide.em@gmail.com",
                firebaseUid = "XqLJxsoQFFWYdK8R0qrxh0b03D22"
            )
            isDev2 -> MemberProfile(
                id = 1002L,
                fullName = "Eduardo Marquez (Matos)",
                nickname = "Dev Matos",
                memberNumber = "TX-DEV-002",
                role = MemberRole.DESARROLLADOR,
                isDirectiva = true,
                solvencyStatus = true,
                avatarInitials = "JM",
                email = "eduardo.jose.marquez.matos@gmail.com",
                firebaseUid = "JF6CyIjXKlYB253CKzi5N6gN02n1"
            )
            isPresident -> MemberProfile(
                id = 1000L,
                fullName = "Presidente Nacional TX",
                nickname = "Presidente",
                memberNumber = "TX-001",
                role = MemberRole.PRESIDENTE,
                isDirectiva = true,
                solvencyStatus = true,
                avatarInitials = "PR"
            )
            isPilot1 -> MemberProfile(
                id = 2001L,
                fullName = "Piloto de Prueba 1",
                nickname = "Piloto 1",
                memberNumber = "TX-P01",
                role = MemberRole.MIEMBRO_ACTIVO,
                isDirectiva = false,
                solvencyStatus = true,
                avatarInitials = "P1"
            )
            isPilot2 -> MemberProfile(
                id = 2002L,
                fullName = "Piloto de Prueba 2",
                nickname = "Piloto 2",
                memberNumber = "TX-P02",
                role = MemberRole.MIEMBRO_ACTIVO,
                isDirectiva = false,
                solvencyStatus = true,
                avatarInitials = "P2"
            )
            else -> null
        }

        if (newProfile != null) {
            val emailFinal = newProfile.email?.ifBlank { null }
            repository.insertMember(newProfile)
            _currentMemberId.value = newProfile.id
            _isAuthenticated.value = true
            _isDirectivaMode.value = newProfile.isDirectiva
            _isLeaderSuperAdmin.value = newProfile.role == MemberRole.PRESIDENTE || newProfile.role == MemberRole.DESARROLLADOR
            _usuarioEstado.value = if (newProfile.isDirectiva || newProfile.role == MemberRole.PRESIDENTE || newProfile.role == MemberRole.DESARROLLADOR) "ACTIVO" else "PENDIENTE"

            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously().await()
            } catch (_: Exception) {}

            val uidParaSesion = newProfile.firebaseUid?.ifBlank { null }
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            saveSession(newProfile.id, emailFinal, uidParaSesion)
            if (!newProfile.firebaseUid.isNullOrBlank()) {
                iniciarSincronizacionDePerfil(newProfile.firebaseUid)
            }
            return Pair(true, "¡Acceso concedido como ${newProfile.role.displayName}!")
        }

        return Pair(false, "Error inesperado al validar credenciales.")
    }

    /**
     * Consulta si un correo ya posee código asignado o si es un correo nuevo que requiere solicitud a la Directiva.
     * Retorna Triple(estaVinculado: Boolean, codigoRegistrado: String, nombrePiloto: String)
     */
    suspend fun verificarEstadoCorreoParaLogin(email: String): Triple<Boolean, String, String> {
        return VINCULACION.consultarEstadoCorreo(email)
    }

    /**
     * Inicia sesión validando el par único Correo <-> Código (el código es la contraseña del correo).
     */
    suspend fun loginWithEmailAndCode(email: String, inputCode: String): Pair<Boolean, String> {
        val cleanEmail = email.trim().lowercase()
        val codeTrim = inputCode.trim().uppercase()

        if (cleanEmail.isBlank() || codeTrim.isBlank()) {
            return Pair(false, "Debes ingresar tu correo y el código asignado.")
        }

        // 1. Validar si el código corresponde al correo en Firestore / Credenciales maestras
        val (esValido, mensajeValidacion) = VINCULACION.validarCodigoParaCorreo(cleanEmail, codeTrim)
        if (!esValido) {
            return Pair(false, mensajeValidacion)
        }

        // 2. REINICIO ABSOLUTO previo: Limpiar sesión previa para no cruzar perfiles
        detenerSincronizacionDePerfil()
        _sesionDesplazadaPorOtroDispositivo.value = false
        repository.nukeEverything()
        clearSession()

        // 3. Perfiles de desarrollador
        val isDev1 = cleanEmail == "eduardo.androide.em@gmail.com"
        val isDev2 = cleanEmail == "eduardo.jose.marquez.matos@gmail.com"

        var perfilCargado: MemberProfile? = when {
            isDev1 -> MemberProfile(
                id = 1001L,
                fullName = "Eduardo Marquez (EM)",
                nickname = "Dev EM",
                memberNumber = "TX-DEV-001",
                role = MemberRole.DESARROLLADOR,
                isDirectiva = true,
                solvencyStatus = true,
                avatarInitials = "EM",
                email = cleanEmail,
                firebaseUid = "XqLJxsoQFFWYdK8R0qrxh0b03D22"
            )
            isDev2 -> MemberProfile(
                id = 1002L,
                fullName = "Eduardo Marquez (Matos)",
                nickname = "Dev Matos",
                memberNumber = "TX-DEV-002",
                role = MemberRole.DESARROLLADOR,
                isDirectiva = true,
                solvencyStatus = true,
                avatarInitials = "JM",
                email = cleanEmail,
                firebaseUid = "JF6CyIjXKlYB253CKzi5N6gN02n1"
            )
            else -> null
        }

        // 4. Si no es desarrollador, consultar vínculo en Firestore para obtener UID y perfil
        var uidFirebase = ""
        try {
            val vinculo = PerfilNube.consultarVinculacionPorEmail(cleanEmail)
            if (vinculo != null) {
                uidFirebase = vinculo.uid_firebase.ifBlank { vinculo.firebaseUid }
                val numMiembro = vinculo.numero_miembro.ifBlank { vinculo.memberNumber }.ifBlank { "TX-MIEMBRO" }
                val nomPiloto = vinculo.nombre_piloto.ifBlank { vinculo.memberName }.ifBlank { "Piloto TX" }

                // Intentar descargar perfil completo de usuarios/{uid}
                if (uidFirebase.isNotBlank() && perfilCargado == null) {
                    val remoto = PerfilNube.descargarPerfil(uidFirebase)
                    if (remoto != null) {
                        perfilCargado = remoto.copy(
                            email = cleanEmail,
                            firebaseUid = uidFirebase,
                            memberNumber = numMiembro
                        )
                    }
                }

                if (perfilCargado == null) {
                    perfilCargado = MemberProfile(
                        id = vinculo.memberId.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        fullName = nomPiloto,
                        nickname = nomPiloto.split(" ").firstOrNull() ?: nomPiloto,
                        memberNumber = numMiembro,
                        role = MemberRole.MIEMBRO_ACTIVO,
                        solvencyStatus = true,
                        email = cleanEmail,
                        firebaseUid = uidFirebase.ifBlank { null }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LOGIN_EMAIL_CODE", "Error consultando perfil remoto: ${e.message}")
        }

        if (perfilCargado == null) {
            // Fallback: perfil base limpio vinculado
            perfilCargado = MemberProfile(
                id = System.currentTimeMillis(),
                fullName = "Piloto TX",
                memberNumber = "TX-PILOTO",
                role = MemberRole.MIEMBRO_ACTIVO,
                solvencyStatus = true,
                email = cleanEmail
            )
        }

        val idDispositivoLocal = SEGURIDAD_CUENTAS.obtenerIdDispositivo(getApplication())

        // Actualizar dispositivo activo en Firestore
        if (uidFirebase.isNotBlank()) {
            PerfilNube.actualizarDispositivoActivo(cleanEmail, uidFirebase, idDispositivoLocal)
        }

        // Guardar perfil en Room
        repository.insertMember(perfilCargado)
        _currentMemberId.value = perfilCargado.id
_isAuthenticated.value = true
            _isDirectivaMode.value = perfilCargado.isDirectiva || perfilCargado.role == MemberRole.DESARROLLADOR || perfilCargado.role == MemberRole.PRESIDENTE
            _isLeaderSuperAdmin.value = perfilCargado.role == MemberRole.PRESIDENTE || perfilCargado.role == MemberRole.DESARROLLADOR
            _usuarioEstado.value = if (perfilCargado.isDirectiva || perfilCargado.role == MemberRole.PRESIDENTE || perfilCargado.role == MemberRole.DESARROLLADOR) "ACTIVO" else "PENDIENTE"

        // Asegurar Auth anónima preventiva si no hay Auth activa
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously().await()
            } catch (_: Exception) {}
        }

        val uidFinal = perfilCargado.firebaseUid?.ifBlank { null }
            ?: uidFirebase.ifBlank { null }
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        saveSession(perfilCargado.id, cleanEmail, uidFinal)

        if (!uidFinal.isNullOrBlank()) {
            iniciarSincronizacionDePerfil(uidFinal)
        }

        return Pair(true, "¡Acceso concedido exitosamente a ${perfilCargado.fullName}!")
    }

    /**
     * Permite al usuario personalizar su código de acceso en Carnet TX, sincronizándolo en Firebase.
     */
    suspend fun cambiarCodigoAcceso(nuevoCodigo: String): Pair<Boolean, String> {
        val member = currentMember.value ?: return Pair(false, "No hay perfil activo.")
        val email = member.email ?: ""
        val uid = member.firebaseUid ?: ""
        if (email.isBlank()) {
            return Pair(false, "Debes vincular un correo a tu carnet antes de personalizar tu código.")
        }
        return VINCULACION.actualizarCodigoAccesoPersonalizado(email, nuevoCodigo, uid)
    }

    /**
     * Switch de cuenta exclusivo para Desarrolladores desde Carnet TX.
     */
    suspend fun cambiarCuentaDesarrollador(codigoDev: String): Pair<Boolean, String> {
        val member = currentMember.value
        val esDev = member?.role == MemberRole.DESARROLLADOR || _isLeaderSuperAdmin.value
        if (!esDev) {
            return Pair(false, "Función exclusiva para cuentas con rol Desarrollador.")
        }

        val codeTrim = codigoDev.trim().uppercase()
        val emailTarget = when (codeTrim) {
            "DESARROLLO1", "TX19554402SB" -> "eduardo.androide.em@gmail.com"
            "DESARROLLO2", "19554402SB" -> "eduardo.jose.marquez.matos@gmail.com"
            else -> return Pair(false, "Código desarrollador inválido. Usa DESARROLLO1 o DESARROLLO2.")
        }

        return loginWithEmailAndCode(emailTarget, codeTrim)
    }

    fun submitAccessRequest(
        fullName: String,
        phone: String,
        cedulaDni: String,
        bikeBrand: String,
        bikeModel: String,
        bikeColor: String,
        bikePlate: String,
        chapterState: String,
        reason: String,
        birthDate: String = "",
        requestedRole: String = "Piloto",
        requestType: String = "ACCESO_APP"
    ): Boolean {
        if (_requestAttemptsLeft.value <= 0) return false
        _requestAttemptsLeft.value -= 1

        viewModelScope.launch {
            val idBase = System.currentTimeMillis()
            val req = AccessRequest(
                id = idBase, // 🛡️ ID Manual Atómico
                fullName = fullName,
                phone = phone,
                cedulaDni = cedulaDni,
                bikeBrand = bikeBrand,
                bikeModel = bikeModel,
                bikeColor = bikeColor,
                bikePlate = bikePlate,
                chapterState = chapterState,
                birthDate = birthDate,
                requestedRole = requestedRole,
                requestType = requestType,
                reasonMessage = reason,
                timestamp = System.currentTimeMillis(),
                status = "PENDIENTE"
            )
            repository.insertAccessRequest(req)

            // Alert Directiva in internal channel
            val alertMsg = ChatMessage(
                id = idBase + 1, // 🛡️ ID Manual Atómico secuencial
                channelId = "DIRECTIVA",
                senderMemberId = 0,
                senderName = "SISTEMA TX",
                senderNickname = "Bot",
                senderMemberNumber = "TX-BOT",
                senderRole = MemberRole.DISCIPLINARIO,
                senderCustomRoleTitle = "Bot de Ingreso",
                senderInitials = "TX",
                messageText = "📥 NUEVA SOLICITUD DE ACCESO: $fullName ($phone). Cédula: $cedulaDni. Moto: $bikeBrand $bikeModel ($bikePlate). Revisa la pestaña de SOLICITUDES para aprobar o rechazar.",
                isRadioCallout = true,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(alertMsg)
        }
        return true
    }

    /**
     * Verifica si un correo electrónico pertenece a una cuenta existente en Firebase/Local.
     * Si existe, genera una solicitud formal a la Directiva Nacional para que le emitan un nuevo código.
     */
    suspend fun solicitarCodigoPorCorreoSincronizado(email: String): Pair<Boolean, String> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Pair(false, "Por favor ingresa un correo electrónico válido.")
        }

        return withContext(Dispatchers.IO) {
            try {
                // 1. Buscar en memoria local primero
                var memberMatch = allMembers.value.find { it.email?.trim()?.lowercase() == cleanEmail }

                // 2. Si no está en memoria local, consultar Firestore en la colección "members"
                if (memberMatch == null) {
                    val firestore = FirebaseFirestore.getInstance()
                    val snapshot = firestore.collection("members")
                        .whereEqualTo("email", cleanEmail)
                        .get()
                        .await()

                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        val remoteProfile = doc.toObject(MemberProfile::class.java)?.copy(
                            id = doc.id.toLongOrNull() ?: System.currentTimeMillis()
                        )
                        if (remoteProfile != null) {
                            repository.insertMember(remoteProfile)
                            memberMatch = remoteProfile
                        }
                    }
                }

                if (memberMatch == null) {
                    return@withContext Pair(
                        false,
                        "No se encontró ninguna cuenta vinculada a '$cleanEmail'. Si eres un nuevo piloto, solicita tu código de entrada en la opción anterior."
                    )
                }

                // 3. Crear solicitud formal a la directiva para recuperar código
                val reqId = System.currentTimeMillis()
                val req = AccessRequest(
                    id = reqId,
                    fullName = memberMatch.fullName,
                    phone = memberMatch.phone,
                    cedulaDni = memberMatch.cedulaDni,
                    bikeBrand = "Keeway",
                    bikeModel = memberMatch.bikeModel.ifBlank { "TX 200 SM" },
                    bikeColor = memberMatch.bikeColor.ifBlank { "Negro / Naranja" },
                    bikePlate = memberMatch.bikePlate.ifBlank { "SIN-PLACA" },
                    chapterState = memberMatch.chapterState.ifBlank { "Aragua" },
                    birthDate = memberMatch.birthDate,
                    requestedRole = memberMatch.role.displayName,
                    requestType = "RECUPERACION_CUENTA",
                    reasonMessage = "El piloto ${memberMatch.fullName} (${memberMatch.memberNumber}) solicita emisión de código para su correo sincronizado: $cleanEmail.",
                    timestamp = reqId,
                    status = "PENDIENTE"
                )
                repository.insertAccessRequest(req)

                // Subir a Firestore para alerta inmediata a la Directiva
                try {
                    FirebaseFirestore.getInstance().collection("access_requests")
                        .document(reqId.toString())
                        .set(req)
                        .await()
                } catch (e: Exception) {
                    Log.e("TeamTxViewModel", "Error sincronizando solicitud a Firestore: ${e.message}")
                }

                // Alerta al canal de Directiva
                val alertMsg = ChatMessage(
                    id = reqId + 1,
                    channelId = "DIRECTIVA",
                    senderMemberId = 0,
                    senderName = "SISTEMA TX",
                    senderNickname = "Bot",
                    senderMemberNumber = "TX-BOT",
                    senderRole = MemberRole.DISCIPLINARIO,
                    senderCustomRoleTitle = "Bot de Ingreso",
                    senderInitials = "TX",
                    messageText = "🔑 RECUPERACIÓN DE CUENTA: El piloto ${memberMatch.fullName} (${memberMatch.memberNumber}) solicita código para su correo sincronizado: $cleanEmail. Suminístrale su código desde el Panel de Solicitudes.",
                    isRadioCallout = true,
                    timestamp = reqId
                )
                repository.insertChatMessage(alertMsg)

                Pair(
                    true,
                    "¡Cuenta verificada exitosamente!\n\n• Piloto: ${memberMatch.fullName}\n• Ficha: ${memberMatch.memberNumber}\n• Rol: ${memberMatch.role.displayName}\n\nSe ha enviado la notificación a la Directiva Nacional. La directiva te suministrará tu nuevo código. Al colocarlo en la casilla de entrada, tu cuenta y todos tus datos se restaurarán automáticamente."
                )
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error en solicitarCodigoPorCorreoSincronizado: ${e.message}")
                Pair(false, "Error al conectar con el servidor: ${e.localizedMessage ?: "Intenta de nuevo."}")
            }
        }
    }

    // ---------------- INVITATION CODES (24H & MAESTROS) ---------------- //
    val allInvitationCodes: StateFlow<List<InvitationCode>> = repository.allInvitationCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateInvitationCode(
        targetRole: MemberRole = MemberRole.MIEMBRO_ACTIVO,
        note: String = "",
        isSpecialGuest: Boolean = false,
        durationHours: Int = 24
    ): String {
        // Standard codes: PURE NUMERIC ONLY (6 digits). Special Guest: ALPHANUMERIC (INV-TX...)
        val codeString = if (isSpecialGuest) {
            val randomSuffix = (1000..9999).random()
            "INV-TX$randomSuffix"
        } else {
            (100000..999999).random().toString()
        }

        viewModelScope.launch {
            val creator = currentMember.value?.let { "${it.nickname} (${it.role.displayName})" } ?: "Líder / Presidente"
            val newCode = InvitationCode(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                code = codeString,
                isMaster = false,
                isSpecialGuest = isSpecialGuest,
                durationHours = durationHours,
                createdBy = creator,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + durationHours * 60 * 60 * 1000L,
                isUsed = false,
                targetRole = if (isSpecialGuest) MemberRole.INVITADO else targetRole,
                note = note.ifBlank { if (isSpecialGuest) "Invitado especial ($durationHours h)" else "Invitación estándar ($durationHours h)" }
            )
            repository.insertInvitationCode(newCode)
        }
        return codeString
    }

    fun deleteInvitationCode(code: InvitationCode) {
        if (code.isMaster) return // Protection: Cannot delete master codes
        viewModelScope.launch {
            repository.deleteInvitationCode(code.id)
        }
    }

    fun darDeBajaInvitado(guestMember: MemberProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = guestMember.copy(
                isSuspended = true,
                suspensionReason = "Acceso de invitado temporal revocado por la Directiva",
                suspensionStartTimestamp = System.currentTimeMillis(),
                suspensionEndTimestamp = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000L,
                suspendedBy = currentMember.value?.fullName ?: "Directiva Nacional",
                isOnline = false,
                solvencyStatus = false
            )
            repository.updateMember(updated)

            // Alert Directiva chat
            val alert = ChatMessage(
                id = System.currentTimeMillis(),
                channelId = "DIRECTIVA",
                senderMemberId = currentMember.value?.id ?: 1,
                senderName = currentMember.value?.fullName ?: "Directiva",
                senderNickname = currentMember.value?.nickname ?: "Directivo",
                senderMemberNumber = currentMember.value?.memberNumber ?: "TX-001",
                senderRole = MemberRole.DISCIPLINARIO,
                senderCustomRoleTitle = "Oficial Disciplinario",
                senderInitials = "TX",
                messageText = "🔴 ACCESO DADO DE BAJA: Se revocó y bloqueó el acceso de invitado para ${guestMember.fullName} (${guestMember.memberNumber}). Su app ha sido cerrada y devuelta al inicio.",
                isRadioCallout = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(alert)
        }
    }

    // ---------------- ACCESS REQUESTS (SOLICITUDES) ---------------- //
    val allAccessRequests: StateFlow<List<AccessRequest>> = repository.allAccessRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAccessRequests: StateFlow<List<AccessRequest>> = repository.pendingAccessRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveAccessRequest(request: AccessRequest): String {
        val generatedCode = (100000..999999).random().toString() // Pure numeric 6 digits
        viewModelScope.launch {
            val idBase = System.currentTimeMillis()
            val creator = currentMember.value?.fullName ?: "Líder / Presidente"
            val newCode = InvitationCode(
                id = idBase, // 🛡️ ID Manual Atómico
                code = generatedCode,
                isMaster = false,
                isSpecialGuest = false,
                durationHours = 24,
                createdBy = creator,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                isUsed = false,
                targetRole = MemberRole.MIEMBRO_ACTIVO,
                note = "Código numérico aprobado para ${request.fullName} (${request.phone})"
            )
            repository.insertInvitationCode(newCode)
            repository.updateAccessRequest(
                request.copy(
                    status = "APROBADO",
                    generatedCode = generatedCode,
                    reviewedBy = creator
                )
            )

            // Notify Directiva chat
            val msg = ChatMessage(
                id = idBase + 1, // 🛡️ ID Manual Atómico secuencial
                channelId = "DIRECTIVA",
                senderMemberId = currentMember.value?.id ?: 2,
                senderName = currentMember.value?.fullName ?: "Presidente",
                senderNickname = currentMember.value?.nickname ?: "Líder",
                senderMemberNumber = currentMember.value?.memberNumber ?: "TX-001",
                senderRole = MemberRole.PRESIDENTE,
                senderCustomRoleTitle = "Presidente Nacional",
                senderInitials = "CM",
                messageText = "✅ Solicitud de acceso APROBADA para ${request.fullName}. Código generado: $generatedCode (Vigente por 24h).",
                isRadioCallout = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(msg)
        }
        return generatedCode
    }

    fun rejectAccessRequest(request: AccessRequest) {
        viewModelScope.launch {
            val reviewer = currentMember.value?.fullName ?: "Directiva Nacional"
            repository.updateAccessRequest(
                request.copy(
                    status = "RECHAZADO",
                    reviewedBy = reviewer
                )
            )
        }
    }

    // ---------------- GESTIÓN DE CARGOS Y ROLES DE PILOTOS ---------------- //
    fun transferCargo(fromMember: MemberProfile, toMember: MemberProfile, cargoToTransfer: MemberRole) {
        viewModelScope.launch(Dispatchers.IO) {
            val isTransferringPresidency = cargoToTransfer == MemberRole.PRESIDENTE || fromMember.role == MemberRole.PRESIDENTE

            // Update toMember to the cargo
            val updatedTo = toMember.copy(
                role = cargoToTransfer,
                isDirectiva = cargoToTransfer.canManageApp || cargoToTransfer == MemberRole.PRESIDENTE || cargoToTransfer == MemberRole.CAPITAN_RUTA
            )
            // Demote fromMember to MIEMBRO_ACTIVO
            val updatedFrom = fromMember.copy(
                role = MemberRole.MIEMBRO_ACTIVO,
                isDirectiva = false
            )
            repository.updateMember(updatedTo)
            repository.updateMember(updatedFrom)

            if (isTransferringPresidency) {
                // Ajustar flags de sesión local si alguno es el usuario activo
                if (currentMember.value?.id == fromMember.id) {
                    _isLeaderSuperAdmin.value = false
                    _isDirectivaMode.value = false
                    saveSession(fromMember.id, fromMember.email, fromMember.firebaseUid)
                }
                if (currentMember.value?.id == toMember.id) {
                    _isLeaderSuperAdmin.value = true
                    _isDirectivaMode.value = true
                    saveSession(toMember.id, toMember.email, toMember.firebaseUid)
                }

                // Sincronizar en Firestore
                try {
                    val db = FirebaseFirestore.getInstance()
                    if (!fromMember.email.isNullOrBlank()) {
                        val idSanitizadoFrom = com.example.data.remote.PerfilNube.sanitizarEmailDocId(fromMember.email ?: "")
                        db.collection("vinculos_google").document(idSanitizadoFrom).update(
                            mapOf("role" to "MIEMBRO_ACTIVO", "is_directiva" to false, "codigo_acceso" to "")
                        ).await()
                    }
                    if (!toMember.email.isNullOrBlank()) {
                        val idSanitizadoTo = com.example.data.remote.PerfilNube.sanitizarEmailDocId(toMember.email ?: "")
                        db.collection("vinculos_google").document(idSanitizadoTo).update(
                            mapOf("role" to "PRESIDENTE", "is_directiva" to true)
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.e("TeamTxViewModel", "Error sincronizando transferencia de presidencia en Firestore: ${e.message}")
                }
            }

            // Post notice in Directiva chat and feed
            val transferNotice = if (isTransferringPresidency) {
                "🏛️ TRANSFERENCIA DE LA PRESIDENCIA NACIONAL: ${fromMember.fullName} ha transferido oficialmente la Presidencia Nacional a ${toMember.fullName} (${toMember.nickname} - ${toMember.memberNumber}). El nuevo Presidente asume todas las facultades institucionales."
            } else {
                "🔄 TRANSFERENCIA OFICIAL DE CARGO: ${fromMember.fullName} ha transferido el cargo de '${cargoToTransfer.displayName}' a ${toMember.fullName} (${toMember.nickname} - ${toMember.memberNumber}). Decisión ratificada."
            }
            repository.insertChatMessage(
                ChatMessage(
                    id = System.currentTimeMillis(),
                    channelId = "DIRECTIVA",
                    senderMemberId = currentMember.value?.id ?: 2,
                    senderName = "Tribunal & Gobernanza TX",
                    senderNickname = "Gobernanza",
                    senderMemberNumber = "TX-DIR",
                    senderRole = MemberRole.DISCIPLINARIO,
                    senderCustomRoleTitle = "Gobernanza Institucional",
                    senderInitials = "TX",
                    messageText = transferNotice,
                    isRadioCallout = true,
                    timestamp = System.currentTimeMillis()
                )
            )
            repository.insertPublication(
                Publication(
                    id = System.currentTimeMillis() + 1,
                    title = if (isTransferringPresidency) "Nueva Presidencia Nacional TX" else "Nombramiento y Transferencia: ${cargoToTransfer.displayName}",
                    content = transferNotice,
                    category = NoticeCategory.COMUNICADO,
                    priority = NoticePriority.URGENTE,
                    authorName = fromMember.fullName,
                    authorRole = "Presidencia Saliente TX Aragua",
                    isPinned = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun abandonCargo(member: MemberProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val previousRole = member.role
            val isPresident = previousRole == MemberRole.PRESIDENTE
            val updated = member.copy(
                role = MemberRole.MIEMBRO_ACTIVO,
                isDirectiva = false
            )
            repository.updateMember(updated)

            // Si era Presidente, anular su código y dejar el cargo libre
            if (isPresident) {
                if (currentMember.value?.id == member.id) {
                    _isLeaderSuperAdmin.value = false
                    _isDirectivaMode.value = false
                    saveSession(member.id, member.email, member.firebaseUid)
                }

                // Anular códigos en Firestore
                try {
                    val db = FirebaseFirestore.getInstance()
                    val codesSnapshot = db.collection("invitation_codes")
                        .whereEqualTo("targetRole", "PRESIDENTE")
                        .get().await()
                    for (doc in codesSnapshot.documents) {
                        doc.reference.update(mapOf("status" to "ANULADO", "isUsed" to true)).await()
                    }

                    if (!member.email.isNullOrBlank()) {
                        val idSanitizado = com.example.data.remote.PerfilNube.sanitizarEmailDocId(member.email ?: "")
                        db.collection("vinculos_google").document(idSanitizado).update(
                            mapOf(
                                "role" to "MIEMBRO_ACTIVO",
                                "is_directiva" to false,
                                "codigo_acceso" to ""
                            )
                        ).await()
                    }

                    if (!member.firebaseUid.isNullOrBlank()) {
                        db.collection("usuarios").document(member.firebaseUid ?: "").update(
                            mapOf(
                                "role" to "MIEMBRO_ACTIVO",
                                "isDirectiva" to false
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.e("TeamTxViewModel", "Error al anular código presidencial en Firestore: ${e.message}")
                }
            }

            val notice = if (isPresident) {
                "⚠️ PRESIDENCIA VACANTE Y CÓDIGO ANULADO: ${member.fullName} (${member.nickname}) ha abandonado la Presidencia Nacional. El cargo queda LIBRE y su código de acceso ha sido ANULADO."
            } else {
                "⚠️ CARGO DECLARADO VACANTE: ${member.fullName} (${member.nickname}) ha abandonado/puesto a disposición el cargo de '${previousRole.displayName}'. El Líder Nacional procederá a la reasignación."
            }

            repository.insertChatMessage(
                ChatMessage(
                    id = System.currentTimeMillis(),
                    channelId = "DIRECTIVA",
                    senderMemberId = member.id,
                    senderName = member.fullName,
                    senderNickname = member.nickname,
                    senderMemberNumber = member.memberNumber,
                    senderRole = MemberRole.MIEMBRO_ACTIVO,
                    senderCustomRoleTitle = "Ex-${previousRole.displayName}",
                    senderInitials = member.avatarInitials,
                    messageText = notice,
                    isRadioCallout = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            if (isPresident) {
                repository.insertPublication(
                    Publication(
                        id = System.currentTimeMillis() + 1,
                        title = "Presidencia Nacional Vacante",
                        content = notice,
                        category = NoticeCategory.COMUNICADO,
                        priority = NoticePriority.URGENTE,
                        authorName = "Tribunal Institucional TX",
                        authorRole = "Gobernanza TX Aragua",
                        isPinned = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun assignMemberRole(member: MemberProfile, newRole: MemberRole) {
        val caller = currentMember.value
        val isDevCaller = caller?.role == MemberRole.DESARROLLADOR || caller?.email?.trim()?.lowercase() == "eduardo.androide.em@gmail.com"

        // 🛡️ Jerarquía: Solo el Desarrollador Máster puede asignar o modificar los cargos de Presidente o Desarrollador
        if ((newRole == MemberRole.PRESIDENTE || newRole == MemberRole.DESARROLLADOR || member.role == MemberRole.DESARROLLADOR) && !isDevCaller) {
            Log.w("TeamTxViewModel", "⚠️ Solo el Desarrollador Máster puede designar o modificar el cargo de Presidente o Desarrollador.")
            return
        }

        viewModelScope.launch {
            val updated = member.copy(
                role = newRole,
                isDirectiva = newRole.canManageApp || newRole == MemberRole.PRESIDENTE || newRole == MemberRole.CAPITAN_RUTA || newRole == MemberRole.DESARROLLADOR
            )
            repository.updateMember(updated)

            // Sincronizar en Firestore
            val uid = member.firebaseUid
            if (!uid.isNullOrBlank()) {
                PerfilNube.subirPerfil(uid, updated)
            }

            val assignNotice = "🎖️ ASIGNACIÓN DE CARGO: El Líder ha designado a ${member.fullName} (${member.nickname}) como '${newRole.displayName}'."
            repository.insertChatMessage(
                ChatMessage(
                    id = System.currentTimeMillis(),
                    channelId = "DIRECTIVA",
                    senderMemberId = currentMember.value?.id ?: 2,
                    senderName = currentMember.value?.fullName ?: "Desarrollador Máster",
                    senderNickname = "Líder",
                    senderMemberNumber = "TX-001",
                    senderRole = currentMember.value?.role ?: MemberRole.DESARROLLADOR,
                    senderCustomRoleTitle = currentMember.value?.role?.displayName ?: "Desarrollador Máster",
                    senderInitials = "EM",
                    messageText = assignNotice,
                    isRadioCallout = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleMemberOnlineStatus(member: MemberProfile) {
        viewModelScope.launch {
            val updated = member.copy(
                isOnline = !member.isOnline,
                lastActiveTimestamp = System.currentTimeMillis()
            )
            repository.updateMember(updated)
        }
    }

    // ---------------- EVENT ATTENDANCE ---------------- //
    val allEventAttendance: StateFlow<List<EventAttendance>> = repository.allEventAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAttendance(eventId: Long, memberId: Long, eventName: String) {
        viewModelScope.launch {
            val existing = allEventAttendance.value.find { it.eventId == eventId && it.memberId == memberId }
            if (existing == null) {
                repository.insertEventAttendance(EventAttendance(id = System.currentTimeMillis(), eventId = eventId, memberId = memberId, eventName = eventName))
                val member = repository.getMemberById(memberId)
                if (member != null) {
                    repository.updateMember(member.copy(attendanceCount = member.attendanceCount + 1))
                }
            }
        }
    }

    // ---------------- DISCIPLINARY RECORDS (TRIBUNAL BIKER) ---------------- //
    val allDisciplinaryRecords: StateFlow<List<DisciplinaryRecord>> = repository.allDisciplinaryRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDisciplinaryRecord(memberId: Long, memberName: String, reason: String, penaltyType: String, applySuspension: Boolean, durationDays: Int = 0) {
        viewModelScope.launch {
            val issuer = currentMember.value?.fullName ?: "Oficial Disciplinario"
            repository.insertDisciplinaryRecord(DisciplinaryRecord(id = System.currentTimeMillis(), memberId = memberId, memberName = memberName, reason = reason, penaltyType = penaltyType, issuedBy = issuer))
            
            if (applySuspension) {
                val member = repository.getMemberById(memberId)
                if (member != null) {
                    suspendMember(member, reason, durationDays, issuer)
                }
            }
        }
    }

    // ---------------- DOCUMENT VAULT (GUANTERA DIGITAL) ---------------- //
    fun updateMemberDocuments(licenseUri: String?, medicalCertUri: String?, bikeRegUri: String?, insuranceUri: String?) {
        viewModelScope.launch {
            val member = currentMember.value ?: return@launch
            val updated = member.copy(
                licenseImageUri = licenseUri ?: member.licenseImageUri,
                medicalCertImageUri = medicalCertUri ?: member.medicalCertImageUri,
                bikeRegImageUri = bikeRegUri ?: member.bikeRegImageUri,
                insuranceImageUri = insuranceUri ?: member.insuranceImageUri
            )
            repository.updateMember(updated)
        }
    }

    // ---------------- COPILOTO OFICIAL ---------------- //
    fun updateCopilot(copilotName: String, copilotRelation: String) {
        viewModelScope.launch {
            val member = currentMember.value ?: return@launch
            val updated = member.copy(
                copilotName = copilotName.takeIf { it.isNotBlank() },
                copilotRelation = copilotRelation.takeIf { it.isNotBlank() }
            )
            repository.updateMember(updated)
        }
    }

    // ---------------- FIREBASE & GOOGLE SIGN-IN ---------------- //

    /**
     * Vincula la cuenta Google del usuario con su perfil actual en Carnet TX.
     * Se llama desde el botón "Vincular con Google" en ProfileAndAdminScreen.
     *
     * FLUJO:
     * 1. El usuario toca "Vincular con Google" en Carnet TX
     * 2. Se abre el selector de cuentas de Google
     * 3. Se obtiene el idToken → se llama a AutenticacionNube.iniciarSesionConGoogle
     * 4. Se actualiza el perfil local con firebaseUid y email
     * 5. Se sincroniza con Firestore via PerfilNube
     */
    suspend fun vincularGoogle(uid: String, email: String, googlePhotoUrl: String? = null): Pair<Boolean, String> {
        val member = currentMember.value ?: return Pair(false, "No hay perfil activo para vincular.")
        val emailTrim = email.trim().lowercase()
        val idDispositivoLocal = SEGURIDAD_CUENTAS.obtenerIdDispositivo(getApplication())

        // 1. Llamar primero a VINCULACION.vincularCuentaGoogle (transacción atómica en Firestore)
        val (exitoVinculacion, mensajeVinculacion) = VINCULACION.vincularCuentaGoogle(
            correo = emailTrim,
            uidFirebase = uid,
            numeroMiembro = member.memberNumber,
            nombrePiloto = member.fullName,
            idDispositivo = idDispositivoLocal
        )

        // Solo si esta función retorna éxito, procede a guardar los datos locales en Room y SharedPreferences
        if (!exitoVinculacion) {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}
            return Pair(false, mensajeVinculacion)
        }

        // 2. Guardar los datos locales en Room y SharedPreferences
        val actualizado = member.copy(
            firebaseUid = uid,
            email = emailTrim,
            profilePhotoUri = member.profilePhotoUri ?: googlePhotoUrl
        )
        repository.updateMember(actualizado)
        saveSession(actualizado.id, emailTrim, uid)

        // 3. Forzar sincronización con Firestore y activar escucha en vivo
        forceSyncFromFirebase(uid)
        iniciarSincronizacionDePerfil(uid)
        _sesionDesplazadaPorOtroDispositivo.value = false

        Log.i("GOOGLE_LINK", "✅ Cuenta Google $emailTrim vinculada exitosamente a ${member.memberNumber}")
        return Pair(true, mensajeVinculacion)
    }

    /**
     * Desvincula la cuenta de Google llamando a VINCULACION.desvincularCuentaGoogle.
     * Si es exitosa, limpia los datos del correo localmente y desconecta el perfil.
     */
    suspend fun desvincularGoogle(): Pair<Boolean, String> {
        val member = currentMember.value ?: return Pair(false, "No hay perfil activo para desvincular.")
        val email = member.email ?: ""
        val uid = member.firebaseUid ?: ""

        if (uid.isBlank() && email.isBlank()) {
            return Pair(false, "Este perfil no tiene ninguna cuenta Google vinculada.")
        }

        // 1. Llamar a VINCULACION.desvincularCuentaGoogle pasando numeroMiembro para autorización
        val (exitoDesvinculacion, mensajeDesvinculacion) = VINCULACION.desvincularCuentaGoogle(
            correo = email,
            uidFirebase = uid,
            numeroMiembro = member.memberNumber
        )

        if (!exitoDesvinculacion) {
            return Pair(false, mensajeDesvinculacion)
        }

        try {
            // 2. Apagar telemetría GPS y eliminar posición en radar
            try {
                com.example.radar.TelemetriaGps.desactivar(getApplication())
            } catch (_: Exception) {}

            // 3. Detener escucha de sincronización remota
            detenerSincronizacionDePerfil()

            // 4. Limpiar datos locales y cerrar sesión por completo
            clearSession()
            repository.nukeEverything()
            com.example.ui.preferences.PreferenciasApp.carnetGooglePhotoUrl = null
            com.example.ui.preferences.PreferenciasApp.carnetTipoFoto = "LOCAL"

            // 5. Purgar cachés de imágenes
            try {
                coil.Coil.imageLoader(getApplication()).memoryCache?.clear()
                coil.Coil.imageLoader(getApplication()).diskCache?.clear()
            } catch (_: Exception) {}

            // 6. Cierre forzoso de sesión de Google
            SEGURIDAD_CUENTAS.forzarCierreSesion(getApplication())

            // 7. Resetear estados de sesión reactivos
            _currentMemberId.value = -1L
            _isAuthenticated.value = false
            _isLeaderSuperAdmin.value = false
            _isDirectivaMode.value = false
            _sesionDesplazadaPorOtroDispositivo.value = false

            Log.i("GOOGLE_LINK", "✅ Cuenta Google desvinculada exitosamente. Sistema reiniciado en 0.")
            return Pair(true, "✅ Cuenta desvinculada exitosamente. El correo y código han quedado completamente libres.")
        } catch (e: Exception) {
            Log.e("GOOGLE_LINK", "Error al desvincular cuenta Google localmente: ${e.message}", e)
            return Pair(false, "Error al desconectar perfil localmente: ${e.message}")
        }
    }

    /** Reclama la sesión activa para este dispositivo físico */
    fun reclamarSesionEnEsteDispositivo() {
        viewModelScope.launch(Dispatchers.IO) {
            val member = currentMember.value ?: return@launch
            val email = member.email ?: ""
            val uid = member.firebaseUid ?: ""
            val localDeviceId = SEGURIDAD_CUENTAS.obtenerIdDispositivo(getApplication())

            if (email.isNotBlank() && uid.isNotBlank()) {
                PerfilNube.actualizarDispositivoActivo(email, uid, localDeviceId)
            }
            _sesionDesplazadaPorOtroDispositivo.value = false
            iniciarSincronizacionDePerfil(uid)
            Log.i("SESION_TX", "Sesión reclamada con éxito en este dispositivo ($localDeviceId)")
        }
    }

    /** Cierra sesión cuando el usuario decide salir ante un desplazamiento de sesión */
    fun cerrarSesionPorDesplazamiento() {
        _sesionDesplazadaPorOtroDispositivo.value = false
        logout()
    }

    suspend fun signInWithGoogle(uid: String, email: String, googlePhotoUrl: String? = null): Pair<Boolean, String> {
        // Find existing user by Firebase UID or Email
        val existingByUid = allMembers.value.find { it.firebaseUid == uid }
        val existingByEmail = allMembers.value.find { it.email == email }
        
        val matchedMember = existingByUid ?: existingByEmail

        if (matchedMember != null) {
            val conFoto = if (matchedMember.profilePhotoUri.isNullOrBlank() && !googlePhotoUrl.isNullOrBlank())
                matchedMember.copy(profilePhotoUri = googlePhotoUrl) else matchedMember
            // Update UID/Email if needed
            if (conFoto.firebaseUid != uid || conFoto.email != email || conFoto != matchedMember) {
                repository.updateMember(conFoto.copy(firebaseUid = uid, email = email))
            }
            
            // Log them in
            _currentMemberId.value = matchedMember.id
            _isAuthenticated.value = true
            _isDirectivaMode.value = matchedMember.role == MemberRole.PRESIDENTE || matchedMember.role.canManageApp || matchedMember.isDirectiva
            if (matchedMember.role == MemberRole.PRESIDENTE) _isLeaderSuperAdmin.value = true
            _usuarioEstado.value = if (matchedMember.role == MemberRole.PRESIDENTE || matchedMember.role.canManageApp || matchedMember.isDirectiva) "ACTIVO" else "PENDIENTE"
            
            return Pair(true, "¡Bienvenido de vuelta, ${matchedMember.nickname}!")
        }

        // If not found, we return false so the UI knows to ask for a phone number to link
        return Pair(false, "Cuenta no vinculada. Por favor, ingresa tu número de teléfono registrado.")
    }

    suspend fun linkPhoneToGoogleAccount(uid: String, email: String, inputPhone: String): Pair<Boolean, String> {
        val cleanInput = inputPhone.replace(Regex("[^0-9]"), "")
        val matched = allMembers.value.find { m ->
            val cleanMemPhone = m.phone.replace(Regex("[^0-9]"), "")
            cleanMemPhone.endsWith(cleanInput) || cleanInput.endsWith(cleanMemPhone) || cleanMemPhone == cleanInput
        }

        if (matched != null) {
            // Link account
            repository.updateMember(matched.copy(firebaseUid = uid, email = email))
            
            _currentMemberId.value = matched.id
            _isAuthenticated.value = true
            if (matched.role == MemberRole.PRESIDENTE || matched.role.canManageApp || matched.isDirectiva) {
                _isDirectivaMode.value = true
                if (matched.role == MemberRole.PRESIDENTE) _isLeaderSuperAdmin.value = true
            }
            
            forceSyncFromFirebase(uid)
            return Pair(true, "¡Cuenta vinculada exitosamente! Bienvenido, ${matched.nickname}.")
        }

        return Pair(false, "El número telefónico no está registrado en el Club. Contacta a Directiva.")
    }

    private fun forceSyncFromFirebase(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mid = _currentMemberId.value
            val local = repository.allMembers.first().find { it.id == mid } ?: return@launch
            val remoto = PerfilNube.descargarPerfil(uid)
            if (remoto == null) {
                PerfilNube.crearSiNoExiste(uid, local)
            } else {
                val fusionado = mezclarRemotoEnLocal(local, remoto)
                if (fusionado != local) {
                    repository.updateMember(fusionado)
                    Log.d("PERFIL_SYNC", "Perfil fusionado con la nube tras login")
                }
            }
            iniciarSincronizacionDePerfil(uid)
        }
    }

    /** Fusiona conservando SIEMPRE los datos locales no vacios; la nube solo rellena huecos. */
    private fun mezclarRemotoEnLocal(local: MemberProfile, remoto: MemberProfile): MemberProfile = local.copy(
        fullName = local.fullName.ifBlank { remoto.fullName },
        nickname = local.nickname.ifBlank { remoto.nickname },
        memberNumber = local.memberNumber.ifBlank { remoto.memberNumber },
        cedulaDni = local.cedulaDni.ifBlank { remoto.cedulaDni },
        phone = local.phone.ifBlank { remoto.phone },
        chapterState = local.chapterState.ifBlank { remoto.chapterState },
        birthDate = local.birthDate.ifBlank { remoto.birthDate },
        bikeBrand = local.bikeBrand.ifBlank { remoto.bikeBrand },
        bikeModel = local.bikeModel.ifBlank { remoto.bikeModel },
        bikeColor = local.bikeColor.ifBlank { remoto.bikeColor },
        bikePlate = local.bikePlate.ifBlank { remoto.bikePlate },
        bloodType = local.bloodType.ifBlank { remoto.bloodType },
        medicalNotes = local.medicalNotes.ifBlank { remoto.medicalNotes },
        emergencyContactName = local.emergencyContactName.ifBlank { remoto.emergencyContactName },
        emergencyContactPhone = local.emergencyContactPhone.ifBlank { remoto.emergencyContactPhone },
        copilotName = local.copilotName ?: remoto.copilotName,
        copilotRelation = local.copilotRelation ?: remoto.copilotRelation,
        profilePhotoUri = local.profilePhotoUri ?: remoto.profilePhotoUri,
        bikePhotoUri = local.bikePhotoUri ?: remoto.bikePhotoUri,
        licenseImageUri = local.licenseImageUri ?: remoto.licenseImageUri,
        medicalCertImageUri = local.medicalCertImageUri ?: remoto.medicalCertImageUri,
        bikeRegImageUri = local.bikeRegImageUri ?: remoto.bikeRegImageUri,
        insuranceImageUri = local.insuranceImageUri ?: remoto.insuranceImageUri,
        isDirectiva = local.isDirectiva || remoto.isDirectiva,
        solvencyStatus = local.solvencyStatus && remoto.solvencyStatus,
        attendanceCount = maxOf(local.attendanceCount, remoto.attendanceCount),
        longRidesCount = maxOf(local.longRidesCount, remoto.longRidesCount),
        bigEventsCount = maxOf(local.bigEventsCount, remoto.bigEventsCount)
    )

    // ==========================================
    // 🛒 MERCADO MOTERO (Bazar TX)
    // ==========================================
    val marketplaceItems: StateFlow<List<MarketplaceItem>> = repository.allMarketplaceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createMarketplaceItem(
        title: String,
        description: String,
        category: String,
        priceUsd: Double,
        condition: String,
        imageUri: Uri? = null,
        phone: String = "",
        location: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var uploadedUrl: String? = null
                if (imageUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), imageUri, "mercado"
                    )
                    uploadedUrl = res.url
                }

                val current = currentMember.value
                val item = MarketplaceItem(
                    id = System.currentTimeMillis(),
                    sellerMemberId = current?.id ?: 0L,
                    sellerName = current?.fullName ?: "Miembro TX",
                    sellerNickname = current?.nickname ?: "",
                    sellerPhone = phone.ifBlank { current?.phone ?: "" },
                    sellerLocation = location.ifBlank { current?.chapterState ?: "Aragua" },
                    title = title,
                    description = description,
                    category = category,
                    priceUsd = priceUsd,
                    condition = condition,
                    imageUrl = uploadedUrl,
                    status = "DISPONIBLE",
                    timestamp = System.currentTimeMillis()
                )
                repository.insertMarketplaceItem(item)
                notifyDirectivaChannel(
                    titulo = "NUEVO ARTÍCULO EN MERCADO MOTERO",
                    detalle = "🛒 ${item.sellerName} publicó: '${item.title}' por $${item.priceUsd} USD (${item.category} - ${item.condition}). Ubicación: ${item.sellerLocation}",
                    tipo = "MERCADO"
                )
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error creando artículo en mercado: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun updateMarketplaceItemStatus(item: MarketplaceItem, newStatus: String) {
        viewModelScope.launch {
            repository.updateMarketplaceItem(item.copy(status = newStatus))
        }
    }

    fun deleteMarketplaceItem(id: Long) {
        viewModelScope.launch {
            repository.deleteMarketplaceItem(id)
        }
    }

    // ==========================================
    // 🛠️ BITÁCORA DE MANTENIMIENTO TX
    // ==========================================
    val allMaintenanceLogs: StateFlow<List<MaintenanceLog>> = repository.allMaintenanceLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myMaintenanceLogs: StateFlow<List<MaintenanceLog>> = _currentMemberId
        .flatMapLatest { memberId ->
            if (memberId != null) repository.getMaintenanceLogsForMember(memberId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createMaintenanceLog(
        odometerKm: Int,
        serviceType: String,
        brandOrDetails: String,
        costUsd: Double,
        workshopName: String,
        serviceDate: String,
        nextServiceKm: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val current = currentMember.value ?: return@launch
            val log = MaintenanceLog(
                id = System.currentTimeMillis(),
                memberId = current.id,
                odometerKm = odometerKm,
                serviceType = serviceType,
                brandOrDetails = brandOrDetails,
                costUsd = costUsd,
                workshopName = workshopName,
                serviceDate = serviceDate,
                nextServiceKm = nextServiceKm,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMaintenanceLog(log)
        }
    }

    fun deleteMaintenanceLog(id: Long) {
        viewModelScope.launch {
            repository.deleteMaintenanceLog(id)
        }
    }

    // ==========================================
    // 📍 DIRECTORIO DE TALLERES Y REPUESTOS
    // ==========================================
    val allWorkshops: StateFlow<List<WorkshopDirectoryItem>> = repository.allWorkshops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createWorkshop(
        name: String,
        type: String,
        state: String,
        city: String,
        address: String,
        phone: String,
        whatsapp: String,
        rating: Double,
        notes: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        hasCredit: Boolean = false,
        creditPlatforms: String = "",
        googleMapsUrl: String = ""
    ) {
        viewModelScope.launch {
            val current = currentMember.value
            val item = WorkshopDirectoryItem(
                id = System.currentTimeMillis(),
                name = name,
                type = type,
                state = state,
                city = city,
                address = address,
                phone = phone,
                whatsapp = whatsapp,
                rating = rating,
                recommendedBy = current?.let { "${it.fullName} (${it.nickname})" } ?: "Directiva TX",
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                hasCredit = hasCredit,
                creditPlatforms = creditPlatforms,
                googleMapsUrl = googleMapsUrl,
                timestamp = System.currentTimeMillis()
            )
            repository.insertWorkshop(item)

            // Auto-sincronizar con el mapa táctico de la app
            if (latitude != 0.0 && longitude != 0.0) {
                try {
                    com.example.radar.GestorRadar.sincronizarDirectorioEnMapa(listOf(item), true)
                } catch (_: Exception) {}
            }

            // 📢 1. Generar Aviso Oficial a la Comunidad en el Módulo de Avisos (Muro)
            try {
                val author = current?.fullName ?: "Directiva Team TX"
                val role = if (_isDirectivaMode.value) "Directiva Nacional" else "Directorio Comercial"
                val creditInfo = if (hasCredit) {
                    "💳 Cuenta con Financiamiento: ${creditPlatforms.ifBlank { "Cashea / Rapikom" }}"
                } else {
                    "💵 Modalidad de pago: Contado"
                }
                val contactInfo = buildString {
                    if (phone.isNotBlank()) append("📞 Tel: $phone")
                    if (whatsapp.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("💬 WhatsApp: $whatsapp")
                    }
                }
                val contentAviso = buildString {
                    appendLine("📍 ¡Nuevo establecimiento registrado en el Directorio Comercial y de Servicios!")
                    appendLine("🏪 Comercio: $name")
                    appendLine("🏷️ Tipo de Servicio: $type")
                    appendLine("📍 Ubicación: $city, Edo. $state")
                    if (address.isNotBlank()) appendLine("🏢 Dirección: $address")
                    appendLine(creditInfo)
                    if (notes.isNotBlank()) appendLine("🔧 Especialidad / Stock: $notes")
                    if (contactInfo.isNotBlank()) appendLine(contactInfo)
                    appendLine()
                    append("🗺️ ¡Disponible ahora en el Directorio y con punto de navegación táctico en el Mapa TX!")
                }

                val pubId = System.currentTimeMillis() + 2
                val coordsStr = if (latitude != 0.0 && longitude != 0.0) "$latitude,$longitude" else null
                val locNameStr = "$name - $city, Edo. $state"

                val publication = Publication(
                    id = pubId,
                    title = "🏪 ¡Nuevo Comercio Afiliado! $name",
                    content = contentAviso,
                    category = NoticeCategory.COMUNICADO,
                    priority = NoticePriority.NORMAL,
                    authorName = author,
                    authorRole = role,
                    isPinned = false,
                    targetChallengeDistanceKm = 0,
                    challengeBadgeText = null,
                    telegramPostUrl = null,
                    imageUrl = null,
                    allowComments = true,
                    locationCoordinates = coordsStr,
                    locationName = locNameStr,
                    eventDate = null,
                    eventTime = null,
                    isEventFinished = false,
                    linkedCalendarEventId = null,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPublication(publication)

                // 🔔 2. Notificación en el Módulo Avisos / Centro de Notificaciones
                com.example.GestorNotificacionesApp.notificarAvisoMuro(
                    titulo = "🏪 Nuevo Comercio: $name",
                    contenido = "Se agregó $type en $city ($creditInfo). ¡Revisa sus repuestos y servicios en la app!",
                    referenciaId = pubId.toString()
                )
            } catch (e: Exception) {
                Log.w("TeamTxViewModel", "Error publicando aviso de nuevo comercio: ${e.message}")
            }
        }
    }

    fun updateWorkshop(item: WorkshopDirectoryItem) {
        viewModelScope.launch {
            repository.updateWorkshop(item.copy(timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteWorkshop(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkshop(id)
        }
    }

    // ==========================================
    // 🛂 PASAPORTE MOTERO TX
    // ==========================================
    val passportDestinations: StateFlow<List<PassportDestination>> = repository.allPassportDestinations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPassportStamps: StateFlow<List<PassportStamp>> = repository.allPassportStamps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myPassportStamps: StateFlow<List<PassportStamp>> = _currentMemberId
        .flatMapLatest { memberId ->
            if (memberId != null) repository.getPassportStampsForMember(memberId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun stampPassportDestination(
        destination: PassportDestination,
        proofImageUri: Uri? = null,
        customDate: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var uploadedUrl: String? = null
                if (proofImageUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), proofImageUri, "pasaporte"
                    )
                    uploadedUrl = res.url
                }

                val current = currentMember.value ?: return@launch
                val formattedDate = customDate.ifBlank {
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                }
                val stamp = PassportStamp(
                    id = System.currentTimeMillis(),
                    memberId = current.id,
                    destinationId = destination.id,
                    destinationTitle = destination.title,
                    state = destination.state,
                    stampedDate = formattedDate,
                    proofImageUrl = uploadedUrl,
                    isVerified = true,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPassportStamp(stamp)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error sellando pasaporte: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun deletePassportStamp(id: Long) {
        viewModelScope.launch {
            repository.deletePassportStamp(id)
        }
    }

    // ═══════════════════════════════════════════════
    // 🏆 RETOS MOTEROS (OFICIALES & PERSONALES)
    // ═══════════════════════════════════════════════
    val challenges: StateFlow<List<BikerChallenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val officialChallenges: StateFlow<List<BikerChallenge>> = repository.officialChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChallengeProgress: StateFlow<List<UserChallengeProgress>> = repository.allChallengeProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createChallenge(
        title: String,
        description: String,
        targetKm: Double,
        cruisingSpeed: String,
        badgeName: String,
        startDate: String,
        endDate: String,
        checkpoints: String,
        isOfficial: Boolean = true,
        imageUri: Uri? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var finalImageUrl: String? = null
                if (imageUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), imageUri, "retos"
                    )
                    finalImageUrl = res.url
                }
                val member = currentMember.value
                val challenge = BikerChallenge(
                    id = System.currentTimeMillis(),
                    title = title,
                    description = description,
                    targetKm = targetKm,
                    cruisingSpeed = cruisingSpeed,
                    badgeName = badgeName,
                    startDate = startDate,
                    endDate = endDate,
                    checkpoints = checkpoints,
                    isOfficial = isOfficial,
                    creatorMemberId = member?.id ?: 1L,
                    creatorName = if (isOfficial) "Directiva Nacional TX" else (member?.fullName ?: "Piloto TX"),
                    imageUrl = finalImageUrl,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertChallenge(challenge)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error creando reto: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun updateChallenge(
        challenge: BikerChallenge,
        newImageUri: Uri? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var finalImageUrl = challenge.imageUrl
                if (newImageUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), newImageUri, "retos"
                    )
                    finalImageUrl = res.url
                }
                repository.updateChallenge(challenge.copy(imageUrl = finalImageUrl))
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error actualizando reto: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun deleteChallenge(id: Long) {
        viewModelScope.launch {
            repository.deleteChallenge(id)
        }
    }

    fun saveChallengeProgress(
        progress: UserChallengeProgress,
        sitePhotoUri: Uri? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var finalPhotoUrl = progress.sitePhotoUrl
                if (sitePhotoUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), sitePhotoUri, "toques_retos"
                    )
                    finalPhotoUrl = res.url
                }
                val updated = progress.copy(
                    sitePhotoUrl = finalPhotoUrl,
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertOrUpdateProgress(updated)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error guardando reporte de reto: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun deleteChallengeProgress(id: Long) {
        viewModelScope.launch {
            repository.deleteProgress(id)
        }
    }

    // ═══════════════════════════════════════════════
    // 📅 CALENDARIO MOTERO (RUTAS, CLIMA, LOGÍSTICA, GARAJE Y AVISOS)
    // ═══════════════════════════════════════════════
    val calendarEvents: StateFlow<List<BikerCalendarEvent>> = repository.allCalendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val officialCalendarEvents: StateFlow<List<BikerCalendarEvent>> = repository.officialCalendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCalendarEvent(
        title: String,
        description: String,
        category: String,
        visibility: String,
        eventDate: String,
        eventTime: String,
        departureTime: String,
        originAddress: String,
        destinationAddress: String,
        originLatitude: Double,
        originLongitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double,
        terrainType: String,
        difficultyLevel: String,
        weatherForecast: String,
        roadCaptain: String,
        tailRider: String,
        remindDaysBefore: Int,
        isOfficialClubEvent: Boolean,
        flyerUri: Uri? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var finalFlyerUrl: String? = null
                if (flyerUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), flyerUri, "calendario"
                    )
                    finalFlyerUrl = res.url
                }
                val member = currentMember.value
                val event = BikerCalendarEvent(
                    id = System.currentTimeMillis(),
                    title = title,
                    description = description,
                    category = category,
                    visibility = visibility,
                    eventDate = eventDate,
                    eventTime = eventTime,
                    departureTime = departureTime,
                    originAddress = originAddress,
                    destinationAddress = destinationAddress,
                    originLatitude = originLatitude,
                    originLongitude = originLongitude,
                    destinationLatitude = destinationLatitude,
                    destinationLongitude = destinationLongitude,
                    terrainType = terrainType,
                    difficultyLevel = difficultyLevel,
                    weatherForecast = weatherForecast,
                    roadCaptain = roadCaptain,
                    tailRider = tailRider,
                    rsvpPilotsCount = if (isOfficialClubEvent) 1 else 0,
                    rsvpPilotsList = if (member != null && isOfficialClubEvent) member.id.toString() else "",
                    rsvpPillionsCount = 0,
                    remindDaysBefore = remindDaysBefore,
                    isOfficialClubEvent = isOfficialClubEvent,
                    creatorMemberId = member?.id ?: 1L,
                    creatorName = if (isOfficialClubEvent) "Directiva Nacional TX" else (member?.fullName ?: "Piloto TX"),
                    flyerUrl = finalFlyerUrl,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertCalendarEvent(event)
                if (isOfficialClubEvent) {
                    try {
                        val pub = Publication(
                            id = System.currentTimeMillis() + 1,
                            title = "⭐ NUEVA RODADA / EVENTO: $title",
                            content = buildString {
                                appendLine(description)
                                appendLine()
                                appendLine("📅 Fecha: $eventDate")
                                appendLine("⏰ Concentración: $eventTime | 🚀 Ruedas en Asfalto: $departureTime")
                                if (originAddress.isNotBlank()) appendLine("📍 Salida: $originAddress")
                                if (destinationAddress.isNotBlank()) appendLine("🗺️ Destino: $destinationAddress")
                                if (roadCaptain.isNotBlank()) appendLine("👨‍✈️ Capitán de Ruta: $roadCaptain")
                                if (tailRider.isNotBlank()) appendLine("🛡️ Barredora: $tailRider")
                                if (weatherForecast.isNotBlank()) appendLine("☀️ Clima: $weatherForecast")
                            },
                            category = NoticeCategory.AVISO_OFICIAL,
                            priority = NoticePriority.IMPORTANTE,
                            isPinned = true,
                            imageUrl = finalFlyerUrl,
                            authorName = "Directiva Nacional TX",
                            authorRole = member?.role?.displayName ?: MemberRole.DIRECTIVA.displayName,
                            timestamp = System.currentTimeMillis()
                        )
                        repository.insertPublication(pub)

                        GestorNotificacionesApp.notificarEventoCalendario(
                            titulo = "⭐ Rodada Oficial: $title",
                            fecha = eventDate,
                            hora = eventTime,
                            lugar = originAddress.ifBlank { "Punto de concentración oficial" },
                            horasAntes = remindDaysBefore * 24
                        )
                    } catch (ePub: Exception) {
                        Log.e("TeamTxViewModel", "No se pudo sincronizar aviso con el muro: ${ePub.message}")
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error creando evento de calendario: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun updateCalendarEvent(
        event: BikerCalendarEvent,
        newFlyerUri: Uri? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var finalFlyerUrl = event.flyerUrl
                if (newFlyerUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), newFlyerUri, "calendario"
                    )
                    finalFlyerUrl = res.url
                }
                repository.updateCalendarEvent(event.copy(flyerUrl = finalFlyerUrl))
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error actualizando evento de calendario: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
        }
    }

    fun toggleEventRsvp(event: BikerCalendarEvent, isAttending: Boolean, hasPillion: Boolean) {
        val member = currentMember.value ?: return
        viewModelScope.launch {
            val memberIdStr = member.id.toString()
            val list = event.rsvpPilotsList.split(",").filter { it.isNotBlank() }.toMutableList()
            var currentPillions = event.rsvpPillionsCount

            if (isAttending) {
                if (!list.contains(memberIdStr)) list.add(memberIdStr)
                if (hasPillion) currentPillions += 1
            } else {
                list.remove(memberIdStr)
                if (hasPillion && currentPillions > 0) currentPillions -= 1
            }

            val updatedEvent = event.copy(
                rsvpPilotsCount = list.size,
                rsvpPilotsList = list.joinToString(","),
                rsvpPillionsCount = currentPillions
            )
            repository.updateCalendarEvent(updatedEvent)
        }
    }

    fun toggleEventReminder(event: BikerCalendarEvent, notifyInApp: Boolean) {
        val member = currentMember.value ?: return
        viewModelScope.launch {
            val memberIdStr = member.id.toString()
            val list = event.remindedMemberIds.split(",").filter { it.isNotBlank() }.toMutableList()
            val isAlreadyReminded = list.contains(memberIdStr)

            if (isAlreadyReminded) {
                list.remove(memberIdStr)
            } else {
                list.add(memberIdStr)
                if (notifyInApp) {
                    GestorNotificacionesApp.notificarEventoCalendario(
                        titulo = event.title,
                        fecha = event.eventDate,
                        hora = event.eventTime,
                        lugar = event.originAddress.ifBlank { "Punto de concentración oficial" },
                        horasAntes = event.remindDaysBefore * 24
                    )
                }
            }

            val updated = event.copy(remindedMemberIds = list.joinToString(","))
            repository.updateCalendarEvent(updated)
        }
    }

    fun publishCalendarEventToFeed(event: BikerCalendarEvent, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val member = currentMember.value
                val contentDetails = buildString {
                    appendLine(event.description)
                    appendLine()
                    appendLine("📅 Fecha: ${event.eventDate}")
                    appendLine("⏰ Concentración: ${event.eventTime} | 🏍️ Salida: ${event.departureTime}")
                    if (event.originAddress.isNotBlank()) appendLine("📍 Salida: ${event.originAddress}")
                    if (event.destinationAddress.isNotBlank()) appendLine("🗺️ Destino: ${event.destinationAddress}")
                    if (event.roadCaptain.isNotBlank()) appendLine("👨‍✈️ Capitán de Ruta: ${event.roadCaptain}")
                    if (event.tailRider.isNotBlank()) appendLine("🛡️ Barredora: ${event.tailRider}")
                    if (event.weatherForecast.isNotBlank()) appendLine("☀️ Clima Previsto: ${event.weatherForecast}")
                }

                val pub = Publication(
                    id = System.currentTimeMillis(),
                    title = "📅 EVENTO OFICIAL: ${event.title}",
                    content = contentDetails,
                    category = NoticeCategory.AVISO_OFICIAL,
                    priority = NoticePriority.IMPORTANTE,
                    isPinned = true,
                    imageUrl = event.flyerUrl,
                    authorName = "Directiva Nacional TX",
                    authorRole = member?.role?.displayName ?: MemberRole.DIRECTIVA.displayName,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPublication(pub)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error publicando evento en muro: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    // 🏕️ SITIOS DE INTERÉS TX & MODERACIÓN DE DENUNCIAS
    val allInterestPoints: StateFlow<List<BikerInterestPoint>> = repository.allInterestPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSpotReports: StateFlow<List<SpotReport>> = repository.allSpotReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPuntoTX(
        name: String,
        category: String,
        description: String,
        address: String,
        lat: Double,
        lon: Double,
        imageUri: Uri? = null,
        phone: String = "",
        iconDrawableName: String = "ic_menu_compass",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                var uploadedUrl: String? = null
                if (imageUri != null) {
                    com.aistudio.teamtxvzla.nube.AutenticacionNube.garantizarSesionActiva()
                    val res = com.aistudio.teamtxvzla.nube.NubeMultimedia.subirImagenAvisoAsync(
                        getApplication(), imageUri, "puntos_tx"
                    )
                    uploadedUrl = res.url
                }

                val current = currentMember.value
                val isComercioServicio = category in listOf("Taller Mecánico", "Repuestos", "Autolavado", "Restaurante / Comida", "Posada / Hotel", "Estación de Servicio")

                if (isComercioServicio) {
                    val workshop = WorkshopDirectoryItem(
                        id = System.currentTimeMillis(),
                        name = name,
                        type = category,
                        address = address,
                        phone = phone,
                        whatsapp = phone,
                        notes = description,
                        latitude = lat,
                        longitude = lon,
                        recommendedBy = current?.fullName ?: "Piloto TX",
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertWorkshop(workshop)
                } else {
                    val spot = BikerInterestPoint(
                        id = System.currentTimeMillis(),
                        name = name,
                        category = category,
                        description = description,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        imageUrl = uploadedUrl,
                        iconDrawableName = iconDrawableName,
                        phone = phone,
                        addedBy = current?.fullName ?: "Piloto TX",
                        addedByMemberId = current?.id ?: 0L,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertInterestPoint(spot)
                }
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error creando punto TX: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun likeSpot(spot: BikerInterestPoint) {
        val memberId = currentMember.value?.id ?: return
        viewModelScope.launch {
            val memberIdStr = memberId.toString()
            val likesList = spot.likedByMemberIds.split(",").filter { it.isNotBlank() }.toMutableList()
            val dislikesList = spot.dislikedByMemberIds.split(",").filter { it.isNotBlank() }.toMutableList()

            dislikesList.remove(memberIdStr)
            if (!likesList.contains(memberIdStr)) {
                likesList.add(memberIdStr)
            } else {
                likesList.remove(memberIdStr)
            }

            val updated = spot.copy(
                likesCount = likesList.size,
                dislikesCount = dislikesList.size,
                likedByMemberIds = likesList.joinToString(","),
                dislikedByMemberIds = dislikesList.joinToString(",")
            )
            repository.updateInterestPoint(updated)
        }
    }

    fun dislikeSpot(spot: BikerInterestPoint) {
        val memberId = currentMember.value?.id ?: return
        viewModelScope.launch {
            val memberIdStr = memberId.toString()
            val likesList = spot.likedByMemberIds.split(",").filter { it.isNotBlank() }.toMutableList()
            val dislikesList = spot.dislikedByMemberIds.split(",").filter { it.isNotBlank() }.toMutableList()

            likesList.remove(memberIdStr)
            if (!dislikesList.contains(memberIdStr)) {
                dislikesList.add(memberIdStr)
            } else {
                dislikesList.remove(memberIdStr)
            }

            val updated = spot.copy(
                likesCount = likesList.size,
                dislikesCount = dislikesList.size,
                likedByMemberIds = likesList.joinToString(","),
                dislikedByMemberIds = dislikesList.joinToString(",")
            )
            repository.updateInterestPoint(updated)
        }
    }

    fun submitSpotReport(spotId: Long, spotName: String, spotType: String, reason: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val member = currentMember.value
                val report = SpotReport(
                    id = System.currentTimeMillis(),
                    spotId = spotId,
                    spotName = spotName,
                    spotType = spotType,
                    reporterMemberId = member?.id ?: 0L,
                    reporterName = member?.fullName ?: "Piloto TX",
                    reporterPhone = member?.phone ?: "N/A",
                    reason = reason,
                    status = "PENDIENTE",
                    timestamp = System.currentTimeMillis()
                )
                repository.insertSpotReport(report)

                notifyDirectivaChannel(
                    titulo = "🚨 DENUNCIA DE PUNTO TX RECIBIDA",
                    detalle = "⚠️ El piloto ${report.reporterName} reportó el sitio '$spotName' ($spotType).\n📝 Motivo: $reason\n🔎 Revisa el panel de Moderación de Desarrollador.",
                    tipo = "DENUNCIA"
                )
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error enviando denuncia de sitio: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun dismissReport(report: SpotReport) {
        viewModelScope.launch {
            val reviewer = currentMember.value?.fullName ?: "Desarrollador Máster"
            repository.updateSpotReport(report.copy(status = "DESESTIMADO", reviewedBy = reviewer))
        }
    }

    fun updateSpotInfo(spot: BikerInterestPoint, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateInterestPoint(spot)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "Error actualizando sitio: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun deleteSpotDefinitively(spotId: Long) {
        viewModelScope.launch {
            repository.deleteInterestPoint(spotId)
        }
    }
}




