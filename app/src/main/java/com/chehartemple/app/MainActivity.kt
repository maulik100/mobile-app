package com.chehartemple.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.*
import com.chehartemple.app.data.api.ActivityTracker
import com.chehartemple.app.data.api.RetrofitClient
import com.chehartemple.app.data.api.TokenManager
import com.chehartemple.app.data.model.LogoutRequest
import com.chehartemple.app.ui.screens.*
import com.chehartemple.app.ui.theme.CheharTempleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestAppPermissions()
        setContent {
            CheharTempleTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }
                if (showSplash) {
                    SplashContent()
                } else {
                    AppRoot()
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@Composable
fun SplashContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_image),
            contentDescription = "Chehar Maa",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun AppRoot() {
    var authState by remember { mutableStateOf<AuthState>(
        // Immediately go to Home if tokens exist, no intermediate loading
        if (runBlocking { TokenManager.hasTokens() }) AuthState.LoggedIn else AuthState.LoggedOut
    ) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        RetrofitClient.onTokenExpired = {
            scope.launch {
                TokenManager.clearTokens()
                authState = AuthState.LoggedOut
            }
        }

        // If logged in, validate token in background
        if (authState == AuthState.LoggedIn) {
            val accessToken = TokenManager.getAccessToken()
            if (accessToken != null) {
                RetrofitClient.setToken(accessToken)
                try {
                    RetrofitClient.api.validateToken()
                } catch (e: Exception) {
                    if (!TokenManager.hasTokens()) {
                        authState = AuthState.LoggedOut
                    }
                }
            } else {
                TokenManager.clearTokens()
                authState = AuthState.LoggedOut
            }
        }
    }

    when (authState) {
        AuthState.LoggedOut -> {
            AuthScreen(
                onLoginSuccess = { accessToken, refreshToken, sessionToken ->
                    scope.launch {
                        TokenManager.saveTokens(accessToken, refreshToken, sessionToken)
                        authState = AuthState.LoggedIn
                    }
                },
                onSkip = { authState = AuthState.LoggedIn }
            )
        }
        AuthState.LoggedIn -> {
            MainScreen(onLogout = {
                scope.launch {
                    try {
                        val sessionToken = TokenManager.getSessionToken()
                        if (sessionToken != null) {
                            RetrofitClient.api.logout(LogoutRequest(sessionToken))
                        }
                    } catch (_: Exception) { }
                    TokenManager.clearTokens()
                    authState = AuthState.LoggedOut
                }
            })
        }
    }
}

sealed class AuthState {
    object LoggedIn : AuthState()
    object LoggedOut : AuthState()
}

sealed class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Events : BottomNavItem("events", "Events", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    object Gallery : BottomNavItem("gallery", "Reels", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow)
    object Timings : BottomNavItem("timings", "Timings", Icons.Filled.Info, Icons.Outlined.Info)
    object More : BottomNavItem("more", "More", Icons.Filled.MoreVert, Icons.Outlined.MoreVert)
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home, BottomNavItem.Events,
        BottomNavItem.Gallery, BottomNavItem.Timings, BottomNavItem.More
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isGallery = currentRoute == "gallery"

    Scaffold(
        containerColor = if (isGallery) Color.Black else MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = if (isGallery) Color.Black else Color.White,
                tonalElevation = 4.dp
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isGallery) Color.White else Color(0xFF800000),
                            selectedTextColor = if (isGallery) Color.White else Color(0xFF800000),
                            unselectedIconColor = if (isGallery) Color.Gray else Color(0xFF9B8B7B),
                            unselectedTextColor = if (isGallery) Color.Gray else Color(0xFF9B8B7B),
                            indicatorColor = if (isGallery) Color.DarkGray else Color(0xFFFFF8E8)
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", Modifier.padding(padding)) {
            composable("home") {
                LaunchedEffect(Unit) { ActivityTracker.trackScreen("Home") }
                HomeScreen()
            }
            composable("events") {
                LaunchedEffect(Unit) { ActivityTracker.trackScreen("Events") }
                EventsScreen()
            }
            composable("gallery") {
                LaunchedEffect(Unit) { ActivityTracker.trackScreen("Gallery") }
                GalleryScreen()
            }
            composable("timings") {
                LaunchedEffect(Unit) { ActivityTracker.trackScreen("Timings") }
                TimingsScreen()
            }
            composable("more") {
                LaunchedEffect(Unit) { ActivityTracker.trackScreen("More") }
                MoreScreen(onLogout = onLogout)
            }
        }
    }
}
