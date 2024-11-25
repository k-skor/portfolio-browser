package pl.krzyssko.portfoliobrowser.store

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable
    data object ProjectsList : Route()
    @Serializable
    data object ProjectDetails : Route()
}

sealed class UserSideEffects {
    data object None: UserSideEffects()
    data object Block : UserSideEffects()
    class Toast(val message: String): UserSideEffects()
    class Trace(val message: String) : UserSideEffects()
    class NavigateTo(route: Route) : UserSideEffects()
    class Filter(val stackFilter: List<String>) : UserSideEffects()
}