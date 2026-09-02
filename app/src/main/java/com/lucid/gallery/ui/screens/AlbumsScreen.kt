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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lucid.gallery.data.Album
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.ui.components.AlbumCard
import com.lucid.gallery.ui.theme.Coral
import com.lucid.gallery.ui.theme.TextSecondary
import com.lucid.gallery.ui.theme.Typography

@Composable
fun AlbumsScreen(
    onAlbumClick: (Long, String) -> Unit,
    onRecentlyDeletedClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        albums = repo.fetchAlbums()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("THE ARCHIVE", color = MaterialTheme.colorScheme.primary, style = Typography.labelMedium)
                    Text("Albums", color = MaterialTheme.colorScheme.onBackground, style = Typography.displaySmall)
                }
                Row {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isGridView) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 144.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalItemSpacing = 24.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                        val dynamicHeight = if (index % 4 == 1 || index % 4 == 2) 260.dp else 170.dp

                        AlbumCard(
                            album = album,
                            imageHeight = dynamicHeight,
                            onClick = { onAlbumClick(album.id, album.name) }
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) { Spacer(modifier = Modifier.height(16.dp)) }
                    item(span = StaggeredGridItemSpan.FullLine) { LockedFolderCard(title = "Secret folder") }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LockedFolderCard(title = "Recently deleted", onClick = onRecentlyDeletedClick)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 144.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(albums, key = { it.id }) { album ->
                        AlbumListRow(album = album, onClick = { onAlbumClick(album.id, album.name) })
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { LockedFolderCard(title = "Secret folder") }
                    item {
                        LockedFolderCard(title = "Recently deleted", onClick = onRecentlyDeletedClick)
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumListRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.coverUri,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
        ) {
            Text(album.name, color = MaterialTheme.colorScheme.onBackground, style = Typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text("${album.mediaCount} items", color = TextSecondary, style = Typography.labelMedium)
        }
        Icon(Icons.Outlined.Folder, contentDescription = null, tint = Coral.copy(alpha = 0.8f))
    }
}

@Composable
fun LockedFolderCard(title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground, style = Typography.bodyLarge)
        Icon(imageVector = Icons.Outlined.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.secondary)
    }
}