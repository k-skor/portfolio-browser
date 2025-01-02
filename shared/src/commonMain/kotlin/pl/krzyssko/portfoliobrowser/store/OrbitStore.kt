package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.toLoginMethod
import pl.krzyssko.portfoliobrowser.data.Config
import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.business.Destination
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.business.SyncHandler
import pl.krzyssko.portfoliobrowser.business.SyncResult
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import kotlin.time.Duration.Companion.milliseconds

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

fun OrbitContainerHost<ProfileState>.initAuth(auth: Auth, config: Configuration) = intent {
    (state as? ProfileState.Created)?.let {
        auth.initAuth()
        if (auth.isUserSignedIn) {
            // Check last sign in method and refresh token
            if (auth.hasGitHubProvider) {
                try {
                    val token = config.config.gitHubApiToken
                    auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.GitHub, token = token)?.let { user ->
                        config.config = Config(gitHubApiUser = config.config.gitHubApiUser, gitHubApiToken = user.token.orEmpty(), lastSignInMethod = user.signInMethod.orEmpty())
                        authState(
                            user = user.copy(
                                account = user.account.copy(
                                    name = config.config.gitHubApiUser
                                )
                            ), auth
                        )
                    }
                } catch (exception: Exception) {
                    postSideEffect(UserSideEffects.Toast(exception.toString()))
                    resetAuth(auth, config)
                }
            } else {
                authState(user = User.Authenticated(account = auth.userProfile!!), auth)
            }
        } else {
            reduce {
                ProfileState.Initialized
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.authState(user: User.Authenticated, auth: Auth, db: Firestore? = null) = intent {
    reduce {
        ProfileState.Authenticated(user = user, linkedProviders = auth.providerData)
    }
    db?.let {
        createUserProfile(it)
        getUserProfile(db)
        if (user.signInMethod?.toLoginMethod() == Auth.LoginMethod.GitHub && db.getLastSyncTimestampForSource(user.account.id, Source.GitHub) == null) {
            delay(1000.milliseconds)
            reduce {
                ProfileState.SourceAvailable
            }
            postSideEffect(UserSideEffects.Toast("New source is available."))
        }
    }
}

fun OrbitContainerHost<ProfileState>.createAccount(uiHandler: Any?, auth: Auth, login: String, password: String, db: Firestore) = intent {
    (state as? ProfileState.Initialized)?.let {
        auth.startSignInFlow(uiHandler, providerType = Auth.LoginMethod.Email, login = login, password = password, create = true)?.let { user ->
            authState(user, auth, db)
            postSideEffect(UserSideEffects.NavigateTo(Route.ProjectsList))
        }
    }
}

fun OrbitContainerHost<ProfileState>.authenticateWithEmail(uiHandler: Any?, auth: Auth, login: String, password: String, firestore: Firestore) = intent {
    (state as? ProfileState.Initialized)?.let {
        auth.startSignInFlow(uiHandler, providerType = Auth.LoginMethod.Email, login = login, password = password)?.let { user ->
            authState(user, auth, firestore)
        }
    }
}

fun OrbitContainerHost<ProfileState>.authenticateWithGitHub(uiHandler: Any?, auth: Auth, config: Configuration, repository: ProjectRepository, db: Firestore, reauthenticate: Boolean = false) = intent {
    (state as? ProfileState.Initialized)?.let {
        auth.startSignInFlow(uiHandler, providerType = Auth.LoginMethod.GitHub, refresh = reauthenticate)?.let { user ->
            config.config = Config(gitHubApiToken = user.token.orEmpty())
            repository.fetchUser().collect { name ->
                config.config = Config(gitHubApiUser = name, gitHubApiToken = user.token.orEmpty(), lastSignInMethod = user.signInMethod.orEmpty())
                authState(
                    user = user.copy(
                        account = user.account.copy(
                            name = name
                        )
                    ), auth, db
                )
                postSideEffect(UserSideEffects.NavigateTo(Route.ProjectsList))
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.createUserProfile(db: Firestore) = intent {
    (state as? ProfileState.Authenticated)?.let {
        (it.user as? User.Authenticated)?.let { user ->
            if (!db.isUserCreated(user.account.id)) {
                postSideEffect(UserSideEffects.Toast("Preparing user account."))
                val profile = Profile(
                    alias = user.account.name,
                )
                db.createProfile(user.account.id, profile)
                reduce {
                    ProfileState.ProfileCreated(profile)
                }
                postSideEffect(UserSideEffects.Toast("Done."))
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.getUserProfile(firestore: Firestore) = intent {
    (state as? ProfileState.Authenticated)?.let {
        (it.user as? User.Authenticated)?.let { user ->
            (firestore.getProfile(user.account.id))?.let {
                reduce {
                    ProfileState.ProfileCreated(it)
                }
            }
        }
    }
}

/**
 *
 * val profile = Profile(
 *   firstName = "Krzysztof",
 *   lastName = "Skórcz",
 *   title = "apps for Android",
 *   role = Role.Developer
 * )
 *
 */
fun OrbitContainerHost<ProfileState>.updateUserProfile(firestore: Firestore, profile: Profile) = intent {
    (state as? ProfileState.Authenticated)?.let {
        (it.user as? User.Authenticated)?.let { user ->
            reduce {
                ProfileState.ProfileCreated(
                    profile = profile
                )
            }
            firestore.createProfile(user.account.id, profile)
        }
    }
}

fun OrbitContainerHost<ProfileState>.linkWithGitHub(uiHandler: Any?, auth: Auth, config: Configuration, repository: ProjectRepository) = intent {
    (state as? ProfileState.Authenticated)?.let {
        val user = (it.user as User.Authenticated)
        auth.startSignInFlow(uiHandler, providerType = Auth.LoginMethod.GitHub, linkWithProvider = true)?.let { authUser ->
            postSideEffect(UserSideEffects.Toast("Linking user account..."))
            config.config = Config(gitHubApiToken = authUser.token.orEmpty())
            repository.fetchUser().collect { name ->
                config.config = Config(gitHubApiUser = name, gitHubApiToken = user.token.orEmpty(), lastSignInMethod = user.signInMethod.orEmpty())
                val account = user.account.copy(
                    name = name
                )
                reduce {
                    it.copy(
                        user = user.copy(
                            account = account,
                            additionalData = authUser.additionalData,
                            token = authUser.token
                        )
                    )
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.ProjectsList))
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.resetAuth(auth: Auth, config: Configuration) = intent {
    auth.signOut()
    config.clear()
    reduce {
        ProfileState.Initialized
    }
}

fun loadPage(
    projectFlow: () -> Flow<PagedResponse<Project>>,
    stackFlow: (projectName: String) -> Flow<List<Stack>>,
    colorPicker: InfiniteColorPicker
): Flow<PagedResponse<Project>> = flow {
    projectFlow().collect {
        emit(it)

        val data = it.page.toMutableList()
        for (index in it.page.indices) {
            val project = it.page[index]
            stackFlow(project.name).collect { list ->
                data[index] = project.copy(stack = list.map { stack ->
                    stack.copy(
                        color = colorPicker.pick(stack.name)
                    )
                })
                getLogging().debug("store stack emit size=${list.size}")
                emit(it.copy(page = data))
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

    if (state.isStateReady()) {
         loadPage(
            { repository.fetchProjects(pageKey) },
            { projectName -> repository.fetchStack(projectName) },
            colorPicker
        ).catch { e ->
            postSideEffect(UserSideEffects.Toast(e.toString()))
            reduce {
                ProjectsListState.Error(e)
            }
        }.collect { page ->
            val paging = Paging(
                currentPageUrl = pageKey,
                nextPageUrl = page.next,
                prevPageUrl = page.prev,
                isLastPage = page.next == null
            )
            (state as? ProjectsListState.Ready)?.let {
                reduce {
                    it.copy(
                        projects = it.projects + (pageKey to page.page),
                        paging = paging
                    )
                }
            } ?: reduce {
                ProjectsListState.Ready(
                    projects = mapOf(pageKey to page.page),
                    paging = paging
                )
            }
        }
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
        ).takeIf { !state.searchPhrase.isNullOrEmpty() }?.collect { page ->
            val current = (this.state as ProjectsListState.Ready)
            reduce {
                current.copy(
                    projects = current.projects + (pageKey to page.page),
                    paging = Paging(
                        currentPageUrl = pageKey,
                        nextPageUrl = page.next,
                        prevPageUrl = page.prev,
                        isLastPage = page.next == null
                    )
                )
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

fun OrbitContainerHost<ProjectsListState>.doImport(repository: ProjectRepository, firestore: Firestore, user: User.Authenticated) = intent {

    val source = pl.krzyssko.portfoliobrowser.business.Source(
        flow {
            var nextPageUrl: String? = null
            var isLastPage = false
            while (!isLastPage) {
                val projects = loadPage(
                    { repository.fetchProjects(nextPageUrl) },
                    { projectName -> repository.fetchStack(projectName) },
                    InfiniteColorPicker()
                ).catch { e ->
                    postSideEffect(UserSideEffects.Toast(e.toString()))
                    reduce {
                        ProjectsListState.ImportError(e)
                    }
                }.onStart {
                    reduce {
                        ProjectsListState.ImportStarted
                    }
                }.onEach { page ->
                    isLastPage = page.next == null
                    nextPageUrl = page.next
                }.last()

                projects.page.forEach { emit(it) }
            }
        }
    )

    val destination = Destination(firestore, user)
    SyncHandler(source, destination).sync().collect {
        reduce {
            when (it) {
                is SyncResult.Success -> ProjectsListState.ImportCompleted
                is SyncResult.Failure -> ProjectsListState.ImportError(it.throwable)
            }
        }
    }
}

class OrbitStore<TState : Any>(private val coroutineScope: CoroutineScope, initialState: TState) {
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

fun OrbitStore<ProfileState>.profile(block: OrbitContainerHost<ProfileState>.() -> Unit) {
    containerHost.apply(block)
}
