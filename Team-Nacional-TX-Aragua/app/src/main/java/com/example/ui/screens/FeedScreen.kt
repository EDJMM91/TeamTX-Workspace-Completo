package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.DigitalCredentialCard
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.model.*
import com.example.dashboard.DashboardFondoConfig
import com.example.dashboard.TipoFondoDashboard
import com.example.ui.components.openUrl
import com.example.ui.theme.*
import com.example.mapa.PuenteMapa
import com.example.mapa.GestorPortapapeles
import com.example.mapa.AnalizadorCoordenadas
import com.example.mapa.GestorSeleccionMapa
import com.example.chat.GestorUbicacion
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    publications: List<Publication>,
    comments: List<NoticeComment> = emptyList(),
    currentMember: MemberProfile? = null,
    allMembers: List<MemberProfile> = emptyList(),
    isDirectivaMode: Boolean,
    onLike: (Publication) -> Unit,
    onDelete: (Long) -> Unit,
    onUpdatePublication: (Publication, Uri?) -> Unit = { _, _ -> },
    onAddComment: (publicationId: Long, content: String) -> Unit = { _, _ -> },
    onCreatePublication: (
        title: String,
        content: String,
        category: NoticeCategory,
        priority: NoticePriority,
        isPinned: Boolean,
        challengeKm: Int,
        challengeBadge: String?,
        telegramUrl: String?,
        imageUri: Uri?,
        allowComments: Boolean,
        locationCoordinates: String?,
        locationName: String?,
        eventDate: String?,
        eventTime: String?,
        syncWithCalendar: Boolean
    ) -> Unit,
    onShare: (Publication) -> Unit = {},
    onSave: (Publication) -> Unit = {},
    dismissedNoticeIds: Set<Long> = emptySet(),
    onDismissNotice: (Long) -> Unit = {},
    onClearAllNotices: (List<Long>) -> Unit = {},
    onRestoreDismissedNotices: () -> Unit = {},
    onNavigateToCalendar: (String) -> Unit = {},
    onToggleEventFinished: (Publication, Boolean) -> Unit = { _, _ -> },
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    uploadError: String? = null,
    uploadSuccess: Boolean = false,
    onDismissUploadStatus: () -> Unit = {},
    allWorkshops: List<WorkshopDirectoryItem> = emptyList(),
    allPrivateGroups: List<PrivateGroup> = emptyList(),
    onNavigateToTab: ((com.example.NavigationTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<NoticeCategory?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPublicationForEdit by remember { mutableStateOf<Publication?>(null) }
    var viewingFlyerPublication by remember { mutableStateOf<Publication?>(null) }
    var sharingPublication by remember { mutableStateOf<Publication?>(null) }
    val context = LocalContext.current
    val isLight = true
    val isDark = false
    val screenBg = DashboardFondoConfig.ColorFondoClaro
    val cardBg = DashboardFondoConfig.ColorTarjetaClara
    val textColorPrimary = DashboardFondoConfig.ColorTextoPrimario
    val textColorSecondary = DashboardFondoConfig.ColorTextoSecundario

    // Auto-dismiss upload status banner after 4 seconds
    LaunchedEffect(uploadError, uploadSuccess) {
        if (uploadError != null || uploadSuccess) {
            delay(4000)
            onDismissUploadStatus()
        }
    }

    val validPublications = remember(publications, dismissedNoticeIds) {
        publications.filter {
            val t = it.title.trim()
            val c = it.content.trim()
            val isDefaultOnly = (t.equals("Aviso Oficial", ignoreCase = true) && c.isEmpty()) || (c.equals("Aviso Oficial", ignoreCase = true) && t.isEmpty())
            val isValid = (t.isNotBlank() || c.isNotBlank()) && !isDefaultOnly && it.id != 0L
            isValid && !dismissedNoticeIds.contains(it.id)
        }
    }

    val filteredList = remember(validPublications, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) validPublications
        else validPublications.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        containerColor = screenBg,
        floatingActionButton = {
            if (isDirectivaMode) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Publicar Aviso / Reto", fontWeight = FontWeight.Bold) },
                    containerColor = TxFlameRed,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_create_notice")
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) { innerPadding ->
        // Upload status banner
        if (uploadError != null || uploadSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = innerPadding.calculateTopPadding() + 8.dp)
                    .testTag("upload_status_banner"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = uploadError != null || uploadSuccess,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uploadError != null) StatusError else StatusSuccess
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (uploadError != null) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (uploadError != null) uploadError else "¡Aviso publicado correctamente!",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onDismissUploadStatus) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
        ) {
            // Incomplete Profile Warning Banner
            if (currentMember != null && !currentMember.isProfileComplete) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isLight) Color(0xFF261908) else Color(0xFFFFF8E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "FICHA DE PERFIL INCOMPLETA",
                                    fontWeight = FontWeight.Black,
                                    color = if (!isLight) TxGoldLight else Color(0xFFB45309),
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Por favor completa tus datos personales, contacto SOS y fotos en Carnet TX.",
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // PANEL DE AVISOS INTELIGENTES: GAMIFICACIÓN, RÉCORDS Y SERVICIOS
            // ═══════════════════════════════════════════════════════════════
            item {
                AvisosComunidadInteligentesSection(
                    allMembers = allMembers,
                    allWorkshops = allWorkshops,
                    allPrivateGroups = allPrivateGroups,
                    currentMember = currentMember,
                    onNavigateToTab = onNavigateToTab
                )
            }

            // Birthdays of the Month
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val birthdaysThisMonth = allMembers.filter { member ->
                try {
                    if (member.birthDate.isNotBlank()) {
                        val parts = member.birthDate.split("/")
                        if (parts.size >= 2) {
                            val month = parts[1].toInt()
                            month == currentMonth
                        } else false
                    } else false
                } catch (e: Exception) {
                    false
                }
            }.sortedBy { member ->
                try {
                    val parts = member.birthDate.split("/")
                    if (parts.isNotEmpty()) parts[0].toInt() else 0
                } catch (e: Exception) {
                    0
                }
            }

            if (birthdaysThisMonth.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DashboardFondoConfig.ColorTarjetaClara
                        ),
                        border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Cake, contentDescription = null, tint = TxFlameRed)
                                Text(
                                    text = "¡CUMPLEAÑEROS DEL MES!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            birthdaysThisMonth.forEach { member ->
                                val day = member.birthDate.split("/").firstOrNull() ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(TxFlameRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TxFlameRed
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "${member.fullName} (${member.memberNumber})",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color.White else Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = member.role.displayName,
                                                fontSize = 10.sp,
                                                color = if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Categories Filter Bar (Chips sin bordes: Activo rojo sólido, Inactivo gris suave)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Muro y Tablero de Anuncios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (filteredList.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { onClearAllNotices(filteredList.map { it.id }) },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Limpiar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_refresh_feed")
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = TxFlameRed
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Actualizar avisos",
                                        tint = TxFlameRed
                                    )
                                }
                            }
                        }
                    }

                    if (dismissedNoticeIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onRestoreDismissedNotices)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MotoGoldSecondary, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = "${dismissedNoticeIds.size} avisos ocultados de tu pantalla",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                                    )
                                }
                                Text(
                                    text = "Restaurar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MotoOrangePrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = "Todos (${publications.size})"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.AVISO_OFICIAL,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.AVISO_OFICIAL) null else NoticeCategory.AVISO_OFICIAL
                                },
                                label = "Avisos"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.RETO_MOTERO,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.RETO_MOTERO) null else NoticeCategory.RETO_MOTERO
                                },
                                label = "Retos TX"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.CAPACITACION,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.CAPACITACION) null else NoticeCategory.CAPACITACION
                                },
                                label = "Mecánica"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.COMUNICADO,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.COMUNICADO) null else NoticeCategory.COMUNICADO
                                },
                                label = "Comunicados"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.EMERGENCIA,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.EMERGENCIA) null else NoticeCategory.EMERGENCIA
                                },
                                label = "🚨 Emergencias SOS"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.OBRA_BENEFICA,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.OBRA_BENEFICA) null else NoticeCategory.OBRA_BENEFICA
                                },
                                label = "❤️ Obra Benéfica"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.MANTENIMIENTO_PREVENTIVO,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.MANTENIMIENTO_PREVENTIVO) null else NoticeCategory.MANTENIMIENTO_PREVENTIVO
                                },
                                label = "🔧 Mantenimiento Preventivo"
                            )
                        }
                        item {
                            FeedFilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.LAVADO_FAMILIAR,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.LAVADO_FAMILIAR) null else NoticeCategory.LAVADO_FAMILIAR
                                },
                                label = "🧼 Lavado en Familia"
                            )
                        }
                    }
                }
            }

            // Publication items
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = MotoGoldSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (dismissedNoticeIds.isNotEmpty()) "¡Pantalla limpia! Todos los avisos fueron leídos." else "No hay avisos en esta categoría",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (dismissedNoticeIds.isNotEmpty()) {
                                TextButton(onClick = onRestoreDismissedNotices) {
                                    Text("Ver avisos ocultados", color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { pub ->
                    val pubComments = remember(comments, pub.id) {
                        comments.filter { it.publicationId == pub.id }
                    }
                    NoticeCard(
                        pub = pub,
                        comments = pubComments,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        onLike = { onLike(pub) },
                        onDelete = { onDelete(pub.id) },
                        onEdit = { selectedPublicationForEdit = pub },
                        onAddComment = { content -> onAddComment(pub.id, content) },
                        onViewFlyer = { viewingFlyerPublication = pub },
                        onOpenShareDialog = { sharingPublication = pub },
                        onDismissNotice = { onDismissNotice(pub.id) },
                        onNavigateToCalendar = { dateStr -> onNavigateToCalendar(dateStr) },
                        onToggleEventFinished = { onToggleEventFinished(pub, !pub.isEventFinished) },
                        onQuickSave = {
                            if (!pub.imageUrl.isNullOrBlank()) {
                                saveFlyerToGallery(context, pub.imageUrl!!, pub) {
                                    onSave(pub)
                                }
                            } else {
                                copyNoticeTextForStatus(context, pub) {
                                    onSave(pub)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (viewingFlyerPublication != null) {
        FlyerImageViewerDialog(
            pub = viewingFlyerPublication!!,
            onDismiss = { viewingFlyerPublication = null },
            onSave = {
                val p = viewingFlyerPublication
                if (p != null && !p.imageUrl.isNullOrBlank()) {
                    saveFlyerToGallery(context, p.imageUrl!!, p) {
                        onSave(p)
                    }
                }
            },
            onShare = {
                val p = viewingFlyerPublication
                if (p != null) {
                    shareNotice(context, p) {
                        onShare(p)
                    }
                }
            },
            onCopyStatusText = {
                val p = viewingFlyerPublication
                if (p != null) {
                    copyNoticeTextForStatus(context, p) {
                        onSave(p)
                    }
                }
            }
        )
    }

    // Modal para compartir y descargar
    if (sharingPublication != null) {
        ShareNoticeDialog(
            pub = sharingPublication!!,
            onDismiss = { sharingPublication = null },
            onShareWhatsApp = {
                val p = sharingPublication!!
                shareNotice(context, p) { onShare(p) }
                sharingPublication = null
            },
            onCopyStatus = {
                val p = sharingPublication!!
                copyNoticeTextForStatus(context, p) { onSave(p) }
                sharingPublication = null
            },
            onSaveFlyer = {
                val p = sharingPublication!!
                if (!p.imageUrl.isNullOrBlank()) {
                    saveFlyerToGallery(context, p.imageUrl!!, p) { onSave(p) }
                }
                sharingPublication = null
            },
            onViewFullFlyer = {
                val p = sharingPublication!!
                sharingPublication = null
                viewingFlyerPublication = p
            }
        )
    }

    if (showCreateDialog) {
        CreateNoticeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments, locCoords, locName, eventDate, eventTime, syncCal ->
                onCreatePublication(title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments, locCoords, locName, eventDate, eventTime, syncCal)
                showCreateDialog = false
            }
        )
    }

    if (selectedPublicationForEdit != null) {
        EditNoticeDialog(
            pub = selectedPublicationForEdit!!,
            onDismiss = { selectedPublicationForEdit = null },
            onConfirmEdit = { updatedPub, imageUri ->
                onUpdatePublication(updatedPub, imageUri)
                selectedPublicationForEdit = null
            }
        )
    }
}

@Composable
fun FeedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val bgColor = if (selected) DashboardFondoConfig.ColorRojoCarrera else DashboardFondoConfig.ColorTarjetaClara
    val textColor = if (selected) Color.White else DashboardFondoConfig.ColorTextoSecundario

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = if (selected) null else BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun NoticeCard(
    pub: Publication,
    comments: List<NoticeComment> = emptyList(),
    currentMember: MemberProfile? = null,
    isDirectivaMode: Boolean,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    onAddComment: (String) -> Unit = {},
    onViewFlyer: () -> Unit = {},
    onOpenShareDialog: () -> Unit = {},
    onQuickSave: () -> Unit = {},
    onDismissNotice: () -> Unit = {},
    onNavigateToCalendar: (String) -> Unit = {},
    onToggleEventFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = false
    var isCommentsExpanded by remember { mutableStateOf(false) }
    var commentInputText by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val canDelete = isDirectivaMode || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true) || (currentMember?.role == MemberRole.PRESIDENTE) || (currentMember?.role == MemberRole.DIRECTIVA)

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError)
                    Text("¿Eliminar aviso?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Text(
                    "Esta acción eliminará el aviso definitivamente de Firebase y desaparecerá de la app para todos los usuarios.\n\n¿Estás seguro de que deseas continuar?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                        android.widget.Toast.makeText(context, "🗑️ Aviso eliminado para todos los usuarios", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val dateStr = remember(pub.timestamp) {
        SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault()).format(Date(pub.timestamp))
    }

    val cardBg = DashboardFondoConfig.ColorTarjetaClara

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pub.isPinned) 4.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("notice_card_${pub.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Tags: Category + Priority / Pinned & Actions (Padding interno)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pub.isPinned) {
                            val pinnedBg = if (isDark) Color(0xFF381419) else Color(0xFFFFEBEE)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = pinnedBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = TxFlameRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("Fijado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TxFlameRed)
                                }
                            }
                        }

                        val (catBg, catText) = when (pub.category) {
                            NoticeCategory.AVISO_OFICIAL -> if (isDark) Pair(Color(0xFF331E18), Color(0xFFFF8A65)) else Pair(Color(0xFFFFEBE6), Color(0xFFD84315))
                            NoticeCategory.RETO_MOTERO -> if (isDark) Pair(Color(0xFF332314), Color(0xFFFFB74D)) else Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
                            NoticeCategory.COMUNICADO -> if (isDark) Pair(Color(0xFF15283E), Color(0xFF64B5F6)) else Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
                            NoticeCategory.EMERGENCIA -> if (isDark) Pair(Color(0xFF3E1515), Color(0xFFFF5252)) else Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
                            NoticeCategory.NOTICIA_RUTA -> if (isDark) Pair(Color(0xFF1B3322), Color(0xFF81C784)) else Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            NoticeCategory.CAPACITACION -> if (isDark) Pair(Color(0xFF2C1938), Color(0xFFBA68C8)) else Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
                            NoticeCategory.OBRA_BENEFICA -> if (isDark) Pair(Color(0xFF3B1528), Color(0xFFF472B6)) else Pair(Color(0xFFFCE7F3), Color(0xFFBE185D))
                            NoticeCategory.MANTENIMIENTO_PREVENTIVO -> if (isDark) Pair(Color(0xFF0C2A3D), Color(0xFF38BDF8)) else Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
                            NoticeCategory.LAVADO_FAMILIAR -> if (isDark) Pair(Color(0xFF0C3030), Color(0xFF2DD4BF)) else Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = catBg
                        ) {
                            Text(
                                text = pub.category.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = catText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (pub.priority == NoticePriority.URGENTE) {
                            val urgBg = if (isDark) Color(0xFF381419) else Color(0xFFFFEBEE)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = urgBg
                            ) {
                                Text(
                                    text = "Urgente",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TxFlameRed,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismissNotice,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Ocultar aviso de mi pantalla",
                                tint = if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (canDelete) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar aviso",
                                    tint = if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar aviso definitivamente",
                                    tint = StatusError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title & Content
                Text(
                    text = pub.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF5F5F5) else Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = pub.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF475569),
                    lineHeight = 20.sp
                )
            }

            // Flyer / Imagen de la publicación (Ancho completo con soporte para tocar y ver pantalla completa)
            if (!pub.imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val imageUrl = pub.imageUrl
                LaunchedEffect(imageUrl) {
                    android.util.Log.d("TEAM_TX_IMAGES", "🖼️ Cargando flyer para: ${pub.title} | URL: $imageUrl")
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 420.dp)
                        .clickable(onClick = onViewFlyer)
                ) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = "Flyer de la Publicación",
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(if (isDark) Color(0xFF262626) else Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = TxFlameRed,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(if (isDark) Color(0xFF262626) else Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "Error al cargar imagen",
                                        tint = if (isDark) Color(0xFF757575) else Color(0xFF9E9E9E),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Error al cargar imagen",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF757575) else Color(0xFF9E9E9E)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 420.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )

                    // Overlay flotante: Ver Flyer en Grande
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Ver en Grande", tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Ver Flyer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom Section (Reto, Footer, Comments)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            ) {
                // If challenge: show special Challenge badge
                if (pub.category == NoticeCategory.RETO_MOTERO && pub.targetChallengeDistanceKm > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2B1F0E) else Color(0xFFFFF8E1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MotoGoldSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = pub.challengeBadgeText ?: "RETO OFICIAL TX",
                                    color = MotoGoldSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Distancia Meta: ${pub.targetChallengeDistanceKm} KM • Sello en Pasaporte Motero",
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // 📍 Lugar del Evento / Punto de Encuentro (Botón para abrir Mapa TX)
                if (!pub.locationCoordinates.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF1B2838) else Color(0xFFE8F4FD),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2E5C8A) else Color(0xFF90CAF9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                PuenteMapa.mostrarUbicacionEnMapa(
                                    contexto = context,
                                    coordenadas = pub.locationCoordinates!!,
                                    tituloEtiqueta = pub.locationName?.ifBlank { null } ?: pub.title
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF5350).copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Ubicación del evento",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pub.locationName?.ifBlank { null } ?: "Ver lugar del evento",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
                                )
                                Text(
                                    text = "📍 ${pub.locationCoordinates} • Toca para abrir Mapa TX",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = "Abrir Mapa TX",
                                tint = MotoOrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 📅 Fecha del Evento y Cuenta Regresiva (con enlace interactivo al Calendario Motero)
                if (!pub.eventDate.isNullOrBlank()) {
                    EventDateCountdownBadge(
                        eventDate = pub.eventDate!!,
                        eventTime = pub.eventTime,
                        isFinished = pub.isEventFinished,
                        onClick = { onNavigateToCalendar(pub.eventDate!!) },
                        isDark = isDark,
                        canToggleFinished = canDelete,
                        onToggleFinished = onToggleEventFinished
                    )
                }

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))

                // Footer: Author Avatar with Carnet TX Photo, Role Badge, Date & Actions
                val secondaryTextColor = if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = pub.authorName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (pub.telegramPostUrl != null) {
                            IconButton(
                                onClick = { openUrl(context, pub.telegramPostUrl!!) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Ver en Telegram",
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Comments toggle button
                        TextButton(
                            onClick = { isCommentsExpanded = !isCommentsExpanded },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_comments_${pub.id}")
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "Respuestas",
                                tint = if (isCommentsExpanded) TxFlameRed else secondaryTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${comments.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCommentsExpanded) TxFlameRed else secondaryTextColor
                            )
                        }

                        // Like button
                        val myMemberIdStr = currentMember?.id?.toString() ?: ""
                        val isLikedByMe = pub.likedByMemberIds.split(",").map { it.trim() }.contains(myMemberIdStr) && myMemberIdStr.isNotEmpty()

                        TextButton(
                            onClick = onLike,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_like_${pub.id}")
                        ) {
                            Icon(
                                if (isLikedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Me gusta",
                                tint = if (isLikedByMe) TxFlameRed else secondaryTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${pub.likesCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLikedByMe) TxFlameRed else secondaryTextColor
                            )
                        }

                        // Share button (WhatsApp / Estados)
                        TextButton(
                            onClick = onOpenShareDialog,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_share_${pub.id}")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartir aviso",
                                tint = if (pub.sharesCount > 0) Color(0xFF25D366) else secondaryTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            if (pub.sharesCount > 0) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${pub.sharesCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF25D366)
                                )
                            }
                        }

                        // Save / Download button
                        TextButton(
                            onClick = onQuickSave,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_save_${pub.id}")
                        ) {
                            Icon(
                                Icons.Default.SaveAlt,
                                contentDescription = "Guardar flyer",
                                tint = if (pub.savesCount > 0) MotoGoldSecondary else secondaryTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            if (pub.savesCount > 0) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${pub.savesCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MotoGoldSecondary
                                )
                            }
                        }
                    }
                }

                // Expandable Comments Section (Respuestas de Miembros)
                AnimatedVisibility(visible = isCommentsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "RESPUESTAS DE LA HERMANDAD (${comments.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TxFlameRed,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (comments.isEmpty()) {
                            Text(
                                text = "No hay respuestas todavía. ¡Sé el primer piloto en comentar!",
                                fontSize = 12.sp,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                comments.forEach { c ->
                                    val roleColor = Color(c.authorRole.badgeColorHex)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF14171F) else Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(roleColor.copy(alpha = 0.25f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = c.authorInitials,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = roleColor
                                                        )
                                                    }
                                                    Text(
                                                        text = "${c.authorNickname} (${c.authorMemberNumber})",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = roleColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = c.authorRole.displayName,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = roleColor,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }

                                                val commentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(c.timestamp))
                                                Text(text = commentTime, fontSize = 9.sp, color = secondaryTextColor)
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = c.content,
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFFECEFF1) else Color(0xFF334155),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (pub.allowComments) {
                            // Comment Reply Input Field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = commentInputText,
                                    onValueChange = { commentInputText = it },
                                    placeholder = { Text("Escribir respuesta al aviso...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_notice_comment_${pub.id}"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TxFlameRed,
                                        unfocusedBorderColor = if (isDark) Color(0xFF333333) else Color(0xFFCBD5E1)
                                    ),
                                    maxLines = 2
                                )

                                IconButton(
                                    onClick = {
                                        if (commentInputText.isNotBlank()) {
                                            onAddComment(commentInputText.trim())
                                            commentInputText = ""
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (commentInputText.isNotBlank()) TxFlameRed else if (isDark) Color(0xFF2C2C2C) else Color(0xFFE2E8F0),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("btn_send_notice_comment_${pub.id}")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Enviar respuesta", modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF262626) else Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(16.dp))
                                    Text("Comentarios bloqueados por la directiva para este aviso", fontSize = 12.sp, color = secondaryTextColor)
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
fun CreateNoticeDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        content: String,
        category: NoticeCategory,
        priority: NoticePriority,
        isPinned: Boolean,
        challengeKm: Int,
        challengeBadge: String?,
        telegramUrl: String?,
        imageUri: Uri?,
        allowComments: Boolean,
        locationCoordinates: String?,
        locationName: String?,
        eventDate: String?,
        eventTime: String?,
        syncWithCalendar: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(NoticeCategory.AVISO_OFICIAL) }
    var priority by remember { mutableStateOf(NoticePriority.NORMAL) }
    var isPinned by remember { mutableStateOf(false) }
    var allowComments by remember { mutableStateOf(true) }
    var challengeKmStr by remember { mutableStateOf("0") }
    var challengeBadge by remember { mutableStateOf("") }
    var telegramUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var locationName by remember { mutableStateOf("") }
    var locationCoordinates by remember { mutableStateOf("") }
    var autoDetectedCoordsMsg by remember { mutableStateOf<String?>(null) }
    var estaCapturandoGps by remember { mutableStateOf(false) }

    // 📅 Estados para Fecha del Evento y Sincronización con Calendario Motero
    var eventDate by remember { mutableStateOf<String?>(null) }
    var eventTime by remember { mutableStateOf("08:00 AM") }
    var syncWithCalendar by remember { mutableStateOf(true) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gestorUbicacion = remember { GestorUbicacion(context) }

    // 🗺️ Auto-captura y retorno asistido desde el Mapa TX
    LaunchedEffect(GestorSeleccionMapa.coordenadaSeleccionada) {
        val coord = GestorSeleccionMapa.consumirCoordenada()
        if (!coord.isNullOrBlank()) {
            locationCoordinates = coord
            autoDetectedCoordsMsg = "¡Coordenada autopegada del Mapa TX: $coord!"
            Toast.makeText(context, "📍 ¡Coordenada pegada automáticamente: $coord!", Toast.LENGTH_SHORT).show()
        }
    }

    // 📋 Detección automática de coordenadas desde el portapapeles al regresar del Mapa TX (Fallback)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val coordsPortapapeles = GestorPortapapeles.leerCoordenadaValida(context)
                if (coordsPortapapeles != null && coordsPortapapeles != locationCoordinates) {
                    locationCoordinates = coordsPortapapeles
                    autoDetectedCoordsMsg = "¡Coordenada copiada desde el Mapa TX detectada!"
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    if (showDatePickerDialog) {
        EventDatePickerDialog(
            initialDate = eventDate,
            initialTime = eventTime,
            onDismiss = { showDatePickerDialog = false },
            onConfirmDate = { date, time ->
                eventDate = date
                eventTime = time
                syncWithCalendar = true
                showDatePickerDialog = false
            },
            onClearDate = {
                eventDate = null
                showDatePickerDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DashboardFondoConfig.ColorTarjetaClara,
        title = {
            Text(
                text = "Nueva Publicación Directiva",
                fontWeight = FontWeight.Bold,
                color = DashboardFondoConfig.ColorTextoPrimario
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título del Aviso, Reto o Alerta") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notice_title")
                    )
                }
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Contenido / Descripción") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notice_content")
                    )
                }
                item {
                    Text("Categoría:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        NoticeCategory.values().forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                if (category == NoticeCategory.RETO_MOTERO) {
                    item {
                        OutlinedTextField(
                            value = challengeKmStr,
                            onValueChange = { challengeKmStr = it },
                            label = { Text("Kilómetros del Reto (ej. 500)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = challengeBadge,
                            onValueChange = { challengeBadge = it },
                            label = { Text("Nombre del Parche / Distintivo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 📅 SECCIÓN: FECHA DEL EVENTO Y SINCRONIZACIÓN CON CALENDARIO MOTERO
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (!eventDate.isNullOrBlank()) Color(0xFF3B82F6).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = if (!eventDate.isNullOrBlank()) Color(0xFF3B82F6) else MotoOrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Fecha del Evento & Calendario Motero",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (!eventDate.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                            Column {
                                                Text(
                                                    text = "Fecha: $eventDate • $eventTime",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSystemInDarkTheme()) Color(0xFF90CAF9) else Color(0xFF1565C0)
                                                )
                                                Text(
                                                    text = if (syncWithCalendar) "Sincronizado con Calendario Motero" else "Solo visible en aviso",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                eventDate = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Quitar fecha", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showDatePickerDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6)),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (eventDate.isNullOrBlank()) "📅 Seleccionar Fecha y Hora del Evento" else "✏️ Cambiar Fecha / Hora ($eventDate)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!eventDate.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sincronizar en Calendario", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Aparecerá en el calendario de todos los miembros", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = syncWithCalendar,
                                        onCheckedChange = { syncWithCalendar = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF3B82F6),
                                            checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 📍 SECCIÓN: UBICACIÓN DEL EVENTO (Extractor Mapa TX / Portapapeles)
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (locationCoordinates.isNotBlank()) Color(0xFF4CAF50).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (locationCoordinates.isNotBlank()) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Ubicación del Evento / Punto de Encuentro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedTextField(
                                value = locationName,
                                onValueChange = { locationName = it },
                                label = { Text("Nombre del Sitio / Local (Opcional)") },
                                placeholder = { Text("Ej: Club de Abogados, Bomba PDV...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Botón para ir al Mapa TX con retorno y auto-pegado automático
                            OutlinedButton(
                                onClick = {
                                    val coordsActuales = if (locationCoordinates.isNotBlank()) locationCoordinates else "10.228,-67.475"
                                    GestorSeleccionMapa.iniciarSeleccion(
                                        contexto = context,
                                        origen = "FEED_CREATE",
                                        coordenadasIniciales = coordsActuales,
                                        titulo = locationName.ifBlank { "Punto de Evento" }
                                    )
                                    Toast.makeText(context, "👉 En el mapa: copia la coordenada deseada y volverás automáticamente aquí pegándola", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00B0FF)),
                                border = BorderStroke(1.dp, Color(0xFF00B0FF).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🗺️ Seleccionar Ubicación en Mapa TX (Auto-retorno)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Botones de acción rápida: Pegar y GPS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val coords = GestorPortapapeles.leerCoordenadaValida(context)
                                        if (coords != null) {
                                            locationCoordinates = coords
                                            autoDetectedCoordsMsg = "Coordenada pegada del portapapeles"
                                            Toast.makeText(context, "✅ Coordenada detectada: $coords", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val texto = GestorPortapapeles.leerTexto(context)
                                            if (texto.isNullOrBlank()) {
                                                Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No se encontraron coordenadas en el texto copiado", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📋 Pegar", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                             estaCapturandoGps = true
                                            val coords = gestorUbicacion.capturarCoordenadaActual()
                                            estaCapturandoGps = false
                                            if (coords != null) {
                                                locationCoordinates = coords
                                                autoDetectedCoordsMsg = "Ubicación GPS actual capturada"
                                                Toast.makeText(context, "📍 GPS actual: $coords", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No se pudo obtener la posición GPS", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (estaCapturandoGps) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("📍 Mi GPS", fontSize = 11.sp)
                                    }
                                }
                            }

                            // Feedback visual de éxito cuando hay coordenadas
                            if (locationCoordinates.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                            Column {
                                                Text(
                                                    text = "Coordenadas fijadas: $locationCoordinates",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
                                                )
                                                autoDetectedCoordsMsg?.let { msg ->
                                                    Text(text = msg, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                locationCoordinates = ""
                                                autoDetectedCoordsMsg = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = telegramUrl,
                        onValueChange = { telegramUrl = it },
                        label = { Text("Enlace a Post Telegram (Opcional)") },
                        placeholder = { Text("https://t.me/teamtxvenezuela/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                        Text("Fijar en el tope del muro", fontSize = 13.sp)
                    }
                }
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Permitir Comentarios", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    if (allowComments) "Los miembros pueden comentar este aviso" else "Comentarios bloqueados",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = allowComments,
                                onCheckedChange = { allowComments = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MotoOrangePrimary,
                                    checkedTrackColor = MotoOrangePrimary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Subir flyer o foto")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageUri == null) "Adjuntar Flyer o Foto" else "Cambiar Imagen")
                        }

                        if (selectedImageUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val km = challengeKmStr.toIntOrNull() ?: 0
                        onCreate(
                            title,
                            content,
                            category,
                            priority,
                            isPinned,
                            km,
                            if (challengeBadge.isNotBlank()) challengeBadge else null,
                            if (telegramUrl.isNotBlank()) telegramUrl else null,
                            selectedImageUri,
                            allowComments,
                            if (locationCoordinates.isNotBlank()) locationCoordinates.trim() else null,
                            if (locationName.isNotBlank()) locationName.trim() else null,
                            eventDate?.trim()?.ifBlank { null },
                            eventTime?.trim()?.ifBlank { null },
                            syncWithCalendar
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                modifier = Modifier.testTag("btn_confirm_create_notice")
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditNoticeDialog(
    pub: Publication,
    onDismiss: () -> Unit,
    onConfirmEdit: (Publication, Uri?) -> Unit
) {
    var title by remember { mutableStateOf(pub.title) }
    var content by remember { mutableStateOf(pub.content) }
    var category by remember { mutableStateOf(pub.category) }
    var priority by remember { mutableStateOf(pub.priority) }
    var isPinned by remember { mutableStateOf(pub.isPinned) }
    var allowComments by remember { mutableStateOf(pub.allowComments) }
    var challengeKmStr by remember { mutableStateOf("${pub.targetChallengeDistanceKm}") }
    var challengeBadge by remember { mutableStateOf(pub.challengeBadgeText ?: "") }
    var telegramUrl by remember { mutableStateOf(pub.telegramPostUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var locationName by remember { mutableStateOf(pub.locationName ?: "") }
    var locationCoordinates by remember { mutableStateOf(pub.locationCoordinates ?: "") }
    var autoDetectedCoordsMsg by remember { mutableStateOf<String?>(null) }
    var estaCapturandoGps by remember { mutableStateOf(false) }

    // 📅 Estados para Fecha del Evento y Sincronización con Calendario Motero
    var eventDate by remember { mutableStateOf<String?>(pub.eventDate) }
    var eventTime by remember { mutableStateOf(pub.eventTime?.ifBlank { "08:00 AM" } ?: "08:00 AM") }
    var syncWithCalendar by remember { mutableStateOf(pub.linkedCalendarEventId != null || !pub.eventDate.isNullOrBlank()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gestorUbicacion = remember { GestorUbicacion(context) }

    // 🗺️ Auto-captura y retorno asistido desde el Mapa TX
    LaunchedEffect(GestorSeleccionMapa.coordenadaSeleccionada) {
        val coord = GestorSeleccionMapa.consumirCoordenada()
        if (!coord.isNullOrBlank()) {
            locationCoordinates = coord
            autoDetectedCoordsMsg = "¡Coordenada autopegada del Mapa TX: $coord!"
            Toast.makeText(context, "📍 ¡Coordenada pegada automáticamente: $coord!", Toast.LENGTH_SHORT).show()
        }
    }

    // 📋 Detección automática de coordenadas desde el portapapeles al regresar del Mapa TX (Fallback)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val coordsPortapapeles = GestorPortapapeles.leerCoordenadaValida(context)
                if (coordsPortapapeles != null && coordsPortapapeles != locationCoordinates) {
                    locationCoordinates = coordsPortapapeles
                    autoDetectedCoordsMsg = "¡Coordenada copiada desde el Mapa TX detectada!"
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    if (showDatePickerDialog) {
        EventDatePickerDialog(
            initialDate = eventDate,
            initialTime = eventTime,
            onDismiss = { showDatePickerDialog = false },
            onConfirmDate = { date, time ->
                eventDate = date
                eventTime = time
                syncWithCalendar = true
                showDatePickerDialog = false
            },
            onClearDate = {
                eventDate = null
                showDatePickerDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DashboardFondoConfig.ColorTarjetaClara,
        title = {
            Text(
                text = "Editar Publicación Directiva",
                fontWeight = FontWeight.Bold,
                color = DashboardFondoConfig.ColorTextoPrimario
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título del Aviso o Reto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Contenido / Descripción") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Categoría:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        NoticeCategory.values().forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                if (category == NoticeCategory.RETO_MOTERO) {
                    item {
                        OutlinedTextField(
                            value = challengeKmStr,
                            onValueChange = { challengeKmStr = it },
                            label = { Text("Kilómetros del Reto") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = challengeBadge,
                            onValueChange = { challengeBadge = it },
                            label = { Text("Nombre del Parche / Distintivo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 📅 SECCIÓN: FECHA DEL EVENTO Y SINCRONIZACIÓN CON CALENDARIO MOTERO
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (!eventDate.isNullOrBlank()) Color(0xFF3B82F6).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = if (!eventDate.isNullOrBlank()) Color(0xFF3B82F6) else MotoOrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Fecha del Evento & Calendario Motero",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (!eventDate.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                            Column {
                                                Text(
                                                    text = "Fecha: $eventDate • $eventTime",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSystemInDarkTheme()) Color(0xFF90CAF9) else Color(0xFF1565C0)
                                                )
                                                Text(
                                                    text = if (syncWithCalendar) "Sincronizado con Calendario Motero" else "Solo visible en aviso",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                eventDate = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Quitar fecha", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showDatePickerDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3B82F6)),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (eventDate.isNullOrBlank()) "📅 Seleccionar Fecha y Hora del Evento" else "✏️ Cambiar Fecha / Hora ($eventDate)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!eventDate.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sincronizar en Calendario", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Aparecerá en el calendario de todos los miembros", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = syncWithCalendar,
                                        onCheckedChange = { syncWithCalendar = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF3B82F6),
                                            checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 📍 SECCIÓN: UBICACIÓN DEL EVENTO (Extractor Mapa TX / Portapapeles)
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (locationCoordinates.isNotBlank()) Color(0xFF4CAF50).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (locationCoordinates.isNotBlank()) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Ubicación del Evento / Punto de Encuentro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedTextField(
                                value = locationName,
                                onValueChange = { locationName = it },
                                label = { Text("Nombre del Sitio / Local (Opcional)") },
                                placeholder = { Text("Ej: Club de Abogados, Bomba PDV...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Botón para ir al Mapa TX con retorno y auto-pegado automático
                            OutlinedButton(
                                onClick = {
                                    val coordsActuales = if (locationCoordinates.isNotBlank()) locationCoordinates else "10.228,-67.475"
                                    GestorSeleccionMapa.iniciarSeleccion(
                                        contexto = context,
                                        origen = "FEED_EDIT",
                                        coordenadasIniciales = coordsActuales,
                                        titulo = locationName.ifBlank { "Punto de Evento" }
                                    )
                                    Toast.makeText(context, "👉 En el mapa: copia la coordenada deseada y volverás automáticamente aquí pegándola", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00B0FF)),
                                border = BorderStroke(1.dp, Color(0xFF00B0FF).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🗺️ Seleccionar Ubicación en Mapa TX (Auto-retorno)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Botones de acción rápida: Pegar y GPS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val coords = GestorPortapapeles.leerCoordenadaValida(context)
                                        if (coords != null) {
                                            locationCoordinates = coords
                                            autoDetectedCoordsMsg = "Coordenada pegada del portapapeles"
                                            Toast.makeText(context, "✅ Coordenada detectada: $coords", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val texto = GestorPortapapeles.leerTexto(context)
                                            if (texto.isNullOrBlank()) {
                                                Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No se encontraron coordenadas en el texto copiado", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📋 Pegar", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            estaCapturandoGps = true
                                            val coords = gestorUbicacion.capturarCoordenadaActual()
                                            estaCapturandoGps = false
                                            if (coords != null) {
                                                locationCoordinates = coords
                                                autoDetectedCoordsMsg = "Ubicación GPS actual capturada"
                                                Toast.makeText(context, "📍 GPS actual: $coords", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No se pudo obtener la posición GPS", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (estaCapturandoGps) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("📍 Mi GPS", fontSize = 11.sp)
                                    }
                                }
                            }

                            // Feedback visual de éxito cuando hay coordenadas
                            if (locationCoordinates.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                            Column {
                                                Text(
                                                    text = "Coordenadas fijadas: $locationCoordinates",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
                                                )
                                                autoDetectedCoordsMsg?.let { msg ->
                                                    Text(text = msg, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                locationCoordinates = ""
                                                autoDetectedCoordsMsg = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = telegramUrl,
                        onValueChange = { telegramUrl = it },
                        label = { Text("Enlace a Post Telegram (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                        Text("Fijar en el tope del muro", fontSize = 13.sp)
                    }
                }
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Permitir Comentarios", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    if (allowComments) "Los miembros pueden comentar este aviso" else "Comentarios bloqueados",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = allowComments,
                                onCheckedChange = { allowComments = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MotoOrangePrimary,
                                    checkedTrackColor = MotoOrangePrimary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Cambiar flyer o foto")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageUri == null && pub.imageUrl.isNullOrEmpty()) "Adjuntar Flyer o Foto" else "Cambiar Imagen / Flyer")
                        }

                        if (selectedImageUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else if (!pub.imageUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = pub.imageUrl,
                                contentDescription = "Imagen actual",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val km = challengeKmStr.toIntOrNull() ?: pub.targetChallengeDistanceKm
                        val updated = pub.copy(
                            title = title,
                            content = content,
                            category = category,
                            priority = priority,
                            isPinned = isPinned,
                            targetChallengeDistanceKm = km,
                            challengeBadgeText = if (challengeBadge.isNotBlank()) challengeBadge else null,
                            telegramPostUrl = if (telegramUrl.isNotBlank()) telegramUrl else null,
                            allowComments = allowComments,
                            locationCoordinates = if (locationCoordinates.isNotBlank()) locationCoordinates.trim() else null,
                            locationName = if (locationName.isNotBlank()) locationName.trim() else null,
                            eventDate = eventDate?.trim()?.ifBlank { null },
                            eventTime = eventTime?.trim()?.ifBlank { null }
                        )
                        onConfirmEdit(updated, selectedImageUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Guarda un flyer o imagen de aviso directamente en la galería del dispositivo (Pictures/TeamTX)
 */
fun saveFlyerToGallery(
    context: Context,
    imageUrl: String,
    pub: Publication,
    onSaved: () -> Unit
) {
    Toast.makeText(context, "📥 Descargando flyer en alta resolución...", Toast.LENGTH_SHORT).show()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()
            val inputStream: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()

            if (bitmap != null) {
                val filename = "TeamTX_Flyer_${pub.id}_${System.currentTimeMillis()}.jpg"
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TeamTX")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "✅ Flyer guardado con éxito en Fotos / TeamTX",
                            Toast.LENGTH_LONG
                        ).show()
                        onSaved()
                    }
                } else {
                    throw Exception("No se pudo crear entrada en MediaStore")
                }
            } else {
                throw Exception("No se pudo procesar el formato de la imagen")
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedScreen", "Error guardando flyer: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "❌ Error al guardar flyer: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

/**
 * Comparte el aviso con formato oficial a WhatsApp o cualquier otra app del sistema
 */
fun shareNotice(
    context: Context,
    pub: Publication,
    onShared: () -> Unit
) {
    try {
        val textBuilder = StringBuilder()
        textBuilder.append("🏍️ *TEAM NACIONAL TX ARAGUA*\n")
        textBuilder.append("📢 *${pub.category.displayName.uppercase()}*\n\n")
        textBuilder.append("📌 *${pub.title}*\n\n")
        textBuilder.append("${pub.content}\n\n")
        if (pub.category == NoticeCategory.RETO_MOTERO && pub.targetChallengeDistanceKm > 0) {
            textBuilder.append("🏆 *Reto Oficial:* ${pub.challengeBadgeText ?: "RETO TX"}\n")
            textBuilder.append("📍 *Distancia Meta:* ${pub.targetChallengeDistanceKm} KM\n\n")
        }
        textBuilder.append("✍️ *Publicado por:* ${pub.authorName} (${pub.authorRole})\n")
        if (!pub.imageUrl.isNullOrBlank()) {
            textBuilder.append("🖼️ *Flyer adjunto:* ${pub.imageUrl}\n")
        }
        textBuilder.append("📲 _Comunidad Oficial Team TX Aragua_")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir Aviso Team TX")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
        onShared()
    } catch (e: Exception) {
        android.util.Log.e("FeedScreen", "Error compartiendo aviso: ${e.message}")
    }
}

/**
 * Copia el texto formateado con emojis y hashtags listo para Estados de WhatsApp o Instagram Stories
 */
fun copyNoticeTextForStatus(
    context: Context,
    pub: Publication,
    onCopied: () -> Unit
) {
    try {
        val textBuilder = StringBuilder()
        textBuilder.append("🏍️ *TEAM NACIONAL TX ARAGUA*\n")
        textBuilder.append("📢 ${pub.category.displayName} • ${pub.title}\n\n")
        textBuilder.append("${pub.content}\n\n")
        if (pub.category == NoticeCategory.RETO_MOTERO && pub.targetChallengeDistanceKm > 0) {
            textBuilder.append("🏁 Reto: ${pub.targetChallengeDistanceKm} KM • ${pub.challengeBadgeText ?: "Sello TX"}\n\n")
        }
        textBuilder.append("🔥 #TeamTX #TeamNacionalTX #MundoBiker #Aragua")

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Aviso Team TX", textBuilder.toString())
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            "📋 ¡Texto copiado para Estados de WhatsApp / Instagram!",
            Toast.LENGTH_SHORT
        ).show()
        onCopied()
    } catch (e: Exception) {
        android.util.Log.e("FeedScreen", "Error copiando texto: ${e.message}")
    }
}

/**
 * Visor de flyer / imagen a pantalla completa con soporte inmersivo para zoom táctil (Pinch to Zoom),
 * guardado en galería, compartir en WhatsApp y copiar texto de estados.
 */
@Composable
fun FlyerImageViewerDialog(
    pub: Publication,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onCopyStatusText: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF00A0E17))
        ) {
            // Central Zoomable Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!pub.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = pub.imageUrl,
                        contentDescription = pub.title,
                        loading = {
                            CircularProgressIndicator(color = TxFlameRed, strokeWidth = 3.dp)
                        },
                        error = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No se pudo cargar la imagen a pantalla completa", color = Color.Gray, fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 80.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }

            // Top Toolbar
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                        Column {
                            Text(
                                text = pub.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pub.category.displayName} • ${pub.authorName}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (scale > 1f) {
                            IconButton(onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            }) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", tint = MotoGoldSecondary)
                            }
                        }
                        IconButton(onClick = onSave) {
                            Icon(Icons.Default.Download, contentDescription = "Guardar Imagen", tint = Color.White)
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White)
                        }
                    }
                }
            }

            // Bottom Actions Bar
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (pub.content.isNotBlank()) {
                        Text(
                            text = pub.content,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Guardar Flyer
                        Button(
                            onClick = onSave,
                            colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Compartir WhatsApp
                        Button(
                            onClick = onShare,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1.2f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Copiar Estado
                        OutlinedButton(
                            onClick = onCopyStatusText,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1.1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo interactivo para seleccionar la forma de compartir el aviso o guardar su flyer
 */
@Composable
fun ShareNoticeDialog(
    pub: Publication,
    onDismiss: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onCopyStatus: () -> Unit,
    onSaveFlyer: () -> Unit,
    onViewFullFlyer: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val hasFlyer = !pub.imageUrl.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Share, contentDescription = null, tint = TxFlameRed)
                Text("Compartir Aviso", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = pub.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Selecciona cómo deseas difundir esta publicación de la hermandad:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Opción 1: WhatsApp / Redes Sociales
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShareWhatsApp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enviar a WhatsApp / Redes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Comparte mensaje con formato oficial y enlace", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Opción 2: Copiar Texto para Estados
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCopyStatus)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MotoOrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Copiar para Estados", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Texto con emojis listo para WhatsApp / Instagram Stories", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Opción 3: Guardar Flyer (si tiene)
                if (hasFlyer) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSaveFlyer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Guardar Flyer en Galería", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Descarga el póster en alta calidad en Fotos/TeamTX", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Opción 4: Ver a Pantalla Completa
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onViewFullFlyer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ver Flyer en Pantalla Grande", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Abre el visor inmersivo con zoom interactivo", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

/**
 * 📅 Diálogo interactivo con selector mensual y selector de hora para eventos del Muro y Calendario Motero
 */
@Composable
fun EventDatePickerDialog(
    initialDate: String?,
    initialTime: String?,
    onDismiss: () -> Unit,
    onConfirmDate: (date: String, time: String) -> Unit,
    onClearDate: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cal = remember {
        val c = Calendar.getInstance()
        if (!initialDate.isNullOrBlank()) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val d = sdf.parse(initialDate)
                if (d != null) c.time = d
            } catch (ignored: Exception) {}
        }
        c
    }

    var selectedDay by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var currentMonthCalendar by remember { mutableStateOf(cal.clone() as Calendar) }
    var selectedTime by remember { mutableStateOf(initialTime?.ifBlank { "08:00 AM" } ?: "08:00 AM") }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }
    val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Generar días del mes para la cuadrícula
    val daysList = remember(currentMonthCalendar) {
        val c = currentMonthCalendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1 // 0: Dom, 1: Lun
        val maxDays = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<Int>()
        for (i in 0 until firstDayOfWeek) list.add(0)
        for (d in 1..maxDays) list.add(d)
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MotoOrangePrimary)
                Text("Seleccionar Fecha del Evento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header navegación de mes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newCal = currentMonthCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentMonthCalendar = newCal
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = MotoOrangePrimary)
                    }

                    Text(
                        text = monthYearFormat.format(currentMonthCalendar.time).replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = {
                            val newCal = currentMonthCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentMonthCalendar = newCal
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = MotoOrangePrimary)
                    }
                }

                // Días de la semana
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("D", "L", "M", "M", "J", "V", "S").forEach { d ->
                        Text(d, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Cuadrícula de días en filas de 7
                val chunkedDays = daysList.chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    chunkedDays.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            for (i in 0 until 7) {
                                val dayNum = week.getOrNull(i) ?: 0
                                if (dayNum > 0) {
                                    val isSelected = (dayNum == selectedDay)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MotoOrangePrimary else Color.Transparent,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { selectedDay = dayNum }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Selector de Hora
                Text("⏰ Hora del Evento:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("07:00 AM", "08:00 AM", "09:00 AM", "02:00 PM").forEach { timeOption ->
                        FilterChip(
                            selected = selectedTime == timeOption,
                            onClick = { selectedTime = timeOption },
                            label = { Text(timeOption, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Hora personalizada (ej. 08:30 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCal = currentMonthCalendar.clone() as Calendar
                    finalCal.set(Calendar.DAY_OF_MONTH, selectedDay)
                    val formattedDate = dayFormat.format(finalCal.time)
                    onConfirmDate(formattedDate, selectedTime)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("✅ Guardar Fecha", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!initialDate.isNullOrBlank()) {
                    TextButton(onClick = onClearDate) {
                        Text("Quitar Fecha", color = StatusError)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

/**
 * ⏳ Badge de Fecha de Evento y Cuenta Regresiva de Días
 */
@Composable
fun EventDateCountdownBadge(
    eventDate: String,
    eventTime: String?,
    isFinished: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    canToggleFinished: Boolean = false,
    onToggleFinished: () -> Unit = {}
) {
    val daysDiff = remember(eventDate) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            val parsed = sdf.parse(eventDate.trim())
            if (parsed != null) {
                val calTarget = Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val calToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMs = calTarget.timeInMillis - calToday.timeInMillis
                (diffMs / (24 * 60 * 60 * 1000L)).toInt()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val (countdownText, badgeColor, iconColor) = when {
        isFinished -> Triple("Evento Concluido ✅", if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), Color(0xFF94A3B8))
        daysDiff == null -> Triple("Fecha Programada 📅", MotoOrangePrimary, MotoOrangePrimary)
        daysDiff == 0 -> Triple("🏍️ ¡HOY ES EL EVENTO!", TxFlameRed, TxFlameRed)
        daysDiff == 1 -> Triple("🔥 ¡ES MAÑANA!", MotoOrangePrimary, MotoOrangePrimary)
        daysDiff in 2..7 -> Triple("⏳ Faltan $daysDiff días", Color(0xFFF59E0B), Color(0xFFF59E0B))
        daysDiff > 7 -> Triple("📅 En $daysDiff días", Color(0xFF3B82F6), Color(0xFF3B82F6))
        else -> Triple("🏁 Evento Realizado", Color(0xFF64748B), Color(0xFF64748B))
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF1E2433) else Color(0xFFF0F7FF),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = "Fecha del Evento",
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Fecha: $eventDate" + if (!eventTime.isNullOrBlank()) " • $eventTime" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = countdownText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "👉 Toca para abrir calendario",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (canToggleFinished) {
                IconButton(
                    onClick = onToggleFinished,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isFinished) Icons.Default.RestartAlt else Icons.Default.CheckCircle,
                        contentDescription = if (isFinished) "Reactivar evento" else "Marcar finalizado",
                        tint = if (isFinished) MotoGoldSecondary else Color(0xFF22C55E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// COMPONENTE: PANEL DE AVISOS INTELIGENTES DE COMUNIDAD TX
// ═══════════════════════════════════════════════════════════════
data class ItemAvisoInteligente(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val consejoOSlogan: String? = null,
    val categoriaTag: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
    val colorAcento: Color,
    val textoBoton: String? = null,
    val accionBoton: (() -> Unit)? = null
)

@Composable
fun AvisosComunidadInteligentesSection(
    allMembers: List<MemberProfile>,
    allWorkshops: List<WorkshopDirectoryItem>,
    allPrivateGroups: List<PrivateGroup>,
    currentMember: MemberProfile?,
    onNavigateToTab: ((com.example.NavigationTab) -> Unit)?
) {
    // 1. Detección del piloto más rápido del equipo
    val fastestPilot = remember(allMembers) {
        allMembers.filter { it.topSpeedRecordKmh > 0f }.maxByOrNull { it.topSpeedRecordKmh }
    }

    // 2. Detección del líder de kilometraje en carretera
    val topMileagePilot = remember(allMembers) {
        allMembers.filter { it.totalKmRidden > 0.0 }.maxByOrNull { it.totalKmRidden }
    }

    // 3. Detección de aliados comerciales / talleres con Cashea
    val casheaWorkshop = remember(allWorkshops) {
        allWorkshops.find { it.hasCredit || it.creditPlatforms.contains("Cashea", ignoreCase = true) }
            ?: allWorkshops.firstOrNull()
    }

    // 4. Detección de última sala privada creada
    val latestPrivateRoom = remember(allPrivateGroups) {
        allPrivateGroups.lastOrNull { !it.isDeleted && !it.isBlockedByDirectiva }
    }

    val avisos = remember(fastestPilot, topMileagePilot, casheaWorkshop, latestPrivateRoom, currentMember) {
        val list = mutableListOf<ItemAvisoInteligente>()

        // AVISO A: RÉCORD DE VELOCIDAD
        if (fastestPilot != null) {
            val nombre = fastestPilot.fullName.ifBlank { fastestPilot.nickname.ifBlank { "Piloto TX" } }
            list.add(
                ItemAvisoInteligente(
                    id = "velocidad_record",
                    titulo = "⚡ PILOTO MÁS RÁPIDO DEL EQUIPO",
                    subtitulo = "El piloto $nombre es el más veloz del equipo registrando ${"%.1f".format(fastestPilot.topSpeedRecordKmh)} km/h en su ${fastestPilot.bikeBrand} ${fastestPilot.bikeModel}.",
                    consejoOSlogan = "⚠️ ¡Maneja con prudencia! Usa siempre casco certificado, guantes y equipo de protección en carretera.",
                    categoriaTag = "Telemetría & Récord",
                    icono = Icons.Default.Speed,
                    colorAcento = Color(0xFFE11D48),
                    textoBoton = "Ver Velocímetro",
                    accionBoton = { onNavigateToTab?.invoke(com.example.NavigationTab.VELOCIMETRO) }
                )
            )
        }

        // AVISO B: MAYOR KILOMETRAJE
        if (topMileagePilot != null) {
            val nombreKm = topMileagePilot.fullName.ifBlank { topMileagePilot.nickname.ifBlank { "Piloto TX" } }
            list.add(
                ItemAvisoInteligente(
                    id = "kilometraje_record",
                    titulo = "🏆 LEYENDA DEL ASFALTO: MAYOR KM",
                    subtitulo = "¡Felicidades al piloto $nombreKm! Lidera el ranking con ${"%.1f".format(topMileagePilot.totalKmRidden)} km acumulados en carretera.",
                    consejoOSlogan = "🔥 ¡Cada kilómetro en rodada cuenta para ascender en el escalafón del club!",
                    categoriaTag = "Ranking Rutero",
                    icono = Icons.Default.WorkspacePremium,
                    colorAcento = Color(0xFFD97706),
                    textoBoton = "Ver Ranking",
                    accionBoton = { onNavigateToTab?.invoke(com.example.NavigationTab.RANKING) }
                )
            )
        }

        // AVISO C: DIRECTORIO Y TALLERES CON CASHEA EN MAPA
        if (casheaWorkshop != null) {
            val hasCashea = casheaWorkshop.hasCredit || casheaWorkshop.creditPlatforms.contains("Cashea", ignoreCase = true)
            list.add(
                ItemAvisoInteligente(
                    id = "taller_mapa",
                    titulo = "🛠️ AGENDA DE SERVICIOS & MAPA TX",
                    subtitulo = "Nuevo aliado disponible: '${casheaWorkshop.name}' (${casheaWorkshop.type}) en ${casheaWorkshop.city.ifBlank { "Aragua" }}.${if (hasCashea) " ¡Con financiamiento Cashea disponible!" else ""}",
                    consejoOSlogan = "📍 Consulta puntos mecánicos, caucheras y auxilio vial directamente en el mapa.",
                    categoriaTag = if (hasCashea) "Cashea Disponible" else "Directorio & Mapa",
                    icono = Icons.Default.Storefront,
                    colorAcento = Color(0xFF0284C7),
                    textoBoton = "Ver en el Mapa",
                    accionBoton = { onNavigateToTab?.invoke(com.example.NavigationTab.MAPA) }
                )
            )
        }

        // AVISO D: SALA PRIVADA MESH TX
        if (latestPrivateRoom != null) {
            list.add(
                ItemAvisoInteligente(
                    id = "sala_mesh",
                    titulo = "🎙️ SALA PRIVADA DE COMUNICACIÓN MESH",
                    subtitulo = "Se ha creado la sala táctica '${latestPrivateRoom.name}'. Solicita el código de acceso a tus compañeros de ruta para entrar a la frecuencia.",
                    consejoOSlogan = "📡 Comunicación inter-casco PTT sin internet ni datos en modo caravana.",
                    categoriaTag = "Malla Mesh TX",
                    icono = Icons.Default.Sensors,
                    colorAcento = Color(0xFF059669),
                    textoBoton = "Frecuencia Táctica",
                    accionBoton = { onNavigateToTab?.invoke(com.example.NavigationTab.CHAT) }
                )
            )
        }

        // AVISO E: ESTATUS DE SOLVENCIA Y CARNET TX
        if (currentMember != null) {
            val isSolvent = currentMember.solvencyStatus
            list.add(
                ItemAvisoInteligente(
                    id = "carnet_solvencia",
                    titulo = if (isSolvent) "🛡️ CARNET TX VALIDADO & SOLVENTE" else "⚠️ FICHA CON CUOTA PENDIENTE",
                    subtitulo = if (isSolvent) {
                        "¡Ficha de Piloto Solvente! Tu carnet digital QR está verificado para rodadas, parches y eventos nacionales."
                    } else {
                        "Tu ficha registra cuota pendiente en el capítulo. Acércate a la directiva para estar al día y disfrutar de todos los beneficios."
                    },
                    consejoOSlogan = "🪪 Mantén tu QR disponible para escaneo en puntos de control y eventos.",
                    categoriaTag = if (isSolvent) "Piloto Solvente" else "Directiva TX",
                    icono = Icons.Default.Badge,
                    colorAcento = if (isSolvent) Color(0xFF16A34A) else Color(0xFFDC2626),
                    textoBoton = "Ver Carnet TX",
                    accionBoton = { onNavigateToTab?.invoke(com.example.NavigationTab.PROFILE) }
                )
            )
        }

        list
    }

    if (avisos.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = "RADAR DE NOVEDADES & COMUNIDAD TX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = DashboardFondoConfig.ColorTextoPrimario,
                    letterSpacing = 0.5.sp
                )
            }
            Surface(
                shape = CircleShape,
                color = MotoOrangePrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${avisos.size} AVISOS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = MotoOrangePrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Carrusel Horizontal de Avisos Inteligentes
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(avisos, key = { it.id }) { aviso ->
                AvisoInteligenteCard(
                    aviso = aviso,
                    modifier = Modifier.width(310.dp)
                )
            }
        }
    }
}

@Composable
fun AvisoInteligenteCard(
    aviso: ItemAvisoInteligente,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
        border = BorderStroke(1.dp, aviso.colorAcento.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = aviso.colorAcento.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = aviso.icono,
                            contentDescription = null,
                            tint = aviso.colorAcento,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = aviso.colorAcento.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, aviso.colorAcento.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = aviso.categoriaTag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = aviso.colorAcento,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = aviso.titulo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = DashboardFondoConfig.ColorTextoPrimario,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = aviso.subtitulo,
                fontSize = 11.sp,
                color = DashboardFondoConfig.ColorTextoSecundario,
                lineHeight = 15.sp
            )

            if (!aviso.consejoOSlogan.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DashboardFondoConfig.ColorFondoClaro,
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aviso.consejoOSlogan,
                        fontSize = 10.sp,
                        color = DashboardFondoConfig.ColorTextoSecundario,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        lineHeight = 13.sp
                    )
                }
            }

            if (!aviso.textoBoton.isNullOrBlank() && aviso.accionBoton != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = aviso.accionBoton,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = aviso.colorAcento),
                    border = BorderStroke(1.dp, aviso.colorAcento.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Text(
                        text = aviso.textoBoton,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = aviso.colorAcento
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = aviso.colorAcento
                    )
                }
            }
        }
    }
}


