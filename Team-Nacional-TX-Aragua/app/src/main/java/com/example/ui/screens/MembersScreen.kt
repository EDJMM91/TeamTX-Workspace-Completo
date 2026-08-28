package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.example.ui.components.*
import com.example.ui.theme.*

enum class MemberStatusFilter(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODOS("Todos", Icons.Default.Groups),
    ACTIVOS("Activos", Icons.Default.CheckCircle),
    SUSPENDIDOS("Suspendidos", Icons.Default.Gavel),
    DIRECTIVA("Directiva", Icons.Default.Shield)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    members: List<MemberProfile>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    onRegisterMember: (
        fullName: String,
        nickname: String,
        memberNumber: String,
        cedulaDni: String,
        phone: String,
        role: MemberRole,
        chapterState: String,
        bikeBrand: String,
        bikeModel: String,
        bikeColor: String,
        bikeDisplacementCc: String,
        bikeTankCapacityLiters: String,
        bikeYear: String,
        bikePlate: String,
        bloodType: String,
        medicalNotes: String,
        emergencyName: String,
        emergencyPhone: String,
        emergencyRelation: String,
        solvencyStatus: Boolean
    ) -> Unit,
    onToggleSolvency: (MemberProfile) -> Unit,
    onUpdateRole: (MemberProfile, MemberRole) -> Unit,
    onSuspendMember: (member: MemberProfile, reason: String, days: Int) -> Unit = { _, _, _ -> },
    onReactivateMember: (member: MemberProfile) -> Unit = {},
    onSelectMemberAsActive: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedChapter by remember { mutableStateOf("TODOS") }
    var selectedStatusFilter by remember { mutableStateOf(MemberStatusFilter.TODOS) }
    var selectedRole by remember { mutableStateOf<MemberRole?>(null) }
    var filterOnlySolvent by remember { mutableStateOf(false) }

    var selectedMemberForDetail by remember { mutableStateOf<MemberProfile?>(null) }
    var memberToSuspend by remember { mutableStateOf<MemberProfile?>(null) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Compute unique chapters
    val chapters = remember(members) {
        listOf("TODOS") + members.map { it.chapterState.split(" ")[0].trim() }.distinct().sorted()
    }

    // Counts
    val activeCount = remember(members) { members.count { !it.isSuspended } }
    val suspendedCount = remember(members) { members.count { it.isSuspended } }
    val directivaCount = remember(members) { members.count { it.isDirectiva || it.role.canManageApp } }

    // Filter members
    val filteredMembers = remember(members, searchQuery, selectedChapter, selectedStatusFilter, selectedRole, filterOnlySolvent) {
        members.filter { m ->
            val matchesQuery = searchQuery.isBlank() ||
                    m.fullName.contains(searchQuery, ignoreCase = true) ||
                    m.nickname.contains(searchQuery, ignoreCase = true) ||
                    m.memberNumber.contains(searchQuery, ignoreCase = true) ||
                    m.bikePlate.contains(searchQuery, ignoreCase = true) ||
                    m.chapterState.contains(searchQuery, ignoreCase = true)

            val matchesChapter = selectedChapter == "TODOS" || m.chapterState.contains(selectedChapter, ignoreCase = true)
            
            val matchesStatus = when (selectedStatusFilter) {
                MemberStatusFilter.TODOS -> true
                MemberStatusFilter.ACTIVOS -> !m.isSuspended
                MemberStatusFilter.SUSPENDIDOS -> m.isSuspended
                MemberStatusFilter.DIRECTIVA -> m.isDirectiva || m.role.canManageApp
            }

            val matchesRole = selectedRole == null || m.role == selectedRole
            val matchesSolvent = !filterOnlySolvent || m.solvencyStatus

            matchesQuery && matchesChapter && matchesStatus && matchesRole && matchesSolvent
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isDirectivaMode) {
                FloatingActionButton(
                    onClick = { showRegisterDialog = true },
                    containerColor = TxFlameRed,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_register_member")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo Piloto")
                        Text("Nuevo Piloto", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 84.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Banner: Official Title & Directiva stats
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF141822)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(TxGoldBrass, TxFlameRed, TxChromeSilver)
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("members_header_banner")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        VenezuelanFlagRibbon()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OfficialClubEmblemBadge(
                                size = 68.dp,
                                showSubtext = false
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = TxFlameRed.copy(alpha = 0.2f),
                                    border = BorderStroke(0.8.dp, TxFlameRed)
                                ) {
                                    Text(
                                        text = "REGISTRO Y DISCIPLINA NACIONAL",
                                        color = TxFlameRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Directorio de Pilotos TX",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Gestión de estado, solvencias, sanciones y rangos del club",
                                    color = TxSteelSilver,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Status Statistics Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C0E14))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatMiniItem(label = "Total", value = "${members.size}", color = TxChromeSilver)
                            VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFF232A38))
                            StatMiniItem(label = "Activos", value = "$activeCount", color = StatusSuccess)
                            VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFF232A38))
                            StatMiniItem(label = "Suspendidos", value = "$suspendedCount", color = StatusError)
                            VerticalDivider(modifier = Modifier.height(20.dp), color = Color(0xFF232A38))
                            StatMiniItem(label = "Directiva", value = "$directivaCount", color = TxGoldBrass)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, alias, N° TX, placa o estado...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = TxGoldBrass
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TxFlameRed,
                        unfocusedBorderColor = AsphaltDarkBorder,
                        focusedContainerColor = AsphaltDarkSurface,
                        unfocusedContainerColor = AsphaltDarkSurface
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_member")
                )
            }

            // Status Filter Tabs (Todos / Activos / Suspendidos / Directiva)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemberStatusFilter.values().forEach { filter ->
                        val isSelected = selectedStatusFilter == filter
                        val filterColor = when (filter) {
                            MemberStatusFilter.TODOS -> TxSteelSilver
                            MemberStatusFilter.ACTIVOS -> StatusSuccess
                            MemberStatusFilter.SUSPENDIDOS -> StatusError
                            MemberStatusFilter.DIRECTIVA -> TxGoldBrass
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) filterColor.copy(alpha = 0.2f) else AsphaltDarkSurface,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) filterColor else AsphaltDarkBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStatusFilter = filter }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) filterColor else Color(0xFF8C9BAE),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = filter.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) filterColor else Color(0xFF8C9BAE)
                                )
                            }
                        }
                    }
                }
            }

            // Chapter Filters Horizontal List
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FILTRAR POR CAPÍTULO / ESTADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TxGoldBrass,
                        letterSpacing = 0.5.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(chapters) { chapter ->
                            val isSelected = selectedChapter == chapter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedChapter = chapter },
                                label = {
                                    Text(
                                        text = chapter,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TxFlameRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = AsphaltDarkSurface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Role Filters Horizontal List
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FILTRAR POR JERARQUÍA / RANGO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TxSteelSilver,
                        letterSpacing = 0.5.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedRole == null,
                                onClick = { selectedRole = null },
                                label = { Text("Todos los Rangos", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TxGoldBrass,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        items(MemberRole.values()) { role ->
                            val isSelected = selectedRole == role
                            val roleColor = Color(role.badgeColorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRole = if (isSelected) null else role },
                                label = {
                                    Text(
                                        text = role.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(roleColor)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = roleColor.copy(alpha = 0.3f),
                                    selectedLabelColor = roleColor
                                )
                            )
                        }
                    }
                }
            }

            // Members Counter & Solvency Filter Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mostrando ${filteredMembers.size} miembros",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { filterOnlySolvent = !filterOnlySolvent }
                    ) {
                        Checkbox(
                            checked = filterOnlySolvent,
                            onCheckedChange = { filterOnlySolvent = it },
                            colors = CheckboxDefaults.colors(checkedColor = StatusSuccess)
                        )
                        Text(
                            text = "Solo Solventes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterOnlySolvent) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Empty State
            if (filteredMembers.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AsphaltDarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TxGoldBrass,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No se encontraron miembros",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Intenta cambiar el criterio de búsqueda o los filtros de estado/capítulo.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Members Cards List
            items(filteredMembers, key = { it.id }) { member ->
                MemberCardItem(
                    member = member,
                    isCurrentActive = currentMember?.id == member.id,
                    isDirectivaMode = isDirectivaMode,
                    onClickDetail = { selectedMemberForDetail = member },
                    onDirectWhatsApp = {
                        val msg = "¡Saludos Hermano Motero ${member.nickname}! Te escribo desde la App Oficial Team TX Venezuela 🏍️."
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=${member.phone.replace("+", "").replace(" ", "")}&text=${Uri.encode(msg)}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp: ${member.phone}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDirectCall = {
                        dialPhoneNumber(context, member.phone)
                    }
                )
            }
        }
    }

    // Pilot Dossier / Detail Dialog
    val detailMember = selectedMemberForDetail
    if (detailMember != null) {
        MemberDetailDossierDialog(
            member = detailMember,
            currentAdmin = currentMember,
            isDirectivaMode = isDirectivaMode,
            onDismiss = { selectedMemberForDetail = null },
            onToggleSolvency = {
                onToggleSolvency(detailMember)
                selectedMemberForDetail = detailMember.copy(solvencyStatus = !detailMember.solvencyStatus)
            },
            onUpdateRole = { newRole ->
                onUpdateRole(detailMember, newRole)
                selectedMemberForDetail = detailMember.copy(role = newRole)
            },
            onOpenSuspendDialog = {
                memberToSuspend = detailMember
            },
            onReactivate = {
                onReactivateMember(detailMember)
                selectedMemberForDetail = detailMember.copy(
                    isSuspended = false,
                    suspensionReason = "",
                    suspensionDurationDays = 0,
                    suspensionEndTimestamp = 0L
                )
                Toast.makeText(context, "Sanción levantada. Piloto reactivado.", Toast.LENGTH_SHORT).show()
            },
            onSelectAsActive = {
                onSelectMemberAsActive(detailMember.id)
                selectedMemberForDetail = null
                Toast.makeText(context, "Perfil activo seleccionado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Manage Suspension Dialog
    val suspendTarget = memberToSuspend
    if (suspendTarget != null) {
        SuspendMemberDialog(
            member = suspendTarget,
            adminName = currentMember?.fullName ?: "Directiva Nacional",
            onDismiss = { memberToSuspend = null },
            onConfirmSuspend = { reason, days ->
                onSuspendMember(suspendTarget, reason, days)
                if (selectedMemberForDetail?.id == suspendTarget.id) {
                    selectedMemberForDetail = selectedMemberForDetail?.copy(
                        isSuspended = true,
                        suspensionReason = reason,
                        suspensionDurationDays = days,
                        suspensionEndTimestamp = if (days > 0) System.currentTimeMillis() + days * 86400000L else 0L
                    )
                }
                val name = suspendTarget.nickname
                memberToSuspend = null
                Toast.makeText(context, "Sanción de $days días aplicada a $name", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Register Member Dialog (Directiva)
    if (showRegisterDialog) {
        RegisterMemberDialog(
            onDismiss = { showRegisterDialog = false },
            onRegister = { fullName, nickname, number, dni, phone, role, chapter, brand, model, color, cc, tank, year, plate, blood, medNotes, emName, emPhone, emRel, solvent ->
                onRegisterMember(fullName, nickname, number, dni, phone, role, chapter, brand, model, color, cc, tank, year, plate, blood, medNotes, emName, emPhone, emRel, solvent)
                showRegisterDialog = false
                Toast.makeText(context, "¡Piloto registrado exitosamente!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun StatMiniItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
        Text(text = label, fontSize = 10.sp, color = Color(0xFF8C9BAE), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MemberCardItem(
    member: MemberProfile,
    isCurrentActive: Boolean,
    isDirectivaMode: Boolean,
    onClickDetail: () -> Unit,
    onDirectWhatsApp: () -> Unit,
    onDirectCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rankColor = Color(member.role.badgeColorHex)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isSuspended) Color(0xFF1E1315) else if (isCurrentActive) Color(0xFF1B2230) else AsphaltDarkSurface
        ),
        border = BorderStroke(
            if (member.isSuspended) 1.5.dp else if (isCurrentActive) 1.5.dp else 1.dp,
            if (member.isSuspended) StatusError else if (isCurrentActive) TxFlameRed else AsphaltDarkBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("member_card_${member.memberNumber.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Bar: Role badge / Suspended alert, Chapter, Member ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member.isSuspended) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StatusError.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, StatusError)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = StatusError, modifier = Modifier.size(11.dp))
                            Text(
                                text = "SUSPENDIDO (${member.suspensionDurationDays}d)",
                                color = StatusError,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = rankColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, rankColor)
                    ) {
                        Text(
                            text = member.role.displayName.uppercase(),
                            color = rankColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCurrentActive) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TxFlameRed
                        ) {
                            Text(
                                text = "TÚ",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F1218),
                        border = BorderStroke(1.dp, if (member.isSuspended) StatusError.copy(alpha = 0.5f) else TxSteelDark.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = member.memberNumber,
                            color = if (member.isSuspended) StatusError else TxGoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Suspension reason banner if suspended
            if (member.isSuspended) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StatusError.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Motivo: ${member.suspensionReason.ifBlank { "Sanción directiva" }}",
                            color = Color(0xFFFFCDD2),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pilot Core Info: Names + Chapter + Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Alias: \"${member.nickname}\"",
                        color = if (member.isSuspended) StatusError else TxGoldBrass,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TxFlameRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = member.chapterState,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(contentAlignment = Alignment.BottomEnd) {
                    PilotAvatar(
                        member = member,
                        size = 56.dp,
                        showRankGlow = true
                    )
                    if (isCurrentActive && member.profilePhotoUri.isNullOrEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = TxFlameRed,
                            border = BorderStroke(1.dp, Color.Black),
                            modifier = Modifier.size(18.dp).offset(x = 4.dp, y = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Añadir foto",
                                tint = Color.White,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
            }

            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF262D3D))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Motorcycle Specs Card inside member card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF131822),
                border = BorderStroke(0.8.dp, Color(0xFF242C3D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(13.dp))
                            Text(
                                text = "${member.bikeBrand} ${member.bikeModel}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = member.bikeDisplacementCc,
                            color = TxGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎨 ${member.bikeColor} • ⛽ ${member.bikeTankCapacityLiters} • 📅 ${member.bikeYear}",
                            color = Color(0xFF8C9BAE),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Placa: ${member.bikePlate}",
                            color = TxSteelSilver,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Specs Row: Blood Type, Solvency, Phone preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StatusError.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, StatusError)
                    ) {
                        Text(
                            text = "🩸 ${member.bloodType}",
                            color = StatusError,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (member.isSuspended) StatusError.copy(alpha = 0.15f) else if (member.solvencyStatus) StatusSuccess.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, if (member.isSuspended) StatusError else if (member.solvencyStatus) StatusSuccess else StatusWarning)
                    ) {
                        Text(
                            text = if (member.isSuspended) "Inhabilitado" else if (member.solvencyStatus) "Solvente ✓" else "Pendiente",
                            color = if (member.isSuspended) StatusError else if (member.solvencyStatus) StatusSuccess else StatusWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "📞 ${member.phone}",
                    color = Color(0xFFA0ADC0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: WhatsApp, Call, Ficha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDirectWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDirectCall,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TxChromeSilver, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Llamar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onClickDetail,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263346)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Ficha", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TxGoldBrass)
                }
                }
            } // End of AnimatedVisibility
        }
    }
}

@Composable
fun MemberDetailDossierDialog(
    member: MemberProfile,
    currentAdmin: MemberProfile?,
    isDirectivaMode: Boolean,
    onDismiss: () -> Unit,
    onToggleSolvency: () -> Unit,
    onUpdateRole: (MemberRole) -> Unit,
    onOpenSuspendDialog: () -> Unit,
    onReactivate: () -> Unit,
    onSelectAsActive: () -> Unit
) {
    val context = LocalContext.current
    var showRoleMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
            ) {
                Text("Cerrar Ficha", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSelectAsActive) {
                Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Usar este Perfil")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = TxGoldBrass)
                Text(
                    text = "Ficha de Piloto - ${member.memberNumber}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    DigitalCredentialCard(member = member)
                }

                if (member.bikePhotoUri != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TxGoldBrass.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            AsyncImage(
                                model = member.bikePhotoUri,
                                contentDescription = "Foto de la moto de ${member.fullName}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Contact & Medical Information
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141822)),
                        border = BorderStroke(1.dp, AsphaltDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DATOS DE CONTACTO Y ASISTENCIA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TxGoldBrass
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Teléfono Piloto:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(member.phone, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(
                                    onClick = { dialPhoneNumber(context, member.phone) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Llamar", tint = StatusSuccess)
                                }
                            }

                            HorizontalDivider(color = Color(0xFF262D3D))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Contacto de Emergencia:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(member.emergencyContactName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(member.emergencyContactPhone, fontSize = 11.sp, color = TxGoldLight)
                                }
                                IconButton(
                                    onClick = { dialPhoneNumber(context, member.emergencyContactPhone) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Emergency, contentDescription = "SOS", tint = StatusError)
                                }
                            }
                        }
                    }
                }

                // Directiva & Admin Disciplinary Controls
                if (isDirectivaMode) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C)),
                            border = BorderStroke(1.dp, TxFlameRed.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(16.dp))
                                    Text("ACCIONES DIRECTIVAS & DISCIPLINA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TxFlameRed)
                                }

                                // Solvency toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Estado de Solvencia:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Button(
                                        onClick = onToggleSolvency,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (member.solvencyStatus) StatusWarning else StatusSuccess
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (member.solvencyStatus) "Marcar Deudor" else "Aprobar Solvencia",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Suspension / Sanction Management
                                if (member.isSuspended) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusError.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, StatusError),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Gavel, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                                Text("Sanción Disciplinaria Activa", color = StatusError, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                            }
                                            Text(
                                                text = "Motivo: ${member.suspensionReason.ifBlank { "Infracción de normas" }}",
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Tiempo: ${member.suspensionDurationDays} días (${member.suspensionStartDate} a ${member.suspensionEndDate})",
                                                color = TxGoldSecondary,
                                                fontSize = 10.sp
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = onReactivate,
                                                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reactivar Piloto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = onOpenSuspendDialog,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Modificar Sanción", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = onOpenSuspendDialog,
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Aplicar Sanción / Suspender Piloto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // Role update dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { showRoleMenu = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = TxGoldBrass)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cambiar Rango: ${member.role.displayName}", fontSize = 12.sp)
                                    }

                                    DropdownMenu(
                                        expanded = showRoleMenu,
                                        onDismissRequest = { showRoleMenu = false }
                                    ) {
                                        MemberRole.values().forEach { role ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(role.badgeColorHex)))
                                                        Text(role.displayName, fontWeight = if (role == member.role) FontWeight.Bold else FontWeight.Normal)
                                                    }
                                                },
                                                onClick = {
                                                    onUpdateRole(role)
                                                    showRoleMenu = false
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
    )
}

@Composable
fun SuspendMemberDialog(
    member: MemberProfile,
    adminName: String,
    onDismiss: () -> Unit,
    onConfirmSuspend: (reason: String, days: Int) -> Unit
) {
    var reason by remember { mutableStateOf(member.suspensionReason.ifBlank { "Adelantamiento imprudente y desobediencia en caravana" }) }
    var selectedDays by remember { mutableStateOf(if (member.suspensionDurationDays > 0) member.suspensionDurationDays else 30) }
    var customReason by remember { mutableStateOf("") }

    val presetReasons = listOf(
        "Adelantamiento imprudente y desobediencia en caravana",
        "Mora reiterada en cuotas de membresía",
        "Falta de respeto a la hermandad motera",
        "Incumplimiento de equipo de seguridad obligatorio",
        "Inasistencia injustificada a compromisos oficiales"
    )

    val durationOptions = listOf(7, 15, 30, 60, 90, 180)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = StatusError)
                Text("Sancionar Piloto: ${member.nickname}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = "La suspensión inhabilitará al miembro para inscribirse en rodadas y marcará su carnet con estatus sancionado.",
                        fontSize = 11.sp,
                        color = Color(0xFFFFCDD2)
                    )
                }

                item {
                    Text(
                        text = "TIEMPO DE SUSPENSIÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TxGoldBrass
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(durationOptions) { days ->
                            val isSelected = selectedDays == days
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDays = days },
                                label = { Text("$days días", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusError,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "MOTIVO DE LA SANCIÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TxGoldBrass
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetReasons.forEach { preReason ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reason = preReason }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = reason == preReason,
                                    onClick = { reason = preReason },
                                    colors = RadioButtonDefaults.colors(selectedColor = StatusError)
                                )
                                Text(text = preReason, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = {
                            customReason = it
                            if (it.isNotBlank()) reason = it
                        },
                        label = { Text("O escribe motivo personalizado") },
                        placeholder = { Text("Ej. Incumplimiento del código...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141822),
                        border = BorderStroke(1.dp, AsphaltDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Autorizado por: $adminName",
                                fontSize = 11.sp,
                                color = TxSteelSilver,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSuspend(reason, selectedDays) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
            ) {
                Text("Confirmar Sanción", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterMemberDialog(
    onDismiss: () -> Unit,
    onRegister: (
        fullName: String,
        nickname: String,
        memberNumber: String,
        cedulaDni: String,
        phone: String,
        role: MemberRole,
        chapterState: String,
        bikeBrand: String,
        bikeModel: String,
        bikeColor: String,
        bikeDisplacementCc: String,
        bikeTankCapacityLiters: String,
        bikeYear: String,
        bikePlate: String,
        bloodType: String,
        medicalNotes: String,
        emergencyName: String,
        emergencyPhone: String,
        emergencyRelation: String,
        solvencyStatus: Boolean
    ) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var memberNumber by remember { mutableStateOf("TX-") }
    var cedulaDni by remember { mutableStateOf("V-") }
    var phone by remember { mutableStateOf("+58 ") }
    var role by remember { mutableStateOf(MemberRole.MIEMBRO_ACTIVO) }
    var chapterState by remember { mutableStateOf("Aragua (Maracay)") }

    // Moto specs
    var bikeBrand by remember { mutableStateOf("Keeway") }
    var bikeModel by remember { mutableStateOf("TX 200 SM") }
    var bikeColor by remember { mutableStateOf("Naranja Racing / Negro") }
    var bikeDisplacementCc by remember { mutableStateOf("200 cc") }
    var bikeTankCapacityLiters by remember { mutableStateOf("11.5 L") }
    var bikeYear by remember { mutableStateOf("2024") }
    var bikePlate by remember { mutableStateOf("") }

    // Medical & SOS
    var bloodType by remember { mutableStateOf("O+") }
    var medicalNotes by remember { mutableStateOf("Sin alergias reportadas") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyRelation by remember { mutableStateOf("Familiar / Esposa") }
    var emergencyPhone by remember { mutableStateOf("+58 ") }
    var solvencyStatus by remember { mutableStateOf(true) }

    var showRoleDropdown by remember { mutableStateOf(false) }
    var showChapterDropdown by remember { mutableStateOf(false) }
    var showBloodDropdown by remember { mutableStateOf(false) }

    val chapterOptions = listOf(
        "Aragua (Maracay)",
        "Aragua (La Victoria)",
        "Distrito Capital (Caracas)",
        "Miranda (Altos Mirandinos)",
        "Carabobo (Valencia)",
        "Lara (Barquisimeto)",
        "Zulia (Maracaibo)",
        "Anzoátegui (Puerto La Cruz)"
    )

    val bloodOptions = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TxFlameRed)
                Text("Registrar Nuevo Piloto TX", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Section: Personal Data
                item {
                    Text(
                        text = "1. DATOS PERSONALES DEL PILOTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TxGoldBrass
                    )
                }
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre y Apellido *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("Alias / Apodo *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = memberNumber,
                            onValueChange = { memberNumber = it },
                            label = { Text("N° TX *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cedulaDni,
                            onValueChange = { cedulaDni = it },
                            label = { Text("Cédula / DNI *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono WhatsApp *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Role Dropdown
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showRoleDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rango: ${role.displayName}", fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = showRoleDropdown,
                            onDismissRequest = { showRoleDropdown = false }
                        ) {
                            MemberRole.values().forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.displayName) },
                                    onClick = {
                                        role = r
                                        showRoleDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Chapter Dropdown
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showChapterDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Capítulo: $chapterState", fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = showChapterDropdown,
                            onDismissRequest = { showChapterDropdown = false }
                        ) {
                            chapterOptions.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text(ch) },
                                    onClick = {
                                        chapterState = ch
                                        showChapterDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Section: Motorcycle Data
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. DATOS DE LA MOTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TxGoldBrass
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bikeBrand,
                            onValueChange = { bikeBrand = it },
                            label = { Text("Marca (ej: Keeway) *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeModel,
                            onValueChange = { bikeModel = it },
                            label = { Text("Modelo (ej: TX 200 SM) *") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bikeColor,
                            onValueChange = { bikeColor = it },
                            label = { Text("Color") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeDisplacementCc,
                            onValueChange = { bikeDisplacementCc = it },
                            label = { Text("Cilindrada (cc)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bikeTankCapacityLiters,
                            onValueChange = { bikeTankCapacityLiters = it },
                            label = { Text("Tanque (L)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeYear,
                            onValueChange = { bikeYear = it },
                            label = { Text("Año") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = bikePlate,
                            onValueChange = { bikePlate = it },
                            label = { Text("Placa *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Section: Medical & Emergency SOS
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3. FICHA MÉDICA & RESCATE SOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusError
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showBloodDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sangre: $bloodType", fontWeight = FontWeight.Bold, color = StatusError)
                            }
                            DropdownMenu(
                                expanded = showBloodDropdown,
                                onDismissRequest = { showBloodDropdown = false }
                            ) {
                                bloodOptions.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text("🩸 $b") },
                                        onClick = {
                                            bloodType = b
                                            showBloodDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = solvencyStatus,
                                onCheckedChange = { solvencyStatus = it },
                                colors = CheckboxDefaults.colors(checkedColor = StatusSuccess)
                            )
                            Text("Solvente 2026", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = medicalNotes,
                        onValueChange = { medicalNotes = it },
                        label = { Text("Notas Médicas / Alergias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = emergencyName,
                            onValueChange = { emergencyName = it },
                            label = { Text("Contacto SOS (Nombre)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = emergencyRelation,
                            onValueChange = { emergencyRelation = it },
                            label = { Text("Parentesco") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        label = { Text("Teléfono de Emergencia SOS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && memberNumber.isNotBlank() && bikePlate.isNotBlank()) {
                        onRegister(
                            fullName,
                            nickname.ifBlank { "Piloto TX" },
                            memberNumber,
                            cedulaDni,
                            phone,
                            role,
                            chapterState,
                            bikeBrand.ifBlank { "Keeway" },
                            bikeModel.ifBlank { "TX 200 SM" },
                            bikeColor.ifBlank { "Naranja / Negro" },
                            bikeDisplacementCc.ifBlank { "200 cc" },
                            bikeTankCapacityLiters.ifBlank { "11.5 L" },
                            bikeYear.ifBlank { "2024" },
                            bikePlate,
                            bloodType,
                            medicalNotes.ifBlank { "Sin observaciones" },
                            emergencyName.ifBlank { "Directiva TX" },
                            emergencyPhone.ifBlank { phone },
                            emergencyRelation.ifBlank { "Familiar" },
                            solvencyStatus
                        )
                    }
                },
                enabled = fullName.isNotBlank() && memberNumber.isNotBlank() && bikePlate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
            ) {
                Text("Registrar Piloto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
