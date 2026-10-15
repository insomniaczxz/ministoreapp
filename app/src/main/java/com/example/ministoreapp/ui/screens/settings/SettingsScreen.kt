package com.example.ministoreapp.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.StoreEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import com.example.ministoreapp.domain.PricingEngine
import com.example.ministoreapp.ui.components.CashierVerificationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securitySettings: SecuritySettingsEntity?,
    products: List<ProductEntity>,
    cashiers: List<CashierEntity>,
    auditLogs: List<AuditLogEntity>,
    banRecords: List<BanRecordEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onSaveProduct: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit,
    onDeleteProduct: (ProductEntity, CashierEntity) -> Unit,
    onSaveCashier: (CashierEntity) -> Unit,
    onDeleteCashier: (CashierEntity) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onSaveMasterData: (type: String, name: String) -> Unit,
    onDeleteMasterData: (type: String, id: String) -> Unit,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit,
    onUpdateSecurityPin: (oldPin: String, newPin: String) -> Boolean
) {
    var activeDetailView by remember { mutableStateOf<String?>(null) }
    val currentSettings = securitySettings ?: SecuritySettingsEntity()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeDetailView != null) {
            // Sub-screen header with Back button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeDetailView = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = getDetailTitle(activeDetailView),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (activeDetailView) {
                    "PRODUCTS" -> ProductsManagementTab(
                        products = products,
                        cashiers = cashiers,
                        stores = stores,
                        categories = categories,
                        brands = brands,
                        units = units,
                        onSaveProduct = onSaveProduct,
                        onDeleteProduct = onDeleteProduct
                    )
                    "CASHIERS" -> CashiersManagementTab(
                        cashiers = cashiers,
                        onSaveCashier = onSaveCashier,
                        onDeleteCashier = onDeleteCashier
                    )
                    "DROPDOWNS", "VENDOR_STORES" -> MasterDataManagementTab(
                        stores = stores,
                        categories = categories,
                        brands = brands,
                        units = units,
                        banRecords = banRecords,
                        cashiers = cashiers,
                        onSaveMasterData = onSaveMasterData,
                        onDeleteMasterData = onDeleteMasterData,
                        onSaveBanRecord = onSaveBanRecord,
                        onDeleteBanRecord = onDeleteBanRecord
                    )
                    "STORE_PROFILE" -> SecurityManagementTab(
                        securitySettings = securitySettings,
                        onUpdateSecuritySettings = onUpdateSecuritySettings,
                        onUpdateSecurityPin = onUpdateSecurityPin
                    )
                    "BAN_LIST" -> BanRecordsSection(banRecords, cashiers, onSaveBanRecord, onDeleteBanRecord)
                    "AUDIT_LOGS" -> AuditLogsTab(auditLogs = auditLogs)
                    "WHOLESALE" -> WholesaleManagementPlaceholder(products)
                    "SYNC" -> SyncBackupPlaceholder(securitySettings)
                    "ABOUT" -> AboutAppPlaceholder()
                }
            }
        } else {
            // Main Tiled List View (Matching 2nd image)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    SettingsTileRow(
                        title = "Store profile",
                        subtitle = currentSettings.storeName,
                        icon = Icons.Default.Storefront,
                        onClick = { activeDetailView = "STORE_PROFILE" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "Products & pricing",
                        subtitle = "${products.size} live products · pricing engine",
                        icon = Icons.Default.Inventory2,
                        onClick = { activeDetailView = "PRODUCTS" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "Cashiers & PIN",
                        subtitle = "${cashiers.size} active · 6-digit PIN set",
                        icon = Icons.Default.People,
                        onClick = { activeDetailView = "CASHIERS" }
                    )
                }

                item {
                    // Dark Theme Toggle Tile
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Dark Theme",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDarkTheme) "Dark mode active" else "Light mode active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onToggleDarkTheme
                            )
                        }
                    }
                }

                item {
                    SettingsTileRow(
                        title = "Vendor stores",
                        subtitle = "${stores.size} stores used for recommendations",
                        icon = Icons.Default.Store,
                        onClick = { activeDetailView = "VENDOR_STORES" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "Dropdown options",
                        subtitle = "${categories.size} categories · ${brands.size} brands",
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        onClick = { activeDetailView = "DROPDOWNS" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "Ban list",
                        subtitle = "${banRecords.size} people flagged",
                        icon = Icons.Default.Block,
                        onClick = { activeDetailView = "BAN_LIST" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "Sync & backup",
                        subtitle = "Google Drive delta sync & local backup",
                        icon = Icons.Default.Sync,
                        onClick = { activeDetailView = "SYNC" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "Audit logs",
                        subtitle = "${auditLogs.size} recorded actions",
                        icon = Icons.Default.Shield,
                        onClick = { activeDetailView = "AUDIT_LOGS" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "Wholesale catalog",
                        subtitle = "Bulk pack pricing & child unit calculation",
                        icon = Icons.Default.Inventory,
                        onClick = { activeDetailView = "WHOLESALE" }
                    )
                }

                item {
                    SettingsTileRow(
                        title = "About this app",
                        subtitle = "POS version 1.0 · offline-first engine",
                        icon = Icons.Default.Info,
                        onClick = { activeDetailView = "ABOUT" }
                    )
                }
            }
        }
    }
}

private fun getDetailTitle(key: String?): String {
    return when (key) {
        "PRODUCTS" -> "Products & Pricing"
        "CASHIERS" -> "Cashiers & PIN"
        "DROPDOWNS" -> "Dropdown Options & Master Data"
        "VENDOR_STORES" -> "Vendor Stores"
        "STORE_PROFILE" -> "Store Profile & Security"
        "BAN_LIST" -> "Ban List"
        "AUDIT_LOGS" -> "Audit Logs"
        "WHOLESALE" -> "Wholesale Catalog"
        "SYNC" -> "Sync & Backup"
        "ABOUT" -> "About This App"
        else -> "Settings"
    }
}

@Composable
fun SettingsTileRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ProductsManagementTab(
    products: List<ProductEntity>,
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    onSaveProduct: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit,
    onDeleteProduct: (ProductEntity, CashierEntity) -> Unit
) {
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddWholesaleDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { showAddProductDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Product", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { showAddWholesaleDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Wholesale", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products, key = { it.id }) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${product.name}${if (product.unitSize.isNotBlank()) " ${product.unitSize}" else ""}${if (product.unit.isNotBlank()) " ${product.unit}" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cost: ₱${"%.2f".format(product.cost)} | Selling: ₱${"%.2f".format(product.sellingPrice)} (${product.markupType})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (product.hasVariants) {
                                Text(text = "• Parent Product (Variants Active)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        IconButton(onClick = { productToDelete = product }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            cashiers = cashiers,
            stores = stores,
            categories = categories,
            brands = brands,
            units = units,
            onDismiss = { showAddProductDialog = false },
            onSave = { prod, cashier, variants ->
                onSaveProduct(prod, cashier, variants)
                showAddProductDialog = false
            }
        )
    }

    if (showAddWholesaleDialog) {
        AddWholesaleProductDialog(
            cashiers = cashiers,
            stores = stores,
            categories = categories,
            brands = brands,
            units = units,
            onDismiss = { showAddWholesaleDialog = false },
            onSave = { prod, cashier, variants ->
                onSaveProduct(prod, cashier, variants)
                showAddWholesaleDialog = false
            }
        )
    }

    productToDelete?.let { prod ->
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Delete Product '${prod.name}'",
            onDismiss = { productToDelete = null },
            onVerified = { cashier ->
                onDeleteProduct(prod, cashier)
                productToDelete = null
            }
        )
    }
}

@Composable
fun AddProductDialog(
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var unitSize by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }

    // Markup choice: FIXED, PERCENTAGE, MANUAL
    var markupChoice by remember { mutableStateOf("FIXED") } // "FIXED", "PERCENTAGE", "MANUAL"
    var percentageInput by remember { mutableStateOf("") }
    var manualPriceInput by remember { mutableStateOf("") }

    // Variant Checkbox
    var hasVariantsChecked by remember { mutableStateOf(false) }
    var customVariantName by remember { mutableStateOf("") }
    var customVariantPriceInput by remember { mutableStateOf("") }

    // Beverage Checkbox (disabled/hidden if hasVariants is checked)
    var isBeverageChecked by remember { mutableStateOf(false) }
    var normalPriceInput by remember { mutableStateOf("") }
    var coldPriceInput by remember { mutableStateOf("") }

    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val cost = costInput.toDoubleOrNull() ?: 0.0
    val percentageVal = percentageInput.toDoubleOrNull() ?: 0.0
    val manualVal = manualPriceInput.toDoubleOrNull()

    // Calculated price from formula or override
    val calculatedPrice = remember(cost, markupChoice, percentageVal, manualVal) {
        if (manualVal != null && manualVal > 0.0) {
            manualVal
        } else {
            PricingEngine.calculateSellingPrice(cost, markupChoice, percentageVal, manualVal)
        }
    }

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Product", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = unitSize, onValueChange = { unitSize = it }, label = { Text("Unit Size (1.5L)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (Bottle)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Vendor Store") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it },
                            label = { Text("Item Cost (₱)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("Markup Choice:", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = markupChoice == "FIXED",
                                onClick = { markupChoice = "FIXED" },
                                label = { Text("Fixed Ladder") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilterChip(
                                selected = markupChoice == "PERCENTAGE",
                                onClick = { markupChoice = "PERCENTAGE" },
                                label = { Text("Percentage") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    if (markupChoice == "PERCENTAGE") {
                        item {
                            OutlinedTextField(
                                value = percentageInput,
                                onValueChange = { percentageInput = it },
                                label = { Text("Markup %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = manualPriceInput,
                            onValueChange = { manualPriceInput = it },
                            label = { Text("Manual Selling Price (₱) - Override") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("Final Price: ₱${"%.2f".format(calculatedPrice)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Variant Checkbox
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = hasVariantsChecked,
                                onCheckedChange = {
                                    hasVariantsChecked = it
                                    if (it) isBeverageChecked = false // Beverage disabled on parent if variants active
                                }
                            )
                            Text("Has Variants / Colors / Flavors", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (hasVariantsChecked) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Add Child Variant:", fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = customVariantName, onValueChange = { customVariantName = it }, label = { Text("Variant Name / Color") }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = customVariantPriceInput, onValueChange = { customVariantPriceInput = it }, label = { Text("Variant Price Override (₱) - Optional") }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }

                    // Beverage Checkbox (Available if Variant is unchecked)
                    if (!hasVariantsChecked) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isBeverageChecked, onCheckedChange = { isBeverageChecked = it })
                                Text("Beverage (Enable Normal/Cold options)")
                            }
                        }

                        if (isBeverageChecked) {
                            item {
                                OutlinedTextField(value = normalPriceInput, onValueChange = { normalPriceInput = it }, label = { Text("Normal Price (₱) - Optional") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            }
                            item {
                                OutlinedTextField(value = coldPriceInput, onValueChange = { coldPriceInput = it }, label = { Text("Cold Price (₱) - Optional") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { verifiedCashier = cashiers.firstOrNull() }, enabled = name.isNotBlank() && cost > 0, shape = RoundedCornerShape(12.dp)) {
                    Text("Verify & Save", fontWeight = FontWeight.Bold)
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
            actionTitle = "Verify Cashier to Save Product",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val parentProduct = ProductEntity(
                    name = name,
                    category = category,
                    brand = brand,
                    unit = unit,
                    unitSize = unitSize,
                    storeName = storeName,
                    cost = cost,
                    markupType = markupChoice,
                    markupValue = percentageVal,
                    manualSellingPrice = manualVal,
                    sellingPrice = calculatedPrice,
                    isBeverage = isBeverageChecked,
                    hasVariants = hasVariantsChecked
                )

                val variantsList = mutableListOf<ProductEntity>()

                if (hasVariantsChecked && customVariantName.isNotBlank()) {
                    val variantPrice = customVariantPriceInput.toDoubleOrNull() ?: calculatedPrice
                    variantsList.add(
                        ProductEntity(
                            name = "$name $customVariantName",
                            category = category,
                            brand = brand,
                            unit = unit,
                            unitSize = unitSize,
                            storeName = storeName,
                            cost = cost,
                            markupType = "MANUAL",
                            manualSellingPrice = variantPrice,
                            sellingPrice = variantPrice,
                            parentProductId = parentProduct.id,
                            variantType = "CUSTOM_VARIANT"
                        )
                    )
                } else if (isBeverageChecked) {
                    val beverageVariants = PricingEngine.generateBeverageVariants(
                        parentProduct = parentProduct,
                        normalPrice = normalPriceInput.toDoubleOrNull(),
                        coldPrice = coldPriceInput.toDoubleOrNull()
                    )
                    variantsList.addAll(beverageVariants)
                }

                onSave(parentProduct, cashier, variantsList)
            }
        )
    }
}

@Composable
fun AddWholesaleProductDialog(
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, CashierEntity, List<ProductEntity>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pack") }
    var unitSize by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var wholesaleCostInput by remember { mutableStateOf("") }
    var itemsPerPackInput by remember { mutableStateOf("12") }

    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    val wholesaleCost = wholesaleCostInput.toDoubleOrNull() ?: 0.0
    val itemsPerPack = itemsPerPackInput.toIntOrNull() ?: 1
    val childCost = PricingEngine.calculateWholesaleChildCost(wholesaleCost, itemsPerPack)
    val childSellingPrice = PricingEngine.calculateSellingPrice(childCost, "FIXED")

    if (verifiedCashier == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Wholesale Product", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Wholesale Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = wholesaleCostInput, onValueChange = { wholesaleCostInput = it }, label = { Text("Wholesale Cost (₱)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = itemsPerPackInput, onValueChange = { itemsPerPackInput = it }, label = { Text("Items per Wholesale Pack") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

                    Text("Calculated Child Cost: ₱${"%.2f".format(childCost)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Calculated Child Selling Price: ₱${"%.2f".format(childSellingPrice)}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(onClick = { verifiedCashier = cashiers.firstOrNull() }, enabled = name.isNotBlank() && wholesaleCost > 0, shape = RoundedCornerShape(12.dp)) {
                    Text("Verify & Save Wholesale", fontWeight = FontWeight.Bold)
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
            actionTitle = "Verify Cashier to Save Wholesale Product",
            onDismiss = onDismiss,
            onVerified = { cashier ->
                val parentProduct = ProductEntity(
                    name = "$name (Wholesale Pack)",
                    category = category,
                    brand = brand,
                    unit = unit,
                    unitSize = "$itemsPerPack items",
                    storeName = storeName,
                    cost = wholesaleCost,
                    markupType = "FIXED",
                    sellingPrice = PricingEngine.calculateSellingPrice(wholesaleCost, "FIXED"),
                    isWholesaleParent = true,
                    itemsPerWholesalePack = itemsPerPack,
                    hasVariants = true
                )

                val childVariant = ProductEntity(
                    name = name,
                    category = category,
                    brand = brand,
                    unit = "pcs",
                    unitSize = "1s",
                    storeName = storeName,
                    cost = childCost,
                    markupType = "FIXED",
                    sellingPrice = childSellingPrice,
                    parentProductId = parentProduct.id,
                    variantType = "WHOLESALE_CHILD"
                )

                onSave(parentProduct, cashier, listOf(childVariant))
            }
        )
    }
}

@Composable
fun CashiersManagementTab(
    cashiers: List<CashierEntity>,
    onSaveCashier: (CashierEntity) -> Unit,
    onDeleteCashier: (CashierEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add New Cashier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Cashier Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it }, label = { Text("4-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onSaveCashier(CashierEntity(name = name, pin = pin))
                        name = ""
                        pin = ""
                    },
                    enabled = name.isNotBlank() && pin.length == 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add Cashier", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Active Cashiers List:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cashiers, key = { it.id }) { cashier ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = cashier.name, fontWeight = FontWeight.Bold)
                            Text(text = "PIN: **** (${cashier.pin.take(1)}***)", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDeleteCashier(cashier) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterDataManagementTab(
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    onSaveMasterData: (type: String, name: String) -> Unit,
    onDeleteMasterData: (type: String, id: String) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit
) {
    var selectedDataTab by remember { mutableIntStateOf(0) }
    val dataTabs = listOf("Stores", "Categories", "Brands", "Units")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedDataTab, edgePadding = 0.dp) {
            dataTabs.forEachIndexed { idx, title ->
                Tab(selected = selectedDataTab == idx, onClick = { selectedDataTab = idx }, text = { Text(title, fontWeight = FontWeight.SemiBold) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedDataTab) {
            0 -> SimpleMasterDataSection("Store", stores.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            1 -> SimpleMasterDataSection("Category", categories.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            2 -> SimpleMasterDataSection("Brand", brands.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            3 -> SimpleMasterDataSection("Unit", units.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
        }
    }
}

@Composable
fun BanRecordsSection(
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var verifiedCashier by remember { mutableStateOf<CashierEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Ban Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Alias") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = fullName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add Ban Record", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(banRecords, key = { it.id }) { ban ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = ban.fullName, fontWeight = FontWeight.Bold)
                            Text(text = "Reason: ${ban.reason}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    verifiedCashier?.let {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Add Ban Record",
            onDismiss = { verifiedCashier = null },
            onVerified = { cashier ->
                onSaveBanRecord(BanRecordEntity(fullName = fullName, nickname = nickname, reason = reason, cashierId = cashier.id, cashierName = cashier.name), cashier)
                fullName = ""
                nickname = ""
                reason = ""
                verifiedCashier = null
            }
        )
    }
}

@Composable
fun SimpleMasterDataSection(
    typeLabel: String,
    itemsList: List<Pair<String, String>>,
    onSave: (String, String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("New $typeLabel Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(typeLabel, nameInput)
                        nameInput = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(itemsList, key = { it.first }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.second, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onDelete(typeLabel, item.first) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityManagementTab(
    securitySettings: SecuritySettingsEntity?,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit,
    onUpdateSecurityPin: (String, String) -> Boolean
) {
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    val currentSettings = securitySettings ?: SecuritySettingsEntity()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Security Protections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require PIN on App Launch", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = currentSettings.pinOnAppLaunchEnabled,
                        onCheckedChange = { onUpdateSecuritySettings(currentSettings.copy(pinOnAppLaunchEnabled = it)) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require PIN on Opening Settings", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = currentSettings.pinOnSettingsOpeningEnabled,
                        onCheckedChange = { onUpdateSecuritySettings(currentSettings.copy(pinOnSettingsOpeningEnabled = it)) }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Change 6-Digit Security PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = currentPinInput, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPinInput = it }, label = { Text("Current 6-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newPinInput, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it }, label = { Text("New 6-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

                pinMessage?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = {
                        val success = onUpdateSecurityPin(currentPinInput, newPinInput)
                        if (success) {
                            pinMessage = "Security PIN updated successfully!"
                            currentPinInput = ""
                            newPinInput = ""
                        } else {
                            pinMessage = "Incorrect current PIN."
                        }
                    },
                    enabled = currentPinInput.length == 6 && newPinInput.length == 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update PIN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AuditLogsTab(auditLogs: List<AuditLogEntity>) {
    if (auditLogs.isEmpty()) {
        Text("No audit log records found.", style = MaterialTheme.typography.bodyMedium)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auditLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.actionType.contains("DELETE")) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = log.actionType, fontWeight = FontWeight.Bold)
                        Text(text = log.description, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Cashier: ${log.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun WholesaleManagementPlaceholder(products: List<ProductEntity>) {
    val wholesaleItems = products.filter { it.isWholesaleParent || (it.itemsPerWholesalePack ?: 0) > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Wholesale Catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (wholesaleItems.isEmpty()) {
            Text("No wholesale items configured yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wholesaleItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = item.name, fontWeight = FontWeight.Bold)
                            Text(text = "Wholesale Price: ₱${"%.2f".format(item.sellingPrice)} | Pack Items: ${item.itemsPerWholesalePack ?: 1}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncBackupPlaceholder(securitySettings: SecuritySettingsEntity?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sync & Local Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Google Drive Delta Sync Engine: Active", fontWeight = FontWeight.Bold)
                Text("Automatic local database pre-sync backups enabled.")
                Text("Store Name: ${securitySettings?.storeName ?: "Default Store"}")
            }
        }
    }
}

@Composable
fun AboutAppPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("About This App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("G&J Store POS & Inventory Engine", fontWeight = FontWeight.Bold)
                Text("Version 1.0 (Build 2026)")
                Text("Offline-First Room Engine with Google Drive Delta Sync")
                Text("Design: Modern Jetpack Compose Daylight / Dark Theme")
            }
        }
    }
}
