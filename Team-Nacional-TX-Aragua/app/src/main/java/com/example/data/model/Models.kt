package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

enum class MemberRole(
    val displayName: String,
    val badgeColorHex: Long,
    val canManageApp: Boolean = false,
    val canCreateRides: Boolean = false,
    val canPostAnnouncements: Boolean = false,
    val canManageMembers: Boolean = false,
    val canManageFinances: Boolean = false,
    val canCoordinateEmergency: Boolean = true,
    val defaultTitle: String = displayName,
    val roleDuties: String = "Participación y deberes de la agrupación."
) {
    PRESIDENTE(
        "Presidente Nacional",
        0xFFFFD700,
        canManageApp = true,
        canCreateRides = true,
        canPostAnnouncements = true,
        canManageMembers = true,
        canManageFinances = true,
        canCoordinateEmergency = true,
        defaultTitle = "Presidente Nacional",
        roleDuties = "Liderazgo supremo del club, representación legal, edición de rangos de directiva y veto institucional."
    ),
    VICEPRESIDENTE(
        "Vicepresidente",
        0xFFFFC107,
        canManageApp = true,
        canCreateRides = true,
        canPostAnnouncements = true,
        canManageMembers = true,
        canManageFinances = true,
        canCoordinateEmergency = true,
        defaultTitle = "Vicepresidente Ejecutivo",
        roleDuties = "Suplencia ejecutiva del Presidente, supervisión de capítulos regionales y coordinación general de logística."
    ),
    DISCIPLINARIO(
        "Oficial Disciplinario / Tribunal de Honor",
        0xFFE53935,
        canManageApp = true,
        canCreateRides = false,
        canPostAnnouncements = true,
        canManageMembers = true,
        canManageFinances = false,
        canCoordinateEmergency = true,
        defaultTitle = "Fiscal Disciplinario / Tribunal de Honor",
        roleDuties = "Aplicación del Código de Honor, régimen disciplinario, investigación de faltas y ejecución de suspensiones."
    ),
    SECRETARIO(
        "Secretario General",
        0xFF00ACC1,
        canManageApp = true,
        canCreateRides = false,
        canPostAnnouncements = true,
        canManageMembers = true,
        canManageFinances = false,
        canCoordinateEmergency = false,
        defaultTitle = "Secretario General de Actas y Prensa",
        roleDuties = "Redacción de actas oficiales, emisión de comunicados institucionales, registro y carnetización de miembros."
    ),
    TESORERO(
        "Tesorero / Finanzas",
        0xFFFF9800,
        canManageApp = true,
        canCreateRides = false,
        canPostAnnouncements = true,
        canManageMembers = false,
        canManageFinances = true,
        canCoordinateEmergency = false,
        defaultTitle = "Tesorero Nacional",
        roleDuties = "Gestión del libro contable, cobro y validación de mensualidades, fondos SOS y potes para rodadas."
    ),
    DIRECTIVA(
        "Directiva Nacional",
        0xFFFFD700,
        canManageApp = true,
        canCreateRides = true,
        canPostAnnouncements = true,
        canManageMembers = true,
        canManageFinances = true,
        canCoordinateEmergency = true,
        defaultTitle = "Miembro de Directiva",
        roleDuties = "Voto en consejo directivo, apoyo en administración y gobernanza del club."
    ),
    CAPITAN_RUTA(
        "Capitán de Ruta (Puntero)",
        0xFFE52323,
        canManageApp = true,
        canCreateRides = true,
        canPostAnnouncements = true,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = true,
        defaultTitle = "Capitán de Ruta Principal",
        roleDuties = "Trazado de rutas, determinación del ritmo de marcha y paradas de gasolina, comando en carretera."
    ),
    SEGURIDAD_VIAL(
        "Oficial de Seguridad Vial / Bloqueador",
        0xFF00B0FF,
        canManageApp = true,
        canCreateRides = true,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = true,
        defaultTitle = "Oficial de Seguridad Vial",
        roleDuties = "Bloqueo de intersecciones, protección de caravana, señalización de peligros y barredor de cola."
    ),
    MECANICO_OFICIAL(
        "Mecánico Oficial / Asistencia Técnica",
        0xFFFF9100,
        canManageApp = false,
        canCreateRides = false,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = true,
        defaultTitle = "Mecánico Oficial TX",
        roleDuties = "Diagnóstico técnico en ruta, auxilio ante varadas mecánicas y custodia del banco de herramientas/repuestos."
    ),
    MEDICO_CLUB(
        "Médico / Primeros Auxilios",
        0xFF00E676,
        canManageApp = false,
        canCreateRides = false,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = true,
        defaultTitle = "Médico Oficial del Club",
        roleDuties = "Atención prehospitalaria, triage de caídas/accidentes, resguardo del botiquín y enlace con ambulancias/clínicas."
    ),
    PARAMEDICO_MOTERO(
        "Paramédico de Ruta",
        0xFF00E676,
        canManageApp = false,
        canCreateRides = false,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = true,
        defaultTitle = "Paramédico de Ruta",
        roleDuties = "Soporte vital básico, inmovilización y primeros auxilios rápidos en carretera."
    ),
    MIEMBRO_ACTIVO(
        "Miembro Activo",
        0xFFCBD5E1,
        canManageApp = false,
        canCreateRides = false,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = false,
        defaultTitle = "Piloto Miembro Activo",
        roleDuties = "Participación en rodadas, chat interno, comentarios en avisos oficiales y cumplimiento del reglamento."
    ),
    ASPIRANTE(
        "Aspirante",
        0xFF90A4AE,
        canManageApp = false,
        canCreateRides = false,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = false,
        defaultTitle = "Piloto Aspirante",
        roleDuties = "Período de prueba y observación, acumulación de kilómetros y evaluación de mérito para pase a Miembro Activo."
    ),
    COPILOTO(
        "Copiloto",
        0xFF78909C,
        canManageApp = false,
        canCreateRides = false,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = false,
        defaultTitle = "Copiloto Oficial",
        roleDuties = "Acompañante frecuente, participación en logística de grupo y apoyo en ruta al piloto principal."
    ),
    INVITADO(
        "Invitado Especial (Temporal)",
        0xFFFFB300,
        canManageApp = false,
        canCreateRides = false,
        canPostAnnouncements = false,
        canManageMembers = false,
        canManageFinances = false,
        canCoordinateEmergency = false,
        defaultTitle = "Invitado Especial",
        roleDuties = "Acceso temporal concedido por Directiva. Sujeto a revocación o caducidad."
    )
}

enum class ConvoyRoleType(val label: String, val shortDesc: String, val colorHex: Long) {
    PUNTERO("Capitán de Ruta (Puntero)", "Guía y marca el ritmo y trazado", 0xFFE52323),
    SEGUNDO_GUIA("Sub-Capitán / Relevo", "Apoyo directo al puntero y enlaces", 0xFFFF9800),
    OFICIAL_SEGURIDAD("Oficial de Seguridad (Bloqueador)", "Bloqueo de cruces y seguridad vial", 0xFF00B0FF),
    MEDICO("Médico / Auxilio Primario", "Atención médica, botiquín y triage", 0xFF00E676),
    MECANICO("Mecánico de Ruta (Asistencia Técnica)", "Auxilio mecánico, herramientas y repuestos", 0xFFFF9100),
    BARREDOR_COLA("Barredor / Jefe de Cola (Escoba)", "Cierra caravana, asiste averías", 0xFFFFD700),
    PILOTO_CENTRAL("Piloto Caravana (Zigzag)", "Piloto en formación escalonada", 0xFFCBD5E1),
    COPILOTO("Copiloto / Acompañante", "Pasajero registrado", 0xFF90A4AE)
}

enum class NoticeCategory(val displayName: String, val iconName: String) {
    AVISO_OFICIAL("Aviso Oficial", "announcement"),
    RETO_MOTERO("Reto Motero TX", "trophy"),
    COMUNICADO("Comunicado", "campaign"),
    EMERGENCIA("Emergencia SOS", "warning"),
    NOTICIA_RUTA("Reporte de Carretera", "map"),
    CAPACITACION("Taller / Mecánica", "build"),
    OBRA_BENEFICA("Obra Benéfica", "volunteer_activism"),
    MANTENIMIENTO_PREVENTIVO("Mantenimiento Preventivo para Rodada", "handyman"),
    LAVADO_FAMILIAR("Lavado de Moto en Familia", "local_car_wash")
}

enum class NoticePriority {
    NORMAL,
    IMPORTANTE,
    URGENTE
}

enum class RideStatus(val label: String) {
    PROGRAMADA("Programada"),
    EN_CURSO("En Ruta"),
    FINALIZADA("Completada"),
    CANCELADA("Cancelada")
}

enum class PaymentCategory(val label: String) {
    MEMBRESIA_MENSUAL("Aporte Voluntario"),
    POTE_EVENTO("Pote para Rodada/Evento"),
    DONACION_BENEFICA("Donación / Ayuda Hermano Motero"),
    FONDO_EMERGENCIA("Fondo de Accidentados y SOS"),
    GASTO_LOGISTICA("Gasto de Logística / Club"),
    COMPRA_REPUESTOS("Compra de Repuestos e Insumos")
}

enum class TransactionType {
    INGRESO,
    GASTO
}

enum class PaymentMethod(val label: String) {
    PAGO_MOVIL("Pago Móvil (VES)"),
    BINANCE_PAY("Binance Pay (USDT)"),
    ZELLE("Zelle (USD)"),
    EFECTIVO_DIVISAS("Efectivo Divisas ($)"),
    EFECTIVO_BS("Efectivo Bolívares (Bs)"),
    TRANSFERENCIA_BANCARIA("Transferencia Bancaria")
}

enum class PaymentStatus(val label: String) {
    VERIFICADO("Aprobado / Verificado"),
    PENDIENTE("En Revisión"),
    RECHAZADO("Rechazado")
}

enum class ItemCategory(val label: String) {
    HERRAMIENTAS_RUTA("Herramientas de Ruta"),
    EQUIPO_EVENTO("Equipo de Campamento y Evento"),
    PRIMEROS_AUXILIOS("Botiquín y Primeros Auxilios"),
    COMUNICACIONES("Radios y Telecomunicaciones"),
    REPUESTOS_COMUNITARIOS("Repuestos Comunitarios TX")
}

enum class ItemCondition(val label: String) {
    EXCELENTE("Excelente Estado"),
    OPERATIVO("Operativo / Normal"),
    MANTENIMIENTO("En Mantenimiento"),
    DANADO("Requiere Reparación")
}

enum class LoanStatus(val label: String) {
    ACTIVO("En Préstamo"),
    DEVUELTO("Devuelto"),
    RETRASADO("Atrasado"),
    DANADO("Reportó Daño")
}

enum class EmergencyType(
    val label: String,
    val levelNumber: Int = 1,
    val levelTag: String = "NIVEL 1",
    val levelName: String = "Leve",
    val severityColorHex: Long = 0xFFFFD600,
    val iconType: String = "gas",
    val recommendedSpecialist: String = "Piloto Enlace / Apoyo",
    val actionProtocol: String = "Asistencia con combustible y resguardo en hombrillo."
) {
    ACCIDENTADO_GASOLINA(
        label = "Falta de Gasolina",
        levelNumber = 1,
        levelTag = "NIVEL 1",
        levelName = "Leve (Combustible)",
        severityColorHex = 0xFFFFD600,
        iconType = "gas",
        recommendedSpecialist = "Piloto Enlace / Apoyo de Ruta",
        actionProtocol = "1. Ubicarse en hombrillo seguro con luces intermitentes. 2. Enviar punto GPS y octanaje requerido (91/95). 3. Piloto más cercano asiste con manguera/bidón."
    ),
    ACCIDENTADO_MECANICO(
        label = "Falla Mecánica / Avería",
        levelNumber = 2,
        levelTag = "NIVEL 2",
        levelName = "Moderado (Mecánico)",
        severityColorHex = 0xFFFF9100,
        iconType = "mechanic",
        recommendedSpecialist = "Mecánico Oficial TX",
        actionProtocol = "1. Despachar al Mecánico Oficial con maleta de herramientas. 2. Solicitar repuesto de inventario (cámara, guaya, eslabón de cadena, bujía). 3. Evaluar si rueda o requiere remolque."
    ),
    CAIDA(
        label = "Caída en Ruta / Deslizamiento",
        levelNumber = 3,
        levelTag = "NIVEL 3",
        levelName = "Urgente (Caída)",
        severityColorHex = 0xFFFF5722,
        iconType = "fall",
        recommendedSpecialist = "Médico / Paramédico + Seguridad",
        actionProtocol = "1. No retirar el casco bruscamente. 2. Oficiales de Seguridad aseguran el perímetro a 50m. 3. Médico/Paramédico evalúa signos, contusiones y aplica botiquín. 4. Mecánico evalúa operatividad de la moto."
    ),
    CHOQUE(
        label = "Choque / Colisión Grave",
        levelNumber = 4,
        levelTag = "NIVEL 4",
        levelName = "Crítico (Alerta Máxima)",
        severityColorHex = 0xFFFF1744,
        iconType = "crash",
        recommendedSpecialist = "VEN 911 + Médico del Club",
        actionProtocol = "1. ACTIVAR VEN 911 Y TRÁNSITO DE INMEDIATO. 2. Bloqueo total preventivo de vía a 100m. 3. Médico del Club inicia triage, inmovilización y control de hemorragia. 4. Notificar a contacto SOS y banco de sangre."
    ),
    // Aliases para compatibilidad con registros existentes
    ACCIDENTE_VIAL(
        label = "Accidente Vial General",
        levelNumber = 4,
        levelTag = "NIVEL 4",
        levelName = "Crítico (Choque / Vial)",
        severityColorHex = 0xFFFF1744,
        iconType = "crash",
        recommendedSpecialist = "VEN 911 + Médico del Club",
        actionProtocol = "Activación inmediata de directiva y servicios de emergencia médica."
    ),
    FALLA_MECANICA_GRAVE(
        label = "Falla Mecánica en Vía",
        levelNumber = 2,
        levelTag = "NIVEL 2",
        levelName = "Moderado (Mecánico)",
        severityColorHex = 0xFFFF9100,
        iconType = "mechanic",
        recommendedSpecialist = "Mecánico Oficial TX",
        actionProtocol = "Asistencia técnica especializada y herramientas de auxilio en carretera."
    ),
    EMERGENCIA_MEDICA(
        label = "Emergencia Médica / Triage",
        levelNumber = 4,
        levelTag = "NIVEL 4",
        levelName = "Crítico (Salud)",
        severityColorHex = 0xFFD500F9,
        iconType = "medical",
        recommendedSpecialist = "Médico del Club / Ambulancia",
        actionProtocol = "Atención médica urgente y coordinación de donantes de sangre."
    ),
    APOYO_SEGURIDAD(
        label = "Situación de Seguridad / Vía",
        levelNumber = 3,
        levelTag = "NIVEL 3",
        levelName = "Urgente (Seguridad)",
        severityColorHex = 0xFFFF5252,
        iconType = "security",
        recommendedSpecialist = "Oficiales de Seguridad Vial",
        actionProtocol = "Agrupación preventiva de caravana y reporte a capitanes de ruta."
    )
}

enum class EmergencyStatus(val label: String) {
    ACTIVA("Activa - Auxilio Requerido"),
    EN_CAMINO("En Camino / Brigada Despachada"),
    ATENDIDA("Atendida en Sitio"),
    RESUELTA("Solucionada / Miembro a Salvo")
}

// ---------------- ROOM ENTITIES ---------------- //

@Entity(tableName = "member_profiles")
data class MemberProfile(
    @PrimaryKey var id: Long = 0,
    var fullName: String = "",
    var nickname: String = "",
    var memberNumber: String = "",
    var cedulaDni: String = "",
    var phone: String = "",
    var role: MemberRole = MemberRole.ASPIRANTE,
    var chapterState: String = "",
    var birthDate: String = "",
    // Ficha Técnica de la Moto
    var bikeBrand: String = "Keeway",
    var bikeModel: String = "TX 200 SM",
    var bikeColor: String = "Negro / Naranja",
    var bikeDisplacementCc: String = "200 cc",
    var bikeTankCapacityLiters: String = "11.5 L",
    var bikeYear: String = "2023",
    var bikePlate: String = "",
    // Ficha Médica y SOS
    var bloodType: String = "O+",
    var medicalNotes: String = "Sin alergias reportadas",
    var emergencyContactName: String = "",
    var emergencyContactPhone: String = "",
    var emergencyContactRelation: String = "Familiar",
    // Estatus y Membresía
    var isDirectiva: Boolean = false,
    var solvencyStatus: Boolean = true,
    var joinYear: String = "2023",
    var avatarInitials: String = "TX",
    // Gestión de Suspensión y Sanciones Directivas
    var isSuspended: Boolean = false,
    var suspensionReason: String = "",
    var suspensionDurationDays: Int = 0,
    var suspendedBy: String = "",
    var suspensionStartTimestamp: Long = 0,
    var suspensionEndTimestamp: Long = 0,
    // Silencio en Chat por Directiva
    var isChatMuted: Boolean = false,
    var muteReason: String = "",
    // Estado de Conexión en Vivo (LED Verde/Rojo)
    var isOnline: Boolean = true,
    var lastActiveTimestamp: Long = System.currentTimeMillis(),
    // Guantera Digital (Documentos)
    var licenseImageUri: String? = null,
    var medicalCertImageUri: String? = null,
    var bikeRegImageUri: String? = null,
    var insuranceImageUri: String? = null,
    // Registro de Copiloto Oficial
    var copilotName: String? = null,
    var copilotRelation: String? = null,
    // Rastreador de Prospectos y Asistencia
    var attendanceCount: Int = 0,
    var longRidesCount: Int = 0,
    var bigEventsCount: Int = 0,
    var prospectStartDate: Long? = null,
    // Perfil y Moto (Imágenes)
    var profilePhotoUri: String? = null,
    var bikePhotoUri: String? = null,
    // Métricas de Rendimiento, Velocímetro, Odómetro y Ranking Gamificado
    var totalKmRidden: Double = 0.0,
    var topSpeedRecordKmh: Float = 0f,
    var sosAssistanceCount: Int = 0,
    var challengesCompletedCount: Int = 0,
    var meritPoints: Int = 0,
    var rankingTitle: String = "Piloto TX",
    var positiveRatingsCount: Int = 0,
    var negativeRatingsCount: Int = 0,
    var reputationPoints: Int = 0,
    var ratedByMemberIdsJson: String = "",
    // Google / Firebase Sync
    var firebaseUid: String? = null,
    var email: String? = null
) {
    @get:Ignore
    val suspensionStartDate: String
        get() = if (suspensionStartTimestamp > 0) {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(suspensionStartTimestamp))
        } else ""

    @get:Ignore
    val suspensionEndDate: String
        get() = if (suspensionEndTimestamp > 0) {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(suspensionEndTimestamp))
        } else ""

    @get:Ignore
    val isSuspensionActive: Boolean
        get() = isSuspended && suspensionEndTimestamp > 0 && suspensionEndTimestamp > System.currentTimeMillis()

    @get:Ignore
    val isProfileComplete: Boolean
        get() = fullName.isNotBlank() && fullName != "Piloto de Pruebas" &&
                cedulaDni.isNotBlank() && phone.isNotBlank() &&
                bikeBrand.isNotBlank() && bikeModel.isNotBlank() && bikePlate.isNotBlank() &&
                emergencyContactName.isNotBlank() && emergencyContactPhone.isNotBlank() &&
                firebaseUid != null
}

@Entity(tableName = "publications")
data class Publication(
    @PrimaryKey var id: Long = 0,
    var title: String = "",
    var content: String = "",
    var category: NoticeCategory = NoticeCategory.AVISO_OFICIAL,
    var priority: NoticePriority = NoticePriority.NORMAL,
    var authorName: String = "",
    var authorRole: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var likesCount: Int = 0,
    var likedByMemberIds: String = "",
    var isPinned: Boolean = false,
    var targetChallengeDistanceKm: Int = 0,
    var challengeBadgeText: String? = null,
    var telegramPostUrl: String? = null,
    var imageUrl: String? = null,
    var allowComments: Boolean = true,
    var sharesCount: Int = 0,
    var savesCount: Int = 0,
    var locationCoordinates: String? = null,
    var locationName: String? = null,
    var eventDate: String? = null,
    var eventTime: String? = null,
    var isEventFinished: Boolean = false,
    var linkedCalendarEventId: Long? = null
)

@Entity(tableName = "ride_events")
data class RideEvent(
    @PrimaryKey var id: Long = 0,
    var title: String = "",
    var description: String = "",
    var originCity: String = "",
    var destinationCity: String = "",
    var departureDate: String = "",
    var meetingTime: String = "",
    var departureTimestamp: Long = 0,
    var distanceKm: Int = 0,
    var terrainType: String = "",
    var convoyLeader: String = "Capitán de Ruta (Puntero)",
    var secondLeader: String = "Segundo Guía / Sub-Capitán",
    var roadSafetyOfficer: String = "Oficial de Seguridad / Bloqueador",
    var tailRider: String = "Barredor / Jefe de Cola (Escoba)",
    var medicOfficer: String = "Médico / Paramédico de Ruta",
    var mechanicOfficer: String = "Mecánico Oficial de Ruta",
    var gasStops: String = "",
    var requiredGear: String = "",
    var status: RideStatus = RideStatus.PROGRAMADA,
    var costUsdCents: Long = 0,
    var costVesCents: Long = 0,
    var registeredCount: Int = 0,
    var maxParticipants: Int = 50,
    var whatsappGroupUrl: String? = null
) {
    val costUsd: Double get() = costUsdCents / 100.0
    val costVes: Double get() = costVesCents / 100.0
    val costUsdFormatted: String get() = String.format("%.2f", costUsd)
    val costVesFormatted: String get() = String.format("%.2f", costVes)
}

@Entity(tableName = "ride_registrations")
data class RideRegistration(
    @PrimaryKey var id: Long = 0,
    var rideId: Long = 0L,
    var memberId: Long = 0L,
    var memberName: String = "",
    var memberAlias: String = "",
    var bikePlate: String = "",
    var hasPillion: Boolean = false,
    var pillionName: String = "",
    var convoyRole: String = "Piloto Caravana",
    var paymentConfirmed: Boolean = true,
    var checkedIn: Boolean = false,
    var registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_transactions")
data class FinancialTransaction(
    @PrimaryKey var id: Long = 0,
    var concept: String = "",
    var description: String = "",
    var category: PaymentCategory = PaymentCategory.MEMBRESIA_MENSUAL,
    var type: TransactionType = TransactionType.INGRESO,
    var amountUsdCents: Long = 0L,
    var amountVesCents: Long = 0L,
    var paymentMethod: PaymentMethod = PaymentMethod.PAGO_MOVIL,
    var referenceCode: String = "",
    var memberName: String = "",
    var memberNumber: String = "",
    var status: PaymentStatus = PaymentStatus.VERIFICADO,
    var timestamp: Long = System.currentTimeMillis(),
    var approvedBy: String = "Directiva Nacional"
) {
    val amountUsd: Double get() = amountUsdCents / 100.0
    val amountVes: Double get() = amountVesCents / 100.0
    val amountUsdFormatted: String get() = String.format("%.2f", amountUsd)
    val amountVesFormatted: String get() = String.format("%.2f", amountVes)
}

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey var id: Long = 0,
    var code: String = "",
    var name: String = "",
    var category: ItemCategory = ItemCategory.HERRAMIENTAS_RUTA,
    var description: String = "",
    var totalStock: Int = 0,
    var availableStock: Int = 0,
    var condition: ItemCondition = ItemCondition.EXCELENTE,
    var location: String = "",
    var lastMaintenanceTimestamp: Long = 0,
    var custodianName: String = ""

) {
    val lastMaintenanceDate: String

        get() = if (lastMaintenanceTimestamp > 0) {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(lastMaintenanceTimestamp))
        } else "Sin mantenimiento"
}

@Entity(tableName = "equipment_loans")
data class EquipmentLoan(
    @PrimaryKey var id: Long = 0,
    var itemId: Long = 0L,
    var itemName: String = "",
    var itemCode: String = "",
    var borrowerName: String = "",
    var borrowerPhone: String = "",
    var borrowerMemberNumber: String = "",
    var loanTimestamp: Long = System.currentTimeMillis(),
    var expectedReturnTimestamp: Long = 0,
    var actualReturnTimestamp: Long? = null,
    var status: LoanStatus = LoanStatus.ACTIVO,
    var purpose: String = "",
    var authorizedBy: String = ""

) {
    val loanDate: String

        get() = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(loanTimestamp))
    val expectedReturnDate: String

        get() = if (expectedReturnTimestamp > 0) {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(expectedReturnTimestamp))
        } else "Pendiente"
    val actualReturnDate: String?

        get() = actualReturnTimestamp?.let {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
        }
}

@Entity(tableName = "emergency_alerts")
data class EmergencyAlert(
    @PrimaryKey var id: Long = 0,
    var reporterName: String = "",
    var reporterPhone: String = "",
    var memberNumber: String = "",
    var emergencyType: EmergencyType = EmergencyType.ACCIDENTADO_GASOLINA,
    var locationDescription: String = "",
    var coordinateLat: Double = 10.4806,
    var coordinateLng: Double = -66.9036,
    var bikeDetails: String = "",
    var bloodTypeNeeded: String? = null,
    var details: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var status: EmergencyStatus = EmergencyStatus.ACTIVA,
    var respondersNotes: String = ""
)

@Entity(tableName = "role_configs")
data class RoleConfig(
    @PrimaryKey var roleKey: String = "",
    var customTitle: String = "",
    var roleDuties: String = "",
    var badgeIconName: String = "shield",
    var canManageApp: Boolean = true,
    var lastUpdatedBy: String = "Presidente Nacional",
    var lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notice_comments")
data class NoticeComment(
    @PrimaryKey var id: Long = 0,
    var publicationId: Long = 0L,
    var memberId: Long = 0L,
    var authorName: String = "",
    var authorNickname: String = "",
    var authorMemberNumber: String = "",
    var authorRole: MemberRole = MemberRole.ASPIRANTE,
    var authorInitials: String = "",
    var content: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "club_chat_messages")
data class ChatMessage(
    @PrimaryKey var id: Long = 0,
    var channelId: String = "GENERAL", // "GENERAL", "RODADAS", "MECANICA_AUXILIO", "DIRECTIVA"
    var senderMemberId: Long = 0L,
    var senderName: String = "",
    var senderNickname: String = "",
    var senderMemberNumber: String = "",
    var senderRole: MemberRole = MemberRole.ASPIRANTE,
    var senderCustomRoleTitle: String? = null,
    var senderInitials: String = "",
    var senderPhotoUrl: String? = null,
    var messageText: String = "",
    var isRadioCallout: Boolean = false,
    var timestamp: Long = System.currentTimeMillis(),
    // Sticker support
    var stickerFileName: String? = null,
    var stickerFilePath: String? = null,
    var messageType: MessageType = MessageType.TEXT,
    
    // Read Receipts
    var readBy: List<Long> = emptyList(),

    // Reply / Quote support (estilo WhatsApp)
    var replyToMessageId: Long? = null,
    var replyToSenderName: String? = null,
    var replyToText: String? = null,

    // Eliminación selectiva ("Eliminar para mí")
    var deletedForMemberIds: String = "",

    // Reacciones con Emojis (formato "👍:1,2|❤️:3")
    var reactions: String = "",

    // Soporte de Notas de Voz / Audio
    var audioDurationSeconds: Int = 0,
    var audioUrl: String? = null,

    // Enfoque Offline-First / Cola Local
    var syncStatus: String = "SENT", // "PENDING", "SENDING", "SENT", "DELIVERED", "READ"
    var localMediaPath: String? = null
) {
    val isPending: Boolean
        get() = syncStatus == "PENDING" || syncStatus == "SENDING"

    val isSticker: Boolean
        get() = messageType == MessageType.STICKER && (stickerFileName != null || stickerFilePath != null)

    fun getReactionsMap(): Map<String, List<Long>> {
        if (reactions.isBlank()) return emptyMap()
        val result = mutableMapOf<String, MutableList<Long>>()
        reactions.split("|").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val emoji = parts[0]
                val ids = parts[1].split(",").mapNotNull { it.trim().toLongOrNull() }
                if (ids.isNotEmpty()) {
                    result[emoji] = ids.toMutableList()
                }
            }
        }
        return result
    }

    fun toggleReaction(emoji: String, memberId: Long): String {
        val map = getReactionsMap().mapValues { it.value.toMutableList() }.toMutableMap()
        val list = map.getOrPut(emoji) { mutableListOf() }
        if (list.contains(memberId)) {
            list.remove(memberId)
            if (list.isEmpty()) map.remove(emoji)
        } else {
            // Remover reacciones previas del mismo miembro (1 reacción por miembro)
            map.forEach { (_, ids) -> ids.remove(memberId) }
            map.entries.removeAll { it.value.isEmpty() }
            map.getOrPut(emoji) { mutableListOf() }.add(memberId)
        }
        return map.entries.joinToString("|") { "${it.key}:${it.value.joinToString(",")}" }
    }
}

enum class MessageType {
    TEXT,
    STICKER,
    IMAGE,
    AUDIO,
    LOCATION
}

@Entity(tableName = "invitation_codes")
data class InvitationCode(
    @PrimaryKey var id: Long = 0,
    var code: String = "",
    var isMaster: Boolean = false, // true for linda19554402 and 19554402sb (immutable/undeletable)
    var isSpecialGuest: Boolean = false, // true for alphanumeric guest codes
    var durationHours: Int = 24,
    var createdBy: String = "Líder / Presidente",
    var createdAt: Long = System.currentTimeMillis(),
    var expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L, // 24 hours validity
    var isUsed: Boolean = false,
    var usedByPhone: String? = null,
    var usedByName: String? = null,
    var targetRole: MemberRole = MemberRole.MIEMBRO_ACTIVO,
    var note: String = ""
)

@Entity(tableName = "access_requests")
data class AccessRequest(
    @PrimaryKey var id: Long = 0,
    var fullName: String = "",
    var phone: String = "",
    var cedulaDni: String = "",
    var bikeBrand: String = "Keeway",
    var bikeModel: String = "TX 200 SM",
    var bikeColor: String = "Negro / Naranja",
    var bikePlate: String = "",
    var chapterState: String = "",
    var birthDate: String = "",
    var requestedRole: String = "Piloto",
    var requestType: String = "ACCESO_APP", // "ACCESO_APP" or "SOLICITUD_DIRECTIVA"
    var reasonMessage: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var status: String = "PENDIENTE", // "PENDIENTE", "APROBADO", "RECHAZADO"
    var generatedCode: String? = null,
    var reviewedBy: String? = null
)

@Entity(tableName = "event_attendance")
data class EventAttendance(
    @PrimaryKey var id: Long = 0,
    var eventId: Long = 0L, // Can link to a ride_events ID or be a standalone event
    var memberId: Long = 0L,
    var eventName: String = "", // e.g., "Jueves Motero - 12 Oct"
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "disciplinary_records")
data class DisciplinaryRecord(
    @PrimaryKey var id: Long = 0,
    var memberId: Long = 0L,
    var memberName: String = "",
    var reason: String = "",
    var penaltyType: String = "", // e.g., "Llamado de atención", "Suspensión 4 semanas", "Expulsión"
    var issuedBy: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "private_groups")
data class PrivateGroup(
    @PrimaryKey var id: String = "", // Formato: GRP_timestamp_creatorId
    var name: String = "",
    var description: String = "",
    var creatorMemberId: Long = 0L,
    var creatorName: String = "",
    var creatorNickname: String = "",
    var memberIds: List<Long> = emptyList(),
    var adminIds: List<Long> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(),
    var isBlockedByDirectiva: Boolean = false,
    var blockedReason: String = "",
    var blockedBy: String = "",
    var lastMessageText: String = "",
    var lastMessageTimestamp: Long = System.currentTimeMillis(),
    var isDeleted: Boolean = false,
    var groupIcon: String? = null
)

@Entity(tableName = "marketplace_items")
data class MarketplaceItem(
    @PrimaryKey var id: Long = 0L,
    var sellerMemberId: Long = 0L,
    var sellerName: String = "",
    var sellerNickname: String = "",
    var sellerPhone: String = "",
    var sellerLocation: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "Repuestos TX", // Repuestos TX, Indumentaria, Motos, Accesorios, Varios
    var priceUsd: Double = 0.0,
    var condition: String = "Buen Estado", // Nuevo, Como Nuevo, Buen Estado, Para Reparar
    var imageUrl: String? = null,
    var status: String = "DISPONIBLE", // DISPONIBLE, RESERVADO, VENDIDO
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "maintenance_logs")
data class MaintenanceLog(
    @PrimaryKey var id: Long = 0L,
    var memberId: Long = 0L,
    var odometerKm: Int = 0,
    var serviceType: String = "Cambio de Aceite", // Cambio de Aceite, Kit de Arrastre, Pastillas de Freno, Guayas, Bujía / Carburador, Cauchos, Batería, General
    var brandOrDetails: String = "",
    var costUsd: Double = 0.0,
    var workshopName: String = "",
    var serviceDate: String = "",
    var nextServiceKm: Int = 0,
    var notes: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workshops_directory")
data class WorkshopDirectoryItem(
    @PrimaryKey var id: Long = 0L,
    var name: String = "",
    var type: String = "Taller Mecánico", // Taller Mecánico, Venta de Repuestos TX, Cauchera, Electricidad, Tornería / Soldadura
    var state: String = "Aragua",
    var city: String = "",
    var address: String = "",
    var phone: String = "",
    var whatsapp: String = "",
    var rating: Double = 5.0,
    var recommendedBy: String = "",
    var notes: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "passport_destinations")
data class PassportDestination(
    @PrimaryKey var id: Long = 0L,
    var title: String = "",
    var state: String = "",
    var description: String = "",
    var badgeIcon: String = "🏆",
    var requiredKm: Int = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var isOfficialRoute: Boolean = true,
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "passport_stamps")
data class PassportStamp(
    @PrimaryKey var id: Long = 0L,
    var memberId: Long = 0L,
    var destinationId: Long = 0L,
    var destinationTitle: String = "",
    var state: String = "",
    var stampedDate: String = "",
    var proofImageUrl: String? = null,
    var isVerified: Boolean = true,
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "biker_challenges")
data class BikerChallenge(
    @PrimaryKey var id: Long = 0L,
    var title: String = "", // ej. "3er. RETO PILOTO EXPERTO", "2do. RETO PILOTO ALAS DE PLATA"
    var description: String = "",
    var targetKm: Double = 0.0, // ej. 7747.3 o 2430.4
    var cruisingSpeed: String = "80-100 km/h",
    var badgeName: String = "Distintivo Oficial TX",
    var startDate: String = "",
    var endDate: String = "",
    var checkpoints: String = "", // "Cabo San Román, Cascada del Vino, Collado del Cóndor, Cuyagua"
    var isOfficial: Boolean = true, // true = Directiva Nacional, false = Reto Personal
    var creatorMemberId: Long = 0L,
    var creatorName: String = "Directiva Nacional TX",
    var imageUrl: String? = null, // Flyer o foto de portada
    var timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_challenge_progress")
data class UserChallengeProgress(
    @PrimaryKey var id: Long = 0L,
    var challengeId: Long = 0L,
    var challengeTitle: String = "",
    var memberId: Long = 0L,
    var pilotName: String = "",
    var pilotPhone: String = "",
    var pilotEmergencyContact: String = "",
    var copilotName: String? = null,
    var copilotPhone: String? = null,
    var bikeModel: String = "TX 200",
    var bikePlate: String = "",
    var departureDate: String = "",
    var returnDate: String = "",
    var originLocation: String = "", // ej. "San Cristóbal (Campamento Base)"
    var destinationLocation: String = "", // ej. "Barquisimeto"
    var initialKm: Double = 0.0,
    var finalKm: Double = 0.0,
    var currentRouteNumber: Int = 1,
    var routeDescription: String = "", // ej. "Ruta #12: San Cristóbal - Barquisimeto"
    var completedCheckpoints: String = "", // "Cabo San Román:true,Cascada del Vino:false"
    var accumulatedKm: Double = 0.0,
    var sitePhotoUrl: String? = null, // Mínimo 1 foto del sitio visitado optimizada
    var notes: String = "",
    var status: String = "EN_PROGRESO", // "EN_PROGRESO", "COMPLETADO"
    var updatedAt: Long = System.currentTimeMillis()
) {
    val routeKm: Double get() = if (finalKm > initialKm) finalKm - initialKm else 0.0
}

@Entity(tableName = "biker_calendar_events")
data class BikerCalendarEvent(
    @PrimaryKey var id: Long = 0L,
    var title: String = "",
    var description: String = "",
    var category: String = "Ruta Oficial", // "Ruta Oficial", "Evento Social", "Mantenimiento / Garaje", "Vencimiento Trámites", "Reunión Directiva", "Personal"
    var visibility: String = "PUBLICO_CLUB", // "PUBLICO_CLUB", "PRIVADO_DIRECTIVA", "PERSONAL_PILOTO"
    var eventDate: String = "", // Formato "DD/MM/YYYY"
    var eventTime: String = "", // "07:30 AM" (Hora de concentración)
    var departureTime: String = "", // "08:00 AM" (Ruedas en el Asfalto)
    var originAddress: String = "", // Punto de encuentro / Concentración
    var destinationAddress: String = "", // Destino final
    var originLatitude: Double = 0.0,
    var originLongitude: Double = 0.0,
    var destinationLatitude: Double = 0.0,
    var destinationLongitude: Double = 0.0,
    var terrainType: String = "Asfalto", // "Asfalto", "Tierra", "Mixto", "Montaña", "Costa"
    var difficultyLevel: String = "Media", // "Fácil", "Media", "Avanzada", "Extrema"
    var weatherForecast: String = "", // Pronóstico del clima estimado ej. "Soleado 28°C / Prob. Lluvia 10%"
    var roadCaptain: String = "", // Capitán de Ruta (Puntero)
    var tailRider: String = "", // Barredora (Cierre)
    var rsvpPilotsCount: Int = 0,
    var rsvpPilotsList: String = "", // IDs separados por coma de miembros confirmados
    var rsvpPillionsCount: Int = 0,
    var remindDaysBefore: Int = 1, // 1 día antes, 2 días antes, etc.
    var remindedMemberIds: String = "", // IDs separados por coma de miembros con recordatorio activo
    var isOfficialClubEvent: Boolean = true,
    var creatorMemberId: Long = 0L,
    var creatorName: String = "",
    var flyerUrl: String? = null,
    var linkedPublicationId: Long? = null,
    var isEventFinished: Boolean = false,
    var timestamp: Long = System.currentTimeMillis()
)
