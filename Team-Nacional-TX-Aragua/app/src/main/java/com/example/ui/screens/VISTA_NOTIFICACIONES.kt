package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.GestorNotificacionesApp
import com.example.dashboard.DashboardFondoConfig
import com.example.data.model.NotificacionApp
import java.text.SimpleDateFormat
import java.util.*

enum class FiltroNotificacion(val titulo: String) {
    TODAS("Todas"),
    SIN_LEER("Sin leer"),
    RODADAS_RETOS("Rodadas & Retos"),
    COMERCIO_FINANZAS("Comercio & Finanzas"),
    SOS_ALERTAS("SOS & Alertas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaNotificaciones(
    onBack: () -> Unit,
    onNotificacionClick: (NotificacionApp) -> Unit = {}
) {
    val notificacionesList by GestorNotificacionesApp.obtenerTodas()
        ?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    val noLeidasCount by GestorNotificacionesApp.obtenerCantidadNoLeidas()
        ?.collectAsState(initial = 0)
        ?: remember { mutableStateOf(0) }

    var filtroSeleccionado by remember { mutableStateOf(FiltroNotificacion.TODAS) }

    val notificacionesFiltradas = remember(notificacionesList, filtroSeleccionado) {
        when (filtroSeleccionado) {
            FiltroNotificacion.TODAS -> notificacionesList
            FiltroNotificacion.SIN_LEER -> notificacionesList.filter { !it.leida }
            FiltroNotificacion.RODADAS_RETOS -> notificacionesList.filter { it.tipo in listOf("RODADA", "RETOS", "CALENDARIO") }
            FiltroNotificacion.COMERCIO_FINANZAS -> notificacionesList.filter { it.tipo in listOf("COMERCIO", "TESORERIA", "MERCADO") }
            FiltroNotificacion.SOS_ALERTAS -> notificacionesList.filter { it.tipo in listOf("SOS", "ACTUALIZACION", "OTA") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Avisos & Notificaciones",
                            color = DashboardFondoConfig.ColorTextoPrimario,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (noLeidasCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = DashboardFondoConfig.ColorRojoCarrera,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (noLeidasCount > 99) "99+" else "$noLeidasCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = DashboardFondoConfig.ColorTextoPrimario
                        )
                    }
                },
                actions = {
                    if (noLeidasCount > 0) {
                        TextButton(onClick = { GestorNotificacionesApp.marcarTodasLeidas() }) {
                            Text(
                                "Marcar leídas",
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(onClick = { GestorNotificacionesApp.eliminarLeidas() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Limpiar leídas",
                            tint = DashboardFondoConfig.ColorTextoSecundario
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DashboardFondoConfig.ColorTarjetaClara
                )
            )
        },
        containerColor = DashboardFondoConfig.ColorFondoClaro
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chips de filtrado rápido
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FiltroNotificacion.values()) { filtro ->
                    val esSeleccionado = filtro == filtroSeleccionado
                    FilterChip(
                        selected = esSeleccionado,
                        onClick = { filtroSeleccionado = filtro },
                        label = {
                            Text(
                                filtro.titulo,
                                fontSize = 12.sp,
                                fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE3F2FD),
                            selectedLabelColor = Color(0xFF1976D2),
                            containerColor = DashboardFondoConfig.ColorTarjetaClara,
                            labelColor = DashboardFondoConfig.ColorTextoSecundario
                        )
                    )
                }
            }

            if (notificacionesFiltradas.isEmpty()) {
                // Estado vacío
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = DashboardFondoConfig.ColorTextoSecundario,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay avisos en esta categoría",
                            color = DashboardFondoConfig.ColorTextoSecundario,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notificacionesFiltradas, key = { it.id }) { notificacion ->
                        NotificacionItem(
                            notificacion = notificacion,
                            onClick = {
                                GestorNotificacionesApp.marcarLeida(notificacion.id)
                                onNotificacionClick(notificacion)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificacionItem(
    notificacion: NotificacionApp,
    onClick: () -> Unit
) {
    val fechaFormateada = remember(notificacion.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(notificacion.timestamp))
    }

    val (icono, colorIcono) = when (notificacion.tipo) {
        "CHAT" -> Pair(Icons.Default.Chat, Color(0xFF1976D2))
        "MURO" -> Pair(Icons.Default.Campaign, Color(0xFFF57C00))
        "SOS" -> Pair(Icons.Default.Emergency, DashboardFondoConfig.ColorRojoCarrera)
        "ACTUALIZACION", "OTA" -> Pair(Icons.Default.SystemUpdate, Color(0xFF00ACC1))
        "RODADA", "CALENDARIO" -> Pair(Icons.Default.TwoWheeler, Color(0xFF43A047))
        "RETOS" -> Pair(Icons.Default.EmojiEvents, DashboardFondoConfig.ColorDoradoOro)
        "MERCADO", "COMERCIO" -> Pair(Icons.Default.Storefront, Color(0xFF3949AB))
        "TESORERIA" -> Pair(Icons.Default.AccountBalanceWallet, Color(0xFF2E7D32))
        "PERFIL" -> Pair(Icons.Default.Badge, Color(0xFF8E24AA))
        "VINCULAR_GOOGLE" -> Pair(Icons.Default.AccountCircle, Color(0xFF00897B))
        else -> Pair(Icons.Default.Notifications, Color(0xFF64748B))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DashboardFondoConfig.ColorTarjetaClara
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!notificacion.leida) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Ícono de categoría con fondo suave
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colorIcono.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contenido del aviso
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notificacion.titulo,
                        color = DashboardFondoConfig.ColorTextoPrimario,
                        fontWeight = if (!notificacion.leida) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notificacion.leida) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1976D2))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notificacion.mensaje,
                    color = DashboardFondoConfig.ColorTextoSecundario,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = fechaFormateada,
                    color = DashboardFondoConfig.ColorTextoSecundario,
                    fontSize = 11.sp
                )
            }
        }
    }
}
