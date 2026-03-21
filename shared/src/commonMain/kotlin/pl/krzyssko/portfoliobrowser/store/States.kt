package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.FilterOptions
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
    data class FilterSelected(val options: FilterOptions) : ProjectsListState()
}

sealed class LoginState {
    data object Initialized : LoginState()
    data class Authenticated(
        val user: User,
        val linkedProviders: List<Provider>? = emptyList()
    ) : LoginState()
    data class Error(val reason: Throwable?): LoginState()
    //data class ProfileCreated(val profile: Profile): LoginState()
}

sealed class ProfileState {
    data object Initialized : ProfileState()
    data class Error(val reason: Throwable?): ProfileState()
    data class Completed(val profile: Profile.Created): ProfileState()
}

sealed class UserOnboardingProfileState {
    data object NotCreated : UserOnboardingProfileState()
    data object FirstTimeCreation : UserOnboardingProfileState()
    data object AlreadyCreated: UserOnboardingProfileState()
    data class Error(val reason: Throwable?): UserOnboardingProfileState()
    data class Completed(val userName: String): UserOnboardingProfileState()
}

sealed class AccountMergeState {
    data object Idle : AccountMergeState()
    data object InProgress : AccountMergeState()
    data class Success(val user: User.Authenticated) : AccountMergeState()
    data class Error(val reason: Throwable?) : AccountMergeState()
}

sealed class UserOnboardingImportState {
    data object Initialized: UserOnboardingImportState()
    data object SourceAvailable: UserOnboardingImportState()
    data object Confirmed: UserOnboardingImportState()
    data object Started: UserOnboardingImportState()
    data class InProgress(val progress: Int, val total: Int, val displayName: String?): UserOnboardingImportState()
    data class Error(val reason: Throwable?): UserOnboardingImportState()
    data object Completed: UserOnboardingImportState()
}