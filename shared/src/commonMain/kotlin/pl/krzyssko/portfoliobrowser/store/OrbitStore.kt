package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

class OrbitContainerHost<TState: State>(coroutineScope: CoroutineScope, initialState: TState): ContainerHost<TState, UserSideEffects> {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState
    )
}

fun OrbitContainerHost<State.ProjectState>.loadFrom(repository: ProjectRepository, projectName: String) = intent {
    postSideEffect(UserSideEffects.Block)

    reduce {
        state.copy(loading = true)
    }

    val colorPicker = InfiniteColorPicker(emptyMap())

    repository.fetchProjectDetails(projectName).map { project ->
        val response2 = repository.fetchStack(project.name)

        response2.map { stack ->
            stack.map { entry -> Stack(entry.key, entry.value, colorPicker.pick(entry.key)) }
        }
            .map { stack2 ->
                Project(
                    project.id,
                    project.name,
                    project.description,
                    stack2,
                    Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg")
                )
            }.reduce { _, value -> value }

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
fun OrbitContainerHost<State.ProjectsListState>.loadPageFrom(repository: ProjectRepository, pageKey: String?) = intent {
    postSideEffect(UserSideEffects.Toast("Loading projects list ${pageKey ?: "initial"} page"))

    reduce {
        state.copy(loading = true)
    }

    val colorPicker = InfiniteColorPicker(state.stackColorMap)

    val response = repository.fetchProjects(pageKey)

    flowOf(response).map {
        response.data.map { project ->
            val response2 = repository.fetchStack(project.name)

            response2.map { stack1 ->
                stack1.map { entry -> Stack(entry.key, entry.value, colorPicker.pick(entry.key)) }
            }.map { stack2 ->
                Project(
                    project.id,
                    project.name,
                    project.description,
                    stack2,
                    Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg")
                )
            }.reduce { _, value -> value }
        }
    }.collect {
        reduce {
            state.copy(loading = false, projectsPage = it, nextPageUrl = response.next, isLastPage = response.next == null, stackColorMap = colorPicker.colorMap)
        }
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
