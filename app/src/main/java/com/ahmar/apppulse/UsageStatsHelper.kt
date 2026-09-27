package com.ahmar.apppulse

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val usageMillis: Long
)

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    suspend fun getAppUsageStats(context: Context, days: Int = 7): List<AppUsageInfo> =
        withContext(Dispatchers.IO) {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val packageManager = context.packageManager

            val endTime = System.currentTimeMillis()
            val startTime = endTime - days * 24L * 60 * 60 * 1000

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()

            // Aggregate total time in foreground per package
            val usageMap = mutableMapOf<String, Long>()
            for (stats in usageStatsList) {
                val pkg = stats.packageName
                usageMap[pkg] = (usageMap[pkg] ?: 0L) + stats.totalTimeInForeground
            }

            // Get launchable apps only
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableApps = packageManager.queryIntentActivities(mainIntent, 0)

            val result = mutableListOf<AppUsageInfo>()
            val seenPackages = mutableSetOf<String>()

            for (resolveInfo in launchableApps) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName in seenPackages) continue
                seenPackages.add(packageName)

                // Skip our own app optionally, but include for completeness
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    // Filter out non-launchable system apps already handled by queryIntentActivities
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val usage = usageMap[packageName] ?: 0L

                    result.add(
                        AppUsageInfo(
                            packageName = packageName,
                            appName = appName,
                            icon = icon,
                            usageMillis = usage
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // Skip if package disappeared
                }
            }

            result
        }

    fun formatUsageTime(millis: Long): String {
        if (millis <= 0) return "0m"
        val totalMinutes = millis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
