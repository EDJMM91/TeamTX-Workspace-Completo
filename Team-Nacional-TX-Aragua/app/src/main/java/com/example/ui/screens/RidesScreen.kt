package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.openUrl
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidesScreen(
    rides: List<RideEvent>,
    registrations: List<RideRegistration>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    onJoinRide: (rideId: Long, hasPillion: Boolean, pillionName: String) -> Unit,
    onCancelJoin: (rideId: Long) -> Unit,
    onUpdateRideStatus: (RideEvent, RideStatus) -> Unit,
    onUpdateRide: (RideEvent) -> Unit = {},
    onDeleteRide: (Long) -> Unit = {},
    onFinalizeRide: (RideEvent, List<Long>) -> Unit = { _, _ -> },
    onUpdateConvoyRole: (registrationId: Long, newRole: ConvoyRoleType) -> Unit = { _, _ -> },
    onCreateRide: (
        title: String,
        description: String,
        origin: String,
        destination: String,
        departureDate: String,
        meetingTime: String,
        distanceKm: Int,
        terrainType: String,
        convoyLeader: String,
        tailRider: String,
        gasStops: String,
        requiredGear: String,
        costUsd: Double,
        maxParticipants: Int,
        whatsappLink: String?
    ) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedRideForEdit by remember { mutableStateOf<RideEvent?>(null) }
    var selectedRideForDelete by remember { mutableStateOf<RideEvent?>(null) }
    var selectedRideForRoster by remember { mutableStateOf<RideEvent?>(null) }
    var selectedRideForJoin by remember { mutableStateOf<RideEvent?>(null) }
    var selectedRideForPostpone by remember { mutableStateOf<RideEvent?>(null) }
    var selectedRideForFinalize by remember { mutableStateOf<RideEvent?>(null) }

    var selectedStatusFilter by remember { mutableStateOf("TODAS") }
    var searchQuery by remember { mutableStateOf("") }

    val statusFilters = listOf("TODAS", "Programadas", "En Ruta", "Pospuestas", "Archivadas")

    val filteredRides = remember(rides, selectedStatusFilter, searchQuery) {
        rides.filter { ride ->
            val matchStatus = when (selectedStatusFilter) {
                "Programadas" -> ride.status == RideStatus.PROGRAMADA && !ride.departureDate.contains("TBD", ignoreCase = true)
                "En Ruta" -> ride.status == RideStatus.EN_CURSO
                "Pospuestas" -> ride.departureDate.contains("TBD", ignoreCase = true) || ride.departureDate.contains("DEFINIR", ignoreCase = true) || ride.status == RideStatus.CANCELADA
                "Archivadas" -> ride.status == RideStatus.FINALIZADA
                else -> true
            }
            val matchQuery = searchQuery.isBlank() ||
                    ride.title.contains(searchQuery, ignoreCase = true) ||
                    ride.destinationCity.contains(searchQuery, ignoreCase = true) ||
                    ride.originCity.contains(searchQuery, ignoreCase = true) ||
                    ride.convoyLeader.contains(searchQuery, ignoreCase = true)

            matchStatus && matchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rodadas y Caravanas TX",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Convoy oficial, logística y cupos",
                            fontSize = 11.sp,
                            color = MotoOrangePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Icon(Icons.Default.Home, contentDescription = "Inicio", tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inicio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (isDirectivaMode) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.AddRoad, contentDescription = null) },
                    text = { Text("Programar Rodada", fontWeight = FontWeight.Bold) },
                    containerColor = MotoOrangePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_create_ride")
                )
            }
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por destino, ruta o capitán...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MotoOrangePrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color(0xFF64748B))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = MotoOrangePrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(statusFilters) { filter ->
                            val isSelected = selectedStatusFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatusFilter = filter },
                                label = {
                                    Text(
                                        filter,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MotoOrangePrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MotoOrangePrimary else Color(0xFFCBD5E1))
                            )
                        }
                    }
                }
            }

            if (filteredRides.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = TxSteelSilver, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay rodadas que coincidan con los filtros",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredRides, key = { it.id }) { ride ->
                    val isRegistered = registrations.any { it.rideId == ride.id && it.memberId == currentMember?.id }
                    val rideRoster = registrations.filter { it.rideId == ride.id }

                    RideItemCard(
                        ride = ride,
                        isRegistered = isRegistered,
                        registeredCount = rideRoster.size,
                        isDirectivaMode = isDirectivaMode,
                        onJoinClick = { selectedRideForJoin = ride },
                        onCancelClick = { onCancelJoin(ride.id) },
                        onViewRoster = { selectedRideForRoster = ride },
                        onStatusChange = { newStatus -> onUpdateRideStatus(ride, newStatus) },
                        onEditClick = { selectedRideForEdit = ride },
                        onDeleteClick = { selectedRideForDelete = ride },
                        onPostponeClick = { selectedRideForPostpone = ride },
                        onFinalizeClick = { selectedRideForFinalize = ride }
                    )
                }
            }
        }
    }

    // Modal: Programar Rodada
    if (showCreateDialog) {
        CreateRideDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, orig, dest, date, time, km, terrain, leader, tail, gas, gear, cost, max, wa ->
                onCreateRide(title, desc, orig, dest, date, time, km, terrain, leader, tail, gas, gear, cost, max, wa)
                showCreateDialog = false
            }
        )
    }

    // Modal: Editar Rodada
    if (selectedRideForEdit != null) {
        EditRideDialog(
            ride = selectedRideForEdit!!,
            onDismiss = { selectedRideForEdit = null },
            onConfirmEdit = { updatedRide ->
                onUpdateRide(updatedRide)
                selectedRideForEdit = null
            }
        )
    }

    // Modal: Posponer Rodada
    if (selectedRideForPostpone != null) {
        PostponeRideDialog(
            ride = selectedRideForPostpone!!,
            onDismiss = { selectedRideForPostpone = null },
            onConfirmPostpone = { updatedRide ->
                onUpdateRide(updatedRide)
                selectedRideForPostpone = null
            }
        )
    }

    // Modal: Finalizar y Archivar con Registro Oficial de Asistencia
    if (selectedRideForFinalize != null) {
        FinalizeAndArchiveRideDialog(
            ride = selectedRideForFinalize!!,
            roster = registrations.filter { it.rideId == selectedRideForFinalize!!.id },
            onDismiss = { selectedRideForFinalize = null },
            onConfirmFinalize = { attendedMemberIds ->
                onFinalizeRide(selectedRideForFinalize!!, attendedMemberIds)
                selectedRideForFinalize = null
            }
        )
    }

    // Modal: Confirmar Eliminación
    if (selectedRideForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedRideForDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar Rodada", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar permanentemente la rodada \"${selectedRideForDelete!!.title}\"? Se anularán todas las inscripciones registradas.",
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRide(selectedRideForDelete!!.id)
                        selectedRideForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRideForDelete = null }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // Modal: Inscripción
    if (selectedRideForJoin != null) {
        JoinRideDialog(
            ride = selectedRideForJoin!!,
            currentMember = currentMember,
            onDismiss = { selectedRideForJoin = null },
            onConfirmJoin = { hasPillion, pillionName ->
                onJoinRide(selectedRideForJoin!!.id, hasPillion, pillionName)
                selectedRideForJoin = null
            }
        )
    }

    // Modal: Lista de Puestos / Convocatoria
    if (selectedRideForRoster != null) {
        RideRosterDialog(
            ride = selectedRideForRoster!!,
            roster = registrations.filter { it.rideId == selectedRideForRoster!!.id },
            isDirectivaMode = isDirectivaMode,
            onUpdateConvoyRole = onUpdateConvoyRole,
            onDismiss = { selectedRideForRoster = null }
        )
    }
}

@Composable
fun RideItemCard(
    ride: RideEvent,
    isRegistered: Boolean,
    registeredCount: Int,
    isDirectivaMode: Boolean,
    onJoinClick: () -> Unit,
    onCancelClick: () -> Unit,
    onViewRoster: () -> Unit,
    onStatusChange: (RideStatus) -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onPostponeClick: () -> Unit = {},
    onFinalizeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expandedDetails by remember { mutableStateOf(false) }
    var showDirectivaMenu by remember { mutableStateOf(false) }

    val isPostponed = ride.departureDate.contains("TBD", ignoreCase = true) || ride.departureDate.contains("DEFINIR", ignoreCase = true)
    val isArchived = ride.status == RideStatus.FINALIZADA

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (isArchived) Color(0xFFD97706) else if (isPostponed) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ride_card_${ride.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Banner with Status & Distance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (isArchived) listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
                            else listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))
                        )
                    )
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    val statusText = when {
                        isArchived -> "ARCHIVADA / COMPLETADA 🎖️"
                        isPostponed -> "POSPUESTA (POR DEFINIR) ⏸️"
                        ride.status == RideStatus.EN_CURSO -> "EN RUTA (EN VIVO) 🏍️"
                        else -> "PROGRAMADA 📅"
                    }
                    val statusColor = when {
                        isArchived -> Color(0xFFD97706)
                        isPostponed -> Color(0xFFB45309)
                        ride.status == RideStatus.EN_CURSO -> Color(0xFFEA580C)
                        else -> Color(0xFF0284C7)
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${ride.originCity} ➔ ${ride.destinationCity}",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MotoOrangePrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${ride.distanceKm} KM",
                                color = Color(0xFFC2410C),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ruta",
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Directiva Options Button (Edit / Postpone / Finalize / Delete)
                    if (isDirectivaMode) {
                        Box {
                            IconButton(
                                onClick = { showDirectivaMenu = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones Directiva", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = showDirectivaMenu,
                                onDismissRequest = { showDirectivaMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Editar Rodada", color = Color(0xFF0F172A))
                                        }
                                    },
                                    onClick = {
                                        showDirectivaMenu = false
                                        onEditClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Update, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Posponer Rodada", color = Color(0xFFD97706))
                                        }
                                    },
                                    onClick = {
                                        showDirectivaMenu = false
                                        onPostponeClick()
                                    }
                                )
                                if (!isArchived) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Finalizar y Archivar", color = Color(0xFF16A34A))
                                            }
                                        },
                                        onClick = {
                                            showDirectivaMenu = false
                                            onFinalizeClick()
                                        }
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Eliminar Rodada", color = Color(0xFFDC2626))
                                        }
                                    },
                                    onClick = {
                                        showDirectivaMenu = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Body info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = ride.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ride.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569),
                    maxLines = if (expandedDetails) 10 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Logistics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = if (isPostponed) Color(0xFFD97706) else MotoOrangePrimary, modifier = Modifier.size(16.dp))
                        Text(ride.departureDate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isPostponed) Color(0xFFD97706) else Color(0xFF0F172A))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                        Text(ride.meetingTime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Convoy Leaders & Tail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("🎖️ CAPITÁN / LÍDER", fontSize = 9.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.Bold)
                        Text(ride.convoyLeader, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("🧹 ESCOBA / BARREDOR", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(ride.tailRider, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    }
                }

                // Convoy Staff Bar: Seguridad, Médico, Mecánico
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🛡️ Seguridad: ${ride.roadSafetyOfficer.ifBlank { "Oficial Vial TX" }}", fontSize = 10.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                            Text("🩺 Médico: ${ride.medicOfficer.ifBlank { "Médico de Ruta TX" }}", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🔧 Mecánico: ${ride.mechanicOfficer.ifBlank { "Mecánico Oficial TX" }}", fontSize = 10.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.Medium)
                            Text("🧹 Barredor: ${ride.tailRider.ifBlank { "Jefe de Cola TX" }}", fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Collapsible detailed logistics
                AnimatedVisibility(visible = expandedDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("⛽ Paradas de Gasolina:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFEA580C))
                        Text(ride.gasStops, fontSize = 12.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(6.dp))

                        Text("🛡️ Indumentaria Obligatoria:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFEA580C))
                        Text(ride.requiredGear, fontSize = 12.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(6.dp))

                        Text("⛰️ Tipo de Terreno:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFEA580C))
                        Text(ride.terrainType, fontSize = 12.sp, color = Color(0xFF0F172A))

                        if (ride.costUsd > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("💵 Cuota / Pote sugerido:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF16A34A))
                            Text("$${ride.costUsd} USD (~${ride.costVes} Bs)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Toggle expand text
                TextButton(
                    onClick = { expandedDetails = !expandedDetails },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (expandedDetails) "Ocultar detalles ▲" else "Ver ruta, paradas y equipo requerido ▼",
                        fontSize = 11.sp,
                        color = MotoOrangePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // FILA 1 DE BOTONES: PUESTOS CONVOY & INSCRIPCIÓN (Sin choques)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Registered count / Roster trigger
                    OutlinedButton(
                        onClick = onViewRoster,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("btn_roster_${ride.id}")
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp), tint = MotoOrangePrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$registeredCount/${ride.maxParticipants} Motos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    // Join / Cancel / Finalized Badge
                    if (isArchived) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text(
                                    text = "Archivada ✓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    } else if (isRegistered) {
                        Button(
                            onClick = onCancelClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("btn_cancel_join_${ride.id}")
                        ) {
                            Text("Inscrito ✓ (Anular)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    } else {
                        Button(
                            onClick = onJoinClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("btn_join_ride_${ride.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inscribirme", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FILA 2 DE BOTONES: ACCIONES TÁCTICAS (DUAL MAPS Y WHATSAPP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mapa TX Interno
                    Button(
                        onClick = {
                            com.example.mapa.PuenteMapa.mostrarUbicacionEnMapa(
                                context,
                                "10.2319,-67.5744",
                                "${ride.title} - ${ride.destinationCity}"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = "Mapa TX", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mapa TX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Google Maps Externo
                    Button(
                        onClick = {
                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode("${ride.destinationCity}, Venezuela")}"))
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode("${ride.destinationCity}, Venezuela")}")))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "G. Maps", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("G. Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // WhatsApp Group
                    if (!ride.whatsappGroupUrl.isNullOrBlank()) {
                        Button(
                            onClick = { openUrl(context, ride.whatsappGroupUrl!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // FILA 3: DIRECTIVA ACCIONES RÁPIDAS
                if (isDirectivaMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Directiva:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = onPostponeClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7)),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFB45309))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Posponer", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                }

                                if (!isArchived) {
                                    Button(
                                        onClick = onFinalizeClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCFCE7)),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF15803D))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Finalizar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
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

@Composable
fun PostponeRideDialog(
    ride: RideEvent,
    onDismiss: () -> Unit,
    onConfirmPostpone: (RideEvent) -> Unit
) {
    var isIndefinite by remember { mutableStateOf(false) }
    var newDate by remember { mutableStateOf(if (ride.departureDate.contains("TBD")) "Sábado 12 Septiembre 2026" else ride.departureDate) }
    var newTime by remember { mutableStateOf(ride.meetingTime) }
    var reason by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF1E293B),
        focusedBorderColor = MotoOrangePrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF8FAFC),
        cursorColor = MotoOrangePrimary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Update, contentDescription = null, tint = Color(0xFFD97706))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Posponer Rodada TX", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecciona la modalidad para posponer \"${ride.title}\":",
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                // Option 1: Indefinite / TBD
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isIndefinite) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isIndefinite) Color(0xFFF59E0B) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isIndefinite = true }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isIndefinite,
                            onClick = { isIndefinite = true },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD97706))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Fecha Indefinida (Por Definir / TBD)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("Se mantendrá en pausa hasta nuevo aviso de Directiva", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Option 2: New Date and Time
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (!isIndefinite) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (!isIndefinite) MotoOrangePrimary else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isIndefinite = false }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isIndefinite,
                            onClick = { isIndefinite = false },
                            colors = RadioButtonDefaults.colors(selectedColor = MotoOrangePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Asignar Nueva Fecha y Hora", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("Reprogramar con fecha exacta de salida", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                if (!isIndefinite) {
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("Nueva Fecha de Salida") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        label = { Text("Nueva Hora de Encuentro") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo del aplazamiento (Opcional)") },
                    placeholder = { Text("ej. Mal clima, permisos de vialidad...") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = if (isIndefinite) {
                        ride.copy(
                            departureDate = "POR DEFINIR (TBD)",
                            meetingTime = "Por Coordinar",
                            status = RideStatus.PROGRAMADA
                        )
                    } else {
                        ride.copy(
                            departureDate = newDate,
                            meetingTime = newTime,
                            status = RideStatus.PROGRAMADA
                        )
                    }
                    onConfirmPostpone(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("Confirmar Aplazamiento", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

@Composable
fun FinalizeAndArchiveRideDialog(
    ride: RideEvent,
    roster: List<RideRegistration>,
    onDismiss: () -> Unit,
    onConfirmFinalize: (List<Long>) -> Unit
) {
    val attendedMemberIds = remember { mutableStateListOf<Long>().apply {
        // Pre-select all registered members by default
        addAll(roster.map { it.memberId })
    } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finalizar y Archivar Rodada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Confirma la asistencia oficial de los pilotos para \"${ride.title}\". Se acreditará la salida a su récord de membresía y la rodada quedará archivada.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pilotos Inscritos (${attendedMemberIds.size}/${roster.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                    TextButton(
                        onClick = {
                            if (attendedMemberIds.size == roster.size) attendedMemberIds.clear()
                            else {
                                attendedMemberIds.clear()
                                attendedMemberIds.addAll(roster.map { it.memberId })
                            }
                        }
                    ) {
                        Text(if (attendedMemberIds.size == roster.size) "Desmarcar Todos" else "Marcar Todos", fontSize = 11.sp, color = MotoOrangePrimary)
                    }
                }

                if (roster.isEmpty()) {
                    Text("No hubo inscripciones registradas en la app para esta rodada.", fontSize = 11.sp, color = Color(0xFF64748B))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(roster, key = { it.id }) { reg ->
                            val isChecked = attendedMemberIds.contains(reg.memberId)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChecked) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isChecked) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) attendedMemberIds.remove(reg.memberId)
                                        else attendedMemberIds.add(reg.memberId)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${reg.memberAlias.ifBlank { reg.memberName }} (${reg.memberName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Placa: ${reg.bikePlate}${if (reg.hasPillion) " • Copiloto: ${reg.pillionName}" else ""}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) attendedMemberIds.add(reg.memberId)
                                            else attendedMemberIds.remove(reg.memberId)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF16A34A))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmFinalize(attendedMemberIds.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("Archivar y Acreditar Asistencia 🏆", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

@Composable
fun JoinRideDialog(
    ride: RideEvent,
    currentMember: MemberProfile?,
    onDismiss: () -> Unit,
    onConfirmJoin: (hasPillion: Boolean, pillionName: String) -> Unit
) {
    var hasPillion by remember { mutableStateOf(false) }
    var pillionName by remember { mutableStateOf("") }

    val isSuspended = currentMember?.isSuspended == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MotoOrangePrimary)
                Text("Inscripción a Rodada TX", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isSuspended) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("🚫 MIEMBRO SUSPENDIDO", fontWeight = FontWeight.Black, color = Color(0xFFDC2626), fontSize = 13.sp)
                            Text(
                                "No puedes inscribirte a rodadas oficiales mientras tu sanción disciplinaria esté activa.",
                                color = Color(0xFF7F1D1D),
                                fontSize = 11.sp
                            )
                            Text(
                                "Motivo: ${currentMember?.suspensionReason}",
                                color = Color(0xFF991B1B),
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Confirma tu participación para la rodada \"${ride.title}\" con destino a ${ride.destinationCity}.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("🏍️ Piloto: ${currentMember?.fullName ?: "Piloto TX"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("Placa: ${currentMember?.bikePlate ?: "TX-000"} • Modelo: ${currentMember?.bikeModel ?: "Keeway TX 200"}", fontSize = 11.sp, color = Color(0xFF475569))
                            Text("🩸 Tipo de Sangre: ${currentMember?.bloodType ?: "O+"}", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = hasPillion,
                            onCheckedChange = { hasPillion = it },
                            colors = CheckboxDefaults.colors(checkedColor = MotoOrangePrimary)
                        )
                        Text("Llevo Copiloto / Acompañante", fontSize = 13.sp, color = Color(0xFF0F172A))
                    }

                    if (hasPillion) {
                        OutlinedTextField(
                            value = pillionName,
                            onValueChange = { pillionName = it },
                            label = { Text("Nombre y Apellido del Copiloto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedBorderColor = MotoOrangePrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = MotoOrangePrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmJoin(hasPillion, pillionName) },
                enabled = !isSuspended,
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                modifier = Modifier.testTag("btn_confirm_join")
            ) {
                Text("Confirmar Salida", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF64748B))
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun RideRosterDialog(
    ride: RideEvent,
    roster: List<RideRegistration>,
    isDirectivaMode: Boolean,
    onUpdateConvoyRole: (registrationId: Long, newRole: ConvoyRoleType) -> Unit,
    onDismiss: () -> Unit
) {
    var editingRegistrationId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Group, contentDescription = null, tint = MotoOrangePrimary)
                Column {
                    Text("Puestos & Convoy Oficial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("${ride.title} (${roster.size} motos inscritas)", fontSize = 11.sp, color = Color(0xFFEA580C))
                }
            }
        },
        text = {
            if (roster.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Aún no hay pilotos inscritos a esta rodada.", color = Color(0xFF64748B))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = if (isDirectivaMode) "Toca el rol de un piloto para cambiar su posición en la caravana (Capitán, Barredor, Seguridad, etc.):" else "Posiciones de caravana y convoy asignadas:",
                            fontSize = 11.sp,
                            color = Color(0xFFC2410C),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(roster, key = { it.id }) { reg ->
                        val roleType = ConvoyRoleType.values().firstOrNull { it.label == reg.convoyRole } ?: ConvoyRoleType.PILOTO_CENTRAL
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${reg.memberAlias.ifBlank { reg.memberName }} (${reg.memberName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Placa: ${reg.bikePlate}${if (reg.hasPillion) " • Copiloto: ${reg.pillionName}" else ""}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    // Convoy Role Badge with Directiva Dropdown
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(roleType.colorHex).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, Color(roleType.colorHex)),
                                            modifier = Modifier.clickable(enabled = isDirectivaMode) {
                                                editingRegistrationId = if (editingRegistrationId == reg.id) null else reg.id
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = roleType.label.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(roleType.colorHex)
                                                )
                                                if (isDirectivaMode) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(roleType.colorHex), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = editingRegistrationId == reg.id,
                                            onDismissRequest = { editingRegistrationId = null },
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            ConvoyRoleType.values().forEach { itemRole ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(itemRole.label, fontWeight = if (itemRole == roleType) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF0F172A))
                                                    },
                                                    onClick = {
                                                        onUpdateConvoyRole(reg.id, itemRole)
                                                        editingRegistrationId = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun CreateRideDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        desc: String,
        origin: String,
        dest: String,
        date: String,
        time: String,
        km: Int,
        terrain: String,
        leader: String,
        tail: String,
        gas: String,
        gear: String,
        cost: Double,
        max: Int,
        wa: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("Maracay (Peaje Tapa Tapa)") }
    var destination by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("Sábado 05 Septiembre 2026") }
    var time by remember { mutableStateOf("06:30 AM") }
    var kmStr by remember { mutableStateOf("150") }
    var terrain by remember { mutableStateOf("Asfalto y Curvas de Montaña") }
    var leader by remember { mutableStateOf("Capitán de Ruta TX") }
    var tail by remember { mutableStateOf("Escoba / Barredor Oficial TX") }
    var gasStops by remember { mutableStateOf("Estación Bohío ARC y El Limón") }
    var requiredGear by remember { mutableStateOf("Casco certificado, Chaqueta con protecciones, Guantes, Impermeable") }
    var costStr by remember { mutableStateOf("5.0") }
    var maxStr by remember { mutableStateOf("40") }
    var waGroup by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar Nueva Rodada TX", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nombre de la Rodada *") },
                        placeholder = { Text("ej. Vuelta al Jarillo & Colonia Tovar") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_ride_title")
                    )
                }
                item {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destino Final *") },
                        placeholder = { Text("ej. Bahía de Cata, Aragua") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = origin,
                            onValueChange = { origin = it },
                            label = { Text("Punto de Partida") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = MotoOrangePrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = MotoOrangePrimary,
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kmStr,
                            onValueChange = { kmStr = it },
                            label = { Text("KM Totales") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = MotoOrangePrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = MotoOrangePrimary,
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Fecha") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = MotoOrangePrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = MotoOrangePrimary,
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Hora de Encuentro") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = MotoOrangePrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = MotoOrangePrimary,
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción de la Rodada") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("ROLES CLAVE DE CONVOY Y SEGURIDAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = leader,
                            onValueChange = { leader = it },
                            label = { Text("Capitán / Líder") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = MotoOrangePrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = MotoOrangePrimary,
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tail,
                            onValueChange = { tail = it },
                            label = { Text("Barredor / Escoba") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = MotoOrangePrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = MotoOrangePrimary,
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = gasStops,
                        onValueChange = { gasStops = it },
                        label = { Text("Paradas de Gasolina") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = requiredGear,
                        onValueChange = { requiredGear = it },
                        label = { Text("Indumentaria y Equipo Requerido") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = waGroup,
                        onValueChange = { waGroup = it },
                        label = { Text("Enlace Grupo WhatsApp (Opcional)") },
                        placeholder = { Text("https://chat.whatsapp.com/...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && destination.isNotBlank()) {
                        val km = kmStr.toIntOrNull() ?: 100
                        val cost = costStr.toDoubleOrNull() ?: 0.0
                        val max = maxStr.toIntOrNull() ?: 50
                        onCreate(
                            title, description, origin, destination, date, time, km,
                            terrain, leader, tail, gasStops, requiredGear, cost, max,
                            if (waGroup.isNotBlank()) waGroup else null
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                modifier = Modifier.testTag("btn_confirm_create_ride")
            ) {
                Text("Guardar Rodada", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

@Composable
fun EditRideDialog(
    ride: RideEvent,
    onDismiss: () -> Unit,
    onConfirmEdit: (RideEvent) -> Unit
) {
    var title by remember { mutableStateOf(ride.title) }
    var description by remember { mutableStateOf(ride.description) }
    var origin by remember { mutableStateOf(ride.originCity) }
    var destination by remember { mutableStateOf(ride.destinationCity) }
    var date by remember { mutableStateOf(ride.departureDate) }
    var time by remember { mutableStateOf(ride.meetingTime) }
    var kmStr by remember { mutableStateOf("${ride.distanceKm}") }
    var terrain by remember { mutableStateOf(ride.terrainType) }
    var leader by remember { mutableStateOf(ride.convoyLeader) }
    var secondLeader by remember { mutableStateOf(ride.secondLeader) }
    var roadSafetyOfficer by remember { mutableStateOf(ride.roadSafetyOfficer) }
    var tail by remember { mutableStateOf(ride.tailRider) }
    var medicOfficer by remember { mutableStateOf(ride.medicOfficer) }
    var mechanicOfficer by remember { mutableStateOf(ride.mechanicOfficer) }
    var gasStops by remember { mutableStateOf(ride.gasStops) }
    var requiredGear by remember { mutableStateOf(ride.requiredGear) }
    var costStr by remember { mutableStateOf("${ride.costUsd}") }
    var maxStr by remember { mutableStateOf("${ride.maxParticipants}") }
    var waGroup by remember { mutableStateOf(ride.whatsappGroupUrl ?: "") }
    var status by remember { mutableStateOf(ride.status) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedBorderColor = MotoOrangePrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = MotoOrangePrimary,
        unfocusedLabelColor = Color(0xFF64748B)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditRoad, contentDescription = null, tint = MotoOrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Rodada TX", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text("Estado de la Rodada:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(RideStatus.values()) { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.label, fontSize = 10.sp, color = if (status == s) MotoOrangePrimary else Color(0xFF334155)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    selectedContainerColor = MotoOrangePrimary.copy(alpha = 0.15f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = status == s,
                                    borderColor = Color(0xFFCBD5E1),
                                    selectedBorderColor = MotoOrangePrimary
                                )
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nombre de la Rodada *") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destino Final *") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = origin,
                            onValueChange = { origin = it },
                            label = { Text("Punto de Partida") },
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kmStr,
                            onValueChange = { kmStr = it },
                            label = { Text("KM Totales") },
                            colors = tfColors,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Fecha") },
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Hora") },
                            colors = tfColors,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        minLines = 2,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("EQUIPO DE CONVOY Y SEGURIDAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = leader,
                            onValueChange = { leader = it },
                            label = { Text("Capitán / Líder") },
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tail,
                            onValueChange = { tail = it },
                            label = { Text("Barredor") },
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roadSafetyOfficer,
                            onValueChange = { roadSafetyOfficer = it },
                            label = { Text("Oficial de Seguridad") },
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = medicOfficer,
                            onValueChange = { medicOfficer = it },
                            label = { Text("Médico de Ruta") },
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = mechanicOfficer,
                        onValueChange = { mechanicOfficer = it },
                        label = { Text("Mecánico Oficial TX") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = gasStops,
                        onValueChange = { gasStops = it },
                        label = { Text("Paradas de Gasolina") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = requiredGear,
                        onValueChange = { requiredGear = it },
                        label = { Text("Indumentaria y Equipo") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = waGroup,
                        onValueChange = { waGroup = it },
                        label = { Text("Enlace Grupo WhatsApp") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && destination.isNotBlank()) {
                        val km = kmStr.toIntOrNull() ?: ride.distanceKm
                        val cost = costStr.toDoubleOrNull() ?: ride.costUsd
                        val max = maxStr.toIntOrNull() ?: ride.maxParticipants
                        val updated = ride.copy(
                            title = title,
                            description = description,
                            originCity = origin,
                            destinationCity = destination,
                            departureDate = date,
                            meetingTime = time,
                            distanceKm = km,
                            terrainType = terrain,
                            convoyLeader = leader,
                            secondLeader = secondLeader,
                            roadSafetyOfficer = roadSafetyOfficer,
                            tailRider = tail,
                            medicOfficer = medicOfficer,
                            mechanicOfficer = mechanicOfficer,
                            gasStops = gasStops,
                            requiredGear = requiredGear,
                            costUsdCents = (cost * 100).toLong(),
                            maxParticipants = max,
                            whatsappGroupUrl = if (waGroup.isNotBlank()) waGroup else null,
                            status = status
                        )
                        onConfirmEdit(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}
