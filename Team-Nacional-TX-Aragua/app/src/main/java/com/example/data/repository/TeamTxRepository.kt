package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TeamTxRepository(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    // Initialize all sync managers
    private val memberSync = MemberSync(database, scope)
    private val publicationSync = PublicationSync(database, scope)
    private val rideSync = RideSync(database, scope)
    private val rideRegistrationSync = RideRegistrationSync(database, scope)
    private val financeSync = FinancialTransactionSync(database, scope)
    private val inventoryItemSync = InventoryItemSync(database, scope)
    private val equipmentLoanSync = EquipmentLoanSync(database, scope)
    private val emergencyAlertSync = EmergencyAlertSync(database, scope)
    private val roleConfigSync = RoleConfigSync(database, scope)
    private val noticeCommentSync = NoticeCommentSync(database, scope)
    private val firebaseChatSync = FirebaseChatSync(database, scope)
    private val invitationCodeSync = InvitationCodeSync(database, scope)
    private val accessRequestSync = AccessRequestSync(database, scope)
    private val privateGroupSync = FirebasePrivateGroupSync(database, scope)

    // Private Groups - Synced with Firestore
    val allPrivateGroups: Flow<List<PrivateGroup>> = database.privateGroupDao().getAllPrivateGroups()
    fun getPrivateGroupById(id: String): Flow<PrivateGroup?> = database.privateGroupDao().getGroupFlowById(id)
    suspend fun insertPrivateGroup(group: PrivateGroup) = privateGroupSync.saveGroup(group)
    suspend fun updatePrivateGroup(group: PrivateGroup) = privateGroupSync.saveGroup(group)
    suspend fun deletePrivateGroup(id: String) = privateGroupSync.deleteGroup(id)
    suspend fun toggleBlockPrivateGroup(id: String, isBlocked: Boolean, reason: String, blockedBy: String) =
        privateGroupSync.toggleBlockGroup(id, isBlocked, reason, blockedBy)
    fun ensureChatChannelListener(channelId: String) = firebaseChatSync.ensureChannelListener(channelId)

    // Members - Synced with Firestore
    val allMembers: Flow<List<MemberProfile>> = memberSync.getAll()
    suspend fun getMemberById(id: Long) = database.memberDao().getMemberById(id)
    suspend fun insertMember(member: MemberProfile) = memberSync.insertOrUpdate(member)
    suspend fun insertMemberAndGetId(member: MemberProfile): Long {
        val id = database.memberDao().insertMember(member)
        memberSync.insertOrUpdate(member.copy(id = id))
        return id
    }
    suspend fun updateMember(member: MemberProfile) = memberSync.insertOrUpdate(member)
    suspend fun deleteMember(member: MemberProfile) = memberSync.delete(member)
    suspend fun nukeEverything() = database.globalNukeDao().nukeSensitiveData()

    // Publications & Retos - Synced with Firestore
    val allPublications: Flow<List<Publication>> = publicationSync.getAll()
    suspend fun insertPublication(publication: Publication) = publicationSync.insertOrUpdate(publication)
    suspend fun updatePublication(publication: Publication) = publicationSync.insertOrUpdate(publication)
    suspend fun deletePublication(id: Long) {
        database.publicationDao().deletePublicationById(id)
        database.noticeCommentDao().deleteCommentsByPublicationId(id)
        publicationSync.deleteById(id)
        noticeCommentSync.deleteCommentsForPublication(id)
    }
    suspend fun deleteAllPublications() = database.publicationDao().deleteAllPublications()
    suspend fun refreshPublications() = publicationSync.refreshFromFirestore()

    // Rides & Registrations - Synced with Firestore
    val allRides: Flow<List<RideEvent>> = rideSync.getAll()
    val allRegistrations: Flow<List<RideRegistration>> = rideRegistrationSync.getAll()
    fun getRegistrationsForRide(rideId: Long): Flow<List<RideRegistration>> = database.rideDao().getRegistrationsForRide(rideId)
    suspend fun insertRide(ride: RideEvent) = rideSync.insertOrUpdate(ride)
    suspend fun updateRide(ride: RideEvent) = rideSync.insertOrUpdate(ride)
    suspend fun deleteRide(id: Long) {
        database.rideDao().deleteRideById(id)
        database.rideDao().deleteRegistrationsByRideId(id)
        rideSync.deleteById(id)
    }
    suspend fun updateRegistration(registration: RideRegistration) = rideRegistrationSync.insertOrUpdate(registration)
    suspend fun registerForRide(registration: RideRegistration) {
        rideRegistrationSync.insertOrUpdate(registration)
        val ride = database.rideDao().getRideById(registration.rideId)
        if (ride != null) {
            rideSync.insertOrUpdate(ride.copy(registeredCount = ride.registeredCount + 1))
        }
    }
    suspend fun cancelRegistration(rideId: Long, memberId: Long) {
        val registration = allRegistrations.first().find { it.rideId == rideId && it.memberId == memberId }
        registration?.let { rideRegistrationSync.delete(it) }
        val ride = database.rideDao().getRideById(rideId)
        if (ride != null && ride.registeredCount > 0) {
            rideSync.insertOrUpdate(ride.copy(registeredCount = ride.registeredCount - 1))
        }
    }

    // Finances - Synced with Firestore
    val allTransactions: Flow<List<FinancialTransaction>> = financeSync.getAll()
    suspend fun insertTransaction(transaction: FinancialTransaction) = financeSync.insertOrUpdate(transaction)
    suspend fun updateTransaction(transaction: FinancialTransaction) = financeSync.insertOrUpdate(transaction)
    suspend fun deleteTransaction(id: Long) = database.financeDao().deleteTransaction(id)

    // Inventory & Loans - Synced with Firestore
    val allInventoryItems: Flow<List<InventoryItem>> = inventoryItemSync.getAll()
    val allLoans: Flow<List<EquipmentLoan>> = equipmentLoanSync.getAll()
    suspend fun insertItem(item: InventoryItem) = inventoryItemSync.insertOrUpdate(item)
    suspend fun updateItem(item: InventoryItem) = inventoryItemSync.insertOrUpdate(item)
    suspend fun insertLoan(loan: EquipmentLoan) = equipmentLoanSync.insertOrUpdate(loan)
    suspend fun updateLoan(loan: EquipmentLoan) = equipmentLoanSync.insertOrUpdate(loan)

    // Emergency Alerts - Synced with Firestore
    val allAlerts: Flow<List<EmergencyAlert>> = emergencyAlertSync.getAll()
    suspend fun insertAlert(alert: EmergencyAlert) = emergencyAlertSync.insertOrUpdate(alert)
    suspend fun updateAlert(alert: EmergencyAlert) = emergencyAlertSync.insertOrUpdate(alert)
    suspend fun deleteAlert(id: Long) = database.emergencyDao().deleteAlert(id)

    // Role Configs - Synced with Firestore
    val allRoleConfigs: Flow<List<RoleConfig>> = roleConfigSync.getAll()
    suspend fun getRoleConfig(roleKey: String) = database.roleConfigDao().getRoleConfig(roleKey)
    suspend fun insertRoleConfig(config: RoleConfig) = roleConfigSync.insertOrUpdate(config)
    suspend fun updateRoleConfig(config: RoleConfig) = roleConfigSync.insertOrUpdate(config)

    // Notice Comments - Synced with Firestore
    val allComments: Flow<List<NoticeComment>> = noticeCommentSync.getAll()
    fun getCommentsForPublication(pubId: Long): Flow<List<NoticeComment>> = database.noticeCommentDao().getCommentsForPublication(pubId)
    suspend fun insertComment(comment: NoticeComment) = noticeCommentSync.insertOrUpdate(comment)
    suspend fun deleteComment(id: Long) = database.noticeCommentDao().deleteComment(id)

    // Club Chat - Uses Firebase Sync
    val allChatMessages: Flow<List<ChatMessage>> = firebaseChatSync.getAllMessages()
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessage>> = firebaseChatSync.getMessagesForChannel(channelId)
    suspend fun insertChatMessage(message: ChatMessage) = firebaseChatSync.sendMessage(message)
    suspend fun updateChatMessage(message: ChatMessage) = firebaseChatSync.sendMessage(message)
    suspend fun updateMessageReactions(messageId: Long, channelId: String, reactions: String) = firebaseChatSync.updateMessageReactions(messageId, channelId, reactions)
    suspend fun markChatMessagesAsRead(messages: List<ChatMessage>) = firebaseChatSync.markMessagesAsRead(messages)
    suspend fun deleteChatMessage(id: Long) = firebaseChatSync.deleteMessage(id)
    suspend fun clearChatChannel(channelId: String) = firebaseChatSync.clearChannel(channelId)

    // Invitation Codes - Synced with Firestore
    val allInvitationCodes: Flow<List<InvitationCode>> = invitationCodeSync.getAll()
    suspend fun getInvitationByCode(code: String) = database.invitationCodeDao().getInvitationByCode(code)
    suspend fun insertInvitationCode(invitation: InvitationCode) = invitationCodeSync.insertOrUpdate(invitation)
    suspend fun updateInvitationCode(invitation: InvitationCode) = invitationCodeSync.insertOrUpdate(invitation)
    suspend fun deleteInvitationCode(id: Long) = database.invitationCodeDao().deleteInvitationCodeById(id)

    // Access Requests - Synced with Firestore
    val allAccessRequests: Flow<List<AccessRequest>> = accessRequestSync.getAll()
    val pendingAccessRequests: Flow<List<AccessRequest>> = database.accessRequestDao().getPendingAccessRequests()
    suspend fun insertAccessRequest(request: AccessRequest) = accessRequestSync.insertOrUpdate(request)
    suspend fun updateAccessRequest(request: AccessRequest) = accessRequestSync.insertOrUpdate(request)
    suspend fun deleteAccessRequest(id: Long) = database.accessRequestDao().deleteAccessRequest(id)

    // Event Attendance
    val allEventAttendance: Flow<List<EventAttendance>> = database.eventAttendanceDao().getAllAttendance()
    fun getAttendanceForMember(memberId: Long): Flow<List<EventAttendance>> = database.eventAttendanceDao().getAttendanceForMember(memberId)
    suspend fun insertEventAttendance(attendance: EventAttendance) = database.eventAttendanceDao().insertAttendance(attendance)
    suspend fun deleteEventAttendance(attendance: EventAttendance) = database.eventAttendanceDao().deleteAttendance(attendance)

    // Disciplinary Records
    val allDisciplinaryRecords: Flow<List<DisciplinaryRecord>> = database.disciplinaryRecordDao().getAllRecords()
    fun getDisciplinaryRecordsForMember(memberId: Long): Flow<List<DisciplinaryRecord>> = database.disciplinaryRecordDao().getRecordsForMember(memberId)
    suspend fun insertDisciplinaryRecord(record: DisciplinaryRecord) = database.disciplinaryRecordDao().insertRecord(record)
    suspend fun deleteDisciplinaryRecord(record: DisciplinaryRecord) = database.disciplinaryRecordDao().deleteRecord(record)

    // 🛒 Mercado Motero
    private val marketplaceSync = MarketplaceSync(database, scope)
    val allMarketplaceItems: Flow<List<MarketplaceItem>> = marketplaceSync.getAll()
    suspend fun insertMarketplaceItem(item: MarketplaceItem) = marketplaceSync.insertOrUpdate(item)
    suspend fun updateMarketplaceItem(item: MarketplaceItem) = marketplaceSync.insertOrUpdate(item)
    suspend fun deleteMarketplaceItem(id: Long) = marketplaceSync.deleteById(id)

    // 🛠️ Bitácora de Mantenimiento
    private val maintenanceSync = MaintenanceSync(database, scope)
    val allMaintenanceLogs: Flow<List<MaintenanceLog>> = maintenanceSync.getAll()
    fun getMaintenanceLogsForMember(memberId: Long): Flow<List<MaintenanceLog>> = database.maintenanceDao().getLogsForMember(memberId)
    suspend fun insertMaintenanceLog(log: MaintenanceLog) = maintenanceSync.insertOrUpdate(log)
    suspend fun updateMaintenanceLog(log: MaintenanceLog) = maintenanceSync.insertOrUpdate(log)
    suspend fun deleteMaintenanceLog(id: Long) = maintenanceSync.deleteById(id)

    // 📍 Directorio de Talleres y Repuestos
    private val workshopDirectorySync = WorkshopDirectorySync(database, scope)
    val allWorkshops: Flow<List<WorkshopDirectoryItem>> = workshopDirectorySync.getAll()
    suspend fun insertWorkshop(item: WorkshopDirectoryItem) = workshopDirectorySync.insertOrUpdate(item)
    suspend fun updateWorkshop(item: WorkshopDirectoryItem) = workshopDirectorySync.insertOrUpdate(item)
    suspend fun deleteWorkshop(id: Long) = workshopDirectorySync.deleteById(id)

    // 🛂 Pasaporte Motero
    private val passportSync = PassportSync(database, scope)
    val allPassportDestinations: Flow<List<PassportDestination>> = passportSync.getAll()
    fun getPassportStampsForMember(memberId: Long): Flow<List<PassportStamp>> = passportSync.getStampsForMember(memberId)
    val allPassportStamps: Flow<List<PassportStamp>> = passportSync.getAllStamps()
    suspend fun insertPassportStamp(stamp: PassportStamp) = passportSync.insertStamp(stamp)
    suspend fun deletePassportStamp(id: Long) = passportSync.deleteStamp(id)
    suspend fun insertPassportDestination(dest: PassportDestination) = passportSync.insertOrUpdate(dest)

    // 🏆 Retos Moteros (Oficiales & Personales)
    private val challengeSync = ChallengeSync(database, scope)
    val allChallenges: Flow<List<BikerChallenge>> = challengeSync.getAll()
    val officialChallenges: Flow<List<BikerChallenge>> = challengeSync.getOfficialChallenges()
    fun getProgressForMember(memberId: Long): Flow<List<UserChallengeProgress>> = challengeSync.getProgressForMember(memberId)
    fun getProgressForChallenge(challengeId: Long): Flow<List<UserChallengeProgress>> = challengeSync.getProgressForChallenge(challengeId)
    val allChallengeProgress: Flow<List<UserChallengeProgress>> = challengeSync.getAllProgress()
    suspend fun insertChallenge(challenge: BikerChallenge) = challengeSync.insertOrUpdate(challenge)
    suspend fun updateChallenge(challenge: BikerChallenge) = challengeSync.insertOrUpdate(challenge)
    suspend fun deleteChallenge(id: Long) = challengeSync.deleteById(id)
    suspend fun insertOrUpdateProgress(progress: UserChallengeProgress) = challengeSync.insertOrUpdateProgress(progress)
    suspend fun deleteProgress(id: Long) = challengeSync.deleteProgress(id)

    // 📅 Calendario Motero (Rutas, Eventos, Clima, Garaje y Avisos)
    private val calendarSync = CalendarSync(database, scope)
    val allCalendarEvents: Flow<List<BikerCalendarEvent>> = calendarSync.getAll()
    val officialCalendarEvents: Flow<List<BikerCalendarEvent>> = calendarSync.getOfficialEvents()
    fun getCalendarEventsByDate(date: String): Flow<List<BikerCalendarEvent>> = calendarSync.getEventsByDate(date)
    suspend fun insertCalendarEvent(event: BikerCalendarEvent) = calendarSync.insertOrUpdate(event)
    suspend fun updateCalendarEvent(event: BikerCalendarEvent) = calendarSync.insertOrUpdate(event)
    suspend fun deleteCalendarEvent(id: Long) = calendarSync.deleteById(id)

    // 🏕️ Sitios de Interés TX & Denuncias
    private val interestPointSync = InterestPointSync(database, scope)
    private val spotReportSync = SpotReportSync(database, scope)
    val allInterestPoints: Flow<List<BikerInterestPoint>> = interestPointSync.getAll()
    val allSpotReports: Flow<List<SpotReport>> = spotReportSync.getAll()

    suspend fun insertInterestPoint(point: BikerInterestPoint) = interestPointSync.insertOrUpdate(point)
    suspend fun updateInterestPoint(point: BikerInterestPoint) = interestPointSync.insertOrUpdate(point)
    suspend fun deleteInterestPoint(id: Long) = interestPointSync.deleteById(id)

    suspend fun insertSpotReport(report: SpotReport) = spotReportSync.insertOrUpdate(report)
    suspend fun updateSpotReport(report: SpotReport) = spotReportSync.insertOrUpdate(report)
    suspend fun deleteSpotReport(id: Long) = spotReportSync.deleteById(id)

    suspend fun shutdown() {
        firebaseChatSync.shutdown()
        memberSync.shutdown()
        publicationSync.shutdown()
        rideSync.shutdown()
        rideRegistrationSync.shutdown()
        financeSync.shutdown()
        inventoryItemSync.shutdown()
        equipmentLoanSync.shutdown()
        emergencyAlertSync.shutdown()
        roleConfigSync.shutdown()
        noticeCommentSync.shutdown()
        invitationCodeSync.shutdown()
        accessRequestSync.shutdown()
        privateGroupSync.shutdown()
        marketplaceSync.shutdown()
        maintenanceSync.shutdown()
        workshopDirectorySync.shutdown()
        passportSync.shutdown()
        challengeSync.shutdown()
        calendarSync.shutdown()
    }
}




