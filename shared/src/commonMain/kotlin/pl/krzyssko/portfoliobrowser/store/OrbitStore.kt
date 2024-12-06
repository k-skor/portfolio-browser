package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

class OrbitContainerHost<TState : Any>(coroutineScope: CoroutineScope, initialState: TState) :
    ContainerHost<TState, UserSideEffects>, KoinComponent {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState
    )
}

fun OrbitContainerHost<ProjectState>.loadFrom(
    repository: ProjectRepository,
    colorPicker: InfiniteColorPicker,
    projectName: String
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading project $projectName details"))

    repository.fetchProjectDetails(projectName).map {
        flow {
            repository.fetchStack(it.name).collect { list ->
                val project = it.copy(stack = list.map { stack ->
                    stack.copy(
                        color = colorPicker.pick(stack.name)
                    )
                })
                emit(project)
            }
        }
    }.onStart {
        reduce { ProjectState.Loading }
    }.collect {
        it.collect {
            reduce {
                ProjectState.Ready(it)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.ProjectDetails))
        }
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
    colorPicker: InfiniteColorPicker,
    pageKey: String?
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading projects list ${pageKey ?: "initial"} page"))

    repository.fetchProjects(pageKey).map {
        flow {
            emit(it)

            val data = it.data.toMutableList()
            for (index in it.data.indices) {
                val project = it.data[index]
                repository.fetchStack(project.name).collect { list ->
                    data[index] = project.copy(stack = list.map { stack ->
                        stack.copy(
                            color = colorPicker.pick(stack.name)
                        )
                    })
                    emit(it.copy(data = data))
                }
            }
        }
    }.onStart {
        reduce { state.copy(loading = true) }
    }.onCompletion {
        reduce { state.copy(loading = false) }
    }.collect {
        it.collect { page ->
            reduce {
                state.copy(
                    projects = state.projects + (pageKey to page.data),
                    currentPageUrl = pageKey,
                    nextPageUrl = page.next,
                    isLastPage = pageKey == page.last
                )
            }
        }
    }
}

fun OrbitContainerHost<ProjectsListState>.updateSearchPhrase(
    phrase: String?,
) = intent {
    if (phrase != state.searchPhrase) {
        reduce { state.copy(searchPhrase = phrase, projects = emptyMap()) }
    }
}

fun OrbitContainerHost<ProjectsListState>.searchProjects(
    repository: ProjectRepository,
    colorPicker: InfiniteColorPicker,
    pageKey: String?
) = intent {
    if (state.searchPhrase.isNullOrEmpty()) {
        return@intent
    }

    // TODO: move to API layer
    val query = "q=${state.searchPhrase} in:name in:description user:k-skor"

    repository.searchProjects(query, pageKey).map {
        flow {
            emit(it)

            val data = it.data.toMutableList()
            for (index in it.data.indices) {
                val project = it.data[index]
                repository.fetchStack(project.name).collect { list ->
                    data[index] = project.copy(stack = list.map { stack ->
                        stack.copy(
                            color = colorPicker.pick(stack.name)
                        )
                    })
                    emit(it.copy(data = data))
                }
            }
        }
    }.onStart {
        reduce { state.copy(loading = true) }
    }.onCompletion {
        reduce { state.copy(loading = false) }
    }.collect {
        it.collect { page ->
            reduce {
                state.copy(
                    projects = state.projects + (pageKey to page.data),
                    currentPageUrl = pageKey,
                    nextPageUrl = page.next,
                    prevPageUrl = page.prev,
                    isLastPage = pageKey == page.last
                )
            }
        }
    }
}

//fun OrbitContainerHost<ProjectState>.loadStackForProject(
//    repository: ProjectRepository,
//    colorPicker: InfiniteColorPicker,
//    projectName: String
//) =
//    intent {
//        (state as? ProjectState.Ready)?.let { state ->
//            repository.fetchStack(projectName).map { stack ->
//                state.project.copy(stack = stack)
//            }.collect {
//                reduce {
//                    state.copy(project = it)
//                }
//            }
//        }
//    }

//fun OrbitContainerHost<ProjectsListState>.loadStack(
//    repository: ProjectRepository,
//    colorPicker: InfiniteColorPicker,
//    pageKey: String?
//) =
//    intent {
//        var list: List<Project> = listOf()
//        state.projects[pageKey]?.onEach { project ->
//            repository.fetchStack(project.name).map { entries ->
//                entries.map { entry ->
//                    Stack(
//                        name = entry.key,
//                        lines = entry.value,
//                        color = colorPicker.pick(entry.key)
//                    )
//                }
//            }.onEach { stack ->
//                list = list + project.copy(stack = stack)
//            }.map {
//                state.projects + (pageKey to list)
//            }.collect {
//                reduce {
//                    state.copy(projects = it)
//                }
//            }
//        }
//    }

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
