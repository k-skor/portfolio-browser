package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectsListViewModel.Companion.COLORS_STATE_KEY
import pl.krzyssko.portfoliobrowser.business.ProjectDetails
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class ProjectDetailsViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), KoinComponent {

    private val projectDetails: ProjectDetails by inject {
        parametersOf(
            viewModelScope
        )
    }
    private val colorPicker: InfiniteColorPicker by inject {
        parametersOf(savedStateHandle.get<StackColorMap>(COLORS_STATE_KEY))
    }

    val state: StateFlow<ProjectState>
        get() = projectDetails.stateFlow
    val sideEffects: Flow<UserSideEffects>
        get() = projectDetails.sideEffectFlow

    val details: Flow<Project>
        get() = projectDetails.details

    fun loadDetails(project: Project) {
        projectDetails.loadFrom(colorPicker, project.createdBy, project.id)
    }

    fun toggleFavorite(favorite: Boolean) {
        projectDetails.followProject(favorite)
    }

    companion object {
        const val TAG = "ProjectDetailsViewModel"
    }
}