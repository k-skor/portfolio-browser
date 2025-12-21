package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel.Companion.COLORS_STATE_KEY
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_DETAILS
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.loadFrom
import pl.krzyssko.portfoliobrowser.store.project
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.exceptionAsResponse

class ProjectDetailsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
    private val firestore: Firestore
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

    //var projectState = stateFlow
    //    .map {
    //        when (it) {
    //            is ProjectState.Loaded -> it.project
    //            else -> null
    //        }
    //    }
    //    .filterNotNull()
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun getProjectDetails(project: Project) {
        store.project {
            loadFrom(repository, colorPicker, project.createdBy, project.id)
        }
    }

    //fun getProjectDetailsFlow(project: Project): Flow<Response<Project>> {
    //    Log.d(TAG, "getProjectDetailsFlow: replay cache=${stateFlow.replayCache}")
    //    return stateFlow
    //        .onSubscription {
    //            Log.d(TAG, "getProjectDetailsFlow: on sub loading project=${project.name}")
    //            store.project {
    //                loadFrom(repository, colorPicker, project.createdBy, project.id)
    //            }
    //        }
    //        .onEach {
    //            Log.d(TAG, "getProjectDetailsFlow: on item=${it}")
    //        }
    //        .map {
    //            when (it) {
    //                is ProjectState.Loaded -> Response.Ok(it.project)
    //                is ProjectState.Error -> throw Error(it.reason)
    //                else -> null
    //            }
    //        }
    //        .filterNotNull()
    //        //.errorAsResponse()
    //}

    //val projectDetailsStateFlow: StateFlow<Response<Project>> = stateFlow
    //    .map {
    //        when (it) {
    //            is ProjectState.Loaded -> Response.Ok(it.project)
    //            is ProjectState.Error -> throw Exception(it.reason)
    //            else -> null
    //        } as? Response<Project>
    //    }
    //    .filterNotNull()
    //    .exceptionAsResponse()
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Response.Ok(Project()))

    val errorFlow: StateFlow<Throwable?> = stateFlow
        .map {
            when (it) {
                is ProjectState.Error -> it.reason
                else -> null
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    //fun getProjectDetails(project: Project): StateFlow<Response<Project>> {
    //    store.project {
    //        loadFrom(repository, colorPicker, project.createdBy, project.id)
    //    }
    //    return projectDetailsStateFlow
    //}

    companion object {
        const val TAG = "ProjectDetailsViewModel"
    }
}