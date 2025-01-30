package pl.krzyssko.portfoliobrowser.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import pl.krzyssko.portfoliobrowser.navigation.Route

data class TopLevelRoute(val name: String, val route: Any, val icon: ImageVector)

val topLevelRoutes = listOf(
    TopLevelRoute("Home", Route.Home, Icons.Default.Home),
    TopLevelRoute("Profile", Route.Profile, Icons.Default.AccountCircle),
    TopLevelRoute("Favorites", Route.Home, Icons.Default.Favorite),
    TopLevelRoute("Settings", Route.Settings, Icons.Default.Settings),
)
