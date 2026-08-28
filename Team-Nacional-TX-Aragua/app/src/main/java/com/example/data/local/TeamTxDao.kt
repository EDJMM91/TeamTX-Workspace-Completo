package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM member_profiles ORDER BY id ASC")
    fun getAllMembers(): Flow<List<MemberProfile>>

    @Query("SELECT * FROM member_profiles WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): MemberProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<MemberProfile>)

    @Update
    suspend fun updateMember(member: MemberProfile)

    @Delete
    suspend fun deleteMember(member: MemberProfile)

    @Query("DELETE FROM member_profiles WHERE id = :id")
    suspend fun deleteMemberById(id: Long)
}

@Dao
interface PublicationDao {
    @Query("SELECT * FROM publications WHERE ((trim(title) != '' AND trim(title) != 'Aviso Oficial') OR (trim(content) != '' AND trim(content) != 'Aviso Oficial')) AND id != 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllPublications(): Flow<List<Publication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublication(publication: Publication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPublications(publications: List<Publication>)

    @Update
    suspend fun updatePublication(publication: Publication)

    @Query("DELETE FROM publications WHERE id = :id")
    suspend fun deletePublicationById(id: Long)

    @Query("DELETE FROM publications WHERE (trim(title) = '' AND trim(content) = '') OR (trim(title) = 'Aviso Oficial' AND trim(content) = '') OR (trim(content) = 'Aviso Oficial' AND trim(title) = '') OR id = 0")
    suspend fun deleteEmptyPublications()

    @Query("DELETE FROM publications")
    suspend fun deleteAllPublications()
}

@Dao
interface RideDao {
    @Query("SELECT * FROM ride_events ORDER BY id DESC")
    fun getAllRides(): Flow<List<RideEvent>>

    @Query("SELECT * FROM ride_events WHERE id = :id LIMIT 1")
    suspend fun getRideById(id: Long): RideEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRides(rides: List<RideEvent>)

    @Update
    suspend fun updateRide(ride: RideEvent)

    @Query("SELECT * FROM ride_registrations WHERE rideId = :rideId ORDER BY registeredAt ASC")
    fun getRegistrationsForRide(rideId: Long): Flow<List<RideRegistration>>

    @Query("SELECT * FROM ride_registrations ORDER BY registeredAt DESC")
    fun getAllRegistrations(): Flow<List<RideRegistration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: RideRegistration): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegistrations(registrations: List<RideRegistration>)

    @Update
    suspend fun updateRegistration(registration: RideRegistration)

    @Query("DELETE FROM ride_registrations WHERE rideId = :rideId AND memberId = :memberId")
    suspend fun cancelRegistration(rideId: Long, memberId: Long)

    @Delete
    suspend fun deleteRegistration(registration: RideRegistration)

    @Query("DELETE FROM ride_events WHERE id = :id")
    suspend fun deleteRideById(id: Long)

    @Query("DELETE FROM ride_registrations WHERE rideId = :rideId")
    suspend fun deleteRegistrationsByRideId(rideId: Long)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM financial_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<FinancialTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<FinancialTransaction>)

    @Update
    suspend fun updateTransaction(transaction: FinancialTransaction)

    @Query("DELETE FROM financial_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY category ASC, name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<InventoryItem>)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Query("SELECT * FROM equipment_loans ORDER BY id DESC")
    fun getAllLoans(): Flow<List<EquipmentLoan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: EquipmentLoan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoans(loans: List<EquipmentLoan>)

    @Update
    suspend fun updateLoan(loan: EquipmentLoan)
}

@Dao
interface EmergencyDao {
    @Query("SELECT * FROM emergency_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<EmergencyAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: EmergencyAlert): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlerts(alerts: List<EmergencyAlert>)

    @Update
    suspend fun updateAlert(alert: EmergencyAlert)

    @Query("DELETE FROM emergency_alerts WHERE id = :id")
    suspend fun deleteAlert(id: Long)
}

@Dao
interface RoleConfigDao {
    @Query("SELECT * FROM role_configs")
    fun getAllRoleConfigs(): Flow<List<RoleConfig>>

    @Query("SELECT * FROM role_configs WHERE roleKey = :roleKey LIMIT 1")
    suspend fun getRoleConfig(roleKey: String): RoleConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoleConfig(config: RoleConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRoleConfigs(configs: List<RoleConfig>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoleConfigs(configs: List<RoleConfig>)

    @Update
    suspend fun updateRoleConfig(config: RoleConfig)
}

@Dao
interface NoticeCommentDao {
    @Query("SELECT * FROM notice_comments WHERE publicationId = :publicationId ORDER BY timestamp ASC")
    fun getCommentsForPublication(publicationId: Long): Flow<List<NoticeComment>>

    @Query("SELECT * FROM notice_comments ORDER BY timestamp DESC")
    fun getAllComments(): Flow<List<NoticeComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: NoticeComment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(comments: List<NoticeComment>)

    @Query("DELETE FROM notice_comments WHERE id = :id")
    suspend fun deleteComment(id: Long)

    @Query("DELETE FROM notice_comments WHERE publicationId = :publicationId")
    suspend fun deleteCommentsByPublicationId(publicationId: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM club_chat_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM club_chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<ChatMessage>)

    @Query("DELETE FROM club_chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM club_chat_messages WHERE channelId = :channelId")
    suspend fun deleteMessagesByChannel(channelId: String)

    @Query("DELETE FROM club_chat_messages")
    suspend fun deleteAllMessages()
}

@Dao
interface InvitationCodeDao {
    @Query("SELECT * FROM invitation_codes ORDER BY isMaster DESC, createdAt DESC")
    fun getAllInvitationCodes(): Flow<List<InvitationCode>>

    @Query("SELECT * FROM invitation_codes WHERE code = :code LIMIT 1")
    suspend fun getInvitationByCode(code: String): InvitationCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitationCode(code: InvitationCode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInvitationCodes(codes: List<InvitationCode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvitationCodes(codes: List<InvitationCode>)

    @Update
    suspend fun updateInvitationCode(code: InvitationCode)

    @Query("DELETE FROM invitation_codes WHERE id = :id AND isMaster = 0")
    suspend fun deleteInvitationCodeById(id: Long): Int
}

@Dao
interface AccessRequestDao {
    @Query("SELECT * FROM access_requests ORDER BY timestamp DESC")
    fun getAllAccessRequests(): Flow<List<AccessRequest>>

    @Query("SELECT * FROM access_requests WHERE status = 'PENDIENTE' ORDER BY timestamp DESC")
    fun getPendingAccessRequests(): Flow<List<AccessRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessRequest(request: AccessRequest): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccessRequests(requests: List<AccessRequest>)

    @Update
    suspend fun updateAccessRequest(request: AccessRequest)

    @Query("DELETE FROM access_requests WHERE id = :id")
    suspend fun deleteAccessRequest(id: Long)
}

@Dao
interface EventAttendanceDao {
    @Query("SELECT * FROM event_attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<EventAttendance>>

    @Query("SELECT * FROM event_attendance WHERE memberId = :memberId ORDER BY timestamp DESC")
    fun getAttendanceForMember(memberId: Long): Flow<List<EventAttendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: EventAttendance): Long

    @Delete
    suspend fun deleteAttendance(attendance: EventAttendance)
}

@Dao
interface DisciplinaryRecordDao {
    @Query("SELECT * FROM disciplinary_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DisciplinaryRecord>>

    @Query("SELECT * FROM disciplinary_records WHERE memberId = :memberId ORDER BY timestamp DESC")
    fun getRecordsForMember(memberId: Long): Flow<List<DisciplinaryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DisciplinaryRecord): Long

    @Delete
    suspend fun deleteRecord(record: DisciplinaryRecord)
}

@Dao
interface PrivateGroupDao {
    @Query("SELECT * FROM private_groups WHERE isDeleted = 0 ORDER BY lastMessageTimestamp DESC, createdAt DESC")
    fun getAllPrivateGroups(): Flow<List<PrivateGroup>>

    @Query("SELECT * FROM private_groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: String): PrivateGroup?

    @Query("SELECT * FROM private_groups WHERE id = :id LIMIT 1")
    fun getGroupFlowById(id: String): Flow<PrivateGroup?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: PrivateGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<PrivateGroup>)

    @Update
    suspend fun updateGroup(group: PrivateGroup)

    @Query("DELETE FROM private_groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)

    @Query("DELETE FROM private_groups")
    suspend fun deleteAllGroups()
}

@Dao
interface MarketplaceDao {
    @Query("SELECT * FROM marketplace_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<MarketplaceItem>>

    @Query("SELECT * FROM marketplace_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): MarketplaceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MarketplaceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<MarketplaceItem>)

    @Update
    suspend fun updateItem(item: MarketplaceItem)

    @Query("DELETE FROM marketplace_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM marketplace_items")
    suspend fun deleteAllItems()
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_logs WHERE memberId = :memberId ORDER BY odometerKm DESC, timestamp DESC")
    fun getLogsForMember(memberId: Long): Flow<List<MaintenanceLog>>

    @Query("SELECT * FROM maintenance_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MaintenanceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MaintenanceLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLogs(logs: List<MaintenanceLog>)

    @Update
    suspend fun updateLog(log: MaintenanceLog)

    @Query("DELETE FROM maintenance_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM maintenance_logs")
    suspend fun deleteAllLogs()
}

@Dao
interface WorkshopDirectoryDao {
    @Query("SELECT * FROM workshops_directory ORDER BY state ASC, name ASC")
    fun getAllWorkshops(): Flow<List<WorkshopDirectoryItem>>

    @Query("SELECT * FROM workshops_directory WHERE state = :state ORDER BY name ASC")
    fun getWorkshopsByState(state: String): Flow<List<WorkshopDirectoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkshop(item: WorkshopDirectoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkshops(items: List<WorkshopDirectoryItem>)

    @Update
    suspend fun updateWorkshop(item: WorkshopDirectoryItem)

    @Query("DELETE FROM workshops_directory WHERE id = :id")
    suspend fun deleteWorkshopById(id: Long)

    @Query("DELETE FROM workshops_directory")
    suspend fun deleteAllWorkshops()
}

@Dao
interface PassportDao {
    @Query("SELECT * FROM passport_destinations ORDER BY requiredKm ASC, state ASC")
    fun getAllDestinations(): Flow<List<PassportDestination>>

    @Query("SELECT * FROM passport_stamps WHERE memberId = :memberId ORDER BY timestamp DESC")
    fun getStampsForMember(memberId: Long): Flow<List<PassportStamp>>

    @Query("SELECT * FROM passport_stamps ORDER BY timestamp DESC")
    fun getAllStamps(): Flow<List<PassportStamp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(destination: PassportDestination): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDestinations(destinations: List<PassportDestination>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStamp(stamp: PassportStamp): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStamps(stamps: List<PassportStamp>)

    @Query("DELETE FROM passport_stamps WHERE id = :id")
    suspend fun deleteStampById(id: Long)

    @Query("DELETE FROM passport_destinations WHERE id = :id")
    suspend fun deleteDestinationById(id: Long)
}

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM biker_challenges ORDER BY timestamp DESC")
    fun getAllChallenges(): Flow<List<BikerChallenge>>

    @Query("SELECT * FROM biker_challenges WHERE isOfficial = 1 ORDER BY timestamp DESC")
    fun getOfficialChallenges(): Flow<List<BikerChallenge>>

    @Query("SELECT * FROM user_challenge_progress WHERE memberId = :memberId ORDER BY updatedAt DESC")
    fun getProgressForMember(memberId: Long): Flow<List<UserChallengeProgress>>

    @Query("SELECT * FROM user_challenge_progress WHERE challengeId = :challengeId ORDER BY updatedAt DESC")
    fun getProgressForChallenge(challengeId: Long): Flow<List<UserChallengeProgress>>

    @Query("SELECT * FROM user_challenge_progress ORDER BY updatedAt DESC")
    fun getAllProgress(): Flow<List<UserChallengeProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: BikerChallenge): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenges(challenges: List<BikerChallenge>)

    @Update
    suspend fun updateChallenge(challenge: BikerChallenge)

    @Query("DELETE FROM biker_challenges WHERE id = :id")
    suspend fun deleteChallengeById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserChallengeProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressList(list: List<UserChallengeProgress>)

    @Update
    suspend fun updateProgress(progress: UserChallengeProgress)

    @Query("DELETE FROM user_challenge_progress WHERE id = :id")
    suspend fun deleteProgressById(id: Long)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM biker_calendar_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<BikerCalendarEvent>>

    @Query("SELECT * FROM biker_calendar_events WHERE eventDate = :date ORDER BY timestamp DESC")
    fun getEventsByDate(date: String): Flow<List<BikerCalendarEvent>>

    @Query("SELECT * FROM biker_calendar_events WHERE isOfficialClubEvent = 1 ORDER BY timestamp DESC")
    fun getOfficialEvents(): Flow<List<BikerCalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: BikerCalendarEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<BikerCalendarEvent>)

    @Update
    suspend fun updateEvent(event: BikerCalendarEvent)

    @Query("DELETE FROM biker_calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}
