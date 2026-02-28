package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsListState

class ProjectsListInteractions(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher
) : KoinComponent, OrbitStore<ProjectsListState>(coroutineScope, dispatcherIO, ProjectsListState.Initialized) {

    fun updateSearchPhrase(
        phrase: String
    ) = intent {
        reduce {
            ProjectsListState.FilterRequested(searchPhrase = phrase)
        }
    }

    fun updateSelectedCategories(
        categories: List<String>
    ) = intent {
        reduce {
            ProjectsListState.FilterRequested(selectedCategories = categories)
        }
    }

    fun updateOnlyFeatured(
        featured: Boolean
    ) = intent {
        reduce {
            ProjectsListState.FilterRequested(onlyFeatured = featured)
        }
    }

    fun clearFilters() = intent {
        reduce {
            ProjectsListState.FilterRequested()
        }
    }
}
