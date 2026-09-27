package com.ahmar.apppulse

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ahmar.apppulse.data.AppDatabase
import com.ahmar.apppulse.data.NotificationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationReaderService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "NotificationReader"

    override fun onCreate() {
        super.onCreate()
        TtsManager.init(applicationContext)
        Log.i(TAG, "NotificationReaderService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Skip our own notifications
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return

        // Skip ongoing / foreground-service notifications
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        // Nothing useful to speak
        if (title.isEmpty() && text.isEmpty()) return

        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val allowlistDao = db.notificationAllowlistDao()
                val logDao = db.notificationLogDao()

                // Only speak if the user has explicitly enabled this package
                val enabled = allowlistDao.isEnabled(packageName) == true
                if (!enabled) return@launch

                val appName = try {
                    val pm = applicationContext.packageManager
                    val ai = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(ai).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName
                }

                val speakText = buildString {
                    append(appName)
                    append(" says: ")
                    if (title.isNotEmpty()) {
                        append(title)
                        if (text.isNotEmpty()) append(". ")
                    }
                    if (text.isNotEmpty()) append(text)
                }

                val spoken = TtsManager.speak(speakText)

                logDao.insert(
                    NotificationLog(
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        wasSpoken = spoken
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from $packageName", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not shut down TTS here — other components may still need it.
        // TtsManager lives for the process lifetime.
        Log.i(TAG, "NotificationReaderService destroyed")
    }
}
