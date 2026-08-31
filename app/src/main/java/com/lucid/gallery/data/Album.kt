package com.lucid.gallery.data

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val coverUri: Uri,
    val mediaCount: Int,
    val isDefaultCamera: Boolean = false
)