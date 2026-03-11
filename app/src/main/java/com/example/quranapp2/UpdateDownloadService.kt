package com.example.quranapp2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class UpdateDownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_APK_URL) ?: run {
            Log.e(TAG, "Missing EXTRA_APK_URL")
            stopSelf()
            return START_NOT_STICKY
        }
        val destPath = intent.getStringExtra(EXTRA_APK_PATH) ?: run {
            Log.e(TAG, "Missing EXTRA_APK_PATH")
            stopSelf()
            return START_NOT_STICKY
        }
        val destFile = File(destPath)
        if (destFile.exists()) {
            showCompletedNotification(destPath)
            sendBroadcast(
                Intent(AppUpdateChecker.ACTION_UPDATE_DOWNLOAD_COMPLETE).apply {
                    setPackage(this@UpdateDownloadService.packageName)
                    putExtra(AppUpdateChecker.EXTRA_APK_PATH, destPath)
                }
            )
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        // FOREGROUND_SERVICE_TYPE_DATA_SYNC = 4 (API 29+); use 0 on older APIs
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) 4 else 0
        ServiceCompat.startForeground(
            this,
            AppUpdateChecker.DOWNLOAD_NOTIFICATION_ID,
            buildProgressNotification(0, 0),
            serviceType
        )
        Thread {
            try {
                runDownload(url, destPath)
                showCompletedNotification(destPath)
                sendBroadcast(
                    Intent(AppUpdateChecker.ACTION_UPDATE_DOWNLOAD_COMPLETE).apply {
                        setPackage(this@UpdateDownloadService.packageName)
                        putExtra(AppUpdateChecker.EXTRA_APK_PATH, destPath)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
                stopSelf()
            }
        }.start()
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_updates),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
    }

    private fun runDownload(assetUrl: String, destPath: String) {
        val url = URL(assetUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/octet-stream")
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        try {
            conn.connect()
            val contentLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                conn.contentLengthLong
            } else {
                @Suppress("DEPRECATION")
                conn.contentLength.toLong()
            }
            val totalBytes = if (contentLength > 0) contentLength else -1L
            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloadedBytes = 0L
                    var lastUpdate = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val now = System.currentTimeMillis()
                        if (totalBytes > 0L && (now - lastUpdate >= UPDATE_INTERVAL_MS || downloadedBytes >= totalBytes)) {
                            lastUpdate = now
                            val notification =
                                buildProgressNotification(totalBytes, downloadedBytes)
                            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                                .notify(AppUpdateChecker.DOWNLOAD_NOTIFICATION_ID, notification)
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun buildProgressNotification(
        totalBytes: Long,
        downloadedBytes: Long
    ): android.app.Notification {
        val totalMb = totalBytes / (1024.0 * 1024.0)
        val doneMb = downloadedBytes / (1024.0 * 1024.0)
        val progressText = if (totalBytes > 0) {
            String.format(Locale.ROOT, "%.2f MB / %.2f MB", doneMb, totalMb)
        } else {
            String.format(Locale.ROOT, "%.2f MB", doneMb)
        }
        val max = if (totalBytes > 0) totalBytes.toInt() else 0
        val progress = if (totalBytes > 0) downloadedBytes.toInt() else 0
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(progressText)
            .setProgress(max, progress, max == 0)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCompletedNotification(apkPath: String) {
        val installIntent = Intent(this, InstallUpdateActivity::class.java).apply {
            putExtra(InstallUpdateActivity.EXTRA_APK_PATH, apkPath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val installPendingIntent = PendingIntent.getActivity(
            this,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_download_complete))
            .setOngoing(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_upload,
                getString(R.string.notification_action_install),
                installPendingIntent
            )
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(AppUpdateChecker.DOWNLOAD_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "UpdateDownloadService"
        private const val CHANNEL_ID = "app_updates"
        private const val EXTRA_APK_URL = "apk_url"
        private const val EXTRA_APK_PATH = "apk_path"
        private const val BUFFER_SIZE = 8192
        private const val UPDATE_INTERVAL_MS = 500L

        fun start(context: android.content.Context, apkUrl: String, apkPath: String) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                putExtra(EXTRA_APK_URL, apkUrl)
                putExtra(EXTRA_APK_PATH, apkPath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
