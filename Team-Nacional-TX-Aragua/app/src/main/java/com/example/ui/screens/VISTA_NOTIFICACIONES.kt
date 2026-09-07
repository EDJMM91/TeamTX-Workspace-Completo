package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.NotificacionApp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaNotificaciones(
    onBack: () -> Unit,
    onNotificacionClick: (NotificacionApp) -> Unit = {}
) {
    val notificaciones by GestorNotificacionesApp.obtenerTodas()
        ?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    val noLeidasCount by GestorNotificacionesApp.obtenerCantidadNoLeidas()
        ?.collectAsState(initial = 0)
        ?: remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notificaciones",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (noLeidasCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White,
                                modifier = Modifier.padding(top = 2.dp)
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (noLeidasCount > 0) {
                        TextButton(onClick = { GestorNotificacionesApp.marcarTodasLeidas() }) {
                            Text(
                                "Marcar todo leído",
                                color = Color(0xFF81C784),
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = AsphaltDarkBackground
    ) { padding ->
        if (notificaciones.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No hay notificaciones",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(notificaciones, key = { it.id }) { notificacion ->
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

@Composable
fun NotificacionItem(
    notificacion: NotificacionApp,
    onClick: () -> Unit
) {
    val fechaFormateada = remember(notificacion.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(notificacion.timestamp))
    }

    val (icono, colorIcono) = when (notificacion.tipo) {
        "CHAT" -> Pair(Icons.Default.Chat, Color(0xFF42A5F5))
        "MURO" -> Pair(Icons.Default.Campaign, Color(0xFFFFA726))
        "SOS" -> Pair(Icons.Default.Emergency, Color(0xFFEF5350))
        "ACTUALIZACION", "OTA" -> Pair(Icons.Default.SystemUpdate, MotoOrangePrimary)
        "PERFIL" -> Pair(Icons.Default.Badge, Color(0xFFAB47BC))
        "VINCULAR_GOOGLE" -> Pair(Icons.Default.AccountCircle, Color(0xFF66BB6A))
        else -> Pair(Icons.Default.Notifications, Color(0xFF78909C))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notificacion.leida)
                Color(0xFF1E2A3A) else Color(0xFF141C24)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Ícono del tipo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorIcono.copy(alpha = 0.15f)),
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

            // Contenido
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notificacion.titulo,
                        color = Color.White,
                        fontWeight = if (!notificacion.leida) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!notificacion.leida) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notificacion.mensaje,
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = fechaFormateada,
                    color = Color(0xFF607D8B),
                    fontSize = 11.sp
                )
            }
        }
    }
}
