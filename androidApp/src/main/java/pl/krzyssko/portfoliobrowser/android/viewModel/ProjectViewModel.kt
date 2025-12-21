package pl.krzyssko.portfoliobrowser.android.viewModel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.api.paging.PagedContentLoader
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_GITHUB
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.PagingState
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.PagedProjectsList
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.importProjects
import pl.krzyssko.portfoliobrowser.store.loadPageFrom
import pl.krzyssko.portfoliobrowser.store.projectsList
import pl.krzyssko.portfoliobrowser.store.resetProjectsList
import pl.krzyssko.portfoliobrowser.store.updateSearchPhrase
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.exceptionAsResponse

class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val firestore: Firestore,
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
    private val sourceRepository: ProjectRepository by inject(NAMED_GITHUB)

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    //val projectsState = stateFlow
    //    .map {
    //        //(it as? ProjectsListState.Loaded)?.projects?.values?.takeIf { projects -> projects.isNotEmpty() }
    //        //    ?.reduce { acc, projects -> acc.toMutableList() + projects }
    //        when (it) {
    //            is ProjectsListState.Loaded -> it.projects.values.flatten()
    //            else -> emptyList()
    //        }
    //    }
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchPhrase = stateFlow
        .map { (it as? ProjectsListState.Loaded)?.searchPhrase }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var pagingSource: MyPagingSource<Project>? = null
    val pagingFlow = Pager(PagingConfig(5)) {
        MyPagingSource(object : PagedContentLoader<Project> {
            override val pagingState: PagingState
                get() = repository.pagingState
            override suspend fun getContent(readFromStart: Boolean): Flow<Response<PagedProjectsList>> {
                getProjectsList(readFromStart)
                return projectsPagedListStateFlow
            }
        }).also {
            pagingSource = it
        }
    }.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            colorPicker.colorMapStateFlow.collectLatest {
                savedStateHandle[COLORS_STATE_KEY] = it
            }
        }

        // Background logic
        //with (store) {
        //    viewModelScope.launch {
        //        stateFlow.collect {
        //            when (it) {
        //                is ProjectsListState.Error -> resetProjects()
        //                else -> return@collect
        //            }
        //        }
        //    }
        //}
    }

    private val rawProjectListStateFlow: StateFlow<PagedProjectsList> = stateFlow
        .map {
            when (it) {
                is ProjectsListState.Loaded -> it.projects
                is ProjectsListState.Error -> throw Exception(it.reason)
                else -> null
            }
        }
        .filterNotNull()
        .catch {
            resetProjects()
            throw it
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val projectsPagedListStateFlow: StateFlow<Response<PagedProjectsList>> = rawProjectListStateFlow
        .map { Response.Ok(it) }
        .filterNotNull()
        .exceptionAsResponse()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Response.Ok(emptyMap()))

    val projectsFlattenListStateFlow: StateFlow<List<Project>> = projectsPagedListStateFlow
        .map {
            when (it) {
                is Response.Ok -> it.data.values.flatten()
                is Response.Error -> emptyList()
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val errorFlow: StateFlow<Throwable?> = stateFlow
        .map {
            when (it) {
                is ProjectsListState.Error -> it.reason
                else -> null
            }
        }
        //.onEach {
        //    it?.let {
        //        resetProjects()
        //    }
        //}
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    //val importProjectsStateFlow: StateFlow<Response<Unit>> = stateFlow
    //    .map {
    //        when (it) {
    //            is ProjectsListState.ImportCompleted -> Response.Ok(Unit)
    //            is ProjectsListState.ImportError -> throw Exception(it.error)
    //            else -> null
    //        }
    //    }
    //    .filterNotNull()
    //    .catch {
    //        resetProjects()
    //        throw it
    //    }
    //    .errorAsResponse()
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Response.Ok(Unit))

    fun getProjectsList(resetPaging: Boolean) {
        if (resetPaging) {
            repository.resetPagingState()
        }
        store.projectsList {
            loadPageFrom(repository)
        }
    }

    fun updateSearchPhrase(searchFieldText: String) {
        store.projectsList {
            updateSearchPhrase(searchFieldText)
        }
    }

    fun importProjectsFor(user: User.Authenticated) {
        store.projectsList {
            importProjects(sourceRepository, firestore, user)
        }
    }

    //suspend fun importProjectsFlow(user: StateFlow<User>): StateFlow<Response<Boolean>> {
    //    return stateFlow
    //        .onSubscription {
    //            store.projectsList {
    //                importProjects(sourceRepository, firestore, user)
    //            }
    //        }
    //        .map {
    //            when (it) {
    //                is ProjectsListState.ImportCompleted -> Response.Ok(true)
    //                is ProjectsListState.ImportError -> throw Error(it.error)
    //                else -> null
    //            }
    //        }
    //        .filterNotNull()
    //        .catch {
    //            resetProjects()
    //            throw it
    //        }
    //        .errorAsResponse()
    //        .stateIn(viewModelScope)
    //}

    fun resetProjects() {
        Log.d(TAG, "resetProjects: reset list")
        store.projectsList {
            resetProjectsList()
        }
    }

    fun refreshProjectsList() {
        Log.d(TAG, "refreshProjectsList: refreshing list")
        pagingSource?.invalidate()
    }
}
