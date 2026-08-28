package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@Composable
fun ProgresoProspectoScreen(
    currentMember: MemberProfile,
    isDirectivaMode: Boolean,
    onRequestValidation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Progreso de Prospecto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Ruta hacia Piloto Oficial",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Requisitos según Normativa:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                RequirementItem(
                    title = "1. Propietario de Moto Tx Marca EK",
                    subtitle = "Validado al registro",
                    isCompleted = true
                )
                
                RequirementItem(
                    title = "2. Documentación Vigente",
                    subtitle = "Guantera Digital completa",
                    isCompleted = false // TODO: check profile files
                )
                
                RequirementItem(
                    title = "3. Jueves Moteros (4 meses)",
                    subtitle = "Asistencias registradas: 0 / 16",
                    isCompleted = false,
                    progress = 0.0f
                )
                
                RequirementItem(
                    title = "4. Rodada Larga Oficial",
                    subtitle = "Al menos 2 estados de distancia",
                    isCompleted = false
                )
                
                RequirementItem(
                    title = "5. Eventos de Envergadura (2)",
                    subtitle = "Motero y Benéfico",
                    isCompleted = false,
                    progress = 0.0f
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onRequestValidation,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("SOLICITAR VALIDACIÓN DE ENLACES", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RequirementItem(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    progress: Float? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (progress != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MotoOrangePrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
