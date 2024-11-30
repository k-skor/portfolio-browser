package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Project

typealias StackColorMap = Map<String, Int>


data class SharedState(val stackColorMap: StackColorMap = emptyMap())

sealed class ProjectState {
    data object Loading: ProjectState()
    data class Ready(val project: Project): ProjectState()
}

data class ProjectsListState(
    val loading: Boolean = false,
    val projects: Map<String?, List<Project>> = emptyMap(),
    val currentPageUrl: String? = null,
    val nextPageUrl: String? = null,
    val isLastPage: Boolean = false,
    val stackFilter: List<String> = emptyList()
)