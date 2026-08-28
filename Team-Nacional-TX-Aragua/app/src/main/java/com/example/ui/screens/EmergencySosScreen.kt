package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.screens.dialogs.EmitSosDialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.openUrl
import com.example.ui.components.sendEmergencyWhatsApp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class LevelFilterOption(val label: String, val levelNumber: Int?, val color: Color) {
    TODOS("Todos", null, Color(0xFF90CAF9)),
    NIVEL_4("Nivel 4: Choque", 4, Color(0xFFFF1744)),
    NIVEL_3("Nivel 3: Caída", 3, Color(0xFFFF5722)),
    NIVEL_2("Nivel 2: Mecánico", 2, Color(0xFFFF9100)),
    NIVEL_1("Nivel 1: Gasolina", 1, Color(0xFFFFD600))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySosScreen(
    alerts: List<EmergencyAlert>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    onBroadcastSos: (
        type: EmergencyType,
        location: String,
        details: String,
        bloodType: String?,
        lat: Double,
        lng: Double
    ) -> Unit,
    onUpdateAlertStatus: (EmergencyAlert, EmergencyStatus, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEmitSosDialog by remember { mutableStateOf(false) }
    var selectedAlertForManage by remember { mutableStateOf<EmergencyAlert?>(null) }
    var selectedLevelFilter by remember { mutableStateOf(LevelFilterOption.TODOS) }
    var showProtocolGuide by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredAlerts = remember(alerts, selectedLevelFilter) {
        if (selectedLevelFilter.levelNumber == null) {
            alerts
        } else {
            alerts.filter { it.emergencyType.levelNumber == selectedLevelFilter.levelNumber }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // SOS Hero Action Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1012)),
                    border = BorderStroke(2.dp, StatusError),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_sos_hero")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(StatusError)
                            )
                            Text(
                                text = "CENTRAL SOS Y TRIAGE POR NIVELES",
                                color = StatusError,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Sistema de Emergencias Team TX",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Respuesta escalonada para incidentes en carretera: Falta de combustible, fallas mecánicas, caídas y colisiones viales con despacho de especialistas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2B7B7),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showEmitSosDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusError,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_trigger_sos")
                        ) {
                            Icon(
                                Icons.Default.Emergency,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EMITIR ALERTA SOS POR NIVEL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Quick Dial Specialist Hotlines (Médico, Mecánico, Seguridad, VEN 911)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Especialistas y Líneas de Auxilio",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1B222E),
                                border = BorderStroke(1.dp, TxGoldBrass.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "24/7 EN RUTA",
                                    color = TxGoldSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 1: Mecánico Oficial + Médico del Club
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mecánico Oficial
                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "+584129904433") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFFF9100).copy(alpha = 0.1f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFF9100)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("Mecánico TX", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("P. Torrealba", fontSize = 9.sp, color = Color(0xFFFF9100))
                                    }
                                }
                            }

                            // Médico del Club
                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "+584245551290") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF00E676).copy(alpha = 0.1f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00E676)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("Médico Club", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Dra. Gómez", fontSize = 9.sp, color = Color(0xFF00E676))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: VEN 911 + Seguridad Vial + Contacto SOS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // VEN 911
                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "911") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, StatusError),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VEN 911", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusError)
                            }

                            // Seguridad Vial
                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "+584128887766") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF00B0FF)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF00B0FF), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Seguridad", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                            }

                            // Contacto SOS Personal
                            OutlinedButton(
                                onClick = {
                                    val phone = currentMember?.emergencyContactPhone ?: "+584141234567"
                                    dialPhoneNumber(context, phone)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, StatusInfo),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Familiar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusInfo)
                            }
                        }
                    }
                }
            }

            // Interactive Protocols by Level Guide (Expandable accordion)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                    border = BorderStroke(1.dp, Color(0xFF283244)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProtocolGuide = !showProtocolGuide },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = TxGoldBrass)
                                Column {
                                    Text(
                                        text = "Protocolos de Emergencia por Niveles",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Guía operativa rápida para moteros TX",
                                        fontSize = 10.sp,
                                        color = Color(0xFF8C9BAE)
                                    )
                                }
                            }
                            IconButton(onClick = { showProtocolGuide = !showProtocolGuide }) {
                                Icon(
                                    imageVector = if (showProtocolGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TxGoldBrass
                                )
                            }
                        }

                        AnimatedVisibility(visible = showProtocolGuide) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProtocolLevelItem(
                                    levelTag = "NIVEL 1: FALTA DE GASOLINA",
                                    levelColor = Color(0xFFFFD600),
                                    icon = Icons.Default.LocalGasStation,
                                    assignedSpecialist = "Piloto Enlace / Apoyo de Ruta",
                                    protocolText = "Ubicarse en hombrillo seguro con luces intermitentes. Compartir GPS y octanaje requerido (91/95). El piloto más cercano abastece con manguera de trasvasado o bidón de reserva."
                                )

                                ProtocolLevelItem(
                                    levelTag = "NIVEL 2: FALLA MECÁNICA",
                                    levelColor = Color(0xFFFF9100),
                                    icon = Icons.Default.Build,
                                    assignedSpecialist = "Mecánico Oficial TX (Pedro Torrealba)",
                                    protocolText = "No obstaculizar la vía. Notificar rotura de guaya, cadena, pinchazo o falla de encendido. El Mecánico Oficial despacha kit de repuestos de inventario comunitario y herramientas."
                                )

                                ProtocolLevelItem(
                                    levelTag = "NIVEL 3: CAÍDA EN RUTA",
                                    levelColor = Color(0xFFFF5722),
                                    icon = Icons.Default.PersonalInjury,
                                    assignedSpecialist = "Médico / Paramédico + Seguridad Vial",
                                    protocolText = "¡NO RETIRAR EL CASCO BRUSCAMENTE! Bloqueadores aseguran el área a 50m con chalecos y luces. Médico aplica triage, limpieza de laceraciones e inmovilización. Mecánico evalúa si la moto rueda."
                                )

                                ProtocolLevelItem(
                                    levelTag = "NIVEL 4: CHOQUE / COLISIÓN GRAVE",
                                    levelColor = Color(0xFFFF1744),
                                    icon = Icons.Default.CarCrash,
                                    assignedSpecialist = "VEN 911 + Médicos del Club + Directiva",
                                    protocolText = "LLAMADO INMEDIATO AL VEN 911 Y TRÁNSITO. Cierre total de seguridad vial a 100m. Inmovilización cervical absoluta, control de signos vitales, notificación a familiares y donantes de sangre."
                                )
                            }
                        }
                    }
                }
            }

            // Level Filter Bar
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Incidentes Reportados (${filteredAlerts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Transmitir WhatsApp",
                            fontSize = 11.sp,
                            color = WhatsAppGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(LevelFilterOption.values()) { option ->
                            FilterChip(
                                selected = selectedLevelFilter == option,
                                onClick = { selectedLevelFilter = option },
                                label = {
                                    Text(
                                        text = option.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedLevelFilter == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(option.color)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = option.color.copy(alpha = 0.2f),
                                    selectedLabelColor = option.color
                                ),
                                border = if (selectedLevelFilter == option) BorderStroke(1.dp, option.color) else null
                            )
                        }
                    }
                }
            }

            if (filteredAlerts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay alertas activas en el nivel seleccionado. ¡Ruta despejada!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredAlerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        isDirectivaMode = isDirectivaMode,
                        onWhatsAppShare = { sendEmergencyWhatsApp(context, alert) },
                        onCallReporter = { dialPhoneNumber(context, alert.reporterPhone) },
                        onManage = { selectedAlertForManage = alert }
                    )
                }
            }
        }
    }

    if (showEmitSosDialog) {
        EmitSosDialog(
            currentMember = currentMember,
            onDismiss = { showEmitSosDialog = false },
            onBroadcast = { type, loc, details, blood, lat, lng ->
                onBroadcastSos(type, loc, details, blood, lat, lng)
                showEmitSosDialog = false
            }
        )
    }

    if (selectedAlertForManage != null) {
        ManageAlertDialog(
            alert = selectedAlertForManage!!,
            onDismiss = { selectedAlertForManage = null },
            onUpdate = { newStatus, notes ->
                onUpdateAlertStatus(selectedAlertForManage!!, newStatus, notes)
                selectedAlertForManage = null
            }
        )
    }
}

@Composable
fun ProtocolLevelItem(
    levelTag: String,
    levelColor: Color,
    icon: ImageVector,
    assignedSpecialist: String,
    protocolText: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1B2230),
        border = BorderStroke(1.dp, levelColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = levelColor, modifier = Modifier.size(16.dp))
                Text(
                    text = levelTag,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = levelColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Especialista: $assignedSpecialist",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TxGoldBrass
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = protocolText,
                fontSize = 11.sp,
                color = Color(0xFFD0D8E4),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun AlertItemCard(
    alert: EmergencyAlert,
    isDirectivaMode: Boolean,
    onWhatsAppShare: () -> Unit,
    onCallReporter: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(alert.timestamp) {
        SimpleDateFormat("hh:mm a • dd/MM", Locale.getDefault()).format(Date(alert.timestamp))
    }

    val levelColor = Color(alert.emergencyType.severityColorHex)
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            when (alert.status) {
                EmergencyStatus.ACTIVA -> levelColor
                EmergencyStatus.EN_CAMINO -> StatusWarning
                EmergencyStatus.ATENDIDA -> StatusInfo
                EmergencyStatus.RESUELTA -> StatusSuccess
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("sos_alert_card_${alert.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Emergency Level Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = levelColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, levelColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        val icon = when (alert.emergencyType.iconType) {
                            "gas" -> Icons.Default.LocalGasStation
                            "mechanic" -> Icons.Default.Build
                            "fall" -> Icons.Default.PersonalInjury
                            "crash" -> Icons.Default.CarCrash
                            "medical" -> Icons.Default.MedicalServices
                            "security" -> Icons.Default.Shield
                            else -> Icons.Default.Warning
                        }
                        Icon(icon, contentDescription = null, tint = levelColor, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${alert.emergencyType.levelTag}: ${alert.emergencyType.label.uppercase()}",
                            color = levelColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (alert.status) {
                        EmergencyStatus.ACTIVA -> StatusError.copy(alpha = 0.15f)
                        EmergencyStatus.EN_CAMINO -> StatusWarning.copy(alpha = 0.15f)
                        EmergencyStatus.ATENDIDA -> StatusInfo.copy(alpha = 0.15f)
                        EmergencyStatus.RESUELTA -> StatusSuccess.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = alert.status.label,
                        color = when (alert.status) {
                            EmergencyStatus.ACTIVA -> StatusError
                            EmergencyStatus.EN_CAMINO -> StatusWarning
                            EmergencyStatus.ATENDIDA -> StatusInfo
                            EmergencyStatus.RESUELTA -> StatusSuccess
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pilot & Vehicle Details
            Text(
                text = "${alert.reporterName} (${alert.memberNumber})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = alert.bikeDetails,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MotoGoldSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location with Map intent click
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B222E))
                    .padding(8.dp)
                    .clickable {
                        openUrl(context, "https://maps.google.com/?q=${alert.coordinateLat},${alert.coordinateLng}")
                    }
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.locationDescription,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "GPS: ${alert.coordinateLat}, ${alert.coordinateLng} • $dateStr (Toca para mapa)",
                        fontSize = 10.sp,
                        color = TxGoldBrass,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specialist recommendation & protocol badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = levelColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, levelColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = levelColor, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Especialista asignado: ${alert.emergencyType.recommendedSpecialist}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = alert.emergencyType.actionProtocol,
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details
            Text(
                text = alert.details,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (alert.bloodTypeNeeded!! != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🩸 Grupo Sanguíneo Requerido:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                    Text(
                        text = alert.bloodTypeNeeded!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusError
                    )
                }
            }

            if (alert.respondersNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reporte de Apoyo: ${alert.respondersNotes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Actions: WhatsApp broadcast, Call, Manage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onWhatsAppShare,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_share_sos_whatsapp_${alert.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCallReporter,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Llamar", fontSize = 11.sp)
                    }
                }

                if (isDirectivaMode) {
                    TextButton(
                        onClick = onManage,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Gestionar", fontSize = 11.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManageAlertDialog(
    alert: EmergencyAlert,
    onDismiss: () -> Unit,
    onUpdate: (EmergencyStatus, notes: String) -> Unit
) {
    var status by remember { mutableStateOf(alert.status) }
    var notes by remember { mutableStateOf(alert.respondersNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestionar Incidente SOS (Directiva)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reportado por: ${alert.reporterName} (${alert.memberNumber})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Nivel: ${alert.emergencyType.levelTag} - ${alert.emergencyType.label}", color = Color(alert.emergencyType.severityColorHex), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Text("Estado del Incidente:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                EmergencyStatus.values().forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.label, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Bitácora de Atención / Notas de Despacho") },
                    placeholder = { Text("ej. El Mecánico Oficial Pedro Torrealba llegó al sitio") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(status, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Actualizar Registro")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
