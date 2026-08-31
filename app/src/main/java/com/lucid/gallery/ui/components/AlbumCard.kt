package com.lucid.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lucid.gallery.data.Album
import com.lucid.gallery.ui.theme.TextSecondary
import com.lucid.gallery.ui.theme.Typography

@Composable
fun AlbumCard(
    album: Album,
    isLarge: Boolean = false,
    onClick: () -> Unit = {}
) {
    val cardRatio = if (isLarge) 1.55f else 0.92f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cardRatio)
                .clip(RoundedCornerShape(if (isLarge) 22.dp else 16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
            if (isLarge) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(album.mediaCount.toString(), color = MaterialTheme.colorScheme.onSurface, style = Typography.labelMedium)
                    Text(" items", color = TextSecondary, style = Typography.labelMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(album.name, color = MaterialTheme.colorScheme.onBackground, style = Typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!isLarge) Text("${album.mediaCount} memories", color = TextSecondary, style = Typography.labelMedium)
            }
            if (isLarge) {
                IconButton(onClick = onClick) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Open ${album.name}", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}