package pl.krzyssko.portfoliobrowser.android.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.di.NAMED_DETAILS
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.State
import pl.krzyssko.portfoliobrowser.store.loadFrom
import pl.krzyssko.portfoliobrowser.store.project

class ProjectDetailsViewModel(private val repository: ProjectRepository, ): ViewModel(), KoinComponent {

    private val store: OrbitStore<State.ProjectState> by inject(NAMED_DETAILS) { parametersOf(viewModelScope, State.ProjectState()) }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    fun loadProjectWith(name: String) {
        store.project {
            loadFrom(repository, name)
        }
    }
}