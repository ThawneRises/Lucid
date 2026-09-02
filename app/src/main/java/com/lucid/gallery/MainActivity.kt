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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.lucid.gallery.ui.components.FloatingNav
import com.lucid.gallery.ui.screens.AlbumViewScreen
import com.lucid.gallery.ui.screens.AlbumsScreen
import com.lucid.gallery.ui.screens.RECENTLY_DELETED_BUCKET_ID
import com.lucid.gallery.ui.screens.PhotosScreen
import com.lucid.gallery.ui.screens.SearchScreen
import com.lucid.gallery.ui.screens.SecretFolderScreen
import com.lucid.gallery.ui.screens.ViewerScreen
import com.lucid.gallery.ui.theme.LucidPhotosTheme

private val CalmEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private const val CALM_DURATION = 260

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
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(CALM_DURATION, easing = CalmEasing)
                                ) + fadeIn(tween(CALM_DURATION, easing = CalmEasing))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(CALM_DURATION, easing = CalmEasing)
                                ) + fadeOut(tween(CALM_DURATION, easing = CalmEasing))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(CALM_DURATION, easing = CalmEasing)
                                ) + fadeIn(tween(CALM_DURATION, easing = CalmEasing))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(CALM_DURATION, easing = CalmEasing)
                                ) + fadeOut(tween(CALM_DURATION, easing = CalmEasing))
                            }
                        ) {
                            composable("home") {
                                HomeTabs(
                                    syncedMediaId = syncedMediaId,
                                    onMediaClick = { media ->
                                        syncedMediaId = media.id
                                        navController.navigate("viewer/-1/${media.id}")
                                    },
                                    onAlbumClick = { bucketId, name ->
                                        navController.navigate("album/$bucketId/$name")
                                    },
                                    onRecentlyDeletedClick = {
                                        navController.navigate("album/$RECENTLY_DELETED_BUCKET_ID/Recently%20deleted")
                                    },
                                    onSecretFolderClick = {
                                        navController.navigate("secret")
                                    }
                                )
                            }
                            composable(
                                route = "album/{bucketId}/{albumName}",
                                arguments = listOf(
                                    navArgument("bucketId") { type = NavType.LongType },
                                    navArgument("albumName") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: 0L
                                val albumName = backStackEntry.arguments?.getString("albumName") ?: "Album"

                                AlbumViewScreen(
                                    bucketId = bucketId,
                                    albumName = albumName,
                                    onMediaClick = { media ->
                                        syncedMediaId = media.id
                                        val viewerBucketId = if (bucketId == RECENTLY_DELETED_BUCKET_ID) {
                                            RECENTLY_DELETED_BUCKET_ID
                                        } else {
                                            media.bucketId
                                        }
                                        navController.navigate("viewer/$viewerBucketId/${media.id}")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "viewer/{bucketId}/{mediaId}",
                                arguments = listOf(navArgument("bucketId") { type = NavType.LongType }, navArgument("mediaId") { type = NavType.LongType })
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
                            composable("secret") {
                                SecretFolderScreen(onBack = { navController.popBackStack() })
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
    onMediaClick: (MediaItem) -> Unit,
    onAlbumClick: (Long, String) -> Unit,
    onRecentlyDeletedClick: () -> Unit,
    onSecretFolderClick: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isNavExpanded by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) isNavExpanded = false
                else if (available.y > 15f) isNavExpanded = true
                return Offset.Zero
            }
        }
    }

    val photosGridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(160, easing = CalmEasing)).togetherWith(fadeOut(tween(100, easing = CalmEasing))) },
            modifier = Modifier.fillMaxSize(),
            label = "tab_transition"
        ) { tab ->
            when (tab) {
                0 -> PhotosScreen(gridState = photosGridState, syncedMediaId = syncedMediaId, onMediaClick = onMediaClick)
                1 -> AlbumsScreen(
                    onAlbumClick = onAlbumClick,
                    onRecentlyDeletedClick = onRecentlyDeletedClick,
                    onSecretFolderClick = onSecretFolderClick
                )
                2 -> SearchScreen(onMediaClick = onMediaClick)
            }
        }

        AnimatedVisibility(
            visible = isNavExpanded,
            enter = fadeIn(tween(180, easing = CalmEasing)) + slideInVertically(tween(180, easing = CalmEasing)) { it },
            exit = fadeOut(tween(140, easing = CalmEasing)) + slideOutVertically(tween(140, easing = CalmEasing)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingNav(
                modifier = Modifier.padding(bottom = 32.dp),
                selectedTab = when (selectedTab) { 0 -> "photos"; 1 -> "albums"; else -> "search" },
                isExpanded = true,
                onPhotosClick = { selectedTab = 0 },
                onAlbumsClick = { selectedTab = 1 },
                onSearchClick = { selectedTab = 2 }
            )
        }
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

    var hasPermission by remember { mutableStateOf(permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map -> hasPermission = map.values.all { it } }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(permissions) }

    if (hasPermission) onPermissionGranted()
    else Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { Text("Permissions required", color = MaterialTheme.colorScheme.onBackground) }
}