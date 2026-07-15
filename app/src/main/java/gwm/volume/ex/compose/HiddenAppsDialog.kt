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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
fun HiddenAppsContent(
    apps: Collection<App>,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val query = searchQuery.lowercase().trim()

    val filteredHidden = apps
        .filter { it.hidden }
        .filter { query.isEmpty() || it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
        .sortedWith(App.comparator)

    val filteredVisible = apps
        .filter { !it.hidden }
        .filter { query.isEmpty() || it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
        .sortedWith(App.comparator)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Hidden Apps") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
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
            val recommended = apps
                .filter { it.packageName in RECOMMENDED_PACKAGES && !it.hidden }

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
                    RecommendedCard(app) { app.hidden = true }
                }
            }

            if (filteredHidden.isNotEmpty()) {
                item(key = "hidden_header") {
                    Text(
                        text = "Hidden (${filteredHidden.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    )
                }

                items(
                    items = filteredHidden,
                    key = { it.packageName.let { "hidden_$it" } }
                ) { app ->
                    HiddenAppCard(
                        app = app,
                        isHidden = true,
                        onToggle = { app.hidden = false }
                    )
                }
            }

            item(key = "visible_header") {
                Text(
                    text = "Visible (${filteredVisible.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp)
                )
            }

            items(
                items = filteredVisible,
                key = { it.packageName.let { "visible_$it" } }
            ) { app ->
                HiddenAppCard(
                    app = app,
                    isHidden = false,
                    onToggle = { app.hidden = true }
                )
            }
        }
    }
}

@Composable
private fun RecommendedCard(
    app: App,
    onHide: () -> Unit
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

            IconButton(onClick = onHide) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Hide app",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun HiddenAppCard(
    app: App,
    isHidden: Boolean,
    onToggle: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isHidden) {
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
                    imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isHidden) "Unhide app" else "Hide app",
                    tint = if (isHidden) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
