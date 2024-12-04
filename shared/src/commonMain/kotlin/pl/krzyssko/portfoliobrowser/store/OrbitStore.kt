package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

class OrbitContainerHost<TState : Any>(coroutineScope: CoroutineScope, initialState: TState) :
    ContainerHost<TState, UserSideEffects>, KoinComponent {
        val logging by inject<Logging>()
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
    logging.debug("container: loadPageFrom[${pageKey}]")
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
        logging.debug("container: loadPageFrom[${pageKey}] reduce projects size=${it[pageKey]?.size}")
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
    colorPicker: InfiniteColorPicker,
    projectName: String
) =
    intent {
        (state as? ProjectState.Ready)?.let {
            repository.fetchStack(projectName).map { entries ->
                entries.map { entry ->
                    Stack(
                        name = entry.key,
                        lines = entry.value,
                        color = colorPicker.pick(entry.key)
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

fun OrbitContainerHost<ProjectsListState>.loadStack(
    repository: ProjectRepository,
    colorPicker: InfiniteColorPicker,
    pageKey: String?
) =
    intent {
        var list: List<Project> = listOf()
        state.projects[pageKey]?.onEach { project ->
            repository.fetchStack(project.name).map { entries ->
                entries.map { entry ->
                    Stack(
                        name = entry.key,
                        lines = entry.value,
                        color = colorPicker.pick(entry.key)
                    )
                }
            }.onEach { stack ->
                list = list + project.copy(stack = stack)
            }.map {
                state.projects + (pageKey to list)
            }.collect {
                reduce {
                    state.copy(projects = it)
                }
            }
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
