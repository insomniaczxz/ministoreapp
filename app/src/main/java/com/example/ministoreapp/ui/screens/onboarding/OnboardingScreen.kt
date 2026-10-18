package com.example.ministoreapp.ui.screens.onboarding

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.CashierEntity

enum class OnboardingStep {
    ASK_GOOGLE_RESTORE,     // Step 1: Sign in with Google to restore OR Skip
    VERIFY_HARDCODED_PIN,   // Step 2A: Verify default 6-digit PIN ("526375")
    CREATE_FIRST_CASHIER,   // Step 2B: Force create at least 1 cashier (Name & PIN)
    ASK_GOOGLE_BACKUP,      // Step 3: Link Google Account for future sync (if skipped)
    COMPLETED               // Finished -> Go to Main Search
}

@Composable
fun OnboardingScreen(
    defaultPin: String = "526375",
    onGoogleSignInRequested: (onSuccess: (cloudBackupJson: String?) -> Unit) -> Unit = {},
    onCreateCashier: (CashierEntity) -> Unit = {},
    onUpdateSecurityPin: (newPin: String) -> Unit = {},
    onOnboardingComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(OnboardingStep.ASK_GOOGLE_RESTORE) }
    var isGoogleLinked by remember { mutableStateOf(false) }

    // Step 2A State
    var pinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Step 2B State
    var cashierNameInput by remember { mutableStateOf("") }
    var cashierPinInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                when (currentStep) {
                    OnboardingStep.ASK_GOOGLE_RESTORE -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Welcome to MiniStoreApp",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Sign in with Google to restore existing store data, products, and credits, or set up as a brand new store.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onGoogleSignInRequested { cloudJson ->
                                        isGoogleLinked = true
                                        if (!cloudJson.isNullOrBlank()) {
                                            Toast.makeText(context, "Existing store data restored from cloud!", Toast.LENGTH_LONG).show()
                                            onOnboardingComplete()
                                        } else {
                                            // Signed in, fresh Google account -> Jump directly to Step 2B (Create First Cashier), skipping Step 2A PIN
                                            currentStep = OnboardingStep.CREATE_FIRST_CASHIER
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with Google to Sync", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    isGoogleLinked = false
                                    currentStep = OnboardingStep.VERIFY_HARDCODED_PIN
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Skip / Set Up as New Store", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Step 1 of 3 · Cloud Restore", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    OnboardingStep.VERIFY_HARDCODED_PIN -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Verify Security PIN",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Enter default 6-digit security PIN (Default: $defaultPin). You can also set a new custom PIN.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                                label = { Text("6-Digit Default PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                                label = { Text("New Custom 6-Digit PIN (Optional)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            pinError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (pinInput == defaultPin) {
                                        if (newPinInput.length == 6) {
                                            onUpdateSecurityPin(newPinInput)
                                        }
                                        currentStep = OnboardingStep.CREATE_FIRST_CASHIER
                                    } else {
                                        pinError = "Incorrect Default Security PIN"
                                    }
                                },
                                enabled = pinInput.length == 6,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Continue", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text("Step 2A of 3 · Security PIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    OnboardingStep.CREATE_FIRST_CASHIER -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Create First Cashier",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Creating a Cashier now prevents any issues when adding products or logging sales later.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = cashierNameInput,
                                onValueChange = { cashierNameInput = it },
                                label = { Text("Cashier Name (e.g. Store Owner / Ate Maria)") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = cashierPinInput,
                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) cashierPinInput = it },
                                label = { Text("4-Digit Cashier PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val firstCashier = CashierEntity(
                                        name = cashierNameInput.trim(),
                                        pin = cashierPinInput
                                    )
                                    onCreateCashier(firstCashier)

                                    if (!isGoogleLinked) {
                                        currentStep = OnboardingStep.ASK_GOOGLE_BACKUP
                                    } else {
                                        onOnboardingComplete()
                                    }
                                },
                                enabled = cashierNameInput.isNotBlank() && cashierPinInput.length == 4,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Cashier & Continue", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Step 2B of 3 · First Cashier", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    OnboardingStep.ASK_GOOGLE_BACKUP -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "Enable Google Sync?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Link your Google Account now so your store data, products, and credits are automatically backed up.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onGoogleSignInRequested {
                                        Toast.makeText(context, "Google Account linked for future sync!", Toast.LENGTH_SHORT).show()
                                        onOnboardingComplete()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Link Google Account", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onOnboardingComplete()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Maybe Later / Finish Setup", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Step 3 of 3 · Final Link", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    OnboardingStep.COMPLETED -> {}
                }
            }
        }
    }
}
