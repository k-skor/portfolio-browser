package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

typealias OrbitStoreOnCreate<TState> = suspend Syntax<TState, UserSideEffects>.() -> Unit

abstract class OrbitStore<TState : Any>(
    coroutineScope: CoroutineScope,
    initialState: TState
) : ContainerHost<TState, UserSideEffects>, KoinComponent {

    private var onCreate: OrbitStoreOnCreate<TState>? = null

    override val container: Container<TState, UserSideEffects> by lazy {
        coroutineScope.container(
            initialState = initialState,
            onCreate = onCreate
        )
    }

    val stateFlow by lazy { container.stateFlow }
    val sideEffectFlow by lazy { container.sideEffectFlow }

    protected fun doOnCreate(block: OrbitStoreOnCreate<TState>) {
        onCreate = block
    }
}
