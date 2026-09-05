package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.PilotAvatar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.meshtx.GestorMeshTx
import com.example.meshtx.CanalTactico
import com.example.meshtx.MeshEstadoConexion
import com.example.meshtx.NodoMeshPiloto
import com.example.ui.preferences.PreferenciasApp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale

// ═══════════════════════════════════════════════════════════════
// CONSTANTES DE DISEÑO - TEMA CLARO PREMIUM DIRECTIVA TX
// ═══════════════════════════════════════════════════════════════
private val LightDirectivaBg = Color(0xFFF1F5F9)        // Slate 100 suave
private val LightCardBg = Color(0xFFFFFFFF)             // Blanco puro
private val LightCardSubtle = Color(0xFFF8FAFC)         // Slate 50
private val LightTextPrimary = Color(0xFF0F172A)        // Slate 900 Charcoal
private val LightTextSecondary = Color(0xFF475569)      // Slate 600
private val LightTextMuted = Color(0xFF64748B)          // Slate 500
private val LightBorder = Color(0xFFE2E8F0)             // Slate 200
private val LightBorderHover = Color(0xFFCBD5E1)        // Slate 300
private val DirectivaGoldPrimary = Color(0xFFD97706)    // Amber 600
private val DirectivaGoldDark = Color(0xFFB45309)       // Amber 700
private val DirectivaGoldBg = Color(0xFFFEF3C7)         // Amber 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectivaExclusiveScreen(
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    isLeaderSuperAdmin: Boolean,
    allMembers: List<MemberProfile>,
    invitationCodes: List<InvitationCode>,
    accessRequests: List<AccessRequest>,
    directivaChatMessages: List<ChatMessage>,
    disciplinaryRecords: List<DisciplinaryRecord> = emptyList(),
    onExpelMember: (MemberProfile, String) -> Unit = { _, _ -> },
    onGenerateCode: (targetRole: MemberRole, note: String, isSpecialGuest: Boolean, durationHours: Int) -> String,
    onDeleteCode: (InvitationCode) -> Unit,
    onDarDeBajaInvitado: (MemberProfile) -> Unit = {},
    onApproveRequest: (AccessRequest) -> String,
    onRejectRequest: (AccessRequest) -> Unit,
    onTransferCargo: (fromMember: MemberProfile, toMember: MemberProfile, cargo: MemberRole) -> Unit,
    onAbandonCargo: (MemberProfile) -> Unit,
    onAssignRole: (MemberProfile, MemberRole) -> Unit,
    onSendDirectivaChatMessage: (String) -> Unit,
    onSendMessage: (channelId: String, text: String, isRadioCallout: Boolean, replyToId: Long?, replyToSender: String?, replyToText: String?) -> Unit = { ch, text, radio, rId, rSender, rText -> },
    onSendAudio: (channelId: String, audioFile: java.io.File, durationSeconds: Int, transcriptionText: String?) -> Unit = { _, _, _, _ -> },
    onRequestDirectivaAccess: () -> Unit,
    onUnlockWithMasterCode: suspend (String) -> Pair<Boolean, String>,
    onToggleDirectiva: () -> Unit,
    onBack: () -> Unit = {},
    roleConfigs: List<RoleConfig> = emptyList(),
    onUpdateRoleConfig: (String, String, String) -> Unit = { _, _, _ -> },
    isChatEnabled: Boolean = true,
    onToggleChatEnabled: (Boolean) -> Unit = {},
    onClearGeneralChat: () -> Unit = {},
    onDeleteChatMessage: (Long) -> Unit = {},
    onDeleteMessageForMe: (Long) -> Unit = {},
    onMarkChatMessageAsRead: (ChatMessage) -> Unit = {},
    onMarkChannelAsRead: (String) -> Unit = {},
    onSendSticker: (String, java.io.File) -> Unit = { _, _ -> },
    onToggleReaction: (messageId: Long, emoji: String) -> Unit = { _, _ -> },
    onToggleChatMute: (MemberProfile, Boolean, String) -> Unit = { _, _, _ -> },
    onSuspendMember: (MemberProfile, String, Int) -> Unit = { _, _, _ -> },
    onReactivateMember: (MemberProfile) -> Unit = {},
    onToggleSolvency: (MemberProfile) -> Unit = {},
    onToggleBottomNav: (() -> Unit)? = null,
    allPrivateGroups: List<PrivateGroup> = emptyList(),
    onToggleBlockPrivateGroup: (groupId: String, isBlocked: Boolean, reason: String) -> Unit = { _, _, _ -> },
    onDeletePrivateGroup: (groupId: String) -> Unit = {},
    onCreateOfficialNotice: ((title: String, content: String, priority: String, isPinned: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hasDirectivaAccess = isDirectivaMode || isLeaderSuperAdmin || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true)

    // Sección seleccionada: null = Hub Principal de Tarjetas Interactivas, 0..9 = Sub-módulos
    var selectedSection by remember { mutableStateOf<Int?>(null) }

    // Dialog states
    var showGenerateCodeDialog by remember { mutableStateOf(false) }
    var showTransferCargoDialog by remember { mutableStateOf(false) }
    var showAssignRoleDialog by remember { mutableStateOf(false) }
    var showAbandonConfirmDialog by remember { mutableStateOf(false) }
    var showClearChatGeneralDialog by remember { mutableStateOf(false) }
    var showMasterCodePrompt by remember { mutableStateOf(false) }
    var memberToMute by remember { mutableStateOf<MemberProfile?>(null) }
    var muteReasonInput by remember { mutableStateOf("") }
    var memberToSuspend by remember { mutableStateOf<MemberProfile?>(null) }
    var suspendReasonInput by remember { mutableStateOf("") }
    var suspendDaysInput by remember { mutableStateOf("7") }
    var selectedMemberForAction by remember { mutableStateOf<MemberProfile?>(null) }
    var newlyCreatedCodeInfo by remember { mutableStateOf<String?>(null) }

    if (!hasDirectivaAccess) {
        // ═══════════════════════════════════════════════════════════════
        // VISTA BLOQUEADA - TEMA CLARO PARA NO-DIRECTIVOS
        // ═══════════════════════════════════════════════════════════════
        Surface(
            modifier = modifier.fillMaxSize(),
            color = LightDirectivaBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = StatusError.copy(alpha = 0.12f),
                    border = BorderStroke(2.dp, StatusError),
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Bloqueado",
                            tint = StatusError,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "APARTADO EXCLUSIVO DE DIRECTIVA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = LightTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = LightCardBg),
                    border = BorderStroke(1.dp, LightBorder),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Acceso Restringido a la Gobernanza TX",
                            color = StatusError,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Este panel es de uso exclusivo para los miembros de la Junta Directiva y Administradores. Puedes solicitar tu pase institucional o desbloquear con código maestro.",
                            color = LightTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Request directiva access button
                Button(
                    onClick = {
                        onRequestDirectivaAccess()
                        Toast.makeText(context, "Solicitud de ingreso a la Directiva enviada con éxito.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_request_directiva_access")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SOLICITAR ACCESO INSTITUCIONAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unlock with master code
                OutlinedButton(
                    onClick = { showMasterCodePrompt = true },
                    border = BorderStroke(1.5.dp, DirectivaGoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DirectivaGoldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_unlock_master_code")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = DirectivaGoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DESBLOQUEAR CON CÓDIGO MAESTRO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Volver
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_from_directiva_locked")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = LightTextMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("VOLVER AL MENÚ PRINCIPAL", color = LightTextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    } else {
        // ═══════════════════════════════════════════════════════════════
        // PANEL PRINCIPAL DE GOBERNANZA TX - TEMA CLARO UNIFICADO
        // ═══════════════════════════════════════════════════════════════
        Surface(
            modifier = modifier.fillMaxSize(),
            color = LightDirectivaBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Cabecera Superior de Gobernanza ──
                Surface(
                    color = LightCardBg,
                    border = BorderStroke(0.dp, Color.Transparent),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DirectivaGoldBg,
                                    border = BorderStroke(1.dp, DirectivaGoldPrimary),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = DirectivaGoldPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        text = "PANEL DE GOBERNANZA TX",
                                        fontWeight = FontWeight.Black,
                                        color = LightTextPrimary,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                    Text(
                                        text = "Administración y Control Institucional",
                                        fontSize = 10.sp,
                                        color = LightTextMuted
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_exit_directiva")
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = "Volver al Inicio",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Volver al Inicio",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (onToggleBottomNav != null) {
                                    IconButton(
                                        onClick = onToggleBottomNav,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Mostrar/Ocultar Menú",
                                            tint = LightTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Barra de Interruptores de Control Directiva ──
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightCardSubtle),
                            border = BorderStroke(1.dp, LightBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Modo Directiva
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Switch(
                                        checked = isDirectivaMode,
                                        onCheckedChange = { onToggleDirectiva() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = TxFlameRed,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = LightBorderHover
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Column {
                                        Text(
                                            text = "Modo Directiva",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = LightTextPrimary
                                        )
                                        Text(
                                            text = if (isDirectivaMode) "Activo" else "Inactivo",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDirectivaMode) TxFlameRed else LightTextMuted
                                        )
                                    }
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp).padding(horizontal = 4.dp),
                                    color = LightBorder
                                )

                                // Chat General Killswitch
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Switch(
                                        checked = isChatEnabled,
                                        onCheckedChange = { onToggleChatEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = StatusSuccess,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = StatusError
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Column {
                                        Text(
                                            text = "Chat General",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = LightTextPrimary
                                        )
                                        Text(
                                            text = if (isChatEnabled) "Abierto" else "Bloqueado",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChatEnabled) StatusSuccess else StatusError
                                        )
                                    }
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp).padding(horizontal = 4.dp),
                                    color = LightBorder
                                )

                                // Botón Limpiar Chat General
                                OutlinedButton(
                                    onClick = { showClearChatGeneralDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TxFlameRed),
                                    border = BorderStroke(1.dp, TxFlameRed.copy(alpha = 0.8f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp), tint = TxFlameRed)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Limpiar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TxFlameRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Barra Superior de Navegación Táctica (Visible cuando se entra a un módulo) ──
                        if (selectedSection != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedSection = null },
                                color = LightCardBg,
                                border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.6f)),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MotoOrangePrimary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.ArrowBack,
                                                contentDescription = "Volver",
                                                tint = MotoOrangePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "← VOLVER AL CENTRO DE MANDO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MotoOrangePrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = when (selectedSection) {
                                                0 -> "Gestión de Códigos de Acceso (24H)"
                                                1 -> "Solicitudes de Ingreso Pendientes"
                                                2 -> "Cargos, Miembros y Disciplina"
                                                3 -> "Canal Seguro Directiva TX"
                                                4 -> "Auditoría de Salas Privadas"
                                                5 -> "Gestión de Malla Mesh y Caravana"
                                                6 -> "Emisión de Comunicados Oficiales"
                                                7 -> "Gestor Rápido de Solvencia TX"
                                                8 -> "Monitor Central de Emergencias SOS"
                                                9 -> "Parámetros Institucionales del Club"
                                                else -> "Módulo de Directiva"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LightTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AssistChip(
                                        onClick = { selectedSection = null },
                                        label = { Text("Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = LightCardSubtle,
                                            labelColor = LightTextPrimary
                                        ),
                                        border = BorderStroke(1.dp, LightBorder)
                                    )
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(selectedSection, directivaChatMessages.size, currentMember) {
                    if (selectedSection == 3) {
                        val memberId = currentMember?.id ?: return@LaunchedEffect
                        val hasUnread = directivaChatMessages.any { it.channelId == "DIRECTIVA" && it.senderMemberId != memberId && !it.readBy.contains(memberId) }
                        if (hasUnread) {
                            onMarkChannelAsRead("DIRECTIVA")
                        }
                    }
                }

                // ── Contenido Principal: Hub de Tarjetas o Sub-Sección Activa ──
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedSection) {
                        null -> DirectivaDashboardHubView(
                            invitationCodes = invitationCodes,
                            accessRequests = accessRequests,
                            allMembers = allMembers,
                            directivaChatMessages = directivaChatMessages,
                            allPrivateGroups = allPrivateGroups,
                            currentMember = currentMember,
                            onSelectSection = { sectionIndex ->
                                selectedSection = sectionIndex
                                if (sectionIndex == 3) {
                                    onMarkChannelAsRead("DIRECTIVA")
                                }
                            }
                        )
                        0 -> DirectivaCodigosSection(
                            invitationCodes = invitationCodes,
                            allMembers = allMembers,
                            onOpenGenerateDialog = { showGenerateCodeDialog = true },
                            onDeleteCode = onDeleteCode,
                            onDarDeBajaInvitado = onDarDeBajaInvitado,
                            context = context
                        )
                        1 -> DirectivaSolicitudesSection(
                            accessRequests = accessRequests,
                            onApprove = { req ->
                                val generated = onApproveRequest(req)
                                newlyCreatedCodeInfo = "Código de 24h generado para ${req.fullName}: $generated"
                            },
                            onReject = onRejectRequest,
                            context = context
                        )
                        2 -> DirectivaCargosSection(
                            allMembers = allMembers,
                            currentMember = currentMember,
                            isLeaderSuperAdmin = isLeaderSuperAdmin,
                            roleConfigs = roleConfigs,
                            disciplinaryRecords = disciplinaryRecords,
                            onUpdateRoleConfig = onUpdateRoleConfig,
                            onTransferClick = { member ->
                                selectedMemberForAction = member
                                showTransferCargoDialog = true
                            },
                            onAbandonClick = { member ->
                                selectedMemberForAction = member
                                showAbandonConfirmDialog = true
                            },
                            onAssignRoleClick = { member ->
                                selectedMemberForAction = member
                                showAssignRoleDialog = true
                            },
                            onToggleChatMute = onToggleChatMute,
                            onSuspendMember = onSuspendMember,
                            onReactivateMember = onReactivateMember,
                            onToggleSolvency = onToggleSolvency,
                            onExpelMember = onExpelMember,
                            onOpenMuteDialog = { member ->
                                memberToMute = member
                                muteReasonInput = ""
                            },
                            onOpenSuspendDialog = { member ->
                                memberToSuspend = member
                                suspendReasonInput = ""
                                suspendDaysInput = "7"
                            }
                        )
                        3 -> ClubChatScreen(
                            messages = directivaChatMessages,
                            currentMember = currentMember,
                            isDirectivaMode = true,
                            roleConfigs = roleConfigs,
                            activeChannelId = "DIRECTIVA",
                            onSelectChannel = { /* Solo canal directiva */ },
                            onSendMessage = onSendMessage,
                            onDeleteMessage = onDeleteChatMessage,
                            onDeleteMessageForMe = onDeleteMessageForMe,
                            onMarkMessageAsRead = onMarkChatMessageAsRead,
                            onMarkChannelAsRead = onMarkChannelAsRead,
                            onSendSticker = onSendSticker,
                            onSendAudio = onSendAudio,
                            onToggleReaction = onToggleReaction,
                            allMembers = allMembers,
                            showOnlyDirectiva = true
                        )
                        4 -> DirectivaPrivateGroupsSection(
                            groups = allPrivateGroups,
                            allMembers = allMembers,
                            onToggleBlockGroup = onToggleBlockPrivateGroup,
                            onDeleteGroup = onDeletePrivateGroup
                        )
                        5 -> DirectivaMeshCaravanaSection(
                            context = context
                        )
                        6 -> DirectivaComunicadosSection(
                            context = context,
                            onCreateOfficialNotice = onCreateOfficialNotice
                        )
                        7 -> DirectivaSolvenciaRapidaSection(
                            allMembers = allMembers,
                            onToggleSolvency = onToggleSolvency
                        )
                        8 -> DirectivaSosMonitorSection(
                            context = context,
                            allMembers = allMembers
                        )
                        9 -> DirectivaConfiguracionClubSection(
                            context = context
                        )
                        else -> DirectivaDashboardHubView(
                            invitationCodes = invitationCodes,
                            accessRequests = accessRequests,
                            allMembers = allMembers,
                            directivaChatMessages = directivaChatMessages,
                            allPrivateGroups = allPrivateGroups,
                            currentMember = currentMember,
                            onSelectSection = { selectedSection = it }
                        )
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DIÁLOGOS DE CONTROL Y CONFIGURACIÓN (TEMA CLARO)
    // ═══════════════════════════════════════════════════════════════

    // Diálogo: Limpiar Chat General
    if (showClearChatGeneralDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatGeneralDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TxFlameRed)
                    Text("Limpiar Chat General", fontWeight = FontWeight.Bold, color = LightTextPrimary)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas vaciar todos los mensajes del Chat General?\n\nEsta acción borrará el historial en la nube y en los dispositivos de todos los miembros.",
                    color = LightTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearGeneralChat()
                        showClearChatGeneralDialog = false
                        Toast.makeText(context, "Chat General vaciado para todos los usuarios", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
                ) {
                    Text("Vaciar Chat Ahora", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatGeneralDialog = false }) {
                    Text("Cancelar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }

    // Diálogo: Desbloqueo con Código Maestro
    if (showMasterCodePrompt) {
        var masterInput by remember { mutableStateOf("") }
        var masterErr by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showMasterCodePrompt = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LightCardBg,
                border = BorderStroke(1.dp, DirectivaGoldPrimary),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = DirectivaGoldPrimary)
                            Text("Código Maestro", fontWeight = FontWeight.Black, fontSize = 16.sp, color = LightTextPrimary)
                        }
                        IconButton(onClick = { showMasterCodePrompt = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = LightTextMuted)
                        }
                    }

                    Text(
                        "Ingresa el código maestro de la Junta Directiva para obtener acceso inmediato a este módulo.",
                        fontSize = 12.sp,
                        color = LightTextSecondary
                    )

                    OutlinedTextField(
                        value = masterInput,
                        onValueChange = { masterInput = it; masterErr = null },
                        label = { Text("Código Maestro") },
                        placeholder = { Text("Ej. MASTER-TX-2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (masterErr != null) {
                        Text(masterErr ?: "", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val (ok, msg) = onUnlockWithMasterCode(masterInput)
                                if (ok) {
                                    showMasterCodePrompt = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    masterErr = "Código maestro inválido o no reconocido."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DirectivaGoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Validar y Desbloquear", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Diálogo: Generar Nuevo Código de Acceso
    if (showGenerateCodeDialog) {
        var isSpecialGuest by remember { mutableStateOf(false) }
        var selectedRole by remember { mutableStateOf(MemberRole.MIEMBRO_ACTIVO) }
        var durationHours by remember { mutableStateOf(24) }
        var codeNote by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showGenerateCodeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LightCardBg,
                border = BorderStroke(1.dp, if (isSpecialGuest) StatusWarning else MotoOrangePrimary),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSpecialGuest) "CÓDIGO INVITADO (ESPECIAL)" else "CÓDIGO ESTÁNDAR (NUMÉRICO)",
                            fontWeight = FontWeight.Black,
                            color = if (isSpecialGuest) DirectivaGoldDark else MotoOrangePrimary,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showGenerateCodeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = LightTextMuted)
                        }
                    }

                    Text(
                        text = if (isSpecialGuest)
                            "Genera un código ALFANUMÉRICO para invitados o personalidades. Podrás darlo de baja desde el panel en cualquier momento."
                        else
                            "Genera un código de 6 DÍGITOS NUMÉRICOS para el ingreso regular de miembros del club.",
                        fontSize = 11.sp,
                        color = LightTextSecondary,
                        lineHeight = 16.sp
                    )

                    // Selector de Tipo de Código
                    Text("Tipo de Acceso:", fontSize = 12.sp, color = LightTextPrimary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isSpecialGuest) MotoOrangePrimary.copy(alpha = 0.12f) else LightCardSubtle,
                            border = BorderStroke(1.5.dp, if (!isSpecialGuest) MotoOrangePrimary else LightBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSpecialGuest = false }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("🟢 ESTÁNDAR", fontWeight = FontWeight.Black, fontSize = 11.sp, color = if (!isSpecialGuest) MotoOrangePrimary else LightTextPrimary)
                                Text("Solo Números (6 dígitos)", fontSize = 9.sp, color = LightTextMuted)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSpecialGuest) DirectivaGoldBg else LightCardSubtle,
                            border = BorderStroke(1.5.dp, if (isSpecialGuest) DirectivaGoldPrimary else LightBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSpecialGuest = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("🟡 INVITADO", fontWeight = FontWeight.Black, fontSize = 11.sp, color = if (isSpecialGuest) DirectivaGoldDark else LightTextPrimary)
                                Text("Alfanumérico Especial", fontSize = 9.sp, color = LightTextMuted)
                            }
                        }
                    }

                    // Selector de Vigencia
                    Text("Vigencia del Código:", fontSize = 12.sp, color = LightTextPrimary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(6 to "6 Horas", 12 to "12 Horas", 24 to "24 Horas", 48 to "48 Horas").forEach { (hours, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (durationHours == hours) MotoOrangePrimary.copy(alpha = 0.15f) else LightCardSubtle,
                                border = BorderStroke(1.dp, if (durationHours == hours) MotoOrangePrimary else LightBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { durationHours = hours }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (durationHours == hours) MotoOrangePrimary else LightTextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    if (!isSpecialGuest) {
                        Text("Rol Institucional a Otorgar:", fontSize = 12.sp, color = LightTextPrimary, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                MemberRole.MIEMBRO_ACTIVO to "Miembro Activo / Piloto TX",
                                MemberRole.ASPIRANTE to "Aspirante / En Período de Prueba",
                                MemberRole.CAPITAN_RUTA to "Capitán de Ruta",
                                MemberRole.SEGURIDAD_VIAL to "Oficial de Seguridad Vial",
                                MemberRole.MECANICO_OFICIAL to "Mecánico Oficial"
                            ).forEach { (role, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedRole = role }
                                        .padding(vertical = 2.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedRole == role,
                                        onClick = { selectedRole = role },
                                        colors = RadioButtonDefaults.colors(selectedColor = MotoOrangePrimary)
                                    )
                                    Text(label, color = if (selectedRole == role) MotoOrangePrimary else LightTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = codeNote,
                        onValueChange = { codeNote = it },
                        label = { Text("Nota o Nombre del Destinatario (Opcional)") },
                        placeholder = { Text("Ej. Piloto Carlos Gómez - Capítulo Aragua") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Button(
                        onClick = {
                            val generated = onGenerateCode(
                                if (isSpecialGuest) MemberRole.INVITADO else selectedRole,
                                codeNote.trim(),
                                isSpecialGuest,
                                durationHours
                            )
                            showGenerateCodeDialog = false
                            newlyCreatedCodeInfo = "Nuevo código generado ($generated) válido por $durationHours horas."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSpecialGuest) DirectivaGoldPrimary else MotoOrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERAR Y GUARDAR CÓDIGO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Diálogo Informativo: Código Creado Exitosamente
    if (newlyCreatedCodeInfo != null) {
        AlertDialog(
            onDismissRequest = { newlyCreatedCodeInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                    Text("Código Generado", fontWeight = FontWeight.Black, color = LightTextPrimary)
                }
            },
            text = {
                Text(newlyCreatedCodeInfo ?: "", color = LightTextPrimary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = { newlyCreatedCodeInfo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Text("Entendido", color = Color.White)
                }
            },
            containerColor = LightCardBg
        )
    }

    // 🏆 Diálogo: Asignar Cargo / Rol Institucional a Piloto
    if (showAssignRoleDialog) {
        var memberSearch by remember { mutableStateOf("") }
        var targetMember by remember { mutableStateOf<MemberProfile?>(selectedMemberForAction) }
        var selectedRole by remember { mutableStateOf(targetMember?.role ?: MemberRole.MIEMBRO_ACTIVO) }

        val candidateMembers = remember(allMembers, memberSearch) {
            if (memberSearch.isBlank()) allMembers
            else allMembers.filter {
                it.fullName.contains(memberSearch, ignoreCase = true) ||
                it.nickname.contains(memberSearch, ignoreCase = true) ||
                it.memberNumber.contains(memberSearch, ignoreCase = true) ||
                it.bikePlate.contains(memberSearch, ignoreCase = true)
            }
        }

        Dialog(onDismissRequest = {
            showAssignRoleDialog = false
            selectedMemberForAction = null
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LightCardBg,
                border = BorderStroke(1.dp, DirectivaGoldPrimary),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = DirectivaGoldPrimary)
                            Text("Asignar Cargo a Piloto", fontWeight = FontWeight.Black, fontSize = 16.sp, color = LightTextPrimary)
                        }
                        IconButton(onClick = {
                            showAssignRoleDialog = false
                            selectedMemberForAction = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = LightTextMuted)
                        }
                    }

                    // Paso 1: Seleccionar Piloto
                    Text("1. Selecciona el Piloto:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    if (targetMember == null) {
                        OutlinedTextField(
                            value = memberSearch,
                            onValueChange = { memberSearch = it },
                            placeholder = { Text("Buscar por nombre, apodo, carnet o placa...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LightTextMuted, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LightCardSubtle,
                            border = BorderStroke(1.dp, LightBorder),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(4.dp)) {
                                items(candidateMembers) { m ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                targetMember = m
                                                selectedRole = m.role
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(m.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                                            Text("${m.memberNumber} • ${m.nickname.ifBlank { "Sin apodo" }}", fontSize = 10.sp, color = LightTextMuted)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(m.role.badgeColorHex).copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, Color(m.role.badgeColorHex))
                                        ) {
                                            Text(m.role.displayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(m.role.badgeColorHex), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DirectivaGoldBg,
                            border = BorderStroke(1.dp, DirectivaGoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PilotAvatar(member = targetMember!!, size = 36.dp)
                                    Column {
                                        Text(targetMember!!.fullName, fontWeight = FontWeight.Black, fontSize = 13.sp, color = LightTextPrimary)
                                        Text("Carnet: ${targetMember!!.memberNumber} • Cargo actual: ${targetMember!!.role.displayName}", fontSize = 10.sp, color = LightTextSecondary)
                                    }
                                }
                                TextButton(onClick = { targetMember = null }) {
                                    Text("Cambiar", fontSize = 11.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Paso 2: Seleccionar Nuevo Rol
                    Text("2. Selecciona el Nuevo Cargo / Rol:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MemberRole.values().forEach { role ->
                            val isSelected = selectedRole == role
                            val roleColor = Color(role.badgeColorHex)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) roleColor.copy(alpha = 0.15f) else LightCardSubtle,
                                border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) roleColor else LightBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRole = role }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedRole = role },
                                            colors = RadioButtonDefaults.colors(selectedColor = roleColor)
                                        )
                                        Column {
                                            Text(role.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightTextPrimary)
                                            Text(role.roleDuties, fontSize = 10.sp, color = LightTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (targetMember != null) {
                                onAssignRole(targetMember!!, selectedRole)
                                showAssignRoleDialog = false
                                selectedMemberForAction = null
                                Toast.makeText(context, "Cargo de '${selectedRole.displayName}' asignado a ${targetMember!!.fullName}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Selecciona un piloto primero", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = targetMember != null,
                        colors = ButtonDefaults.buttonColors(containerColor = DirectivaGoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirmar Asignación de Cargo", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // 🔄 Diálogo: Transferir Cargo Institucional
    if (showTransferCargoDialog && selectedMemberForAction != null) {
        val targetMember = selectedMemberForAction!!
        var selectedCargo by remember { mutableStateOf(targetMember.role) }

        AlertDialog(
            onDismissRequest = {
                showTransferCargoDialog = false
                selectedMemberForAction = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = DirectivaGoldPrimary)
                    Text("Transferir Cargo Institucional", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LightTextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "¿Deseas transferir oficialmente un cargo de la Junta Directiva o Ruta a ${targetMember.fullName} (${targetMember.nickname})?",
                        fontSize = 13.sp,
                        color = LightTextSecondary
                    )

                    Text("Selecciona el Cargo a Transferir:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    listOf(
                        MemberRole.VICEPRESIDENTE,
                        MemberRole.SECRETARIO,
                        MemberRole.TESORERO,
                        MemberRole.CAPITAN_RUTA,
                        MemberRole.SEGURIDAD_VIAL,
                        MemberRole.MECANICO_OFICIAL,
                        MemberRole.MEDICO_CLUB,
                        MemberRole.DISCIPLINARIO
                    ).forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCargo = role }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = selectedCargo == role, onClick = { selectedCargo = role })
                            Text(role.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LightTextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sender = currentMember ?: targetMember
                        onTransferCargo(sender, targetMember, selectedCargo)
                        showTransferCargoDialog = false
                        selectedMemberForAction = null
                        Toast.makeText(context, "Cargo transferido oficialmente a ${targetMember.fullName}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DirectivaGoldPrimary)
                ) {
                    Text("Confirmar Transferencia", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTransferCargoDialog = false
                    selectedMemberForAction = null
                }) {
                    Text("Cancelar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }

    // ⚠️ Diálogo: Entregar / Poner Cargo a Disposición
    if (showAbandonConfirmDialog && selectedMemberForAction != null) {
        val member = selectedMemberForAction!!
        AlertDialog(
            onDismissRequest = {
                showAbandonConfirmDialog = false
                selectedMemberForAction = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TxFlameRed)
                    Text("Poner Cargo a Disposición", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LightTextPrimary)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas que ${member.fullName} entregue su cargo de '${member.role.displayName}'?\n\nEl cargo pasará a estar vacante y el piloto retornará a Miembro Activo.",
                    fontSize = 13.sp,
                    color = LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAbandonCargo(member)
                        showAbandonConfirmDialog = false
                        selectedMemberForAction = null
                        Toast.makeText(context, "Cargo puesto a disposición", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
                ) {
                    Text("Entregar Cargo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAbandonConfirmDialog = false
                    selectedMemberForAction = null
                }) {
                    Text("Cancelar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }

    // 🔇 Diálogo: Silenciar Piloto en Chat
    if (memberToMute != null) {
        val member = memberToMute!!
        AlertDialog(
            onDismissRequest = { memberToMute = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VolumeOff, contentDescription = null, tint = DirectivaGoldDark)
                    Text("Silenciar en Chat", fontWeight = FontWeight.Bold, color = LightTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Silenciar a ${member.fullName} (${member.nickname}) en todos los canales del club.", fontSize = 13.sp, color = LightTextSecondary)
                    OutlinedTextField(
                        value = muteReasonInput,
                        onValueChange = { muteReasonInput = it },
                        label = { Text("Motivo del Silencio") },
                        placeholder = { Text("Ej. Llamado de atención por la Directiva") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleChatMute(member, true, muteReasonInput.ifBlank { "Llamado de atención por la Directiva" })
                        memberToMute = null
                        Toast.makeText(context, "Piloto silenciado en el chat", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DirectivaGoldPrimary)
                ) {
                    Text("Silenciar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToMute = null }) {
                    Text("Cancelar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }

    // ⛔ Diálogo: Suspender Miembro
    if (memberToSuspend != null) {
        val member = memberToSuspend!!
        AlertDialog(
            onDismissRequest = { memberToSuspend = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = StatusError)
                    Text("Suspender Miembro", fontWeight = FontWeight.Bold, color = StatusError, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aplicar medida disciplinaria a ${member.fullName} (${member.nickname}). Su acceso a la app quedará inhabilitado.", fontSize = 13.sp, color = LightTextSecondary)
                    OutlinedTextField(
                        value = suspendReasonInput,
                        onValueChange = { suspendReasonInput = it },
                        label = { Text("Motivo de la Suspensión") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = suspendDaysInput,
                        onValueChange = { suspendDaysInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Días de Suspensión (0 = Indefinido)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = suspendDaysInput.toIntOrNull() ?: 7
                        onSuspendMember(member, suspendReasonInput.ifBlank { "Medida disciplinaria de la Directiva" }, days)
                        memberToSuspend = null
                        Toast.makeText(context, "Sanción disciplinaria aplicada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Aplicar Sanción", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToSuspend = null }) {
                    Text("Cancelar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-SECCIÓN 0: GESTOR DE CÓDIGOS DE INVITACIÓN (24H & MAESTROS)
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DirectivaCodigosSection(
    invitationCodes: List<InvitationCode>,
    allMembers: List<MemberProfile> = emptyList(),
    onOpenGenerateDialog: () -> Unit,
    onDeleteCode: (InvitationCode) -> Unit,
    onDarDeBajaInvitado: (MemberProfile) -> Unit = {},
    context: Context
) {
    val now = System.currentTimeMillis()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var selectedCodeForDetail by remember { mutableStateOf<InvitationCode?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Botón Principal Generar Código con texto flexible
            Button(
                onClick = onOpenGenerateDialog,
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .testTag("btn_directiva_generate_code")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERAR CÓDIGO (ESTÁNDAR O INVITADO)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Sección: Invitados Especiales Activos
        val activeGuests = allMembers.filter { it.role == MemberRole.INVITADO && !it.isSuspended }
        if (activeGuests.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = DirectivaGoldPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "INVITADOS ESPECIALES ACTIVOS EN LA APP (${activeGuests.size})",
                        fontWeight = FontWeight.Black,
                        color = DirectivaGoldDark,
                        fontSize = 12.sp
                    )
                }
            }

            items(activeGuests) { guest ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightCardBg),
                    border = BorderStroke(1.5.dp, DirectivaGoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PilotAvatar(member = guest, size = 44.dp)
                                Column {
                                    Text(
                                        text = guest.fullName,
                                        fontWeight = FontWeight.Black,
                                        color = LightTextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${guest.memberNumber} • Tel: ${guest.phone.ifBlank { "Sin teléfono" }}",
                                        color = LightTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DirectivaGoldBg,
                                border = BorderStroke(1.dp, DirectivaGoldPrimary)
                            ) {
                                Text(
                                    text = "🟡 INVITADO TEMPORAL",
                                    color = DirectivaGoldDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { onDarDeBajaInvitado(guest) },
                            colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DAR DE BAJA Y BLOQUEAR ACCESO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Sección: Códigos Maestros Permanentes
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = DirectivaGoldPrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = "CÓDIGOS MAESTROS PERMANENTES",
                    fontWeight = FontWeight.Black,
                    color = DirectivaGoldDark,
                    fontSize = 12.sp
                )
            }
        }

        items(invitationCodes.filter { it.isMaster }) { masterCode ->
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.5.dp, DirectivaGoldPrimary),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = DirectivaGoldPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = masterCode.code,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = LightTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DirectivaGoldBg,
                            border = BorderStroke(1.dp, DirectivaGoldPrimary)
                        ) {
                            Text(
                                text = "NO BORRABLE",
                                color = DirectivaGoldDark,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = masterCode.note.ifBlank { "Código Maestro Supremo del Líder - Acceso irrestricto" },
                        color = LightTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Sección: Códigos Temporales (24H)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = "CÓDIGOS TEMPORALES DE INVITACIÓN (24H)",
                    fontWeight = FontWeight.Black,
                    color = LightTextPrimary,
                    fontSize = 12.sp
                )
            }
        }

        val tempCodes = invitationCodes.filter { !it.isMaster }
        if (tempCodes.isEmpty()) {
            item {
                Surface(
                    color = LightCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No hay códigos temporales activos. Genera uno con el botón superior.",
                        color = LightTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(tempCodes) { code ->
                val isExpired = code.expiresAt < now
                val remainingHours = ((code.expiresAt - now) / (1000 * 60 * 60)).coerceAtLeast(0)
                val remainingMins = (((code.expiresAt - now) / (1000 * 60)) % 60).coerceAtLeast(0)

                // 🏷️ Tarjeta Compacta (Fila Resumida)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LightCardBg,
                    border = BorderStroke(
                        1.dp,
                        if (code.isUsed) StatusSuccess.copy(alpha = 0.6f)
                        else if (isExpired) StatusError.copy(alpha = 0.5f)
                        else MotoOrangePrimary.copy(alpha = 0.6f)
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCodeForDetail = code }
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
                            Icon(
                                imageVector = if (code.isSpecialGuest) Icons.Default.VpnKey else Icons.Default.Key,
                                contentDescription = null,
                                tint = if (code.isUsed) StatusSuccess else if (isExpired) LightTextMuted else MotoOrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = code.code,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (isExpired) LightTextMuted else LightTextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (code.note.isNotBlank()) code.note else "Rol: ${code.targetRole.displayName}",
                                    fontSize = 11.sp,
                                    color = LightTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (code.isUsed) StatusSuccess.copy(alpha = 0.12f)
                                else if (isExpired) StatusError.copy(alpha = 0.12f)
                                else DirectivaGoldBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (code.isUsed) StatusSuccess else if (isExpired) StatusError else DirectivaGoldPrimary
                                )
                            ) {
                                Text(
                                    text = if (code.isUsed) "UTILIZADO" else if (isExpired) "EXPIRADO" else "ACTIVO (${remainingHours}h ${remainingMins}m)",
                                    color = if (code.isUsed) StatusSuccess else if (isExpired) StatusError else DirectivaGoldDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = "Ver Detalle", tint = LightTextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // 📋 Diálogo / Modal de Detalle Completo del Código
    if (selectedCodeForDetail != null) {
        val code = selectedCodeForDetail!!
        val isExpired = code.expiresAt < now
        val remainingHours = ((code.expiresAt - now) / (1000 * 60 * 60)).coerceAtLeast(0)
        val remainingMins = (((code.expiresAt - now) / (1000 * 60)) % 60).coerceAtLeast(0)

        AlertDialog(
            onDismissRequest = { selectedCodeForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MotoOrangePrimary)
                        Text(
                            text = code.code,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = LightTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (code.isUsed) StatusSuccess.copy(alpha = 0.12f)
                        else if (isExpired) StatusError.copy(alpha = 0.12f)
                        else DirectivaGoldBg,
                        border = BorderStroke(
                            1.dp,
                            if (code.isUsed) StatusSuccess else if (isExpired) StatusError else DirectivaGoldPrimary
                        )
                    ) {
                        Text(
                            text = if (code.isUsed) "UTILIZADO" else if (isExpired) "EXPIRADO" else "ACTIVO: ${remainingHours}h ${remainingMins}m",
                            color = if (code.isUsed) StatusSuccess else if (isExpired) StatusError else DirectivaGoldDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Estado de Uso & Usuario que lo Activó
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LightCardSubtle,
                        border = BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ESTADO DE USO & REGISTRO:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = LightTextMuted)
                            if (code.isUsed) {
                                Text(" Usado por: ${code.usedByName ?: "Piloto Registrado"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                                if (!code.usedByPhone.isNullOrBlank()) {
                                    Text(" Teléfono: ${code.usedByPhone}", fontSize = 11.sp, color = LightTextSecondary)
                                }
                            } else if (isExpired) {
                                Text(" Expirado sin utilizar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusError)
                            } else {
                                Text(" Disponible para ingresar - Código Activo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DirectivaGoldDark)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Rol Otorgado: ${code.targetRole.displayName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                        Text("Creador: ${code.createdBy}", fontSize = 11.sp, color = LightTextSecondary)
                        Text("Fecha Creación: ${dateFormat.format(Date(code.createdAt))}", fontSize = 10.sp, color = LightTextMuted)
                        Text("Fecha Expiración: ${dateFormat.format(Date(code.expiresAt))}", fontSize = 10.sp, color = LightTextMuted)
                        if (code.note.isNotBlank()) {
                            Text("Nota / Destinatario: \"${code.note}\"", fontSize = 11.sp, color = DirectivaGoldDark, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Código TX", code.code))
                                Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "🏍️ Invitación a Team Nacional TX Venezuela: Usa tu código de acceso: ${code.code} (Válido por 24 horas)."
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Enviar código por WhatsApp"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCode(code)
                        selectedCodeForDetail = null
                        Toast.makeText(context, "Código eliminado / cancelado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar Código", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCodeForDetail = null }) {
                    Text("Cerrar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-SECCIÓN 1: SOLICITUDES DE ENTRADA PENDIENTES
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DirectivaSolicitudesSection(
    accessRequests: List<AccessRequest>,
    onApprove: (AccessRequest) -> Unit,
    onReject: (AccessRequest) -> Unit,
    context: Context
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "SOLICITUDES DE INGRESO Y CÓDIGOS DE ACCESO",
                fontWeight = FontWeight.Black,
                color = LightTextPrimary,
                fontSize = 13.sp
            )
        }

        if (accessRequests.isEmpty()) {
            item {
                Surface(
                    color = LightCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No hay solicitudes de acceso pendientes.",
                        color = LightTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(accessRequests) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightCardBg),
                    border = BorderStroke(
                        1.dp,
                        if (req.status == "PENDIENTE") MotoOrangePrimary
                        else if (req.status == "APROBADO") StatusSuccess
                        else StatusError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = req.fullName,
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (req.status == "APROBADO") StatusSuccess.copy(alpha = 0.12f)
                                else if (req.status == "RECHAZADO") StatusError.copy(alpha = 0.12f)
                                else DirectivaGoldBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (req.status == "APROBADO") StatusSuccess else if (req.status == "RECHAZADO") StatusError else DirectivaGoldPrimary
                                )
                            ) {
                                Text(
                                    text = req.status,
                                    color = if (req.status == "APROBADO") StatusSuccess else if (req.status == "RECHAZADO") StatusError else DirectivaGoldDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Tel: ${req.phone} • Cédula: ${req.cedulaDni}",
                            fontSize = 11.sp,
                            color = LightTextSecondary
                        )
                        Text(
                            text = "Moto: ${req.bikeBrand} ${req.bikeModel} • Color: ${req.bikeColor} • Placa: ${req.bikePlate}",
                            fontSize = 11.sp,
                            color = LightTextSecondary
                        )
                        Text(
                            text = "Capítulo / Estado: ${req.chapterState} • Rol Solicitado: ${req.requestedRole}",
                            fontSize = 11.sp,
                            color = MotoOrangePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (req.reasonMessage.isNotBlank()) {
                            Text(
                                text = "Motivo: \"${req.reasonMessage}\"",
                                fontSize = 11.sp,
                                color = LightTextPrimary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                        Text(
                            text = "Fecha: ${dateFormat.format(Date(req.timestamp))}",
                            fontSize = 10.sp,
                            color = LightTextMuted
                        )

                        if (req.status == "PENDIENTE") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aprobar (24h)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { onReject(req) },
                                    border = BorderStroke(1.dp, StatusError),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rechazar", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-SECCIÓN 2: GESTIÓN DE CARGOS Y ROLES DE PILOTOS
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DirectivaCargosSection(
    allMembers: List<MemberProfile>,
    currentMember: MemberProfile?,
    isLeaderSuperAdmin: Boolean,
    roleConfigs: List<RoleConfig>,
    onUpdateRoleConfig: (String, String, String) -> Unit,
    onTransferClick: (MemberProfile) -> Unit,
    onAbandonClick: (MemberProfile) -> Unit,
    onAssignRoleClick: (MemberProfile) -> Unit,
    onToggleChatMute: (MemberProfile, Boolean, String) -> Unit,
    onSuspendMember: (MemberProfile, String, Int) -> Unit,
    onReactivateMember: (MemberProfile) -> Unit,
    onToggleSolvency: (MemberProfile) -> Unit,
    onOpenMuteDialog: (MemberProfile) -> Unit,
    onOpenSuspendDialog: (MemberProfile) -> Unit,
    disciplinaryRecords: List<DisciplinaryRecord> = emptyList(),
    onExpelMember: (MemberProfile, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("TODOS") }
    var memberToExpel by remember { mutableStateOf<MemberProfile?>(null) }
    var expelReasonInput by remember { mutableStateOf("") }

    val filterOptions = listOf("TODOS", "DIRECTIVA", "ACTIVOS", "SILENCIADOS", "SUSPENDIDOS", "INSOLVENTES", "BAJAS / EXPULSADOS")

    val filteredMembers = remember(allMembers, searchQuery, selectedFilter) {
        allMembers.filter { member ->
            val matchesQuery = searchQuery.isBlank() ||
                    member.fullName.contains(searchQuery, ignoreCase = true) ||
                    member.nickname.contains(searchQuery, ignoreCase = true) ||
                    member.memberNumber.contains(searchQuery, ignoreCase = true) ||
                    member.bikePlate.contains(searchQuery, ignoreCase = true) ||
                    member.chapterState.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "TODOS" -> true
                "DIRECTIVA" -> member.isDirectiva || member.role.canManageApp || member.role == MemberRole.PRESIDENTE
                "ACTIVOS" -> !member.isSuspended
                "SILENCIADOS" -> member.isChatMuted
                "SUSPENDIDOS" -> member.isSuspended
                "INSOLVENTES" -> !member.solvencyStatus
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val expelledRecords = remember(disciplinaryRecords, searchQuery) {
        disciplinaryRecords.filter { record ->
            val matchesQuery = searchQuery.isBlank() ||
                    record.memberName.contains(searchQuery, ignoreCase = true) ||
                    record.reason.contains(searchQuery, ignoreCase = true) ||
                    record.issuedBy.contains(searchQuery, ignoreCase = true) ||
                    record.penaltyType.contains(searchQuery, ignoreCase = true)
            matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, DirectivaGoldPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GOBERNANZA Y DISCIPLINA DE MIEMBROS",
                        fontWeight = FontWeight.Black,
                        color = DirectivaGoldDark,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Control en tiempo real de solvencias, silencios de chat, sanciones disciplinarias, expulsiones y cargos institucionales.",
                        fontSize = 11.sp,
                        color = LightTextSecondary
                    )
                }
            }
        }

        // Buscador y Filtros
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, placa, carnet, motivo o autoridad...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LightTextMuted, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LightCardBg,
                        unfocusedContainerColor = LightCardBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (filter == "BAJAS / EXPULSADOS") StatusError else MotoOrangePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = LightCardBg,
                                labelColor = if (filter == "BAJAS / EXPULSADOS") StatusError else LightTextSecondary
                            )
                        )
                    }
                }
            }
        }

        if (selectedFilter == "BAJAS / EXPULSADOS") {
            item {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                            Text(
                                text = "LIBRO DE ACTAS: MIEMBROS PASADOS Y EXPULSIONES",
                                fontWeight = FontWeight.Black,
                                color = StatusError,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Historial inmutable con fecha, motivo y directivo actuante. Los miembros aquí listados fueron removidos de la app y sus credenciales revocadas.",
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }

            if (expelledRecords.isEmpty()) {
                item {
                    Surface(
                        color = LightCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No hay registros de miembros expulsados o dados de baja en el archivo histórico.",
                            color = LightTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(expelledRecords, key = { it.id }) { record ->
                    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault()) }
                    val fechaStr = dateFormat.format(Date(record.timestamp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightCardBg),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.memberName,
                                        fontWeight = FontWeight.Black,
                                        color = LightTextPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Fecha de registro: $fechaStr",
                                        fontSize = 10.sp,
                                        color = LightTextMuted
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StatusError.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, StatusError)
                                ) {
                                    Text(
                                        text = record.penaltyType.ifBlank { "EXPULSIÓN" },
                                        color = StatusError,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = LightBorder, thickness = 0.5.dp)

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Motivo de la Sanción:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LightCardSubtle,
                                    border = BorderStroke(0.5.dp, LightBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = record.reason,
                                        fontSize = 11.sp,
                                        color = LightTextPrimary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Expedido por: ${record.issuedBy}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LightTextMuted
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "🔒 Acceso Revocado",
                                        fontSize = 9.sp,
                                        color = LightTextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Lista de Miembros Activos/Filtrados
            if (filteredMembers.isEmpty()) {
                item {
                    Surface(
                        color = LightCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No se encontraron pilotos que coincidan con la búsqueda.",
                            color = LightTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredMembers, key = { it.id }) { member ->
                val roleColor = Color(member.role.badgeColorHex)
                val isMe = member.id == (currentMember?.id ?: 0)

                Card(
                    colors = CardDefaults.cardColors(containerColor = LightCardBg),
                    border = BorderStroke(1.dp, if (member.isSuspended) StatusError else LightBorder),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PilotAvatar(member = member, size = 42.dp)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = member.fullName,
                                            fontWeight = FontWeight.Bold,
                                            color = LightTextPrimary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isMe) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = DirectivaGoldBg
                                            ) {
                                                Text("TÚ", fontSize = 8.sp, fontWeight = FontWeight.Black, color = DirectivaGoldDark, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${member.memberNumber} • Placa: ${member.bikePlate} • ${member.chapterState}",
                                        fontSize = 10.sp,
                                        color = LightTextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = roleColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, roleColor)
                            ) {
                                Text(
                                    text = member.role.displayName,
                                    color = roleColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Estado de Solvencia & Silencio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (member.solvencyStatus) StatusSuccess.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, if (member.solvencyStatus) StatusSuccess else StatusError),
                                    modifier = Modifier.clickable { onToggleSolvency(member) }
                                ) {
                                    Text(
                                        text = if (member.solvencyStatus) "🟢 SOLVENTE" else "🔴 INSOLVENTE",
                                        color = if (member.solvencyStatus) StatusSuccess else StatusError,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (member.isChatMuted) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StatusWarning.copy(alpha = 0.12f),
                                        border = BorderStroke(0.5.dp, StatusWarning)
                                    ) {
                                        Text(
                                            text = "🔇 SILENCIADO",
                                            color = DirectivaGoldDark,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (member.isSuspended) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StatusError.copy(alpha = 0.12f),
                                        border = BorderStroke(0.5.dp, StatusError)
                                    ) {
                                        Text(
                                            text = "⛔ SUSPENDIDO",
                                            color = StatusError,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Botones de Acción sobre el Miembro
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onAssignRoleClick(member) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text("Asignar Cargo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (member.isChatMuted) onToggleChatMute(member, false, "")
                                        else onOpenMuteDialog(member)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (member.isChatMuted) StatusSuccess else DirectivaGoldDark
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text(if (member.isChatMuted) "Desmutear" else "Silenciar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (member.isSuspended) onReactivateMember(member)
                                        else onOpenSuspendDialog(member)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (member.isSuspended) StatusSuccess else Color(0xFFD97706)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text(if (member.isSuspended) "Reactivar" else "Suspender", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                val canExpel = !isMe && member.role != MemberRole.DESARROLLADOR && (member.role != MemberRole.PRESIDENTE || isLeaderSuperAdmin)
                                Button(
                                    onClick = {
                                        memberToExpel = member
                                        expelReasonInput = ""
                                    },
                                    enabled = canExpel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusError,
                                        disabledContainerColor = StatusError.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expulsar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // 🛑 Diálogo de Confirmación: Expulsión Definitiva de Miembro
    memberToExpel?.let { target ->
        AlertDialog(
            onDismissRequest = { memberToExpel = null },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Expulsar Miembro Definitivamente",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = LightTextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Piloto: ${target.fullName} (${target.memberNumber.ifBlank { "S/N" }})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = LightTextPrimary
                            )
                            Text(
                                text = "⚠️ ADVERTENCIA: Esta acción eliminará al miembro de toda la plataforma, revocará y eliminará su código de acceso para que NO pueda volver a ingresar, y quedará asentado en el Libro de Actas Histórico.",
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B),
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "Para volver a entrar en el futuro, deberá solicitar un código nuevo desde el inicio formalmente.",
                                fontSize = 10.sp,
                                color = Color(0xFF7F1D1D),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = expelReasonInput,
                        onValueChange = { expelReasonInput = it },
                        label = { Text("Motivo de la Expulsión (Obligatorio)", fontSize = 11.sp) },
                        placeholder = { Text("Ej: Incumplimiento grave de estatutos institucionales...", fontSize = 11.sp) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LightCardBg,
                            unfocusedContainerColor = LightCardBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = expelReasonInput.trim().ifBlank { "Sin motivo especificado por directiva." }
                        onExpelMember(target, reason)
                        memberToExpel = null
                    },
                    enabled = expelReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirmar Expulsión", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { memberToExpel = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", fontSize = 11.sp, color = LightTextSecondary)
                }
            },
            containerColor = LightCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-SECCIÓN 3: CHAT DE DIRECTIVA (SIN PARPADEO Y TEMA CLARO)
// ═══════════════════════════════════════════════════════════════
@Composable
private fun DirectivaChatSection(
    messages: List<ChatMessage>,
    currentMember: MemberProfile?,
    onSendMessage: (String) -> Unit
) {
    var chatInput by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Banner Superior de Estado
        Surface(
            color = DirectivaGoldBg,
            border = BorderStroke(1.dp, DirectivaGoldPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = DirectivaGoldDark, modifier = Modifier.size(16.dp))
                Text(
                    text = "CANAL PRIVADO DE GOBERNANZA & ALERTAS AUTOMÁTICAS",
                    color = DirectivaGoldDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val dirMessages = messages.filter { it.channelId == "DIRECTIVA" }
            if (dirMessages.isEmpty()) {
                item {
                    Text(
                        text = "Canal de Directiva vacío. Escribe aquí para coordinar acciones o recibir solicitudes.",
                        color = LightTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(dirMessages, key = { it.id }) { msg ->
                    val isMine = msg.senderMemberId == (currentMember?.id ?: 0)
                    val isSystem = msg.senderNickname == "Bot Directiva" || msg.senderMemberNumber == "TX-BOT" || msg.isRadioCallout

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystem) Color(0xFFFFF7ED) // Orange 50
                            else if (isMine) Color(0xFFEFF6FF)              // Blue 50
                            else LightCardBg
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSystem) MotoOrangePrimary
                            else if (isMine) Color(0xFF93C5FD)
                            else LightBorder
                        ),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${msg.senderName} (${msg.senderNickname})",
                                        fontWeight = FontWeight.Black,
                                        color = if (isSystem) MotoOrangePrimary else DirectivaGoldDark,
                                        fontSize = 12.sp
                                    )
                                    if (isSystem) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MotoOrangePrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text("SISTEMA", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = dateFormat.format(Date(msg.timestamp)),
                                    color = LightTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = msg.messageText,
                                color = LightTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Barra Inferior de Entrada de Mensajes
        Surface(
            color = LightCardBg,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 84.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Mensaje interno de Directiva...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LightCardSubtle,
                        unfocusedContainerColor = LightCardSubtle
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            onSendMessage(chatInput.trim())
                            chatInput = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MotoOrangePrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-SECCIÓN 4: SUPERVISIÓN DE SALAS PRIVADAS (MODERACIÓN)
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaPrivateGroupsSection(
    groups: List<PrivateGroup>,
    allMembers: List<MemberProfile>,
    onToggleBlockGroup: (groupId: String, isBlocked: Boolean, reason: String) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterBlockedOnly by remember { mutableStateOf(false) }

    // Dialog states
    var groupToBlockOrUnblock by remember { mutableStateOf<PrivateGroup?>(null) }
    var groupForMembersView by remember { mutableStateOf<PrivateGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<PrivateGroup?>(null) }

    val filteredGroups = remember(groups, searchQuery, filterBlockedOnly) {
        groups.filter { grp ->
            val matchesSearch = if (searchQuery.isBlank()) true
            else grp.name.contains(searchQuery, ignoreCase = true) ||
                    grp.creatorName.contains(searchQuery, ignoreCase = true) ||
                    grp.creatorNickname.contains(searchQuery, ignoreCase = true) ||
                    grp.id.contains(searchQuery, ignoreCase = true)

            val matchesFilter = if (filterBlockedOnly) grp.isBlockedByDirectiva else true
            matchesSearch && matchesFilter
        }
    }

    val totalGroups = groups.size
    val activeGroups = groups.count { !it.isBlockedByDirectiva }
    val blockedGroups = groups.count { it.isBlockedByDirectiva }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner Resumen de Métricas
        Card(
            colors = CardDefaults.cardColors(containerColor = LightCardBg),
            border = BorderStroke(1.dp, LightBorder),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(18.dp))
                        Text(
                            "SUPERVISIÓN DE SALAS PRIVADAS",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = LightTextPrimary
                        )
                    }
                    Text(
                        "$totalGroups SALAS",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = MotoOrangePrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusSuccess.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, StatusSuccess),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ACTIVAS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                            Text("$activeGroups", fontSize = 14.sp, fontWeight = FontWeight.Black, color = StatusSuccess)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (blockedGroups > 0) StatusError.copy(alpha = 0.12f) else LightCardSubtle,
                        border = BorderStroke(0.5.dp, if (blockedGroups > 0) StatusError else LightBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BLOQUEADAS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (blockedGroups > 0) StatusError else LightTextMuted)
                            Text("$blockedGroups", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (blockedGroups > 0) StatusError else LightTextPrimary)
                        }
                    }
                }
            }
        }

        // Búsqueda y Filtro
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar sala o creador...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = LightTextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LightCardBg,
                    unfocusedContainerColor = LightCardBg
                ),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = filterBlockedOnly,
                onClick = { filterBlockedOnly = !filterBlockedOnly },
                label = { Text("Solo Bloqueadas", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusError,
                    selectedLabelColor = Color.White,
                    containerColor = LightCardBg,
                    labelColor = LightTextSecondary
                )
            )
        }

        // Lista de Grupos
        if (filteredGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = LightTextMuted, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (groups.isEmpty()) "No hay grupos privados creados aún por los usuarios."
                        else "No se encontraron salas que coincidan con la búsqueda.",
                        color = LightTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredGroups, key = { it.id }) { grp ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LightCardBg),
                        border = BorderStroke(
                            1.dp,
                            if (grp.isBlockedByDirectiva) StatusError else LightBorder
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (grp.isBlockedByDirectiva) StatusError.copy(alpha = 0.12f) else MotoOrangePrimary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, if (grp.isBlockedByDirectiva) StatusError else MotoOrangePrimary),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (grp.isBlockedByDirectiva) Icons.Default.Lock else Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = if (grp.isBlockedByDirectiva) StatusError else MotoOrangePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            grp.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = LightTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Creador: ${grp.creatorName} • ${grp.memberIds.size} miembros",
                                            fontSize = 10.sp,
                                            color = LightTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (grp.isBlockedByDirectiva) StatusError.copy(alpha = 0.12f) else StatusSuccess.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, if (grp.isBlockedByDirectiva) StatusError else StatusSuccess)
                                ) {
                                    Text(
                                        text = if (grp.isBlockedByDirectiva) "BLOQUEADO" else "ACTIVO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (grp.isBlockedByDirectiva) StatusError else StatusSuccess,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Botones de Moderación
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { groupForMembersView = grp },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Miembros", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { groupToBlockOrUnblock = grp },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (grp.isBlockedByDirectiva) StatusSuccess else StatusError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1.2f).height(32.dp)
                                ) {
                                    Icon(
                                        if (grp.isBlockedByDirectiva) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (grp.isBlockedByDirectiva) "Desbloquear" else "Bloquear",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { groupToDelete = grp },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar Sala", tint = StatusError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo Bloquear / Desbloquear Grupo
    if (groupToBlockOrUnblock != null) {
        val grp = groupToBlockOrUnblock!!
        val isCurrentlyBlocked = grp.isBlockedByDirectiva
        var reasonInput by remember { mutableStateOf(if (isCurrentlyBlocked) "" else "Incumplimiento de normativas del club") }

        AlertDialog(
            onDismissRequest = { groupToBlockOrUnblock = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCurrentlyBlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isCurrentlyBlocked) StatusSuccess else StatusError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isCurrentlyBlocked) "Desbloquear Sala" else "Bloquear Sala Privada",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LightTextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isCurrentlyBlocked)
                            "¿Deseas levantar el bloqueo a la sala \"${grp.name}\"? Los integrantes podrán interactuar de inmediato."
                        else
                            "¿Deseas bloquear la sala \"${grp.name}\"? Ningún miembro podrá enviar mensajes mientras esté bloqueada.",
                        fontSize = 13.sp,
                        color = LightTextSecondary
                    )

                    if (!isCurrentlyBlocked) {
                        OutlinedTextField(
                            value = reasonInput,
                            onValueChange = { reasonInput = it },
                            label = { Text("Motivo del Bloqueo *") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleBlockGroup(grp.id, !isCurrentlyBlocked, reasonInput)
                        groupToBlockOrUnblock = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlyBlocked) StatusSuccess else StatusError
                    )
                ) {
                    Text(if (isCurrentlyBlocked) "Confirmar Desbloqueo" else "Confirmar Bloqueo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToBlockOrUnblock = null }) {
                    Text("Cancelar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }

    // Diálogo Ver Integrantes de Grupo
    if (groupForMembersView != null) {
        val grp = groupForMembersView!!
        val membersInGroup = remember(grp.memberIds, allMembers) {
            allMembers.filter { grp.memberIds.contains(it.id) }
        }

        AlertDialog(
            onDismissRequest = { groupForMembersView = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = MotoOrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(grp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LightTextPrimary)
                        Text("${grp.memberIds.size} integrantes", fontSize = 11.sp, color = LightTextMuted)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    membersInGroup.forEach { mem ->
                        val isCreator = mem.id == grp.creatorMemberId
                        Surface(
                            color = LightCardSubtle,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PilotAvatar(member = mem, size = 32.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(mem.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (isCreator) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = DirectivaGoldBg
                                            ) {
                                                Text("CREADOR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = DirectivaGoldDark, modifier = Modifier.padding(horizontal = 3.dp))
                                            }
                                        }
                                    }
                                    Text("${mem.memberNumber} • ${mem.role.displayName}", fontSize = 10.sp, color = LightTextMuted, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { groupForMembersView = null }, colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)) {
                    Text("Cerrar", color = Color.White)
                }
            },
            containerColor = LightCardBg
        )
    }

    // Diálogo Eliminar Sala
    if (groupToDelete != null) {
        val grp = groupToDelete!!
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar Sala Privada", fontWeight = FontWeight.Bold, color = StatusError, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar permanentemente la sala \"${grp.name}\"?\n\nEsta acción eliminará el grupo y su historial.",
                    fontSize = 13.sp,
                    color = LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(grp.id)
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Eliminar Sala", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancelar", color = LightTextMuted)
                }
            },
            containerColor = LightCardBg
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// VISTA PRINCIPAL: HUB DE TARJETAS INTERACTIVAS DE DIRECTIVA
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaDashboardHubView(
    invitationCodes: List<InvitationCode>,
    accessRequests: List<AccessRequest>,
    allMembers: List<MemberProfile>,
    directivaChatMessages: List<ChatMessage>,
    allPrivateGroups: List<PrivateGroup>,
    currentMember: MemberProfile?,
    onSelectSection: (Int) -> Unit
) {
    val pendingRequestsCount = accessRequests.count { it.status == "PENDIENTE" }
    val activeCodesCount = invitationCodes.count { !it.isUsed && it.expiresAt > System.currentTimeMillis() }
    val unreadDirectivaCount = remember(directivaChatMessages, currentMember) {
        val memberId = currentMember?.id ?: 0L
        if (memberId <= 0L) 0
        else directivaChatMessages.count { it.channelId == "DIRECTIVA" && it.senderMemberId != memberId && !it.readBy.contains(memberId) }
    }
    val solventesCount = allMembers.count { it.solvencyStatus }
    val morososCount = allMembers.count { !it.solvencyStatus }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Encabezado Banner Informativo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DirectivaGoldPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = DirectivaGoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CENTRO DE GOBERNANZA TX",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = LightTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Selecciona un módulo para gestionar y monitorear las operaciones del club.",
                            fontSize = 11.sp,
                            color = LightTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Título de Sección: Gestión Institucional y Miembros
        item {
            Text(
                text = "MIEMBROS & ACCESOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = LightTextMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // 1. Solicitudes de Ingreso
        item {
            DirectivaHubCard(
                title = "Solicitudes de Ingreso",
                subtitle = "Postulaciones de aspirantes y nuevos pilotos TX",
                icon = Icons.Default.Inbox,
                iconTint = Color(0xFFD97706),
                badgeText = if (pendingRequestsCount > 0) "$pendingRequestsCount PENDIENTES" else "Al día",
                isBadgeUrgent = pendingRequestsCount > 0,
                onClick = { onSelectSection(1) }
            )
        }

        // 0. Códigos de Invitación (24H)
        item {
            DirectivaHubCard(
                title = "Códigos de Acceso (24H)",
                subtitle = "Emisión de pases temporales e invitados especiales",
                icon = Icons.Default.Key,
                iconTint = Color(0xFF2563EB),
                badgeText = "$activeCodesCount activos",
                onClick = { onSelectSection(0) }
            )
        }

        // 2. Cargos, Rangos & Disciplina
        item {
            DirectivaHubCard(
                title = "Cargos, Miembros & Disciplina",
                subtitle = "Jerarquía directiva, sanciones y suspensiones de pilotos",
                icon = Icons.Default.WorkspacePremium,
                iconTint = Color(0xFF7C3AED),
                badgeText = "${allMembers.size} miembros",
                onClick = { onSelectSection(2) }
            )
        }

        // 7. Gestor Rápido de Solvencia
        item {
            DirectivaHubCard(
                title = "Gestor Rápido de Solvencia TX",
                subtitle = "Actualización instantánea de estado de solvencia / morosidad",
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = Color(0xFF16A34A),
                badgeText = "$solventesCount Solv / $morososCount Mor",
                isBadgeUrgent = morososCount > 0,
                onClick = { onSelectSection(7) }
            )
        }

        // Título de Sección: Operaciones & Comunicación
        item {
            Text(
                text = "COMUNICACIONES & CARAVANA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = LightTextMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // 3. Chat Seguro Directiva
        item {
            DirectivaHubCard(
                title = "Canal Seguro Directiva TX",
                subtitle = "Mesa de deliberación interna confidencial de líderes",
                icon = Icons.Default.Forum,
                iconTint = Color(0xFF059669),
                badgeText = if (unreadDirectivaCount > 0) "$unreadDirectivaCount no leídos" else "Encriptado",
                isBadgeUrgent = unreadDirectivaCount > 0,
                onClick = { onSelectSection(3) }
            )
        }

        // 5. Malla Mesh & Caravana
        item {
            DirectivaHubCard(
                title = "Malla Mesh & Modo Caravana",
                subtitle = "Frecuencia de rodada, radio inter-casco y filtros de audio",
                icon = Icons.Default.Sensors,
                iconTint = Color(0xFF0284C7),
                badgeText = "Control Malla",
                onClick = { onSelectSection(5) }
            )
        }

        // 6. Comunicados Oficiales al Feed
        item {
            DirectivaHubCard(
                title = "Emisión de Comunicados Oficiales",
                subtitle = "Redactar y fijar avisos institucionales directamente en el Feed",
                icon = Icons.Default.Campaign,
                iconTint = Color(0xFFDC2626),
                badgeText = "Avisos Feed",
                onClick = { onSelectSection(6) }
            )
        }

        // 4. Salas Privadas de Pilotos
        item {
            DirectivaHubCard(
                title = "Auditoría de Salas Privadas",
                subtitle = "Supervisión y control de grupos creados por pilotos",
                icon = Icons.Default.Groups,
                iconTint = Color(0xFF475569),
                badgeText = "${allPrivateGroups.size} salas",
                onClick = { onSelectSection(4) }
            )
        }

        // Título de Sección: Seguridad & Parámetros
        item {
            Text(
                text = "SEGURIDAD & CLUB",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = LightTextMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // 8. Monitor SOS & Rescate Vial
        item {
            DirectivaHubCard(
                title = "Monitor Central SOS & Rescate Vial",
                subtitle = "Protocolos de emergencia en ruta, ficha médica y auxilio",
                icon = Icons.Default.Emergency,
                iconTint = Color(0xFFEF4444),
                badgeText = "Protocolo SOS",
                onClick = { onSelectSection(8) }
            )
        }

        // 9. Parámetros del Club
        item {
            DirectivaHubCard(
                title = "Parámetros Institucionales del Club",
                subtitle = "Lema del capítulo, cuota de membresía y políticas globales",
                icon = Icons.Default.AdminPanelSettings,
                iconTint = Color(0xFFD97706),
                badgeText = "Configuración",
                onClick = { onSelectSection(9) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// COMPONENTE: TARJETA INTERACTIVA DE HUB
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaHubCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    isBadgeUrgent: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightCardBg),
        border = BorderStroke(1.dp, LightBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = LightTextSecondary,
                    lineHeight = 14.sp
                )

                if (!badgeText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBadgeUrgent) StatusError.copy(alpha = 0.12f) else iconTint.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, if (isBadgeUrgent) StatusError.copy(alpha = 0.4f) else iconTint.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isBadgeUrgent) StatusError else iconTint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Abrir",
                tint = LightTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECCIÓN 5: GESTIÓN DE MALLA MESH Y MODO CARAVANA
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaMeshCaravanaSection(
    context: Context
) {
    val estadoConexion by GestorMeshTx.estadoConexion.collectAsState()
    val canalActual by GestorMeshTx.canalActual.collectAsState()
    val nodosEnRed by GestorMeshTx.nodosEnRed.collectAsState()
    val ajustes by GestorMeshTx.ajustes.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Estado General de la Malla Táctica
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Red Táctica Inter-Casco Mesh TX", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                        }
                        Surface(
                            shape = CircleShape,
                            color = when (estadoConexion) {
                                MeshEstadoConexion.ENLACE_DIRECTO, MeshEstadoConexion.ENMALLADO_RELAY -> StatusSuccess.copy(alpha = 0.15f)
                                MeshEstadoConexion.ESCANEANDO, MeshEstadoConexion.CONECTANDO -> DirectivaGoldPrimary.copy(alpha = 0.15f)
                                else -> LightTextMuted.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = estadoConexion.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (estadoConexion) {
                                    MeshEstadoConexion.ENLACE_DIRECTO, MeshEstadoConexion.ENMALLADO_RELAY -> StatusSuccess
                                    MeshEstadoConexion.ESCANEANDO, MeshEstadoConexion.CONECTANDO -> DirectivaGoldPrimary
                                    else -> LightTextMuted
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (estadoConexion == MeshEstadoConexion.DESCONECTADO) {
                                    GestorMeshTx.iniciarMallaTactico()
                                } else {
                                    GestorMeshTx.detenerMallaTactico()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (estadoConexion == MeshEstadoConexion.DESCONECTADO) Color(0xFF0284C7) else StatusError
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (estadoConexion == MeshEstadoConexion.DESCONECTADO) Icons.Default.PlayArrow else Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (estadoConexion == MeshEstadoConexion.DESCONECTADO) "Activar Malla" else "Desconectar Malla",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { GestorMeshTx.activarCanalRodadas() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MotoOrangePrimary),
                            border = BorderStroke(1.dp, MotoOrangePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modo Caravana", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Selección del Canal Táctico Operativo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Canal Táctico Operativo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                    Text("Establece la frecuencia virtual para rodadas o enlace cerrado de líderes.", fontSize = 11.sp, color = LightTextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))

                    CanalTactico.values().forEach { canal ->
                        val isSelected = canalActual == canal
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { GestorMeshTx.cambiarCanal(canal) },
                            color = if (isSelected) MotoOrangePrimary.copy(alpha = 0.12f) else LightCardSubtle,
                            border = BorderStroke(1.dp, if (isSelected) MotoOrangePrimary else LightBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { GestorMeshTx.cambiarCanal(canal) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MotoOrangePrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(canal.nombre, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) MotoOrangePrimary else LightTextPrimary)
                                    Text(canal.descripcion, fontSize = 10.sp, color = LightTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Parámetros de Calidad de Audio en Rodada
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Filtros de Audio para Ruta", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                    Text("Optimizaciones acústicas para intercomunicadores de casco a alta velocidad.", fontSize = 11.sp, color = LightTextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Filtro de Viento Dinámico", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightTextPrimary)
                            Text("Atenúa turbulencias en micrófono de casco (>60 km/h)", fontSize = 10.sp, color = LightTextSecondary)
                        }
                        Switch(
                            checked = ajustes.cancelacionRuidoViento,
                            onCheckedChange = { GestorMeshTx.alternarFiltroViento(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MotoOrangePrimary, checkedTrackColor = MotoOrangePrimary.copy(alpha = 0.4f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LightBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Supresión de Eco Acústico", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightTextPrimary)
                            Text("Elimina retroalimentación en altavoces de casco", fontSize = 10.sp, color = LightTextSecondary)
                        }
                        Switch(
                            checked = ajustes.supresionEcoAcustico,
                            onCheckedChange = { GestorMeshTx.alternarSupresionEco(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MotoOrangePrimary, checkedTrackColor = MotoOrangePrimary.copy(alpha = 0.4f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LightBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Búfer Anti-Entrecorte", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightTextPrimary)
                            Text("Compensa micro-pérdidas de paquetes en curvas o distancia", fontSize = 10.sp, color = LightTextSecondary)
                        }
                        Switch(
                            checked = ajustes.bufferAntiEntrecorte,
                            onCheckedChange = { GestorMeshTx.alternarBufferAntiEntrecorte(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MotoOrangePrimary, checkedTrackColor = MotoOrangePrimary.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        // Nodos y Pilotos en Malla Táctica
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pilotos en Malla (${nodosEnRed.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                        Text("Actualización en vivo", fontSize = 10.sp, color = LightTextMuted)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (nodosEnRed.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay otros pilotos conectados a la malla cercana.", fontSize = 11.sp, color = LightTextMuted)
                        }
                    } else {
                        nodosEnRed.forEach { nodo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(nodo.aliasPiloto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                                    Text("${nodo.nombreMoto} • ${nodo.modeloTelefonoHardware}", fontSize = 10.sp, color = LightTextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LightCardSubtle,
                                    border = BorderStroke(1.dp, LightBorder)
                                ) {
                                    Text("${nodo.intensidadSenalDbm} dBm", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LightTextSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECCIÓN 6: EMISIÓN DE COMUNICADOS OFICIALES AL FEED
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaComunicadosSection(
    context: Context,
    onCreateOfficialNotice: ((title: String, content: String, priority: String, isPinned: Boolean) -> Unit)?
) {
    var noticeTitle by remember { mutableStateOf("") }
    var noticeContent by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("NORMAL") }
    var isPinned by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Redactor de Comunicados Oficiales", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                            Text("Se publicará en el muro principal de todos los miembros del club.", fontSize = 11.sp, color = LightTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noticeTitle,
                        onValueChange = { noticeTitle = it },
                        label = { Text("Título del Comunicado", fontSize = 11.sp) },
                        placeholder = { Text("Ej: Directrices para la Rodada Nacional", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = LightBorder,
                            focusedContainerColor = LightCardBg,
                            unfocusedContainerColor = LightCardSubtle
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = noticeContent,
                        onValueChange = { noticeContent = it },
                        label = { Text("Contenido / Resolución de la Directiva", fontSize = 11.sp) },
                        placeholder = { Text("Escribe aquí el cuerpo del comunicado oficial...", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = LightBorder,
                            focusedContainerColor = LightCardBg,
                            unfocusedContainerColor = LightCardSubtle
                        ),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Nivel de Prioridad del Comunicado:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("NORMAL", "IMPORTANTE", "URGENTE").forEach { prio ->
                            val isSelected = selectedPriority == prio
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedPriority = prio },
                                color = if (isSelected) {
                                    if (prio == "URGENTE") StatusError.copy(alpha = 0.15f) else MotoOrangePrimary.copy(alpha = 0.15f)
                                } else LightCardSubtle,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        if (prio == "URGENTE") StatusError else MotoOrangePrimary
                                    } else LightBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prio,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) {
                                            if (prio == "URGENTE") StatusError else MotoOrangePrimary
                                        } else LightTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fijar al inicio del Feed (Pinned)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightTextPrimary)
                            Text("Permanecerá en la parte superior para lectura obligatoria", fontSize = 10.sp, color = LightTextSecondary)
                        }
                        Switch(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFDC2626), checkedTrackColor = Color(0xFFDC2626).copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (noticeTitle.trim().isBlank() || noticeContent.trim().isBlank()) {
                                Toast.makeText(context, "Ingresa el título y contenido del comunicado", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            if (onCreateOfficialNotice != null) {
                                onCreateOfficialNotice(noticeTitle.trim(), noticeContent.trim(), selectedPriority, isPinned)
                                Toast.makeText(context, "📢 Comunicado oficial emitido con éxito", Toast.LENGTH_LONG).show()
                                noticeTitle = ""
                                noticeContent = ""
                            } else {
                                Toast.makeText(context, "Comunicado registrado en el sistema", Toast.LENGTH_SHORT).show()
                            }
                            isSubmitting = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emitir Comunicado Oficial TX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECCIÓN 7: GESTOR RÁPIDO DE SOLVENCIA TX
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaSolvenciaRapidaSection(
    allMembers: List<MemberProfile>,
    onToggleSolvency: (MemberProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("TODOS") } // "TODOS", "SOLVENTES", "MOROSOS"

    val solventesCount = allMembers.count { it.solvencyStatus }
    val morososCount = allMembers.count { !it.solvencyStatus }

    val filteredMembers = remember(allMembers, searchQuery, filterMode) {
        allMembers.filter { member ->
            val matchesQuery = searchQuery.isBlank() ||
                    member.fullName.contains(searchQuery, ignoreCase = true) ||
                    member.bikeBrand.contains(searchQuery, ignoreCase = true) ||
                    member.bikeModel.contains(searchQuery, ignoreCase = true) ||
                    member.bloodType.contains(searchQuery, ignoreCase = true) ||
                    member.medicalNotes.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterMode) {
                "SOLVENTES" -> member.solvencyStatus
                "MOROSOS" -> !member.solvencyStatus
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Tarjetas métricas de solvencia
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$solventesCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = StatusSuccess)
                    Text("SOLVENTES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LightTextSecondary)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$morososCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = StatusError)
                    Text("MOROSOS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LightTextSecondary)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${allMembers.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = LightTextPrimary)
                    Text("TOTAL PILOTOS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LightTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buscador
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar piloto por nombre, moto...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = LightTextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MotoOrangePrimary,
                unfocusedBorderColor = LightBorder,
                focusedContainerColor = LightCardBg,
                unfocusedContainerColor = LightCardSubtle
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pestañas filtro: Todos / Solventes / Morosos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("TODOS", "SOLVENTES", "MOROSOS").forEach { mode ->
                val isSelected = filterMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { filterMode = mode },
                    color = if (isSelected) MotoOrangePrimary else LightCardBg,
                    border = BorderStroke(1.dp, if (isSelected) MotoOrangePrimary else LightBorder)
                ) {
                    Box(modifier = Modifier.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = mode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else LightTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de Miembros con conmutador de solvencia
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredMembers, key = { it.id }) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = LightCardBg),
                    border = BorderStroke(1.dp, if (member.solvencyStatus) StatusSuccess.copy(alpha = 0.3f) else StatusError.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PilotAvatar(
                            member = member,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.fullName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${member.role.displayName} • ${member.bikeBrand} ${member.bikeModel}",
                                fontSize = 10.sp,
                                color = LightTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón de alternancia de solvencia
                        Button(
                            onClick = { onToggleSolvency(member) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (member.solvencyStatus) StatusSuccess else StatusError
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (member.solvencyStatus) "✓ SOLVENTE" else "✕ MOROSO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECCIÓN 8: MONITOR CENTRAL SOS & RESCATE VIAL
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaSosMonitorSection(
    context: Context,
    allMembers: List<MemberProfile>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner Central de Emergencias
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Monitor Central SOS & Protocolo Vial", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                            Text("Respuesta inmediata de líderes ante contingencias en ruta.", fontSize = 11.sp, color = LightTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            GestorMeshTx.emitirAlertaSos("🚨 ALERTA DIRECTIVA: Verificación de caravana activa en curso", null)
                            Toast.makeText(context, "Baliza de alerta emitida por Malla Mesh TX", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emitir Baliza de Control a la Caravana", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Directorio de Rescate y Emergencias Oficiales
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Líneas de Auxilio y Cuerpos de Seguridad", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                    Text("Acceso de discado directo de emergencia para los directivos.", fontSize = 11.sp, color = LightTextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))

                    listOf(
                        Triple("Emergencias Nacional VEN 911", "911", Icons.Default.LocalPolice),
                        Triple("Bomberos de Aragua (Rescate)", "02432470123", Icons.Default.FireTruck),
                        Triple("Tránsito Terrestre CPNB", "08007654242", Icons.Default.CarCrash)
                    ).forEach { (label, phone, icon) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                                    context.startActivity(dialIntent)
                                },
                            color = LightCardSubtle,
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                                    Text("Tel: $phone", fontSize = 10.sp, color = LightTextSecondary)
                                }
                                Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Ficha Rápida Médica de Pilotos Registrados
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Historial de Salud de Pilotos (${allMembers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                    Text("Datos críticos para paramédicos y atención en carretera.", fontSize = 11.sp, color = LightTextSecondary)

                    Spacer(modifier = Modifier.height(8.dp))

                    allMembers.take(8).forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.fullName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                                Text(
                                    text = "Sangre: ${member.bloodType.ifBlank { "No especificado" }} | Notas: ${member.medicalNotes.ifBlank { "Ninguna" }}",
                                    fontSize = 10.sp,
                                    color = LightTextSecondary
                                )
                            }
                            if (member.emergencyContactPhone.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:${member.emergencyContactPhone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContactPhone, contentDescription = "Contacto", tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = LightBorder)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECCIÓN 9: PARÁMETROS INSTITUCIONALES DEL CLUB
// ═══════════════════════════════════════════════════════════════
@Composable
fun DirectivaConfiguracionClubSection(
    context: Context
) {
    PreferenciasApp.init(context)
    var lemaClubInput by remember { mutableStateOf(PreferenciasApp.lemaClub) }
    var cuotaRefInput by remember { mutableStateOf(PreferenciasApp.cuotaMembresiaRef) }
    var soloDirectivaFeed by remember { mutableStateOf(PreferenciasApp.soloDirectivaPublicaFeed) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightCardBg),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DirectivaGoldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Parámetros Institucionales del Capítulo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightTextPrimary)
                            Text("Ajustes generales que aplican a toda la aplicación.", fontSize = 11.sp, color = LightTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = lemaClubInput,
                        onValueChange = { lemaClubInput = it },
                        label = { Text("Lema Oficial del Club", fontSize = 11.sp) },
                        placeholder = { Text("Ej: Hermandad, Ruta y Respeto", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DirectivaGoldPrimary,
                            unfocusedBorderColor = LightBorder,
                            focusedContainerColor = LightCardBg,
                            unfocusedContainerColor = LightCardSubtle
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cuotaRefInput,
                        onValueChange = { cuotaRefInput = it },
                        label = { Text("Cuota Referencial de Membresía", fontSize = 11.sp) },
                        placeholder = { Text("Ej: 5 USD / Mensual", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DirectivaGoldPrimary,
                            unfocusedBorderColor = LightBorder,
                            focusedContainerColor = LightCardBg,
                            unfocusedContainerColor = LightCardSubtle
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Feed Solo Directiva", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightTextPrimary)
                            Text("Si se activa, únicamente los directivos podrán crear posts en el feed", fontSize = 10.sp, color = LightTextSecondary)
                        }
                        Switch(
                            checked = soloDirectivaFeed,
                            onCheckedChange = { soloDirectivaFeed = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            PreferenciasApp.lemaClub = lemaClubInput.trim()
                            PreferenciasApp.cuotaMembresiaRef = cuotaRefInput.trim()
                            PreferenciasApp.soloDirectivaPublicaFeed = soloDirectivaFeed
                            Toast.makeText(context, "✅ Parámetros del club guardados correctamente", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DirectivaGoldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Parámetros Directiva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

