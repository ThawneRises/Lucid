package com.lucid.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lucid.gallery.ui.theme.Coral
import kotlinx.coroutines.delay

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
    val inactiveColor = Color.White.copy(alpha = 0.45f)

    val selectedIndex = when (selectedTab) { "photos" -> 0; "albums" -> 1; else -> 2 }
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    var isSquishing by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != previousIndex) {
            isSquishing = true
            delay(120)
            isSquishing = false
            previousIndex = selectedIndex
        }
    }

    val indicatorOffset by animateDpAsState(
        targetValue = (selectedIndex * 60).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_offset"
    )

    val indicatorWidth by animateDpAsState(
        targetValue = if (isSquishing) 68.dp else 48.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "nav_width"
    )

    val containerScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "nav_scale"
    )

    val glassBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .shadow(elevation = 32.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF141414).copy(alpha = 0.65f))
            .border(1.dp, glassBorderBrush, RoundedCornerShape(32.dp))
            .padding(8.dp)
    ) {
        Box(modifier = Modifier.width(168.dp).height(48.dp)) {

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            )

            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    selected = selectedTab == "photos",
                    onClick = onPhotosClick,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor
                ) { color, scale ->
                    Icon(Icons.Outlined.Image, null, tint = if (selectedTab == "photos") Coral else color, modifier = Modifier.size(24.dp).scale(scale))
                }

                NavItem(
                    selected = selectedTab == "albums",
                    onClick = onAlbumsClick,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor
                ) { color, scale ->
                    Icon(Icons.Outlined.FilterNone, null, tint = if (selectedTab == "albums") Coral else color, modifier = Modifier.size(24.dp).scale(scale))
                }

                NavItem(
                    selected = selectedTab == "search",
                    onClick = onSearchClick,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor
                ) { color, scale ->
                    Icon(Icons.Outlined.Search, null, tint = if (selectedTab == "search") Coral else color, modifier = Modifier.size(26.dp).scale(scale))
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedColor by animateColorAsState(targetValue = if (selected) activeColor else inactiveColor, label = "icon_color")

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1200f),
        label = "icon_scale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content(animatedColor, animatedScale)
    }
}