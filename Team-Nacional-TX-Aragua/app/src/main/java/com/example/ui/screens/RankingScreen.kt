package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
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
    RETOS("Retos & Desafíos", Icons.Default.MilitaryTech),
    DESTACADOS("Destacados", Icons.Default.Star)
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
    BikerHonorRank("Aspirante TX", 0, 299, Color(0xFF64748B), Icons.Default.Person, "Iniciando el camino motero en el club."),
    BikerHonorRank("Piloto de Ruta", 300, 799, Color(0xFF0288D1), Icons.Default.TwoWheeler, "Rodador activo en caravanas oficiales."),
    BikerHonorRank("Capitán de Asfalto", 800, 1999, Color(0xFFD97706), Icons.Default.MilitaryTech, "Veterano de carreteras y rutas interurbanas."),
    BikerHonorRank("Centurión Legendario", 2000, 4999, TxFlameRed, Icons.Default.Shield, "Líder de asfalto con miles de kilómetros y asistencias."),
    BikerHonorRank("Titán del Asfalto", 5000, Int.MAX_VALUE, Color(0xFFB45309), Icons.Default.WorkspacePremium, "Máxima gloria y leyenda viviente del Team TX.")
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
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(RankingCategoryFilter.GENERAL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedChapterFilter by remember { mutableStateOf<String?>(null) }
    var ratingTargetMember by remember { mutableStateOf<MemberProfile?>(null) }
    var showMeritInfoDialog by remember { mutableStateOf(false) }

    val chapters = listOf(
        "TODOS", "Aragua", "Carabobo", "Distrito Capital", "Miranda", "Lara",
        "Falcón", "Zulia", "Táchira", "Mérida", "Guárico", "Anzoátegui", "Bolívar", "Yaracuy", "Portuguesa", "Barinas"
    )

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
            val matchesChapter = selectedChapterFilter == null ||
                    selectedChapterFilter == "TODOS" ||
                    member.chapterState.equals(selectedChapterFilter, ignoreCase = true)
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
                                    tint = Color(0xFFD97706),
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
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Gamificación y Récords de Pilotos",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
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
                    // Botón Compartir Ranking
                    IconButton(onClick = {
                        shareRankingViaWhatsApp(context, rankedMembers.take(5), selectedFilter.displayName)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir Podio", tint = Color(0xFF0F172A))
                    }

                    // Botón Ayuda / Explicación del Sistema de Mérito
                    IconButton(onClick = { showMeritInfoDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Sistema de Mérito", tint = Color(0xFFD97706))
                    }

                    // Botón Volver al Inicio
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onBack)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Volver al Inicio",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Inicio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // BARRA DE BÚSQUEDA Y FILTROS RÁPIDOS EN TEMA CLARO
            // ═══════════════════════════════════════════════════════════════════
            Surface(
                color = Color.White,
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    // Campo de Búsqueda
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar piloto por nombre, apodo o placa...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("input_search_ranking")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Selector de Categorías Gamificadas
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(RankingCategoryFilter.values()) { cat ->
                            val isSelected = selectedFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = cat },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Black else Color(0xFF334155),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(cat.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MotoOrangePrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFFE2E8F0),
                                    selectedBorderColor = MotoOrangePrimary,
                                    borderWidth = 1.dp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. NUEVO: Selector Deslizable de Capítulos / Estados
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(chapters) { chap ->
                            val isCurrentChap = (selectedChapterFilter == null && chap == "TODOS") || (selectedChapterFilter == chap)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrentChap) Color(0xFF0288D1) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isCurrentChap) Color(0xFF0288D1) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedChapterFilter = if (chap == "TODOS") null else chap
                                    }
                            ) {
                                Text(
                                    text = if (chap == "TODOS") "🇻🇪 Todo el País" else "📍 $chap",
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrentChap) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrentChap) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // CONTENIDO DEL RANKING (PODIO + TABLA)
            // ═══════════════════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Podio Visual Top 3 (Solo si no hay búsqueda activa)
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
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TABLA DE CLASIFICACIÓN (${rankedMembers.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF475569),
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
                                    tint = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No se encontraron pilotos con ese filtro",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
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
        }
    }

    // Diálogo Modal para Calificar al Piloto
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

    // Diálogo Modal: Explicación del Sistema de Mérito y Rangos
    if (showMeritInfoDialog) {
        MeritSystemInfoDialog(onDismiss = { showMeritInfoDialog = false })
    }
}

/**
 * Podio Visual Top 3 (Oro, Plata, Bronce) en Tema Claro
 */
@Composable
fun VisualPodiumView(
    topMembers: List<MemberProfile>,
    onOpenMemberCarnet: (MemberProfile) -> Unit,
    onRateMember: (MemberProfile) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                Text(
                    text = "PODIO DE HONOR TEAM TX",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color(0xFF92400E),
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        stepColor = Color(0xFF64748B),
                        crownColor = Color(0xFF94A3B8),
                        fillColor = Color(0xFFF1F5F9),
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
                        stepColor = Color(0xFFB45309),
                        crownColor = Color(0xFFD97706),
                        fillColor = Color(0xFFFEF08A),
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
                        stepColor = Color(0xFFC2410C),
                        crownColor = Color(0xFFEA580C),
                        fillColor = Color(0xFFFFEDD5),
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
    fillColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .background(Color(0xFFE2E8F0))
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
                    color = Color(0xFF0F172A)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (member.nickname.isNotBlank()) member.nickname else member.fullName.split(" ").firstOrNull() ?: "Piloto",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color(0xFF0F172A),
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

        // Escalón del Podio en Tema Claro
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = fillColor,
            border = BorderStroke(1.dp, stepColor.copy(alpha = 0.5f)),
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
 * Tarjeta Individual de Piloto en la Lista del Ranking (Tema Claro)
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
    val meritPoints = calculateMemberMeritPoints(member)
    val rank = getMemberHonorRank(meritPoints)

    val positionBadgeColor = when (position) {
        1 -> Color(0xFFFEF08A)
        2 -> Color(0xFFE2E8F0)
        3 -> Color(0xFFFFEDD5)
        else -> Color(0xFFF1F5F9)
    }

    val positionTextColor = when (position) {
        1 -> Color(0xFF854D0E)
        2 -> Color(0xFF334155)
        3 -> Color(0xFF9A3412)
        else -> Color(0xFF475569)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFFFFFBEB) else Color.White
        ),
        border = BorderStroke(
            if (isCurrentUser) 1.5.dp else 1.dp,
            if (isCurrentUser) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                border = BorderStroke(1.dp, positionTextColor.copy(alpha = 0.3f)),
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
                    .background(Color(0xFFE2E8F0))
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
                        color = Color(0xFF0F172A)
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
                        color = Color(0xFF0F172A),
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
                    Text(text = "•", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = if (member.bikeModel.isNotBlank()) "${member.bikeBrand} ${member.bikeModel}" else "Keeway TX 200",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
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
                        tint = Color(0xFF16A34A)
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
                        value = "${member.totalKmRidden.toInt()} km",
                        tint = Color(0xFFD97706)
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

            // Puntuación Principal y Botones de Acción
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFB45309)
                )
                Text(
                    text = metricUnit,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Calificar (Solo si no soy yo)
                    if (!isCurrentUser) {
                        Surface(
                            onClick = onRateClick,
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = Color(0xFF92400E),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Calificar",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    // Botón Ver Carnet Digital
                    Surface(
                        onClick = onClick,
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = "Carnet",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Carnet",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
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
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
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
                color = Color(0xFF334155)
            )
        }
    }
}

/**
 * Diálogo Modal: Explicación del Sistema de Méritos y Rangos de Honor
 */
@Composable
fun MeritSystemInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFD97706))
                Text("Sistema de Mérito & Rangos", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "El Ranking Team TX premia la constancia, la hermandad en carretera, la participación en caravanas y el auxilio a hermanos caídos.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                }

                item {
                    Text("Puntos de Mérito por Acción:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🛣️ 10 KM Recorridos en Moto = +1 Punto", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("🏍️ Asistencia a Rodada Oficial = +50 Puntos", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("🏁 Rodada de Fondo (>100 KM) = +100 Puntos", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("🏆 Evento Nacional / Aniversario = +200 Puntos", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("🚨 Asistencia a Emergencia SOS Vial = +150 Puntos", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            Text("🎖️ Reto Motero Completado = +75 Puntos", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("👍 Calificación Positiva de Hermano = +15 Puntos", fontSize = 11.sp, color = Color(0xFF16A34A))
                            Text("👎 Falta o Llamado de Atención = -15 Puntos", fontSize = 11.sp, color = TxFlameRed)
                        }
                    }
                }

                item {
                    Text("Rangos Honoríficos TX:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        BIKER_RANKS.forEach { r ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(r.icon, contentDescription = null, tint = r.badgeColor, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(r.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = r.badgeColor)
                                        Text(r.description, fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Text("${r.minPoints} - ${if (r.maxPoints == Int.MAX_VALUE) "+" else r.maxPoints.toString()} pts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White
    )
}

/**
 * Compartir Podio del Ranking por WhatsApp
 */
fun shareRankingViaWhatsApp(context: Context, topPilots: List<MemberProfile>, categoryName: String) {
    val shareText = buildString {
        appendLine("🏆🏍️ RANKING OFICIAL TEAM TX VENEZUELA 🏍️🏆")
        appendLine("📊 Categoría: $categoryName")
        appendLine("📅 Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}")
        appendLine()
        appendLine("🎖️ CUADRO DE HONOR Y LÍDERES:")
        topPilots.forEachIndexed { index, pilot ->
            val pts = calculateMemberMeritPoints(pilot)
            val rank = getMemberHonorRank(pts)
            val medal = when (index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "#${index + 1}"
            }
            appendLine("$medal ${pilot.fullName} (${pilot.nickname.ifBlank { "Piloto" }})")
            appendLine("   🎖️ ${rank.title} • $pts Puntos • ${pilot.chapterState}")
        }
        appendLine()
        appendLine("¡Suma kilómetros, asiste a las rodadas y lidera el podio con la App Oficial Team TX!")
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            setPackage("com.whatsapp")
        }
        context.startActivity(sendIntent)
    } catch (_: Exception) {
        try {
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(generalIntent, "Compartir Ranking Team TX"))
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo compartir el ranking", Toast.LENGTH_SHORT).show()
        }
    }
}
