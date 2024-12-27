package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Config
import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

class OrbitContainerHost<TState : Any>(coroutineScope: CoroutineScope, initialState: TState) :
    ContainerHost<TState, UserSideEffects>, KoinComponent {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState,
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

fun OrbitContainerHost<ProfileState>.initAuth(auth: Auth) = intent {
    (state as? ProfileState.Created)?.let {
        auth.initAuth()
        reduce {
            ProfileState.Initialized
        }
    }
}

fun OrbitContainerHost<ProfileState>.createAccount(uiHandler: Any?, auth: Auth, login: String, password: String) = intent {
    (state as? ProfileState.Initialized)?.let {
        auth.startSignInFlow(uiHandler, provider = Auth.Provider.Email, login = login, password = password, create = true)?.let { user ->
            reduce {
                ProfileState.Authenticated(user = user)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.ProjectsList))
        }
    }
}

fun OrbitContainerHost<ProfileState>.authenticateWithEmail(uiHandler: Any?, auth: Auth, login: String, password: String) = intent {
    (state as? ProfileState.Initialized)?.let {
        auth.startSignInFlow(uiHandler, provider = Auth.Provider.Email, login = login, password = password)?.let { user ->
            reduce {
                ProfileState.Authenticated(user = user)
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.authenticateWithGitHub(uiHandler: Any?, auth: Auth, repository: ProjectRepository, config: Configuration, reauthenticate: Boolean = false) = intent {
    (state as? ProfileState.Initialized)?.let {
        auth.startSignInFlow(uiHandler, provider = Auth.Provider.GitHub, refresh = reauthenticate)?.let { user ->
            postSideEffect(UserSideEffects.Toast("Preparing user account..."))
            config.config = Config(gitHubApiUser = "", gitHubApiToken = user.token.orEmpty())
            repository.fetchUser().collect { name ->
                config.config = Config(gitHubApiUser = name, gitHubApiToken = user.token.orEmpty())
                reduce {
                    ProfileState.Authenticated(
                        user = user.copy(
                            profile = user.profile.copy(
                                name = name
                            )
                        )
                    )
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.ProjectsList))
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.getGitHubUser(repository: ProjectRepository) = intent {
    (state as? ProfileState.Authenticated)?.let {
        postSideEffect(UserSideEffects.Toast("Linking user account..."))
        repository.fetchUser().collect { name ->
            reduce {
                it.copy(
                    user = it.user.copy(
                        profile = it.user.profile.copy(
                            name = name
                        )
                    )
                )
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.linkWithGitHub(uiHandler: Any?, auth: Auth, repository: ProjectRepository, config: Configuration, firestore: Firestore) = intent {
    (state as? ProfileState.Authenticated)?.let {
        auth.startSignInFlow(uiHandler, provider = Auth.Provider.GitHub, linkWithProvider = true)?.let { user ->
            postSideEffect(UserSideEffects.Toast("Linking user account..."))
            config.config = Config(gitHubApiUser = "", gitHubApiToken = user.token.orEmpty())
            repository.fetchUser().collect { name ->
                config.config = Config(gitHubApiUser = name, gitHubApiToken = user.token.orEmpty())
                val profile = it.user.profile.copy(
                    name = name
                )
                firestore.createUser(user.profile.id, profile)
                reduce {
                    it.copy(
                        user = it.user.copy(
                            profile = profile,
                            additionalData = user.additionalData,
                            token = user.token
                        )
                    )
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.ProjectsList))
            }
            //reduce {
            //    ProfileState.Authenticated(
            //        user = user.copy(
            //            additionalData = user.additionalData,
            //            token = user.token
            //        )
            //    )
            //}
            //getGitHubUser(repository).join()
        }
    }
}

fun OrbitContainerHost<ProfileState>.resetAuth(auth: Auth) = intent {
    auth.signOut()
    reduce {
        ProfileState.Initialized
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

val logging = getLogging()
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

    when (state) {
        is ProjectsListState.Loading, is ProjectsListState.Ready -> loadPage(
            { repository.fetchProjects(pageKey) },
            { projectName -> repository.fetchStack(projectName) },
            colorPicker
        ).catch { exception -> logging.debug(exception.message ?: "exception") }.collect {
            it.catch { exception1 -> logging.debug(exception1.message ?: "exception1") }.collect { page ->
                val paging = Paging(
                        currentPageUrl = pageKey,
                        nextPageUrl = page.next,
                        prevPageUrl = page.prev,
                        isLastPage = pageKey == page.last
                    )
                (state as? ProjectsListState.Ready)?.let {
                    reduce {
                        it.copy(
                            projects = it.projects + (pageKey to page.data),
                            paging = paging
                        )
                    }
                } ?: reduce {
                    ProjectsListState.Ready(
                        projects = mapOf(pageKey to page.data),
                        paging = paging
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
                val current = (this.state as ProjectsListState.Ready)
                reduce {
                    current.copy(
                        projects = current.projects + (pageKey to page.data),
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

fun OrbitContainerHost<ProjectsListState>.clearProjects() = intent {
    (state as? ProjectsListState.Ready)?.let {
        reduce {
            ProjectsListState.Ready(searchPhrase = it.searchPhrase)
        }
    }
}

//fun OrbitContainerHost<ProjectsListState>.createUser(
//    firestore: Firestore
//) = intent {
//    (state as? ProjectsListState.Authenticated)?.let {
//        firestore.createUser(it.user?.profile ?: return@intent)
//    }
//}

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

fun OrbitStore<ProfileState>.profile(block: OrbitContainerHost<ProfileState>.() -> Unit) {
    containerHost.apply(block)
}
