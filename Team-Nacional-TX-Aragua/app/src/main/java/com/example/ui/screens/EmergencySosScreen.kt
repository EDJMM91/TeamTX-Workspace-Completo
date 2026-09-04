package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.dashboard.DashboardFondoConfig
import com.example.data.model.*
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.openUrl
import com.example.ui.components.sendEmergencyWhatsApp
import com.example.ui.preferences.PreferenciasApp
import com.example.ui.screens.dialogs.EmitSosDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class LevelFilterOption(val label: String, val levelNumber: Int?, val color: Color) {
    TODOS("Todos", null, Color(0xFF0284C7)),
    NIVEL_4("Nivel 4: Choque", 4, Color(0xFFDC2626)),
    NIVEL_3("Nivel 3: Caída", 3, Color(0xFFE11D48)),
    NIVEL_2("Nivel 2: Mecánico", 2, Color(0xFFEA580C)),
    NIVEL_1("Nivel 1: Gasolina", 1, Color(0xFFD97706))
}

data class QuickAlertData(
    val type: EmergencyType,
    val nivelTag: String,
    val titulo: String,
    val subtitulo: String,
    val color: Color,
    val bgTint: Color,
    val icono: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySosScreen(
    alerts: List<EmergencyAlert>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    onBack: () -> Unit = {},
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
    val context = LocalContext.current
    PreferenciasApp.init(context)

    var showConsentDialog by remember { mutableStateOf(!PreferenciasApp.sosConsentimientoUbicacionAceptado) }
    var showEmitSosDialog by remember { mutableStateOf(false) }
    var initialSosType by remember { mutableStateOf(EmergencyType.CAIDA) }
    var selectedAlertForManage by remember { mutableStateOf<EmergencyAlert?>(null) }
    var selectedLevelFilter by remember { mutableStateOf(LevelFilterOption.TODOS) }
    var showProtocolGuide by remember { mutableStateOf(false) }

    val filteredAlerts = remember(alerts, selectedLevelFilter) {
        if (selectedLevelFilter.levelNumber == null) {
            alerts
        } else {
            alerts.filter { it.emergencyType.levelNumber == selectedLevelFilter.levelNumber }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC)
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
            // ─── Header con Botón de Volver y Estado ──────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DashboardFondoConfig.ColorTarjetaClara)
                                .border(1.dp, DashboardFondoConfig.ColorBordeClaro, CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = DashboardFondoConfig.ColorTextoPrimario,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SOS Vial y Emergencias",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = DashboardFondoConfig.ColorTextoPrimario
                            )
                            Text(
                                text = "Central de Asistencia y Despacho en Ruta",
                                fontSize = 12.sp,
                                color = DashboardFondoConfig.ColorTextoSecundario
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                            Text(
                                text = "ACTIVO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // ─── SOS Hero Action Card (Tema Claro) ───────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_sos_hero")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                            Text(
                                text = "CENTRAL SOS Y TRIAGE POR NIVELES",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sistema de Emergencias Team TX",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Respuesta escalonada para incidentes en carretera: combustible, fallas mecánicas, caídas y choques. Con despacho automático a Chat, WhatsApp, Avisos y Mapa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ─── Botonera Simétrica de Alertas Rápidas por Niveles ─────────────
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Alertas Rápidas por Nivel (Toca para reportar)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val quickAlertTypes = listOf(
                        QuickAlertData(
                            type = EmergencyType.ACCIDENTADO_GASOLINA,
                            nivelTag = "NIVEL 1",
                            titulo = "Sin Gasolina",
                            subtitulo = "Apoyo en ruta / Bidón",
                            color = Color(0xFFD97706),
                            bgTint = Color(0xFFFEF3C7),
                            icono = Icons.Default.LocalGasStation
                        ),
                        QuickAlertData(
                            type = EmergencyType.ACCIDENTADO_MECANICO,
                            nivelTag = "NIVEL 2",
                            titulo = "Falla Mecánica",
                            subtitulo = "Mecánico Oficial TX",
                            color = Color(0xFFEA580C),
                            bgTint = Color(0xFFFFEDD5),
                            icono = Icons.Default.Build
                        ),
                        QuickAlertData(
                            type = EmergencyType.CAIDA,
                            nivelTag = "NIVEL 3",
                            titulo = "Caída en Ruta",
                            subtitulo = "Triage y Primeros Auxilios",
                            color = Color(0xFFE11D48),
                            bgTint = Color(0xFFFFE4E6),
                            icono = Icons.Default.PersonalInjury
                        ),
                        QuickAlertData(
                            type = EmergencyType.CHOQUE,
                            nivelTag = "NIVEL 4",
                            titulo = "Choque Grave",
                            subtitulo = "VEN 911 / Ambulancia",
                            color = Color(0xFFDC2626),
                            bgTint = Color(0xFFFEE2E2),
                            icono = Icons.Default.CarCrash
                        ),
                        QuickAlertData(
                            type = EmergencyType.EMERGENCIA_MEDICA,
                            nivelTag = "MÉDICO",
                            titulo = "Auxilio Médico",
                            subtitulo = "Médico del Club",
                            color = Color(0xFF059669),
                            bgTint = Color(0xFFD1FAE5),
                            icono = Icons.Default.MedicalServices
                        ),
                        QuickAlertData(
                            type = EmergencyType.APOYO_SEGURIDAD,
                            nivelTag = "SEGURIDAD",
                            titulo = "Peligro en Vía",
                            subtitulo = "Seguridad / Barredor",
                            color = Color(0xFF0284C7),
                            bgTint = Color(0xFFE0F2FE),
                            icono = Icons.Default.Shield
                        )
                    )

                    for (i in quickAlertTypes.indices step 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickAlertButton(
                                data = quickAlertTypes[i],
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    initialSosType = quickAlertTypes[i].type
                                    showEmitSosDialog = true
                                }
                            )
                            if (i + 1 < quickAlertTypes.size) {
                                QuickAlertButton(
                                    data = quickAlertTypes[i + 1],
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        initialSosType = quickAlertTypes[i + 1].type
                                        showEmitSosDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ─── Especialistas y Líneas de Auxilio (Tema Claro) ───────────────
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
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
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "24/7 EN RUTA",
                                    color = Color(0xFFB45309),
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
                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "+584129904433") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFFFF7ED)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFB923C)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("Mecánico TX", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Text("P. Torrealba", fontSize = 9.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "+584245551290") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFECFDF5)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF34D399)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("Médico Club", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Text("Dra. Gómez", fontSize = 9.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
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
                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "911") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFF87171)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VEN 911", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }

                            OutlinedButton(
                                onClick = { dialPhoneNumber(context, "+584128887766") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0F9FF)),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Seguridad", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }

                            OutlinedButton(
                                onClick = {
                                    val phone = currentMember?.emergencyContactPhone ?: "+584141234567"
                                    dialPhoneNumber(context, phone)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Familiar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            }
                        }
                    }
                }
            }

            // ─── Protocolos de Emergencia por Niveles (Acordeón Claro) ────────
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
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
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFD97706))
                                Column {
                                    Text(
                                        text = "Protocolos de Emergencia por Niveles",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Guía operativa rápida para moteros TX",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                            IconButton(onClick = { showProtocolGuide = !showProtocolGuide }) {
                                Icon(
                                    imageVector = if (showProtocolGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A)
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
                                    levelColor = Color(0xFFD97706),
                                    icon = Icons.Default.LocalGasStation,
                                    assignedSpecialist = "Piloto Enlace / Apoyo de Ruta",
                                    protocolText = "Ubicarse en hombrillo seguro con luces intermitentes. Compartir GPS y octanaje requerido (91/95). El piloto más cercano abastece con manguera de trasvasado o bidón de reserva."
                                )

                                ProtocolLevelItem(
                                    levelTag = "NIVEL 2: FALLA MECÁNICA",
                                    levelColor = Color(0xFFEA580C),
                                    icon = Icons.Default.Build,
                                    assignedSpecialist = "Mecánico Oficial TX (Pedro Torrealba)",
                                    protocolText = "No obstaculizar la vía. Notificar rotura de guaya, cadena, pinchazo o falla de encendido. El Mecánico Oficial despacha kit de repuestos de inventario comunitario y herramientas."
                                )

                                ProtocolLevelItem(
                                    levelTag = "NIVEL 3: CAÍDA EN RUTA",
                                    levelColor = Color(0xFFE11D48),
                                    icon = Icons.Default.PersonalInjury,
                                    assignedSpecialist = "Médico / Paramédico + Seguridad Vial",
                                    protocolText = "¡NO RETIRAR EL CASCO BRUSCAMENTE! Bloqueadores aseguran el área a 50m con chalecos y luces. Médico aplica triage, limpieza de laceraciones e inmovilización. Mecánico evalúa si la moto rueda."
                                )

                                ProtocolLevelItem(
                                    levelTag = "NIVEL 4: CHOQUE / COLISIÓN GRAVE",
                                    levelColor = Color(0xFFDC2626),
                                    icon = Icons.Default.CarCrash,
                                    assignedSpecialist = "VEN 911 + Médicos del Club + Directiva",
                                    protocolText = "LLAMADO INMEDIATO AL VEN 911 Y TRÁNSITO. Cierre total de seguridad vial a 100m. Inmovilización cervical absoluta, control de signos vitales, notificación a familiares y donantes de sangre."
                                )
                            }
                        }
                    }
                }
            }

            // ─── Barra de Filtro de Incidentes ────────────────────────────────
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
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF25D366).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "WhatsApp Directo",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
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
                                        fontWeight = if (selectedLevelFilter == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedLevelFilter == option) option.color else Color(0xFF334155)
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
                                    selectedContainerColor = option.color.copy(alpha = 0.15f),
                                    containerColor = DashboardFondoConfig.ColorTarjetaClara
                                ),
                                border = BorderStroke(
                                    if (selectedLevelFilter == option) 1.5.dp else 1.dp,
                                    if (selectedLevelFilter == option) option.color else DashboardFondoConfig.ColorBordeClaro
                                )
                            )
                        }
                    }
                }
            }

            // ─── Listado de Alertas SOS ───────────────────────────────────────
            if (filteredAlerts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DashboardFondoConfig.ColorTarjetaClara,
                        border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No hay alertas activas en el nivel seleccionado.",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "¡Ruta despejada y hermandad rodando en paz!",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }
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

    // ─── Diálogo de Consentimiento de Ubicación (Primer Inicio) ─────────────
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                PreferenciasApp.sosConsentimientoUbicacionAceptado = true
                showConsentDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = DashboardFondoConfig.ColorRojoCarrera,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Aviso de Seguridad y Ubicación",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "El sistema de emergencias SOS Vial accede a tu ubicación GPS en tiempo real para compartirla inmediatamente con los pilotos, mecánicos y directiva de ruta solo cuando emitas una alerta de auxilio.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            Text(
                                text = "Tu ubicación será visible en el mapa táctico y grupo de WhatsApp para que la ayuda llegue con precisión.",
                                fontSize = 11.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PreferenciasApp.sosConsentimientoUbicacionAceptado = true
                        showConsentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido y Aceptar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }

    // ─── Modal de Emisión de Alerta SOS ──────────────────────────────────────
    if (showEmitSosDialog) {
        EmitSosDialog(
            currentMember = currentMember,
            initialType = initialSosType,
            onDismiss = { showEmitSosDialog = false },
            onBroadcast = { type, loc, details, blood, lat, lng ->
                onBroadcastSos(type, loc, details, blood, lat, lng)
                try {
                    com.example.meshtx.GestorMeshTx.emitirAlertaSos("🚨 SOS ${type.name}: $loc - $details", if (lat != 0.0) "$lat,$lng" else null)
                } catch (_: Exception) {}
                showEmitSosDialog = false
            }
        )
    }

    // ─── Modal de Gestión de Alerta (Directiva) ──────────────────────────────
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
private fun QuickAlertButton(
    data: QuickAlertData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = data.bgTint,
        border = BorderStroke(1.5.dp, data.color.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(74.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(data.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    data.icono,
                    contentDescription = null,
                    tint = data.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.nivelTag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = data.color
                )
                Text(
                    text = data.titulo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.subtitulo,
                    fontSize = 9.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, levelColor.copy(alpha = 0.35f)),
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
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = protocolText,
                fontSize = 11.sp,
                color = Color(0xFF334155),
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
        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
        border = BorderStroke(
            1.5.dp,
            when (alert.status) {
                EmergencyStatus.ACTIVA -> levelColor
                EmergencyStatus.EN_CAMINO -> StatusWarning
                EmergencyStatus.ATENDIDA -> StatusInfo
                EmergencyStatus.RESUELTA -> StatusSuccess
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                color = Color(0xFF0F172A)
            )
            Text(
                text = alert.bikeDetails,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location with Map intent click (Fondo Claro, Texto Oscuro)
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
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
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "GPS: ${alert.coordinateLat}, ${alert.coordinateLng} • $dateStr (Toca para mapa)",
                        fontSize = 10.sp,
                        color = Color(0xFFB45309),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specialist recommendation & protocol badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = levelColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, levelColor.copy(alpha = 0.25f)),
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
                        color = Color(0xFF334155),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details
            Text(
                text = alert.details,
                fontSize = 12.sp,
                color = Color(0xFF334155)
            )

            val bloodNeeded = alert.bloodTypeNeeded
            if (!bloodNeeded.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🩸 Grupo Sanguíneo Requerido:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                    Text(
                        text = bloodNeeded,
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
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reporte de Apoyo: ${alert.respondersNotes}",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_share_sos_whatsapp_${alert.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onCallReporter,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Llamar", fontSize = 11.sp, color = Color(0xFF0F172A))
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
        title = { Text("Gestionar Incidente SOS (Directiva)", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reportado por: ${alert.reporterName} (${alert.memberNumber})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                Text("Nivel: ${alert.emergencyType.levelTag} - ${alert.emergencyType.label}", color = Color(alert.emergencyType.severityColorHex), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Text("Estado del Incidente:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
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
                Text("Actualizar Registro", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
