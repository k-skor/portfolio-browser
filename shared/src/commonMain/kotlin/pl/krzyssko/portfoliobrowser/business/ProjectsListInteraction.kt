package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.data.FilterOptions
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsListState

class ProjectsListInteraction(
    coroutineScope: CoroutineScope
) : KoinComponent, OrbitStore<ProjectsListState>(coroutineScope, ProjectsListState.Initialized) {

    fun updateFilters(filterOptions: FilterOptions) = intent {
        reduce {
            ProjectsListState.FilterSelected(filterOptions)
        }
    }

    fun clearFilters() = intent {
        reduce {
            ProjectsListState.FilterSelected(FilterOptions())
        }
    }
}
