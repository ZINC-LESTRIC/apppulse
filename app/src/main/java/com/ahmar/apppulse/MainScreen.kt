package com.ahmar.apppulse

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppPulseViewModel) {
    val selectedTab = viewModel.selectedTab
    val hasUsagePermission = viewModel.hasUsagePermission
    val hasNotificationAccess = viewModel.hasNotificationAccess
    val isLoading = viewModel.isLoading
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allowlistItems by viewModel.allowlistItems.collectAsStateWithLifecycle()
    val notificationLogs by viewModel.notificationLogs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AppPulse", fontWeight = FontWeight.Bold) },
                actions = {
                    if (selectedTab == AppTab.USAGE && hasUsagePermission) {
                        IconButton(onClick = { viewModel.toggleSortOrder() }) {
                            Icon(Icons.Default.Sort, contentDescription = "Toggle sort")
                        }
                        IconButton(onClick = { viewModel.loadUsageData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.USAGE,
                    onClick = { viewModel.selectTab(AppTab.USAGE) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Usage") },
                    label = { Text("Usage") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.ALLOWLIST,
                    onClick = { viewModel.selectTab(AppTab.ALLOWLIST) },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Allowlist") },
                    label = { Text("Allowlist") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.LOG,
                    onClick = { viewModel.selectTab(AppTab.LOG) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Log") },
                    label = { Text("Log") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.USAGE -> {
                if (!hasUsagePermission) {
                    GrantAccessScreen(
                        onGrantClick = { viewModel.openUsageSettings() },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = if (sortOrder == SortOrder.MOST_USED)
                                "Sorted by: Most used (last 7 days)"
                            else
                                "Sorted by: Least used (last 7 days)",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isLoading && appList.isEmpty()) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(appList, key = { it.packageName }) { item ->
                                    AppUsageRow(
                                        item = item,
                                        existingCategories = categories,
                                        onCategorySelected = { newCategory ->
                                            viewModel.setCategory(item.packageName, newCategory)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AppTab.ALLOWLIST -> {
                if (!hasNotificationAccess) {
                    NotificationAccessScreen(
                        onGrantClick = { viewModel.openNotificationSettings() },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    AllowlistScreen(
                        items = allowlistItems,
                        onToggle = { pkg, enabled ->
                            viewModel.setAllowlistEnabled(pkg, enabled)
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            AppTab.LOG -> {
                if (!hasNotificationAccess) {
                    NotificationAccessScreen(
                        onGrantClick = { viewModel.openNotificationSettings() },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    NotificationLogScreen(
                        logs = notificationLogs,
                        onClear = { viewModel.clearNotificationLog() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun GrantAccessScreen(onGrantClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Usage Access Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "AppPulse needs the \"Usage Access\" special permission to show how long you use each app. This is required by Android and cannot be granted via a normal dialog.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrantClick) {
            Text("Grant Access in Settings")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "After granting, return to this app. It will detect the permission automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppUsageRow(
    item: AppListItem,
    existingCategories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = remember(item.icon) {
                try {
                    item.icon.toBitmap(96, 96).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = item.appName,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.appName.take(1).uppercase())
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = UsageStatsHelper.formatUsageTime(item.usageMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AssistChip(
                onClick = { showDialog = true },
                label = {
                    Text(
                        text = item.category,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }

    if (showDialog) {
        CategoryDialog(
            currentCategory = item.category,
            existingCategories = existingCategories,
            onDismiss = { showDialog = false },
            onConfirm = { selected ->
                onCategorySelected(selected)
                showDialog = false
            }
        )
    }
}

@Composable
fun CategoryDialog(
    currentCategory: String,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentCategory) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Set Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingCategories.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Quick select:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Column {
                        existingCategories.chunked(3).forEach { rowCats ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowCats.forEach { cat ->
                                    FilterChip(
                                        selected = text.equals(cat, ignoreCase = true),
                                        onClick = { text = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(text) }) { Text("Save") }
                }
            }
        }
    }
}
