package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel.Companion.COLORS_STATE_KEY
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_DETAILS
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.loadFrom
import pl.krzyssko.portfoliobrowser.store.project
import pl.krzyssko.portfoliobrowser.store.updateProject

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

    var projectState =
        stateFlow.map { (it as? ProjectState.Ready)?.project }.filterNotNull()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun loadProjectDetails(project: Project) {
        store.project {
            loadFrom(repository, colorPicker, project.createdBy, project.id)
        }
    }

    fun toggleFavorite(favorite: Boolean, follower: Follower) {
        projectState.value?.let {
            viewModelScope.launch {
                if (favorite) {
                    firestore.followProject(follower.uid, it.id, follower)
                } else {
                    firestore.unfollowProject(follower.uid, it.id, follower)
                }
            }
            store.project {
                if (favorite) {
                    updateProject(it.copy(followers = it.followers + follower))
                } else {
                    updateProject(it.copy(followers = it.followers - follower))
                }
            }
        }
    }
}