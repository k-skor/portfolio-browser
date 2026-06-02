package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

typealias OrbitStoreSyntax<TState> = suspend Syntax<TState, UserSideEffects>.() -> Unit

abstract class OrbitStore<TState : Any>(
    private val initialState: TState
) : ContainerHost<TState, UserSideEffects>, KoinComponent {

    override lateinit var container: Container<TState, UserSideEffects>

    private fun createContainer(config: StoreConfig<TState>) {
        container = config.coroutineScope.container(
            initialState = initialState,
            onCreate = config.onCreateBlock
        )
        config.coroutineScope.launch {
            config.onInitBlock?.invoke()
        }
    }

    val stateFlow: StateFlow<TState>
        get() = container.stateFlow
    val sideEffectsFlow: Flow<UserSideEffects>
        get() = container.sideEffectFlow

    protected fun storeConfig(scope: CoroutineScope, block: StoreConfig<TState>.() -> Unit = {}) {
        val config = StoreConfig<TState>(scope).apply(block)
        createContainer(config)
    }
}

class StoreConfig<TState : Any>(val coroutineScope: CoroutineScope) {
    var onCreateBlock: OrbitStoreSyntax<TState>? = null
    var onInitBlock: (suspend () -> Unit)? = null
}

fun <TState : Any> StoreConfig<TState>.onCreate(block: OrbitStoreSyntax<TState>) {
    this.onCreateBlock = block
}

fun <TState : Any> StoreConfig<TState>.onInitialize(block: OrbitStoreSyntax<TState>) {
    this.onCreateBlock = block
}
