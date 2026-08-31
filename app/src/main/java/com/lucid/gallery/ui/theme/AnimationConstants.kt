package com.lucid.gallery.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AnimationConstants {
    fun <T> expressiveSpring() = spring<T>(
        dampingRatio = 0.6f,
        stiffness = 1000f
    )

    val EaseInOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    fun <T> expressiveTween(durationMillis: Int = 350) = tween<T>(
        durationMillis = durationMillis,
        easing = EaseInOutExpo
    )
}