package gwm.volume.ex.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
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
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    key = { it.packageName.let { "excluded_$it" } }
                ) { app ->
                    ExcludedAppCard(
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
                key = { it.packageName.let { "all_$it" } }
            ) { app ->
                ExcludedAppCard(
                    app = app,
                    isExcluded = false,
                    onToggle = { onExcludeChange(app.packageName, true) }
                )
            }
        }
    }
}

@Composable
private fun ExcludedAppCard(
    app: App,
    isExcluded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateItemPlacement(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExcluded) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon!!,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp))
                )
            }

            Text(
                text = app.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
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
}
