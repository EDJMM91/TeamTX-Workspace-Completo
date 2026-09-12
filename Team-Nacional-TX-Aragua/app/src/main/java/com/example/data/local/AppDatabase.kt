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
        EstadoReproductorEntity::class,
        BikerInterestPoint::class,
        SpotReport::class
    ],
    version = 33, // 🛡️ Módulos: Adición de Sitios de Interés TX y Denuncias
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
    abstract fun interestPointDao(): InterestPointDao
    abstract fun spotReportDao(): SpotReportDao
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

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `member_profiles` ADD COLUMN `disabledModulesJson` TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `biker_interest_points` (
                            `id` INTEGER PRIMARY KEY NOT NULL,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `latitude` REAL NOT NULL,
                            `longitude` REAL NOT NULL,
                            `imageUrl` TEXT,
                            `iconDrawableName` TEXT NOT NULL DEFAULT 'ic_menu_compass',
                            `phone` TEXT NOT NULL DEFAULT '',
                            `addedBy` TEXT NOT NULL DEFAULT 'Piloto Team TX',
                            `addedByMemberId` INTEGER NOT NULL DEFAULT 0,
                            `likesCount` INTEGER NOT NULL DEFAULT 0,
                            `dislikesCount` INTEGER NOT NULL DEFAULT 0,
                            `likedByMemberIds` TEXT NOT NULL DEFAULT '',
                            `dislikedByMemberIds` TEXT NOT NULL DEFAULT '',
                            `reportsCount` INTEGER NOT NULL DEFAULT 0,
                            `isReported` INTEGER NOT NULL DEFAULT 0,
                            `reportReason` TEXT,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `spot_reports` (
                            `id` INTEGER PRIMARY KEY NOT NULL,
                            `spotId` INTEGER NOT NULL,
                            `spotName` TEXT NOT NULL,
                            `spotType` TEXT NOT NULL,
                            `reporterMemberId` INTEGER NOT NULL,
                            `reporterName` TEXT NOT NULL,
                            `reporterPhone` TEXT NOT NULL,
                            `reason` TEXT NOT NULL,
                            `status` TEXT NOT NULL DEFAULT 'PENDIENTE',
                            `reviewedBy` TEXT,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `workshops_directory` ADD COLUMN `imageUrl` TEXT DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `workshops_directory` ADD COLUMN `iconDrawableName` TEXT NOT NULL DEFAULT 'ic_action_repair'")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "team_tx_venezuela_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34)
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
            ),
            // ══════════════════════════════════════════════════════════
            // 🏖️ PLAYAS Y DESTINOS COSTEROS (RUTAS BIKER ARAGUA Y VENEZUELA)
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 23L,
                name = "Bahía de Cata",
                type = "Playa / Costa",
                state = "Aragua",
                city = "Ocumare de la Costa",
                address = "Bahía de Cata, Municipio Costa de Oro, Aragua",
                phone = "0412-0000001",
                whatsapp = "04120000001",
                rating = 5.0,
                recommendedBy = "Team TX Aragua",
                notes = "Hermosa ensenada semicircular de arenas blancas y palmeras cocoteras. Parada obligada para moteros en la costa aragüeña.",
                latitude = 10.4917,
                longitude = -67.7389,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.4917,-67.7389"
            ),
            WorkshopDirectoryItem(
                id = 24L,
                name = "Playa Cuyagua",
                type = "Playa / Costa",
                state = "Aragua",
                city = "Cuyagua",
                address = "Carretera Cata - Cuyagua, Municipio Costa de Oro, Aragua",
                phone = "0412-0000002",
                whatsapp = "04120000002",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Mundialmente famosa por su oleaje y desembocadura de río. Destino predilecto para camping y aventura motera.",
                latitude = 10.4903,
                longitude = -67.6744,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.4903,-67.6744"
            ),
            WorkshopDirectoryItem(
                id = 25L,
                name = "Playa Grande (Choroní / Puerto Colombia)",
                type = "Playa / Costa",
                state = "Aragua",
                city = "Choroní",
                address = "Final Calle El Morro, Puerto Colombia, Choroní, Aragua",
                phone = "0412-0000003",
                whatsapp = "04120000003",
                rating = 5.0,
                recommendedBy = "Presidente Nacional TX",
                notes = "Extenso arenal dorado con cocoteros gigantes y ambiente festivo tradicional. Punto culmen de la ruta Henri Pittier.",
                latitude = 10.5050,
                longitude = -67.6067,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.5050,-67.6067"
            ),
            WorkshopDirectoryItem(
                id = 26L,
                name = "Playa El Playón",
                type = "Playa / Costa",
                state = "Aragua",
                city = "Ocumare de la Costa",
                address = "Malecón de El Playón, Ocumare de la Costa, Aragua",
                phone = "0412-0000004",
                whatsapp = "04120000004",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Bulevar costero y malecón con amplia variedad de kioskos gastronómicos de pescado frito y fácil acceso para motos.",
                latitude = 10.4852,
                longitude = -67.7661,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.4852,-67.7661"
            ),
            WorkshopDirectoryItem(
                id = 27L,
                name = "Bahía de Patanemo",
                type = "Playa / Costa",
                state = "Carabobo",
                city = "Puerto Cabello",
                address = "Pueblo de Patanemo, Municipio Puerto Cabello, Carabobo",
                phone = "0412-0000005",
                whatsapp = "04120000005",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Ensenada caribeña de aguas azul turquesa y laguna natural adyacente. Destino dominical tradicional de rodadas desde Aragua y Carabobo.",
                latitude = 10.4286,
                longitude = -67.9250,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.4286,-67.9250"
            ),
            WorkshopDirectoryItem(
                id = 28L,
                name = "Cayo Sombrero (Parque Nacional Morrocoy)",
                type = "Playa / Costa",
                state = "Falcón",
                city = "Chichiriviche",
                address = "P.N. Morrocoy, embarcadero Chichiriviche / Tucacas, Falcón",
                phone = "0412-0000006",
                whatsapp = "04120000006",
                rating = 5.0,
                recommendedBy = "Directiva Nacional TX",
                notes = "La joya coralina de Morrocoy. Arena blanca resplandeciente, aguas mansas y palmeras. Rodada épica costera por la carretera nacional.",
                latitude = 10.8800,
                longitude = -68.2100,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.8800,-68.2100"
            ),

            // ══════════════════════════════════════════════════════════
            // ⛰️ MIRADORES Y PARADORES BIKER
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 29L,
                name = "Mirador de Portachuelo (Henri Pittier)",
                type = "Mirador / Parador Biker",
                state = "Aragua",
                city = "Maracay",
                address = "Carretera Maracay - Ocumare de la Costa (Km 12), Aragua",
                phone = "0412-0000007",
                whatsapp = "04120000007",
                rating = 5.0,
                recommendedBy = "Team TX Aragua",
                notes = "Paso geográfico entre el Lago de Valencia y el Mar Caribe a 1.128 msnm. Selva nublada, clima fresco y paso de aves migratorias.",
                latitude = 10.3541,
                longitude = -67.6833,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.3541,-67.6833"
            ),
            WorkshopDirectoryItem(
                id = 30L,
                name = "Parador Turístico El Arco (Colonia Tovar)",
                type = "Mirador / Parador Biker",
                state = "Aragua",
                city = "Colonia Tovar",
                address = "Entrada principal a la Colonia Tovar, Aragua",
                phone = "0412-0000008",
                whatsapp = "04120000008",
                rating = 5.0,
                recommendedBy = "Directiva TX Aragua",
                notes = "El arco icónico de bienvenida a la Colonia Tovar. Foto obligatoria de caravana, clima templado de 16°C y dulces típicos.",
                latitude = 10.4078,
                longitude = -67.2911,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.4078,-67.2911"
            ),
            WorkshopDirectoryItem(
                id = 31L,
                name = "Mirador El Vigía (Faro de Choroní)",
                type = "Mirador / Parador Biker",
                state = "Aragua",
                city = "Choroní",
                address = "Colina del Morro de Puerto Colombia, Choroní, Aragua",
                phone = "0412-0000009",
                whatsapp = "04120000009",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Vista panorámica 360° al Mar Caribe, Puerto Colombia y el romper de las olas en los acantilados rocosos.",
                latitude = 10.5097,
                longitude = -67.6033,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.5097,-67.6033"
            ),
            WorkshopDirectoryItem(
                id = 32L,
                name = "Mirador El Jarillo",
                type = "Mirador / Parador Biker",
                state = "Miranda",
                city = "El Jarillo",
                address = "Ruta agrícola El Junquito - El Jarillo - Colonia Tovar",
                phone = "0412-0000010",
                whatsapp = "04120000010",
                rating = 4.9,
                recommendedBy = "Team TX Miranda",
                notes = "Valle de cultivo de duraznos y fresas, despegue de parapentes y curvas cerradas ideales para motos de doble propósito como la TX.",
                latitude = 10.3700,
                longitude = -67.1600,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.3700,-67.1600"
            ),
            WorkshopDirectoryItem(
                id = 33L,
                name = "Monumento y Mirador José Félix Ribas (Cerro de la Juventud)",
                type = "Mirador / Parador Biker",
                state = "Aragua",
                city = "La Victoria",
                address = "Cerro La Juventud, Autopista Regional del Centro, La Victoria, Aragua",
                phone = "0412-0000011",
                whatsapp = "04120000011",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Impresionante monumento conmemorativo con vista privilegiada a todos los Valles de Aragua y la autopista.",
                latitude = 10.2290,
                longitude = -67.3320,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2290,-67.3320"
            ),
            WorkshopDirectoryItem(
                id = 34L,
                name = "Mirador de la Represa de Taiguaiguay",
                type = "Mirador / Parador Biker",
                state = "Aragua",
                city = "Cagua",
                address = "Carretera Cagua - Villa de Cura, Sector Taiguaiguay, Aragua",
                phone = "0412-0000012",
                whatsapp = "04120000012",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Parador al borde del embalse artificial Taiguaiguay, excelente brisa y parada de descanso hacia los llanos.",
                latitude = 10.1550,
                longitude = -67.4850,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.1550,-67.4850"
            ),

            // ══════════════════════════════════════════════════════════
            // 🏞️ CASCADAS, RÍOS Y POZOS NATURALES
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 35L,
                name = "Pozo del Diablo / Las Cocuizas",
                type = "Cascadas / Ríos / Pozos",
                state = "Aragua",
                city = "Maracay",
                address = "Entrada Parque Recreacional Las Cocuizas, Río Güey, Maracay, Aragua",
                phone = "0412-0000013",
                whatsapp = "04120000013",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Pozos de agua cristalina y fría provenientes de las cumbres del Henri Pittier. Sitio predilecto de esparcimiento en Maracay.",
                latitude = 10.2789,
                longitude = -67.5850,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2789,-67.5850"
            ),
            WorkshopDirectoryItem(
                id = 36L,
                name = "Cascada y Río El Playón de Ocumare",
                type = "Cascadas / Ríos / Pozos",
                state = "Aragua",
                city = "Ocumare de la Costa",
                address = "Paso del Río Ocumare, Sector Las Monjas, Costa de Oro, Aragua",
                phone = "0412-0000014",
                whatsapp = "04120000014",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Balneario natural de agua dulce rodeado de frondosos árboles para refrescarse luego de la travesía de montaña.",
                latitude = 10.4550,
                longitude = -67.7550,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.4550,-67.7550"
            ),
            WorkshopDirectoryItem(
                id = 37L,
                name = "Aguas Termales de Las Trincheras",
                type = "Cascadas / Ríos / Pozos",
                state = "Carabobo",
                city = "Naguanagua",
                address = "Autopista Valencia - Puerto Cabello, Sector Las Trincheras, Carabobo",
                phone = "0241-8083888",
                whatsapp = "04144000001",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Una de las aguas termales minerales más calientes del mundo (hasta 92°C en manantial). Reconocidas por sus propiedades terapéuticas.",
                latitude = 10.3060,
                longitude = -68.0850,
                hasCredit = true,
                creditPlatforms = "Punto de Venta / Pago Móvil",
                googleMapsUrl = "https://maps.google.com/?q=10.3060,-68.0850"
            ),

            // ══════════════════════════════════════════════════════════
            // 🏛️ MONUMENTOS Y SITIOS HISTÓRICOS
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 38L,
                name = "Plaza Bolívar de Maracay y Teatro de la Ópera",
                type = "Monumento / Sitio Histórico",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar c/c Av. Miranda y Calle López Aveledo, Maracay, Aragua",
                phone = "0243-2000000",
                whatsapp = "04120000015",
                rating = 5.0,
                recommendedBy = "Directiva TX Aragua",
                notes = "La plaza urbana más grande de Latinoamérica (320m de longitud) con bellos jardines arbolados, fuentes y arquitectura histórica.",
                latitude = 10.2489,
                longitude = -67.5992,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2489,-67.5992"
            ),
            WorkshopDirectoryItem(
                id = 39L,
                name = "Hacienda Santa Teresa (Ruta del Ron)",
                type = "Monumento / Sitio Histórico",
                state = "Aragua",
                city = "El Consejo",
                address = "Carretera Panamericana, El Consejo, Municipio Revenga, Aragua",
                phone = "0244-4002600",
                whatsapp = "04140000002",
                rating = 5.0,
                recommendedBy = "Team TX Aragua",
                notes = "Hacienda bicentenaria fundada en 1796. Famosa por sus campos de rugby, callejón de chaguaramos centenarios y destilería de ron premium.",
                latitude = 10.2375,
                longitude = -67.2889,
                hasCredit = true,
                creditPlatforms = "Cashea / Tarjeta",
                googleMapsUrl = "https://maps.google.com/?q=10.2375,-67.2889"
            ),
            WorkshopDirectoryItem(
                id = 40L,
                name = "Monumento Histórico Campo de Carabobo",
                type = "Monumento / Sitio Histórico",
                state = "Carabobo",
                city = "Campo Carabobo",
                address = "Autopista del Sur, Municipio Libertador, Carabobo",
                phone = "0241-0000001",
                whatsapp = "04120000016",
                rating = 5.0,
                recommendedBy = "Directiva Nacional TX",
                notes = "Santuario de la independencia venezolana. Monumento del Arco de Triunfo, tumba del Soldado Desconocido y fuego sagrado eterno.",
                latitude = 10.0069,
                longitude = -68.1636,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.0069,-68.1636"
            ),
            WorkshopDirectoryItem(
                id = 41L,
                name = "Monumento Manto de María Divina Pastora",
                type = "Monumento / Sitio Histórico",
                state = "Lara",
                city = "Barquisimeto",
                address = "Sector Veragacha / El Ujano, Barquisimeto, Lara",
                phone = "0251-0000001",
                whatsapp = "04120000017",
                rating = 5.0,
                recommendedBy = "Team TX Lara",
                notes = "El monumento mariano más alto del planeta (62 metros). Estructura cinética vanguardista visible desde casi toda la ciudad.",
                latitude = 10.0639,
                longitude = -69.2789,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.0639,-69.2789"
            ),
            WorkshopDirectoryItem(
                id = 42L,
                name = "Monumento a la Virgen de la Paz",
                type = "Monumento / Sitio Histórico",
                state = "Trujillo",
                city = "Trujillo",
                address = "Peña de la Virgen, Municipio Trujillo, Estado Trujillo",
                phone = "0272-0000001",
                whatsapp = "04120000018",
                rating = 5.0,
                recommendedBy = "Directiva Nacional TX",
                notes = "Colosal escultura de 46,72 metros de altura a 1.600 msnm. Miradores internos en los ojos y brazos con vista hacia el Lago de Maracaibo.",
                latitude = 9.3486,
                longitude = -70.4633,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=9.3486,-70.4633"
            ),

            // ══════════════════════════════════════════════════════════
            // 🏍️ PUNTOS DE ENCUENTRO Y SALIDA DE CARAVANAS
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 43L,
                name = "Redoma del Avión (Punto Clave Caravana Aragua)",
                type = "Punto de Encuentro Caravana",
                state = "Aragua",
                city = "Maracay",
                address = "Intersección Autopista Regional del Centro con Av. Maracay, Aragua",
                phone = "0412-0000019",
                whatsapp = "04120000019",
                rating = 5.0,
                recommendedBy = "Presidente Nacional TX",
                notes = "Punto de reunión histórico y oficial del Team TX Aragua antes de iniciar rodadas hacia Caracas, Carabobo o el Llano.",
                latitude = 10.2222,
                longitude = -67.5756,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2222,-67.5756"
            ),
            WorkshopDirectoryItem(
                id = 44L,
                name = "Peaje de La Cabrera (Punto Enlace ARC)",
                type = "Punto de Encuentro Caravana",
                state = "Aragua",
                city = "Maracay",
                address = "Autopista Regional del Centro, Límite Aragua - Carabobo",
                phone = "0412-0000020",
                whatsapp = "04120000020",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Punto de parada técnica y reagrupamiento de caravanas de motos entre los capítulos de Aragua y Carabobo.",
                latitude = 10.2520,
                longitude = -67.6950,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2520,-67.6950"
            ),
            WorkshopDirectoryItem(
                id = 45L,
                name = "E/S Bohío (ARC Carabobo - Parada Biker)",
                type = "Punto de Encuentro Caravana",
                state = "Carabobo",
                city = "Guacara",
                address = "Autopista Regional del Centro Km 147, Tramo Valencia - Guacara, Carabobo",
                phone = "0412-0000021",
                whatsapp = "04120000021",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Estación de servicio, mini-market 24 horas y amplio estacionamiento para decenas de motocicletas en ruta.",
                latitude = 10.1890,
                longitude = -67.9250,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.1890,-67.9250"
            ),
            WorkshopDirectoryItem(
                id = 46L,
                name = "Redoma de Guaparo (Salida a Puerto Cabello)",
                type = "Punto de Encuentro Caravana",
                state = "Carabobo",
                city = "Valencia",
                address = "Final Av. Bolívar Norte con inicio de autopista a Puerto Cabello, Valencia",
                phone = "0412-0000022",
                whatsapp = "04120000022",
                rating = 4.8,
                recommendedBy = "Team TX Carabobo",
                notes = "Punto de partida de los moteros del centro del país para bajar en caravana hacia las costas de Carabobo y Falcón.",
                latitude = 10.2260,
                longitude = -68.0080,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2260,-68.0080"
            ),

            // ══════════════════════════════════════════════════════════
            // 🛞 CAUCHERAS Y VULCANIZADORAS PARA MOTOS
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 47L,
                name = "Cauchera y Vulcanizadora Los Samanes 24H",
                type = "Cauchera & Vulcanizadora",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Los Samanes c/c Av. Fuerzas Aéreas, Maracay, Aragua",
                phone = "0412-3456789",
                whatsapp = "04123456789",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Servicio ininterrumpido 24 horas. Vulcanizado en frío y caliente, parches tubeless de moto, inflado con nitrógeno y tripas TX.",
                latitude = 10.2190,
                longitude = -67.5920,
                hasCredit = true,
                creditPlatforms = "Pago Móvil / Efectivo / Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.2190,-67.5920"
            ),
            WorkshopDirectoryItem(
                id = 48L,
                name = "Cauchera San Agustín Moto Biker",
                type = "Cauchera & Vulcanizadora",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar Este, sector San Agustín, Maracay, Aragua",
                phone = "0414-4567890",
                whatsapp = "04144567890",
                rating = 4.8,
                recommendedBy = "Directiva TX Aragua",
                notes = "Especialistas en montaje y balanceo de neumáticos para motos de alta y media cilindrada. Venta de cauchos mixtos y doble propósito.",
                latitude = 10.2450,
                longitude = -67.5890,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.2450,-67.5890"
            ),
            WorkshopDirectoryItem(
                id = 49L,
                name = "Multiservicios & Cauchera El Limón",
                type = "Cauchera & Vulcanizadora",
                state = "Aragua",
                city = "El Limón",
                address = "Av. Universidad c/c Calle Caracas, El Limón, Municipio Mario Briceño Iragorry, Aragua",
                phone = "0424-5678901",
                whatsapp = "04245678901",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Punto de chequeo obligado antes de subir a Choroní u Ocumare. Calibración digital de presión y revisión de válvulas.",
                latitude = 10.2760,
                longitude = -67.6320,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2760,-67.6320"
            ),
            WorkshopDirectoryItem(
                id = 50L,
                name = "Vulcanizadora La Encrucijada de Cagua",
                type = "Cauchera & Vulcanizadora",
                state = "Aragua",
                city = "Cagua",
                address = "Nudo vial La Encrucijada, salida hacia Carretera Nacional Cagua, Aragua",
                phone = "0416-6789012",
                whatsapp = "04166789012",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Ubicación neurálgica entre la ARC y la Carretera Nacional. Reparación express de cauchos pinchados en carretera.",
                latitude = 10.1980,
                longitude = -67.4780,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.1980,-67.4780"
            ),
            WorkshopDirectoryItem(
                id = 51L,
                name = "Cauchera Express Palo Negro",
                type = "Cauchera & Vulcanizadora",
                state = "Aragua",
                city = "Palo Negro",
                address = "Av. Principal Palo Negro, frente a la Plaza Bolívar, Aragua",
                phone = "0412-7890123",
                whatsapp = "04127890123",
                rating = 4.7,
                recommendedBy = "Team TX Aragua",
                notes = "Cambio rápido de tripas de moto, venta de válvulas metálicas, parches reforzados y alineación de rines de rayos.",
                latitude = 10.1740,
                longitude = -67.5510,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.1740,-67.5510"
            ),
            WorkshopDirectoryItem(
                id = 52L,
                name = "Cauchera & Alineación Los Guayos (ARC)",
                type = "Cauchera & Vulcanizadora",
                state = "Carabobo",
                city = "Los Guayos",
                address = "Lateral ARC Km 152, sentido Maracay - Valencia, Carabobo",
                phone = "0414-8901234",
                whatsapp = "04148901234",
                rating = 4.8,
                recommendedBy = "Team TX Carabobo",
                notes = "Atención de emergencia para motos accidentadas por pinchazos en la Autopista Regional del Centro.",
                latitude = 10.1850,
                longitude = -67.9350,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.1850,-67.9350"
            ),
            WorkshopDirectoryItem(
                id = 53L,
                name = "Cauchera 24 Horas Puerto Cabello (San Esteban)",
                type = "Cauchera & Vulcanizadora",
                state = "Carabobo",
                city = "Puerto Cabello",
                address = "Entrada a San Esteban, bajando de la autopista, Puerto Cabello, Carabobo",
                phone = "0424-9012345",
                whatsapp = "04249012345",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Vulcanizadora activa 24H al pie de la bajada de Puerto Cabello. Auxilio seguro tras descender de la montaña.",
                latitude = 10.4600,
                longitude = -68.0200,
                hasCredit = true,
                creditPlatforms = "Pago Móvil / Divisas",
                googleMapsUrl = "https://maps.google.com/?q=10.4600,-68.0200"
            ),

            // ══════════════════════════════════════════════════════════
            // 🔧 TALLERES MECÁNICOS ESPECIALIZADOS
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 54L,
                name = "Taller Moto Rendimiento TX La Julia",
                type = "Taller Mecánico",
                state = "Aragua",
                city = "Maracay",
                address = "Zona Industrial La Julia, Calle 2, Maracay, Aragua",
                phone = "0414-4601122",
                whatsapp = "04144601122",
                rating = 5.0,
                recommendedBy = "Mecánico Oficial TX",
                notes = "Taller de alta especialidad en Empire TX 200: caja de cambios, sistema de arranque, carburación de competencia y tren motriz.",
                latitude = 10.2410,
                longitude = -67.6150,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.google.com/?q=10.2410,-67.6150"
            ),
            WorkshopDirectoryItem(
                id = 55L,
                name = "Mecánica Biker 23 de Enero",
                type = "Taller Mecánico",
                state = "Aragua",
                city = "Maracay",
                address = "Sector 23 de Enero, Calle Principal #45, Maracay, Aragua",
                phone = "0412-5502233",
                whatsapp = "04125502233",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Entonación de motor, cambio rápido de aceite 20W50 / 10W40 semi-sintético, pastillas de freno y tensado de cadena.",
                latitude = 10.2490,
                longitude = -67.6120,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2490,-67.6120"
            ),
            WorkshopDirectoryItem(
                id = 56L,
                name = "Taller El Negro Moto Sport (Cagua)",
                type = "Taller Mecánico",
                state = "Aragua",
                city = "Cagua",
                address = "Calle Boyacá c/c Calle Sabana Larga, Cagua, Aragua",
                phone = "0424-3304455",
                whatsapp = "04243304455",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Diagnóstico mecánico integral, embrague, cambio de guayas, filtros y mantenimiento de barras telescópicas.",
                latitude = 10.1820,
                longitude = -67.4590,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.1820,-67.4590"
            ),
            WorkshopDirectoryItem(
                id = 57L,
                name = "Taller y Rectificadora Motos La Victoria",
                type = "Taller Mecánico",
                state = "Aragua",
                city = "La Victoria",
                address = "Av. Francisco de Miranda, Sector La Estación, La Victoria, Aragua",
                phone = "0416-2206677",
                whatsapp = "04162206677",
                rating = 4.8,
                recommendedBy = "Directiva TX Aragua",
                notes = "Rectificación de cilindros, ajuste de válvulas, encamisado y banco de herramientas completo para armar motores desde cero.",
                latitude = 10.2250,
                longitude = -67.3380,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2250,-67.3380"
            ),
            WorkshopDirectoryItem(
                id = 58L,
                name = "Mecánica Integral Motos Turmero",
                type = "Taller Mecánico",
                state = "Aragua",
                city = "Turmero",
                address = "Calle Rivas Dávila c/c Calle Petión, Centro de Turmero, Aragua",
                phone = "0412-8807788",
                whatsapp = "04128807788",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Mantenimiento preventivo, ajuste de rines de rayos, rodamientos cónicos de dirección y suspensión trasera monoshock.",
                latitude = 10.2280,
                longitude = -67.4950,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.2280,-67.4950"
            ),
            WorkshopDirectoryItem(
                id = 59L,
                name = "Taller Moto Box Valencia (Av. Lara)",
                type = "Taller Mecánico",
                state = "Carabobo",
                city = "Valencia",
                address = "Av. Lara cruce con Calle Boyacá, Valencia, Carabobo",
                phone = "0414-4109900",
                whatsapp = "04144109900",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Taller amplio con mecánicos certificados, limpieza de inyectores/carburadores por ultrasonido y repuestos genuinos.",
                latitude = 10.1780,
                longitude = -67.9950,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.google.com/?q=10.1780,-67.9950"
            ),

            // ══════════════════════════════════════════════════════════
            // ⚡ ELECTRICIDAD, BATERÍAS Y SISTEMAS LED
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 60L,
                name = "Electro Motos Maracay (Av. Ayacucho)",
                type = "Electricidad & Baterías",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Ayacucho Norte, entre Av. Bolívar y Santos Michelena, Maracay, Aragua",
                phone = "0412-1112233",
                whatsapp = "04121112233",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Especialistas en ramales eléctricos, coronas de encendido, estatores, CDI racing, reguladores de voltaje y cableado resistente al calor.",
                latitude = 10.2460,
                longitude = -67.6040,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.2460,-67.6040"
            ),
            WorkshopDirectoryItem(
                id = 61L,
                name = "Baterías & Luces Biker C.A",
                type = "Electricidad & Baterías",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Constitución Este, frente al Terminal Central, Maracay, Aragua",
                phone = "0424-2223344",
                whatsapp = "04242223344",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Venta e instalación de baterías de gel y secas de alto amperaje para TX 200. Instalación de exploradoras LED duales y relés de protección.",
                latitude = 10.2380,
                longitude = -67.5950,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.2380,-67.5950"
            ),

            // ══════════════════════════════════════════════════════════
            // ⚙️ TORNERÍA, SOLDADURA Y FABRICACIÓN BIKER
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 62L,
                name = "Tornería & Soldaduras Especiales Aragua (Piñonal)",
                type = "Tornería & Soldadura",
                state = "Aragua",
                city = "Maracay",
                address = "Calle 10, Sector Piñonal, Maracay, Aragua",
                phone = "0414-3334455",
                whatsapp = "04143334455",
                rating = 5.0,
                recommendedBy = "Directiva TX Aragua",
                notes = "Soldadura TIG y MIG en aluminio y hierro. Fabricación y adaptación de defensas tubulares, sliders, parrillas porta-alforjas y reposapiés.",
                latitude = 10.2310,
                longitude = -67.5820,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.2310,-67.5820"
            ),

            // ══════════════════════════════════════════════════════════
            // 🚨 AUXILIO VIAL Y RESCATE 24 HORAS
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 63L,
                name = "Grúas & Auxilio Vial Motero Aragua 24H",
                type = "Auxilio Vial 24H",
                state = "Aragua",
                city = "Maracay",
                address = "Base de operaciones Maracay Centro con cobertura en toda la ARC y carreteras de costa",
                phone = "0412-9998877",
                whatsapp = "04129998877",
                rating = 5.0,
                recommendedBy = "Presidente Nacional TX",
                notes = "Camión plataforma especializado con calzos y cinchas de amarre para motos. Rescate de pilotos varados en autopista y carreteras de montaña 24/7.",
                latitude = 10.2400,
                longitude = -67.6000,
                hasCredit = true,
                creditPlatforms = "Pago Móvil / Transferencia",
                googleMapsUrl = "https://maps.google.com/?q=10.2400,-67.6000"
            ),

            // ══════════════════════════════════════════════════════════
            // 🌲 PARQUES NACIONALES Y RUTAS RETO
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 64L,
                name = "Parque Nacional Henri Pittier (Paso Ecológico)",
                type = "Parque Nacional / Reserva Natural",
                state = "Aragua",
                city = "Maracay",
                address = "Carretera Maracay - Choroní y Maracay - Ocumare, Aragua",
                phone = "0243-2001111",
                whatsapp = "04120000023",
                rating = 5.0,
                recommendedBy = "Directiva Nacional TX",
                notes = "El parque nacional pionero de Venezuela (1937). 107.800 hectáreas de biodiversidad extrema, desde las cumbres de montaña hasta el Caribe.",
                latitude = 10.3333,
                longitude = -67.6667,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=10.3333,-67.6667"
            ),
            WorkshopDirectoryItem(
                id = 65L,
                name = "Ruta Transandina - Paso del Cóndor (Pico El Águila)",
                type = "Montaña / Ruta",
                state = "Mérida",
                city = "Apartaderos",
                address = "Carretera Trasandina Troncal 7, Paso del Cóndor, Estado Mérida",
                phone = "0412-0000024",
                whatsapp = "04120000024",
                rating = 5.0,
                recommendedBy = "Presidente Nacional TX",
                notes = "El punto carretero más elevado de Venezuela (4.118 msnm). Clima glacial, monumento al Cóndor y la rodada de montaña más emblemática del país.",
                latitude = 8.8524,
                longitude = -70.8312,
                hasCredit = false,
                creditPlatforms = "",
                googleMapsUrl = "https://maps.google.com/?q=8.8524,-70.8312"
            ),
            // ══════════════════════════════════════════════════════════
            // 🏍️ CONCESIONARIOS OFICIALES DE MOTOS (EMPIRE KEEWAY, BERA, BAJAJ, TORO, SUZUKI, YAMAHA)
            // ══════════════════════════════════════════════════════════
            WorkshopDirectoryItem(
                id = 66L,
                name = "Empire Keeway Concesionario Principal Maracay (Paradise Motors)",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Sucre c/c Av. Casanova Godoy, Maracay, Aragua",
                phone = "0424-3457013",
                whatsapp = "04243457013",
                rating = 5.0,
                recommendedBy = "Presidente Nacional TX",
                notes = "Concesionario oficial de la ensambladora Empire Keeway. Venta de TX 200, Horse, Owen, Arsen, repuestos originales de fábrica y garantía.",
                latitude = 10.2511,
                longitude = -67.5977,
                hasCredit = true,
                creditPlatforms = "Cashea / Financiamiento Directo",
                googleMapsUrl = "https://maps.google.com/?q=10.2511,-67.5977"
            ),
            WorkshopDirectoryItem(
                id = 67L,
                name = "Empire Keeway Concesionario La Encrucijada / Cagua",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Cagua",
                address = "Distribuidor La Encrucijada, C.C. La Pirámide, Carretera Nacional Cagua, Aragua",
                phone = "0414-5880424",
                whatsapp = "04145880424",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Concesionario oficial Empire Keeway para el eje este de Aragua. Exhibición completa de motos 0 Km, repuestos originales y servicio post-venta.",
                latitude = 10.1990,
                longitude = -67.4765,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.google.com/?q=10.1990,-67.4765"
            ),
            WorkshopDirectoryItem(
                id = 68L,
                name = "Empire Keeway Concesionario Valencia (Paseo Cabriales)",
                type = "Concesionario de Motos",
                state = "Carabobo",
                city = "Valencia",
                address = "Av. Paseo Cabriales c/c Calle Navas Spinola, Valencia, Carabobo",
                phone = "0414-4208899",
                whatsapp = "04144208899",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Sede oficial Empire Keeway en Valencia. Venta de motocicletas nuevas, taller certificado y repuestos directos de planta ensambladora.",
                latitude = 10.1920,
                longitude = -68.0010,
                hasCredit = true,
                creditPlatforms = "Cashea / Financiamiento de Fábrica",
                googleMapsUrl = "https://maps.google.com/?q=10.1920,-68.0010"
            ),
            WorkshopDirectoryItem(
                id = 69L,
                name = "Empire Keeway Concesionario Caracas (Av. Francisco de Miranda)",
                type = "Concesionario de Motos",
                state = "Distrito Capital",
                city = "Caracas",
                address = "Av. Francisco de Miranda, Edif. Centro Seguros La Paz, La California / Los Cortijos, Caracas",
                phone = "0212-2374455",
                whatsapp = "04122374455",
                rating = 5.0,
                recommendedBy = "Directiva Nacional TX",
                notes = "Concesionario insignia de la capital. Stock permanente de modelos TX 200, motos automáticas y motos de trabajo con entrega inmediata.",
                latitude = 10.4890,
                longitude = -66.8280,
                hasCredit = true,
                creditPlatforms = "Cashea / Crédito Bancario",
                googleMapsUrl = "https://maps.google.com/?q=10.4890,-66.8280"
            ),
            WorkshopDirectoryItem(
                id = 70L,
                name = "Bera Motorcycles Concesionario Principal Maracay",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar Este, frente a la Torre Sindoni, Maracay, Aragua",
                phone = "0412-4001122",
                whatsapp = "04124001122",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Sede central de Motos Bera en Maracay. Venta de SBR, Bera Kavak, León, Runner, repuestos genuinos y taller de servicio oficial.",
                latitude = 10.2465,
                longitude = -67.5935,
                hasCredit = true,
                creditPlatforms = "Cashea / Plan Bera Cuotas",
                googleMapsUrl = "https://maps.google.com/?q=10.2465,-67.5935"
            ),
            WorkshopDirectoryItem(
                id = 71L,
                name = "Bera Motorcycles Concesionario Cagua",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Cagua",
                address = "Carretera Nacional Cagua - La Villa, Sector La Romana, Cagua, Aragua",
                phone = "0424-3112233",
                whatsapp = "04243112233",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Concesionario Bera en Cagua. Entrega inmediata de motos 0 Km, cascos, placas, repuestos y servicio técnico autorizado.",
                latitude = 10.1870,
                longitude = -67.4620,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.1870,-67.4620"
            ),
            WorkshopDirectoryItem(
                id = 72L,
                name = "Bera Motorcycles Concesionario Turmero",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Turmero",
                address = "Av. Intercomunal Santiago Mariño, Sector La Morita, Turmero, Aragua",
                phone = "0414-4889900",
                whatsapp = "04144889900",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Showroom de exhibición Bera. Amplio stock de repuestos de carrocería, motor, cauchos originales y financiamiento.",
                latitude = 10.2240,
                longitude = -67.5350,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.google.com/?q=10.2240,-67.5350"
            ),
            WorkshopDirectoryItem(
                id = 73L,
                name = "Bera Motorcycles Concesionario Valencia (Av. Lara)",
                type = "Concesionario de Motos",
                state = "Carabobo",
                city = "Valencia",
                address = "Av. Lara c/c Av. Branger, Sector Centro-Sur, Valencia, Carabobo",
                phone = "0412-4993311",
                whatsapp = "04124993311",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Gran agencia oficial de Bera Motorcycles en Carabobo. Venta de modelos SBR 150, repuestos y accesorios.",
                latitude = 10.1775,
                longitude = -67.9920,
                hasCredit = true,
                creditPlatforms = "Cashea / Financiamiento Directo",
                googleMapsUrl = "https://maps.google.com/?q=10.1775,-67.9920"
            ),
            WorkshopDirectoryItem(
                id = 74L,
                name = "Bajaj Venezuela Concesionario Maracay (Pulsar / Dominar)",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar Oeste, sector San José, diagonal a Makro, Maracay, Aragua",
                phone = "0414-5992211",
                whatsapp = "04145992211",
                rating = 5.0,
                recommendedBy = "Team TX Aragua",
                notes = "Concesionario oficial de motos Bajaj en Aragua. Líneas Pulsar NS 200, Dominar 400, Boxer 150, repuestos originales DTS-i y servicio técnico de alta gama.",
                latitude = 10.2530,
                longitude = -67.6180,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom / Financiamiento",
                googleMapsUrl = "https://maps.google.com/?q=10.2530,-67.6180"
            ),
            WorkshopDirectoryItem(
                id = 75L,
                name = "Bajaj Venezuela Concesionario Valencia (Av. Bolívar Norte)",
                type = "Concesionario de Motos",
                state = "Carabobo",
                city = "Valencia",
                address = "Av. Bolívar Norte, Urbanización La Alegría, Valencia, Carabobo",
                phone = "0424-4118822",
                whatsapp = "04244118822",
                rating = 5.0,
                recommendedBy = "Team TX Carabobo",
                notes = "Representante exclusivo Bajaj en Carabobo. Venta de motos touring y urbanas, escaneo computarizado y repuestos genuinos de fábrica.",
                latitude = 10.2150,
                longitude = -68.0050,
                hasCredit = true,
                creditPlatforms = "Cashea / Crédito Bancario",
                googleMapsUrl = "https://maps.google.com/?q=10.2150,-68.0050"
            ),
            WorkshopDirectoryItem(
                id = 76L,
                name = "Bajaj Venezuela Concesionario Caracas (Las Mercedes)",
                type = "Concesionario de Motos",
                state = "Miranda",
                city = "Caracas",
                address = "Av. Río de Janeiro c/c Calle Trinidad, Las Mercedes, Caracas",
                phone = "0212-9915566",
                whatsapp = "04129915566",
                rating = 5.0,
                recommendedBy = "Directiva Nacional TX",
                notes = "Showroom premium de motos Bajaj en Caracas. Modelos Dominar 250/400 para motoviajeros, Pulsar N250 y taller oficial con repuestos garantizados.",
                latitude = 10.4780,
                longitude = -66.8620,
                hasCredit = true,
                creditPlatforms = "Cashea / Tarjeta / Transferencia",
                googleMapsUrl = "https://maps.google.com/?q=10.4780,-66.8620"
            ),
            WorkshopDirectoryItem(
                id = 77L,
                name = "Toro Motorcycles Concesionario Maracay (Av. Las Delicias)",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Las Delicias, cruce con Av. Casanova Godoy, Maracay, Aragua",
                phone = "0412-8884433",
                whatsapp = "04128884433",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Concesionario oficial de la nueva marca Toro Motorcycles. Modelos Rex 150, Jaguar, Leon 200, repuestos, garantía y financiamiento por cuotas.",
                latitude = 10.2610,
                longitude = -67.5980,
                hasCredit = true,
                creditPlatforms = "Cashea / Plan Toro Cuotas",
                googleMapsUrl = "https://maps.google.com/?q=10.2610,-67.5980"
            ),
            WorkshopDirectoryItem(
                id = 78L,
                name = "Toro Motorcycles Concesionario Cagua",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Cagua",
                address = "Calle Sabana Larga, Centro Comercial Cagua Plaza, Cagua, Aragua",
                phone = "0424-3771199",
                whatsapp = "04243771199",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Agencia Toro autorizada. Venta de motos nuevas con casco y placa incluidos, repuestos originales y taller de garantía.",
                latitude = 10.1835,
                longitude = -67.4580,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.1835,-67.4580"
            ),
            WorkshopDirectoryItem(
                id = 79L,
                name = "Toro Motorcycles Concesionario Valencia (Av. Cedeño)",
                type = "Concesionario de Motos",
                state = "Carabobo",
                city = "Valencia",
                address = "Av. Cedeño cruce con Av. Díaz Moreno, Valencia, Carabobo",
                phone = "0414-4330088",
                whatsapp = "04144330088",
                rating = 4.9,
                recommendedBy = "Team TX Carabobo",
                notes = "Gran agencia Toro en Valencia. Venta de motocicletas 0 Km, entrega inmediata, repuestos originales y crédito por aplicación.",
                latitude = 10.1890,
                longitude = -68.0040,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.google.com/?q=10.1890,-68.0040"
            ),
            WorkshopDirectoryItem(
                id = 80L,
                name = "Suzuki Motos Venezuela (Le Tour Bike Maracay)",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Bolívar Oeste c/c Calle Mariño, Maracay, Aragua",
                phone = "0414-3928552",
                whatsapp = "04143928552",
                rating = 5.0,
                recommendedBy = "Presidente Nacional TX",
                notes = "Concesionario oficial Suzuki en Maracay. Motos V-Strom, GN 125, Gixxer 150/250, repuestos japoneses originales, cafetería biker y accesorios premium.",
                latitude = 10.2540,
                longitude = -67.6111,
                hasCredit = true,
                creditPlatforms = "Cashea / Financiamiento Directo",
                googleMapsUrl = "https://maps.google.com/?q=10.2540,-67.6111"
            ),
            WorkshopDirectoryItem(
                id = 81L,
                name = "Yamaha Motor Venezuela (Valencia Sede Norte)",
                type = "Concesionario de Motos",
                state = "Carabobo",
                city = "Valencia",
                address = "Av. Bolívar Norte, frente al C.C. Camoruco, Valencia, Carabobo",
                phone = "0241-8241100",
                whatsapp = "04144241100",
                rating = 5.0,
                recommendedBy = "Team TX Carabobo",
                notes = "Distribuidor autorizado Yamaha. Modelos XTZ 125/250, MT-03, FZ-25, lubricantes Yamalube, repuestos genuinos y servicio técnico de primer nivel.",
                latitude = 10.2180,
                longitude = -68.0060,
                hasCredit = true,
                creditPlatforms = "Crédito Bancario / Tarjeta",
                googleMapsUrl = "https://maps.google.com/?q=10.2180,-68.0060"
            ),
            WorkshopDirectoryItem(
                id = 82L,
                name = "Yamaha Motor Caracas (Av. Francisco de Miranda)",
                type = "Concesionario de Motos",
                state = "Miranda",
                city = "Caracas",
                address = "Av. Francisco de Miranda, Edif. Yamaha, Los Ruices, Caracas",
                phone = "0212-2390011",
                whatsapp = "04122390011",
                rating = 5.0,
                recommendedBy = "Directiva Nacional TX",
                notes = "Concesionario central Yamaha en la capital. Modelos de alta y media cilindrada, motos doble propósito, boutique de cascos y repuestos originales.",
                latitude = 10.4930,
                longitude = -66.8350,
                hasCredit = true,
                creditPlatforms = "Cashea / Tarjeta",
                googleMapsUrl = "https://maps.google.com/?q=10.4930,-66.8350"
            ),
            WorkshopDirectoryItem(
                id = 83L,
                name = "Moto Planet Concesionario Multimarca Maracay",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "Maracay",
                address = "Av. Constitución c/c Calle Pichincha, Maracay, Aragua",
                phone = "0412-6778899",
                whatsapp = "04126778899",
                rating = 4.9,
                recommendedBy = "Team TX Aragua",
                notes = "Concesionario multimarca integral: Empire Keeway, Bera, Toro, Benelli y Suzuki. Venta de motos nuevas y usadas seleccionadas con garantía.",
                latitude = 10.2440,
                longitude = -67.5985,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.google.com/?q=10.2440,-67.5985"
            ),
            WorkshopDirectoryItem(
                id = 84L,
                name = "Mundo Motos Venezuela (La Victoria)",
                type = "Concesionario de Motos",
                state = "Aragua",
                city = "La Victoria",
                address = "Av. Loreto c/c Calle Rivas Dávila, La Victoria, Aragua",
                phone = "0416-5443322",
                whatsapp = "04165443322",
                rating = 4.8,
                recommendedBy = "Team TX Aragua",
                notes = "Agencia multimarca para el eje oriental de Aragua. Venta de motocicletas nuevas, trámites de registro automotor INTT y repuestos.",
                latitude = 10.2270,
                longitude = -67.3350,
                hasCredit = true,
                creditPlatforms = "Cashea",
                googleMapsUrl = "https://maps.google.com/?q=10.2270,-67.3350"
            ),
            WorkshopDirectoryItem(
                id = 85L,
                name = "Empire Keeway Concesionario Barquisimeto",
                type = "Concesionario de Motos",
                state = "Lara",
                city = "Barquisimeto",
                address = "Av. Venezuela c/c Calle 26, Barquisimeto, Lara",
                phone = "0251-2334455",
                whatsapp = "04145233445",
                rating = 4.9,
                recommendedBy = "Team TX Lara",
                notes = "Distribuidor líder Empire Keeway en la región centro-occidental. Amplia sala de ventas, repuestos originales de fábrica y servicio de taller.",
                latitude = 10.0710,
                longitude = -69.3180,
                hasCredit = true,
                creditPlatforms = "Cashea / Financiamiento Directo",
                googleMapsUrl = "https://maps.google.com/?q=10.0710,-69.3180"
            ),
            WorkshopDirectoryItem(
                id = 86L,
                name = "Bera Motorcycles Barquisimeto (Av. Pedro León Torres)",
                type = "Concesionario de Motos",
                state = "Lara",
                city = "Barquisimeto",
                address = "Av. Pedro León Torres con Calle 54, Barquisimeto, Lara",
                phone = "0412-5118800",
                whatsapp = "04125118800",
                rating = 4.8,
                recommendedBy = "Team TX Lara",
                notes = "Concesionario Bera en Barquisimeto. Venta de motos al mayor y detal, planes de financiamiento, repuestos originales y entrega de placas.",
                latitude = 10.0650,
                longitude = -69.3390,
                hasCredit = true,
                creditPlatforms = "Cashea / Plan Cuotas",
                googleMapsUrl = "https://maps.google.com/?q=10.0650,-69.3390"
            ),
            WorkshopDirectoryItem(
                id = 87L,
                name = "Bajaj Venezuela Concesionario Barquisimeto (Carrera 19)",
                type = "Concesionario de Motos",
                state = "Lara",
                city = "Barquisimeto",
                address = "Carrera 19 con Calle 30, Centro de Barquisimeto, Lara",
                phone = "0424-5332211",
                whatsapp = "04245332211",
                rating = 4.9,
                recommendedBy = "Team TX Lara",
                notes = "Agencia autorizada Bajaj para motos Pulsar, Dominar y Boxer. Repuestos genuinos de la India, escáner y servicio especializado.",
                latitude = 10.0680,
                longitude = -69.3240,
                hasCredit = true,
                creditPlatforms = "Cashea / Rapikom",
                googleMapsUrl = "https://maps.google.com/?q=10.0680,-69.3240"
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
