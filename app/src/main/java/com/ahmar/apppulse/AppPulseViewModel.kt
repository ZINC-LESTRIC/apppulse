package com.ahmar.apppulse

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmar.apppulse.data.AppCategory
import com.ahmar.apppulse.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppListItem(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable,
    val usageMillis: Long,
    val category: String
)

enum class SortOrder { MOST_USED, LEAST_USED }

class AppPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).appCategoryDao()

    var hasPermission by mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(application))
        private set

    var isLoading by mutableStateOf(false)
        private set

    var sortOrder by mutableStateOf(SortOrder.MOST_USED)
        private set

    private var rawUsageList by mutableStateOf<List<AppUsageInfo>>(emptyList())

    val categories: StateFlow<List<String>> = dao.getDistinctCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appList: StateFlow<List<AppListItem>> = combine(
        dao.getAll()
    ) { categoriesList ->
        val categoryMap = categoriesList.associate { it.packageName to it.category }
        val items = rawUsageList.map { usage ->
            AppListItem(
                packageName = usage.packageName,
                appName = usage.appName,
                icon = usage.icon,
                usageMillis = usage.usageMillis,
                category = categoryMap[usage.packageName] ?: "Uncategorized"
            )
        }
        when (sortOrder) {
            SortOrder.MOST_USED -> items.sortedByDescending { it.usageMillis }
            SortOrder.LEAST_USED -> items.sortedBy { it.usageMillis }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Re-compute when sort changes by triggering a dummy update via categories flow or manual
    // Since combine only depends on dao, we expose a derived list and refresh on sort change

    init {
        if (hasPermission) {
            loadUsageData()
        }
    }

    fun checkPermission() {
        hasPermission = UsageStatsHelper.hasUsageStatsPermission(getApplication())
        if (hasPermission && rawUsageList.isEmpty()) {
            loadUsageData()
        }
    }

    fun openUsageSettings() {
        UsageStatsHelper.openUsageAccessSettings(getApplication())
    }

    fun toggleSortOrder() {
        sortOrder = if (sortOrder == SortOrder.MOST_USED) SortOrder.LEAST_USED else SortOrder.MOST_USED
        // Force refresh of derived list by reloading or using a different approach
        // We'll recompute in UI or keep a separate state
        viewModelScope.launch {
            // Trigger recomposition by updating a trigger or just rely on UI reading sortOrder
        }
    }

    fun loadUsageData() {
        viewModelScope.launch {
            isLoading = true
            try {
                rawUsageList = UsageStatsHelper.getAppUsageStats(getApplication())
            } finally {
                isLoading = false
            }
        }
    }

    fun setCategory(packageName: String, category: String) {
        viewModelScope.launch {
            dao.insertOrUpdate(AppCategory(packageName, category.trim().ifEmpty { "Uncategorized" }))
        }
    }

    // Helper to get sorted list considering current sortOrder (since StateFlow combine doesn't auto-react to sortOrder)
    fun getSortedList(categoriesMap: Map<String, String>): List<AppListItem> {
        val items = rawUsageList.map { usage ->
            AppListItem(
                packageName = usage.packageName,
                appName = usage.appName,
                icon = usage.icon,
                usageMillis = usage.usageMillis,
                category = categoriesMap[usage.packageName] ?: "Uncategorized"
            )
        }
        return when (sortOrder) {
            SortOrder.MOST_USED -> items.sortedByDescending { it.usageMillis }
            SortOrder.LEAST_USED -> items.sortedBy { it.usageMillis }
        }
    }
}
