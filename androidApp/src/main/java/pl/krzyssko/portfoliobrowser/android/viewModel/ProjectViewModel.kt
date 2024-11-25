package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.State

class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository
) : ViewModel(), KoinComponent {

    private val store: OrbitStore<State.ProjectsListState> by inject(NAMED_LIST) {
        parametersOf(
            viewModelScope,
            State.ProjectsListState()
        )
    }

    val stateFlow = store.stateFlow
    val sideEffectFlow = store.sideEffectFlow

    val pagingFlow = Pager(PagingConfig(5)) {
        MyPagingSource(repository, store)
    }.flow.cachedIn(viewModelScope)
}
