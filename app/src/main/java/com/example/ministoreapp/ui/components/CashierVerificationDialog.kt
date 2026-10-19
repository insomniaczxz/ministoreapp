package com.example.ministoreapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.CashierEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierVerificationDialog(
    cashiers: List<CashierEntity>,
    actionTitle: String = "Cashier Verification",
    isInitialSetupBypassAllowed: Boolean = false,
    onDismiss: () -> Unit,
    onVerified: (cashier: CashierEntity) -> Unit
) {
    if (cashiers.isEmpty() && isInitialSetupBypassAllowed) {
        val dummyInitialCashier = CashierEntity(id = "system_initial", name = "Initial Admin", pin = "")
        SideEffect {
            onVerified(dummyInitialCashier)
        }
        return
    }

    var selectedCashier by remember { mutableStateOf(cashiers.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        title = {
            Text(
                text = actionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (cashiers.isEmpty()) {
                    Text(
                        text = "No cashiers available. Please create a cashier in Settings first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Select Cashier Name and enter 4-Digit PIN:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCashier?.name ?: "Select Cashier",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            label = { Text("Cashier Name") }
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            cashiers.forEach { cashier ->
                                DropdownMenuItem(
                                    text = { Text(cashier.name, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        selectedCashier = cashier
                                        expandedDropdown = false
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                enteredPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cashier = selectedCashier
                    if (cashier == null) {
                        errorMessage = "Please select a cashier."
                        return@Button
                    }
                    if (enteredPin == cashier.pin) {
                        onVerified(cashier)
                    } else {
                        errorMessage = "Invalid PIN for ${cashier.name}"
                    }
                },
                enabled = cashiers.isNotEmpty() && enteredPin.length == 4,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Verify", fontWeight = FontWeight.Bold)
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
