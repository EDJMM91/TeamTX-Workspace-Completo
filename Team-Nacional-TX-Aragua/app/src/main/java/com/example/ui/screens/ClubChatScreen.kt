@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class ChatChannel(
    val id: String,
    val title: String,
    val shortDesc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val directivaOnly: Boolean = false
) {
    GENERAL("GENERAL", "#General TX", "Conversación abierta, hermandad y fotos", Icons.Default.ChatBubble),
    RODADAS("RODADAS", "#Rutas & Rodadas", "Coordinación de salidas, ritmo y puntos de encuentro", Icons.Default.TwoWheeler),
    MECANICA_AUXILIO("MECANICA_AUXILIO", "#Auxilio Mecánico", "Diagnósticos, repuestos y ayuda técnica", Icons.Default.Build),
    DIRECTIVA("DIRECTIVA", "#Directiva & Staff", "Consejo de directiva y administración central", Icons.Default.Shield, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubChatScreen(
    messages: List<ChatMessage>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    roleConfigs: List<RoleConfig>,
    activeChannelId: String,
    onSelectChannel: (String) -> Unit,
    onSendMessage: (channelId: String, text: String, isRadioCallout: Boolean, replyToId: Long?, replyToSender: String?, replyToText: String?) -> Unit,
    onDeleteMessage: (Long) -> Unit = {},
    onDeleteMessageForMe: (Long) -> Unit = {},
    onMarkMessageAsRead: (ChatMessage) -> Unit = {},
    onMarkChannelAsRead: (String) -> Unit = {},
    onSendSticker: (String, java.io.File) -> Unit = { _, _ -> },
    onSendAudio: (channelId: String, audioFile: java.io.File, durationSeconds: Int) -> Unit = { _, _, _ -> },
    onToggleReaction: (messageId: Long, emoji: String) -> Unit = { _, _ -> },
    onToggleBottomNav: (() -> Unit)? = null,
    onBack: () -> Unit = {},
    isChatEnabled: Boolean = true,
    showOnlyDirectiva: Boolean = false,
    privateGroups: List<PrivateGroup> = emptyList(),
    allMembers: List<MemberProfile> = emptyList(),
    myCreatedGroupsCount: Int = 0,
    onCreatePrivateGroup: (name: String, description: String, memberIds: List<Long>, onResult: (Boolean, String) -> Unit) -> Unit = { _, _, _, _ -> },
    onAddMembersToGroup: (groupId: String, memberIds: List<Long>) -> Unit = { _, _ -> },
    onRemoveMemberFromGroup: (groupId: String, memberId: Long) -> Unit = { _, _ -> },
    onLeaveGroup: (groupId: String) -> Unit = {},
    onDeleteGroup: (groupId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showStickerBox by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Estados de diálogos de Grupos Privados
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showGroupMembersDialog by remember { mutableStateOf(false) }
    var showAddMembersDialog by remember { mutableStateOf(false) }
    var showLeaveGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Estados de Grabación y Reproducción de Audio
    val estaGrabandoAudio by com.example.chat.GestorAudio.estaGrabando.collectAsState()
    val segundosGrabados by com.example.chat.GestorAudio.segundosGrabados.collectAsState()
    val audioPlayingUrl by com.example.chat.GestorAudio.audioEnReproduccionUrl.collectAsState()
    val audioPlaybackProgress by com.example.chat.GestorAudio.progresoReproduccion.collectAsState()
    val isAudioPaused by com.example.chat.GestorAudio.estaPausado.collectAsState()

    // Permiso de micrófono para notas de voz
    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.chat.GestorAudio.iniciarGrabacion(context)
        } else {
            android.widget.Toast.makeText(context, "Se requiere permiso de micrófono para notas de voz", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-ocultar teclado al hacer scroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            keyboardController?.hide()
        }
    }

    // Auto-ocultar teclado tras 15s de inactividad sin escribir
    var lastTypingTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(messageInput) {
        lastTypingTime = System.currentTimeMillis()
        if (messageInput.isNotEmpty()) {
            kotlinx.coroutines.delay(15000L)
            if (System.currentTimeMillis() - lastTypingTime >= 15000L) {
                keyboardController?.hide()
            }
        }
    }

    val currentPrivateGroup = remember(privateGroups, activeChannelId) {
        privateGroups.find { it.id == activeChannelId }
    }
    val isPrivateGroupActive = currentPrivateGroup != null || activeChannelId.startsWith("GRP_")
    val isGroupBlocked = currentPrivateGroup?.isBlockedByDirectiva == true

    val myMemberIdStr = currentMember?.id?.toString() ?: ""
    val filteredMessages = remember(messages, activeChannelId, myMemberIdStr) {
        messages.filter {
            it.channelId == activeChannelId &&
            (myMemberIdStr.isBlank() || !it.deletedForMemberIds.split(",").contains(myMemberIdStr))
        }
    }

    val activeChannel = remember(activeChannelId, isPrivateGroupActive) {
        if (isPrivateGroupActive) null
        else ChatChannel.values().find { it.id == activeChannelId } ?: ChatChannel.GENERAL
    }

    // Marcar automáticamente mensajes del canal activo como leídos
    LaunchedEffect(activeChannelId, messages.size, currentMember) {
        val memberId = currentMember?.id ?: return@LaunchedEffect
        val hasUnread = messages.any { it.channelId == activeChannelId && it.senderMemberId != memberId && !it.readBy.contains(memberId) }
        if (hasUnread) {
            onMarkChannelAsRead(activeChannelId)
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    // Auto-scroll when keyboard opens
    LaunchedEffect(messageInput) {
        if (filteredMessages.isNotEmpty() && messageInput.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            // ═══════════════════════════════════════════════
            // HEADER: Canal + Info + Botón Configuración
            // ═══════════════════════════════════════════════
            Surface(
                color = Color(0xFF1A1F2E),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top bar: Channel selector + Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showOnlyDirectiva) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Volver al Panel",
                                    tint = MotoOrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val channelsToShow = if (showOnlyDirectiva) {
                                ChatChannel.values().filter { it.directivaOnly }
                            } else {
                                ChatChannel.values().filter { !it.directivaOnly }
                            }
                            items(channelsToShow) { channel ->
                                val isSelected = channel.id == activeChannelId
                                val unreadForThisChannel = remember(messages, currentMember, channel.id) {
                                    val memberId = currentMember?.id ?: 0L
                                    if (memberId <= 0L) 0
                                    else messages.count { it.channelId == channel.id && it.senderMemberId != memberId && !it.readBy.contains(memberId) }
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectChannel(channel.id) },
                                    leadingIcon = {
                                        Icon(
                                            channel.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                channel.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                            if (unreadForThisChannel > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = StatusError,
                                                    modifier = Modifier.defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = if (unreadForThisChannel > 99) "99+" else "$unreadForThisChannel",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (channel.directivaOnly) TxRedDark else TxFlameRed,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            // ═══════════════════════════════════════════════
                            // BOTÓN CREAR GRUPO PRIVADO (Con ícono de grupo)
                            // ═══════════════════════════════════════════════
                            if (!showOnlyDirectiva) {
                                item {
                                    AssistChip(
                                        onClick = { showCreateGroupDialog = true },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.GroupAdd,
                                                contentDescription = "Crear Grupo Privado",
                                                tint = MotoGoldSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                "+ Crear Grupo",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MotoGoldSecondary
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color(0xFF262114)
                                        ),
                                        border = BorderStroke(1.dp, MotoGoldSecondary.copy(alpha = 0.6f))
                                    )
                                }

                                // ═══════════════════════════════════════════════
                                // CHIPS DE GRUPOS PRIVADOS ACTIVOS DEL USUARIO
                                // ═══════════════════════════════════════════════
                                items(privateGroups) { group ->
                                    val isSelected = group.id == activeChannelId
                                    val unreadForThisGroup = remember(messages, currentMember, group.id) {
                                        val memberId = currentMember?.id ?: 0L
                                        if (memberId <= 0L) 0
                                        else messages.count { it.channelId == group.id && it.senderMemberId != memberId && !it.readBy.contains(memberId) }
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectChannel(group.id) },
                                        leadingIcon = {
                                            Icon(
                                                if (group.isBlockedByDirectiva) Icons.Default.Lock else Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = if (group.isBlockedByDirectiva) StatusError else if (isSelected) Color.White else MotoOrangePrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    group.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                                if (unreadForThisGroup > 0) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = StatusError,
                                                        modifier = Modifier.defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = if (unreadForThisGroup > 99) "99+" else "$unreadForThisGroup",
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (group.isBlockedByDirectiva) StatusError.copy(alpha = 0.8f) else MotoOrangePrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF1E2333)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (group.isBlockedByDirectiva) StatusError.copy(alpha = 0.5f) else Color(0xFF333B50)
                                        )
                                    )
                                }
                            }
                        }

                        // Botón Configuración del Chat
                        Box {
                            IconButton(
                                onClick = { showSettingsMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Configuración del chat",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Menú desplegable
                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false }
                            ) {
                                if (isPrivateGroupActive && currentPrivateGroup != null) {
                                    val isCreatorOrAdmin = currentPrivateGroup.creatorMemberId == currentMember?.id || currentPrivateGroup.adminIds.contains(currentMember?.id)
                                    
                                    DropdownMenuItem(
                                        text = { Text("Miembros (${currentPrivateGroup.memberIds.size})") },
                                        onClick = {
                                            showSettingsMenu = false
                                            showGroupMembersDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = MotoOrangePrimary) }
                                    )

                                    if (isCreatorOrAdmin) {
                                        DropdownMenuItem(
                                            text = { Text("Agregar miembros") },
                                            onClick = {
                                                showSettingsMenu = false
                                                showAddMembersDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MotoGoldSecondary) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Eliminar grupo", color = StatusError) },
                                            onClick = {
                                                showSettingsMenu = false
                                                showDeleteGroupDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusError) }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Salir del grupo", color = StatusError) },
                                            onClick = {
                                                showSettingsMenu = false
                                                showLeaveGroupDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = StatusError) }
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF2A2F3E))
                                }

                                DropdownMenuItem(
                                    text = { Text("Configuración del chat") },
                                    onClick = {
                                        showSettingsMenu = false
                                        showSettingsDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Notificaciones") },
                                    onClick = {
                                        showSettingsMenu = false
                                        com.example.chat.PreferenciasChat.notificacionesCanal =
                                            !com.example.chat.PreferenciasChat.notificacionesCanal
                                        android.widget.Toast.makeText(
                                            context,
                                            if (com.example.chat.PreferenciasChat.notificacionesCanal) "Notificaciones activadas" else "Notificaciones desactivadas",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (com.example.chat.PreferenciasChat.notificacionesCanal) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        Text(
                                            if (com.example.chat.PreferenciasChat.notificacionesCanal) "ON" else "OFF",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (com.example.chat.PreferenciasChat.notificacionesCanal) StatusSuccess else StatusError
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sonido de mensajes") },
                                    onClick = {
                                        showSettingsMenu = false
                                        com.example.chat.PreferenciasChat.sonidoMensajes =
                                            !com.example.chat.PreferenciasChat.sonidoMensajes
                                        if (com.example.chat.PreferenciasChat.sonidoMensajes) {
                                            com.example.chat.PreferenciasChat.sonidoMensajeEnviado(context)
                                        }
                                        android.widget.Toast.makeText(
                                            context,
                                            if (com.example.chat.PreferenciasChat.sonidoMensajes) "Sonidos activados" else "Sonidos desactivados",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (com.example.chat.PreferenciasChat.sonidoMensajes) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        Text(
                                            if (com.example.chat.PreferenciasChat.sonidoMensajes) "ON" else "OFF",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (com.example.chat.PreferenciasChat.sonidoMensajes) StatusSuccess else StatusError
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Info del canal / sala") },
                                    onClick = {
                                        showSettingsMenu = false
                                        if (isPrivateGroupActive) showGroupMembersDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                                )
                            }
                        }
                    }

                    // Channel description bar
                    Surface(
                        color = Color(0xFF131722),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (isGroupBlocked) Icons.Default.Lock
                                else if (isPrivateGroupActive) Icons.Default.Groups
                                else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isGroupBlocked) StatusError else TxGoldSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isPrivateGroupActive && currentPrivateGroup != null) {
                                    if (currentPrivateGroup.isBlockedByDirectiva) {
                                        "🔒 BLOQUEADO POR DIRECTIVA: ${currentPrivateGroup.blockedReason.ifBlank { "Sanción preventiva" }}"
                                    } else {
                                        "${currentPrivateGroup.name} • ${currentPrivateGroup.memberIds.size} miembros (Creador: ${currentPrivateGroup.creatorNickname})"
                                    }
                                } else {
                                    activeChannel?.shortDesc ?: ""
                                },
                                fontSize = 10.sp,
                                color = if (isGroupBlocked) Color(0xFFFFCDD2) else Color(0xFF90A4AE),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════
            // MENSAJES
            // ═══════════════════════════════════════════════
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (filteredMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF4A5568),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No hay mensajes aún",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF90A4AE),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Sé el primero en saludar a los hermanos del club",
                                    fontSize = 12.sp,
                                    color = Color(0xFF607D8B)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredMessages, key = { it.id }) { msg ->
                        LaunchedEffect(msg.id) {
                            onMarkMessageAsRead(msg)
                        }
                        // Sonido al recibir mensaje de otro usuario
                        if (msg.senderMemberId != currentMember?.id && filteredMessages.indexOf(msg) == filteredMessages.lastIndex) {
                            LaunchedEffect(Unit) {
                                com.example.chat.PreferenciasChat.sonidoMensajeRecibido(context)
                            }
                        }

                        val canDeleteAll = isDirectivaMode || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true) || (currentMember?.role == MemberRole.PRESIDENTE) || (currentMember?.role == MemberRole.DIRECTIVA) || (msg.senderMemberId == currentMember?.id)

                        ChatMessageBubble(
                            message = msg,
                            isMe = msg.senderMemberId == currentMember?.id,
                            isDirectivaMode = isDirectivaMode,
                            canDeleteForAll = canDeleteAll,
                            onDelete = { onDeleteMessage(msg.id) },
                            onDeleteForMe = { onDeleteMessageForMe(msg.id) },
                            onReply = { replyingToMessage = it },
                            onToggleReaction = { emoji -> onToggleReaction(msg.id, emoji) },
                            currentMemberId = currentMember?.id,
                            audioPlayingUrl = audioPlayingUrl,
                            audioPlaybackProgress = audioPlaybackProgress,
                            isAudioPaused = isAudioPaused
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════
            // INPUT BAR (estilo WhatsApp con Citas / Respuestas / Notas de Voz)
            // ═══════════════════════════════════════════════
            Surface(
                color = Color(0xFF1A1F2E),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Banner de respuesta / citar mensaje
                    if (replyingToMessage != null) {
                        Surface(
                            color = Color(0xFF242C3D),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                            border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(28.dp)
                                            .background(MotoOrangePrimary, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Respondiendo a ${replyingToMessage?.senderName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MotoOrangePrimary
                                        )
                                        Text(
                                            text = replyingToMessage?.messageText ?: "Sticker",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { replyingToMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar respuesta", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    val isMutedInThisChannel = currentMember?.isChatMuted == true && activeChannelId != "DIRECTIVA"
                    val canSendInThisChannel = !isMutedInThisChannel && !isGroupBlocked && (isChatEnabled || isDirectivaMode)
                    
                    if (canSendInThisChannel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (estaGrabandoAudio) {
                                // 🎙️ PANEL DE GRABACIÓN DE AUDIO EN VIVO
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .background(Color(0xFF242C3D), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(TxFlameRed)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val min = segundosGrabados / 60
                                        val sec = segundosGrabados % 60
                                        Text(
                                            text = String.format("%02d:%02d", min, sec),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Grabando nota de voz...",
                                            color = Color(0xFF90A4AE),
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Botón Cancelar Grabación
                                    IconButton(
                                        onClick = {
                                            com.example.chat.GestorAudio.detenerGrabacion(descartar = true)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Cancelar", tint = StatusError, modifier = Modifier.size(20.dp))
                                    }
                                }

                                // Botón Enviar Audio
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF25D366),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable {
                                            val resultado = com.example.chat.GestorAudio.detenerGrabacion(descartar = false)
                                            if (resultado != null) {
                                                val (archivoAudio, duracionSeg) = resultado
                                                onSendAudio(activeChannelId, archivoAudio, duracionSeg)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Send, contentDescription = "Enviar Audio", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            } else {
                                // Botón menú/adjuntar
                                if (onToggleBottomNav != null) {
                                    IconButton(
                                        onClick = onToggleBottomNav,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Menú",
                                            tint = Color(0xFF90A4AE),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Campo de texto estilo WhatsApp
                                OutlinedTextField(
                                    value = messageInput,
                                    onValueChange = { messageInput = it },
                                    placeholder = {
                                        Text(
                                            "Mensaje",
                                            fontSize = 14.sp,
                                            color = Color(0xFF607D8B)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_chat_message"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color(0xFF2A2F3E),
                                        unfocusedContainerColor = Color(0xFF2A2F3E),
                                        cursorColor = TxFlameRed
                                    ),
                                    maxLines = 5,
                                    textStyle = LocalTextStyle.current.copy(
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                )

                                // Botón Stickers
                                IconButton(
                                    onClick = { showStickerBox = !showStickerBox },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEmotions,
                                        contentDescription = "Stickers",
                                        tint = if (showStickerBox) MotoOrangePrimary else Color(0xFF90A4AE),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Botón Enviar / Micrófono
                                Surface(
                                    shape = CircleShape,
                                    color = if (messageInput.isNotBlank()) TxFlameRed else Color(0xFF2A2F3E),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable {
                                            if (messageInput.isNotBlank()) {
                                                com.example.chat.PreferenciasChat.sonidoMensajeEnviado(context)
                                                onSendMessage(
                                                    activeChannelId,
                                                    messageInput,
                                                    false,
                                                    replyingToMessage?.id,
                                                    replyingToMessage?.senderName,
                                                    replyingToMessage?.messageText
                                                )
                                                replyingToMessage = null
                                                messageInput = ""
                                                keyboardController?.hide()
                                            } else {
                                                // Grabar audio con comprobación de permisos
                                                val permCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context,
                                                    android.Manifest.permission.RECORD_AUDIO
                                                )
                                                if (permCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    com.example.chat.GestorAudio.iniciarGrabacion(context)
                                                } else {
                                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                    border = BorderStroke(
                                        1.dp,
                                        if (messageInput.isNotBlank()) TxFlameRed else Color(0xFF4A5568)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (messageInput.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                                            contentDescription = if (messageInput.isNotBlank()) "Enviar" else "Micrófono",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ═══════════════════════════════════════════════
                        // STICKER BOX
                        // ═══════════════════════════════════════════════
                        AnimatedVisibility(
                            visible = showStickerBox,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val stickers by com.example.chat.GestorStickers.stickersGuardados.collectAsState()

                            LaunchedEffect(Unit) {
                                com.example.chat.GestorStickers.cargarStickersGuardados(context)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(Color(0xFF141923))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Stickers",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "(${stickers.size})",
                                            color = MotoGoldSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Botón Escanear WhatsApp
                                        Button(
                                            onClick = {
                                                android.widget.Toast.makeText(context, "Buscando stickers de WhatsApp...", android.widget.Toast.LENGTH_SHORT).show()
                                                com.example.chat.GestorStickers.escanearStickersWhatsApp(context) { count: Int ->
                                                    val msg = if (count > 0) "✅ Se sincronizaron $count stickers de WhatsApp" else "No se detectaron stickers nuevos en las carpetas de WhatsApp"
                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("WhatsApp", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        // Botón Elegir Carpeta
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    com.example.chat.GestorStickers.abrirSelectorCarpetaStickers(context as android.app.Activity)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Error abriendo selector de carpeta", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MotoGoldSecondary),
                                            border = BorderStroke(1.dp, MotoGoldSecondary),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(12.dp), tint = MotoGoldSecondary)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Carpeta", fontSize = 9.sp, color = MotoGoldSecondary)
                                        }

                                        // Botón Importar archivos
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    com.example.chat.GestorStickers.abrirSelectorStickers(context as android.app.Activity)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Error abriendo selector", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MotoOrangePrimary),
                                            border = BorderStroke(1.dp, MotoOrangePrimary),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Archivos", fontSize = 9.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (stickers.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "No hay stickers guardados.\nImporta desde tus archivos.",
                                            color = Color.Gray,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(70.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(stickers.size) { index ->
                                            val file = stickers[index]
                                            Surface(
                                                color = Color.Transparent,
                                                modifier = Modifier
                                                    .size(70.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .combinedClickable(
                                                        onClick = {
                                                            onSendSticker(activeChannelId, file)
                                                            showStickerBox = false
                                                        },
                                                        onLongClick = {
                                                            com.example.chat.GestorStickers.eliminarSticker(context, file)
                                                            android.widget.Toast.makeText(context, "Sticker eliminado de tu colección", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                            ) {
                                                coil.compose.AsyncImage(
                                                    model = file,
                                                    contentDescription = "Sticker",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isGroupBlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusError.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = "SALA PRIVADA BLOQUEADA POR LA DIRECTIVA",
                                        color = StatusError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = if (currentPrivateGroup?.blockedReason.isNullOrBlank()) "Medida disciplinaria preventiva aplicada por la Directiva." else "Motivo: ${currentPrivateGroup?.blockedReason}",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    } else if (isMutedInThisChannel) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusError.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.VolumeOff, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = "HAS SIDO SILENCIADO EN EL CHAT GENERAL",
                                        color = StatusError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = if (currentMember?.muteReason.isNullOrBlank()) "Medida disciplinaria aplicada por la Directiva." else "Motivo: ${currentMember?.muteReason}",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chat deshabilitado por la Directiva",
                                color = Color(0xFF607D8B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════
    // DIÁLOGO: Limpiar Chat
    // ═══════════════════════════════════════════════
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Limpiar chat", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar todos los mensajes de este canal? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        filteredMessages.forEach { onDeleteMessage(it.id) }
                        showClearChatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ═══════════════════════════════════════════════
    // DIÁLOGO: Configuración del Chat
    // ═══════════════════════════════════════════════
    if (showSettingsDialog) {
        var tempSonido by remember { mutableStateOf(com.example.chat.PreferenciasChat.sonidoMensajes) }
        var tempVibracion by remember { mutableStateOf(com.example.chat.PreferenciasChat.vibracion) }
        var tempNotificaciones by remember { mutableStateOf(com.example.chat.PreferenciasChat.notificacionesCanal) }
        var tempAvatares by remember { mutableStateOf(com.example.chat.PreferenciasChat.mostrarAvatares) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MotoOrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configuración del Chat", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Sonido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sonido de mensajes", fontSize = 14.sp)
                        }
                        Switch(
                            checked = tempSonido,
                            onCheckedChange = {
                                tempSonido = it
                                com.example.chat.PreferenciasChat.sonidoMensajes = it
                                if (it) com.example.chat.PreferenciasChat.sonidoMensajeEnviado(context)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MotoOrangePrimary)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF2A2F3E))

                    // Vibración
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vibración", fontSize = 14.sp)
                        }
                        Switch(
                            checked = tempVibracion,
                            onCheckedChange = {
                                tempVibracion = it
                                com.example.chat.PreferenciasChat.vibracion = it
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MotoOrangePrimary)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF2A2F3E))

                    // Notificaciones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notificaciones", fontSize = 14.sp)
                        }
                        Switch(
                            checked = tempNotificaciones,
                            onCheckedChange = {
                                tempNotificaciones = it
                                com.example.chat.PreferenciasChat.notificacionesCanal = it
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MotoOrangePrimary)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF2A2F3E))

                    // Mostrar avatares
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mostrar avatares", fontSize = 14.sp)
                        }
                        Switch(
                            checked = tempAvatares,
                            onCheckedChange = {
                                tempAvatares = it
                                com.example.chat.PreferenciasChat.mostrarAvatares = it
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MotoOrangePrimary)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF2A2F3E))

                    // Limpiar chat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettingsDialog = false
                                showClearChatDialog = true
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Limpiar chat", fontSize = 14.sp, color = StatusError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Text("Listo")
                }
            }
        )
    }

    // ═══════════════════════════════════════════════
    // DIÁLOGOS DE GRUPOS PRIVADOS
    // ═══════════════════════════════════════════════
    if (showCreateGroupDialog) {
        CreatePrivateGroupDialog(
            allMembers = allMembers,
            currentMember = currentMember,
            myCreatedGroupsCount = myCreatedGroupsCount,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, memberIds ->
                onCreatePrivateGroup(name, desc, memberIds) { success, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showGroupMembersDialog && currentPrivateGroup != null) {
        GroupMembersDialog(
            group = currentPrivateGroup,
            allMembers = allMembers,
            currentMemberId = currentMember?.id,
            onDismiss = { showGroupMembersDialog = false },
            onRemoveMember = { memberId ->
                onRemoveMemberFromGroup(currentPrivateGroup.id, memberId)
            }
        )
    }

    if (showAddMembersDialog && currentPrivateGroup != null) {
        AddMembersDialog(
            group = currentPrivateGroup,
            allMembers = allMembers,
            onDismiss = { showAddMembersDialog = false },
            onAddMembers = { memberIds ->
                onAddMembersToGroup(currentPrivateGroup.id, memberIds)
            }
        )
    }

    if (showLeaveGroupDialog && currentPrivateGroup != null) {
        AlertDialog(
            onDismissRequest = { showLeaveGroupDialog = false },
            title = { Text("Salir del Grupo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas salir del grupo privado \"${currentPrivateGroup.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onLeaveGroup(currentPrivateGroup.id)
                        showLeaveGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Salir del Grupo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteGroupDialog && currentPrivateGroup != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Eliminar Grupo Privado", fontWeight = FontWeight.Bold, color = StatusError) },
            text = { Text("¿Deseas eliminar definitivamente el grupo \"${currentPrivateGroup.name}\"? Todos los mensajes y accesos se cerrarán para todos los miembros.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(currentPrivateGroup.id)
                        showDeleteGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Eliminar Grupo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// BURBUJA DE MENSAJE (estilo WhatsApp con citas/respuestas + vistos + long-press)
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isDirectivaMode: Boolean,
    canDeleteForAll: Boolean,
    onDelete: () -> Unit,
    onDeleteForMe: () -> Unit = {},
    onReply: (ChatMessage) -> Unit = {},
    onToggleReaction: (String) -> Unit = {},
    currentMemberId: Long? = null,
    audioPlayingUrl: String? = null,
    audioPlaybackProgress: Float = 0f,
    isAudioPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    val roleColor = Color(message.senderRole.badgeColorHex)
    var showContextMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isMe) {
                // Avatar del remitente
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    color = roleColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, roleColor.copy(alpha = 0.5f))
                ) {
                    if (!message.senderPhotoUrl.isNullOrBlank()) {
                        coil.compose.SubcomposeAsyncImage(
                            model = message.senderPhotoUrl,
                            contentDescription = "Foto de ${message.senderNickname}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            loading = { CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 2.dp) },
                            error = {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = message.senderInitials,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = roleColor
                                    )
                                }
                            }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = message.senderInitials,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = roleColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Burbuja del mensaje
            Column(
                modifier = Modifier.widthIn(max = 290.dp),
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                // Nombre del remitente
                if (!isMe) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = message.senderNickname,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = roleColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, roleColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = message.senderCustomRoleTitle ?: message.senderRole.displayName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = roleColor,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // La burbuja
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = when {
                        message.isRadioCallout -> Color(0xFF241C10)
                        isMe -> TxFlameRed.copy(alpha = 0.85f)
                        else -> Color(0xFF2A2F3E)
                    },
                    border = if (message.isRadioCallout) BorderStroke(1.dp, MotoGoldSecondary)
                    else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showContextMenu = true }
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        // Radio callout header
                        if (message.isRadioCallout) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = MotoGoldSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "RADIO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MotoGoldSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Cita / Mensaje respondido (estilo WhatsApp)
                        if (!message.replyToSenderName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(26.dp)
                                            .background(MotoOrangePrimary, RoundedCornerShape(1.5.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = message.replyToSenderName ?: "",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotoGoldSecondary
                                        )
                                        Text(
                                            text = message.replyToText ?: "",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Contenido del mensaje (Sticker, Audio o Texto)
                        if (message.messageType == com.example.data.model.MessageType.STICKER || message.isSticker) {
                            val stickerSource = message.stickerFilePath ?: message.stickerFileName
                            Box(
                                modifier = Modifier
                                    .size(125.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!stickerSource.isNullOrBlank()) {
                                            com.example.chat.GestorStickers.guardarStickerComoFavorito(context, stickerSource)
                                        }
                                    }
                            ) {
                                coil.compose.AsyncImage(
                                    model = stickerSource,
                                    contentDescription = "Sticker",
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Ícono sutil de estrella para guardar a favoritos
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Añadir a Favoritos",
                                        tint = MotoGoldSecondary,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }
                            }
                        } else if (message.messageType == com.example.data.model.MessageType.AUDIO || !message.audioUrl.isNullOrBlank()) {
                            // 🎙️ REPRODUCTOR DE NOTA DE VOZ
                            val audioUrl = message.audioUrl ?: ""
                            val isPlaying = audioPlayingUrl == audioUrl && !isAudioPaused
                            val progress = if (audioPlayingUrl == audioUrl) audioPlaybackProgress else 0f

                            Row(
                                modifier = Modifier
                                    .width(220.dp)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPlaying) TxFlameRed else MotoOrangePrimary,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable {
                                            if (audioUrl.isNotBlank()) {
                                                com.example.chat.GestorAudio.toggleReproducirAudio(audioUrl)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MotoGoldSecondary,
                                        trackColor = Color.White.copy(alpha = 0.2f),
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🎤 Audio", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                        val dur = message.audioDurationSeconds
                                        val min = dur / 60
                                        val sec = dur % 60
                                        Text(String.format("%02d:%02d", min, sec), fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = message.messageText,
                                fontSize = 14.sp,
                                color = Color.White,
                                lineHeight = 19.sp
                            )
                        }

                        // ═══════════════════════════════════════════════
                        // REACCIONES CON EMOJIS (Chips de conteo)
                        // ═══════════════════════════════════════════════
                        val reactionsMap = remember(message.reactions) { message.getReactionsMap() }
                        if (reactionsMap.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                reactionsMap.forEach { (emoji, userIds) ->
                                    val hasMyReaction = currentMemberId != null && userIds.contains(currentMemberId)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasMyReaction) MotoOrangePrimary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, if (hasMyReaction) MotoOrangePrimary else Color.White.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onToggleReaction(emoji)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(text = emoji, fontSize = 12.sp)
                                            Text(
                                                text = "${userIds.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasMyReaction) MotoOrangePrimary else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hora + Checks (vistos)
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.65f) else Color(0xFF78909C)
                            )
                            if (isMe) {
                                val readCount = message.readBy.size
                                Icon(
                                    when {
                                        readCount > 1 -> Icons.Default.DoneAll
                                        readCount == 1 -> Icons.Default.DoneAll
                                        else -> Icons.Default.Done
                                    },
                                    contentDescription = when {
                                        readCount > 1 -> "Visto"
                                        readCount == 1 -> "Entregado"
                                        else -> "Enviado"
                                    },
                                    tint = when {
                                        readCount > 1 -> Color(0xFF4FC3F7) // ✓✓ azul = visto
                                        readCount == 1 -> Color(0xFF90A4AE) // ✓✓ gris = entregado
                                        else -> Color.White.copy(alpha = 0.5f) // ✓ gris = enviado
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isMe) {
                Spacer(modifier = Modifier.width(8.dp))
                // Mi avatar
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    color = TxFlameRed.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    if (!message.senderPhotoUrl.isNullOrBlank()) {
                        coil.compose.SubcomposeAsyncImage(
                            model = message.senderPhotoUrl,
                            contentDescription = "Mi Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            loading = { CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 2.dp) },
                            error = {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = message.senderInitials,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = message.senderInitials,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════
        // MENÚ CONTEXTUAL (long-press con Emojis de Reacción)
        // ═══════════════════════════════════════════════
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            // Barra de Emojis de Reacción Rápida
            val quickEmojis = listOf("👍", "❤️", "😂", "🏍️", "🔥", "🙏", "😮", "😢")
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .background(Color(0xFF1E2433), RoundedCornerShape(18.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickEmojis.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A3142))
                            .clickable {
                                showContextMenu = false
                                onToggleReaction(emoji)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 16.sp)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.1f))

            DropdownMenuItem(
                text = { Text("Responder") },
                onClick = {
                    showContextMenu = false
                    onReply(message)
                },
                leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Copiar") },
                onClick = {
                    showContextMenu = false
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Mensaje", message.messageText)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Mensaje copiado", android.widget.Toast.LENGTH_SHORT).show()
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Eliminar para mí") },
                onClick = {
                    showContextMenu = false
                    onDeleteForMe()
                },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(18.dp)) }
            )
            if (canDeleteForAll) {
                DropdownMenuItem(
                    text = { Text("Eliminar para todos", color = StatusError) },
                    onClick = {
                        showContextMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp)) }
                )
            }
            DropdownMenuItem(
                text = { Text("Info del mensaje") },
                onClick = { showContextMenu = false },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: CREAR GRUPO PRIVADO (Con límite de 2 salas y selección de miembros)
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun CreatePrivateGroupDialog(
    allMembers: List<MemberProfile>,
    currentMember: MemberProfile?,
    myCreatedGroupsCount: Int,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, memberIds: List<Long>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var memberSearchQuery by remember { mutableStateOf("") }
    val selectedMemberIds = remember { mutableStateListOf<Long>() }
    val isLimitReached = myCreatedGroupsCount >= 2

    val availableMembers = remember(allMembers, currentMember, memberSearchQuery) {
        allMembers.filter { it.id != currentMember?.id }
            .filter {
                if (memberSearchQuery.isBlank()) true
                else it.fullName.contains(memberSearchQuery, ignoreCase = true) ||
                     it.nickname.contains(memberSearchQuery, ignoreCase = true) ||
                     it.memberNumber.contains(memberSearchQuery, ignoreCase = true)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = MotoOrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Grupo Privado", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info banner sobre límite de salas
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLimitReached) StatusError.copy(alpha = 0.15f) else Color(0xFF1E2538),
                    border = BorderStroke(1.dp, if (isLimitReached) StatusError else MotoOrangePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isLimitReached) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isLimitReached) StatusError else MotoGoldSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isLimitReached) "Has alcanzado el límite de 2 salas creadas por usuario. Elimina una sala anterior si deseas crear otra."
                                   else "Salas creadas por ti: $myCreatedGroupsCount de 2 permitidas.",
                            fontSize = 11.sp,
                            color = if (isLimitReached) Color(0xFFFFCDD2) else Color(0xFFCFD8DC),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isLimitReached) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { if (it.length <= 40) groupName = it },
                        label = { Text("Nombre de la Sala *") },
                        placeholder = { Text("Ej. Rodada Fin de Semana") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { if (it.length <= 100) groupDesc = it },
                        label = { Text("Descripción / Propósito (Opcional)") },
                        placeholder = { Text("Ej. Coordinación de ruta y paradas") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Text(
                        text = "Seleccionar Integrantes (${selectedMemberIds.size} seleccionados):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MotoGoldSecondary
                    )

                    OutlinedTextField(
                        value = memberSearchQuery,
                        onValueChange = { memberSearchQuery = it },
                        placeholder = { Text("Buscar piloto por nombre o apodo...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        color = Color(0xFF131722),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        if (availableMembers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No se encontraron miembros", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                availableMembers.forEach { mem ->
                                    val isSelected = selectedMemberIds.contains(mem.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) selectedMemberIds.remove(mem.id)
                                                else selectedMemberIds.add(mem.id)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(mem.role.badgeColorHex).copy(alpha = 0.2f),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(mem.avatarInitials, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(mem.role.badgeColorHex))
                                                }
                                            }
                                            Column {
                                                Text(mem.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${mem.nickname} • ${mem.role.displayName}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                            }
                                        }
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                if (it) selectedMemberIds.add(mem.id)
                                                else selectedMemberIds.remove(mem.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = MotoOrangePrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLimitReached) {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onCreate(groupName, groupDesc, selectedMemberIds.toList())
                            onDismiss()
                        }
                    },
                    enabled = groupName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Text("Crear Sala e Invitar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isLimitReached) "Cerrar" else "Cancelar")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: MIEMBROS DE SALA PRIVADA
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun GroupMembersDialog(
    group: PrivateGroup,
    allMembers: List<MemberProfile>,
    currentMemberId: Long?,
    onDismiss: () -> Unit,
    onRemoveMember: (Long) -> Unit
) {
    val membersInGroup = remember(group.memberIds, allMembers) {
        allMembers.filter { group.memberIds.contains(it.id) }
    }
    val isCreatorOrAdmin = group.creatorMemberId == currentMemberId || group.adminIds.contains(currentMemberId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = MotoOrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${group.memberIds.size} integrantes", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (group.description.isNotBlank()) {
                    Text(
                        text = "Propósito: ${group.description}",
                        fontSize = 12.sp,
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                membersInGroup.forEach { mem ->
                    val isMemberCreator = mem.id == group.creatorMemberId
                    Surface(
                        color = Color(0xFF1E2538),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(mem.role.badgeColorHex).copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(mem.avatarInitials, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(mem.role.badgeColorHex))
                                    }
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(mem.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (isMemberCreator) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = TxGoldSecondary.copy(alpha = 0.2f),
                                                border = BorderStroke(0.5.dp, TxGoldSecondary)
                                            ) {
                                                Text("CREADOR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TxGoldSecondary, modifier = Modifier.padding(horizontal = 3.dp))
                                            }
                                        }
                                    }
                                    Text("${mem.nickname} • ${mem.role.displayName}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                }
                            }

                            if (isCreatorOrAdmin && mem.id != currentMemberId && !isMemberCreator) {
                                IconButton(
                                    onClick = { onRemoveMember(mem.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.PersonRemove, contentDescription = "Expulsar", tint = StatusError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)) {
                Text("Listo")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: AGREGAR INTEGRANTES A SALA PRIVADA
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun AddMembersDialog(
    group: PrivateGroup,
    allMembers: List<MemberProfile>,
    onDismiss: () -> Unit,
    onAddMembers: (List<Long>) -> Unit
) {
    var memberSearchQuery by remember { mutableStateOf("") }
    val selectedMemberIds = remember { mutableStateListOf<Long>() }

    val availableMembers = remember(allMembers, group.memberIds, memberSearchQuery) {
        allMembers.filter { !group.memberIds.contains(it.id) }
            .filter {
                if (memberSearchQuery.isBlank()) true
                else it.fullName.contains(memberSearchQuery, ignoreCase = true) ||
                     it.nickname.contains(memberSearchQuery, ignoreCase = true) ||
                     it.memberNumber.contains(memberSearchQuery, ignoreCase = true)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MotoGoldSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Miembros", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Selecciona usuarios para invitar a \"${group.name}\":", fontSize = 12.sp, color = Color(0xFFCFD8DC))

                OutlinedTextField(
                    value = memberSearchQuery,
                    onValueChange = { memberSearchQuery = it },
                    placeholder = { Text("Buscar piloto por nombre o apodo...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    color = Color(0xFF131722),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    if (availableMembers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No hay otros miembros disponibles para agregar", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            availableMembers.forEach { mem ->
                                val isSelected = selectedMemberIds.contains(mem.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedMemberIds.remove(mem.id)
                                            else selectedMemberIds.add(mem.id)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(mem.role.badgeColorHex).copy(alpha = 0.2f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(mem.avatarInitials, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(mem.role.badgeColorHex))
                                            }
                                        }
                                        Column {
                                            Text(mem.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${mem.nickname} • ${mem.role.displayName}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                        }
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (it) selectedMemberIds.add(mem.id)
                                            else selectedMemberIds.remove(mem.id)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = MotoOrangePrimary)
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
                onClick = {
                    if (selectedMemberIds.isNotEmpty()) {
                        onAddMembers(selectedMemberIds.toList())
                        onDismiss()
                    }
                },
                enabled = selectedMemberIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Agregar (${selectedMemberIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
