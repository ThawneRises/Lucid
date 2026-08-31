package com.lucid.gallery.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.data.MediaStoreRepo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    bucketId: Long,
    initialMediaId: Long,
    onMediaChanged: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var initialIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(bucketId) {
        val items = repo.fetchMediaInAlbum(bucketId)
        mediaItems = items
        val index = items.indexOfFirst { it.id == initialMediaId }
        initialIndex = if (index != -1) index else 0
    }

    if (mediaItems.isNotEmpty() && initialIndex != -1) {
        val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaItems.size })

        LaunchedEffect(pagerState.currentPage) {
            mediaItems.getOrNull(pagerState.currentPage)?.let { onMediaChanged(it.id) }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> mediaItems.getOrNull(page)?.id ?: page }
            ) { page ->
                val mediaItem = mediaItems[page]

                AsyncImage(
                    model = mediaItem.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    }
}