package com.example.ministoreapp.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_RELEASE_URL = "https://api.github.com/repos/insomniaczxz/ministoreapp/releases/latest"

    suspend fun checkForUpdates(context: Context) = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MiniStoreApp-Android")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(jsonText)

                val tagName = rootJson.optString("tag_name", "")
                val releaseNotes = rootJson.optString("body", "A new update for MiniStoreApp is available.")

                var downloadUrl = ""
                val assetsArray = rootJson.optJSONArray("assets")
                if (assetsArray != null && assetsArray.length() > 0) {
                    val firstAsset = assetsArray.getJSONObject(0)
                    downloadUrl = firstAsset.optString("browser_download_url", "")
                }

                if (downloadUrl.isBlank()) {
                    downloadUrl = rootJson.optString("html_url", "https://github.com/insomniaczxz/ministoreapp/releases/latest")
                }

                val currentVersionName = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.0"
                } catch (e: Exception) {
                    "1.0"
                }

                val cleanTag = tagName.removePrefix("v").removePrefix("V").trim()
                val cleanCurrent = currentVersionName.removePrefix("v").removePrefix("V").trim()

                if (isNewerVersion(cleanTag, cleanCurrent)) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(context, tagName, releaseNotes, downloadUrl)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        if (remoteTag.isBlank()) return false
        if (remoteTag.equals(localVersion, ignoreCase = true)) return false

        val remoteParts = remoteTag.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = localVersion.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun showUpdateDialog(context: Context, tagName: String, releaseNotes: String, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Update Available ($tagName)")
            .setMessage("A new version ($tagName) of MiniStoreApp is available.\n\nRelease Notes:\n$releaseNotes")
            .setPositiveButton("Update") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Later", null)
            .setCancelable(true)
            .show()
    }
}
