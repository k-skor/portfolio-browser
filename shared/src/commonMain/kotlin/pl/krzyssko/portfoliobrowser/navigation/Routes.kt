package pl.krzyssko.portfoliobrowser.navigation

import kotlinx.serialization.Serializable


enum class ViewType {
    Login,
    Register,
    SourceSelection
}

sealed class Route {
    @Serializable
    data object Welcome: Route()
    @Serializable
    data object LoginRoot: Route()
    @Serializable
    data class Login(val viewType: ViewType): Route()
    @Serializable
    data object HomeRoot: Route()
    @Serializable
    data object HomeScaffold: Route()
    sealed class HomeRoute: Route()
    @Serializable
    data object Home: HomeRoute() {
    }
    @Serializable
    data object List: HomeRoute()
    @Serializable
    data class Details(val userId: String, val projectId: String): HomeRoute()
    @Serializable
    data object Profile: HomeRoute()
    @Serializable
    data object Favorites: HomeRoute()
    @Serializable
    data object Settings: HomeRoute()
    @Serializable
    data class Error(val title: String, val message: String): Route()
    @Serializable
    data object AccountsMerge: Route()
    @Serializable
    data object Onboarding: Route()
    @Serializable
    data object PrepareProfile: Route()
    @Serializable
    data object ProviderImport: Route()
    @Serializable
    data object ProviderImportOngoing: Route()
}

fun Throwable?.toRoute(): Route.Error =
    Route.Error(this?.message ?: "Unknown", this?.stackTraceToString().orEmpty())
