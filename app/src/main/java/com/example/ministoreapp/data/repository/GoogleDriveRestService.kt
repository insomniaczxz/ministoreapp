package com.example.ministoreapp.data.repository

import android.content.Context
import com.example.ministoreapp.util.NetworkUtils
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GoogleDriveRestService {

    private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    private const val SYNC_FILE_NAME = "ministore_sync.json"

    suspend fun getOAuthToken(context: Context, account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val androidAccount = account.account ?: return@withContext null
            GoogleAuthUtil.getToken(context, androidAccount, DRIVE_SCOPE)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Suppress("DEPRECATION")
    suspend fun invalidateToken(context: Context, token: String) = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.clearToken(context, token)
        } catch (e: Exception) {
            try {
                GoogleAuthUtil.invalidateToken(context, token)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    suspend fun findSyncFileId(context: Context, token: String): String? = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode("name='$SYNC_FILE_NAME'", "UTF-8")
        val urlStr = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$encodedQuery"
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 10000
            readTimeout = 10000
        }

        if (conn.responseCode == 200) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseStr)
            val files = json.optJSONArray("files")
            if (files != null && files.length() > 0) {
                return@withContext files.getJSONObject(0).optString("id", null)
            }
            return@withContext null
        } else if (conn.responseCode == 401 || conn.responseCode == 403) {
            invalidateToken(context, token)
            val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP ${conn.responseCode} - Forbidden/Auth Error: $errBody")
        } else {
            val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP ${conn.responseCode} - $errBody")
        }
    }

    suspend fun uploadToDriveAppData(context: Context, token: String, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val existingFileId = findSyncFileId(context, token)

            if (!existingFileId.isNullOrBlank()) {
                // PATCH existing file
                val urlStr = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                conn.outputStream.use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode in 200..299) {
                    Result.success(existingFileId)
                } else {
                    if (conn.responseCode == 401 || conn.responseCode == 403) {
                        invalidateToken(context, token)
                    }
                    val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Result.failure(Exception("HTTP ${conn.responseCode} - Forbidden/Auth Error: $errBody"))
                }
            } else {
                // POST new file to appDataFolder using multipart
                val boundary = "=====MiniStoreAppBoundary====="
                val urlStr = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val metadataJson = JSONObject().apply {
                    put("name", SYNC_FILE_NAME)
                    put("parents", org.json.JSONArray().put("appDataFolder"))
                }.toString()

                val multipartBody = buildString {
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadataJson)
                    append("\r\n--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(jsonString)
                    append("\r\n--$boundary--\r\n")
                }

                conn.outputStream.use { out ->
                    out.write(multipartBody.toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode in 200..299) {
                    val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val newId = JSONObject(resStr).optString("id", "uploaded")
                    Result.success(newId)
                } else {
                    if (conn.responseCode == 401 || conn.responseCode == 403) {
                        invalidateToken(context, token)
                    }
                    val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Result.failure(Exception("HTTP ${conn.responseCode} - Forbidden/Auth Error: $errBody"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(NetworkUtils.getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun downloadFromDriveAppData(context: Context, token: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val fileId = findSyncFileId(context, token)
            if (fileId.isNullOrBlank()) {
                return@withContext Result.success(null)
            }

            val urlStr = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(jsonStr)
            } else {
                if (conn.responseCode == 401 || conn.responseCode == 403) {
                    invalidateToken(context, token)
                }
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("HTTP ${conn.responseCode} - Forbidden/Auth Error: $errBody"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(NetworkUtils.getFriendlyErrorMessage(e), e))
        }
    }
}
