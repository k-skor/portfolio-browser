package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
import pl.krzyssko.portfoliobrowser.business.ProjectsList
import pl.krzyssko.portfoliobrowser.data.FilterOptions
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.SearchRepository
import pl.krzyssko.portfoliobrowser.store.ProjectsQueryState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class ProjectsListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val searchRepository: SearchRepository,
    private val logging: Logging
) : ViewModel(), KoinComponent {

    companion object {
        val TAG = ProjectsListViewModel::class.java.simpleName.toString()
        const val COLORS_STATE_KEY = "app.state.colors"
    }

    private val colorPicker: InfiniteColorPicker by inject {
        parametersOf(savedStateHandle.get<StackColorMap>(COLORS_STATE_KEY))
    }

    private val query: ProjectsList by inject {
        parametersOf(viewModelScope)
    }

    val state: StateFlow<ProjectsQueryState>
        get() = query.stateFlow
    val sideEffects: Flow<UserSideEffects>
        get() = query.sideEffectFlow

    val searchPhrase: Flow<String?>
        get() = query.searchPhrase

    private var pagingSource: MyPagingSource<Project>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = query.stateFlow
        .map { it as? ProjectsQueryState.FilterSelected }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { params ->
            Pager(
                config = PagingConfig(pageSize = 5),
                pagingSourceFactory = {
                    MyPagingSource<Project>(
                        repository = repository,
                        searchRepository = searchRepository,
                        query = params.options.query,
                        categories = params.options.categories,
                        featured = params.options.featured
                    ).also { pagingSource = it }
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
        query.reset()
    }

    fun search(searchFieldText: String) {
        query.filter(FilterOptions(query = searchFieldText))
        refreshProjectsList()
    }

    fun selectCategories(selectedCategories: List<String>) {
        query.filter(FilterOptions(categories = selectedCategories))
    }

    fun selectFeatured(favoritesSelected: Boolean) {
        query.filter(FilterOptions(featured = favoritesSelected))
    }

    fun refreshProjectsList() {
        pagingSource?.invalidate()
    }
}
