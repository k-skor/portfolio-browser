package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User

typealias StackColorMap = Map<String, Int>
typealias PagedProjectsList = Map<String?, List<Project>>

class UserFetchException(reason: Throwable?) : Exception("Failed to fetch user.", reason)

sealed class ProjectState {
    data object Loading: ProjectState()
    data class Error(val reason: Throwable?): ProjectState()
    data class Loaded(val project: Project): ProjectState()
    //data object Updated: ProjectState()
}

sealed class ProjectsListState {
    data object Initialized : ProjectsListState()
    data class FilterRequested(
        val searchPhrase: String = "",
        val selectedCategories: List<String> = emptyList(),
        val onlyFeatured: Boolean = false
    ) : ProjectsListState()
}

sealed class ProfileState {
    data object Initialized : ProfileState()
    data class Authenticated(
        val user: User,
        val linkedProviders: List<Provider>? = emptyList()
    ) : ProfileState()
    data class Error(val reason: Throwable?): ProfileState()
    data class ProfileCreated(val profile: Profile): ProfileState()
}

sealed class ProjectsImportState {
    data object Initialized: ProjectsImportState()
    data object SourceAvailable: ProjectsImportState()
    data object SourceImportAttempted: ProjectsImportState()
    data object ImportStarted: ProjectsImportState()
    data class ImportError(val reason: Throwable?): ProjectsImportState()
    data object ImportCompleted: ProjectsImportState()
}