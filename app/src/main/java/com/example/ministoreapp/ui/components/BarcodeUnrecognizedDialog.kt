package com.example.ministoreapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.ProductEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeUnrecognizedDialog(
    scannedBarcode: String,
    allProducts: List<ProductEntity>,
    onDismiss: () -> Unit,
    onLinkAsUnitBarcode: (ProductEntity, String) -> Unit,
    onLinkAsCaseBarcode: (ProductEntity, String) -> Unit,
    onAddNewProduct: (String) -> Unit
) {
    var mode by remember { mutableStateOf<String?>(null) } // null, "LINK_UNIT", "LINK_CASE"
    var searchText by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val matchingProducts = remember(searchText, allProducts) {
        if (searchText.isBlank()) emptyList() else allProducts.filter {
            it.name.contains(searchText, ignoreCase = true) ||
            it.brand.contains(searchText, ignoreCase = true) ||
            it.category.contains(searchText, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = {
            Text(
                text = "Barcode Not Recognized",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Scanned Barcode: '$scannedBarcode'",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (mode == null) {
                    Text(
                        text = "Choose how to handle this barcode:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { mode = "LINK_UNIT" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Link as Unit / Bottle Barcode", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { mode = "LINK_CASE" },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Link as Wholesale Case Barcode", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAddNewProduct(scannedBarcode) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add as New Product", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Product Lookup Mode to Link Barcode
                    val isCase = mode == "LINK_CASE"
                    Text(
                        text = if (isCase) "Select product to assign as Wholesale Case Barcode:" else "Select product to assign as Unit Barcode:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    var expandedDropdown by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: searchText,
                            onValueChange = {
                                searchText = it
                                selectedProduct = null
                                expandedDropdown = true
                            },
                            placeholder = { Text("Search product name...") },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )

                        if (matchingProducts.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                matchingProducts.take(10).forEach { prod ->
                                    DropdownMenuItem(
                                        text = { Text(prod.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = {
                                            selectedProduct = prod
                                            searchText = prod.name
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { mode = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                selectedProduct?.let { prod ->
                                    if (isCase) {
                                        onLinkAsCaseBarcode(prod, scannedBarcode)
                                    } else {
                                        onLinkAsUnitBarcode(prod, scannedBarcode)
                                    }
                                }
                            },
                            enabled = selectedProduct != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Link", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (mode == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
