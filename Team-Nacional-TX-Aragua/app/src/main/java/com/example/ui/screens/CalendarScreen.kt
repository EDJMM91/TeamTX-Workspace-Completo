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
import androidx.compose.foundation.clickable
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
import java.text.SimpleDateFormat
import java.util.*

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDirectiva = isDirectivaMode || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true) || (currentMember?.role == MemberRole.PRESIDENTE) || (currentMember?.role == MemberRole.DIRECTIVA)

    var currentCalendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDateStr by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var selectedFilterCategory by remember { mutableStateOf("TODOS") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<BikerCalendarEvent?>(null) }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }
    val dayMonthYearFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Generar días del mes actual para la cuadrícula
    val daysInMonth = remember(currentCalendarMonth) {
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
            daysList.add(CalendarDayInfo(day, dateStr, true))
        }
        daysList
    }

    // Filtrar eventos
    val filteredEvents = remember(events, selectedFilterCategory, selectedDateStr) {
        events.filter { ev ->
            val matchCategory = when (selectedFilterCategory) {
                "TODOS" -> true
                "RUTAS" -> ev.category.contains("Ruta", ignoreCase = true) || ev.category.contains("Oficial", ignoreCase = true)
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
                onClick = { showCreateDialog = true },
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
                        IconButton(onClick = {
                            val newCal = currentCalendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentCalendarMonth = newCal
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = MotoOrangePrimary)
                        }

                        Text(
                            text = monthFormat.format(currentCalendarMonth.time).replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.White
                        )

                        IconButton(onClick = {
                            val newCal = currentCalendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentCalendarMonth = newCal
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = MotoOrangePrimary)
                        }
                    }

                    // Días de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val weekDays = listOf("DOM", "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB")
                        weekDays.forEach { wDay ->
                            Text(
                                text = wDay,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (wDay == "DOM" || wDay == "SÁB") MotoOrangePrimary else Color(0xFF90A4AE),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // CUADRÍCULA DE DÍAS DEL MES
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp),
                        userScrollEnabled = false
                    ) {
                        items(daysInMonth) { dayInfo ->
                            if (dayInfo.isCurrentMonth) {
                                val isSelected = selectedDateStr == dayInfo.dateString
                                val dayEvents = events.filter { it.eventDate == dayInfo.dateString }
                                val hasOfficial = dayEvents.any { it.isOfficialClubEvent }
                                val hasMaintenance = dayEvents.any { it.category.contains("Mantenimiento", ignoreCase = true) }

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1.2f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> MotoOrangePrimary.copy(alpha = 0.35f)
                                                dayEvents.isNotEmpty() -> Color(0xFF263238)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            selectedDateStr = dayInfo.dateString
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${dayInfo.dayNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || dayEvents.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MotoGoldSecondary else Color.White
                                        )
                                        if (dayEvents.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                if (hasOfficial) {
                                                    Box(modifier = Modifier.size(4.dp).background(TxFlameRed, CircleShape))
                                                }
                                                if (hasMaintenance) {
                                                    Box(modifier = Modifier.size(4.dp).background(Color(0xFF25D366), CircleShape))
                                                }
                                                if (!hasOfficial && !hasMaintenance) {
                                                    Box(modifier = Modifier.size(4.dp).background(MotoGoldSecondary, CircleShape))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.aspectRatio(1.2f))
                            }
                        }
                    }
                }
            }

            // CHIPS DE FILTRO DE CATEGORÍAS
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
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

            // LISTA DE EVENTOS PROGRAMADOS
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFF607D8B), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No hay eventos programados en esta vista", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Presiona el botón '+' para agendar una nueva rodada, mantenimiento o reunión.", color = Color(0xFF90A4AE), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEvents, key = { it.id }) { event ->
                        BikerEventCard(
                            event = event,
                            currentMember = currentMember,
                            isDirectiva = isDirectiva,
                            isCreator = event.creatorMemberId == currentMember?.id,
                            onEdit = { eventToEdit = event },
                            onDelete = { onDeleteEvent(event.id) },
                            onToggleRsvp = { isAttending, hasPillion ->
                                onToggleRsvp(event, isAttending, hasPillion)
                            },
                            onToggleReminder = { notifyInApp ->
                                onToggleReminder(event, notifyInApp)
                            },
                            onPublishToFeed = {
                                onPublishToFeed(event) { success ->
                                    if (success) Toast.makeText(context, "Evento publicado en el Muro Oficial", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenGps = { lat, lng, label ->
                                openGpsLocation(context, lat, lng, label)
                            },
                            onShareWhatsApp = {
                                shareCalendarEventViaWhatsApp(context, event)
                            }
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGO CREAR / EDITAR EVENTO
    if (showCreateDialog || eventToEdit != null) {
        CreateEditCalendarEventDialog(
            existingEvent = eventToEdit,
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

data class CalendarDayInfo(
    val dayNumber: Int,
    val dateString: String,
    val isCurrentMonth: Boolean
)

// ═══════════════════════════════════════════════════════════════════════
// TARJETA DE EVENTO DEL CALENDARIO MOTERO
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
    val myMemberIdStr = currentMember?.id?.toString() ?: ""
    val isAttending = remember(event.rsvpPilotsList, myMemberIdStr) {
        myMemberIdStr.isNotBlank() && event.rsvpPilotsList.split(",").contains(myMemberIdStr)
    }
    val isReminded = remember(event.remindedMemberIds, myMemberIdStr) {
        myMemberIdStr.isNotBlank() && event.remindedMemberIds.split(",").contains(myMemberIdStr)
    }

    Surface(
        color = Color(0xFF1E2433),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (event.isOfficialClubEvent) MotoGoldSecondary.copy(alpha = 0.4f) else Color(0xFF37474F)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Categoría + Tipo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (event.isOfficialClubEvent) MotoGoldSecondary.copy(alpha = 0.2f) else MotoOrangePrimary.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, if (event.isOfficialClubEvent) MotoGoldSecondary else MotoOrangePrimary)
                    ) {
                        Text(
                            text = if (event.isOfficialClubEvent) "👑 ${event.category.uppercase()}" else "🏍️ ${event.category.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (event.isOfficialClubEvent) MotoGoldSecondary else MotoOrangePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (event.difficultyLevel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF263238),
                            border = BorderStroke(0.5.dp, Color(0xFF546E7A))
                        ) {
                            Text(
                                text = "Nivel: ${event.difficultyLevel}",
                                fontSize = 9.sp,
                                color = Color(0xFFCFD8DC),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (isDirectiva || isCreator) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF90A4AE), modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusError, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Título
            Text(
                text = event.title,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color.White
            )

            // Flyer si existe
            if (!event.flyerUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                SubcomposeAsyncImage(
                    model = event.flyerUrl,
                    contentDescription = "Flyer",
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = event.description,
                fontSize = 12.sp,
                color = Color(0xFFB0BEC5),
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cuadro de Logística: Horarios, Ruta, Clima
            Surface(
                color = Color(0xFF131722),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📅 Fecha:", fontSize = 11.sp, color = Color(0xFF78909C))
                        Text(event.eventDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("⏰ Horario:", fontSize = 11.sp, color = Color(0xFF78909C))
                        Text("Conc. ${event.eventTime} • Ruedas ${event.departureTime}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                    }
                    if (event.originAddress.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🚀 Salida:", fontSize = 11.sp, color = Color(0xFF78909C))
                            Text(event.originAddress, fontSize = 11.sp, color = Color(0xFFECEFF1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (event.destinationAddress.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🗺️ Destino:", fontSize = 11.sp, color = Color(0xFF78909C))
                            Text(event.destinationAddress, fontSize = 11.sp, color = MotoOrangePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (event.weatherForecast.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("☀️ Pronóstico Clima:", fontSize = 11.sp, color = Color(0xFF78909C))
                            Text(event.weatherForecast, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81D4FA))
                        }
                    }
                    if (event.roadCaptain.isNotBlank() || event.tailRider.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🛡️ Caravana:", fontSize = 11.sp, color = Color(0xFF78909C))
                            Text("Capitán: ${event.roadCaptain.ifBlank { "N/A" }} | Cierre: ${event.tailRider.ifBlank { "N/A" }}", fontSize = 10.sp, color = Color(0xFFCFD8DC))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RSVP Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 Confirmados: ${event.rsvpPilotsCount} Pilotos • ${event.rsvpPillionsCount} Copilotos",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MotoOrangePrimary
                )

                // Botón Recordatorio en Avisos
                IconButton(
                    onClick = { onToggleReminder(!isReminded) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isReminded) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Recordatorio en Avisos",
                        tint = if (isReminded) MotoGoldSecondary else Color(0xFF90A4AE),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Confirmar Asistencia RSVP
                Button(
                    onClick = { onToggleRsvp(!isAttending, false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAttending) Color(0xFF2E7D32) else Color(0xFF263238)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(if (isAttending) Icons.Default.CheckCircle else Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAttending) "Asistiré ✓" else "Confirmar RSVP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Abrir GPS
                if (event.originLatitude != 0.0 || event.destinationLatitude != 0.0) {
                    IconButton(
                        onClick = {
                            val lat = if (event.originLatitude != 0.0) event.originLatitude else event.destinationLatitude
                            val lng = if (event.originLongitude != 0.0) event.originLongitude else event.destinationLongitude
                            onOpenGps(lat, lng, event.title)
                        },
                        modifier = Modifier.size(38.dp).background(Color(0xFF263238), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "GPS / Maps", tint = Color(0xFF4FC3F7), modifier = Modifier.size(18.dp))
                    }
                }

                // Publicar en Muro (Directiva)
                if (isDirectiva && event.isOfficialClubEvent) {
                    IconButton(
                        onClick = onPublishToFeed,
                        modifier = Modifier.size(38.dp).background(Color(0xFF263238), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Publicar en Muro", tint = MotoGoldSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Compartir WhatsApp
                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var category by remember { mutableStateOf(existingEvent?.category ?: "Ruta Oficial") }
    var visibility by remember { mutableStateOf(existingEvent?.visibility ?: "PUBLICO_CLUB") }
    var eventDate by remember { mutableStateOf(existingEvent?.eventDate ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var eventTime by remember { mutableStateOf(existingEvent?.eventTime ?: "07:00 AM") }
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

    val categories = listOf("Ruta Oficial", "Evento Social", "Mantenimiento / Garaje", "Vencimiento Trámites", "Reunión Directiva", "Personal")
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

                // Categoría
                Text("Categoría:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
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
    try {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
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
