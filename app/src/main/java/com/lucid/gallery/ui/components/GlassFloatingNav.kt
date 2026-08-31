package com.lucid.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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

    val surfaceColor = if (darkTheme) Color(0xFF121212) else Paper
    val activeColor = if (darkTheme) TextPrimaryDark else Ink
    val inactiveColor = TextSecondary
    val activeSurface = if (darkTheme) DarkSurface else Color(0xFFE5E2D8)
    val borderColor = if (darkTheme) Color(0xFF2A2A2A) else Color(0xFFE0DDD2)

    val selectedIndex = when (selectedTab) {
        "photos" -> 0
        "albums" -> 1
        else -> 2
    }

    val indicatorOffset by animateDpAsState(
        targetValue = (selectedIndex * 52).dp,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 600f
        ),
        label = "nav_indicator"
    )

    Box(
        modifier = modifier
            .shadow(elevation = if (darkTheme) 8.dp else 12.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(32.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(32.dp))
            .padding(6.dp)
    ) {
        Box(modifier = Modifier.width(152.dp).height(48.dp)) {

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(activeSurface)
            )

            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    selected = selectedTab == "photos",
                    onClick = onPhotosClick,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor
                ) { color, scale ->
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = "Photos",
                        tint = if (selectedTab == "photos") Coral else color,
                        modifier = Modifier.size(24.dp).scale(scale)
                    )
                }

                NavItem(
                    selected = selectedTab == "albums",
                    onClick = onAlbumsClick,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor
                ) { color, scale ->
                    Icon(
                        Icons.Outlined.FilterNone,
                        contentDescription = "Albums",
                        tint = if (selectedTab == "albums") Coral else color,
                        modifier = Modifier.size(24.dp).scale(scale)
                    )
                }

                NavItem(
                    selected = selectedTab == "search",
                    onClick = onSearchClick,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor
                ) { color, scale ->
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = if (selectedTab == "search") Coral else color,
                        modifier = Modifier.size(26.dp).scale(scale)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    content: @Composable (tint: Color, scale: Float) -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "icon_color"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 600f
        ),
        label = "icon_scale"
    )

    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            content(animatedColor, animatedScale)
        }
    }
}