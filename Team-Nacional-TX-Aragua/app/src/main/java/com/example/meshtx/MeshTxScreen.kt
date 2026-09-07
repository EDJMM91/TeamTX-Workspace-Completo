package com.example.meshtx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val idPilotoHablando by GestorMeshTx.idPilotoHablandoAhora.collectAsState()
    val ajustes by GestorMeshTx.ajustes.collectAsState()
    val saltosRelay by GestorMeshTx.totalSaltosRelay.collectAsState()
    val cascoBluetoothConectado by GestorMeshTx.cascoBluetoothConectado.collectAsState()
    val modoAltavozActivo by GestorMeshTx.modoAltavozActivo.collectAsState()
    val salaPrivadaActiva by GestorMeshTx.salaPrivadaActiva.collectAsState()
    val modoAlcabalaVivo by GestorMeshTx.modoAlcabalaEnVivoActivo.collectAsState()

    val estadoEnlaceCopiloto by GestorMeshTx.estadoEnlaceCopiloto.collectAsState()
    val rolEnMoto by GestorMeshTx.rolEnMoto.collectAsState()
    val nombreCopilotoConectado by GestorMeshTx.nombreCopilotoConectado.collectAsState()
    val estaHablandoCopiloto by GestorMeshTx.estaHablandoCopiloto.collectAsState()
    val modoEnlaceIntramoto by GestorMeshTx.modoEnlaceIntramoto.collectAsState()

    var mostrarDialogoSos by remember { mutableStateOf(false) }
    var mostrarAjustesAudio by remember { mutableStateOf(false) }
    var mostrarGuiaInteractiva by remember { mutableStateOf(false) }
    var mostrarDialogoSalaPrivada by remember { mutableStateOf(false) }
    var mostrarDialogoCopiloto by remember { mutableStateOf(false) }

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

    // El inicio de la malla ahora espera la pulsación explícita del usuario en el botón superior

    LaunchedEffect(currentMember) {
        if (currentMember != null) {
            val foto = currentMember.profilePhotoUri
                ?: contextoLocal.getSharedPreferences("radar_prefs", android.content.Context.MODE_PRIVATE).getString("radar_avatar", "")
                ?: ""
            val alias = currentMember.nickname.ifBlank { currentMember.fullName.ifBlank { "Piloto TX" } }
            val moto = currentMember.bikeModel.ifBlank { "Keeway TX 200" }
            val ficha = currentMember.memberNumber
            GestorMeshTx.actualizarPerfilLocal(alias, foto, moto, ficha)
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
                    // Botón de Ajustes Previos del Intercomunicador (disponible sin conectar)
                    IconButton(
                        onClick = { mostrarAjustesAudio = true }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Ajustes del Intercomunicador",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

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
            // 1.5. TARJETA INTERCOM INTRAMOTO: ENLACE PILOTO & COPILOTO (MICRO-MALLA PRIVADA)
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Encabezado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (estadoEnlaceCopiloto) {
                                        EstadoEnlaceCopiloto.CONECTADO -> Color(0xFFDCFCE7)
                                        EstadoEnlaceCopiloto.ESPERANDO_COPILOTO, EstadoEnlaceCopiloto.CONECTANDO -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFF1F5F9)
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bluetooth,
                                            contentDescription = "Bluetooth Intramoto",
                                            tint = when (estadoEnlaceCopiloto) {
                                                EstadoEnlaceCopiloto.CONECTADO -> Color(0xFF16A34A)
                                                EstadoEnlaceCopiloto.ESPERANDO_COPILOTO, EstadoEnlaceCopiloto.CONECTANDO -> Color(0xFFD97706)
                                                else -> Color(0xFF64748B)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Intercom Intramoto (Piloto-Copiloto)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "MICRO-MALLA PRIVADA • NODO PUENTE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2563EB),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Badge de modo activo
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (modoEnlaceIntramoto) {
                                    ModoEnlaceIntramoto.SOLO_BLUETOOTH -> Color(0xFFDCFCE7)
                                    ModoEnlaceIntramoto.SOLO_WIFI -> Color(0xFFEFF6FF)
                                    ModoEnlaceIntramoto.HIBRIDO_TRIMODAL -> Color(0xFFFFEDD5)
                                }
                            ) {
                                Text(
                                    text = when (modoEnlaceIntramoto) {
                                        ModoEnlaceIntramoto.SOLO_BLUETOOTH -> "Solo BT 🔋"
                                        ModoEnlaceIntramoto.SOLO_WIFI -> "Wi-Fi 📡"
                                        ModoEnlaceIntramoto.HIBRIDO_TRIMODAL -> "Trimodal ⚡"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when (modoEnlaceIntramoto) {
                                        ModoEnlaceIntramoto.SOLO_BLUETOOTH -> Color(0xFF15803D)
                                        ModoEnlaceIntramoto.SOLO_WIFI -> Color(0xFF1D4ED8)
                                        ModoEnlaceIntramoto.HIBRIDO_TRIMODAL -> Color(0xFFC2410C)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // ⚠️ ADVERTENCIA DE ALCANCE BLUETOOTH OBLIGATORIA
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "⚠️ Cobertura Bluetooth: Alcance físico máximo de 10 a 15 metros. Diseñado exclusivamente para Piloto y Copiloto montados en la misma moto TX 200.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        // Estado de conexión en tiempo real y quién habla
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (estadoEnlaceCopiloto) {
                                                EstadoEnlaceCopiloto.CONECTADO -> Color(0xFF16A34A)
                                                EstadoEnlaceCopiloto.ESPERANDO_COPILOTO, EstadoEnlaceCopiloto.CONECTANDO -> Color(0xFFF59E0B)
                                                else -> Color(0xFF94A3B8)
                                            }
                                        )
                                )
                                Text(
                                    text = when (estadoEnlaceCopiloto) {
                                        EstadoEnlaceCopiloto.CONECTADO -> "Enlazado con: ${nombreCopilotoConectado ?: ajustes.nombreDispositivoCopiloto.ifBlank { "Compañero" }}"
                                        EstadoEnlaceCopiloto.ESPERANDO_COPILOTO -> "Esperando conexión de Copiloto..."
                                        EstadoEnlaceCopiloto.CONECTANDO -> "Conectando por Bluetooth..."
                                        EstadoEnlaceCopiloto.DESCONECTADO -> "Enlace intramoto desconectado"
                                        EstadoEnlaceCopiloto.ERROR -> "Error de enlace Bluetooth"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            if (estaHablandoCopiloto) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFDCFCE7),
                                    border = BorderStroke(1.dp, Color(0xFF16A34A))
                                ) {
                                    Text(
                                        text = "🎙️ HABLANDO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (estadoEnlaceCopiloto == EstadoEnlaceCopiloto.CONECTADO) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "< 40ms Latencia",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        // Selector Rápido de Rol en la Moto
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Tu Rol en la Moto:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    RolEnMoto.PILOTO_GATEWAY to "🏍️ Piloto (Gateway)",
                                    RolEnMoto.COPILOTO_ENLACE to "🎒 Copiloto",
                                    RolEnMoto.SOLO_PILOTO_INDIVIDUAL to "👤 Individual"
                                ).forEach { (rol, etiqueta) ->
                                    val seleccionado = rolEnMoto == rol
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (seleccionado) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (seleccionado) Color(0xFF1D4ED8) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { GestorMeshTx.alternarRolEnMoto(rol) }
                                    ) {
                                        Text(
                                            text = etiqueta,
                                            fontSize = 10.sp,
                                            fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Bold,
                                            color = if (seleccionado) Color.White else Color(0xFF334155),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Selector Rápido de Modo de Enlace Intramoto (Trimodal)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Modo de Red Intramoto:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    ModoEnlaceIntramoto.SOLO_BLUETOOTH to "Solo BT 🔋",
                                    ModoEnlaceIntramoto.SOLO_WIFI to "Solo Wi-Fi 📡",
                                    ModoEnlaceIntramoto.HIBRIDO_TRIMODAL to "Trimodal ⚡"
                                ).forEach { (modo, etiqueta) ->
                                    val seleccionado = modoEnlaceIntramoto == modo
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (seleccionado) Color(0xFFFF6B00) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (seleccionado) Color(0xFFEA580C) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { GestorMeshTx.alternarModoEnlaceIntramoto(modo) }
                                    ) {
                                        Text(
                                            text = etiqueta,
                                            fontSize = 10.sp,
                                            fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Bold,
                                            color = if (seleccionado) Color.White else Color(0xFF334155),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Acciones según el rol
                        if (rolEnMoto == RolEnMoto.PILOTO_GATEWAY) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Retransmitir Copiloto a Caravana",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (ajustes.retransmitirCopilotoACaravana)
                                                "Puente Activo: Tu teléfono reenvía la voz del copiloto a la caravana por Wi-Fi y 4G"
                                            else
                                                "Privado: Solo ustedes dos se escuchan en la moto",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Switch(
                                        checked = ajustes.retransmitirCopilotoACaravana,
                                        onCheckedChange = { GestorMeshTx.alternarRetransmitirCopilotoCaravana(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF2563EB)
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }
                        } else if (rolEnMoto == RolEnMoto.COPILOTO_ENLACE) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Piloto Vinculado:",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = if (ajustes.nombreDispositivoCopiloto.isNotBlank())
                                                ajustes.nombreDispositivoCopiloto
                                            else
                                                "Ningún teléfono seleccionado",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { mostrarDialogoCopiloto = true },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Vincular Piloto", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                    }
                                }
                            }
                        }

                        // Botones de conectar / desconectar / vincular
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (estadoEnlaceCopiloto != EstadoEnlaceCopiloto.DESCONECTADO) {
                                OutlinedButton(
                                    onClick = { GestorMeshTx.desconectarEnlaceCopiloto() },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Text("Desconectar Enlace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (rolEnMoto == RolEnMoto.COPILOTO_ENLACE) {
                                            if (ajustes.macDispositivoCopiloto.isNotBlank()) {
                                                GestorMeshTx.iniciarModoCopiloto(ajustes.macDispositivoCopiloto)
                                            } else {
                                                mostrarDialogoCopiloto = true
                                            }
                                        } else {
                                            GestorMeshTx.iniciarModoPilotoGateway()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text(
                                        text = if (rolEnMoto == RolEnMoto.COPILOTO_ENLACE) "Conectar con Piloto" else "Activar Modo Gateway",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            IconButton(
                                onClick = { mostrarDialogoCopiloto = true }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bluetooth,
                                            contentDescription = "Dispositivos Bluetooth",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 2. SELECTOR TÁCTIL DE CANALES VIRTUALES Y SALA PRIVADA
            // ─────────────────────────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Banner si hay transmisión de Alcabala en vivo activa
                    if (modoAlcabalaVivo) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(2.dp, Color(0xFFDC2626)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Podcasts,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "🚨 ALCABALA POLICIAL EN VIVO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = "Micrófono transmitiendo continuamente por Mesh y Datos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7F1D1D)
                                        )
                                    }
                                }
                                Button(
                                    onClick = { GestorMeshTx.desactivarModoAlcabalaSos() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Detener", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // Banner si hay sala privada activa
                    if (salaPrivadaActiva != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "SALA PRIVADA AISLADA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1D4ED8)
                                        )
                                        Text(
                                            text = salaPrivadaActiva ?: "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { GestorMeshTx.salirASalaGeneral() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Text(
                                        text = "Salir a General",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frecuencia Táctica Virtual",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF475569),
                            letterSpacing = 0.3.sp
                        )

                        // Botón de acceso rápido a Crear/Unirse a Sala Privada
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (salaPrivadaActiva != null) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { mostrarDialogoSalaPrivada = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (salaPrivadaActiva != null) Color(0xFF1D4ED8) else Color(0xFF475569),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (salaPrivadaActiva != null) "Sala: $salaPrivadaActiva" else "+ Sala Privada",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (salaPrivadaActiva != null) Color(0xFF1D4ED8) else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    val scrollCanales = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollCanales),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Canal 1: General
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (canalActual == CanalTactico.GENERAL_TX && salaPrivadaActiva == null) Color(0xFFFFEDD5) else Color.White,
                            border = BorderStroke(
                                width = if (canalActual == CanalTactico.GENERAL_TX && salaPrivadaActiva == null) 2.dp else 1.dp,
                                color = if (canalActual == CanalTactico.GENERAL_TX && salaPrivadaActiva == null) Color(0xFFFF6B00) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (canalActual == CanalTactico.GENERAL_TX && salaPrivadaActiva == null) 2.dp else 1.dp,
                            modifier = Modifier
                                .width(115.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    GestorMeshTx.salirASalaGeneral()
                                    GestorMeshTx.cambiarCanal(CanalTactico.GENERAL_TX)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CANAL 1",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (canalActual == CanalTactico.GENERAL_TX && salaPrivadaActiva == null) Color(0xFFC2410C) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "General",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canalActual == CanalTactico.GENERAL_TX && salaPrivadaActiva == null) Color(0xFF0F172A) else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Canal 2: Sala Rodadas
                        val esRodadas = (canalActual == CanalTactico.CARAVANA_CONVOY) && (salaPrivadaActiva == null)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (esRodadas) Color(0xFFFEF3C7) else Color.White,
                            border = BorderStroke(
                                width = if (esRodadas) 2.dp else 1.dp,
                                color = if (esRodadas) Color(0xFFD97706) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (esRodadas) 2.dp else 1.dp,
                            modifier = Modifier
                                .width(125.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { GestorMeshTx.activarCanalRodadas() }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CANAL 2",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (esRodadas) Color(0xFFB45309) else Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        tint = if (esRodadas) Color(0xFFB45309) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Rodadas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (esRodadas) Color(0xFF0F172A) else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Canal 3: SOS Vial
                        val esSos = canalActual == CanalTactico.EMERGENCIA_SOS && salaPrivadaActiva == null
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (esSos) Color(0xFFFEE2E2) else Color.White,
                            border = BorderStroke(
                                width = if (esSos) 2.dp else 1.dp,
                                color = if (esSos) Color(0xFFDC2626) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (esSos) 2.dp else 1.dp,
                            modifier = Modifier
                                .width(115.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    GestorMeshTx.salirASalaGeneral()
                                    GestorMeshTx.cambiarCanal(CanalTactico.EMERGENCIA_SOS)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CANAL 3",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (esSos) Color(0xFFDC2626) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SOS Vial",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (esSos) Color(0xFF7F1D1D) else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Canal 4: Directiva
                        val esDirectiva = canalActual == CanalTactico.DIRECTIVA && salaPrivadaActiva == null
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (esDirectiva) Color(0xFFF3E8FF) else Color.White,
                            border = BorderStroke(
                                width = if (esDirectiva) 2.dp else 1.dp,
                                color = if (esDirectiva) Color(0xFF9333EA) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (esDirectiva) 2.dp else 1.dp,
                            modifier = Modifier
                                .width(115.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    GestorMeshTx.salirASalaGeneral()
                                    GestorMeshTx.cambiarCanal(CanalTactico.DIRECTIVA)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CANAL 4",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (esDirectiva) Color(0xFF7E22CE) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Directiva",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (esDirectiva) Color(0xFF581C87) else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Botón Sala Privada
                        val esPrivada = salaPrivadaActiva != null
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (esPrivada) Color(0xFFDBEAFE) else Color.White,
                            border = BorderStroke(
                                width = if (esPrivada) 2.dp else 1.dp,
                                color = if (esPrivada) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (esPrivada) 2.dp else 1.dp,
                            modifier = Modifier
                                .width(135.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { mostrarDialogoSalaPrivada = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (esPrivada) Color(0xFF1D4ED8) else Color(0xFF64748B),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "CANAL PRIVADO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (esPrivada) Color(0xFF1D4ED8) else Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (esPrivada) (salaPrivadaActiva ?: "Privada") else "Crear/Entrar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (esPrivada) Color(0xFF1E3A8A) else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 3. BOTÓN CENTRAL TÁCTICO PTT (GRANDE, CENTRADO Y ERGONÓMICO)
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
                        val estaDesconectado = estadoConexion == MeshEstadoConexion.DESCONECTADO
                        val colorBotonPtt by animateColorAsState(
                            targetValue = when {
                                estaDesconectado -> Color(0xFF94A3B8)
                                estaTransmitiendoPtt -> Color(0xFFDC2626)
                                else -> Color(0xFFFF6B00)
                            },
                            animationSpec = tween(durationMillis = 150),
                            label = "BotonPttColor"
                        )

                        // Botón PTT circular ergonómico de 144 dp (Fijo verticalmente sin desplazamientos)
                        Surface(
                            shape = CircleShape,
                            color = colorBotonPtt,
                            border = BorderStroke(
                                width = if (estaTransmitiendoPtt) 4.dp else 2.dp,
                                color = when {
                                    estaDesconectado -> Color(0xFFCBD5E1)
                                    estaTransmitiendoPtt -> Color.White
                                    else -> Color(0xFFFFD8A8)
                                }
                            ),
                            shadowElevation = if (estaTransmitiendoPtt) 8.dp else 3.dp,
                            modifier = Modifier
                                .size(144.dp)
                                .clip(CircleShape)
                                .pointerInput(estadoConexion) {
                                    if (estadoConexion == MeshEstadoConexion.DESCONECTADO) {
                                        detectTapGestures(
                                            onTap = {
                                                conmutarMallaTactico()
                                            }
                                        )
                                    } else {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitFirstDown(requireUnconsumed = false)
                                                GestorMeshTx.setTransmitiendoPtt(true)
                                                var presionado = true
                                                while (presionado) {
                                                    val evento = awaitPointerEvent()
                                                    if (evento.changes.all { !it.pressed }) {
                                                        presionado = false
                                                    }
                                                }
                                                GestorMeshTx.setTransmitiendoPtt(false)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        estaDesconectado -> Icons.Default.PlayArrow
                                        estaTransmitiendoPtt -> Icons.Default.Mic
                                        else -> Icons.Default.MicNone
                                    },
                                    contentDescription = "Botón PTT",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        estaDesconectado -> "CONECTAR"
                                        estaTransmitiendoPtt -> "AL AIRE"
                                        else -> "PULSAR PTT"
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = when {
                                        estaDesconectado -> "Toca para activar radio"
                                        estaTransmitiendoPtt -> "Transmitiendo (Cola activa)"
                                        else -> "Mantener presionado"
                                    },
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
            // 4. MONITOR EN VIVO DEBAJO DEL BOTÓN: TRANSMISIÓN DE VOZ (QUIÉN HABLA)
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
                            val esMiTransmision = pilotoHablando?.contains("Tú") == true || pilotoHablando == currentMember?.nickname || pilotoHablando == currentMember?.fullName
                            val nodoHablando = if (!esMiTransmision) nodosEnRed.find { it.idMiembro == idPilotoHablando || it.aliasPiloto == pilotoHablando } else null
                            val fotoHablando = if (esMiTransmision) {
                                if (ajustes.mostrarFotoPerfil) GestorMeshTx.fotoPerfilLocal else ""
                            } else {
                                if (ajustes.mostrarFotoPerfil) (nodoHablando?.fotoUrl ?: "") else ""
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (fotoHablando.isNotBlank()) Color.Transparent else Color(0xFF0284C7),
                                border = if (fotoHablando.isNotBlank()) BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
                                modifier = Modifier.size(34.dp)
                            ) {
                                if (fotoHablando.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = fotoHablando,
                                        contentDescription = pilotoHablando,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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

            // 6. MI NODO LOCAL (Tú)
            item {
                val estaTransmitiendo = estaTransmitiendoPtt
                val miFoto = if (ajustes.mostrarFotoPerfil) GestorMeshTx.fotoPerfilLocal else ""
                val miAlias = currentMember?.nickname?.ifBlank { currentMember.fullName.ifBlank { "Piloto TX" } } ?: "Piloto TX"
                val miMoto = GestorMeshTx.modeloMotoLocal
                val miFicha = if (GestorMeshTx.fichaLocal.isNotBlank()) GestorMeshTx.fichaLocal else "Piloto Local"

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (estaTransmitiendo) Color(0xFFEFF6FF) else Color.White,
                    border = BorderStroke(
                        if (estaTransmitiendo) 2.dp else 1.dp,
                        if (estaTransmitiendo) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (estaTransmitiendo) 3.dp else 1.dp,
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
                                color = when {
                                    estaTransmitiendo -> Color(0xFFDBEAFE)
                                    ajustes.mostrarPerfilSincronizado -> Color(0xFFFFF7ED)
                                    else -> Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    if (estaTransmitiendo) 2.dp else 1.dp,
                                    if (estaTransmitiendo) Color(0xFF2563EB) else Color(0xFFFDBA74)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                if (ajustes.mostrarFotoPerfil && miFoto.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = miFoto,
                                        contentDescription = miAlias,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (estaTransmitiendo) Icons.Default.VolumeUp else Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = if (estaTransmitiendo) Color(0xFF1D4ED8) else Color(0xFFFF6B00),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "$miAlias (Tú)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = if (estaTransmitiendo) Color(0xFF1D4ED8) else Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = "MI NODO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (estaTransmitiendo)
                                        "🎙️ TRANSMITIENDO TU VOZ AL CANAL"
                                    else
                                        "$miMoto • $miFicha • Transmisor Local",
                                    fontSize = 10.sp,
                                    color = if (estaTransmitiendo) Color(0xFF2563EB) else Color(0xFF64748B),
                                    fontWeight = if (estaTransmitiendo) FontWeight.Bold else FontWeight.Medium
                                )
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
                    val estaHablando = nodo.estaTransmitiendoVoz ||
                            (idPilotoHablando != null && nodo.idMiembro == idPilotoHablando) ||
                            (!pilotoHablando.isNullOrBlank() && nodo.aliasPiloto.isNotBlank() && nodo.aliasPiloto == pilotoHablando)

                    val transition = rememberInfiniteTransition(label = "pulse_${nodo.idMiembro}")
                    val animAlpha by transition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(450, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha_${nodo.idMiembro}"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (estaHablando) Color(0xFFF0FDF4) else Color.White,
                        border = if (estaHablando) {
                            BorderStroke(2.dp, Color(0xFF16A34A).copy(alpha = animAlpha))
                        } else {
                            BorderStroke(1.dp, Color(0xFFE2E8F0))
                        },
                        shadowElevation = if (estaHablando) 4.dp else 1.dp,
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
                                    color = when {
                                        estaHablando -> Color(0xFFDCFCE7)
                                        ajustes.mostrarPerfilSincronizado -> Color(0xFFFFF7ED)
                                        else -> Color(0xFFF1F5F9)
                                    },
                                    border = BorderStroke(
                                        if (estaHablando) 2.dp else 1.dp,
                                        when {
                                            estaHablando -> Color(0xFF16A34A)
                                            ajustes.mostrarPerfilSincronizado -> Color(0xFFFDBA74)
                                            else -> Color(0xFFCBD5E1)
                                        }
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    if (ajustes.mostrarFotoPerfil && nodo.fotoUrl.isNotBlank()) {
                                        coil.compose.AsyncImage(
                                            model = nodo.fotoUrl,
                                            contentDescription = nodo.aliasPiloto,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when {
                                                    estaHablando -> Icons.Default.VolumeUp
                                                    ajustes.mostrarPerfilSincronizado -> Icons.Default.TwoWheeler
                                                    else -> Icons.Default.Smartphone
                                                },
                                                contentDescription = null,
                                                tint = when {
                                                    estaHablando -> Color(0xFF15803D)
                                                    ajustes.mostrarPerfilSincronizado -> Color(0xFFFF6B00)
                                                    else -> Color(0xFF475569)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
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
                                        color = if (estaHablando) Color(0xFF15803D) else Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val esRemotoNube = nodo.direccionNodo.contains("Nube") || nodo.direccionNodo.contains("Firebase")
                                    Text(
                                        text = if (estaHablando)
                                            "🔊 ¡RECIBIENDO AUDIO EN VIVO! • ${if (esRemotoNube) "🌐 Enlace Firebase (4G)" else "~${nodo.distanciaAproximadaMetros.toInt()}m"}"
                                        else if (ajustes.mostrarPerfilSincronizado)
                                            "${nodo.nombreMoto.ifBlank { "TX 200" }} • ${nodo.fichaMiembro.ifBlank { "Ficha #${nodo.idMiembro and 0x3FFL}" }} • ${if (esRemotoNube) "🌐 Enlace Nube / 4G (Fuera de rango)" else "~${nodo.distanciaAproximadaMetros.toInt()}m"}"
                                        else
                                            "Hardware: ${if (esRemotoNube) "🌐 Enlace Firebase (4G/Wi-Fi)" else "${nodo.direccionNodo.ifBlank { "Radio Local" }} • ${nodo.intensidadSenalDbm} dBm • ~${nodo.distanciaAproximadaMetros.toInt()}m"}",
                                        fontSize = 10.sp,
                                        color = if (estaHablando) Color(0xFF16A34A) else Color(0xFF64748B),
                                        fontWeight = if (estaHablando) FontWeight.Bold else FontWeight.Medium
                                    )

                                    val canalLocalEfectivo = GestorMeshTx.obtenerCanalIdEfectivo()
                                    val esMismoCanal = (nodo.idCanalActual == canalLocalEfectivo) &&
                                            (nodo.salaPrivada.isNullOrBlank() == salaPrivadaActiva.isNullOrBlank()) &&
                                            (nodo.salaPrivada == salaPrivadaActiva)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (esRemotoNube) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFE0F2FE)
                                            ) {
                                                Text(
                                                    text = "🌐 Nube / 4G",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0369A1),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (!nodo.salaPrivada.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (nodo.salaPrivada == salaPrivadaActiva) Color(0xFFEFF6FF) else Color(0xFFF3E8FF)
                                            ) {
                                                Text(
                                                    text = "🔒 Sala: ${nodo.salaPrivada}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (nodo.salaPrivada == salaPrivadaActiva) Color(0xFF2563EB) else Color(0xFF7E22CE),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (esMismoCanal) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = if (esMismoCanal) "📻 En tu canal (${nodo.nombreCanalActual})" else "📻 En ${nodo.nombreCanalActual} (Distinto)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (esMismoCanal) Color(0xFF15803D) else Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (estaHablando) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7),
                                    border = BorderStroke(1.dp, Color(0xFF16A34A))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "HABLANDO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            } else {
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ═════════════════════════════════════════════════════════
                    // SECCIÓN INTERCOM INTRAMOTO: PILOTO & COPILOTO
                    // ═════════════════════════════════════════════════════════
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "🏍️ Enlace Intramoto (Piloto-Copiloto)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Micro-Malla Bluetooth Clásico & Modo Puente",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // ⚠️ Advertencia de distancia máxima de Bluetooth
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "⚠️ Alcance Bluetooth: Máx 10-15 metros. Solo para Piloto y Copiloto montados en la misma moto TX 200.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            // Selector de Modo de Red (Trimodal)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Modo de Red Intramoto:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        ModoEnlaceIntramoto.SOLO_BLUETOOTH to Pair("Solo Bluetooth 🔋", "Ahorro extremo: Copiloto apaga Wi-Fi, latencia < 40ms"),
                                        ModoEnlaceIntramoto.SOLO_WIFI to Pair("Solo Wi-Fi 📡", "Malla local P2P de alcance extendido (~150m)"),
                                        ModoEnlaceIntramoto.HIBRIDO_TRIMODAL to Pair("Híbrido Trimodal ⚡", "Bluetooth + Wi-Fi + 4G redundante sin pérdida de voz")
                                    ).forEach { (modo, info) ->
                                        val seleccionado = ajustes.modoEnlacePrivado == modo
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (seleccionado) Color(0xFFFFEDD5) else Color.White,
                                            border = BorderStroke(
                                                1.dp,
                                                if (seleccionado) Color(0xFFFF6B00) else Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { GestorMeshTx.alternarModoEnlaceIntramoto(modo) }
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                Text(
                                                    text = info.first,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Bold,
                                                    color = if (seleccionado) Color(0xFFC2410C) else Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = info.second,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Selector de Rol en la Moto
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Rol en la Moto:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        RolEnMoto.PILOTO_GATEWAY to "🏍️ Piloto",
                                        RolEnMoto.COPILOTO_ENLACE to "🎒 Copiloto",
                                        RolEnMoto.SOLO_PILOTO_INDIVIDUAL to "👤 Solo"
                                    ).forEach { (rol, etiqueta) ->
                                        val seleccionado = ajustes.rolEnMoto == rol
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (seleccionado) Color(0xFF2563EB) else Color.White,
                                            border = BorderStroke(1.dp, if (seleccionado) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { GestorMeshTx.alternarRolEnMoto(rol) }
                                        ) {
                                            Text(
                                                text = etiqueta,
                                                fontSize = 10.sp,
                                                fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Bold,
                                                color = if (seleccionado) Color.White else Color(0xFF334155),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dispositivo vinculado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Dispositivo Compañero:",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = if (ajustes.nombreDispositivoCopiloto.isNotBlank())
                                            ajustes.nombreDispositivoCopiloto
                                        else
                                            "Sin vincular",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { mostrarDialogoCopiloto = true },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Seleccionar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                            }

                            // Retransmitir Copiloto a Caravana
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Retransmitir Copiloto a Caravana",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "El Piloto reenvía la voz del copiloto hacia el convoy por Wi-Fi y 4G",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = ajustes.retransmitirCopilotoACaravana,
                                    onCheckedChange = { GestorMeshTx.alternarRetransmitirCopilotoCaravana(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF2563EB)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFFE2E8F0))

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

                    // Retardo de Fin de PTT (Hang-Time al soltar)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "⏱️ Retardo al Soltar PTT (Cola de Audio)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Mantiene la captura abierta unos milisegundos tras soltar el botón para que nunca se corte la última palabra.",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                400L to "400 ms",
                                800L to "800 ms (Recomendado)",
                                1200L to "1.2 s",
                                1500L to "1.5 s"
                            ).forEach { (ms, etiqueta) ->
                                val seleccionado = ajustes.retardoFinPttMs == ms
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (seleccionado) Color(0xFFFF6B00) else Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { GestorMeshTx.ajustarRetardoFinPtt(ms) }
                                ) {
                                    Text(
                                        text = etiqueta,
                                        fontSize = 10.sp,
                                        fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Medium,
                                        color = if (seleccionado) Color.White else Color(0xFF334155),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Tono Roger Beep Táctico
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🔔 Tono Roger Beep Táctico",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Beep de confirmación al soltar PTT para avisar que la frecuencia quedó libre",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.tonoRogerBeep,
                            onCheckedChange = { GestorMeshTx.alternarTonoRogerBeep(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B00)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Fidelidad de Audio HD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🎙️ Fidelidad de Audio HD (16 kHz)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.fidelidadAudioAlta)
                                    "Activo: Voz clara en banda ancha (16 kHz) optimizada para cascos"
                                else
                                    "Inactivo: Banda estrecha (8 kHz) ultra-comprimida",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.fidelidadAudioAlta,
                            onCheckedChange = { GestorMeshTx.alternarFidelidadAudio(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A)
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

                    // Switch Búfer Adaptativo Anti-Entrecorte (Jitter Buffer + PLC)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Búfer Anti-Entrecorte Táctico",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Jitter buffer adaptativo y suavizado PLC para eliminar cortes",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = ajustes.bufferAntiEntrecorte,
                                onCheckedChange = { GestorMeshTx.alternarBufferAntiEntrecorte(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF6B00)
                                )
                            )
                        }

                        if (ajustes.bufferAntiEntrecorte) {
                            Text(
                                "Tamaño del búfer de compensación de jitter:",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    120 to "120 ms (Rápido)",
                                    200 to "200 ms (Recomendado)",
                                    320 to "320 ms (Robusto)"
                                ).forEach { (ms, etiqueta) ->
                                    val seleccionado = ajustes.tamanoBufferJitterMs == ms
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (seleccionado) Color(0xFFFF6B00) else Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { GestorMeshTx.ajustarBufferJitter(ms) }
                                    ) {
                                        Text(
                                            text = etiqueta,
                                            fontSize = 10.sp,
                                            fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Medium,
                                            color = if (seleccionado) Color.White else Color(0xFF334155),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Supresión de Eco Acústico (AEC Hardware + Software Gate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Cancelación Activa de Eco Acústico",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "AEC por hardware y compuerta anti-retroalimentación de altavoz",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.supresionEcoAcustico,
                            onCheckedChange = { GestorMeshTx.alternarSupresionEco(it) },
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

                    // Switch Mostrar Foto de Perfil en Malla
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Mostrar Foto de Perfil en Malla",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.mostrarFotoPerfil)
                                    "Activo: Muestra tu foto y las fotos de los compañeros conectados"
                                else
                                    "Inactivo: Oculta tu foto y muestra el icono de moto táctica en todos los dispositivos",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.mostrarFotoPerfil,
                            onCheckedChange = { GestorMeshTx.alternarMostrarFotoPerfil(it) },
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

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Botón Flotante PTT en Pantalla (Nube Flotante Táctica)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🔘 Nube / Botón PTT Flotante en Pantalla",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.botonFlotantePttActivo)
                                    "Activo: Muestra una burbuja movible para hablar PTT desde cualquier pantalla de la app"
                                else
                                    "Inactivo: El botón PTT solo se muestra dentro de esta pantalla de Mesh TX",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.botonFlotantePttActivo,
                            onCheckedChange = { GestorMeshTx.alternarBotonFlotantePtt(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B00)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Ayuda con Datos (Firebase Sync)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📶 Ayuda con Datos (Firebase Sync)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.ayudaConDatosFirebase)
                                    "Activo: Puente VoIP celular habilitado para enlazar islas y caravanas sin límite de distancia"
                                else
                                    "Inactivo: Operación 100% offline local por Wi-Fi Direct y radio P2P",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.ayudaConDatosFirebase,
                            onCheckedChange = { GestorMeshTx.alternarAyudaConDatos(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2563EB)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Ruido de Confort Táctico (CNG)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📻 Ruido de Confort Táctico (CNG)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.cngRuidoConfort)
                                    "Activo: Sutil estática de línea (-48 dBFS) que confirma enlace auditivo con el convoy"
                                else
                                    "Inactivo: Silencio absoluto entre transmisiones de voz",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.cngRuidoConfort,
                            onCheckedChange = { GestorMeshTx.alternarCngRuidoConfort(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B00)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE2E8F0))

                    // Switch Corrección de Errores (FEC Redundancia N + N-1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🛡️ Corrección de Errores (FEC Redundante)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                if (ajustes.fecRedundanciaActiva)
                                    "Activo: Entrelaza copia de respaldo anterior (N-1) para rescatar paquetes perdidos por viento o distancia"
                                else
                                    "Inactivo: Envío simple de un único frame sin redundancia",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = ajustes.fecRedundanciaActiva,
                            onCheckedChange = { GestorMeshTx.alternarFecRedundancia(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B00)
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

    // ─────────────────────────────────────────────────────────────────────────
    // DIÁLOGO DE SALA PRIVADA AISLADA (OFFLINE)
    // ─────────────────────────────────────────────────────────────────────────
    if (mostrarDialogoSalaPrivada) {
        DialogoCrearOUnirseASalaPrivada(
            salaActual = salaPrivadaActiva,
            onDismiss = { mostrarDialogoSalaPrivada = false }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIÁLOGO DE VINCULACIÓN BLUETOOTH INTRAMOTO (PILOTO - COPILOTO)
    // ─────────────────────────────────────────────────────────────────────────
    if (mostrarDialogoCopiloto) {
        DialogoSeleccionarDispositivoCopiloto(
            dispositivoSeleccionadoMac = ajustes.macDispositivoCopiloto,
            onDispositivoSeleccionado = { nombre, mac ->
                GestorMeshTx.configurarDispositivoCopiloto(nombre, mac)
                mostrarDialogoCopiloto = false
            },
            onDismiss = { mostrarDialogoCopiloto = false }
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

/**
 * Diálogo para crear o unirse a una Sala Privada / Sub-grupo offline.
 * Mapea el nombre o PIN a un canal numérico aislado para que únicamente
 * los pilotos con el mismo código puedan escucharse e intercambiar audio.
 */
@Composable
fun DialogoCrearOUnirseASalaPrivada(
    salaActual: String?,
    onDismiss: () -> Unit
) {
    var codigoIngresado by remember { mutableStateOf(salaActual ?: "") }
    val sugerencias = listOf("CARAVANA-1", "GRUPO-VIP", "PUNTEROS", "BARREDORAS", "RODADA-DOMINGO")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDBEAFE),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Sala Privada Offline",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "CANAL EXCLUSIVO CIFRADO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ingresa un código, PIN o nombre de sala. Todos los compañeros que coloquen este mismo valor quedarán enlazados en su propio canal de radiofrecuencia aislado.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = codigoIngresado,
                    onValueChange = { codigoIngresado = it.uppercase() },
                    label = { Text("Nombre o Código de Sala") },
                    placeholder = { Text("Ej: VIP-2026 ó 4455") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Text(
                    text = "Sugerencias rápidas:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sugerencias.forEach { sugerencia ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { codigoIngresado = sugerencia }
                        ) {
                            Text(
                                text = sugerencia,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (salaActual != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Actualmente en: $salaActual",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            TextButton(
                                onClick = {
                                    GestorMeshTx.salirASalaGeneral()
                                    onDismiss()
                                }
                            ) {
                                Text(
                                    text = "Salir a General",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626)
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
                    if (codigoIngresado.isNotBlank()) {
                        GestorMeshTx.entrarASalaPrivada(codigoIngresado)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                enabled = codigoIngresado.isNotBlank()
            ) {
                Text(
                    text = if (salaActual != null) "Cambiar Sala" else "Entrar a Sala",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Diálogo para seleccionar el dispositivo Bluetooth del Copiloto o Piloto.
 * Permite vincular directamente el teléfono de la persona con la que viajas en la moto.
 */
@Composable
fun DialogoSeleccionarDispositivoCopiloto(
    dispositivoSeleccionadoMac: String,
    onDispositivoSeleccionado: (nombre: String, mac: String) -> Unit,
    onDismiss: () -> Unit
) {
    val dispositivosEmparejados = remember {
        GestorMeshTx.obtenerDispositivosBluetoothEmparejados()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Vincular Teléfono Intramoto",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "DISPOSITIVOS BLUETOOTH EMPAREJADOS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Advertencia de distancia máxima
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "⚠️ Alcance Bluetooth: Máximo 10 a 15 metros. Solo para Piloto y Copiloto en la misma moto.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                Text(
                    text = "Selecciona el teléfono de tu compañero(a) de moto para enlazar el intercomunicador privado:",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )

                if (dispositivosEmparejados.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No se encontraron dispositivos vinculados",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Ve a los Ajustes de Android > Bluetooth en ambos teléfonos, empareja el equipo de tu copiloto o piloto una sola vez, y vuelve a esta pantalla.",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dispositivosEmparejados.forEach { (nombre, mac) ->
                            val esSeleccionado = mac.equals(dispositivoSeleccionadoMac, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (esSeleccionado) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (esSeleccionado) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDispositivoSeleccionado(nombre, mac)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = if (esSeleccionado) Color(0xFF2563EB) else Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = nombre.ifBlank { "Dispositivo Android" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (esSeleccionado) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = mac,
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    if (esSeleccionado) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}


