package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MemberProfile
import com.example.data.model.PassportDestination
import com.example.data.model.PassportStamp
import com.example.dashboard.DashboardFondoConfig
import com.example.dashboard.TipoFondoDashboard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportScreen(
    destinations: List<PassportDestination>,
    myStamps: List<PassportStamp>,
    currentMember: MemberProfile?,
    onStampDestination: (destination: PassportDestination, proofUri: Uri?, customDate: String, onComplete: (Boolean) -> Unit) -> Unit,
    onDeleteStamp: (id: Long) -> Unit,
    onBack: () -> Unit = {}
) {
    var selectedDestinationForStamp by remember { mutableStateOf<PassportDestination?>(null) }
    var showStampDialog by remember { mutableStateOf(false) }

    val stampedDestinationIds = remember(myStamps) { myStamps.map { it.destinationId }.toSet() }
    val totalDestinations = destinations.size
    val stampedCount = stampedDestinationIds.size
    val progressPercent = if (totalDestinations > 0) (stampedCount.toFloat() / totalDestinations) * 100 else 0f
    val totalKmEarned = remember(myStamps, destinations) {
        destinations.filter { it.id in stampedDestinationIds }.sumOf { it.requiredKm }
    }

    val isLight = DashboardFondoConfig.tipoFondo == TipoFondoDashboard.TEMA_CLARO_ESTANDAR
    val bgColor = if (isLight) DashboardFondoConfig.ColorFondoClaro else Color.Black
    val cardBgColor = if (isLight) DashboardFondoConfig.ColorTarjetaClara else TxCarbonDark
    val textColorPrimary = if (isLight) DashboardFondoConfig.ColorTextoPrimario else Color.White
    val textColorSecondary = if (isLight) DashboardFondoConfig.ColorTextoSecundario else TxSteelSilver
    val borderColor = if (isLight) DashboardFondoConfig.ColorBordeClaro else TxSteelSilver.copy(alpha = 0.2f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = TxGoldBrass)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PASAPORTE MOTERO TX", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isLight) Color.White else Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TxCarbonDark)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Passport Booklet Cover Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBgColor,
                border = androidx.compose.foundation.BorderStroke(2.dp, TxGoldBrass),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TEAM NACIONAL TX VENEZUELA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TxGoldBrass,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "PASAPORTE OFICIAL DE RUTAS",
                                    fontSize = 10.sp,
                                    color = textColorSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MotoOrangePrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = currentMember?.memberNumber ?: "TX-PILOT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MotoOrangePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PILOTO TITULAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColorSecondary)
                            Text(currentMember?.fullName ?: "Piloto TX", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
                            Text(currentMember?.nickname?.let { "\"$it\"" } ?: "", fontSize = 12.sp, color = TxGoldBrass)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("PROGRESO DE SELLOS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColorSecondary)
                            Text("$stampedCount / $totalDestinations Destinos", fontSize = 14.sp, fontWeight = FontWeight.Black, color = textColorPrimary)
                            Text("$totalKmEarned KM Sellados", fontSize = 12.sp, color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = TxGoldBrass,
                        trackColor = if (isLight) Color(0xFFE2E8F0) else TxCharcoalSurface
                    )
                }
            }

            Text(
                text = "DESTINOS EMBLEMÁTICOS DE VENEZUELA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColorSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Destinations List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(destinations, key = { it.id }) { destination ->
                    val stamp = myStamps.find { it.destinationId == destination.id }
                    val isStamped = stamp != null

                    PassportDestinationCard(
                        destination = destination,
                        stamp = stamp,
                        isStamped = isStamped,
                        cardBgColor = cardBgColor,
                        textColorPrimary = textColorPrimary,
                        textColorSecondary = textColorSecondary,
                        onStampClick = {
                            selectedDestinationForStamp = destination
                            showStampDialog = true
                        },
                        onDeleteStamp = {
                            stamp?.let { onDeleteStamp(it.id) }
                        }
                    )
                }
            }
        }
    }

    if (showStampDialog && selectedDestinationForStamp != null) {
        StampDestinationDialog(
            destination = selectedDestinationForStamp!!,
            isLight = isLight,
            onDismiss = { showStampDialog = false },
            onConfirm = { proofUri, customDate ->
                onStampDestination(selectedDestinationForStamp!!, proofUri, customDate) { success ->
                    if (success) showStampDialog = false
                }
            }
        )
    }
}

@Composable
fun PassportDestinationCard(
    destination: PassportDestination,
    stamp: PassportStamp?,
    isStamped: Boolean,
    cardBgColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    onStampClick: () -> Unit,
    onDeleteStamp: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBgColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isStamped) TxGoldBrass else textColorSecondary.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Surface(
                shape = CircleShape,
                color = if (isStamped) TxGoldBrass.copy(alpha = 0.2f) else TxCharcoalSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isStamped) TxGoldBrass else TxSteelSilver.copy(alpha = 0.4f)),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = destination.badgeIcon,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = destination.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textColorPrimary
                    )
                }

                Text(
                    text = "${destination.state} • ${destination.requiredKm} KM de ruta",
                    fontSize = 11.sp,
                    color = MotoOrangePrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = destination.description,
                    fontSize = 11.sp,
                    color = textColorSecondary,
                    lineHeight = 15.sp
                )

                if (isStamped && stamp != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TxGoldBrass.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TxGoldBrass)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SELLADO OFICIAL: ${stamp.stampedDate}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TxGoldBrass
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isStamped) {
                IconButton(onClick = onDeleteStamp) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar sello", tint = TxFlameRed, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = onStampClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sellar", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampDestinationDialog(
    destination: PassportDestination,
    isLight: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (proofUri: Uri?, customDate: String) -> Unit
) {
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var customDate by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var isUploading by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(destination.badgeIcon, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sellar: ${destination.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Registra tu visita a este destino emblemático para añadir el sello dorado a tu pasaporte.",
                    fontSize = 12.sp,
                    color = TxSteelSilver
                )

                // Photo proof picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TxCharcoalSurface)
                        .border(1.dp, TxSteelSilver.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { photoLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedPhotoUri != null) {
                        AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Foto de la rodada / comprobante (Opcional)", fontSize = 11.sp, color = TxSteelSilver)
                        }
                    }
                }

                OutlinedTextField(
                    value = customDate,
                    onValueChange = { customDate = it },
                    label = { Text("Fecha de la visita (DD/MM/AAAA)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isUploading = true
                    onConfirm(selectedPhotoUri, customDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TxGoldBrass)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Aplicar Sello 🎖️", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = TxCarbonDark
    )
}
