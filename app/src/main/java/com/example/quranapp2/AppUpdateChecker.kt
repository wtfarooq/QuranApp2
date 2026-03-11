package com.example.quranapp2

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import androidx.core.content.edit
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {

    const val DOWNLOAD_NOTIFICATION_ID = 2001
    const val ACTION_UPDATE_DOWNLOAD_COMPLETE = "com.example.quranapp2.UPDATE_DOWNLOAD_COMPLETE"
    const val EXTRA_APK_PATH = "apk_path"

    private const val TAG = "AppUpdateChecker"
    private const val PREFS_UPDATE = "update_checker"
    private const val KEY_PENDING_APK_PATH = "pending_apk_path"

    fun cancelDownloadNotification(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.cancel(DOWNLOAD_NOTIFICATION_ID)
    }

    private fun isUpdateDownloadRunning(context: Context): Boolean {
        val am =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == UpdateDownloadService::class.java.name }
    }

    fun checkForUpdate(activity: Activity) {
        Thread {
            try {
                val release = fetchLatestRelease()
                if (release == null) {
                    activity.runOnUiThread { showUpdateDialogIfDownloaded(activity) }
                    return@Thread
                }
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

                val updateDir = getUpdateDir(activity)
                updateDir.mkdirs()
                val apkFile = File(updateDir, "update-$latestVersion.apk")

                if (apkFile.exists()) {
                    if (isUpdateDownloadRunning(activity.applicationContext)) return@Thread
                    activity.runOnUiThread {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            showUpdateDialog(activity, apkFile)
                        } else {
                            activity.applicationContext.getSharedPreferences(
                                PREFS_UPDATE,
                                Activity.MODE_PRIVATE
                            ).edit {
                                putString(KEY_PENDING_APK_PATH, apkFile.absolutePath)
                            }
                        }
                    }
                } else {
                    UpdateDownloadService.start(
                        activity.applicationContext,
                        apkUrl,
                        apkFile.absolutePath
                    )
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

    private fun isNewer(remote: String): Boolean =
        isVersionNewer(remote, BuildConfig.VERSION_NAME)

    private fun isVersionNewer(a: String, b: String): Boolean {
        val aParts = a.split(".").map { it.toIntOrNull() ?: 0 }
        val bParts = b.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(aParts.size, bParts.size)
        for (i in 0 until len) {
            val x = aParts.getOrElse(i) { 0 }
            val y = bParts.getOrElse(i) { 0 }
            if (x > y) return true
            if (x < y) return false
        }
        return false
    }

    fun showPendingUpdateIfAny(activity: Activity): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        val path = activity.getSharedPreferences(PREFS_UPDATE, Activity.MODE_PRIVATE)
            .getString(KEY_PENDING_APK_PATH, null) ?: return false
        val apkFile = File(path)
        if (!apkFile.exists()) return false
        activity.getSharedPreferences(PREFS_UPDATE, Activity.MODE_PRIVATE)
            .edit { remove(KEY_PENDING_APK_PATH) }
        showUpdateDialog(activity, apkFile)
        return true
    }

    /**
     * If an update APK was downloaded (e.g. while user was in PageActivity or app was backgrounded),
     * show the update dialog. No network. Skips if download is still in progress.
     */
    fun showUpdateDialogIfDownloaded(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (isUpdateDownloadRunning(activity.applicationContext)) return
        val updateDir = getUpdateDir(activity)
        if (!updateDir.exists()) return
        val apkFiles = updateDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".apk") && it.name.startsWith("update-") }
            ?: return
        var bestFile: File? = null
        var bestVersion: String? = null
        for (file in apkFiles) {
            val version = file.name.removePrefix("update-").removeSuffix(".apk")
            if (!isNewer(version)) continue
            if (bestVersion == null || isVersionNewer(version, bestVersion)) {
                bestVersion = version
                bestFile = file
            }
        }
        bestFile?.let { showUpdateDialog(activity, it) }
    }

    /**
     * Show the update dialog for an APK at the given path. Used when download completes
     * while the user is in MainActivity (via broadcast).
     */
    fun showUpdateDialogForPath(activity: Activity, apkPath: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        val apkFile = File(apkPath)
        if (!apkFile.exists()) return
        showUpdateDialog(activity, apkFile)
    }

    private fun getUpdateDir(context: Context): File {
        val root = context.getExternalFilesDir(null) ?: context.cacheDir
        return File(root, "updates")
    }

    private fun deleteDownloadedApks(activity: Activity) {
        val updateDir = getUpdateDir(activity)
        if (!updateDir.exists()) return
        updateDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                file.delete()
            }
        }
        val legacyCacheDir = File(activity.cacheDir, "updates")
        if (legacyCacheDir.exists()) {
            legacyCacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk")) file.delete()
            }
        }
    }

    private fun showUpdateDialog(activity: Activity, apkFile: File) {
        if (activity.isFinishing || activity.isDestroyed) return
        val dialog = BottomSheetDialog(activity)
        val root = android.widget.FrameLayout(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_update, root, false)
        view.background = null
        dialog.setContentView(view)

        view.findViewById<MaterialButton>(R.id.updateNowBtn).setOnClickListener {
            dialog.dismiss()
            cancelDownloadNotification(activity.applicationContext)
            installApk(activity, apkFile)
        }
        view.findViewById<View>(R.id.maybeLaterBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.post {
                val params = bottomSheet.layoutParams ?: return@post
                params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                bottomSheet.layoutParams = params
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
                val radiusPx = 24 * bottomSheet.resources.displayMetrics.density
                val shape = GradientDrawable().apply {
                    setColor(
                        MaterialColors.getColor(
                            bottomSheet,
                            com.google.android.material.R.attr.colorSurface
                        )
                    )
                    cornerRadii =
                        floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f)
                }
                bottomSheet.background = shape
            }
        }

        dialog.show()
    }

    internal fun installApk(activity: Activity, apkFile: File) {
        cancelDownloadNotification(activity.applicationContext)
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
