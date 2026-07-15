package gwm.volume.ex.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gwm.volume.ex.data.App

private val RECOMMENDED_PACKAGES = setOf("android", "com.android.systemui")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsContent(
    apps: Collection<App>,
    excludedPackages: Set<String>,
    onExcludeChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val query = searchQuery.lowercase().trim()

    val filteredExcluded = apps
        .filter { it.packageName in excludedPackages }
        .filter { query.isEmpty() || it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
        .sortedWith(App.comparator)

    val filteredNonExcluded = apps
        .filter { it.packageName !in excludedPackages }
        .filter { query.isEmpty() || it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
        .sortedWith(App.comparator)

    val recommended = apps
        .filter { it.packageName in RECOMMENDED_PACKAGES && it.packageName !in excludedPackages }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Excluded Apps") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
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
            if (recommended.isNotEmpty() && query.isEmpty()) {
                item(key = "recommended_header") {
                    Text(
                        text = "Recommended",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    )
                }

                items(
                    items = recommended,
                    key = { it.packageName.let { "recommended_$it" } }
                ) { app ->
                    RecommendedCard(
                        app = app,
                        onAdd = { onExcludeChange(app.packageName, true) }
                    )
                }
            }

            if (filteredExcluded.isNotEmpty()) {
                item(key = "excluded_header") {
                    Text(
                        text = "Excluded (${filteredExcluded.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    )
                }

                items(
                    items = filteredExcluded,
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
                    text = "All Apps (${filteredNonExcluded.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp)
                )
            }

            items(
                items = filteredNonExcluded,
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
private fun RecommendedCard(
    app: App,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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

            Text(
                text = "Recommended",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to exclusions",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
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
    val cardColor by animateColorAsState(
        targetValue = if (isExcluded) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
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
