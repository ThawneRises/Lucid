package com.lucid.gallery.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepo(private val context: Context) {

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
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
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
                val isDefaultCamera = bucketName.equals("Camera", ignoreCase = true) ||
                        path.replace('\\', '/').contains("/DCIM/Camera/", ignoreCase = true)

                val contentUri = ContentUris.withAppendedId(queryUri, id)

                val albumBuilder = albumsMap.getOrPut(bucketId) {
                    AlbumBuilder(bucketId, bucketName, contentUri, 0, isDefaultCamera)
                }
                albumBuilder.count++
            }
        }

        return@withContext albumsMap.values.map {
            Album(it.id, it.name, it.coverUri, it.count, it.isDefaultCamera)
        }.sortedByDescending { it.mediaCount }
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

                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        bucketId = bucketId,
                        timestamp = cursor.getLong(dateTakenColumn).takeIf { it > 0L } ?: (cursor.getLong(dateAddedColumn) * 1000L),
                        addedTimestamp = cursor.getLong(dateAddedColumn) * 1000L,
                        capturedTimestamp = cursor.getLong(dateTakenColumn).takeIf { it > 0L } ?: (cursor.getLong(dateAddedColumn) * 1000L),
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