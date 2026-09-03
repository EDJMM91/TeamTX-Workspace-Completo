package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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

    // Auto-sembrado y sincronización local inmediata en Room
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(Dispatchers.IO))
                db.workshopDirectoryDao().upsertWorkshops(AppDatabase.INITIAL_WORKSHOPS)
            } catch (_: Exception) {}
        }
    }

    // Combinar siempre la lista inicial oficial de Excel con cualquier taller añadido manualmente por el usuario
    val combinedWorkshops = remember(workshops) {
        val map = AppDatabase.INITIAL_WORKSHOPS.associateBy { it.id }.toMutableMap()
        for (w in workshops) {
            map[w.id] = w
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
            (currentMember?.role?.displayName?.contains("Admin", ignoreCase = true) == true) ||
            (currentMember?.role?.displayName?.contains("Directiv", ignoreCase = true) == true)

    val states = listOf(
        "TODOS", "Aragua", "Carabobo", "Distrito Capital", "Miranda", "Lara", "Falcón",
        "Zulia", "Táchira", "Mérida", "Guárico", "Anzoátegui", "Bolívar", "Yaracuy", "Portuguesa", "Barinas"
    )
    val types = listOf(
        "TODOS",
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
            val matchState = selectedState == "TODOS" || w.state.equals(selectedState, ignoreCase = true)
            val matchType = selectedType == "TODOS" || w.type.equals(selectedType, ignoreCase = true)
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
                                Text("🏪", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Directorio Comercial & Servicios", fontWeight = FontWeight.Black, fontSize = 16.sp, color = DashboardFondoConfig.ColorTextoPrimario)
                            Text("Talleres, Repuestos, Autolavados y Auxilio Vial", fontSize = 11.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar al Dashboard", tint = DashboardFondoConfig.ColorTextoPrimario)
                    }
                },
                actions = {
                    if (isAuthorizedAdmin) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.AddBusiness, contentDescription = "Registrar Comercio", tint = MotoOrangePrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DashboardFondoConfig.ColorTarjetaClara)
            )
        },
        floatingActionButton = {
            if (isAuthorizedAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = TxFlameRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                    text = { Text("Registrar Negocio", fontWeight = FontWeight.Bold) }
                )
            }
        },
        containerColor = DashboardFondoConfig.ColorFondoClaro
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buscador libre dinámico
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar tienda, repuesto, Cashea, Rapikom, ciudad...", fontSize = 13.sp, color = Color(0xFF90A4AE)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MotoOrangePrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color(0xFF90A4AE))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MotoOrangePrimary,
                    unfocusedBorderColor = Color(0xFF37474F),
                    focusedContainerColor = Color(0xFF1E2433),
                    unfocusedContainerColor = Color(0xFF1E2433),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            // Switch Interactivo: Mostrar Directorio en Mapa TX
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (mostrarEnMapa) Color(0xFF0D2818) else Color(0xFF1C2230),
                border = BorderStroke(1.dp, if (mostrarEnMapa) Color(0xFF00E676) else Color(0xFF37474F)),
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
                            tint = if (mostrarEnMapa) Color(0xFF69F0AE) else Color(0xFF90A4AE),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mostrar Directorio en Mapa TX",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (mostrarEnMapa) Color.White else Color(0xFFCFD8DC)
                            )
                            Text(
                                text = if (mostrarEnMapa) "Puntos tácticos con bandera y Cashea visibles" else "Puntos ocultados del mapa táctico",
                                fontSize = 10.sp,
                                color = if (mostrarEnMapa) Color(0xFFB9F6CA) else Color(0xFF90A4AE)
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
                            uncheckedThumbColor = Color(0xFF90A4AE),
                            uncheckedTrackColor = Color(0xFF263238)
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
                    color = Color(0xFF90A4AE)
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
                        containerColor = Color(0xFF1E2433),
                        labelColor = Color(0xFF81C784)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = onlyCreditFilter,
                        borderColor = if (onlyCreditFilter) Color(0xFF4CAF50) else Color(0xFF37474F),
                        selectedBorderColor = Color(0xFF81C784),
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
                        label = { Text(state, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoOrangePrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E2433),
                            labelColor = Color.White
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
                        label = { Text(type, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoGoldSecondary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E2433),
                            labelColor = Color.White
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
                        Icon(Icons.Default.BuildCircle, contentDescription = null, tint = Color(0xFF607D8B), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No se encontraron comercios con estos filtros", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Intenta buscar por otra palabra clave o desactiva los filtros.", color = Color(0xFF90A4AE), fontSize = 12.sp)
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
                            onEdit = { workshopToEdit = workshop },
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

    // Modal para Registrar Negocio
    if (showCreateDialog) {
        CreateCommercialServiceDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, type, state, city, addr, ph, wa, rat, notes, lat, lng, hasCredit, creditPlatforms, gMapsUrl ->
                onCreateWorkshop(name, type, state, city, addr, ph, wa, rat, notes, lat, lng, hasCredit, creditPlatforms, gMapsUrl)
                showCreateDialog = false
            }
        )
    }

    // Modal para Editar Negocio
    workshopToEdit?.let { item ->
        EditCommercialServiceDialog(
            item = item,
            onDismiss = { workshopToEdit = null },
            onConfirm = { updated ->
                onUpdateWorkshop(updated)
                workshopToEdit = null
                Toast.makeText(context, "Establecimiento actualizado ✓", Toast.LENGTH_SHORT).show()
            }
        )
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

    // Modal Confirmación Eliminar
    workshopToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { workshopToDelete = null },
            title = { Text("¿Eliminar Establecimiento?", fontWeight = FontWeight.Black, color = Color.White) },
            text = { Text("¿Estás seguro de que deseas eliminar '${item.name}' del directorio del Team TX?", color = Color(0xFFCFD8DC)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWorkshop(item.id)
                        workshopToDelete = null
                        Toast.makeText(context, "Establecimiento eliminado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { workshopToDelete = null }) {
                    Text("Cancelar", color = Color(0xFF90A4AE))
                }
            },
            containerColor = Color(0xFF1E2433)
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
        border = BorderStroke(
            1.2.dp,
            if (isTopRated) Color(0xFFFFB300)
            else if (hasCredit) Color(0xFF2E7D32)
            else DashboardFondoConfig.ColorBordeClaro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila de Cabecera: Nombre y Puntuación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = workshop.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = DashboardFondoConfig.ColorTextoPrimario
                        )
                        if (isTopRated) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⭐", fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 ${workshop.state} • ${workshop.city}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashboardFondoConfig.ColorRojoCarrera
                    )
                }

                // Badge de Calificación (Clicable para calificar)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isTopRated) Color(0xFFFFF8E1) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isTopRated) Color(0xFFFFB300) else DashboardFondoConfig.ColorBordeClaro),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRate() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Calificación",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", workshop.rating)} ${if (isTopRated) "• TOP" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardFondoConfig.ColorTextoPrimario
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Fila de Etiquetas: Tipo de Negocio y Recomendación Top
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
                                        .height(24.dp)
                                        .width(36.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Text("💳", fontSize = 14.sp)
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
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = DashboardFondoConfig.ColorTextoSecundario, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = workshop.address,
                        fontSize = 12.sp,
                        color = DashboardFondoConfig.ColorTextoPrimario
                    )
                }
            }

            if (workshop.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔧 Especialidad / Stock: ${workshop.notes}",
                    fontSize = 11.sp,
                    color = DashboardFondoConfig.ColorTextoSecundario
                )
            }

            if (workshop.recommendedBy.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⭐ Recomendado por: ${workshop.recommendedBy}",
                    fontSize = 10.sp,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // FILA PRINCIPAL DE BOTONES DE ACCIÓN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Botón Llamar
                    if (workshop.phone.isNotBlank()) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${workshop.phone}")))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Llamar", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón WhatsApp
                    if (workshop.whatsapp.isNotBlank()) {
                        val clean = workshop.whatsapp.replace(Regex("[^0-9]"), "")
                        val formatted = if (clean.startsWith("0")) "58" + clean.substring(1) else clean
                        Button(
                            onClick = {
                                val url = "https://wa.me/$formatted?text=Hola,%20te%20escribo%20desde%20la%20App%20Team%20Nacional%20TX%20Venezuela"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón Ir en Mapa TX
                    if (workshop.latitude != 0.0 && workshop.longitude != 0.0) {
                        Button(
                            onClick = {
                                if (onNavigateToMap != null) {
                                    onNavigateToMap(workshop.latitude, workshop.longitude, workshop.name)
                                } else {
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${workshop.latitude},${workshop.longitude}?q=${workshop.latitude},${workshop.longitude}(${Uri.encode(workshop.name)})"))
                                    context.startActivity(mapIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mapa TX", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón Google Maps Web / App Externa
                    val gMapsUrl = if (workshop.googleMapsUrl.isNotBlank()) workshop.googleMapsUrl else if (workshop.latitude != 0.0) "https://maps.google.com/?q=${workshop.latitude},${workshop.longitude}" else ""
                    if (gMapsUrl.isNotBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(gMapsUrl)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(34.dp).background(Color(0xFF1E2433), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Public, contentDescription = "Google Maps", tint = Color(0xFF4FC3F7), modifier = Modifier.size(16.dp))
                        }
                    }

                    // Botón Calificar Establecimiento
                    IconButton(
                        onClick = onRate,
                        modifier = Modifier.size(34.dp).background(Color(0xFF332600), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Calificar Establecimiento", tint = Color(0xFFFFD700), modifier = Modifier.size(17.dp))
                    }

                    // Compartir
                    IconButton(
                        onClick = {
                            shareCommercialServiceViaWhatsApp(context, workshop)
                        },
                        modifier = Modifier.size(34.dp).background(Color(0xFF1E2433), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                    }

                    // Copiar Datos
                    IconButton(
                        onClick = {
                            val textToCopy = "${workshop.name}\n${workshop.type}\nUbicación: ${workshop.address}, ${workshop.city}, ${workshop.state}\nTlf: ${workshop.phone}\nWhatsApp: ${workshop.whatsapp}\nCalificación: ${workshop.rating} ⭐\nCrédito: ${if (hasCredit) workshop.creditPlatforms else "Contado"}\nEspecialidad: ${workshop.notes}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Datos Comercio TX", textToCopy))
                            Toast.makeText(context, "Datos copiados al portapapeles ✓", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp).background(Color(0xFF1E2433), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                    }
                }

                // ACCIONES DE GESTIÓN PARA DIRECTIVOS / ADMINISTRADORES
                if (isAuthorizedAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp).background(Color(0xFF263238), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MotoGoldSecondary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).background(Color(0xFF263238), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusError, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: CALIFICAR COMERCIO / ESTABLECIMIENTO
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
            color = Color(0xFF161B26),
            border = BorderStroke(1.2.dp, Color(0xFFFFD700)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF332600),
                    border = BorderStroke(1.dp, Color(0xFFFFD700)),
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
                    color = Color.White
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
                    color = Color(0xFF90A4AE),
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
                                tint = if (i <= selectedStars) Color(0xFFFFD700) else Color(0xFF455A64),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10141D),
                    border = BorderStroke(0.5.dp, Color(0xFF2A3644)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedStars >= 4) Color(0xFFFFD700) else Color(0xFFECEFF1),
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
                        border = BorderStroke(1.dp, Color(0xFF455A64))
                    ) {
                        Text("Cancelar", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onConfirm(selectedStars.toDouble()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                    ) {
                        Text("Guardar ⭐", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DIÁLOGO: EXPLICACIÓN DE PLATAFORMAS DE CRÉDITO
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
                    Text("Opciones de Financiamiento", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    Text(workshop.name, fontSize = 12.sp, color = MotoGoldSecondary)
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
                    color = Color(0xFFCFD8DC)
                )

                if (acceptsCashea) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F2B1D),
                        border = BorderStroke(1.dp, Color(0xFF00E676)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🟢", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CASHEA", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF69F0AE))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Compra tus repuestos pagando una inicial en tienda y el saldo restante en 3 cuotas cada 14 días sin intereses.",
                                fontSize = 11.sp,
                                color = Color(0xFFB9F6CA)
                            )
                        }
                    }
                }

                if (acceptsRapikom) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF261230),
                        border = BorderStroke(1.dp, Color(0xFFAB47BC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🟣", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RAPIKOM", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFCE93D8))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Financiamiento flexible por aplicación para repuestos mecánicos, cauchos y baterías en comercios afiliados.",
                                fontSize = 11.sp,
                                color = Color(0xFFE1BEE7)
                            )
                        }
                    }
                }

                if (acceptsConvenio || (!acceptsCashea && !acceptsRapikom)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2B2109),
                        border = BorderStroke(1.dp, MotoGoldSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🟡", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONVENIO TEAM TX & DUEÑO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MotoGoldSecondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Descuento y facilidades exclusivas para miembros activos del Team Nacional TX Venezuela presentando su Carnet Digital en la app.",
                                fontSize = 11.sp,
                                color = Color(0xFFFFECB3)
                            )
                        }
                    }
                }

                if (workshop.phone.isNotBlank() || workshop.whatsapp.isNotBlank()) {
                    Text(
                        text = "💡 Recomendación: Consulta disponibilidad de tu línea de crédito antes de realizar tu compra.",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
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
        containerColor = Color(0xFF1E2433)
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
        title = { Text("🏪 Registrar Negocio en Directorio", fontWeight = FontWeight.Black, color = Color.White) },
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2230)),
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
                                    Text("¿Acepta Crédito? (Cashea, etc.)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF90A4AE)) }
        },
        containerColor = Color(0xFF1E2433)
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
    var name by remember { mutableStateOf(item.name) }
    var type by remember { mutableStateOf(item.type) }
    var state by remember { mutableStateOf(item.state) }
    var city by remember { mutableStateOf(item.city) }
    var address by remember { mutableStateOf(item.address) }
    var phone by remember { mutableStateOf(item.phone) }
    var whatsapp by remember { mutableStateOf(item.whatsapp) }
    var notes by remember { mutableStateOf(item.notes) }
    var hasCredit by remember { mutableStateOf(item.hasCredit || item.creditPlatforms.isNotBlank()) }
    var creditPlatforms by remember { mutableStateOf(item.creditPlatforms) }
    var googleMapsUrl by remember { mutableStateOf(item.googleMapsUrl) }
    var rating by remember { mutableStateOf(item.rating) }
    var latStr by remember { mutableStateOf(item.latitude.toString()) }
    var lngStr by remember { mutableStateOf(item.longitude.toString()) }

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
        title = { Text("✏️ Editar Comercio / Taller", fontWeight = FontWeight.Black, color = Color.White) },
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2230)),
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
                                    Text("¿Acepta Crédito? (Cashea, etc.)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                    latStr = item.latitude.toString()
                                    lngStr = item.longitude.toString()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
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
                        val lat = latStr.toDoubleOrNull() ?: item.latitude
                        val lng = lngStr.toDoubleOrNull() ?: item.longitude
                        val updated = item.copy(
                            name = name,
                            type = type,
                            state = state,
                            city = city,
                            address = address,
                            phone = phone,
                            whatsapp = whatsapp,
                            notes = notes,
                            latitude = lat,
                            longitude = lng,
                            rating = rating,
                            hasCredit = hasCredit,
                            creditPlatforms = if (hasCredit) creditPlatforms else "",
                            googleMapsUrl = googleMapsUrl,
                            timestamp = System.currentTimeMillis()
                        )
                        onConfirm(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF90A4AE)) }
        },
        containerColor = Color(0xFF1E2433)
    )
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



