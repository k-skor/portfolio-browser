package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.repository.Paging

typealias StackColorMap = Map<String, Int>
typealias PagedProjectsList = Map<String?, List<Project>>

class UserFetchException(reason: Throwable?) : Exception("Failed to fetch user.", reason)
class UserMissingException() : Exception("User expected but got null.")

sealed class ProjectState {
    data object Loading: ProjectState()
    data class Error(val reason: Throwable?): ProjectState()
    data class Loaded(val project: Project): ProjectState()
}

sealed class ProjectsListState {
    data object Initialized: ProjectsListState()
    data class Error(val reason: Throwable?): ProjectsListState()
    data class Loaded(
        val loading: Boolean = false,
        val projects: PagedProjectsList = emptyMap(),
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
        val linkedProviders: List<Provider>? = emptyList(),
        val hasProfile: Boolean = false
    ) : ProfileState()
    data class Error(val reason: Throwable?): ProfileState()
    //data class UserFetchError(val reason: Throwable?): ProfileState()
    //data object UserError: ProfileState()
    data class ProfileCreated(val profile: Profile): ProfileState()
    data object SourceAvailable: ProfileState()
    data object SourceImportAttempted: ProfileState()
}

fun ProjectsListState.isStateReady() =
    this is ProjectsListState.Initialized || this is ProjectsListState.Loaded || this is ProjectsListState.ImportCompleted

fun ProfileState.isLoggedIn() = this !is ProfileState.Created && this !is ProfileState.Initialized

fun ProfileState.isNotLoggedIn() =
    this is ProfileState.Initialized || (this is ProfileState.Authenticated && this.user !is User.Authenticated)