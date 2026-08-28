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
            .height(3.dp)
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
    onToggleDirectiva: () -> Unit,
    onOpenSosModal: () -> Unit,
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
                    // Telegram link shortcut
                    IconButton(
                        onClick = {
                            openUrl(context, "https://t.me/+R_oloXwkGqhkNzJh")
                        },
                        modifier = Modifier.testTag("btn_top_telegram")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram Team TX",
                            tint = TelegramBlue
                        )
                    }


                    // TikTok link shortcut
                    IconButton(
                        onClick = {
                            openUrl(context, "https://www.tiktok.com/@teamnacionaltx.aragua")
                        },
                        modifier = Modifier.testTag("btn_top_tiktok")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "TikTok Team TX",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Directiva Mode Toggle Pill
                    val canToggle = isDeveloperMode || (currentMember?.isDirectiva == true) || (currentMember?.role?.canManageApp == true)
                    
                    if (canToggle) {
                        FilterChip(
                            selected = isDirectivaMode,
                            onClick = onToggleDirectiva,
                            label = {
                                Text(
                                    text = if (isDirectivaMode) "DIRECTIVA" else "MIEMBRO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDirectivaMode) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = "Modo",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MotoOrangePrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("btn_toggle_directiva_mode")
                        )
                    } else {
                        FilterChip(
                            selected = false,
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    text = "MIEMBRO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Modo",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
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
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "SANCIÓN DIRECTIVA ACTIVA",
                                color = StatusError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Motivo: ${member.suspensionReason.ifBlank { "Incumplimiento de normas" }}",
                                color = Color(0xFFFFCDD2),
                                fontSize = 10.sp
                            )
                            if (member.suspensionEndDate.isNotBlank()) {
                                Text(
                                    text = "Vigencia: ${member.suspensionEndDate} • Por: ${member.suspendedBy.ifBlank { "Directiva" }}",
                                    color = TxGoldSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            VenezuelanFlagRibbon(modifier = Modifier.clip(RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.height(14.dp))

            // Body: Pilot Avatar + Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pilot Avatar
                PilotAvatar(
                    member = member,
                    size = 72.dp,
                    showRankGlow = true
                )

                // Member Name, Alias & Role Badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Alias: \"${member.nickname}\"",
                        color = TxGoldBrass,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (member.isSuspended) StatusError.copy(alpha = 0.2f) else Color(member.role.badgeColorHex).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (member.isSuspended) StatusError else Color(member.role.badgeColorHex))
                    ) {
                        Text(
                            text = if (member.isSuspended) "SUSPENDIDO (${member.role.displayName.uppercase()})" else member.role.displayName.uppercase(),
                            color = if (member.isSuspended) StatusError else Color(member.role.badgeColorHex),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF28303E))
            Spacer(modifier = Modifier.height(10.dp))

            // FICHA TÉCNICA DE LA MOTO
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF161C26),
                border = BorderStroke(1.dp, Color(0xFF263346)),
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

            // QR de verificacion del carnet (persistente: Configuraciones > Carnet TX)
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
                        bmp.asImageBitmap()
                    } catch (_: Exception) { null }
                }
                if (qrImage != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(2.dp, TxGoldBrass)
                            ) {
                                Image(
                                    bitmap = qrImage,
                                    contentDescription = "QR del carnet",
                                    modifier = Modifier.padding(6.dp).size(110.dp)
                                )
                            }
                            Text(
                                text = "CREDENCIAL VERIFICABLE TEAM TX",
                                fontSize = 8.sp,
                                color = TxGoldBrass,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // FICHA MÉDICA Y PERSONAL
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
                            color = Color(0xFFFFCDD2),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Contacto Personal & Teléfono
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161C26),
                    border = BorderStroke(1.dp, Color(0xFF263346)),
                    modifier = Modifier.weight(1.3f)
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
