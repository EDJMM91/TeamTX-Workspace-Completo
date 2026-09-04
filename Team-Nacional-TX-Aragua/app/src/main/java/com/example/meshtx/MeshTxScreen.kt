package com.example.meshtx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.MemberProfile

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PANTALLA TÁCTICA DEL INTERCOMUNICADOR MESH TX (TEMA CLARO PURO DE ALTO CONTRASTE)
 * ═══════════════════════════════════════════════════════════════════════════
 * Diseñada específicamente para uso humano motero en carretera:
 * - Tema Claro: Fondo #F8FAFC, tarjetas blancas con sombra y borde sutil.
 * - Tipografía de alto contraste legible bajo la luz solar intensa (#0F172A).
 * - Botón central PTT grande (144 dp) accesible con guantes de moto.
 * - Conmutador rápido PTT/VOX a un toque sin menús escondidos.
 * - Selector táctil de canales de radiofrecuencia virtual.
 * - Radar de proximidad con barras de señal y distancia estimada en metros.
 * - Indicador de casco Bluetooth conectado (Sena / Cardo / Ejeas vía SCO).
 * - Cero textos superpuestos o elementos tapados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshTxScreen(
    currentMember: MemberProfile?,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Observables del Gestor Central
    val estadoConexion by GestorMeshTx.estadoConexion.collectAsState()
    val canalActual by GestorMeshTx.canalActual.collectAsState()
    val nodosEnRed by GestorMeshTx.nodosEnRed.collectAsState()
    val estaTransmitiendoPtt by GestorMeshTx.estaTransmitiendoPtt.collectAsState()
    val pilotoHablando by GestorMeshTx.pilotoHablandoAhora.collectAsState()
    val ajustes by GestorMeshTx.ajustes.collectAsState()
    val saltosRelay by GestorMeshTx.totalSaltosRelay.collectAsState()
    val cascoBluetoothConectado by GestorMeshTx.cascoBluetoothConectado.collectAsState()
    val modoAltavozActivo by GestorMeshTx.modoAltavozActivo.collectAsState()

    var mostrarDialogoSos by remember { mutableStateOf(false) }
    var mostrarAjustesAudio by remember { mutableStateOf(false) }
    var mostrarGuiaInteractiva by remember { mutableStateOf(false) }

    val contextoLocal = LocalContext.current

    // Permisos de sistema requeridos para operar el enmallado táctico
    val permisosRequeridos = remember {
        val lista = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lista.add(Manifest.permission.BLUETOOTH_SCAN)
            lista.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            lista.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lista.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        lista.toTypedArray()
    }

    val launcherPermisosMalla = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val audioConcedido = resultados[Manifest.permission.RECORD_AUDIO] == true
        if (audioConcedido) {
            GestorMeshTx.iniciarMallaTactico()
        }
    }

    fun conmutarMallaTactico() {
        if (estadoConexion == MeshEstadoConexion.DESCONECTADO) {
            val faltanPermisos = permisosRequeridos.any { permiso ->
                ContextCompat.checkSelfPermission(contextoLocal, permiso) != PackageManager.PERMISSION_GRANTED
            }
            if (faltanPermisos) {
                launcherPermisosMalla.launch(permisosRequeridos)
            } else {
                GestorMeshTx.iniciarMallaTactico()
            }
        } else {
            GestorMeshTx.detenerMallaTactico()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mesh TX",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFEDD5)
                            ) {
                                Text(
                                    text = "OFFLINE 3.0",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "INTERCOMUNICADOR TÁCTICO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF6B00),
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    // Botón de Guía Interactiva Táctica (Manual del Piloto)
                    IconButton(
                        onClick = { mostrarGuiaInteractiva = true }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFEDD5),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = "Guía del Piloto",
                                    tint = Color(0xFFC2410C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Botón de encendido / apagado de antenas
                    IconButton(
                        onClick = { conmutarMallaTactico() }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (estadoConexion == MeshEstadoConexion.DESCONECTADO) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (estadoConexion == MeshEstadoConexion.DESCONECTADO) Icons.Default.PlayArrow else Icons.Default.Stop,
                                    contentDescription = "Iniciar/Detener",
                                    tint = if (estadoConexion == MeshEstadoConexion.DESCONECTADO) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─────────────────────────────────────────────────────────────────
            // 1. TARJETA PRINCIPAL: ESTADO DE MALLA, SEÑAL Y CASCO BLUETOOTH
            // ─────────────────────────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Indicador circular pulsante de estado
                                val alphaAnim by rememberInfiniteTransition(label = "PulseAlpha").animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(900, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "PulseAnim"
                                )

                                val colorPunto = when (estadoConexion) {
                                    MeshEstadoConexion.DESCONECTADO -> Color(0xFF94A3B8)
                                    MeshEstadoConexion.ESCANEANDO -> Color(0xFFF59E0B)
                                    MeshEstadoConexion.ENLACE_DIRECTO -> Color(0xFF16A34A)
                                    MeshEstadoConexion.ENMALLADO_RELAY -> Color(0xFF0284C7)
                                    MeshEstadoConexion.CONECTANDO -> Color(0xFF3B82F6)
                                    MeshEstadoConexion.ERROR -> Color(0xFFDC2626)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .scale(if (estadoConexion != MeshEstadoConexion.DESCONECTADO) alphaAnim else 1f)
                                        .clip(CircleShape)
                                        .background(colorPunto)
                                )

                                Column {
                                    Text(
                                        text = estadoConexion.descripcion,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (estadoConexion == MeshEstadoConexion.DESCONECTADO)
                                            "Presiona el botón de encendido arriba para enlazar"
                                        else
                                            "${nodosEnRed.size} pilotos detectados en radio local",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Botones rápidos de Altavoz, Ajustes y SOS
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Botón rápido de Altavoz / Manos Libres
                                Surface(
                                    shape = CircleShape,
                                    color = if (modoAltavozActivo) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = { GestorMeshTx.alternarModoAltavoz(!modoAltavozActivo) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = if (modoAltavozActivo) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                            contentDescription = "Modo Altavoz",
                                            tint = if (modoAltavozActivo) Color(0xFF16A34A) else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = { mostrarAjustesAudio = true },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = "Ajustes",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = { mostrarDialogoSos = true },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = "Alerta SOS",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Badge informativo de Hardware Casco Bluetooth
                        if (cascoBluetoothConectado) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Intercomunicador de Casco Activo (Bluetooth SCO)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3730A3)
                                    )
                                }
                            }
                        }

                        // Control Horizontal Compacto de Modo Altavoz (Loudspeaker)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (modoAltavozActivo) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (modoAltavozActivo) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (modoAltavozActivo) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        tint = if (modoAltavozActivo) Color(0xFF16A34A) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (modoAltavozActivo) "Modo Altavoz Activo" else "Modo Auricular Privado",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (modoAltavozActivo) Color(0xFF15803D) else Color(0xFF334155)
                                        )
                                        Text(
                                            text = if (modoAltavozActivo) "Sonando por altavoz exterior" else "Sonando pegado a la oreja",
                                            fontSize = 10.sp,
                                            color = if (modoAltavozActivo) Color(0xFF16A34A) else Color(0xFF64748B)
                                        )
                                    }
                                }
                                Switch(
                                    checked = modoAltavozActivo,
                                    onCheckedChange = { GestorMeshTx.alternarModoAltavoz(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF16A34A)
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 2. SELECTOR TÁCTIL DE CANALES VIRTUALES
            // ─────────────────────────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Frecuencia Táctica Virtual",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF475569),
                        letterSpacing = 0.3.sp
                    )

                    val scrollCanales = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollCanales),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanalTactico.values().forEach { canal ->
                            val seleccionado = canal == canalActual
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (seleccionado) Color(0xFFFFEDD5) else Color.White,
                                border = BorderStroke(
                                    width = if (seleccionado) 2.dp else 1.dp,
                                    color = if (seleccionado) Color(0xFFFF6B00) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (seleccionado) 2.dp else 1.dp,
                                modifier = Modifier
                                    .width(115.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { GestorMeshTx.cambiarCanal(canal) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "CANAL ${canal.idCanal}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (seleccionado) Color(0xFFC2410C) else Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (canal) {
                                            CanalTactico.GENERAL_TX -> "General"
                                            CanalTactico.CARAVANA_CONVOY -> "Caravana"
                                            CanalTactico.EMERGENCIA_SOS -> "SOS Vial"
                                            CanalTactico.DIRECTIVA -> "Directiva"
                                            CanalTactico.PERSONALIZADO -> "Privado"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (seleccionado) Color(0xFF0F172A) else Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 3. MONITOR EN VIVO: TRANSMISIÓN DE VOZ (QUIÉN HABLA AHORA)
            // ─────────────────────────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = pilotoHablando != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EN EL CANAL AHORA:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0369A1)
                                )
                                Text(
                                    text = pilotoHablando ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            if (saltosRelay > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDBEAFE)
                                ) {
                                    Text(
                                        text = "$saltosRelay saltos",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 4. BOTÓN CENTRAL TÁCTICO PTT (GRANDE, CENTRADO Y ERGONÓMICO)
            // ─────────────────────────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val escalaTransmision by animateFloatAsState(
                            targetValue = if (estaTransmitiendoPtt) 1.08f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "BotonPttEscala"
                        )

                        // Botón PTT circular ergonómico de 144 dp
                        Surface(
                            shape = CircleShape,
                            color = if (estaTransmitiendoPtt) Color(0xFFDC2626) else Color(0xFFFF6B00),
                            shadowElevation = if (estaTransmitiendoPtt) 10.dp else 4.dp,
                            modifier = Modifier
                                .size(144.dp)
                                .scale(escalaTransmision)
                                .clip(CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            GestorMeshTx.setTransmitiendoPtt(true)
                                            tryAwaitRelease()
                                            GestorMeshTx.setTransmitiendoPtt(false)
                                        }
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (estaTransmitiendoPtt) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "Botón PTT",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (estaTransmitiendoPtt) "AL AIRE" else "PULSAR PTT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (estaTransmitiendoPtt) "Transmitiendo" else "Mantener presionado",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selector rápido de Modo: PTT vs VOX
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Chip PTT
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (ajustes.modoPtt) Color.White else Color.Transparent,
                                shadowElevation = if (ajustes.modoPtt) 1.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { GestorMeshTx.alternarModoPtt(true) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = if (ajustes.modoPtt) Color(0xFFFF6B00) else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Pulsar (PTT)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ajustes.modoPtt) Color(0xFF0F172A) else Color(0xFF64748B)
                                    )
                                }
                            }

                            // Chip VOX
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (!ajustes.modoPtt) Color.White else Color.Transparent,
                                shadowElevation = if (!ajustes.modoPtt) 1.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { GestorMeshTx.alternarModoPtt(false) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = if (!ajustes.modoPtt) Color(0xFF16A34A) else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Voz (VOX)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!ajustes.modoPtt) Color(0xFF0F172A) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 5. RADAR TÁCTICO: LISTA DE COMPAÑEROS EN RANGO DE MALLA
            // ─────────────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compañeros en Radar Mesh (${nodosEnRed.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        if (nodosEnRed.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "Enmallado Activo ✓",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Selector Rápido: Modo Piloto vs Modo Hardware
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { GestorMeshTx.alternarVisualizacionPerfil(true) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (ajustes.mostrarPerfilSincronizado) Color(0xFFFF6B00) else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        tint = if (ajustes.mostrarPerfilSincronizado) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ver Pilotos TX",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ajustes.mostrarPerfilSincronizado) Color.White else Color(0xFF64748B)
                                    )
                                }
                            }
                            Surface(
                                onClick = { GestorMeshTx.alternarVisualizacionPerfil(false) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!ajustes.mostrarPerfilSincronizado) Color(0xFF0F172A) else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = if (!ajustes.mostrarPerfilSincronizado) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ver Dispositivos",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!ajustes.mostrarPerfilSincronizado) Color.White else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (nodosEnRed.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Podcasts,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (estadoConexion == MeshEstadoConexion.DESCONECTADO)
                                    "Malla Táctica Apagada"
                                else
                                    "Escaneando Malla Cercana...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (estadoConexion == MeshEstadoConexion.DESCONECTADO)
                                    "Toca el botón verde de encendido arriba a la derecha para iniciar la transmisión y escucha."
                                else
                                    "Esperando balizas BLE y Wi-Fi Direct de otros pilotos en el convoy.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(nodosEnRed, key = { it.idMiembro }) { nodo ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (ajustes.mostrarPerfilSincronizado) Color(0xFFFFF7ED) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (ajustes.mostrarPerfilSincronizado) Color(0xFFFDBA74) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (ajustes.mostrarPerfilSincronizado) Icons.Default.TwoWheeler else Icons.Default.Smartphone,
                                            contentDescription = null,
                                            tint = if (ajustes.mostrarPerfilSincronizado) Color(0xFFFF6B00) else Color(0xFF475569),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (ajustes.mostrarPerfilSincronizado)
                                            if (nodo.aliasPiloto.isNotBlank() && nodo.aliasPiloto != nodo.modeloTelefonoHardware) nodo.aliasPiloto else "Piloto TX #${nodo.idMiembro and 0x3FFL}"
                                        else
                                            nodo.modeloTelefonoHardware.ifBlank { "Dispositivo BLE/WiFi" },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (ajustes.mostrarPerfilSincronizado)
                                            "${nodo.nombreMoto.ifBlank { "TX 200" }} • ${nodo.fichaMiembro.ifBlank { "Ficha #${nodo.idMiembro and 0x3FFL}" }} • ~${nodo.distanciaAproximadaMetros.toInt()}m"
                                        else
                                            "Hardware: ${nodo.direccionNodo.ifBlank { "Radio Local" }} • ${nodo.intensidadSenalDbm} dBm • ~${nodo.distanciaAproximadaMetros.toInt()}m",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Badge de calidad de señal
                            val calidadColor = when {
                                nodo.intensidadSenalDbm > -65 -> Color(0xFF16A34A)
                                nodo.intensidadSenalDbm > -80 -> Color(0xFFF59E0B)
                                else -> Color(0xFF64748B)
                            }
                            val calidadFondo = when {
                                nodo.intensidadSenalDbm > -65 -> Color(0xFFDCFCE7)
                                nodo.intensidadSenalDbm > -80 -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF1F5F9)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = calidadFondo
                            ) {
                                Text(
                                    text = if (nodo.intensidadSenalDbm > -65) "Fuerte" else if (nodo.intensidadSenalDbm > -80) "Media" else "Lejana",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = calidadColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIÁLOGO DE AJUSTES TÁCTICOS DE AUDIO
    // ─────────────────────────────────────────────────────────────────────────
    if (mostrarAjustesAudio) {
        AlertDialog(
            onDismissRequest = { mostrarAjustesAudio = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Ajustes del Intercomunicador",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Switch Modo PTT vs VOX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Modo Push-To-Talk (PTT)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Pulsar botón para hablar en vez de detección vocal",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.modoPtt,
                            onCheckedChange = { GestorMeshTx.alternarModoPtt(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B00)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Filtro de Ruido de Viento
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Filtro de Viento para Casco",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Filtro pasa-altos digital (IIR 300 Hz) para carretera",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.cancelacionRuidoViento,
                            onCheckedChange = { GestorMeshTx.alternarFiltroViento(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B00)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Visualización de Nodos en Convoy (Amigable vs Técnico)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ver Perfil de Piloto Sincronizado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.mostrarPerfilSincronizado)
                                    "Mostrando alias motero, moto TX y ficha del compañero"
                                else
                                    "Mostrando modelo técnico de teléfono y dirección de radio",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.mostrarPerfilSincronizado,
                            onCheckedChange = { GestorMeshTx.alternarVisualizacionPerfil(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B00)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Modo Altavoz Potente (Loudspeaker / Manos Libres)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🔊 Modo Altavoz Potente (Loudspeaker)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.modoAltavozActivo)
                                    "Activo: El audio sale por el altavoz exterior sin necesidad de auriculares"
                                else
                                    "Inactivo: Enrutado al auricular privado del teléfono",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.modoAltavozActivo,
                            onCheckedChange = { GestorMeshTx.alternarModoAltavoz(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarAjustesAudio = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Listo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIÁLOGO DE CONFIRMACIÓN ALERTA SOS PRIORITARIA
    // ─────────────────────────────────────────────────────────────────────────
    if (mostrarDialogoSos) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSos = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "🚨 Transmitir Alerta SOS",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            },
            text = {
                Text(
                    text = "¿Deseas transmitir un llamado prioritario de auxilio vial a toda la caravana? Interrumpirá el canal actual para alertar a capitanes, punteros y barredoras.",
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nombrePiloto = currentMember?.nickname?.ifBlank { currentMember.fullName } ?: "Compañero"
                        GestorMeshTx.emitirAlertaSosMalla("¡ALERTA AUXILIO VIAL! Piloto: $nombrePiloto requiere apoyo urgente.")
                        mostrarDialogoSos = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Transmitir Auxilio", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSos = false }) {
                    Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUÍA DE USO INTERACTIVA (MANUAL DEL PILOTO MESH TX)
    // ─────────────────────────────────────────────────────────────────────────
    if (mostrarGuiaInteractiva) {
        DialogoGuiaInteractivaMeshTx(
            onDismiss = { mostrarGuiaInteractiva = false }
        )
    }
}

/**
 * Diálogo interactivo paso a paso para aprender a usar el intercomunicador táctico en carretera.
 */
@Composable
fun DialogoGuiaInteractivaMeshTx(
    onDismiss: () -> Unit
) {
    var pasoActual by remember { mutableStateOf(1) }
    val totalPasos = 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFEDD5),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFC2410C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Manual del Piloto TX",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Paso $pasoActual de $totalPasos • Intercomunicador Mesh 3.0",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B00)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Indicador de progreso tipo puntos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..totalPasos) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (i == pasoActual) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == pasoActual) Color(0xFFFF6B00)
                                    else if (i < pasoActual) Color(0xFF16A34A)
                                    else Color(0xFFCBD5E1)
                                )
                        )
                    }
                }

                // Contenido dinámico del paso
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (pasoActual) {
                            1 -> {
                                Text(
                                    text = "1. Encendido y Permisos Tácticos",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Toca el botón verde ▶ arriba a la derecha. El teléfono encenderá las antenas Bluetooth LE y Wi-Fi Direct.\n\n✓ 100% Offline: No consume saldo ni megas.\n✓ Concede los permisos de micrófono y dispositivos cercanos al solicitarlos.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                            2 -> {
                                Text(
                                    text = "2. Canales de Frecuencia Virtual",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Toca los botones de canales para cambiar la sala:\n\n• Canal 1 (General): Para hablar con toda la rodada.\n• Canal 2 (Caravana): Exclusivo para Capitán, Punteros y Barredoras.\n• Canal 3 (SOS Vial): Prioridad máxima ante fallas o auxilio.\n• Canal 4 (Directiva): Sala de líderes.\n• Canal 5 (Privado): Enlace 1 a 1.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                            3 -> {
                                Text(
                                    text = "3. Cómo Hablar: PTT vs Manos Libres",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Tienes 2 modos de transmisión:\n\n• PTT (Pulsar para hablar): Mantén presionado el botón central naranja de 144 dp. Al ponerse rojo 'AL AIRE', habla fuerte y suelta al terminar.\n• VOX (Manos Libres): Toca el selector 'Voz (VOX)' para hablar sin soltar el manubrio ni el acelerador de la moto.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                            4 -> {
                                Text(
                                    text = "4. Cascos con Intercomunicador Bluetooth",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Si usas intercomunicador en el casco (Sena, Cardo, Ejeas, etc.):\n\n• Enciéndelo y conéctalo al teléfono por Bluetooth.\n• La app activa automáticamente Bluetooth SCO para canalizar el micrófono y parlantes del casco.\n• El Filtro de Viento digital a 300 Hz anula el zumbido de carretera a altas velocidades.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                            5 -> {
                                Text(
                                    text = "5. Enrutamiento Mesh 3.0 (Multi-hop)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "¿Un compañero va muy lejos en una curva cerrada?\n\n• Tu voz no se pierde: El teléfono del compañero intermedio retransmite el paquete en milisegundos (Relay).\n• La señal rebota de moto en moto en cadena (A -> B -> C) para cubrir todo el convoy aunque no haya línea de vista directa.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                            6 -> {
                                Text(
                                    text = "6. Puente de Rescate SOS Híbrido",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Si un piloto se accidenta en la montaña sin cobertura:\n\n• Pulsa el botón de Alerta SOS ⚠️.\n• La alerta viaja por la malla offline.\n• En cuanto CUALQUIER compañero de la rodada alcance una curva con señal 3G/4G, su teléfono sube el auxilio a Firebase automáticamente con las coordenadas GPS para alertar a la central y rescatistas.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pasoActual > 1) {
                    OutlinedButton(
                        onClick = { pasoActual-- },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Anterior", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (pasoActual < totalPasos) {
                            pasoActual++
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (pasoActual < totalPasos) "Siguiente" else "¡Entendido!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

