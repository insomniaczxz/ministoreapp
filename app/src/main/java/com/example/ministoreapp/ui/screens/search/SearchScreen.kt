package com.example.ministoreapp.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.ProductGroupEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.ui.components.BarcodeUnrecognizedDialog
import com.example.ministoreapp.ui.components.CameraBarcodeScannerDialog
import com.example.ministoreapp.ui.components.PriceOptionTag
import com.example.ministoreapp.ui.components.PriceStatePickerModal
import kotlin.math.roundToInt

data class PosCartItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val product: ProductEntity,
    var selectedOptionTitle: String,
    var unitPrice: Double,
    var activeTypeKey: String = "RETAIL",
    var quantity: Int = 1
)

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
    productGroups: List<ProductGroupEntity> = emptyList(),
    cashiers: List<CashierEntity> = emptyList(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddToOrder: (RestockItemEntity) -> Unit,
    onUpdateProductStock: (ProductEntity, Int) -> Unit = { _, _ -> },
    onCheckoutCashSale: (items: List<PosCartItem>, total: Double, cashTendered: Double, change: Double) -> Unit = { _, _, _, _ -> },
    onChargeToUtangRequested: (items: List<PosCartItem>, total: Double) -> Unit = { _, _ -> },
    onAddNewProductRequested: (scannedBarcode: String) -> Unit = {},
    onLinkProductBarcode: (product: ProductEntity, code: String, isCase: Boolean) -> Unit = { _, _, _ -> },
    externalScannedBarcode: String? = null,
    onExternalBarcodeConsumed: () -> Unit = {}
) {
    var activeCategoryFilter by remember { mutableStateOf("All") }
    var selectedVariantForRestock by remember { mutableStateOf<VariantGroupModel?>(null) }

    // POS Cart & Scanner States
    val isPosCartEnabled = com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.POS_CART_CHECKOUT)
    val cartItems = remember { mutableStateListOf<PosCartItem>() }
    var showCartSheet by remember { mutableStateOf(false) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var unrecognizedBarcode by remember { mutableStateOf<String?>(null) }
    var activePricePickerGroup by remember { mutableStateOf<VariantGroupModel?>(null) }

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

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else androidx.compose.ui.graphics.Color(0xFFF3F4F6))
            .padding(12.dp)
    ) {
        // Search Bar (Pure White background in Light Theme)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search products, category, alias...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CAMERA_BARCODE_SCANNER)) {
                        IconButton(onClick = { showCameraScannerDialog = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else androidx.compose.ui.graphics.Color(0xFFE5E7EB)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.CUSTOM_DROPDOWNS)) {
            // Filter Chips Row (Pure White inactive chips in Light Theme)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                FilterChip(
                    selected = activeCategoryFilter == category,
                    onClick = { activeCategoryFilter = category },
                    label = { Text(category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
                    )
                )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isTabletLayout = configuration.screenWidthDp >= 600 && com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.TABLET_POS_LAYOUT)
        val isPosCartEnabled = com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.POS_CART_CHECKOUT)

        if (isTabletLayout && isPosCartEnabled) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left 60% Pane: Product Grid
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                    if (filteredVariantGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No products matching '$searchQuery'", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = filteredVariantGroups, key = { it.key }) { variantGroup ->
                                VariantSearchCard(
                                    group = variantGroup,
                                    productGroups = productGroups,
                                    onRestockClick = { selectedVariantForRestock = variantGroup },
                                    onAddToCart = { prod, title, price ->
                                        val idx = cartItems.indexOfFirst { it.product.id == prod.id && it.selectedOptionTitle == title }
                                        if (idx >= 0) {
                                            cartItems[idx] = cartItems[idx].copy(quantity = cartItems[idx].quantity + 1)
                                        } else {
                                            cartItems.add(PosCartItem(product = prod, selectedOptionTitle = title, unitPrice = price))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Right 40% Pane: Docked POS Checkout Panel
                Card(
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    PosCartContent(
                        cartItems = cartItems,
                        cashiers = cashiers,
                        onCheckoutCashSale = onCheckoutCashSale,
                        onChargeToUtangRequested = onChargeToUtangRequested
                    )
                }
            }
        } else {
            // Standard Phone / Non-Split View
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (filteredVariantGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No products matching '$searchQuery'", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = if (isTabletLayout) GridCells.Adaptive(minSize = 320.dp) else GridCells.Fixed(1),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = filteredVariantGroups, key = { it.key }) { variantGroup ->
                                VariantSearchCard(
                                    group = variantGroup,
                                    productGroups = productGroups,
                                    onRestockClick = { selectedVariantForRestock = variantGroup },
                                    onAddToCart = { prod, title, price ->
                                        val idx = cartItems.indexOfFirst { it.product.id == prod.id && it.selectedOptionTitle == title }
                                        if (idx >= 0) {
                                            cartItems[idx] = cartItems[idx].copy(quantity = cartItems[idx].quantity + 1)
                                        } else {
                                            cartItems.add(PosCartItem(product = prod, selectedOptionTitle = title, unitPrice = price))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Active Cart Bar
                if (isPosCartEnabled && cartItems.isNotEmpty()) {
                    val totalQty = cartItems.sumOf { it.quantity }
                    val totalPrice = cartItems.sumOf { it.unitPrice * it.quantity }

                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                            .fillMaxWidth()
                            .clickable { showCartSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                                Column {
                                    Text("$totalQty Items in POS Cart", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
                                    Text("Subtotal: ₱${totalPrice.roundToInt()}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                                }
                            }
                            Button(
                                onClick = { showCartSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("View Cart", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    val handleScannedBarcode = { rawCode: String ->
        val code = rawCode.trim()
        if (code.isNotBlank()) {
            val unitMatches = products.filter { it.barcode?.equals(code, ignoreCase = true) == true || it.comboKey.contains(code, ignoreCase = true) }
            val caseMatches = products.filter { it.caseBarcode?.equals(code, ignoreCase = true) == true }

            when {
                unitMatches.isNotEmpty() -> {
                    val firstMatch = unitMatches.first()
                    val groupModel = variantGroups.find { g -> g.priceOptions.any { it.id == firstMatch.id } }
                    if (groupModel != null && groupModel.priceOptions.size > 1) {
                        activePricePickerGroup = groupModel
                    } else {
                        if (isPosCartEnabled) {
                            val idx = cartItems.indexOfFirst { it.product.id == firstMatch.id }
                            if (idx >= 0) {
                                cartItems[idx] = cartItems[idx].copy(quantity = cartItems[idx].quantity + 1)
                            } else {
                                cartItems.add(PosCartItem(product = firstMatch, selectedOptionTitle = firstMatch.name, unitPrice = firstMatch.sellingPrice, activeTypeKey = firstMatch.variantType ?: "RETAIL"))
                            }
                        } else {
                            onSearchQueryChange(firstMatch.name)
                        }
                    }
                }
                caseMatches.isNotEmpty() -> {
                    val caseMatch = caseMatches.find { it.variantType == "WHOLESALE" || it.variantType == "COLD_WHOLESALE" } ?: caseMatches.first()
                    if (isPosCartEnabled) {
                        val idx = cartItems.indexOfFirst { it.product.id == caseMatch.id }
                        if (idx >= 0) {
                            cartItems[idx] = cartItems[idx].copy(quantity = cartItems[idx].quantity + 1)
                        } else {
                            cartItems.add(PosCartItem(product = caseMatch, selectedOptionTitle = "${caseMatch.name} (Case)", unitPrice = caseMatch.sellingPrice, activeTypeKey = "WHOLESALE"))
                        }
                    } else {
                        onSearchQueryChange(caseMatch.name)
                    }
                }
                else -> {
                    unrecognizedBarcode = code
                }
            }
        }
    }

    LaunchedEffect(externalScannedBarcode) {
        externalScannedBarcode?.let { code ->
            if (code.isNotBlank()) {
                handleScannedBarcode(code)
                onExternalBarcodeConsumed()
            }
        }
    }

    // Camera Barcode Scanner Overlay
    if (showCameraScannerDialog) {
        CameraBarcodeScannerDialog(
            title = "Scan Product Barcode",
            onDismiss = { showCameraScannerDialog = false },
            onBarcodeScanned = { scannedCode ->
                showCameraScannerDialog = false
                handleScannedBarcode(scannedCode)
            }
        )
    }

    // Unrecognized Barcode Dialog
    unrecognizedBarcode?.let { code ->
        BarcodeUnrecognizedDialog(
            scannedBarcode = code,
            allProducts = products,
            onDismiss = { unrecognizedBarcode = null },
            onLinkAsUnitBarcode = { targetProd, scanned ->
                onLinkProductBarcode(targetProd, scanned, false)
                unrecognizedBarcode = null
            },
            onLinkAsCaseBarcode = { targetProd, scanned ->
                onLinkProductBarcode(targetProd, scanned, true)
                unrecognizedBarcode = null
            },
            onAddNewProduct = { scanned ->
                unrecognizedBarcode = null
                onAddNewProductRequested(scanned)
            }
        )
    }

    // Price State Picker Modal
    activePricePickerGroup?.let { groupModel ->
        val options = groupModel.priceOptions.map { opt ->
            val iconStr = when (opt.variantType) {
                "WHOLESALE" -> "📦"
                "COLD_WHOLESALE" -> "🧊"
                "COLD_RETAIL" -> "❄️"
                else -> "🏷️"
            }
            val titleStr = when (opt.variantType) {
                "WHOLESALE" -> "Case (${opt.unit.ifBlank { "Case" }})"
                "COLD_WHOLESALE" -> "Cold Case"
                "COLD_RETAIL" -> "Cold Retail"
                else -> "Retail (${opt.unit.ifBlank { "Piece" }})"
            }
            PriceOptionTag(
                product = opt,
                tagTitle = titleStr,
                price = opt.sellingPrice,
                icon = iconStr,
                typeKey = opt.variantType ?: "RETAIL"
            )
        }

        PriceStatePickerModal(
            productName = groupModel.title,
            options = options,
            onDismiss = { activePricePickerGroup = null },
            onOptionSelected = { selectedTag ->
                activePricePickerGroup = null
                if (isPosCartEnabled) {
                    val idx = cartItems.indexOfFirst { it.product.id == selectedTag.product.id && it.selectedOptionTitle == selectedTag.tagTitle }
                    if (idx >= 0) {
                        cartItems[idx] = cartItems[idx].copy(quantity = cartItems[idx].quantity + 1)
                    } else {
                        cartItems.add(PosCartItem(product = selectedTag.product, selectedOptionTitle = selectedTag.tagTitle, unitPrice = selectedTag.price, activeTypeKey = selectedTag.typeKey))
                    }
                } else {
                    onSearchQueryChange(selectedTag.product.name)
                }
            }
        )
    }

    // Cart Bottom Sheet
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                PosCartContent(
                    cartItems = cartItems,
                    cashiers = cashiers,
                    onCheckoutCashSale = { items, total, cash, change ->
                        onCheckoutCashSale(items, total, cash, change)
                        showCartSheet = false
                    },
                    onChargeToUtangRequested = { items, total ->
                        onChargeToUtangRequested(items, total)
                        showCartSheet = false
                    }
                )
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
    productGroups: List<ProductGroupEntity> = emptyList(),
    onRestockClick: () -> Unit,
    onAddToCart: (product: ProductEntity, optionTitle: String, price: Double) -> Unit = { _, _, _ -> }
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val firstOpt = group.priceOptions.firstOrNull()
    val parentGroup = remember(firstOpt, productGroups) {
        productGroups.find { it.id == firstOpt?.groupId }
    }
    val totalStock = group.priceOptions.sumOf { it.stockQty }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else androidx.compose.ui.graphics.Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Title & Restock Button (Vendor Info Removed)
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
                    if (totalStock > 0) {
                        Text(
                            text = "📦 Stock: $totalStock pcs",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (com.example.ministoreapp.domain.FeatureFlagManager.isFeatureActive(com.example.ministoreapp.domain.AppFeature.RESTOCK_MODULE)) {
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
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Restock",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restock", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price Badges Row (Separated Retail & Bundle Tags, Doubled Size)
            val badges = remember(group.priceOptions, parentGroup) {
                val list = mutableListOf<Triple<String, String, String>>() // icon, label, variantType key
                val sorted = group.priceOptions.sortedBy { option ->
                    when (option.variantType) {
                        "COLD_RETAIL" -> 1
                        "RETAIL" -> 2
                        "COLD_WHOLESALE" -> 3
                        "WHOLESALE" -> 4
                        else -> 5
                    }
                }
                for (option in sorted) {
                    when (option.variantType) {
                        "WHOLESALE" -> list.add(Triple("📦", "₱${option.sellingPrice.roundToInt()} (${option.unit.ifBlank { "Case" }})", "WHOLESALE"))
                        "COLD_WHOLESALE" -> list.add(Triple("🧊", "₱${option.sellingPrice.roundToInt()} (Cold Case)", "COLD_WHOLESALE"))
                        "COLD_RETAIL" -> list.add(Triple("❄️", "₱${option.sellingPrice.roundToInt()} (Cold)", "COLD_RETAIL"))
                        else -> {
                            if (parentGroup != null && parentGroup.bundlePrice > 0) {
                                list.add(Triple("🛍️", "${parentGroup.bundleQty} for ₱${parentGroup.bundlePrice.roundToInt()} (Bundle)", "BUNDLE"))
                                list.add(Triple("🏷️", "₱${option.sellingPrice.roundToInt()} / pc", "RETAIL"))
                            } else {
                                list.add(Triple("🏷️", "₱${option.sellingPrice.roundToInt()}", "RETAIL"))
                            }
                        }
                    }
                }
                list
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(badges) { (badgeIcon, badgeLabel, badgeType) ->
                    val (bgFill, borderCol, textCol) = when (badgeType) {
                        "COLD_RETAIL" -> if (isDark) {
                            Triple(Color(0xFF38BDF8).copy(alpha = 0.15f), Color(0xFF38BDF8).copy(alpha = 0.75f), Color(0xFF7DD3FC))
                        } else {
                            Triple(Color(0xFF0284C7).copy(alpha = 0.12f), Color(0xFF0284C7).copy(alpha = 0.65f), Color(0xFF0369A1))
                        }
                        "COLD_WHOLESALE" -> if (isDark) {
                            Triple(Color(0xFF818CF8).copy(alpha = 0.16f), Color(0xFF818CF8).copy(alpha = 0.80f), Color(0xFFA5B4FC))
                        } else {
                            Triple(Color(0xFF6366F1).copy(alpha = 0.12f), Color(0xFF6366F1).copy(alpha = 0.65f), Color(0xFF4338CA))
                        }
                        "BUNDLE" -> if (isDark) {
                            Triple(Color(0xFF4ADE80).copy(alpha = 0.15f), Color(0xFF4ADE80).copy(alpha = 0.75f), Color(0xFF86EFAC))
                        } else {
                            Triple(Color(0xFF22C55E).copy(alpha = 0.14f), Color(0xFF16A34A).copy(alpha = 0.65f), Color(0xFF15803D))
                        }
                        "WHOLESALE" -> if (isDark) {
                            Triple(Color(0xFFFACC15).copy(alpha = 0.15f), Color(0xFFFACC15).copy(alpha = 0.75f), Color(0xFFFDE047))
                        } else {
                            Triple(Color(0xFFEAB308).copy(alpha = 0.16f), Color(0xFFD97706).copy(alpha = 0.65f), Color(0xFFB45309))
                        }
                        else -> if (isDark) { // RETAIL
                            Triple(Color(0xFFFFFFFF).copy(alpha = 0.06f), Color(0xFF49454F), Color(0xFFF3F4F6))
                        } else {
                            Triple(Color(0xFF000000).copy(alpha = 0.05f), Color(0xFFCAC4D0), Color(0xFF111827))
                        }
                    }

                    val targetOpt = group.priceOptions.find {
                        when (badgeType) {
                            "WHOLESALE" -> it.variantType == "WHOLESALE"
                            "COLD_WHOLESALE" -> it.variantType == "COLD_WHOLESALE"
                            "COLD_RETAIL" -> it.variantType == "COLD_RETAIL"
                            else -> it.variantType == "RETAIL" || it.variantType == null
                        }
                    } ?: group.priceOptions.first()

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bgFill,
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.clickable {
                            onAddToCart(targetOpt, badgeLabel, targetOpt.sellingPrice)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(badgeIcon, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textCol
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

@Composable
fun PosCartContent(
    cartItems: MutableList<PosCartItem>,
    cashiers: List<CashierEntity>,
    onCheckoutCashSale: (items: List<PosCartItem>, total: Double, cashTendered: Double, change: Double) -> Unit,
    onChargeToUtangRequested: (items: List<PosCartItem>, total: Double) -> Unit
) {
    val total = cartItems.sumOf { it.unitPrice * it.quantity }
    var cashTenderedInput by remember { mutableStateOf("") }
    val cashTendered = cashTenderedInput.toDoubleOrNull() ?: 0.0
    val change = (cashTendered - total).coerceAtLeast(0.0)

    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("POS Active Checkout Cart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (cartItems.isNotEmpty()) {
                TextButton(onClick = { cartItems.clear() }) {
                    Text("Clear Cart", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cart is empty. Tap items in Search to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
            ) {
                items(cartItems, key = { it.id }) { item ->
                    val badgeLabel = when (item.activeTypeKey) {
                        "COLD_RETAIL" -> "❄️ Cold"
                        "COLD_WHOLESALE" -> "❄️📦 Cold Case"
                        "WHOLESALE" -> "📦 Case"
                        "BUNDLE" -> "🛍️ Bundle"
                        else -> "🏷️ Retail"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable {
                                        // Cycle through available pricing options
                                        val idx = cartItems.indexOf(item)
                                        if (idx >= 0) {
                                            when (item.activeTypeKey) {
                                                "RETAIL" -> {
                                                    cartItems[idx] = item.copy(
                                                        selectedOptionTitle = "${item.product.name} (Cold)",
                                                        unitPrice = item.product.sellingPrice + 5.0,
                                                        activeTypeKey = "COLD_RETAIL"
                                                    )
                                                }
                                                "COLD_RETAIL" -> {
                                                    cartItems[idx] = item.copy(
                                                        selectedOptionTitle = "${item.product.name} (Case)",
                                                        unitPrice = item.product.sellingPrice * (item.product.itemsPerWholesalePack ?: 12),
                                                        activeTypeKey = "WHOLESALE"
                                                    )
                                                }
                                                else -> {
                                                    cartItems[idx] = item.copy(
                                                        selectedOptionTitle = item.product.name,
                                                        unitPrice = item.product.sellingPrice,
                                                        activeTypeKey = "RETAIL"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("₱${item.unitPrice.roundToInt()} / unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    val idx = cartItems.indexOf(item)
                                    if (idx >= 0) {
                                        if (item.quantity > 1) {
                                            cartItems[idx] = item.copy(quantity = item.quantity - 1)
                                        } else {
                                            cartItems.removeAt(idx)
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }

                            Text("${item.quantity}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                            IconButton(
                                onClick = {
                                    val idx = cartItems.indexOf(item)
                                    if (idx >= 0) {
                                        cartItems[idx] = item.copy(quantity = item.quantity + 1)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { cartItems.remove(item) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Amount:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("₱${total.roundToInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = cashTenderedInput,
                onValueChange = { cashTenderedInput = it },
                label = { Text("Cash Tendered (₱)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (cashTendered > 0) {
                Text("Change: ₱${change.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onCheckoutCashSale(cartItems, total, cashTendered, change)
                        cartItems.clear()
                        cashTenderedInput = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cash Checkout", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onChargeToUtangRequested(cartItems, total)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Charge Utang", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
