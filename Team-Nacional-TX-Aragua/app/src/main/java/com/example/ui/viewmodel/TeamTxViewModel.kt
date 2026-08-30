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
import com.example.data.repository.TeamTxRepository
import com.example.GestorNotificacionesApp
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

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = TeamTxRepository(db, viewModelScope)
        GestorNotificacionesApp.inicializar(db)
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

        checkAndRestoreSession()
    }

    fun setChatEnabled(enabled: Boolean) {
        FirebaseFirestore.getInstance().collection("app_settings").document("global")
            .set(mapOf("chatEnabled" to enabled), SetOptions.merge())
    }

    // Directiva mode switch
    private val _isDirectivaMode = MutableStateFlow(false)
    val isDirectivaMode: StateFlow<Boolean> = _isDirectivaMode.asStateFlow()

    fun toggleDirectivaMode() {
        val member = currentMember.value
        val hasPermission = member?.isDirectiva == true || member?.role?.canManageApp == true || _isLeaderSuperAdmin.value
        if (hasPermission) {
            _isDirectivaMode.value = !_isDirectivaMode.value
        }
    }

    // Exchange rate USD -> VES
    private val _bcvRate = MutableStateFlow(36.00)
    val bcvRate: StateFlow<Double> = _bcvRate.asStateFlow()

    fun setBcvRate(rate: Double) {
        if (rate > 0) _bcvRate.value = rate
    }

    // All Members
    val allMembers: StateFlow<List<MemberProfile>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active User Profile (Defaults to first or selected)
    private val _currentMemberId = MutableStateFlow<Long>(1)
    val currentMember: StateFlow<MemberProfile?> = combine(allMembers, _currentMemberId) { members, id ->
        members.find { it.id == id } ?: members.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectMember(memberId: Long) {
        _currentMemberId.value = memberId
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
        viewModelScope.launch {
            repository.updateMember(member.copy(solvencyStatus = !member.solvencyStatus))
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

    fun updateMemberRole(member: MemberProfile, newRole: MemberRole) {

        viewModelScope.launch {
            val isDir = newRole.canManageApp || newRole == MemberRole.DIRECTIVA || newRole == MemberRole.PRESIDENTE || newRole == MemberRole.CAPITAN_RUTA
            repository.updateMember(member.copy(role = newRole, isDirectiva = isDir))
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
        locationName: String? = null
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
                val pub = Publication(
                    id = System.currentTimeMillis(),
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
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPublication(pub)
                _publicationUploadSuccess.value = true
                Log.i("TeamTxViewModel", "✅ Publicación creada con ubicación: ${pub.locationCoordinates} (imagen: $uploadOrigen)")
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
                repository.updatePublication(publication.copy(imageUrl = finalImageUrl))
                _publicationUploadSuccess.value = true
                onComplete(true)
            } catch (e: Exception) {
                Log.e("TeamTxViewModel", "❌ Error actualizando publicación: ${e.message}", e)
                _publicationUploadError.value = "Error actualizando aviso: ${e.message}"
                onComplete(false)
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
        lat: Double = 10.4806,
        lng: Double = -66.9036
    ) {
        viewModelScope.launch {
            val member = currentMember.value
            val alert = EmergencyAlert(
                id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                reporterName = member?.fullName ?: "Hermano Motero TX",
                reporterPhone = member?.phone ?: "+58 412 000 0000",
                memberNumber = member?.memberNumber ?: "TX-SOS",
                emergencyType = emergencyType,
                locationDescription = locationDesc,
                coordinateLat = lat,
                coordinateLng = lng,
                bikeDetails = "${member?.bikeModel ?: "Keeway TX 200"} - Placa: ${member?.bikePlate ?: "N/A"}",
                bloodTypeNeeded = bloodTypeNeeded ?: member?.bloodType,
                details = details,
                status = EmergencyStatus.ACTIVA,
                respondersNotes = "Alerta emitida. Grupo de apoyo y Directiva notificados."
            )
            repository.insertAlert(alert)
            Log.i(TAG_SOS, "🚨 Alerta SOS emitida: ${alert.reporterName} en $locationDesc")

            notifyDirectivaChannel(
                titulo = "EMERGENCIA SOS VIAL",
                detalle = "🚨 ${alert.reporterName} (${alert.memberNumber}) ha emitido una alerta de ${emergencyType.name} en: $locationDesc.\nDetalles: $details",
                tipo = "SOS"
            )

            // Auto-publicación en el Muro como Aviso Oficial Urgente
            postSystemNotice(
                title = "🚨 ALERTA SOS VIAL: ${alert.reporterName} en $locationDesc",
                content = "⚠️ *Tipo de Emergencia:* ${emergencyType.name}\n" +
                        "📍 *Ubicación:* $locationDesc\n" +
                        "🏍️ *Vehículo:* ${alert.bikeDetails}\n" +
                        (if (!alert.bloodTypeNeeded.isNullOrBlank()) "🩸 *Tipo de Sangre:* ${alert.bloodTypeNeeded}\n" else "") +
                        "📝 *Detalles:* $details\n\n" +
                        "📲 Contacto: ${alert.reporterPhone}. Hermanos moteros en la zona, prestar asistencia inmediata.",
                category = NoticeCategory.AVISO_OFICIAL,
                priority = NoticePriority.URGENTE,
                isPinned = true
            )
        }
    }

    fun updateAlertStatus(alert: EmergencyAlert, newStatus: EmergencyStatus, notes: String) {
        viewModelScope.launch {
            repository.updateAlert(alert.copy(status = newStatus, respondersNotes = notes))
            Log.i(TAG_SOS, "🚨 Alerta SOS ${alert.id} actualizada a $newStatus")

            if (newStatus == EmergencyStatus.RESUELTA || newStatus == EmergencyStatus.ATENDIDA) {
                postSystemNotice(
                    title = "✅ SOS VIAL RESUELTO: ${alert.reporterName}",
                    content = "La alerta de emergencia en ${alert.locationDescription} ha sido atendida con éxito.\n" +
                            "📋 Notas: ${notes.ifBlank { "Hermano asistido por el equipo de ruta." }}\n" +
                            "¡Gracias a la hermandad por la respuesta y solidaridad!",
                    category = NoticeCategory.COMUNICADO,
                    priority = NoticePriority.NORMAL
                )
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
                senderPhotoUrl = member?.profilePhotoUri, // 📸 Foto configurada (Google o Manual)
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

    // ---------------- AUTHENTICATION & GATEKEEPER ---------------- //
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isLeaderSuperAdmin = MutableStateFlow(false)
    val isLeaderSuperAdmin: StateFlow<Boolean> = _isLeaderSuperAdmin.asStateFlow()

    private val _requestAttemptsLeft = MutableStateFlow(prefs.getInt("PREF_REQUEST_ATTEMPTS_LEFT", 3))
    val requestAttemptsLeft: StateFlow<Int> = _requestAttemptsLeft.asStateFlow()

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

        perfilListener = PerfilNube.escucharPerfil(uid) { remoto ->
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
                val googlePhoto = firebaseUser?.photoUrl?.toString()
                if (finalMember.profilePhotoUri.isNullOrBlank() && !googlePhoto.isNullOrBlank()) {
                    finalMember = finalMember.copy(profilePhotoUri = googlePhoto)
                    repository.updateMember(finalMember)
                }
                _currentMemberId.value = finalMember.id
                _isAuthenticated.value = true
                if (finalMember.role == MemberRole.PRESIDENTE || finalMember.role.canManageApp || finalMember.isDirectiva) {
                    _isDirectivaMode.value = true
                    if (finalMember.role == MemberRole.PRESIDENTE) _isLeaderSuperAdmin.value = true
                }
                saveSession(finalMember.id, finalMember.email, finalMember.firebaseUid ?: savedUid)
                iniciarSincronizacionDePerfil(finalMember.firebaseUid ?: savedUid)
                
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
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}
            detenerSincronizacionDePerfil()
            clearSession()
            _isAuthenticated.value = false
            _isLeaderSuperAdmin.value = false
            _isDirectivaMode.value = false
        }
    }

    fun loginWithPhone(inputPhone: String): Boolean {
        val cleanInput = inputPhone.replace(Regex("[^0-9]"), "")
        val matched = allMembers.value.find { m ->
            val cleanMemPhone = m.phone.replace(Regex("[^0-9]"), "")
            cleanMemPhone.endsWith(cleanInput) || cleanInput.endsWith(cleanMemPhone) || cleanMemPhone == cleanInput
        }
        return if (matched != null) {
            _currentMemberId.value = matched.id
            _isAuthenticated.value = true
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

    suspend fun loginWithGoogle(email: String, firebaseUid: String? = null, googlePhotoUrl: String? = null): Pair<Boolean, String> {
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

        // 3. Developer / President account special handling
        if (matched == null && cleanEmail == "eduardo.androide.em@gmail.com") {
            val president = localMembers.find { it.role == MemberRole.PRESIDENTE }
            if (president != null) {
                matched = president.copy(email = cleanEmail, firebaseUid = authUid)
                repository.updateMember(matched)
            } else {
                val newPresident = MemberProfile(
                    id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                    fullName = "Eduardo Androide",
                    nickname = "Dev TX",
                    memberNumber = "TX-001",
                    role = MemberRole.PRESIDENTE,
                    isDirectiva = true,
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
                repository.insertMember(newPresident)
                matched = newPresident
            }
        }

        if (matched != null) {
            val conFoto = if (matched.profilePhotoUri.isNullOrBlank() && !googlePhotoUrl.isNullOrBlank())
                matched.copy(profilePhotoUri = googlePhotoUrl) else matched
            val updated = conFoto.copy(
                email = cleanEmail,
                firebaseUid = authUid ?: conFoto.firebaseUid
            )
            repository.updateMember(updated)
            _currentMemberId.value = updated.id
            _isAuthenticated.value = true
            if (updated.role == MemberRole.PRESIDENTE || updated.role.canManageApp || updated.isDirectiva) {
                _isDirectivaMode.value = true
                if (updated.role == MemberRole.PRESIDENTE) _isLeaderSuperAdmin.value = true
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

        val isPresidentCode = codeTrim.equals("TX19554402", ignoreCase = true)
        val isDevCode1 = codeTrim.equals("TX19554402SB", ignoreCase = true)
        val isDevCode2 = codeTrim.equals("19554402SB", ignoreCase = true) && phone.trim() == "04243769999"
        val isTestPilotCode = codeTrim.equals("PILOTO19", ignoreCase = true) || codeTrim.equals("PILOT019", ignoreCase = true)
        val isTestAdminCode = codeTrim.equals("TX-TEST-ADMIN", ignoreCase = true)
        val isTestDirectivaCode = codeTrim.equals("TX-TEST-DIRECTIVA", ignoreCase = true)
        val isTestPilotoCode = codeTrim.equals("TX-TEST-PILOTO", ignoreCase = true)
        val isTestGoogleCode = isTestAdminCode || isTestDirectivaCode || isTestPilotoCode
        
        val isMasterCode = isPresidentCode || isDevCode1 || isDevCode2

        if (isTestPilotCode) {
            val pilot = allMembers.value.find { it.memberNumber == "TX-999" }
            if (pilot != null) {
                _currentMemberId.value = pilot.id
            } else {
                val newProfile = MemberProfile(
                    id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                    fullName = if (fullName.isNotBlank()) fullName else "Piloto de Pruebas",
                    nickname = "Tester",
                    memberNumber = "TX-999",
                    role = MemberRole.MIEMBRO_ACTIVO,
                    isDirectiva = false,
                    solvencyStatus = true,
                    avatarInitials = "PP"
                )
                repository.insertMember(newProfile)
                _currentMemberId.value = newProfile.id
            }
            
            // 🛡️ Asegurar login anónimo para que Firebase no bloquee las fotos del tester
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously().await()
                } catch (e: Exception) {
                    Log.e("TeamTxViewModel", "Error Auth Anónima para Tester: ${e.message}")
                }
            }

            _isDirectivaMode.value = false
            _isLeaderSuperAdmin.value = false
            _isAuthenticated.value = true
            
            // Guardar sesión para que no se pierda al rotar o reiniciar
            saveSession(_currentMemberId.value, "tester@teamtx.com", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
            
            return Pair(true, "Acceso concedido como Piloto de Pruebas.")
        }

        // 1. Check Master Codes
        if (isMasterCode) {
            val president = allMembers.value.find { it.role == MemberRole.PRESIDENTE }
            if (president != null) {
                _currentMemberId.value = president.id
            } else {
                // Create a president profile if none exists
                val newPresident = MemberProfile(
                    id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                    fullName = if (isPresidentCode) "Presidente TX" else "Eduardo Androide",
                    nickname = if (isPresidentCode) "Presidente" else "Dev TX",
                    memberNumber = "TX-001",
                    role = MemberRole.PRESIDENTE,
                    isDirectiva = true,
                    solvencyStatus = true,
                    email = "eduardo.androide.em@gmail.com",
                    avatarInitials = if (isPresidentCode) "PR" else "EA"
                )
                repository.insertMember(newPresident)
                _currentMemberId.value = newPresident.id
            }
            _isDirectivaMode.value = true
            _isLeaderSuperAdmin.value = true
            _isAuthenticated.value = true
            
            // Asegurar autenticación en Firebase para códigos maestros
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener { Log.d("TeamTxViewModel", "Login Maestro: Auth anónima exitosa") }
            }

            val memberForSession = allMembers.value.find { it.role == MemberRole.PRESIDENTE }
            saveSession(memberForSession?.id ?: _currentMemberId.value, "eduardo.androide.em@gmail.com", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)

            return Pair(true, "¡Acceso Supremo Concedido!")
        }

        // 1.5 Códigos de prueba Google (asignan rol específico)
        if (isTestGoogleCode) {
            val targetRole = when {
                isTestAdminCode -> MemberRole.PRESIDENTE
                isTestDirectivaCode -> MemberRole.DIRECTIVA
                isTestPilotoCode -> MemberRole.MIEMBRO_ACTIVO
                else -> MemberRole.MIEMBRO_ACTIVO
            }
            val isDirectiva = targetRole == MemberRole.PRESIDENTE || targetRole == MemberRole.DIRECTIVA

            val existing = allMembers.value.find { it.role == targetRole }
            if (existing != null) {
                _currentMemberId.value = existing.id
            } else {
                val newProfile = MemberProfile(
                    id = System.currentTimeMillis(),
                    fullName = when (targetRole) {
                        MemberRole.PRESIDENTE -> "Admin de Prueba"
                        MemberRole.DIRECTIVA -> "Directivo de Prueba"
                        else -> "Piloto de Prueba Google"
                    },
                    nickname = when (targetRole) {
                        MemberRole.PRESIDENTE -> "Admin Test"
                        MemberRole.DIRECTIVA -> "Directiva Test"
                        else -> "Piloto Test"
                    },
                    memberNumber = when (targetRole) {
                        MemberRole.PRESIDENTE -> "TX-TEST-001"
                        MemberRole.DIRECTIVA -> "TX-TEST-002"
                        else -> "TX-TEST-003"
                    },
                    role = targetRole,
                    isDirectiva = isDirectiva,
                    solvencyStatus = true,
                    avatarInitials = when (targetRole) {
                        MemberRole.PRESIDENTE -> "AT"
                        MemberRole.DIRECTIVA -> "DT"
                        else -> "PT"
                    }
                )
                repository.insertMember(newProfile)
                _currentMemberId.value = newProfile.id
            }

            _isDirectivaMode.value = isDirectiva
            _isLeaderSuperAdmin.value = targetRole == MemberRole.PRESIDENTE
            _isAuthenticated.value = true

            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener { Log.d("TeamTxViewModel", "Login Test Code: Auth anónima exitosa") }
            }

            saveSession(_currentMemberId.value, "test@teamtx.com", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)

            return Pair(true, "Acceso concedido — Rol: ${targetRole.displayName}")
        }

        // 2. Check Database Invitation Codes
        val inv = repository.getInvitationByCode(codeTrim)
        if (inv != null) {
            val now = System.currentTimeMillis()
            if (!inv.isMaster && inv.expiresAt < now) {
                return Pair(false, "El código de invitación ha expirado (${inv.durationHours} horas de vigencia). Solicita uno nuevo.")
            }
            if (inv.isUsed && !inv.isMaster) {
                return Pair(false, "Este código ya fue utilizado previamente.")
            }

            val isSpecialGuest = inv.isSpecialGuest || inv.targetRole == MemberRole.INVITADO || !codeTrim.all { it.isDigit() }
            val roleEnum = if (isSpecialGuest) MemberRole.INVITADO else (if (role.equals("Copiloto", ignoreCase = true)) MemberRole.COPILOTO else inv.targetRole)

            // Register or activate member
            val existing = allMembers.value.find { m ->
                val cleanMemPhone = m.phone.replace(Regex("[^0-9]"), "")
                val cleanInPhone = phone.replace(Regex("[^0-9]"), "")
                cleanInPhone.isNotBlank() && cleanMemPhone.endsWith(cleanInPhone)
            }

            if (existing != null) {
                val updatedExisting = existing.copy(
                    role = roleEnum,
                    isSuspended = false,
                    isOnline = true
                )
                repository.updateMember(updatedExisting)
                _currentMemberId.value = existing.id
            } else {
                val initials = if (fullName.isNotBlank()) fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase() else if (isSpecialGuest) "INV" else "TX"
                val newMem = MemberProfile(
                    id = System.currentTimeMillis(), // 🛡️ ID Manual Atómico
                    fullName = fullName.ifBlank { if (isSpecialGuest) "Invitado Especial" else "Piloto (Registro Pendiente)" },
                    nickname = if (isSpecialGuest) "Invitado" else "Piloto",
                    memberNumber = if (isSpecialGuest) "TX-INV-${(100..999).random()}" else "TX-${(100..999).random()}",
                    cedulaDni = "",
                    phone = phone,
                    role = roleEnum,
                    chapterState = "Venezuela",
                    birthDate = birthDate,
                    bikeBrand = "Keeway",
                    bikeModel = bikeModel.ifBlank { "TX 200 SM" },
                    bikePlate = bikePlate.ifBlank { "SIN-PLACA" },
                    emergencyContactName = "",
                    emergencyContactPhone = "",
                    isDirectiva = !isSpecialGuest && inv.targetRole.canManageApp,
                    avatarInitials = initials,
                    isOnline = true
                )
                repository.insertMember(newMem)
                _currentMemberId.value = newMem.id
            }

            // Mark code as used if not master
            if (!inv.isMaster) {
                repository.updateInvitationCode(
                    inv.copy(
                        isUsed = true,
                        usedByName = fullName.ifBlank { if (isSpecialGuest) "Invitado Especial" else "Piloto (Registro Pendiente)" },
                        usedByPhone = phone
                    )
                )
            }

            // Notify Directiva if it is a Special Guest
            if (isSpecialGuest) {
                val alertMsg = ChatMessage(
                    id = System.currentTimeMillis(),
                    channelId = "DIRECTIVA",
                    senderMemberId = 0,
                    senderName = "SISTEMA DE CONTROL TX",
                    senderNickname = "Bot Seguridad",
                    senderMemberNumber = "TX-GATE",
                    senderRole = MemberRole.DISCIPLINARIO,
                    senderCustomRoleTitle = "Control de Acceso Temporal",
                    senderInitials = "TX",
                    messageText = "🟡 INVITADO TEMPORAL ACTIVO: Ingresó '${fullName.ifBlank { "Invitado Especial" }}' (Tel: $phone) con código especial '$codeTrim'. Acceso vigente por ${inv.durationHours}h. La directiva puede DAR DE BAJA su acceso en cualquier momento desde el panel.",
                    isRadioCallout = true,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertChatMessage(alertMsg)
            }

            // Asegurar autenticación en Firebase para Invitaciones
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener { Log.d("TeamTxViewModel", "Login con Código: Auth anónima exitosa") }
            }

            _isAuthenticated.value = true
            val memberForSession2 = allMembers.value.find { it.id == _currentMemberId.value }
            saveSession(_currentMemberId.value, memberForSession2?.email, com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
            return Pair(true, if (isSpecialGuest) "¡Acceso concedido como Invitado Especial! Bienvenido." else "¡Código verificado con éxito! Bienvenido a Team Nacional TX Aragua.")
        }

        return Pair(false, "Código no válido o no encontrado. Verifica con la Directiva.")
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
        viewModelScope.launch {
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

            // Post notice in Directiva chat and feed
            val transferNotice = "🔄 TRANSFERENCIA OFICIAL DE CARGO: ${fromMember.fullName} ha transferido el cargo de '${cargoToTransfer.displayName}' a ${toMember.fullName} (${toMember.nickname} - ${toMember.memberNumber}). Decisión ratificada."
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
                    title = "Nombramiento y Transferencia: ${cargoToTransfer.displayName}",
                    content = transferNotice,
                    category = NoticeCategory.COMUNICADO,
                    priority = NoticePriority.IMPORTANTE,
                    authorName = currentMember.value?.fullName ?: "Presidente Nacional",
                    authorRole = "Directiva Nacional TX Aragua",
                    isPinned = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun abandonCargo(member: MemberProfile) {
        viewModelScope.launch {
            val previousRole = member.role
            val updated = member.copy(
                role = MemberRole.MIEMBRO_ACTIVO,
                isDirectiva = false
            )
            repository.updateMember(updated)

            val notice = "⚠️ CARGO DECLARADO VACANTE: ${member.fullName} (${member.nickname}) ha abandonado/puesto a disposición el cargo de '${previousRole.displayName}'. El Líder Nacional procederá a la reasignación."
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
        }
    }

    fun assignMemberRole(member: MemberProfile, newRole: MemberRole) {
        viewModelScope.launch {
            val updated = member.copy(
                role = newRole,
                isDirectiva = newRole.canManageApp || newRole == MemberRole.PRESIDENTE || newRole == MemberRole.CAPITAN_RUTA
            )
            repository.updateMember(updated)

            val assignNotice = "🎖️ ASIGNACIÓN DE CARGO: El Líder ha designado a ${member.fullName} (${member.nickname}) como '${newRole.displayName}'."
            repository.insertChatMessage(
                ChatMessage(
                    id = System.currentTimeMillis(),
                    channelId = "DIRECTIVA",
                    senderMemberId = currentMember.value?.id ?: 2,
                    senderName = currentMember.value?.fullName ?: "Presidente",
                    senderNickname = "Líder",
                    senderMemberNumber = "TX-001",
                    senderRole = MemberRole.PRESIDENTE,
                    senderCustomRoleTitle = "Presidente Nacional",
                    senderInitials = "CM",
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
        val member = currentMember.value
        if (member == null) {
            return Pair(false, "No hay perfil activo para vincular.")
        }

        // Actualizar el perfil local con los datos de Google
        val actualizado = member.copy(
            firebaseUid = uid,
            email = email,
            profilePhotoUri = member.profilePhotoUri ?: googlePhotoUrl
        )
        repository.updateMember(actualizado)

        // Forzar sincronización con Firestore
        forceSyncFromFirebase(uid)

        Log.i("GOOGLE_LINK", "✅ Cuenta Google vinculada al perfil: ${member.nickname} (UID: $uid)")
        return Pair(true, "¡Cuenta Google vinculada exitosamente! Tus datos se sincronizan con la nube.")
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
            if (matchedMember.role == MemberRole.PRESIDENTE || matchedMember.role.canManageApp || matchedMember.isDirectiva) {
                _isDirectivaMode.value = true
                if (matchedMember.role == MemberRole.PRESIDENTE) _isLeaderSuperAdmin.value = true
            }
            
            // Force sync their data from Firestore (mocked logic for modularity)
            forceSyncFromFirebase(uid)
            
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
        longitude: Double = 0.0
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
                timestamp = System.currentTimeMillis()
            )
            repository.insertWorkshop(item)
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
}




