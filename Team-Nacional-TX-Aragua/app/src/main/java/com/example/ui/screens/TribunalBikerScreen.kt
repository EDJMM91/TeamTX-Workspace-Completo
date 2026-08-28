package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DisciplinaryRecord
import com.example.data.model.MemberProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TribunalBikerScreen(
    records: List<DisciplinaryRecord>,
    allMembers: List<MemberProfile>,
    isDirectivaMode: Boolean,
    onAddRecord: (memberId: Long, memberName: String, reason: String, penaltyType: String, applySuspension: Boolean, durationDays: Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tribunal Disciplinario TX",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Historial y expedientes disciplinarios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isDirectivaMode) {
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.background(StatusError.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Gavel, contentDescription = "Sancionar", tint = StatusError)
                }
            }
        }

        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay registros disciplinarios. ¡Excelente comportamiento!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records.sortedByDescending { it.timestamp }) { record ->
                    DisciplinaryRecordCard(record)
                }
            }
        }
    }

    if (showAddDialog) {
        AddDisciplinaryRecordDialog(
            allMembers = allMembers,
            onDismiss = { showAddDialog = false },
            onSubmit = { id, name, reason, type, susp, days ->
                onAddRecord(id, name, reason, type, susp, days)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DisciplinaryRecordCard(record: DisciplinaryRecord) {
    val dateString = SimpleDateFormat("dd MMM yyyy", Locale("es", "VE")).format(Date(record.timestamp))
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError)
                    Text(
                        text = record.penaltyType.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = StatusError,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Miembro: ${record.memberName}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Motivo: ${record.reason}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Emitido por: ${record.issuedBy}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDisciplinaryRecordDialog(
    allMembers: List<MemberProfile>,
    onDismiss: () -> Unit,
    onSubmit: (Long, String, String, String, Boolean, Int) -> Unit
) {
    var expandedMember by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<MemberProfile?>(null) }
    var reason by remember { mutableStateOf("") }
    
    val penaltyOptions = listOf("Amonestación Verbal", "Suspensión Temporal", "Expulsión del Capítulo", "Expulsión Nacional")
    var expandedPenalty by remember { mutableStateOf(false) }
    var selectedPenalty by remember { mutableStateOf(penaltyOptions[0]) }
    
    var durationDaysStr by remember { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Sanción Disciplinaria", fontWeight = FontWeight.Bold, color = StatusError) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Member Selector
                ExposedDropdownMenuBox(
                    expanded = expandedMember,
                    onExpandedChange = { expandedMember = it }
                ) {
                    OutlinedTextField(
                        value = selectedMember?.fullName ?: "Seleccionar Miembro",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Miembro a sancionar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMember) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMember,
                        onDismissRequest = { expandedMember = false }
                    ) {
                        allMembers.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.nickname})") },
                                onClick = {
                                    selectedMember = m
                                    expandedMember = false
                                }
                            )
                        }
                    }
                }

                // Penalty Type Selector
                ExposedDropdownMenuBox(
                    expanded = expandedPenalty,
                    onExpandedChange = { expandedPenalty = it }
                ) {
                    OutlinedTextField(
                        value = selectedPenalty,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Sanción") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPenalty) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPenalty,
                        onDismissRequest = { expandedPenalty = false }
                    ) {
                        penaltyOptions.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    selectedPenalty = p
                                    expandedPenalty = false
                                }
                            )
                        }
                    }
                }

                if (selectedPenalty == "Suspensión Temporal") {
                    OutlinedTextField(
                        value = durationDaysStr,
                        onValueChange = { durationDaysStr = it },
                        label = { Text("Días de Suspensión") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo / Detalles") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = selectedMember
                    if (m != null && reason.isNotBlank()) {
                        val isSusp = selectedPenalty != "Amonestación Verbal"
                        val days = durationDaysStr.toIntOrNull() ?: 0
                        onSubmit(m.id, m.fullName, reason, selectedPenalty, isSusp, days)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
            ) {
                Text("APLICAR SANCIÓN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
