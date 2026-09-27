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

enum class SortOrder { MOST_USED, LEAST_USED }

class AppPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).appCategoryDao()

    var hasPermission by mutableStateOf(UsageStatsHelper.hasUsageStatsPermission(application))
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _sortOrder = MutableStateFlow(SortOrder.MOST_USED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _rawUsageList = MutableStateFlow<List<AppUsageInfo>>(emptyList())

    val categories: StateFlow<List<String>> = dao.getDistinctCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Properly combine raw usage + categories + sort order
    val appList: StateFlow<List<AppListItem>> = combine(
        _rawUsageList,
        dao.getAll(),
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

    init {
        if (hasPermission) {
            loadUsageData()
        }
    }

    fun checkPermission() {
        val granted = UsageStatsHelper.hasUsageStatsPermission(getApplication())
        hasPermission = granted
        if (granted && _rawUsageList.value.isEmpty()) {
            loadUsageData()
        }
    }

    fun openUsageSettings() {
        UsageStatsHelper.openUsageAccessSettings(getApplication())
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
            dao.insertOrUpdate(AppCategory(packageName, clean))
        }
    }
}
