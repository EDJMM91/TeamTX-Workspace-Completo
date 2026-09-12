package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.teamtxvzla.R
import com.example.dashboard.DashboardFondoConfig
import com.example.data.local.AppDatabase
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.example.data.model.WorkshopDirectoryItem
import com.example.radar.GestorRadar
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopDirectoryScreen(
    workshops: List<WorkshopDirectoryItem>,
    interestPoints: List<com.example.data.model.BikerInterestPoint> = emptyList(),
    currentMember: MemberProfile?,
    onCreateWorkshop: (
        name: String,
        type: String,
        state: String,
        city: String,
        address: String,
        phone: String,
        whatsapp: String,
        rating: Double,
        notes: String,
        latitude: Double,
        longitude: Double,
        hasCredit: Boolean,
        creditPlatforms: String,
        googleMapsUrl: String
    ) -> Unit,
    onUpdateWorkshop: (item: WorkshopDirectoryItem) -> Unit,
    onDeleteWorkshop: (id: Long) -> Unit,
    onLikeSpot: (com.example.data.model.BikerInterestPoint) -> Unit = {},
    onDislikeSpot: (com.example.data.model.BikerInterestPoint) -> Unit = {},
    onSubmitReport: (spotId: Long, spotName: String, spotType: String, reason: String) -> Unit = { _, _, _, _ -> },
    onNavigateToMap: ((latitude: Double, longitude: Double, title: String) -> Unit)? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE) }
    var mostrarEnMapa by remember { mutableStateOf(prefs.getBoolean("mostrar_directorio_en_mapa", true)) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("TODOS") }
    var selectedType by remember { mutableStateOf("TODOS") }
    var onlyCreditFilter by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var workshopToEdit by remember { mutableStateOf<WorkshopDirectoryItem?>(null) }
    var workshopToDelete by remember { mutableStateOf<WorkshopDirectoryItem?>(null) }
    var workshopForCreditInfo by remember { mutableStateOf<WorkshopDirectoryItem?>(null) }
    var workshopToRate by remember { mutableStateOf<WorkshopDirectoryItem?>(null) }

    // Sincronización local respetando eliminaciones permanentes
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Seeding inicial manejado de forma única y persistente en WorkshopDirectorySync
                Log.d("TAG_COMERCIOS_TX", "Directorio cargado con ${workshops.size} talleres y ${interestPoints.size} sitios")
            } catch (_: Exception) {}
        }
    }

    // Combinar los talleres y sitios turísticos existentes en base de datos sin resucitar eliminados
    val combinedWorkshops = remember(workshops, interestPoints) {
        val map = workshops.associateBy { it.id }.toMutableMap()
        for (spot in interestPoints) {
            map[spot.id] = WorkshopDirectoryItem(
                id = spot.id,
                name = spot.name,
                type = spot.category,
                state = "Aragua",
                city = spot.address,
                address = spot.address,
                phone = spot.phone,
                whatsapp = spot.phone,
                rating = 5.0,
                recommendedBy = spot.addedBy,
                notes = spot.description,
                latitude = spot.latitude,
                longitude = spot.longitude,
                hasCredit = spot.description.contains("[CASHEA]") || spot.category.contains("Cashea", ignoreCase = true),
                creditPlatforms = if (spot.description.contains("[CASHEA]")) "Cashea" else "",
                imageUrl = spot.imageUrl,
                iconDrawableName = spot.iconDrawableName,
                timestamp = spot.timestamp
            )
        }
        map.values.toList().sortedBy { it.id }
    }

    // Sincronizar con el mapa cada vez que cambie la lista o el switch
    LaunchedEffect(combinedWorkshops, mostrarEnMapa) {
        GestorRadar.sincronizarDirectorioEnMapa(combinedWorkshops, mostrarEnMapa)
    }

    val isAuthorizedAdmin = currentMember?.isDirectiva == true ||
            currentMember?.role?.canManageApp == true ||
            currentMember?.role == MemberRole.PRESIDENTE ||
            currentMember?.role == MemberRole.DIRECTIVA ||
            currentMember?.role == MemberRole.DESARROLLADOR ||
            (currentMember?.role?.displayName?.contains("Admin", ignoreCase = true) == true) ||
            (currentMember?.role?.displayName?.contains("Directiv", ignoreCase = true) == true) ||
            (currentMember?.role?.displayName?.contains("Desarrollador", ignoreCase = true) == true)

    val states = listOf(
        "TODOS", "Aragua", "Carabobo", "Distrito Capital", "Miranda", "Lara", "Falcón",
        "Zulia", "Táchira", "Mérida", "Trujillo", "Guárico", "Anzoátegui", "Bolívar", "Yaracuy", "Portuguesa", "Barinas"
    )
    val types = listOf(
        "TODOS",
        "🏬 Comercios & Servicios",
        "🏕️ Sitios Turísticos & Ruta",
        "Mirador / Parador Biker",
        "Playa / Costa",
        "Montaña / Ruta",
        "Cascadas / Ríos / Pozos",
        "Monumento / Sitio Histórico",
        "Parque Nacional / Reserva Natural",
        "Camping / Pernocta",
        "Punto de Encuentro Caravana",
        "Venta de Repuestos TX",
        "Taller Mecánico",
        "Tienda de Accesorios",
        "Cauchera & Vulcanizadora",
        "Autolavado Motero",
        "Electricidad & Baterías",
        "Tornería & Soldadura",
        "Auxilio Vial 24H"
    )

    val filteredWorkshops = remember(combinedWorkshops, selectedState, selectedType, onlyCreditFilter, searchQuery) {
        combinedWorkshops.filter { w ->
            val isComercioGroup = w.type in listOf("Venta de Repuestos TX", "Taller Mecánico", "Tienda de Accesorios", "Cauchera & Vulcanizadora", "Autolavado Motero", "Electricidad & Baterías", "Tornería & Soldadura", "Auxilio Vial 24H", "Taller", "Repuestos", "Autolavado", "Restaurante / Comida", "Posada / Hotel", "Estación de Servicio")
            val matchState = selectedState == "TODOS" || w.state.equals(selectedState, ignoreCase = true)
            val matchType = when (selectedType) {
                "TODOS" -> true
                "🏬 Comercios & Servicios" -> isComercioGroup
                "🏕️ Sitios Turísticos & Ruta" -> !isComercioGroup
                else -> w.type.equals(selectedType, ignoreCase = true)
            }
            val matchCredit = !onlyCreditFilter || (w.hasCredit || w.creditPlatforms.isNotBlank())
            val matchQuery = searchQuery.isBlank() ||
                    w.name.contains(searchQuery, ignoreCase = true) ||
                    w.city.contains(searchQuery, ignoreCase = true) ||
                    w.address.contains(searchQuery, ignoreCase = true) ||
                    w.notes.contains(searchQuery, ignoreCase = true) ||
                    w.creditPlatforms.contains(searchQuery, ignoreCase = true) ||
                    w.type.contains(searchQuery, ignoreCase = true)
            matchState && matchType && matchCredit && matchQuery
        }
    }

    fun abrirFormularioUnificado(itemExistente: WorkshopDirectoryItem? = null) {
        val act = (context as? android.app.Activity) ?: return
        val app = act.applicationContext as? net.osmand.plus.OsmandApplication
        val loc = try { app?.locationProvider?.lastKnownLocation } catch (_: Exception) { null }
        val defaultLat = itemExistente?.latitude ?: loc?.latitude ?: 10.3541
        val defaultLon = itemExistente?.longitude ?: loc?.longitude ?: -67.6102

        val datos = if (itemExistente != null) {
            com.example.radar.DialogosMapaTx.DatosEdicionPunto(
                id = itemExistente.id,
                esEdicion = true,
                esComercio = true,
                nombre = itemExistente.name,
                categoria = itemExistente.type,
                descripcion = itemExistente.notes,
                direccion = itemExistente.address,
                telefono = itemExistente.phone.ifBlank { itemExistente.whatsapp },
                tieneCashea = itemExistente.hasCredit,
                imageUrl = itemExistente.imageUrl,
                iconoNombre = itemExistente.iconDrawableName
            )
        } else null

        com.example.radar.DialogosMapaTx.mostrarFormularioPuntoTX(
            activity = act,
            lat = defaultLat,
            lon = defaultLon,
            datosIniciales = datos
        ) { name, cat, desc, addr, latVal, lonVal, imageUri, phone, iconName ->
            val hasCasheaCredit = desc.contains("[CASHEA]") || cat.contains("Cashea", ignoreCase = true)
            if (itemExistente != null) {
                val updated = itemExistente.copy(
                    name = name,
                    type = cat,
                    address = addr,
                    phone = phone,
                    whatsapp = phone,
                    notes = desc.replace("[CASHEA]", "").trim(),
                    hasCredit = hasCasheaCredit,
                    creditPlatforms = if (hasCasheaCredit) "Cashea" else "",
                    latitude = latVal,
                    longitude = lonVal,
                    imageUrl = imageUri?.toString() ?: itemExistente.imageUrl,
                    iconDrawableName = iconName,
                    timestamp = System.currentTimeMillis()
                )
                onUpdateWorkshop(updated)
                Toast.makeText(context, "Establecimiento '$name' actualizado ✓", Toast.LENGTH_SHORT).show()
            } else {
                onCreateWorkshop(
                    name, cat, "Aragua", "Maracay", addr, phone, phone, 5.0, desc, latVal, lonVal, hasCasheaCredit, if (hasCasheaCredit) "Cashea" else "", ""
                )
                Toast.makeText(context, "Nuevo punto '$name' registrado ✓", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrangePrimary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MotoOrangePrimary),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌐", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Directorio Global TX", fontWeight = FontWeight.Black, fontSize = 16.sp, color = DashboardFondoConfig.ColorTextoPrimario)
                            Text("Comercios, servicios, miradores y destinos turísticos", fontSize = 11.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar al Dashboard", tint = DashboardFondoConfig.ColorTextoPrimario)
                    }
                },
                actions = {
                    IconButton(onClick = { abrirFormularioUnificado() }) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = "Registrar Punto de Interés o Comercio", tint = MotoOrangePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { abrirFormularioUnificado() },
                containerColor = TxFlameRed,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                text = { Text("Registrar Punto / Comercio", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = DashboardFondoConfig.ColorFondoClaro
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buscador libre dinámico en tema claro
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar tienda, repuesto, Cashea, Rapikom, ciudad...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MotoOrangePrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color(0xFF64748B))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MotoOrangePrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                ),
                singleLine = true
            )

            // Switch Interactivo: Mostrar Directorio en Mapa TX (Tema Claro)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (mostrarEnMapa) Color(0xFFE8F5E9) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (mostrarEnMapa) Color(0xFF81C784) else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .clickable {
                        val nuevo = !mostrarEnMapa
                        mostrarEnMapa = nuevo
                        prefs.edit().putBoolean("mostrar_directorio_en_mapa", nuevo).apply()
                        GestorRadar.sincronizarDirectorioEnMapa(workshops, nuevo)
                        Toast.makeText(context, if (nuevo) "🗺️ Directorio activado en Mapa TX ✓" else "🗺️ Directorio ocultado del mapa", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = if (mostrarEnMapa) Color(0xFF2E7D32) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mostrar Directorio en Mapa TX",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (mostrarEnMapa) Color(0xFF1B5E20) else Color(0xFF0F172A)
                            )
                            Text(
                                text = if (mostrarEnMapa) "Puntos tácticos con bandera y Cashea visibles" else "Puntos ocultados del mapa táctico",
                                fontSize = 10.sp,
                                color = if (mostrarEnMapa) Color(0xFF2E7D32) else Color(0xFF64748B)
                            )
                        }
                    }
                    Switch(
                        checked = mostrarEnMapa,
                        onCheckedChange = { nuevo ->
                            mostrarEnMapa = nuevo
                            prefs.edit().putBoolean("mostrar_directorio_en_mapa", nuevo).apply()
                            GestorRadar.sincronizarDirectorioEnMapa(workshops, nuevo)
                            Toast.makeText(context, if (nuevo) "🗺️ Directorio activado en Mapa TX ✓" else "🗺️ Directorio ocultado del mapa", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E7D32),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Selector rápido: Filtro de Crédito
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredWorkshops.size} comercios encontrados",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                FilterChip(
                    selected = onlyCreditFilter,
                    onClick = { onlyCreditFilter = !onlyCreditFilter },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💳 Con Crédito (Cashea/Rapikom)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2E7D32),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF1F5F9),
                        labelColor = Color(0xFF1B5E20)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = onlyCreditFilter,
                        borderColor = Color(0xFFCBD5E1),
                        selectedBorderColor = Color(0xFF2E7D32),
                        borderWidth = 1.dp
                    )
                )
            }

            // Selector de Estados
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(states) { state ->
                    val isSelected = selectedState == state
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedState = state },
                        label = { Text(state, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoOrangePrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF334155)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = MotoOrangePrimary,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            // Selector de Tipos de Servicio
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(types) { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(type, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoGoldSecondary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF334155)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = MotoGoldSecondary,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Lista de Servicios y Talleres
            if (filteredWorkshops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BuildCircle, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No se encontraron comercios con estos filtros", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Intenta buscar por otra palabra clave o desactiva los filtros.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredWorkshops, key = { it.id }) { workshop ->
                        CommercialServiceCard(
                            workshop = workshop,
                            isAuthorizedAdmin = isAuthorizedAdmin,
                            onEdit = { abrirFormularioUnificado(workshop) },
                            onDelete = { workshopToDelete = workshop },
                            onShowCreditInfo = { workshopForCreditInfo = workshop },
                            onRate = { workshopToRate = workshop },
                            onNavigateToMap = onNavigateToMap,
                            context = context
                        )
                    }
                }
            }
        }
    }

    // Modal para Calificar Establecimiento
    workshopToRate?.let { item ->
        RateCommercialServiceDialog(
            workshop = item,
            onDismiss = { workshopToRate = null },
            onConfirm = { newRating ->
                val updated = item.copy(rating = newRating)
                onUpdateWorkshop(updated)
                workshopToRate = null
                Toast.makeText(context, "¡Calificación de ${String.format(java.util.Locale.US, "%.1f", newRating)} ⭐ guardada con éxito! ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Explicativo de Plataformas de Crédito
    workshopForCreditInfo?.let { item ->
        CreditPlatformsInfoDialog(
            workshop = item,
            onDismiss = { workshopForCreditInfo = null },
            context = context
        )
    }

    // Modal Confirmación Eliminar (Tema Claro)
    workshopToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { workshopToDelete = null },
            title = { Text("¿Eliminar Establecimiento?", fontWeight = FontWeight.Black, color = Color(0xFF0F172A)) },
            text = { Text("¿Estás seguro de que deseas eliminar '${item.name}' del directorio del Team TX?", color = Color(0xFF475569)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWorkshop(item.id)
                        workshopToDelete = null
                        Toast.makeText(context, "Establecimiento eliminado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { workshopToDelete = null }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// TARJETA DE SERVICIO / COMERCIO MOTERO
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun CommercialServiceCard(
    workshop: WorkshopDirectoryItem,
    isAuthorizedAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowCreditInfo: () -> Unit,
    onRate: () -> Unit,
    onNavigateToMap: ((latitude: Double, longitude: Double, title: String) -> Unit)?,
    context: Context
) {
    val hasCredit = workshop.hasCredit || workshop.creditPlatforms.isNotBlank()
    val isTopRated = workshop.rating >= 4.5
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
        border = BorderStroke(
            1.dp,
            if (isTopRated) Color(0xFFFFB300)
            else if (hasCredit) Color(0xFF2E7D32)
            else DashboardFondoConfig.ColorBordeClaro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Fila Compacta Superior (Minimizada)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = workshop.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            color = DashboardFondoConfig.ColorTextoPrimario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isTopRated) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⭐", fontSize = 12.sp)
                        }
                        if (hasCredit && workshop.creditPlatforms.contains("Cashea", ignoreCase = true)) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Image(
                                painter = painterResource(id = R.drawable.logocashea),
                                contentDescription = "Cashea",
                                modifier = Modifier
                                    .height(18.dp)
                                    .width(28.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Text(
                        text = "📍 ${workshop.state} • ${workshop.city.ifBlank { workshop.type }}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashboardFondoConfig.ColorRojoCarrera,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge Calificación
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isTopRated) Color(0xFFFFF8E1) else Color(0xFFF1F5F9),
                        border = BorderStroke(0.8.dp, if (isTopRated) Color(0xFFFFB300) else DashboardFondoConfig.ColorBordeClaro),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRate() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", workshop.rating),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashboardFondoConfig.ColorTextoPrimario
                            )
                        }
                    }

                    // Pestaña/Chevron Expansor
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir tarjeta",
                        tint = DashboardFondoConfig.ColorRojoCarrera,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // CONTENIDO DESPLEGABLE (EXPANDIBLE)
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Etiquetas: Tipo de Negocio y Recomendación Top
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(0.5.dp, DashboardFondoConfig.ColorBordeClaro)
                        ) {
                            Text(
                                text = "🏷️ ${workshop.type}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashboardFondoConfig.ColorTextoPrimario,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (isTopRated) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF8E1),
                                border = BorderStroke(0.8.dp, Color(0xFFFFB300))
                            ) {
                                Text(
                                    text = "🏆 RECOMENDADO TEAM TX",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB28704),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // APARTADO DESTACADO: FINANCIAMIENTO / CRÉDITO
                    if (hasCredit) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val tieneCashea = workshop.creditPlatforms.contains("Cashea", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onShowCreditInfo() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (tieneCashea) {
                                        Image(
                                            painter = painterResource(id = R.drawable.logocashea),
                                            contentDescription = "Cashea",
                                            modifier = Modifier
                                                .height(22.dp)
                                                .width(32.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Text("💳", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Column {
                                        Text(
                                            text = "Crédito: ${workshop.creditPlatforms.ifBlank { "Cashea / Rapikom" }}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1B5E20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Toca para ver plataformas y condiciones",
                                            fontSize = 9.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Ver detalles de crédito",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (workshop.address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = DashboardFondoConfig.ColorTextoSecundario, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = workshop.address,
                                fontSize = 11.5.sp,
                                color = DashboardFondoConfig.ColorTextoPrimario
                            )
                        }
                    }

                    if (workshop.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔧 Especialidad: ${workshop.notes}",
                            fontSize = 11.sp,
                            color = DashboardFondoConfig.ColorTextoSecundario
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BOTONES DE ACCIÓN (AJUSTADOS Y SIN RECORTE DE TEXTO)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Llamar
                        if (workshop.phone.isNotBlank()) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${workshop.phone.replace(Regex("[^0-9+]"), "")}")))
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Llamar", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Botón WhatsApp
                        if (workshop.whatsapp.isNotBlank()) {
                            val clean = workshop.whatsapp.replace(Regex("[^0-9]"), "")
                            val formatted = if (clean.startsWith("0")) "58" + clean.substring(1) else clean
                            Button(
                                onClick = {
                                    try {
                                        val url = "https://wa.me/$formatted?text=Hola,%20te%20escribo%20desde%20la%20App%20Team%20Nacional%20TX%20Venezuela"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.1f).height(32.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("WhatsApp", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Botón Mapa TX / Navegación GPS por Voz
                        val tieneGps = workshop.latitude != 0.0 && workshop.longitude != 0.0
                        Button(
                            onClick = {
                                if (tieneGps) {
                                    com.example.rutas.GestorNavegacionOsmand.navegarADestino(
                                        context, workshop.latitude, workshop.longitude, workshop.name
                                    )
                                } else {
                                    Toast.makeText(context, "Este comercio no tiene coordenadas GPS cargadas", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (tieneGps) Color(0xFF0288D1) else Color(0xFF94A3B8)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Mapa TX", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Botón Compartir Universal
                        Button(
                            onClick = {
                                try {
                                    val shareText = "${workshop.name}\n📍 Ubicación: ${workshop.address}, ${workshop.city}, ${workshop.state}\n🏷️ Tipo: ${workshop.type}\n📞 Teléfono: ${workshop.phone}\n💬 WhatsApp: https://wa.me/${workshop.whatsapp.replace(Regex("[^0-9]"), "")}\n🗺️ Coordenadas: ${workshop.latitude}, ${workshop.longitude}\n🔧 Notas: ${workshop.notes}"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir Punto TX"))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Compartir", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Botón Edición / Borrado para Administradores
                        if (isAuthorizedAdmin) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusError, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: CALIFICAR COMERCIO / ESTABLECIMIENTO (TEMA CLARO)
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun RateCommercialServiceDialog(
    workshop: WorkshopDirectoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var selectedStars by remember { mutableStateOf(workshop.rating.toInt().coerceIn(1, 5)) }

    val feedbackText = when (selectedStars) {
        5 -> "⭐⭐⭐⭐⭐ ¡Excelente! Comercio 100% Recomendado por Pilotos TX"
        4 -> "⭐⭐⭐⭐ Muy Bueno, Gran Atención y Confianza"
        3 -> "⭐⭐⭐ Bueno y Aceptable"
        2 -> "⭐⭐ Regular / Atención Mejorable"
        else -> "⭐ No Recomendado"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⭐", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Calificar Establecimiento",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = workshop.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MotoOrangePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu puntuación ayuda a toda la comunidad del Team TX a encontrar los mejores servicios y repuestos.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de 5 estrellas interactivas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { selectedStars = i },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "$i Estrellas",
                                tint = if (i <= selectedStars) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedStars >= 4) Color(0xFFB45309) else Color(0xFF334155),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancelar", color = Color(0xFF64748B), fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onConfirm(selectedStars.toDouble()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text("Guardar ⭐", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: EXPLICACIÓN DE PLATAFORMAS DE CRÉDITO (TEMA CLARO)
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun CreditPlatformsInfoDialog(
    workshop: WorkshopDirectoryItem,
    onDismiss: () -> Unit,
    context: Context
) {
    val platforms = workshop.creditPlatforms.ifBlank { "Cashea / Rapikom" }
    val acceptsCashea = platforms.contains("Cashea", ignoreCase = true)
    val acceptsRapikom = platforms.contains("Rapikom", ignoreCase = true)
    val acceptsConvenio = platforms.contains("Convenio", ignoreCase = true) || platforms.contains("Team", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💳", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Opciones de Financiamiento", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text(workshop.name, fontSize = 12.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Este establecimiento ofrece opciones de crédito y facilidades de pago para repuestos, accesorios o mano de obra:",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                if (acceptsCashea) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🟢", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CASHEA", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1B5E20))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Compra tus repuestos pagando una inicial en tienda y el saldo restante en 3 cuotas cada 14 días sin intereses.",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                if (acceptsRapikom) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF3E8FF),
                        border = BorderStroke(1.dp, Color(0xFFC084FC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🟣", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RAPIKOM", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF6B21A8))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Financiamiento flexible por aplicación para repuestos mecánicos, cauchos y baterías en comercios afiliados.",
                                fontSize = 11.sp,
                                color = Color(0xFF7E22CE)
                            )
                        }
                    }
                }

                if (acceptsConvenio || (!acceptsCashea && !acceptsRapikom)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🟡", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONVENIO TEAM TX & DUEÑO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF92400E))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Descuento y facilidades exclusivas para miembros activos del Team Nacional TX Venezuela presentando su Carnet Digital en la app.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                if (workshop.phone.isNotBlank() || workshop.whatsapp.isNotBlank()) {
                    Text(
                        text = "💡 Recomendación: Consulta disponibilidad de tu línea de crédito antes de realizar tu compra.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White
    )
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: REGISTRAR COMERCIO O TALLER
// ═══════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCommercialServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: String,
        state: String,
        city: String,
        address: String,
        phone: String,
        whatsapp: String,
        rating: Double,
        notes: String,
        latitude: Double,
        longitude: Double,
        hasCredit: Boolean,
        creditPlatforms: String,
        googleMapsUrl: String
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Venta de Repuestos TX") }
    var state by remember { mutableStateOf("Aragua") }
    var city by remember { mutableStateOf("Maracay") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var hasCredit by remember { mutableStateOf(false) }
    var creditPlatforms by remember { mutableStateOf("") }
    var googleMapsUrl by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("10.2469") }
    var lngStr by remember { mutableStateOf("-67.5958") }

    val mapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        try {
            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clip.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            val coords = extraerCoordenadasDeTexto(text)
            if (coords != null) {
                latStr = coords.first.toString()
                lngStr = coords.second.toString()
                Toast.makeText(context, "📍 Coordenada tomada del mapa: ${coords.first}, ${coords.second} ✓", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {}
    }

    val states = listOf(
        "Aragua", "Carabobo", "Distrito Capital", "Miranda", "Lara", "Falcón", "Zulia",
        "Táchira", "Mérida", "Guárico", "Anzoátegui", "Bolívar", "Yaracuy", "Portuguesa", "Barinas"
    )
    val types = listOf(
        "Venta de Repuestos TX",
        "Taller Mecánico",
        "Tienda de Accesorios",
        "Cauchera & Vulcanizadora",
        "Autolavado Motero",
        "Electricidad & Baterías",
        "Tornería & Soldadura",
        "Auxilio Vial 24H"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🏪 Registrar Negocio en Directorio", fontWeight = FontWeight.Black, color = Color(0xFF0F172A)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Local / Taller *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Tipo de Servicio:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(types) { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("Estado") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Ciudad / Municipio") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Dirección Exacta") },
                        placeholder = { Text("Av. Principal, Sector, etc.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono") },
                            placeholder = { Text("0412-...") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp") },
                            placeholder = { Text("0412...") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = MotoGoldSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("¿Acepta Crédito? (Cashea, etc.)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Switch(
                                    checked = hasCredit,
                                    onCheckedChange = { hasCredit = it }
                                )
                            }
                            if (hasCredit) {
                                OutlinedTextField(
                                    value = creditPlatforms,
                                    onValueChange = { creditPlatforms = it },
                                    label = { Text("Plataformas / Convenios") },
                                    placeholder = { Text("Cashea / Rapikom / Convenio Team TX") },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = googleMapsUrl,
                        onValueChange = { googleMapsUrl = it },
                        label = { Text("Link de Google Maps") },
                        placeholder = { Text("https://maps.app.goo.gl/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Repuestos disponibles / Especialidad") },
                        placeholder = { Text("Ej: Kits de arrastre, cauchos, rectificación...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("📍 Ubicación Geográfica en Mapa TX:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(context, net.osmand.plus.activities.MapActivity::class.java)
                                Toast.makeText(context, "🗺️ Toca un punto en el mapa y copia sus coordenadas", Toast.LENGTH_LONG).show()
                                mapLauncher.launch(intent)
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp), tint = MotoOrangePrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("🗺️ Mapa TX", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = clip.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    if (text.startsWith("http")) {
                                        googleMapsUrl = text
                                    }
                                    val coords = extraerCoordenadasDeTexto(text)
                                    if (coords != null) {
                                        latStr = coords.first.toString()
                                        lngStr = coords.second.toString()
                                        Toast.makeText(context, "📍 Coordenadas extraídas: ${coords.first}, ${coords.second}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Link de Google Maps copiado ✓", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("📋 Pegar GPS", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val prefs = context.getSharedPreferences("prefs_radar_tx", Context.MODE_PRIVATE)
                                val lastLat = prefs.getString("last_lat", null)?.toDoubleOrNull()
                                val lastLon = prefs.getString("last_lon", null)?.toDoubleOrNull()
                                if (lastLat != null && lastLon != null && lastLat != 0.0) {
                                    latStr = lastLat.toString()
                                    lngStr = lastLon.toString()
                                    Toast.makeText(context, "📍 Posición GPS actual fijada ✓", Toast.LENGTH_SHORT).show()
                                } else {
                                    latStr = "10.2469"
                                    lngStr = "-67.5958"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📍 Mi GPS", fontSize = 10.sp)
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = latStr,
                            onValueChange = { latStr = it },
                            label = { Text("Latitud") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngStr,
                            onValueChange = { lngStr = it },
                            label = { Text("Longitud") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val lat = latStr.toDoubleOrNull() ?: 10.2469
                        val lng = lngStr.toDoubleOrNull() ?: -67.5958
                        onConfirm(name, type, state, city, address, phone, whatsapp, 5.0, notes, lat, lng, hasCredit, creditPlatforms, googleMapsUrl)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
            ) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: EDITAR COMERCIO O TALLER (ADMIN / DIRECTIVA)
// ═══════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCommercialServiceDialog(
    item: WorkshopDirectoryItem,
    onDismiss: () -> Unit,
    onConfirm: (WorkshopDirectoryItem) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val act = (context as? android.app.Activity)
        if (act != null) {
            val datos = com.example.radar.DialogosMapaTx.DatosEdicionPunto(
                id = item.id,
                esEdicion = true,
                esComercio = true,
                nombre = item.name,
                categoria = item.type,
                descripcion = item.notes,
                direccion = item.address,
                telefono = item.phone.ifBlank { item.whatsapp },
                tieneCashea = item.hasCredit,
                imageUrl = item.imageUrl,
                iconoNombre = item.iconDrawableName
            )
            com.example.radar.DialogosMapaTx.mostrarFormularioPuntoTX(
                activity = act,
                lat = item.latitude,
                lon = item.longitude,
                datosIniciales = datos
            ) { name, cat, desc, addr, latVal, lonVal, imageUri, phone, iconName ->
                val hasCasheaCredit = desc.contains("[CASHEA]") || cat.contains("Cashea", ignoreCase = true)
                val updated = item.copy(
                    name = name,
                    type = cat,
                    address = addr,
                    phone = phone,
                    whatsapp = phone,
                    notes = desc.replace("[CASHEA]", "").trim(),
                    hasCredit = hasCasheaCredit,
                    creditPlatforms = if (hasCasheaCredit) "Cashea" else "",
                    latitude = latVal,
                    longitude = lonVal,
                    imageUrl = imageUri?.toString() ?: item.imageUrl,
                    iconDrawableName = iconName,
                    timestamp = System.currentTimeMillis()
                )
                onConfirm(updated)
            }
        } else {
            onDismiss()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// COMPARTIR POR WHATSAPP
// ═══════════════════════════════════════════════════════════════════════
fun shareCommercialServiceViaWhatsApp(context: Context, workshop: WorkshopDirectoryItem) {
    val shareText = buildString {
        appendLine("🏪🏍️ RECOMENDACIÓN TEAM TX VENEZUELA 🏍️🏪")
        appendLine("✨ ${workshop.name} ✨")
        appendLine()
        appendLine("🏷️ Servicio: ${workshop.type}")
        appendLine("📍 Ubicación: ${workshop.address}, ${workshop.city}, Edo. ${workshop.state}")
        if (workshop.phone.isNotBlank()) appendLine("☎️ Teléfono: ${workshop.phone}")
        if (workshop.whatsapp.isNotBlank()) appendLine("💬 WhatsApp: https://wa.me/${workshop.whatsapp.replace(Regex("[^0-9]"), "")}")
        if (workshop.hasCredit || workshop.creditPlatforms.isNotBlank()) {
            appendLine("💳 Financiamiento: ${workshop.creditPlatforms.ifBlank { "Cashea / Rapikom" }}")
        }
        if (workshop.notes.isNotBlank()) appendLine("🔧 Especialidad: ${workshop.notes}")
        if (workshop.googleMapsUrl.isNotBlank()) {
            appendLine("🗺️ Google Maps: ${workshop.googleMapsUrl}")
        } else if (workshop.latitude != 0.0 && workshop.longitude != 0.0) {
            appendLine("🗺️ Coordenadas: https://maps.google.com/?q=${workshop.latitude},${workshop.longitude}")
        }
        appendLine()
        appendLine("¡Encuentra más talleres y repuestos en la App Oficial Team TX!")
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            setPackage("com.whatsapp")
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        try {
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(generalIntent, "Compartir Comercio"))
        } catch (ex: Exception) {
            Toast.makeText(context, "No se pudo compartir", Toast.LENGTH_SHORT).show()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// EXTRAER COORDENADAS GPS DE TEXTO O ENLACE
// ═══════════════════════════════════════════════════════════════════════
fun extraerCoordenadasDeTexto(texto: String): Pair<Double, Double>? {
    val regex = Regex("""[-+]?([1-8]?\d(\.\d+)?|90(\.0+)?),\s*[-+]?(180(\.0+)?|((1[0-7]\d)|([1-9]?\d))(\.\d+)?)""")
    val match = regex.find(texto)
    if (match != null) {
        val partes = match.value.split(",")
        if (partes.size >= 2) {
            val lat = partes[0].trim().toDoubleOrNull()
            val lon = partes[1].trim().toDoubleOrNull()
            if (lat != null && lon != null) return Pair(lat, lon)
        }
    }
    return null
}



