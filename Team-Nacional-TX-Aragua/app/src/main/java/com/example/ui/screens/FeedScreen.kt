package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.openUrl
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

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
        allowComments: Boolean
    ) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    uploadError: String? = null,
    uploadSuccess: Boolean = false,
    onDismissUploadStatus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<NoticeCategory?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPublicationForEdit by remember { mutableStateOf<Publication?>(null) }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) Color(0xFF121212) else Color(0xFFF2F4F7)

    // Auto-dismiss upload status banner after 4 seconds
    LaunchedEffect(uploadError, uploadSuccess) {
        if (uploadError != null || uploadSuccess) {
            delay(4000)
            onDismissUploadStatus()
        }
    }

    val validPublications = remember(publications) {
        publications.filter {
            val t = it.title.trim()
            val c = it.content.trim()
            val isDefaultOnly = (t.equals("Aviso Oficial", ignoreCase = true) && c.isEmpty()) || (c.equals("Aviso Oficial", ignoreCase = true) && t.isEmpty())
            (t.isNotBlank() || c.isNotBlank()) && !isDefaultOnly && it.id != 0L
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
                            containerColor = if (isDark) Color(0xFF261908) else Color(0xFFFFF8E1)
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
                                    color = if (isDark) TxGoldLight else Color(0xFFB45309),
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
                            containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
                        ),
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
                        Text(
                            text = "No hay avisos en esta categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        onAddComment = { content -> onAddComment(pub.id, content) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateNoticeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments ->
                onCreatePublication(title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments)
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
    val isDark = isSystemInDarkTheme()
    val bgColor = if (selected) {
        TxFlameRed
    } else {
        if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
    }
    val textColor = if (selected) {
        Color.White
    } else {
        if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
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

    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
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
                            NoticeCategory.NOTICIA_RUTA -> if (isDark) Pair(Color(0xFF1B3322), Color(0xFF81C784)) else Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            NoticeCategory.CAPACITACION -> if (isDark) Pair(Color(0xFF2C1938), Color(0xFFBA68C8)) else Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
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

                    if (canDelete) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    contentDescription = "Eliminar aviso",
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

            // Flyer / Imagen de la publicación (Ancho completo de borde a borde sin padding lateral)
            if (!pub.imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val imageUrl = pub.imageUrl
                LaunchedEffect(imageUrl) {
                    android.util.Log.d("TEAM_TX_IMAGES", "🖼️ Cargando flyer para: ${pub.title} | URL: $imageUrl")
                }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 420.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
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

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))

                // Footer: Author, Date, Likes, Telegram post & Comments Button
                val secondaryTextColor = if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = pub.authorName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                        )
                        Text(
                            text = dateStr,
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (pub.telegramPostUrl != null) {
                            IconButton(
                                onClick = { openUrl(context, pub.telegramPostUrl!!) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Ver en Telegram",
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Comments toggle button
                        TextButton(
                            onClick = { isCommentsExpanded = !isCommentsExpanded },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_comments_${pub.id}")
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "Respuestas",
                                tint = if (isCommentsExpanded) TxFlameRed else secondaryTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${comments.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCommentsExpanded) TxFlameRed else secondaryTextColor
                            )
                        }

                        // Like button
                        val myMemberIdStr = currentMember?.id?.toString() ?: ""
                        val isLikedByMe = pub.likedByMemberIds.split(",").map { it.trim() }.contains(myMemberIdStr) && myMemberIdStr.isNotEmpty()

                        TextButton(
                            onClick = onLike,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_like_${pub.id}")
                        ) {
                            Icon(
                                if (isLikedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Me gusta",
                                tint = if (isLikedByMe) TxFlameRed else secondaryTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${pub.likesCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLikedByMe) TxFlameRed else secondaryTextColor
                            )
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
        allowComments: Boolean
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

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Publicación Directiva",
                fontWeight = FontWeight.Bold
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NoticeCategory.values().take(3).forEach { cat ->
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
                            allowComments
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

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Publicación Directiva",
                fontWeight = FontWeight.Bold
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NoticeCategory.values().take(3).forEach { cat ->
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
                            allowComments = allowComments
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

