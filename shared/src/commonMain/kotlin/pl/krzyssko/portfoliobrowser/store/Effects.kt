package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.navigation.Route

sealed class UserSideEffects {
    class Toast(val message: String): UserSideEffects()
    class Trace(val message: String) : UserSideEffects()
    class NavigateTo(val route: Route) : UserSideEffects()
    class SyncSnack(val source: Source, val message: String = "Import projects from $source?") : UserSideEffects()
    class LinkSnack(val message: String = "Seems you already have an account but linking failed, sign in to that account instead?") : UserSideEffects()
    class Error(val throwable: Throwable): UserSideEffects()
}