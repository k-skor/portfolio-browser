package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Project

typealias StackColorMap = Map<String, Int>


sealed class State {
    data class ProjectState(val loading: Boolean = false, val project: Project? = null) :
        State()

    data class ProjectsListState(
        val loading: Boolean = false,
        val projectsPage: List<Project> = emptyList(),
        val nextPageUrl: String? = null,
        val isLastPage: Boolean = false,
        val stackFilter: List<String> = emptyList(),
        val stackColorMap: StackColorMap = emptyMap()
    ) : State()
}