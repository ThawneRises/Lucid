package com.lucid.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lucid.gallery.data.Album
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.ui.components.AlbumCard
import com.lucid.gallery.ui.theme.TextSecondary
import com.lucid.gallery.ui.theme.Typography

@Composable
fun AlbumsScreen() {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }

    LaunchedEffect(Unit) {
        albums = repo.fetchAlbums()
    }

    val topAlbums = albums.take(4)
    val regularAlbums = albums.drop(4)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("THE ARCHIVE", color = MaterialTheme.colorScheme.primary, style = Typography.labelMedium)
                    Text("Albums", color = MaterialTheme.colorScheme.onBackground, style = Typography.displaySmall)
                }
                Row {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.MoreHoriz, contentDescription = "More options", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArchiveStat(value = albums.size.toString(), label = "collections")
                ArchiveStat(value = albums.sumOf { it.mediaCount }.toString(), label = "memories")
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 144.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(topAlbums, span = { album -> GridItemSpan(if (album == topAlbums.firstOrNull()) 2 else 1) }) { album ->
                    AlbumCard(
                        album = album,
                        isLarge = album == topAlbums.firstOrNull(),
                        onClick = { /* Navigate to album */ }
                    )
                }

                items(regularAlbums) { album ->
                    AlbumCard(album = album, isLarge = false, onClick = { /* Navigate to album */ })
                }

                item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(16.dp)) }
                item(span = { GridItemSpan(2) }) { LockedFolderCard(title = "Hidden") }
                item(span = { GridItemSpan(2) }) { LockedFolderCard(title = "Recently deleted") }
            }
        }
    }
}

@Composable
private fun ArchiveStat(value: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(value, color = MaterialTheme.colorScheme.onBackground, style = Typography.labelMedium)
        Text(" $label", color = TextSecondary, style = Typography.labelMedium)
    }
}

@Composable
fun LockedFolderCard(title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground, style = Typography.bodyLarge)
        Icon(imageVector = Icons.Outlined.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.secondary)
    }
}