package com.example.quranapp2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import java.io.File

class InstallUpdateActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent?.getStringExtra(EXTRA_APK_PATH) ?: run {
            finish()
            return
        }
        val apkFile = File(path)
        if (!apkFile.exists()) {
            finish()
            return
        }
        AppUpdateChecker.cancelDownloadNotification(applicationContext)
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(installIntent)
        finish()
    }

    companion object {
        const val EXTRA_APK_PATH = "apk_path"
    }
}
