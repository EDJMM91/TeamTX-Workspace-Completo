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
    onSendAudio: (channelId: String, audioFile: java.io.File, durationSeconds: Int, transcriptionText: String?) -> Unit = { _, _, _, _ -> },
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

    // Estados de Chats Privados 1 a 1 (DMs)
    var showPrivateChatsDialog by remember { mutableStateOf(false) }
    var showNewDirectChatDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Estados de Grabación y Reproducción de Audio
    val estaGrabandoAudio by com.example.chat.GestorAudio.estaGrabando.collectAsState()
    val segundosGrabados by com.example.chat.GestorAudio.segundosGrabados.collectAsState()
    val audioPlayingUrl by com.example.chat.GestorAudio.audioEnReproduccionUrl.collectAsState()
    val audioPlaybackProgress by com.example.chat.GestorAudio.progresoReproduccion.collectAsState()
    val isAudioPaused by com.example.chat.GestorAudio.estaPausado.collectAsState()

    // Estados de Transcripción de Voz a Texto
    val estaDictandoVoz by com.example.chat.TranscriptorVoz.estaEscuchando.collectAsState()
    val textoDictadoParcial by com.example.chat.TranscriptorVoz.textoParcial.collectAsState()

    // Permiso de micrófono para notas de voz
    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.chat.GestorAudio.iniciarGrabacion(context)
            if (com.example.chat.PreferenciasChat.transcribirAudiosAuto) {
                com.example.chat.TranscriptorVoz.iniciarEscucha(context)
            }
        } else {
            android.widget.Toast.makeText(context, "Se requiere permiso de micrófono para notas de voz", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Permiso de micrófono para transcriptor de voz a texto
    val dictadoPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.chat.TranscriptorVoz.iniciarEscucha(
                contexto = context,
                alObtenerTexto = { textoFinal ->
                    messageInput = if (messageInput.isBlank()) textoFinal else "$messageInput $textoFinal"
                },
                alRecibirError = { err ->
                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            android.widget.Toast.makeText(context, "Se requiere permiso de micrófono para dictado de voz a texto", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val currentPrivateGroup = remember(privateGroups, activeChannelId) {
        privateGroups.find { it.id == activeChannelId }
    }
    val isPrivateGroupActive = currentPrivateGroup != null || activeChannelId.startsWith("GRP_")
    val isGroupBlocked = currentPrivateGroup?.isBlockedByDirectiva == true

    // Detección de Chat Privado 1 a 1 (Direct Message)
    val isDirectChatActive = activeChannelId.startsWith("DM_")
    val otherDirectMember = remember(activeChannelId, allMembers, currentMember) {
        if (isDirectChatActive) {
            val parts = activeChannelId.removePrefix("DM_").split("_")
            val myId = currentMember?.id ?: 0L
            val otherId = parts.mapNotNull { it.toLongOrNull() }.find { it != myId } ?: parts.firstOrNull()?.toLongOrNull()
            allMembers.find { it.id == otherId }
        } else null
    }

    val myMemberIdStr = currentMember?.id?.toString() ?: ""
    val filteredMessages = remember(messages, activeChannelId, myMemberIdStr) {
        messages.filter {
            it.channelId == activeChannelId &&
            (myMemberIdStr.isBlank() || !it.deletedForMemberIds.split(",").contains(myMemberIdStr))
        }
    }

    val activeChannel = remember(activeChannelId, isPrivateGroupActive, isDirectChatActive) {
        if (isPrivateGroupActive || isDirectChatActive) null
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
            listState.scrollToItem(filteredMessages.size - 1)
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
            // ═══════════════════════════════════════════════
            // HEADER: TopAppBar WhatsApp Red + Selector de Canales
            // ═══════════════════════════════════════════════
            Surface(
                color = Color(0xFF8C1414),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Barra superior sólida WhatsApp Red
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Avatar circular del chat
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            border = BorderStroke(
                                1.dp,
                                if (isDirectChatActive && otherDirectMember != null) Color(otherDirectMember.role.badgeColorHex)
                                else Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDirectChatActive && otherDirectMember != null) {
                                    if (!otherDirectMember.profilePhotoUri.isNullOrBlank()) {
                                        coil.compose.AsyncImage(
                                            model = otherDirectMember.profilePhotoUri,
                                            contentDescription = otherDirectMember.fullName,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = if (otherDirectMember.nickname.isNotBlank()) otherDirectMember.nickname.take(2).uppercase() else otherDirectMember.avatarInitials,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Icon(
                                        if (isPrivateGroupActive) Icons.Default.Groups
                                        else activeChannel?.icon ?: Icons.Default.ChatBubble,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Título y subtítulo de estado
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDirectChatActive && otherDirectMember != null) {
                                    otherDirectMember.fullName
                                } else if (isPrivateGroupActive && currentPrivateGroup != null) {
                                    currentPrivateGroup.name
                                } else {
                                    activeChannel?.title ?: "Chat TX"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isDirectChatActive && otherDirectMember != null) {
                                    val alias = if (otherDirectMember.nickname.isNotBlank()) "\"${otherDirectMember.nickname}\" • " else ""
                                    val online = if (otherDirectMember.isOnline) "🟢 en línea" else "últ. vez reciente"
                                    "$alias${otherDirectMember.role.displayName} • $online"
                                } else if (isPrivateGroupActive && currentPrivateGroup != null) {
                                    if (currentPrivateGroup.isBlockedByDirectiva) "🔒 Bloqueado por Directiva"
                                    else "${currentPrivateGroup.memberIds.size} miembros • Grupo Privado"
                                } else if (activeChannelId == "DIRECTIVA") {
                                    "Consejo Directivo • Privado"
                                } else {
                                    "en línea • Comunidad TX"
                                },
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Botón de Contactos / Chats Privados
                        IconButton(
                            onClick = { showPrivateChatsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = "Chats Privados",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Botones de acción a la derecha
                        if (isPrivateGroupActive && currentPrivateGroup != null) {
                            IconButton(
                                onClick = { showGroupMembersDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = "Miembros",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Menú de opciones ⋮
                        Box {
                            IconButton(
                                onClick = { showSettingsMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

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
                }
            }

            // Sub-barra: Selector de Canales & Grupos
            if (!showOnlyDirectiva) {
                Surface(
                    color = Color(0xFF161B26),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val channelsToShow = ChatChannel.values().filter { !it.directivaOnly }
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
                                    selectedContainerColor = TxFlameRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        // Botón de acceso a Bandeja de Chats Privados Directos (1 a 1)
                        item {
                            val totalUnreadDMs = remember(messages, currentMember) {
                                val memberId = currentMember?.id ?: 0L
                                if (memberId <= 0L) 0
                                else messages.count { it.channelId.startsWith("DM_") && it.senderMemberId != memberId && !it.readBy.contains(memberId) }
                            }

                            AssistChip(
                                onClick = { showPrivateChatsDialog = true },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = "Chats Privados",
                                        tint = if (isDirectChatActive) Color.White else Color(0xFF38BDF8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            if (isDirectChatActive && otherDirectMember != null) "💬 ${otherDirectMember.fullName}" else "💬 Chats Privados",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDirectChatActive) Color.White else Color(0xFF38BDF8),
                                            maxLines = 1
                                        )
                                        if (totalUnreadDMs > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = StatusError,
                                                modifier = Modifier.defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) {
                                                    Text(
                                                        text = if (totalUnreadDMs > 99) "99+" else "$totalUnreadDMs",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isDirectChatActive) TxFlameRed else Color(0xFF142236)
                                ),
                                border = BorderStroke(1.dp, if (isDirectChatActive) TxFlameRed else Color(0xFF38BDF8).copy(alpha = 0.6f))
                            )
                        }

                        // Botón Crear Grupo Privado
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

                        // Chips de grupos privados
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
                            allMembers = allMembers,
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            text = "Grabando audio...",
                                            color = Color(0xFF90A4AE),
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Botón Cancelar Grabación
                                    IconButton(
                                        onClick = {
                                            com.example.chat.GestorAudio.detenerGrabacion(descartar = true)
                                            com.example.chat.TranscriptorVoz.cancelarEscucha()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Cancelar", tint = StatusError, modifier = Modifier.size(20.dp))
                                    }
                                }

                                // Botón Enviar Audio (FAB Circular Rojo Sólido)
                                Surface(
                                    shape = CircleShape,
                                    color = TxFlameRed,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            val textoTranscrito = if (com.example.chat.PreferenciasChat.transcribirAudiosAuto) {
                                                com.example.chat.TranscriptorVoz.textoTranscrito.value.ifBlank { null }
                                            } else null
                                            com.example.chat.TranscriptorVoz.detenerEscucha()
                                            val resultado = com.example.chat.GestorAudio.detenerGrabacion(descartar = false)
                                            if (resultado != null) {
                                                val (archivoAudio, duracionSeg) = resultado
                                                onSendAudio(activeChannelId, archivoAudio, duracionSeg, textoTranscrito)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Send, contentDescription = "Enviar Audio", tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                            } else {
                                // 💊 Campo de texto en píldora (85% ancho)
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0xFF242938),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Botón Emojis/Stickers
                                        IconButton(
                                            onClick = { showStickerBox = !showStickerBox },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                if (showStickerBox) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                                                contentDescription = "Stickers",
                                                tint = if (showStickerBox) MotoOrangePrimary else Color(0xFF94A3B8),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Campo de texto
                                        OutlinedTextField(
                                            value = messageInput,
                                            onValueChange = { messageInput = it },
                                            placeholder = {
                                                Text(
                                                    text = if (estaDictandoVoz) {
                                                        if (textoDictadoParcial.isNotBlank()) "🎙️ $textoDictadoParcial" else "🎙️ Escuchando... habla ahora"
                                                    } else {
                                                        "Mensaje"
                                                    },
                                                    fontSize = 14.sp,
                                                    color = if (estaDictandoVoz) TxFlameRed else Color(0xFF94A3B8)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("input_chat_message"),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                cursorColor = TxFlameRed
                                            ),
                                            maxLines = 5,
                                            textStyle = LocalTextStyle.current.copy(
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        )

                                        // 🎙️ Botón de Dictado de Voz a Texto (SpeechRecognizer)
                                        IconButton(
                                            onClick = {
                                                if (estaDictandoVoz) {
                                                    com.example.chat.TranscriptorVoz.detenerEscucha()
                                                } else {
                                                    val permCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                                        context,
                                                        android.Manifest.permission.RECORD_AUDIO
                                                    )
                                                    if (permCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                        com.example.chat.TranscriptorVoz.iniciarEscucha(
                                                            contexto = context,
                                                            alObtenerTexto = { textoFinal ->
                                                                messageInput = if (messageInput.isBlank()) textoFinal else "$messageInput $textoFinal"
                                                            },
                                                            alRecibirError = { err ->
                                                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    } else {
                                                        dictadoPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (estaDictandoVoz) Icons.Default.GraphicEq else Icons.Default.KeyboardVoice,
                                                contentDescription = "Dictar por voz",
                                                tint = if (estaDictandoVoz) TxFlameRed else Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Botón Adjuntar Stickers
                                        IconButton(
                                            onClick = { showStickerBox = true },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = "Adjuntar",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // 🔴 FAB Circular Rojo Sólido (15% ancho / 48dp)
                                Surface(
                                    shape = CircleShape,
                                    color = TxFlameRed,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(48.dp)
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
                                                val permCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context,
                                                    android.Manifest.permission.RECORD_AUDIO
                                                )
                                                if (permCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    com.example.chat.GestorAudio.iniciarGrabacion(context)
                                                    if (com.example.chat.PreferenciasChat.transcribirAudiosAuto) {
                                                        com.example.chat.TranscriptorVoz.iniciarEscucha(context)
                                                    }
                                                } else {
                                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (messageInput.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                                            contentDescription = if (messageInput.isNotBlank()) "Enviar" else "Micrófono",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ═══════════════════════════════════════════════
                        // STICKER BOX CON PESTAÑAS (FAVORITOS, WHATSAPP, IMPORTADOS)
                        // ═══════════════════════════════════════════════
                        AnimatedVisibility(
                            visible = showStickerBox,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val stickersTodos by com.example.chat.GestorStickers.stickersGuardados.collectAsState()
                            val stickersFavs by com.example.chat.GestorStickers.stickersFavoritos.collectAsState()
                            var selectedStickerTab by remember { mutableStateOf(0) }

                            LaunchedEffect(Unit) {
                                com.example.chat.GestorStickers.cargarStickersGuardados(context)
                                com.example.chat.GestorStickers.cargarFavoritos(context)
                            }

                            val stickersWhatsApp = remember(stickersTodos) {
                                stickersTodos.filter { it.name.startsWith("wa_") }
                            }

                            val activeStickersList = when (selectedStickerTab) {
                                1 -> stickersFavs
                                2 -> stickersWhatsApp
                                3 -> stickersTodos
                                else -> emptyList()
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(290.dp)
                                    .background(Color(0xFF141923))
                                    .padding(8.dp)
                            ) {
                                // Pestañas de Emojis y Stickers
                                TabRow(
                                    selectedTabIndex = selectedStickerTab,
                                    containerColor = Color(0xFF1B2130),
                                    contentColor = Color.White,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                ) {
                                    Tab(
                                        selected = selectedStickerTab == 0,
                                        onClick = { selectedStickerTab = 0 },
                                        text = { Text("😀 Emojis", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = selectedStickerTab == 1,
                                        onClick = { selectedStickerTab = 1 },
                                        text = { Text("⭐ Favs (${stickersFavs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = selectedStickerTab == 2,
                                        onClick = { selectedStickerTab = 2 },
                                        text = { Text("📱 WA (${stickersWhatsApp.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = selectedStickerTab == 3,
                                        onClick = { selectedStickerTab = 3 },
                                        text = { Text("📁 Stickers (${stickersTodos.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (selectedStickerTab == 0) {
                                    // 🌟 Pestaña 0: Emojis Universales
                                    UniversalEmojiGrid(
                                        onEmojiClick = { emoji ->
                                            messageInput += emoji
                                        },
                                        onBackspace = {
                                            if (messageInput.isNotEmpty()) {
                                                messageInput = messageInput.dropLast(1)
                                            }
                                        }
                                    )
                                } else {
                                    // Fila de acciones (Escanear WhatsApp, Elegir Carpeta, Importar Archivos)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                android.widget.Toast.makeText(context, "Buscando stickers de WhatsApp...", android.widget.Toast.LENGTH_SHORT).show()
                                                com.example.chat.GestorStickers.escanearStickersWhatsApp(context) { count: Int ->
                                                    val msg = if (count > 0) "✅ Se sincronizaron $count stickers de WhatsApp" else "No se detectaron stickers nuevos en WhatsApp"
                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Escanear WA", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

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
                                            Text("Carpeta", fontSize = 10.sp, color = MotoGoldSecondary)
                                        }

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
                                            Text("Archivos", fontSize = 10.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (activeStickersList.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                if (selectedStickerTab == 1) "No tienes stickers favoritos.\nToca la estrella ⭐ en cualquier sticker para guardarlo."
                                                else if (selectedStickerTab == 2) "No hay stickers de WhatsApp detectados.\nPresiona 'Escanear WA' para buscar."
                                                else "No hay stickers guardados.\nImporta desde tus archivos.",
                                                color = Color.Gray,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                fontSize = 11.sp
                                            )
                                        }
                                    } else {
                                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(70.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(activeStickersList.size) { index ->
                                                val file = activeStickersList[index]
                                                val isFav = stickersFavs.any { it.name == file.name }
                                                Box(
                                                    modifier = Modifier
                                                        .size(70.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF1E2430))
                                                        .combinedClickable(
                                                            onClick = {
                                                                onSendSticker(activeChannelId, file)
                                                                showStickerBox = false
                                                            },
                                                            onLongClick = {
                                                                if (isFav) {
                                                                    com.example.chat.GestorStickers.eliminarFavorito(context, file.name)
                                                                    android.widget.Toast.makeText(context, "Eliminado de favoritos", android.widget.Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    com.example.chat.GestorStickers.guardarStickerComoFavorito(context, file.absolutePath)
                                                                    android.widget.Toast.makeText(context, "⭐ Guardado en Favoritos", android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        )
                                                ) {
                                                    coil.compose.AsyncImage(
                                                        model = file,
                                                        contentDescription = "Sticker",
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (isFav) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color.Black.copy(alpha = 0.6f),
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(2.dp)
                                                                .size(16.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Star,
                                                                contentDescription = null,
                                                                tint = MotoGoldSecondary,
                                                                modifier = Modifier.padding(2.dp)
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
        var tempTranscribirAudios by remember { mutableStateOf(com.example.chat.PreferenciasChat.transcribirAudiosAuto) }

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

                    // Transcripción automática de audios (Estilo WhatsApp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Transcribir audios a texto", fontSize = 14.sp)
                                Text("Muestra el texto debajo de las notas de voz (tipo WhatsApp)", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = tempTranscribirAudios,
                            onCheckedChange = {
                                tempTranscribirAudios = it
                                com.example.chat.PreferenciasChat.transcribirAudiosAuto = it
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

    // ═══════════════════════════════════════════════
    // DIÁLOGOS DE CHATS PRIVADOS 1 A 1
    // ═══════════════════════════════════════════════
    if (showPrivateChatsDialog) {
        PrivateChatsManagerDialog(
            allMessages = messages,
            allMembers = allMembers,
            currentMember = currentMember,
            activeChannelId = activeChannelId,
            onSelectDirectChat = { otherMemberId ->
                val myId = currentMember?.id ?: 0L
                val dmChannel = "DM_${minOf(myId, otherMemberId)}_${maxOf(myId, otherMemberId)}"
                android.util.Log.d("TEAM_TX_CHAT", "💬 Abriendo chat privado: $dmChannel")
                onSelectChannel(dmChannel)
            },
            onOpenNewChat = {
                showNewDirectChatDialog = true
            },
            onDismiss = { showPrivateChatsDialog = false }
        )
    }

    if (showNewDirectChatDialog) {
        NewDirectChatSelectMemberDialog(
            allMembers = allMembers,
            currentMember = currentMember,
            onSelectMember = { member ->
                val myId = currentMember?.id ?: 0L
                val dmChannel = "DM_${minOf(myId, member.id)}_${maxOf(myId, member.id)}"
                android.util.Log.d("TEAM_TX_CHAT", "💬 Iniciando nuevo chat privado con ${member.fullName}: $dmChannel")
                onSelectChannel(dmChannel)
                showPrivateChatsDialog = false
                showNewDirectChatDialog = false
            },
            onDismiss = { showNewDirectChatDialog = false }
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
    allMembers: List<MemberProfile> = emptyList(),
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
    var showInfoDialog by remember { mutableStateOf(false) }
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
                modifier = Modifier.widthIn(max = 295.dp),
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                // Nombre del remitente (Gris minimalista / Blanco según tema, eliminando amarillo chillón)
                if (!isMe) {
                    val nameColor = if (isSystemInDarkTheme()) Color(0xFFCBD5E1) else Color(0xFF475569)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = message.senderNickname,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = nameColor
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

                if (message.messageType == com.example.data.model.MessageType.STICKER || message.isSticker) {
                    // 🌟 STICKER FLOTANTE TIPO PNG (SIN RECUADRO DE FONDO)
                    val stickerSource = message.stickerFilePath ?: message.stickerFileName
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .combinedClickable(
                                onClick = {
                                    if (!stickerSource.isNullOrBlank()) {
                                        com.example.chat.GestorStickers.guardarStickerComoFavorito(context, stickerSource)
                                        android.widget.Toast.makeText(context, "⭐ Guardado en Favoritos", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onLongClick = { showContextMenu = true }
                            )
                    ) {
                        coil.compose.SubcomposeAsyncImage(
                            model = stickerSource,
                            contentDescription = "Sticker",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            loading = {
                                Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF8C1414))
                                }
                            }
                        )

                        // Píldora sutil flotante en la esquina inferior con hora y estado de entrega
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = dateStr,
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                                if (isMe) {
                                    if (message.isPending) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = "Pendiente",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    } else {
                                        val readCount = message.readBy.size
                                        Icon(
                                            when {
                                                readCount > 1 -> Icons.Default.DoneAll
                                                readCount == 1 -> Icons.Default.DoneAll
                                                else -> Icons.Default.Done
                                            },
                                            contentDescription = null,
                                            tint = if (readCount > 1) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // La burbuja con forma Tail (Pico inferior)
                    val bubbleShape = if (isMe) {
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
                    } else {
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
                    }

                    val bubbleBgColor = when {
                        message.isRadioCallout -> Color(0xFF241C10)
                        isMe -> if (isSystemInDarkTheme()) Color(0xFF381419) else Color(0xFFFFEBEE)
                        else -> if (isSystemInDarkTheme()) Color(0xFF1E2430) else Color(0xFFFFFFFF)
                    }

                    val bubbleBorder = when {
                        message.isRadioCallout -> BorderStroke(1.dp, MotoGoldSecondary)
                        isMe -> BorderStroke(0.5.dp, Color(0xFF8C1414).copy(alpha = if (isSystemInDarkTheme()) 0.4f else 0.25f))
                        else -> BorderStroke(0.5.dp, if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                    }

                    val mainTextColor = when {
                        message.isRadioCallout -> Color.White
                        isMe -> if (isSystemInDarkTheme()) Color.White else Color(0xFF1E293B)
                        else -> if (isSystemInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                    }

                    Surface(
                        shape = bubbleShape,
                        color = bubbleBgColor,
                        border = bubbleBorder,
                        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 1.dp,
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
                                    color = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.35f) else Color(0xFFF1F5F9),
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
                                                .background(Color(0xFF8C1414), RoundedCornerShape(1.5.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = message.replyToSenderName ?: "",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8C1414)
                                            )
                                            Text(
                                                text = message.replyToText ?: "",
                                                fontSize = 9.sp,
                                                color = mainTextColor.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Contenido del mensaje (Audio o Texto)
                            if (message.messageType == com.example.data.model.MessageType.AUDIO || !message.audioUrl.isNullOrBlank()) {
                                // 🎙️ REPRODUCTOR DE NOTA DE VOZ FLOTANTE Y ALINEADO VERTICALMENTE
                                val audioUrl = message.audioUrl ?: ""
                                val isPlaying = audioPlayingUrl == audioUrl && !isAudioPaused
                                val progress = if (audioPlayingUrl == audioUrl) audioPlaybackProgress else 0f

                                Row(
                                    modifier = Modifier
                                        .width(235.dp)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF8C1414),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier
                                            .size(38.dp)
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
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = Color(0xFF8C1414),
                                            trackColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1),
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val dur = message.audioDurationSeconds
                                            val min = dur / 60
                                            val sec = dur % 60
                                            Text(
                                                text = String.format("%02d:%02d", min, sec),
                                                fontSize = 11.sp,
                                                color = mainTextColor.copy(alpha = 0.75f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = Color(0xFF8C1414),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                // 📝 TRANSCRIPCIÓN DE AUDIO (Estilo WhatsApp)
                                val mostrarTranscripcion = com.example.chat.PreferenciasChat.transcribirAudiosAuto
                                val tieneTextoValido = message.messageText.isNotBlank() && !message.messageText.startsWith("🎤 Nota de voz")
                                if (mostrarTranscripcion && tieneTextoValido) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
                                        border = BorderStroke(0.5.dp, if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.widthIn(min = 200.dp, max = 260.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Subtitles,
                                                contentDescription = "Transcripción",
                                                tint = Color(0xFF8C1414),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .padding(top = 2.dp)
                                            )
                                            Text(
                                                text = message.messageText,
                                                fontSize = 12.sp,
                                                color = mainTextColor.copy(alpha = 0.95f),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = message.messageText,
                                    fontSize = 14.sp,
                                    color = mainTextColor,
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
                                            color = if (hasMyReaction) Color(0xFF8C1414).copy(alpha = 0.25f) else if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                                            border = BorderStroke(1.dp, if (hasMyReaction) Color(0xFF8C1414) else Color.White.copy(alpha = 0.15f)),
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
                                                    color = if (hasMyReaction) Color(0xFF8C1414) else mainTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Hora + Estado de sincronización (Reloj si PENDING, Ticks si SENT)
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = dateStr,
                                    fontSize = 10.sp,
                                    color = mainTextColor.copy(alpha = 0.65f)
                                )
                                if (isMe) {
                                    if (message.isPending) {
                                        // 🕒 Ícono de reloj (En cola / Offline)
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = "Pendiente (En cola local)",
                                            tint = mainTextColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
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
                                                else -> mainTextColor.copy(alpha = 0.5f) // ✓ gris = enviado
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
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
                onClick = {
                    showContextMenu = false
                    showInfoDialog = true
                },
                leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(18.dp)) }
            )
        }

        if (showInfoDialog) {
            MessageInfoDialog(
                message = message,
                allMembers = allMembers,
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: INFO DEL MENSAJE (Visto por / Integrantes que leyeron)
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun MessageInfoDialog(
    message: ChatMessage,
    allMembers: List<MemberProfile>,
    onDismiss: () -> Unit
) {
    val dateFull = remember(message.timestamp) {
        SimpleDateFormat("dd/MM/yyyy • hh:mm:ss a", Locale.getDefault()).format(Date(message.timestamp))
    }

    val readersList = remember(message.readBy, allMembers) {
        message.readBy.mapNotNull { readId ->
            allMembers.find { it.id == readId }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(24.dp))
                Column {
                    Text("Info del Mensaje", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(dateFull, fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Vista previa del mensaje
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${message.senderName} (${message.senderNickname})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(message.senderRole.badgeColorHex)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message.messageText.ifBlank { if (message.isSticker) "🖼️ Sticker" else "🎙️ Nota de voz (${message.audioDurationSeconds}s)" },
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF2A3142))

                // Encabezado de Leídos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(16.dp))
                        Text("Leído por", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4FC3F7))
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4FC3F7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF4FC3F7))
                    ) {
                        Text(
                            text = "${message.readBy.size} integrantes",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4FC3F7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Lista de miembros que leyeron
                if (readersList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no ha sido leído por otros integrantes.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(readersList) { reader ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1A1F2C),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                            color = Color(reader.role.badgeColorHex).copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, Color(reader.role.badgeColorHex).copy(alpha = 0.6f)),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = reader.avatarInitials,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(reader.role.badgeColorHex)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = reader.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${reader.nickname} • ${reader.memberNumber} • ${reader.role.displayName}",
                                                fontSize = 10.sp,
                                                color = Color(reader.role.badgeColorHex),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = "Leído",
                                        tint = Color(0xFF4FC3F7),
                                        modifier = Modifier.size(16.dp)
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Listo")
            }
        }
    )
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

// ═══════════════════════════════════════════════════════════════
// EMOJI GRID UNIVERSAL CATEGORIZADO
// ═══════════════════════════════════════════════════════════════
@Composable
fun UniversalEmojiGrid(
    onEmojiClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(0) }
    val categories = remember {
        listOf(
            "😀 Caritas" to listOf(
                "😀","😃","😄","😁","😆","😅","😂","🤣","🥲","🥹","☺️","😊","😇","🙂","🙃","😉","😌","😍","🥰","😘","😗","😙","😚",
                "😋","😛","😝","😜","🤪","🤨","🧐","🤓","😎","🥸","🤩","🥳","😏","😒","😞","😔","😟","😕","🙁","☹️","😣","😖",
                "😫","😩","🥺","😢","😭","😮‍💨","😤","😠","😡","🤬","🤯","😳","🥵","🥶","😱","😨","😰","😥","😓","🤗","🤔","🫣",
                "🤭","🫢","🫡","🤫","🫠","🤥","😶","🫥","😐","🫤","😑","🫨","😬","🙄","😯","😦","😧","😮","😲","🥱","😴","🤤",
                "😪","😵","😵‍💫","🤐","🥴","🤢","🤮","🤧","😷","🤒","🤕","🤑","🤠","😈","👿","👹","👺","🤡","💩","👻","💀","☠️","👽","👾","🤖","🎃"
            ),
            "👍 Gestos" to listOf(
                "👍","👎","👊","✊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍️","💅","🤳","💪","🦾","🦿","🦵","🦶","👂","🦻","👃",
                "🧠","🫀","🫁","🦷","🦴","👀","👁️","👅","👄","🫦","💋","🫰","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","🖐️","✋","🖖","👋"
            ),
            "🏍️ Motos & Motor" to listOf(
                "🏍️","🛵","🏎️","🚗","🚘","🚙","🚚","🚛","🚜","🚲","🛴","🚨","🚔","🛣️","⛽","🏁","🏆","🥇","🥈","🥉","🎖️","🎫","🗺️",
                "🧭","🏔️","🏕️","⛺","🔧","🔨","⚙️","🔩","🧰","🛡️","🚦","🚧","🛑","⚓","✈️","🚀"
            ),
            "❤️ Pasión & Fuego" to listOf(
                "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❤️‍🔥","❤️‍🩹","❣️","💕","💞","💓","💗","💖","💘","💝","🔥","💥","✨","🌟","💫","⚡","☄️","🧨","🎉","🎊","💯"
            ),
            "🇻🇪 Banderas" to listOf(
                "🇻🇪","🇨🇴","🇧🇷","🇦🇷","🇪🇸","🇲🇽","🇺🇸","🏁","🚩","🏴","🏳️"
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Selector horizontal de categoría + Botón de retroceso
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories.size) { idx ->
                    val isCatSelected = selectedCategory == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCatSelected) Color(0xFF8C1414) else Color(0xFF222838),
                        border = BorderStroke(0.5.dp, if (isCatSelected) Color(0xFFE53935) else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.clickable { selectedCategory = idx }
                    ) {
                        Text(
                            text = categories[idx].first,
                            fontSize = 11.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onBackspace,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Backspace,
                    contentDescription = "Borrar",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val currentEmojis = categories[selectedCategory].second
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(38.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentEmojis.size) { i ->
                val emoji = currentEmojis[i]
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// BANDEJA DE CHATS PRIVADOS DIRECTOS (1 A 1)
// ═══════════════════════════════════════════════════════════════
@Composable
fun PrivateChatsManagerDialog(
    allMessages: List<ChatMessage>,
    allMembers: List<MemberProfile>,
    currentMember: MemberProfile?,
    activeChannelId: String,
    onSelectDirectChat: (otherMemberId: Long) -> Unit,
    onOpenNewChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val myId = currentMember?.id ?: 0L
    var searchQuery by remember { mutableStateOf("") }

    val activeDMs = remember(allMessages, allMembers, myId, searchQuery) {
        val dmChannels = allMessages.filter { it.channelId.startsWith("DM_") }
        val memberMap = allMembers.associateBy { it.id }

        val grouped = dmChannels.groupBy { msg ->
            val parts = msg.channelId.removePrefix("DM_").split("_")
            parts.mapNotNull { it.toLongOrNull() }.find { it != myId } ?: parts.firstOrNull()?.toLongOrNull() ?: 0L
        }.filterKeys { it > 0L && it != myId }

        grouped.mapNotNull { (otherId, msgs) ->
            val other = memberMap[otherId]
            if (other != null) {
                val lastMsg = msgs.maxByOrNull { it.timestamp }
                val unreadCount = msgs.count { it.senderMemberId != myId && !it.readBy.contains(myId) }
                Triple(other, lastMsg, unreadCount)
            } else null
        }.filter { (other, lastMsg, _) ->
            searchQuery.isBlank() ||
            other.fullName.contains(searchQuery, ignoreCase = true) ||
            other.nickname.contains(searchQuery, ignoreCase = true) ||
            other.bikePlate.contains(searchQuery, ignoreCase = true) ||
            (lastMsg?.messageText?.contains(searchQuery, ignoreCase = true) == true)
        }.sortedByDescending { it.second?.timestamp ?: 0L }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B26),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Forum, contentDescription = null, tint = MotoOrangePrimary)
                    Text("CHATS PRIVADOS (1 A 1)", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Buscador
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar conversación o miembro...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF222838),
                        focusedContainerColor = Color(0xFF222838)
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (activeDMs.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No tienes conversaciones privadas activas", color = Color.Gray, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Toca el botón '+' abajo para iniciar un chat privado con cualquier miembro del club.", color = Color(0xFF64748B), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeDMs) { (otherMember, lastMsg, unreadCount) ->
                            val expectedChannelId = "DM_${minOf(myId, otherMember.id)}_${maxOf(myId, otherMember.id)}"
                            val isCurrentlyOpen = expectedChannelId == activeChannelId

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentlyOpen) TxFlameRed.copy(alpha = 0.2f) else Color(0xFF1E2434),
                                border = BorderStroke(1.dp, if (isCurrentlyOpen) TxFlameRed else Color(0xFF2E374D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectDirectChat(otherMember.id)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF262626))
                                            .border(1.5.dp, Color(otherMember.role.badgeColorHex), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!otherMember.profilePhotoUri.isNullOrBlank()) {
                                            coil.compose.AsyncImage(
                                                model = otherMember.profilePhotoUri,
                                                contentDescription = otherMember.fullName,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = if (otherMember.nickname.isNotBlank()) otherMember.nickname.take(2).uppercase() else otherMember.avatarInitials,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = otherMember.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (otherMember.isOnline) {
                                                Box(modifier = Modifier.size(7.dp).background(Color(0xFF22C55E), CircleShape))
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = otherMember.role.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(otherMember.role.badgeColorHex)
                                            )
                                            Text(text = "•", fontSize = 9.sp, color = Color.Gray)
                                            Text(
                                                text = if (otherMember.nickname.isNotBlank()) "\"${otherMember.nickname}\"" else otherMember.bikePlate,
                                                fontSize = 9.sp,
                                                color = TxSteelSilver,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Último mensaje
                                        val snippet = when {
                                            lastMsg == null -> "Sin mensajes aún"
                                            lastMsg.audioUrl != null || lastMsg.audioDurationSeconds > 0 -> "🎤 Nota de voz (${lastMsg.audioDurationSeconds}s)"
                                            lastMsg.stickerFileName != null -> "🏷️ Sticker"
                                            lastMsg.messageText.isNotBlank() -> lastMsg.messageText
                                            else -> "Mensaje"
                                        }

                                        Text(
                                            text = snippet,
                                            fontSize = 11.sp,
                                            color = if (unreadCount > 0) Color.White else Color(0xFF94A3B8),
                                            fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Hora y Badge no leído
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (lastMsg != null) {
                                            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastMsg.timestamp))
                                            Text(text = timeStr, fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (unreadCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = StatusError,
                                                modifier = Modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) {
                                                    Text(text = "$unreadCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenNewChat()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Iniciar Nuevo Chat con Miembro", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {}
    )
}

// ═══════════════════════════════════════════════════════════════
// DIÁLOGO: SELECCIONAR MIEMBRO PARA NUEVO CHAT PRIVADO
// ═══════════════════════════════════════════════════════════════
@Composable
fun NewDirectChatSelectMemberDialog(
    allMembers: List<MemberProfile>,
    currentMember: MemberProfile?,
    onSelectMember: (MemberProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val myId = currentMember?.id ?: 0L
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = remember(allMembers, myId, searchQuery) {
        allMembers.filter { it.id != myId }.filter { member ->
            searchQuery.isBlank() ||
            member.fullName.contains(searchQuery, ignoreCase = true) ||
            member.nickname.contains(searchQuery, ignoreCase = true) ||
            member.cedulaDni.contains(searchQuery, ignoreCase = true) ||
            member.bikePlate.contains(searchQuery, ignoreCase = true) ||
            member.chapterState.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.fullName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B26),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MotoOrangePrimary)
                    Text("NUEVO CHAT PRIVADO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Selecciona un miembro de la agrupación para abrir una conversación 1 a 1 directa:",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, apodo, cédula, placa o estado...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF222838),
                        focusedContainerColor = Color(0xFF222838)
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredMembers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron miembros con ese criterio", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredMembers) { member ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E2434),
                                border = BorderStroke(1.dp, Color(0xFF2E374D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectMember(member)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF262626))
                                            .border(1.5.dp, Color(member.role.badgeColorHex), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!member.profilePhotoUri.isNullOrBlank()) {
                                            coil.compose.AsyncImage(
                                                model = member.profilePhotoUri,
                                                contentDescription = member.fullName,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = if (member.nickname.isNotBlank()) member.nickname.take(2).uppercase() else member.avatarInitials,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Datos
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = member.role.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(member.role.badgeColorHex)
                                            )
                                            Text(text = "•", fontSize = 9.sp, color = Color.Gray)
                                            Text(
                                                text = if (member.nickname.isNotBlank()) "\"${member.nickname}\"" else "${member.bikeBrand} ${member.bikeModel}",
                                                fontSize = 9.sp,
                                                color = TxSteelSilver,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (member.chapterState.isNotBlank()) {
                                                Text(text = "•", fontSize = 9.sp, color = Color.Gray)
                                                Text(text = member.chapterState, fontSize = 9.sp, color = MotoGoldSecondary)
                                            }
                                        }
                                    }

                                    Icon(
                                        Icons.Default.ChatBubbleOutline,
                                        contentDescription = "Chatear",
                                        tint = MotoOrangePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}

