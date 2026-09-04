package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.model.BikerChallenge
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.example.data.model.UserChallengeProgress
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    challenges: List<BikerChallenge>,
    progressList: List<UserChallengeProgress>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    onCreateChallenge: (
        title: String,
        description: String,
        targetKm: Double,
        cruisingSpeed: String,
        badgeName: String,
        startDate: String,
        endDate: String,
        checkpoints: String,
        isOfficial: Boolean,
        badgeImageUri: Uri?,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onUpdateChallenge: (challenge: BikerChallenge, newBadgeImageUri: Uri?, onComplete: (Boolean) -> Unit) -> Unit,
    onDeleteChallenge: (Long) -> Unit,
    onSaveProgress: (progress: UserChallengeProgress, sitePhotoUri: Uri?, onComplete: (Boolean) -> Unit) -> Unit,
    onDeleteProgress: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDirectiva = isDirectivaMode || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true) || (currentMember?.role == MemberRole.PRESIDENTE) || (currentMember?.role == MemberRole.DIRECTIVA)

    var selectedTab by remember { mutableStateOf(0) } // 0: Retos Oficiales, 1: Mis Retos & Reportes
    var showCreateChallengeDialog by remember { mutableStateOf(false) }
    var challengeToEdit by remember { mutableStateOf<BikerChallenge?>(null) }
    var challengeForProgress by remember { mutableStateOf<BikerChallenge?>(null) }
    var progressToEdit by remember { mutableStateOf<UserChallengeProgress?>(null) }

    val officialChallenges = remember(challenges) {
        challenges.filter { it.isOfficial }
    }
    val personalChallenges = remember(challenges, currentMember) {
        challenges.filter { !it.isOfficial && (it.creatorMemberId == currentMember?.id || currentMember == null) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Retos Moteros TX",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Rutas, Checkpoints y Reportes Oficiales",
                                fontSize = 11.sp,
                                color = MotoOrangePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onBack() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Inicio", tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inicio", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateChallengeDialog = true },
                containerColor = MotoOrangePrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Reto")
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pestañas Retos Oficiales vs Mis Retos
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MotoOrangePrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Oficiales Club (${officialChallenges.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) MotoOrangePrimary else Color(0xFF64748B)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Mis Retos & Reportes (${personalChallenges.size + progressList.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) MotoOrangePrimary else Color(0xFF64748B)
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // LISTA DE RETOS OFICIALES
                if (officialChallenges.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No hay retos oficiales activos", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("La directiva publicará nuevos desafíos de ruta pronto.", color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(officialChallenges, key = { it.id }) { challenge ->
                            val myProgress = progressList.find { it.challengeId == challenge.id && (currentMember == null || it.memberId == currentMember.id) }
                            ChallengeCard(
                                challenge = challenge,
                                userProgress = myProgress,
                                isDirectiva = isDirectiva,
                                isCreator = challenge.creatorMemberId == currentMember?.id,
                                onEdit = { challengeToEdit = challenge },
                                onDelete = { onDeleteChallenge(challenge.id) },
                                onRegisterProgress = {
                                    challengeForProgress = challenge
                                    progressToEdit = myProgress
                                },
                                onShareReport = { progress ->
                                    shareReportViaWhatsApp(context, challenge, progress, currentMember)
                                }
                            )
                        }
                    }
                }
            } else {
                // MIS RETOS PERSONALES Y REPORTES REGISTRADOS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (personalChallenges.isNotEmpty()) {
                        item {
                            Text(
                                text = "🏍️ RETOS PERSONALES CREADOS POR TI",
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(personalChallenges, key = { it.id }) { challenge ->
                            val myProgress = progressList.find { it.challengeId == challenge.id && (currentMember == null || it.memberId == currentMember.id) }
                            ChallengeCard(
                                challenge = challenge,
                                userProgress = myProgress,
                                isDirectiva = true, // Es su propio reto
                                isCreator = true,
                                onEdit = { challengeToEdit = challenge },
                                onDelete = { onDeleteChallenge(challenge.id) },
                                onRegisterProgress = {
                                    challengeForProgress = challenge
                                    progressToEdit = myProgress
                                },
                                onShareReport = { progress ->
                                    shareReportViaWhatsApp(context, challenge, progress, currentMember)
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = "📋 MIS REPORTES DE RUTA ACTIVOS",
                            color = MotoOrangePrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    if (progressList.isEmpty()) {
                        item {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Aún no tienes reportes de ruta registrados", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Selecciona cualquier reto oficial o personal y pulsa 'Llenar Reporte de Ruta'.", color = Color(0xFF64748B), fontSize = 12.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        items(progressList, key = { it.id }) { prog ->
                            val challenge = challenges.find { it.id == prog.challengeId }
                            RouteReportSummaryCard(
                                progress = prog,
                                challenge = challenge,
                                onEdit = {
                                    challengeForProgress = challenge ?: BikerChallenge(
                                        id = prog.challengeId,
                                        title = prog.challengeTitle,
                                        description = "",
                                        targetKm = 0.0,
                                        cruisingSpeed = "80-100 km/h",
                                        badgeName = "Reto TX",
                                        startDate = prog.departureDate,
                                        endDate = prog.returnDate,
                                        checkpoints = prog.completedCheckpoints
                                    )
                                    progressToEdit = prog
                                },
                                onDelete = { onDeleteProgress(prog.id) },
                                onShare = {
                                    shareReportViaWhatsApp(context, challenge, prog, currentMember)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO CREAR / EDITAR RETO
    if (showCreateChallengeDialog || challengeToEdit != null) {
        CreateEditChallengeDialog(
            existingChallenge = challengeToEdit,
            isDirectiva = isDirectiva,
            onDismiss = {
                showCreateChallengeDialog = false
                challengeToEdit = null
            },
            onSave = { title, desc, targetKm, speed, badge, start, end, checkpoints, isOfficial, imgUri ->
                if (challengeToEdit != null) {
                    onUpdateChallenge(
                        challengeToEdit!!.copy(
                            title = title,
                            description = desc,
                            targetKm = targetKm,
                            cruisingSpeed = speed,
                            badgeName = badge,
                            startDate = start,
                            endDate = end,
                            checkpoints = checkpoints,
                            isOfficial = isOfficial
                        ),
                        imgUri
                    ) { success ->
                        if (success) Toast.makeText(context, "Reto actualizado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    onCreateChallenge(title, desc, targetKm, speed, badge, start, end, checkpoints, isOfficial, imgUri) { success ->
                        if (success) Toast.makeText(context, "Reto creado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                }
                showCreateChallengeDialog = false
                challengeToEdit = null
            }
        )
    }

    // DIÁLOGO LLENAR / ACTUALIZAR REPORTE DE RUTA DEL PILOTO
    if (challengeForProgress != null) {
        ChallengeProgressDialog(
            challenge = challengeForProgress!!,
            existingProgress = progressToEdit,
            currentMember = currentMember,
            onDismiss = {
                challengeForProgress = null
                progressToEdit = null
            },
            onSave = { updatedProgress, photoUri ->
                onSaveProgress(updatedProgress, photoUri) { success ->
                    if (success) {
                        Toast.makeText(context, "Reporte de ruta guardado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al guardar reporte", Toast.LENGTH_SHORT).show()
                    }
                }
                challengeForProgress = null
                progressToEdit = null
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CARD DEL RETO
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun ChallengeCard(
    challenge: BikerChallenge,
    userProgress: UserChallengeProgress?,
    isDirectiva: Boolean,
    isCreator: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegisterProgress: () -> Unit,
    onShareReport: (UserChallengeProgress) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (challenge.isOfficial) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Insignia & Tipo de Reto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (challenge.isOfficial) Color(0xFFFEF3C7) else Color(0xFFFFEDD5),
                    border = BorderStroke(1.dp, if (challenge.isOfficial) Color(0xFFF59E0B) else MotoOrangePrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (challenge.isOfficial) "👑 RETO OFICIAL CLUB" else "🏍️ RETO PERSONAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (challenge.isOfficial) Color(0xFFB45309) else MotoOrangePrimary
                        )
                    }
                }

                if (isDirectiva || isCreator) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Título del Reto
            Text(
                text = challenge.title,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = Color(0xFF0F172A)
            )

            // Distintivo / Parche
            if (challenge.badgeName.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = challenge.badgeName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            // Imagen / Flyer si existe
            if (!challenge.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                SubcomposeAsyncImage(
                    model = challenge.imageUrl,
                    contentDescription = "Flyer del Reto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MotoOrangePrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = challenge.description,
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Métricas: Kms, Velocidad, Fechas
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🎯 Objetivo:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Text("${String.format(Locale.US, "%.1f", challenge.targetKm)} KM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⚡ Velocidad Crucero:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Text(challenge.cruisingSpeed.ifBlank { "80-100 km/h" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoOrangePrimary)
                    }
                    if (challenge.startDate.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("📅 Vigencia:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Text("${challenge.startDate} al ${challenge.endDate.ifBlank { "Indefinido" }}", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Toques / Checkpoints obligatorios
            if (challenge.checkpoints.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📍 Toques / Sitios del Reto:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
                Text(
                    text = challenge.checkpoints,
                    fontSize = 12.sp,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción sin choques
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Llenar / Modificar Reporte
                Button(
                    onClick = onRegisterProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (userProgress != null) "Editar Reporte" else "Llenar Reporte", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Botón Compartir WhatsApp
                if (userProgress != null) {
                    Button(
                        onClick = { onShareReport(userProgress) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar este reto?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Se eliminará permanentemente el reto '${challenge.title}'.", color = Color(0xFF334155)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CARD RESUMEN DE REPORTE DE RUTA
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun RouteReportSummaryCard(
    progress: UserChallengeProgress,
    challenge: BikerChallenge?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏍️ ${progress.challengeTitle}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "🚀 ${progress.originLocation} ➔ 🗺️ ${progress.destinationLocation}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0284C7)
            )
            Text(
                text = "🏍️ Moto: ${progress.bikeModel} (Placa: ${progress.bikePlate}) | Piloto: ${progress.pilotName}",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "🛣️ Recorrido: ${String.format(Locale.US, "%.1f", progress.finalKm - progress.initialKm)} KM | Acumulados: ${String.format(Locale.US, "%.1f", progress.accumulatedKm)} KM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MotoOrangePrimary
            )

            // Foto miniatura si existe
            if (!progress.sitePhotoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                SubcomposeAsyncImage(
                    model = progress.sitePhotoUrl,
                    contentDescription = "Foto Sitio",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir Reporte por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar este reporte?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Se eliminará el reporte de ruta de ${progress.pilotName} (${progress.destinationLocation}).", color = Color(0xFF334155)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: CREAR / EDITAR RETO (Directiva u Oficial)
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun CreateEditChallengeDialog(
    existingChallenge: BikerChallenge?,
    isDirectiva: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, targetKm: Double, speed: String, badge: String, start: String, end: String, checkpoints: String, isOfficial: Boolean, imageUri: Uri?) -> Unit
) {
    var title by remember { mutableStateOf(existingChallenge?.title ?: "") }
    var description by remember { mutableStateOf(existingChallenge?.description ?: "") }
    var targetKmStr by remember { mutableStateOf(existingChallenge?.targetKm?.toString() ?: "1000") }
    var cruisingSpeed by remember { mutableStateOf(existingChallenge?.cruisingSpeed ?: "80-100 km/h") }
    var badgeName by remember { mutableStateOf(existingChallenge?.badgeName ?: "") }
    var startDate by remember { mutableStateOf(existingChallenge?.startDate ?: "") }
    var endDate by remember { mutableStateOf(existingChallenge?.endDate ?: "") }
    var checkpoints by remember { mutableStateOf(existingChallenge?.checkpoints ?: "") }
    var isOfficial by remember { mutableStateOf(existingChallenge?.isOfficial ?: isDirectiva) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

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
            Text(
                text = if (existingChallenge != null) "Editar Reto Motero" else "Nuevo Reto Motero",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del Reto") },
                    placeholder = { Text("Ej: 3er. RETO PILOTO EXPERTO") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / Reglas") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetKmStr,
                        onValueChange = { targetKmStr = it },
                        label = { Text("KM Objetivo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cruisingSpeed,
                        onValueChange = { cruisingSpeed = it },
                        label = { Text("Velocidad") },
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = badgeName,
                    onValueChange = { badgeName = it },
                    label = { Text("Insignia / Parche a Otorgar") },
                    placeholder = { Text("Ej: Parche Piloto Experto 🏆") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Fecha Inicio") },
                        placeholder = { Text("DD/MM/AAAA") },
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("Fecha Fin") },
                        placeholder = { Text("DD/MM/AAAA") },
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = checkpoints,
                    onValueChange = { checkpoints = it },
                    label = { Text("Toques / Sitios (Separados por coma)") },
                    placeholder = { Text("Ej: Cabo San Román, Collado del Cóndor, Cuyagua") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isDirectiva) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isOfficial,
                            onCheckedChange = { isOfficial = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                        )
                        Text("Reto Oficial del Club (Visible para todos)", fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                    }
                }

                // Selector de Flyer / Imagen
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { photoPickerLauncher.launch("image/*") }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MotoOrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedImageUri != null) "Flyer seleccionado ✓" else "Subir Flyer / Imagen del Reto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val km = targetKmStr.toDoubleOrNull() ?: 1000.0
                        onSave(title, description, km, cruisingSpeed, badgeName, startDate, endDate, checkpoints, isOfficial, selectedImageUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Guardar Reto", color = Color.White, fontWeight = FontWeight.Bold)
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

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: LLENAR REPORTE DE RUTA DEL PILOTO
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun ChallengeProgressDialog(
    challenge: BikerChallenge,
    existingProgress: UserChallengeProgress?,
    currentMember: MemberProfile?,
    onDismiss: () -> Unit,
    onSave: (UserChallengeProgress, Uri?) -> Unit
) {
    var pilotName by remember { mutableStateOf(existingProgress?.pilotName ?: (currentMember?.fullName ?: "")) }
    var pilotPhone by remember { mutableStateOf(existingProgress?.pilotPhone ?: (currentMember?.phone ?: "")) }
    var emergencyContact by remember { mutableStateOf(existingProgress?.pilotEmergencyContact ?: (currentMember?.emergencyContactPhone ?: "")) }
    var copilotName by remember { mutableStateOf(existingProgress?.copilotName ?: "") }
    var copilotPhone by remember { mutableStateOf(existingProgress?.copilotPhone ?: "") }
    var bikeModel by remember { mutableStateOf(existingProgress?.bikeModel ?: (currentMember?.bikeModel ?: "Keeway TX 200")) }
    var bikePlate by remember { mutableStateOf(existingProgress?.bikePlate ?: (currentMember?.bikePlate ?: "")) }
    var departureDate by remember { mutableStateOf(existingProgress?.departureDate ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var returnDate by remember { mutableStateOf(existingProgress?.returnDate ?: "") }
    var originLocation by remember { mutableStateOf(existingProgress?.originLocation ?: "San Cristóbal (Campamento Base)") }
    var destinationLocation by remember { mutableStateOf(existingProgress?.destinationLocation ?: "") }
    var initialKmStr by remember { mutableStateOf(existingProgress?.initialKm?.toString() ?: "0.0") }
    var finalKmStr by remember { mutableStateOf(existingProgress?.finalKm?.toString() ?: "0.0") }
    var currentRouteNumberStr by remember { mutableStateOf(existingProgress?.currentRouteNumber?.toString() ?: "1") }
    var routeDescription by remember { mutableStateOf(existingProgress?.routeDescription ?: "") }
    var accumulatedKmStr by remember { mutableStateOf(existingProgress?.accumulatedKm?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(existingProgress?.notes ?: "") }
    var selectedSitePhotoUri by remember { mutableStateOf<Uri?>(null) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedBorderColor = MotoOrangePrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = MotoOrangePrimary,
        unfocusedLabelColor = Color(0xFF64748B)
    )

    // Checkpoints mapping (Lugar -> Boolean visitado)
    val challengeCheckpointsList = remember(challenge.checkpoints) {
        challenge.checkpoints.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val completedMap = remember {
        val initialMap = mutableStateMapOf<String, Boolean>()
        val existingSaved = existingProgress?.completedCheckpoints ?: ""
        challengeCheckpointsList.forEach { place ->
            initialMap[place] = existingSaved.contains("$place: ✅")
        }
        initialMap
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedSitePhotoUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🏍️ Reporte de Ruta - ${challenge.title}",
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Llenado de datos oficiales para el reporte de WhatsApp y registro de ruta.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                // Fechas
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = departureDate,
                        onValueChange = { departureDate = it },
                        label = { Text("Fecha Salida") },
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = returnDate,
                        onValueChange = { returnDate = it },
                        label = { Text("Fecha Llegada") },
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Origen y Destino
                OutlinedTextField(
                    value = originLocation,
                    onValueChange = { originLocation = it },
                    label = { Text("🚀 SALIDA (Origen)") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = destinationLocation,
                    onValueChange = { destinationLocation = it },
                    label = { Text("🗺️ DESTINO") },
                    placeholder = { Text("Ej: Barquisimeto (Aniversario MC)") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Moto y Placa
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bikeModel,
                        onValueChange = { bikeModel = it },
                        label = { Text("🏍️ Moto") },
                        colors = tfColors,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = bikePlate,
                        onValueChange = { bikePlate = it },
                        label = { Text("🆔 Placa") },
                        colors = tfColors,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Kilometrajes
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialKmStr,
                        onValueChange = { initialKmStr = it },
                        label = { Text("🏁 KM Inicial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = finalKmStr,
                        onValueChange = { finalKmStr = it },
                        label = { Text("🏆 KM Final") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = tfColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Ruta Número y Descripción del Tramo
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentRouteNumberStr,
                        onValueChange = { currentRouteNumberStr = it },
                        label = { Text("Ruta #") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = tfColors,
                        modifier = Modifier.weight(0.5f)
                    )
                    OutlinedTextField(
                        value = routeDescription,
                        onValueChange = { routeDescription = it },
                        label = { Text("Descripción Tramo / Vía") },
                        placeholder = { Text("Ej: San Cristóbal - El Vigía - Barquisimeto") },
                        colors = tfColors,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                // Piloto y Teléfono
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pilotName,
                        onValueChange = { pilotName = it },
                        label = { Text("👨‍🚀 Piloto") },
                        colors = tfColors,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = pilotPhone,
                        onValueChange = { pilotPhone = it },
                        label = { Text("☎️ Teléfono") },
                        colors = tfColors,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Copiloto (Opcional)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = copilotName,
                        onValueChange = { copilotName = it },
                        label = { Text("👨‍🚀 Copiloto (Opcional)") },
                        colors = tfColors,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = copilotPhone,
                        onValueChange = { copilotPhone = it },
                        label = { Text("☎️ Tel Copiloto") },
                        colors = tfColors,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Contacto de Emergencia
                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("☎️ Contactos de Emergencia (Nombre y Tlf)") },
                    placeholder = { Text("Ej: María Pérez (Esposa): 0414-1234567") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Checkpoints del Reto (Toques marcados ✅ / ❎)
                if (challengeCheckpointsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 PUNTOS DE CONTROL / TOQUES:",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color(0xFFD97706)
                    )
                    challengeCheckpointsList.forEach { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { completedMap[place] = !(completedMap[place] ?: false) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = completedMap[place] ?: false,
                                onCheckedChange = { completedMap[place] = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF16A34A))
                            )
                            Text(
                                text = place,
                                fontSize = 12.sp,
                                color = if (completedMap[place] == true) Color(0xFF0F172A) else Color(0xFF64748B),
                                fontWeight = if (completedMap[place] == true) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // KMs Acumulados
                OutlinedTextField(
                    value = accumulatedKmStr,
                    onValueChange = { accumulatedKmStr = it },
                    label = { Text("🛣️ Kms Acumulados Totales") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notas / Observaciones
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota / Observación") },
                    placeholder = { Text("Ej: Ruta sin novedades mecánicas.") },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Foto del Sitio Visitado
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { photoPickerLauncher.launch("image/*") }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MotoOrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedSitePhotoUri != null) "Foto del Sitio seleccionada ✓" else "Foto del Sitio Visitado (1 Foto)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initKm = initialKmStr.toDoubleOrNull() ?: 0.0
                    val finKm = finalKmStr.toDoubleOrNull() ?: 0.0
                    val accumKm = accumulatedKmStr.toDoubleOrNull() ?: (finKm - initKm)
                    val rNum = currentRouteNumberStr.toIntOrNull() ?: 1

                    val formattedCheckpoints = challengeCheckpointsList.joinToString("\n") { place ->
                        val isDone = completedMap[place] == true
                        "$place: ${if (isDone) "✅" else "❎"}"
                    }

                    val progress = UserChallengeProgress(
                        id = existingProgress?.id ?: System.currentTimeMillis(),
                        challengeId = challenge.id,
                        challengeTitle = challenge.title,
                        memberId = currentMember?.id ?: 1L,
                        pilotName = pilotName,
                        pilotPhone = pilotPhone,
                        pilotEmergencyContact = emergencyContact,
                        copilotName = copilotName.ifBlank { null },
                        copilotPhone = copilotPhone.ifBlank { null },
                        bikeModel = bikeModel,
                        bikePlate = bikePlate,
                        departureDate = departureDate,
                        returnDate = returnDate,
                        originLocation = originLocation,
                        destinationLocation = destinationLocation,
                        initialKm = initKm,
                        finalKm = finKm,
                        currentRouteNumber = rNum,
                        routeDescription = routeDescription,
                        completedCheckpoints = formattedCheckpoints,
                        accumulatedKm = accumKm,
                        sitePhotoUrl = existingProgress?.sitePhotoUrl,
                        notes = notes,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(progress, selectedSitePhotoUri)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Guardar Reporte", color = Color.White, fontWeight = FontWeight.Bold)
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

// ═══════════════════════════════════════════════════════════════════════
// GENERADOR Y ENLACE DE REPORTE POR WHATSAPP
// ═══════════════════════════════════════════════════════════════════════
fun shareReportViaWhatsApp(
    context: Context,
    challenge: BikerChallenge?,
    progress: UserChallengeProgress,
    currentMember: MemberProfile?
) {
    val challengeName = challenge?.title ?: progress.challengeTitle
    val kmRecorridos = progress.finalKm - progress.initialKm

    val reportText = buildString {
        appendLine("🏍️💨 REPORTE DE RUTA 🏍️💨")
        appendLine("✨ $challengeName ✨")
        appendLine()
        appendLine("📆 Fecha de Salida: ${progress.departureDate}")
        if (progress.returnDate.isNotBlank()) {
            appendLine("📆 Fecha de Llegada: ${progress.returnDate}")
        }
        appendLine("🚀 SALIDA: ${progress.originLocation}")
        appendLine()
        appendLine("🗺️ DESTINO: ${progress.destinationLocation}")
        appendLine("🏍️ MOTO: ${progress.bikeModel}")
        appendLine("🆔 PLACA: ${progress.bikePlate}")
        appendLine("🏁 KM INICIAL: ${String.format(Locale.US, "%.1f", progress.initialKm)}")
        appendLine("🏆 KM FINAL: ${String.format(Locale.US, "%.1f", progress.finalKm)}")
        appendLine("🛣️ Kms Recorridos de la Ruta #${progress.currentRouteNumber}: ${String.format(Locale.US, "%.1f", kmRecorridos)}")
        if (progress.routeDescription.isNotBlank()) {
            appendLine("🛣️ Ruta #${progress.currentRouteNumber}: ${progress.routeDescription}")
        }
        appendLine("👨‍🚀 PILOTO: ${progress.pilotName}")
        appendLine("☎️ Teléfono: ${progress.pilotPhone}")
        if (!progress.copilotName.isNullOrBlank()) {
            appendLine("👨‍🚀 COPILOTO: ${progress.copilotName}")
            if (!progress.copilotPhone.isNullOrBlank()) {
                appendLine("☎️ Teléfono: ${progress.copilotPhone}")
            }
        }
        if (progress.pilotEmergencyContact.isNotBlank()) {
            appendLine("☎️ Contactos de Emergencia:")
            appendLine(progress.pilotEmergencyContact)
        }
        if (progress.completedCheckpoints.isNotBlank()) {
            appendLine()
            appendLine("Toques / Puntos de Control:")
            appendLine(progress.completedCheckpoints)
        }
        appendLine()
        appendLine("🛣️ Kms Acumulados de $challengeName: ${String.format(Locale.US, "%.1f", progress.accumulatedKm)}")
        if (progress.notes.isNotBlank()) {
            appendLine("Nota: ${progress.notes}")
        }
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reportText)
            setPackage("com.whatsapp")
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        // Fallback a cualquier app de mensajería si WhatsApp no está disponible
        try {
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reportText)
            }
            context.startActivity(Intent.createChooser(generalIntent, "Compartir Reporte de Ruta"))
        } catch (ex: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
}
