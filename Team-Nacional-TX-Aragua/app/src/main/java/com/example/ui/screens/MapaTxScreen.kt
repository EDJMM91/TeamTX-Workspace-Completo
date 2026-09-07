package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmergencyType
import com.example.data.model.MemberProfile
import com.example.ui.preferences.PreferenciasApp
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.StatusError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ─── Tipos de alerta disponibles para emisión rápida ────────────────────────
private val SOS_QUICK_TYPES = listOf(
    Triple(EmergencyType.ACCIDENTADO_GASOLINA, Icons.Default.LocalGasStation, "Sin Gasolina"),
    Triple(EmergencyType.ACCIDENTADO_MECANICO, Icons.Default.Build, "Falla Mecánica"),
    Triple(EmergencyType.CAIDA, Icons.Default.PersonalInjury, "Caída en Ruta"),
    Triple(EmergencyType.CHOQUE, Icons.Default.CarCrash, "Choque Grave"),
    Triple(EmergencyType.EMERGENCIA_MEDICA, Icons.Default.MedicalServices, "Auxilio Médico"),
    Triple(EmergencyType.APOYO_SEGURIDAD, Icons.Default.Shield, "Peligro en Vía"),
    Triple(EmergencyType.ALCABALA_RETEN, Icons.Default.GppBad, "Alcabala/Retén")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxMapLauncher(
    onBackClick: () -> Unit,
    onNavigateToDirectory: (() -> Unit)? = null,
    onOpenMemberCarnetById: ((String) -> Unit)? = null,
    currentMember: MemberProfile? = null,
    onBroadcastSos: ((EmergencyType, String, String, String?, Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    var launched by remember { mutableStateOf(false) }

    // ─── Estado del Botón SOS Rápido ────────────────────────────────────────
    var showEmitSosDialog by remember { mutableStateOf(false) }
    var showSosTypeSelector by remember { mutableStateOf(false) }
    var selectedSosType by remember {
        val saved = try {
            PreferenciasApp.sosMapaTipoRapido?.let { EnumUtilesMapaTx.parseSosType(it) }
        } catch (_: Exception) { null }
        mutableStateOf(saved ?: EmergencyType.CAIDA)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Cuando MapActivity hace finish(), limpiar radar, eventos y directorio
        com.example.radar.GestorRadar.limpiarEventosDelMapa()
        com.example.radar.GestorRadar.limpiarDirectorioDelMapa()
        com.example.radar.GestorRadar.detener()

        val prefs = context.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
        val targetWorkshop = prefs.getString("target_workshop_name", null)
        val targetPilotCarnetId = prefs.getString("target_carnet_pilot_id", null)

        if (!targetWorkshop.isNullOrBlank()) {
            prefs.edit().remove("target_workshop_name").apply()
            onNavigateToDirectory?.invoke() ?: onBackClick()
        } else if (!targetPilotCarnetId.isNullOrBlank()) {
            prefs.edit().remove("target_carnet_pilot_id").apply()
            onOpenMemberCarnetById?.invoke(targetPilotCarnetId) ?: onBackClick()
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val app = context.applicationContext as? net.osmand.plus.OsmandApplication

            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                if (uid != null) {
                    val perfil = com.example.data.remote.PerfilNube.descargarPerfil(uid)
                    val nombre = perfil?.fullName?.ifBlank { "Piloto TX" } ?: "Piloto TX"
                    val rango = perfil?.role?.displayName ?: ""
                    val avatar = perfil?.profilePhotoUri ?: ""
                    val esDirectivo = perfil?.isDirectiva == true || perfil?.role?.canManageApp == true || 
                                     perfil?.role == com.example.data.model.MemberRole.PRESIDENTE || 
                                     perfil?.role == com.example.data.model.MemberRole.DIRECTIVA

                    val prefs = context.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("es_directivo_o_admin", esDirectivo)
                        .putString("radar_user_id", uid)
                        .putString("radar_nombre", nombre)
                        .putString("radar_rango", rango)
                        .putString("radar_avatar", avatar)
                        .apply()

                    if (avatar.isNotBlank()) {
                        com.example.radar.RadarFirebase.actualizarAvatarLocalDesdeUri(context, avatar)
                    }

                    if (com.example.radar.TelemetriaGps.estaActivo(context) || com.example.ui.preferences.PreferenciasApp.radarActivo) {
                        com.example.radar.TelemetriaGps.activar(context, uid, nombre, rango, avatar)
                    }

                    if (app != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            com.example.radar.GestorRadar.iniciar(app, uid, nombre, rango, avatar)
                        }
                    }
                }

                // Sincronizar eventos publicados y alertas SOS viales activas en el mapa
                try {
                    val db = com.example.data.local.AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
                    val pubs = db.publicationDao().getAllPublications().first()
                    val events = db.calendarDao().getAllEvents().first()
                    val allAlerts = db.emergencyDao().getAllAlerts().first()
                    val activeAlerts = allAlerts.filter { it.status != com.example.data.model.EmergencyStatus.RESUELTA }
                    com.example.radar.GestorRadar.sincronizarEventosEnMapa(pubs, events, activeAlerts)
                } catch (e: Exception) {
                    android.util.Log.w("MAPA_TX", "Error sincronizando eventos y SOS: ${e.message}")
                }

                // Sincronizar directorio de servicios y repuestos en el mapa
                try {
                    val db = com.example.data.local.AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
                    val workshops = db.workshopDirectoryDao().getAllWorkshops().first()
                    val prefs = context.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
                    val mostrarDirectorio = prefs.getBoolean("mostrar_directorio_en_mapa", true)
                    com.example.radar.GestorRadar.sincronizarDirectorioEnMapa(workshops, mostrarDirectorio)
                } catch (e: Exception) {
                    android.util.Log.w("MAPA_TX", "Error sincronizando directorio: ${e.message}")
                }

                // Centrar en destino específico si viene desde la Guía de Servicios o Calendario
                val prefs = context.getSharedPreferences("prefs_radar_tx", android.content.Context.MODE_PRIVATE)
                val targetLat = prefs.getString("target_dest_lat", null)?.toDoubleOrNull()
                val targetLon = prefs.getString("target_dest_lon", null)?.toDoubleOrNull()
                val targetName = prefs.getString("target_dest_name", null)

                if (targetLat != null && targetLon != null && targetLat != 0.0 && app != null) {
                    prefs.edit().remove("target_dest_lat").remove("target_dest_lon").remove("target_dest_name").apply()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        try {
                            val mapView = app.osmandMap?.mapView
                            mapView?.setLatLon(targetLat, targetLon)
                            if ((mapView?.zoom ?: 0) < 15) {
                                mapView?.setIntZoom(16)
                            }
                            mapView?.refreshMap(true)
                        } catch (_: Exception) {}
                    }
                }
            }

            // Intent directo y tipado al MapActivity de OsmAnd integrado en el mismo APK
            val intent = Intent(context, net.osmand.plus.activities.MapActivity::class.java)
            launcher.launch(intent)
        }
    }

    // ─── Diálogo de emisión SOS desde el Mapa ───────────────────────────────
    if (showEmitSosDialog && onBroadcastSos != null) {
        com.example.ui.screens.dialogs.EmitSosDialog(
            currentMember = currentMember,
            initialType = selectedSosType,
            onDismiss = { showEmitSosDialog = false },
            onBroadcast = { type, loc, details, blood, lat, lng ->
                onBroadcastSos(type, loc, details, blood, lat, lng)
                try {
                    if (type == EmergencyType.ALCABALA_RETEN) {
                        com.example.meshtx.GestorMeshTx.activarModoAlcabalaSos("Ubicación: $loc. $details")
                    } else {
                        com.example.meshtx.GestorMeshTx.emitirAlertaSos(
                            "🚨 SOS ${type.name}: $loc - $details",
                            if (lat != 0.0) "$lat,$lng" else null
                        )
                    }
                } catch (_: Exception) {}
                showEmitSosDialog = false
            }
        )
    }

    // ─── Selector de Tipo de Alerta (Long-Press) ─────────────────────────────
    if (showSosTypeSelector) {
        AlertDialog(
            onDismissRequest = { showSosTypeSelector = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, tint = StatusError, modifier = Modifier.size(22.dp))
                    Text(
                        "Tipo de Alerta Rápida",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            "Mantén presionado el botón SOS para cambiar el tipo. Al tocar, emite directamente con el tipo activo.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    items(SOS_QUICK_TYPES) { (type, icon, label) ->
                        val isSelected = selectedSosType == type
                        val typeColor = Color(type.severityColorHex)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) typeColor.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            tonalElevation = if (isSelected) 2.dp else 0.dp,
                            onClick = {
                                selectedSosType = type
                                PreferenciasApp.sosMapaTipoRapido = type.name
                                showSosTypeSelector = false
                                if (onBroadcastSos != null) showEmitSosDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${type.levelTag}: $label",
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) typeColor else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = type.recommendedSpecialist,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSosTypeSelector = false }) {
                    Text("Cerrar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // ─── Pantalla de Carga Mapa TX ──────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E24), Color(0xFF121212))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(
                color = MotoOrangePrimary,
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cargando Mapa TX...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Iniciando motor de navegación offline",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Volver al Dashboard",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ─── Botón SOS Rápido ─────────────────────────────────────────
            if (onBroadcastSos != null) {
                Spacer(modifier = Modifier.height(14.dp))

                val sosTypeColor = Color(selectedSosType.severityColorHex)
                val sosIcon = SOS_QUICK_TYPES.firstOrNull { it.first == selectedSosType }?.second ?: Icons.Default.Emergency

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E1E24),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(selectedSosType) {
                                detectTapGestures(
                                    onTap = {
                                        // Toque: emitir directamente con el tipo activo
                                        showEmitSosDialog = true
                                    },
                                    onLongPress = {
                                        // Presión larga: cambiar tipo de alerta
                                        showSosTypeSelector = true
                                    }
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = sosIcon,
                                contentDescription = "Emitir SOS",
                                tint = sosTypeColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "EMITIR ALERTA SOS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = sosTypeColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${selectedSosType.levelTag}: ${selectedSosType.label}",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = sosTypeColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toca para emitir · Mantén presionado para cambiar tipo",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─── Utilidades internas del Mapa TX ────────────────────────────────────────
private object EnumUtilesMapaTx {
    fun parseSosType(name: String): EmergencyType? =
        try { EmergencyType.valueOf(name) } catch (_: Exception) { null }
}

