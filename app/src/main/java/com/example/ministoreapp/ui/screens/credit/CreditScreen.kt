package com.example.ministoreapp.ui.screens.credit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.CustomerEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.ui.components.CashierVerificationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditScreen(
    customers: List<CustomerEntity>,
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    products: List<ProductEntity>,
    onAddCustomer: (CustomerEntity, CashierEntity) -> Unit,
    onAddCredit: (CustomerEntity, Double, String, CashierEntity) -> Unit,
    onMakePayment: (CustomerEntity, Double, CashierEntity) -> Unit,
    getTransactionsForCustomerFlow: (String) -> kotlinx.coroutines.flow.Flow<List<CreditTransactionEntity>>
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForCreditAdd by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForPayment by remember { mutableStateOf<CustomerEntity?>(null) }

    val totalOutstandingCredit = customers.sumOf { it.balance }

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.nickname.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Total Outstanding Credit Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "₱${"%.2f".format(totalOutstandingCredit)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search & Add Customer Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer name / alias...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = { showAddCustomerDialog = true },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customer List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredCustomers, key = { it.id }) { customer ->
                CustomerCard(
                    customer = customer,
                    onCardClick = { selectedCustomerForLedger = customer },
                    onAddCreditClick = { customerForCreditAdd = customer },
                    onPayClick = { customerForPayment = customer }
                )
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            banRecords = banRecords,
            cashiers = cashiers,
            onDismiss = { showAddCustomerDialog = false },
            onAdd = { newCustomer, cashier ->
                onAddCustomer(newCustomer, cashier)
                showAddCustomerDialog = false
            }
        )
    }

    selectedCustomerForLedger?.let { customer ->
        val txns by getTransactionsForCustomerFlow(customer.id).collectAsState(initial = emptyList())
        CustomerLedgerDialog(
            customer = customer,
            transactions = txns,
            onDismiss = { selectedCustomerForLedger = null }
        )
    }

    customerForCreditAdd?.let { customer ->
        AddCreditDialog(
            customer = customer,
            cashiers = cashiers,
            products = products,
            onDismiss = { customerForCreditAdd = null },
            onAddCreditConfirmed = { amount, notes, cashier ->
                onAddCredit(customer, amount, notes, cashier)
                customerForCreditAdd = null
            }
        )
    }

    customerForPayment?.let { customer ->
        PaymentDialog(
            customer = customer,
            cashiers = cashiers,
            onDismiss = { customerForPayment = null },
            onPaymentConfirmed = { amount, cashier ->
                onMakePayment(customer, amount, cashier)
                customerForPayment = null
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    onCardClick: () -> Unit,
    onAddCreditClick: () -> Unit,
    onPayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (customer.nickname.isNotBlank()) {
                    Text(
                        text = "Alias: ${customer.nickname}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Balance: ₱${"%.2f".format(customer.balance)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (customer.balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(onClick = onAddCreditClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add Credit", tint = MaterialTheme.colorScheme.primary)
                }
                FilledTonalIconButton(onClick = onPayClick, enabled = customer.balance > 0) {
                    Icon(Icons.Default.Payment, contentDescription = "Pay", tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onAdd: (CustomerEntity, CashierEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var matchedBanRecord by remember { mutableStateOf<BanRecordEntity?>(null) }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    LaunchedEffect(name, nickname) {
        val match = banRecords.find {
            (name.isNotBlank() && it.fullName.contains(name, ignoreCase = true)) ||
            (nickname.isNotBlank() && it.nickname.isNotBlank() && it.nickname.contains(nickname, ignoreCase = true))
        }
        matchedBanRecord = match
    }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add New Customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    matchedBanRecord?.let { ban ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚠️ BAN ALERT: Matches Ban Record!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Name: ${ban.fullName} | Reason: ${ban.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Alias / Nickname") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify Cashier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Add Customer",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                onAdd(CustomerEntity(name = name, nickname = nickname), cashier)
            }
        )
    }
}

@Composable
fun CustomerLedgerDialog(
    customer: CustomerEntity,
    transactions: List<CreditTransactionEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${customer.name}'s Ledger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Current Balance: ₱${"%.2f".format(customer.balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (customer.balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (transactions.isEmpty()) {
                    Text("No credit transactions recorded yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(transactions, key = { it.id }) { txn ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = txn.type, fontWeight = FontWeight.Bold)
                                        Text(text = dateFormat.format(Date(txn.timestamp)), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(text = "Amount: ₱${"%.2f".format(txn.totalAmount)}", fontWeight = FontWeight.SemiBold)
                                    if (txn.amountTendered > 0) {
                                        Text(text = "Tendered: ₱${"%.2f".format(txn.amountTendered)} | Remaining: ₱${"%.2f".format(txn.remainingBalance)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (txn.notes.isNotBlank()) {
                                        Text(text = "Notes: ${txn.notes}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(text = "Cashier: ${txn.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Close") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun AddCreditDialog(
    customer: CustomerEntity,
    cashiers: List<CashierEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onAddCreditConfirmed: (Double, String, CashierEntity) -> Unit
) {
    var manualName by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var tenderedInput by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val totalAmount = amountInput.toDoubleOrNull() ?: 0.0
    val tendered = tenderedInput.toDoubleOrNull() ?: 0.0
    val netCreditAdded = if (tendered > 0) (totalAmount - tendered).coerceAtLeast(0.0) else totalAmount

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Credit for ${customer.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Description / Items") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Credit Amount (₱)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tenderedInput,
                        onValueChange = { tenderedInput = it },
                        label = { Text("Partial Tendered Payment (₱) - Optional") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tendered > 0) {
                        Text(
                            text = "Net Credit to Add: ₱${"%.2f".format(netCreditAdded)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = totalAmount > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify Cashier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier for Credit Addition",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val notes = if (manualName.isNotBlank()) manualName else "Credit Entry"
                onAddCreditConfirmed(netCreditAdded, notes, cashier)
            }
        )
    }
}

@Composable
fun PaymentDialog(
    customer: CustomerEntity,
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onPaymentConfirmed: (Double, CashierEntity) -> Unit
) {
    var isFullPayment by remember { mutableStateOf(true) }
    var partialAmountInput by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val paymentAmt = if (isFullPayment) customer.balance else (partialAmountInput.toDoubleOrNull() ?: 0.0)

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Settle Payment for ${customer.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Outstanding Balance: ₱${"%.2f".format(customer.balance)}", fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isFullPayment,
                            onClick = { isFullPayment = true },
                            label = { Text("Full Payment") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = !isFullPayment,
                            onClick = { isFullPayment = false },
                            label = { Text("Partial Payment") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (!isFullPayment) {
                        OutlinedTextField(
                            value = partialAmountInput,
                            onValueChange = { partialAmountInput = it },
                            label = { Text("Partial Payment Amount (₱)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = paymentAmt > 0 && paymentAmt <= customer.balance,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify Cashier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier for Payment",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                onPaymentConfirmed(paymentAmt, cashier)
            }
        )
    }
}
