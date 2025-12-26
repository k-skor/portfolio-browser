package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.navigation.Route

sealed class UserSideEffects {
    class Toast(val message: String): UserSideEffects()
    class Trace(val message: String) : UserSideEffects()
    class NavigateTo(val route: Route, val popFromBackStack: Boolean = false) : UserSideEffects()
    class SyncSnack(val source: Source, val message: String = "Import projects from $source?") : UserSideEffects()
    open class Error(val throwable: Throwable): UserSideEffects()
    class ErrorAccountExists(throwable: Throwable) : Error(kotlin.Error("Seems you already have an account, please log in instead.", throwable))
}