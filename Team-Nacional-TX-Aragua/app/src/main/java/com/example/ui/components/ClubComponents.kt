package com.example.ui.components

import com.aistudio.teamtxvzla.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.preferences.PreferenciasApp
import com.example.ui.theme.*
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun VenezuelanFlagRibbon(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(VzlaYellow))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(VzlaBlue))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(VzlaRed))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubTopBar(
    currentMember: MemberProfile? = null,
    isDirectivaMode: Boolean,
    onToggleDirectiva: () -> Unit = {},
    onOpenSosModal: () -> Unit,
    onOpenCarnet: () -> Unit = {},
    isDeveloperMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            var showRoleInfoDialog by remember { mutableStateOf(false) }

            VenezuelanFlagRibbon()
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable {
                            openUrl(context, "https://linktr.ee/Teamnacionaltx200aragua")
                        }
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.logoteam),
                            contentDescription = "Logo Team",
                            modifier = Modifier.size(48.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        
                        // LED de estado del usuario actual
                        val ledColor = when {
                            isDeveloperMode -> StatusSuccess // Verde para desarrollador
                            currentMember?.role == MemberRole.PRESIDENTE -> TxFlameRed // Rojo para líder/presidente
                            currentMember?.role?.canManageApp == true -> TxGoldBrass // Dorado para directiva
                            currentMember != null -> StatusInfo // Azul para usuarios normales
                            else -> Color.Gray // Gris para visitantes no autenticados
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ledColor)
                                .border(1.dp, ledColor.copy(alpha = 0.5f), CircleShape)
                                .clickable { showRoleInfoDialog = true }
                        )
                    }
                },
                actions = {
                    // Telegram link shortcut (más a la izquierda y compacto)
                    IconButton(
                        onClick = {
                            openUrl(context, "https://t.me/+R_oloXwkGqhkNzJh")
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_top_telegram")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram Team TX",
                            tint = TelegramBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // TikTok link shortcut (compacto)
                    IconButton(
                        onClick = {
                            openUrl(context, "https://www.tiktok.com/@teamnacionaltx.aragua")
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_top_tiktok")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "TikTok Team TX",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val isDarkTheme = isSystemInDarkTheme()

                    // Determinación del Rol de la cuenta actual
                    val roleLabel = when {
                        currentMember?.role == MemberRole.COPILOTO -> "COPILOTO"
                        isDirectivaMode || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true) ||
                        (currentMember?.role == MemberRole.PRESIDENTE) || (currentMember?.role == MemberRole.DIRECTIVA) ||
                        (currentMember?.role == MemberRole.VICEPRESIDENTE) || (currentMember?.role == MemberRole.SECRETARIO) ||
                        (currentMember?.role == MemberRole.TESORERO) || (currentMember?.role == MemberRole.CAPITAN_RUTA) ||
                        (currentMember?.role == MemberRole.DISCIPLINARIO) -> "DIRECTIVO"
                        else -> "PILOTO"
                    }

                    val borderColor = when (roleLabel) {
                        "DIRECTIVO" -> TxGoldBrass
                        "COPILOTO" -> Color(0xFF94A3B8)
                        else -> TxFlameRed
                    }

                    // Botón de Perfil / Carnet TX con texto de Rol a la izquierda
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenCarnet)
                            .padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                            .testTag("btn_top_carnet_profile")
                    ) {
                        Text(
                            text = roleLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (roleLabel == "DIRECTIVO") TxGoldBrass else if (roleLabel == "COPILOTO") Color(0xFF94A3B8) else (if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDarkTheme) Color(0xFF262626) else Color(0xFFE2E8F0))
                                .border(1.5.dp, borderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val photoUri = currentMember?.profilePhotoUri
                            if (!photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Carnet TX",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initials = if (currentMember != null && currentMember.fullName.isNotBlank()) {
                                    currentMember.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                                } else "TX"
                                Text(
                                    text = initials.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            )
            
            if (showRoleInfoDialog) {
                val (roleTitle, roleDesc) = when {
                    isDeveloperMode -> Pair("Desarrollador de la App", "Tienes acceso maestro al sistema, todas las funciones de depuración y administración habilitadas.")
                    currentMember?.role == MemberRole.PRESIDENTE -> Pair("Presidente Nacional", currentMember.role.roleDuties)
                    currentMember?.role?.canManageApp == true -> Pair(currentMember.role.displayName, currentMember.role.roleDuties)
                    currentMember != null -> Pair("Miembro Oficial / Piloto", "Piloto registrado en Team TX Venezuela.")
                    else -> Pair("Invitado", "Por favor inicia sesión para acceder a las funciones del club.")
                }
                val ledColor = when {
                    isDeveloperMode -> StatusSuccess
                    currentMember?.role == MemberRole.PRESIDENTE -> TxFlameRed
                    currentMember?.role?.canManageApp == true -> TxGoldBrass
                    currentMember != null -> StatusInfo
                    else -> Color.Gray
                }
                AlertDialog(
                    onDismissRequest = { showRoleInfoDialog = false },
                    containerColor = Color(0xFF1B2230),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ledColor))
                            Text("Información de Conexión", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        }
                    },
                    text = {
                        Column {
                            Text("Rol: $roleTitle", fontWeight = FontWeight.Black, color = ledColor, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(roleDesc, color = Color(0xFFB0BEC5), fontSize = 13.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRoleInfoDialog = false }) {
                            Text("Entendido")
                        }
                    }
                )
            }
        } // End Column
    } // End Surface
} // End ClubTopBar


@Composable
fun OfficialClubEmblemBadge(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    showSubtext: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF262D3D),
                            Color(0xFF141822),
                            Color(0xFF0A0D12)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        2.5.dp,
                        Brush.sweepGradient(
                            listOf(
                                TxGoldBrass,
                                TxChromeSilver,
                                TxFlameRed,
                                TxGoldBrass,
                                TxChromeSilver,
                                TxFlameRed,
                                TxGoldBrass
                            )
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Venezuelan Flag Halo & Official Club Logo
            Box(
                modifier = Modifier
                    .size(size * 0.88f)
                    .clip(CircleShape)
                    .border(1.5.dp, Brush.horizontalGradient(listOf(VzlaYellow, VzlaBlue, VzlaRed)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.aistudio.teamtxvzla.R.drawable.logoteam),
                    contentDescription = "Logo Oficial Team TX",
                    modifier = Modifier
                        .size(size * 0.78f)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }

        if (showSubtext) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "HONOR • LEALTAD • RESPETO",
                color = TxGoldBrass,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun ConnectionLedIndicator(
    isOnline: Boolean,
    isGuest: Boolean = false,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 8.dp,
    showLabel: Boolean = false
) {
    val ledColor = if (isGuest) StatusWarning else if (isOnline) StatusSuccess else StatusError
    val borderCol = if (isGuest) Color(0xFFFFD54F) else if (isOnline) Color(0xFF69F0AE) else Color(0xFFFF8A80)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(ledColor)
                .border(1.dp, borderCol, CircleShape)
        )
        if (showLabel) {
            Text(
                text = if (isGuest) "INVITADO TEMPORAL" else if (isOnline) "ACTIVO EN LÍNEA" else "DESCONECTADO",
                color = ledColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun PilotAvatar(
    member: MemberProfile,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 54.dp,
    showRankGlow: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (member.isDirectiva) Brush.radialGradient(listOf(Color(0xFF4A3800), Color(0xFF1B2230)))
                    else Brush.radialGradient(listOf(Color(0xFF2A3447), Color(0xFF131924)))
                )
                .border(
                    width = if (member.isDirectiva) 2.dp else 1.5.dp,
                    color = if (member.isSuspended) StatusError
                    else if (member.isDirectiva) TxGoldSecondary
                    else if (member.role == MemberRole.INVITADO) StatusWarning
                    else AsphaltDarkBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!member.profilePhotoUri.isNullOrBlank()) {
                val photoUrl = member.profilePhotoUri
                LaunchedEffect(photoUrl) {
                    android.util.Log.d("TEAM_TX_IMAGES", "👤 Cargando Avatar: ${member.nickname} | URL: $photoUrl")
                }
                AsyncImage(
                    model = photoUrl,
                    contentDescription = member.fullName,
                    onSuccess = { android.util.Log.i("TEAM_TX_IMAGES", "✅ Avatar cargado: ${member.nickname}") },
                    onError = { e -> android.util.Log.e("TEAM_TX_IMAGES", "❌ Error Avatar: ${member.nickname} | ${e.result.throwable.message}") },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = member.avatarInitials,
                    color = if (member.isSuspended) StatusError else if (member.isDirectiva) TxGoldSecondary else if (member.role == MemberRole.INVITADO) StatusWarning else TxChromeSilver,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.38f).sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Live Connection LED indicator (Green = Online, Yellow = Guest, Red = Offline)
        Box(
            modifier = Modifier
                .size((size.value * 0.28f).dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(
                    if (member.role == MemberRole.INVITADO) StatusWarning
                    else if (member.isOnline) StatusSuccess
                    else StatusError
                )
                .border(1.5.dp, Color(0xFF0F131C), CircleShape)
        )

        // Solvency / Status Dot indicator
        Box(
            modifier = Modifier
                .size((size.value * 0.26f).dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(
                    if (member.isSuspended) StatusError
                    else if (member.role == MemberRole.INVITADO) StatusWarning
                    else if (member.solvencyStatus) StatusSuccess
                    else StatusWarning
                )
                .border(1.5.dp, AsphaltDarkSurface, CircleShape)
        )
    }
}

@Composable
fun DigitalCredentialCard(
    member: MemberProfile,
    currentLoggedInMemberId: Long = 0L,
    onRateMember: (MemberProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isSuspended) Color(0xFF1A1012) else Color(0xFF121620)
        ),
        border = BorderStroke(
            1.8.dp,
            if (member.isSuspended)
                Brush.linearGradient(listOf(StatusError, TxGoldBrass, StatusError))
            else
                Brush.linearGradient(listOf(TxGoldBrass, TxFlameRed, TxChromeSilver, TxGoldBrass))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("digital_credential_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Club Brand, Tricolor Ribbon and Serial
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (member.isSuspended) listOf(StatusError, Color(0xFF7F1D1D)) else listOf(TxFlameRed, TxRedDark)
                                )
                            )
                            .border(1.dp, TxGoldBrass, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TX",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Column {
                        Text(
                            text = "TEAM NACIONAL TX VZLA",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (member.isSuspended) "ESTATUS: MIEMBRO SUSPENDIDO" else "CREDENCIAL OFICIAL DE PILOTO",
                            color = if (member.isSuspended) StatusError else TxGoldBrass,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (member.isSuspended) StatusError.copy(alpha = 0.25f) else TxFlameRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (member.isSuspended) StatusError else TxFlameRed)
                ) {
                    Text(
                        text = member.memberNumber,
                        color = if (member.isSuspended) StatusError else TxGoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (member.isSuspended) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusError.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                        Column {
                            Text("MOTIVO: ${member.suspensionReason}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Vence: ${member.suspensionEndDate}", color = TxGoldLight, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Member Photo, Names and Roles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar / Photo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2638))
                        .border(
                            2.dp,
                            if (member.isSuspended) StatusError else if (member.isDirectiva) TxGoldBrass else TxSteelSilver,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!member.profilePhotoUri.isNullOrBlank()) {
                        val photoUrl = member.profilePhotoUri
                        LaunchedEffect(photoUrl) {
                            android.util.Log.d("TEAM_TX_IMAGES", "📸 Cargando Foto Perfil: ${member.fullName} | URL: $photoUrl")
                        }
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto de perfil",
                            onSuccess = { android.util.Log.i("TEAM_TX_IMAGES", "✅ Foto Perfil cargada: ${member.fullName}") },
                            onError = { e -> android.util.Log.e("TEAM_TX_IMAGES", "❌ Error Foto Perfil: ${member.fullName} | ${e.result.throwable.message}") },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = member.avatarInitials,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (member.nickname.isNotBlank()) {
                        Text(
                            text = "\"${member.nickname}\"",
                            color = TxGoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (member.isDirectiva) TxGoldBrass.copy(alpha = 0.2f) else Color(0xFF2A3447),
                            border = BorderStroke(1.dp, if (member.isDirectiva) TxGoldBrass else Color(0xFF3E4C66))
                        ) {
                            Text(
                                text = member.role.displayName.uppercase(),
                                color = if (member.isDirectiva) TxGoldLight else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = member.chapterState,
                            color = Color(0xFFA0ADC0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ficha Técnica de la Moto
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF171D28),
                border = BorderStroke(1.dp, Color(0xFF283244)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!member.bikePhotoUri.isNullOrBlank()) {
                        val bikeUrl = member.bikePhotoUri
                        LaunchedEffect(bikeUrl) {
                            android.util.Log.d("TEAM_TX_IMAGES", "🏍️ Cargando Foto Moto: ${member.nickname} | URL: $bikeUrl")
                        }
                        AsyncImage(
                            model = bikeUrl,
                            contentDescription = "Foto de la moto",
                            onSuccess = { android.util.Log.i("TEAM_TX_IMAGES", "✅ Foto Moto cargada: ${member.nickname}") },
                            onError = { e -> android.util.Log.e("TEAM_TX_IMAGES", "❌ Error Foto Moto: ${member.nickname} | ${e.result.throwable.message}") },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(16.dp))
                        Text(
                            text = "FICHA TÉCNICA DE LA MOTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = TxGoldBrass,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("MOTO Y MODELO", color = Color(0xFF8C9BAE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${member.bikeBrand} ${member.bikeModel}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Color: ${member.bikeColor}", color = TxGoldLight, fontSize = 10.sp)
                        }
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("CILINDRADA", color = Color(0xFF8C9BAE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(member.bikeDisplacementCc, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Año: ${member.bikeYear}", color = Color(0xFFA0ADC0), fontSize = 10.sp)
                        }
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text("TANQUE / PLACA", color = Color(0xFF8C9BAE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("⛽ ${member.bikeTankCapacityLiters}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Placa: ${member.bikePlate}", color = TxSteelSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Récord de Velocidad Registrado por el Velocímetro
                    if (PreferenciasApp.topSpeedRecordKmh > 0f) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MotoOrangePrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("RÉCORD TOP SPEED:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                                }
                                val topKmh = PreferenciasApp.topSpeedRecordKmh.toInt()
                                val topMph = (PreferenciasApp.topSpeedRecordKmh * 0.621371f).toInt()
                                Text("$topKmh KM/H ($topMph MPH)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // QR de verificacion del carnet
            if (PreferenciasApp.carnetMostrarQr) {
                val qrPayload = buildString {
                    append("TEAMTX VZLA|")
                    append("N°:${member.memberNumber}|")
                    append("ID:${member.id}|")
                    append("NOMBRE:${member.fullName}|")
                    append("ALIAS:${member.nickname}|")
                    append("CÉDULA:${member.cedulaDni}|")
                    append("TEL:${member.phone}|")
                    append("ROL:${member.role.displayName}|")
                    append("ESTADO:${member.chapterState}|")
                    append("NAC:${member.birthDate}|")
                    append("MIEMBRO_DESDE:${member.joinYear}|")
                    append("MOTO:${member.bikeBrand} ${member.bikeModel} ${member.bikeYear}|")
                    append("COLOR:${member.bikeColor}|")
                    append("CC:${member.bikeDisplacementCc}|")
                    append("TANQUE:${member.bikeTankCapacityLiters}|")
                    append("PLACA:${member.bikePlate}|")
                    append("SANGRE:${member.bloodType}|")
                    append("ALERGIAS:${member.medicalNotes}|")
                    append("EMERGENCIA:${member.emergencyContactName} ${member.emergencyContactPhone} (${member.emergencyContactRelation})|")
                    append("DIRECTIVA:${if (member.isDirectiva) "SI" else "NO"}|")
                    append("SOLVENTE:${if (member.solvencyStatus) "SI" else "NO"}|")
                    if (!member.copilotName.isNullOrBlank()) {
                        append("COPILOTO:${member.copilotName} (${member.copilotRelation ?: ""})|")
                    }
                    append("TEAMTX_OK")
                }
                val qrImage = remember(member.memberNumber, member.fullName, member.id, member.phone, member.bikePlate, member.bloodType, member.copilotName, member.cedulaDni) {
                    try {
                        val hints = mapOf(EncodeHintType.MARGIN to 1)
                        val matrix = QRCodeWriter().encode(qrPayload, BarcodeFormat.QR_CODE, 320, 320, hints)
                        val bmp = Bitmap.createBitmap(320, 320, Bitmap.Config.RGB_565)
                        for (x in 0 until 320) for (y in 0 until 320)
                            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                        bmp
                    } catch (e: Exception) {
                        null
                    }
                }
                if (qrImage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF171D28),
                        border = BorderStroke(1.dp, Color(0xFF283244)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = qrImage.asImageBitmap(),
                                contentDescription = "QR de Verificacion",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("QR DE VERIFICACIÓN OFICIAL", color = TxGoldBrass, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text("Escaneable para verificar autenticidad de la credencial en rodadas y puntos de control.", color = Color(0xFF8C9BAE), fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("TEAMTX-OK-2026", color = Color(0xFF5A6E85), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Medical & Emergency Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sangre & Alergias
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusError.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = StatusError, modifier = Modifier.size(12.dp))
                            Text("GRUPO SANGUÍNEO", fontSize = 9.sp, color = StatusError, fontWeight = FontWeight.Black)
                        }
                        Text(
                            text = "🩸 ${member.bloodType}",
                            color = StatusError,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = member.medicalNotes,
                            color = Color(0xFFE2E8F0),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Contacto SOS
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1B2230),
                    border = BorderStroke(1.dp, Color(0xFF2E384D)),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(12.dp))
                            Text("CONTACTO SOS", fontSize = 9.sp, color = TxGoldLight, fontWeight = FontWeight.Black)
                        }
                        Text(
                            text = member.emergencyContactName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "📞 ${member.emergencyContactPhone} (${member.emergencyContactRelation})",
                            color = Color(0xFFA0ADC0),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Copiloto Oficial y Teléfono / DNI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!member.copilotName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A2234),
                        border = BorderStroke(1.dp, Color(0xFF2A3A55)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AirlineSeatReclineNormal, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(12.dp))
                                Text("COPILOTO OFICIAL", fontSize = 9.sp, color = TxGoldBrass, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = member.copilotName ?: "",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Parentesco: ${member.copilotRelation ?: "Familiar"}",
                                color = Color(0xFFA0ADC0),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1B2230),
                    border = BorderStroke(1.dp, Color(0xFF2E384D)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TxSteelSilver, modifier = Modifier.size(12.dp))
                            Text("TELÉFONO & DNI", fontSize = 9.sp, color = Color(0xFF8C9BAE), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = member.phone,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "C.I. ${member.cedulaDni} • ${member.chapterState}",
                            color = Color(0xFFA0ADC0),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── REPUTACIÓN, GAMIFICACIÓN Y CALIFICACIONES ─────────────
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF161F2C),
                border = BorderStroke(1.dp, TxGoldBrass.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(16.dp))
                            Text(
                                text = "REPUTACIÓN & GAMIFICACIÓN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TxGoldBrass,
                                letterSpacing = 0.5.sp
                            )
                        }

                        val rank = com.example.ui.screens.getMemberHonorRank(com.example.ui.screens.calculateMemberMeritPoints(member))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = rank.badgeColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, rank.badgeColor)
                        ) {
                            Text(
                                text = rank.title,
                                color = rank.badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF16A34A).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "${member.positiveRatingsCount} Likes",
                                        color = Color(0xFF22C55E),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TxFlameRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, TxFlameRed.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ThumbDown, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "${member.negativeRatingsCount} Dislikes",
                                        color = TxFlameRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${com.example.ui.screens.calculateMemberMeritPoints(member)} PTS",
                            color = TxGoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (currentLoggedInMemberId > 0L && member.id != currentLoggedInMemberId) {
                        Button(
                            onClick = { onRateMember(member) },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth().height(34.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Calificar a este Piloto (Likes / Dislikes)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Solvency status & DNI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B222E))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (member.isSuspended) Icons.Default.Block else if (member.solvencyStatus) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (member.isSuspended) StatusError else if (member.solvencyStatus) StatusSuccess else StatusWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (member.isSuspended) "INHABILITADO PARA RODADAS" else if (member.solvencyStatus) "SOLVENTE 2026" else "CUOTA PENDIENTE",
                        color = if (member.isSuspended) StatusError else if (member.solvencyStatus) StatusSuccess else StatusWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "DESDE ${member.joinYear}",
                    color = Color(0xFFA0ADC0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Enlace: $url", Toast.LENGTH_SHORT).show()
    }
}

fun sendEmergencyWhatsApp(context: Context, alert: EmergencyAlert) {
    val levelHeader = when (alert.emergencyType.levelNumber) {
        4 -> "🚨🚨 *ALERTA SOS CRÍTICA - NIVEL 4: CHOQUE* 🚨🚨"
        3 -> "⚠️⚠️ *ALERTA SOS URGENTE - NIVEL 3: CAÍDA* ⚠️⚠️"
        2 -> "🔧 *ALERTA SOS MODERADA - NIVEL 2: MECÁNICA* 🔧"
        else -> "⛽ *ALERTA SOS LEVE - NIVEL 1: GASOLINA* ⛽"
    }

    val message = """
$levelHeader
*TEAM NACIONAL TX VENEZUELA*
━━━━━━━━━━━━━━━━━━━━
📌 *Clasificación:* ${alert.emergencyType.levelTag} - ${alert.emergencyType.label}
🏍️ *Piloto:* ${alert.reporterName} (${alert.memberNumber})
📞 *Teléfono:* ${alert.reporterPhone}
📍 *Ubicación:* ${alert.locationDescription}
🗺️ *GPS:* https://maps.google.com/?q=${alert.coordinateLat},${alert.coordinateLng}
🏍️ *Vehículo:* ${alert.bikeDetails}
🩸 *Tipo de Sangre:* ${alert.bloodTypeNeeded ?: "Registrado en Ficha"}
🛠️ *Especialista Asignado:* ${alert.emergencyType.recommendedSpecialist}
📋 *Protocolo de Acción:*
${alert.emergencyType.actionProtocol}
📝 *Detalles:* ${alert.details}
━━━━━━━━━━━━━━━━━━━━
_Emitido automáticamente desde la App Oficial Team TX Venezuela_
    """.trimIndent()

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp no disponible. Copiando mensaje SOS.", Toast.LENGTH_LONG).show()
    }
}

fun dialPhoneNumber(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.replace(" ", "")}"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Llamar a $phoneNumber", Toast.LENGTH_SHORT).show()
    }
}
