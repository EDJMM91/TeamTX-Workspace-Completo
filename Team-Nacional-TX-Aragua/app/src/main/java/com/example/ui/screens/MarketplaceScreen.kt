package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MarketplaceItem
import com.example.data.model.MemberProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    items: List<MarketplaceItem>,
    currentMember: MemberProfile?,
    bcvRate: Double,
    onCreateItem: (
        title: String,
        description: String,
        category: String,
        priceUsd: Double,
        condition: String,
        imageUri: Uri?,
        phone: String,
        location: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onUpdateStatus: (item: MarketplaceItem, newStatus: String) -> Unit,
    onDeleteItem: (id: Long) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("TODOS") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val categories = listOf("TODOS", "Repuestos TX", "Indumentaria", "Motos", "Accesorios", "Varios")

    val filteredItems = remember(items, selectedCategory, searchQuery) {
        items.filter { item ->
            val matchCategory = selectedCategory == "TODOS" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true) ||
                    item.sellerLocation.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MotoOrangePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MERCADO MOTERO TX", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        if (bcvRate > 0) {
                            Text(
                                text = "Tasa Oficial BCV: Bs. ${String.format("%.2f", bcvRate)} / $",
                                fontSize = 11.sp,
                                color = TxGoldBrass,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TxCarbonDark
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MotoOrangePrimary,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                text = { Text("Publicar Artículo", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar repuestos, cascos, motos...", fontSize = 13.sp, color = TxSteelSilver) },
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MotoOrangePrimary,
                    unfocusedBorderColor = TxSteelSilver.copy(alpha = 0.3f),
                    focusedContainerColor = TxCarbonDark,
                    unfocusedContainerColor = TxCarbonDark
                ),
                singleLine = true
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MotoOrangePrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = TxCarbonDark,
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = TxSteelSilver, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No hay artículos en esta categoría", color = TxSteelSilver, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        MarketplaceItemCard(
                            item = item,
                            currentMember = currentMember,
                            bcvRate = bcvRate,
                            onUpdateStatus = { onUpdateStatus(item, it) },
                            onDelete = { onDeleteItem(item.id) },
                            context = context
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateMarketplaceItemDialog(
            currentMember = currentMember,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, cat, price, cond, uri, phone, loc ->
                onCreateItem(title, desc, cat, price, cond, uri, phone, loc) { success ->
                    if (success) showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun MarketplaceItemCard(
    item: MarketplaceItem,
    currentMember: MemberProfile?,
    bcvRate: Double,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit,
    context: Context
) {
    val isOwner = currentMember != null && item.sellerMemberId == currentMember.id
    val isDirectiva = currentMember?.isDirectiva == true
    val priceBs = if (bcvRate > 0) item.priceUsd * bcvRate else 0.0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TxCarbonDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.status == "VENDIDO") Color.Gray.copy(alpha = 0.3f) else MotoOrangePrimary.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Category, Condition, and Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MotoOrangePrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotoOrangePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TxCharcoalSurface
                    ) {
                        Text(
                            text = item.condition,
                            fontSize = 10.sp,
                            color = TxSteelSilver,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status Badge
                val statusColor = when (item.status) {
                    "DISPONIBLE" -> StatusSuccess
                    "RESERVADO" -> TxGoldBrass
                    else -> Color.Gray
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = item.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Image and Content Row
            Row(modifier = Modifier.fillMaxWidth()) {
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TxCharcoalSurface)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = TxSteelSilver,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Price Display
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$${String.format("%.2f", item.priceUsd)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = StatusSuccess
                        )
                        if (priceBs > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(Bs. ${String.format("%.2f", priceBs)})",
                                fontSize = 12.sp,
                                color = TxGoldBrass,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = TxSteelSilver.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Seller info & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vende: ${item.sellerName} (${item.sellerNickname})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Ubicación: ${item.sellerLocation}",
                        fontSize = 10.sp,
                        color = TxSteelSilver
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Contact WhatsApp
                    if (item.sellerPhone.isNotBlank() && item.status != "VENDIDO") {
                        val cleanPhone = item.sellerPhone.replace(Regex("[^0-9]"), "")
                        val formattedPhone = if (cleanPhone.startsWith("0")) "58" + cleanPhone.substring(1) else cleanPhone
                        IconButton(
                            onClick = {
                                val url = "https://wa.me/$formattedPhone?text=Hola%20${Uri.encode(item.sellerName)},%20te%20escribo%20desde%20la%20App%20Team%20TX%20por%20tu%20publicación:%20${Uri.encode(item.title)}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Call Button
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.sellerPhone}")))
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MotoOrangePrimary)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Owner Options Menu
                    if (isOwner || isDirectiva) {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(TxCarbonDark)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Marcar como Disponible") },
                                    onClick = { onUpdateStatus("DISPONIBLE"); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Marcar como Reservado") },
                                    onClick = { onUpdateStatus("RESERVADO"); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Marcar como Vendido") },
                                    onClick = { onUpdateStatus("VENDIDO"); showMenu = false }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Eliminar Publicación", color = TxFlameRed) },
                                    onClick = { onDelete(); showMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMarketplaceItemDialog(
    currentMember: MemberProfile?,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        category: String,
        priceUsd: Double,
        condition: String,
        imageUri: Uri?,
        phone: String,
        location: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Repuestos TX") }
    var priceStr by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Buen Estado") }
    var phone by remember { mutableStateOf(currentMember?.phone ?: "") }
    var location by remember { mutableStateOf(currentMember?.chapterState ?: "Aragua") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val categories = listOf("Repuestos TX", "Indumentaria", "Motos", "Accesorios", "Varios")
    val conditions = listOf("Nuevo", "Como Nuevo", "Buen Estado", "Para Reparar")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publicar en Mercado TX", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Image Picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TxCharcoalSurface)
                            .border(1.dp, TxSteelSilver.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Toca para agregar foto", fontSize = 12.sp, color = TxSteelSilver)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título del artículo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción / Detalles") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Precio en USD ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = StatusSuccess) }
                    )
                }

                item {
                    Text("Categoría:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Condición:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(conditions) { cond ->
                            FilterChip(
                                selected = condition == cond,
                                onClick = { condition = cond },
                                label = { Text(cond, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono / WhatsApp de contacto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Ciudad / Estado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && price > 0) {
                        isUploading = true
                        onConfirm(title, description, category, price, condition, selectedImageUri, phone, location)
                    }
                },
                enabled = title.isNotBlank() && (priceStr.toDoubleOrNull() ?: 0.0) > 0 && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Publicar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = TxCarbonDark
    )
}
