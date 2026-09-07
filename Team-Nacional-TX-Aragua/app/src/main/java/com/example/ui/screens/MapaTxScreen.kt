package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoOrangePrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TxMapLauncher(
    onBackClick: () -> Unit,
    onNavigateToDirectory: (() -> Unit)? = null,
    onOpenMemberCarnetById: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var launched by remember { mutableStateOf(false) }

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

                // Sincronizar eventos publicados en el mapa
                try {
                    val db = com.example.data.local.AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
                    val pubs = db.publicationDao().getAllPublications().first()
                    val events = db.calendarDao().getAllEvents().first()
                    com.example.radar.GestorRadar.sincronizarEventosEnMapa(pubs, events)
                } catch (e: Exception) {
                    android.util.Log.w("MAPA_TX", "Error sincronizando eventos: ${e.message}")
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

    // Pantalla de carga mientras MapActivity arranca
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
        }
    }
}