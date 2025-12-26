package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
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
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_GITHUB
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.store.clearProjects
import pl.krzyssko.portfoliobrowser.store.importProjects
import pl.krzyssko.portfoliobrowser.store.projectsList
import pl.krzyssko.portfoliobrowser.store.updateSearchPhrase

class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val firestore: Firestore,
    private val logging: Logging
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
            ProjectsListState.Initialized
        )
    }
    private val sourceRepository: ProjectRepository by inject(NAMED_GITHUB)

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow.onEach {
        when {
            it is UserSideEffects.Error -> error.value = true
        }
    }

    val projectsState = stateFlow
        .map {
            (it as? ProjectsListState.Ready)?.projects?.values?.takeIf { projects -> projects.isNotEmpty() }
                ?.reduce { acc, projects -> acc.toMutableList() + projects }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchPhrase =
        stateFlow.map { (it as? ProjectsListState.Ready)?.searchPhrase }.filterNotNull()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val error = MutableStateFlow(false)
    val errorState: StateFlow<Boolean> = error

    fun dismissError() {
        error.value = false
    }

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

    fun importProjects(user: StateFlow<User>) {
        store.projectsList {
            importProjects(sourceRepository, firestore, user)
        }
    }

    fun resetProjects() {
        store.projectsList {
            clearProjects()
        }
    }
}
