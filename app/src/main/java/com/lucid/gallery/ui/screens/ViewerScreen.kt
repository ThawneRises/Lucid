package com.lucid.gallery.ui.screens

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.exifinterface.media.ExifInterface
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var showInfoSheet by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isRecentlyDeleted = bucketId == com.lucid.gallery.ui.screens.RECENTLY_DELETED_BUCKET_ID
    val coroutineScope = rememberCoroutineScope()
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) onBack()
    }

    LaunchedEffect(bucketId) {
        if (bucketId == -1L) {
            val allMedia = repo.fetchAllMedia()
            val albums = repo.fetchAlbums()
            val cameraAlbumId = albums.firstOrNull { it.isDefaultCamera }?.id ?: -1L
            val selectedFilters = prefs.selectedFilters.ifEmpty { setOf("Camera") }
            val sortMode = prefs.sortMode
            val selectedBucketIds = selectedFilters.mapNotNull { filter ->
                if (filter == "Camera") {
                    cameraAlbumId.takeIf { it != -1L }
                } else {
                    albums.firstOrNull { it.name == filter }?.id
                }
            }.toSet()

            val filtered = allMedia.filter { item -> selectedBucketIds.contains(item.bucketId) }
            mediaItems = if (sortMode == "added") filtered.sortedByDescending { it.addedTimestamp } else filtered.sortedByDescending { it.capturedTimestamp }
        } else if (isRecentlyDeleted) {
            mediaItems = repo.fetchTrashedMedia()
        } else {
            mediaItems = repo.fetchMediaInAlbum(bucketId)
        }

        val index = mediaItems.indexOfFirst { it.id == initialMediaId }
        initialIndex = if (index != -1) index else 0
    }

    if (mediaItems.isNotEmpty() && initialIndex != -1) {
        val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaItems.size })
        val currentMediaItem = mediaItems.getOrNull(pagerState.currentPage)

        LaunchedEffect(pagerState.currentPage) {
            currentMediaItem?.let { onMediaChanged(it.id) }
        }

        LaunchedEffect(currentMediaItem?.uri) {
            isFavorite = currentMediaItem?.let { repo.isFavorite(it.uri) } ?: false
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showControls = !showControls },
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
                    if (!isRecentlyDeleted) {
                        IconButton(onClick = {
                        currentMediaItem?.let { item ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (item.isVideo) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, item.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                        }
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = {
                        currentMediaItem?.let { item ->
                            val editIntent = Intent(Intent.ACTION_EDIT).apply {
                                setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            // Fallback catch if no editor is installed
                            try {
                                context.startActivity(Intent.createChooser(editIntent, "Edit Media"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No editor installed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = {
                        currentMediaItem?.let { item ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                coroutineScope.launch {
                                    val updated = repo.setFavorite(item.uri, !isFavorite)
                                    if (updated) isFavorite = !isFavorite
                                }
                            } else {
                                Toast.makeText(context, "Favorites require Android 11 or newer", Toast.LENGTH_SHORT).show()
                            }
                        }
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = {
                        showInfoSheet = true
                    }) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
                    }
                    if (isRecentlyDeleted) {
                        IconButton(onClick = {
                            currentMediaItem?.let { item ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    repo.createPermanentDeleteRequest(item.uri, item.isVideo)?.let { request ->
                                        deleteLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(request.intentSender).build()
                                        )
                                    }
                                } else {
                                    Toast.makeText(context, "Permanent delete requires Android 11 or newer", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete permanently", tint = Color.White)
                        }
                        IconButton(onClick = {
                            currentMediaItem?.let { item ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    repo.createRestoreRequest(item.uri, item.isVideo)?.let { request ->
                                        deleteLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(request.intentSender).build()
                                        )
                                    }
                                } else {
                                    Toast.makeText(context, "Restore requires Android 11 or newer", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.RestoreFromTrash, contentDescription = "Restore", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = {
                            currentMediaItem?.let { item ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    repo.createTrashRequest(item.uri, item.isVideo)?.let { request ->
                                        deleteLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(request.intentSender).build()
                                        )
                                    } ?: Toast.makeText(
                                        context,
                                        "Unable to move this media to Recently deleted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Recently deleted requires Android 11 or newer",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Move to Recently deleted", tint = Color.White)
                        }
                    }
                }
            }

            currentMediaItem?.let { item ->
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete media?") },
                        text = { Text("This will move the item to Recently deleted.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                coroutineScope.launch {
                                    if (repo.delete(item.uri)) onBack()
                                }
                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }

            if (showInfoSheet && currentMediaItem != null) {
                MediaInfoSheet(
                    mediaItem = currentMediaItem,
                    onDismiss = { showInfoSheet = false }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    }
}

private data class MediaInfo(
    val name: String,
    val path: String,
    val type: String,
    val size: String,
    val dimensions: String,
    val date: String,
    val camera: String,
    val aperture: String,
    val exposure: String
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MediaInfoSheet(mediaItem: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var info by remember(mediaItem.uri) { mutableStateOf<MediaInfo?>(null) }

    LaunchedEffect(mediaItem.uri) {
        info = loadMediaInfo(context, mediaItem)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Text("Media information", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(18.dp))
            if (info == null) {
                Text("Loading details...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val details = listOf(
                    "Name" to info!!.name,
                    "Folder" to info!!.path,
                    "Type" to info!!.type,
                    "Size" to info!!.size,
                    "Dimensions" to info!!.dimensions,
                    "Date taken" to info!!.date,
                    "Camera" to info!!.camera,
                    "Aperture" to info!!.aperture,
                    "Exposure" to info!!.exposure
                )
                details.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                    Column(modifier = Modifier.padding(vertical = 7.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(value, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

private suspend fun loadMediaInfo(context: Context, mediaItem: MediaItem): MediaInfo = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver
    var name = mediaItem.uri.lastPathSegment.orEmpty()
    var size = ""
    var type = if (mediaItem.isVideo) "Video" else "Image"

    contentResolver.query(
        mediaItem.uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, "mime_type"),
        null,
        null,
        null
    )?.use { cursor ->
        val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
        val typeColumn = cursor.getColumnIndex("mime_type")
        if (cursor.moveToFirst()) {
            if (nameColumn >= 0) name = cursor.getString(nameColumn) ?: name
            if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = formatFileSize(cursor.getLong(sizeColumn))
            if (typeColumn >= 0) type = cursor.getString(typeColumn).orEmpty().ifBlank { type }
        }
    }

    var dimensions = ""
    var date = ""
    var camera = ""
    var aperture = ""
    var exposure = ""
    val inputStream = contentResolver.openInputStream(mediaItem.uri)
    if (!mediaItem.isVideo && inputStream != null) {
        inputStream.use { stream ->
            val exif = ExifInterface(stream)
            camera = exif.getAttribute(ExifInterface.TAG_MODEL).orEmpty()
            aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }.orEmpty()
            exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME).orEmpty()
            date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL).orEmpty()
        }
        contentResolver.openInputStream(mediaItem.uri)?.use { stream ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, bounds)
            if (bounds.outWidth > 0 && bounds.outHeight > 0) dimensions = "${bounds.outWidth} x ${bounds.outHeight}"
        }
    } else {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, mediaItem.uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (!width.isNullOrBlank() && !height.isNullOrBlank()) dimensions = "$width x $height"
            date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE).orEmpty()
        } finally {
            retriever.release()
        }
    }

    MediaInfo(
        name = name,
        path = mediaItem.uri.toString(),
        type = type,
        size = size,
        dimensions = dimensions,
        date = date,
        camera = camera,
        aperture = aperture,
        exposure = exposure
    )
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024f)
    bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    else -> "%.1f GB".format(bytes / (1024f * 1024f * 1024f))
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
        delay(350.milliseconds)

        val player = ExoPlayer.Builder(context).build()
        // Attach listener BEFORE prepare() so we catch the STATE_READY signal instantly
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    duration = player.duration.toFloat().coerceAtLeast(0f)
                    isReady = true
                }
            }
        })
        player.setMediaItem(ExoMediaItem.fromUri(mediaItem.uri))
        player.prepare()
        player.playWhenReady = true

        exoPlayer = player
    }

    DisposableEffect(exoPlayer) {
        onDispose {
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