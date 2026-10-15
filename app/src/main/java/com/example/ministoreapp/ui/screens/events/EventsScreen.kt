package com.example.ministoreapp.ui.screens.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.EventEntity
import com.example.ministoreapp.ui.components.CashierVerificationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    events: List<EventEntity>,
    cashiers: List<CashierEntity>,
    onCreateEvent: (EventEntity) -> Unit,
    onSettleEvent: (EventEntity, CashierEntity, String) -> Unit,
    onDeleteEvent: (EventEntity, CashierEntity) -> Unit = { _, _ -> }
) {
    var showCreateModal by remember { mutableStateOf(false) }
    var defaultCreateType by remember { mutableStateOf("BUY") }
    var eventToSettle by remember { mutableStateOf<EventEntity?>(null) }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }
    var settlementNoteInput by remember { mutableStateOf("") }
    var filterPendingOnly by remember { mutableStateOf(false) }

    // Stats Calculations
    val todayStart = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val todayEvents = events.filter { it.timestamp >= todayStart }
    val eventsTodayCount = todayEvents.size
    val amountTodayTotal = todayEvents.sumOf { it.total }
    val pendingSettleCount = events.count { (it.type == "FUND_TRANSFER" || it.type == "TASK") && !it.isSettled }

    val displayedEvents = if (filterPendingOnly) {
        events.filter { (it.type == "FUND_TRANSFER" || it.type == "TASK") && !it.isSettled }
    } else {
        events
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F6))
            .padding(16.dp)
    ) {
        // Top 3 Stat Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Events today
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E6E4))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "$eventsTodayCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1D1C)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Events today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B726E)
                    )
                }
            }

            // Card 2: Amount today
            Card(
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E6E4))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "₱${"%.2f".format(amountTodayTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1D1C),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Amount today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B726E)
                    )
                }
            }

            // Card 3: Pending settle
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E6E4))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "$pendingSettleCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1D1C)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pending settle",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B726E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Action Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Button 1: Buy (purchase)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            defaultCreateType = "BUY"
                            showCreateModal = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6E4))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5ECE0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color(0xFF8C581E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Buy (purchase)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1D1C)
                        )
                    }
                }

                // Button 2: Fund in / out
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            defaultCreateType = "FUND_TRANSFER"
                            showCreateModal = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6E4))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3EFE9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = null,
                                tint = Color(0xFF0B5E38),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fund in / out",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1D1C)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Button 3: Settle a transfer
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filterPendingOnly = !filterPendingOnly },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filterPendingOnly) Color(0xFFE3EFE9) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (filterPendingOnly) Color(0xFF0B5E38) else Color(0xFFE2E6E4))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE6F0EC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF0B5E38),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Settle a transfer",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1D1C)
                        )
                    }
                }

                // Button 4: Store info note
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            defaultCreateType = "TASK"
                            showCreateModal = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6E4))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F2EC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF0B5E38),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Store info note",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1D1C)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Events List Feed
        if (displayedEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterPendingOnly) "No pending settlements." else "No events logged.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF6B726E)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedEvents, key = { it.id }) { event ->
                    EventCardItem(
                        event = event,
                        onSettleClick = { eventToSettle = event },
                        onDeleteClick = { eventToDelete = event }
                    )
                }
            }
        }
    }

    if (showCreateModal) {
        CreateEventDialog(
            initialType = defaultCreateType,
            cashiers = cashiers,
            onDismiss = { showCreateModal = false },
            onCreate = { newEvent ->
                onCreateEvent(newEvent)
                showCreateModal = false
            }
        )
    }

    // Settlement Modal
    eventToSettle?.let { event ->
        var pendingSettlingCashier by remember { mutableStateOf<CashierEntity?>(null) }
        val isTask = event.type == "TASK"

        if (pendingSettlingCashier == null) {
            AlertDialog(
                onDismissRequest = { eventToSettle = null },
                title = {
                    Text(
                        text = if (isTask) "Complete Task" else "Settle Fund Transfer",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isTask) {
                                "Mark task '${event.description}' as completed?"
                            } else {
                                "Settle transfer of ₱${"%.2f".format(event.total)} (${event.transferType})"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = settlementNoteInput,
                            onValueChange = { settlementNoteInput = it },
                            label = { Text(if (isTask) "Completion Note (Optional)" else "Settlement Note") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { pendingSettlingCashier = cashiers.firstOrNull() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Proceed to Verification", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { eventToSettle = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            CashierVerificationDialog(
                cashiers = cashiers,
                actionTitle = if (isTask) "Verify Cashier to Complete Task" else "Verify Cashier for Settlement",
                onDismiss = {
                    eventToSettle = null
                    pendingSettlingCashier = null
                },
                onVerified = { cashier ->
                    onSettleEvent(event, cashier, settlementNoteInput)
                    eventToSettle = null
                    pendingSettlingCashier = null
                    settlementNoteInput = ""
                }
            )
        }
    }

    // Delete Modal
    eventToDelete?.let { event ->
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Delete Event",
            onDismiss = { eventToDelete = null },
            onVerified = { cashier ->
                onDeleteEvent(event, cashier)
                eventToDelete = null
            }
        )
    }
}

@Composable
fun EventCardItem(
    event: EventEntity,
    onSettleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault())

    val (bgCircleColor, iconColor, iconVector) = when (event.type) {
        "BUY" -> Triple(Color(0xFFF5ECE0), Color(0xFF8C581E), Icons.Default.ShoppingCart)
        "FUND_TRANSFER" -> Triple(Color(0xFFE3EFE9), Color(0xFF0B5E38), Icons.Default.Paid)
        "TASK" -> Triple(Color(0xFFE8F2EC), Color(0xFF0B5E38), Icons.Default.Description)
        else -> Triple(Color(0xFFEBF1EE), Color(0xFF334A3E), Icons.Default.Description)
    }

    val displayTypeTitle = when (event.type) {
        "BUY" -> "BUY"
        "FUND_TRANSFER" -> if (event.transferType.isNullOrBlank()) "FUND TRANSFER" else "OUT · ${event.transferType.uppercase()}"
        "TASK" -> "STORE INFO NOTE"
        else -> event.type.uppercase()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E6E4))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Icon + Type Title + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(bgCircleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = displayTypeTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF526058)
                    )
                }

                if (event.total > 0) {
                    Text(
                        text = "₱${"%.2f".format(event.total)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1D1C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Description Title
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1D1C)
                )
            }

            // Subtitle Details
            if (event.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B726E)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Date · Cashier Name, Delete Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${dateFormat.format(Date(event.timestamp))} · ${event.cashierName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A938E)
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF8A938E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Settlement / Completion Box
            if (event.type == "FUND_TRANSFER" || event.type == "TASK") {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !event.isSettled, onClick = onSettleClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFCFB)),
                    border = BorderStroke(1.dp, Color(0xFFE2E6E4))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = event.isSettled,
                            onCheckedChange = { if (!event.isSettled) onSettleClick() },
                            enabled = !event.isSettled
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (event.isSettled) {
                                    if (event.type == "TASK") "Completed" else "Settled"
                                } else {
                                    if (event.type == "TASK") "Mark as Completed" else "Mark as Settled"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1D1C)
                            )
                            Text(
                                text = if (event.isSettled) {
                                    "By ${event.settlementCashierName ?: "Cashier"}${if (event.settlementNote.isNullOrBlank()) "" else " (${event.settlementNote})"}"
                                } else {
                                    "Asks for cashier + 4-digit PIN"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B726E)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateEventDialog(
    initialType: String = "BUY",
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onCreate: (EventEntity) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var description by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var transferType by remember { mutableStateOf("CASH TO GCASH") }
    var amountInput by remember { mutableStateOf("") }

    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Log Event / Note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Type:", fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedType == "BUY",
                            onClick = { selectedType = "BUY" },
                            label = { Text("Buy") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = selectedType == "FUND_TRANSFER",
                            onClick = { selectedType = "FUND_TRANSFER" },
                            label = { Text("Transfer") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = selectedType == "TASK",
                            onClick = { selectedType = "TASK" },
                            label = { Text("Note / Task") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = selectedType == "INFO",
                            onClick = { selectedType = "INFO" },
                            label = { Text("Info") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    when (selectedType) {
                        "BUY" -> {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Item Description") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                label = { Text("Total Amount (₱)") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text("Additional Details") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "FUND_TRANSFER" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = transferType == "CASH TO GCASH",
                                    onClick = { transferType = "CASH TO GCASH" },
                                    label = { Text("Cash -> GCash") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = transferType == "GCASH TO CASH",
                                    onClick = { transferType = "GCASH TO CASH" },
                                    label = { Text("GCash -> Cash") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                label = { Text("Amount (₱)") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text("Additional Details") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "TASK" -> {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Title / What to Do") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text("Note / Instructions") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Event / Note Description") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text("Additional Details") },
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
                    enabled = description.isNotBlank() || selectedType == "FUND_TRANSFER",
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify Cashier & Save", fontWeight = FontWeight.Bold)
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
            actionTitle = "Verify Cashier to Log ${if (selectedType == "TASK") "Note" else "Event"}",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val amt = amountInput.toDoubleOrNull() ?: 0.0

                val event = EventEntity(
                    type = selectedType,
                    description = description,
                    cost = amt,
                    quantity = 1,
                    total = amt,
                    details = details,
                    transferType = if (selectedType == "FUND_TRANSFER") transferType else null,
                    cashierId = cashier.id,
                    cashierName = cashier.name
                )
                onCreate(event)
            }
        )
    }
}
