package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.dashboard.DashboardFondoConfig
import com.example.data.model.BikerInterestPoint
import com.example.data.model.MemberProfile
import com.example.radar.GestorRadar
import com.example.ui.theme.*
import com.example.util.SanitizadorImagenUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitiosInteresScreen(
    interestPoints: List<BikerInterestPoint>,
    currentMember: MemberProfile?,
    onNavigateToMap: (BikerInterestPoint) -> Unit = {},
    onLikeSpot: (BikerInterestPoint) -> Unit = {},
    onDislikeSpot: (BikerInterestPoint) -> Unit = {},
    onSubmitReport: (spotId: Long, spotName: String, spotType: String, reason: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE) }
    var mostrarEnMapa by remember { mutableStateOf(prefs.getBoolean("mostrar_sitios_en_mapa", true)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var reportSpotTarget by remember { mutableStateOf<BikerInterestPoint?>(null) }
    var reportReasonText by remember { mutableStateOf("") }

    val categoriesList = remember {
        listOf(
            "Todos",
            "Mirador / Parador Biker",
            "Playa / Costa",
            "Montaña / Ruta",
            "Camping / Descanso",
            "Punto de Encuentro"
        )
    }

    val filteredList = remember(interestPoints, searchQuery, selectedCategoryFilter) {
        interestPoints.filter { spot ->
            val matchQuery = searchQuery.isBlank() ||
                    spot.name.contains(searchQuery, ignoreCase = true) ||
                    spot.description.contains(searchQuery, ignoreCase = true) ||
                    spot.address.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategoryFilter == null || selectedCategoryFilter == "Todos" ||
                    spot.category.equals(selectedCategoryFilter, ignoreCase = true)
            matchQuery && matchCat
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sitios de Interés TX", fontWeight = FontWeight.Black, fontSize = 17.sp, color = DashboardFondoConfig.ColorTextoPrimario)
                        Text("Destinos turísticos, miradores y paradores biker", fontSize = 11.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = DashboardFondoConfig.ColorTextoPrimario)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DashboardFondoConfig.ColorTarjetaClara)
            )
        },
        containerColor = DashboardFondoConfig.ColorFondoClaro
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar destino, mirador o playa...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MotoOrangePrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DashboardFondoConfig.ColorTarjetaClara,
                    unfocusedContainerColor = DashboardFondoConfig.ColorTarjetaClara
                ),
                singleLine = true
            )

            // Switch Táctico para Activar / Desactivar Sitios en el Mapa
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (mostrarEnMapa) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .clickable {
                        val nuevo = !mostrarEnMapa
                        mostrarEnMapa = nuevo
                        prefs.edit().putBoolean("mostrar_sitios_en_mapa", nuevo).apply()
                        GestorRadar.sincronizarSitiosInteresEnMapa(interestPoints, nuevo)
                        Toast.makeText(context, if (nuevo) "🏕️ Sitios turísticos activados en Mapa TX ✓" else "🏕️ Sitios turísticos ocultados del mapa", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (mostrarEnMapa) Color(0xFF0284C7) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mostrar Sitios Turísticos en Mapa TX",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (mostrarEnMapa) Color(0xFF0369A1) else Color(0xFF0F172A)
                            )
                            Text(
                                text = if (mostrarEnMapa) "Destinos, miradores y paradores biker visibles" else "Destinos ocultados del mapa táctico",
                                fontSize = 10.sp,
                                color = if (mostrarEnMapa) Color(0xFF0284C7) else Color(0xFF64748B)
                            )
                        }
                    }
                    Switch(
                        checked = mostrarEnMapa,
                        onCheckedChange = { nuevo ->
                            mostrarEnMapa = nuevo
                            prefs.edit().putBoolean("mostrar_sitios_en_mapa", nuevo).apply()
                            GestorRadar.sincronizarSitiosInteresEnMapa(interestPoints, nuevo)
                            Toast.makeText(context, if (nuevo) "🏕️ Sitios turísticos activados en Mapa TX ✓" else "🏕️ Sitios turísticos ocultados del mapa", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0284C7),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Chips de categorías
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(categoriesList) { cat ->
                    val isSel = (cat == "Todos" && selectedCategoryFilter == null) || selectedCategoryFilter == cat
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCategoryFilter = if (cat == "Todos") null else cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoOrangePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = DashboardFondoConfig.ColorTarjetaClara,
                            labelColor = DashboardFondoConfig.ColorTextoSecundario
                        )
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No se encontraron destinos o sitios registrados", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { spot ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                            border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MotoOrangePrimary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = spot.category.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotoOrangePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { reportSpotTarget = spot },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = "Denunciar sitio", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(text = spot.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)

                                if (spot.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = spot.description, fontSize = 12.sp, color = DashboardFondoConfig.ColorTextoSecundario, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }

                                if (!spot.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val model = SanitizadorImagenUrl.obtenerModelParaCoil(spot.imageUrl)
                                    SubcomposeAsyncImage(
                                        model = model,
                                        contentDescription = spot.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Botón Like
                                        val liked = currentMember != null && spot.likedByMemberIds.split(",").contains(currentMember.id.toString())
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { onLikeSpot(spot) }
                                        ) {
                                            Icon(
                                                Icons.Default.ThumbUp,
                                                contentDescription = "Me gusta",
                                                tint = if (liked) MotoOrangePrimary else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${spot.likesCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Botón Dislike
                                        val disliked = currentMember != null && spot.dislikedByMemberIds.split(",").contains(currentMember.id.toString())
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { onDislikeSpot(spot) }
                                        ) {
                                            Icon(
                                                Icons.Default.ThumbDown,
                                                contentDescription = "No me gusta",
                                                tint = if (disliked) StatusError else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${spot.dislikesCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Botón Navegar / Ver en Mapa TX
                                    Button(
                                        onClick = { onNavigateToMap(spot) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ver en Mapa TX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Denuncia
    reportSpotTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { reportSpotTarget = null },
            icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = StatusError, modifier = Modifier.size(32.dp)) },
            title = { Text("Denunciar Sitio TX", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Denunciar '${target.name}'. Explica el motivo para revisión de la Directiva:")
                    OutlinedTextField(
                        value = reportReasonText,
                        onValueChange = { reportReasonText = it },
                        label = { Text("Motivo del reporte") },
                        placeholder = { Text("Ej: Información falsa, sitio cerrado o peligroso...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReasonText.isNotBlank()) {
                            onSubmitReport(target.id, target.name, "SITIO_INTERES", reportReasonText.trim())
                            Toast.makeText(context, "🚨 Denuncia enviada a la Directiva", Toast.LENGTH_SHORT).show()
                            reportSpotTarget = null
                            reportReasonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Enviar Denuncia")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportSpotTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
