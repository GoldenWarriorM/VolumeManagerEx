package gwm.volume.ex.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItem
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import gwm.volume.ex.data.App

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsContent(
    apps: Collection<App>,
    excludedPackages: Set<String>,
    onExcludeChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val excludedApps = apps.filter { it.packageName in excludedPackages }.sortedWith(App.comparator)
    val nonExcludedApps = apps.filter { it.packageName !in excludedPackages }.sortedWith(App.comparator)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excluded Apps") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (excludedApps.isNotEmpty()) {
                item(key = "excluded_header") {
                    Text(
                        text = "Excluded (${excludedApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    )
                }

                items(
                    items = excludedApps,
                    key = { it.packageName }
                ) { app ->
                    ExcludedAppRow(
                        app = app,
                        isExcluded = true,
                        onToggle = { onExcludeChange(app.packageName, false) }
                    )
                }
            }

            item(key = "all_apps_header") {
                Text(
                    text = "All Apps (${nonExcludedApps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp)
                )
            }

            items(
                items = nonExcludedApps,
                key = { it.packageName }
            ) { app ->
                ExcludedAppRow(
                    app = app,
                    isExcluded = false,
                    onToggle = { onExcludeChange(app.packageName, true) }
                )
            }
        }
    }
}

@Composable
private fun ExcludedAppRow(
    app: App,
    isExcluded: Boolean,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isExcluded) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    )

    Row(
        modifier = Modifier
            .animateItem()
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )

        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isExcluded) Icons.Default.RemoveCircle else Icons.Default.Close,
                contentDescription = if (isExcluded) "Remove from exclusions" else "Exclude from overlay",
                tint = if (isExcluded) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
