package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.business.ProjectDetails
import pl.krzyssko.portfoliobrowser.business.ProjectsList
import pl.krzyssko.portfoliobrowser.business.toDetails
import pl.krzyssko.portfoliobrowser.business.toSearchPhrase
import pl.krzyssko.portfoliobrowser.data.FilterOptions
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.SearchRepository
import pl.krzyssko.portfoliobrowser.store.ProjectsQueryState
import pl.krzyssko.portfoliobrowser.store.StackColorMap

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val searchRepository: SearchRepository,
    private val logging: Logging
) : ViewModel(), KoinComponent {

    companion object {
        val TAG = ProjectViewModel::class.java.simpleName.toString()
        const val COLORS_STATE_KEY = "app.state.colors"
    }

    private val colorPicker: InfiniteColorPicker by inject {
        parametersOf(savedStateHandle.get<StackColorMap>(COLORS_STATE_KEY))
    }

    val list: ProjectsList by inject {
        parametersOf(viewModelScope)
    }
    val details: StateFlow<ProjectDetails?> by lazy {
        list.stateFlow
            .map { listState ->
                if (listState is ProjectsQueryState.DetailsSelected) {
                    val detail = get<ProjectDetails> {
                        parametersOf(
                            viewModelScope,
                            listState.projectId,
                            listState.createdBy
                        )
                    }
                    detail
                } else {
                    null
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    //val state: StateFlow<ProjectsQueryState>
    //    get() = list.stateFlow
    //val sideEffects: Flow<UserSideEffects>
    //    get() = list.sideEffectFlow

    val searchPhrase: Flow<String?>
        get() = list.stateFlow.toSearchPhrase()

    val detail: Flow<Project?>
        get() = details.flatMapLatest { it?.stateFlow?.toDetails() ?: flowOf(null) }

    private var pagingSource: MyPagingSource<Project>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = list.stateFlow
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
        list.reset()
    }

    fun search(searchFieldText: String) {
        list.filter(FilterOptions(query = searchFieldText))
        refreshProjectsList()
    }

    fun selectCategories(selectedCategories: List<String>) {
        list.filter(FilterOptions(categories = selectedCategories))
    }

    fun selectFeatured(favoritesSelected: Boolean) {
        list.filter(FilterOptions(featured = favoritesSelected))
    }

    fun select(project: Project) {
        list.select(project)
    }

    fun toggleFavorite(favorite: Boolean) {
        details.value?.followProject(favorite)
    }

    fun refreshProjectsList() {
        pagingSource?.invalidate()
    }
}
