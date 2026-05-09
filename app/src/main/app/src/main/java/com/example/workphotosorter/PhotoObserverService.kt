package com.example.workphotosorter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

class PhotoObserverService : Service() {
    private lateinit var contentObserver: ContentObserver
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification()); registerContentObserver(); return START_STICKY
    }
    private fun registerContentObserver() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) { uri?.let { checkLatestImage(it) } }
        }
        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
    }
    private fun checkLatestImage(uri: Uri) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val prefs = getSharedPreferences(GeofenceBroadcastReceiver.PREFS_NAME, MODE_PRIVATE)
                val isInOffice = prefs.getBoolean(GeofenceBroadcastReceiver.KEY_IN_OFFICE, false)
                PhotoClassifier.isWorkPhoto(this, contentUri, isInOffice) { isWork ->
                    if (isWork) moveToWorkFolder(contentUri)
                }
            }
        }
    }
    private fun moveToWorkFolder(uri: Uri) {
        val workDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Work")
        if (!workDir.exists()) workDir.mkdirs()
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = PhotoClassifier.getFileName(this, uri) ?: "work_${System.currentTimeMillis()}.jpg"
        val newFile = File(workDir, fileName)
        inputStream?.use { input -> FileOutputStream(newFile).use { output -> input.copyTo(output) } }
        contentResolver.delete(uri, null, null)
    }
    private fun createNotification(): Notification {
        val channelId = "WorkPhotoChannel"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Work Photo Sorter", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId).setContentTitle("Work Photo Sorter attivo").setSmallIcon(android.R.drawable.ic_menu_camera).build()
    }
    override fun onDestroy() { contentResolver.unregisterContentObserver(contentObserver); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
