package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.BikerCalendarEvent
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.example.ui.theme.*
import com.example.mapa.PuenteMapa
import com.example.mapa.GestorPortapapeles
import com.example.mapa.AnalizadorCoordenadas
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════
// MODELOS Y AYUDANTES DE CALENDARIO
// ═══════════════════════════════════════════════════════════════════════

enum class CalendarViewMode(val label: String, val icon: ImageVector) {
    MES("Mes", Icons.Default.CalendarMonth),
    DIA("Día (Horas)", Icons.Default.Schedule),
    ANIO("Año", Icons.Default.DateRange)
}

data class FeriadoInfo(
    val nombre: String,
    val esOficial: Boolean = true,
    val esMotero: Boolean = false,
    val emoji: String = "🎉"
)

object FeriadosHelper {
    /**
     * Retorna feriados oficiales de Venezuela y conmemoraciones moteras.
     */
    fun obtenerFeriado(day: Int, month: Int, year: Int): FeriadoInfo? {
        return when (month) {
            1 -> when (day) {
                1 -> FeriadoInfo("Año Nuevo", esOficial = true, emoji = "🎆")
                6 -> FeriadoInfo("Día de Reyes", esOficial = false, emoji = "👑")
                15 -> FeriadoInfo("Día del Maestro", esOficial = false, emoji = "📚")
                else -> null
            }
            2 -> when (day) {
                12 -> FeriadoInfo("Día de la Juventud (Batalla de La Victoria)", esOficial = false, emoji = "⚔️")
                else -> null
            }
            3 -> when (day) {
                8 -> FeriadoInfo("Día Internacional de la Mujer", esOficial = false, emoji = "🌸")
                19 -> FeriadoInfo("Día de San José", esOficial = false, emoji = "⛪")
                else -> null
            }
            4 -> when (day) {
                19 -> FeriadoInfo("Proclamación de la Independencia", esOficial = true, emoji = "🇻🇪")
                else -> null
            }
            5 -> when (day) {
                1 -> FeriadoInfo("Día Internacional del Trabajador", esOficial = true, emoji = "🛠️")
                2 -> FeriadoInfo("Día Int. de las Mujeres Motociclistas", esOficial = false, esMotero = true, emoji = "🏍️")
                3 -> FeriadoInfo("Día de la Cruz de Mayo", esOficial = false, emoji = "✝️")
                else -> null
            }
            6 -> when (day) {
                24 -> FeriadoInfo("Batalla de Carabobo", esOficial = true, emoji = "⚔️")
                else -> null
            }
            7 -> when (day) {
                5 -> FeriadoInfo("Día de la Independencia", esOficial = true, emoji = "🇻🇪")
                18 -> FeriadoInfo("Día Nacional del Motociclista", esOficial = false, esMotero = true, emoji = "🏍️")
                24 -> FeriadoInfo("Natalicio del Libertador Simón Bolívar", esOficial = true, emoji = "⭐")
                else -> null
            }
            8 -> when (day) {
                3 -> FeriadoInfo("Día de la Bandera Nacional", esOficial = false, emoji = "🇻🇪")
                else -> null
            }
            10 -> when (day) {
                12 -> FeriadoInfo("Día de la Resistencia Indígena", esOficial = true, emoji = "🏹")
                else -> null
            }
            11 -> when (day) {
                1 -> FeriadoInfo("Día de Todos los Santos", esOficial = false, emoji = "🕯️")
                18 -> FeriadoInfo("Día de la Virgen de Chiquinquirá (Chinita)", esOficial = false, emoji = "👑")
                else -> null
            }
            12 -> when (day) {
                24 -> FeriadoInfo("Víspera de Navidad", esOficial = true, emoji = "🎄")
                25 -> FeriadoInfo("Navidad", esOficial = true, emoji = "🎁")
                31 -> FeriadoInfo("Fin de Año", esOficial = true, emoji = "🥂")
                else -> null
            }
            else -> null
        }
    }

    fun obtenerFeriadoPorFecha(dateStr: String): FeriadoInfo? {
        try {
            val parts = dateStr.trim().split("/")
            if (parts.size == 3) {
                val d = parts[0].toIntOrNull() ?: return null
                val m = parts[1].toIntOrNull() ?: return null
                val y = parts[2].toIntOrNull() ?: return null
                return obtenerFeriado(d, m, y)
            }
        } catch (e: Exception) {}
        return null
    }
}

/**
 * 🎨 Asignación armónica de color según la categoría del evento o área
 */
fun getCategoryColor(category: String, isOfficial: Boolean = false): Color {
    return when {
        isOfficial -> MotoGoldSecondary // #F59E0B
        category.contains("Ruta", ignoreCase = true) || category.contains("Rodada", ignoreCase = true) -> MotoOrangePrimary // #FF6B00
        category.contains("Mantenimiento", ignoreCase = true) || category.contains("Garaje", ignoreCase = true) || category.contains("Mecánica", ignoreCase = true) -> Color(0xFF38BDF8) // Sky Blue
        category.contains("Lavado", ignoreCase = true) -> Color(0xFF06B6D4) // Cyan / Agua
        category.contains("Benéfica", ignoreCase = true) || category.contains("Solidaria", ignoreCase = true) -> Color(0xFFEC4899) // Pink / Solidario
        category.contains("Bar", ignoreCase = true) || category.contains("Cerveza", ignoreCase = true) -> Color(0xFFEAB308) // Amber Gold / Cerveza
        category.contains("Cumpleaños", ignoreCase = true) -> Color(0xFF8B5CF6) // Violet / Fiesta
        category.contains("Trámite", ignoreCase = true) || category.contains("Trabajo", ignoreCase = true) -> Color(0xFF94A3B8) // Slate
        category.contains("Salud", ignoreCase = true) || category.contains("SOS", ignoreCase = true) || category.contains("Emergencia", ignoreCase = true) -> TxFlameRed // #EF4444
        category.contains("Personal", ignoreCase = true) || category.contains("Familia", ignoreCase = true) || category.contains("Social", ignoreCase = true) -> Color(0xFFA855F7) // Purple
        else -> MotoGoldSecondary
    }
}

data class CalendarDayInfo(
    val dayNumber: Int,
    val dateString: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean = false,
    val isThursday: Boolean = false,
    val isWeekend: Boolean = false,
    val feriado: FeriadoInfo? = null
)

// ═══════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL: CALENDARIO MOTERO TX
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    events: List<BikerCalendarEvent>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    onCreateEvent: (
        title: String,
        desc: String,
        category: String,
        visibility: String,
        date: String,
        eventTime: String,
        depTime: String,
        origin: String,
        dest: String,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        terrain: String,
        difficulty: String,
        weather: String,
        captain: String,
        tail: String,
        remindDays: Int,
        isOfficial: Boolean,
        flyerUri: Uri?,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onUpdateEvent: (event: BikerCalendarEvent, flyerUri: Uri?, onComplete: (Boolean) -> Unit) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onToggleRsvp: (event: BikerCalendarEvent, isAttending: Boolean, hasPillion: Boolean) -> Unit,
    onToggleReminder: (event: BikerCalendarEvent, notifyInApp: Boolean) -> Unit,
    onPublishToFeed: (event: BikerCalendarEvent, onComplete: (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
    initialSelectedDate: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDirectiva = isDirectivaMode || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true) || (currentMember?.role == MemberRole.PRESIDENTE) || (currentMember?.role == MemberRole.DIRECTIVA)

    var currentViewMode by remember { mutableStateOf(CalendarViewMode.MES) }
    var currentCalendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val todayDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var selectedDateStr by remember { mutableStateOf(todayDateStr) }
    var selectedFilterCategory by remember { mutableStateOf("TODOS") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var prefilledTimeForCreate by remember { mutableStateOf("08:00 AM") }
    var eventToEdit by remember { mutableStateOf<BikerCalendarEvent?>(null) }
    var showThursdayPlanDialog by remember { mutableStateOf(false) }
    var selectedThursdayDate by remember { mutableStateOf("") }
    var showWeekendPlanDialog by remember { mutableStateOf(false) }
    var selectedWeekendDate by remember { mutableStateOf("") }
    var showGeneralDayPlanDialog by remember { mutableStateOf(false) }
    var selectedGeneralDayDate by remember { mutableStateOf("") }

    // Auto-seleccionar y enfocar la fecha cuando se navega desde el Muro TX
    LaunchedEffect(initialSelectedDate) {
        if (!initialSelectedDate.isNullOrBlank()) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val d = sdf.parse(initialSelectedDate.trim())
                if (d != null) {
                    selectedDateStr = initialSelectedDate.trim()
                    val cal = Calendar.getInstance().apply { time = d }
                    currentCalendarMonth = cal
                }
            } catch (ignored: Exception) {}
        }
    }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }
    val dayMonthYearFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Generar días del mes actual para la cuadrícula
    val daysInMonth = remember(currentCalendarMonth, todayDateStr) {
        val cal = currentCalendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0: Dom, 1: Lun...
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val daysList = mutableListOf<CalendarDayInfo>()
        // Días vacíos previos
        for (i in 0 until firstDayOfWeek) {
            daysList.add(CalendarDayInfo(0, "", false))
        }
        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = dayMonthYearFormat.format(cal.time)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isToday = (dateStr == todayDateStr)
            val isThursday = (dayOfWeek == Calendar.THURSDAY)
            val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
            val feriado = FeriadosHelper.obtenerFeriado(day, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))

            daysList.add(
                CalendarDayInfo(
                    dayNumber = day,
                    dateString = dateStr,
                    isCurrentMonth = true,
                    isToday = isToday,
                    isThursday = isThursday,
                    isWeekend = isWeekend,
                    feriado = feriado
                )
            )
        }
        daysList
    }

    // Filtrar eventos según categoría
    val filteredEvents = remember(events, selectedFilterCategory, selectedDateStr, currentViewMode) {
        events.filter { ev ->
            val matchCategory = when (selectedFilterCategory) {
                "TODOS" -> true
                "RUTAS" -> ev.category.contains("Ruta", ignoreCase = true) || ev.category.contains("Oficial", ignoreCase = true) || ev.category.contains("Rodada", ignoreCase = true)
                "GARAJE" -> ev.category.contains("Mantenimiento", ignoreCase = true) || ev.category.contains("Garaje", ignoreCase = true) || ev.category.contains("Trámite", ignoreCase = true)
                "PERSONALES" -> !ev.isOfficialClubEvent || ev.creatorMemberId == currentMember?.id
                else -> ev.category.equals(selectedFilterCategory, ignoreCase = true)
            }
            matchCategory
        }.sortedBy { it.eventDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Calendario Motero TX", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.White)
                            Text("Rutas, Clima, Garaje y Avisos en Vivo", fontSize = 11.sp, color = MotoGoldSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TxCarbonDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    prefilledTimeForCreate = "08:00 AM"
                    showCreateDialog = true
                },
                containerColor = TxFlameRed,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Evento")
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // SELECTOR DE VISTA: [ DÍA | MES | AÑO ]
            Surface(
                color = Color(0xFF131722),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarViewMode.values().forEach { mode ->
                        val isSelected = currentViewMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MotoOrangePrimary else Color(0xFF1E2433),
                            border = BorderStroke(1.dp, if (isSelected) MotoOrangePrimary else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentViewMode = mode }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = mode.label,
                                    tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }

            // CHIPS DE FILTRO DE CATEGORÍAS
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filterTabs = listOf("TODOS", "RUTAS", "GARAJE", "PERSONALES")
                items(filterTabs) { tab ->
                    val isSelected = selectedFilterCategory == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterCategory = tab },
                        label = {
                            Text(
                                text = when (tab) {
                                    "TODOS" -> "🌐 Todos (${events.size})"
                                    "RUTAS" -> "🏍️ Rutas & Rodadas"
                                    "GARAJE" -> "🔧 Garaje & Mecánica"
                                    "PERSONALES" -> "👤 Mis Eventos"
                                    else -> tab
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoOrangePrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E2433),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // CUERPO SEGÚN EL MODO DE VISTA SELECCIONADO
            when (currentViewMode) {
                CalendarViewMode.MES -> {
                    CalendarMonthView(
                        currentCalendarMonth = currentCalendarMonth,
                        monthFormat = monthFormat,
                        daysInMonth = daysInMonth,
                        selectedDateStr = selectedDateStr,
                        todayDateStr = todayDateStr,
                        events = events,
                        filteredEvents = filteredEvents,
                        currentMember = currentMember,
                        isDirectiva = isDirectiva,
                        onPrevMonth = {
                            val newCal = currentCalendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentCalendarMonth = newCal
                        },
                        onNextMonth = {
                            val newCal = currentCalendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentCalendarMonth = newCal
                        },
                        onSelectDate = { dateStr -> selectedDateStr = dateStr },
                        onSelectThursday = { dateStr ->
                            selectedThursdayDate = dateStr
                            selectedDateStr = dateStr
                            showThursdayPlanDialog = true
                        },
                        onSelectWeekend = { dateStr ->
                            selectedWeekendDate = dateStr
                            selectedDateStr = dateStr
                            showWeekendPlanDialog = true
                        },
                        onSelectGeneralDay = { dateStr ->
                            selectedGeneralDayDate = dateStr
                            selectedDateStr = dateStr
                            showGeneralDayPlanDialog = true
                        },
                        onEditEvent = { eventToEdit = it },
                        onDeleteEvent = onDeleteEvent,
                        onToggleRsvp = onToggleRsvp,
                        onToggleReminder = onToggleReminder,
                        onPublishToFeed = onPublishToFeed,
                        onOpenGps = { lat, lng, label -> openGpsLocation(context, lat, lng, label) },
                        onShareWhatsApp = { shareCalendarEventViaWhatsApp(context, it) },
                        onQuickAddEvent = {
                            prefilledTimeForCreate = "08:00 AM"
                            showCreateDialog = true
                        }
                    )
                }

                CalendarViewMode.DIA -> {
                    CalendarDayView(
                        selectedDateStr = selectedDateStr,
                        todayDateStr = todayDateStr,
                        events = events,
                        currentMember = currentMember,
                        isDirectiva = isDirectiva,
                        onChangeDate = { newDateStr -> selectedDateStr = newDateStr },
                        onGoToToday = {
                            selectedDateStr = todayDateStr
                            currentCalendarMonth = Calendar.getInstance()
                        },
                        onOpenThursdayPlan = {
                            selectedThursdayDate = selectedDateStr
                            showThursdayPlanDialog = true
                        },
                        onOpenWeekendPlan = {
                            selectedWeekendDate = selectedDateStr
                            showWeekendPlanDialog = true
                        },
                        onOpenGeneralDayPlan = {
                            selectedGeneralDayDate = selectedDateStr
                            showGeneralDayPlanDialog = true
                        },
                        onScheduleAtHour = { hourText ->
                            prefilledTimeForCreate = hourText
                            showCreateDialog = true
                        },
                        onEditEvent = { eventToEdit = it },
                        onDeleteEvent = onDeleteEvent,
                        onToggleRsvp = onToggleRsvp,
                        onToggleReminder = onToggleReminder,
                        onPublishToFeed = onPublishToFeed,
                        onOpenGps = { lat, lng, label -> openGpsLocation(context, lat, lng, label) },
                        onShareWhatsApp = { shareCalendarEventViaWhatsApp(context, it) }
                    )
                }

                CalendarViewMode.ANIO -> {
                    CalendarYearView(
                        currentYear = currentCalendarMonth.get(Calendar.YEAR),
                        events = events,
                        todayDateStr = todayDateStr,
                        onChangeYear = { yearDelta ->
                            val newCal = currentCalendarMonth.clone() as Calendar
                            newCal.add(Calendar.YEAR, yearDelta)
                            currentCalendarMonth = newCal
                        },
                        onSelectMonth = { selectedMonthIndex ->
                            val newCal = currentCalendarMonth.clone() as Calendar
                            newCal.set(Calendar.MONTH, selectedMonthIndex)
                            currentCalendarMonth = newCal
                            currentViewMode = CalendarViewMode.MES
                        }
                    )
                }
            }
        }
    }

    // DIÁLOGO PLANIFICAR JUEVES MOTERO
    if (showThursdayPlanDialog) {
        PlanJuevesMoteroDialog(
            targetDate = selectedThursdayDate,
            isDirectiva = isDirectiva,
            onDismiss = { showThursdayPlanDialog = false },
            onSelectPreset = { title, desc, cat, time, depTime, dest, oLat, oLng ->
                onCreateEvent(
                    title,
                    desc,
                    cat,
                    "PUBLICO_CLUB",
                    selectedThursdayDate,
                    time,
                    depTime,
                    "Redoma de San Jacinto / Sede TX",
                    dest,
                    oLat,
                    oLng,
                    0.0,
                    0.0,
                    "Asfalto",
                    "Fácil",
                    "Noche despejada 24°C",
                    "Capitán de Turno",
                    "Barredora Oficial",
                    1,
                    isDirectiva,
                    null
                ) { success ->
                    if (success) Toast.makeText(context, "¡Jueves Motero programado con éxito!", Toast.LENGTH_SHORT).show()
                }
                showThursdayPlanDialog = false
            },
            onOpenCustomCreate = {
                prefilledTimeForCreate = "07:00 PM"
                showThursdayPlanDialog = false
                showCreateDialog = true
            }
        )
    }

    // DIÁLOGO PLANIFICAR FIN DE SEMANA (SÁBADOS Y DOMINGOS)
    if (showWeekendPlanDialog) {
        PlanFinDeSemanaDialog(
            targetDate = selectedWeekendDate,
            isDirectiva = isDirectiva,
            onDismiss = { showWeekendPlanDialog = false },
            onSelectPreset = { title, desc, cat, time, depTime, dest, oLat, oLng ->
                onCreateEvent(
                    title,
                    desc,
                    cat,
                    "PUBLICO_CLUB",
                    selectedWeekendDate,
                    time,
                    depTime,
                    "Redoma de San Jacinto / Sede TX",
                    dest,
                    oLat,
                    oLng,
                    0.0,
                    0.0,
                    "Asfalto",
                    "Media",
                    "Soleado 29°C",
                    "Capitán de Turno",
                    "Barredora Oficial",
                    1,
                    isDirectiva,
                    null
                ) { success ->
                    if (success) Toast.makeText(context, "¡Rodada de Fin de Semana programada con éxito!", Toast.LENGTH_SHORT).show()
                }
                showWeekendPlanDialog = false
            },
            onOpenCustomCreate = { defaultTime ->
                prefilledTimeForCreate = defaultTime
                showWeekendPlanDialog = false
                showCreateDialog = true
            }
        )
    }

    // DIÁLOGO PLANIFICAR ACTIVIDAD DE CUALQUIER DÍA (LUNES, MARTES, MIÉRCOLES, VIERNES)
    if (showGeneralDayPlanDialog) {
        PlanDiaActividadDialog(
            targetDate = selectedGeneralDayDate,
            isDirectiva = isDirectiva,
            onDismiss = { showGeneralDayPlanDialog = false },
            onSelectPreset = { title, desc, cat, time, depTime, dest, oLat, oLng ->
                onCreateEvent(
                    title,
                    desc,
                    cat,
                    "PUBLICO_CLUB",
                    selectedGeneralDayDate,
                    time,
                    depTime,
                    "Sede TX / Punto de Encuentro",
                    dest,
                    oLat,
                    oLng,
                    0.0,
                    0.0,
                    "Asfalto",
                    "Fácil",
                    "Despejado",
                    "Coordinador de Actividad",
                    "Apoyo Oficial",
                    1,
                    isDirectiva,
                    null
                ) { success ->
                    if (success) Toast.makeText(context, "¡Actividad agendada con éxito!", Toast.LENGTH_SHORT).show()
                }
                showGeneralDayPlanDialog = false
            },
            onOpenCustomCreate = { defaultTime ->
                prefilledTimeForCreate = defaultTime
                showGeneralDayPlanDialog = false
                showCreateDialog = true
            }
        )
    }

    // DIÁLOGO CREAR / EDITAR EVENTO GENERAL
    if (showCreateDialog || eventToEdit != null) {
        CreateEditCalendarEventDialog(
            existingEvent = eventToEdit,
            initialDate = selectedDateStr,
            initialTime = prefilledTimeForCreate,
            isDirectiva = isDirectiva,
            onDismiss = {
                showCreateDialog = false
                eventToEdit = null
            },
            onSave = { title, desc, cat, vis, date, eTime, dTime, orig, dest, oLat, oLng, dLat, dLng, terrain, diff, weather, capt, tail, remindDays, isOfficial, flyerUri ->
                if (eventToEdit != null) {
                    onUpdateEvent(
                        eventToEdit!!.copy(
                            title = title,
                            description = desc,
                            category = cat,
                            visibility = vis,
                            eventDate = date,
                            eventTime = eTime,
                            departureTime = dTime,
                            originAddress = orig,
                            destinationAddress = dest,
                            originLatitude = oLat,
                            originLongitude = oLng,
                            destinationLatitude = dLat,
                            destinationLongitude = dLng,
                            terrainType = terrain,
                            difficultyLevel = diff,
                            weatherForecast = weather,
                            roadCaptain = capt,
                            tailRider = tail,
                            remindDaysBefore = remindDays,
                            isOfficialClubEvent = isOfficial
                        ),
                        flyerUri
                    ) { success ->
                        if (success) Toast.makeText(context, "Evento actualizado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    onCreateEvent(title, desc, cat, vis, date, eTime, dTime, orig, dest, oLat, oLng, dLat, dLng, terrain, diff, weather, capt, tail, remindDays, isOfficial, flyerUri) { success ->
                        if (success) Toast.makeText(context, "Evento agendado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                }
                showCreateDialog = false
                eventToEdit = null
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// VISTA 1: MES (CUADRÍCULA MENSUAL, JUEVES MOTERO, FINES DE SEMANA)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun CalendarMonthView(
    currentCalendarMonth: Calendar,
    monthFormat: SimpleDateFormat,
    daysInMonth: List<CalendarDayInfo>,
    selectedDateStr: String,
    todayDateStr: String,
    events: List<BikerCalendarEvent>,
    filteredEvents: List<BikerCalendarEvent>,
    currentMember: MemberProfile?,
    isDirectiva: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectThursday: (String) -> Unit,
    onSelectWeekend: (String) -> Unit,
    onSelectGeneralDay: (String) -> Unit,
    onEditEvent: (BikerCalendarEvent) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onToggleRsvp: (BikerCalendarEvent, Boolean, Boolean) -> Unit,
    onToggleReminder: (BikerCalendarEvent, Boolean) -> Unit,
    onPublishToFeed: (BikerCalendarEvent, (Boolean) -> Unit) -> Unit,
    onOpenGps: (Double, Double, String) -> Unit,
    onShareWhatsApp: (BikerCalendarEvent) -> Unit,
    onQuickAddEvent: () -> Unit
) {
    val context = LocalContext.current
    val selectedDayFeriado = remember(selectedDateStr) { FeriadosHelper.obtenerFeriadoPorFecha(selectedDateStr) }

    Column(modifier = Modifier.fillMaxSize()) {
        // CABECERA DEL MES (Selector Mes Anterior / Siguiente)
        Surface(
            color = Color(0xFF1E2433),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = MotoOrangePrimary)
                    }

                    Text(
                        text = monthFormat.format(currentCalendarMonth.time).replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = MotoOrangePrimary)
                    }
                }

                // Días de la semana con resaltado de Sáb/Dom en Amarillo y Jueves Motero
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val weekDays = listOf("DOM", "LUN", "MAR", "MIÉ", "🏍️ JUE", "VIE", "SÁB")
                    weekDays.forEach { wDay ->
                        val isWeekend = (wDay == "DOM" || wDay == "SÁB")
                        val isThursday = wDay.contains("JUE")
                        Text(
                            text = wDay,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isWeekend -> Color(0xFFFACC15) // Amarillo Libre
                                isThursday -> MotoOrangePrimary // Jueves Motero
                                else -> Color(0xFF90A4AE)
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // CUADRÍCULA DE DÍAS DEL MES
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    userScrollEnabled = false
                ) {
                    items(daysInMonth) { dayInfo ->
                        if (dayInfo.isCurrentMonth) {
                            val isSelected = selectedDateStr == dayInfo.dateString
                            val dayEvents = events.filter { it.eventDate == dayInfo.dateString }

                            // Estilos de bordes y fondo según condición
                            val cellBackground = when {
                                isSelected -> MotoOrangePrimary.copy(alpha = 0.35f)
                                dayInfo.isToday -> MotoOrangePrimary.copy(alpha = 0.20f)
                                dayInfo.isWeekend -> Color(0xFFFACC15).copy(alpha = 0.08f)
                                dayInfo.isThursday -> Color(0xFFEA580C).copy(alpha = 0.08f)
                                dayEvents.isNotEmpty() -> Color(0xFF263238)
                                else -> Color.Transparent
                            }

                            val cellBorder: BorderStroke? = when {
                                dayInfo.isToday -> BorderStroke(1.5.dp, TxFlameRed)
                                isSelected -> BorderStroke(1.5.dp, MotoGoldSecondary)
                                dayInfo.isWeekend -> BorderStroke(0.5.dp, Color(0xFFFACC15).copy(alpha = 0.4f))
                                dayInfo.isThursday -> BorderStroke(0.5.dp, MotoOrangePrimary.copy(alpha = 0.4f))
                                else -> null
                            }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.15f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cellBackground)
                                    .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(8.dp)) else Modifier)
                                    .clickable {
                                        onSelectDate(dayInfo.dateString)
                                        if (dayInfo.isThursday) {
                                            onSelectThursday(dayInfo.dateString)
                                        } else if (dayInfo.isWeekend) {
                                            onSelectWeekend(dayInfo.dateString)
                                        } else {
                                            onSelectGeneralDay(dayInfo.dateString)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${dayInfo.dayNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || dayInfo.isToday || dayEvents.isNotEmpty()) FontWeight.Black else FontWeight.Normal,
                                        color = when {
                                            dayInfo.isToday -> TxFlameRed
                                            isSelected -> MotoGoldSecondary
                                            dayInfo.isWeekend -> Color(0xFFFACC15)
                                            dayInfo.isThursday -> MotoOrangePrimary
                                            else -> Color.White
                                        }
                                    )

                                    // Indicador "HOY", Feriado o Eventos
                                    if (dayInfo.isToday) {
                                        Text("HOY", fontSize = 7.sp, fontWeight = FontWeight.Black, color = TxFlameRed)
                                    } else if (dayInfo.feriado != null) {
                                        Text(dayInfo.feriado.emoji, fontSize = 7.sp)
                                    } else if (dayInfo.isThursday) {
                                        Text("🏍️", fontSize = 7.sp)
                                    }

                                    // Puntos de eventos coloreados según categoría
                                    if (dayEvents.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 1.dp)) {
                                            dayEvents.take(3).forEach { ev ->
                                                val catColor = getCategoryColor(ev.category, ev.isOfficialClubEvent)
                                                Box(modifier = Modifier.size(4.dp).background(catColor, CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.aspectRatio(1.15f))
                        }
                    }
                }
            }
        }

        // BANNER DE FECHA SELECCIONADA / FERIADO
        if (selectedDayFeriado != null) {
            Surface(
                color = Color(0xFF312E81).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color(0xFF818CF8)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(selectedDayFeriado.emoji, fontSize = 16.sp)
                    Column {
                        Text(
                            text = if (selectedDayFeriado.esOficial) "Feriado Oficial: ${selectedDayFeriado.nombre}" else "Conmemoración: ${selectedDayFeriado.nombre}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFE0E7FF)
                        )
                        Text(
                            text = "Fecha: $selectedDateStr • Ideal para agendar rutas o descanso",
                            fontSize = 10.sp,
                            color = Color(0xFFC7D2FE)
                        )
                    }
                }
            }
        }

        // LISTA DE EVENTOS PROGRAMADOS
        val dayEvents = filteredEvents.filter { it.eventDate == selectedDateStr }
        val eventsToShow = if (dayEvents.isNotEmpty()) dayEvents else filteredEvents

        if (eventsToShow.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFF607D8B), modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Sin eventos para esta fecha", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Presiona '+' o toca un día para agendar una rodada, mantenimiento o reunión.", color = Color(0xFF90A4AE), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onQuickAddEvent,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agendar Evento Aquí")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = if (dayEvents.isNotEmpty()) "📌 Eventos del día ($selectedDateStr)" else "📅 Próximos eventos programados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MotoGoldSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                items(eventsToShow, key = { it.id }) { event ->
                    BikerEventCard(
                        event = event,
                        currentMember = currentMember,
                        isDirectiva = isDirectiva,
                        isCreator = event.creatorMemberId == currentMember?.id,
                        onEdit = { onEditEvent(event) },
                        onDelete = { onDeleteEvent(event.id) },
                        onToggleRsvp = { isAttending: Boolean, hasPillion: Boolean -> onToggleRsvp(event, isAttending, hasPillion) },
                        onToggleReminder = { notifyInApp: Boolean -> onToggleReminder(event, notifyInApp) },
                        onPublishToFeed = {
                            onPublishToFeed(event) { success ->
                                if (success) Toast.makeText(context, "Evento publicado en el Muro Oficial", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenGps = onOpenGps,
                        onShareWhatsApp = { onShareWhatsApp(event) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// VISTA 2: DÍA (BLOQUES DE TIEMPO / TIME-BLOCKING DE 06:00 A 23:00)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun CalendarDayView(
    selectedDateStr: String,
    todayDateStr: String,
    events: List<BikerCalendarEvent>,
    currentMember: MemberProfile?,
    isDirectiva: Boolean,
    onChangeDate: (String) -> Unit,
    onGoToToday: () -> Unit,
    onOpenThursdayPlan: () -> Unit,
    onOpenWeekendPlan: () -> Unit,
    onOpenGeneralDayPlan: () -> Unit,
    onScheduleAtHour: (String) -> Unit,
    onEditEvent: (BikerCalendarEvent) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onToggleRsvp: (BikerCalendarEvent, Boolean, Boolean) -> Unit,
    onToggleReminder: (BikerCalendarEvent, Boolean) -> Unit,
    onPublishToFeed: (BikerCalendarEvent, (Boolean) -> Unit) -> Unit,
    onOpenGps: (Double, Double, String) -> Unit,
    onShareWhatsApp: (BikerCalendarEvent) -> Unit
) {
    val context = LocalContext.current
    val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fullSpanishFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES")) }

    val currentCal = remember(selectedDateStr) {
        val c = Calendar.getInstance()
        try {
            val d = dayFormat.parse(selectedDateStr)
            if (d != null) c.time = d
        } catch (ignored: Exception) {}
        c
    }

    val isToday = (selectedDateStr == todayDateStr)
    val dayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK)
    val isThursday = (dayOfWeek == Calendar.THURSDAY)
    val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
    val feriado = FeriadosHelper.obtenerFeriadoPorFecha(selectedDateStr)

    // Eventos de este día específico
    val dayEvents = remember(events, selectedDateStr) {
        events.filter { it.eventDate == selectedDateStr }
    }

    // 18 Bloques de tiempo de 06:00 a 23:00
    val timeSlots = listOf(
        "06:00 AM", "07:00 AM", "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
        "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM",
        "06:00 PM", "07:00 PM", "08:00 PM", "09:00 PM", "10:00 PM", "11:00 PM"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // CABECERA DEL DÍA (Navegador ◀ Hoy ▶)
        Surface(
            color = Color(0xFF1E2433),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val c = currentCal.clone() as Calendar
                            c.add(Calendar.DAY_OF_MONTH, -1)
                            onChangeDate(dayFormat.format(c.time))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Día anterior", tint = MotoOrangePrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = fullSpanishFormat.format(currentCal.time).replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isToday) "🌟 DÍA EN CURSO (HOY)" else selectedDateStr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) TxFlameRed else MotoGoldSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            val c = currentCal.clone() as Calendar
                            c.add(Calendar.DAY_OF_MONTH, 1)
                            onChangeDate(dayFormat.format(c.time))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Día siguiente", tint = MotoOrangePrimary)
                    }
                }

                // INSIGNIAS DEL DÍA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isToday) {
                        Surface(shape = RoundedCornerShape(4.dp), color = TxFlameRed.copy(alpha = 0.2f), border = BorderStroke(0.5.dp, TxFlameRed)) {
                            Text("🌟 HOY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TxFlameRed, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    if (isThursday) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MotoOrangePrimary.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, MotoOrangePrimary),
                            modifier = Modifier.clickable(onClick = onOpenThursdayPlan)
                        ) {
                            Text("🏍️ JUEVES MOTERO • Toca para planificar nocturna", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MotoOrangePrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    if (isWeekend) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFACC15).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFFFACC15)),
                            modifier = Modifier.clickable(onClick = onOpenWeekendPlan)
                        ) {
                            Text("🏖️ LIBRE PARA RODADAS • Toca para planificar fin de semana", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFACC15), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    if (!isThursday && !isWeekend) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MotoGoldSecondary.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, MotoGoldSecondary),
                            modifier = Modifier.clickable(onClick = onOpenGeneralDayPlan)
                        ) {
                            Text("📅 PLANIFICAR DÍA / CUMPLEAÑOS 🎂", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    if (feriado != null) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF6366F1).copy(alpha = 0.2f), border = BorderStroke(0.5.dp, Color(0xFF818CF8))) {
                            Text("${feriado.emoji} ${feriado.nombre}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    if (!isToday) {
                        TextButton(onClick = onGoToToday, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                            Text("👉 Ir a Hoy", fontSize = 10.sp, color = MotoGoldSecondary)
                        }
                    }
                }
            }
        }

        // LÍNEA DE TIEMPO: BLOQUES HORARIOS (TIME-BLOCKING)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timeSlots) { slotHour ->
                // Buscar eventos asignados a esta hora aproximada
                val slotEvents = dayEvents.filter { ev ->
                    val evTime = ev.eventTime.trim().uppercase()
                    val slotPrefix = slotHour.substring(0, 2) // "08", "02"
                    val isPm = slotHour.contains("PM")
                    val isAm = slotHour.contains("AM")
                    (evTime.contains(slotPrefix) && ((isPm && evTime.contains("PM")) || (isAm && evTime.contains("AM")))) ||
                            (evTime.contains(slotHour.take(5)))
                }

                Surface(
                    color = Color(0xFF131722),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, if (slotEvents.isNotEmpty()) MotoOrangePrimary else Color(0xFF263238)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Etiqueta de la hora
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(68.dp)
                        ) {
                            Text(
                                text = slotHour,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (slotEvents.isNotEmpty()) MotoOrangePrimary else Color(0xFF90A4AE)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (slotEvents.isNotEmpty()) MotoGoldSecondary else Color(0xFF37474F))
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(if (slotEvents.isNotEmpty()) 80.dp else 36.dp)
                                .padding(horizontal = 8.dp),
                            color = Color(0xFF2A3447)
                        )

                        // Contenido del bloque horario
                        Column(modifier = Modifier.weight(1f)) {
                            if (slotEvents.isEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Horario libre",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onScheduleAtHour(slotHour) }
                                    ) {
                                        Text(
                                            text = "➕ Agendar a las $slotHour",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotoGoldSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                slotEvents.forEach { ev ->
                                    val catColor = getCategoryColor(ev.category, ev.isOfficialClubEvent)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E2433),
                                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = catColor.copy(alpha = 0.2f)) {
                                                    Text(ev.category.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = catColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                                Text("⏰ Salida: ${ev.departureTime}", fontSize = 10.sp, color = MotoGoldSecondary)
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(ev.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                            if (ev.originAddress.isNotBlank()) {
                                                Text("📍 Salida: ${ev.originAddress}", fontSize = 10.sp, color = Color(0xFF90A4AE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = { onToggleRsvp(ev, true, false) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Text("RSVP (${ev.rsvpPilotsCount})", fontSize = 9.sp)
                                                }

                                                if (ev.originLatitude != 0.0 || ev.destinationLatitude != 0.0) {
                                                    IconButton(
                                                        onClick = {
                                                            val lat = if (ev.originLatitude != 0.0) ev.originLatitude else ev.destinationLatitude
                                                            val lng = if (ev.originLongitude != 0.0) ev.originLongitude else ev.destinationLongitude
                                                            onOpenGps(lat, lng, ev.title)
                                                        },
                                                        modifier = Modifier.size(26.dp).background(Color(0xFF263238), RoundedCornerShape(4.dp))
                                                    ) {
                                                        Icon(Icons.Default.Navigation, contentDescription = "GPS", tint = Color(0xFF4FC3F7), modifier = Modifier.size(14.dp))
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { onShareWhatsApp(ev) },
                                                    modifier = Modifier.size(26.dp).background(Color(0xFF25D366), RoundedCornerShape(4.dp))
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(14.dp))
                                                }

                                                if (isDirectiva || ev.creatorMemberId == currentMember?.id) {
                                                    IconButton(
                                                        onClick = { onEditEvent(ev) },
                                                        modifier = Modifier.size(26.dp).background(Color(0xFF37474F), RoundedCornerShape(4.dp))
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// VISTA 3: AÑO (PANORÁMICA DE LOS 12 MESES)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun CalendarYearView(
    currentYear: Int,
    events: List<BikerCalendarEvent>,
    todayDateStr: String,
    onChangeYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit
) {
    val monthNames = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // CABECERA DE AÑO (◀ 2026 ▶)
        Surface(
            color = Color(0xFF1E2433),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onChangeYear(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Año anterior", tint = MotoOrangePrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AÑO $currentYear",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "12 Meses • Rutas, Feriados y Jueves Moteros",
                        fontSize = 11.sp,
                        color = MotoGoldSecondary
                    )
                }

                IconButton(onClick = { onChangeYear(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Año siguiente", tint = MotoOrangePrimary)
                }
            }
        }

        // MATRIZ DE 12 MESES
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(12) { monthIndex ->
                val monthName = monthNames[monthIndex]
                val monthEventsCount = events.count {
                    val parts = it.eventDate.split("/")
                    parts.size == 3 && (parts[1].toIntOrNull() == monthIndex + 1) && (parts[2].toIntOrNull() == currentYear)
                }

                Surface(
                    color = Color(0xFF131722),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (monthEventsCount > 0) MotoOrangePrimary.copy(alpha = 0.5f) else Color(0xFF263238)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMonth(monthIndex) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthName,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            if (monthEventsCount > 0) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MotoGoldSecondary.copy(alpha = 0.2f)) {
                                    Text(
                                        text = "$monthEventsCount rodadas",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MotoGoldSecondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Mini-calendario ilustrativo del mes
                        val tempCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, monthIndex)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val startOffset = tempCal.get(Calendar.DAY_OF_WEEK) - 1

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Cabecera mini días
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                listOf("D", "L", "M", "M", "J", "V", "S").forEachIndexed { idx, d ->
                                    Text(
                                        d,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (idx) {
                                            0, 6 -> Color(0xFFFACC15)
                                            4 -> MotoOrangePrimary
                                            else -> Color(0xFF64748B)
                                        }
                                    )
                                }
                            }

                            // Días resumidos
                            val allMiniDays = mutableListOf<Int>()
                            for (i in 0 until startOffset) allMiniDays.add(0)
                            for (d in 1..maxDays) allMiniDays.add(d)

                            allMiniDays.chunked(7).take(5).forEach { week ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                    for (i in 0 until 7) {
                                        val dayVal = week.getOrNull(i) ?: 0
                                        if (dayVal > 0) {
                                            val isTodayMini = todayDateStr == String.format("%02d/%02d/%04d", dayVal, monthIndex + 1, currentYear)
                                            Text(
                                                text = "$dayVal",
                                                fontSize = 8.sp,
                                                fontWeight = if (isTodayMini) FontWeight.Black else FontWeight.Normal,
                                                color = if (isTodayMini) TxFlameRed else Color(0xFFCBD5E1)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "👉 Toca para abrir mes completo",
                            fontSize = 9.sp,
                            color = MotoOrangePrimary
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO 1: PLANIFICADOR RÁPIDO DE JUEVES MOTERO
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PlanJuevesMoteroDialog(
    targetDate: String,
    isDirectiva: Boolean = false,
    onDismiss: () -> Unit,
    onSelectPreset: (title: String, desc: String, cat: String, time: String, depTime: String, dest: String, oLat: Double, oLng: Double) -> Unit,
    onOpenCustomCreate: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MotoOrangePrimary)
                Column {
                    Text("🏍️ Jueves Motero TX", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    Text("Fecha: $targetDate • Planificar Actividad", fontSize = 11.sp, color = MotoGoldSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SECCIÓN DIRECTIVA: SINCRONIZACIÓN Y COORDENADAS DEL MAPA TX
                if (isDirectiva) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, MotoGoldSecondary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🛡️", fontSize = 14.sp)
                                Text("Directiva: Sitio de Encuentro en Mapa", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MotoGoldSecondary)
                            }
                            Text(
                                text = "Busca la ubicación en el Mapa TX, copia las coordenadas y pégalas aquí:",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val coords = GestorPortapapeles.leerCoordenadaValida(context)
                                        if (coords != null) {
                                            val parts = coords.split(",")
                                            if (parts.size >= 2) {
                                                val lat = parts[0].trim().toDoubleOrNull() ?: 10.2319
                                                val lng = parts[1].trim().toDoubleOrNull() ?: -67.5744
                                                onSelectPreset(
                                                    "🏍️ Jueves Motero TX - $targetDate",
                                                    "Encuentro oficial del club en coordenadas fijadas por la Directiva desde Mapa TX ($coords).",
                                                    "Ruta Oficial",
                                                    "07:00 PM",
                                                    "07:30 PM",
                                                    "Punto de Encuentro Mapa ($coords)",
                                                    lat,
                                                    lng
                                                )
                                                Toast.makeText(context, "✅ Ubicación fijada desde Mapa: $coords", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val texto = GestorPortapapeles.leerTexto(context)
                                            if (texto.isNullOrBlank()) {
                                                Toast.makeText(context, "Portapapeles vacío. Copia coordenadas en Mapa TX primero.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No se detectaron coordenadas válidas en el texto copiado.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp), tint = MotoGoldSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📋 Pegar Coords", fontSize = 10.sp, color = MotoGoldSecondary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        PuenteMapa.mostrarUbicacionEnMapa(context, "10.2469,-67.5958", "Jueves Motero TX")
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🗺️ Abrir Mapa TX", fontSize = 10.sp, color = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "¿Qué se va a hacer este Jueves Motero? Selecciona una actividad o personalízala:",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                // Opción 1: Nocturna Motera
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🌙 Nocturna Motera TX - $targetDate",
                                "Rodada nocturna semanal del club. Concentración y recorrido por la ciudad con parada gastronómica y hermandad biker.",
                                "Ruta Oficial",
                                "07:00 PM",
                                "07:30 PM",
                                "Zona Gastronómica / Las Delicias",
                                10.2469,
                                -67.5958
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🌙", fontSize = 24.sp)
                        Column {
                            Text("Nocturna Motera TX", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("7:00 PM • Rodada urbana + cena y hermandad biker", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 2: Encuentro de Garaje & Taller
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🛠️ Mantenimiento Preventivo de Jueves - $targetDate",
                                "Revisión técnica de motos, ajuste y lubricación de cadenas, revisión de fluidos previa a la rodada de fin de semana.",
                                "Mantenimiento / Garaje",
                                "06:30 PM",
                                "07:00 PM",
                                "Taller Oficial Team TX",
                                10.2319,
                                -67.5744
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🛠️", fontSize = 24.sp)
                        Column {
                            Text("Garaje & Mantenimiento Preventivo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("6:30 PM • Ajustes, cadenas y revisión para el fin de semana", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 3: Bar & Encuentro Biker
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🍺 Bar & Encuentro Biker TX - $targetDate",
                                "Compartir y hermandad motera, música, cerveza fría y amistad entre hermanos del club.",
                                "Bar & Encuentro",
                                "07:00 PM",
                                "07:30 PM",
                                "Bar Biker Central",
                                10.2469,
                                -67.5958
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🍺", fontSize = 24.sp)
                        Column {
                            Text("Bar & Encuentro Biker", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("7:00 PM • Cerveza fría, buena música y hermandad motera", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 4: Café Biker & Hermandad
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, MotoGoldSecondary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "☕ Café Biker & Hermandad TX - $targetDate",
                                "Encuentro social de hermandad motera para compartir un café y planificar las rutas del sábado y domingo.",
                                "Evento Social",
                                "07:00 PM",
                                "07:30 PM",
                                "Cafetería Biker Central",
                                10.2400,
                                -67.5800
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("☕", fontSize = 24.sp)
                        Column {
                            Text("Café Biker & Hermandad", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("7:00 PM • Hermandad y planificación de rodadas", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 5: Cumpleaños Motero
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🎂 Cumpleaños Motero TX - $targetDate",
                                "Celebración y homenaje especial a hermanos moteros del club que cumplen años este mes.",
                                "Cumpleaños Motero",
                                "07:00 PM",
                                "07:30 PM",
                                "Sede TX / Local de Celebración",
                                10.2400,
                                -67.5800
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎂", fontSize = 24.sp)
                        Column {
                            Text("Cumpleaños Motero", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("7:00 PM • Celebración y felicitaciones a hermanos del club", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 6: Personalizado
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenCustomCreate)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("✏️", fontSize = 24.sp)
                        Column {
                            Text("Personalizado / Otra Actividad", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("Abrir formulario completo para definir hora, ruta y flyer", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF90A4AE)) }
        },
        containerColor = Color(0xFF131722)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO 2: PLANIFICADOR DE RODADAS DE FIN DE SEMANA (SÁBADOS Y DOMINGOS)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PlanFinDeSemanaDialog(
    targetDate: String,
    isDirectiva: Boolean = false,
    onDismiss: () -> Unit,
    onSelectPreset: (title: String, desc: String, cat: String, time: String, depTime: String, dest: String, oLat: Double, oLng: Double) -> Unit,
    onOpenCustomCreate: (defaultTime: String) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SportsMotorsports, contentDescription = null, tint = Color(0xFFFACC15))
                Column {
                    Text("🏖️ Rodada de Fin de Semana TX", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    Text("Fecha: $targetDate • Planificar Rutas y Rodadas", fontSize = 11.sp, color = Color(0xFFFACC15))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SECCIÓN DIRECTIVA: SINCRONIZACIÓN Y COORDENADAS DEL MAPA TX
                if (isDirectiva) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFFFACC15).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🛡️", fontSize = 14.sp)
                                Text("Directiva: Sitio de Salida / Destino en Mapa", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFACC15))
                            }
                            Text(
                                text = "Busca la ubicación en el Mapa TX, copia las coordenadas y pégalas aquí:",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val coords = GestorPortapapeles.leerCoordenadaValida(context)
                                        if (coords != null) {
                                            val parts = coords.split(",")
                                            if (parts.size >= 2) {
                                                val lat = parts[0].trim().toDoubleOrNull() ?: 10.2319
                                                val lng = parts[1].trim().toDoubleOrNull() ?: -67.5744
                                                onSelectPreset(
                                                    "🏖️ Rodada Oficial de Fin de Semana - $targetDate",
                                                    "Rodada oficial de fin de semana con salida desde coordenadas fijadas en Mapa TX ($coords).",
                                                    "Ruta Oficial",
                                                    "07:00 AM",
                                                    "07:30 AM",
                                                    "Punto de Salida Mapa ($coords)",
                                                    lat,
                                                    lng
                                                )
                                                Toast.makeText(context, "✅ Ubicación de rodada fijada desde Mapa: $coords", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val texto = GestorPortapapeles.leerTexto(context)
                                            if (texto.isNullOrBlank()) {
                                                Toast.makeText(context, "Portapapeles vacío. Copia coordenadas en Mapa TX primero.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No se detectaron coordenadas válidas en el texto copiado.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFFACC15))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📋 Pegar Coords", fontSize = 10.sp, color = Color(0xFFFACC15))
                                }

                                OutlinedButton(
                                    onClick = {
                                        PuenteMapa.mostrarUbicacionEnMapa(context, "10.4900,-67.7300", "Rodada Fin de Semana TX")
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🗺️ Abrir Mapa TX", fontSize = 10.sp, color = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Selecciona el plan o rodada para este fin de semana:",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                // Opción 1: Rodada Playera & Costera
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🏖️ Rodada Playera & Costera TX - $targetDate",
                                "Rodada hacia la costa con sol, mar, comida típica y hermandad biker. Concentración temprana y recorrido en caravana.",
                                "Ruta Oficial",
                                "07:00 AM",
                                "07:30 AM",
                                "Bahía de Cata / Ocumare / Cuyagua",
                                10.4900,
                                -67.7300
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🏖️", fontSize = 24.sp)
                        Column {
                            Text("Rodada Playera & Costera", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("7:00 AM • Sol, mar, comida y hermandad en la costa", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 2: Ruta de Montaña & Curvas
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "⛰️ Ruta de Montaña & Curvas TX - $targetDate",
                                "Rodada de montaña, clima fresco, curvas técnicas y paradas gastronómicas.",
                                "Ruta Oficial",
                                "07:30 AM",
                                "08:00 AM",
                                "Colonia Tovar / Choroní",
                                10.4200,
                                -67.2800
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⛰️", fontSize = 24.sp)
                        Column {
                            Text("Ruta de Montaña & Curvas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("7:30 AM • Clima fresco, curvas y vistas panorámicas", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 3: Lavado de Moto en Familia
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🧼 Lavado de Moto en Familia TX - $targetDate",
                                "Jornada de limpieza, brillo y compartir familiar biker del club. Trae a tu familia a consentir las máquinas.",
                                "Lavado de Moto en Familia",
                                "09:00 AM",
                                "09:30 AM",
                                "Sede Club / Autolavado Biker",
                                10.2319,
                                -67.5744
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🧼", fontSize = 24.sp)
                        Column {
                            Text("Lavado de Moto en Familia", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("9:00 AM • Limpieza, brillo y encuentro familiar del club", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 4: Mantenimiento Preventivo para Rodada
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🛠️ Mantenimiento Preventivo para Rodada - $targetDate",
                                "Revisión técnica de motos, ajuste de cadenas, frenos, lubricación y fluidos para las rodadas.",
                                "Mantenimiento Preventivo",
                                "08:00 AM",
                                "08:30 AM",
                                "Taller Oficial Team TX",
                                10.2319,
                                -67.5744
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🛠️", fontSize = 24.sp)
                        Column {
                            Text("Mantenimiento Preventivo para Rodada", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("8:00 AM • Ajuste de cadenas, frenos, fluidos y puesta a punto", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 5: Obra Benéfica & Labor Social
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "❤️ Obra Benéfica & Labor Social TX - $targetDate",
                                "Rodada solidaria del club con entrega de donaciones y apoyo social a la comunidad.",
                                "Obra Benéfica",
                                "08:30 AM",
                                "09:00 AM",
                                "Comunidad / Centro Solidario",
                                10.2319,
                                -67.5744
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("❤️", fontSize = 24.sp)
                        Column {
                            Text("Obra Benéfica & Labor Social", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("8:30 AM • Rodada solidaria, donaciones y apoyo comunitario", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 6: Bar & Encuentro Biker
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🍺 Bar & Encuentro Biker TX - $targetDate",
                                "Tarde de relax, buena música, cerveza fría y hermandad biker tras la rodada de fin de semana.",
                                "Bar & Encuentro",
                                "04:00 PM",
                                "04:30 PM",
                                "Bar Biker Central",
                                10.2469,
                                -67.5958
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🍺", fontSize = 24.sp)
                        Column {
                            Text("Bar & Encuentro Biker", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("4:00 PM • Cerveza fría, buena música y hermandad motera", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 7: Cumpleaños Motero
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🎂 Cumpleaños Motero TX - $targetDate",
                                "Festejo y homenaje especial a hermanos moteros del club que cumplen años este fin de semana.",
                                "Cumpleaños Motero",
                                "05:00 PM",
                                "05:30 PM",
                                "Sede TX / Local de Celebración",
                                10.2400,
                                -67.5800
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎂", fontSize = 24.sp)
                        Column {
                            Text("Cumpleaños Motero", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("5:00 PM • Festejo y rodada de cumpleaños", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 8: Personalizado
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenCustomCreate("07:30 AM") }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("✏️", fontSize = 24.sp)
                        Column {
                            Text("Personalizado / Otra Rodada", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("Abrir formulario completo para definir hora, ruta y flyer", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF90A4AE)) }
        },
        containerColor = Color(0xFF131722)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO 3: PLANIFICADOR DE ACTIVIDADES PARA CUALQUIER DÍA
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PlanDiaActividadDialog(
    targetDate: String,
    isDirectiva: Boolean = false,
    onDismiss: () -> Unit,
    onSelectPreset: (title: String, desc: String, cat: String, time: String, depTime: String, dest: String, oLat: Double, oLng: Double) -> Unit,
    onOpenCustomCreate: (defaultTime: String) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MotoGoldSecondary)
                Column {
                    Text("📅 Planificar Actividad TX", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    Text("Fecha: $targetDate • Asignar Evento o Celebración", fontSize = 11.sp, color = MotoGoldSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SECCIÓN DIRECTIVA: SINCRONIZACIÓN Y COORDENADAS DEL MAPA TX
                if (isDirectiva) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, MotoGoldSecondary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🛡️", fontSize = 14.sp)
                                Text("Directiva: Sitio de Encuentro en Mapa", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MotoGoldSecondary)
                            }
                            Text(
                                text = "Busca la ubicación en el Mapa TX, copia las coordenadas y pégalas aquí:",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val coords = GestorPortapapeles.leerCoordenadaValida(context)
                                        if (coords != null) {
                                            val parts = coords.split(",")
                                            if (parts.size >= 2) {
                                                val lat = parts[0].trim().toDoubleOrNull() ?: 10.2319
                                                val lng = parts[1].trim().toDoubleOrNull() ?: -67.5744
                                                onSelectPreset(
                                                    "📍 Actividad Oficial TX - $targetDate",
                                                    "Encuentro en coordenadas fijadas por la Directiva desde Mapa TX ($coords).",
                                                    "Ruta Oficial",
                                                    "06:00 PM",
                                                    "06:30 PM",
                                                    "Punto de Encuentro Mapa ($coords)",
                                                    lat,
                                                    lng
                                                )
                                                Toast.makeText(context, "✅ Ubicación fijada desde Mapa: $coords", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val texto = GestorPortapapeles.leerTexto(context)
                                            if (texto.isNullOrBlank()) {
                                                Toast.makeText(context, "Portapapeles vacío. Copia coordenadas en Mapa TX primero.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No se detectaron coordenadas válidas en el texto copiado.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp), tint = MotoGoldSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📋 Pegar Coords", fontSize = 10.sp, color = MotoGoldSecondary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        PuenteMapa.mostrarUbicacionEnMapa(context, "10.2469,-67.5958", "Actividad TX")
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF38BDF8))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🗺️ Abrir Mapa TX", fontSize = 10.sp, color = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Selecciona una actividad para programar en este día:",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                // Opción 1: Cumpleaños Motero
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🎂 Cumpleaños Motero TX - $targetDate",
                                "¡Feliz Cumpleaños a nuestro hermano motero! Recordatorio y celebración en hermandad con el club.",
                                "Cumpleaños Motero",
                                "06:00 PM",
                                "06:30 PM",
                                "Sede TX / Local de Celebración",
                                10.2400,
                                -67.5800
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎂", fontSize = 24.sp)
                        Column {
                            Text("Cumpleaños Motero", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("6:00 PM • Homenaje, felicitaciones y aviso sincronizado", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 2: Bar & Encuentro Biker
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🍺 Bar & Encuentro Biker TX - $targetDate",
                                "Compartir al terminar la jornada, cerveza fría, buena música y hermandad motera entre hermanos.",
                                "Bar & Encuentro",
                                "06:30 PM",
                                "07:00 PM",
                                "Bar Biker Central",
                                10.2469,
                                -67.5958
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🍺", fontSize = 24.sp)
                        Column {
                            Text("Bar & Encuentro Biker", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("6:30 PM • Cerveza fría, música y hermandad motera", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 3: Mantenimiento Preventivo para Rodada
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🛠️ Mantenimiento Preventivo para Rodada - $targetDate",
                                "Revisión técnica de motos, ajuste y lubricación de cadenas, revisión de fluidos y puesta a punto.",
                                "Mantenimiento Preventivo",
                                "05:00 PM",
                                "05:30 PM",
                                "Taller Oficial Team TX",
                                10.2319,
                                -67.5744
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🛠️", fontSize = 24.sp)
                        Column {
                            Text("Mantenimiento Preventivo para Rodada", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("5:00 PM • Taller, lubricación y ajustes técnicos", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 4: Lavado de Moto en Familia
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🧼 Lavado de Moto en Familia TX - $targetDate",
                                "Jornada de limpieza, brillo y compartir con la familia y amigos del club.",
                                "Lavado de Moto en Familia",
                                "04:00 PM",
                                "04:30 PM",
                                "Sede Club / Autolavado",
                                10.2319,
                                -67.5744
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🧼", fontSize = 24.sp)
                        Column {
                            Text("Lavado de Moto en Familia", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("4:00 PM • Limpieza y brillo en familia", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 5: Obra Benéfica & Labor Social
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "❤️ Obra Benéfica & Labor Social TX - $targetDate",
                                "Jornada de apoyo comunitario, donaciones y labor social del club.",
                                "Obra Benéfica",
                                "03:00 PM",
                                "03:30 PM",
                                "Comunidad Solidaria",
                                10.2319,
                                -67.5744
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("❤️", fontSize = 24.sp)
                        Column {
                            Text("Obra Benéfica & Labor Social", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("3:00 PM • Solidaridad y apoyo comunitario", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 6: Café Biker & Hermandad
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, MotoGoldSecondary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "☕ Café Biker & Hermandad TX - $targetDate",
                                "Encuentro social para compartir un café, conversar y planificar actividades.",
                                "Evento Social",
                                "06:00 PM",
                                "06:30 PM",
                                "Cafetería Biker Central",
                                10.2400,
                                -67.5800
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("☕", fontSize = 24.sp)
                        Column {
                            Text("Café Biker & Hermandad", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("6:00 PM • Compartir un café y amistad biker", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 7: Nocturna Motera Urbana
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectPreset(
                                "🌙 Nocturna Motera Urbana TX - $targetDate",
                                "Recorrido nocturno por la ciudad con parada gastronómica y hermandad biker.",
                                "Ruta Oficial",
                                "07:00 PM",
                                "07:30 PM",
                                "Zona Gastronómica Las Delicias",
                                10.2469,
                                -67.5958
                            )
                        }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🌙", fontSize = 24.sp)
                        Column {
                            Text("Nocturna Motera Urbana", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("7:00 PM • Rodada urbana nocturna y comida", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }

                // Opción 8: Personalizado
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenCustomCreate("06:00 PM") }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("✏️", fontSize = 24.sp)
                        Column {
                            Text("Personalizado / Otra Actividad", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text("Abrir formulario completo para definir hora, lugar y flyer", fontSize = 10.sp, color = Color(0xFF90A4AE))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF90A4AE)) }
        },
        containerColor = Color(0xFF131722)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// TARJETA DE EVENTO MOTERO (BIKER EVENT CARD)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BikerEventCard(
    event: BikerCalendarEvent,
    currentMember: MemberProfile?,
    isDirectiva: Boolean,
    isCreator: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleRsvp: (Boolean, Boolean) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onPublishToFeed: () -> Unit,
    onOpenGps: (Double, Double, String) -> Unit,
    onShareWhatsApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = getCategoryColor(event.category, event.isOfficialClubEvent)
    val myIdStr = currentMember?.id?.toString() ?: ""
    val isMyRsvpAttending = myIdStr.isNotBlank() && event.rsvpPilotsList.split(",").map { it.trim() }.contains(myIdStr)
    val isMyRsvpPillion = isMyRsvpAttending && event.rsvpPillionsCount > 0
    val isMyReminderActive = myIdStr.isNotBlank() && event.remindedMemberIds.split(",").map { it.trim() }.contains(myIdStr)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202E)),
        border = BorderStroke(1.dp, if (event.isOfficialClubEvent) MotoGoldSecondary else catColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Category badge + Official badge + Actions (Edit/Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = catColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, catColor)
                    ) {
                        Text(
                            text = event.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (event.isOfficialClubEvent) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MotoGoldSecondary.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, MotoGoldSecondary)
                        ) {
                            Text(
                                text = "⭐ OFICIAL TX",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MotoGoldSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    if (isCreator || isDirectiva) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = TxFlameRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Flyer if present
            if (!event.flyerUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = event.flyerUrl,
                    contentDescription = event.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title
            Text(
                text = event.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            // Description
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Time details
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF131722),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(14.dp))
                            Text("Fecha: ${event.eventDate}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MotoGoldSecondary, modifier = Modifier.size(14.dp))
                            Text("Conc: ${event.eventTime} • Salida: ${event.departureTime}", fontSize = 11.sp, color = MotoGoldSecondary)
                        }
                    }

                    if (event.originAddress.isNotBlank() || event.destinationAddress.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFF263238), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                        if (event.originAddress.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(14.dp))
                                Text("Salida: ${event.originAddress}", fontSize = 10.sp, color = Color(0xFF90A4AE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (event.destinationAddress.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                                Text("Destino: ${event.destinationAddress}", fontSize = 10.sp, color = Color(0xFF90A4AE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // Route details (terrain, difficulty, captain, weather)
                    if (event.terrainType.isNotBlank() || event.difficultyLevel.isNotBlank() || event.roadCaptain.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFF263238), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🛣️ ${event.terrainType} • Dificultad: ${event.difficultyLevel}", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                            if (event.roadCaptain.isNotBlank()) {
                                Text("👨‍✈️ ${event.roadCaptain}", fontSize = 10.sp, color = MotoGoldSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RSVP & Attendance summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Text(
                        text = "${event.rsvpPilotsCount} Pilotos • ${event.rsvpPillionsCount} Copilotos",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }

                // Reminder toggle button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isMyReminderActive) MotoGoldSecondary.copy(alpha = 0.2f) else Color(0xFF263238),
                    border = BorderStroke(0.5.dp, if (isMyReminderActive) MotoGoldSecondary else Color.Transparent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleReminder(!isMyReminderActive) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isMyReminderActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = if (isMyReminderActive) MotoGoldSecondary else Color(0xFF90A4AE),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isMyReminderActive) "Avisar ✓" else "Recordar",
                            fontSize = 10.sp,
                            color = if (isMyReminderActive) MotoGoldSecondary else Color(0xFF90A4AE)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RSVP action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onToggleRsvp(true, false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMyRsvpAttending && !isMyRsvpPillion) Color(0xFF2E7D32) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text(
                        if (isMyRsvpAttending && !isMyRsvpPillion) "Asistiré ✓" else "Voy Solo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMyRsvpAttending && !isMyRsvpPillion) Color.White else Color(0xFFCBD5E1)
                    )
                }

                Button(
                    onClick = { onToggleRsvp(true, true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMyRsvpAttending && isMyRsvpPillion) Color(0xFF1565C0) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.2f).height(32.dp)
                ) {
                    Text(
                        if (isMyRsvpAttending && isMyRsvpPillion) "+ Copiloto ✓" else "+ Copiloto",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMyRsvpAttending && isMyRsvpPillion) Color.White else Color(0xFFCBD5E1)
                    )
                }

                if (isMyRsvpAttending) {
                    Button(
                        onClick = { onToggleRsvp(false, false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("No iré", fontSize = 10.sp, color = Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: GPS, WhatsApp, Publish to Muro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (event.originLatitude != 0.0 || event.destinationLatitude != 0.0) {
                    Button(
                        onClick = {
                            val lat = if (event.originLatitude != 0.0) event.originLatitude else event.destinationLatitude
                            val lng = if (event.originLongitude != 0.0) event.originLongitude else event.destinationLongitude
                            onOpenGps(lat, lng, event.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.2f).height(32.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Mapa TX", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🧭 Ir al Mapa", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compartir", fontSize = 10.sp, color = Color.White)
                }

                if (isDirectiva || isCreator) {
                    Button(
                        onClick = onPublishToFeed,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.2f).height(32.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Publicar", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Al Muro", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO CREAR / EDITAR EVENTO DEL CALENDARIO
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun CreateEditCalendarEventDialog(
    existingEvent: BikerCalendarEvent?,
    initialDate: String? = null,
    initialTime: String? = null,
    isDirectiva: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        desc: String,
        category: String,
        visibility: String,
        date: String,
        eventTime: String,
        depTime: String,
        origin: String,
        dest: String,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        terrain: String,
        difficulty: String,
        weather: String,
        captain: String,
        tail: String,
        remindDays: Int,
        isOfficial: Boolean,
        flyerUri: Uri?
    ) -> Unit
) {
    val context = LocalContext.current
    val defaultToday = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var category by remember { mutableStateOf(existingEvent?.category ?: "Ruta Oficial") }
    var visibility by remember { mutableStateOf(existingEvent?.visibility ?: "PUBLICO_CLUB") }
    var eventDate by remember { mutableStateOf(existingEvent?.eventDate ?: initialDate ?: defaultToday) }
    var eventTime by remember { mutableStateOf(existingEvent?.eventTime ?: initialTime ?: "07:00 AM") }
    var departureTime by remember { mutableStateOf(existingEvent?.departureTime ?: "07:30 AM") }
    var originAddress by remember { mutableStateOf(existingEvent?.originAddress ?: "") }
    var destinationAddress by remember { mutableStateOf(existingEvent?.destinationAddress ?: "") }
    var originLatStr by remember { mutableStateOf(existingEvent?.originLatitude?.toString() ?: "10.2319") }
    var originLngStr by remember { mutableStateOf(existingEvent?.originLongitude?.toString() ?: "-67.5744") }
    var terrainType by remember { mutableStateOf(existingEvent?.terrainType ?: "Asfalto") }
    var difficultyLevel by remember { mutableStateOf(existingEvent?.difficultyLevel ?: "Media") }
    var weatherForecast by remember { mutableStateOf(existingEvent?.weatherForecast ?: "Soleado 28°C") }
    var roadCaptain by remember { mutableStateOf(existingEvent?.roadCaptain ?: "") }
    var tailRider by remember { mutableStateOf(existingEvent?.tailRider ?: "") }
    var isOfficial by remember { mutableStateOf(existingEvent?.isOfficialClubEvent ?: isDirectiva) }
    var remindDaysStr by remember { mutableStateOf(existingEvent?.remindDaysBefore?.toString() ?: "1") }
    var selectedFlyerUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFlyerUri = uri
    }

    val categories = listOf(
        "Ruta Oficial",
        "Rodada Fin de Semana",
        "Nocturna Motera",
        "Mantenimiento Preventivo",
        "Lavado de Moto en Familia",
        "Obra Benéfica",
        "Bar & Encuentro",
        "Cumpleaños Motero",
        "Evento Social",
        "Vencimiento Trámites",
        "Reunión Directiva",
        "Personal"
    )
    val terrains = listOf("Asfalto", "Tierra", "Mixto", "Montaña", "Costa")
    val difficulties = listOf("Fácil", "Media", "Avanzada", "Extrema")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingEvent != null) "Editar Evento" else "Agendar Evento / Rodada",
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del Evento / Rodada") },
                    placeholder = { Text("Ej: Rodada Costera a Cuyagua") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Categoría con colores distintivos
                Text("Categoría:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(categories) { cat ->
                        val catColor = getCategoryColor(cat)
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / Detalles") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = { Text("Fecha (DD/MM/AAAA)") },
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = remindDaysStr,
                        onValueChange = { remindDaysStr = it },
                        label = { Text("Avisar (Días)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = eventTime,
                        onValueChange = { eventTime = it },
                        label = { Text("Concentración") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = departureTime,
                        onValueChange = { departureTime = it },
                        label = { Text("Ruedas Asfalto") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = originAddress,
                    onValueChange = { originAddress = it },
                    label = { Text("Punto de Encuentro / Salida") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Botones de Mapa TX y Portapapeles
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val coords = GestorPortapapeles.leerCoordenadaValida(context)
                            if (coords != null) {
                                val parts = coords.split(",")
                                if (parts.size >= 2) {
                                    originLatStr = parts[0].trim()
                                    originLngStr = parts[1].trim()
                                    if (originAddress.isBlank()) {
                                        originAddress = "Punto en Mapa ($coords)"
                                    }
                                    Toast.makeText(context, "✅ Coordenadas fijadas: $coords", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val texto = GestorPortapapeles.leerTexto(context)
                                if (texto.isNullOrBlank()) {
                                    Toast.makeText(context, "Portapapeles vacío. Copia coordenadas en Mapa TX.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No se encontraron coordenadas en el texto copiado.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp), tint = MotoGoldSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📋 Pegar Coords", fontSize = 10.sp, color = MotoGoldSecondary)
                    }

                    OutlinedButton(
                        onClick = {
                            val lat = originLatStr.toDoubleOrNull() ?: 10.2319
                            val lng = originLngStr.toDoubleOrNull() ?: -67.5744
                            PuenteMapa.mostrarUbicacionEnMapa(context, "$lat,$lng", title.ifBlank { "Punto de Encuentro" })
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🗺️ Ver en Mapa TX", fontSize = 10.sp, color = Color(0xFF38BDF8))
                    }
                }

                if (originLatStr != "0.0" && originLngStr != "0.0" && originLatStr.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            Text("📍 Coordenadas Salida: $originLatStr, $originLngStr", fontSize = 10.sp, color = Color(0xFFBAE6FD))
                        }
                    }
                }

                OutlinedTextField(
                    value = destinationAddress,
                    onValueChange = { destinationAddress = it },
                    label = { Text("Destino Final") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Terreno y Dificultad
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = terrainType,
                        onValueChange = { terrainType = it },
                        label = { Text("Terreno") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = difficultyLevel,
                        onValueChange = { difficultyLevel = it },
                        label = { Text("Dificultad") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Clima
                OutlinedTextField(
                    value = weatherForecast,
                    onValueChange = { weatherForecast = it },
                    label = { Text("Pronóstico Clima Estimado") },
                    placeholder = { Text("Ej: Soleado 29°C • 10% Lluvia") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Roles de caravana
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = roadCaptain,
                        onValueChange = { roadCaptain = it },
                        label = { Text("Capitán de Ruta") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tailRider,
                        onValueChange = { tailRider = it },
                        label = { Text("Barredora (Cierre)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isDirectiva) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isOfficial,
                            onCheckedChange = { isOfficial = it },
                            colors = CheckboxDefaults.colors(checkedColor = MotoGoldSecondary)
                        )
                        Text("Evento Oficial del Club (Publicar a toda la comunidad)", fontSize = 12.sp, color = Color.White)
                    }
                }

                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2F3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedFlyerUri != null) "Flyer seleccionado ✓" else "Subir Flyer / Imagen")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val oLat = originLatStr.toDoubleOrNull() ?: 10.2319
                        val oLng = originLngStr.toDoubleOrNull() ?: -67.5744
                        val rDays = remindDaysStr.toIntOrNull() ?: 1
                        onSave(title, description, category, visibility, eventDate, eventTime, departureTime, originAddress, destinationAddress, oLat, oLng, 0.0, 0.0, terrainType, difficultyLevel, weatherForecast, roadCaptain, tailRider, rDays, isOfficial, selectedFlyerUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
            ) {
                Text("Guardar Evento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF90A4AE)) }
        },
        containerColor = Color(0xFF1E2433)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// ACCIONES: MAPA GPS Y WHATSAPP
// ═══════════════════════════════════════════════════════════════════════

fun openGpsLocation(context: Context, lat: Double, lng: Double, label: String) {
    if (lat != 0.0 || lng != 0.0) {
        PuenteMapa.mostrarUbicacionEnMapa(context, "$lat,$lng", label)
    } else {
        Toast.makeText(context, "No hay coordenadas disponibles para este evento", Toast.LENGTH_SHORT).show()
    }
}

fun shareCalendarEventViaWhatsApp(context: Context, event: BikerCalendarEvent) {
    val shareText = buildString {
        appendLine("📅🏍️ EVENTO CALENDARIO MOTERO TX 🏍️📅")
        appendLine("✨ ${event.title} ✨")
        appendLine()
        appendLine("📆 Fecha: ${event.eventDate}")
        appendLine("⏰ Hora de Concentración: ${event.eventTime}")
        appendLine("🚀 Ruedas en el Asfalto: ${event.departureTime}")
        if (event.originAddress.isNotBlank()) appendLine("📍 Salida: ${event.originAddress}")
        if (event.destinationAddress.isNotBlank()) appendLine("🗺️ Destino: ${event.destinationAddress}")
        appendLine("🛣️ Terreno: ${event.terrainType} • Dificultad: ${event.difficultyLevel}")
        if (event.weatherForecast.isNotBlank()) appendLine("☀️ Clima: ${event.weatherForecast}")
        if (event.roadCaptain.isNotBlank()) appendLine("👨‍✈️ Capitán de Ruta: ${event.roadCaptain}")
        if (event.tailRider.isNotBlank()) appendLine("🛡️ Barredora: ${event.tailRider}")
        appendLine("👥 Confirmados: ${event.rsvpPilotsCount} Pilotos")
        appendLine()
        appendLine("📝 ${event.description}")
        appendLine()
        appendLine("¡Únete en la App Oficial Team TX Venezuela!")
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            setPackage("com.whatsapp")
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        try {
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(generalIntent, "Compartir Evento"))
        } catch (ex: Exception) {
            Toast.makeText(context, "No se pudo compartir", Toast.LENGTH_SHORT).show()
        }
    }
}
