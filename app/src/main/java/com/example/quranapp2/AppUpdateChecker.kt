package com.example.quranapp2

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"

    fun checkForUpdate(activity: Activity) {
        Thread {
            try {
                val release = fetchLatestRelease() ?: return@Thread
                val latestVersion = release.getString("tag_name").removePrefix("v")

                if (!isNewer(latestVersion)) {
                    deleteDownloadedApks(activity)
                    return@Thread
                }

                val assets = release.getJSONArray("assets")
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                if (apkUrl == null) return@Thread

                val updateDir = File(activity.cacheDir, "updates")
                updateDir.mkdirs()
                val apkFile = File(updateDir, "update-$latestVersion.apk")

                if (!apkFile.exists()) {
                    downloadApk(apkUrl, apkFile)
                }

                activity.runOnUiThread {
                    showUpdateDialog(activity, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
        }.start()
    }

    private fun fetchLatestRelease(): JSONObject? {
        val url = URL(
            "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000

        return try {
            if (conn.responseCode != 200) {
                Log.w(TAG, "GitHub API returned ${conn.responseCode}")
                return null
            }
            val body = conn.inputStream.bufferedReader().readText()
            JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadApk(assetUrl: String, destination: File) {
        val url = URL(assetUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/octet-stream")
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000

        try {
            conn.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun isNewer(remote: String): Boolean {
        val local = BuildConfig.VERSION_NAME
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until len) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun deleteDownloadedApks(activity: Activity) {
        val updateDir = File(activity.cacheDir, "updates")
        if (!updateDir.exists()) return
        updateDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                file.delete()
            }
        }
    }

    private fun showUpdateDialog(activity: Activity, apkFile: File) {
        val dialog = BottomSheetDialog(activity)
        val root = android.widget.FrameLayout(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_update, root, false)
        dialog.setContentView(view)

        view.findViewById<MaterialButton>(R.id.updateNowBtn).setOnClickListener {
            dialog.dismiss()
            installApk(activity, apkFile)
        }
        view.findViewById<View>(R.id.maybeLaterBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.post {
                val params = bottomSheet.layoutParams ?: return@post
                params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                bottomSheet.layoutParams = params
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        dialog.show()
    }

    private fun installApk(activity: Activity, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
