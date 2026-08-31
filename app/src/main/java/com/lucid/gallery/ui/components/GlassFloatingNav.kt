package com.lucid.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lucid.gallery.ui.theme.Coral
import com.lucid.gallery.ui.theme.DarkSurface
import com.lucid.gallery.ui.theme.Ink
import com.lucid.gallery.ui.theme.Paper
import com.lucid.gallery.ui.theme.TextPrimaryDark
import com.lucid.gallery.ui.theme.TextSecondary

@Composable
fun GlassFloatingNav(
    modifier: Modifier = Modifier,
    selectedTab: String = "photos",
    onPhotosClick: () -> Unit = {},
    onAlbumsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val darkTheme = isSystemInDarkTheme()
    val surfaceColor = if (darkTheme) Color(0xFF171817) else Paper
    val activeColor = if (darkTheme) TextPrimaryDark else Ink
    val inactiveColor = TextSecondary
    val activeSurface = if (darkTheme) DarkSurface else Color(0xFFE2DFD6)
    val borderColor = if (darkTheme) Color(0xFF3D3E3C) else Color(0xFFE3E0D8)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                selected = selectedTab == "photos",
                onClick = onPhotosClick,
                activeSurface = activeSurface,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            ) { color ->
                Icon(Icons.Outlined.Image, contentDescription = "Photos", tint = if (selectedTab == "photos") Coral else color, modifier = Modifier.size(24.dp))
            }

            NavItem(
                selected = selectedTab == "albums",
                onClick = onAlbumsClick,
                activeSurface = activeSurface,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            ) { color ->
                Icon(Icons.Outlined.FilterNone, contentDescription = "Albums", tint = if (selectedTab == "albums") Coral else color, modifier = Modifier.size(24.dp))
            }

            NavItem(
                selected = selectedTab == "search",
                onClick = onSearchClick,
                activeSurface = activeSurface,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            ) { color ->
                Icon(Icons.Outlined.Search, contentDescription = "Search", tint = if (selectedTab == "search") Coral else color, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    activeSurface: Color,
    activeColor: Color,
    inactiveColor: Color,
    content: @Composable (Color) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) activeSurface else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            content(if (selected) activeColor else inactiveColor)
        }
    }
}