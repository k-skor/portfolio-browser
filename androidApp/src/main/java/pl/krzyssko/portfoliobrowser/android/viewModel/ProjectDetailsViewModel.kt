package pl.krzyssko.portfoliobrowser.android.viewModel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel.Companion.COLORS_STATE_KEY
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_DETAILS
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.followProject
import pl.krzyssko.portfoliobrowser.store.loadFrom
import pl.krzyssko.portfoliobrowser.store.project
import pl.krzyssko.portfoliobrowser.store.updateProject
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.exceptionAsResponse
import pl.krzyssko.portfoliobrowser.util.getOrThrow

class ProjectDetailsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val firestore: Firestore,
    private val auth: Auth
) : ViewModel(), KoinComponent {

    private val store: OrbitStore<ProjectState> by inject(NAMED_DETAILS) {
        parametersOf(
            viewModelScope,
            ProjectState.Loading
        )
    }
    private val colorPicker: InfiniteColorPicker by inject {
        parametersOf(savedStateHandle.get<StackColorMap>(COLORS_STATE_KEY))
    }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    val projectDetailsState: StateFlow<Response<Project>> = stateFlow
        .map {
            when (it) {
                is ProjectState.Loading -> Response.Pending
                is ProjectState.Loaded -> Response.Ok(it.project)
                is ProjectState.Error -> Response.Error(it.reason)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Response.Pending)

    val projectDetailsFlow: Flow<Project> = projectDetailsState
        .map { it.getOrThrow() }
        .filterNotNull()

    val errorFlow: Flow<Throwable?> = stateFlow
        .map {
            when (it) {
                is ProjectState.Error -> throw it.reason ?: Error("Unknown error.")
                else -> null
            }
        }

    fun getProjectDetails(project: Project) {
        store.project {
            loadFrom(repository, colorPicker, project.createdBy, project.id)
        }
    }

    fun toggleFavorite(favorite: Boolean) {
        store.project {
            followProject(firestore, auth, favorite)
        }
    }

    companion object {
        const val TAG = "ProjectDetailsViewModel"
    }
}