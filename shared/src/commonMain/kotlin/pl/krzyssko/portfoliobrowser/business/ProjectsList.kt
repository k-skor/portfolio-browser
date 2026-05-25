package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.api.AzureTokenProvider
import pl.krzyssko.portfoliobrowser.data.FilterOptions
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsQueryState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class ProjectsList(
    coroutineScope: CoroutineScope,
    private val azureAuth: AzureTokenProvider
) : KoinComponent, OrbitStore<ProjectsQueryState>(coroutineScope, ProjectsQueryState.Initialized) {

    init {
        coroutineScope.launch {
            azureAuth.initialize()
        }
    }

    fun select(project: Project) = intent {
        reduce {
            ProjectsQueryState.DetailsSelected(project.id, project.createdBy)
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.Details(project.createdBy, project.id)))
    }

    fun filter(filterOptions: FilterOptions) = intent {
        reduce {
            ProjectsQueryState.FilterSelected(filterOptions)
        }
    }

    fun reset() = intent {
        reduce {
            ProjectsQueryState.FilterSelected(FilterOptions())
        }
    }
}

fun StateFlow<ProjectsQueryState>.toSearchPhrase() =
    map { (it as? ProjectsQueryState.FilterSelected)?.options?.query }

fun StateFlow<ProjectsQueryState>.toSelected() =
    map { (it as? ProjectsQueryState.DetailsSelected) }
