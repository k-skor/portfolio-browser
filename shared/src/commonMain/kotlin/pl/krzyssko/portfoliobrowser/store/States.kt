package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User

typealias StackColorMap = Map<String, Int>

sealed class ProjectState {
    data object Loading: ProjectState()
    data class Ready(val project: Project): ProjectState()
}

sealed class ProjectsListState {
    data object Initialized: ProjectsListState()
    data class Error(val error: Throwable): ProjectsListState()
    data class Ready(
        val loading: Boolean = false,
        val projects: Map<String?, List<Project>> = emptyMap(),
        val paging: Paging = Paging(),
        val stackFilter: List<String> = emptyList(),
        val searchPhrase: String? = null
    ): ProjectsListState()
    data object ImportStarted: ProjectsListState()
    data class ImportError(val error: Throwable): ProjectsListState()
    data object ImportCompleted: ProjectsListState()
}

sealed class ProfileState {
    data object Created : ProfileState()
    data object Initialized : ProfileState()
    data class Authenticated(
        val user: User,
        val linkedProviders: List<Provider>? = emptyList()
    ) : ProfileState()
    data class ProfileCreated(val profile: Profile): ProfileState()
    data object SourceAvailable: ProfileState()
}

fun ProjectsListState.isStateReady() =
    this is ProjectsListState.Initialized || this is ProjectsListState.Ready || this is ProjectsListState.ImportCompleted
