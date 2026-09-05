package com.example.ui.screens

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import android.content.Intent
import android.widget.Toast
import com.example.data.model.MemberProfile
import com.example.ui.preferences.PreferenciasApp
import com.example.ui.theme.*
import com.example.mapa.GestorPortapapeles
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

enum class SpeedometerMode(val displayName: String, val shortName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DIGITAL("Digital Sport", "Digital", Icons.Default.Speed),
    ANALOGICO("Analógico TX", "Agujas", Icons.Default.AvTimer),
    MIXTO("Mixto Rally", "Rally", Icons.Default.DashboardCustomize),
    HUD("Proyector HUD", "HUD", Icons.Default.NightlightRound)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelocimetroScreen(
    currentMember: MemberProfile? = null,
    onAccumulateKm: (Double) -> Unit = {},
    onUpdateTopSpeed: (Float) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(SpeedometerMode.ANALOGICO) }
    
    // Unidad de velocidad (KM/H vs MPH)
    var isMph by remember { mutableStateOf(PreferenciasApp.velocidadEnMph) }
    
    // Modo Pantalla Completa Gigante
    var isFullscreen by remember { mutableStateOf(false) }

    // Control de visibilidad de paneles inferiores (Auto-escondido para más espacio)
    var areMetricsVisible by remember { mutableStateOf(true) }

    // Toggle de modo espejo en HUD
    var isHudMirrored by remember { mutableStateOf(false) }

    // Estados de Velocidad y Telemetría
    var currentGpsSpeedKmh by remember { mutableFloatStateOf(0f) }
    var effectiveSpeedKmh by remember { mutableFloatStateOf(0f) }
    var tripMaxSpeedKmh by remember { mutableFloatStateOf(0f) }
    var persistentRecordKmh by remember { mutableFloatStateOf(PreferenciasApp.topSpeedRecordKmh) }
    var isNewRecordAchieved by remember { mutableStateOf(false) }

    // Odómetro Total (ODO) y Parcial (TRIP)
    var tripDistanceMeters by remember { mutableDoubleStateOf(0.0) }
    var totalOdoKm by remember { mutableDoubleStateOf(PreferenciasApp.odometroTotalKm) }

    // Métricas de Viaje y Telemetría Avanzada
    var altitudeMeters by remember { mutableDoubleStateOf(0.0) }
    var maxAltitudeMeters by remember { mutableDoubleStateOf(0.0) }
    var bearingDegrees by remember { mutableFloatStateOf(0f) }
    var gForceInstant by remember { mutableFloatStateOf(0f) }
    var maxGForceSession by remember { mutableFloatStateOf(0f) }
    var arrancadaGForce by remember { mutableFloatStateOf(0f) }
    var instantAccelMss by remember { mutableFloatStateOf(0f) }
    var maxAccelMssSession by remember { mutableFloatStateOf(0f) }
    var isGpsActive by remember { mutableStateOf(false) }
    var isGpsSearching by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var isTracking by remember { mutableStateOf(true) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var lastAccelTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val sessionStartTimeMillis = remember { System.currentTimeMillis() }

    // Variables detectoras de arrancada (0 -> >20 km/h)
    var wasStopped by remember { mutableStateOf(true) }
    var launchPeakG by remember { mutableFloatStateOf(0f) }

    // 📣 Avisos Interactivos y Animaciones Flotantes
    var avisoActualTitulo by remember { mutableStateOf<String?>(null) }
    var avisoActualSubtitulo by remember { mutableStateOf<String?>(null) }
    var avisoActualIcono by remember { mutableStateOf(Icons.Default.Speed) }
    var avisoActualColor by remember { mutableStateOf(MotoOrangePrimary) }
    var lastAlertedMilestone by remember { mutableIntStateOf(0) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Temporizador para auto-descartar aviso flotante interactivo
    LaunchedEffect(avisoActualTitulo) {
        if (avisoActualTitulo != null) {
            delay(4200L)
            avisoActualTitulo = null
        }
    }

    // Estados de Intercomunicador Mesh TX en Malla
    val estadoMalla by com.example.meshtx.GestorMeshTx.estadoConexion.collectAsState()
    val nodosMalla by com.example.meshtx.GestorMeshTx.nodosEnRed.collectAsState()
    val canalMalla by com.example.meshtx.GestorMeshTx.canalActual.collectAsState()

    // Animación de Inicialización (Self-Test / Needle Sweep)
    var isSelfTestRunning by remember { mutableStateOf(true) }
    val testAnim = remember { Animatable(0f) }

    // Diálogo para resetear récord histórico
    var showResetRecordDialog by remember { mutableStateOf(false) }

    // Permisos de Ubicación en Tiempo Real
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Mantener la pantalla encendida mientras se usa el velocímetro
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Cronómetro de viaje
    LaunchedEffect(isTracking) {
        while (isTracking) {
            delay(1000L)
            if (!isSelfTestRunning) {
                elapsedSeconds++
            }
        }
    }

    // Animación de inicio tipo tablero racing (0 -> MAX -> 0)
    LaunchedEffect(Unit) {
        val maxTest = if (isMph) 120f else 200f
        testAnim.animateTo(
            targetValue = maxTest,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
        delay(200L)
        testAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        isSelfTestRunning = false
    }

    // Sensor de Acelerómetro (Cálculo Físico de Fuerza G Instantánea y Aceleración)
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val now = System.currentTimeMillis()
                lastAccelTimestamp = now

                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                // Magnitud vectorial de aceleración instantánea y Fuerza G
                val totalAccel = sqrt(ax * ax + ay * ay + az * az)
                instantAccelMss = totalAccel
                val currentG = (totalAccel / 9.80665f).coerceIn(0f, 6.0f)
                gForceInstant = currentG

                if (currentG > maxGForceSession) {
                    maxGForceSession = currentG
                }
                if (totalAccel > maxAccelMssSession) {
                    maxAccelMssSession = totalAccel
                }
                if (wasStopped && currentG > launchPeakG) {
                    launchPeakG = currentG
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(
            sensorListener,
            accelSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
        }
    }

    // Listener de GPS de alta frecuencia (Sincronización en tiempo real y Odómetro)
    DisposableEffect(context, hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        try {
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastGps?.let { loc ->
                altitudeMeters = loc.altitude
                bearingDegrees = loc.bearing
                lastLocation = loc
            }
        } catch (_: SecurityException) {}

        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                isGpsActive = true
                isGpsSearching = false

                // Cálculo puro de velocidad por satélite GPS (m/s * 3.6 = km/h)
                val speedKmh = if (loc.hasSpeed()) {
                    loc.speed * 3.6f
                } else {
                    lastLocation?.let { prev ->
                        val dt = (loc.time - prev.time) / 1000f
                        if (dt in 0.2f..5.0f) {
                            (prev.distanceTo(loc) / dt) * 3.6f
                        } else 0f
                    } ?: 0f
                }

                // Filtrar ruido de vibración o deriva quieta
                val filteredSpeed = if (speedKmh < 1.2f) 0f else speedKmh
                currentGpsSpeedKmh = filteredSpeed
                effectiveSpeedKmh = filteredSpeed

                // Récord del viaje actual
                if (filteredSpeed > tripMaxSpeedKmh) {
                    tripMaxSpeedKmh = filteredSpeed
                }

                val speedUnitLabel = if (isMph) "MPH" else "KM/H"
                val displayCurrentSpeed = if (isMph) filteredSpeed * 0.621371f else filteredSpeed

                // Detección de Arrancada (Fuerza G de despegue desde 0 km/h)
                if (filteredSpeed < 4f) {
                    wasStopped = true
                } else if (wasStopped && filteredSpeed >= 20f) {
                    wasStopped = false
                    val gArrancada = if (launchPeakG > 0.25f) launchPeakG else (gForceInstant.coerceAtLeast(0.42f))
                    arrancadaGForce = gArrancada
                    launchPeakG = 0f
                    avisoActualTitulo = "🚀 ¡ARRANCADA TÁCTICA TX!"
                    avisoActualSubtitulo = "Tu moto tiene una fuerza de ${String.format(Locale.US, "%.2f", gArrancada)} G de arrancada"
                    avisoActualIcono = Icons.Default.RocketLaunch
                    avisoActualColor = MotoOrangePrimary
                }

                // Detección de Hitos de Velocidad (40, 60, 80, 100, 120, 140)
                val curSpeedInt = displayCurrentSpeed.toInt()
                val milestones = listOf(40, 60, 80, 100, 120, 140)
                for (m in milestones) {
                    if (curSpeedInt >= m && lastAlertedMilestone < m) {
                        lastAlertedMilestone = m
                        val (msgSub, iconoHito) = when (m) {
                            40 -> Pair("Ritmo urbano fluido en caravana 🏍️", Icons.Default.TwoWheeler)
                            60 -> Pair("Velocidad de crucero avenida 💨", Icons.Default.Speed)
                            80 -> Pair("Modo carretera y curvas interurbanas 🛣️", Icons.Default.AltRoute)
                            100 -> Pair("Convoy rápido en autopista regional ⚡", Icons.Default.Bolt)
                            120 -> Pair("¡Velocidad crucero de ruta TX alcanzada! 🔥", Icons.Default.LocalFireDepartment)
                            else -> Pair("⚠️ Alerta táctica de alta velocidad en asfalto", Icons.Default.Warning)
                        }
                        avisoActualTitulo = "¡HAS ALCANZADO $m $speedUnitLabel!"
                        avisoActualSubtitulo = msgSub
                        avisoActualIcono = iconoHito
                        avisoActualColor = if (m >= 100) TxFlameRed else MotoOrangePrimary
                        break
                    }
                }
                if (curSpeedInt < 25 && lastAlertedMilestone > 0) {
                    lastAlertedMilestone = 0
                }

                // Récord histórico persistente del piloto con aviso interactivo
                if (filteredSpeed > persistentRecordKmh && filteredSpeed < 300f) {
                    if (persistentRecordKmh > 10f && !isNewRecordAchieved) {
                        avisoActualTitulo = "🏆 ¡PASASTE EL RÉCORD!"
                        avisoActualSubtitulo = "¡Nueva marca histórica: ${String.format(Locale.US, "%.1f", displayCurrentSpeed)} $speedUnitLabel! 🏁"
                        avisoActualIcono = Icons.Default.EmojiEvents
                        avisoActualColor = MotoGoldSecondary
                    }
                    persistentRecordKmh = filteredSpeed
                    PreferenciasApp.topSpeedRecordKmh = filteredSpeed
                    isNewRecordAchieved = true
                    onUpdateTopSpeed(filteredSpeed)
                }

                altitudeMeters = loc.altitude
                if (loc.altitude > maxAltitudeMeters) {
                    maxAltitudeMeters = loc.altitude
                }
                bearingDegrees = loc.bearing

                // Odómetro: Sumar distancia acumulada si hay movimiento real (>1.2 km/h)
                lastLocation?.let { prev ->
                    val dist = prev.distanceTo(loc)
                    if (dist > 0.8f && filteredSpeed > 1.2f) {
                        tripDistanceMeters += dist
                        val addedKm = dist / 1000.0
                        totalOdoKm += addedKm
                        PreferenciasApp.odometroTotalKm = totalOdoKm
                        onAccumulateKm(addedKm)
                    }
                }
                lastLocation = loc

                android.util.Log.d("TEAM_TX_VELOCIMETRO", "🛰️ GPS: $filteredSpeed km/h | G: $gForceInstant | ODO: $totalOdoKm km | TRIP: $tripDistanceMeters m")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) isGpsSearching = true
            }
            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    isGpsActive = false
                    isGpsSearching = false
                }
            }
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                300L,
                0.2f,
                locationListener
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                1.0f,
                locationListener
            )
        } catch (_: SecurityException) {
            isGpsActive = false
            isGpsSearching = false
        }

        onDispose {
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (_: Exception) {}
        }
    }

    // Velocidad a renderizar (Self-Test o Velocidad Real)
    val rawSpeed = if (isSelfTestRunning) testAnim.value else effectiveSpeedKmh

    // Conversión de unidades
    val displaySpeed = if (isMph) rawSpeed * 0.621371f else rawSpeed
    val displayTripMax = if (isMph) tripMaxSpeedKmh * 0.621371f else tripMaxSpeedKmh
    val displayRecord = if (isMph) persistentRecordKmh * 0.621371f else persistentRecordKmh

    val animatedSpeed by animateFloatAsState(
        targetValue = displaySpeed,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "SpeedGaugeAnimation"
    )

    val avgSpeed = if (elapsedSeconds > 0) {
        val distKm = tripDistanceMeters / 1000.0
        val distUnit = if (isMph) distKm * 0.621371 else distKm
        distUnit / (elapsedSeconds / 3600.0)
    } else 0.0

    val cardinalDirection = when (bearingDegrees) {
        in 22.5f..67.5f -> "NE"
        in 67.5f..112.5f -> "E"
        in 112.5f..157.5f -> "SE"
        in 157.5f..202.5f -> "S"
        in 202.5f..247.5f -> "SO"
        in 247.5f..292.5f -> "O"
        in 292.5f..337.5f -> "NO"
        else -> "N"
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VELOCÍMETRO TX",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                        }
                    },
                    actions = {
                        // Botón KM/H <-> MPH
                        OutlinedButton(
                            onClick = {
                                isMph = !isMph
                                PreferenciasApp.velocidadEnMph = isMph
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isMph) MotoGoldSecondary.copy(alpha = 0.2f) else MotoOrangePrimary.copy(alpha = 0.2f),
                                contentColor = if (isMph) MotoGoldSecondary else MotoOrangePrimary
                            ),
                            border = BorderStroke(1.dp, if (isMph) MotoGoldSecondary else MotoOrangePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(text = if (isMph) "MPH" else "KM/H", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Botón Reporte de Telemetría
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = "Generar Reporte Telemetría", tint = MotoGoldSecondary, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Botón Expandir Pantalla Completa
                        IconButton(
                            onClick = { isFullscreen = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Pantalla Completa", tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        // Estado del GPS Píldora
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                !hasLocationPermission -> StatusError.copy(alpha = 0.2f)
                                isGpsActive -> StatusSuccess.copy(alpha = 0.2f)
                                isGpsSearching -> MotoGoldSecondary.copy(alpha = 0.2f)
                                else -> StatusError.copy(alpha = 0.2f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    !hasLocationPermission -> StatusError
                                    isGpsActive -> StatusSuccess
                                    isGpsSearching -> MotoGoldSecondary
                                    else -> StatusError
                                }
                            ),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clickable {
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                !hasLocationPermission -> StatusError
                                                isGpsActive -> StatusSuccess
                                                isGpsSearching -> MotoGoldSecondary
                                                else -> StatusError
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        !hasLocationPermission -> "SIN PERMISO"
                                        isGpsActive -> "GPS ON"
                                        isGpsSearching -> "BUSCANDO"
                                        else -> "SIN GPS"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !hasLocationPermission -> StatusError
                                        isGpsActive -> StatusSuccess
                                        isGpsSearching -> MotoGoldSecondary
                                        else -> StatusError
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = TxCarbonDark)
                )
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else padding)
                .background(Color.Black)
        ) {
            // ═══════════════════════════════════════════════
            // 📣 AVISOS INTERACTIVOS & ANIMACIONES FLOTANTES (HUD)
            // ═══════════════════════════════════════════════
            AnimatedVisibility(
                visible = avisoActualTitulo != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isFullscreen) 56.dp else 8.dp)
                    .zIndex(30f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10141E).copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, avisoActualColor),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { avisoActualTitulo = null }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(avisoActualColor.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, avisoActualColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = avisoActualIcono,
                                contentDescription = null,
                                tint = avisoActualColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = avisoActualTitulo ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = avisoActualColor
                            )
                            avisoActualSubtitulo?.let { sub ->
                                Text(
                                    text = sub,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isFullscreen) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ═══════════════════════════════════════════════
                // BARRA SUPERIOR EXCLUSIVA PANTALLA COMPLETA
                // SOLO 2 BOTONES: [KM/H - MPH] Y [VOLVER A PANTALLA NORMAL]
                // ═══════════════════════════════════════════════
                if (isFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Selector de KM/H a MPH
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TxCarbonDark.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, if (isMph) MotoGoldSecondary else MotoOrangePrimary),
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    isMph = !isMph
                                    PreferenciasApp.velocidadEnMph = isMph
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (isMph) MotoGoldSecondary else MotoOrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMph) "MPH" else "KM/H",
                                    color = if (isMph) MotoGoldSecondary else MotoOrangePrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // 2. Volver a Pantalla Normal
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TxCarbonDark.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .height(36.dp)
                                .clickable { isFullscreen = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Volver a Pantalla Normal",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Normal",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // BARRA SELECTORA DE MODOS (Solo en Vista Normal)
                // ═══════════════════════════════════════════════
                if (!isFullscreen) {
                    Surface(
                        color = TxCarbonDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TxSteelSilver.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SpeedometerMode.values().forEach { mode ->
                                val isSelected = selectedMode == mode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MotoOrangePrimary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clickable { selectedMode = mode }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = mode.icon,
                                            contentDescription = mode.displayName,
                                            tint = if (isSelected) Color.Black else Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = mode.shortName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 🎙️ BANDA TÁCTICA INTERCOMUNICADOR MESH TX (Solo en Vista Normal)
                if (!isFullscreen) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (estadoMalla != com.example.meshtx.MeshEstadoConexion.DESCONECTADO) Color(0xFF1E293B) else Color(0xFF121620),
                        border = BorderStroke(1.dp, if (estadoMalla != com.example.meshtx.MeshEstadoConexion.DESCONECTADO) MotoOrangePrimary else Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    Icons.Default.Podcasts,
                                    contentDescription = null,
                                    tint = if (estadoMalla != com.example.meshtx.MeshEstadoConexion.DESCONECTADO) MotoOrangePrimary else Color.Gray,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (estadoMalla != com.example.meshtx.MeshEstadoConexion.DESCONECTADO)
                                        "Mesh TX • ${canalMalla.nombre}"
                                    else
                                        "Mesh TX Apagado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (estadoMalla != com.example.meshtx.MeshEstadoConexion.DESCONECTADO)
                                    "${nodosMalla.size} pilotos en convoy"
                                else
                                    "Offline",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (estadoMalla != com.example.meshtx.MeshEstadoConexion.DESCONECTADO) Color(0xFF38BDF8) else Color.Gray
                            )
                        }
                    }
                }

                // 🛡️ BANDA DE IDENTIFICACIÓN DE PILOTO CON FOTO DE CARNET TX (Solo en Vista Normal)
                if (!isFullscreen) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF121620),
                        border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Foto del piloto sincronizada de Carnet TX
                                val fotoPiloto = currentMember?.profilePhotoUri?.ifBlank { null } ?: PreferenciasApp.carnetGooglePhotoUrl?.ifBlank { null }
                                if (!fotoPiloto.isNullOrBlank()) {
                                    SubcomposeAsyncImage(
                                        model = fotoPiloto,
                                        contentDescription = "Foto Piloto",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, MotoOrangePrimary, CircleShape),
                                        loading = {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MotoOrangePrimary, strokeWidth = 2.dp)
                                            }
                                        },
                                        error = {
                                            Icon(
                                                imageVector = Icons.Default.TwoWheeler,
                                                contentDescription = null,
                                                tint = MotoOrangePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MotoOrangePrimary.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, MotoOrangePrimary),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.TwoWheeler,
                                                contentDescription = "Piloto TX",
                                                tint = MotoOrangePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Piloto:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TxSteelSilver
                                        )
                                        Text(
                                            text = currentMember?.fullName?.ifBlank { "Piloto Oficial TX" } ?: "Piloto Oficial TX",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!currentMember?.nickname.isNullOrBlank()) {
                                        Text(
                                            text = "\"${currentMember?.nickname}\"",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MotoOrangePrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MotoOrangePrimary.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, MotoOrangePrimary)
                            ) {
                                Text(
                                    text = currentMember?.memberNumber?.ifBlank { "TX-CARNET" } ?: "TX-CARNET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MotoOrangePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // VELOCÍMETRO PRINCIPAL SEGÚN MODO
                // ═══════════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val isHud = selectedMode == SpeedometerMode.HUD
                    val hudScaleY = if (isHud && isHudMirrored) -1f else 1f

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(scaleX = 1f, scaleY = hudScaleY),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedMode) {
                            SpeedometerMode.DIGITAL -> {
                                DigitalSpeedometerView(
                                    speed = animatedSpeed,
                                    isMph = isMph,
                                    isHud = false,
                                    gForce = gForceInstant,
                                    accelMss = instantAccelMss,
                                    isFullscreen = isFullscreen
                                )
                            }
                            SpeedometerMode.ANALOGICO -> {
                                AnalogSpeedometerView(
                                    speed = animatedSpeed,
                                    isMph = isMph,
                                    gForce = gForceInstant,
                                    isFullscreen = isFullscreen
                                )
                            }
                            SpeedometerMode.MIXTO -> {
                                MixtoSpeedometerView(
                                    speed = animatedSpeed,
                                    isMph = isMph,
                                    cardinal = cardinalDirection,
                                    altitude = altitudeMeters,
                                    gForce = gForceInstant,
                                    isFullscreen = isFullscreen
                                )
                            }
                            SpeedometerMode.HUD -> {
                                DigitalSpeedometerView(
                                    speed = animatedSpeed,
                                    isMph = isMph,
                                    isHud = true,
                                    gForce = gForceInstant,
                                    accelMss = instantAccelMss,
                                    isFullscreen = isFullscreen
                                )
                            }
                        }
                    }

                    // Botón para activar/desactivar modo espejo en HUD
                    if (selectedMode == SpeedometerMode.HUD && !isFullscreen) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, if (isHudMirrored) MotoGoldSecondary else Color.Gray),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clickable { isHudMirrored = !isHudMirrored }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, tint = if (isHudMirrored) MotoGoldSecondary else Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHudMirrored) "Espejo ON" else "Espejo OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHudMirrored) MotoGoldSecondary else Color.White
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // BARRA DE AUTO-ESCONDIDO & TELEMETRÍA INFERIOR
                // ═══════════════════════════════════════════════
                if (!isFullscreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TELEMETRÍA & ODÓMETRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TxSteelSilver
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MotoOrangePrimary.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, MotoOrangePrimary),
                                modifier = Modifier
                                    .clickable { showReportDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Reporte", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { areMetricsVisible = !areMetricsVisible },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(
                                    if (areMetricsVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MotoOrangePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (areMetricsVisible) "Ocultar" else "Mostrar",
                                    fontSize = 10.sp,
                                    color = MotoOrangePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = areMetricsVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = TxCarbonDark,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, TxSteelSilver.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val distUnitTrip = if (isMph) (tripDistanceMeters / 1000.0) * 0.621371 else tripDistanceMeters / 1000.0
                                val distUnitOdo = if (isMph) totalOdoKm * 0.621371 else totalOdoKm
                                val unitLabel = if (isMph) "MI" else "KM"
                                val speedUnit = if (isMph) "MPH" else "KM/H"

                                val hours = elapsedSeconds / 3600
                                val minutes = (elapsedSeconds % 3600) / 60
                                val seconds = elapsedSeconds % 60
                                val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                                // Fila 1: ODO TOTAL + TRIP (con botón reset) + TIEMPO
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TripStatItem(
                                        title = "ODO TOTAL",
                                        value = String.format("%.1f %s", distUnitOdo, unitLabel),
                                        icon = Icons.Default.Numbers,
                                        valueColor = MotoGoldSecondary
                                    )

                                    // TRIP con botón de reset incorporado
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "TRIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TxSteelSilver)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MotoOrangePrimary.copy(alpha = 0.2f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        tripDistanceMeters = 0.0
                                                        tripMaxSpeedKmh = 0f
                                                        elapsedSeconds = 0L
                                                        isNewRecordAchieved = false
                                                        android.widget.Toast.makeText(context, "🔄 Odómetro TRIP reiniciado a 0.00", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(text = "RESET", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                                            }
                                        }
                                        Text(
                                            text = String.format("%.2f %s", distUnitTrip, unitLabel),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    TripStatItem(
                                        title = "TIEMPO",
                                        value = timeFormatted,
                                        icon = Icons.Default.Timer,
                                        valueColor = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = TxSteelSilver.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Fila 2: MÁXIMA VIAJE + PROMEDIO + RÉCORD CARNET TX (con botón reset)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TripStatItem(
                                        title = "MÁX. VIAJE",
                                        value = String.format("%.0f %s", displayTripMax, speedUnit),
                                        icon = Icons.Default.Speed,
                                        valueColor = Color.White
                                    )

                                    TripStatItem(
                                        title = "PROMEDIO",
                                        value = String.format("%.0f %s", avgSpeed, speedUnit),
                                        icon = Icons.Default.TrendingUp,
                                        valueColor = Color.White
                                    )

                                    // RÉCORD HISTÓRICO (CARNET TX)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = if (isNewRecordAchieved) MotoGoldSecondary else Color(0xFFFFD54F), modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(text = "TOP RECORD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isNewRecordAchieved) MotoGoldSecondary else TxSteelSilver)
                                            IconButton(
                                                onClick = { showResetRecordDialog = true },
                                                modifier = Modifier.size(16.dp).padding(start = 2.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                        Text(
                                            text = String.format("%.0f %s", displayRecord, speedUnit),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isNewRecordAchieved) MotoGoldSecondary else MotoOrangePrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = TxSteelSilver.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Fila 3: FUERZA G + ARRANCADA + ACELERACIÓN + RUMBO / ALT
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TripStatItem(
                                        title = "FUERZA G",
                                        value = String.format("%.2f G", gForceInstant),
                                        icon = Icons.Default.Compress,
                                        valueColor = if (gForceInstant > 0.4f) MotoOrangePrimary else Color.White
                                    )

                                    TripStatItem(
                                        title = "ARRANCADA",
                                        value = if (arrancadaGForce > 0f) String.format("%.2f G", arrancadaGForce) else "-- G",
                                        icon = Icons.Default.RocketLaunch,
                                        valueColor = MotoGoldSecondary
                                    )

                                    TripStatItem(
                                        title = "ACELERACIÓN",
                                        value = String.format("+%.1f m/s²", instantAccelMss),
                                        icon = Icons.Default.FastForward,
                                        valueColor = Color.White
                                    )

                                    TripStatItem(
                                        title = "RUMBO / ALT",
                                        value = "$cardinalDirection • ${altitudeMeters.toInt()}m",
                                        icon = Icons.Default.Explore,
                                        valueColor = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════
            // CONTROLES INFERIORES EN PANTALLA COMPLETA
            // SOLO LOS DATOS MOSTRADOS ABAJO (SIN BOTONES QUE TAPEN ARRIBA)
            // ═══════════════════════════════════════════════
            if (isFullscreen) {
                val distUnitTrip = if (isMph) (tripDistanceMeters / 1000.0) * 0.621371 else tripDistanceMeters / 1000.0
                val unitLabel = if (isMph) "MI" else "KM"
                val speedUnit = if (isMph) "MPH" else "KM/H"

                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, TxSteelSilver.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "TRIP: ${String.format("%.2f %s", distUnitTrip, unitLabel)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "•", color = Color.DarkGray)
                        Text(text = "MÁX: ${String.format("%.0f %s", displayTripMax, speedUnit)}", color = MotoOrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "•", color = Color.DarkGray)
                        Text(text = "G: ${String.format("%.2f G", gForceInstant)}", color = MotoGoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "•", color = Color.DarkGray)
                        Text(text = timeFormatted, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "•", color = Color.DarkGray)
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = MotoOrangePrimary.copy(alpha = 0.2f),
                            border = BorderStroke(0.8.dp, MotoOrangePrimary),
                            modifier = Modifier
                                .clickable { showReportDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Reporte", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación para resetear Récord Histórico
    if (showResetRecordDialog) {
        AlertDialog(
            onDismissRequest = { showResetRecordDialog = false },
            title = { Text("Reiniciar Récord de Velocidad", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("¿Deseas reiniciar el récord de velocidad máxima del carnet a 0 km/h?", color = Color(0xFFB0BEC5)) },
            confirmButton = {
                Button(
                    onClick = {
                        persistentRecordKmh = 0f
                        PreferenciasApp.topSpeedRecordKmh = 0f
                        isNewRecordAchieved = false
                        showResetRecordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Reiniciar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetRecordDialog = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = TxCarbonDark
        )
    }

    // Diálogo de Reporte Táctico de Telemetría
    if (showReportDialog) {
        ReporteTelemetriaDialog(
            member = currentMember,
            distanceMeters = tripDistanceMeters,
            elapsedSeconds = elapsedSeconds,
            maxSpeedKmh = tripMaxSpeedKmh,
            avgSpeedKmh = avgSpeed.toFloat(),
            arrancadaGForce = arrancadaGForce,
            maxGForce = maxGForceSession,
            maxAccelMss = maxAccelMssSession,
            maxAltitude = maxAltitudeMeters,
            isMph = isMph,
            onDismiss = { showReportDialog = false }
        )
    }
}

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * VISTA DIGITAL SPORT CON BARRAS LED & INDICADOR DE FUERZA G
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun DigitalSpeedometerView(
    speed: Float,
    isMph: Boolean,
    isHud: Boolean,
    gForce: Float,
    accelMss: Float,
    isFullscreen: Boolean = false
) {
    val maxSpeed = if (isMph) 120f else 200f
    val speedInt = speed.toInt().coerceAtLeast(0)
    val barProgress = (speed / maxSpeed).coerceIn(0f, 1f)

    val primaryColor = if (isHud) Color(0xFF00FF66) else MotoOrangePrimary
    val textColor = if (isHud) Color(0xFF00FF66) else Color.White

    val speedFontSize = if (isFullscreen) 115.sp else 86.sp
    val ledBarHeight = if (isFullscreen) 24.dp else 16.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Indicador de Arrancada / Fuerza G
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (gForce > 0.3f) primaryColor.copy(alpha = 0.2f) else TxCarbonDark,
            border = BorderStroke(1.dp, if (gForce > 0.3f) primaryColor else TxSteelSilver.copy(alpha = 0.2f)),
            modifier = Modifier.padding(bottom = if (isFullscreen) 16.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("G-FORCE: %.2f G (+%.1f m/s²)", gForce, accelMss),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor
                )
            }
        }

        // Display Digital Gigante
        Text(
            text = "$speedInt",
            fontSize = speedFontSize,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            letterSpacing = (-2).sp
        )

        Text(
            text = if (isMph) "MPH" else "KM/H",
            fontSize = if (isFullscreen) 22.sp else 18.sp,
            fontWeight = FontWeight.Black,
            color = primaryColor,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(if (isFullscreen) 24.dp else 14.dp))

        // Barra de LEDs Progresiva Racing
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isFullscreen) 0.92f else 0.85f)
                .height(ledBarHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(TxCarbonDark)
                .border(1.dp, TxSteelSilver.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            val numSegments = 24
            val activeSegments = (barProgress * numSegments).toInt()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (i in 0 until numSegments) {
                    val isActive = i < activeSegments
                    val segColor = when {
                        i >= numSegments * 0.75f -> TxFlameRed
                        i >= numSegments * 0.5f -> MotoGoldSecondary
                        else -> primaryColor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isActive) segColor else TxCharcoalSurface.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * VISTA ANALÓGICA CALIBRADA CON NÚMEROS (0 a 200 KM/H o 0 a 120 MPH)
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun AnalogSpeedometerView(
    speed: Float,
    isMph: Boolean,
    gForce: Float,
    isFullscreen: Boolean = false
) {
    val maxSpeed = if (isMph) 120f else 200f
    val speedClamped = speed.coerceIn(0f, maxSpeed)
    val gaugeSize = if (isFullscreen) 340.dp else 260.dp

    // Arco de 140° a 400° (260° de barrido total)
    val startAngle = 140f
    val sweepAngle = 260f
    val needleAngle = startAngle + (speedClamped / maxSpeed) * sweepAngle

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.size(gaugeSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 16.dp.toPx()

                // Fondo Esfera Tacómetro
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E2430), Color(0xFF0F1218), Color.Black),
                        center = center,
                        radius = radius + 12.dp.toPx()
                    ),
                    radius = radius + 12.dp.toPx(),
                    center = center
                )

                // Bisel Exterior Deportivo
                drawCircle(
                    color = MotoOrangePrimary.copy(alpha = 0.4f),
                    radius = radius + 12.dp.toPx(),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Arco de fondo completo
                drawArc(
                    color = Color(0xFF263238),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Arco de Zona Roja
                val redZoneRatio = if (isMph) 90f / 120f else 140f / 200f
                val redZoneStartAngle = startAngle + (redZoneRatio * sweepAngle)
                val redZoneSweep = sweepAngle * (1f - redZoneRatio)
                drawArc(
                    color = TxFlameRed.copy(alpha = 0.8f),
                    startAngle = redZoneStartAngle,
                    sweepAngle = redZoneSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Arco Activo Iluminado
                val activeSweep = (speedClamped / maxSpeed) * sweepAngle
                if (activeSweep > 0.5f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to MotoOrangePrimary,
                            0.7f to MotoGoldSecondary,
                            1.0f to TxFlameRed,
                            center = center
                        ),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Dibujar Marcas y Números
                val step = if (isMph) 10 else 20
                val minorStep = if (isMph) 2 else 5
                val totalMajorTicks = (maxSpeed / step).toInt()
                val totalMinorTicks = (maxSpeed / minorStep).toInt()

                // Ticks menores
                for (i in 0..totalMinorTicks) {
                    val tickSpeed = i * minorStep
                    val angleDeg = startAngle + (tickSpeed / maxSpeed) * sweepAngle
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val innerR = radius - 8.dp.toPx()
                    val outerR = radius

                    val startX = center.x + innerR * cos(angleRad).toFloat()
                    val startY = center.y + innerR * sin(angleRad).toFloat()
                    val endX = center.x + outerR * cos(angleRad).toFloat()
                    val endY = center.y + outerR * sin(angleRad).toFloat()

                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Ticks mayores y Números
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = if (isFullscreen) 32f else 26f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val redTextPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#E53935")
                    textSize = if (isFullscreen) 32f else 26f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                for (i in 0..totalMajorTicks) {
                    val tickSpeed = i * step
                    val angleDeg = startAngle + (tickSpeed / maxSpeed) * sweepAngle
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val innerR = radius - 14.dp.toPx()
                    val outerR = radius

                    val startX = center.x + innerR * cos(angleRad).toFloat()
                    val startY = center.y + innerR * sin(angleRad).toFloat()
                    val endX = center.x + outerR * cos(angleRad).toFloat()
                    val endY = center.y + outerR * sin(angleRad).toFloat()

                    val isRed = tickSpeed >= (if (isMph) 90 else 140)

                    drawLine(
                        color = if (isRed) TxFlameRed else Color.White,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Posición de texto
                    val textR = radius - 26.dp.toPx()
                    val textX = center.x + textR * cos(angleRad).toFloat()
                    val textY = center.y + textR * sin(angleRad).toFloat() + 9f

                    drawContext.canvas.nativeCanvas.drawText(
                        "$tickSpeed",
                        textX,
                        textY,
                        if (isRed) redTextPaint else textPaint
                    )
                }

                // Aguja de Fuego TX
                val needleRad = Math.toRadians(needleAngle.toDouble())
                val needleLength = radius - 16.dp.toPx()
                val needleEnd = Offset(
                    center.x + needleLength * cos(needleRad).toFloat(),
                    center.y + needleLength * sin(needleRad).toFloat()
                )

                // Resplandor de la aguja
                drawLine(
                    color = TxFlameRed.copy(alpha = 0.4f),
                    start = center,
                    end = needleEnd,
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Aguja principal
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(MotoOrangePrimary, TxFlameRed),
                        start = center,
                        end = needleEnd
                    ),
                    start = center,
                    end = needleEnd,
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Centro de la aguja (Tapa de perno)
                drawCircle(
                    color = Color(0xFF1E2430),
                    radius = 14.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = MotoOrangePrimary,
                    radius = 8.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }

            // Display Digital en el Centro Inferior de la Esfera
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = if (isFullscreen) 60.dp else 45.dp)
            ) {
                Text(
                    text = "${speed.toInt().coerceAtLeast(0)}",
                    fontSize = if (isFullscreen) 36.sp else 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = if (isMph) "MPH" else "KM/H",
                    fontSize = if (isFullscreen) 14.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MotoOrangePrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (gForce > 0.3f) MotoOrangePrimary.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, if (gForce > 0.3f) MotoOrangePrimary else TxSteelSilver.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = String.format("%.2f G", gForce),
                        fontSize = if (isFullscreen) 11.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gForce > 0.3f) MotoOrangePrimary else MotoGoldSecondary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * VISTA MIXTA RALLY CON BRÚJULA & TELEMETRÍA CENTRAL
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun MixtoSpeedometerView(
    speed: Float,
    isMph: Boolean,
    cardinal: String,
    altitude: Double,
    gForce: Float,
    isFullscreen: Boolean = false
) {
    val speedInt = speed.toInt().coerceAtLeast(0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Brújula Rally
        Surface(
            shape = CircleShape,
            color = TxCarbonDark,
            border = BorderStroke(2.dp, MotoGoldSecondary.copy(alpha = 0.5f)),
            modifier = Modifier.size(if (isFullscreen) 110.dp else 80.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = MotoGoldSecondary, modifier = Modifier.size(if (isFullscreen) 28.dp else 22.dp))
                Text(text = cardinal, fontSize = if (isFullscreen) 20.sp else 15.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(if (isFullscreen) 16.dp else 10.dp))

        // Velocidad Principal
        Text(
            text = "$speedInt",
            fontSize = if (isFullscreen) 110.sp else 80.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
        Text(
            text = if (isMph) "MPH" else "KM/H",
            fontSize = if (isFullscreen) 22.sp else 17.sp,
            fontWeight = FontWeight.Black,
            color = MotoOrangePrimary
        )

        Spacer(modifier = Modifier.height(if (isFullscreen) 16.dp else 10.dp))

        // Altitud y Fuerza G
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TxCarbonDark,
                border = BorderStroke(1.dp, TxSteelSilver.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terrain, contentDescription = null, tint = MotoGoldSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = String.format("%.0f M ALT", altitude), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TxCarbonDark,
                border = BorderStroke(1.dp, TxSteelSilver.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Compress, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = String.format("%.2f G", gForce), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TripStatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TxSteelSilver, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                color = TxSteelSilver,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
    }
}

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 📊 DIÁLOGO DE REPORTE TÁCTICO DE TELEMETRÍA & RENDIMIENTO TX
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun ReporteTelemetriaDialog(
    member: MemberProfile?,
    distanceMeters: Double,
    elapsedSeconds: Long,
    maxSpeedKmh: Float,
    avgSpeedKmh: Float,
    arrancadaGForce: Float,
    maxGForce: Float,
    maxAccelMss: Float,
    maxAltitude: Double,
    isMph: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val unitDist = if (isMph) "MI" else "KM"
    val unitSpeed = if (isMph) "MPH" else "KM/H"
    val distVal = if (isMph) (distanceMeters / 1000.0) * 0.621371 else distanceMeters / 1000.0
    val maxSpeedVal = if (isMph) maxSpeedKmh * 0.621371f else maxSpeedKmh
    val avgSpeedVal = if (isMph) avgSpeedKmh * 0.621371f else avgSpeedKmh

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    val fechaHora = remember {
        SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault()).format(Date())
    }

    val textoReporte = remember(member, distanceMeters, elapsedSeconds, maxSpeedKmh, avgSpeedKmh, arrancadaGForce) {
        buildString {
            appendLine("🏍️ *REPORTE DE TELEMETRÍA TÁCTICO - TEAM TX* 🏍️")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("👤 *Piloto:* ${member?.fullName?.ifBlank { "Piloto Oficial TX" } ?: "Piloto Oficial TX"}")
            if (!member?.nickname.isNullOrBlank()) {
                appendLine("🏷️ *Apodo:* \"${member?.nickname}\"")
            }
            appendLine("💳 *Carnet:* ${member?.memberNumber?.ifBlank { "TX-OFICIAL" } ?: "TX-OFICIAL"}")
            appendLine("📅 *Fecha:* $fechaHora")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🏁 *Distancia:* ${String.format(Locale.US, "%.2f %s", distVal, unitDist)}")
            appendLine("⏱️ *Tiempo en Marcha:* $timeFormatted")
            appendLine("⚡ *Velocidad Máxima:* ${String.format(Locale.US, "%.1f %s", maxSpeedVal, unitSpeed)}")
            appendLine("📈 *Velocidad Promedio:* ${String.format(Locale.US, "%.1f %s", avgSpeedVal, unitSpeed)}")
            appendLine("🚀 *Arrancada Táctica:* ${if (arrancadaGForce > 0f) String.format(Locale.US, "%.2f G", arrancadaGForce) else "-- G"}")
            appendLine("💥 *Fuerza G Máx:* ${String.format(Locale.US, "%.2f G", maxGForce)}")
            appendLine("⏩ *Aceleración Máx:* ${String.format(Locale.US, "+%.1f m/s²", maxAccelMss)}")
            appendLine("⛰️ *Altitud Máx:* ${maxAltitude.toInt()} m")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("_Generado por Tablero Táctico Team TX_")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REPORTE DE TELEMETRÍA",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ficha del Piloto
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10141E),
                    border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val fotoPiloto = member?.profilePhotoUri?.ifBlank { null } ?: PreferenciasApp.carnetGooglePhotoUrl?.ifBlank { null }
                        if (!fotoPiloto.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = fotoPiloto,
                                contentDescription = "Foto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MotoOrangePrimary, CircleShape),
                                loading = {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MotoOrangePrimary, strokeWidth = 2.dp)
                                    }
                                },
                                error = {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(24.dp))
                                }
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MotoOrangePrimary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MotoOrangePrimary),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member?.fullName?.ifBlank { "Piloto Oficial TX" } ?: "Piloto Oficial TX",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Carnet: ${member?.memberNumber?.ifBlank { "TX-OFICIAL" } ?: "TX-OFICIAL"} • $fechaHora",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = TxSteelSilver
                            )
                        }
                    }
                }

                // Cuadrícula de Métricas
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TxCarbonDark,
                    border = BorderStroke(1.dp, TxSteelSilver.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ReportItem(title = "DISTANCIA", value = String.format(Locale.US, "%.2f %s", distVal, unitDist), icon = Icons.Default.DirectionsBike, color = Color.White)
                            ReportItem(title = "TIEMPO", value = timeFormatted, icon = Icons.Default.Timer, color = Color.White)
                        }
                        HorizontalDivider(color = TxSteelSilver.copy(alpha = 0.15f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ReportItem(title = "VEL. MÁXIMA", value = String.format(Locale.US, "%.1f %s", maxSpeedVal, unitSpeed), icon = Icons.Default.Speed, color = MotoOrangePrimary)
                            ReportItem(title = "VEL. PROMEDIO", value = String.format(Locale.US, "%.1f %s", avgSpeedVal, unitSpeed), icon = Icons.Default.TrendingUp, color = Color.White)
                        }
                        HorizontalDivider(color = TxSteelSilver.copy(alpha = 0.15f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ReportItem(title = "G ARRANCADA", value = if (arrancadaGForce > 0f) String.format(Locale.US, "%.2f G", arrancadaGForce) else "-- G", icon = Icons.Default.RocketLaunch, color = MotoGoldSecondary)
                            ReportItem(title = "G MÁXIMA", value = String.format(Locale.US, "%.2f G", maxGForce), icon = Icons.Default.Compress, color = MotoGoldSecondary)
                        }
                        HorizontalDivider(color = TxSteelSilver.copy(alpha = 0.15f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ReportItem(title = "ACEL. MÁXIMA", value = String.format(Locale.US, "+%.1f m/s²", maxAccelMss), icon = Icons.Default.FastForward, color = Color.White)
                            ReportItem(title = "ALTITUD MÁX", value = "${maxAltitude.toInt()} m", icon = Icons.Default.Terrain, color = Color.White)
                        }
                    }
                }

                // Botones de Acción: Compartir en WhatsApp y Copiar
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, textoReporte)
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (_: Exception) {
                            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, textoReporte)
                            }, "Compartir Reporte Telemetría TX")
                            context.startActivity(chooser)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir por WhatsApp", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        GestorPortapapeles.copiarTexto(context, "Reporte Telemetría TX", textoReporte)
                        Toast.makeText(context, "📋 Reporte copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, MotoOrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar Reporte Texto", color = MotoOrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF141824)
    )
}

@Composable
private fun ReportItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TxSteelSilver, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TxSteelSilver)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
    }
}
