package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.clearFilters
import pl.krzyssko.portfoliobrowser.store.updateOnlyFeatured
import pl.krzyssko.portfoliobrowser.store.updateSearchPhrase
import pl.krzyssko.portfoliobrowser.store.updateSelectedCategories

class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val logging: Logging
) : ViewModel(), KoinComponent {

    companion object {
        const val TAG = "ProjectViewModel"
        const val COLORS_STATE_KEY = "app.state.colors"
    }

    private val colorPicker: InfiniteColorPicker by inject {
        parametersOf(savedStateHandle.get<StackColorMap>(COLORS_STATE_KEY))
    }
    private val store: OrbitStore<ProjectsListState> by inject(NAMED_LIST) {
        parametersOf(
            viewModelScope,
            ProjectsListState.Initialized
        )
    }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    val searchPhrase = stateFlow
        .map { (it as? ProjectsListState.FilterRequested)?.searchPhrase }
        .filterNotNull()

    private var pagingSource: MyPagingSource<Project>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = store.stateFlow
        .map { it as? ProjectsListState.FilterRequested }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { params ->
            Pager(
                config = PagingConfig(pageSize = 5),
                pagingSourceFactory = {
                    MyPagingSource<Project>(
                        repository = repository,
                        query = params.searchPhrase,
                        stack = params.selectedCategories.firstOrNull(),
                        featured = params.onlyFeatured
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            colorPicker.colorMapStateFlow.collectLatest {
                savedStateHandle[COLORS_STATE_KEY] = it
            }
        }
    }

    fun clearFilters() {
        store.clearFilters()
    }

    fun updateSearchPhrase(searchFieldText: String) {
        store.updateSearchPhrase(searchFieldText)
    }

    fun updateSelectedCategories(selectedCategories: List<String>) {
        store.updateSelectedCategories(selectedCategories)
    }

    fun updateOnlyFeatured(favoritesSelected: Boolean) {
        store.updateOnlyFeatured(favoritesSelected)
    }

    fun refreshProjectsList() {
        pagingSource?.invalidate()
    }
}
