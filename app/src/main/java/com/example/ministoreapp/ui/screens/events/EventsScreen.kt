package com.example.ministoreapp.ui.screens.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.EventEntity
import com.example.ministoreapp.ui.components.CashierVerificationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun getRelativeTimeSpan(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val mins = (diffMs / (1000 * 60)).toInt()
    val hours = (diffMs / (1000 * 60 * 60)).toInt()
    val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()

    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "$mins mins ago"
        hours < 24 -> "$hours hrs ago"
        days == 1 -> "Yesterday"
        else -> "$days days ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    events: List<EventEntity>,
    cashiers: List<CashierEntity>,
    onCreateEvent: (EventEntity) -> Unit,
    onSettleEvent: (EventEntity, CashierEntity, String) -> Unit,
    onDeleteEvent: (EventEntity, CashierEntity) -> Unit = { _, _ -> }
) {
    // Dedicated Modal Type: "BUY", "FUND_TRANSFER", "TASK", "INFO", or null
    var activeModalType by remember { mutableStateOf<String?>(null) }

    var eventToSettle by remember { mutableStateOf<EventEntity?>(null) }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }
    var settlementNoteInput by remember { mutableStateOf("") }

    // Filter Mode: "ALL", "PENDING_TASKS", "PENDING_SETTLES"
    var filterMode by remember { mutableStateOf("ALL") }

    val todayStart = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val todayEventsCount = events.count { it.timestamp >= todayStart }
    val pendingTasksCount = events.count { it.type == "TASK" && !it.isSettled }
    val pendingSettlesCount = events.count { it.type == "FUND_TRANSFER" && !it.isSettled }

    val displayedEvents = remember(events, filterMode) {
        when (filterMode) {
            "PENDING_TASKS" -> events.filter { it.type == "TASK" && !it.isSettled }
            "PENDING_SETTLES" -> events.filter { it.type == "FUND_TRANSFER" && !it.isSettled }
            else -> events
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 1. TOP STATS ROW (3 Equal Columns, No Clipping/Scroll)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Events Today (Neutral Dark)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Events Today",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$todayEventsCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
                    Text(
                        text = "Daily Total",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card 2: Pending Tasks (Tinted Green, Clickable Filter Toggle)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        filterMode = if (filterMode == "PENDING_TASKS") "ALL" else "PENDING_TASKS"
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (filterMode == "PENDING_TASKS") Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Pending Tasks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$pendingTasksCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Click to View",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card 3: Pending Settle (Tinted Amber, Clickable Filter Toggle)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        filterMode = if (filterMode == "PENDING_SETTLES") "ALL" else "PENDING_SETTLES"
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (filterMode == "PENDING_SETTLES") Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Pending Settle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$pendingSettlesCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "Click to Settle",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. ADD EVENT (Action Grid - 4 Equal Flexible Action Chips)
        Text(
            text = "Add Event",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EventActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShoppingCart,
                label = "Purchase",
                iconColor = MaterialTheme.colorScheme.tertiary,
                onClick = { activeModalType = "BUY" }
            )
            EventActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Paid,
                label = "Fund In/Out",
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { activeModalType = "FUND_TRANSFER" }
            )
            EventActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TaskAlt,
                label = "Task",
                iconColor = Color(0xFF10B981),
                onClick = { activeModalType = "TASK" }
            )
            EventActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Info,
                label = "Info",
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = { activeModalType = "INFO" }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. FEED SECTION HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (filterMode) {
                    "PENDING_TASKS" -> "Active Pending Tasks (${displayedEvents.size})"
                    "PENDING_SETTLES" -> "Pending Fund Transfers (${displayedEvents.size})"
                    else -> "Today's Events Feed"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (filterMode != "ALL") {
                TextButton(
                    onClick = { filterMode = "ALL" },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Show All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. FEED CONTENT OR ELEVATED EMPTY STATE
        if (displayedEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (filterMode) {
                            "PENDING_TASKS" -> Icons.Default.TaskAlt
                            "PENDING_SETTLES" -> Icons.Default.Paid
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = when (filterMode) {
                            "PENDING_TASKS" -> "No pending tasks found."
                            "PENDING_SETTLES" -> "No pending settlements found."
                            else -> "No events logged today."
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isTabletLayout = configuration.screenWidthDp >= 600 && com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.TABLET_POS_LAYOUT)

            LazyVerticalGrid(
                columns = if (isTabletLayout) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = displayedEvents, key = { it.id }) { event ->
                    EventCardFeedItem(
                        event = event,
                        onSettleClick = { eventToSettle = event },
                        onDeleteClick = { eventToDelete = event }
                    )
                }
            }
        }
    }

    // Launch Dedicated Modals per event type
    when (activeModalType) {
        "BUY" -> PurchaseModal(
            cashiers = cashiers,
            onDismiss = { activeModalType = null },
            onCreate = { newEvent ->
                onCreateEvent(newEvent)
                activeModalType = null
            }
        )
        "FUND_TRANSFER" -> FundsTransferModal(
            cashiers = cashiers,
            onDismiss = { activeModalType = null },
            onCreate = { newEvent ->
                onCreateEvent(newEvent)
                activeModalType = null
            }
        )
        "TASK" -> TaskModal(
            cashiers = cashiers,
            onDismiss = { activeModalType = null },
            onCreate = { newEvent ->
                onCreateEvent(newEvent)
                activeModalType = null
            }
        )
        "INFO" -> InfoModal(
            cashiers = cashiers,
            onDismiss = { activeModalType = null },
            onCreate = { newEvent ->
                onCreateEvent(newEvent)
                activeModalType = null
            }
        )
    }

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
                                "Settle transfer of ₱${event.total.roundToInt()} (${event.transferType})"
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
                        Text("Verify Cashier", fontWeight = FontWeight.Bold)
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
            val effectiveCashiers = remember(cashiers) {
                if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
            }
            CashierVerificationDialog(
                cashiers = effectiveCashiers,
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

    eventToDelete?.let { event ->
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
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
fun EventActionChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EventCardFeedItem(
    event: EventEntity,
    onSettleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val relativeTimeStr = getRelativeTimeSpan(event.timestamp)

    when {
        // 1. PENDING SETTLE TRANSFER
        event.type == "FUND_TRANSFER" && !event.isSettled -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E18)),
                border = BorderStroke(1.5.dp, Color(0xFFFFA000))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                            Text("Pending Settle Transfer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFA000))
                        }
                        Text(relativeTimeStr, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFCC80))
                    }

                    Text("⚠️ Requires Cashier Settle", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))

                    Text(
                        text = "Fund Transfer - ${event.transferType ?: "Cash in transit"} (₱${event.total.roundToInt()})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

                    if (event.description.isNotBlank()) {
                        Text(event.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFE0B2))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onSettleClick,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFA000)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFA000))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MARK SETTLED", fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF8A80))
                        }
                    }
                }
            }
        }

        // 2. ACTIVE PENDING TASK
        event.type == "TASK" && !event.isSettled -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Task", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(relativeTimeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Task (Pending)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (event.details.isNotBlank()) {
                        Text(event.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onSettleClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Completed by", fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        // 3. COMPLETED TASK
        event.type == "TASK" && event.isSettled -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Completed Task", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(relativeTimeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Text(event.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "✓ TASK COMPLETED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "Completed by ${event.settlementCashierName ?: "Cashier"}${if (event.settlementNote.isNullOrBlank()) "" else " (${event.settlementNote})"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 4. PURCHASES (BUY)
        event.type == "BUY" -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Text("Purchase", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Text(relativeTimeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Text(event.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (event.total > 0) {
                        Text("Total Amount: ₱${event.total.roundToInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    if (event.details.isNotBlank()) {
                        Text(event.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text("Processed by ${event.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // 5. FUND IN / OUT
        event.type == "FUND_TRANSFER" && event.isSettled -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Fund In/Out", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(relativeTimeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Text(event.description.ifBlank { event.transferType ?: "Fund Transfer" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (event.total > 0) {
                        Text("Amount: ₱${event.total.roundToInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    if (event.details.isNotBlank()) {
                        Text(event.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text("Settled by ${event.settlementCashierName ?: event.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // 6. INFO
        else -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Text("Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(relativeTimeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Text(event.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (event.details.isNotBlank()) {
                        Text(event.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text("Logged by ${event.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ---------------- DEDICATED EVENT MODALS ----------------

@Composable
fun PurchaseModal(
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onCreate: (EventEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Log Purchase / Expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Item Description") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Total Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Additional Details (Optional)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = description.isNotBlank() && (amountInput.toDoubleOrNull() ?: 0.0) > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Verify Cashier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Log Purchase",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val amt = amountInput.toDoubleOrNull() ?: 0.0
                onCreate(
                    EventEntity(
                        type = "BUY",
                        description = description,
                        cost = amt,
                        quantity = 1,
                        total = amt,
                        details = details,
                        cashierId = cashier.id,
                        cashierName = cashier.name
                    )
                )
            }
        )
    }
}

@Composable
fun FundsTransferModal(
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onCreate: (EventEntity) -> Unit
) {
    var transferType by remember { mutableStateOf("STORE CASH TO GCASH") }
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Funds Exchange", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Direction:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = transferType == "STORE CASH TO GCASH",
                            onClick = { transferType = "STORE CASH TO GCASH" },
                            label = { Text("Store Cash ➔ GCash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = transferType == "GCASH TO STORE CASH",
                            onClick = { transferType = "GCASH TO STORE CASH" },
                            label = { Text("GCash ➔ Store Cash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Transfer Purpose / Title") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Additional Details (Optional)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = (amountInput.toDoubleOrNull() ?: 0.0) > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify Cashier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier for Transfer",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val amt = amountInput.toDoubleOrNull() ?: 0.0
                val title = description.ifBlank { transferType }
                onCreate(
                    EventEntity(
                        type = "FUND_TRANSFER",
                        description = title,
                        cost = amt,
                        quantity = 1,
                        total = amt,
                        details = details,
                        transferType = transferType,
                        cashierId = cashier.id,
                        cashierName = cashier.name
                    )
                )
            }
        )
    }
}

@Composable
fun TaskModal(
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onCreate: (EventEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFF10B981))
                    Text("Log To-Do Task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Task Title / What to Do") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Note / Instructions (Optional)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = description.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Verify Cashier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Log Task",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                onCreate(
                    EventEntity(
                        type = "TASK",
                        description = description,
                        cost = 0.0,
                        quantity = 1,
                        total = 0.0,
                        details = details,
                        cashierId = cashier.id,
                        cashierName = cashier.name
                    )
                )
            }
        )
    }
}

@Composable
fun InfoModal(
    cashiers: List<CashierEntity>,
    onDismiss: () -> Unit,
    onCreate: (EventEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Log Store Note / Info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Title / Note Description") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Additional Details (Optional)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = description.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify Cashier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        val effectiveCashiers = remember(cashiers) {
            if (cashiers.isEmpty()) listOf(CashierEntity(id = "default_owner", name = "Store Owner", pin = "0000")) else cashiers
        }
        CashierVerificationDialog(
            cashiers = effectiveCashiers,
            actionTitle = "Verify Cashier to Log Note",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                onCreate(
                    EventEntity(
                        type = "INFO",
                        description = description,
                        cost = 0.0,
                        quantity = 1,
                        total = 0.0,
                        details = details,
                        cashierId = cashier.id,
                        cashierName = cashier.name
                    )
                )
            }
        )
    }
}
