package com.example.ministoreapp.util

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ministoreapp.domain.AppReleaseInfo
import com.example.ministoreapp.domain.AppUpdateManager
import kotlinx.coroutines.launch

@Composable
fun AppUpdateCheckerHost() {
    val context = LocalContext.current
    var releaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    val currentVersion = remember { AppUpdateManager.getCurrentVersion(context) }

    LaunchedEffect(Unit) {
        val result = AppUpdateManager.fetchLatestRelease(context)
        if (result.isSuccess) {
            val info = result.getOrNull()
            if (info != null && info.hasUpdate) {
                releaseInfo = info
            }
        }
    }

    releaseInfo?.let { info ->
        AppUpdateModalDialog(
            info = info,
            currentVersion = currentVersion,
            onDismiss = { releaseInfo = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateModalDialog(
    info: AppReleaseInfo,
    currentVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    BasicAlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        }
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Info Circle Icon Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title
                Text(
                    text = "New Update Available\n(v${info.version})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Subtitle
                Text(
                    text = "Latest Release: v${info.version} (Current: v$currentVersion)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Changelog
                if (info.releaseNotes.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Changelog:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Download Progress Bar & Text (Image 2)
                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF0B5E38)
                        )
                        Text(
                            text = "Downloading APK... $downloadProgress%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B5E38)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Action Button (Download & Install)
                Button(
                    onClick = {
                        if (!info.downloadUrl.isNullOrBlank() && !isDownloading) {
                            isDownloading = true
                            coroutineScope.launch {
                                val downloadResult = AppUpdateManager.downloadAndInstallApk(
                                    context = context,
                                    downloadUrl = info.downloadUrl,
                                    onProgress = { downloadProgress = it }
                                )
                                isDownloading = false
                                if (downloadResult.isFailure) {
                                    val err = downloadResult.exceptionOrNull()?.message ?: "Download failed"
                                    Toast.makeText(context, "Download Error: $err", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isDownloading && !info.downloadUrl.isNullOrBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B5E38),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isDownloading) "Downloading APK... $downloadProgress%" else "Download & Install Update",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isDownloading,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
