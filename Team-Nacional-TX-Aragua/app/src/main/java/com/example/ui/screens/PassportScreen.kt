package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MemberProfile
import com.example.data.model.PassportDestination
import com.example.data.model.PassportStamp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "PASAPORTE MOTERO TX",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            "Libreta Oficial de Rutas y Sellos",
                            fontSize = 11.sp,
                            color = MotoOrangePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onBack() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Inicio", tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inicio", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Passport Booklet Cover Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, Color(0xFFD97706)),
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
                            Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TEAM NACIONAL TX VENEZUELA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFD97706),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "PASAPORTE OFICIAL DE RUTAS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEDD5),
                            border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.5f))
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
                            Text("PILOTO TITULAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text(currentMember?.fullName ?: "Piloto TX", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text(currentMember?.nickname?.let { "\"$it\"" } ?: "", fontSize = 12.sp, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("PROGRESO DE SELLOS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("$stampedCount / $totalDestinations Destinos", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Text("$totalKmEarned KM Sellados", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
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
                        color = Color(0xFFD97706),
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }

            Text(
                text = "DESTINOS EMBLEMÁTICOS DE VENEZUELA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
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
    onStampClick: () -> Unit,
    onDeleteStamp: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            if (isStamped) Color(0xFFD97706) else Color(0xFFE2E8F0)
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
                color = if (isStamped) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isStamped) Color(0xFFD97706) else Color(0xFFCBD5E1)),
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
                        color = Color(0xFF0F172A)
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
                    color = Color(0xFF475569),
                    lineHeight = 15.sp
                )

                if (isStamped && stamp != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFD97706))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SELLADO OFICIAL: ${stamp.stampedDate}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isStamped) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar sello", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                }
            } else {
                Button(
                    onClick = onStampClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sellar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar sello?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Se eliminará la estampa de visita a '${destination.title}'. Podrás sellarla nuevamente cuando quieras.", color = Color(0xFF334155)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteStamp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampDestinationDialog(
    destination: PassportDestination,
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

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedBorderColor = MotoOrangePrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = MotoOrangePrimary,
        unfocusedLabelColor = Color(0xFF64748B)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(destination.badgeIcon, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sellar: ${destination.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
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
                    color = Color(0xFF64748B)
                )

                // Photo proof picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
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
                            Text("Foto de la rodada / comprobante (Opcional)", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                OutlinedTextField(
                    value = customDate,
                    onValueChange = { customDate = it },
                    label = { Text("Fecha de la visita (DD/MM/AAAA)") },
                    colors = tfColors,
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
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("Aplicar Sello 🎖️", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF64748B))
            }
        },
        containerColor = Color.White
    )
}
