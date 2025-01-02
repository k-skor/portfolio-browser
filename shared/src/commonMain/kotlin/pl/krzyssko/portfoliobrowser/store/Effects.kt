package pl.krzyssko.portfoliobrowser.store

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable
    data object ProjectsList : Route()
    @Serializable
    data object ProjectDetails : Route()
    @Serializable
    data object Account : Route()
}

sealed class UserSideEffects {
    class Toast(val message: String): UserSideEffects()
    class Trace(val message: String) : UserSideEffects()
    class NavigateTo(val route: Route) : UserSideEffects()
    class SyncSnack(val message: String) : UserSideEffects()
}