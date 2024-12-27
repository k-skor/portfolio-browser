package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.User

typealias StackColorMap = Map<String, Int>

sealed class ProjectState {
    data object Loading: ProjectState()
    data class Ready(val project: Project): ProjectState()
}

sealed class ProjectsListState {
    data object Loading: ProjectsListState()
    data class Ready(
        val loading: Boolean = false,
        val projects: Map<String?, List<Project>> = emptyMap(),
        val paging: Paging = Paging(),
        val stackFilter: List<String> = emptyList(),
        val searchPhrase: String? = null
    ): ProjectsListState()
}

sealed class ProfileState {
    data object Created: ProfileState()
    data object Initialized: ProfileState()
    data class Authenticated(val user: User): ProfileState()
}
