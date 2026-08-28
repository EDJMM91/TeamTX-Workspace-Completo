package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
        floatingActionButton = {
            if (isDirectivaMode) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Publicar Aviso / Reto") },
                    containerColor = MotoOrangePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_create_notice")
                )
            }
        },
        modifier = modifier.fillMaxSize()
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uploadError != null) StatusError.copy(alpha = 0.9f) else StatusSuccess.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                                    fontSize = 14.sp,
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Incomplete Profile Warning Banner
            if (currentMember != null && !currentMember.isProfileComplete) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261908)),
                        border = BorderStroke(1.5.dp, MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(30.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FICHA DE PERFIL INCOMPLETA", fontWeight = FontWeight.Black, color = TxGoldLight, fontSize = 13.sp)
                                Text("Por favor completa tus datos personales, contacto SOS y fotos en Carnet TX.", color = Color.White, fontSize = 11.sp)
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF19222E)),
                        border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.5f)),
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
                                Icon(Icons.Default.Cake, contentDescription = null, tint = MotoOrangePrimary)
                                Text(
                                    text = "¡CUMPLEAÑEROS DEL MES!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
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
                                                .background(MotoOrangePrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MotoOrangePrimary
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "${member.fullName} (${member.memberNumber})",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = member.role.displayName,
                                                fontSize = 10.sp,
                                                color = Color(0xFFB0BEC5)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Categories Filter Bar
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
                            color = MaterialTheme.colorScheme.onBackground
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
                                    color = MotoOrangePrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar avisos",
                                    tint = MotoOrangePrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(end = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("Todos (${publications.size})", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.AVISO_OFICIAL,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.AVISO_OFICIAL) null else NoticeCategory.AVISO_OFICIAL
                                },
                                label = { Text("Avisos", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.RETO_MOTERO,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.RETO_MOTERO) null else NoticeCategory.RETO_MOTERO
                                },
                                label = { Text("Retos TX", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == NoticeCategory.CAPACITACION,
                                onClick = {
                                    selectedCategoryFilter =
                                        if (selectedCategoryFilter == NoticeCategory.CAPACITACION) null else NoticeCategory.CAPACITACION
                                },
                                label = { Text("Mecánica", fontSize = 12.sp) }
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

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (pub.isPinned) MotoOrangePrimary else if (pub.priority == NoticePriority.URGENTE) StatusError else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pub.isPinned) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("notice_card_${pub.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Tags: Category + Priority / Pinned
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
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MotoOrangePrimary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MotoOrangePrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = MotoOrangePrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("FIJADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MotoOrangePrimary)
                            }
                        }
                    }

                    val catBg = when (pub.category) {
                        NoticeCategory.AVISO_OFICIAL -> MotoOrangeDark
                        NoticeCategory.RETO_MOTERO -> Color(0xFFE65100)
                        NoticeCategory.COMUNICADO -> Color(0xFF0D47A1)
                        NoticeCategory.NOTICIA_RUTA -> Color(0xFF2E7D32)
                        NoticeCategory.CAPACITACION -> Color(0xFF4A148C)
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = catBg.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, catBg)
                    ) {
                        Text(
                            text = pub.category.displayName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (pub.priority == NoticePriority.URGENTE) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StatusError.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, StatusError)
                        ) {
                            Text(
                                text = "URGENTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusError,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Fijado",
                            tint = MotoOrangePrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
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
                                tint = MotoOrangePrimary,
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
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pub.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            // Flyer / Imagen de la publicación
            if (!pub.imageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val imageUrl = pub.imageUrl
                    LaunchedEffect(imageUrl) {
                        android.util.Log.d("TEAM_TX_IMAGES", "🖼️ Intentando cargar imagen para aviso: ${pub.title} | URL: $imageUrl")
                    }
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = "Flyer de la Publicación",
                        onSuccess = {
                            android.util.Log.i("TEAM_TX_IMAGES", "✅ Imagen cargada con éxito: $imageUrl")
                        },
                        onError = { state ->
                            android.util.Log.e("TEAM_TX_IMAGES", "❌ Error cargando imagen: $imageUrl | Causa: ${state.result.throwable.message}")
                        },
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = MotoOrangePrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(Color.DarkGray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "Error al cargar imagen",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Error al cargar flyer",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 450.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }

            // If challenge: show special Challenge badge
            if (pub.category == NoticeCategory.RETO_MOTERO && pub.targetChallengeDistanceKm > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF231C10),
                    border = BorderStroke(1.dp, MotoGoldSecondary)
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
                            modifier = Modifier.size(28.dp)
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
                                color = Color(0xFFD4E0ED),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Author, Date, Likes, Telegram post & Comments Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pub.authorName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                modifier = Modifier.size(18.dp)
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
                            tint = if (isCommentsExpanded) TxFlameRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comments.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCommentsExpanded) TxFlameRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Like button
                    val isLikedByMe = pub.likedByMemberIds.split(",").filter { it.isNotBlank() }.contains(currentMember?.id?.toString() ?: "")
                    TextButton(
                        onClick = onLike,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("btn_like_${pub.id}")
                    ) {
                        Icon(
                            if (isLikedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Me gusta",
                            tint = if (isLikedByMe) MotoOrangePrimary else Color(0xFF90A4AE),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pub.likesCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLikedByMe) MotoOrangePrimary else Color(0xFF90A4AE)
                        )
                    }
                }
            }

            // Expandable Comments Section (Respuestas de Miembros)
            AnimatedVisibility(visible = isCommentsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "RESPUESTAS DE LA HERMANDAD (${comments.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TxGoldSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (comments.isEmpty()) {
                        Text(
                            text = "No hay respuestas todavía. ¡Sé el primer piloto en comentar!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            comments.forEach { c ->
                                val roleColor = Color(c.authorRole.badgeColorHex)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF161C26),
                                    border = BorderStroke(0.5.dp, AsphaltDarkBorder),
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
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = roleColor.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, roleColor)
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
                                            Text(text = commentTime, fontSize = 9.sp, color = Color(0xFF78909C))
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = c.content,
                                            fontSize = 12.sp,
                                            color = Color(0xFFECEFF1),
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
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                                    containerColor = if (commentInputText.isNotBlank()) TxFlameRed else MaterialTheme.colorScheme.surfaceVariant,
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
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Text("Comentarios bloqueados por la directiva para este aviso", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

