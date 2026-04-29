package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.api.AzureTokenProvider
import pl.krzyssko.portfoliobrowser.data.FilterOptions
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsQueryState

class ProjectsList(
    coroutineScope: CoroutineScope,
    private val azureAuth: AzureTokenProvider
) : KoinComponent, OrbitStore<ProjectsQueryState>(coroutineScope, ProjectsQueryState.Initialized) {

    val searchPhrase: Flow<String?>
        get() = stateFlow
            .map { (it as? ProjectsQueryState.FilterSelected)?.options?.query }

    init {
        coroutineScope.launch {
            azureAuth.initialize()
        }
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
