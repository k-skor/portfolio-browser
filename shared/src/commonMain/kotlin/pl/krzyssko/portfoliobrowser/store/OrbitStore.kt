package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthAccountExistsException
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.business.Destination
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.business.SyncHandler
import pl.krzyssko.portfoliobrowser.business.UserOnboardingImportFromExternalSource
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.ProfileRole
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.db.transfer.toProfile
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.platform.Config
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.Paging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.util.Response

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
fun Flow<Project>.mapProject(colorPicker: InfiniteColorPicker, ownerId: String): Flow<Project> =
    map { project ->
        project.copy(stack = project.stack.map { stack ->
            stack.copy(
                color = colorPicker.pick(
                    stack.name
                )
            )
        }, favorite = project.followers.any { it.uid == ownerId })
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
                    postSideEffect(UserSideEffects.Error(exception))
                    null
                }
            }
        }
        .filterNotNull()
        .mapProject(colorPicker, ownerId)
        .collect {
            getLogging().debug("store project emit")
            reduce {
                ProjectState.Loaded(it)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Details(ownerId, it.id)))
        }
}

fun OrbitStore<ProjectState>.followProject(db: Firestore, auth: Auth, follow: Boolean) = intent {
    if (!auth.isUserSignedIn) {
        postSideEffect(UserSideEffects.Error(IllegalStateException("User not signed in.")))
        return@intent
    }
    val project = (state as ProjectState.Loaded).project
    val userId = auth.userAccount!!.id
    val ownerId = project.createdBy
    val profile = db.getProfile(userId)
    val follower = Follower(
        uid = userId,
        name = profile?.alias ?: "${profile?.firstName} ${profile?.lastName}"
    )

    withContext(dispatcherIO) {
        db.toggleFollowProject(ownerId, project.id, follower, follow)
    }

    reduce {
        ProjectState.Loaded(
            if (follow) {
                project.copy(followers = project.followers + follower)
            } else {
                project.copy(followers = project.followers - follower)
            }
        )
    }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProjectState>.updateProject(
    project: Project,
) = subIntent {
    reduce {
        ProjectState.Loaded(project)
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

@OptIn(OrbitExperimental::class)
fun OrbitStore<ProfileState>.initAuth(auth: Auth, config: Configuration, db: Firestore, userOnboarding: UserOnboardingImportFromExternalSource) = intent {
    auth.initAuth()
    if (auth.isUserSignedIn) {
        val account = auth.userAccount!!
        val user = if (account.anonymous) {
            User.Guest
        } else {
            User.Authenticated(account = account)
        }
        reduce {
            ProfileState.Authenticated(user = user)
        }
        if (user is User.Authenticated) {
            if (!db.hasUser(account.id)) {
                createUserProfile(user, db)
            }
            getUserProfile(user, db)
            verifyProfileState(auth, config)
            if (auth.hasGitHubProvider) {
                userOnboarding.checkImport()
            }
        }
    } else {
        reduce {
            ProfileState.Initialized
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
    }
}

fun OrbitStore<ProjectsImportState>.checkImport(auth: Auth, db: Firestore) = intent {
    if (!auth.isUserSignedIn) {
        return@intent
    }
    val hasTimestamp = withContext(dispatcherIO) {
        db.getLastSyncTimestampForSource(auth.userAccount!!.id, Source.GitHub) != null
    }
    if (!hasTimestamp) {
        reduce {
            ProjectsImportState.SourceAvailable
        }
        postSideEffect(UserSideEffects.Toast("New source is available."))
    }
}

fun OrbitStore<ProjectsImportState>.openImport() = intent {
    reduce {
        ProjectsImportState.SourceImportAttempted
    }
    postSideEffect(UserSideEffects.NavigateTo(Route.Login(ViewType.SourceSelection)))
}

fun OrbitStore<ProfileState>.createAccount(uiHandler: Any?, auth: Auth, login: String, password: String, db: Firestore, config: Configuration) = intent {
    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(
                uiHandler,
                providerType = Auth.LoginMethod.Email,
                login = login,
                password = password,
                create = true
            )
        }
    }
    when {
        result.isFailure -> {
            handleFailure(result)
            resetAuthSub(auth, config)
        }

        else -> {
            val user = result.getOrNull()!!
            reduce {
                ProfileState.Authenticated(user = user)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            createUserProfile(user, db)
            verifyProfileState(auth, config)
        }
    }
}

fun OrbitStore<ProfileState>.authenticateAnonymous(auth: Auth, firestore: Firestore, config: Configuration) = intent {
    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.Anonymous)
        }
    }
    when {
        result.isFailure -> {
            handleFailure(result)
            resetAuthSub(auth, config)
        }

        else -> {
            reduce {
                ProfileState.Authenticated(user = User.Guest)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        }
    }
}

fun OrbitStore<ProfileState>.authenticateWithEmail(uiHandler: Any?, auth: Auth, login: String, password: String, db: Firestore, config: Configuration) = intent {
    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(
                uiHandler = uiHandler,
                providerType = Auth.LoginMethod.Email,
                login = login,
                password = password
            )
        }
    }
    when {
        result.isFailure -> {
            handleFailure(result)
            resetAuthSub(auth, config)
        }

        else -> {
            val user = result.getOrNull()!!
            val hasUser = db.hasUser(user.account.id)
            reduce {
                ProfileState.Authenticated(user = user)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            if (!hasUser) {
                createUserProfile(user, db)
            }
            getUserProfile(user, db)
            verifyProfileState(auth, config)
        }
    }
}

fun OrbitStore<ProfileState>.authenticateWithGitHub(
    uiHandler: Any?,
    auth: Auth,
    config: Configuration,
    repository: ProjectRepository,
    db: Firestore,
    userOnboarding: UserOnboardingImportFromExternalSource,
    reauthenticate: Boolean = false,
    forceSignIn: Boolean = false
) = intent {
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
            handleFailure(result)
            resetAuthSub(auth, config)
            return@intent
        }

        else -> result.getOrNull()!!
    }

    config.update(Config(gitHubApiToken = user.oauthToken.orEmpty()))
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
            config.update(config.config.copy(gitHubApiUser = name))
            val hasUser = db.hasUser(user.account.id)
            reduce {
                ProfileState.Authenticated(
                    user = user.copy(
                        account = user.account.copy(
                            name = name
                        )
                    )
                )
            }
            if (!hasUser) {
                createUserProfile(user, db)
            }
            getUserProfile(user, db)
            verifyProfileState(auth, config)
            var profileStatusCreated = true
            if (auth.hasGitHubProvider) {
                userOnboarding.checkImport()
                profileStatusCreated = userOnboarding.stateFlow.value !is ProjectsImportState.SourceAvailable
            }
            if (profileStatusCreated) {
                postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            }
        }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProfileState>.handleFailure(result: Result<*>) = subIntent {
    val exception = result.exceptionOrNull()
    reduce {
        ProfileState.Error(exception)
    }
    if (exception is AuthAccountExistsException) {
        postSideEffect(UserSideEffects.ErrorAccountExists(exception))
    } else {
        postSideEffect(UserSideEffects.Error(exception))
        postSideEffect(UserSideEffects.NavigateTo(Route.Error))
    }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProfileState>.verifyProfileState(auth: Auth, config: Configuration) = subIntent {
    val profileErrorStatus = state is ProfileState.Error
    if (profileErrorStatus) {
        resetAuthSub(auth, config)
    }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProfileState>.createUserProfile(user: User.Authenticated, db: Firestore) = subIntent {
    val hasUser = withContext(dispatcherIO) {
        db.hasUser(user.account.id)
    }
    if (!hasUser) {
        postSideEffect(UserSideEffects.Toast("Preparing user account."))
        val profile = Profile(
            firstName = "Krzysztof",
            lastName = "Skorcz",
            alias = user.account.name,
            title = "apps for Android",
            role = listOf(ProfileRole.Developer),
            avatarUrl = user.account.avatarUrl,
            about = "I'm a developer...",
            experience = 10,
            location = "Poznań, Poland",
        )
        val result = runCatching {
            withContext(dispatcherIO) {
                db.createProfile(user.account.id, profile.toDto())
            }
        }
        when {
            result.isSuccess -> {
                reduce {
                    ProfileState.ProfileCreated(profile)
                }
            }
            else -> {
                handleFailure(result)
            }
        }
    }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProfileState>.getUserProfile(user: User.Authenticated, firestore: Firestore) = subIntent {
    val result = runCatching {
        withContext(dispatcherIO) {
            firestore.getProfile(user.account.id)?.toProfile() ?: throw Exception("Profile not found.")
        }
    }
    when {
        result.isSuccess -> {
            reduce {
                ProfileState.ProfileCreated(result.getOrNull()!!)
            }
        }

        else -> {
            handleFailure(result)
        }
    }
}

fun OrbitStore<ProfileState>.updateUserProfile(firestore: Firestore, profile: Profile) = intent {
    (state as? ProfileState.Authenticated)?.let {
        (it.user as? User.Authenticated)?.let { user ->
            reduce {
                ProfileState.ProfileCreated(
                    profile = profile
                )
            }
            firestore.createProfile(user.account.id, profile.toDto())
        }
    }
}

fun OrbitStore<ProfileState>.linkWithGitHub(uiHandler: Any?, auth: Auth, config: Configuration, repository: ProjectRepository) = intent {
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
            handleFailure(result)
            return@intent
        }

        else -> result.getOrNull()!!
    }
    postSideEffect(UserSideEffects.Toast("Linking user account..."))
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
                    postSideEffect(UserSideEffects.Error(exception))
                    null
                }
            }
        }
        .filterNotNull()
        .collect { name ->
            config.update(Config(
                gitHubApiUser = name,
                gitHubApiToken = user.oauthToken.orEmpty(),
                lastSignInMethod = user.signInMethod.orEmpty()
            ))
            val account = user.account.copy(
                name = name
            )
            reduce {
                ProfileState.Authenticated(
                    user = user.copy(
                        account = account,
                        additionalData = user.additionalData,
                        oauthToken = user.oauthToken
                    )
                )
            }
    }
}

fun OrbitStore<ProfileState>.resetAuth(auth: Auth, config: Configuration) = intent {
    resetAuthSub(auth, config)
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProfileState>.resetAuthSub(auth: Auth, config: Configuration) = subIntent {
    auth.signOut()
    config.clear()
    reduce {
        ProfileState.Initialized
    }
    postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
}

fun OrbitStore<ProfileState>.deleteAccount(auth: Auth, config: Configuration) = intent {
    withContext(dispatcherIO) {
        auth.delete()
    }
    config.clear()
    reduce {
        ProfileState.Initialized
    }
    postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
}


enum class UserIntent {
    Default,
    Search,
    Favorites
}

/**
 * 1. Called from PagingSource
 * 2. Take repository
 * 3. Reduce to state
 * 4. Return State, Flow, etc.
 */
fun OrbitStore<ProjectsListState>.loadPageFrom(
    repository: ProjectRepository,
    intent: UserIntent
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading $intent list ${repository.pagingState.paging.nextPageKey} page"))

    val colorPicker = InfiniteColorPicker()

    val projectsPageFlow = when (intent) {
        UserIntent.Default -> repository.nextPage()
        UserIntent.Favorites -> repository.nextFavoritePage()
        UserIntent.Search -> flowOf(Result.failure(NotImplementedError()))
    }
    val prevState = (state as? ProjectsListState.Loaded)
    projectsPageFlow
        .flowOn(dispatcherIO)
        .onStart {
            resetProjectsList()
        }
        .map {
            when {
                it.isSuccess -> {
                    val page = it.getOrNull()!!.map { project ->
                        project.copy(stack = project.stack.map { stack ->
                            stack.copy(
                                color = colorPicker.pick(
                                    stack.name
                                )
                            )
                        })
                    }
                    val newPageKey = repository.pagingState.paging.pageKey
                    prevState?.let { state ->
                        state.copy(
                            projects = state.projects + (newPageKey to page)
                        )
                    } ?: ProjectsListState.Loaded(
                        projects = mapOf(newPageKey to page)
                    ) as ProjectsListState
                }

                else -> {
                    val exception = it.exceptionOrNull()
                    postSideEffect(UserSideEffects.Error(exception))
                    ProjectsListState.Error(exception)
                }
            }
        }
        .collect {
            reduce {
                it
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

        if (pageKey == null) {
            return@intent
        }

        repository.searchProjects(pageKey, pageKey)
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
                        postSideEffect(UserSideEffects.Error(exception))
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

fun OrbitStore<ProjectsImportState>.importProjects(repository: ProjectRepository, firestore: Firestore, auth: Auth, uiHandler: Any?) = intent {

    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(
                uiHandler = uiHandler,
                providerType = Auth.LoginMethod.GitHub,
                refresh = true
            )
        }
    }
    when {
        result.isFailure -> {
            reduce {
                ProjectsImportState.ImportError(result.exceptionOrNull())
            }
            return@intent
        }
    }
    val projectsSourceFlow: Flow<Project> = flow {
        var isLastPage = false
        while (!isLastPage && currentCoroutineContext().isActive) {
            repository.nextPage().collect {
                val projects = it.getOrThrow()
                projects.forEach { project ->
                    repository.fetchStack(project.name).collect { stack ->
                        emit(project.copy(stack = stack.getOrThrow()))
                    }
                }
                isLastPage = repository.pagingState.paging.isLastPage || projects.isEmpty()
            }
        }
    }
    val source = pl.krzyssko.portfoliobrowser.business.Source(
        projectsSourceFlow,
        Source.GitHub
    )
    val destination = Destination(firestore, auth)

    SyncHandler(source, destination)
        .sync()
        .flowOn(dispatcherIO)
        .onStart {
            repository.resetPagingState()
            reduce {
                ProjectsImportState.ImportStarted
            }
        }
        .onCompletion { throwable ->
            if (throwable != null) {
                reduce {
                    ProjectsImportState.ImportError(throwable)
                }
                postSideEffect(UserSideEffects.Error(throwable))
            } else {
                reduce {
                    ProjectsImportState.ImportCompleted
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            }
        }
        .collect {
            if (it) {
                postSideEffect(UserSideEffects.Toast("Done."))
            }
        }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProjectsListState>.handleException(exception: Throwable) = subIntent {
    //val exception = result.exceptionOrNull()
    reduce {
        ProjectsListState.Error(exception)
    }
    postSideEffect(UserSideEffects.Error(exception))
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProjectsListState>.resetProjectsList() = subIntent {
    reduce {
        ProjectsListState.Initialized
    }
}

fun OrbitStore<ProjectsListState>.clearProjectsList() = intent {
    resetProjectsList()
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
