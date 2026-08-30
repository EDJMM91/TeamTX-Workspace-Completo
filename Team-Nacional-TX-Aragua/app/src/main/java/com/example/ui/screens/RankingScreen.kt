package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.example.ui.components.RatePilotDialog
import com.example.ui.theme.*

private const val TAG_LOGCAT = "TEAM_TX_RANKING"

enum class RankingCategoryFilter(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GENERAL("Mérito General", Icons.Default.EmojiEvents),
    REPUTACION("Reputación Biker", Icons.Default.ThumbUp),
    KILOMETRAJE("Más KM Recorridos", Icons.Default.Speed),
    ASISTENCIA("Rodadas & Eventos", Icons.Default.TwoWheeler),
    HEROES_SOS("Héroes SOS Vial", Icons.Default.Emergency),
    RETOS("Retos & Retos KM", Icons.Default.MilitaryTech),
    DESTACADOS("Destacados del Mes", Icons.Default.Star)
}

/**
 * Rango y Título de Honor Gamificado según Puntos de Mérito
 */
data class BikerHonorRank(
    val title: String,
    val minPoints: Int,
    val maxPoints: Int,
    val badgeColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

val BIKER_RANKS = listOf(
    BikerHonorRank("Aspirante TX", 0, 299, Color(0xFF9E9E9E), Icons.Default.Person, "Iniciando el camino motero en el club."),
    BikerHonorRank("Piloto de Ruta", 300, 799, Color(0xFF42A5F5), Icons.Default.TwoWheeler, "Rodador activo en caravanas oficiales."),
    BikerHonorRank("Capitán de Asfalto", 800, 1999, MotoGoldSecondary, Icons.Default.MilitaryTech, "Veterano de carreteras y rutas interurbanas."),
    BikerHonorRank("Centurión Legendario", 2000, 4999, TxFlameRed, Icons.Default.Shield, "Líder de asfalto con miles de kilómetros y asistencias."),
    BikerHonorRank("Titán del Asfalto", 5000, Int.MAX_VALUE, Color(0xFFFFD700), Icons.Default.WorkspacePremium, "Máxima gloria y leyenda viviente del Team TX.")
)

fun calculateMemberMeritPoints(member: MemberProfile): Int {
    val kmPoints = (member.totalKmRidden / 10.0).toInt()
    val attendancePoints = member.attendanceCount * 50
    val longRidesPoints = member.longRidesCount * 100
    val bigEventsPoints = member.bigEventsCount * 200
    val sosPoints = member.sosAssistanceCount * 150
    val challengePoints = member.challengesCompletedCount * 75
    val ratingNet = (member.positiveRatingsCount * 15) - (member.negativeRatingsCount * 15) + member.reputationPoints
    return (kmPoints + attendancePoints + longRidesPoints + bigEventsPoints + sosPoints + challengePoints + ratingNet).coerceAtLeast(0)
}

fun getMemberHonorRank(points: Int): BikerHonorRank {
    return BIKER_RANKS.find { points in it.minPoints..it.maxPoints } ?: BIKER_RANKS.first()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    allMembers: List<MemberProfile>,
    currentMember: MemberProfile? = null,
    onOpenMemberCarnet: (MemberProfile) -> Unit = {},
    onRateMember: (MemberProfile, Boolean, String, Int, String) -> Unit = { _, _, _, _, _ -> },
    onBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var selectedFilter by remember { mutableStateOf(RankingCategoryFilter.GENERAL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedChapterFilter by remember { mutableStateOf<String?>(null) }
    var ratingTargetMember by remember { mutableStateOf<MemberProfile?>(null) }

    // Logcat inicial de auditoría
    LaunchedEffect(Unit) {
        Log.d(TAG_LOGCAT, "📊 Módulo Ranking inicializado con ${allMembers.size} pilotos registrados")
    }

    // Lista ordenada según la categoría seleccionada
    val rankedMembers = remember(allMembers, selectedFilter, searchQuery, selectedChapterFilter) {
        val filtered = allMembers.filter { member ->
            val matchesSearch = searchQuery.isBlank() ||
                    member.fullName.contains(searchQuery, ignoreCase = true) ||
                    member.nickname.contains(searchQuery, ignoreCase = true) ||
                    member.bikePlate.contains(searchQuery, ignoreCase = true)
            val matchesChapter = selectedChapterFilter == null || member.chapterState.equals(selectedChapterFilter, ignoreCase = true)
            matchesSearch && matchesChapter
        }

        when (selectedFilter) {
            RankingCategoryFilter.GENERAL -> filtered.sortedByDescending { calculateMemberMeritPoints(it) }
            RankingCategoryFilter.REPUTACION -> filtered.sortedByDescending { (it.positiveRatingsCount * 15) - (it.negativeRatingsCount * 15) + it.reputationPoints }
            RankingCategoryFilter.KILOMETRAJE -> filtered.sortedByDescending { it.totalKmRidden }
            RankingCategoryFilter.ASISTENCIA -> filtered.sortedByDescending { it.attendanceCount + it.longRidesCount * 2 + it.bigEventsCount * 3 }
            RankingCategoryFilter.HEROES_SOS -> filtered.sortedByDescending { it.sosAssistanceCount }
            RankingCategoryFilter.RETOS -> filtered.sortedByDescending { it.challengesCompletedCount }
            RankingCategoryFilter.DESTACADOS -> filtered.sortedByDescending { calculateMemberMeritPoints(it) + (if (it.isOnline) 100 else 0) }
        }
    }

    val top3 = remember(rankedMembers) { rankedMembers.take(3) }
    val remainingMembers = remember(rankedMembers) { if (rankedMembers.size > 3) rankedMembers.drop(3) else emptyList() }

    // Posición del usuario actual
    val myPosition = remember(rankedMembers, currentMember) {
        if (currentMember != null) {
            val idx = rankedMembers.indexOfFirst { it.id == currentMember.id }
            if (idx >= 0) idx + 1 else null
        } else null
    }

    val myPoints = remember(currentMember) {
        if (currentMember != null) calculateMemberMeritPoints(currentMember) else 0
    }
    val myRank = remember(myPoints) { getMemberHonorRank(myPoints) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MotoGoldSecondary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = MotoGoldSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RANKING & MÉRITO TX",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Gamificación y Récords de Pilotos",
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF121212) else Color.White
                )
            )
        },
        containerColor = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF1F5F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // BARRA DE BÚSQUEDA Y FILTROS RÁPIDOS
            // ═══════════════════════════════════════════════════════════════════
            Surface(
                color = if (isDark) Color(0xFF18181B) else Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    // Campo de Búsqueda
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar piloto por nombre, apodo o placa...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC),
                            focusedContainerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("input_search_ranking")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Categorías / Filtros Gamificados
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(RankingCategoryFilter.values()) { cat ->
                            val isSelected = selectedFilter == cat
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MotoOrangePrimary else (if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedFilter = cat }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else (if (isDark) Color.White else Color(0xFF1E293B)),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = cat.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else (if (isDark) Color.White else Color(0xFF1E293B))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // CONTENIDO DEL RANKING (PODIO + LISTA)
            // ═══════════════════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Podio Visual Top 3 (Solo en categoría General o cuando hay al menos 2 pilotos)
                if (top3.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        VisualPodiumView(
                            topMembers = top3,
                            onOpenMemberCarnet = onOpenMemberCarnet,
                            onRateMember = { ratingTargetMember = it }
                        )
                    }
                }

                // Encabezado de la Tabla
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TABLA DE CLASIFICACIÓN (${rankedMembers.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = when (selectedFilter) {
                                RankingCategoryFilter.GENERAL -> "Puntos de Mérito"
                                RankingCategoryFilter.REPUTACION -> "Reputación (+Likes/-Dislikes)"
                                RankingCategoryFilter.KILOMETRAJE -> "KM Totales"
                                RankingCategoryFilter.ASISTENCIA -> "Rodadas"
                                RankingCategoryFilter.HEROES_SOS -> "Auxilios SOS"
                                RankingCategoryFilter.RETOS -> "Retos"
                                RankingCategoryFilter.DESTACADOS -> "Actividad"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotoOrangePrimary
                        )
                    }
                }

                if (rankedMembers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No se encontraron pilotos con ese filtro",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(rankedMembers) { index, member ->
                        val position = index + 1
                        val isCurrentUser = currentMember != null && member.id == currentMember.id
                        RankingPilotCard(
                            position = position,
                            member = member,
                            isCurrentUser = isCurrentUser,
                            filterCategory = selectedFilter,
                            onClick = { onOpenMemberCarnet(member) },
                            onRateClick = { ratingTargetMember = member }
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // TARJETA FLOTANTE FIJA: "MI POSICIÓN ACTUAL EN EL RANKING"
            // ═══════════════════════════════════════════════════════════════════
            if (currentMember != null && myPosition != null) {
                Surface(
                    color = if (isDark) Color(0xFF18181B) else Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MotoGoldSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MotoOrangePrimary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#$myPosition",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }
                            }

                            Column {
                                val myPoints = calculateMemberMeritPoints(currentMember)
                                val myRank = getMemberHonorRank(myPoints)
                                Text(
                                    text = "Tu Posición: ${currentMember.fullName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${myRank.title} • $myPoints pts",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = myRank.badgeColor
                                )
                            }
                        }

                        Button(
                            onClick = { onOpenMemberCarnet(currentMember) },
                            colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mi Carnet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (ratingTargetMember != null) {
        RatePilotDialog(
            targetMember = ratingTargetMember!!,
            currentMember = currentMember,
            onDismiss = { ratingTargetMember = null },
            onConfirmRating = { isPositive, category, pointsDelta, comment ->
                onRateMember(ratingTargetMember!!, isPositive, category, pointsDelta, comment)
            }
        )
    }
}

/**
 * Podio Visual Top 3 (Oro, Plata, Bronce)
 */
@Composable
fun VisualPodiumView(
    topMembers: List<MemberProfile>,
    onOpenMemberCarnet: (MemberProfile) -> Unit,
    onRateMember: (MemberProfile) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF18181B) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MotoGoldSecondary, modifier = Modifier.size(18.dp))
                Text(
                    text = "PODIO DE HONOR TEAM TX",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = MotoGoldSecondary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 🥈 PUESTO 2 (Plata)
                if (topMembers.size >= 2) {
                    val p2 = topMembers[1]
                    PodiumStepItem(
                        position = 2,
                        member = p2,
                        stepHeight = 85.dp,
                        stepColor = Color(0xFF94A3B8),
                        crownColor = Color(0xFFCBD5E1),
                        onClick = { onOpenMemberCarnet(p2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 🥇 PUESTO 1 (Oro - Al Centro y Más Alto)
                if (topMembers.isNotEmpty()) {
                    val p1 = topMembers[0]
                    PodiumStepItem(
                        position = 1,
                        member = p1,
                        stepHeight = 110.dp,
                        stepColor = Color(0xFFFFD700),
                        crownColor = Color(0xFFFFD700),
                        onClick = { onOpenMemberCarnet(p1) },
                        modifier = Modifier.weight(1.15f)
                    )
                }

                // 🥉 PUESTO 3 (Bronce)
                if (topMembers.size >= 3) {
                    val p3 = topMembers[2]
                    PodiumStepItem(
                        position = 3,
                        member = p3,
                        stepHeight = 65.dp,
                        stepColor = Color(0xFFCD7F32),
                        crownColor = Color(0xFFD97706),
                        onClick = { onOpenMemberCarnet(p3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PodiumStepItem(
    position: Int,
    member: MemberProfile,
    stepHeight: androidx.compose.ui.unit.Dp,
    stepColor: Color,
    crownColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val points = calculateMemberMeritPoints(member)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        // Corona / Medalla
        Icon(
            imageVector = if (position == 1) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech,
            contentDescription = null,
            tint = crownColor,
            modifier = Modifier.size(if (position == 1) 24.dp else 18.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Avatar con Borde de Corona
        Box(
            modifier = Modifier
                .size(if (position == 1) 52.dp else 44.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF262626) else Color(0xFFE2E8F0))
                .border(2.dp, crownColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!member.profilePhotoUri.isNullOrBlank()) {
                AsyncImage(
                    model = member.profilePhotoUri,
                    contentDescription = member.fullName,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = if (member.nickname.isNotBlank()) member.nickname.take(2).uppercase() else member.avatarInitials,
                    fontWeight = FontWeight.Black,
                    fontSize = if (position == 1) 14.sp else 12.sp,
                    color = if (isDark) Color.White else Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (member.nickname.isNotBlank()) member.nickname else member.fullName.split(" ").firstOrNull() ?: "Piloto",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "$points pts",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = stepColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Escalón del Podio
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = stepColor.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, stepColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(stepHeight)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$position",
                    fontSize = if (position == 1) 22.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    color = stepColor
                )
            }
        }
    }
}

/**
 * Tarjeta Individual de Piloto en la Lista del Ranking
 */
@Composable
fun RankingPilotCard(
    position: Int,
    member: MemberProfile,
    isCurrentUser: Boolean,
    filterCategory: RankingCategoryFilter,
    onClick: () -> Unit,
    onRateClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val meritPoints = calculateMemberMeritPoints(member)
    val rank = getMemberHonorRank(meritPoints)

    val positionBadgeColor = when (position) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFCBD5E1)
        3 -> Color(0xFFD97706)
        else -> if (isDark) Color(0xFF3F3F46) else Color(0xFFE2E8F0)
    }

    val positionTextColor = when (position) {
        1, 2, 3 -> Color.Black
        else -> if (isDark) Color.White else Color(0xFF334155)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentUser) {
            MotoOrangePrimary.copy(alpha = 0.12f)
        } else {
            if (isDark) Color(0xFF18181B) else Color.White
        },
        border = BorderStroke(
            if (isCurrentUser) 1.5.dp else 1.dp,
            if (isCurrentUser) MotoOrangePrimary else (if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("ranking_item_${member.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Posición Numérica
            Surface(
                shape = CircleShape,
                color = positionBadgeColor,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$position",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = positionTextColor
                    )
                }
            }

            // Foto / Avatar de Carnet TX
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF262626) else Color(0xFFE2E8F0))
                    .border(1.5.dp, rank.badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!member.profilePhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = member.profilePhotoUri,
                        contentDescription = member.fullName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = if (member.nickname.isNotBlank()) member.nickname.take(2).uppercase() else member.avatarInitials,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            }

            // Datos del Piloto y Méritos
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = member.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MotoOrangePrimary
                        ) {
                            Text(
                                text = "TÚ",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = rank.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = rank.badgeColor
                    )
                    Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = if (member.bikeModel.isNotBlank()) "${member.bikeBrand} ${member.bikeModel}" else "TX 200",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Métricas Rápidas en Píldoras
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricChip(
                        icon = Icons.Default.ThumbUp,
                        value = "${member.positiveRatingsCount}",
                        tint = Color(0xFF22C55E)
                    )
                    if (member.negativeRatingsCount > 0) {
                        MetricChip(
                            icon = Icons.Default.ThumbDown,
                            value = "${member.negativeRatingsCount}",
                            tint = TxFlameRed
                        )
                    }
                    MetricChip(
                        icon = Icons.Default.Speed,
                        value = "${member.totalKmRidden.toInt()}k",
                        tint = MotoGoldSecondary
                    )
                    if (member.sosAssistanceCount > 0) {
                        MetricChip(
                            icon = Icons.Default.Emergency,
                            value = "${member.sosAssistanceCount}",
                            tint = StatusError
                        )
                    }
                }
            }

            // Puntuación Principal Destacada según Filtro y Botón Calificar
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val primaryMetricText = when (filterCategory) {
                    RankingCategoryFilter.GENERAL, RankingCategoryFilter.DESTACADOS -> "$meritPoints"
                    RankingCategoryFilter.REPUTACION -> "+${member.positiveRatingsCount} / -${member.negativeRatingsCount}"
                    RankingCategoryFilter.KILOMETRAJE -> String.format("%.1f", member.totalKmRidden)
                    RankingCategoryFilter.ASISTENCIA -> "${member.attendanceCount}"
                    RankingCategoryFilter.HEROES_SOS -> "${member.sosAssistanceCount}"
                    RankingCategoryFilter.RETOS -> "${member.challengesCompletedCount}"
                }

                val metricUnit = when (filterCategory) {
                    RankingCategoryFilter.GENERAL, RankingCategoryFilter.DESTACADOS -> "PTS"
                    RankingCategoryFilter.REPUTACION -> "VOTOS"
                    RankingCategoryFilter.KILOMETRAJE -> "KM"
                    RankingCategoryFilter.ASISTENCIA -> "RUTAS"
                    RankingCategoryFilter.HEROES_SOS -> "AUXILIOS"
                    RankingCategoryFilter.RETOS -> "RETOS"
                }

                Text(
                    text = primaryMetricText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MotoOrangePrimary
                )
                Text(
                    text = metricUnit,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                if (!isCurrentUser) {
                    Surface(
                        onClick = onRateClick,
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, MotoGoldSecondary.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = MotoGoldSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Calificar",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MotoGoldSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(10.dp))
            Text(
                text = value,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            )
        }
    }
}
