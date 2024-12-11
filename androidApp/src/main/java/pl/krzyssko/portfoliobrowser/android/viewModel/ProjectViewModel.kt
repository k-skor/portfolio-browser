package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.cachedIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.paging.MyPagingSource
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getConfiguration
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.authenticateWithGitHub
import pl.krzyssko.portfoliobrowser.store.createUser
import pl.krzyssko.portfoliobrowser.store.initAuth
import pl.krzyssko.portfoliobrowser.store.projectsList
import pl.krzyssko.portfoliobrowser.store.reauthenticate
import pl.krzyssko.portfoliobrowser.store.resetAuth
import pl.krzyssko.portfoliobrowser.store.updateSearchPhrase

class ProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val auth: Auth,
    private val firestore: Firestore
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
            ProjectsListState.Idling
        )
    }
    private val config: Configuration by inject()
    private val logging: Logging by inject()

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
        viewModelScope.launch {
            stateFlow.filter { it is ProjectsListState.Authenticated }.collectLatest {
                val state = (it as ProjectsListState.Authenticated)
                logging.debug("AUTH user=${state.user.profile.id}, token=${state.user.token}")
                //config.config.gitHubApiUser = state.user.profile.id
                config.config.gitHubApiToken = state.user.token
                firestore.createUser(state.user.profile)
            }
        }
        // TODO: move to store initializer block
        initAuthentication()
    }

    fun updateSearchPhrase(searchFieldText: String) {
        store.projectsList {
            updateSearchPhrase(searchFieldText)
        }
    }

    fun initAuthentication() {
        store.projectsList {
            initAuth(auth)
        }
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false) {
        store.projectsList {
            if (refreshOnly) reauthenticate(activity, auth) else authenticateWithGitHub(activity, auth)
        }
    }

    fun resetAuthentication() {
        store.projectsList {
            resetAuth(auth)
        }
    }
}
