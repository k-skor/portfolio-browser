package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthAccountExistsException
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.ConfigurationData
import pl.krzyssko.portfoliobrowser.repository.UserRepository
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.store.onCreate
import pl.krzyssko.portfoliobrowser.store.onInitialize
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.getOrNull

class Login(
    private val dispatcherIO: CoroutineDispatcher,
    private val auth: Auth,
    private val config: Configuration,
    private val userRepository: UserRepository
) : KoinComponent, OrbitStore<LoginState>(LoginState.Initialized) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        intent {
            reduce {
                LoginState.Error(throwable)
            }
            resetAuthSub()
            postSideEffect(UserSideEffects.Error(throwable))
            postSideEffect(UserSideEffects.NavigateTo(throwable.toRoute()))
        }
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    init {
        storeConfig(coroutineScope) {
            onCreate {
                repeatOnSubscription {
                    launch {
                        stateFlow.collect(::handleInternalState)
                    }
                }
            }
            onInitialize {
                initialize()
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    suspend fun handleInternalState(state: LoginState) = subIntent {
        when (state) {
            is LoginState.Error -> {

            }

            else -> {}
        }
    }

    fun welcome() = intent {
        if (state is LoginState.Initialized) {
            postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
        }
    }

    fun initialize() = intent {
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
            // TODO: zamiast side effect sprawdzić stan przy inicjalizacji UI
            //postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
        }
    }

    fun login() = intent {
        withContext(dispatcherIO) {
            loginAnonymous()
        }
        reduce {
            LoginState.Authenticated(user = User.Guest)
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.HomeRoot))
    }

    fun login(uiHandler: Any?, refresh: Boolean = false, forceSignIn: Boolean = false) = intent {
        try {
            val user = withContext(dispatcherIO) {
                loginProvider(uiHandler, refresh, forceSignIn)
            }
            if (user != null) {
                val userName = fetchGitHubUser(user)
                val updatedUser = user.copy(
                    account = user.account.copy(
                        name = userName
                    )
                )
                reduce {
                    LoginState.Authenticated(updatedUser)
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.HomeRoot))
            }
        } catch (e: AuthAccountExistsException) {
            startLink()
        }
    }

    fun login(uiHandler: Any?, login: String, password: String, create: Boolean = false) = intent {
        val user = withContext(dispatcherIO) {
            loginEmail(uiHandler, login, password, create)
        }
        if (user != null) {
            reduce {
                LoginState.Authenticated(user = user)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.HomeRoot))
        }
    }

    fun logout() = intent {
        resetAuthSub()
    }

    fun reset() = intent {
        resetAuthSub()
    }

    fun delete() = intent {
        withContext(dispatcherIO) {
            auth.delete()
        }
        config.clear()
        reduce {
            LoginState.Initialized
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
    }

    fun stop() {
        coroutineScope.cancel()
    }

    @OptIn(OrbitExperimental::class)
    fun completeLogin(user: User.Authenticated) = intent {
        reduce {
            LoginState.Authenticated(user = user)
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun resetAuthSub() = subIntent {
        auth.signOut()
        config.clear()
        reduce {
            LoginState.Initialized
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun handleException(exception: Throwable?) = subIntent {
        reduce {
            LoginState.Error(exception)
        }
        postSideEffect(UserSideEffects.Error(exception))
        if (exception !is AuthAccountExistsException) {
            postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
        }
    }


    // TODO: dokończyć i przenieść linkowanie konta
    fun link(uiHandler: Any?) = intent {
        postSideEffect(UserSideEffects.Toast("Linking user account..."))
        try {
            val user = withContext(dispatcherIO) {
                linkProvider(uiHandler)
            }
            if (user != null) {
                val userName = fetchGitHubUser(user)
                val updatedUser = user.copy(
                    account = user.account.copy(
                        name = userName
                    )
                )
                reduce {
                    LoginState.Authenticated(updatedUser)
                }
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun startLink() = subIntent {
        postSideEffect(UserSideEffects.NavigateTo(Route.AccountsMerge))
        reduce {
            LoginState.LinkInProgress
        }
    }

    //@OptIn(OrbitExperimental::class)
    //private suspend fun fetchGitHubUser(user: User.Authenticated) = subIntent {
    //    flow {
    //        emit(userRepository.fetchUser())
    //    }
    //        .flowOn(dispatcherIO)
    //        .onStart {
    //            config.update(ConfigurationData(gitHubApiToken = user.oauthAccessToken.orEmpty()))
    //        }
    //        .map {
    //            when {
    //                it.isSuccess -> it.getOrNull()
    //                else -> {
    //                    handleException(UserFetchException(it.exceptionOrNull()))
    //                    null
    //                }
    //            }
    //        }
    //        .filterNotNull()
    //        .collect { name ->
    //            config.update(config.configurationData.copy(gitHubApiUser = name, lastSignInMethod = user.signInMethod.orEmpty()))
    //            val updatedUser = user.copy(
    //                account = user.account.copy(
    //                    name = name
    //                )
    //            )
    //            reduce {
    //                LoginState.Authenticated(updatedUser)
    //            }
    //        }
    //}

    private suspend fun loginAnonymous() = auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.Anonymous)

    private suspend fun loginProvider(
        uiHandler: Any?,
        refresh: Boolean = false,
        forceSignIn: Boolean = false
    ) =
        auth.startSignInFlow(
            uiHandler = uiHandler,
            providerType = Auth.LoginMethod.GitHub,
            refresh = refresh,
            linkWithProvider = !forceSignIn && auth.shouldLinkAccounts(Auth.LoginMethod.GitHub)
        )

    private suspend fun loginEmail(
        uiHandler: Any?,
        login: String,
        password: String,
        create: Boolean = false
    ) =
        auth.startSignInFlow(
            uiHandler = uiHandler,
            providerType = Auth.LoginMethod.Email,
            login = login,
            password = password,
            create = create
        )

    private suspend fun linkProvider(uiHandler: Any?) =
        auth.startSignInFlow(
            uiHandler,
            providerType = Auth.LoginMethod.GitHub,
            linkWithProvider = true
        )

    private suspend fun fetchGitHubUser(user: User.Authenticated): String? {
        config.update(ConfigurationData(gitHubApiToken = user.oauthAccessToken.orEmpty()))
        val gitHubUser = userRepository.fetchUser().getOrNull()
        config.update(config.configurationData.copy(gitHubApiUser = gitHubUser, lastSignInMethod = user.signInMethod.orEmpty()))
        return gitHubUser
    }

    //@OptIn(OrbitExperimental::class)
    //private suspend fun handleException(exception: Throwable?) = subIntent {
    //    reduce {
    //        AccountMergeState.Error(exception)
    //    }
    //    postSideEffect(UserSideEffects.Error(exception))
    //    postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
    //}
}

fun StateFlow<LoginState>.toUser() =
    map {
        when (it) {
            is LoginState.LinkInProgress,
            is LoginState.Initialized -> Response.Pending
            is LoginState.Authenticated -> Response.Ok(it.user)
            is LoginState.Error -> Response.Error(it.reason)
        }
    }
        .map { it.getOrNull() }
