package com.lucid.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lucid.gallery.data.Album
import com.lucid.gallery.data.MediaStoreRepo
import com.lucid.gallery.ui.theme.Coral
import com.lucid.gallery.ui.theme.TextSecondary
import com.lucid.gallery.ui.theme.Typography

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val repo = remember { MediaStoreRepo(context) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    val results = albums.filter { it.name.contains(query.trim(), ignoreCase = true) }

    LaunchedEffect(Unit) { albums = repo.fetchAlbums() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("FIND A MEMORY", color = MaterialTheme.colorScheme.primary, style = Typography.labelMedium)
                Text("Search", color = MaterialTheme.colorScheme.onBackground, style = Typography.displaySmall)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Search collections", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Coral) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
            Spacer(Modifier.height(16.dp))

            if (results.isEmpty() && query.isNotBlank()) {
                Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
                    Text("No collections match \"$query\"", color = MaterialTheme.colorScheme.onBackground, style = Typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results, key = { it.id }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { /* Album Click Implementation later */ }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = album.coverUri,
                                contentDescription = album.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(62.dp).clip(RoundedCornerShape(11.dp))
                            )
                            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                                Text(album.name, color = MaterialTheme.colorScheme.onBackground, style = Typography.bodyLarge)
                                Text("${album.mediaCount} memories", color = TextSecondary, style = Typography.labelMedium)
                            }
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = Coral)
                        }
                    }
                }
            }
        }
    }
}