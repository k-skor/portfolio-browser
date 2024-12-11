package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.platform.getLogging
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
    }.collect {
        it.collect {
            reduce {
                ProjectState.Ready(it)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.ProjectDetails))
        }
    }
}

fun OrbitContainerHost<ProjectsListState>.initAuth(auth: Auth) = intent {
    (state as? ProjectsListState.Idling)?.let {
        auth.initAuth()
        reduce {
            ProjectsListState.Initialized
        }
    }
}

fun OrbitContainerHost<ProjectsListState>.reauthenticate(uiHandler: Any?, auth: Auth) = authenticateWithGitHub(uiHandler, auth, true)

fun OrbitContainerHost<ProjectsListState>.authenticateWithGitHub(uiHandler: Any?, auth: Auth, reauthenticate: Boolean = false) = intent {
    (state as? ProjectsListState.Initialized)?.let {
        auth.startSignInFlow(uiHandler, reauthenticate)?.let {
            reduce {
                ProjectsListState.Authenticated(user = it)
            }
            postSideEffect(UserSideEffects.Toast("Preparing user account..."))
        }
    }
}

fun OrbitContainerHost<ProjectsListState>.resetAuth(auth: Auth) = intent {
    auth.signOut()
    reduce {
        ProjectsListState.Initialized
    }
}

fun loadPage(
    projectFlow: () -> Flow<PagedResponse<Project>>,
    stackFlow: (projectName: String) -> Flow<List<Stack>>,
    colorPicker: InfiniteColorPicker
): Flow<Flow<PagedResponse<Project>>> {
    return projectFlow().map {
        flow {
            emit(it)

            val data = it.data.toMutableList()
            for (index in it.data.indices) {
                val project = it.data[index]
                stackFlow(project.name).collect { list ->
                    data[index] = project.copy(stack = list.map { stack ->
                        stack.copy(
                            color = colorPicker.pick(stack.name)
                        )
                    })
                    getLogging().debug("store stack emit size=${list.size}")
                    emit(it.copy(data = data))
                }
            }
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

    val getPaging = { page: PagedResponse<Any> ->
        Paging(
            currentPageUrl = pageKey,
            nextPageUrl = page.next,
            prevPageUrl = page.prev,
            isLastPage = pageKey == page.last
        )
    }

    when (state) {
        is ProjectsListState.Authenticated -> loadPage(
            { repository.fetchProjects(pageKey) },
            { projectName -> repository.fetchStack(projectName) },
            colorPicker
        ).collect {
            it.collect { page ->
                reduce {
                    ProjectsListState.Ready(
                        projects = mapOf(pageKey to page.data),
                        paging = getPaging(page)
                    )
                }
            }
        }
        is ProjectsListState.Ready -> loadPage(
            { repository.fetchProjects(pageKey) },
            { projectName -> repository.fetchStack(projectName) },
            colorPicker
        ).collect {
            it.collect { page ->
                reduce {
                    val state = (state as ProjectsListState.Ready)
                    state.copy(
                        projects = state.projects + (pageKey to page.data),
                        paging = getPaging(page)
                    )
                }
            }
        }
        else -> return@intent
    }
}

fun OrbitContainerHost<ProjectsListState>.updateSearchPhrase(
    phrase: String?,
) = intent {
    (state as? ProjectsListState.Ready)?.let {
        if (phrase != it.searchPhrase) {
            reduce { it.copy(searchPhrase = phrase, projects = emptyMap()) }
        }
    }
}

fun OrbitContainerHost<ProjectsListState>.searchProjects(
    repository: ProjectRepository,
    colorPicker: InfiniteColorPicker,
    pageKey: String?
) = intent {
    (state as? ProjectsListState.Ready)?.let { state ->

        // TODO: move to API layer
        val query = "q=${state.searchPhrase} in:name in:description user:k-skor"

        loadPage(
            { repository.searchProjects(query, pageKey) },
            { projectName -> repository.fetchStack(projectName) },
            colorPicker
        ).takeIf { !state.searchPhrase.isNullOrEmpty() }?.collect {
            it.collect { page ->
                reduce {
                    state.copy(
                        projects = state.projects + (pageKey to page.data),
                        paging = Paging(
                            currentPageUrl = pageKey,
                            nextPageUrl = page.next,
                            prevPageUrl = page.prev,
                            isLastPage = pageKey == page.last
                        )
                    )
                }
            }
        }
    }
}

fun OrbitContainerHost<ProjectsListState>.createUser(
    firestore: Firestore
) = intent {
    (state as? ProjectsListState.Authenticated)?.let {
        firestore.createUser(it.user?.profile ?: return@intent)
    }
}

class OrbitStore<TState : Any>(private val coroutineScope: CoroutineScope, initialState: TState) {
    val containerHost = OrbitContainerHost(coroutineScope, initialState)

    val stateFlow = containerHost.container.stateFlow
    val sideEffectFlow = containerHost.container.sideEffectFlow

    fun stateIn(stateFlow: Flow<TState>, initialState: TState) = stateFlow.stateIn(coroutineScope, SharingStarted.Lazily, initialState)
}

fun OrbitStore<ProjectState>.project(block: OrbitContainerHost<ProjectState>.() -> Unit) {
    containerHost.apply(block)
}

fun OrbitStore<ProjectsListState>.projectsList(block: OrbitContainerHost<ProjectsListState>.() -> Unit) {
    containerHost.apply(block)
}
