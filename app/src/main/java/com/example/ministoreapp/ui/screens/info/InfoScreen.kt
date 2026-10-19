package com.example.ministoreapp.ui.screens.info

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.CustomerEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.VendorEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class StoreBulletinLogItem(
    val id: String,
    val category: String, // "PRICE", "VENDOR", "BAN", "CREDIT"
    val title: String,
    val subtitle: String,
    val detailsList: List<String>,
    val timestamp: Long,
    val performer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    securitySettings: SecuritySettingsEntity?,
    priceHistory: List<PriceHistoryEntity>,
    vendors: List<VendorEntity> = emptyList(),
    allProducts: List<ProductEntity> = emptyList(),
    banRecords: List<BanRecordEntity>,
    creditTransactions: List<CreditTransactionEntity> = emptyList(),
    customers: List<CustomerEntity> = emptyList(),
    wholesaleProducts: List<ProductEntity> = emptyList()
) {
    var activeFilter by remember { mutableStateOf("ALL") }
    var expandedLogId by remember { mutableStateOf<String?>(null) }
    var showVendorsDirectoryModal by remember { mutableStateOf(false) }
    var showBanListModal by remember { mutableStateOf(false) }

    val sevenDaysAgo = remember { System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) }

    // Unified 7-Day Store Bulletin Logs List
    val bulletinLogs = remember(priceHistory, vendors, banRecords, creditTransactions, customers) {
        val list = mutableListOf<StoreBulletinLogItem>()

        // 1. Price Changes (Past 7 Days)
        priceHistory.filter { it.timestamp >= sevenDaysAgo }.forEach { ph ->
            val diff = ph.newPrice - ph.oldPrice
            val diffText = if (diff > 0) "(+₱${diff.roundToInt()})" else if (diff < 0) "(-₱${Math.abs(diff).roundToInt()})" else ""
            list.add(
                StoreBulletinLogItem(
                    id = "ph_${ph.id}",
                    category = "PRICE",
                    title = "🏷️ PRICE CHANGE · ${ph.productName}",
                    subtitle = "Old: ₱${ph.oldPrice.roundToInt()}  ➔  New: ₱${ph.newPrice.roundToInt()} $diffText",
                    detailsList = emptyList(),
                    timestamp = ph.timestamp,
                    performer = ph.cashierName
                )
            )
        }

        // 2. Vendors (Updated or Active)
        vendors.forEach { v ->
            val routeText = if (v.isTruckPanel && v.routeSchedule.isNotBlank()) "Route: ${v.routeSchedule}" else ""
            val agentText = if (v.agentName.isNotBlank()) "Agent: ${v.agentName}${if (v.agentContactNumber.isNotBlank()) " (${v.agentContactNumber})" else ""}" else ""
            val productsText = if (v.productsSold.isNotBlank()) "Products: ${v.productsSold}" else ""

            val details = listOfNotNull(
                if (v.ownerName.isNotBlank()) "Owner: ${v.ownerName}" else null,
                if (productsText.isNotBlank()) productsText else null,
                if (agentText.isNotBlank()) agentText else null,
                if (routeText.isNotBlank()) routeText else null
            )

            list.add(
                StoreBulletinLogItem(
                    id = "v_${v.id}",
                    category = "VENDOR",
                    title = "🚚 VENDOR UPDATE · ${v.name}",
                    subtitle = if (v.contactNumber.isNotBlank()) "Contact: ${v.contactNumber}" else "Vendor Supplier Profile",
                    detailsList = details,
                    timestamp = v.updatedAt,
                    performer = "System"
                )
            )
        }

        // 3. Ban Records (Past 7 Days)
        banRecords.filter { it.createdAt >= sevenDaysAgo }.forEach { ban ->
            list.add(
                StoreBulletinLogItem(
                    id = "ban_${ban.id}",
                    category = "BAN",
                    title = "🚫 NEW BAN LIST ENTRY · ${ban.fullName}",
                    subtitle = "Reason: ${ban.reason}",
                    detailsList = if (ban.nickname.isNotBlank()) listOf("Alias: ${ban.nickname}") else emptyList(),
                    timestamp = ban.createdAt,
                    performer = ban.cashierName
                )
            )
        }

        // 4. Credit / Payment Transactions (Past 7 Days - Uses borrowedAt so backdated old notebook debts skip feed!)
        creditTransactions.filter { it.timestamp >= sevenDaysAgo || it.borrowedAt >= sevenDaysAgo }.forEach { tx ->
            val customerName = customers.find { it.id == tx.customerId }?.name ?: "Customer"

            if (tx.type == "CREDIT_ADD") {
                list.add(
                    StoreBulletinLogItem(
                        id = "cred_${tx.id}",
                        category = "CREDIT",
                        title = "🔴 NEW CREDIT (UTANG) · $customerName",
                        subtitle = "Borrowed: +₱${tx.totalAmount.roundToInt()}${if (tx.notes.isNotBlank()) " (${tx.notes})" else ""}",
                        detailsList = listOf("Total Balance: ₱${tx.remainingBalance.roundToInt()}"),
                        timestamp = tx.borrowedAt,
                        performer = tx.cashierName
                    )
                )
            } else {
                val balanceText = if (tx.remainingBalance <= 0) "Account Fully Paid! 🎉 (₱0)" else "Remaining Balance: ₱${tx.remainingBalance.roundToInt()}"
                list.add(
                    StoreBulletinLogItem(
                        id = "cred_${tx.id}",
                        category = "CREDIT",
                        title = "🟢 CREDIT PAYMENT · $customerName",
                        subtitle = "Paid: -₱${tx.amountTendered.roundToInt()}",
                        detailsList = listOf(balanceText),
                        timestamp = tx.timestamp,
                        performer = tx.cashierName
                    )
                )
            }
        }

        list.sortedByDescending { it.timestamp }
    }

    val priceCount = bulletinLogs.count { it.category == "PRICE" }
    val creditCount = bulletinLogs.count { it.category == "CREDIT" }
    val vendorCount = bulletinLogs.count { it.category == "VENDOR" }
    val banCount = bulletinLogs.count { it.category == "BAN" }

    val filteredLogs = remember(bulletinLogs, activeFilter) {
        when (activeFilter) {
            "PRICE" -> bulletinLogs.filter { it.category == "PRICE" }
            "CREDIT" -> bulletinLogs.filter { it.category == "CREDIT" }
            "VENDOR" -> bulletinLogs.filter { it.category == "VENDOR" }
            "BAN" -> bulletinLogs.filter { it.category == "BAN" }
            else -> bulletinLogs
        }
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Text(
            text = "Store Intelligence & Updates (Last 7 Days)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = activeFilter == "ALL",
                    onClick = { activeFilter = "ALL" },
                    label = { Text("All (${bulletinLogs.size})", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == "PRICE",
                    onClick = { activeFilter = "PRICE" },
                    label = { Text("🏷️ Prices ($priceCount)", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == "CREDIT",
                    onClick = { activeFilter = "CREDIT" },
                    label = { Text("💳 Credit ($creditCount)", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == "VENDOR",
                    onClick = { activeFilter = "VENDOR" },
                    label = { Text("🚚 Vendors ($vendorCount)", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == "BAN",
                    onClick = { activeFilter = "BAN" },
                    label = { Text("🚫 Ban List ($banCount)", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All quiet! No price, credit, vendor, or ban list updates in the past 7 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    val isExpanded = expandedLogId == log.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedLogId = if (isExpanded) null else log.id
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (log.category) {
                                "BAN" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                "PRICE" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                "CREDIT" -> if (log.title.contains("CREDIT (UTANG)")) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f) else Color(0xFFE3EFE9)
                                else -> MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = log.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = log.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (log.detailsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                log.detailsList.forEach { detail ->
                                    Text(
                                        text = "• $detail",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Performer: ${log.performer}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Logged: ${dateFormat.format(Date(log.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isExpanded) "▲ Tap to collapse" else "▼ Tap to view details",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Directory Lookup Buttons at Bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showVendorsDirectoryModal = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("All Vendors Directory", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { showBanListModal = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Full Ban List", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showVendorsDirectoryModal) {
        AlertDialog(
            onDismissRequest = { showVendorsDirectoryModal = false },
            title = { Text("All Vendors Directory", fontWeight = FontWeight.Bold) },
            text = {
                if (vendors.isEmpty()) {
                    Text("No vendors registered in catalog.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        items(vendors, key = { it.id }) { v ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(v.name, fontWeight = FontWeight.Bold)
                                    if (v.ownerName.isNotBlank()) Text("Owner: ${v.ownerName}", style = MaterialTheme.typography.bodySmall)
                                    if (v.productsSold.isNotBlank()) Text("Products: ${v.productsSold}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    if (v.contactNumber.isNotBlank()) Text("Contact: ${v.contactNumber}", style = MaterialTheme.typography.bodySmall)
                                    if (v.agentName.isNotBlank()) Text("Agent: ${v.agentName} (${v.agentContactNumber})", style = MaterialTheme.typography.bodySmall)
                                    if (v.isTruckPanel) Text("🚛 Route: ${v.routeSchedule}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0B5E38))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showVendorsDirectoryModal = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showBanListModal) {
        AlertDialog(
            onDismissRequest = { showBanListModal = false },
            title = { Text("Full Ban List", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                if (banRecords.isEmpty()) {
                    Text("No records in Ban List.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        items(banRecords, key = { it.id }) { ban ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("🛑 ${ban.fullName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    if (ban.nickname.isNotBlank()) Text("Alias: ${ban.nickname}", style = MaterialTheme.typography.bodySmall)
                                    Text("Reason: ${ban.reason}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBanListModal = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
