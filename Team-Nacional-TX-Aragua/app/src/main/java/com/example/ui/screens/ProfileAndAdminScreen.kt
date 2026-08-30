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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.NubeArchivos
import com.example.data.model.*
import com.example.ui.components.DigitalCredentialCard
import com.example.ui.components.openUrl
import com.example.ui.theme.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.launch

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
    onUnlockWithMasterCode: suspend (String) -> Pair<Boolean, String> = { _ -> Pair(false, "") },
    modifier: Modifier = Modifier
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMemberSelectorDialog by remember { mutableStateOf(false) }
    var isGoogleAuthLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // ═══════════════════════════════════════════════
    // GOOGLE SIGN-IN: Usando módulo centralizado
    // ═══════════════════════════════════════════════
    val googleHelper = com.aistudio.teamtxvzla.nube.AutenticacionGoogle.recordarLauncherGoogle(
        onExito = { datos ->
            isGoogleAuthLoading = false
            onVincularGoogle(datos.uid, datos.correo, datos.fotoUrl)
        },
        onError = { mensaje ->
            isGoogleAuthLoading = false
            android.widget.Toast.makeText(context, mensaje, android.widget.Toast.LENGTH_SHORT).show()
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Member Digital Credential Card
            item {
                if (currentMember != null) {
                    DigitalCredentialCard(member = currentMember)
                }
            }

            // Incomplete profile warning banner
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
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FICHA DE PERFIL INCOMPLETA", fontWeight = FontWeight.Black, color = TxGoldLight, fontSize = 13.sp)
                                Text("Por favor actualiza tus datos personales, ficha médica SOS y fotos.", color = Color.White, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showEditProfileDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("EDITAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Quick Actions: Edit Profile + Switch Demo Member
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_edit_profile")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar Mi Perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showMemberSelectorDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_switch_member")
                    ) {
                        Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp), tint = TxGoldBrass)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cambiar Piloto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Google Sign-In: Vincular cuenta Google para sincronizar en la nube
            if (currentMember != null) {
                item {
                    val yaVinculada = !currentMember.firebaseUid.isNullOrBlank()
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (yaVinculada) StatusSuccess.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (yaVinculada) StatusSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = if (yaVinculada) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (yaVinculada) "Google Vinculado" else "Sincronizar con la Nube",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (yaVinculada) {
                                        val correo = currentMember.email?.ifBlank { currentMember.firebaseUid ?: "" } ?: (currentMember.firebaseUid ?: "")
                                        if (correo.isNotBlank()) "Cuenta: $correo" else "Sincronizado y respaldado automáticamente"
                                    } else "Vincula tu cuenta Google para backup y acceso desde cualquier dispositivo",
                                    fontSize = 10.sp,
                                    color = if (yaVinculada) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (isGoogleAuthLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text("Vincular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        if (isGoogleAuthLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = Color(0xFF4285F4)
                                            )
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF4285F4))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Cambiar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                                        }
                                    }
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Vinculado", tint = StatusSuccess, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }


            // Pilot Milestones & Experience Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141822)),
                    border = BorderStroke(1.dp, AsphaltDarkBorder),
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
                            Icon(Icons.Default.Speed, contentDescription = null, tint = TxGoldBrass)
                            Text(
                                text = "HISTORIAL Y ESTADÍSTICAS DEL PILOTO",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2230)),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("KM RECORRIDOS", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text("${currentMember?.totalKmRidden?.toInt() ?: 0} KM", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TxGoldLight)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2230)),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("TOP VELOCIDAD", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text("${currentMember?.topSpeedRecordKmh?.toInt() ?: 0} KM/H", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2230)),
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("PUNTOS MÉRITO", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text("${currentMember?.meritPoints ?: 0} PTS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = StatusSuccess)
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, if (docsCount > 0) TxGoldSecondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                                    color = TxGoldSecondary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = TxGoldSecondary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Guantera\nDigital", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (docsCount > 0) "$docsCount/4 Docs" else "Sin documentos",
                                    fontSize = 10.sp,
                                    color = if (docsCount > 0) TxGoldSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Copiloto Oficial
                        val tieneCopiloto = !currentMember.copilotName.isNullOrBlank()

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, if (tieneCopiloto) TxFlameRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                                    color = TxFlameRed.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Copiloto\nOficial", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (tieneCopiloto) currentMember.copilotName!!.take(12) else "Sin asignar",
                                    fontSize = 10.sp,
                                    color = if (tieneCopiloto) TxFlameRed else MaterialTheme.colorScheme.onSurfaceVariant
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

        }
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
            title = { Text("Seleccionar Miembro (Demo)", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(allMembers) { mem ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (mem.id == currentMember?.id) MotoOrangePrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (mem.id == currentMember?.id) BorderStroke(1.dp, MotoOrangePrimary) else null,
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
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${mem.role.displayName} • ${mem.bikeModel}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (mem.id == currentMember?.id) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MotoOrangePrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemberSelectorDialog = false }) {
                    Text("Cerrar")
                }
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
                    val url = NubeArchivos.subirArchivo(uriToUpload, NubeArchivos.TipoArchivo.IMAGEN, "profile_${member.id}_${System.currentTimeMillis()}.jpg")
                    if (url != null) {
                        profilePhotoUri = url
                        Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al subir la foto de perfil", Toast.LENGTH_SHORT).show()
                    }
                    isUploadingProfile = false
                } else if (target == "bike") {
                    isUploadingBike = true
                    val url = NubeArchivos.subirArchivo(uriToUpload, NubeArchivos.TipoArchivo.IMAGEN, "bike_${member.id}_${System.currentTimeMillis()}.jpg")
                    if (url != null) {
                        bikePhotoUri = url
                        Toast.makeText(context, "Foto de la moto actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al subir la foto de la moto", Toast.LENGTH_SHORT).show()
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
                val url = NubeArchivos.subirArchivo(it, NubeArchivos.TipoArchivo.IMAGEN, "profile_${member.id}_${System.currentTimeMillis()}.jpg")
                if (url != null) {
                    profilePhotoUri = url
                    Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
                isUploadingProfile = false
            }
        }
    }

    val bikeGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploadingBike = true
                val url = NubeArchivos.subirArchivo(it, NubeArchivos.TipoArchivo.IMAGEN, "bike_${member.id}_${System.currentTimeMillis()}.jpg")
                if (url != null) {
                    bikePhotoUri = url
                    Toast.makeText(context, "Foto de la moto actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
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
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = TxFlameRed)
                Text("Editar Ficha y Perfil de Piloto", fontWeight = FontWeight.Bold)
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
                                .background(Color(0xFF1E2430))
                                .border(2.dp, TxGoldBrass, CircleShape)
                                .clickable { showProfileImageSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingProfile) {
                                CircularProgressIndicator(color = TxGoldBrass, modifier = Modifier.size(24.dp))
                            } else if (profilePhotoUri != null) {
                                AsyncImage(
                                    model = profilePhotoUri,
                                    contentDescription = "Foto de Perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = TxChromeSilver, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Toca para cambiar foto de perfil", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                item {
                    Text(
                        text = "DATOS PERSONALES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TxGoldBrass
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
                        color = TxGoldBrass
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
                                .background(Color(0xFF1E2430))
                                .border(1.dp, TxGoldBrass.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { showBikeImageSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingBike) {
                                CircularProgressIndicator(color = TxGoldBrass, modifier = Modifier.size(32.dp))
                            } else if (bikePhotoUri != null) {
                                AsyncImage(
                                    model = bikePhotoUri,
                                    contentDescription = "Foto de la moto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = TxChromeSilver, modifier = Modifier.size(48.dp))
                                    Text("Añadir foto de la moto", color = TxChromeSilver, fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Toca para cambiar foto de la moto", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bikeBrand,
                            onValueChange = { bikeBrand = it },
                            label = { Text("Marca (ej: Keeway)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeModel,
                            onValueChange = { bikeModel = it },
                            label = { Text("Modelo (ej: TX 200 SM)") },
                            modifier = Modifier.weight(1.2f)
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
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bikeYear,
                            onValueChange = { bikeYear = it },
                            label = { Text("Año (ej: 2023)") },
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
                        color = TxGoldBrass
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
                            .background(Color(0xFF1E2430))
                            .clickable { showBloodMenu = true }
                            .padding(12.dp)
                    ) {
                        Text("Tipo de Sangre:", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(bloodType, color = TxFlameRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
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
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                modifier = Modifier.testTag("btn_confirm_save_profile")
            ) {
                Text("Guardar Ficha")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    // Modal: Selector de Fuente para Foto de Perfil (Cámara / Galería)
    if (showProfileImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showProfileImageSourceDialog = false },
            title = {
                Text("Foto de Perfil de Piloto", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona el origen de la foto:", fontSize = 12.sp, color = TxSteelSilver)
                    
                    Button(
                        onClick = {
                            showProfileImageSourceDialog = false
                            try {
                                val uri = createCameraUri("profile")
                                tempCameraUri = uri
                                cameraTarget = "profile"
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar Foto con Cámara", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showProfileImageSourceDialog = false
                            profileGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TxGoldBrass)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TxGoldBrass)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de Galería", color = TxGoldBrass, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProfileImageSourceDialog = false }) {
                    Text("Cancelar", color = TxSteelSilver)
                }
            },
            containerColor = Color(0xFF1B2230)
        )
    }

    // Modal: Selector de Fuente para Foto de la Moto (Cámara / Galería)
    if (showBikeImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showBikeImageSourceDialog = false },
            title = {
                Text("Foto de la Moto Keeway TX", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona el origen de la foto:", fontSize = 12.sp, color = TxSteelSilver)
                    
                    Button(
                        onClick = {
                            showBikeImageSourceDialog = false
                            try {
                                val uri = createCameraUri("bike")
                                tempCameraUri = uri
                                cameraTarget = "bike"
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar Foto con Cámara", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showBikeImageSourceDialog = false
                            bikeGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TxGoldBrass)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TxGoldBrass)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de Galería", color = TxGoldBrass, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBikeImageSourceDialog = false }) {
                    Text("Cancelar", color = TxSteelSilver)
                }
            },
            containerColor = Color(0xFF1B2230)
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
