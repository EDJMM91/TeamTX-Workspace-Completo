package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.aistudio.teamtxvzla.BuildConfig
import com.aistudio.teamtxvzla.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.chat.GestorStickers
import com.example.data.model.*
import com.example.ui.components.ClubTopBar
import com.example.ui.preferences.PreferenciasApp
import com.example.ui.screens.*
import com.example.ui.screens.dialogs.EmitSosDialog
import com.example.ui.screens.permissions.PermissionHandler
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusError
import com.example.ui.theme.TxFlameRed
import com.example.ui.viewmodel.TeamTxViewModel
import com.example.GestorNotificacionesApp
import com.aistudio.teamtxvzla.nube.NubeMultimedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

enum class NavigationTab(val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector, val tag: String) {
    FEED("Muro", Icons.Default.Campaign, Icons.Outlined.Campaign, "tab_feed"),
    CHAT("Chat", Icons.Default.Forum, Icons.Outlined.Forum, "tab_chat"),
    NOTIFICACIONES("Avisos", Icons.Default.Notifications, Icons.Outlined.Notifications, "tab_notificaciones"),
    CALENDARIO("Calendario", Icons.Default.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendario"),
    RETOS("Retos", Icons.Default.EmojiEvents, Icons.Outlined.EmojiEvents, "tab_retos"),
    VELOCIMETRO("Velocímetro", Icons.Default.Speed, Icons.Outlined.Speed, "tab_velocimetro"),
    RIDES("Rodadas", Icons.Default.TwoWheeler, Icons.Outlined.TwoWheeler, "tab_rides"),
    MERCADO("Mercado", Icons.Default.Storefront, Icons.Outlined.Storefront, "tab_mercado"),
    BITACORA("Bitácora", Icons.Default.Build, Icons.Outlined.Build, "tab_bitacora"),
    DIRECTORIO("Servicios", Icons.Default.Storefront, Icons.Outlined.Storefront, "tab_directorio"),
    PASAPORTE("Pasaporte", Icons.Default.Explore, Icons.Outlined.Explore, "tab_pasaporte"),
    MEMBERS("Miembros", Icons.Default.Groups, Icons.Outlined.Groups, "tab_members"),
    FINANCES("Tesorería", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_finances"),
    INVENTORY("Inventario", Icons.Default.Handyman, Icons.Outlined.Handyman, "tab_inventory"),
    SOS("SOS Vial", Icons.Default.Emergency, Icons.Outlined.Emergency, "tab_sos"),
    NORMATIVAS("Normativas", Icons.Default.Gavel, Icons.Outlined.Gavel, "tab_normativas"),
    DIRECTIVA("Directiva", Icons.Default.Shield, Icons.Outlined.Shield, "tab_directiva"),
    PROFILE("Carnet TX", Icons.Default.Badge, Icons.Outlined.Badge, "tab_profile"),
    MAPA("Mapa TX", Icons.Default.Map, Icons.Outlined.Map, "tab_mapa"),
    INFO("Info", Icons.Default.Info, Icons.Outlined.Info, "tab_info"),
    CONFIGURACIONES("Ajustes", Icons.Default.Settings, Icons.Outlined.Settings, "tab_configuraciones")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Imprimir versión en log para depuración
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        Log.d("AppVersion", "Versión: $versionName (Código: $versionCode)")
        
        // Inicializar GestorStickers para el selector de archivos WebP
        GestorStickers.inicializarLauncher(this)
        
        // Inicializar Nube Multimedia (Supabase Storage) - stickers, avisos, perfiles, flyers, integrantes
        NubeMultimedia.inicializar(applicationContext)
        
        // Inicializar preferencias persistentes de la app
        PreferenciasApp.init(applicationContext)

        // 📁 Inicializar estructura organizada de carpetas locales en Español
        com.example.util.GestorCarpetasApp.inicializarCarpetas(applicationContext)

        // Inicializar preferencias del chat (sonidos, notificaciones, tema)
        com.example.chat.PreferenciasChat.inicializar(applicationContext)

        // 🛡️ Ciclo de Vida: Limpieza periódica de audios antiguos en Firebase Storage (>30 días)
        com.example.chat.GestorAudio.ejecutarLimpiezaAudiosAntiguos(applicationContext, diasRetencion = 30)

        // Obtener token FCM y guardarlo en Firestore
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM_INICIO", "Token FCM: $token")
                    // Guardar en Firestore si hay usuario autenticado
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null && token != null) {
                        val datos = mapOf(
                            "fcmToken" to token,
                            "fcmTokenTimestamp" to System.currentTimeMillis(),
                            "deviceId" to android.os.Build.MODEL,
                            "deviceBrand" to android.os.Build.BRAND,
                            "androidVersion" to android.os.Build.VERSION.RELEASE
                        )
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("fcm_tokens")
                            .document(uid)
                            .set(datos)
                            .addOnSuccessListener { Log.d("FCM_INICIO", "Token guardado en Firestore") }
                            .addOnFailureListener { e -> Log.e("FCM_INICIO", "Error guardando token", e) }
                    }
                } else {
                    Log.w("FCM_INICIO", "Error obteniendo token FCM", task.exception)
                }
            }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppEntryPoint()
            }
        }
    }
}

@Composable
fun AppEntryPoint(
    viewModel: TeamTxViewModel = viewModel()
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val attemptsLeft by viewModel.requestAttemptsLeft.collectAsStateWithLifecycle()
    
    // 🛡️ Usamos rememberSaveable para que el Splash no se repita al girar o recrear la actividad
    var showSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    var permissionsGranted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Manejo automático de notificaciones
    SolicitadorNotificaciones()
    
    // Verificación de actualizaciones OTA
    VerificadorOta()

    if (showSplash) {
        SplashScreen(onComplete = { showSplash = false })
    } else if (!permissionsGranted) {
        PermissionHandler(onAllPermissionsGranted = { permissionsGranted = true }, scope = scope)
    } else if (!isAuthenticated) {
        VistaLogin(
            onRequestCodeLogin = { code -> viewModel.loginWithCode(code) },
            onSubmitAccessRequest = { name, phone, dni, brand, model, color, plate, chapter, reason, birthDate, role ->
                viewModel.submitAccessRequest(name, phone, dni, brand, model, color, plate, chapter, reason, birthDate, role)
            },
            attemptsLeft = attemptsLeft
        )
    } else {
        val currentMember by viewModel.currentMember.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var skipOnboarding by remember { mutableStateOf(false) }

        // 🛡️ Si el perfil está incompleto, mostrar notificación (no bloquear)
        LaunchedEffect(currentMember) {
            if (currentMember != null && !currentMember!!.isProfileComplete) {
                notificarPerfilIncompleto(context)
            }
        }

        LaunchedEffect(currentMember?.isSuspended) {
            if (currentMember != null && currentMember!!.isSuspended) {
                val reason = currentMember!!.suspensionReason.ifBlank { "Acceso temporal finalizado o revocado por la Directiva." }
                Toast.makeText(context, "Acceso Revocado: $reason", Toast.LENGTH_LONG).show()
                viewModel.logout()
            }
        }

        MainAppScreen(viewModel = viewModel)
    }
}

@Composable
fun MainAppScreen(viewModel: TeamTxViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NavigationTab.FEED) }
    val isDirectivaMode by viewModel.isDirectivaMode.collectAsStateWithLifecycle()
    val isLeaderSuperAdmin by viewModel.isLeaderSuperAdmin.collectAsStateWithLifecycle()
    val currentMember by viewModel.currentMember.collectAsStateWithLifecycle()
    val allMembers by viewModel.allMembers.collectAsStateWithLifecycle()

    val publications by viewModel.publications.collectAsStateWithLifecycle()
    val isRefreshingFeed by viewModel.isRefreshingFeed.collectAsStateWithLifecycle()
    val rides by viewModel.rides.collectAsStateWithLifecycle()
    val registrations by viewModel.registrations.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val financialSummary by viewModel.financialSummary.collectAsStateWithLifecycle()
    val bcvRate by viewModel.bcvRate.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val equipmentLoans by viewModel.equipmentLoans.collectAsStateWithLifecycle()
    val emergencyAlerts by viewModel.emergencyAlerts.collectAsStateWithLifecycle()
    val noticeComments by viewModel.allNoticeComments.collectAsStateWithLifecycle()
    val chatMessages by viewModel.allChatMessages.collectAsStateWithLifecycle()
    val isChatEnabled by viewModel.isChatEnabled.collectAsStateWithLifecycle()
    val selectedChatChannel by viewModel.selectedChatChannel.collectAsStateWithLifecycle()
    val roleConfigs by viewModel.roleConfigs.collectAsStateWithLifecycle()

    val unreadPublicChatCount by viewModel.unreadPublicChatCount.collectAsStateWithLifecycle()
    val unreadDirectivaChatCount by viewModel.unreadDirectivaChatCount.collectAsStateWithLifecycle()
    val directivaChatMessages by viewModel.directivaChatMessages.collectAsStateWithLifecycle()
    val accessRequests by viewModel.allAccessRequests.collectAsStateWithLifecycle()
    val allPrivateGroups by viewModel.allPrivateGroups.collectAsStateWithLifecycle()
    val myPrivateGroups by viewModel.myPrivateGroups.collectAsStateWithLifecycle()
    val myCreatedGroupsCount by viewModel.myCreatedGroupsCount.collectAsStateWithLifecycle()

    val marketplaceItems by viewModel.marketplaceItems.collectAsStateWithLifecycle()
    val myMaintenanceLogs by viewModel.myMaintenanceLogs.collectAsStateWithLifecycle()
    val allWorkshops by viewModel.allWorkshops.collectAsStateWithLifecycle()
    val passportDestinations by viewModel.passportDestinations.collectAsStateWithLifecycle()
    val myPassportStamps by viewModel.myPassportStamps.collectAsStateWithLifecycle()
    val challenges by viewModel.challenges.collectAsStateWithLifecycle()
    val allChallengeProgress by viewModel.allChallengeProgress.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()

    val notificacionesNoLeidas by GestorNotificacionesApp.obtenerCantidadNoLeidas()
        ?.collectAsState(initial = 0)
        ?: remember { mutableStateOf(0) }

    var showQuickSosModal by remember { mutableStateOf(false) }
    var isBottomNavVisible by remember { mutableStateOf(true) }

    // Auto-hide bottom nav when entering chat, directiva, or velocímetro
    LaunchedEffect(selectedTab) {
        if (selectedTab == NavigationTab.CHAT || selectedTab == NavigationTab.DIRECTIVA || selectedTab == NavigationTab.VELOCIMETRO) {
            isBottomNavVisible = false
        } else {
            isBottomNavVisible = true
        }
    }

    val showTopBar = selectedTab == NavigationTab.FEED

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            if (showTopBar) {
                ClubTopBar(
                    currentMember = currentMember,
                    isDirectivaMode = isDirectivaMode,
                    onToggleDirectiva = { viewModel.toggleDirectivaMode() },
                    onOpenSosModal = { showQuickSosModal = true },
                    isDeveloperMode = currentMember?.role == MemberRole.PRESIDENTE && isDirectivaMode
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isBottomNavVisible) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(NavigationTab.values()) { tab ->
                            val isSelected = selectedTab == tab
                            val isSos = tab == NavigationTab.SOS
                            val isNotificaciones = tab == NavigationTab.NOTIFICACIONES
                            val isChat = tab == NavigationTab.CHAT
                            val isDirectiva = tab == NavigationTab.DIRECTIVA
                            val hasDirectivaAccess = isDirectivaMode || isLeaderSuperAdmin || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true)
                            val pendingRequestsCount = accessRequests.count { it.status == "PENDIENTE" }
                            val directivaBadgeCount = if (hasDirectivaAccess) unreadDirectivaChatCount + pendingRequestsCount else 0
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedTab = tab }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .widthIn(min = 66.dp)
                                    .testTag(tab.tag)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (isSos && emergencyAlerts.any { it.status == com.example.data.model.EmergencyStatus.ACTIVA }) {
                                                    Badge(containerColor = StatusError)
                                                } else if (isNotificaciones && notificacionesNoLeidas > 0) {
                                                    Badge(containerColor = StatusError) {
                                                        Text(
                                                            text = if (notificacionesNoLeidas > 99) "99+" else "$notificacionesNoLeidas",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                } else if (isChat && unreadPublicChatCount > 0) {
                                                    Badge(containerColor = StatusError) {
                                                        Text(
                                                            text = if (unreadPublicChatCount > 99) "99+" else "$unreadPublicChatCount",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                } else if (isDirectiva && directivaBadgeCount > 0) {
                                                    Badge(containerColor = StatusError) {
                                                        Text(
                                                            text = if (directivaBadgeCount > 99) "99+" else "$directivaBadgeCount",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
                                                contentDescription = tab.label,
                                                tint = when {
                                                    isSos -> StatusError
                                                    isSelected -> TxFlameRed
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        isSos && isSelected -> StatusError
                                        isSos -> StatusError.copy(alpha = 0.8f)
                                        isSelected -> TxFlameRed
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                NavigationTab.FEED -> {
                    val uploadError by viewModel.publicationUploadError.collectAsStateWithLifecycle()
                    val uploadSuccess by viewModel.publicationUploadSuccess.collectAsStateWithLifecycle()
                    
                    FeedScreen(
                        publications = publications,
                        comments = noticeComments,
                        currentMember = currentMember,
                        allMembers = allMembers,
                        isDirectivaMode = isDirectivaMode,
                        onLike = { viewModel.likePublication(it) },
                        onDelete = { viewModel.deletePublication(it) },
                        onUpdatePublication = { pub, uri -> viewModel.updatePublication(pub, uri) },
                        onAddComment = { pubId, content -> viewModel.addNoticeComment(pubId, content) },
                        onCreatePublication = { title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments ->
                            viewModel.createPublication(title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments)
                        },
                        isRefreshing = isRefreshingFeed,
                        onRefresh = { viewModel.refreshFeed() },
                        uploadError = uploadError,
                        uploadSuccess = uploadSuccess,
                        onDismissUploadStatus = { viewModel.clearPublicationUploadStatus() }
                    )
                }
                NavigationTab.CHAT -> {
                    ClubChatScreen(
                        messages = chatMessages,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        roleConfigs = roleConfigs,
                        activeChannelId = selectedChatChannel,
                        isChatEnabled = isChatEnabled,
                        onSelectChannel = { viewModel.selectChatChannel(it) },
                        onSendMessage = { ch, text, isRadio, replyId, replySender, replyText ->
                            viewModel.sendChatMessage(ch, text, isRadio, replyId, replySender, replyText)
                        },
                        onSendSticker = { ch, file ->
                            viewModel.sendStickerMessage(ch, file)
                        },
                        onSendAudio = { ch, file, dur, transcription ->
                            viewModel.sendAudioMessage(ch, file, dur, transcription)
                        },
                        onToggleReaction = { msgId, emoji ->
                            viewModel.toggleMessageReaction(msgId, emoji)
                        },
                        onDeleteMessage = { viewModel.deleteChatMessage(it) },
                        onDeleteMessageForMe = { viewModel.deleteChatMessageForMe(it) },
                        onMarkMessageAsRead = { viewModel.markMessageAsRead(it) },
                        onMarkChannelAsRead = { viewModel.markChannelMessagesAsRead(it) },
                        onToggleBottomNav = { isBottomNavVisible = !isBottomNavVisible },
                        onBack = { selectedTab = NavigationTab.FEED },
                        privateGroups = myPrivateGroups,
                        allMembers = allMembers,
                        myCreatedGroupsCount = myCreatedGroupsCount,
                        onCreatePrivateGroup = { name, desc, memberIds, callback ->
                            viewModel.createPrivateGroup(name, desc, memberIds, callback)
                        },
                        onAddMembersToGroup = { groupId, memberIds ->
                            viewModel.addMembersToPrivateGroup(groupId, memberIds)
                        },
                        onRemoveMemberFromGroup = { groupId, memberId ->
                            viewModel.removeMemberFromPrivateGroup(groupId, memberId)
                        },
                        onLeaveGroup = { groupId ->
                            viewModel.leavePrivateGroup(groupId)
                        },
                        onDeleteGroup = { groupId ->
                            viewModel.deletePrivateGroup(groupId)
                        }
                    )
                }
                NavigationTab.NOTIFICACIONES -> {
                    VistaNotificaciones(
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.CALENDARIO -> {
                    CalendarScreen(
                        events = calendarEvents,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        onCreateEvent = { title, desc, cat, vis, date, eTime, dTime, orig, dest, oLat, oLng, dLat, dLng, terrain, diff, weather, capt, tail, remindDays, isOfficial, flyerUri, onComplete ->
                            viewModel.createCalendarEvent(title, desc, cat, vis, date, eTime, dTime, orig, dest, oLat, oLng, dLat, dLng, terrain, diff, weather, capt, tail, remindDays, isOfficial, flyerUri, onComplete)
                        },
                        onUpdateEvent = { event, flyerUri, onComplete ->
                            viewModel.updateCalendarEvent(event, flyerUri, onComplete)
                        },
                        onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                        onToggleRsvp = { event, isAttending, hasPillion ->
                            viewModel.toggleEventRsvp(event, isAttending, hasPillion)
                        },
                        onToggleReminder = { event, notifyInApp ->
                            viewModel.toggleEventReminder(event, notifyInApp)
                        },
                        onPublishToFeed = { event, onComplete ->
                            viewModel.publishCalendarEventToFeed(event, onComplete)
                        },
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.RETOS -> {
                    ChallengesScreen(
                        challenges = challenges,
                        progressList = allChallengeProgress.filter { currentMember == null || it.memberId == currentMember!!.id },
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        onCreateChallenge = { title, desc, targetKm, speed, badge, start, end, checkpoints, isOfficial, imgUri, onComplete ->
                            viewModel.createChallenge(title, desc, targetKm, speed, badge, start, end, checkpoints, isOfficial, imgUri, onComplete)
                        },
                        onUpdateChallenge = { challenge, newImageUri, onComplete ->
                            viewModel.updateChallenge(challenge, newImageUri, onComplete)
                        },
                        onDeleteChallenge = { viewModel.deleteChallenge(it) },
                        onSaveProgress = { prog, photoUri, onComplete ->
                            viewModel.saveChallengeProgress(prog, photoUri, onComplete)
                        },
                        onDeleteProgress = { viewModel.deleteChallengeProgress(it) },
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.VELOCIMETRO -> {
                    VelocimetroScreen(
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.MERCADO -> {
                    MarketplaceScreen(
                        items = marketplaceItems,
                        currentMember = currentMember,
                        bcvRate = bcvRate,
                        onCreateItem = { title, desc, cat, price, cond, uri, ph, loc, onComplete ->
                            viewModel.createMarketplaceItem(title, desc, cat, price, cond, uri, ph, loc, onComplete)
                        },
                        onUpdateStatus = { item, status ->
                            viewModel.updateMarketplaceItemStatus(item, status)
                        },
                        onDeleteItem = { viewModel.deleteMarketplaceItem(it) },
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.BITACORA -> {
                    MaintenanceScreen(
                        logs = myMaintenanceLogs,
                        currentMember = currentMember,
                        bcvRate = bcvRate,
                        onCreateLog = { odo, type, brand, cost, workshop, date, nextKm, notes ->
                            viewModel.createMaintenanceLog(odo, type, brand, cost, workshop, date, nextKm, notes)
                        },
                        onDeleteLog = { viewModel.deleteMaintenanceLog(it) },
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.DIRECTORIO -> {
                    WorkshopDirectoryScreen(
                        workshops = allWorkshops,
                        currentMember = currentMember,
                        onCreateWorkshop = { name, type, state, city, addr, ph, wa, rat, notes, lat, lng ->
                            viewModel.createWorkshop(name, type, state, city, addr, ph, wa, rat, notes, lat, lng)
                        },
                        onDeleteWorkshop = { viewModel.deleteWorkshop(it) },
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.PASAPORTE -> {
                    PassportScreen(
                        destinations = passportDestinations,
                        myStamps = myPassportStamps,
                        currentMember = currentMember,
                        onStampDestination = { dest, uri, date, onComplete ->
                            viewModel.stampPassportDestination(dest, uri, date, onComplete)
                        },
                        onDeleteStamp = { viewModel.deletePassportStamp(it) },
                        onBack = { selectedTab = NavigationTab.FEED }
                    )
                }
                NavigationTab.RIDES -> {
                    RidesScreen(
                        rides = rides,
                        registrations = registrations,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        onJoinRide = { rideId, hasPillion, pillionName ->
                            viewModel.joinRide(rideId, hasPillion, pillionName)
                        },
                        onCancelJoin = { rideId ->
                            viewModel.cancelRideJoin(rideId)
                        },
                        onUpdateRideStatus = { ride, status ->
                            viewModel.updateRideStatus(ride, status)
                        },
                        onUpdateRide = { updatedRide ->
                            viewModel.updateRide(updatedRide)
                        },
                        onDeleteRide = { rideId ->
                            viewModel.deleteRide(rideId)
                        },
                        onFinalizeRide = { ride, attendedMemberIds ->
                            viewModel.finalizeRideAndRegisterAttendance(ride, attendedMemberIds)
                        },
                        onUpdateConvoyRole = { regId, newRole ->
                            viewModel.updateRegistrationConvoyRole(regId, newRole)
                        },
                        onCreateRide = { title, desc, orig, dest, date, time, km, terrain, leader, tail, gas, gear, cost, max, wa ->
                            viewModel.createRide(
                                title = title,
                                description = desc,
                                origin = orig,
                                destination = dest,
                                departureDate = date,
                                meetingTime = time,
                                distanceKm = km,
                                terrainType = terrain,
                                convoyLeader = leader,
                                tailRider = tail,
                                gasStops = gas,
                                requiredGear = gear,
                                costUsd = cost,
                                maxParticipants = max,
                                whatsappLink = wa
                            )
                        }
                    )
                }
                NavigationTab.MEMBERS -> {
                    MembersScreen(
                        members = allMembers,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        onRegisterMember = { fullName, nickname, number, dni, phone, role, chapter, brand, model, color, cc, tank, year, plate, blood, medNotes, emName, emPhone, emRel, solvent ->
                            viewModel.registerMember(
                                fullName = fullName,
                                nickname = nickname,
                                memberNumber = number,
                                cedulaDni = dni,
                                phone = phone,
                                role = role,
                                chapterState = chapter,
                                bikeBrand = brand,
                                bikeModel = model,
                                bikeColor = color,
                                bikeDisplacementCc = cc,
                                bikeTankCapacityLiters = tank,
                                bikeYear = year,
                                bikePlate = plate,
                                bloodType = blood,
                                medicalNotes = medNotes,
                                emergencyName = emName,
                                emergencyPhone = emPhone,
                                emergencyRelation = emRel,
                                solvencyStatus = solvent
                            )
                        },
                        onToggleSolvency = { member ->
                            viewModel.toggleMemberSolvency(member)
                        },
                        onUpdateRole = { member, role ->
                            viewModel.updateMemberRole(member, role)
                        },
                        onSuspendMember = { member, reason, days ->
                            viewModel.suspendMember(member, reason, days)
                        },
                        onReactivateMember = { member ->
                            viewModel.reactivateMember(member)
                        },
                        onSelectMemberAsActive = { memberId ->
                            viewModel.selectMember(memberId)
                        }
                    )
                }
                NavigationTab.FINANCES -> {
                    FinancesScreen(
                        summary = financialSummary,
                        transactions = transactions,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        bcvRate = bcvRate,
                        onSetBcvRate = { viewModel.setBcvRate(it) },
                        onSubmitPayment = { concept, desc, cat, usd, method, ref ->
                            viewModel.submitPayment(concept, desc, cat, usd, method, ref)
                        },
                        onSubmitExpense = { concept, desc, cat, usd, method, ref ->
                            viewModel.submitExpense(concept, desc, cat, usd, method, ref)
                        },
                        onUpdateTxStatus = { tx, status ->
                            viewModel.updateTransactionStatus(tx, status)
                        }
                    )
                }
                NavigationTab.INVENTORY -> {
                    InventoryScreen(
                        items = inventoryItems,
                        loans = equipmentLoans,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        onRequestLoan = { item, returnDate, purpose ->
                            viewModel.requestEquipmentLoan(item, returnDate, purpose)
                        },
                        onReturnLoan = { loan, items ->
                            viewModel.returnEquipmentLoan(loan, items)
                        },
                        onCreateItem = { code, name, cat, desc, stock, cond, loc, cust ->
                            viewModel.createInventoryItem(code, name, cat, desc, stock, cond, loc, cust)
                        }
                    )
                }
                NavigationTab.SOS -> {
                    EmergencySosScreen(
                        alerts = emergencyAlerts,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        onBroadcastSos = { type, loc, details, blood, lat, lng ->
                            viewModel.broadcastSosEmergency(type, loc, details, blood, lat, lng)
                        },
                        onUpdateAlertStatus = { alert, status, notes ->
                            viewModel.updateAlertStatus(alert, status, notes)
                        }
                    )
                }
                NavigationTab.PROFILE -> {
                    var showDirectivaChat by remember { mutableStateOf(false) }
                    val isLeaderSuperAdmin by viewModel.isLeaderSuperAdmin.collectAsStateWithLifecycle()
                    val invitationCodes by viewModel.allInvitationCodes.collectAsStateWithLifecycle()
                    val accessRequests by viewModel.allAccessRequests.collectAsStateWithLifecycle()
                    val directivaChatMessages by viewModel.getMessagesForChannel("DIRECTIVA").collectAsStateWithLifecycle()
                    
                    if (showDirectivaChat) {
                        ClubChatScreen(
                            messages = directivaChatMessages,
                            currentMember = currentMember,
                            isDirectivaMode = isDirectivaMode,
                            roleConfigs = roleConfigs,
                            activeChannelId = "DIRECTIVA",
                            onSelectChannel = { /* No-op, only one channel */ },
                            onSendMessage = { ch, text, radio, rId, rSender, rText -> viewModel.sendChatMessage(ch, text, radio, rId, rSender, rText) },
                            onDeleteMessage = { viewModel.deleteChatMessage(it) },
                            showOnlyDirectiva = true
                        )
                        // Añadimos un botón flotante sencillo para salir del chat
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            FloatingActionButton(
                                onClick = { showDirectivaChat = false },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp),
                                containerColor = TxFlameRed
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Volver al Perfil", tint = Color.White)
                            }
                        }
                    } else {
                        ProfileAndAdminScreen(
                            currentMember = currentMember,
                            allMembers = allMembers,
                            isDirectivaMode = isDirectivaMode,
                            isLeaderSuperAdmin = isLeaderSuperAdmin,
                            onSelectMember = { viewModel.selectMember(it) },
                            onUpdateProfile = { viewModel.updateProfile(it) },
                            onVincularGoogle = { uid, email, photo ->
                                kotlinx.coroutines.MainScope().launch {
                                    val (exito, mensaje) = viewModel.vincularGoogle(uid, email, photo)
                                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                }
                            },
                            onUnlockWithMasterCode = { code -> viewModel.loginWithCode(code) }
                        )
                    }
                }
                NavigationTab.NORMATIVAS -> {
                    NormativasScreen()
                }
                NavigationTab.DIRECTIVA -> {
                    DirectivaExclusiveScreen(
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        isLeaderSuperAdmin = isLeaderSuperAdmin,
                        allMembers = allMembers,
                        invitationCodes = viewModel.allInvitationCodes.collectAsStateWithLifecycle().value ?: emptyList(),
                        accessRequests = accessRequests,
                        directivaChatMessages = directivaChatMessages,
                        onGenerateCode = { role, note, isGuest, hours -> viewModel.generateInvitationCode(role, note, isGuest, hours) },
                        onDeleteCode = { viewModel.deleteInvitationCode(it) },
                        onDarDeBajaInvitado = { member -> viewModel.darDeBajaInvitado(member) },
                        onApproveRequest = { viewModel.approveAccessRequest(it) },
                        onRejectRequest = { viewModel.rejectAccessRequest(it) },
                        onTransferCargo = { from, to, cargo -> viewModel.transferCargo(from, to, cargo) },
                        onAbandonCargo = { member -> viewModel.abandonCargo(member) },
                        onAssignRole = { member, role -> viewModel.updateMemberRole(member, role) },
                        onSendDirectivaChatMessage = { text -> viewModel.sendChatMessage("DIRECTIVA", text, true) },
                        onSendMessage = { ch, text, radio, rId, rSender, rText -> viewModel.sendChatMessage(ch, text, radio, rId, rSender, rText) },
                        onSendAudio = { ch, file, dur, trans -> viewModel.sendAudioMessage(ch, file, dur, trans) },
                        onRequestDirectivaAccess = { viewModel.submitAccessRequest("", "", "", "", "", "", "", "", "SOLICITUD_DIRECTIVA") },
                        onUnlockWithMasterCode = { code -> viewModel.loginWithCode(code) },
                        onToggleDirectiva = { viewModel.toggleDirectivaMode() },
                        onBack = { selectedTab = NavigationTab.FEED },
                        roleConfigs = roleConfigs,
                        onUpdateRoleConfig = { roleKey, title, duties -> viewModel.updateRoleConfig(roleKey, title, duties) },
                        isChatEnabled = isChatEnabled,
                        onToggleChatEnabled = { viewModel.toggleChatEnabled(it) },
                        onClearGeneralChat = { viewModel.clearGeneralChat() },
                        onDeleteChatMessage = { viewModel.deleteChatMessage(it) },
                        onDeleteMessageForMe = { viewModel.deleteChatMessageForMe(it) },
                        onMarkChatMessageAsRead = { viewModel.markMessageAsRead(it) },
                        onMarkChannelAsRead = { viewModel.markChannelMessagesAsRead(it) },
                        onSendSticker = { ch, file -> viewModel.sendStickerMessage(ch, file) },
                        onToggleReaction = { msgId, emoji -> viewModel.toggleMessageReaction(msgId, emoji) },
                        onToggleChatMute = { member, isMuted, reason -> viewModel.toggleMemberChatMute(member, isMuted, reason) },
                        onSuspendMember = { member, reason, days -> viewModel.suspendMember(member, reason, days) },
                        onReactivateMember = { member -> viewModel.reactivateMember(member) },
                        onToggleSolvency = { member -> viewModel.toggleMemberSolvency(member) },
                        onToggleBottomNav = { isBottomNavVisible = !isBottomNavVisible },
                        allPrivateGroups = allPrivateGroups,
                        onToggleBlockPrivateGroup = { groupId, isBlocked, reason ->
                            viewModel.toggleBlockPrivateGroup(groupId, isBlocked, reason)
                        },
                        onDeletePrivateGroup = { groupId ->
                            viewModel.deletePrivateGroup(groupId)
                        }
                    )
                }
                NavigationTab.MAPA -> {
                    TxMapLauncher(onBackClick = { selectedTab = NavigationTab.FEED })
                }
                NavigationTab.INFO -> {
                    VistaInfoScreen()
                }
                NavigationTab.CONFIGURACIONES -> {
                    ConfiguracionesScreen(
                        isDirectivaMode = isDirectivaMode,
                        onCerrarSesion = { viewModel.logout() },
                        onDarseDeBaja = { motivo ->
                            // Deshabilita la cuenta indefinidamente + cierra sesión
                            // Solo un directivo puede reactivar con reactivateMember()
                            viewModel.darseDeBAja(motivo)
                        }
                    )
                }
            }
        }
    }

    if (showQuickSosModal) {
        EmitSosDialog(
            currentMember = currentMember,
            onDismiss = { showQuickSosModal = false },
            onBroadcast = { type, loc, details, blood, lat, lng ->
                viewModel.broadcastSosEmergency(type, loc, details, blood, lat, lng)
                showQuickSosModal = false
                selectedTab = NavigationTab.SOS
            }
        )
    }
}
