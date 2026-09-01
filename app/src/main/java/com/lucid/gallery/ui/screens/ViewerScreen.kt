package com.lucid.gallery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.data.PreferencesManager
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(bucketId) {
        if (bucketId == -1L) {
            val allMedia = repo.fetchAllMedia()
            val albums = repo.fetchAlbums()
            val cameraAlbumId = albums.firstOrNull { it.isDefaultCamera }?.id ?: -1L
            val selectedFilters = prefs.selectedFilters.ifEmpty { setOf("Camera") }
            val sortMode = prefs.sortMode
            val isAllSelected = selectedFilters.contains("All")

            val filtered = if (isAllSelected) allMedia else {
                allMedia.filter { item ->
                    var matches = false
                    if (selectedFilters.contains("Camera") && item.bucketId == cameraAlbumId) matches = true
                    if (selectedFilters.contains("Photo") && !item.isVideo) matches = true
                    if (selectedFilters.contains("Video") && item.isVideo) matches = true
                    if (selectedFilters.contains("Screenshots") && item.uri.path?.contains("Screenshot", ignoreCase = true) == true) matches = true
                    matches
                }
            }
            mediaItems = if (sortMode == "added") filtered.sortedByDescending { it.addedTimestamp } else filtered.sortedByDescending { it.capturedTimestamp }
        } else {
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
                modifier = Modifier.fillMaxSize().clickable { showControls = !showControls },
                beyondViewportPageCount = 1,
                key = { page -> mediaItems.getOrNull(page)?.id ?: page }
            ) { page ->
                val mediaItem = mediaItems[page]

                if (mediaItem.isVideo && pagerState.currentPage == page) {
                    VideoPlayerComponent(mediaItem = mediaItem, showControls = showControls)
                } else {
                    AsyncImage(
                        model = mediaItem.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.48f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1A1A1A).copy(alpha = 0.75f))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerComponent(mediaItem: MediaItem, showControls: Boolean) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isReady by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(mediaItem.uri) {
        delay(400.milliseconds)
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(mediaItem.uri))
            prepare()
            playWhenReady = true
        }
        exoPlayer = player
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    duration = exoPlayer?.duration?.toFloat()?.coerceAtLeast(0f) ?: 0f
                    isReady = true
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        while (isPlaying && exoPlayer != null) {
            currentPosition = exoPlayer?.currentPosition?.toFloat() ?: 0f
            delay(100.milliseconds)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = mediaItem.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        if (exoPlayer != null) {
            AnimatedVisibility(visible = isReady, enter = fadeIn(tween(300))) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showControls && isReady,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 112.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Slider(
                        value = if (duration > 0f) currentPosition / duration else 0f,
                        onValueChange = { percent ->
                            val seekPos = (percent * duration).toLong()
                            exoPlayer?.seekTo(seekPos)
                            currentPosition = seekPos.toFloat()
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}