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
    data class Login(val viewType: ViewType): Route()
    @Serializable
    data object Home: Route()
    @Serializable
    data object List: Route()
    @Serializable
    data class Details(val userId: String, val projectId: String): Route()
    @Serializable
    data object Profile: Route()
    @Serializable
    data object Favorites: Route()
    @Serializable
    data object Settings: Route()
    @Serializable
    data class Error(val title: String, val message: String): Route()
    @Serializable
    data object AccountsMerge: Route()
    @Serializable
    data object ProviderImport: Route()
}

fun Throwable?.toRoute(): Route.Error =
    Route.Error(this?.message ?: "Unknown", this?.stackTraceToString().orEmpty())

