package com.lucid.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lucid.gallery.ui.theme.Coral

@Composable
fun FloatingNav(
    modifier: Modifier = Modifier,
    selectedTab: String = "photos",
    isExpanded: Boolean = true,
    onPhotosClick: () -> Unit = {},
    onAlbumsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val activeColor = Color.White
    val inactiveColor = Color(0xFF888888)

    val selectedIndex = when (selectedTab) {
        "photos" -> 0
        "albums" -> 1
        else -> 2
    }

    val indicatorOffset by animateDpAsState(
        targetValue = (selectedIndex * 62).dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "nav_indicator"
    )

    val containerScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.75f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "nav_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF141414))
            .padding(8.dp)
    ) {
        Box(modifier = Modifier.width(180.dp).height(56.dp)) {

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF282828))
            )

            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        modifier = Modifier.size(28.dp).scale(scale)
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
                        modifier = Modifier.size(28.dp).scale(scale)
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
                        modifier = Modifier.size(30.dp).scale(scale)
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
    val animatedColor by animateColorAsState(targetValue = if (selected) activeColor else inactiveColor, label = "icon_color")
    val animatedScale by animateFloatAsState(targetValue = if (selected) 1.15f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f), label = "icon_scale")

    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) { content(animatedColor, animatedScale) }
    }
}