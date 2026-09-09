package com.example.ui.screens

import com.aistudio.teamtxvzla.BuildConfig
import com.aistudio.teamtxvzla.R
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.NubeArchivos
import com.example.dashboard.DashboardFondoConfig
import com.example.data.model.*
import com.example.ui.components.DigitalCredentialCard
import com.example.ui.components.openUrl
import com.example.ui.preferences.PreferenciasApp
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.launch

import com.example.ui.components.RatePilotDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndAdminScreen(
    currentMember: MemberProfile?,
    allMembers: List<MemberProfile>,
    isDirectivaMode: Boolean,
    isLeaderSuperAdmin: Boolean = false,
    onSelectMember: (Long) -> Unit,
    onUpdateProfile: (MemberProfile) -> Unit,
    onVincularGoogle: (String, String, String?) -> Unit = { _, _, _ -> },
    onDesvincularGoogle: () -> Unit = {},
    onCambiarCodigoAcceso: suspend (String) -> Pair<Boolean, String> = { _ -> Pair(false, "") },
    onCambiarCuentaDev: suspend (String) -> Pair<Boolean, String> = { _ -> Pair(false, "") },
    onUnlockWithMasterCode: suspend (String) -> Pair<Boolean, String> = { _ -> Pair(false, "") },
    onRateMember: (MemberProfile, Boolean, String, Int, String) -> Unit = { _, _, _, _, _ -> },
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMemberSelectorDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDesvincularConfirmDialog by remember { mutableStateOf(false) }
    var showCambiarCodigoDialog by remember { mutableStateOf(false) }
    var showCambiarDevDialog by remember { mutableStateOf(false) }
    var ratingTargetMember by remember { mutableStateOf<MemberProfile?>(null) }
    var isGoogleAuthLoading by remember { mutableStateOf(false) }
    var tipoFotoSeleccionada by remember { mutableStateOf(PreferenciasApp.carnetTipoFoto) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ═══════════════════════════════════════════════
    // GOOGLE SIGN-IN: Usando módulo centralizado
    // ═══════════════════════════════════════════════
    val googleHelper = com.aistudio.teamtxvzla.nube.AutenticacionGoogle.recordarLauncherGoogle(
        onExito = { datos ->
            isGoogleAuthLoading = false
            if (!datos.fotoUrl.isNullOrBlank()) {
                PreferenciasApp.carnetGooglePhotoUrl = datos.fotoUrl
            }
            onVincularGoogle(datos.uid, datos.correo, datos.fotoUrl)
        },
        onError = { mensaje ->
            isGoogleAuthLoading = false
            android.widget.Toast.makeText(context, mensaje, android.widget.Toast.LENGTH_SHORT).show()
        }
    )

    val authCurrentEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
    val memberEmail = currentMember?.email?.lowercase()?.trim()
    val googlePhoto = if (memberEmail != null && authCurrentEmail == memberEmail) {
        PreferenciasApp.carnetGooglePhotoUrl ?: FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
    } else if (memberEmail == null) {
        PreferenciasApp.carnetGooglePhotoUrl ?: FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
    } else {
        null
    }
    val fotoCarnetActiva = if (tipoFotoSeleccionada == "CORREO" && !googlePhoto.isNullOrBlank()) {
        googlePhoto
    } else {
        currentMember?.profilePhotoUri
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DashboardFondoConfig.ColorFondoClaro
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(DashboardFondoConfig.ColorFondoClaro)
        ) {
            // Member Digital Credential Card
            item {
                if (currentMember != null) {
                    DigitalCredentialCard(
                        member = currentMember,
                        currentLoggedInMemberId = currentMember.id,
                        onRateMember = { ratingTargetMember = it },
                        isLightTheme = true,
                        photoUrlOverride = fotoCarnetActiva
                    )
                }
            }

            // ─── SELECTOR INTERACTIVO DE FOTO PARA EL CARNET TX ──────────────
            item {
                val tieneGoogle = !currentMember?.firebaseUid.isNullOrBlank() || !googlePhoto.isNullOrBlank()

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = DashboardFondoConfig.ColorRojoCarrera,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "FOTO EN TU CARNET DIGITAL",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = DashboardFondoConfig.ColorTextoPrimario,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DashboardFondoConfig.ColorContenedorDorado
                            ) {
                                Text(
                                    text = if (tipoFotoSeleccionada == "CORREO") "USANDO CORREO" else "USANDO PERFIL",
                                    color = Color(0xFFB45309),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Elige cuál imagen mostrar oficialmente en tu carnet de piloto:",
                            fontSize = 11.sp,
                            color = DashboardFondoConfig.ColorTextoSecundario
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Opción 1: Foto de Perfil
                            val esPerfilActivo = tipoFotoSeleccionada == "PERFIL"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esPerfilActivo) DashboardFondoConfig.ColorContenedorRojo.copy(alpha = 0.4f) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(
                                    if (esPerfilActivo) 2.dp else 1.dp,
                                    if (esPerfilActivo) DashboardFondoConfig.ColorRojoCarrera else DashboardFondoConfig.ColorBordeClaro
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        tipoFotoSeleccionada = "PERFIL"
                                        PreferenciasApp.carnetTipoFoto = "PERFIL"
                                        Toast.makeText(context, "Mostrando Foto de Perfil en carnet", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0))
                                            .border(
                                                2.dp,
                                                if (esPerfilActivo) DashboardFondoConfig.ColorRojoCarrera else Color(0xFFCBD5E1),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!currentMember?.profilePhotoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = com.example.util.SanitizadorImagenUrl.obtenerUrlEfectiva(currentMember?.profilePhotoUri),
                                                contentDescription = "Foto Perfil",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Text(
                                        text = "Foto de Perfil",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DashboardFondoConfig.ColorTextoPrimario
                                    )

                                    Text(
                                        text = if (currentMember?.profilePhotoUri.isNullOrBlank()) "Sin imagen" else "Subida al team",
                                        fontSize = 9.sp,
                                        color = DashboardFondoConfig.ColorTextoSecundario
                                    )

                                    if (esPerfilActivo) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DashboardFondoConfig.ColorRojoCarrera
                                        ) {
                                            Text(
                                                text = "EN USO",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Toca para elegir",
                                            fontSize = 9.sp,
                                            color = DashboardFondoConfig.ColorTextoSecundario
                                        )
                                    }
                                }
                            }

                            // Opción 2: Foto de Correo Google
                            val esCorreoActivo = tipoFotoSeleccionada == "CORREO"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (esCorreoActivo) DashboardFondoConfig.ColorContenedorAzul.copy(alpha = 0.4f) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(
                                    if (esCorreoActivo) 2.dp else 1.dp,
                                    if (esCorreoActivo) Color(0xFF2563EB) else DashboardFondoConfig.ColorBordeClaro
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (!googlePhoto.isNullOrBlank()) {
                                            tipoFotoSeleccionada = "CORREO"
                                            PreferenciasApp.carnetTipoFoto = "CORREO"
                                            Toast.makeText(context, "Mostrando Foto del Correo en carnet", Toast.LENGTH_SHORT).show()
                                        } else if (!tieneGoogle) {
                                            Toast.makeText(context, "Vincula tu cuenta Google para usar su foto", Toast.LENGTH_LONG).show()
                                            isGoogleAuthLoading = true
                                            googleHelper.abrirSelector()
                                        } else {
                                            tipoFotoSeleccionada = "CORREO"
                                            PreferenciasApp.carnetTipoFoto = "CORREO"
                                            Toast.makeText(context, "Mostrando imagen de cuenta Google vinculada", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE2E8F0))
                                            .border(
                                                2.dp,
                                                if (esCorreoActivo) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!googlePhoto.isNullOrBlank()) {
                                            AsyncImage(
                                                model = com.example.util.SanitizadorImagenUrl.obtenerUrlEfectiva(googlePhoto),
                                                contentDescription = "Foto Google",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Text(
                                        text = "Foto del Correo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DashboardFondoConfig.ColorTextoPrimario
                                    )

                                    Text(
                                        text = if (!googlePhoto.isNullOrBlank()) "Cuenta Google" else if (tieneGoogle) "Sin foto en correo" else "Sin vincular",
                                        fontSize = 9.sp,
                                        color = DashboardFondoConfig.ColorTextoSecundario
                                    )

                                    if (esCorreoActivo) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF2563EB)
                                        ) {
                                            Text(
                                                text = "EN USO",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = if (!tieneGoogle) "Toca para vincular" else "Toca para elegir",
                                            fontSize = 9.sp,
                                            color = if (!tieneGoogle) Color(0xFF2563EB) else DashboardFondoConfig.ColorTextoSecundario,
                                            fontWeight = if (!tieneGoogle) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Incomplete profile warning banner
            if (currentMember != null && !currentMember.isProfileComplete) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorContenedorDorado),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FICHA DE PERFIL INCOMPLETA", fontWeight = FontWeight.Black, color = Color(0xFF92400E), fontSize = 13.sp)
                                Text("Por favor actualiza tus datos personales, ficha médica SOS y fotos.", color = DashboardFondoConfig.ColorTextoPrimario, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showEditProfileDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("EDITAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Quick Actions: Edit Profile + Switch Demo Member (Estrictamente Directivos o Desarrolladores)
            item {
                val canSwitchPilot = currentMember?.isDirectiva == true ||
                        currentMember?.role == MemberRole.DESARROLLADOR ||
                        isDirectivaMode ||
                        isLeaderSuperAdmin

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                        shape = RoundedCornerShape(10.dp),
                        modifier = if (canSwitchPilot) Modifier.weight(1f).testTag("btn_edit_profile") else Modifier.fillMaxWidth().testTag("btn_edit_profile")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar Mi Perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (canSwitchPilot) {
                        OutlinedButton(
                            onClick = { showMemberSelectorDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_switch_member")
                        ) {
                            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp), tint = DashboardFondoConfig.ColorDoradoOro)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar Piloto (Dev)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                        }
                    }
                }
            }

            // Google Sign-In: Vincular cuenta Google para sincronizar en la nube
            if (currentMember != null) {
                item {
                    val yaVinculada = !currentMember.firebaseUid.isNullOrBlank() || !currentMember.email.isNullOrBlank()
                    val correoVinculado = currentMember.email?.ifBlank { null }
                        ?: (if (currentMember.role == MemberRole.DESARROLLADOR) {
                            if (currentMember.memberNumber == "TX-DEV-001") "eduardo.androide.em@gmail.com" else "eduardo.jose.marquez.matos@gmail.com"
                        } else null)
                        ?: currentMember.firebaseUid
                        ?: "Sincronizado"

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                        border = BorderStroke(1.dp, if (yaVinculada) Color(0xFF16A34A).copy(alpha = 0.4f) else DashboardFondoConfig.ColorBordeClaro),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // --- FILA 1: ESTADO DE LA CUENTA & CORREO VINCULADO (ESPACIO COMPLETO) ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (yaVinculada) DashboardFondoConfig.ColorContenedorVerde else DashboardFondoConfig.ColorContenedorAzul,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = if (yaVinculada) Color(0xFF15803D) else Color(0xFF2563EB),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (yaVinculada) "Cuenta Google Vinculada" else "Sincronizar con la Nube",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = DashboardFondoConfig.ColorTextoPrimario
                                        )
                                        if (yaVinculada) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Vinculado",
                                                tint = Color(0xFF15803D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (yaVinculada) "Cuenta: $correoVinculado" else "Vincula tu cuenta Google para backup y acceso",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (yaVinculada) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (yaVinculada) Color(0xFF15803D) else DashboardFondoConfig.ColorTextoSecundario
                                    )
                                }

                                if (!yaVinculada) {
                                    Button(
                                        onClick = {
                                            val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                                            if (availability != ConnectionResult.SUCCESS) {
                                                val activity = context as? android.app.Activity
                                                    ?: return@Button
                                                GoogleApiAvailability.getInstance().getErrorDialog(activity, availability, 9001)?.show()
                                                Toast.makeText(context, "Se requiere Google Play Services para vincular", Toast.LENGTH_LONG).show()
                                                return@Button
                                            }
                                            isGoogleAuthLoading = true
                                            googleHelper.abrirSelector()
                                        },
                                        enabled = !isGoogleAuthLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        if (isGoogleAuthLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                        } else {
                                            Text("Vincular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }

                            // --- FILA 2: ACCIONES DE VINCULACIÓN (CAMBIAR / DESVINCULAR) ---
                            if (yaVinculada) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                                            if (availability != ConnectionResult.SUCCESS) {
                                                val activity = context as? android.app.Activity
                                                    ?: return@OutlinedButton
                                                GoogleApiAvailability.getInstance().getErrorDialog(activity, availability, 9001)?.show()
                                                Toast.makeText(context, "Se requiere Google Play Services para cambiar correo", Toast.LENGTH_LONG).show()
                                                return@OutlinedButton
                                            }
                                            isGoogleAuthLoading = true
                                            googleHelper.abrirSelector()
                                        },
                                        enabled = !isGoogleAuthLoading,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(34.dp)
                                    ) {
                                        if (isGoogleAuthLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = Color(0xFF4285F4)
                                            )
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF4285F4))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cambiar Correo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { showDesvincularConfirmDialog = true },
                                        enabled = !isGoogleAuthLoading,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFFFFF1F2)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(34.dp)
                                    ) {
                                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFE11D48))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Desvincular", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                                    }
                                }
                            }

                            // --- FILA 3: CÓDIGO DE ACCESO Y SWITCH DEV ---
                            HorizontalDivider(color = DashboardFondoConfig.ColorBordeClaro)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showCambiarCodigoDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = MotoOrangePrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Código de Acceso", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                                }

                                val esDev = currentMember.role == MemberRole.DESARROLLADOR
                                if (esDev) {
                                    OutlinedButton(
                                        onClick = { showCambiarDevDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFEFF6FF)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2563EB))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cambiar Cuenta Dev", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // Pilot Milestones & Experience Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = DashboardFondoConfig.ColorDoradoOro)
                            Text(
                                text = "HISTORIAL Y ESTADÍSTICAS DEL PILOTO",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = DashboardFondoConfig.ColorTextoPrimario,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("KM RECORRIDOS", fontSize = 8.sp, color = DashboardFondoConfig.ColorTextoSecundario, fontWeight = FontWeight.Bold)
                                    Text("${currentMember?.totalKmRidden?.toInt() ?: 0} KM", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFB45309))
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("TOP VELOCIDAD", fontSize = 8.sp, color = DashboardFondoConfig.ColorTextoSecundario, fontWeight = FontWeight.Bold)
                                    Text("${currentMember?.topSpeedRecordKmh?.toInt() ?: 0} KM/H", fontSize = 13.sp, fontWeight = FontWeight.Black, color = DashboardFondoConfig.ColorRojoCarrera)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("PUNTOS MÉRITO", fontSize = 8.sp, color = DashboardFondoConfig.ColorTextoSecundario, fontWeight = FontWeight.Bold)
                                    Text("${currentMember?.meritPoints ?: 0} PTS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                                }
                            }
                        }
                    }
                }
            }

            // Copiloto Oficial y Guantera Digital
            if (currentMember != null) {
                item {
                    var showGuanteraDialog by remember { mutableStateOf(false) }
                    var showCopilotoDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Guantera Digital
                        val docsCount = listOf(
                            currentMember.licenseImageUri,
                            currentMember.medicalCertImageUri,
                            currentMember.bikeRegImageUri,
                            currentMember.insuranceImageUri
                        ).count { !it.isNullOrBlank() }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                            border = BorderStroke(1.dp, if (docsCount > 0) DashboardFondoConfig.ColorDoradoOro.copy(alpha = 0.5f) else DashboardFondoConfig.ColorBordeClaro),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable { showGuanteraDialog = true }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DashboardFondoConfig.ColorContenedorDorado,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Guantera\nDigital", fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario, textAlign = TextAlign.Center, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (docsCount > 0) "$docsCount/4 Docs" else "Sin documentos",
                                    fontSize = 10.sp,
                                    color = if (docsCount > 0) Color(0xFFB45309) else DashboardFondoConfig.ColorTextoSecundario
                                )
                            }
                        }

                        // Copiloto Oficial
                        val tieneCopiloto = !currentMember.copilotName.isNullOrBlank()

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                            border = BorderStroke(1.dp, if (tieneCopiloto) DashboardFondoConfig.ColorRojoCarrera.copy(alpha = 0.5f) else DashboardFondoConfig.ColorBordeClaro),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable { showCopilotoDialog = true }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DashboardFondoConfig.ColorContenedorRojo,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = DashboardFondoConfig.ColorRojoCarrera, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Copiloto\nOficial", fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario, textAlign = TextAlign.Center, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (tieneCopiloto) currentMember.copilotName!!.take(12) else "Sin asignar",
                                    fontSize = 10.sp,
                                    color = if (tieneCopiloto) DashboardFondoConfig.ColorRojoCarrera else DashboardFondoConfig.ColorTextoSecundario
                                )
                            }
                        }
                    }

                    if (showGuanteraDialog) {
                        GuanteraDigitalDialog(
                            member = currentMember,
                            onDismiss = { showGuanteraDialog = false },
                            onUpdate = { onUpdateProfile(it) }
                        )
                    }
                    if (showCopilotoDialog) {
                        CopilotoOficialDialog(
                            member = currentMember,
                            allMembers = allMembers,
                            onDismiss = { showCopilotoDialog = false },
                            onUpdate = { onUpdateProfile(it) }
                        )
                    }
                }
            }

            // ─── CERRAR SESIÓN DEL PILOTO ─────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorContenedorRojo),
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorRojoCarrera.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = DashboardFondoConfig.ColorRojoCarrera,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "SESIÓN DE USUARIO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = DashboardFondoConfig.ColorRojoCarrera
                            )
                        }
                        Text(
                            text = "Al cerrar sesión, la aplicación limpiará el perfil activo y volverá a la pantalla de inicio para que puedas ingresar con otra cuenta.",
                            fontSize = 11.sp,
                            color = DashboardFondoConfig.ColorTextoSecundario
                        )
                        Button(
                            onClick = { showLogoutConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

        }
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            containerColor = DashboardFondoConfig.ColorTarjetaClara,
            title = {
                Text(
                    "¿Cerrar Sesión?",
                    fontWeight = FontWeight.Black,
                    color = DashboardFondoConfig.ColorTextoPrimario
                )
            },
            text = {
                Text(
                    "Se cerrará la sesión del piloto actual y la aplicación quedará lista para ingresar con una nueva cuenta, sin dejar fotos ni datos anteriores.",
                    fontSize = 13.sp,
                    color = DashboardFondoConfig.ColorTextoSecundario
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera)
                ) {
                    Text("Sí, Cerrar Sesión", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancelar", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }

    if (showDesvincularConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDesvincularConfirmDialog = false },
            containerColor = DashboardFondoConfig.ColorTarjetaClara,
            icon = {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "¿Desvincular Cuenta?",
                    fontWeight = FontWeight.Black,
                    color = DashboardFondoConfig.ColorTextoPrimario
                )
            },
            text = {
                Text(
                    "¿Estás seguro de desvincular tu cuenta? Al hacerlo, tu correo y código quedarán 100% libres para nuevas vinculaciones y se cerrará la sesión de forma limpia para evitar cruce de datos con otros dispositivos.",
                    fontSize = 13.sp,
                    color = DashboardFondoConfig.ColorTextoSecundario
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDesvincularConfirmDialog = false
                        onDesvincularGoogle()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Sí, Desvincular", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDesvincularConfirmDialog = false }) {
                    Text("Cancelar", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }

    // Diálogo para Cambiar Código de Acceso Personalizado
    if (showCambiarCodigoDialog) {
        var nuevoCodigoInput by remember { mutableStateOf("") }
        var isSavingCode by remember { mutableStateOf(false) }
        var errorCodeMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSavingCode) showCambiarCodigoDialog = false },
            containerColor = Color.White,
            icon = {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MotoOrangePrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Personalizar Código de Acceso",
                    fontWeight = FontWeight.Black,
                    color = DashboardFondoConfig.ColorTextoPrimario,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresa tu nuevo código personal (mínimo 5 caracteres). Este código quedará vinculado a tu correo y servirá como tu contraseña única de acceso.",
                        fontSize = 12.5.sp,
                        color = DashboardFondoConfig.ColorTextoSecundario
                    )

                    OutlinedTextField(
                        value = nuevoCodigoInput,
                        onValueChange = {
                            nuevoCodigoInput = it.uppercase()
                            errorCodeMsg = null
                        },
                        label = { Text("Nuevo Código") },
                        placeholder = { Text("Ej: MI-CLAVE-2026") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoOrangePrimary,
                            focusedLabelColor = MotoOrangePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorCodeMsg != null) {
                        Text(
                            text = errorCodeMsg ?: "",
                            color = Color(0xFFE11D48),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val codigoLimpio = nuevoCodigoInput.trim()
                        if (codigoLimpio.length < 5) {
                            errorCodeMsg = "El código debe tener al menos 5 caracteres."
                            return@Button
                        }
                        isSavingCode = true
                        errorCodeMsg = null
                        coroutineScope.launch {
                            val (exito, mensaje) = onCambiarCodigoAcceso(codigoLimpio)
                            isSavingCode = false
                            if (exito) {
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                showCambiarCodigoDialog = false
                            } else {
                                errorCodeMsg = mensaje
                            }
                        }
                    },
                    enabled = !isSavingCode,
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    if (isSavingCode) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Guardar Código", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCambiarCodigoDialog = false },
                    enabled = !isSavingCode
                ) {
                    Text("Cancelar", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }

    // Diálogo Switch de Cuenta Exclusivo para Desarrolladores
    if (showCambiarDevDialog) {
        var isSwitching by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSwitching) showCambiarDevDialog = false },
            containerColor = Color.White,
            icon = {
                Icon(
                    Icons.Default.ManageAccounts,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Cambiar de Cuenta (Dev Switch)",
                    fontWeight = FontWeight.Black,
                    color = DashboardFondoConfig.ColorTextoPrimario,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Selecciona la cuenta de desarrollador a la que deseas alternar. El sistema ejecutará un reinicio absoluto para cargar los datos limpios de la cuenta elegida.",
                        fontSize = 12.5.sp,
                        color = DashboardFondoConfig.ColorTextoSecundario
                    )

                    // Opción Dev 1
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isSwitching) {
                            isSwitching = true
                            coroutineScope.launch {
                                val (ok, msg) = onCambiarCuentaDev("DESARROLLO1")
                                isSwitching = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (ok) showCambiarDevDialog = false
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("👑 Desarrollador 1 (Dev EM)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("eduardo.androide.em@gmail.com • Ficha: TX-DEV-001", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    // Opción Dev 2
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isSwitching) {
                            isSwitching = true
                            coroutineScope.launch {
                                val (ok, msg) = onCambiarCuentaDev("DESARROLLO2")
                                isSwitching = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (ok) showCambiarDevDialog = false
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("👑 Desarrollador 2 (Dev Matos)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("eduardo.jose.marquez.matos@gmail.com • Ficha: TX-DEV-002", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showCambiarDevDialog = false },
                    enabled = !isSwitching
                ) {
                    Text("Cerrar", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }

    if (showEditProfileDialog && currentMember != null) {
        EditProfileDialog(
            member = currentMember,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updated ->
                onUpdateProfile(updated)
                showEditProfileDialog = false
            }
        )
    }

    if (showMemberSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showMemberSelectorDialog = false },
            containerColor = DashboardFondoConfig.ColorTarjetaClara,
            title = { Text("Seleccionar Miembro (Demo)", fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(allMembers) { mem ->
                        val isSelected = mem.id == currentMember?.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DashboardFondoConfig.ColorContenedorRojo else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) DashboardFondoConfig.ColorRojoCarrera else DashboardFondoConfig.ColorBordeClaro),
                            onClick = {
                                onSelectMember(mem.id)
                                showMemberSelectorDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${mem.fullName} (${mem.memberNumber})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DashboardFondoConfig.ColorTextoPrimario
                                    )
                                    Text(
                                        text = "${mem.role.displayName} • ${mem.bikeModel}",
                                        fontSize = 11.sp,
                                        color = DashboardFondoConfig.ColorTextoSecundario
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DashboardFondoConfig.ColorRojoCarrera)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemberSelectorDialog = false }) {
                    Text("Cerrar", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }

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
}
@Composable
fun EditProfileDialog(
    member: MemberProfile,
    onDismiss: () -> Unit,
    onSave: (MemberProfile) -> Unit
) {
    var fullName by remember { mutableStateOf(member.fullName) }
    var nickname by remember { mutableStateOf(member.nickname) }
    var phone by remember { mutableStateOf(member.phone) }
    var cedulaDni by remember { mutableStateOf(member.cedulaDni) }
    var chapterState by remember { mutableStateOf(member.chapterState) }
    
    var profilePhotoUri by remember { mutableStateOf(member.profilePhotoUri) }
    var bikePhotoUri by remember { mutableStateOf(member.bikePhotoUri) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploadingProfile by remember { mutableStateOf(false) }
    var isUploadingBike by remember { mutableStateOf(false) }

    var showProfileImageSourceDialog by remember { mutableStateOf(false) }
    var showBikeImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTarget by remember { mutableStateOf<String?>(null) } // "profile" or "bike"

    // Helper for creating camera URI
    fun createCameraUri(prefix: String): Uri {
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            val uriToUpload = tempCameraUri!!
            val target = cameraTarget
            coroutineScope.launch {
                if (target == "profile") {
                    isUploadingProfile = true
                    val bytes = context.contentResolver.openInputStream(uriToUpload)?.readBytes()
                    if (bytes != null) {
                        val url = NubeArchivos.subirBytes(bytes, NubeArchivos.TipoArchivo.IMAGEN, "profile_${member.id}_${System.currentTimeMillis()}.jpg")
                        if (url != null) {
                            profilePhotoUri = url
                            com.example.radar.RadarFirebase.actualizarAvatarLocalDesdeUri(context, url)
                            PreferenciasApp.carnetFotoPerfil = url
                            Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al subir la foto de perfil", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_SHORT).show()
                    }
                    isUploadingProfile = false
                } else if (target == "bike") {
                    isUploadingBike = true
                    val bytes = context.contentResolver.openInputStream(uriToUpload)?.readBytes()
                    if (bytes != null) {
                        val url = NubeArchivos.subirBytes(bytes, NubeArchivos.TipoArchivo.IMAGEN, "bike_${member.id}_${System.currentTimeMillis()}.jpg")
                        if (url != null) {
                            bikePhotoUri = url
                            Toast.makeText(context, "Foto de la moto actualizada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al subir la foto de la moto", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_SHORT).show()
                    }
                    isUploadingBike = false
                }
            }
        }
    }

    val profileGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploadingProfile = true
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                if (bytes != null) {
                    val url = NubeArchivos.subirBytes(bytes, NubeArchivos.TipoArchivo.IMAGEN, "profile_${member.id}_${System.currentTimeMillis()}.jpg")
                    if (url != null) {
                        profilePhotoUri = url
                        com.example.radar.RadarFirebase.actualizarAvatarLocalDesdeUri(context, url)
                        PreferenciasApp.carnetFotoPerfil = url
                        Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_SHORT).show()
                }
                isUploadingProfile = false
            }
        }
    }

    val bikeGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploadingBike = true
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                if (bytes != null) {
                    val url = NubeArchivos.subirBytes(bytes, NubeArchivos.TipoArchivo.IMAGEN, "bike_${member.id}_${System.currentTimeMillis()}.jpg")
                    if (url != null) {
                        bikePhotoUri = url
                        Toast.makeText(context, "Foto de la moto actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_SHORT).show()
                }
                isUploadingBike = false
            }
        }
    }

    // Motorcycle specs
    var bikeBrand by remember { mutableStateOf(member.bikeBrand) }
    var bikeModel by remember { mutableStateOf(member.bikeModel) }
    var bikeColor by remember { mutableStateOf(member.bikeColor) }
    var bikeDisplacementCc by remember { mutableStateOf(member.bikeDisplacementCc) }
    var bikeTankCapacityLiters by remember { mutableStateOf(member.bikeTankCapacityLiters) }
    var bikeYear by remember { mutableStateOf(member.bikeYear) }
    var bikePlate by remember { mutableStateOf(member.bikePlate) }

    // Medical & SOS
    var bloodType by remember { mutableStateOf(member.bloodType) }
    var medicalNotes by remember { mutableStateOf(member.medicalNotes) }
    var emergencyContactName by remember { mutableStateOf(member.emergencyContactName) }
    var emergencyContactRelation by remember { mutableStateOf(member.emergencyContactRelation) }
    var emergencyContactPhone by remember { mutableStateOf(member.emergencyContactPhone) }

    val bloodOptions = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    var showBloodMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DashboardFondoConfig.ColorTarjetaClara,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = DashboardFondoConfig.ColorRojoCarrera)
                Text("Editar Ficha y Perfil de Piloto", fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Photo UI
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .border(2.dp, DashboardFondoConfig.ColorDoradoOro, CircleShape)
                                .clickable { showProfileImageSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingProfile) {
                                CircularProgressIndicator(color = DashboardFondoConfig.ColorDoradoOro, modifier = Modifier.size(24.dp))
                            } else if (profilePhotoUri != null) {
                                AsyncImage(
                                    model = com.example.util.SanitizadorImagenUrl.obtenerUrlEfectiva(profilePhotoUri),
                                    contentDescription = "Foto de Perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = DashboardFondoConfig.ColorTextoSecundario, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Toca para cambiar foto de perfil", fontSize = 10.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                    }
                }
                item {
                    Text(
                        text = "DATOS PERSONALES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = DashboardFondoConfig.ColorDoradoOro
                    )
                }
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre y Apellido") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_fullname")
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("Alias / Apodo") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cedulaDni,
                            onValueChange = { cedulaDni = it },
                            label = { Text("Cédula / DNI") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono WhatsApp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1.1f)
                        )
                        OutlinedTextField(
                            value = chapterState,
                            onValueChange = { chapterState = it },
                            label = { Text("Capítulo / Estado") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Section: Motorcycle Specs
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DATOS DE LA MOTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = DashboardFondoConfig.ColorDoradoOro
                    )
                }
                
                // Bike Photo UI
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, DashboardFondoConfig.ColorBordeClaro, RoundedCornerShape(8.dp))
                                .clickable { showBikeImageSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingBike) {
                                CircularProgressIndicator(color = DashboardFondoConfig.ColorDoradoOro, modifier = Modifier.size(32.dp))
                            } else if (bikePhotoUri != null) {
                                AsyncImage(
                                    model = com.example.util.SanitizadorImagenUrl.obtenerUrlEfectiva(bikePhotoUri),
                                    contentDescription = "Foto de la moto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = DashboardFondoConfig.ColorTextoSecundario, modifier = Modifier.size(48.dp))
                                    Text("Añadir foto de la moto", color = DashboardFondoConfig.ColorTextoSecundario, fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Toca para cambiar foto de la moto", fontSize = 10.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bikeBrand,
                            onValueChange = { bikeBrand = it },
                            label = { Text("Marca") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeModel,
                            onValueChange = { bikeModel = it },
                            label = { Text("Modelo") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bikeColor,
                            onValueChange = { bikeColor = it },
                            label = { Text("Color") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeDisplacementCc,
                            onValueChange = { bikeDisplacementCc = it },
                            label = { Text("Cilindrada (cc)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bikeTankCapacityLiters,
                            onValueChange = { bikeTankCapacityLiters = it },
                            label = { Text("Tanque (ej: 11.5 L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeYear,
                            onValueChange = { bikeYear = it },
                            label = { Text("Año (ej: 2023)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = bikePlate,
                            onValueChange = { bikePlate = it },
                            label = { Text("Placa") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Section: Medical & Emergency SOS
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FICHA MÉDICA Y CONTACTO SOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = DashboardFondoConfig.ColorDoradoOro
                    )
                }
                item {
                    // Blood Type Dropdown / Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, DashboardFondoConfig.ColorBordeClaro, RoundedCornerShape(8.dp))
                            .clickable { showBloodMenu = true }
                            .padding(12.dp)
                    ) {
                        Text("Tipo de Sangre:", color = DashboardFondoConfig.ColorTextoPrimario, fontWeight = FontWeight.Bold)
                        Text(bloodType, color = DashboardFondoConfig.ColorRojoCarrera, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        DropdownMenu(
                            expanded = showBloodMenu,
                            onDismissRequest = { showBloodMenu = false }
                        ) {
                            bloodOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        bloodType = option
                                        showBloodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = medicalNotes,
                        onValueChange = { medicalNotes = it },
                        label = { Text("Notas Médicas / Alergias") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emergencyContactName,
                            onValueChange = { emergencyContactName = it },
                            label = { Text("Contacto SOS (Nombre)") },
                            modifier = Modifier.weight(1.1f)
                        )
                        OutlinedTextField(
                            value = emergencyContactRelation,
                            onValueChange = { emergencyContactRelation = it },
                            label = { Text("Parentesco") },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = { Text("Teléfono de Emergencia") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initials = if (nickname.isNotBlank()) {
                        nickname.take(2).uppercase()
                    } else {
                        fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    }
                    val updated = member.copy(
                        fullName = fullName,
                        nickname = nickname,
                        cedulaDni = cedulaDni,
                        phone = phone,
                        chapterState = chapterState,
                        bikeBrand = bikeBrand,
                        bikeModel = bikeModel,
                        bikeColor = bikeColor,
                        bikeDisplacementCc = bikeDisplacementCc,
                        bikeTankCapacityLiters = bikeTankCapacityLiters,
                        bikeYear = bikeYear,
                        bikePlate = bikePlate,
                        bloodType = bloodType,
                        medicalNotes = medicalNotes,
                        emergencyContactName = emergencyContactName,
                        emergencyContactRelation = emergencyContactRelation,
                        emergencyContactPhone = emergencyContactPhone,
                        avatarInitials = initials.ifBlank { "TX" },
                        profilePhotoUri = profilePhotoUri,
                        bikePhotoUri = bikePhotoUri
                    )
                    PreferenciasApp.carnetFotoPerfil = profilePhotoUri ?: ""
                    PreferenciasApp.carnetFotoMoto = bikePhotoUri ?: ""
                    PreferenciasApp.carnetMotoMarca = bikeBrand
                    PreferenciasApp.carnetMotoModelo = bikeModel
                    PreferenciasApp.carnetMotoPlaca = bikePlate
                    PreferenciasApp.carnetMotoColor = bikeColor
                    PreferenciasApp.carnetSangre = bloodType
                    PreferenciasApp.carnetSosNombre = emergencyContactName
                    PreferenciasApp.carnetSosTelefono = emergencyContactPhone

                    if (!profilePhotoUri.isNullOrBlank()) {
                        com.example.radar.RadarFirebase.actualizarAvatarLocalDesdeUri(context, profilePhotoUri!!)
                    }
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                modifier = Modifier.testTag("btn_confirm_save_profile")
            ) {
                Text("Guardar Ficha", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DashboardFondoConfig.ColorTextoSecundario)
            }
        }
    )

    // Modal: Selector de Fuente para Foto de Perfil (Cámara / Galería)
    if (showProfileImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showProfileImageSourceDialog = false },
            containerColor = DashboardFondoConfig.ColorTarjetaClara,
            title = {
                Text("Foto de Perfil de Piloto", fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona el origen de la foto:", fontSize = 12.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                    
                    Button(
                        onClick = {
                            showProfileImageSourceDialog = false
                            try {
                                try {
                                    val uri = createCameraUri("profile")
                                    tempCameraUri = uri
                                    cameraTarget = "profile"
                                    cameraLauncher.launch(uri)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    Toast.makeText(context, "No se encontró aplicación de cámara", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar Foto con Cámara", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            showProfileImageSourceDialog = false
                            profileGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = DashboardFondoConfig.ColorDoradoOro)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de Galería", color = DashboardFondoConfig.ColorTextoPrimario, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProfileImageSourceDialog = false }) {
                    Text("Cancelar", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }

    // Modal: Selector de Fuente para Foto de la Moto (Cámara / Galería)
    if (showBikeImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showBikeImageSourceDialog = false },
            containerColor = DashboardFondoConfig.ColorTarjetaClara,
            title = {
                Text("Foto de la Moto Keeway TX", fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona el origen de la foto:", fontSize = 12.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                    
                    Button(
                        onClick = {
                            showBikeImageSourceDialog = false
                            try {
                                try {
                                    val uri = createCameraUri("bike")
                                    tempCameraUri = uri
                                    cameraTarget = "bike"
                                    cameraLauncher.launch(uri)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    Toast.makeText(context, "No se encontró aplicación de cámara", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar Foto con Cámara", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            showBikeImageSourceDialog = false
                            bikeGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = DashboardFondoConfig.ColorDoradoOro)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de Galería", color = DashboardFondoConfig.ColorTextoPrimario, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBikeImageSourceDialog = false }) {
                    Text("Cancelar", color = DashboardFondoConfig.ColorTextoSecundario)
                }
            }
        )
    }
}

// ─── Diálogo: Guantera Digital ────────────────────────────────────────────────

private data class DocInfo(val key: String, val label: String, val uri: String?, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun GuanteraDigitalDialog(
    member: MemberProfile,
    onDismiss: () -> Unit,
    onUpdate: (MemberProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadTarget by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val target = uploadTarget ?: return@let
            coroutineScope.launch {
                isUploading = true
                val url = NubeArchivos.subirArchivo(it, NubeArchivos.TipoArchivo.IMAGEN, "guantera_${member.id}_${target}_${System.currentTimeMillis()}.jpg")
                if (url != null) {
                    val updated = when (target) {
                        "license" -> member.copy(licenseImageUri = url)
                        "medical" -> member.copy(medicalCertImageUri = url)
                        "bikeReg" -> member.copy(bikeRegImageUri = url)
                        "insurance" -> member.copy(insuranceImageUri = url)
                        else -> member
                    }
                    onUpdate(updated)
                    Toast.makeText(context, "Documento subido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al subir documento", Toast.LENGTH_SHORT).show()
                }
                isUploading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = TxGoldSecondary)
                Text("Guantera Digital", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Documentos del piloto vinculados a tu cuenta.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val documents = listOf(
                    DocInfo("license", "Licencia de Conducir", member.licenseImageUri, Icons.Default.Badge),
                    DocInfo("medical", "Certificado Médico", member.medicalCertImageUri, Icons.Default.LocalHospital),
                    DocInfo("bikeReg", "Registro de la Moto", member.bikeRegImageUri, Icons.Default.DirectionsBike),
                    DocInfo("insurance", "Seguro Vehicular", member.insuranceImageUri, Icons.Default.Shield)
                )

                documents.forEach { doc ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(doc.icon, contentDescription = null, tint = TxGoldSecondary, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                if (!doc.uri.isNullOrBlank()) {
                                    Text("Vinculado", fontSize = 10.sp, color = StatusSuccess)
                                } else {
                                    Text("Sin documento", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (!isUploading) {
                                IconButton(onClick = {
                                    uploadTarget = doc.key
                                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (!doc.uri.isNullOrBlank()) Icons.Default.Edit else Icons.Default.Add,
                                        contentDescription = if (!doc.uri.isNullOrBlank()) "Actualizar" else "Agregar",
                                        tint = TxGoldSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else if (uploadTarget == doc.key) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TxGoldSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ─── Diálogo: Copiloto Oficial ────────────────────────────────────────────────
@Composable
fun CopilotoOficialDialog(
    member: MemberProfile,
    allMembers: List<MemberProfile>,
    onDismiss: () -> Unit,
    onUpdate: (MemberProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showManualEntry by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualRelation by remember { mutableStateOf("") }
    var showRequestCode by remember { mutableStateOf(false) }

    val activeMembers = allMembers.filter {
        it.id != member.id && it.role != MemberRole.DISCIPLINARIO
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.People, contentDescription = null, tint = TxFlameRed)
                Text("Copiloto Oficial", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!member.copilotName.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = TxFlameRed.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, TxFlameRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.copilotName!!, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(member.copilotRelation ?: "Copiloto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                onUpdate(member.copy(copilotName = null, copilotRelation = null))
                                Toast.makeText(context, "Copiloto removido", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = TxFlameRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }

                Text(
                    if (member.copilotName.isNullOrBlank()) "Selecciona un miembro activo del Team:" else "Cambiar copiloto:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(activeMembers) { m ->
                        val isSelected = member.copilotName == m.fullName
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) TxFlameRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (isSelected) BorderStroke(1.dp, TxFlameRed) else null,
                            onClick = {
                                onUpdate(member.copy(copilotName = m.fullName, copilotRelation = "Miembro del Team"))
                                Toast.makeText(context, "Copiloto asignado: ${m.fullName}", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(m.avatarInitials.take(2), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.fullName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(m.memberNumber, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Opción manual
                if (!showManualEntry) {
                    OutlinedButton(
                        onClick = { showManualEntry = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ingresar nombre manualmente", fontSize = 12.sp)
                    }
                } else {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Nombre completo del copiloto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualRelation,
                        onValueChange = { manualRelation = it },
                        label = { Text("Parentesco (Ej: Esposo, Hermano)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (manualName.isNotBlank()) {
                                onUpdate(member.copy(copilotName = manualName.trim(), copilotRelation = manualRelation.trim().ifBlank { "Personal" }))
                                Toast.makeText(context, "Copiloto asignado: ${manualName.trim()}", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        enabled = manualName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
                    ) {
                        Text("Guardar Copiloto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Solicitar código a la Directiva
                if (!showRequestCode) {
                    OutlinedButton(
                        onClick = { showRequestCode = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TxGoldSecondary)
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = TxGoldSecondary)
                        Spacer(Modifier.width(6.dp))
                        Text("Solicitar Código a la Directiva", fontSize = 12.sp, color = TxGoldSecondary)
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = TxGoldSecondary.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, TxGoldSecondary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Solicitud de Código de Copiloto", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TxGoldSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Se enviará una solicitud a la Directiva para generar un código de aprobación para tu copiloto. La directiva revisará y aprobará la solicitud.",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val solicitud = ChatMessage(
                                        id = System.currentTimeMillis(),
                                        channelId = "DIRECTIVA",
                                        senderMemberId = member.id,
                                        senderName = member.fullName,
                                        senderNickname = member.nickname,
                                        senderMemberNumber = member.memberNumber,
                                        senderRole = member.role,
                                        senderCustomRoleTitle = null,
                                        senderInitials = member.avatarInitials,
                                        messageText = "SOLICITUD DE CÓDIGO COPILOTOficial: ${member.fullName} solicita un código de aprobación para registrar a su copiloto oficial. Pendiente de revisión por la Directiva.",
                                        isRadioCallout = true,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    coroutineScope.launch {
                                        val db = com.example.data.local.AppDatabase.getDatabase(context, coroutineScope)
                                        db.chatDao().insertMessage(solicitud)
                                    }
                                    Toast.makeText(context, "Solicitud enviada a la Directiva", Toast.LENGTH_SHORT).show()
                                    showRequestCode = false
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TxGoldSecondary)
                            ) {
                                Text("Enviar Solicitud", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        }
    )
}
