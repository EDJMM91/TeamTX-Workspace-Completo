package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.aistudio.teamtxvzla.BuildConfig
import com.aistudio.teamtxvzla.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.example.dashboard.DashboardFondoConfig
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
import com.example.ui.screens.FormularioIngreso
import com.example.ui.screens.SalaEspera
import com.example.ui.screens.SolicitudesIngreso
import com.example.ui.screens.dialogs.EmitSosDialog
import com.example.ui.screens.permissions.PermissionHandler
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusError
import com.example.ui.theme.TxFlameRed
import com.example.ui.viewmodel.TeamTxViewModel
import com.example.GestorNotificacionesApp
import com.example.GestorSesion
import com.aistudio.teamtxvzla.nube.NubeMultimedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

enum class NavigationTab(val label: String, val iconFilled: ImageVector, val iconOutlined: ImageVector, val tag: String) {
    DASHBOARD("Inicio", Icons.Default.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    FEED("Muro", Icons.Default.Campaign, Icons.Outlined.Campaign, "tab_feed"),
    CHAT("Chat", Icons.Default.Forum, Icons.Outlined.Forum, "tab_chat"),
    NOTIFICACIONES("Avisos", Icons.Default.Notifications, Icons.Outlined.Notifications, "tab_notificaciones"),
    RANKING("Ranking", Icons.Default.MilitaryTech, Icons.Outlined.MilitaryTech, "tab_ranking"),
    CALENDARIO("Calendario", Icons.Default.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendario"),
    PLAYER("Player TX", Icons.Default.MusicNote, Icons.Outlined.MusicNote, "tab_player"),
    RETOS("Retos", Icons.Default.EmojiEvents, Icons.Outlined.EmojiEvents, "tab_retos"),
    VELOCIMETRO("Velocímetro", Icons.Default.Speed, Icons.Outlined.Speed, "tab_velocimetro"),
    RIDES("Rodadas", Icons.Default.TwoWheeler, Icons.Outlined.TwoWheeler, "tab_rides"),
    MERCADO("Mercado", Icons.Default.ShoppingCart, Icons.Outlined.ShoppingCart, "tab_mercado"),
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
    CONFIGURACIONES("Ajustes", Icons.Default.Settings, Icons.Outlined.Settings, "tab_configuraciones"),
    MESHTX("Mesh TX", Icons.Default.Podcasts, Icons.Default.Podcasts, "tab_meshtx"),
    REDES("Redes TX", Icons.Default.Share, Icons.Outlined.Share, "tab_redes"),
    RUTAS("Rutas TX", Icons.Default.Navigation, Icons.Outlined.Navigation, "tab_rutas")
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

        // 🎨 Inicializar fondo configurable del Dashboard
        com.example.dashboard.DashboardFondoConfig.inicializar(applicationContext)

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
fun GoogleLoginPantalla(viewModel: TeamTxViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("554561749183-q637440n43im5mjfg5pp9lsb4uu0a441.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val fallbackEmail = account?.email ?: ""
            val googlePhotoUrl = account?.photoUrl?.toString()

            if (idToken != null) {
                com.aistudio.teamtxvzla.nube.AutenticacionNube.iniciarSesionConGoogle(idToken) { usuarioFirebase ->
                    if (usuarioFirebase != null) {
                        coroutineScope.launch {
                            val uid = usuarioFirebase.uid
                            val userEmail = usuarioFirebase.email ?: fallbackEmail
                            viewModel.procesarIngresoGoogle(uid, userEmail, googlePhotoUrl)
                        }
                    } else {
                        Toast.makeText(context, "Error conectando a Firebase Auth.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501 && e.statusCode != 12502) {
                Log.e("GoogleSignIn", "ApiException code: ${e.statusCode}", e)
                Toast.makeText(context, "Error al acceder con Google (${e.statusCode}): ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido a Team Nacional TX Aragua",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Selecciona tu cuenta de Google vinculada o autorizada por la Directiva para ingresar.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    isLoading = true
                    // 🛡️ REGLA OBLIGATORIA: Forzar Sign-Out previo para romper sesiones cacheadas
                    // y obligar a Android a desplegar siempre el diálogo selector de cuentas de Google
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("ACCEDER CON GOOGLE", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🌐 Opción para crear cuenta de Google si no posee una
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://accounts.google.com/signup")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Abre tu navegador para crear una cuenta de Google", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MotoOrangePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿No tienes cuenta de Google?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Toca aquí para crear una cuenta nueva y volver a la app",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
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
    
    if (showSplash) {
        SplashScreen(onComplete = { showSplash = false })
    } else if (!permissionsGranted) {
        PermissionHandler(onAllPermissionsGranted = { permissionsGranted = true }, scope = scope)
    } else if (!isAuthenticated) {
        GoogleLoginPantalla(viewModel)
    } else {
        val currentMember by viewModel.currentMember.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var skipOnboarding by remember { mutableStateOf(false) }
        
        PreferenciasApp.init(context)
        var mostrarCompromisoHonor by remember(currentMember?.id) {
            mutableStateOf(
                currentMember != null && !PreferenciasApp.haAceptadoCompromisoBiker(currentMember!!.id)
            )
        }

        if (mostrarCompromisoHonor && currentMember != null) {
            com.example.ui.components.DialogoCompromisoHonorBiker(
                nombrePiloto = currentMember?.nickname?.ifBlank { currentMember?.fullName } ?: "Hermano Motero",
                onAceptarCompromiso = {
                    PreferenciasApp.setCompromisoBikerAceptado(currentMember!!.id, true)
                    mostrarCompromisoHonor = false
                }
            )
        }

        LaunchedEffect(currentMember) {
            if (currentMember != null && !currentMember!!.isProfileComplete) {
                notificarPerfilIncompleto(context)
            }
        }

        LaunchedEffect(currentMember?.isSuspended) {
            val emailLimpio = (currentMember?.email ?: "").trim().lowercase()
            val esDevBypass = emailLimpio == "eduardo.androide.em@gmail.com" || emailLimpio == "eduardo.jose.marquez.matos@gmail.com"
            if (currentMember != null && currentMember!!.isSuspended && !esDevBypass) {
                val reason = currentMember!!.suspensionReason.ifBlank { "Acceso temporal finalizado o revocado por la Directiva." }
                Toast.makeText(context, "Acceso Revocado: $reason", Toast.LENGTH_LONG).show()
                viewModel.logout()
            }
        }

        // 🔐 ENRUTAMIENTO BASADO EN ESTADO DE FIRESTORE (usuarioEstado):
        // "NUEVO_REGISTRO" -> FormularioIngreso (Carnet TX)
        // "PENDIENTE"      -> SalaEspera (Cuarentena)
        // "RECHAZADO"      -> SalaEspera / Rechazo
        // "ACTIVO"         -> MainAppScreen
        val usuarioEstado by viewModel.usuarioEstado.collectAsStateWithLifecycle()
        
        when (usuarioEstado) {
            "NUEVO_REGISTRO" -> {
                FormularioIngreso(
                    onCompletarRegistro = { nuevoPerfil ->
                        viewModel.guardarNuevoPerfilFirestore(nuevoPerfil)
                    },
                    onVolver = {
                        viewModel.logout()
                    }
                )
            }
            "PENDIENTE" -> {
                SalaEspera(
                    onVerificarYRedirigir = {
                        viewModel.verificarEstadoFirestore()
                    },
                    userUid = currentMember?.firebaseUid ?: ""
                )
            }
            "RECHAZADO" -> {
                SalaEspera(
                    onVerificarYRedirigir = {
                        viewModel.verificarEstadoFirestore()
                    },
                    userUid = currentMember?.firebaseUid ?: "",
                    initialState = "RECHAZADO"
                )
            }
            else -> {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: TeamTxViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    var targetCalendarDate by remember { mutableStateOf<String?>(null) }
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

    val sesionDesplazada by viewModel.sesionDesplazadaPorOtroDispositivo.collectAsStateWithLifecycle()

    var showQuickSosModal by remember { mutableStateOf(false) }
    var readOnlyCarnetMember by remember { mutableStateOf<MemberProfile?>(null) }
    var isBottomNavVisible by remember { mutableStateOf(true) }
    var showMenuRutasBottomSheet by remember { mutableStateOf(false) }

    // 🎛️ Estado para los 5 accesos rápidos principales de la barra inferior (personalizables y persistentes)
    var bottomTabs by remember {
        val guardados = PreferenciasApp.obtenerBottomTabs()?.mapNotNull { nombre ->
            try { NavigationTab.valueOf(nombre) } catch (_: Exception) { null }
        }
        mutableStateOf(
            if (guardados != null && guardados.size == 5) {
                guardados
            } else {
                listOf(
                    NavigationTab.DASHBOARD,
                    NavigationTab.MAPA,
                    NavigationTab.RUTAS,
                    NavigationTab.SOS,
                    NavigationTab.NOTIFICACIONES
                )
            }
        )
    }
    var slotToEditIndex by remember { mutableStateOf<Int?>(null) }

    // Auto-hide bottom nav when entering chat, directiva, or velocímetro
    LaunchedEffect(selectedTab) {
        if (selectedTab == NavigationTab.CHAT || selectedTab == NavigationTab.DIRECTIVA || selectedTab == NavigationTab.VELOCIMETRO) {
            isBottomNavVisible = false
        } else {
            isBottomNavVisible = true
        }
    }

    val appCtx = LocalContext.current

    LaunchedEffect(currentMember) {
        val androidId = try {
            android.provider.Settings.Secure.getString(appCtx.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: android.os.Build.MODEL
        } catch (_: Exception) {
            android.os.Build.MODEL
        }
        val hardwareHash = (androidId.hashCode().toLong() and 0xFFFFL)
        val miembro = currentMember
        // Cada dispositivo físico tiene un ID de nodo de malla único, evitando colisión si se prueba con la misma cuenta:
        val idPilotoUnico = if (miembro != null) {
            (miembro.id shl 16) xor hardwareHash
        } else {
            100000L + hardwareHash
        }
        val aliasBase = miembro?.nickname?.ifBlank { miembro.fullName.ifBlank { "Piloto TX" } } ?: "Piloto TX"
        val fotoPerfil = miembro?.profilePhotoUri
            ?: appCtx.getSharedPreferences("radar_prefs", android.content.Context.MODE_PRIVATE).getString("radar_avatar", "")
            ?: ""
        val modeloMoto = miembro?.bikeModel?.ifBlank { "Keeway TX 200" } ?: "Keeway TX 200"
        val fichaMiembro = miembro?.memberNumber ?: ""

        com.example.meshtx.GestorMeshTx.inicializar(
            contexto = appCtx,
            idPiloto = idPilotoUnico,
            aliasPiloto = aliasBase,
            fotoPerfil = fotoPerfil,
            modeloMoto = modeloMoto,
            ficha = fichaMiembro
        )
        com.example.meshtx.GestorMeshTx.actualizarPerfilLocal(
            alias = aliasBase,
            fotoPerfil = fotoPerfil,
            modeloMoto = modeloMoto,
            ficha = fichaMiembro
        )
    }

    // 🌐 Odómetro Continuo Global en Toda la App (Si está habilitado en Ajustes)
    val hasLocPerm = remember {
        ContextCompat.checkSelfPermission(appCtx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(hasLocPerm) {
        if (!hasLocPerm) return@DisposableEffect onDispose {}
        val locManager = appCtx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var lastAppLoc: Location? = null

        val globalListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (!PreferenciasApp.odometroGlobalActivo) return
                val speedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
                lastAppLoc?.let { prev ->
                    val dist = prev.distanceTo(loc)
                    if (dist > 1.2f && (speedKmh > 1.2f || dist / 2.0f > 0.5f)) {
                        val addedKm = dist / 1000.0
                        PreferenciasApp.odometroTotalKm += addedKm
                        viewModel.accumulateMemberKilometers(addedKm)
                        Log.d("TEAM_TX_VELOCIMETRO", "🌐 Odómetro continuo global: +$addedKm km")
                    }
                }
                lastAppLoc = loc
            }
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }

        try {
            locManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2.0f, globalListener)
        } catch (_: SecurityException) {}

        onDispose {
            try { locManager?.removeUpdates(globalListener) } catch (_: Exception) {}
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
                    onOpenCarnet = { selectedTab = NavigationTab.PROFILE },
                    isDeveloperMode = currentMember?.role == MemberRole.DESARROLLADOR || (currentMember?.role == MemberRole.PRESIDENTE && isDirectivaMode) || currentMember?.memberNumber?.startsWith("TX-DEV-") == true
                )
            }
        },
        bottomBar = {
            val isCalendarOrFullscreen = selectedTab == NavigationTab.CALENDARIO ||
                    selectedTab == NavigationTab.NOTIFICACIONES ||
                    selectedTab == NavigationTab.PLAYER ||
                    selectedTab == NavigationTab.DIRECTORIO ||
                    selectedTab == NavigationTab.RANKING ||
                    selectedTab == NavigationTab.MESHTX
            AnimatedVisibility(visible = isBottomNavVisible && !isCalendarOrFullscreen) {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = DashboardFondoConfig.ColorTarjetaClara,
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomTabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == tab
                            val isSos = tab == NavigationTab.SOS
                            val isNotificaciones = tab == NavigationTab.NOTIFICACIONES
                            val isChat = tab == NavigationTab.CHAT

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .pointerInput(tab) {
                                        detectTapGestures(
                                            onTap = {
                                                if (tab == NavigationTab.RUTAS) {
                                                    showMenuRutasBottomSheet = true
                                                } else {
                                                    selectedTab = tab
                                                }
                                            },
                                            onLongPress = { slotToEditIndex = index }
                                        )
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                                    .testTag(tab.tag)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(40.dp)
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
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
                                            contentDescription = tab.label,
                                            tint = when {
                                                isSos -> StatusError
                                                isSelected -> TxFlameRed
                                                else -> DashboardFondoConfig.ColorTextoSecundario
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        isSos && isSelected -> StatusError
                                        isSos -> StatusError.copy(alpha = 0.8f)
                                        isSelected -> TxFlameRed
                                        else -> DashboardFondoConfig.ColorTextoSecundario
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // ⚙️ Diálogo para editar el slot de la barra inferior al dejar presionado
            if (slotToEditIndex != null) {
                val idx = slotToEditIndex!!
                AlertDialog(
                    onDismissRequest = { slotToEditIndex = null },
                    title = {
                        Text(
                            "Personalizar Acceso Rápido",
                            fontWeight = FontWeight.Bold,
                            color = DashboardFondoConfig.ColorTextoPrimario,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Mantén presionado cualquier icono inferior para cambiarlo. Selecciona el módulo deseado para este espacio:",
                                fontSize = 12.sp,
                                color = DashboardFondoConfig.ColorTextoSecundario
                            )
                            NavigationTab.values().forEach { candidateTab ->
                                Surface(
                                    color = DashboardFondoConfig.ColorContenedorAzul,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val updated = bottomTabs.toMutableList()
                                            updated[idx] = candidateTab
                                            bottomTabs = updated
                                            PreferenciasApp.guardarBottomTabs(updated.map { it.name })
                                            slotToEditIndex = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(candidateTab.iconFilled, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(20.dp))
                                        Text(candidateTab.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DashboardFondoConfig.ColorTextoPrimario)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { slotToEditIndex = null }) {
                            Text("Cancelar", color = DashboardFondoConfig.ColorTextoSecundario)
                        }
                    },
                    containerColor = DashboardFondoConfig.ColorTarjetaClara
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            val isCurrentModuleDisabled = (currentMember?.isModuleDisabled(tab.name) == true || currentMember?.isModuleDisabled(tab.tag) == true) &&
                    !isLeaderSuperAdmin && currentMember?.role != MemberRole.DESARROLLADOR

            if (isCurrentModuleDisabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Módulo Inhabilitado", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El módulo '${tab.label}' ha sido inhabilitado para tu cuenta por la Directiva del Club. Contacta al Presidente o Desarrollador Máster para solicitar acceso.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { selectedTab = NavigationTab.DASHBOARD },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Volver al Inicio", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                when (tab) {
                NavigationTab.DASHBOARD -> {
                    com.example.dashboard.DashboardScreen(
                        currentMember = currentMember,
                        publications = publications,
                        calendarEvents = calendarEvents,
                        emergencyAlerts = emergencyAlerts,
                        unreadChatCount = unreadPublicChatCount,
                        notificacionesNoLeidasCount = notificacionesNoLeidas,
                        isDirectivaMode = isDirectivaMode,
                        onNavigateToTab = { selectedTab = it },
                        onOpenSosModal = { showQuickSosModal = true }
                    )
                }
                NavigationTab.FEED -> {
                    val uploadError by viewModel.publicationUploadError.collectAsStateWithLifecycle()
                    val uploadSuccess by viewModel.publicationUploadSuccess.collectAsStateWithLifecycle()
                    val dismissedNoticeIds by viewModel.dismissedNoticeIds.collectAsStateWithLifecycle()
                    
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
                        onCreatePublication = { title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments, locCoords, locName, eventDate, eventTime, syncCal ->
                            viewModel.createPublication(title, content, cat, prio, pinned, km, badge, tg, imageUri, allowComments, locCoords, locName, eventDate, eventTime, syncCal)
                        },
                        onShare = { viewModel.sharePublication(it) },
                        onSave = { viewModel.savePublication(it) },
                        dismissedNoticeIds = dismissedNoticeIds,
                        onDismissNotice = { viewModel.dismissNotice(it) },
                        onClearAllNotices = { viewModel.clearAllNoticesFromScreen(it) },
                        onRestoreDismissedNotices = { viewModel.restoreDismissedNotices() },
                        onNavigateToCalendar = { dateStr ->
                            targetCalendarDate = dateStr
                            selectedTab = NavigationTab.CALENDARIO
                        },
                        onToggleEventFinished = { pub, isFinished ->
                            viewModel.togglePublicationEventFinished(pub, isFinished)
                        },
                        isRefreshing = isRefreshingFeed,
                        onRefresh = { viewModel.refreshFeed() },
                        uploadError = uploadError,
                        uploadSuccess = uploadSuccess,
                        onDismissUploadStatus = { viewModel.clearPublicationUploadStatus() },
                        allWorkshops = allWorkshops,
                        allPrivateGroups = allPrivateGroups,
                        onNavigateToTab = { selectedTab = it }
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
                        onSendLocation = { ch, coords ->
                            viewModel.sendLocationMessage(ch, coords)
                        },
                        onToggleReaction = { msgId, emoji ->
                            viewModel.toggleMessageReaction(msgId, emoji)
                        },
                        onDeleteMessage = { viewModel.deleteChatMessage(it) },
                        onDeleteMessageForMe = { viewModel.deleteChatMessageForMe(it) },
                        onMarkMessageAsRead = { viewModel.markMessageAsRead(it) },
                        onMarkChannelAsRead = { viewModel.markChannelMessagesAsRead(it) },
                        onToggleBottomNav = { isBottomNavVisible = !isBottomNavVisible },
                        onBack = { selectedTab = NavigationTab.DASHBOARD },
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
                        onBack = { selectedTab = NavigationTab.DASHBOARD },
                        onNotificacionClick = { notif ->
                            when (notif.tipo) {
                                "ACTUALIZACION", "OTA" -> selectedTab = NavigationTab.INFO
                                "MURO" -> selectedTab = NavigationTab.FEED
                                "CHAT" -> selectedTab = NavigationTab.CHAT
                                "SOS" -> selectedTab = NavigationTab.SOS
                                "PERFIL" -> selectedTab = NavigationTab.PROFILE
                                else -> {}
                            }
                        }
                    )
                }
                NavigationTab.CALENDARIO -> {
                    CalendarScreen(
                        events = calendarEvents,
                        currentMember = currentMember,
                        isDirectivaMode = isDirectivaMode,
                        initialSelectedDate = targetCalendarDate,
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
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
                    )
                }
                NavigationTab.PLAYER -> {
                    com.example.reproductor.REPRODUCTOR_PRINCIPAL(
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
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
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
                    )
                }
                NavigationTab.RANKING -> {
                    RankingScreen(
                        allMembers = allMembers,
                        currentMember = currentMember,
                        onOpenMemberCarnet = { member ->
                            if (member.id == currentMember?.id) {
                                selectedTab = NavigationTab.PROFILE
                            } else {
                                readOnlyCarnetMember = member
                            }
                        },
                        onRateMember = { target, isPos, cat, pts, comm ->
                            viewModel.ratePilotMember(target.id, isPos, cat, pts, comm) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
                    )
                }
                NavigationTab.VELOCIMETRO -> {
                    VelocimetroScreen(
                        currentMember = currentMember,
                        onAccumulateKm = { viewModel.accumulateMemberKilometers(it) },
                        onUpdateTopSpeed = { viewModel.updateMemberTopSpeed(it) },
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
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
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
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
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
                    )
                }
                NavigationTab.DIRECTORIO -> {
                    WorkshopDirectoryScreen(
                        workshops = allWorkshops,
                        currentMember = currentMember,
                        onCreateWorkshop = { name, type, state, city, addr, ph, wa, rat, notes, lat, lng, hasCredit, creditPlatforms, gMapsUrl ->
                            viewModel.createWorkshop(name, type, state, city, addr, ph, wa, rat, notes, lat, lng, hasCredit, creditPlatforms, gMapsUrl)
                        },
                        onUpdateWorkshop = { updatedItem ->
                            viewModel.updateWorkshop(updatedItem)
                        },
                        onDeleteWorkshop = { viewModel.deleteWorkshop(it) },
                        onNavigateToMap = { lat, lng, title ->
                            val prefs = context.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("target_dest_lat", lat.toString())
                                .putString("target_dest_lon", lng.toString())
                                .putString("target_dest_name", title)
                                .apply()
                            selectedTab = NavigationTab.MAPA
                        },
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
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
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
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
                        },
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
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
                        },
                        onRateMember = { target, isPos, cat, pts, comm ->
                            viewModel.ratePilotMember(target.id, isPos, cat, pts, comm) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenPrivateChat = { member ->
                            val activeMember = currentMember
                            val dmChannel = if (activeMember != null) {
                                val ids = listOf(activeMember.id, member.id).sorted()
                                "DM_${ids[0]}_${ids[1]}"
                            } else {
                                "DM_${member.id}"
                            }
                            viewModel.selectChatChannel(dmChannel)
                            selectedTab = NavigationTab.CHAT
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
                        allMembers = allMembers,
                        isDirectivaMode = isDirectivaMode,
                        onBack = { selectedTab = NavigationTab.DASHBOARD },
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
                            onSendLocation = { ch, coords -> viewModel.sendLocationMessage(ch, coords) },
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
                                if (!photo.isNullOrBlank()) {
                                    PreferenciasApp.carnetGooglePhotoUrl = photo
                                }
                                kotlinx.coroutines.MainScope().launch {
                                    val (exito, mensaje) = viewModel.vincularGoogle(uid, email, photo)
                                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                }
                            },
                            onDesvincularGoogle = {
                                kotlinx.coroutines.MainScope().launch {
                                    val (exito, mensaje) = viewModel.desvincularGoogle()
                                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                }
                            },
                            onCambiarCodigoAcceso = { code -> viewModel.cambiarCodigoAcceso(code) },
                            onCambiarCuentaDev = { code -> viewModel.cambiarCuentaDesarrollador(code) },
                            onUnlockWithMasterCode = { code -> viewModel.loginWithCode(code) },
                            onRateMember = { target, isPos, cat, pts, comm ->
                                viewModel.ratePilotMember(target.id, isPos, cat, pts, comm) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLogout = { viewModel.logout() }
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
                        disciplinaryRecords = viewModel.allDisciplinaryRecords.collectAsStateWithLifecycle().value ?: emptyList(),
                        onExpelMember = { member, _ -> viewModel.eliminarUsuarioDefinitivamente(member) },
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
                        onBack = { selectedTab = NavigationTab.DASHBOARD },
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
                        onToggleModuloPiloto = { member, tag -> viewModel.toggleModuloPiloto(member, tag) },
                        onToggleSolvency = { member -> viewModel.toggleMemberSolvency(member) },
                        onToggleBottomNav = { isBottomNavVisible = !isBottomNavVisible },
                        allPrivateGroups = allPrivateGroups,
                        onToggleBlockPrivateGroup = { groupId, isBlocked, reason ->
                            viewModel.toggleBlockPrivateGroup(groupId, isBlocked, reason)
                        },
                        onDeletePrivateGroup = { groupId ->
                            viewModel.deletePrivateGroup(groupId)
                        },
                        onCreateOfficialNotice = { title, content, prio, pinned ->
                            viewModel.createPublication(
                                title = title,
                                content = content,
                                category = NoticeCategory.AVISO_OFICIAL,
                                priority = if (prio == "URGENTE") NoticePriority.URGENTE else if (prio == "IMPORTANTE") NoticePriority.IMPORTANTE else NoticePriority.NORMAL,
                                isPinned = pinned
                            )
                        }
                    )
                }
                NavigationTab.MAPA -> {
                    TxMapLauncher(
                        onBackClick = { selectedTab = NavigationTab.DASHBOARD },
                        onNavigateToDirectory = { selectedTab = NavigationTab.DIRECTORIO },
                        currentMember = currentMember,
                        onBroadcastSos = { type, loc, details, blood, lat, lng ->
                            viewModel.broadcastSosEmergency(type, loc, details, blood, lat, lng)
                            try {
                                if (type == com.example.data.model.EmergencyType.ALCABALA_RETEN) {
                                    com.example.meshtx.GestorMeshTx.activarModoAlcabalaSos("Ubicación: $loc. $details")
                                } else {
                                    com.example.meshtx.GestorMeshTx.emitirAlertaSos(
                                        "🚨 SOS ${type.name}: $loc - $details",
                                        if (lat != 0.0) "$lat,$lng" else null
                                    )
                                }
                            } catch (_: Exception) {}
                        },
                        onOpenMemberCarnetById = { pilotId ->
                            val found = allMembers.find {
                                it.id.toString() == pilotId ||
                                it.firebaseUid == pilotId ||
                                it.memberNumber.equals(pilotId, ignoreCase = true) ||
                                it.fullName.contains(pilotId, ignoreCase = true)
                            }
                            if (found != null) {
                                if (found.id == currentMember?.id) {
                                    selectedTab = NavigationTab.PROFILE
                                } else {
                                    readOnlyCarnetMember = found
                                }
                            }
                        }
                    )
                }
                NavigationTab.INFO -> {
                    VistaInfoScreen(
                        currentMember = currentMember,
                        allMembers = allMembers,
                        viewModel = viewModel,
                        onNavigateToAvisos = { selectedTab = NavigationTab.FEED },
                        onOpenPrivateChatWithDeveloper = {
                            val devMember = allMembers.find {
                                it.phone.contains("04243769999") || it.phone.contains("4243769999") ||
                                it.email.equals("eduardo.androide.em@gmail.com", ignoreCase = true) ||
                                it.cedulaDni.trim() == "19554402" ||
                                (it.fullName.contains("Eduardo", ignoreCase = true) && (it.fullName.contains("Márquez", ignoreCase = true) || it.fullName.contains("Marquez", ignoreCase = true) || it.fullName.contains("Androide", ignoreCase = true))) ||
                                it.role == MemberRole.DESARROLLADOR || it.role == MemberRole.PRESIDENTE
                            }
                            val devId = devMember?.id ?: 1L
                            val myId = currentMember?.id ?: 0L
                            val dmChannel = "DM_${minOf(myId, devId)}_${maxOf(myId, devId)}"
                            viewModel.selectChatChannel(dmChannel)
                            selectedTab = NavigationTab.CHAT
                        },
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
                    )
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
                NavigationTab.MESHTX -> {
                    com.example.meshtx.MeshTxScreen(
                        currentMember = currentMember,
                        onBackToDashboard = { selectedTab = NavigationTab.DASHBOARD }
                    )
                }
                NavigationTab.REDES -> {
                    RedesScreen(
                        onBack = { selectedTab = NavigationTab.DASHBOARD }
                    )
                }
                NavigationTab.RUTAS -> {
                    com.example.rutas.VistaRutasScreen(
                        currentMember = currentMember,
                        onVolver = { selectedTab = NavigationTab.DASHBOARD }
                    )
                }
            }
        }
    }
    }

    // 🗺️ Menú Táctico Desplegable para el Botón Central (Rutas TX & Telemetría)
    if (showMenuRutasBottomSheet) {
        com.example.rutas.MenuRutasBottomSheet(
            currentMember = currentMember,
            onDismiss = { showMenuRutasBottomSheet = false },
            onNavegarAPanelCompleto = {
                selectedTab = NavigationTab.RUTAS
            },
            onCrearRutaGuiada = {
                selectedTab = NavigationTab.RUTAS
            },
            onGenerarVideoYReporte = {
                selectedTab = NavigationTab.RUTAS
            }
        )
    }

    if (showQuickSosModal) {
        EmitSosDialog(
            currentMember = currentMember,
            onDismiss = { showQuickSosModal = false },
            onBroadcast = { type, loc, details, blood, lat, lng ->
                viewModel.broadcastSosEmergency(type, loc, details, blood, lat, lng)
                try {
                    if (type == com.example.data.model.EmergencyType.ALCABALA_RETEN) {
                        com.example.meshtx.GestorMeshTx.activarModoAlcabalaSos("Ubicación: $loc. $details")
                    } else {
                        com.example.meshtx.GestorMeshTx.emitirAlertaSos("🚨 SOS ${type.name}: $loc - $details", if (lat != 0.0) "$lat,$lng" else null)
                    }
                } catch (_: Exception) {}
                showQuickSosModal = false
                selectedTab = NavigationTab.SOS
            }
        )
    }

    // Modal de Carnet TX en Modo Solo Lectura (sin suplantar identidad ni cambiar sesión)
    if (readOnlyCarnetMember != null) {
        com.example.ui.screens.dialogs.ReadOnlyCarnetDialog(
            member = readOnlyCarnetMember!!,
            currentMember = currentMember,
            currentLoggedInMemberId = currentMember?.id ?: 0L,
            onDismiss = { readOnlyCarnetMember = null },
            onConfirmRate = { target, isPos, cat, pts, comm ->
                viewModel.ratePilotMember(target.id, isPos, cat, pts, comm) { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Burbuja Flotante In-App ("La Nube") del Reproductor TX Pro
    val configNubeAudio by com.example.reproductor.GESTOR_AUDIO_TX.configuracion.collectAsState()
    val cancionNubeAudio by com.example.reproductor.GESTOR_AUDIO_TX.cancionActual.collectAsState()
    val estadoNubeAudio by com.example.reproductor.GESTOR_AUDIO_TX.estado.collectAsState()

    if (selectedTab != NavigationTab.PLAYER && com.example.reproductor.GESTOR_AUDIO_TX.estaActivoOEnPausa()) {
        com.example.reproductor.NubeAudioFlotante(
            cancionActual = cancionNubeAudio,
            estado = estadoNubeAudio,
            config = configNubeAudio,
            onAnterior = { com.example.reproductor.GESTOR_AUDIO_TX.anteriorCancion() },
            onAlternarPlayPausa = { com.example.reproductor.GESTOR_AUDIO_TX.alternarPlayPausa() },
            onSiguiente = { com.example.reproductor.GESTOR_AUDIO_TX.siguienteCancion() },
            onAbrirReproductor = { selectedTab = NavigationTab.PLAYER },
            onActualizarPosicion = { x, y -> com.example.reproductor.GESTOR_AUDIO_TX.actualizarPosicionNube(x, y) }
        )
    }

    // 🔘 Botón PTT Flotante ("Nube Flotante Táctica") de Mesh TX fuera de la pantalla de intercomunicador
    val ajustesMesh by com.example.meshtx.GestorMeshTx.ajustes.collectAsState()
    val modoAlcabalaVivo by com.example.meshtx.GestorMeshTx.modoAlcabalaEnVivoActivo.collectAsState()

    if (selectedTab != NavigationTab.MESHTX && (ajustesMesh.botonFlotantePttActivo || modoAlcabalaVivo)) {
        com.example.meshtx.BotonPttFlotanteOverlay()
    }

    // 🛡️ Modal de Alerta de Sesión Desplazada por Otro Dispositivo (Anti-trampas en gamificación)
    if (sesionDesplazada) {
        AlertDialog(
            onDismissRequest = { /* Bloqueante para evitar trampas en gamificación */ },
            containerColor = DashboardFondoConfig.ColorTarjetaClara,
            icon = {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = TxFlameRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "Sesión Activa en Otro Dispositivo",
                    fontWeight = FontWeight.Black,
                    color = DashboardFondoConfig.ColorTextoPrimario,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    "Se ha detectado el inicio de sesión de tu cuenta en otro celular.\n\nPara proteger la integridad de los retos moteros, odómetro y puntuaciones en el ranking, esta sesión se ha pausado en este equipo.",
                    fontSize = 13.sp,
                    color = DashboardFondoConfig.ColorTextoSecundario
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.reclamarSesionEnEsteDispositivo() },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Text("Reclamar Sesión Aquí", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cerrarSesionPorDesplazamiento() }) {
                    Text("Cerrar Sesión", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }
}
