package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthAccountExistsException
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.db.transfer.toProfile
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.platform.Config
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

open class OrbitStore<TState : Any>(
    val coroutineScope: CoroutineScope,
    val dispatcherIO: CoroutineDispatcher,
    initialState: TState
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

    flow {
        emit(repository.fetchProjectDetails(uid = ownerId, id = projectId))
    }
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
fun OrbitStore<LoginState>.initAuth(auth: Auth) = intent {
    auth.initAuth()
    if (auth.isUserSignedIn) {
        val account = auth.userAccount!!
        val user = if (account.anonymous) {
            User.Guest
        } else {
            User.Authenticated(account = account)
        }
        reduce {
            LoginState.Authenticated(user = user)
        }
    } else {
        reduce {
            LoginState.Initialized
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
    }
}

fun OrbitStore<LoginState>.createAccount(uiHandler: Any?, auth: Auth, login: String, password: String, config: Configuration) = intent {
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
            val exception = result.exceptionOrNull()
            if (exception is AuthAccountExistsException) {
                postSideEffect(UserSideEffects.NavigateTo(Route.AccountsMerge))
            } else {
                handleException(result.exceptionOrNull())
                resetAuthSub(auth, config)
            }
        }

        else -> {
            val user = result.getOrNull()!!
            reduce {
                LoginState.Authenticated(user = user)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        }
    }
}

fun OrbitStore<LoginState>.authenticateAnonymous(auth: Auth, config: Configuration) = intent {
    val result = runCatching {
        withContext(dispatcherIO) {
            auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.Anonymous)
        }
    }
    when {
        result.isFailure -> {
            handleException(result.exceptionOrNull())
            resetAuthSub(auth, config)
        }

        else -> {
            reduce {
                LoginState.Authenticated(user = User.Guest)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        }
    }
}

fun OrbitStore<LoginState>.authenticateWithEmail(uiHandler: Any?, auth: Auth, login: String, password: String, config: Configuration) = intent {
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
            val exception = result.exceptionOrNull()
            if (exception is AuthAccountExistsException) {
                postSideEffect(UserSideEffects.NavigateTo(Route.AccountsMerge))
            } else {
                handleException(result.exceptionOrNull())
                resetAuthSub(auth, config)
            }
        }

        else -> {
            val user = result.getOrNull()!!
            reduce {
                LoginState.Authenticated(user = user)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Home))
        }
    }
}

fun OrbitStore<LoginState>.authenticateWithGitHub(
    uiHandler: Any?,
    auth: Auth,
    config: Configuration,
    repository: ProjectRepository,
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
            val exception = result.exceptionOrNull()
            if (exception is AuthAccountExistsException) {
                postSideEffect(UserSideEffects.NavigateTo(Route.AccountsMerge))
            } else {
                handleException(result.exceptionOrNull())
                resetAuthSub(auth, config)
            }
            return@intent
        }

        else -> result.getOrNull()!!
    }

    flow {
        emit(repository.fetchUser())
    }
        .flowOn(dispatcherIO)
        .onStart {
            config.update(Config(gitHubApiToken = user.oauthToken.orEmpty()))
        }
        .map {
            when {
                it.isSuccess -> it.getOrNull()
                else -> {
                    handleException(UserFetchException(it.exceptionOrNull()))
                    null
                }
            }
        }
        .filterNotNull()
        .collect { name ->
            config.update(config.config.copy(gitHubApiUser = name))
            reduce {
                LoginState.Authenticated(
                    user = user.copy(
                        account = user.account.copy(
                            name = name
                        )
                    )
                )
            }
        }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<LoginState>.handleException(exception: Throwable?) = subIntent {
    reduce {
        LoginState.Error(exception)
    }
    postSideEffect(UserSideEffects.Error(exception))
    postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
}

fun OrbitStore<ProfileState>.fetchUserProfile(account: Account, firestore: Firestore) = intent {
    val result = runCatching {
        withContext(dispatcherIO) {
            firestore.getProfile(account.id)?.toProfile() ?: throw Exception("Profile not found.")
        }
    }
    when {
        result.isSuccess -> {
            reduce {
                ProfileState.ProfileCreated(result.getOrNull()!!)
            }
        }

        else -> {
            val exception = result.exceptionOrNull()
            reduce {
                ProfileState.Error(exception)
            }
            postSideEffect(UserSideEffects.Error(exception))
            postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
        }
    }
}

//fun OrbitStore<LoginState>.updateUserProfile(firestore: Firestore, profile: Profile) = intent {
//    (state as? LoginState.Authenticated)?.let {
//        (it.user as? User.Authenticated)?.let { user ->
//            reduce {
//                LoginState.ProfileCreated(
//                    profile = profile
//                )
//            }
//            firestore.createProfile(user.account.id, profile.toDto())
//        }
//    }
//}

fun OrbitStore<LoginState>.linkWithGitHub(uiHandler: Any?, auth: Auth, config: Configuration, repository: ProjectRepository) = intent {
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
            handleException(result.exceptionOrNull())
            return@intent
        }

        else -> result.getOrNull()!!
    }
    postSideEffect(UserSideEffects.Toast("Linking user account..."))
    flow {
        emit(repository.fetchUser())
    }
        .flowOn(dispatcherIO)
        .map {
            when {
                it.isSuccess -> it.getOrNull()

                else -> {
                    handleException(it.exceptionOrNull())
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
                LoginState.Authenticated(
                    user = user.copy(
                        account = account,
                        additionalData = user.additionalData,
                        oauthToken = user.oauthToken
                    )
                )
            }
    }
}

fun OrbitStore<LoginState>.resetAuth(auth: Auth, config: Configuration) = intent {
    resetAuthSub(auth, config)
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<LoginState>.resetAuthSub(auth: Auth, config: Configuration) = subIntent {
    auth.signOut()
    config.clear()
    reduce {
        LoginState.Initialized
    }
    postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
}

fun OrbitStore<LoginState>.deleteAccount(auth: Auth, config: Configuration) = intent {
    withContext(dispatcherIO) {
        auth.delete()
    }
    config.clear()
    reduce {
        LoginState.Initialized
    }
    postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
}

fun OrbitStore<ProjectsListState>.updateSearchPhrase(
    phrase: String
) = intent {
    reduce {
        ProjectsListState.FilterRequested(searchPhrase = phrase)
    }
}

fun OrbitStore<ProjectsListState>.updateSelectedCategories(
    categories: List<String>
) = intent {
    reduce {
        ProjectsListState.FilterRequested(selectedCategories = categories)
    }
}

fun OrbitStore<ProjectsListState>.updateOnlyFeatured(
    featured: Boolean
) = intent {
    reduce {
        ProjectsListState.FilterRequested(onlyFeatured = featured)
    }
}

fun OrbitStore<ProjectsListState>.clearFilters() = intent {
    reduce {
        ProjectsListState.FilterRequested()
    }
}
