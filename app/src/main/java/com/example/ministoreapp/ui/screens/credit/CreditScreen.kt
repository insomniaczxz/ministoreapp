package com.example.ministoreapp.ui.screens.credit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.CustomerEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.ui.components.CashierVerificationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

fun getCreditAgeBadge(borrowedAt: Long, dueDate: Long?): String {
    val now = System.currentTimeMillis()
    val daysOld = if (borrowedAt > 0) ((now - borrowedAt) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0) else 0

    val ageText = when {
        daysOld >= 365 -> "🔴 ${daysOld / 365} yr+ old"
        daysOld >= 90 -> "🔴 3+ mos old (${daysOld}d)"
        daysOld >= 30 -> "🟠 ${daysOld / 30} mos old"
        else -> "${daysOld}d old"
    }

    val isOverdue = dueDate != null && now > dueDate
    return if (isOverdue) "⚠️ OVERDUE ($ageText)" else ageText
}

fun isExactBanMatch(checkName: String, checkNickname: String, banFullName: String, banNickname: String): Boolean {
    val nameClean = checkName.trim()
    val nickClean = checkNickname.trim()
    val banNameClean = banFullName.trim()
    val banNickClean = banNickname.trim()

    if (nameClean.isBlank() && nickClean.isBlank()) return false

    val nameExact = nameClean.equals(banNameClean, ignoreCase = true) ||
        (banNickClean.isNotBlank() && nameClean.equals(banNickClean, ignoreCase = true))

    val nickExact = nickClean.isNotBlank() && (
        nickClean.equals(banNickClean, ignoreCase = true) ||
        nickClean.equals(banNameClean, ignoreCase = true)
    )

    return nameExact || nickExact
}

fun isPotentialBanMatch(checkName: String, checkNickname: String, banFullName: String, banNickname: String): Boolean {
    val nameClean = checkName.trim()
    val nickClean = checkNickname.trim()
    val banNameClean = banFullName.trim()
    val banNickClean = banNickname.trim()

    if (nameClean.isBlank() && nickClean.isBlank()) return false

    val nameMatch = nameClean.length >= 2 && (
        banNameClean.contains(nameClean, ignoreCase = true) ||
        nameClean.contains(banNameClean, ignoreCase = true) ||
        (banNickClean.isNotBlank() && (banNickClean.contains(nameClean, ignoreCase = true) || nameClean.contains(banNickClean, ignoreCase = true)))
    )

    val nickMatch = nickClean.length >= 2 && (
        (banNickClean.isNotBlank() && (banNickClean.contains(nickClean, ignoreCase = true) || nickClean.contains(banNickClean, ignoreCase = true))) ||
        banNameClean.contains(nickClean, ignoreCase = true)
    )

    if (nameMatch || nickMatch) return true

    val searchWords = nameClean.lowercase().split("\\s+".toRegex()).filter { it.length >= 2 }
    val banWords = banNameClean.lowercase().split("\\s+".toRegex()).filter { it.length >= 2 }

    if (searchWords.isNotEmpty() && banWords.isNotEmpty()) {
        return searchWords.any { sWord ->
            banWords.any { bWord -> bWord.contains(sWord) || sWord.contains(bWord) }
        }
    }

    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditScreen(
    customers: List<CustomerEntity>,
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    products: List<ProductEntity>,
    onAddCustomer: (CustomerEntity, CashierEntity) -> Unit,
    onAddCredit: (CustomerEntity, Double, String, Long, CashierEntity) -> Unit,
    onMakePayment: (CustomerEntity, Double, CashierEntity) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit = { _, _ -> },
    getTransactionsForCustomerFlow: (String) -> kotlinx.coroutines.flow.Flow<List<CreditTransactionEntity>>
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterAgedOnly by remember { mutableStateOf(false) }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showGeneralAddCreditDialog by remember { mutableStateOf(false) }
    var showGeneralPaymentDialog by remember { mutableStateOf(false) }

    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForCreditAdd by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForPayment by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToBan by remember { mutableStateOf<CustomerEntity?>(null) }

    val totalOutstandingCredit = customers.sumOf { it.balance }
    val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000L
    val nowMs = System.currentTimeMillis()

    val aged90DaysCustomers = remember(customers, nowMs) {
        customers.filter { it.balance > 0 && it.borrowedAt > 0 && (nowMs - it.borrowedAt) >= ninetyDaysMs }
    }
    val agedTotalAmount = aged90DaysCustomers.sumOf { it.balance }

    val filteredCustomers = customers.filter { customer ->
        val matchesSearch = searchQuery.isBlank() ||
            customer.name.contains(searchQuery, ignoreCase = true) ||
            customer.nickname.contains(searchQuery, ignoreCase = true)
        val matchesAged = !filterAgedOnly || (customer.balance > 0 && customer.borrowedAt > 0 && (nowMs - customer.borrowedAt) >= ninetyDaysMs)
        matchesSearch && matchesAged
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Card styling copied from Events tab (Dynamic tint + outlineVariant)
    val amberCardBg = Color(0xFFF59E0B).copy(alpha = 0.12f)
    val amberTextColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val amberBorderColor = Color(0xFFF59E0B).copy(alpha = 0.5f)

    val redCardBg = Color(0xFFEF4444).copy(alpha = if (filterAgedOnly) 0.25f else 0.12f)
    val redTextColor = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    val redBorderColor = Color(0xFFEF4444).copy(alpha = 0.5f)

    val addCreditBtnBg = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    val settlePayBtnBg = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Total Outstanding Credit Balance
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = amberCardBg),
                border = BorderStroke(1.dp, amberBorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Total Credit Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = amberTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₱${totalOutstandingCredit.roundToInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = amberTextColor
                    )
                }
            }

            // Card 2: 3+ Month Aged Overdue Debts Summary
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { filterAgedOnly = !filterAgedOnly },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = redCardBg),
                border = BorderStroke(1.dp, redBorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🔴 3+ Mos Overdue (${aged90DaysCustomers.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = redTextColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₱${agedTotalAmount.roundToInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = redTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Action Buttons Row (Add Credit = Warning Red, Settle Pay = Success Green)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showGeneralAddCreditDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = addCreditBtnBg, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Credit", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showGeneralPaymentDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = settlePayBtnBg, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Settle Pay", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar (Full Width)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search name or alias...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Customer List
        if (filteredCustomers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (filterAgedOnly) "No 3+ Month overdue debts found." else "No customer credit records found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val matchingBan = banRecords.find { ban ->
                        isExactBanMatch(customer.name, customer.nickname, ban.fullName, ban.nickname)
                    }

                    CustomerCard(
                        customer = customer,
                        banRecord = matchingBan,
                        onCardClick = { selectedCustomerForLedger = customer },
                        onAddCreditClick = { customerForCreditAdd = customer },
                        onPayClick = { customerForPayment = customer },
                        onBanClick = { customerToBan = customer }
                    )
                }
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

    if (showGeneralAddCreditDialog) {
        UniversalAddCreditDialog(
            customers = customers,
            banRecords = banRecords,
            cashiers = cashiers,
            products = products,
            onDismiss = { showGeneralAddCreditDialog = false },
            onAddCreditConfirmed = { targetCustomer, amount, notes, borrowedAt, cashier ->
                onAddCredit(targetCustomer, amount, notes, borrowedAt, cashier)
                showGeneralAddCreditDialog = false
            },
            onRegisterAndAddCredit = { newCust, amount, notes, borrowedAt, cashier ->
                onAddCustomer(newCust, cashier)
                onAddCredit(newCust, amount, notes, borrowedAt, cashier)
                showGeneralAddCreditDialog = false
            }
        )
    }

    if (showGeneralPaymentDialog) {
        UniversalPaymentDialog(
            customers = customers.filter { it.balance > 0 },
            cashiers = cashiers,
            onDismiss = { showGeneralPaymentDialog = false },
            onPaymentConfirmed = { targetCustomer, amount, cashier ->
                onMakePayment(targetCustomer, amount, cashier)
                showGeneralPaymentDialog = false
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
            banRecords = banRecords,
            cashiers = cashiers,
            products = products,
            onDismiss = { customerForCreditAdd = null },
            onAddCreditConfirmed = { amount, notes, borrowedAt, cashier ->
                onAddCredit(customer, amount, notes, borrowedAt, cashier)
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

    customerToBan?.let { target ->
        var banReason by remember { mutableStateOf("Overdue Credit Debt") }
        var showVerificationDialog by remember { mutableStateOf(false) }

        if (!showVerificationDialog) {
            AlertDialog(
                onDismissRequest = { customerToBan = null },
                title = { Text("Ban Customer '${target.name}'", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Move customer to Ban List? They will be blocked from getting future credit.", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = banReason,
                            onValueChange = { banReason = it },
                            label = { Text("Ban Reason") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showVerificationDialog = true },
                        enabled = banReason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Ban", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { customerToBan = null }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            val effectiveCashiers = remember(cashiers) {
                if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
            }
            CashierVerificationDialog(
                cashiers = effectiveCashiers,
                actionTitle = "Verify Cashier to Ban '${target.name}'",
                onDismiss = { customerToBan = null },
                onVerified = { cashier ->
                    onSaveBanRecord(
                        BanRecordEntity(
                            fullName = target.name,
                            nickname = target.nickname,
                            reason = banReason,
                            cashierId = cashier.id,
                            cashierName = cashier.name
                        ),
                        cashier
                    )
                    customerToBan = null
                }
            )
        }
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    banRecord: BanRecordEntity?,
    onCardClick: () -> Unit,
    onAddCreditClick: () -> Unit,
    onPayClick: () -> Unit,
    onBanClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val nowMs = System.currentTimeMillis()
    val isAged90Days = customer.balance > 0 && customer.borrowedAt > 0 && (nowMs - customer.borrowedAt) >= (90L * 24 * 60 * 60 * 1000L)
    val ageBadge = getCreditAgeBadge(customer.borrowedAt, customer.dueDate)

    // Card styling copied from Events tab (surfaceContainerLow + outlineVariant)
    val cardBg = when {
        banRecord != null -> Color(0xFFEF4444).copy(alpha = 0.15f)
        isAged90Days -> Color(0xFFEF4444).copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val cardBorderColor = when {
        banRecord != null || isAged90Days -> Color(0xFFEF4444).copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val badgeBg = when {
        isAged90Days -> Color(0xFFEF4444)
        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
    }

    val badgeTextColor = when {
        isAged90Days -> Color.White
        else -> if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
    }

    val balanceTextColor = if (customer.balance > 0) {
        if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    } else {
        if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    }

    val actionBtnContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val addIconTint = if (banRecord == null) (if (isDark) Color(0xFF10B981) else Color(0xFF059669)) else MaterialTheme.colorScheme.outline
    val payIconTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val banIconTint = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (customer.balance > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = if (isAged90Days) "🔴 3+ MOS OLD" else ageBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (customer.nickname.isNotBlank()) {
                        Text(
                            text = "Alias: ${customer.nickname}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    banRecord?.let { ban ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🚫 BANNED: ${ban.reason}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Balance: ₱${customer.balance.roundToInt()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = balanceTextColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = onAddCreditClick,
                        enabled = banRecord == null,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = actionBtnContainer)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Credit", tint = addIconTint)
                    }
                    FilledTonalIconButton(
                        onClick = onPayClick,
                        enabled = customer.balance > 0,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = actionBtnContainer)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = "Pay", tint = payIconTint)
                    }
                    if (banRecord == null) {
                        IconButton(onClick = onBanClick) {
                            Icon(Icons.Default.Block, contentDescription = "Ban Customer", tint = banIconTint)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalAddCreditDialog(
    customers: List<CustomerEntity>,
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onAddCreditConfirmed: (CustomerEntity, Double, String, Long, CashierEntity) -> Unit,
    onRegisterAndAddCredit: (CustomerEntity, Double, String, Long, CashierEntity) -> Unit
) {
    var customerSearchText by remember { mutableStateOf(customers.firstOrNull()?.name ?: "") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(customers.firstOrNull()) }
    var expandedCustomerDropdown by remember { mutableStateOf(false) }

    var newNicknameInput by remember { mutableStateOf("") }

    // Manual Items
    var manualDescInput by remember { mutableStateOf("") }
    var manualAmountInput by remember { mutableStateOf("") }
    val manualItemLines = remember { mutableStateListOf<Pair<String, Double>>() }

    // Products Picker
    var productSearchText by remember { mutableStateOf("") }
    var expandedProductDropdown by remember { mutableStateOf(false) }
    var selectedProductForAdd by remember { mutableStateOf<ProductEntity?>(null) }
    var productQtyInput by remember { mutableStateOf("1") }
    val productItemLines = remember { mutableStateListOf<Pair<ProductEntity, Int>>() }

    // Direct Amount & Tendered
    var directAmountInput by remember { mutableStateOf("") }
    var tenderedInput by remember { mutableStateOf("") }

    // Backdate
    var borrowedAtTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    // Computations
    val manualItemsTotal = manualItemLines.sumOf { it.second }
    val productItemsTotal = productItemLines.sumOf { it.first.sellingPrice * it.second }
    val directAmount = directAmountInput.toDoubleOrNull() ?: 0.0
    val grossCreditTotal = manualItemsTotal + productItemsTotal + directAmount

    val tenderedAmount = tenderedInput.toDoubleOrNull() ?: 0.0
    val netCreditAdded = if (tenderedAmount > 0) (grossCreditTotal - tenderedAmount).coerceAtLeast(0.0) else grossCreditTotal

    val matchingCustomers = remember(customerSearchText, customers) {
        if (customerSearchText.isBlank()) customers else customers.filter {
            it.name.contains(customerSearchText, ignoreCase = true) || it.nickname.contains(customerSearchText, ignoreCase = true)
        }
    }

    val isNewCustomerRegistration = selectedCustomer == null && customerSearchText.isNotBlank()

    val exactBan = remember(customerSearchText, newNicknameInput, selectedCustomer, banRecords) {
        val checkName = selectedCustomer?.name ?: customerSearchText
        val checkNick = selectedCustomer?.nickname ?: newNicknameInput
        banRecords.find { ban ->
            isExactBanMatch(checkName, checkNick, ban.fullName, ban.nickname)
        }
    }

    val potentialBan = remember(customerSearchText, newNicknameInput, selectedCustomer, banRecords) {
        if (exactBan != null) return@remember null
        val checkName = selectedCustomer?.name ?: customerSearchText
        val checkNick = selectedCustomer?.nickname ?: newNicknameInput
        banRecords.find { ban ->
            isPotentialBanMatch(checkName, checkNick, ban.fullName, ban.nickname)
        }
    }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Credit / Utang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    exactBan?.let { ban ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🚫 BAN ALERT: Exact Banned Customer Match!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Name: ${ban.fullName} | Reason: ${ban.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Cannot issue credit to banned customers.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    potentialBan?.let { ban ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚠️ POTENTIAL BAN MATCH WARNING",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Partial name match with banned record '${ban.fullName}' (Reason: ${ban.reason}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Verify customer identity. You may proceed if this is a different person.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    // Customer AutoComplete Field (Rule 2: Smart Customer Search + Dynamic Add)
                    ExposedDropdownMenuBox(
                        expanded = expandedCustomerDropdown,
                        onExpandedChange = { expandedCustomerDropdown = !expandedCustomerDropdown }
                    ) {
                        OutlinedTextField(
                            value = customerSearchText,
                            onValueChange = {
                                customerSearchText = it
                                val exactMatch = customers.find { c -> c.name.equals(it, ignoreCase = true) }
                                selectedCustomer = exactMatch
                                expandedCustomerDropdown = true
                            },
                            label = { Text("Customer Name / Alias (Type or Select)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomerDropdown) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )

                        if (matchingCustomers.isNotEmpty() || (customerSearchText.isNotBlank() && customers.none { it.name.equals(customerSearchText, ignoreCase = true) })) {
                            ExposedDropdownMenu(
                                expanded = expandedCustomerDropdown,
                                onDismissRequest = { expandedCustomerDropdown = false }
                            ) {
                                matchingCustomers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.name}${if (cust.nickname.isNotBlank()) " (${cust.nickname})" else ""}") },
                                        onClick = {
                                            selectedCustomer = cust
                                            customerSearchText = cust.name
                                            expandedCustomerDropdown = false
                                        }
                                    )
                                }

                                if (customerSearchText.isNotBlank() && customers.none { it.name.equals(customerSearchText, ignoreCase = true) }) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "➕ Add New Customer: \"${customerSearchText}\"",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {
                                            selectedCustomer = null
                                            expandedCustomerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isNewCustomerRegistration) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "✨ New Customer '${customerSearchText}' will be registered automatically",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        OutlinedTextField(
                            value = newNicknameInput,
                            onValueChange = { newNicknameInput = it },
                            label = { Text("Alias / Nickname (Optional)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // Products Selection (Full-Width Search + Trailing Qty/Add Row)
                    Text("Select Products from Store Inventory:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    val matchingProducts = remember(productSearchText, products) {
                        if (productSearchText.isBlank()) products else products.filter {
                            it.name.contains(productSearchText, ignoreCase = true) ||
                            it.brand.contains(productSearchText, ignoreCase = true) ||
                            it.category.contains(productSearchText, ignoreCase = true)
                        }
                    }

                    // 1. Full-Width Search Dropdown Bar
                    ExposedDropdownMenuBox(
                        expanded = expandedProductDropdown,
                        onExpandedChange = { expandedProductDropdown = !expandedProductDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedProductForAdd?.name ?: productSearchText,
                            onValueChange = {
                                productSearchText = it
                                selectedProductForAdd = null
                                expandedProductDropdown = true
                            },
                            placeholder = { Text("Search product name, brand...", style = MaterialTheme.typography.bodyMedium) },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductDropdown) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )

                        if (matchingProducts.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = expandedProductDropdown,
                                onDismissRequest = { expandedProductDropdown = false },
                                modifier = Modifier.heightIn(max = 250.dp)
                            ) {
                                matchingProducts.take(15).forEach { prod ->
                                    val variantDetail = buildList {
                                        if (prod.brand.isNotBlank()) add(prod.brand)
                                        val vt = prod.variantType.orEmpty()
                                        if (vt.isNotBlank() && vt != "RETAIL") add(vt.replace("_", " "))
                                        if (prod.unit.isNotBlank()) add("1 ${prod.unit}")
                                    }.joinToString(" • ").ifBlank { "Store Item" }

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = variantDetail,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "₱${prod.sellingPrice.roundToInt()}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedProductForAdd = prod
                                            productSearchText = prod.name
                                            expandedProductDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. 50/50 Row for Quantity & Add Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = productQtyInput,
                            onValueChange = { productQtyInput = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                selectedProductForAdd?.let { prod ->
                                    val qty = productQtyInput.toIntOrNull() ?: 1
                                    if (qty > 0) {
                                        productItemLines.add(Pair(prod, qty))
                                        selectedProductForAdd = null
                                        productSearchText = ""
                                        productQtyInput = "1"
                                    }
                                }
                            },
                            enabled = selectedProductForAdd != null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Product Lines List
                    if (productItemLines.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            productItemLines.forEachIndexed { idx, pair ->
                                val (prod, qty) = pair
                                val itemTotal = prod.sellingPrice * qty
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${prod.name} x$qty", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text("₱${itemTotal.roundToInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { productItemLines.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Block, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // Manual Items (Old Notebook Records)
                    Text("Manual Items & Historical Record Amounts:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    // Description Field on its own full-width line
                    OutlinedTextField(
                        value = manualDescInput,
                        onValueChange = { manualDescInput = it },
                        label = { Text("Item Description") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 50/50 Row for Amount and Add Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualAmountInput,
                            onValueChange = { manualAmountInput = it },
                            label = { Text("Amount (₱)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val amt = manualAmountInput.toDoubleOrNull() ?: 0.0
                                if (manualDescInput.isNotBlank() && amt > 0) {
                                    manualItemLines.add(Pair(manualDescInput, amt))
                                    manualDescInput = ""
                                    manualAmountInput = ""
                                }
                            },
                            enabled = manualDescInput.isNotBlank() && (manualAmountInput.toDoubleOrNull() ?: 0.0) > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Manual Item", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Manual", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Manual Item Lines List
                    if (manualItemLines.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            manualItemLines.forEachIndexed { idx, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${pair.first}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text("₱${pair.second.roundToInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { manualItemLines.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Block, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tenderedInput,
                        onValueChange = { tenderedInput = it },
                        label = { Text("Partial Tendered Payment (₱) - Optional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // Date Selection (Rule 3: Clean Single Line Date Selector)
                    var showDatePickerDialog by remember { mutableStateOf(false) }
                    val displayDateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

                    OutlinedButton(
                        onClick = { showDatePickerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = "Date", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Date: ${displayDateFormat.format(Date(borrowedAtTimestamp))}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Change",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showDatePickerDialog) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = borrowedAtTimestamp
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePickerDialog = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            borrowedAtTimestamp = it
                                        }
                                        showDatePickerDialog = false
                                    }
                                ) {
                                    Text("OK", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePickerDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    // Auto Calculated Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Auto-Calculated Total Credit Summary:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Items Gross Total: ₱${grossCreditTotal.roundToInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (tenderedAmount > 0) {
                                Text("Tendered Payment: -₱${tenderedAmount.roundToInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Text("Net Credit to Save: ₱${netCreditAdded.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = grossCreditTotal > 0 && exactBan == null && customerSearchText.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
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
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Issue Credit",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val combinedDetails = buildList {
                    productItemLines.forEach { add("${it.first.name} x${it.second}") }
                    manualItemLines.forEach { add(it.first) }
                }.joinToString(", ").ifBlank { "Credit Entry" }

                val targetCust = selectedCustomer ?: CustomerEntity(
                    id = UUID.randomUUID().toString(),
                    name = customerSearchText,
                    nickname = newNicknameInput,
                    balance = 0.0,
                    borrowedAt = borrowedAtTimestamp
                )

                if (selectedCustomer != null) {
                    onAddCreditConfirmed(targetCust, netCreditAdded, combinedDetails, borrowedAtTimestamp, cashier)
                } else {
                    onRegisterAndAddCredit(targetCust, netCreditAdded, combinedDetails, borrowedAtTimestamp, cashier)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalPaymentDialog(
    customers: List<CustomerEntity>,
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onPaymentConfirmed: (CustomerEntity, Double, CashierEntity) -> Unit
) {
    var customerSearchText by remember { mutableStateOf(customers.firstOrNull()?.name ?: "") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(customers.firstOrNull()) }
    var isFullPayment by remember { mutableStateOf(true) }
    var partialAmountInput by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val currentBal = selectedCustomer?.balance ?: 0.0
    val paymentAmt = if (isFullPayment) currentBal else (partialAmountInput.toDoubleOrNull() ?: 0.0)

    val matchingCustomers = remember(customerSearchText, customers) {
        if (customerSearchText.isBlank()) customers else customers.filter {
            it.name.contains(customerSearchText, ignoreCase = true) || it.nickname.contains(customerSearchText, ignoreCase = true)
        }
    }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Settle Payment (Bayad)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (customers.isEmpty()) {
                        Text("No customers currently have an active credit balance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        var expandedDropdown by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = !expandedDropdown }
                        ) {
                            OutlinedTextField(
                                value = customerSearchText,
                                onValueChange = {
                                    customerSearchText = it
                                    val exact = customers.find { c -> c.name.equals(it, ignoreCase = true) }
                                    if (exact != null) selectedCustomer = exact
                                    expandedDropdown = true
                                },
                                label = { Text("Customer Name / Alias (Type or Select)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                                    .fillMaxWidth()
                            )

                            if (matchingCustomers.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    matchingCustomers.forEach { cust ->
                                        DropdownMenuItem(
                                            text = { Text("${cust.name} (Balance: ₱${cust.balance.roundToInt()})") },
                                            onClick = {
                                                selectedCustomer = cust
                                                customerSearchText = cust.name
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text("Outstanding Balance: ₱${currentBal.roundToInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

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
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = selectedCustomer != null && paymentAmt > 0 && paymentAmt <= currentBal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White)
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
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier for Payment",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                selectedCustomer?.let { cust ->
                    onPaymentConfirmed(cust, paymentAmt, cashier)
                }
            }
        )
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
        val match = banRecords.find { ban ->
            isExactBanMatch(name, nickname, ban.fullName, ban.nickname)
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
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
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
                    text = "Current Balance: ₱${customer.balance.roundToInt()}",
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
                                    Text(text = "Amount: ₱${txn.totalAmount.roundToInt()}", fontWeight = FontWeight.SemiBold)
                                    if (txn.amountTendered > 0) {
                                        Text(text = "Tendered: ₱${txn.amountTendered.roundToInt()} | Remaining: ₱${txn.remainingBalance.roundToInt()}", style = MaterialTheme.typography.bodySmall)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCreditDialog(
    customer: CustomerEntity,
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onAddCreditConfirmed: (Double, String, Long, CashierEntity) -> Unit
) {
    var manualName by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var tenderedInput by remember { mutableStateOf("") }
    var selectedBackdateChip by remember { mutableStateOf("TODAY") }
    var borrowedAtTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val totalAmount = amountInput.toDoubleOrNull() ?: 0.0
    val tendered = tenderedInput.toDoubleOrNull() ?: 0.0
    val netCreditAdded = if (tendered > 0) (totalAmount - tendered).coerceAtLeast(0.0) else totalAmount

    val matchedBan = remember(customer, banRecords) {
        banRecords.find { ban ->
            ban.fullName.equals(customer.name, ignoreCase = true) ||
                (customer.nickname.isNotBlank() && ban.nickname.isNotBlank() && ban.nickname.equals(customer.nickname, ignoreCase = true))
        }
    }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Credit for ${customer.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    matchedBan?.let { ban ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🚫 BAN ALERT: Customer is BANNED!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Name: ${ban.fullName} | Reason: ${ban.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Cannot issue credit to banned customers.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Description / Items Borrowed") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Credit Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tenderedInput,
                        onValueChange = { tenderedInput = it },
                        label = { Text("Partial Tendered Payment (₱) - Optional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    var showDatePickerDialog by remember { mutableStateOf(false) }
                    val displayDateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

                    Text("Date Borrowed (Backdate old paper notebook lists):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Date: ${displayDateFormat.format(Date(borrowedAtTimestamp))}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "📅 Pick Specific Date",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedBackdateChip == "TODAY",
                            onClick = {
                                selectedBackdateChip = "TODAY"
                                borrowedAtTimestamp = System.currentTimeMillis()
                            },
                            label = { Text("Today") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = selectedBackdateChip == "1_YEAR",
                            onClick = {
                                selectedBackdateChip = "1_YEAR"
                                borrowedAtTimestamp = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000L)
                            },
                            label = { Text("1 Yr Ago") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = selectedBackdateChip == "2_YEARS",
                            onClick = {
                                selectedBackdateChip = "2_YEARS"
                                borrowedAtTimestamp = System.currentTimeMillis() - (2L * 365 * 24 * 60 * 60 * 1000L)
                            },
                            label = { Text("2 Yrs Ago") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = selectedBackdateChip == "CUSTOM",
                            onClick = {
                                selectedBackdateChip = "CUSTOM"
                                showDatePickerDialog = true
                            },
                            label = { Text("📅 Custom Date") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (showDatePickerDialog) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = borrowedAtTimestamp
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePickerDialog = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            borrowedAtTimestamp = it
                                            selectedBackdateChip = "CUSTOM"
                                        }
                                        showDatePickerDialog = false
                                    }
                                ) {
                                    Text("OK", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePickerDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    if (tendered > 0) {
                        Text(
                            text = "Net Credit to Add: ₱${netCreditAdded.roundToInt()}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = totalAmount > 0 && matchedBan == null,
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
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier for Credit Addition",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val notes = if (manualName.isNotBlank()) manualName else "Credit Entry"
                onAddCreditConfirmed(netCreditAdded, notes, borrowedAtTimestamp, cashier)
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
                    Text("Outstanding Balance: ₱${customer.balance.roundToInt()}", fontWeight = FontWeight.Bold)

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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier for Payment",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                onPaymentConfirmed(paymentAmt, cashier)
            }
        )
    }
}
