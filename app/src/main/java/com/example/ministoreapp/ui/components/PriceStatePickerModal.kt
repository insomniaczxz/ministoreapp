package com.example.ministoreapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.data.local.entities.ProductEntity
import kotlin.math.roundToInt

data class PriceOptionTag(
    val product: ProductEntity,
    val tagTitle: String,
    val price: Double,
    val icon: String,
    val typeKey: String
)

@Composable
fun PriceStatePickerModal(
    productName: String,
    options: List<PriceOptionTag>,
    onDismiss: () -> Unit,
    onOptionSelected: (PriceOptionTag) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = {
            Text(
                text = "Select Price State",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Multiple price options available. Tap a tag to select:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEach { option ->
                        val (bgFill, borderCol, textCol) = when (option.typeKey) {
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

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgFill,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionSelected(option) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(option.icon, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = option.tagTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol
                                    )
                                }
                                Text(
                                    text = "₱${option.price.roundToInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
