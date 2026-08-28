package com.example.ui.screens

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
import com.example.data.model.MemberProfile
import com.example.ui.theme.MotoOrangePrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ControlAsistenciaScreen(
    allMembers: List<MemberProfile>,
    isDirectivaMode: Boolean,
    onSaveAttendance: (eventName: String, eventDate: Long, attendedMemberIds: List<Long>) -> Unit
) {
    var showNewEventDialog by remember { mutableStateOf(false) }

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
                    text = "Control de Asistencia",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Jueves Moteros y Eventos Oficiales",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isDirectivaMode) {
                IconButton(
                    onClick = { showNewEventDialog = true },
                    modifier = Modifier.background(MotoOrangePrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = "Pasar Lista", tint = MotoOrangePrimary)
                }
            }
        }
        
        // Placeholder for History
        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = "El historial de asistencia aparecerá aquí.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showNewEventDialog) {
        NewAttendanceDialog(
            allMembers = allMembers,
            onDismiss = { showNewEventDialog = false },
            onSubmit = { eventName, attendedIds ->
                onSaveAttendance(eventName, System.currentTimeMillis(), attendedIds)
                showNewEventDialog = false
            }
        )
    }
}

@Composable
fun NewAttendanceDialog(
    allMembers: List<MemberProfile>,
    onDismiss: () -> Unit,
    onSubmit: (String, List<Long>) -> Unit
) {
    var eventName by remember { mutableStateOf("Jueves Motero") }
    val attendedIds = remember { mutableStateListOf<Long>() }
    val dateString = SimpleDateFormat("dd MMM yyyy", Locale("es", "VE")).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pasar Asistencia", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.8f)) {
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text("Nombre del Evento") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fecha: $dateString", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allMembers) { member ->
                        val isChecked = attendedIds.contains(member.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) attendedIds.add(member.id)
                                    else attendedIds.remove(member.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MotoOrangePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(member.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(member.role.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(eventName, attendedIds) },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("GUARDAR ASISTENCIA")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
