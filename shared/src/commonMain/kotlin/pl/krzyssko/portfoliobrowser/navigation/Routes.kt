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
    data object Projects: Route()
    @Serializable
    data class Details(val userId: String, val projectId: String): Route()
    @Serializable
    data object Profile: Route()
    @Serializable
    data object Settings: Route()
}

