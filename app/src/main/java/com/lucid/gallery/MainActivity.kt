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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.lucid.gallery.ui.screens.PhotosScreen
import com.lucid.gallery.ui.screens.ViewerScreen
import com.lucid.gallery.ui.theme.LucidPhotosTheme

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

                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        NavHost(
                            navController = navController,
                            startDestination = "photos",
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(300))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(300))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(300))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(300))
                            }
                        ) {
                            composable("photos") {
                                PhotosScreen(
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
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Permissions required", color = Color.White)
        }
    }
}