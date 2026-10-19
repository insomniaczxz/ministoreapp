package com.example.ministoreapp.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.ministoreapp.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val hasUpdate: Boolean,
    val rawTagName: String
)

object AppUpdateManager {

    const val CURRENT_VERSION = "1.0.0"

    var defaultOwner: String = "insomniaczxz"
    var defaultRepo: String = "ministoreapp"

    suspend fun checkForUpdates(
        owner: String = defaultOwner,
        repo: String = defaultRepo,
        currentVersion: String = CURRENT_VERSION
    ): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val code = connection.responseCode
            if (code == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                val rawTag = json.optString("tag_name", "")
                val cleanTag = rawTag.removePrefix("v").removePrefix("V").trim()
                val notes = json.optString("body", "No release notes provided.")
                val assets = json.optJSONArray("assets")

                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "")
                            if (apkUrl.isBlank()) apkUrl = null
                            break
                        }
                    }
                }

                val hasUpdate = isVersionNewer(cleanTag, currentVersion)

                Result.success(
                    AppReleaseInfo(
                        version = if (cleanTag.isNotBlank()) cleanTag else "1.0.0",
                        releaseNotes = notes,
                        downloadUrl = apkUrl,
                        hasUpdate = hasUpdate,
                        rawTagName = rawTag
                    )
                )
            } else if (code == 404) {
                Result.failure(Exception("No GitHub releases published yet for repo '$owner/$repo'."))
            } else {
                Result.failure(Exception("GitHub API returned HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkUtils.getFriendlyErrorMessage(e), e))
        }
    }

    private fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        val remoteParts = remoteVersion.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val remotePart = remoteParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (remotePart > currentPart) return true
            if (remotePart < currentPart) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Server returned HTTP $code when downloading update."))
            }

            val totalSize = connection.contentLength
            val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalSize > 0) {
                            val progress = ((downloaded.toDouble() / totalSize) * 100).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(Exception(NetworkUtils.getFriendlyErrorMessage(e), e))
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
