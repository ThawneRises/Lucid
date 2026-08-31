package com.lucid.gallery.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.ui.theme.TextSecondary
import com.lucid.gallery.ui.theme.Typography

@Composable
fun PhotosScreen(
    gridState: LazyGridState,
    syncedMediaId: Long?,
    onMediaClick: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val albums = repo.fetchAlbums()
        val cameraAlbum = albums.firstOrNull { it.isDefaultCamera } ?: albums.firstOrNull()
        if (cameraAlbum != null) {
            mediaItems = repo.fetchMediaInAlbum(cameraAlbum.id)
        }
    }

    LaunchedEffect(syncedMediaId, mediaItems) {
        if (syncedMediaId != null && mediaItems.isNotEmpty()) {
            val index = mediaItems.indexOfFirst { it.id == syncedMediaId }
            if (index != -1 && !gridState.layoutInfo.visibleItemsInfo.any { it.index == index }) {
                gridState.scrollToItem(index)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("ALL MOMENTS", color = MaterialTheme.colorScheme.primary, style = Typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Photos", color = MaterialTheme.colorScheme.onBackground, style = Typography.displaySmall)
                    Row {
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1 folder(s) selected", color = TextSecondary, style = Typography.labelMedium)
                    Text("${mediaItems.size} shown", color = TextSecondary, style = Typography.labelMedium)
                }
            }
            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 120.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaItems, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
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
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}