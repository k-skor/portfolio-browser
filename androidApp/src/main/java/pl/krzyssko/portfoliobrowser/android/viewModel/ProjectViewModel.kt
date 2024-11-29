package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.di.NAMED_SHARED
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.State
import pl.krzyssko.portfoliobrowser.store.saveStackColors
import pl.krzyssko.portfoliobrowser.store.shared

class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository
) : ViewModel(), KoinComponent {

    companion object {
        const val SHARED_STATE_KEY = "app.state.shared"
    }

    private val logging by inject<Logging>()
    private val store: OrbitStore<State.ProjectsListState> by inject(NAMED_LIST) {
        parametersOf(
            viewModelScope,
            State.ProjectsListState()
        )
    }
    private val shared by inject<OrbitStore<State.SharedState>>(NAMED_SHARED) {
        parametersOf(savedStateHandle.get<StackColorMap>(SHARED_STATE_KEY)
            ?.let { State.SharedState(it) })
    }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    val sharedStateFlow = shared.stateFlow

    val pagingFlow = Pager(PagingConfig(5)) {
        MyPagingSource(repository, store)
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            stateFlow.map { it.projects.flatMap { page -> page.value.map { project -> project.stack } } }
                .filter { it.isNotEmpty() }
                .distinctUntilChanged().collect {
                    logging.debug("updating colors map")
                    shared.shared {
                        it.onEach {
                            saveStackColors(it)
                        }
                    }
                }
        }
    }
}
