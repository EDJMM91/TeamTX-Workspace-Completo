package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.preferences.PreferenciasApp
import com.example.ui.theme.*

// ─── Pantalla Principal de Configuraciones ────────────────────────────────────

@Composable
fun ConfiguracionesScreen(
    isDirectivaMode: Boolean = false,
    onCerrarSesion: () -> Unit,
    onDarseDeBaja: (motivo: String) -> Unit
) {
    val context = LocalContext.current

    // Inicializar prefs si no están inicializadas
    PreferenciasApp.init(context)

    var showBajaDialog by remember { mutableStateOf(false) }

    // ─── Estado reactivo de todas las preferencias ─────────────────────────
    // Global
    var modoOscuroApp by remember { mutableStateOf(PreferenciasApp.modoOscuro) }
    var notificaciones by remember { mutableStateOf(PreferenciasApp.notificacionesActivas) }
    var sonidoChat by remember { mutableStateOf(PreferenciasApp.sonidoChat) }
    var textoGrande by remember { mutableStateOf(PreferenciasApp.tamanoTexto == "grande") }

    // Muro
    var muroFijadosPrimero by remember { mutableStateOf(PreferenciasApp.muroFijadosPrimero) }
    var muroAutoColapsar by remember { mutableStateOf(PreferenciasApp.muroAutoColapsarComentarios) }

    // Chat
    var chatAvatar by remember { mutableStateOf(PreferenciasApp.chatMostrarAvatar) }
    var chatVibrar by remember { mutableStateOf(PreferenciasApp.chatVibrar) }

    // Rodadas
    var rodadasKm by remember { mutableStateOf(PreferenciasApp.rodasUnidadKm) }
    var rodadasRecordatorio by remember { mutableStateOf(PreferenciasApp.rodadasRecordatorio) }

    // Miembros
    var miembrosLista by remember { mutableStateOf(PreferenciasApp.miembrosVistaLista) }
    var miembrosSoloActivos by remember { mutableStateOf(PreferenciasApp.miembrosSoloActivos) }

    // Tesorería
    var tesUsd by remember { mutableStateOf(PreferenciasApp.tesoreraMoneda == "USD") }
    var tesBcvAuto by remember { mutableStateOf(PreferenciasApp.tesoreriaBcvAuto) }

    // Inventario
    var invAlertaStock by remember { mutableStateOf(PreferenciasApp.inventarioAlertaStock) }
    var invSoloDisponibles by remember { mutableStateOf(PreferenciasApp.inventarioSoloDisponibles) }

    // SOS
    var sosUbicacion by remember { mutableStateOf(PreferenciasApp.sosCompartirUbicacion) }
    var sosSonido by remember { mutableStateOf(PreferenciasApp.sosSonidoAlerta) }

    // Normativas
    var normativasGrande by remember { mutableStateOf(PreferenciasApp.normativasTamanoTexto == "grande") }

    // Directiva
    var directivaAutoOcultar by remember { mutableStateOf(PreferenciasApp.directivaAutoOcultarNav) }

    // Carnet
    var carnetQr by remember { mutableStateOf(PreferenciasApp.carnetMostrarQr) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = TxFlameRed.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = TxFlameRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Configuraciones",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Ajustes y preferencias de la app",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Sección Global ────────────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "General",
                icono = Icons.Default.Tune,
                defaultExpanded = true
            ) {
                ItemToggle(
                    label = "Modo oscuro",
                    descripcion = if (modoOscuroApp) "Tema oscuro activo" else "Tema claro activo",
                    icono = if (modoOscuroApp) Icons.Default.DarkMode else Icons.Default.LightMode,
                    checked = modoOscuroApp,
                    onCheckedChange = {
                        modoOscuroApp = it
                        PreferenciasApp.modoOscuro = it
                    }
                )
                ItemToggle(
                    label = "Notificaciones push",
                    descripcion = "Recibir avisos, SOS y novedades",
                    icono = Icons.Default.Notifications,
                    checked = notificaciones,
                    onCheckedChange = {
                        notificaciones = it
                        PreferenciasApp.notificacionesActivas = it
                    }
                )
                ItemToggle(
                    label = "Sonido en el chat",
                    descripcion = "Reproducir sonido al recibir mensajes",
                    icono = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = sonidoChat,
                    onCheckedChange = {
                        sonidoChat = it
                        PreferenciasApp.sonidoChat = it
                    }
                )
                ItemToggle(
                    label = "Texto grande",
                    descripcion = "Aumentar tamaño de texto en toda la app",
                    icono = Icons.Default.FormatSize,
                    checked = textoGrande,
                    onCheckedChange = {
                        textoGrande = it
                        PreferenciasApp.tamanoTexto = if (it) "grande" else "normal"
                    }
                )
            }
        }

        // ── Módulo 1: Muro ────────────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Muro",
                icono = Icons.Default.Campaign,
            ) {
                ItemToggle(
                    label = "Fijados primero",
                    descripcion = "Mostrar publicaciones fijadas al inicio del muro",
                    icono = Icons.Default.PushPin,
                    checked = muroFijadosPrimero,
                    onCheckedChange = {
                        muroFijadosPrimero = it
                        PreferenciasApp.muroFijadosPrimero = it
                    }
                )
                ItemToggle(
                    label = "Colapsar comentarios",
                    descripcion = "Ocultar comentarios por defecto al abrir el muro",
                    icono = Icons.Default.UnfoldLess,
                    checked = muroAutoColapsar,
                    onCheckedChange = {
                        muroAutoColapsar = it
                        PreferenciasApp.muroAutoColapsarComentarios = it
                    }
                )
            }
        }

        // ── Módulo 2: Chat ────────────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Chat",
                icono = Icons.Default.Forum,
            ) {
                ItemToggle(
                    label = "Mostrar avatares",
                    descripcion = "Ver foto de perfil junto a cada mensaje",
                    icono = Icons.Default.AccountCircle,
                    checked = chatAvatar,
                    onCheckedChange = {
                        chatAvatar = it
                        PreferenciasApp.chatMostrarAvatar = it
                    }
                )
                ItemToggle(
                    label = "Vibrar al recibir mensaje",
                    descripcion = "Vibración al llegar un mensaje nuevo",
                    icono = Icons.Default.Vibration,
                    checked = chatVibrar,
                    onCheckedChange = {
                        chatVibrar = it
                        PreferenciasApp.chatVibrar = it
                    }
                )
            }
        }

        // ── Módulo 3: Rodadas ─────────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Rodadas",
                icono = Icons.Default.TwoWheeler,
            ) {
                ItemToggle(
                    label = "Distancia en kilómetros",
                    descripcion = "Mostrar distancias en km (desactivar para millas)",
                    icono = Icons.Default.Speed,
                    checked = rodadasKm,
                    onCheckedChange = {
                        rodadasKm = it
                        PreferenciasApp.rodasUnidadKm = it
                    }
                )
                ItemToggle(
                    label = "Recordatorio de rodada",
                    descripcion = "Notificar antes de una rodada programada",
                    icono = Icons.Default.Alarm,
                    checked = rodadasRecordatorio,
                    onCheckedChange = {
                        rodadasRecordatorio = it
                        PreferenciasApp.rodadasRecordatorio = it
                    }
                )
            }
        }

        // ── Módulo 4: Miembros ────────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Miembros",
                icono = Icons.Default.Groups,
            ) {
                ItemToggle(
                    label = "Vista en lista",
                    descripcion = "Mostrar miembros en lista (desactivar para tarjetas)",
                    icono = Icons.AutoMirrored.Filled.ViewList,
                    checked = miembrosLista,
                    onCheckedChange = {
                        miembrosLista = it
                        PreferenciasApp.miembrosVistaLista = it
                    }
                )
                ItemToggle(
                    label = "Solo activos",
                    descripcion = "Mostrar únicamente miembros activos por defecto",
                    icono = Icons.Default.FilterList,
                    checked = miembrosSoloActivos,
                    onCheckedChange = {
                        miembrosSoloActivos = it
                        PreferenciasApp.miembrosSoloActivos = it
                    }
                )
            }
        }

        // ── Módulo 5: Tesorería ───────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Tesorería",
                icono = Icons.Default.AccountBalanceWallet,
            ) {
                ItemToggle(
                    label = "Moneda en USD",
                    descripcion = "Mostrar montos en dólares (desactivar para Bolívares)",
                    icono = Icons.Default.AttachMoney,
                    checked = tesUsd,
                    onCheckedChange = {
                        tesUsd = it
                        PreferenciasApp.tesoreraMoneda = if (it) "USD" else "BS"
                    }
                )
                ItemToggle(
                    label = "Tasa BCV automática",
                    descripcion = "Actualizar la tasa BCV al abrir tesorería",
                    icono = Icons.Default.Refresh,
                    checked = tesBcvAuto,
                    onCheckedChange = {
                        tesBcvAuto = it
                        PreferenciasApp.tesoreriaBcvAuto = it
                    }
                )
            }
        }

        // ── Módulo 6: Inventario ──────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Inventario",
                icono = Icons.Default.Handyman,
            ) {
                ItemToggle(
                    label = "Alerta de stock bajo",
                    descripcion = "Advertir cuando un ítem tenga 2 o menos unidades",
                    icono = Icons.Default.Warning,
                    checked = invAlertaStock,
                    onCheckedChange = {
                        invAlertaStock = it
                        PreferenciasApp.inventarioAlertaStock = it
                    }
                )
                ItemToggle(
                    label = "Solo disponibles",
                    descripcion = "Filtrar por defecto mostrando ítems disponibles",
                    icono = Icons.Default.CheckCircle,
                    checked = invSoloDisponibles,
                    onCheckedChange = {
                        invSoloDisponibles = it
                        PreferenciasApp.inventarioSoloDisponibles = it
                    }
                )
            }
        }

        // ── Módulo 7: SOS Vial ────────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "SOS Vial",
                icono = Icons.Default.Emergency,
            ) {
                ItemToggle(
                    label = "Compartir ubicación en SOS",
                    descripcion = "Enviar coordenadas GPS al emitir una alerta",
                    icono = Icons.Default.LocationOn,
                    checked = sosUbicacion,
                    onCheckedChange = {
                        sosUbicacion = it
                        PreferenciasApp.sosCompartirUbicacion = it
                    }
                )
                ItemToggle(
                    label = "Sonido de alerta SOS",
                    descripcion = "Reproducir alarma al recibir un SOS activo",
                    icono = Icons.Default.SurroundSound,
                    checked = sosSonido,
                    onCheckedChange = {
                        sosSonido = it
                        PreferenciasApp.sosSonidoAlerta = it
                    }
                )
            }
        }

        // ── Módulo 8: Normativas ──────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Normativas",
                icono = Icons.Default.Gavel,
            ) {
                ItemToggle(
                    label = "Texto grande en normativas",
                    descripcion = "Aumentar tamaño de letra en las normativas",
                    icono = Icons.Default.FormatSize,
                    checked = normativasGrande,
                    onCheckedChange = {
                        normativasGrande = it
                        PreferenciasApp.normativasTamanoTexto = if (it) "grande" else "normal"
                    }
                )
            }
        }

        // ── Módulo 9: Directiva (solo visible para directivos) ─────────────────
        if (isDirectivaMode) {
            item {
                SeccionConfiguraciones(
                    titulo = "Directiva",
                    icono = Icons.Default.Shield,
                ) {
                    ItemToggle(
                        label = "Auto-ocultar barra inferior",
                        descripcion = "Ocultar la barra de navegación al entrar al panel directiva",
                        icono = Icons.Default.HideSource,
                        checked = directivaAutoOcultar,
                        onCheckedChange = {
                            directivaAutoOcultar = it
                            PreferenciasApp.directivaAutoOcultarNav = it
                        }
                    )
                }
            }
        }

        // ── Módulo 10: Carnet TX ──────────────────────────────────────────────
        item {
            SeccionConfiguraciones(
                titulo = "Carnet TX",
                icono = Icons.Default.Badge,
            ) {
                ItemToggle(
                    label = "Mostrar QR en carnet",
                    descripcion = "Incluir código QR en tu carnet digital",
                    icono = Icons.Default.QrCode,
                    checked = carnetQr,
                    onCheckedChange = {
                        carnetQr = it
                        PreferenciasApp.carnetMostrarQr = it
                    }
                )
            }
        }

        // ── Sección: Sesión ───────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sesión",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        // Botón: Cerrar sesión
        item {
            Button(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TxFlameRed.copy(alpha = 0.18f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, TxFlameRed.copy(alpha = 0.6f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = TxFlameRed)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Cerrar Sesión",
                    color = TxFlameRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Botón: Darse de baja del Team
        item {
            OutlinedButton(
                onClick = { showBajaDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7A1515))
            ) {
                Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color(0xFF7A1515))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Darme de Baja del Team",
                    color = Color(0xFF7A1515),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Diálogo: Motivo de Baja ────────────────────────────────────────────────
    if (showBajaDialog) {
        DialogDarseDeBaja(
            onConfirmar = { motivo ->
                showBajaDialog = false
                onDarseDeBaja(motivo)
            },
            onCancelar = { showBajaDialog = false }
        )
    }
}

// ─── Componente: Sección Colapsable ──────────────────────────────────────────

@Composable
private fun SeccionConfiguraciones(
    titulo: String,
    icono: ImageVector,
    defaultExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expandida by remember { mutableStateOf(defaultExpanded) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column {
            // Header de la sección
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandida = !expandida }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TxFlameRed.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icono, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    titulo,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expandida) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expandida) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Contenido animado
            AnimatedVisibility(
                visible = expandida,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))
                    content()
                }
            }
        }
    }
}

// ─── Componente: Toggle de configuración ─────────────────────────────────────

@Composable
private fun ItemToggle(
    label: String,
    descripcion: String,
    icono: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = if (checked) TxFlameRed else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                descripcion,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TxFlameRed,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

// ─── Diálogo: Darse de baja del Team ─────────────────────────────────────────

@Composable
private fun DialogDarseDeBaja(
    onConfirmar: (motivo: String) -> Unit,
    onCancelar: () -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var confirmado by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancelar) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF7A1515), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Ícono de advertencia
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF7A1515).copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PersonRemove,
                                contentDescription = null,
                                tint = Color(0xFF7A1515),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "¿Seguro que quieres darte de baja?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tu perfil y acceso serán eliminados. Esta acción no se puede deshacer. Puedes decirnos el motivo:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo de la baja (opcional)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("Ej: me cambio de ciudad, razones personales...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TxFlameRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = TxFlameRed,
                        cursorColor = TxFlameRed,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (!confirmado) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ Este paso es irreversible.",
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancelar,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (!confirmado) {
                                confirmado = true
                            } else {
                                onConfirmar(motivo.ifBlank { "Sin motivo especificado" })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1515))
                    ) {
                        Text(
                            if (confirmado) "CONFIRMAR BAJA" else "Dar de Baja",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
