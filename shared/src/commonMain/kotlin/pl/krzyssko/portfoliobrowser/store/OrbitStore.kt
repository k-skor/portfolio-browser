package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

class OrbitContainerHost<TState : Any>(coroutineScope: CoroutineScope, initialState: TState) :
    ContainerHost<TState, UserSideEffects> {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState
    )
}

fun OrbitContainerHost<ProjectState>.loadFrom(
    repository: ProjectRepository,
    projectName: String
) = intent {
    postSideEffect(UserSideEffects.Block)

    reduce {
        ProjectState.Loading
    }

    val response = repository.fetchProjectDetails(projectName)

    response.map { project ->
        Project(
            id = project.id,
            name = project.name,
            description = project.description,
            icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg")
        )
    }.collect {
        reduce {
            ProjectState.Ready(it)
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.ProjectDetails))
    }
}

/**
 * 1. Called from PagingSource
 * 2. Take repository
 * 3. Reduce to state
 * 4. Return State, Flow, etc.
 */
fun OrbitContainerHost<ProjectsListState>.loadPageFrom(
    repository: ProjectRepository,
    pageKey: String?
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading projects list ${pageKey ?: "initial"} page"))

    val response = repository.fetchProjects(pageKey)

    flowOf(response).map {
        response.data.map { project ->
            Project(
                id = project.id,
                name = project.name,
                description = project.description,
                icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg")
            )
        }
    }.map {
        state.projects + (pageKey to it)
    }.collect {
        reduce {
            state.copy(
                loading = false,
                projects = it,
                currentPageUrl = pageKey,
                nextPageUrl = response.next,
                isLastPage = response.next == null
            )
        }
    }
}

fun OrbitContainerHost<ProjectState>.loadStackForProject(
    repository: ProjectRepository,
    projectName: String
) =
    intent {
        (state as? ProjectState.Ready)?.let {
            repository.fetchStack(projectName).map { entries ->
                entries.map { entry ->
                    Stack(
                        name = entry.key,
                        lines = entry.value
                    )
                }
            }.map { stack ->
                it.project.copy(stack = stack)
            }.collect { project ->
                reduce {
                    it.copy(project = project)
                }
            }
        }
    }

fun OrbitContainerHost<ProjectsListState>.loadStackForProjects(
    repository: ProjectRepository,
    projectName: String
) =
    intent {
        repository.fetchStack(projectName).map { entries ->
            entries.map { entry ->
                Stack(
                    name = entry.key,
                    lines = entry.value
                )
            }
        }.map { stack ->
            state.projects.mapValues {
                it.value.map { project ->
                    if (project.name == projectName) project.copy(stack = stack) else project
                }
            }
        }.collect {
            reduce {
                state.copy(loading = false, projects = it)
            }
        }
    }

fun OrbitContainerHost<SharedState>.saveStackColors(stack: List<Stack>) = intent {
    reduce {
        state.copy(stackColorMap = InfiniteColorPicker(state.stackColorMap).also { picker ->
            stack.onEach {
                picker.pick(it.name)
            }
        }.colorMap)
    }
}

class OrbitStore<TState : Any>(coroutineScope: CoroutineScope, initialState: TState) {
    val containerHost = OrbitContainerHost(coroutineScope, initialState)

    val stateFlow = containerHost.container.stateFlow
    val sideEffectFlow = containerHost.container.sideEffectFlow
}

fun OrbitStore<ProjectState>.project(block: OrbitContainerHost<ProjectState>.() -> Unit) {
    containerHost.apply(block)
}

fun OrbitStore<ProjectsListState>.projectsList(block: OrbitContainerHost<ProjectsListState>.() -> Unit) {
    containerHost.apply(block)
}

fun OrbitStore<SharedState>.shared(block: OrbitContainerHost<SharedState>.() -> Unit) {
    containerHost.apply(block)
}
