package com.lucid.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.ui.theme.Coral
import com.lucid.gallery.ui.theme.TextSecondary
import com.lucid.gallery.ui.theme.Typography

@Composable
fun SearchScreen(onMediaClick: (MediaItem) -> Unit) {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        allMedia = repo.fetchAllMedia().sortedByDescending { it.addedTimestamp }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            contentPadding = PaddingValues(top = 220.dp, bottom = 120.dp, start = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), 
            verticalItemSpacing = 4.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(allMedia, key = { _, item -> item.id }) { index, item ->
                val ratio = if (index % 5 == 0) 0.6f else 1f 
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onMediaClick(item) }
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                .padding(bottom = 16.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("EXPLORE", color = MaterialTheme.colorScheme.primary, style = Typography.labelMedium)
                Text("Discover", color = MaterialTheme.colorScheme.onBackground, style = Typography.displaySmall)
                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    placeholder = { Text("Search places, dates, tags...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Favorites", "Videos", "Screenshots", "Recent")) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                            .clickable { query = chip }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(chip, color = MaterialTheme.colorScheme.onBackground, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}