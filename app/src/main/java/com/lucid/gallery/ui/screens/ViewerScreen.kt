package com.lucid.gallery.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    val prefs = remember { PreferencesManager(context) }

    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var initialIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(bucketId) {
        if (bucketId == -1L) {
            // Apply global filtering logic from the PhotosScreen
            val allMedia = repo.fetchAllMedia()
            val albums = repo.fetchAlbums()
            val cameraAlbumId = albums.firstOrNull { it.isDefaultCamera }?.id ?: -1L
            val selectedFilters = prefs.selectedFilters
            val sortMode = prefs.sortMode
            val isAllSelected = selectedFilters.isEmpty() || selectedFilters.contains("All")

            val filtered = if (isAllSelected) {
                allMedia
            } else {
                allMedia.filter { item ->
                    var matches = false
                    if (selectedFilters.contains("Camera") && item.bucketId == cameraAlbumId) matches = true
                    if (selectedFilters.contains("Photo") && !item.isVideo) matches = true
                    if (selectedFilters.contains("Video") && item.isVideo) matches = true
                    if (selectedFilters.contains("Screenshots") && item.uri.path?.contains("Screenshot", ignoreCase = true) == true) matches = true
                    matches
                }
            }

            mediaItems = if (sortMode == "added") {
                filtered.sortedByDescending { it.addedTimestamp }
            } else {
                filtered.sortedByDescending { it.capturedTimestamp }
            }
        } else {
            // Fetch isolated album
            mediaItems = repo.fetchMediaInAlbum(bucketId)
        }

        val index = mediaItems.indexOfFirst { it.id == initialMediaId }
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

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.48f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    }
}