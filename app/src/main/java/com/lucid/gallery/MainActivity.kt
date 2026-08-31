package com.lucid.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import com.lucid.gallery.data.MediaItem
import com.lucid.gallery.ui.components.GlassFloatingNav
import com.lucid.gallery.ui.screens.AlbumsScreen
import com.lucid.gallery.ui.screens.PhotosScreen
import com.lucid.gallery.ui.screens.SearchScreen
import com.lucid.gallery.ui.screens.ViewerScreen
import com.lucid.gallery.ui.theme.LucidPhotosTheme

private val NavSpring = spring<IntOffset>(dampingRatio = 0.85f, stiffness = 450f)
private val AlphaSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 450f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageLoader = ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            LucidPhotosTheme {
                PermissionWrapper {
                    val navController = rememberNavController()
                    var syncedMediaId by remember { mutableStateOf<Long?>(null) }

                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = NavSpring) + fadeIn(AlphaSpring)
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = NavSpring) + fadeOut(AlphaSpring)
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = NavSpring) + fadeIn(AlphaSpring)
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { it }, animationSpec = NavSpring) + fadeOut(AlphaSpring)
                            }
                        ) {
                            composable("home") {
                                HomeTabs(
                                    syncedMediaId = syncedMediaId,
                                    onMediaClick = { media ->
                                        syncedMediaId = media.id
                                        navController.navigate("viewer/${media.bucketId}/${media.id}")
                                    }
                                )
                            }
                            composable(
                                route = "viewer/{bucketId}/{mediaId}",
                                arguments = listOf(
                                    navArgument("bucketId") { type = NavType.LongType },
                                    navArgument("mediaId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: 0L
                                val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L

                                ViewerScreen(
                                    bucketId = bucketId,
                                    initialMediaId = mediaId,
                                    onMediaChanged = { newMediaId -> syncedMediaId = newMediaId },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTabs(
    syncedMediaId: Long?,
    onMediaClick: (MediaItem) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val photosGridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize()) {
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(220),
            modifier = Modifier.fillMaxSize(),
            label = "tab_crossfade"
        ) { tab ->
            when (tab) {
                0 -> PhotosScreen(gridState = photosGridState, syncedMediaId = syncedMediaId, onMediaClick = onMediaClick)
                1 -> AlbumsScreen()
                2 -> SearchScreen()
            }
        }

        GlassFloatingNav(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            selectedTab = when (selectedTab) {
                0 -> "photos"
                1 -> "albums"
                else -> "search"
            },
            onPhotosClick = { selectedTab = 0 },
            onAlbumsClick = { selectedTab = 1 },
            onSearchClick = { selectedTab = 2 }
        )
    }
}

@Composable
fun PermissionWrapper(onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermission by remember {
        mutableStateOf(permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
        hasPermission = map.values.all { it }
    }

    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(permissions) }

    if (hasPermission) {
        onPermissionGranted()
    } else {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Permissions required", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}