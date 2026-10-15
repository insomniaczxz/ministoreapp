package com.example.ministoreapp.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    products: List<ProductEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddToOrder: (RestockItemEntity) -> Unit
) {
    var selectedProductForQuantity by remember { mutableStateOf<ProductEntity?>(null) }
    var quantityInput by remember { mutableStateOf("1") }
    var activeCategoryFilter by remember { mutableStateOf("All") }

    val categories = remember(products) {
        val list = mutableListOf("All", "Wholesale")
        val uniqueCats = products.mapNotNull { it.category.ifBlank { null } }.distinct().sorted()
        list.addAll(uniqueCats)
        list
    }

    val filteredProducts = remember(products, searchQuery, activeCategoryFilter) {
        products.filter { product ->
            val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true) ||
                product.brand.contains(searchQuery, ignoreCase = true) ||
                (searchQuery.equals("Wholesale", ignoreCase = true) && (product.isWholesaleParent || (product.itemsPerWholesalePack ?: 0) > 0))

            val matchesCategory = when (activeCategoryFilter) {
                "All" -> true
                "Wholesale" -> product.isWholesaleParent || (product.itemsPerWholesalePack ?: 0) > 0
                else -> product.category.equals(activeCategoryFilter, ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search products, category, or 'Wholesale'...") },
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

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = activeCategoryFilter == category,
                    onClick = { activeCategoryFilter = category },
                    label = { Text(category, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
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
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onAddClick = { selectedProductForQuantity = product }
                    )
                }
            }
        }
    }

    // Restock Quantity Dialog
    selectedProductForQuantity?.let { product ->
        val fullTitle = buildString {
            append(product.name)
            if (product.unitSize.isNotBlank()) append(" ${product.unitSize}")
            if (product.unit.isNotBlank()) append(" ${product.unit}")
        }

        AlertDialog(
            onDismissRequest = { selectedProductForQuantity = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                    Text(text = fullTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Selling Price: ₱${"%.2f".format(product.sellingPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cost: ₱${"%.2f".format(product.cost)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
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
                        val qty = quantityInput.toIntOrNull() ?: 1
                        val restockItem = RestockItemEntity(
                            productId = product.id,
                            productName = fullTitle,
                            cost = product.cost,
                            sellingPrice = product.sellingPrice,
                            quantity = qty,
                            storeName = product.storeName,
                            unit = product.unit,
                            unitSize = product.unitSize
                        )
                        onAddToOrder(restockItem)
                        selectedProductForQuantity = null
                        quantityInput = "1"
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add to Restock", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProductForQuantity = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onAddClick: () -> Unit
) {
    // Format Name + Unit Size + Unit (e.g. Coca-Cola 1.5L Plastic Bottle)
    val fullTitle = buildString {
        append(product.name)
        if (product.unitSize.isNotBlank()) append(" ${product.unitSize}")
        if (product.unit.isNotBlank()) append(" ${product.unit}")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Price + Add to Restock Button directly below it
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₱${"%.2f".format(product.sellingPrice)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Restock",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+ Restock", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Side: Name + Unit Size + Unit
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = fullTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (product.storeName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
