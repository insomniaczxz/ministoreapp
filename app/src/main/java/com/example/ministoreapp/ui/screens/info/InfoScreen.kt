package com.example.ministoreapp.ui.screens.info

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.StoreEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InfoScreen(
    securitySettings: SecuritySettingsEntity?,
    priceHistory: List<PriceHistoryEntity>,
    vendorStores: List<StoreEntity> = emptyList(),
    allProducts: List<ProductEntity> = emptyList(),
    banRecords: List<BanRecordEntity>,
    wholesaleProducts: List<ProductEntity>
) {
    var selectedTileIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F6))
            .padding(16.dp)
    ) {
        Text(
            text = "Store Intelligence & Catalog",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B1D1C)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Tiled Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tile 1: Price Changes
                InfoTileCard(
                    title = "Price Changes",
                    badgeText = "${priceHistory.size} updates",
                    icon = Icons.Default.Sell,
                    iconBgColor = Color(0xFFE3EFE9),
                    iconTintColor = Color(0xFF0B5E38),
                    isSelected = selectedTileIndex == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTileIndex = 0 }
                )

                // Tile 2: Vendor Info (Suppliers / Vendor Stores)
                InfoTileCard(
                    title = "Vendor Info",
                    badgeText = "${vendorStores.size} vendors",
                    icon = Icons.Default.Store,
                    iconBgColor = Color(0xFFF5ECE0),
                    iconTintColor = Color(0xFF8C581E),
                    isSelected = selectedTileIndex == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTileIndex = 1 }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tile 3: Ban List
                InfoTileCard(
                    title = "Ban List",
                    badgeText = "${banRecords.size} records",
                    icon = Icons.Default.Block,
                    iconBgColor = Color(0xFFFCE8E6),
                    iconTintColor = Color(0xFFC53929),
                    isSelected = selectedTileIndex == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTileIndex = 2 }
                )

                // Tile 4: Wholesale
                InfoTileCard(
                    title = "Wholesale",
                    badgeText = "${wholesaleProducts.size} items",
                    icon = Icons.Default.Inventory,
                    iconBgColor = Color(0xFFE8F2EC),
                    iconTintColor = Color(0xFF0B5E38),
                    isSelected = selectedTileIndex == 3,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTileIndex = 3 }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tile Detailed Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTileIndex) {
                0 -> PriceHistoryTileContent(priceHistory)
                1 -> VendorInfoTileContent(vendorStores, allProducts)
                2 -> BanListTileContent(banRecords)
                3 -> WholesaleCatalogTileContent(wholesaleProducts)
            }
        }
    }
}

@Composable
fun InfoTileCard(
    title: String,
    badgeText: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3EFE9) else Color.White
        ),
        border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF0B5E38) else Color(0xFFE2E6E4))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Active", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1D1C)
            )

            Text(
                text = badgeText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B726E)
            )
        }
    }
}

@Composable
fun PriceHistoryTileContent(priceHistory: List<PriceHistoryEntity>) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault())

    if (priceHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No price changes recorded in the past 7 days.", color = Color(0xFF6B726E))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(priceHistory, key = { it.id }) { ph ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6E4))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = ph.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Old Price: ₱${ph.oldPrice.roundToInt()}  ➔  New Price: ₱${ph.newPrice.roundToInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF0B5E38),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Changed by: ${ph.cashierName} · ${dateFormat.format(Date(ph.timestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8A938E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VendorInfoTileContent(
    vendorStores: List<StoreEntity>,
    allProducts: List<ProductEntity>
) {
    val distinctVendorNames = remember(vendorStores, allProducts) {
        val set = mutableSetOf<String>()
        vendorStores.forEach { set.add(it.name) }
        allProducts.map { it.storeName }.filter { it.isNotBlank() }.forEach { set.add(it) }
        set.sorted()
    }

    if (distinctVendorNames.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No supplier/vendor store information recorded yet.", color = Color(0xFF6B726E))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(distinctVendorNames, key = { it }) { vendorName ->
                val vendorProducts = allProducts.filter { it.storeName.equals(vendorName, ignoreCase = true) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6E4))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vendor: $vendorName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1D1C)
                            )

                            SuggestionChip(
                                onClick = {},
                                label = { Text("${vendorProducts.size} items", fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        if (vendorProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFE2E6E4))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Supplied Products:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B726E)
                            )

                            vendorProducts.take(4).forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "• ${prod.name}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Cost: ₱${prod.cost.roundToInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (vendorProducts.size > 4) {
                                Text(
                                    text = "+ ${vendorProducts.size - 4} more items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8A938E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BanListTileContent(banRecords: List<BanRecordEntity>) {
    if (banRecords.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No individuals in the Ban List.", color = Color(0xFF6B726E))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(banRecords, key = { it.id }) { ban ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E6)),
                    border = BorderStroke(1.dp, Color(0xFFF5C2C1))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🛑 ${ban.fullName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC53929)
                        )
                        if (ban.nickname.isNotBlank()) {
                            Text(text = "Alias: ${ban.nickname}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC53929))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Reason: ${ban.reason}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Logged by Cashier: ${ban.cashierName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8A938E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WholesaleCatalogTileContent(wholesaleProducts: List<ProductEntity>) {
    if (wholesaleProducts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No wholesale items recorded in catalog.", color = Color(0xFF6B726E))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(wholesaleProducts, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6E4))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Wholesale Pack Price: ₱${product.sellingPrice.roundToInt()} | Pack Size: ${product.itemsPerWholesalePack ?: 1} items",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (product.cost > 0 && (product.itemsPerWholesalePack ?: 0) > 0) {
                            val childCost = product.cost / (product.itemsPerWholesalePack ?: 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Calculated Child Unit Cost: ₱${childCost.roundToInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0B5E38),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
