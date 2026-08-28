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
import com.example.ui.viewmodel.FinancialSummary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesScreen(
    summary: FinancialSummary,
    transactions: List<FinancialTransaction>,
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean,
    bcvRate: Double,
    onSetBcvRate: (Double) -> Unit,
    onSubmitPayment: (
        concept: String,
        description: String,
        category: PaymentCategory,
        amountUsd: Double,
        paymentMethod: PaymentMethod,
        referenceCode: String
    ) -> Unit,
    onSubmitExpense: (
        concept: String,
        description: String,
        category: PaymentCategory,
        amountUsd: Double,
        paymentMethod: PaymentMethod,
        referenceCode: String
    ) -> Unit,
    onUpdateTxStatus: (FinancialTransaction, PaymentStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<PaymentCategory?>(null) }

    val filteredList = remember(transactions, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) transactions
        else transactions.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDirectivaMode) {
                    SmallFloatingActionButton(
                        onClick = { showExpenseDialog = true },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_create_expense")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Registrar Gasto")
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = { showPaymentDialog = true },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    text = { Text("Reportar Pago / Aporte") },
                    containerColor = MotoOrangePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_create_payment")
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
            // Header Balance Cards
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141922)),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(StatusSuccess, MotoGoldSecondary))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TESORERÍA NACIONAL TRANSPARENTE",
                                color = MotoGoldSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF222B3A),
                                modifier = Modifier.testTag("btn_bcv_rate_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Tasa BCV: $bcvRate Bs/$", color = Color(0xFFB0BEC5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { showRateDialog = true },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = MotoGoldSecondary, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Net Balance Display
                        Text("Balance Total en Caja", color = Color(0xFF90A4AE), fontSize = 12.sp)
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", summary.netBalanceUsd)}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp
                            )
                            Text(
                                text = "USD",
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "(~${String.format(Locale.US, "%.2f", summary.netBalanceUsd * bcvRate)} Bs)",
                                color = Color(0xFFB0BEC5),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFF28303E))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini summary metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("INGRESOS", color = StatusSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("+$${String.format(Locale.US, "%.2f", summary.totalIncomeUsd)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("GASTOS", color = StatusError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("-$${String.format(Locale.US, "%.2f", summary.totalExpenseUsd)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("FONDO SOS", color = MotoOrangeLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("$${String.format(Locale.US, "%.2f", summary.emergencyFundUsd)}", color = MotoOrangeLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Historial de Movimientos (${filteredList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                            selected = selectedCategoryFilter == PaymentCategory.MEMBRESIA_MENSUAL,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == PaymentCategory.MEMBRESIA_MENSUAL) null else PaymentCategory.MEMBRESIA_MENSUAL
                            },
                            label = { Text("Aportes", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == PaymentCategory.POTE_EVENTO,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == PaymentCategory.POTE_EVENTO) null else PaymentCategory.POTE_EVENTO
                            },
                            label = { Text("Potes", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == PaymentCategory.FONDO_EMERGENCIA,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == PaymentCategory.FONDO_EMERGENCIA) null else PaymentCategory.FONDO_EMERGENCIA
                            },
                            label = { Text("Fondo SOS", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Transactions list
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay transacciones registradas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { tx ->
                    TransactionItemCard(
                        tx = tx,
                        isDirectivaMode = isDirectivaMode,
                        onStatusChange = { newStatus -> onUpdateTxStatus(tx, newStatus) }
                    )
                }
            }
        }
    }

    if (showPaymentDialog) {
        CreatePaymentDialog(
            currentMember = currentMember,
            bcvRate = bcvRate,
            onDismiss = { showPaymentDialog = false },
            onSubmit = { concept, desc, cat, usd, method, ref ->
                onSubmitPayment(concept, desc, cat, usd, method, ref)
                showPaymentDialog = false
            }
        )
    }

    if (showExpenseDialog) {
        CreateExpenseDialog(
            bcvRate = bcvRate,
            onDismiss = { showExpenseDialog = false },
            onSubmit = { concept, desc, cat, usd, method, ref ->
                onSubmitExpense(concept, desc, cat, usd, method, ref)
                showExpenseDialog = false
            }
        )
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = { Text("Actualizar Tasa BCV (Bs/$)") },
            text = {
                var rateText by remember { mutableStateOf(bcvRate.toString()) }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Tasa de Referencia") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRateDialog = false
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
fun TransactionItemCard(
    tx: FinancialTransaction,
    isDirectivaMode: Boolean,
    onStatusChange: (PaymentStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(tx.timestamp) {
        SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_card_${tx.id}")
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
                // Method Badge + Category
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (tx.paymentMethod) {
                            PaymentMethod.PAGO_MOVIL -> Color(0xFF0055D4).copy(alpha = 0.2f)
                            PaymentMethod.BINANCE_PAY -> Color(0xFFF3BA2F).copy(alpha = 0.2f)
                            PaymentMethod.ZELLE -> Color(0xFF7414CA).copy(alpha = 0.2f)
                            else -> MotoOrangePrimary.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = tx.paymentMethod.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (tx.paymentMethod) {
                                PaymentMethod.PAGO_MOVIL -> Color(0xFF448AFF)
                                PaymentMethod.BINANCE_PAY -> Color(0xFFF3BA2F)
                                PaymentMethod.ZELLE -> Color(0xFFB388FF)
                                else -> MotoOrangeLight
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = tx.category.label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Amount
                Text(
                    text = "${if (tx.type == TransactionType.INGRESO) "+" else "-"}$${String.format(Locale.US, "%.2f", tx.amountUsd)} USD",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (tx.type == TransactionType.INGRESO) StatusSuccess else StatusError
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Concept & Description
            Text(
                text = tx.concept,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (tx.description.isNotBlank()) {
                Text(
                    text = tx.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))

            // Member & Ref details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aportante: ${tx.memberName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ref: ${tx.referenceCode} • $dateStr",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (tx.status) {
                        PaymentStatus.VERIFICADO -> StatusSuccess.copy(alpha = 0.15f)
                        PaymentStatus.PENDIENTE -> StatusWarning.copy(alpha = 0.15f)
                        PaymentStatus.RECHAZADO -> StatusError.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = tx.status.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (tx.status) {
                            PaymentStatus.VERIFICADO -> StatusSuccess
                            PaymentStatus.PENDIENTE -> StatusWarning
                            PaymentStatus.RECHAZADO -> StatusError
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Directiva approval toggles if pending
            if (isDirectivaMode && tx.status == PaymentStatus.PENDIENTE) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onStatusChange(PaymentStatus.RECHAZADO) },
                        border = BorderStroke(1.dp, StatusError),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Rechazar", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onStatusChange(PaymentStatus.VERIFICADO) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Verificar ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePaymentDialog(
    currentMember: MemberProfile?,
    bcvRate: Double,
    onDismiss: () -> Unit,
    onSubmit: (
        concept: String,
        description: String,
        category: PaymentCategory,
        amountUsd: Double,
        paymentMethod: PaymentMethod,
        referenceCode: String
    ) -> Unit
) {
    var concept by remember { mutableStateOf("Aporte Voluntario Mensual") }
    var description by remember { mutableStateOf("Aporte voluntario de sostenimiento Team TX") }
    var category by remember { mutableStateOf(PaymentCategory.MEMBRESIA_MENSUAL) }
    var amountUsdStr by remember { mutableStateOf("5.0") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.PAGO_MOVIL) }
    var referenceCode by remember { mutableStateOf("") }

    val amountVesEstimated = remember(amountUsdStr, bcvRate) {
        val usd = amountUsdStr.toDoubleOrNull() ?: 0.0
        usd * bcvRate
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar Pago / Aporte", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text("Destino / Categoría:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            PaymentCategory.MEMBRESIA_MENSUAL,
                            PaymentCategory.POTE_EVENTO,
                            PaymentCategory.FONDO_EMERGENCIA
                        ).forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    if (cat == PaymentCategory.MEMBRESIA_MENSUAL) concept = "Aporte Voluntario"
                                    if (cat == PaymentCategory.POTE_EVENTO) concept = "Pote para Rodada"
                                    if (cat == PaymentCategory.FONDO_EMERGENCIA) concept = "Aporte Fondo SOS"
                                },
                                label = { Text(cat.label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = concept,
                        onValueChange = { concept = it },
                        label = { Text("Concepto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountUsdStr,
                            onValueChange = { amountUsdStr = it },
                            label = { Text("Monto USD ($)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_amount_usd")
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Equivalente Bs:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.US, "%.2f", amountVesEstimated)} Bs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                            }
                        }
                    }
                }
                item {
                    Text("Método de Pago:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            PaymentMethod.PAGO_MOVIL,
                            PaymentMethod.BINANCE_PAY,
                            PaymentMethod.ZELLE,
                            PaymentMethod.EFECTIVO_DIVISAS
                        ).forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method.label.split(" ")[0], fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = referenceCode,
                        onValueChange = { referenceCode = it },
                        label = { Text("Número de Referencia / Comprobante") },
                        placeholder = { Text("ej. Banesco 489201 o Hash Binance") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_payment_ref")
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Nota / Observación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val usd = amountUsdStr.toDoubleOrNull() ?: 0.0
                    if (concept.isNotBlank() && usd > 0 && referenceCode.isNotBlank()) {
                        onSubmit(concept, description, category, usd, paymentMethod, referenceCode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                modifier = Modifier.testTag("btn_confirm_submit_payment")
            ) {
                Text("Registrar Pago")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateExpenseDialog(
    bcvRate: Double,
    onDismiss: () -> Unit,
    onSubmit: (
        concept: String,
        description: String,
        category: PaymentCategory,
        amountUsd: Double,
        paymentMethod: PaymentMethod,
        referenceCode: String
    ) -> Unit
) {
    var concept by remember { mutableStateOf("Compra de Insumos / Repuestos") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PaymentCategory.COMPRA_REPUESTOS) }
    var amountUsdStr by remember { mutableStateOf("25.0") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.PAGO_MOVIL) }
    var referenceCode by remember { mutableStateOf("FACTURA-TX") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto Directiva", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = concept,
                        onValueChange = { concept = it },
                        label = { Text("Concepto del Gasto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = amountUsdStr,
                        onValueChange = { amountUsdStr = it },
                        label = { Text("Monto USD ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = referenceCode,
                        onValueChange = { referenceCode = it },
                        label = { Text("Factura / Comprobante") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Justificación del Gasto") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val usd = amountUsdStr.toDoubleOrNull() ?: 0.0
                    if (concept.isNotBlank() && usd > 0) {
                        onSubmit(concept, description, category, usd, paymentMethod, referenceCode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Registrar Egreso")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
