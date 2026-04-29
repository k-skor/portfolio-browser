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
}

sealed class ProjectsQueryState {
    data object Initialized : ProjectsQueryState()
    data class FilterSelected(val options: FilterOptions) : ProjectsQueryState()
}

sealed class LoginState {
    data object Initialized : LoginState()
    data object LinkInProgress : LoginState()
    data class Authenticated(
        val user: User,
        val linkedProviders: List<Provider>? = emptyList()
    ) : LoginState()
    data class Error(val reason: Throwable?): LoginState()
}

sealed class ProfileState {
    data object Initialized : ProfileState()
    data class Error(val reason: Throwable?): ProfileState()
    data class Completed(val profile: Profile): ProfileState()
}

sealed class OnboardingState {
    data object Initialized : OnboardingState()
    data object ProfileNotCreated : OnboardingState()
    data object FirstTimeSignUp : OnboardingState()
    data object ProfileExists: OnboardingState()
    data class FirstTimeSignUpCompleted(val userName: String): OnboardingState()
    data object ImportSourceAvailable: OnboardingState()
    data object ImportConfirmed: OnboardingState()
    data object ImportStarted: OnboardingState()
    data class ImportProgress(val progress: Int, val total: Int, val displayName: String?): OnboardingState()
    data class Error(val reason: Throwable?): OnboardingState()
    data object OnboardingCompleted: OnboardingState()
}