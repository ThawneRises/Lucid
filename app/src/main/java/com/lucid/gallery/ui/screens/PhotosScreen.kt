package com.lucid.gallery.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.data.MediaStoreRepo

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotosScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    syncedMediaId: Long?,
    onMediaClick: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val gridState = rememberLazyGridState()

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        with(sharedTransitionScope) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaItems, key = { it.id }) { item ->
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "media-${item.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(350, easing = FastOutSlowInEasing) }
                            )
                            .clickable { onMediaClick(item) }
                    )
                }
            }
        }
    }
}