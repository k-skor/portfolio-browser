package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.projectsList
import pl.krzyssko.portfoliobrowser.store.updateSearchPhrase

class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository
) : ViewModel(), KoinComponent {

    companion object {
        const val COLORS_STATE_KEY = "app.state.colors"
    }

    private val colorPicker: InfiniteColorPicker by inject {
        parametersOf(savedStateHandle.get<StackColorMap>(COLORS_STATE_KEY))
    }
    private val store: OrbitStore<ProjectsListState> by inject(NAMED_LIST) {
        parametersOf(
            viewModelScope,
            ProjectsListState()
        )
    }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    val pagingFlow = Pager(PagingConfig(5)) {
        MyPagingSource(repository, colorPicker, store)
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            colorPicker.colorMapStateFlow.collectLatest {
                savedStateHandle[COLORS_STATE_KEY] = it
            }
        }
    }

    fun updateSearchPhrase(searchFieldText: String) {
        store.projectsList {
            updateSearchPhrase(searchFieldText)
        }
    }
}
