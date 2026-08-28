package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberProfile
import com.example.data.model.WorkshopDirectoryItem
import com.example.ui.theme.*

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
        longitude: Double
    ) -> Unit,
    onDeleteWorkshop: (id: Long) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedState by remember { mutableStateOf("TODOS") }
    var selectedType by remember { mutableStateOf("TODOS") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val states = listOf(
        "TODOS", "Aragua", "Carabobo", "Distrito Capital", "Miranda", "Lara", "Falcón",
        "Zulia", "Táchira", "Mérida", "Guárico", "Anzoátegui", "Bolívar", "Yaracuy", "Portuguesa", "Barinas"
    )
    val types = listOf(
        "TODOS",
        "Taller Mecánico",
        "Venta de Repuestos TX",
        "Cauchera & Vulcanizadora",
        "Autolavado Motero",
        "Electricidad & Baterías",
        "Tornería & Soldadura",
        "Tienda de Accesorios",
        "Auxilio Vial 24H"
    )

    val filteredWorkshops = remember(workshops, selectedState, selectedType, searchQuery) {
        workshops.filter { w ->
            val matchState = selectedState == "TODOS" || w.state.equals(selectedState, ignoreCase = true)
            val matchType = selectedType == "TODOS" || w.type.equals(selectedType, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    w.name.contains(searchQuery, ignoreCase = true) ||
                    w.city.contains(searchQuery, ignoreCase = true) ||
                    w.address.contains(searchQuery, ignoreCase = true) ||
                    w.notes.contains(searchQuery, ignoreCase = true) ||
                    w.type.contains(searchQuery, ignoreCase = true)
            matchState && matchType && matchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏪", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Guía de Servicios & Repuestos", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            Text("Talleres, Caucheras, Repuestos y Auxilio Vial", fontSize = 11.sp, color = MotoGoldSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TxCarbonDark)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = TxFlameRed,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                text = { Text("Registrar Negocio", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color.Black
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
                placeholder = { Text("Buscar taller, repuesto, cauchera, ciudad...", fontSize = 13.sp, color = Color(0xFF90A4AE)) },
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
                    unfocusedContainerColor = Color(0xFF1E2433)
                ),
                singleLine = true
            )

            // Selector de Estados
            Text("Filtrar por Estado:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
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
            Text("Tipo de Negocio / Servicio:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
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
                        Text("Intenta buscar por otra palabra clave o selecciona 'TODOS'.", color = Color(0xFF90A4AE), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredWorkshops, key = { it.id }) { workshop ->
                        CommercialServiceCard(
                            workshop = workshop,
                            currentMember = currentMember,
                            onDelete = { onDeleteWorkshop(workshop.id) },
                            context = context
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCommercialServiceDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, type, state, city, addr, ph, wa, rat, notes, lat, lng ->
                onCreateWorkshop(name, type, state, city, addr, ph, wa, rat, notes, lat, lng)
                showCreateDialog = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// TARJETA DE SERVICIO / COMERCIO MOTERO
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun CommercialServiceCard(
    workshop: WorkshopDirectoryItem,
    currentMember: MemberProfile?,
    onDelete: () -> Unit,
    context: Context
) {
    val isDirectiva = currentMember?.isDirectiva == true || currentMember?.role?.canManageApp == true || currentMember?.role == com.example.data.model.MemberRole.PRESIDENTE || currentMember?.role == com.example.data.model.MemberRole.DIRECTIVA

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E2433),
        border = BorderStroke(1.dp, Color(0xFF37474F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workshop.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${workshop.state} • ${workshop.city}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MotoOrangePrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MotoGoldSecondary.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, MotoGoldSecondary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MotoGoldSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", workshop.rating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotoGoldSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF131722),
                border = BorderStroke(0.5.dp, Color(0xFF263238))
            ) {
                Text(
                    text = "🏷️ ${workshop.type}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCFD8DC),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (workshop.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF90A4AE), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = workshop.address,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            if (workshop.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔧 Especialidad / Repuestos: ${workshop.notes}",
                    fontSize = 11.sp,
                    color = Color(0xFFB0BEC5)
                )
            }

            if (workshop.recommendedBy.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⭐ Recomendado por: ${workshop.recommendedBy}",
                    fontSize = 10.sp,
                    color = Color(0xFF81C784)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // FILA DE BOTONES DE ACCIÓN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Llamar
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

                    // WhatsApp
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

                    // Compartir WhatsApp
                    IconButton(
                        onClick = {
                            shareCommercialServiceViaWhatsApp(context, workshop)
                        },
                        modifier = Modifier.size(34.dp).background(Color(0xFF131722), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                    }

                    // Copiar Datos
                    IconButton(
                        onClick = {
                            val textToCopy = "${workshop.name}\n${workshop.type}\nUbicación: ${workshop.address}, ${workshop.city}, ${workshop.state}\nTlf: ${workshop.phone}\nWhatsApp: ${workshop.whatsapp}\nEspecialidad: ${workshop.notes}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Datos Comercio TX", textToCopy))
                            Toast.makeText(context, "Datos copiados al portapapeles ✓", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp).background(Color(0xFF131722), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                    }

                    // Mapa
                    if (workshop.latitude != 0.0 && workshop.longitude != 0.0) {
                        IconButton(
                            onClick = {
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${workshop.latitude},${workshop.longitude}?q=${workshop.latitude},${workshop.longitude}(${Uri.encode(workshop.name)})"))
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.size(34.dp).background(Color(0xFF131722), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Mapa", tint = Color(0xFF4FC3F7), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (isDirectiva) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = StatusError, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
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
        longitude: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Taller Mecánico") }
    var state by remember { mutableStateOf("Aragua") }
    var city by remember { mutableStateOf("Maracay") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("10.2469") }
    var lngStr by remember { mutableStateOf("-67.5958") }

    val states = listOf(
        "Aragua", "Carabobo", "Distrito Capital", "Miranda", "Lara", "Falcón", "Zulia",
        "Táchira", "Mérida", "Guárico", "Anzoátegui", "Bolívar", "Yaracuy", "Portuguesa", "Barinas"
    )
    val types = listOf(
        "Taller Mecánico",
        "Venta de Repuestos TX",
        "Cauchera & Vulcanizadora",
        "Autolavado Motero",
        "Electricidad & Baterías",
        "Tornería & Soldadura",
        "Tienda de Accesorios",
        "Auxilio Vial 24H"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Negocio / Taller", fontWeight = FontWeight.Black, color = Color.White) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Local / Negocio") },
                        placeholder = { Text("Ej: Repuestos & Taller Moto Power") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Tipo de Servicio:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(types) { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Estado:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(states) { s ->
                            FilterChip(
                                selected = state == s,
                                onClick = { state = s },
                                label = { Text(s, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Ciudad / Municipio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Dirección Exacta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp") },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val lat = latStr.toDoubleOrNull() ?: 10.2469
                        val lng = lngStr.toDoubleOrNull() ?: -67.5958
                        onConfirm(name, type, state, city, address, phone, whatsapp, 5.0, notes, lat, lng)
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
        if (workshop.notes.isNotBlank()) appendLine("🔧 Especialidad: ${workshop.notes}")
        if (workshop.latitude != 0.0 && workshop.longitude != 0.0) {
            appendLine("🗺️ Google Maps: https://maps.google.com/?q=${workshop.latitude},${workshop.longitude}")
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
