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
    onGenerateCode: (targetRole: MemberRole, note: String, isSpecialGuest: Boolean, durationHours: Int) -> String,
    onDeleteCode: (InvitationCode) -> Unit,
    onDarDeBajaInvitado: (MemberProfile) -> Unit = {},
    onApproveRequest: (AccessRequest) -> String,
    onRejectRequest: (AccessRequest) -> Unit,
    onTransferCargo: (fromMember: MemberProfile, toMember: MemberProfile, cargo: MemberRole) -> Unit,
    onAbandonCargo: (MemberProfile) -> Unit,
    onAssignRole: (MemberProfile, MemberRole) -> Unit,
    onSendDirectivaChatMessage: (String) -> Unit,
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
    onMarkChatMessageAsRead: (ChatMessage) -> Unit = {},
    onMarkChannelAsRead: (String) -> Unit = {},
    onSendSticker: (String, java.io.File) -> Unit = { _, _ -> },
    onToggleChatMute: (MemberProfile, Boolean, String) -> Unit = { _, _, _ -> },
    onSuspendMember: (MemberProfile, String, Int) -> Unit = { _, _, _ -> },
    onReactivateMember: (MemberProfile) -> Unit = {},
    onToggleSolvency: (MemberProfile) -> Unit = {},
    onToggleBottomNav: (() -> Unit)? = null,
    allPrivateGroups: List<PrivateGroup> = emptyList(),
    onToggleBlockPrivateGroup: (groupId: String, isBlocked: Boolean, reason: String) -> Unit = { _, _, _ -> },
    onDeletePrivateGroup: (groupId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hasDirectivaAccess = isDirectivaMode || isLeaderSuperAdmin || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true)

    // Sub-tab selection: 0 = Codigos, 1 = Solicitudes, 2 = Cargos, 3 = Chat Directiva, 4 = Salas Privadas
    var selectedSection by remember { mutableStateOf(0) }

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
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isLeaderSuperAdmin) DirectivaGoldBg else MotoOrangePrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (isLeaderSuperAdmin) DirectivaGoldPrimary else MotoOrangePrimary)
                                ) {
                                    Text(
                                        text = if (isLeaderSuperAdmin) "👑 LÍDER SUPREMO" else "🛡️ DIRECTIVO",
                                        color = if (isLeaderSuperAdmin) DirectivaGoldDark else MotoOrangePrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

                        // ── Pestañas de Navegación del Panel (Tabs) ──
                        ScrollableTabRow(
                            selectedTabIndex = selectedSection,
                            containerColor = LightCardBg,
                            contentColor = MotoOrangePrimary,
                            edgePadding = 0.dp,
                            divider = { HorizontalDivider(color = LightBorder) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            Tab(
                                selected = selectedSection == 0,
                                onClick = { selectedSection = 0 },
                                text = { Text("CÓDIGOS (24H)", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                selectedContentColor = MotoOrangePrimary,
                                unselectedContentColor = LightTextMuted
                            )
                            Tab(
                                selected = selectedSection == 1,
                                onClick = { selectedSection = 1 },
                                text = {
                                    val pendingCount = accessRequests.count { it.status == "PENDIENTE" }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("SOLICITUDES", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        if (pendingCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = StatusError,
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("$pendingCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                selectedContentColor = MotoOrangePrimary,
                                unselectedContentColor = LightTextMuted
                            )
                            Tab(
                                selected = selectedSection == 2,
                                onClick = { selectedSection = 2 },
                                text = { Text("CARGOS & MIEMBROS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                selectedContentColor = MotoOrangePrimary,
                                unselectedContentColor = LightTextMuted
                            )
                            Tab(
                                selected = selectedSection == 3,
                                onClick = {
                                    selectedSection = 3
                                    onMarkChannelAsRead("DIRECTIVA")
                                },
                                text = {
                                    val unreadDirectiva = remember(directivaChatMessages, currentMember) {
                                        val memberId = currentMember?.id ?: 0L
                                        if (memberId <= 0L) 0
                                        else directivaChatMessages.count { it.channelId == "DIRECTIVA" && it.senderMemberId != memberId && !it.readBy.contains(memberId) }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("CHAT DIRECTIVA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        if (unreadDirectiva > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = StatusError,
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(if (unreadDirectiva > 99) "99+" else "$unreadDirectiva", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                selectedContentColor = MotoOrangePrimary,
                                unselectedContentColor = LightTextMuted
                            )
                            Tab(
                                selected = selectedSection == 4,
                                onClick = { selectedSection = 4 },
                                text = {
                                    val totalCount = allPrivateGroups.size
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("SALAS PRIVADAS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        if (totalCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = LightBorderHover,
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("$totalCount", color = LightTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                selectedContentColor = MotoOrangePrimary,
                                unselectedContentColor = LightTextMuted
                            )
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

                // ── Contenido de la Sub-Sección Activa ──
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedSection) {
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
                        3 -> DirectivaChatSection(
                            messages = directivaChatMessages,
                            currentMember = currentMember,
                            onSendMessage = onSendDirectivaChatMessage
                        )
                        4 -> DirectivaPrivateGroupsSection(
                            groups = allPrivateGroups,
                            allMembers = allMembers,
                            onToggleBlockGroup = onToggleBlockPrivateGroup,
                            onDeleteGroup = onDeletePrivateGroup
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

                Card(
                    colors = CardDefaults.cardColors(containerColor = LightCardBg),
                    border = BorderStroke(
                        1.dp,
                        if (code.isUsed) StatusSuccess
                        else if (isExpired) StatusError
                        else MotoOrangePrimary
                    ),
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
                            Text(
                                text = code.code,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isExpired) LightTextMuted else LightTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )

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
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Rol asignado: ${code.targetRole.displayName} • Creador: ${code.createdBy}",
                            fontSize = 11.sp,
                            color = LightTextSecondary
                        )
                        if (code.note.isNotBlank()) {
                            Text(text = "Nota: ${code.note}", fontSize = 11.sp, color = DirectivaGoldDark, fontWeight = FontWeight.Medium)
                        }

                        // Acciones: Copiar, Compartir WhatsApp, Eliminar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Código TX", code.code))
                                    Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = LightTextSecondary)
                            }

                            IconButton(
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
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = WhatsAppGreen)
                            }

                            IconButton(onClick = { onDeleteCode(code) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusError)
                            }
                        }
                    }
                }
            }
        }
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
    onOpenSuspendDialog: (MemberProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("TODOS") }

    val filterOptions = listOf("TODOS", "DIRECTIVA", "ACTIVOS", "SILENCIADOS", "SUSPENDIDOS", "INSOLVENTES")

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
                        text = "Control en tiempo real de solvencias, silencios de chat, sanciones disciplinarias y cargos institucionales.",
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
                    placeholder = { Text("Buscar por nombre, placa, carnet o capítulo...", fontSize = 12.sp) },
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
                                selectedContainerColor = MotoOrangePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = LightCardBg,
                                labelColor = LightTextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Lista de Miembros
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
                                modifier = Modifier.weight(0.9f).height(32.dp)
                            ) {
                                Text(if (member.isChatMuted) "Desmutear" else "Silenciar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (member.isSuspended) onReactivateMember(member)
                                    else onOpenSuspendDialog(member)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (member.isSuspended) StatusSuccess else StatusError
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.weight(0.9f).height(32.dp)
                            ) {
                                Text(if (member.isSuspended) "Reactivar" else "Suspender", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
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
