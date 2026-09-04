package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MaintenanceLog
import com.example.data.model.MemberProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    logs: List<MaintenanceLog>,
    currentMember: MemberProfile?,
    bcvRate: Double,
    onCreateLog: (
        odometerKm: Int,
        serviceType: String,
        brandOrDetails: String,
        costUsd: Double,
        workshopName: String,
        serviceDate: String,
        nextServiceKm: Int,
        notes: String
    ) -> Unit,
    onDeleteLog: (id: Long) -> Unit,
    onBack: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("TODOS") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val serviceTypes = listOf(
        "TODOS",
        "Cambio de Aceite",
        "Kit de Arrastre",
        "Frenos",
        "Guayas",
        "Bujía / Carburador",
        "Cauchos",
        "Batería",
        "Mantenimiento General"
    )

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == "TODOS") logs
        else logs.filter { it.serviceType.equals(selectedFilter, ignoreCase = true) }
    }

    val latestOdometer = logs.maxOfOrNull { it.odometerKm } ?: 0
    val totalSpentUsd = logs.sumOf { it.costUsd }
    val totalSpentBs = if (bcvRate > 0) totalSpentUsd * bcvRate else 0.0

    val lastOilChange = logs.filter { it.serviceType.contains("Aceite", ignoreCase = true) }.maxByOrNull { it.timestamp }
    val nextOilChangeKm = lastOilChange?.nextServiceKm ?: (latestOdometer + 3000)
    val kmRemainingForOil = nextOilChangeKm - latestOdometer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "BITÁCORA DE MANTENIMIENTO",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            "Control Mecánico y Odómetro TX",
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MotoOrangePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar Servicio", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Overview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ODÓMETRO REGISTRADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("$latestOdometer KM", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("GASTO TOTAL ACUMULADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("$${String.format("%.2f", totalSpentUsd)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                            if (totalSpentBs > 0) {
                                Text("Bs. ${String.format("%.2f", totalSpentBs)}", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Next Oil Alert Pill
                    val isOilDueSoon = kmRemainingForOil <= 500
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOilDueSoon) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOilDueSoon) Color(0xFFF87171) else Color(0xFF86EFAC))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOilDueSoon) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isOilDueSoon) Color(0xFFDC2626) else Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isOilDueSoon) "⚠️ PRÓXIMO CAMBIO DE ACEITE URGENTE" else "ESTADO DEL ACEITE DEL MOTOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOilDueSoon) Color(0xFFB91C1C) else Color(0xFF15803D)
                                )
                                Text(
                                    text = "Próximo cambio a los $nextOilChangeKm KM (Restan $kmRemainingForOil KM)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(serviceTypes) { type ->
                    val isSelected = selectedFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = type },
                        label = {
                            Text(
                                type,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MotoOrangePrimary else Color(0xFF334155)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoOrangePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = MotoOrangePrimary,
                            containerColor = Color.White,
                            labelColor = Color(0xFF334155)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFCBD5E1),
                            selectedBorderColor = MotoOrangePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Logs List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No hay mantenimientos registrados aún", color = Color(0xFF64748B), fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        MaintenanceLogCard(
                            log = log,
                            bcvRate = bcvRate,
                            onDelete = { onDeleteLog(log.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateMaintenanceLogDialog(
            currentOdometer = latestOdometer,
            onDismiss = { showCreateDialog = false },
            onConfirm = { odo, type, brand, cost, workshop, date, nextKm, notes ->
                onCreateLog(odo, type, brand, cost, workshop, date, nextKm, notes)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun MaintenanceLogCard(
    log: MaintenanceLog,
    bcvRate: Double,
    onDelete: () -> Unit
) {
    val costBs = if (bcvRate > 0) log.costUsd * bcvRate else 0.0
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MotoOrangePrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = log.serviceType.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MotoOrangePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${log.odometerKm} KM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Text(
                    text = log.serviceDate,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (log.brandOrDetails.isNotBlank()) {
                Text(
                    text = "Detalle: ${log.brandOrDetails}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            }

            if (log.workshopName.isNotBlank()) {
                Text(
                    text = "Taller: ${log.workshopName}",
                    fontSize = 12.sp,
                    color = Color(0xFF334155)
                )
            }

            if (log.notes.isNotBlank()) {
                Text(
                    text = "Notas: ${log.notes}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Costo: $${String.format("%.2f", log.costUsd)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF16A34A)
                    )
                    if (costBs > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Bs. ${String.format("%.2f", costBs)})",
                            fontSize = 11.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (log.nextServiceKm > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Próximo: ${log.nextServiceKm} KM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFDC2626), modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar registro?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Esta acción eliminará permanentemente el registro de servicio de ${log.serviceType} (${log.odometerKm} KM).", color = Color(0xFF334155)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
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
fun CreateMaintenanceLogDialog(
    currentOdometer: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        odometerKm: Int,
        serviceType: String,
        brandOrDetails: String,
        costUsd: Double,
        workshopName: String,
        serviceDate: String,
        nextServiceKm: Int,
        notes: String
    ) -> Unit
) {
    var odometerStr by remember { mutableStateOf(if (currentOdometer > 0) "$currentOdometer" else "") }
    var serviceType by remember { mutableStateOf("Cambio de Aceite") }
    var brandOrDetails by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var workshopName by remember { mutableStateOf("") }
    var serviceDate by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var nextServiceKmStr by remember { mutableStateOf("${currentOdometer + 3000}") }
    var notes by remember { mutableStateOf("") }

    val serviceTypes = listOf(
        "Cambio de Aceite",
        "Kit de Arrastre",
        "Frenos",
        "Guayas",
        "Bujía / Carburador",
        "Cauchos",
        "Batería",
        "Mantenimiento General"
    )

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedBorderColor = MotoOrangePrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = MotoOrangePrimary,
        unfocusedLabelColor = Color(0xFF64748B)
    )

    // Auto calculate recommended next KM when service type changes
    LaunchedEffect(serviceType, odometerStr) {
        val currentKm = odometerStr.toIntOrNull() ?: currentOdometer
        val increment = when {
            serviceType.contains("Aceite") -> 3000
            serviceType.contains("Arrastre") -> 15000
            serviceType.contains("Frenos") -> 8000
            serviceType.contains("Bujía") -> 6000
            serviceType.contains("Cauchos") -> 20000
            else -> 5000
        }
        nextServiceKmStr = "${currentKm + increment}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MotoOrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Mantenimiento TX", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Tipo de Servicio:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(serviceTypes) { type ->
                            val isSel = serviceType == type
                            FilterChip(
                                selected = isSel,
                                onClick = { serviceType = type },
                                label = {
                                    Text(
                                        type,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MotoOrangePrimary else Color(0xFF334155)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MotoOrangePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = MotoOrangePrimary,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSel,
                                    borderColor = Color(0xFFCBD5E1),
                                    selectedBorderColor = MotoOrangePrimary
                                )
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = odometerStr,
                        onValueChange = { odometerStr = it },
                        label = { Text("Kilometraje Actual (KM)") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = brandOrDetails,
                        onValueChange = { brandOrDetails = it },
                        label = { Text("Marca / Repuesto / Detalle") },
                        placeholder = { Text("ej. Motul 20W50, Bujía NGK") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Costo en USD ($)") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = workshopName,
                        onValueChange = { workshopName = it },
                        label = { Text("Taller / Mecánico (Opcional)") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = serviceDate,
                        onValueChange = { serviceDate = it },
                        label = { Text("Fecha del Servicio (DD/MM/AAAA)") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = nextServiceKmStr,
                        onValueChange = { nextServiceKmStr = it },
                        label = { Text("Próximo Servicio a los (KM)") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observaciones / Notas") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odometerStr.toIntOrNull() ?: 0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    val nextKm = nextServiceKmStr.toIntOrNull() ?: 0
                    onConfirm(odo, serviceType, brandOrDetails, cost, workshopName, serviceDate, nextKm, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Guardar Servicio", color = Color.White, fontWeight = FontWeight.Bold)
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
