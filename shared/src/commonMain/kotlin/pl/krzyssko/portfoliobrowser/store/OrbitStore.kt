package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

open class OrbitStore<TState : Any>(
    coroutineScope: CoroutineScope,
    initialState: TState,
    onCreate: (suspend Syntax<TState, UserSideEffects>.() -> Unit)? = null
) : ContainerHost<TState, UserSideEffects>, KoinComponent {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState,
    )
    val stateFlow = container.stateFlow
    val sideEffectFlow = container.sideEffectFlow
}
