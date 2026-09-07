package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import com.example.reproductor.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(
    entities = [
        MemberProfile::class,
        Publication::class,
        RideEvent::class,
        RideRegistration::class,
        FinancialTransaction::class,
        InventoryItem::class,
        EquipmentLoan::class,
        EmergencyAlert::class,
        RoleConfig::class,
        NoticeComment::class,
        ChatMessage::class,
        InvitationCode::class,
        AccessRequest::class,
        EventAttendance::class,
        DisciplinaryRecord::class,
        NotificacionApp::class,
        PrivateGroup::class,
        MarketplaceItem::class,
        MaintenanceLog::class,
        WorkshopDirectoryItem::class,
        PassportDestination::class,
        PassportStamp::class,
        BikerChallenge::class,
        UserChallengeProgress::class,
        BikerCalendarEvent::class,
        CancionLocalEntity::class,
        ListaReproduccionEntity::class,
        EstadoReproductorEntity::class
    ],
    version = 31, // 🛡️ Módulos: Actualización base de datos tras cambios en modelos
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun publicationDao(): PublicationDao
    abstract fun rideDao(): RideDao
    abstract fun financeDao(): FinanceDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun emergencyDao(): EmergencyDao
    abstract fun roleConfigDao(): RoleConfigDao
    abstract fun noticeCommentDao(): NoticeCommentDao
    abstract fun chatDao(): ChatDao
    abstract fun invitationCodeDao(): InvitationCodeDao
    abstract fun accessRequestDao(): AccessRequestDao
    abstract fun eventAttendanceDao(): EventAttendanceDao
    abstract fun disciplinaryRecordDao(): DisciplinaryRecordDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun privateGroupDao(): PrivateGroupDao
    abstract fun marketplaceDao(): MarketplaceDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun workshopDirectoryDao(): WorkshopDirectoryDao
    abstract fun passportDao(): PassportDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun calendarDao(): CalendarDao
    abstract fun cancionLocalDao(): CancionLocalDao
    abstract fun listaReproduccionDao(): ListaReproduccionDao
    abstract fun estadoReproductorDao(): EstadoReproductorDao
    abstract fun globalNukeDao(): GlobalNukeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN isSuspended INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN suspensionReason TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN suspensionDurationDays INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_messages ADD COLUMN senderCustomRoleTitle TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN birthDate TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE access_requests ADD COLUMN birthDate TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE access_requests ADD COLUMN requestedRole TEXT NOT NULL DEFAULT 'Piloto'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // MemberProfile updates
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN licenseImageUri TEXT")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN medicalCertImageUri TEXT")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN bikeRegImageUri TEXT")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN insuranceImageUri TEXT")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN copilotName TEXT")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN copilotRelation TEXT")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN attendanceCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN longRidesCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN bigEventsCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN prospectStartDate INTEGER")
                
                // New Tables
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `event_attendance` (
                        `id` INTEGER PRIMARY KEY NOT NULL, 
                        `eventId` INTEGER NOT NULL, 
                        `memberId` INTEGER NOT NULL, 
                        `eventName` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `disciplinary_records` (
                        `id` INTEGER PRIMARY KEY NOT NULL, 
                        `memberId` INTEGER NOT NULL, 
                        `memberName` TEXT NOT NULL, 
                        `reason` TEXT NOT NULL, 
                        `penaltyType` TEXT NOT NULL, 
                        `issuedBy` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN firebaseUid TEXT")
                database.execSQL("ALTER TABLE member_profiles ADD COLUMN email TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE member_profiles ADD COLUMN profilePhotoUri TEXT")
                db.execSQL("ALTER TABLE member_profiles ADD COLUMN bikePhotoUri TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE club_chat_messages ADD COLUMN readBy TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE publications ADD COLUMN imageUrl TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Baseline migration for version 10
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invitation_codes ADD COLUMN isSpecialGuest INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE invitation_codes ADD COLUMN durationHours INTEGER NOT NULL DEFAULT 24")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 📸 Agregar columna para fotos de perfil en el chat
                db.execSQL("ALTER TABLE club_chat_messages ADD COLUMN senderPhotoUrl TEXT")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE member_profiles ADD COLUMN isChatMuted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE member_profiles ADD COLUMN muteReason TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `private_groups` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `creatorMemberId` INTEGER NOT NULL,
                        `creatorName` TEXT NOT NULL,
                        `creatorNickname` TEXT NOT NULL,
                        `memberIds` TEXT NOT NULL,
                        `adminIds` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isBlockedByDirectiva` INTEGER NOT NULL,
                        `blockedReason` TEXT NOT NULL,
                        `blockedBy` TEXT NOT NULL,
                        `lastMessageText` TEXT NOT NULL,
                        `lastMessageTimestamp` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `groupIcon` TEXT
                    )
                """)
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `marketplace_items` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `sellerMemberId` INTEGER NOT NULL,
                        `sellerName` TEXT NOT NULL,
                        `sellerNickname` TEXT NOT NULL,
                        `sellerPhone` TEXT NOT NULL,
                        `sellerLocation` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `priceUsd` REAL NOT NULL,
                        `condition` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `status` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `maintenance_logs` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `memberId` INTEGER NOT NULL,
                        `odometerKm` INTEGER NOT NULL,
                        `serviceType` TEXT NOT NULL,
                        `brandOrDetails` TEXT NOT NULL,
                        `costUsd` REAL NOT NULL,
                        `workshopName` TEXT NOT NULL,
                        `serviceDate` TEXT NOT NULL,
                        `nextServiceKm` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workshops_directory` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `whatsapp` TEXT NOT NULL,
                        `rating` REAL NOT NULL,
                        `recommendedBy` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `passport_destinations` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `title` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `badgeIcon` TEXT NOT NULL,
                        `requiredKm` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `isOfficialRoute` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `passport_stamps` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `memberId` INTEGER NOT NULL,
                        `destinationId` INTEGER NOT NULL,
                        `destinationTitle` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `stampedDate` TEXT NOT NULL,
                        `proofImageUrl` TEXT,
                        `isVerified` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `likedByMemberIds` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToMessageId` INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToSenderName` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToText` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `deletedForMemberIds` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `biker_challenges` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `targetKm` REAL NOT NULL,
                        `cruisingSpeed` TEXT NOT NULL,
                        `badgeName` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `checkpoints` TEXT NOT NULL,
                        `isOfficial` INTEGER NOT NULL,
                        `creatorMemberId` INTEGER NOT NULL,
                        `creatorName` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_challenge_progress` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `challengeId` INTEGER NOT NULL,
                        `challengeTitle` TEXT NOT NULL,
                        `memberId` INTEGER NOT NULL,
                        `pilotName` TEXT NOT NULL,
                        `pilotPhone` TEXT NOT NULL,
                        `pilotEmergencyContact` TEXT NOT NULL,
                        `copilotName` TEXT,
                        `copilotPhone` TEXT,
                        `bikeModel` TEXT NOT NULL,
                        `bikePlate` TEXT NOT NULL,
                        `departureDate` TEXT NOT NULL,
                        `returnDate` TEXT NOT NULL,
                        `originLocation` TEXT NOT NULL,
                        `destinationLocation` TEXT NOT NULL,
                        `initialKm` REAL NOT NULL,
                        `finalKm` REAL NOT NULL,
                        `currentRouteNumber` INTEGER NOT NULL,
                        `routeDescription` TEXT NOT NULL,
                        `completedCheckpoints` TEXT NOT NULL,
                        `accumulatedKm` REAL NOT NULL,
                        `sitePhotoUrl` TEXT,
                        `notes` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `likedByMemberIds` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToMessageId` INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToSenderName` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToText` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `deletedForMemberIds` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `biker_calendar_events` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `visibility` TEXT NOT NULL,
                        `eventDate` TEXT NOT NULL,
                        `eventTime` TEXT NOT NULL,
                        `departureTime` TEXT NOT NULL,
                        `originAddress` TEXT NOT NULL,
                        `destinationAddress` TEXT NOT NULL,
                        `originLatitude` REAL NOT NULL,
                        `originLongitude` REAL NOT NULL,
                        `destinationLatitude` REAL NOT NULL,
                        `destinationLongitude` REAL NOT NULL,
                        `terrainType` TEXT NOT NULL,
                        `difficultyLevel` TEXT NOT NULL,
                        `weatherForecast` TEXT NOT NULL,
                        `roadCaptain` TEXT NOT NULL,
                        `tailRider` TEXT NOT NULL,
                        `rsvpPilotsCount` INTEGER NOT NULL,
                        `rsvpPilotsList` TEXT NOT NULL,
                        `rsvpPillionsCount` INTEGER NOT NULL,
                        `remindDaysBefore` INTEGER NOT NULL,
                        `remindedMemberIds` TEXT NOT NULL,
                        `isOfficialClubEvent` INTEGER NOT NULL,
                        `creatorMemberId` INTEGER NOT NULL,
                        `creatorName` TEXT NOT NULL,
                        `flyerUrl` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `likedByMemberIds` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToMessageId` INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToSenderName` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `replyToText` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `deletedForMemberIds` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `biker_calendar_events` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `visibility` TEXT NOT NULL,
                        `eventDate` TEXT NOT NULL,
                        `eventTime` TEXT NOT NULL,
                        `departureTime` TEXT NOT NULL,
                        `originAddress` TEXT NOT NULL,
                        `destinationAddress` TEXT NOT NULL,
                        `originLatitude` REAL NOT NULL,
                        `originLongitude` REAL NOT NULL,
                        `destinationLatitude` REAL NOT NULL,
                        `destinationLongitude` REAL NOT NULL,
                        `terrainType` TEXT NOT NULL,
                        `difficultyLevel` TEXT NOT NULL,
                        `weatherForecast` TEXT NOT NULL,
                        `roadCaptain` TEXT NOT NULL,
                        `tailRider` TEXT NOT NULL,
                        `rsvpPilotsCount` INTEGER NOT NULL,
                        `rsvpPilotsList` TEXT NOT NULL,
                        `rsvpPillionsCount` INTEGER NOT NULL,
                        `remindDaysBefore` INTEGER NOT NULL,
                        `remindedMemberIds` TEXT NOT NULL,
                        `isOfficialClubEvent` INTEGER NOT NULL,
                        `creatorMemberId` INTEGER NOT NULL,
                        `creatorName` TEXT NOT NULL,
                        `flyerUrl` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `reactions` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `audioDurationSeconds` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `audioUrl` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SENT'")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `localMediaPath` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `sharesCount` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `savesCount` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `totalKmRidden` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `topSpeedRecordKmh` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `sosAssistanceCount` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `challengesCompletedCount` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `meritPoints` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `rankingTitle` TEXT NOT NULL DEFAULT 'Piloto TX'")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `positiveRatingsCount` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `negativeRatingsCount` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `reputationPoints` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `ratedByMemberIdsJson` TEXT NOT NULL DEFAULT '{}'")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `locationCoordinates` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `locationName` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `eventDate` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `eventTime` TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `isEventFinished` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `publications` ADD COLUMN `linkedCalendarEventId` INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `biker_calendar_events` ADD COLUMN `linkedPublicationId` INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `biker_calendar_events` ADD COLUMN `isEventFinished` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `workshops_directory` ADD COLUMN `hasCredit` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `workshops_directory` ADD COLUMN `creditPlatforms` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `workshops_directory` ADD COLUMN `googleMapsUrl` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `isPoll` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `pollQuestion` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `pollOptionsJson` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `club_chat_messages` ADD COLUMN `pollVotesJson` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `isOnline` INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `lastActiveTimestamp` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `canciones_locales` (
                            `id` INTEGER PRIMARY KEY NOT NULL,
                            `titulo` TEXT NOT NULL,
                            `artista` TEXT NOT NULL,
                            `album` TEXT NOT NULL,
                            `duracionMs` INTEGER NOT NULL,
                            `rutaArchivo` TEXT NOT NULL,
                            `uriStr` TEXT NOT NULL,
                            `portadaUriStr` TEXT,
                            `fechaAgregada` INTEGER NOT NULL,
                            `esFavorita` INTEGER NOT NULL DEFAULT 0,
                            `tamanoBytes` INTEGER NOT NULL,
                            `carpetaContenedora` TEXT NOT NULL,
                            `metadatosPersonalizadosJson` TEXT
                        )
                    """.trimIndent())
                } catch (e: Exception) {}
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `listas_reproduccion` (
                            `id` TEXT PRIMARY KEY NOT NULL,
                            `nombre` TEXT NOT NULL,
                            `descripcion` TEXT NOT NULL,
                            `fechaCreacion` INTEGER NOT NULL,
                            `icono` TEXT NOT NULL,
                            `cancionIdsJson` TEXT NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {}
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `estado_reproductor` (
                            `clave` TEXT PRIMARY KEY NOT NULL DEFAULT 'global',
                            `ultimaCancionId` INTEGER,
                            `colaIdsJson` TEXT NOT NULL,
                            `indiceColaActual` INTEGER NOT NULL,
                            `modoBucle` INTEGER NOT NULL,
                            `modoAleatorio` INTEGER NOT NULL DEFAULT 0,
                            `posicionMs` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "team_tx_venezuela_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31)
                .fallbackToDestructiveMigration(dropAllTables = true) // 🛡️ Fuerza la limpieza total para evitar conflicto de IDs
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            val workshops = database.workshopDirectoryDao().getAllWorkshops().first()
                            if (workshops.isEmpty()) {
                                database.workshopDirectoryDao().upsertWorkshops(INITIAL_WORKSHOPS)
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("AppDatabase", "Error sembrando workshops en onOpen: ${e.message}")
                        }
                    }
                }
            }
        }

        val INITIAL_WORKSHOPS: List<WorkshopDirectoryItem> = listOf(
            WorkshopDirectoryItem(
                id = 1L,
                name = "Moto Repuestos San Onofre",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Sector San Onofre, Maracay",
                phone = "0412-7839728",
                whatsapp = "04127839728",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Venta de repuestos, lubricantes y accesorios para TX",
                latitude = 10.2427886,
                longitude = -67.5965123,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.app.goo.gl/WCas6HVW1xZxW1th8"
            ),
            WorkshopDirectoryItem(
                id = 2L,
                name = "Novod Speed C.A",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Principal, Maracay",
                phone = "0424-3377209",
                whatsapp = "04243377209",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos, cauchos y accesorios moteros de alta calidad",
                latitude = 10.2454563,
                longitude = -67.6007084,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.app.goo.gl/jUFsPteuLofYkC7T8"
            ),
            WorkshopDirectoryItem(
                id = 3L,
                name = "Moto Central",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Sector Central, Maracay",
                phone = "0412-8699791",
                whatsapp = "04128699791",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos, kits de arrastre, bujías y mantenimiento",
                latitude = 10.2529454,
                longitude = -67.6082767,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.app.goo.gl/cQQ1MbzMhto2rDnu9"
            ),
            WorkshopDirectoryItem(
                id = 4L,
                name = "MOTO REPUESTOS MJC CARS MILENIUM",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Intercomunal / Sector Milenium, Maracay",
                phone = "0412-4122699",
                whatsapp = "04124122699",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos para motos, baterías y consumibles",
                latitude = 10.2052125,
                longitude = -67.5678244,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.app.goo.gl/Jm7h3fKrewUKmX43A"
            ),
            WorkshopDirectoryItem(
                id = 5L,
                name = "GG Motors",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Constitución Este, Maracay",
                phone = "0414-2967696",
                whatsapp = "04142967696",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Venta de repuestos, lubricantes y servicio rápido",
                latitude = 10.2303945,
                longitude = -67.5905278,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.app.goo.gl/yz1N5J6q3Pv8bUcS6"
            ),
            WorkshopDirectoryItem(
                id = 6L,
                name = "Egrob",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Sector La Romana / Bolívar Este, Maracay",
                phone = "0412-4292720",
                whatsapp = "04124292720",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos y accesorios para motocicletas",
                latitude = 10.2469,
                longitude = -67.5958,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.app.goo.gl/bsFmcyob4nQCSZFP7"
            ),
            WorkshopDirectoryItem(
                id = 7L,
                name = "LE TOUR BIKE CAFÉ (Suzuki Bolívar)",
                type = "Tienda de Accesorios",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar Oeste, Maracay",
                phone = "0414-3928552",
                whatsapp = "04143928552",
                rating = 5.0,
                recommendedBy = "Team TX Aragua",
                notes = "Accesorios moteros premium, cascos, repuestos y cafetería",
                latitude = 10.2540368,
                longitude = -67.6111177,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.app.goo.gl/ZaooMHVa5kJGzm399"
            ),
            WorkshopDirectoryItem(
                id = 8L,
                name = "Moto Repuestos Milano C.A",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Calle Milano c/c Av. Bolívar, Maracay",
                phone = "0414-4656424",
                whatsapp = "04144656424",
                rating = 4.8,
                recommendedBy = "Directiva TX Aragua",
                notes = "Repuestos de motor, frenos y kits de arrastre para TX",
                latitude = 10.2419038,
                longitude = -67.6032231,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/vH5BbTJyRjayjB8u8"
            ),
            WorkshopDirectoryItem(
                id = 9L,
                name = "Los Mangos Repuestos",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Sector Los Mangos, Maracay",
                phone = "0412-8440636",
                whatsapp = "04128440636",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos generales, guayas, cables y consumibles",
                latitude = 10.2469,
                longitude = -67.5958,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/VkxNhEgfgfMCj6Za6"
            ),
            WorkshopDirectoryItem(
                id = 10L,
                name = "Almendrones Moto Partes",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Sector Los Almendrones, Maracay",
                phone = "0414-5891290",
                whatsapp = "04145891290",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Filtros de aire, pastillas de freno y repuestos eléctricos",
                latitude = 10.2469,
                longitude = -67.5958,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/jRYas65HcMV5C9FW8"
            ),
            WorkshopDirectoryItem(
                id = 11L,
                name = "Asdrúbal Moto",
                type = "Taller Mecánico",
                state = "Aragua",
                city = "Maracay",
                address = "Sector 23 de Enero / La Romana, Maracay",
                phone = "0414-4517108",
                whatsapp = "04144517108",
                rating = 4.8,
                recommendedBy = "Directiva TX Aragua",
                notes = "Taller de mecánica general, entonación y repuestos",
                latitude = 10.2456393,
                longitude = -67.6096241,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/w84JQenmrrt9f4nD7"
            ),
            WorkshopDirectoryItem(
                id = 12L,
                name = "Moto Repuestos Alex 2006 C.A",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Miranda c/c Calle Mariño, Maracay",
                phone = "0412-7480661",
                whatsapp = "04127480661",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Amplio stock de repuestos para TX 200, coronas y piñones",
                latitude = 10.239389,
                longitude = -67.6022268,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/etjBnukj9cKMEuJj9"
            ),
            WorkshopDirectoryItem(
                id = 13L,
                name = "Nicomoto Aragua",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar Oeste c/c Santos Michelena, Maracay",
                phone = "0412-4975654",
                whatsapp = "04124975654",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Venta de repuestos, accesorios, luces LED y baterías",
                latitude = 10.2509257,
                longitude = -67.6076853,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/5u2JdjVB9VNi5hdKA"
            ),
            WorkshopDirectoryItem(
                id = 14L,
                name = "MOTO ELITE 3000 MARACAY",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Casanova Godoy, sector El Bosque, Maracay",
                phone = "0412-9060180",
                whatsapp = "04129060180",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos de gama alta, cauchos y accesorios",
                latitude = 10.2558151,
                longitude = -67.6185126,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/4jb1NnAKFZYWJUns8"
            ),
            WorkshopDirectoryItem(
                id = 15L,
                name = "Motorepuestos RG",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Sector Santa Rosa / Av. Fuerzas Aéreas, Maracay",
                phone = "0412-8722621",
                whatsapp = "04128722621",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Cadenas reforzadas, bandas de freno y repuestos TX",
                latitude = 10.2360582,
                longitude = -67.6010938,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/WfXh64n1rXTbVrh19"
            ),
            WorkshopDirectoryItem(
                id = 16L,
                name = "Moto Repuestos Latino",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Las Delicias, sector El Toro, Maracay",
                phone = "0424-3078879",
                whatsapp = "04243078879",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Venta de repuestos, bujías Iridium y aceites 4T",
                latitude = 10.2469,
                longitude = -67.5958,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/B8EuDtPQvwGLzH4a7"
            ),
            WorkshopDirectoryItem(
                id = 17L,
                name = "Motorcar Repuestos",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Calle Boyacá c/c Libertad, Maracay",
                phone = "0412-4579955",
                whatsapp = "04124579955",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos mecánicos, eléctricos y suspensión",
                latitude = 10.2469,
                longitude = -67.5958,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/iNPvcZ9w4gN4DtbTA"
            ),
            WorkshopDirectoryItem(
                id = 18L,
                name = "Inversiones Pereira 2020",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar Oeste, sector La Julia, Maracay",
                phone = "0412-4773617",
                whatsapp = "04124773617",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos y consumibles para motos Empire TX",
                latitude = 10.2507339,
                longitude = -67.617933,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/VpKLfDU8hUPLt1xy8"
            ),
            WorkshopDirectoryItem(
                id = 19L,
                name = "PARADISE MARACAY, CA (Empire Keeway)",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Sucre c/c Av. Casanova Godoy, Maracay",
                phone = "0424-3457013",
                whatsapp = "04243457013",
                rating = 5.0,
                recommendedBy = "Presidente Nacional TX",
                notes = "Concesionario oficial Empire Keeway, repuestos originales y servicio",
                latitude = 10.2511258,
                longitude = -67.597661,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/kqZDq4rgAEVAZv9p9"
            ),
            WorkshopDirectoryItem(
                id = 20L,
                name = "SUPER MOTOS EL SHADDAI C.A (Empire)",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Sector 9no Inning, Av. Intercomunal, Maracay",
                phone = "0414-5880424",
                whatsapp = "04145880424",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Repuestos originales Empire Keeway, servicio técnico y repuestos",
                latitude = 10.2144251,
                longitude = -67.5761185,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/EJuYw4TzoSM77ErQ6"
            ),
            WorkshopDirectoryItem(
                id = 21L,
                name = "Empire Keeway Papi Moto",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Aragua c/c Av. Fuerzas Aéreas, Maracay",
                phone = "0412-5120029",
                whatsapp = "04125120029",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Venta de motos, repuestos originales Empire, cauchos y aceite",
                latitude = 10.2336375,
                longitude = -67.6006529,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/vECLjzKnC81qWUdv8"
            ),
            WorkshopDirectoryItem(
                id = 22L,
                name = "EMPIRE KEEWAY Paradise Motors Parts",
                type = "Venta de Repuestos TX",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Constitución Oeste c/c Calle Mariño, Maracay",
                phone = "0424-3190583",
                whatsapp = "04243190583",
                rating = 5.0,
                recommendedBy = "Team TX Aragua",
                notes = "Concesionario y repuestos genuinos Empire Keeway, servicio y accesorios",
                latitude = 10.2453587,
                longitude = -67.6001947,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.app.goo.gl/1h9NqscNq6rkrGBh8"
            )
        )

        private suspend fun populateInitialData(db: AppDatabase) {
            val memberDao = db.memberDao()
            val pubDao = db.publicationDao()
            val rideDao = db.rideDao()
            val finDao = db.financeDao()
            val invDao = db.inventoryDao()
            val sosDao = db.emergencyDao()
            val roleConfigDao = db.roleConfigDao()
            val commentDao = db.noticeCommentDao()
            val chatDao = db.chatDao()

            // 0. Seed Role Configurations (Editable by President)
            val roleConfigs = listOf(
                RoleConfig(
                    roleKey = "PRESIDENTE",
                    customTitle = "Presidente Nacional",
                    roleDuties = "Liderazgo supremo del club, representación legal, edición de rangos de directiva y veto institucional.",
                    badgeIconName = "crown",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "VICEPRESIDENTE",
                    customTitle = "Vicepresidente Ejecutivo",
                    roleDuties = "Suplencia ejecutiva del Presidente, supervisión de capítulos regionales y coordinación general de logística.",
                    badgeIconName = "shield",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "DISCIPLINARIO",
                    customTitle = "Oficial Disciplinario / Tribunal de Honor",
                    roleDuties = "Aplicación del Código de Honor, régimen disciplinario, investigación de faltas y ejecución de suspensiones.",
                    badgeIconName = "gavel",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "SECRETARIO",
                    customTitle = "Secretario General de Actas y Prensa",
                    roleDuties = "Redacción de actas oficiales, emisión de comunicados institucionales, registro y carnetización de miembros.",
                    badgeIconName = "edit_document",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "TESORERO",
                    customTitle = "Tesorero Nacional / Finanzas",
                    roleDuties = "Gestión del libro contable, cobro y validación de mensualidades, fondos SOS y potes para rodadas.",
                    badgeIconName = "payments",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "CAPITAN_RUTA",
                    customTitle = "Capitán de Ruta Principal (Puntero)",
                    roleDuties = "Trazado de rutas, determinación del ritmo de marcha y paradas de gasolina, comando en carretera.",
                    badgeIconName = "navigation",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "SEGURIDAD_VIAL",
                    customTitle = "Oficial de Seguridad Vial / Bloqueador",
                    roleDuties = "Bloqueo de intersecciones, protección de caravana, señalización de peligros y barredor de cola.",
                    badgeIconName = "traffic",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "MECANICO_OFICIAL",
                    customTitle = "Mecánico Oficial TX / Asistencia Técnica",
                    roleDuties = "Diagnóstico técnico en ruta, auxilio ante varadas mecánicas y custodia del banco de herramientas/repuestos.",
                    badgeIconName = "build",
                    canManageApp = false,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "MEDICO_CLUB",
                    customTitle = "Médico Oficial del Club",
                    roleDuties = "Atención prehospitalaria, triage de caídas/accidentes, resguardo del botiquín y enlace con ambulancias/clínicas.",
                    badgeIconName = "medical_services",
                    canManageApp = false,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "DIRECTIVA",
                    customTitle = "Miembro de Directiva Nacional",
                    roleDuties = "Voto en consejo directivo, apoyo en administración y gobernanza del club.",
                    badgeIconName = "account_balance",
                    canManageApp = true,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "PARAMEDICO_MOTERO",
                    customTitle = "Paramédico de Ruta",
                    roleDuties = "Soporte vital básico, inmovilización y primeros auxilios rápidos en carretera.",
                    badgeIconName = "local_hospital",
                    canManageApp = false,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "MIEMBRO_ACTIVO",
                    customTitle = "Piloto Miembro Activo",
                    roleDuties = "Participación en rodadas, chat interno, comentarios en avisos oficiales y cumplimiento del reglamento.",
                    badgeIconName = "person",
                    canManageApp = false,
                    lastUpdatedBy = "Presidente Nacional"
                ),
                RoleConfig(
                    roleKey = "ASPIRANTE",
                    customTitle = "Piloto Aspirante",
                    roleDuties = "Período de prueba y observación, acumulación de kilómetros y evaluación de mérito para pase a Miembro Activo.",
                    badgeIconName = "star_border",
                    canManageApp = false,
                    lastUpdatedBy = "Presidente Nacional"
                )
            )
            roleConfigDao.insertAllRoleConfigs(roleConfigs)


            // Seed Master Codes (Immutable, never expire, for Leader/President) & Initial Invitation Codes
            val invitationCodeDao = db.invitationCodeDao()
            val masterCode1 = InvitationCode(
                code = "linda19554402",
                isMaster = true,
                createdBy = "SISTEMA MAESTRO / LÍDER",
                createdAt = System.currentTimeMillis(),
                expiresAt = Long.MAX_VALUE,
                isUsed = false,
                targetRole = MemberRole.PRESIDENTE,
                note = "Código Maestro Inmutable #1 - Acceso Total Líder / Presidente"
            )
            val masterCode2 = InvitationCode(
                code = "19554402sb",
                isMaster = true,
                createdBy = "SISTEMA MAESTRO / LÍDER",
                createdAt = System.currentTimeMillis(),
                expiresAt = Long.MAX_VALUE,
                isUsed = false,
                targetRole = MemberRole.PRESIDENTE,
                note = "Código Maestro Inmutable #2 - Acceso Total Líder / Presidente"
            )
            val sampleInvite1 = InvitationCode(
                code = "TX-INV-8921",
                isMaster = false,
                createdBy = "Carlos Mendoza (Presidente)",
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                isUsed = false,
                targetRole = MemberRole.MIEMBRO_ACTIVO,
                note = "Invitación para nuevo piloto de Maracay (Vigencia 24h)"
            )
            val sampleInvite2 = InvitationCode(
                code = "TX-INV-3310",
                isMaster = false,
                createdBy = "Carlos Mendoza (Presidente)",
                createdAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000L,
                expiresAt = System.currentTimeMillis() + 22 * 60 * 60 * 1000L,
                isUsed = false,
                targetRole = MemberRole.MIEMBRO_ACTIVO,
                note = "Invitación para integrante de Valencia (Vigencia 24h)"
            )
            invitationCodeDao.insertAllInvitationCodes(listOf(masterCode1, masterCode2, sampleInvite1, sampleInvite2))

            // Seed Initial Passport Destinations (Venezuela)
            val passportDao = db.passportDao()
            val destinations = listOf(
                PassportDestination(
                    id = 1L,
                    title = "Paso de Portachuelo (Henri Pittier)",
                    state = "Aragua",
                    description = "Cruce de montaña entre Maracay y Ocumare de la Costa. Curvas técnicas y bosque nublado.",
                    badgeIcon = "🌲",
                    requiredKm = 45,
                    latitude = 10.3541,
                    longitude = -67.6833
                ),
                PassportDestination(
                    id = 2L,
                    title = "Colonia Tovar",
                    state = "Aragua",
                    description = "Pueblo de arquitectura alemana a más de 2.000 msnm. Subida emblemática por La Victoria o El Junquito.",
                    badgeIcon = "🏰",
                    requiredKm = 70,
                    latitude = 10.4056,
                    longitude = -67.2897
                ),
                PassportDestination(
                    id = 3L,
                    title = "El Jarillo",
                    state = "Miranda",
                    description = "Zona de parapente y duraznos con bajadas pronunciadas y paisajes de montaña.",
                    badgeIcon = "🪂",
                    requiredKm = 85,
                    latitude = 10.3700,
                    longitude = -67.1600
                ),
                PassportDestination(
                    id = 4L,
                    title = "Malecón de Puerto Cabello",
                    state = "Carabobo",
                    description = "Ruta costera y de autopista hacia el histórico malecón y centro colonial.",
                    badgeIcon = "⚓",
                    requiredKm = 110,
                    latitude = 10.4742,
                    longitude = -68.0125
                ),
                PassportDestination(
                    id = 5L,
                    title = "Monumento Manto de María",
                    state = "Lara",
                    description = "El monumento mariano más grande de Venezuela en Barquisimeto.",
                    badgeIcon = "⭐",
                    requiredKm = 240,
                    latitude = 10.0639,
                    longitude = -69.2789
                ),
                PassportDestination(
                    id = 6L,
                    title = "Médanos de Coro",
                    state = "Falcón",
                    description = "Parque Nacional de dunas y desierto costero. Reto de largo recorrido.",
                    badgeIcon = "🏜️",
                    requiredKm = 360,
                    latitude = 11.4417,
                    longitude = -69.6792
                )
            )
            passportDao.upsertDestinations(destinations)

            // Seed Initial Workshops (Tiendas y Talleres Aragua)
            val workshopDao = db.workshopDirectoryDao()
            workshopDao.upsertWorkshops(INITIAL_WORKSHOPS)

            // Seed Initial Official Challenges
            val challengeDao = db.challengeDao()
            val initialChallenges = listOf(
                BikerChallenge(
                    id = 1L,
                    title = "3er. RETO PILOTO EXPERTO",
                    description = "Recorre más de 7.500 KM a nivel nacional tocando puntos emblemáticos de Venezuela y fronteras.",
                    targetKm = 7747.3,
                    cruisingSpeed = "80-100 km/h",
                    badgeName = "Parche Piloto Experto Nacional 🏆",
                    startDate = "01/08/2026",
                    endDate = "31/12/2026",
                    checkpoints = "Cabo San Román, Cascada del Vino, Collado del Cóndor, Cuyagua, Frontera Colombia",
                    isOfficial = true,
                    creatorMemberId = 1L,
                    creatorName = "Directiva Nacional TX"
                ),
                BikerChallenge(
                    id = 2L,
                    title = "2do. RETO PILOTO ALAS DE PLATA",
                    description = "Demuestra tu resistencia y navegación en rutas de media y larga distancia por el occidente y centro del país.",
                    targetKm = 2430.4,
                    cruisingSpeed = "80-95 km/h",
                    badgeName = "Distintivo Alas de Plata ✨",
                    startDate = "15/08/2026",
                    endDate = "30/11/2026",
                    checkpoints = "San Cristóbal, El Vigía, Palmarito Mérida, Barquisimeto, Chichiriviche",
                    isOfficial = true,
                    creatorMemberId = 1L,
                    creatorName = "Directiva Nacional TX"
                )
            )
            challengeDao.upsertChallenges(initialChallenges)

            // Seed Initial Calendar Events
            val calendarDao = db.calendarDao()
            val initialEvents = listOf(
                BikerCalendarEvent(
                    id = 1L,
                    title = "Gran Rodada Costera a Cuyagua 🏍️🌊",
                    description = "Rodada oficial del clan. Curvas del Parque Nacional Henri Pittier, parada en mirador y almuerzo playero.",
                    category = "Ruta Oficial",
                    visibility = "PUBLICO_CLUB",
                    eventDate = "15/09/2026",
                    eventTime = "06:30 AM",
                    departureTime = "07:00 AM",
                    originAddress = "Redoma del Avión, Maracay (Estación de Servicio)",
                    destinationAddress = "Bahía de Cuyagua, Costa de Aragua",
                    originLatitude = 10.2319,
                    originLongitude = -67.5744,
                    destinationLatitude = 10.4903,
                    destinationLongitude = -67.7025,
                    terrainType = "Montaña / Mixto",
                    difficultyLevel = "Media",
                    weatherForecast = "Soleado 29°C • Viento 12 km/h • 5% Lluvia",
                    roadCaptain = "Capitán de Ruta TX Aragua",
                    tailRider = "Barredora Logística TX",
                    rsvpPilotsCount = 18,
                    rsvpPilotsList = "1,2,3,4",
                    rsvpPillionsCount = 8,
                    remindDaysBefore = 2,
                    isOfficialClubEvent = true,
                    creatorMemberId = 1L,
                    creatorName = "Directiva Nacional TX"
                ),
                BikerCalendarEvent(
                    id = 2L,
                    title = "Jornada de Mantenimiento: Válvulas y Aceite 🔧",
                    description = "Calibración con galgas de válvulas CG200 (Adm 0.05 / Esc 0.08) y cambio de aceite de alto rendimiento.",
                    category = "Mantenimiento / Garaje",
                    visibility = "PUBLICO_CLUB",
                    eventDate = "22/09/2026",
                    eventTime = "09:00 AM",
                    departureTime = "09:30 AM",
                    originAddress = "Taller Central Team TX, Maracay",
                    destinationAddress = "Taller Central Team TX, Maracay",
                    originLatitude = 10.2469,
                    originLongitude = -67.5958,
                    destinationLatitude = 10.2469,
                    destinationLongitude = -67.5958,
                    terrainType = "Asfalto",
                    difficultyLevel = "Fácil",
                    weatherForecast = "Parcialmente Nublado 27°C",
                    roadCaptain = "Jefe de Mecánica TX",
                    tailRider = "",
                    rsvpPilotsCount = 12,
                    rsvpPilotsList = "1,2",
                    rsvpPillionsCount = 0,
                    remindDaysBefore = 1,
                    isOfficialClubEvent = true,
                    creatorMemberId = 1L,
                    creatorName = "Directiva Nacional TX"
                )
            )
            calendarDao.upsertEvents(initialEvents)
        }
    }
}
