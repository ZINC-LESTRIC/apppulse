package com.ahmar.apppulse

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmar.apppulse.data.AppCategory
import com.ahmar.apppulse.data.AppDatabase
import com.ahmar.apppulse.data.NotificationAllowlist
import com.ahmar.apppulse.data.NotificationLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppListItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val usageMillis: Long,
    val category: String
)

data class AllowlistItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val enabled: Boolean
)

enum class SortOrder { MOST_USED, LEAST_USED }

enum class AppTab { USAGE, ALLOWLIST, LOG }

class AppPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val categoryDao = db.appCategoryDao()
    private val allowlistDao = db.notificationAllowlistDao()
    private val logDao = db.notificationLogDao()

    // ---------- Phase 1: Usage ----------
    var hasUsagePermission by mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(application))
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _sortOrder = MutableStateFlow(SortOrder.MOST_USED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _rawUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())

    val categories: StateFlow<List<String>> = categoryDao.getDistinctCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appList: StateFlow<List<AppListItem>> = combine(
        _rawUsageList,
        categoryDao.getAll(),
        _sortOrder
    ) { usageList, categoryList, order ->
        val categoryMap = categoryList.associate { it.packageName to it.category }
        val items = usageList.map { usage ->
            AppListItem(
                packageName = usage.packageName,
                appName = usage.appName,
                icon = usage.icon,
                usageMillis = usage.usageMillis,
                category = categoryMap[usage.packageName] ?: "Uncategorized"
            )
        }
        when (order) {
            SortOrder.MOST_USED -> items.sortedByDescending { it.usageMillis }
            SortOrder.LEAST_USED -> items.sortedBy { it.usageMillis }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Phase 2: Notifications ----------
    var hasNotificationAccess by mutableStateOf(
        NotificationAccessHelper.hasNotificationAccess(application)
    )
        private set

    var selectedTab by mutableStateOf(AppTab.USAGE)
        private set

    private val _rawAppsForAllowlist = MutableStateFlow<List<AppUsageInfo>>(emptyList())

    val allowlistItems: StateFlow<List<AllowlistItem>> = combine(
        _rawAppsForAllowlist,
        allowlistDao.getAll()
    ) { apps, allowlist ->
        val enabledMap = allowlist.associate { it.packageName to it.enabled }
        apps
            .map { app ->
                AllowlistItem(
                    packageName = app.packageName,
                    appName = app.appName,
                    icon = app.icon,
                    enabled = enabledMap[app.packageName] == true
                )
            }
            .sortedBy { it.appName.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationLogs: StateFlow<List<NotificationLog>> = logDao.getRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (hasUsagePermission) {
            loadUsageData()
        }
        // Always try to load the list of installed apps for the allowlist screen
        loadInstalledAppsForAllowlist()
    }

    fun selectTab(tab: AppTab) {
        selectedTab = tab
    }

    fun checkPermissions() {
        val usageGranted = UsageStatsHelper.hasUsageStatsPermission(getApplication())
        hasUsagePermission = usageGranted
        if (usageGranted && _rawUsageList.value.isEmpty()) {
            loadUsageData()
        }

        hasNotificationAccess = NotificationAccessHelper.hasNotificationAccess(getApplication())
    }

    // Keep old name for Phase 1 compatibility
    fun checkPermission() = checkPermissions()

    fun openUsageSettings() {
        UsageStatsHelper.openUsageAccessSettings(getApplication())
    }

    fun openNotificationSettings() {
        NotificationAccessHelper.openNotificationListenerSettings(getApplication())
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.MOST_USED) {
            SortOrder.LEAST_USED
        } else {
            SortOrder.MOST_USED
        }
    }

    fun loadUsageData() {
        viewModelScope.launch {
            isLoading = true
            try {
                _rawUsageList.value = UsageStatsHelper.getAppUsageStats(getApplication())
            } finally {
                isLoading = false
            }
        }
    }

    fun setCategory(packageName: String, category: String) {
        viewModelScope.launch {
            val clean = category.trim().ifEmpty { "Uncategorized" }
            categoryDao.insertOrUpdate(AppCategory(packageName, clean))
        }
    }

    private fun loadInstalledAppsForAllowlist() {
        viewModelScope.launch {
            // Reuse the same helper; we only need package + name + icon
            val apps = UsageStatsHelper.getAppUsageStats(getApplication())
            _rawAppsForAllowlist.value = apps
        }
    }

    fun setAllowlistEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            allowlistDao.insertOrUpdate(NotificationAllowlist(packageName, enabled))
        }
    }

    fun clearNotificationLog() {
        viewModelScope.launch {
            logDao.clearAll()
        }
    }
}
