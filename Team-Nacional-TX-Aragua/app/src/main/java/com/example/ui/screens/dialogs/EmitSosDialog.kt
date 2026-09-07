package com.example.ui.screens.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.chat.GestorUbicacion
import com.example.data.model.EmergencyStatus
import com.example.data.model.EmergencyType
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.StatusError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmitSosDialog(
    currentMember: com.example.data.model.MemberProfile?,
    initialType: EmergencyType = EmergencyType.CAIDA,
    onDismiss: () -> Unit,
    onBroadcast: (type: EmergencyType, location: String, details: String, blood: String?, lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gestorUbicacion = remember { GestorUbicacion(context) }
    val prefs = remember { context.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE) }

    var currentLat by remember {
        val sLat = prefs.getString("last_lat", null)?.toDoubleOrNull()
        mutableStateOf(if (sLat != null && sLat != 0.0 && sLat != 10.4806) sLat else null)
    }
    var currentLng by remember {
        val sLon = prefs.getString("last_lon", null)?.toDoubleOrNull()
        mutableStateOf(if (sLon != null && sLon != 0.0 && sLon != -66.9036) sLon else null)
    }
    var gpsCaptured by remember { mutableStateOf(currentLat != null && currentLng != null) }
    var isResolvingGps by remember { mutableStateOf(true) }

    var selectedType by remember { mutableStateOf(initialType) }
    var location by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf(currentMember?.bloodType ?: "O+") }

    val primaryTypes = remember {
        listOf(
            EmergencyType.ACCIDENTADO_GASOLINA,
            EmergencyType.ACCIDENTADO_MECANICO,
            EmergencyType.CAIDA,
            EmergencyType.CHOQUE,
            EmergencyType.EMERGENCIA_MEDICA,
            EmergencyType.APOYO_SEGURIDAD,
            EmergencyType.ALCABALA_RETEN
        )
    }

    LaunchedEffect(Unit) {
        isResolvingGps = true
        try {
            val loc = gestorUbicacion.capturarLocationObjeto()
            if (loc != null) {
                currentLat = loc.latitude
                currentLng = loc.longitude
                gpsCaptured = true
                if (location.isBlank()) {
                    location = gestorUbicacion.obtenerNombreUbicacion(loc.latitude, loc.longitude)
                }
            } else if (currentLat != null && currentLng != null && location.isBlank()) {
                location = gestorUbicacion.obtenerNombreUbicacion(currentLat!!, currentLng!!)
            }
        } catch (_: Exception) {
        } finally {
            isResolvingGps = false
            if (location.isBlank()) {
                location = if (currentLat != null && currentLng != null) {
                    "Coordenadas GPS: %.5f, %.5f".format(currentLat, currentLng)
                } else {
                    "Tramo Carretero / Ubicación del Piloto"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError)
                Text("EMITIR ALERTA SOS POR NIVEL", fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text("1. Selecciona el Nivel del Incidente:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        primaryTypes.forEach { type ->
                            val isSelected = selectedType == type
                            val typeColor = Color(type.severityColorHex)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) typeColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) typeColor else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedType = type }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val icon = when (type.iconType) {
                                        "gas" -> Icons.Default.LocalGasStation
                                        "mechanic" -> Icons.Default.Build
                                        "fall" -> Icons.Default.PersonalInjury
                                        "crash" -> Icons.Default.CarCrash
                                        "medical" -> Icons.Default.MedicalServices
                                        "security" -> Icons.Default.Shield
                                        else -> Icons.Default.Warning
                                    }
                                    Icon(icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${type.levelTag}: ${type.label.uppercase()}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = typeColor
                                        )
                                        Text(
                                            text = "Especialista: ${type.recommendedSpecialist}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick preset buttons based on selected level
                item {
                    Text("Sugerencias rápidas para el reporte:", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                    val quickPresets = when (selectedType) {
                        EmergencyType.ALCABALA_RETEN -> listOf("Retén policial en vía", "Matraca / Retención indebida", "Punto de control sin identificación", "Revisión preventiva de papeles")
                        EmergencyType.ACCIDENTADO_GASOLINA -> listOf("Sin 95 octanos en hombrillo", "Tanque seco, requiere 3L", "Falla de medidor flotante")
                        EmergencyType.ACCIDENTADO_MECANICO -> listOf("Guaya de embrague rota", "Caucho espichado", "Cadena rota", "Falla eléctrica / batería")
                        EmergencyType.CAIDA -> listOf("Deslizamiento en asfalto húmedo", "Piloto consciente / Raspaduras", "Manubrio doblado")
                        EmergencyType.CHOQUE -> listOf("Colisión lateral con vehículo", "Impacto contra defensa", "Auxilio médico urgente")
                        EmergencyType.EMERGENCIA_MEDICA -> listOf("Mareo / Descompensación en ruta", "Reacción alérgica", "Traumatismo en muñeca/tobillo")
                        EmergencyType.APOYO_SEGURIDAD -> listOf("Derrame de aceite en vía", "Obstáculo / Escombros en curva", "Protesta / Paso cerrado")
                        else -> listOf("Auxilio requerido en sitio")
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickPresets) { preset ->
                            SuggestionChip(
                                onClick = {
                                    details = if (details.isBlank()) preset else "$details. $preset"
                                },
                                label = { Text(preset, fontSize = 10.sp, color = Color(0xFF1E293B)) }
                            )
                        }
                    }

                    if (selectedType == EmergencyType.ALCABALA_RETEN) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEDE9FE),
                            border = BorderStroke(1.dp, Color(0xFFC4B5FD)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Podcasts, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = "🎙️ Transmisión de Audio en Vivo Automática",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color(0xFF5B21B6)
                                    )
                                    Text(
                                        text = "Al pulsar emitir, el micrófono transmitirá continuamente por Mesh TX y datos móviles para que el convoy escuche todo en tiempo real.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF4C1D95),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Ubicación Exacta / Referencia Vial") },
                        placeholder = { Text("ej. ARC Km 68, pasando La Encrucijada") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sos_location")
                    )
                }
                item {
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Detalles del Incidente y Estado") },
                        placeholder = { Text("ej. Estado de la moto, lesiones o repuestos requeridos") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sos_details")
                    )
                }
                item {
                    OutlinedTextField(
                        value = bloodType,
                        onValueChange = { bloodType = it },
                        label = { Text("Tipo de Sangre del Afectado (O+, O-, A+, etc.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Indicador visual de Coordenada GPS capturada
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (gpsCaptured) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (gpsCaptured) Color(0xFF6EE7B7) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isResolvingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MotoOrangePrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = if (gpsCaptured) Color(0xFF059669) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isResolvingGps) "Detectando satélites GPS en tiempo real..."
                                           else if (gpsCaptured) "GPS en tiempo real detectado"
                                           else "Esperando señal satelital GPS...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gpsCaptured) Color(0xFF065F46) else Color(0xFF1E293B)
                                )
                                val coordsText = if (currentLat != null && currentLng != null) {
                                    "%.5f, %.5f (Copiado automático al portapapeles y Mapa TX)".format(currentLat, currentLng)
                                } else {
                                    "Esperando coordenadas exactas del piloto..."
                                }
                                Text(
                                    text = coordsText,
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isResolvingGps = true
                                        try {
                                            val loc = gestorUbicacion.capturarLocationObjeto()
                                            if (loc != null) {
                                                currentLat = loc.latitude
                                                currentLng = loc.longitude
                                                gpsCaptured = true
                                                location = gestorUbicacion.obtenerNombreUbicacion(loc.latitude, loc.longitude)
                                                Toast.makeText(context, "GPS actualizado: %.5f, %.5f".format(loc.latitude, loc.longitude), Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (_: Exception) {
                                        } finally {
                                            isResolvingGps = false
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Re-capturar GPS",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (location.isNotBlank()) {
                        coroutineScope.launch {
                            var finalLat = currentLat ?: 0.0
                            var finalLng = currentLng ?: 0.0
                            try {
                                val fresh = gestorUbicacion.capturarCoordenadaActual()
                                if (fresh != null && fresh.contains(",")) {
                                    val parts = fresh.split(",")
                                    val la = parts[0].trim().toDoubleOrNull()
                                    val lo = parts[1].trim().toDoubleOrNull()
                                    if (la != null && lo != null && la != 0.0 && lo != 0.0) {
                                        finalLat = la
                                        finalLng = lo
                                    }
                                }
                            } catch (_: Exception) {}

                            // Copiar coordenada exacta al portapapeles del sistema
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Coordenadas SOS TX", "$finalLat, $finalLng")
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "📍 Coordenadas $finalLat, $finalLng copiadas al portapapeles", Toast.LENGTH_LONG).show()
                            } catch (_: Exception) {}

                            // Registrar para centrado automático en Mapa TX
                            try {
                                prefs.edit()
                                    .putString("target_dest_lat", finalLat.toString())
                                    .putString("target_dest_lon", finalLng.toString())
                                    .putString("target_dest_name", "🚨 SOS: ${selectedType.label}")
                                    .apply()
                            } catch (_: Exception) {}

                            onBroadcast(
                                selectedType,
                                location,
                                details.ifBlank { "Auxilio requerido en sitio." },
                                bloodType,
                                finalLat,
                                finalLng
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(selectedType.severityColorHex)),
                modifier = Modifier.testTag("btn_confirm_broadcast_sos")
            ) {
                Text("EMITIR ALERTA SOS AHORA", fontWeight = FontWeight.Black, color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SuggestionChip(onClick: () -> Unit, label: @Composable () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        label()
    }
}