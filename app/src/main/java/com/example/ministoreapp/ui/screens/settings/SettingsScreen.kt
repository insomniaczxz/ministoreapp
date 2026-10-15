package com.example.ministoreapp.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.ContainerEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.ProductGroupEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.StoreEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import com.example.ministoreapp.data.local.entities.WholesaleUnitEntity
import com.example.ministoreapp.domain.AppReleaseInfo
import com.example.ministoreapp.domain.AppUpdateManager
import com.example.ministoreapp.domain.PricingEngine
import com.example.ministoreapp.ui.components.CashierVerificationDialog
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    activeDetailView: String? = null,
    onActiveDetailViewChange: (String?) -> Unit = {},
    securitySettings: SecuritySettingsEntity?,
    products: List<ProductEntity>,
    productGroups: List<ProductGroupEntity> = emptyList(),
    cashiers: List<CashierEntity>,
    auditLogs: List<AuditLogEntity>,
    banRecords: List<BanRecordEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onSaveProductGroup: (group: ProductGroupEntity, generatedItems: List<ProductEntity>, cashier: CashierEntity) -> Unit,
    onDeleteSingleProduct: (product: ProductEntity, cashier: CashierEntity) -> Unit,
    onDeleteProductGroup: (groupId: String, cashier: CashierEntity) -> Unit,
    onSaveCashier: (CashierEntity, performerName: String, reason: String) -> Unit,
    onDeleteCashier: (CashierEntity, performerName: String, reason: String) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onSaveMasterData: (type: String, name: String) -> Unit,
    onDeleteMasterData: (type: String, id: String) -> Unit,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit
) {
    val currentSettings = securitySettings ?: SecuritySettingsEntity()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeDetailView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (activeDetailView) {
                    "PRODUCTS" -> ProductsManagementTab(
                        products = products,
                        productGroups = productGroups,
                        cashiers = cashiers,
                        stores = stores,
                        categories = categories,
                        brands = brands,
                        units = units,
                        containers = containers,
                        wholesaleUnits = wholesaleUnits,
                        onSaveProductGroup = onSaveProductGroup,
                        onDeleteSingleProduct = onDeleteSingleProduct,
                        onDeleteProductGroup = onDeleteProductGroup
                    )
                    "CASHIERS" -> CashiersManagementTab(
                        cashiers = cashiers,
                        securitySettings = currentSettings,
                        onSaveCashier = onSaveCashier,
                        onDeleteCashier = onDeleteCashier
                    )
                    "DROPDOWNS", "VENDOR_STORES" -> MasterDataManagementTab(
                        stores = stores,
                        categories = categories,
                        brands = brands,
                        units = units,
                        containers = containers,
                        wholesaleUnits = wholesaleUnits,
                        banRecords = banRecords,
                        cashiers = cashiers,
                        onSaveMasterData = onSaveMasterData,
                        onDeleteMasterData = onDeleteMasterData,
                        onSaveBanRecord = onSaveBanRecord,
                        onDeleteBanRecord = onDeleteBanRecord
                    )
                    "SECURITY" -> SecurityManagementTab(
                        securitySettings = securitySettings,
                        onUpdateSecuritySettings = onUpdateSecuritySettings
                    )
                    "BAN_LIST" -> BanRecordsSection(banRecords, cashiers, onSaveBanRecord, onDeleteBanRecord)
                    "SYNC" -> SyncBackupPlaceholder(securitySettings)
                    "AUDIT_LOGS" -> AuditLogsTab(auditLogs = auditLogs)
                    "STORE_PROFILE" -> StoreProfileManagementTab(
                        securitySettings = securitySettings,
                        onUpdateSecuritySettings = onUpdateSecuritySettings
                    )
                    "ABOUT" -> AboutAppPlaceholder()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Products
                item {
                    SettingsTileRow(
                        title = "Products",
                        subtitle = "${products.size} live product rows · bulk calculator",
                        icon = Icons.Default.Inventory2,
                        onClick = { onActiveDetailViewChange("PRODUCTS") }
                    )
                }

                // 2. Cashiers & PIN
                item {
                    SettingsTileRow(
                        title = "Cashiers & PIN",
                        subtitle = "${cashiers.size} active · 6-digit PIN set",
                        icon = Icons.Default.People,
                        onClick = { onActiveDetailViewChange("CASHIERS") }
                    )
                }

                // 3. Dark Theme Switch
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
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

                // 4. Vendor stores
                item {
                    SettingsTileRow(
                        title = "Vendor stores",
                        subtitle = "${stores.size} stores used for recommendations",
                        icon = Icons.Default.Store,
                        onClick = { onActiveDetailViewChange("VENDOR_STORES") }
                    )
                }

                // 5. Dropdown options
                item {
                    SettingsTileRow(
                        title = "Dropdown options",
                        subtitle = "${categories.size} categories · ${brands.size} brands · ${containers.size} containers",
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        onClick = { onActiveDetailViewChange("DROPDOWNS") }
                    )
                }

                // 6. Ban list
                item {
                    SettingsTileRow(
                        title = "Ban list",
                        subtitle = "${banRecords.size} people flagged",
                        icon = Icons.Default.Block,
                        onClick = { onActiveDetailViewChange("BAN_LIST") }
                    )
                }

                // 7. Security
                item {
                    SettingsTileRow(
                        title = "Security",
                        subtitle = "App launch, settings PIN & 6-digit PIN change",
                        icon = Icons.Default.Lock,
                        onClick = { onActiveDetailViewChange("SECURITY") }
                    )
                }

                // 8. Sync & backup
                item {
                    SettingsTileRow(
                        title = "Sync & backup",
                        subtitle = "Google Drive delta sync & local backup",
                        icon = Icons.Default.Sync,
                        onClick = { onActiveDetailViewChange("SYNC") }
                    )
                }

                // 9. Audit logs
                item {
                    SettingsTileRow(
                        title = "Audit logs",
                        subtitle = "${auditLogs.size} recorded actions",
                        icon = Icons.Default.Shield,
                        onClick = { onActiveDetailViewChange("AUDIT_LOGS") }
                    )
                }

                // 10. Store profile
                item {
                    SettingsTileRow(
                        title = "Store profile",
                        subtitle = currentSettings.storeName,
                        icon = Icons.Default.Storefront,
                        onClick = { onActiveDetailViewChange("STORE_PROFILE") }
                    )
                }

                // 11. About this app
                item {
                    SettingsTileRow(
                        title = "About this app",
                        subtitle = "Ministoreapp · Powered by James Lasquite",
                        icon = Icons.Default.Info,
                        onClick = { onActiveDetailViewChange("ABOUT") }
                    )
                }
            }
        }
    }
}

fun getDetailTitle(key: String?): String {
    return when (key) {
        "PRODUCTS" -> "Products"
        "CASHIERS" -> "Cashiers & PIN"
        "DROPDOWNS" -> "Dropdown Options & Master Data"
        "VENDOR_STORES" -> "Vendor Stores"
        "SECURITY" -> "Security Protections"
        "BAN_LIST" -> "Ban List"
        "AUDIT_LOGS" -> "Audit Logs"
        "SYNC" -> "Sync & Backup"
        "STORE_PROFILE" -> "Store Profile & Info"
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
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
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ProductsManagementTab(
    products: List<ProductEntity>,
    productGroups: List<ProductGroupEntity> = emptyList(),
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    onSaveProductGroup: (group: ProductGroupEntity, generatedItems: List<ProductEntity>, cashier: CashierEntity) -> Unit,
    onDeleteSingleProduct: (product: ProductEntity, cashier: CashierEntity) -> Unit,
    onDeleteProductGroup: (groupId: String, cashier: CashierEntity) -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProductGroup by remember { mutableStateOf<ProductGroupEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    var verifySingleDeleteProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var verifyGroupDeleteId by remember { mutableStateOf<String?>(null) }

    val filteredProducts = remember(products, filterQuery) {
        products.filter {
            filterQuery.isBlank() ||
                it.name.contains(filterQuery, ignoreCase = true) ||
                it.aliases.contains(filterQuery, ignoreCase = true) ||
                it.brand.contains(filterQuery, ignoreCase = true) ||
                it.category.contains(filterQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter products, alias...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = { showAddProductDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B5E38)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredProducts, key = { it.id }) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val parentGroup = productGroups.find { it.id == product.groupId }
                            editingProductGroup = parentGroup ?: ProductGroupEntity(
                                id = product.groupId ?: UUID.randomUUID().toString(),
                                baseName = product.name,
                                aliases = product.aliases,
                                brand = product.brand,
                                category = product.category,
                                storeName = product.storeName,
                                unitSize = product.unitSize,
                                containerType = product.unit
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE3EFE9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = Color(0xFF0B5E38),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Cost ₱${product.cost.roundToInt()}${if (product.storeName.isNotBlank()) " · ${product.storeName}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (product.aliases.isNotBlank()) {
                                    Text(
                                        text = "Aliases: ${product.aliases}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "₱${product.sellingPrice.roundToInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { productToDelete = product },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog || editingProductGroup != null) {
        AddProductDialog(
            existingGroup = editingProductGroup,
            cashiers = cashiers,
            stores = stores,
            categories = categories,
            brands = brands,
            units = units,
            containers = containers,
            wholesaleUnits = wholesaleUnits,
            onDismiss = {
                showAddProductDialog = false
                editingProductGroup = null
            },
            onSaveGroup = { group, generatedItems, cashier ->
                onSaveProductGroup(group, generatedItems, cashier)
                showAddProductDialog = false
                editingProductGroup = null
            }
        )
    }

    productToDelete?.let { prod ->
        val groupItemCount = products.count { it.groupId == prod.groupId && prod.groupId != null }

        if (groupItemCount > 1) {
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("Delete Product Option", fontWeight = FontWeight.Bold) },
                text = { Text("Do you want to delete ONLY '${prod.name}' or delete ALL $groupItemCount items in this group?") },
                confirmButton = {
                    Button(
                        onClick = {
                            verifySingleDeleteProduct = prod
                            productToDelete = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete ONLY This Item")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            verifyGroupDeleteId = prod.groupId ?: prod.id
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete ALL $groupItemCount Items")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            CashierVerificationDialog(
                cashiers = cashiers,
                actionTitle = "Verify Cashier to Delete '${prod.name}'",
                onDismiss = { productToDelete = null },
                onVerified = { cashier ->
                    if (prod.groupId != null) {
                        onDeleteProductGroup(prod.groupId, cashier)
                    } else {
                        onDeleteSingleProduct(prod, cashier)
                    }
                    productToDelete = null
                }
            )
        }
    }

    verifySingleDeleteProduct?.let { prod ->
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Delete Item '${prod.name}'",
            onDismiss = { verifySingleDeleteProduct = null },
            onVerified = { cashier ->
                onDeleteSingleProduct(prod, cashier)
                verifySingleDeleteProduct = null
            }
        )
    }

    verifyGroupDeleteId?.let { groupId ->
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Delete Product Group",
            onDismiss = { verifyGroupDeleteId = null },
            onVerified = { cashier ->
                onDeleteProductGroup(groupId, cashier)
                verifyGroupDeleteId = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompletableOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) suggestions else suggestions.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            singleLine = true
        )

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredSuggestions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onValueChange(selectionOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

data class PreviewItem(
    val comboKey: String,
    val displayName: String,
    val unitSize: String,
    val unit: String,
    val cost: Double,
    val defaultSellingPrice: Double,
    val variantType: String,
    val isWholesale: Boolean,
    val itemsPerPack: Int?,
    val isCheckedState: MutableState<Boolean> = mutableStateOf(true),
    val customPriceInput: MutableState<String> = mutableStateOf("")
)

fun buildProductNames(baseName: String, variantsInput: String): List<String> {
    val cleanVariants = variantsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (cleanVariants.isEmpty()) {
        return listOf(baseName.replace("*", "").trim())
    }
    return cleanVariants.map { variant ->
        if (baseName.contains("*")) {
            baseName.replace("*", variant).trim()
        } else {
            "$baseName $variant".trim()
        }
    }
}

@Composable
fun AddProductDialog(
    existingGroup: ProductGroupEntity? = null,
    cashiers: List<CashierEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    brands: List<BrandEntity>,
    units: List<UnitEntity>,
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveGroup: (group: ProductGroupEntity, generatedItems: List<ProductEntity>, cashier: CashierEntity) -> Unit
) {
    var baseName by remember { mutableStateOf(existingGroup?.baseName ?: "") }
    var aliases by remember { mutableStateOf(existingGroup?.aliases ?: "") }
    var brand by remember { mutableStateOf(existingGroup?.brand ?: "") }
    var category by remember { mutableStateOf(existingGroup?.category ?: "") }
    var storeName by remember { mutableStateOf(existingGroup?.storeName ?: "") }
    var unitSize by remember { mutableStateOf(existingGroup?.unitSize ?: "") }
    var containerType by remember { mutableStateOf(existingGroup?.containerType ?: "") }
    var variantsInput by remember { mutableStateOf(existingGroup?.variants ?: "") }

    // Block 2: Wholesale (Defaults cleared)
    var hasWholesale by remember { mutableStateOf(existingGroup?.hasWholesale ?: false) }
    var wholesaleUnit by remember { mutableStateOf(existingGroup?.wholesaleUnit ?: "") }
    var wholesaleCostInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.wholesaleCost > 0) existingGroup.wholesaleCost.roundToInt().toString() else "") }
    var itemsPerPackInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.itemsPerPack > 0) existingGroup.itemsPerPack.toString() else "") }
    var wholesaleMarkupType by remember { mutableStateOf("FIXED") }
    var wholesaleMarkupInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.wholesaleMarkupValue > 0) existingGroup.wholesaleMarkupValue.roundToInt().toString() else "") }
    var wholesaleManualPriceInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.wholesaleSellingPrice > 0) existingGroup.wholesaleSellingPrice.roundToInt().toString() else "") }
    var showWholesaleInSearch by remember { mutableStateOf(existingGroup?.showWholesaleInSearch ?: true) }

    // Block 3: Retail / Per Piece (Retail Fixed Markup Removed)
    var hasRetail by remember { mutableStateOf(existingGroup?.hasRetail ?: true) }
    var retailCostInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.retailCost > 0) existingGroup.retailCost.roundToInt().toString() else "") }
    var retailManualPriceInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.retailSellingPrice > 0) existingGroup.retailSellingPrice.roundToInt().toString() else "") }

    // Block 4: Cold Beverage (Defaults cleared)
    var isBeverage by remember { mutableStateOf(existingGroup?.isBeverage ?: false) }
    var coldRetailMarkupInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.coldRetailMarkup > 0) existingGroup.coldRetailMarkup.roundToInt().toString() else "") }
    var coldCaseMarkupInput by remember { mutableStateOf(if (existingGroup != null && existingGroup.coldCaseMarkup > 0) existingGroup.coldCaseMarkup.roundToInt().toString() else "") }

    // Master Data Lists for AutoComplete Dropdowns
    val brandSuggestions = remember(brands) { brands.map { it.name }.distinct() }
    val categorySuggestions = remember(categories) { categories.map { it.name }.distinct() }
    val containerSuggestions = remember(containers, units) { (containers.map { it.name } + units.map { it.name }).distinct() }
    val wholesaleUnitSuggestions = remember(wholesaleUnits) { (listOf("Case", "Box", "Tie", "Dozen", "Pack") + wholesaleUnits.map { it.name }).distinct() }

    // Computed Values
    val wholesaleCost = wholesaleCostInput.toDoubleOrNull() ?: 0.0
    val itemsPerPack = itemsPerPackInput.toIntOrNull() ?: 1

    val computedRetailCost = if (hasWholesale && wholesaleCost > 0 && itemsPerPack > 0) {
        wholesaleCost / itemsPerPack
    } else {
        retailCostInput.toDoubleOrNull() ?: 0.0
    }

    // Wholesale Selling Price Calculation Fix (Wholesale Cost + Fixed Markup)
    val wholesaleSellingPrice = remember(wholesaleCost, wholesaleMarkupInput, wholesaleManualPriceInput) {
        val manualVal = wholesaleManualPriceInput.toDoubleOrNull()
        val markupVal = wholesaleMarkupInput.toDoubleOrNull() ?: 0.0
        if (manualVal != null && manualVal > 0) {
            manualVal
        } else if (wholesaleCost > 0) {
            if (markupVal > 0) {
                wholesaleCost + markupVal
            } else {
                PricingEngine.calculateSellingPrice(wholesaleCost, "FIXED", 0.0, null)
            }
        } else {
            0.0
        }
    }

    val retailSellingPrice = remember(computedRetailCost, retailManualPriceInput) {
        val manualVal = retailManualPriceInput.toDoubleOrNull()
        if (manualVal != null && manualVal > 0) manualVal
        else PricingEngine.calculateSellingPrice(computedRetailCost, "FIXED", 0.0, null)
    }

    val coldRetailPrice = remember(retailSellingPrice, coldRetailMarkupInput) {
        val markup = coldRetailMarkupInput.toDoubleOrNull() ?: 0.0
        if (markup > 0) retailSellingPrice + markup else 0.0
    }

    val coldCasePrice = remember(wholesaleSellingPrice, coldCaseMarkupInput) {
        val markup = coldCaseMarkupInput.toDoubleOrNull() ?: 0.0
        if (markup > 0) wholesaleSellingPrice + markup else 0.0
    }

    val excludedKeysList = remember(existingGroup) {
        existingGroup?.excludedKeys?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }

    // Live Preview Generation using * Placeholder Trick
    val previewItems = remember(
        baseName, aliases, unitSize, containerType, variantsInput,
        hasWholesale, wholesaleUnit, wholesaleCost, itemsPerPack, wholesaleSellingPrice, showWholesaleInSearch,
        hasRetail, computedRetailCost, retailSellingPrice,
        isBeverage, coldRetailPrice, coldCasePrice, excludedKeysList
    ) {
        val list = mutableListOf<PreviewItem>()
        if (baseName.isBlank()) return@remember list

        val generatedNames = buildProductNames(baseName, variantsInput)

        generatedNames.forEach { name ->
            val sizePackagingText = buildString {
                if (unitSize.isNotBlank()) append(" $unitSize")
                if (containerType.isNotBlank()) append(" $containerType")
            }

            // 1. Wholesale Case
            if (hasWholesale && showWholesaleInSearch && wholesaleCost > 0) {
                val comboKey = "${name}_wholesale_reg"
                list.add(
                    PreviewItem(
                        comboKey = comboKey,
                        displayName = "$name$sizePackagingText (1 ${wholesaleUnit.ifBlank { "Case" }})",
                        unitSize = if (itemsPerPack > 1) "$itemsPerPack pcs" else unitSize,
                        unit = wholesaleUnit.ifBlank { "Case" },
                        cost = wholesaleCost,
                        defaultSellingPrice = wholesaleSellingPrice,
                        variantType = "WHOLESALE",
                        isWholesale = true,
                        itemsPerPack = itemsPerPack,
                        isCheckedState = mutableStateOf(!excludedKeysList.contains(comboKey))
                    )
                )

                if (isBeverage && coldCasePrice > 0) {
                    val coldComboKey = "${name}_wholesale_cold"
                    list.add(
                        PreviewItem(
                            comboKey = coldComboKey,
                            displayName = "$name$sizePackagingText (COLD) (1 ${wholesaleUnit.ifBlank { "Case" }})",
                            unitSize = if (itemsPerPack > 1) "$itemsPerPack pcs" else unitSize,
                            unit = wholesaleUnit.ifBlank { "Case" },
                            cost = wholesaleCost,
                            defaultSellingPrice = coldCasePrice,
                            variantType = "COLD_WHOLESALE",
                            isWholesale = true,
                            itemsPerPack = itemsPerPack,
                            isCheckedState = mutableStateOf(!excludedKeysList.contains(coldComboKey))
                        )
                    )
                }
            }

            // 2. Retail Single Piece
            if (hasRetail) {
                val pieceUnitText = containerType.ifBlank { "pcs" }
                val comboKey = "${name}_main_reg"
                list.add(
                    PreviewItem(
                        comboKey = comboKey,
                        displayName = "$name$sizePackagingText (1 $pieceUnitText)",
                        unitSize = unitSize,
                        unit = pieceUnitText,
                        cost = computedRetailCost,
                        defaultSellingPrice = retailSellingPrice,
                        variantType = "RETAIL",
                        isWholesale = false,
                        itemsPerPack = 1,
                        isCheckedState = mutableStateOf(!excludedKeysList.contains(comboKey))
                    )
                )

                if (isBeverage && coldRetailPrice > 0) {
                    val coldComboKey = "${name}_main_cold"
                    list.add(
                        PreviewItem(
                            comboKey = coldComboKey,
                            displayName = "$name$sizePackagingText (COLD) (1 $pieceUnitText)",
                            unitSize = unitSize,
                            unit = pieceUnitText,
                            cost = computedRetailCost,
                            defaultSellingPrice = coldRetailPrice,
                            variantType = "COLD_RETAIL",
                            isWholesale = false,
                            itemsPerPack = 1,
                            isCheckedState = mutableStateOf(!excludedKeysList.contains(coldComboKey))
                        )
                    )
                }
            }
        }
        list
    }

    var pendingSaveMode by remember { mutableStateOf<String?>(null) } // "SAVE" or "DIFF_SIZE"

    if (pendingSaveMode == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (existingGroup != null) "Edit Product Calculator" else "Add Product Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Block 1: Shared Identity
                    Text("Block 1: Shared Identity", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = baseName,
                        onValueChange = { baseName = it },
                        label = { Text("Base Product Name (Use * for position, e.g. * Mismo)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(value = aliases, onValueChange = { aliases = it }, label = { Text("Aliases / Search Keywords (e.g. Coke, Soda)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AutoCompletableOutlinedTextField(value = brand, onValueChange = { brand = it }, label = "Brand", suggestions = brandSuggestions, modifier = Modifier.weight(1f))
                        AutoCompletableOutlinedTextField(value = category, onValueChange = { category = it }, label = "Category", suggestions = categorySuggestions, modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = unitSize, onValueChange = { unitSize = it }, label = { Text("Unit Size (1L / 65g)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true)
                        AutoCompletableOutlinedTextField(value = containerType, onValueChange = { containerType = it }, label = "Container (Bottle/Sachet)", suggestions = containerSuggestions, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = variantsInput, onValueChange = { variantsInput = it }, label = { Text("Variants Tags (Coke, Sprite, Royal / Red, Blue)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 2: Wholesale
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasWholesale, onCheckedChange = { hasWholesale = it })
                        Text("Has Wholesale / Case?", fontWeight = FontWeight.Bold)
                    }

                    if (hasWholesale) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AutoCompletableOutlinedTextField(value = wholesaleUnit, onValueChange = { wholesaleUnit = it }, label = "Wholesale Unit (Case/Box/Tie)", suggestions = wholesaleUnitSuggestions)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = wholesaleCostInput, onValueChange = { wholesaleCostInput = it }, label = { Text("Wholesale Cost (₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = itemsPerPackInput, onValueChange = { itemsPerPackInput = it }, label = { Text("Pcs per Pack") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f), singleLine = true)
                                }
                                OutlinedTextField(value = wholesaleMarkupInput, onValueChange = { wholesaleMarkupInput = it }, label = { Text("Wholesale Fixed Markup (+₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(value = wholesaleManualPriceInput, onValueChange = { wholesaleManualPriceInput = it }, label = { Text("Wholesale Price Override (₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Text("Calculated Wholesale Price: ₱${wholesaleSellingPrice.roundToInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showWholesaleInSearch, onCheckedChange = { showWholesaleInSearch = it })
                                    Text("Show Wholesale item in Main Search", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 3: Retail
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasRetail, onCheckedChange = { hasRetail = it })
                        Text("Main Product (Per Piece / Retail)", fontWeight = FontWeight.Bold)
                    }

                    if (hasRetail) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Cost per Piece: ₱${computedRetailCost.roundToInt()}", fontWeight = FontWeight.SemiBold)
                                if (!hasWholesale) {
                                    OutlinedTextField(value = retailCostInput, onValueChange = { retailCostInput = it }, label = { Text("Cost per Piece (₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                }
                                OutlinedTextField(value = retailManualPriceInput, onValueChange = { retailManualPriceInput = it }, label = { Text("Retail Price Override (₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Text("Calculated Retail Price: ₱${retailSellingPrice.roundToInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 4: Beverage Cold
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isBeverage, onCheckedChange = { isBeverage = it })
                        Text("Is Beverage? (Generate Cold Prices)", fontWeight = FontWeight.Bold)
                    }

                    if (isBeverage) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = coldRetailMarkupInput, onValueChange = { coldRetailMarkupInput = it }, label = { Text("Cold Retail Fixed Markup (+₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Text("Cold Retail Price: ₱${coldRetailPrice.roundToInt()}", fontWeight = FontWeight.Bold)
                                if (hasWholesale) {
                                    OutlinedTextField(value = coldCaseMarkupInput, onValueChange = { coldCaseMarkupInput = it }, label = { Text("Cold Wholesale Case Fixed Markup (+₱)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    Text("Cold Case Price: ₱${coldCasePrice.roundToInt()}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Block 5: Live Preview with Editable Price Overrides
                    Text("Items that will be added to Search (${previewItems.count { it.isCheckedState.value }}):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        previewItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = item.isCheckedState.value,
                                        onCheckedChange = { item.isCheckedState.value = it }
                                    )
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedTextField(
                                    value = if (item.customPriceInput.value.isNotBlank()) item.customPriceInput.value else item.defaultSellingPrice.roundToInt().toString(),
                                    onValueChange = { item.customPriceInput.value = it },
                                    label = { Text("₱") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(90.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { pendingSaveMode = "SAVE" },
                        enabled = baseName.isNotBlank() && previewItems.any { it.isCheckedState.value },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { pendingSaveMode = "DIFF_SIZE" },
                        enabled = baseName.isNotBlank() && previewItems.any { it.isCheckedState.value },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Diff. Size", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    } else {
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = if (pendingSaveMode == "DIFF_SIZE") "Verify Cashier (Current Form Saved, Proceeding to Diff. Size)" else "Verify Cashier to Save Product Group",
            onDismiss = { pendingSaveMode = null },
            onVerified = { cashier ->
                val sharedGroupId = existingGroup?.id ?: UUID.randomUUID().toString()

                val uncheckedKeys = previewItems.filter { !it.isCheckedState.value }.map { it.comboKey }
                val excludedKeysString = uncheckedKeys.joinToString(",")

                val groupRecord = ProductGroupEntity(
                    id = sharedGroupId,
                    baseName = baseName,
                    aliases = aliases,
                    namingMode = "append",
                    variants = variantsInput,
                    brand = brand,
                    category = category,
                    storeName = storeName,
                    unitSize = unitSize,
                    containerType = containerType,
                    hasWholesale = hasWholesale,
                    wholesaleUnit = wholesaleUnit,
                    wholesaleCost = wholesaleCost,
                    itemsPerPack = itemsPerPack,
                    wholesaleMarkupType = wholesaleMarkupType,
                    wholesaleMarkupValue = wholesaleMarkupInput.toDoubleOrNull() ?: 0.0,
                    wholesaleSellingPrice = wholesaleSellingPrice,
                    showWholesaleInSearch = showWholesaleInSearch,
                    hasRetail = hasRetail,
                    retailCost = computedRetailCost,
                    retailMarkupType = "FIXED",
                    retailMarkupValue = 0.0,
                    retailSellingPrice = retailSellingPrice,
                    isBeverage = isBeverage,
                    coldRetailMarkup = coldRetailMarkupInput.toDoubleOrNull() ?: 0.0,
                    coldCaseMarkup = coldCaseMarkupInput.toDoubleOrNull() ?: 0.0,
                    excludedKeys = excludedKeysString,
                    updatedAt = System.currentTimeMillis()
                )

                val selectedItems = previewItems.filter { it.isCheckedState.value }

                val generatedEntities = selectedItems.map { item ->
                    val finalPrice = item.customPriceInput.value.toDoubleOrNull() ?: item.defaultSellingPrice
                    ProductEntity(
                        groupId = sharedGroupId,
                        comboKey = item.comboKey,
                        name = item.displayName,
                        aliases = aliases,
                        category = category,
                        brand = brand,
                        unit = item.unit,
                        unitSize = item.unitSize,
                        storeName = storeName,
                        cost = item.cost,
                        sellingPrice = finalPrice,
                        isBeverage = isBeverage,
                        isWholesaleParent = item.isWholesale,
                        itemsPerWholesalePack = item.itemsPerPack,
                        variantType = item.variantType,
                        updatedAt = System.currentTimeMillis()
                    )
                }

                onSaveGroup(groupRecord, generatedEntities, cashier)

                if (pendingSaveMode == "DIFF_SIZE") {
                    unitSize = ""
                    containerType = ""
                    wholesaleCostInput = ""
                    retailCostInput = ""
                    wholesaleManualPriceInput = ""
                    retailManualPriceInput = ""
                    pendingSaveMode = null
                } else {
                    pendingSaveMode = null
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun CashiersManagementTab(
    cashiers: List<CashierEntity>,
    securitySettings: SecuritySettingsEntity,
    onSaveCashier: (CashierEntity, performerName: String, reason: String) -> Unit,
    onDeleteCashier: (CashierEntity, performerName: String, reason: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    var editingCashier by remember { mutableStateOf<CashierEntity?>(null) }
    var deletingCashier by remember { mutableStateOf<CashierEntity?>(null) }
    var pendingAddCashier by remember { mutableStateOf<CashierEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add New Cashier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Cashier Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it }, label = { Text("4-Digit PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = {
                        val newCashier = CashierEntity(name = name, pin = pin)
                        if (cashiers.isEmpty()) {
                            // First cashier setup: direct save
                            onSaveCashier(newCashier, name, "Initial Cashier Setup")
                            name = ""
                            pin = ""
                        } else {
                            // 2nd+ cashier: prompt cashier verification
                            pendingAddCashier = newCashier
                        }
                    },
                    enabled = name.isNotBlank() && pin.length == 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Active Cashiers List:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cashiers, key = { it.id }) { cashier ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = cashier.name, fontWeight = FontWeight.Bold)
                            Text(text = "PIN: **** (${cashier.pin.take(1)}***)", style = MaterialTheme.typography.bodySmall)
                        }

                        Row {
                            IconButton(onClick = { editingCashier = cashier }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Cashier", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deletingCashier = cashier }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Cashier", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingAddCashier?.let { newCashier ->
        CashierVerificationDialog(
            cashiers = cashiers,
            actionTitle = "Verify Cashier to Add New Cashier '${newCashier.name}'",
            onDismiss = { pendingAddCashier = null },
            onVerified = { verifyingCashier ->
                onSaveCashier(newCashier, verifyingCashier.name, "Added cashier '${newCashier.name}'")
                name = ""
                pin = ""
                pendingAddCashier = null
            }
        )
    }

    editingCashier?.let { cashier ->
        var editName by remember { mutableStateOf(cashier.name) }
        var editPin by remember { mutableStateOf(cashier.pin) }
        var securityPinInput by remember { mutableStateOf("") }
        var performerName by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingCashier = null },
            title = { Text("Edit Cashier '${cashier.name}'", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Cashier Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = editPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) editPin = it }, label = { Text("4-Digit Cashier PIN") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Security Verification Required:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = securityPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) securityPinInput = it },
                        label = { Text("6-Digit Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(value = performerName, onValueChange = { performerName = it }, label = { Text("Who is editing? (Your Name)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for edit") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = securitySettings.securityPin
                        if (securityPinInput != expectedPin) {
                            errorMessage = "Incorrect 6-Digit Security PIN"
                        } else {
                            onSaveCashier(cashier.copy(name = editName, pin = editPin, updatedAt = System.currentTimeMillis()), performerName, reason)
                            editingCashier = null
                        }
                    },
                    enabled = editName.isNotBlank() && editPin.length == 4 && securityPinInput.length == 6 && performerName.isNotBlank() && reason.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCashier = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    deletingCashier?.let { cashier ->
        var securityPinInput by remember { mutableStateOf("") }
        var performerName by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { deletingCashier = null },
            title = { Text("Delete Cashier '${cashier.name}'", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Are you sure you want to delete this cashier?")

                    OutlinedTextField(
                        value = securityPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) securityPinInput = it },
                        label = { Text("6-Digit Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(value = performerName, onValueChange = { performerName = it }, label = { Text("Who is deleting? (Your Name)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for delete") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = securitySettings.securityPin
                        if (securityPinInput != expectedPin) {
                            errorMessage = "Incorrect 6-Digit Security PIN"
                        } else {
                            onDeleteCashier(cashier, performerName, reason)
                            deletingCashier = null
                        }
                    },
                    enabled = securityPinInput.length == 6 && performerName.isNotBlank() && reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCashier = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SecurityManagementTab(
    securitySettings: SecuritySettingsEntity?,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit
) {
    val currentSettings = securitySettings ?: SecuritySettingsEntity()

    var launchPinEnabled by remember(currentSettings) { mutableStateOf(currentSettings.pinOnAppLaunchEnabled) }
    var settingsPinEnabled by remember(currentSettings) { mutableStateOf(currentSettings.pinOnSettingsOpeningEnabled) }

    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Security Toggles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require 6-Digit PIN on App Launch", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Prompts for 6-digit PIN when opening app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = launchPinEnabled,
                        onCheckedChange = { launchPinEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require 6-Digit PIN on Opening Settings", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Prompts for 6-digit PIN when entering settings tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = settingsPinEnabled,
                        onCheckedChange = { settingsPinEnabled = it }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Change 6-Digit Security PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                    label = { Text("New 6-Digit PIN (Leave blank to keep)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Security Verification (REQUIRED FOR ANY CHANGE):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = currentPinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPinInput = it },
                    label = { Text("Enter Current 6-Digit Security PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val expectedPin = currentSettings.securityPin
                        if (currentPinInput != expectedPin) {
                            statusMessage = "Incorrect current 6-Digit Security PIN"
                            isError = true
                        } else {
                            val updatedPin = if (newPinInput.length == 6) newPinInput else currentSettings.securityPin
                            val updated = currentSettings.copy(
                                pinOnAppLaunchEnabled = launchPinEnabled,
                                pinOnSettingsOpeningEnabled = settingsPinEnabled,
                                securityPin = updatedPin,
                                updatedAt = System.currentTimeMillis()
                            )
                            onUpdateSecuritySettings(updated)
                            statusMessage = "Security settings updated successfully!"
                            isError = false
                            currentPinInput = ""
                            newPinInput = ""
                        }
                    },
                    enabled = currentPinInput.length == 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StoreProfileManagementTab(
    securitySettings: SecuritySettingsEntity?,
    onUpdateSecuritySettings: (SecuritySettingsEntity) -> Unit
) {
    val currentSettings = securitySettings ?: SecuritySettingsEntity()
    var storeName by remember(currentSettings) { mutableStateOf(currentSettings.storeName) }
    var ownerName by remember { mutableStateOf("James Lasquite") }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Edit Owner & Store Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name (Displayed in Header)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Store Owner Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                saveMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val updated = currentSettings.copy(
                            storeName = storeName,
                            updatedAt = System.currentTimeMillis()
                        )
                        onUpdateSecuritySettings(updated)
                        saveMessage = "Store info updated successfully!"
                    },
                    enabled = storeName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
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
    containers: List<ContainerEntity> = emptyList(),
    wholesaleUnits: List<WholesaleUnitEntity> = emptyList(),
    banRecords: List<BanRecordEntity>,
    cashiers: List<CashierEntity>,
    onSaveMasterData: (type: String, name: String) -> Unit,
    onDeleteMasterData: (type: String, id: String) -> Unit,
    onSaveBanRecord: (BanRecordEntity, CashierEntity) -> Unit,
    onDeleteBanRecord: (BanRecordEntity, CashierEntity) -> Unit
) {
    var selectedDataTab by remember { mutableIntStateOf(0) }
    val dataTabs = listOf("Stores", "Categories", "Brands", "Containers", "Wholesale Units")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedDataTab, edgePadding = 0.dp) {
            dataTabs.forEachIndexed { idx, title ->
                Tab(selected = selectedDataTab == idx, onClick = { selectedDataTab = idx }, text = { Text(title, fontWeight = FontWeight.SemiBold) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedDataTab) {
            0 -> SimpleMasterDataSection("Store", stores.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            1 -> SimpleMasterDataSection("Category", categories.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            2 -> SimpleMasterDataSection("Brand", brands.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            3 -> SimpleMasterDataSection("Container", containers.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
            4 -> SimpleMasterDataSection("WholesaleUnit", wholesaleUnits.map { it.id to it.name }, onSaveMasterData, onDeleteMasterData)
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
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Ban Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Alias") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = { verifiedCashier = cashiers.firstOrNull() },
                    enabled = fullName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("New $typeLabel Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), singleLine = true)
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

        Spacer(modifier = Modifier.height(12.dp))

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
                        Text(text = "Performer: ${log.cashierName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun SyncBackupPlaceholder(securitySettings: SecuritySettingsEntity?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sync & Local Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Google Drive Delta Sync Engine: Active", fontWeight = FontWeight.Bold)
                Text("Automatic local database pre-sync backups enabled.")
                Text("Store Name: ${securitySettings?.storeName ?: "Default Store"}")
            }
        }
    }
}

@Composable
fun AboutAppPlaceholder() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    var releaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var statusDialogMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("About This App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ministoreapp", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Powered by James Lasquite", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Current Version: v${AppUpdateManager.CURRENT_VERSION}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Button(
                    onClick = {
                        isCheckingUpdates = true
                        coroutineScope.launch {
                            val result = AppUpdateManager.checkForUpdates(
                                owner = "insomniaczxz",
                                repo = "ministoreapp"
                            )
                            isCheckingUpdates = false
                            if (result.isSuccess) {
                                val info = result.getOrNull()!!
                                releaseInfo = info
                            } else {
                                statusDialogMessage = "Update Check Result:\n\n${result.exceptionOrNull()?.message ?: "Unable to connect to GitHub"}\n\nRunning version v${AppUpdateManager.CURRENT_VERSION}."
                            }
                        }
                    },
                    enabled = !isCheckingUpdates && !isDownloading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking GitHub...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    releaseInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { releaseInfo = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (info.hasUpdate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (info.hasUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                    )
                }
            },
            title = {
                Text(
                    text = if (info.hasUpdate) "New Update Available (v${info.version})" else "You are Up to Date!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Latest Release: v${info.version} (Current: v${AppUpdateManager.CURRENT_VERSION})", fontWeight = FontWeight.SemiBold)
                    if (info.releaseNotes.isNotBlank()) {
                        Text("Changelog:\n${info.releaseNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Downloading APK... $downloadProgress%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                if (info.hasUpdate && !info.downloadUrl.isNullOrBlank()) {
                    Button(
                        onClick = {
                            isDownloading = true
                            coroutineScope.launch {
                                val downloadResult = AppUpdateManager.downloadAndInstallApk(
                                    context = context,
                                    downloadUrl = info.downloadUrl,
                                    onProgress = { downloadProgress = it }
                                )
                                isDownloading = false
                                if (downloadResult.isFailure) {
                                    statusDialogMessage = "Download Failed: ${downloadResult.exceptionOrNull()?.message}"
                                }
                                releaseInfo = null
                            }
                        },
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isDownloading) "Downloading..." else "Download & Install Update", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = { releaseInfo = null }, shape = RoundedCornerShape(12.dp)) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (info.hasUpdate) {
                    TextButton(onClick = { releaseInfo = null }) {
                        Text("Later")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    statusDialogMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { statusDialogMessage = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            title = { Text("App Update Check", fontWeight = FontWeight.Bold) },
            text = { Text(msg, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { statusDialogMessage = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
