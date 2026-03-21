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
import pl.krzyssko.portfoliobrowser.business.ProjectsListInteraction
import pl.krzyssko.portfoliobrowser.data.FilterOptions
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.SearchRepository
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap

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
    private val interactions: ProjectsListInteraction by inject {
        parametersOf(viewModelScope)
    }

    val stateFlow = interactions.stateFlow
    val sideEffectsFlow = interactions.sideEffectFlow

    val searchPhrase = stateFlow
        .map { (it as? ProjectsListState.FilterSelected)?.options?.query }
        .filterNotNull()

    private var pagingSource: MyPagingSource<Project>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = interactions.stateFlow
        .map { it as? ProjectsListState.FilterSelected }
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
        interactions.clearFilters()
    }

    fun updateSearchPhrase(searchFieldText: String) {
        interactions.updateFilters(FilterOptions(query = searchFieldText))
    }

    fun updateSelectedCategories(selectedCategories: List<String>) {
        interactions.updateFilters(FilterOptions(categories = selectedCategories))
    }

    fun updateOnlyFeatured(favoritesSelected: Boolean) {
        interactions.updateFilters(FilterOptions(featured = favoritesSelected))
    }

    fun refreshProjectsList() {
        pagingSource?.invalidate()
    }
}
