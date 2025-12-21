package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthLinkFailedException
import pl.krzyssko.portfoliobrowser.data.Config
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.business.Destination
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.business.SyncHandler
import pl.krzyssko.portfoliobrowser.data.Role
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.Paging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

class OrbitStore<TState : Any>(
    coroutineScope: CoroutineScope,
    initialState: TState,
    val dispatcherIO: CoroutineDispatcher
) :
    ContainerHost<TState, UserSideEffects>, KoinComponent {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState,
    )
    val stateFlow = container.stateFlow
    val sideEffectFlow = container.sideEffectFlow
}

@OptIn(ExperimentalCoroutinesApi::class)
fun OrbitStore<ProjectState>.loadFrom(
    repository: ProjectRepository,
    colorPicker: InfiniteColorPicker,
    ownerId: String,
    projectId: String
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading project $ownerId:$projectId details"))

    repository.fetchProjectDetails(uid = ownerId, id = projectId)
        .flowOn(dispatcherIO)
        .map {
            when {
                it.isSuccess -> {
                    it.getOrNull()
                }

                else -> {
                    val exception = it.exceptionOrNull()
                    reduce {
                        ProjectState.Error(exception)
                    }
                    exception?.printStackTrace()
                    postSideEffect(UserSideEffects.Toast(exception?.message.toString()))
                    null
                }
            }
        }
        .filterNotNull()
        .flatMapLatest {
            //flow {
            //    repository.fetchStack(it.name).collect { list ->
            //        val project = it.copy(stack = list.map { stack ->
            //            stack.copy(
            //                color = colorPicker.pick(stack.name)
            //            )
            //        })
            //        emit(project)
            //    }
            //}
            repository.fetchStack(it.name)
                .map { result ->
                    when {
                        result.isSuccess -> result.getOrNull()
                        else -> emptyList()
                    }?.let { list ->
                        it.copy(stack = list.map { stack ->
                            stack.copy(
                                color = colorPicker.pick(stack.name)
                            )
                        })
                    }
                }
        }
        .filterNotNull()
        .collect {
            getLogging().debug("store project emit")
            reduce {
                ProjectState.Loaded(it)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Details(ownerId, it.id)))
        }
}

fun OrbitStore<ProjectState>.updateProject(
    project: Project,
) = intent {
    (state as? ProjectState.Loaded)?.let {
        reduce {
            ProjectState.Loaded(project)
        }
    }
}

fun OrbitStore<ProjectState>.saveProject(
    project: Project,
    db: Firestore,
    createNew: Boolean = false
) = intent {
    (state as? ProjectState.Loaded)?.let {
        try {
            db.updateProject(project.createdBy, if (!createNew) project.id else null, project.toDto())
            reduce {
                ProjectState.Loaded(project)
            }
        } catch (e: Exception) {
            postSideEffect(UserSideEffects.Toast(e.toString()))
        }
    }
}

fun OrbitStore<ProfileState>.initAuth(auth: Auth, config: Configuration, db: Firestore) = intent {
    auth.initAuth(config.config)
    if (auth.isUserSignedIn) {
        val account = auth.userAccount!!
        val user = if (account.anonymous) {
            User.Guest
        } else {
            User.Authenticated(account = account)
        }
        //onAuth(user = user, db = db, auth = auth)
        reduce {
            ProfileState.Authenticated(user = user, linkedProviders = auth.providerData)
        }
        //postAuth(user, db)
        postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        if (user is User.Authenticated) {
            if (!db.hasUser(account.id)) {
                createUserProfile(user, db)
            }
            getUserProfile(user, db)
        }
    } else {
        reduce {
            ProfileState.Initialized
        }
    }
}

fun OrbitStore<ProfileState>.getOrCreateProfile(user: User.Authenticated, db: Firestore) = intent {
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
    if (!db.hasUser(user.account.id)) {
        createUserProfile(user, db)
    }
    getUserProfile(user, db)
    //checkImport(user.account.id, db)
}

fun OrbitStore<ProfileState>.checkImport(user: User.Authenticated, db: Firestore) = intent {
    if (db.getLastSyncTimestampForSource(user.account.id, Source.GitHub) == null) {
        reduce {
            ProfileState.SourceAvailable
        }
        postSideEffect(UserSideEffects.Toast("New source is available."))
        //postSideEffect(UserSideEffects.SyncSnack(Source.GitHub))
    }
}

fun OrbitStore<ProfileState>.openImport() = intent {
    reduce {
        ProfileState.SourceImportAttempted
    }
    postSideEffect(UserSideEffects.NavigateTo(Route.Login(ViewType.SourceSelection)))
}

fun OrbitStore<ProfileState>.createAccount(uiHandler: Any?, auth: Auth, login: String, password: String, db: Firestore, config: Configuration) = intent {
    if (auth.isUserSignedIn) {
        return@intent
    }
    val result = runCatching {
        withContext(dispatcherIO) {
            //val shouldLinkAccounts = (state as? ProfileState.Authenticated)?.user is User.Guest
            auth.startSignInFlow(uiHandler, providerType = Auth.LoginMethod.Email, login = login, password = password, create = true, linkWithProvider = auth.shouldLinkAccounts(Auth.LoginMethod.Email))
        }
    }
    when {
        result.isFailure -> {
            reduce {
                ProfileState.Error(result.exceptionOrNull())
            }
            resetAuth(auth, config)
        }

        else -> {
            val user = result.getOrNull() ?: run {
                reduce {
                    ProfileState.Error(UserMissingException())
                }
                return@intent
            }
            //config.config = config.config.copy(lastSignInMethod = auth.requestedLoginMethod.toString())
            //onAuth(user, auth, db, config)
            reduce {
                ProfileState.Authenticated(user = user, linkedProviders = auth.providerData)
            }
            createUserProfile(user, db)
            //postAuth(user, db)
        }
    }
}

fun OrbitStore<ProfileState>.authenticateAnonymous(auth: Auth, firestore: Firestore, config: Configuration) = intent {
    if (auth.isUserSignedIn) {
        return@intent
    }
    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.Anonymous)
        }
    }
    when {
        result.isFailure -> {
            reduce {
                ProfileState.Error(result.exceptionOrNull())
            }
            resetAuth(auth, config)
        }

        else -> {
            result.getOrNull() ?: run {
                reduce {
                    ProfileState.Error(UserMissingException())
                }
                return@intent
            }
            //config.config = config.config.copy(lastSignInMethod = auth.requestedLoginMethod.toString())
            //onAuth(User.Guest, auth, firestore, config)
            reduce {
                ProfileState.Authenticated(user = User.Guest, linkedProviders = auth.providerData)
            }
            //postAuth(User.Guest, null)
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        }
    }
}

fun OrbitStore<ProfileState>.authenticateWithEmail(uiHandler: Any?, auth: Auth, login: String, password: String, db: Firestore, config: Configuration) = intent {
    if (auth.isUserSignedIn) {
        return@intent
    }
    val result = runCatching {
        withContext(dispatcherIO) {
            //val shouldLinkAccounts = (state as? ProfileState.Authenticated)?.user is User.Guest
            auth.startSignInFlow(uiHandler = uiHandler, providerType = Auth.LoginMethod.Email, login = login, password = password, linkWithProvider = auth.shouldLinkAccounts(Auth.LoginMethod.Email))
        }
    }
    when {
        result.isFailure -> {
            reduce {
                ProfileState.Error(result.exceptionOrNull())
            }
            resetAuth(auth, config)
        }

        else -> {
            val user = result.getOrNull() ?: run {
                reduce {
                    ProfileState.Error(UserMissingException())
                }
                return@intent
            }
            //config.config = config.config.copy(lastSignInMethod = user.signInMethod?.toLoginMethod()?.toString().orEmpty())
            //config.config = config.config.copy(lastSignInMethod = auth.requestedLoginMethod.toString())
            //onAuth(user, auth, firestore, config)
            val hasUser = db.hasUser(user.account.id)
            reduce {
                ProfileState.Authenticated(user = user, linkedProviders = auth.providerData, hasProfile = hasUser)
            }
            //postAuth(user, firestore)
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            if (!hasUser) {
                createUserProfile(user, db)
            }
            getUserProfile(user, db)
        }
    }
}

fun OrbitStore<ProfileState>.authenticateWithGitHub(uiHandler: Any?, auth: Auth, config: Configuration, repository: ProjectRepository, db: Firestore, reauthenticate: Boolean = false, forceSignIn: Boolean = false) = intent {
    //val user = try {
    //    auth.startSignInFlow(
    //        uiHandler = uiHandler,
    //        providerType = Auth.LoginMethod.GitHub,
    //        refresh = reauthenticate,
    //        linkWithProvider = !forceSignIn && auth.shouldLinkAccounts(Auth.LoginMethod.GitHub)
    //    )
    //} catch (exception: Exception) {
    //    reduce {
    //        ProfileState.Error(exception)
    //    }
    //    if (exception is AuthLinkFailedException) {
    //        postSideEffect(UserSideEffects.LinkSnack())
    //    }
    //    return@intent
    //}
    //if (user == null) {
    //    reduce {
    //        ProfileState.UserError
    //    }
    //    return@intent
    //}
    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(
                uiHandler = uiHandler,
                providerType = Auth.LoginMethod.GitHub,
                refresh = reauthenticate,
                linkWithProvider = !forceSignIn && auth.shouldLinkAccounts(Auth.LoginMethod.GitHub)
            )
        }
    }
    val user = when {
        result.isFailure -> {
            val exception = result.exceptionOrNull()
            reduce {
                ProfileState.Error(exception)
            }
            if (exception is AuthLinkFailedException) {
                postSideEffect(UserSideEffects.LinkSnack())
            } else {
                resetAuth(auth, config)
            }
            return@intent
        }

        else -> result.getOrNull() ?: run {
            reduce {
                ProfileState.Error(UserMissingException())
            }
            return@intent
        }
    }

    config.config = Config(gitHubApiToken = user.token.orEmpty())
    repository.fetchUser()
        .flowOn(dispatcherIO)
        .map {
            when {
                it.isSuccess -> it.getOrNull()
                else -> {
                    reduce {
                        ProfileState.Error(UserFetchException(it.exceptionOrNull()))
                    }
                    null
                }
            }
        }
        .filterNotNull()
        .collect { name ->
            config.config = config.config.copy(gitHubApiUser = name)
            val hasUser = db.hasUser(user.account.id)
            reduce {
                ProfileState.Authenticated(
                    user = user.copy(
                        account = user.account.copy(
                            name = name
                        )
                    ),
                    linkedProviders = auth.providerData,
                    hasProfile = hasUser
                )
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            if (!hasUser) {
                createUserProfile(user, db)
            }
            getUserProfile(user, db)
        }
}

fun OrbitStore<ProfileState>.createUserProfile(user: User.Authenticated, db: Firestore) = intent {
    if (!db.hasUser(user.account.id)) {
        postSideEffect(UserSideEffects.Toast("Preparing user account."))
        val profile = Profile(
            alias = user.account.name,
            title = "apps for Android",
            role = listOf(Role.Developer.toString()),
            avatarUrl = user.account.avatarUrl,
            about = "I'm a developer...",
        )
        db.createProfile(user.account.id, profile)
        reduce {
            ProfileState.ProfileCreated(profile)
        }
        if (user.signInMethod == Auth.LoginMethod.GitHub.toString()) {
            checkImport(user, db)
        } else {
            postSideEffect(UserSideEffects.Toast("Done."))
        }
    } else {
        reduce {
            ProfileState.Error(Exception("User account already exists."))
        }
    }
}

fun OrbitStore<ProfileState>.getUserProfile(user: User.Authenticated, firestore: Firestore) = intent {
    (firestore.getProfile(user.account.id))?.let {
        reduce {
            ProfileState.ProfileCreated(it)
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
fun OrbitStore<ProfileState>.updateUserProfile(firestore: Firestore, profile: Profile) = intent {
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

fun OrbitStore<ProfileState>.linkWithGitHub(uiHandler: Any?, auth: Auth, config: Configuration, repository: ProjectRepository, user: User.Authenticated) = intent {
    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(
                uiHandler,
                providerType = Auth.LoginMethod.GitHub,
                linkWithProvider = true
            )
        }
    }
    val user = when {
        result.isFailure -> {
            reduce {
                ProfileState.Error(result.exceptionOrNull())
            }
            return@intent
        }

        else -> result.getOrNull() ?: run {
            reduce {
                ProfileState.Error(UserMissingException())
            }
            return@intent
        }
    }
    postSideEffect(UserSideEffects.Toast("Linking user account..."))
    config.config = Config(gitHubApiToken = user.token.orEmpty())
    repository.fetchUser()
        .flowOn(dispatcherIO)
        .map {
            when {
                it.isSuccess -> it.getOrNull()

                else -> {
                    val exception = it.exceptionOrNull()
                    reduce {
                        ProfileState.Error(exception)
                    }
                    exception?.printStackTrace()
                    postSideEffect(UserSideEffects.Toast(exception?.message.toString()))
                    null
                }
            }
        }
        .filterNotNull()
        .collect { name ->
            config.config = Config(
                gitHubApiUser = name,
                gitHubApiToken = user.token.orEmpty(),
                lastSignInMethod = user.signInMethod.orEmpty()
            )
            val account = user.account.copy(
                name = name
            )
            reduce {
                ProfileState.Authenticated(
                    user = user.copy(
                        account = account,
                        additionalData = user.additionalData,
                        token = user.token
                    ),
                    linkedProviders = (state as? ProfileState.Authenticated)?.linkedProviders,
                    hasProfile = (state as? ProfileState.Authenticated)?.hasProfile == true
                )
            }
    }
}

fun OrbitStore<ProfileState>.resetAuth(auth: Auth, config: Configuration) = intent {
    auth.signOut()
    config.clear()
    reduce {
        ProfileState.Initialized
    }
    postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
}

//fun loadPage(
//    projectFlow: () -> Flow<PagedResponse<Project>>,
//    stackFlow: (projectName: String) -> Flow<List<Stack>>,
//    colorPicker: InfiniteColorPicker
//): Flow<PagedResponse<Project>> = flow {
//    projectFlow().collect {
//        emit(it)
//
//        val data = it.page.toMutableList()
//        for (index in it.page.indices) {
//            val project = it.page[index]
//            stackFlow(project.name).collect { list ->
//                data[index] = project.copy(stack = list.map { stack ->
//                    stack.copy(
//                        color = colorPicker.pick(stack.name)
//                    )
//                })
//                getLogging().debug("store stack emit size=${list.size}")
//                emit(it.copy(page = data))
//            }
//        }
//    }
//}
//
//fun loadPage2(
//    projectFlow: () -> Flow<List<Project>>,
//    stackFlow: ((projectName: String) -> Flow<List<Stack>>)?
//): Flow<List<Project>> = flow {
//    projectFlow().collect {
//        emit(it)
//
//        if (stackFlow == null) {
//            return@collect
//        }
//        val data = it.toMutableList()
//        for (index in it.indices) {
//            val project = it[index]
//            stackFlow(project.name).collect { list ->
//                data[index] = project.copy(stack = list)
//                getLogging().debug("store stack emit size=${list.size}")
//                emit(data)
//            }
//        }
//    }
//}

//@OptIn(ExperimentalCoroutinesApi::class)
//fun Flow<List<Project>>.getStack(stackFlow: ((projectName: String) -> Flow<List<Stack>>)): Flow<List<Project>> {
//    return flatMapLatest {
//        combine(it.map { project ->
//            stackFlow(project.name)
//                .map { stack -> project.copy(stack = stack) }
//        }) { projectFlow -> projectFlow.asList() }
//    }
//}

/**
 * 1. Called from PagingSource
 * 2. Take repository
 * 3. Reduce to state
 * 4. Return State, Flow, etc.
 */
fun OrbitStore<ProjectsListState>.loadPageFrom(
    repository: ProjectRepository
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading projects list ${repository.pagingState.paging.pageKey ?: "initial"} page"))

    val colorPicker = InfiniteColorPicker()
    //loadPage2(
    //    { repository.nextPage() },
    //    null
    //)

    repository.nextPage()
        .flowOn(dispatcherIO)
        .map {
            when {
                it.isSuccess -> {
                    it.getOrNull()
                }

                else -> {
                    val exception = it.exceptionOrNull()
                    reduce {
                        ProjectsListState.Error(exception)
                    }
                    exception?.printStackTrace()
                    postSideEffect(UserSideEffects.Toast(exception?.message.toString()))
                    resetProjectsList()
                    null
                }
            }
        }
        .filterNotNull()
        .map {
            it.map { project ->
                project.copy(stack = project.stack.map { stack ->
                    stack.copy(
                        color = colorPicker.pick(
                            stack.name
                        )
                    )
                })
            }
        }
        .collect { page ->
            val newPageKey = repository.pagingState.paging.pageKey
            (state as? ProjectsListState.Loaded)?.let {
                reduce {
                    it.copy(
                        projects = it.projects + (newPageKey to page)
                    )
                }
            } ?: reduce {
                ProjectsListState.Loaded(
                    projects = mapOf(newPageKey to page)
                )
            }
        }
}

fun OrbitStore<ProjectsListState>.updateSearchPhrase(
    phrase: String?,
) = intent {
    (state as? ProjectsListState.Loaded)?.let {
        if (phrase != it.searchPhrase) {
            reduce { it.copy(searchPhrase = phrase, projects = emptyMap()) }
        }
    }
}

fun OrbitStore<ProjectsListState>.searchProjects(
    repository: ProjectRepository,
    colorPicker: InfiniteColorPicker,
    pageKey: String?
) = intent {
    (state as? ProjectsListState.Loaded)?.let { state ->

        // TODO: move to API layer
        val query = "q=${state.searchPhrase} in:name in:description user:k-skor"

        //loadPage(
        //    { repository.searchProjects(query, pageKey) },
        //    { projectName -> repository.fetchStack(projectName) },
        //    colorPicker
        //)
        repository.searchProjects(query, pageKey)
            .flowOn(dispatcherIO)
            .takeIf { !state.searchPhrase.isNullOrEmpty() }
            ?.map {
                when {
                    it.isSuccess -> {
                        it.getOrNull()
                    }

                    else -> {
                        val exception = it.exceptionOrNull()
                        reduce {
                            ProjectsListState.Error(exception)
                        }
                        exception?.printStackTrace()
                        postSideEffect(UserSideEffects.Toast(exception?.message.toString()))
                        null
                    }
                }
            }
            ?.filterNotNull()
            ?.collect { page ->
                val current = (this.state as ProjectsListState.Loaded)
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

//fun OrbitStore<ProjectsListState>.clearProjects() = intent {
//    (state as? ProjectsListState.Loaded)?.let {
//        reduce {
//            ProjectsListState.Loaded(searchPhrase = it.searchPhrase)
//        }
//    }
//}

fun OrbitStore<ProjectsListState>.importProjects(repository: ProjectRepository, firestore: Firestore, user: User.Authenticated) = intent {

    repository.resetPagingState()

    val source = pl.krzyssko.portfoliobrowser.business.Source(
        flow {
            var isLastPage = false
            while (!isLastPage && currentCoroutineContext().isActive) {
                //val projects = loadPage2(
                //    { repository.nextPage() },
                //    { projectName -> repository.fetchStack(projectName) }
                //)
                val result = repository.nextPage()
                    .onStart {
                        reduce {
                            ProjectsListState.ImportStarted
                        }
                    }.last()

                when {
                    result.isSuccess -> {
                        val projects = result.getOrNull()
                        projects?.forEach { emit(it) }
                        isLastPage = repository.pagingState.paging.isLastPage == true || projects?.isEmpty() == true
                    }
                    else -> break
                }
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
            postSideEffect(UserSideEffects.Toast("Done."))
        }
    }
}

fun OrbitStore<ProjectsListState>.resetProjectsList() = intent {
    reduce {
        ProjectsListState.Initialized
    }
}

fun OrbitStore<ProjectState>.project(block: OrbitStore<ProjectState>.() -> Unit) {
    apply(block)
}

fun OrbitStore<ProjectsListState>.projectsList(block: OrbitStore<ProjectsListState>.() -> Unit) {
    apply(block)
}

fun OrbitStore<ProfileState>.profile(block: OrbitStore<ProfileState>.() -> Unit) {
    apply(block)
}
