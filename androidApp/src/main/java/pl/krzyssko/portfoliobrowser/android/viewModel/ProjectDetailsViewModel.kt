package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel.Companion.COLORS_STATE_KEY
import pl.krzyssko.portfoliobrowser.di.NAMED_DETAILS
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.store.loadFrom
import pl.krzyssko.portfoliobrowser.store.loadStackForProject
import pl.krzyssko.portfoliobrowser.store.project

class ProjectDetailsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProjectRepository,
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

    fun loadProjectWith(name: String) {
        store.project {
            viewModelScope.launch {
                loadFrom(repository, name).join()
                loadStackForProject(repository, colorPicker, name)
            }
        }
    }
}