package pl.krzyssko.portfoliobrowser.android.viewModel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.api.paging.PagedContentLoader
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.di.NAMED_GITHUB
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.PagingState
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.PagedProjectsList
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.UserIntent
import pl.krzyssko.portfoliobrowser.store.clearProjectsList
import pl.krzyssko.portfoliobrowser.store.loadPageFrom
import pl.krzyssko.portfoliobrowser.store.projectsList
import pl.krzyssko.portfoliobrowser.store.updateSearchPhrase
import pl.krzyssko.portfoliobrowser.util.Response

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
        .map { (it as? ProjectsListState.Loaded)?.searchPhrase }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var pagingSource: MyPagingSource<Project>? = null
    val pagingFlow = Pager(PagingConfig(5)) {
        MyPagingSource(object : PagedContentLoader<Project> {
            override val pagingState: PagingState
                get() = repository.pagingState
            override suspend fun getContent(readFromStart: Boolean): Flow<Response<PagedProjectsList>> = flow {
                if (readFromStart) {
                    repository.resetPagingState()
                }
                getProjectsList(userIntent)
                emitAll(projectsPagedListStateFlow)
            }
        }).also {
            pagingSource = it
        }
    }.flow.cachedIn(viewModelScope)

    var userIntent = UserIntent.Default
        set(value) {
            if (field != value) {
                field = value
                //resetProjects()
                refreshProjectsList()
            }
        }

    init {
        viewModelScope.launch {
            colorPicker.colorMapStateFlow.collectLatest {
                savedStateHandle[COLORS_STATE_KEY] = it
            }
        }
    }

    private val projectsPagedListStateFlow: Flow<Response<PagedProjectsList>> = stateFlow
        .shareIn(viewModelScope, SharingStarted.Eagerly)
        .map {
            when (it) {
                is ProjectsListState.Initialized -> Response.Pending
                is ProjectsListState.Loaded -> Response.Ok(it.projects)
                is ProjectsListState.Error -> Response.Error(it.reason)
            }
        }

    val projectsFlattenListStateFlow: StateFlow<List<Project>> = projectsPagedListStateFlow
        .map {
            when (it) {
                is Response.Ok -> it.data.values.flatten()
                is Response.Pending,
                is Response.Error -> emptyList()
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val errorFlow: Flow<Throwable?> = stateFlow
        .map {
            when (it) {
                is ProjectsListState.Error -> throw it.reason ?: Error("Unknown error.")
                else -> null
            }
        }

    fun getProjectsList(userIntent: UserIntent) {
        store.projectsList {
            loadPageFrom(repository, userIntent)
        }
    }

    fun updateSearchPhrase(searchFieldText: String) {
        store.projectsList {
            updateSearchPhrase(searchFieldText)
        }
    }

    fun clearProjects() {
        store.projectsList {
            clearProjectsList()
        }
    }

    fun refreshProjectsList() {
        pagingSource?.invalidate()
    }
}
