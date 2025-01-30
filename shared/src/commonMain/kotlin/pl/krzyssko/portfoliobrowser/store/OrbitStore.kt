package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthLinkFailedException
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
//import pl.krzyssko.portfoliobrowser.business.SyncResult
import pl.krzyssko.portfoliobrowser.data.Contact
import pl.krzyssko.portfoliobrowser.data.Role
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.navigation.Route
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
    ownerId: String,
    projectId: String
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading project $ownerId:$projectId details"))

    repository.fetchProjectDetails(uid = ownerId, id = projectId).map {
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
            //postSideEffect(UserSideEffects.NavigateTo(Route.ProjectDetails))
        }
    }
}

fun OrbitContainerHost<ProjectState>.updateProject(
    project: Project,
) = intent {
    (state as? ProjectState.Ready)?.let {
        reduce {
            ProjectState.Ready(project)
        }
    }
}

fun OrbitContainerHost<ProjectState>.saveProject(
    project: Project,
    db: Firestore,
    createNew: Boolean = false
) = intent {
    (state as? ProjectState.Ready)?.let {
        try {
            db.updateProject(project.createdBy, if (!createNew) project.id else null, project.toDto())
            reduce {
                ProjectState.Ready(project)
            }
        } catch (e: Exception) {
            postSideEffect(UserSideEffects.Toast(e.toString()))
        }
    }
}

fun OrbitContainerHost<ProfileState>.initAuth(auth: Auth, config: Configuration, db: Firestore) = intent {
    (state as? ProfileState.Created)?.let {
        auth.initAuth(config.config)
        if (auth.isUserSignedIn) {
            val account = auth.userProfile!!
            val user = if (account.anonymous) {
                User.Guest
            } else {
                User.Authenticated(account = account)
            }
            //onAuth(user = user, db = db, auth = auth)
            reduce {
                ProfileState.Authenticated(user = user, linkedProviders = auth.providerData)
            }
            postAuth(user, db)
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        } else {
            reduce {
                ProfileState.Initialized
            }
        }
    }
}

fun OrbitContainerHost<ProfileState>.onAuth(user: User, auth: Auth, db: Firestore? = null, config: Configuration? = null) = intent {
    reduce {
        ProfileState.Authenticated(user = user, linkedProviders = auth.providerData)
    }
    db?.let {
        createUserProfile(it)
        getUserProfile(it)
        //if (user is User.Authenticated && user.signInMethod?.toLoginMethod() == Auth.LoginMethod.GitHub && it.getLastSyncTimestampForSource(
        //        user.account.id,
        //        Source.GitHub
        //    ) == null
        //) {
        //    reduce {
        //        ProfileState.SourceAvailable
        //    }
        //    //postSideEffect(UserSideEffects.Toast("New source is available."))
        //    postSideEffect(UserSideEffects.SyncSnack(Source.GitHub))
        //}
    }
    config?.let {
        it.update(it.config.copy(lastSignInMethod = auth.requestedLoginMethod.toString()))
    }
}

fun OrbitContainerHost<ProfileState>.postAuth(user: User, db: Firestore? = null) = intent {
    // Check last sign in method and refresh token
    //if (auth.hasGitHubProvider) {
    //    try {
    //        val token = config.config.gitHubApiToken
    //        auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.GitHub, token = token)?.let { user ->
    //            config.config = Config(gitHubApiUser = config.config.gitHubApiUser, gitHubApiToken = user.token)
    //            onAuth(
    //                user = user.copy(
    //                    account = user.account.copy(
    //                        name = config.config.gitHubApiUser.orEmpty()
    //                    )
    //                ), auth = auth, config = config
    //            )
    //        }
    //        db?.let {
    //            checkImport(user as User.Authenticated, auth, it)
    //        }
    //    } catch (exception: Exception) {
    //        postSideEffect(UserSideEffects.Toast(exception.toString()))
    //        resetAuth(auth, config)
    //    }
    //} else {
    //}
    db?.let {
        createUserProfile(it)
        getUserProfile(it)
        (user as? User.Authenticated)?.account?.id?.let { id ->
            checkImport(id, it)
        }
    }
}

fun OrbitContainerHost<ProfileState>.checkImport(uid: String, db: Firestore) = intent {
    if (state.isLoggedIn()) {
        if (db.getLastSyncTimestampForSource(uid, Source.GitHub) == null) {
            reduce {
                ProfileState.SourceAvailable
            }
            //postSideEffect(UserSideEffects.Toast("New source is available."))
            postSideEffect(UserSideEffects.SyncSnack(Source.GitHub))
        }
    }
}

fun OrbitContainerHost<ProfileState>.createAccount(uiHandler: Any?, auth: Auth, login: String, password: String, db: Firestore, config: Configuration) = intent {
    if (state.isNotLoggedIn()) {
        //val shouldLinkAccounts = (state as? ProfileState.Authenticated)?.user is User.Guest
        auth.startSignInFlow(uiHandler, providerType = Auth.LoginMethod.Email, login = login, password = password, create = true, linkWithProvider = auth.shouldLinkAccounts(Auth.LoginMethod.Email))?.let { user ->
            //config.config = config.config.copy(lastSignInMethod = auth.requestedLoginMethod.toString())
            //onAuth(user, auth, db, config)
            reduce {
                ProfileState.Authenticated(user = user, linkedProviders = auth.providerData)
            }
            postAuth(user, db)
        }
    }
}

fun OrbitContainerHost<ProfileState>.authenticateAnonymous(auth: Auth, firestore: Firestore, config: Configuration) = intent {
    (state as? ProfileState.Initialized)?.let {
        auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.Anonymous)?.let {
            //config.config = config.config.copy(lastSignInMethod = auth.requestedLoginMethod.toString())
            //onAuth(User.Guest, auth, firestore, config)
            reduce {
                ProfileState.Authenticated(user = User.Guest, linkedProviders = auth.providerData)
            }
            postAuth(User.Guest, null)
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        }
    }
}

fun OrbitContainerHost<ProfileState>.authenticateWithEmail(uiHandler: Any?, auth: Auth, login: String, password: String, firestore: Firestore, config: Configuration) = intent {
    if (state.isNotLoggedIn()) {
        //val shouldLinkAccounts = (state as? ProfileState.Authenticated)?.user is User.Guest
        auth.startSignInFlow(uiHandler = uiHandler, providerType = Auth.LoginMethod.Email, login = login, password = password, linkWithProvider = auth.shouldLinkAccounts(Auth.LoginMethod.Email))?.let { user ->
            //config.config = config.config.copy(lastSignInMethod = user.signInMethod?.toLoginMethod()?.toString().orEmpty())
            //config.config = config.config.copy(lastSignInMethod = auth.requestedLoginMethod.toString())
            //onAuth(user, auth, firestore, config)
            reduce {
                ProfileState.Authenticated(user = user, linkedProviders = auth.providerData)
            }
            postAuth(user, firestore)
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        }
    }
}

fun OrbitContainerHost<ProfileState>.authenticateWithGitHub(uiHandler: Any?, auth: Auth, config: Configuration, repository: ProjectRepository, db: Firestore, reauthenticate: Boolean = false, forceSignIn: Boolean = false) = intent {
    //config?.let {
    //    val lastLoginMethod = (user as? User.Authenticated)?.run {
    //        signInMethod?.toLoginMethod()?.toString().orEmpty()
    //    } ?: Auth.LoginMethod.Anonymous.toString()
    //    config.config = config.config.copy(lastSignInMethod = lastLoginMethod)
    //}
    try {
        auth.startSignInFlow(
            uiHandler = uiHandler,
            providerType = Auth.LoginMethod.GitHub,
            refresh = reauthenticate,
            linkWithProvider = !forceSignIn && auth.shouldLinkAccounts(Auth.LoginMethod.GitHub)
        )?.let { user ->
            config.config = Config(gitHubApiToken = user.token.orEmpty())
            repository.fetchUser().collect { name ->
                config.config = config.config.copy(gitHubApiUser = name)
                //onAuth(
                //    user = user.copy(
                //        account = user.account.copy(
                //            name = name
                //        )
                //    ), auth, db, config
                //)
                reduce {
                    ProfileState.Authenticated(
                        user = user.copy(
                            account = user.account.copy(
                                name = name
                            )
                        ), linkedProviders = auth.providerData
                    )
                }
                postAuth(user, db)
                postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            }
        }
    } catch (exception: Exception) {
        if (exception is AuthLinkFailedException) {
            postSideEffect(UserSideEffects.LinkSnack())
        }
        reduce {
            ProfileState.AuthenticationFailed(exception)
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
                    title = "apps for Android",
                    role = Role.Developer.toString(),
                    avatarUrl = user.account.avatarUrl,
                    about = "I'm a developer...",
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
    postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
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

fun loadPage2(
    projectFlow: () -> Flow<List<Project>>,
    stackFlow: ((projectName: String) -> Flow<List<Stack>>)?
): Flow<List<Project>> = flow {
    projectFlow().collect {
        emit(it)

        if (stackFlow == null) {
            return@collect
        }
        val data = it.toMutableList()
        for (index in it.indices) {
            val project = it[index]
            stackFlow(project.name).collect { list ->
                data[index] = project.copy(stack = list)
                getLogging().debug("store stack emit size=${list.size}")
                emit(data)
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
    repository: ProjectRepository
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading projects list ${repository.pagingState?.paging?.pageKey ?: "initial"} page"))

    if (state.isStateReady()) {
        val colorPicker = InfiniteColorPicker()
        loadPage2(
            { repository.nextPage() },
            null
        ).catch { e ->
            e.printStackTrace()
            postSideEffect(UserSideEffects.Toast(e.toString()))
            reduce {
                ProjectsListState.Error(e)
            }
        }.map {
            it.map { project ->
                project.copy(stack = project.stack.map { stack ->
                    stack.copy(
                        color = colorPicker.pick(
                            stack.name
                        )
                    )
                })
            }
        }.collect { page ->
            val newPageKey = repository.pagingState?.paging?.pageKey
            (state as? ProjectsListState.Ready)?.let {
                reduce {
                    it.copy(
                        projects = it.projects + (newPageKey to page)
                    )
                }
            } ?: reduce {
                ProjectsListState.Ready(
                    projects = mapOf(newPageKey to page)
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
                        pageKey = pageKey,
                        nextPageKey = page.next,
                        prevPageKey = page.prev,
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

fun OrbitContainerHost<ProjectsListState>.importProjects(repository: ProjectRepository, firestore: Firestore, user: StateFlow<User>) = intent {

    repository.resetPagingState()

    val source = pl.krzyssko.portfoliobrowser.business.Source(
        flow {
            var isLastPage = false
            while (!isLastPage) {
                val projects = loadPage2(
                    { repository.nextPage() },
                    { projectName -> repository.fetchStack(projectName) }
                )
                //    .catch { e ->
                //    postSideEffect(UserSideEffects.Toast(e.toString()))
                //    reduce {
                //        ProjectsListState.ImportError(e)
                //    }
                //    emit(emptyList())
                //}
                    .onStart {
                    reduce {
                        ProjectsListState.ImportStarted
                    }
                }.last()

                projects.forEach { emit(it) }
                isLastPage = repository.pagingState?.paging?.isLastPage == true || projects.isEmpty()
            }
        },
        Source.GitHub
    )

    val destination = Destination(firestore, user)
    SyncHandler(source, destination).sync().catch { e ->
        reduce {
            ProjectsListState.ImportError(e)
        }
        emit(false)
    }.collect {
        if (it) {
            reduce {
                ProjectsListState.ImportCompleted
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

fun OrbitStore<ProfileState>.profile(block: OrbitContainerHost<ProfileState>.() -> Unit) {
    containerHost.apply(block)
}
