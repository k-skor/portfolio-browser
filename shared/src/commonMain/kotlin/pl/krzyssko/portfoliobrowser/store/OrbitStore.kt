package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

class OrbitContainerHost<TState : State>(coroutineScope: CoroutineScope, initialState: TState) :
    ContainerHost<TState, UserSideEffects> {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState
    )
}

fun OrbitContainerHost<State.ProjectState>.loadFrom(
    repository: ProjectRepository,
    projectName: String
) = intent {
    postSideEffect(UserSideEffects.Block)

    reduce {
        state.copy(loading = true)
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
            state.copy(loading = false, project = it)
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
fun OrbitContainerHost<State.ProjectsListState>.loadPageFrom(
    repository: ProjectRepository,
    pageKey: String?
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading projects list ${pageKey ?: "initial"} page"))

    reduce {
        state.copy(loading = true)
    }

    val colorPicker = InfiniteColorPicker(state.stackColorMap)

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
                isLastPage = response.next == null,
                stackColorMap = colorPicker.colorMap
            )
        }
    }
}

fun OrbitContainerHost<State.ProjectsListState>.loadStackForProject(
    repository: ProjectRepository,
    projectName: String
) =
    intent {
        reduce {
            state.copy(loading = true)
        }
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

fun OrbitContainerHost<State.SharedState>.saveStackColors(stack: List<Stack>) = intent {
    reduce {
        state.copy(stackColorMap = InfiniteColorPicker(state.stackColorMap).also { picker ->
            stack.onEach {
                picker.pick(it.name)
            }
        }.colorMap)
    }
}

class OrbitStore<TState : State>(coroutineScope: CoroutineScope, initialState: TState) {
    val containerHost = OrbitContainerHost(coroutineScope, initialState)

    val stateFlow = containerHost.container.stateFlow
    val sideEffectFlow = containerHost.container.sideEffectFlow
}

fun OrbitStore<State.ProjectState>.project(block: OrbitContainerHost<State.ProjectState>.() -> Unit) {
    containerHost.apply(block)
}

fun OrbitStore<State.ProjectsListState>.projectsList(block: OrbitContainerHost<State.ProjectsListState>.() -> Unit) {
    containerHost.apply(block)
}

fun OrbitStore<State.SharedState>.shared(block: OrbitContainerHost<State.SharedState>.() -> Unit) {
    containerHost.apply(block)
}
