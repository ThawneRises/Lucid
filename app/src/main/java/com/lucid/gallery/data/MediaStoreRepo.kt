package com.lucid.gallery.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContentResolver
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepo(private val context: Context) {

    suspend fun isFavorite(uri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext false
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.IS_FAVORITE),
            null,
            null,
            null
        )?.use { cursor ->
            val column = cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
            if (column >= 0 && cursor.moveToFirst()) cursor.getInt(column) == 1 else false
        } ?: false
    }

    suspend fun setFavorite(uri: android.net.Uri, favorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext false
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_FAVORITE, if (favorite) 1 else 0)
        }
        context.contentResolver.update(uri, values, null, null) > 0
    }

    fun createTrashRequest(uri: android.net.Uri, isVideo: Boolean): android.app.PendingIntent? {
        return createMediaStoreRequest(uri, isVideo, trash = true)
    }

    fun createRestoreRequest(uri: android.net.Uri, isVideo: Boolean): android.app.PendingIntent? {
        return createMediaStoreRequest(uri, isVideo, trash = false)
    }

    fun createPermanentDeleteRequest(uri: android.net.Uri, isVideo: Boolean): android.app.PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val itemUri = mediaUri(uri, isVideo) ?: return null
            MediaStore.createDeleteRequest(context.contentResolver, listOf(itemUri))
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    fun createRestoreRequest(items: List<MediaItem>): android.app.PendingIntent? {
        return createMediaStoreRequest(items, trash = false)
    }

    fun createPermanentDeleteRequest(items: List<MediaItem>): android.app.PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val mediaUris = items.mapNotNull { mediaUri(it.uri, it.isVideo) }
            if (mediaUris.isEmpty()) null else MediaStore.createDeleteRequest(context.contentResolver, mediaUris)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun createMediaStoreRequest(uri: android.net.Uri, isVideo: Boolean, trash: Boolean): android.app.PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val itemUri = mediaUri(uri, isVideo) ?: return null
            MediaStore.createTrashRequest(context.contentResolver, listOf(itemUri), trash)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun createMediaStoreRequest(items: List<MediaItem>, trash: Boolean): android.app.PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val mediaUris = items.mapNotNull { mediaUri(it.uri, it.isVideo) }
            if (mediaUris.isEmpty()) null else MediaStore.createTrashRequest(context.contentResolver, mediaUris, trash)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun mediaUri(uri: android.net.Uri, isVideo: Boolean): android.net.Uri? {
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return try {
            ContentUris.withAppendedId(collection, ContentUris.parseId(uri))
        } catch (_: NumberFormatException) {
            null
        }
    }

    suspend fun delete(uri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        context.contentResolver.delete(uri, null, null) > 0
    }

    suspend fun fetchTrashedMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.IS_TRASHED} = 1 AND (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val queryUri = MediaStore.Files.getContentUri("external")

        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1)
        }

        context.contentResolver.query(queryUri, projection, queryArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val added = cursor.getLong(dateAddedColumn) * 1000L
                val taken = cursor.getLong(dateTakenColumn)
                mediaList.add(
                    MediaItem(
                        id = cursor.getLong(idColumn),
                        uri = ContentUris.withAppendedId(queryUri, cursor.getLong(idColumn)),
                        bucketId = cursor.getLong(bucketIdColumn),
                        timestamp = if (taken > 0L) taken else added,
                        addedTimestamp = added,
                        capturedTimestamp = if (taken > 0L) taken else added,
                        isVideo = cursor.getInt(typeColumn) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    )
                )
            }
        }
        mediaList
    }

    suspend fun fetchAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumsMap = mutableMapOf<Long, AlbumBuilder>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketId = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                val path = cursor.getString(pathColumn).orEmpty()
                val isDefaultCamera = bucketName.equals("Camera", ignoreCase = true) || path.replace('\\', '/').contains("/DCIM/Camera/", ignoreCase = true)
                val contentUri = ContentUris.withAppendedId(queryUri, id)

                val albumBuilder = albumsMap.getOrPut(bucketId) { AlbumBuilder(bucketId, bucketName, contentUri, 0, isDefaultCamera) }
                albumBuilder.count++
            }
        }
        return@withContext albumsMap.values.map { Album(it.id, it.name, it.coverUri, it.count, it.isDefaultCamera) }.sortedByDescending { it.mediaCount }
    }

    suspend fun fetchAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val queryUri = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val added = cursor.getLong(dateAddedColumn) * 1000L
                val taken = cursor.getLong(dateTakenColumn)
                val mediaType = cursor.getInt(typeColumn)

                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = mediaUri(
                            ContentUris.withAppendedId(queryUri, id),
                            mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        ) ?: ContentUris.withAppendedId(queryUri, id),
                        bucketId = cursor.getLong(bucketIdColumn),
                        timestamp = if (taken > 0L) taken else added,
                        addedTimestamp = added,
                        capturedTimestamp = if (taken > 0L) taken else added,
                        isVideo = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                    )
                )
            }
        }
        return@withContext mediaList
    }

    suspend fun fetchMediaInAlbum(bucketId: Long): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.BUCKET_ID} = ? AND (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val selectionArgs = arrayOf(
            bucketId.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaType = cursor.getInt(typeColumn)
                val contentUri = ContentUris.withAppendedId(queryUri, id)
                val added = cursor.getLong(dateAddedColumn) * 1000L
                val taken = cursor.getLong(dateTakenColumn)

                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = mediaUri(contentUri, mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) ?: contentUri,
                        bucketId = bucketId,
                        timestamp = if (taken > 0L) taken else added,
                        addedTimestamp = added,
                        capturedTimestamp = if (taken > 0L) taken else added,
                        isVideo = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                    )
                )
            }
        }
        return@withContext mediaList
    }

    private class AlbumBuilder(
        val id: Long,
        val name: String,
        val coverUri: android.net.Uri,
        var count: Int,
        val isDefaultCamera: Boolean
    )
}