package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    loans: List<EquipmentLoan>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    onRequestLoan: (item: InventoryItem, returnDate: String, purpose: String) -> Unit,
    onReturnLoan: (EquipmentLoan, List<InventoryItem>) -> Unit,
    onCreateItem: (
        code: String,
        name: String,
        category: ItemCategory,
        description: String,
        totalStock: Int,
        condition: ItemCondition,
        location: String,
        custodianName: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Inventario, 1: Préstamos Activos
    var showCreateItemDialog by remember { mutableStateOf(false) }
    var selectedItemForLoan by remember { mutableStateOf<InventoryItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<ItemCategory?>(null) }

    val filteredItems = remember(items, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) items
        else items.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        floatingActionButton = {
            if (isDirectivaMode) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateItemDialog = true },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                    text = { Text("Registrar Bien") },
                    containerColor = MotoOrangePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_inventory_item")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header summary
            item {
                Column {
                    Text(
                        text = "Inventario & Bienes del Club",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Herramientas de ruta, radios, insumos de emergencia y control de préstamos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Selector: Catálogo vs Préstamos
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MotoOrangePrimary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Catálogo de Bienes (${items.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            val activeCount = loans.count { it.status == LoanStatus.ACTIVO }
                            Text("Préstamos ($activeCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                // Category Filter Bar
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("Todos", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == ItemCategory.HERRAMIENTAS_RUTA,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == ItemCategory.HERRAMIENTAS_RUTA) null else ItemCategory.HERRAMIENTAS_RUTA
                            },
                            label = { Text("Herramientas", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == ItemCategory.PRIMEROS_AUXILIOS,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == ItemCategory.PRIMEROS_AUXILIOS) null else ItemCategory.PRIMEROS_AUXILIOS
                            },
                            label = { Text("Botiquín", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == ItemCategory.REPUESTOS_COMUNITARIOS,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == ItemCategory.REPUESTOS_COMUNITARIOS) null else ItemCategory.REPUESTOS_COMUNITARIOS
                            },
                            label = { Text("Repuestos", fontSize = 11.sp) }
                        )
                    }
                }

                if (filteredItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay artículos en esta categoría", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        InventoryCardItem(
                            item = item,
                            onRequestLoan = { selectedItemForLoan = item }
                        )
                    }
                }
            } else {
                // Loans list tab
                if (loans.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay préstamos activos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(loans, key = { it.id }) { loan ->
                        LoanCardItem(
                            loan = loan,
                            isDirectivaMode = isDirectivaMode,
                            onReturn = { onReturnLoan(loan, items) }
                        )
                    }
                }
            }
        }
    }

    if (selectedItemForLoan != null) {
        RequestLoanDialog(
            item = selectedItemForLoan!!,
            currentMember = currentMember,
            onDismiss = { selectedItemForLoan = null },
            onConfirm = { returnDate, purpose ->
                onRequestLoan(selectedItemForLoan!!, returnDate, purpose)
                selectedItemForLoan = null
            }
        )
    }

    if (showCreateItemDialog) {
        CreateItemDialog(
            onDismiss = { showCreateItemDialog = false },
            onCreate = { code, name, cat, desc, stock, cond, loc, cust ->
                onCreateItem(code, name, cat, desc, stock, cond, loc, cust)
                showCreateItemDialog = false
            }
        )
    }
}

@Composable
fun InventoryCardItem(
    item: InventoryItem,
    onRequestLoan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("inventory_item_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Code + Condition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MotoOrangePrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MotoOrangePrimary)
                ) {
                    Text(
                        text = item.code,
                        color = MotoOrangeLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (item.condition) {
                        ItemCondition.EXCELENTE -> StatusSuccess.copy(alpha = 0.15f)
                        ItemCondition.OPERATIVO -> StatusInfo.copy(alpha = 0.15f)
                        ItemCondition.MANTENIMIENTO -> StatusWarning.copy(alpha = 0.15f)
                        ItemCondition.DANADO -> StatusError.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = item.condition.label,
                        color = when (item.condition) {
                            ItemCondition.EXCELENTE -> StatusSuccess
                            ItemCondition.OPERATIVO -> StatusInfo
                            ItemCondition.MANTENIMIENTO -> StatusWarning
                            ItemCondition.DANADO -> StatusError
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Item Name & Description
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stock & Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("UBICACIÓN / CUSTODIA", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(item.location, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.availableStock > 0) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Stock:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${item.availableStock} / ${item.totalStock} disp.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.availableStock > 0) StatusSuccess else StatusError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Loan Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mantenimiento: ${item.lastMaintenanceDate}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onRequestLoan,
                    enabled = item.availableStock > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_request_loan_${item.id}")
                ) {
                    Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pedir Préstamo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoanCardItem(
    loan: EquipmentLoan,
    isDirectivaMode: Boolean,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (loan.status == LoanStatus.ACTIVO) MotoOrangePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("loan_card_${loan.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loan.itemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (loan.status == LoanStatus.ACTIVO) StatusWarning.copy(alpha = 0.2f) else StatusSuccess.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = loan.status.label,
                        color = if (loan.status == LoanStatus.ACTIVO) StatusWarning else StatusSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Responsable: ${loan.borrowerName} (${loan.borrowerMemberNumber})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("Motivo: ${loan.purpose}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Fecha de Préstamo: ${loan.loanDate} • Entrega Estimada: ${loan.expectedReturnDate}", fontSize = 11.sp, color = MotoOrangeLight)

            if (loan.status == LoanStatus.ACTIVO) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onReturn,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_return_loan_${loan.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Registrar Devolución", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestLoanDialog(
    item: InventoryItem,
    currentMember: MemberProfile?,
    onDismiss: () -> Unit,
    onConfirm: (returnDate: String, purpose: String) -> Unit
) {
    var returnDate by remember { mutableStateOf("En 3 días (Lunes)") }
    var purpose by remember { mutableStateOf("Mantenimiento preventivo moto antes de la rodada") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Préstamo de Equipo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = MotoOrangePrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Código: ${item.code} • Custodia actual: ${item.custodianName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = returnDate,
                    onValueChange = { returnDate = it },
                    label = { Text("Fecha Estimada de Devolución") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_loan_return_date")
                )

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Motivo / Uso del Equipo") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_loan_purpose")
                )

                Text(
                    text = "📋 Como miembro te comprometes a cuidar y devolver el equipo en perfecto estado funcional.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (returnDate.isNotBlank() && purpose.isNotBlank()) {
                        onConfirm(returnDate, purpose)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                modifier = Modifier.testTag("btn_confirm_request_loan")
            ) {
                Text("Confirmar Préstamo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CreateItemDialog(
    onDismiss: () -> Unit,
    onCreate: (
        code: String,
        name: String,
        category: ItemCategory,
        desc: String,
        stock: Int,
        condition: ItemCondition,
        location: String,
        custodian: String
    ) -> Unit
) {
    var code by remember { mutableStateOf("TX-INV-007") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCategory.HERRAMIENTAS_RUTA) }
    var desc by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("1") }
    var condition by remember { mutableStateOf(ItemCondition.EXCELENTE) }
    var location by remember { mutableStateOf("Sede Central Caracas") }
    var custodian by remember { mutableStateOf("Directiva Nacional") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Bien al Club", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Código") },
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Cantidad") },
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Bien / Herramienta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Descripción / Especificaciones") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Ubicación Física") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stock = stockStr.toIntOrNull() ?: 1
                    if (name.isNotBlank() && code.isNotBlank()) {
                        onCreate(code, name, category, desc, stock, condition, location, custodian)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Text("Guardar Bien")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
