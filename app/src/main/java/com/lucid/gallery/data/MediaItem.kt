package com.lucid.gallery.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val bucketId: Long = 0L,
    val timestamp: Long = 0L,
    val addedTimestamp: Long = timestamp,
    val capturedTimestamp: Long = timestamp,
    val isVideo: Boolean = false
)