package com.example.ministoreapp.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import kotlin.math.roundToInt

data class VariantGroupModel(
    val key: String,
    val title: String,
    val brand: String,
    val category: String,
    val storeName: String,
    val aliases: String,
    val isBeverage: Boolean,
    val hasWholesale: Boolean,
    val priceOptions: List<ProductEntity>
)

fun extractCleanVariantTitle(name: String): String {
    return name
        .replace(Regex("""\s*\(COLD\)""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*\(\d+\s*[^)]+\)"""), "")
        .trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    products: List<ProductEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddToOrder: (RestockItemEntity) -> Unit
) {
    var activeCategoryFilter by remember { mutableStateOf("All") }
    var selectedVariantForRestock by remember { mutableStateOf<VariantGroupModel?>(null) }

    // Group Products by Variant
    val variantGroups = remember(products) {
        val map = LinkedHashMap<String, MutableList<ProductEntity>>()
        for (prod in products) {
            val cleanTitle = extractCleanVariantTitle(prod.name)
            val groupKey = "${prod.groupId ?: "nogroup"}_$cleanTitle"
            map.getOrPut(groupKey) { mutableListOf() }.add(prod)
        }

        map.map { (key, groupProducts) ->
            val first = groupProducts.first()
            val cleanTitle = extractCleanVariantTitle(first.name)
            val hasWholesale = groupProducts.any { it.variantType == "WHOLESALE" || it.variantType == "COLD_WHOLESALE" }
            val isBeverage = groupProducts.any { it.isBeverage }

            VariantGroupModel(
                key = key,
                title = cleanTitle,
                brand = first.brand,
                category = first.category,
                storeName = first.storeName,
                aliases = first.aliases,
                isBeverage = isBeverage,
                hasWholesale = hasWholesale,
                priceOptions = groupProducts
            )
        }
    }

    val categories = remember(variantGroups) {
        val list = mutableListOf("All", "Wholesale", "Beverage")
        val uniqueCats = variantGroups.mapNotNull { it.category.ifBlank { null } }.distinct().sorted()
        list.addAll(uniqueCats.filter { !it.equals("Wholesale", ignoreCase = true) && !it.equals("Beverage", ignoreCase = true) })
        list
    }

    val filteredVariantGroups = remember(variantGroups, searchQuery, activeCategoryFilter) {
        variantGroups.filter { group ->
            val matchesSearch = searchQuery.isBlank() ||
                group.title.contains(searchQuery, ignoreCase = true) ||
                group.aliases.contains(searchQuery, ignoreCase = true) ||
                group.category.contains(searchQuery, ignoreCase = true) ||
                group.brand.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (activeCategoryFilter) {
                "All" -> true
                "Wholesale" -> group.hasWholesale
                "Beverage" -> group.isBeverage || group.category.equals("Beverage", ignoreCase = true)
                else -> group.category.equals(activeCategoryFilter, ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search products, category, alias...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row (All, Wholesale, Beverage, Categories)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = activeCategoryFilter == category,
                    onClick = { activeCategoryFilter = category },
                    label = { Text(category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredVariantGroups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No products in inventory." else "No products matching '$searchQuery'",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredVariantGroups, key = { it.key }) { variantGroup ->
                    VariantSearchCard(
                        group = variantGroup,
                        onRestockClick = { selectedVariantForRestock = variantGroup }
                    )
                }
            }
        }
    }

    // Restock Selection Dialog (Excludes Cold Items)
    selectedVariantForRestock?.let { variantGroup ->
        val nonColdOptions = remember(variantGroup) {
            variantGroup.priceOptions.filter {
                it.variantType != "COLD_RETAIL" &&
                    it.variantType != "COLD_WHOLESALE" &&
                    !it.name.contains("(COLD)", ignoreCase = true)
            }
        }

        RestockSelectionModal(
            variantTitle = variantGroup.title,
            options = if (nonColdOptions.isNotEmpty()) nonColdOptions else variantGroup.priceOptions,
            onDismiss = { selectedVariantForRestock = null },
            onConfirmRestock = { selectedProduct, qty ->
                val restockItem = RestockItemEntity(
                    name = selectedProduct.name,
                    cost = selectedProduct.cost,
                    sellingPrice = selectedProduct.sellingPrice,
                    quantity = qty,
                    unit = selectedProduct.unit,
                    unitSize = selectedProduct.unitSize
                )
                onAddToOrder(restockItem)
                selectedVariantForRestock = null
            }
        )
    }
}

@Composable
fun VariantSearchCard(
    group: VariantGroupModel,
    onRestockClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Title & Restock Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (group.storeName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = group.storeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onRestockClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Restock",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+ Restock", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price Badges Row (Sorted: Cold Retail -> Retail -> Cold Case -> Case)
            val sortedOptions = remember(group.priceOptions) {
                group.priceOptions.sortedBy { option ->
                    when (option.variantType) {
                        "COLD_RETAIL" -> 1
                        "RETAIL" -> 2
                        "COLD_WHOLESALE" -> 3
                        "WHOLESALE" -> 4
                        else -> 5
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sortedOptions) { option ->
                    val (badgeIcon, badgeLabel) = when (option.variantType) {
                        "WHOLESALE" -> Pair("📦", "₱${option.sellingPrice.roundToInt()} (${option.unit.ifBlank { "Case" }})")
                        "COLD_WHOLESALE" -> Pair("🧊", "₱${option.sellingPrice.roundToInt()} (Cold Case)")
                        "COLD_RETAIL" -> Pair("❄️", "₱${option.sellingPrice.roundToInt()} (Cold)")
                        else -> Pair("🏷️", "₱${option.sellingPrice.roundToInt()} (${option.unit.ifBlank { "Bottle" }})")
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(badgeIcon, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestockSelectionModal(
    variantTitle: String,
    options: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirmRestock: (ProductEntity, Int) -> Unit
) {
    var selectedProduct by remember(options) { mutableStateOf(options.firstOrNull()) }
    var quantityInput by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCartCheckout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = { Text("Add to Restock List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = variantTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (options.size > 1) {
                    Text("Select Packaging / Unit:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { option ->
                            val unitLabel = if (option.isWholesaleParent || option.variantType == "WHOLESALE") {
                                "${option.unit.ifBlank { "Case" }} (₱${option.sellingPrice.roundToInt()})"
                            } else {
                                "${option.unit.ifBlank { "Piece" }} (₱${option.sellingPrice.roundToInt()})"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selectedProduct?.id == option.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                    .clickable { selectedProduct = option }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProduct?.id == option.id,
                                    onClick = { selectedProduct = option }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Cost: ₱${option.cost.roundToInt()} · Price: ₱${option.sellingPrice.roundToInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else selectedProduct?.let { prod ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Price: ₱${prod.sellingPrice.roundToInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cost: ₱${prod.cost.roundToInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantityInput = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedProduct?.let { prod ->
                        val qty = quantityInput.toIntOrNull() ?: 1
                        onConfirmRestock(prod, qty)
                    }
                },
                enabled = selectedProduct != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
