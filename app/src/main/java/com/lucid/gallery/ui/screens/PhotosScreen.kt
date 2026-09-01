package com.lucid.gallery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.data.PreferencesManager
import com.lucid.gallery.ui.theme.Typography

@Composable
fun PhotosScreen(
    gridState: LazyGridState,
    syncedMediaId: Long?,
    onMediaClick: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    val prefs = remember { PreferencesManager(context) }

    var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var cameraAlbumId by remember { mutableStateOf(-1L) }
    var albumFilters by remember { mutableStateOf<List<com.lucid.gallery.data.Album>>(emptyList()) }

    var selectedFilters by remember {
        mutableStateOf(
            prefs.selectedFilters
                .takeIf { it.isNotEmpty() }
                ?: setOf("Camera")
        )
    }
    var sortMode by remember { mutableStateOf(prefs.sortMode) }
    var showMenu by remember { mutableStateOf(false) }
    var isFilterExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val albums = repo.fetchAlbums()
        albumFilters = albums
        cameraAlbumId = albums.firstOrNull { it.isDefaultCamera }?.id ?: -1L
        allMedia = repo.fetchAllMedia()

        if (prefs.selectedFilters.isEmpty()) {
            selectedFilters = setOf("Camera")
            prefs.selectedFilters = setOf("Camera")
        }
    }

    val filterOptions = remember(albumFilters) {
        listOf("Camera") + albumFilters.filter { !it.isDefaultCamera }.map { it.name }
    }

    val displayedItems = remember(allMedia, selectedFilters, sortMode, cameraAlbumId, albumFilters) {
        val selectedBucketIds = selectedFilters.mapNotNull { filter ->
            if (filter == "Camera") {
                cameraAlbumId.takeIf { it != -1L }
            } else {
                albumFilters.firstOrNull { it.name == filter }?.id
            }
        }.toSet()

        val filtered = allMedia.filter { item -> selectedBucketIds.contains(item.bucketId) }
        if (sortMode == "added") {
            filtered.sortedByDescending { it.addedTimestamp }
        } else {
            filtered.sortedByDescending { it.capturedTimestamp }
        }
    }

    LaunchedEffect(syncedMediaId, displayedItems) {
        if (syncedMediaId != null && displayedItems.isNotEmpty()) {
            val index = displayedItems.indexOfFirst { it.id == syncedMediaId }
            if (index != -1 && !gridState.layoutInfo.visibleItemsInfo.any { it.index == index }) {
                gridState.scrollToItem(index)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Photos", color = MaterialTheme.colorScheme.onBackground, style = Typography.displaySmall)
                Row {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort and Filter", tint = MaterialTheme.colorScheme.onBackground)
                        }

                        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(24.dp))) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .width(230.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by recently added", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { if (sortMode == "added") Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { sortMode = "added"; prefs.sortMode = "added" }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by date captured", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { if (sortMode == "captured") Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { sortMode = "captured"; prefs.sortMode = "captured" }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = { Text("Filter", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { Icon(if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { isFilterExpanded = !isFilterExpanded }
                                )
                                AnimatedVisibility(
                                    visible = isFilterExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column {
                                        filterOptions.forEach { filter ->
                                            val isChecked = selectedFilters.contains(filter)
                                            DropdownMenuItem(
                                                text = { Text(filter, color = MaterialTheme.colorScheme.onSurface) },
                                                trailingIcon = { if (isChecked) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    val set = selectedFilters.toMutableSet()
                                                    if (set.contains(filter)) {
                                                        set.remove(filter)
                                                    } else {
                                                        set.add(filter)
                                                    }
                                                    val updated = if (set.isEmpty()) setOf("Camera") else set
                                                    selectedFilters = updated
                                                    prefs.selectedFilters = updated
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (displayedItems.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("Nothing here yet", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = Typography.bodyLarge)
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedItems, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onMediaClick(item) }
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (item.isVideo) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}