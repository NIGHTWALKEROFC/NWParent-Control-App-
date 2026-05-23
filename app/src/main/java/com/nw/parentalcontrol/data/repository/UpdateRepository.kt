// PATH: app/src/main/java/com/nw/parentalcontrol/data/repository/UpdateRepository.kt
package com.nw.parentalcontrol.data.repository

import android.content.Context
import com.nw.parentalcontrol.data.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Points to version.txt in the parent app's own GitHub repo root
    // Replace YOUR_USERNAME with your actual GitHub username
    private val VERSION_URL =
        "https://raw.githubusercontent.com/NIGHTWALKEROFC/nw-parent-app/main/version.txt"

    suspend fun checkForUpdates(currentVersionCode: Int): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(VERSION_URL).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                parseVersionInfo(body, currentVersionCode)
            } catch (e: Exception) {
                null // Network error — silently ignore
            }
        }

    private fun parseVersionInfo(content: String, currentVersionCode: Int): UpdateInfo? {
        return try {
            val props = mutableMapOf<String, String>()
            content.trim().lines().forEach { line ->
                if (line.startsWith("#") || line.isBlank()) return@forEach
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) props[parts[0].trim()] = parts[1].trim()
            }
            val remoteCode = props["parent_version_code"]?.toIntOrNull() ?: return null
            if (remoteCode <= currentVersionCode) return null
            UpdateInfo(
                versionCode  = remoteCode,
                versionName  = props["parent_version_name"] ?: "",
                downloadUrl  = props["parent_download_url"] ?: "",
                releaseNotes = props["release_notes"] ?: "Bug fixes and improvements",
                mandatory    = props["mandatory"]?.toBooleanStrictOrNull() ?: false
            )
        } catch (e: Exception) { null }
    }
}