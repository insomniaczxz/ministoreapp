package com.example.ministoreapp.ui.screens.restock

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.domain.RecommendationEngine
import com.example.ministoreapp.ui.components.CameraBarcodeScannerDialog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    restockItems: List<RestockItemEntity>,
    allProducts: List<ProductEntity> = emptyList(),
    onUpdateQuantity: (RestockItemEntity, Int) -> Unit,
    onRemoveItem: (RestockItemEntity) -> Unit,
    onClearRestock: () -> Unit,
    onAddRestockItem: (RestockItemEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var scannedProductForRestock by remember { mutableStateOf<ProductEntity?>(null) }

    val totalPurchasingCost = restockItems.sumOf { it.cost * it.quantity }
    val totalSellingPrice = restockItems.sumOf { it.sellingPrice * it.quantity }
    val estimatedProfit = (totalSellingPrice - totalPurchasingCost).coerceAtLeast(0.0)
    val marginPercent = if (totalSellingPrice > 0) (estimatedProfit / totalSellingPrice * 100) else 0.0

    val recommendation = remember(restockItems, allProducts) {
        RecommendationEngine.recommendStore(restockItems, allProducts)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Hero Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Restock Overhead Cost",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "₱${totalPurchasingCost.roundToInt()}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Est. Margin",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${marginPercent.roundToInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Retail Value: ₱${totalSellingPrice.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Est. Profit: ₱${estimatedProfit.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        recommendation?.let { rec ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Recommendation",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Vendor Recommendation",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Purchase from '${rec.recommendedStoreName}' for lowest total cost of ₱${rec.totalCost.roundToInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val formattedText = generateRestockListText(restockItems, totalPurchasingCost)
                    clipboardManager.setText(AnnotatedString(formattedText))
                    Toast.makeText(context, "Restock list copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy List", fontWeight = FontWeight.SemiBold)
            }

            FilledTonalButton(
                onClick = {
                    exportRestockListToDownloads(context, restockItems, totalPurchasingCost)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export .txt", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (restockItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Empty",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Restock List is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search products and tap '+' to add items for restock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isTabletLayout = configuration.screenWidthDp >= 600 && com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.TABLET_POS_LAYOUT)

            LazyVerticalGrid(
                columns = if (isTabletLayout) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = restockItems, key = { it.id }) { item ->
                    RestockItemCard(
                        item = item,
                        onUpdateQuantity = { newQty -> onUpdateQuantity(item, newQty) },
                        onRemove = { onRemoveItem(item) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onClearRestock,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear All Restock Items", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCameraScannerDialog) {
        CameraBarcodeScannerDialog(
            title = "Scan Product to Restock",
            onDismiss = { showCameraScannerDialog = false },
            onBarcodeScanned = { scannedCode ->
                showCameraScannerDialog = false
                val code = scannedCode.trim()
                val matched = allProducts.find { it.barcode.equals(code, ignoreCase = true) || it.comboKey.contains(code, ignoreCase = true) }
                if (matched != null) {
                    scannedProductForRestock = matched
                } else {
                    Toast.makeText(context, "Product with barcode '$code' not found.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    scannedProductForRestock?.let { product ->
        var qtyInput by remember { mutableStateOf("1") }
        var isCaseMode by remember { mutableStateOf(product.isWholesaleParent) }

        AlertDialog(
            onDismissRequest = { scannedProductForRestock = null },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restock '${product.name}'", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isCaseMode,
                            onClick = { isCaseMode = false },
                            label = { Text("By Pieces") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isCaseMode,
                            onClick = { isCaseMode = true },
                            label = { Text("By Cases/Bundles (${product.itemsPerWholesalePack ?: 12} pcs)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        label = { Text(if (isCaseMode) "Number of Cases/Bundles" else "Number of Pieces") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val inputQty = qtyInput.toIntOrNull() ?: 1
                    val totalPcs = if (isCaseMode) inputQty * (product.itemsPerWholesalePack ?: 12) else inputQty
                    Text("Total Stock Addition: $totalPcs pcs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputQty = qtyInput.toIntOrNull() ?: 1
                        val totalPcs = if (isCaseMode) inputQty * (product.itemsPerWholesalePack ?: 12) else inputQty
                        val restockItem = RestockItemEntity(
                            name = product.name,
                            cost = product.cost,
                            sellingPrice = product.sellingPrice,
                            quantity = totalPcs,
                            unit = product.unit,
                            unitSize = product.unitSize
                        )
                        onAddRestockItem(restockItem)
                        scannedProductForRestock = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add to Restock List", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { scannedProductForRestock = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun RestockItemCard(
    item: RestockItemEntity,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cost: ₱${item.cost.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Total: ₱${(item.cost * item.quantity).roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { if (item.quantity > 1) onUpdateQuantity(item.quantity - 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = { onUpdateQuantity(item.quantity + 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun generateRestockListText(items: List<RestockItemEntity>, totalCost: Double): String {
    val sb = StringBuilder()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    sb.appendLine("=== G&J STORE RESTOCK LIST ===")
    sb.appendLine("Date: ${dateFormat.format(Date())}")
    sb.appendLine("---------------------------")
    for (item in items) {
        sb.appendLine("${item.name} - Qty: ${item.quantity} x ₱${item.cost.roundToInt()} = ₱${(item.cost * item.quantity).roundToInt()}")
    }
    sb.appendLine("---------------------------")
    sb.appendLine("TOTAL RESTOCK COST: ₱${totalCost.roundToInt()}")
    return sb.toString()
}

fun exportRestockListToDownloads(context: Context, items: List<RestockItemEntity>, totalCost: Double) {
    try {
        val fileName = "gj_store_restock_${System.currentTimeMillis()}.txt"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        val text = generateRestockListText(items, totalCost)

        FileOutputStream(file).use { out ->
            out.write(text.toByteArray())
        }
        Toast.makeText(context, "Exported to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
