package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthAccountExistsException
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.platform.Config
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.UserRepository
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserFetchException
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.getOrNull
import kotlin.getValue

class Login(
    coroutineScope: CoroutineScope,
    private val dispatcherIO: CoroutineDispatcher,
    private val auth: Auth,
    private val config: Configuration,
    private val userRepository: UserRepository
) : KoinComponent, OrbitStore<LoginState>(coroutineScope, LoginState.Initialized) {

    private val logging: Logging by inject()

    init {
        initialize()
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
        loginAnonymous()
            .catch { handleException(it) }
            .flowOn(dispatcherIO)
            .onCompletion {
                if (it != null) {
                    resetAuthSub()
                }
            }
            .collect {
                reduce {
                    LoginState.Authenticated(user = User.Guest)
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.HomeRoot))
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun login(uiHandler: Any?, refresh: Boolean = false, forceSignIn: Boolean = false) = intent {
        loginProvider(uiHandler, refresh, forceSignIn)
            .catch {
                handleException(it)
            }
            .flatMapLatest { user ->
                fetchGitHubUserFlow(user)
                    .map { userName ->
                        user.copy(
                            account = user.account.copy(
                                name = userName
                            )
                        )
                    }
                    .catch {
                        handleException(UserFetchException(it))
                    }
            }
            .flowOn(dispatcherIO)
            .onCompletion {
                if (it != null && it !is AuthAccountExistsException) {
                    resetAuthSub()
                }
            }
            .collect {
                reduce {
                    LoginState.Authenticated(it)
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.HomeRoot))
            }
    }

    fun login(uiHandler: Any?, login: String, password: String, create: Boolean = false) = intent {
        loginEmail(uiHandler, login, password, create)
            .catch { handleException(it) }
            .flowOn(dispatcherIO)
            .onCompletion {
                if (it != null) {
                    resetAuthSub()
                }
            }
            .collect {
                reduce {
                    LoginState.Authenticated(user = it)
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
        getLogging().info(exception.toString())

        reduce {
            LoginState.Error(exception)
        }
        postSideEffect(UserSideEffects.Error(exception))
        if (exception !is AuthAccountExistsException) {
            postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
        }
        else {
            postSideEffect(UserSideEffects.NavigateTo(Route.AccountsMerge))
        }
    }


    // TODO: dokończyć i przenieść linkowanie konta
    @OptIn(ExperimentalCoroutinesApi::class)
    fun link(uiHandler: Any?) = intent {
        // CHECK: Can I place reduce here (from the startLink() method)?
        reduce {
            LoginState.LinkInProgress
        }

        linkProvider(uiHandler)
            .catch {
                handleException(it)
            }
            .flatMapLatest { user ->
                fetchGitHubUserFlow(user)
                    .map { userName ->
                        user.copy(
                            account = user.account.copy(
                                name = userName
                            )
                        )
                    }
                    .catch {
                        handleException(UserFetchException(it))
                    }
            }
            .onStart {
                postSideEffect(UserSideEffects.Toast("Linking user account..."))
            }
            .flowOn(dispatcherIO)
            .collect {
                reduce {
                    LoginState.Authenticated(it)
                }
            }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun displayWarningDialog() = subIntent {
        postSideEffect(UserSideEffects.NavigateTo(Route.AccountsMerge))
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun fetchGitHubUser(user: User.Authenticated) = subIntent {
        flow {
            emit(userRepository.fetchUser())
        }
            .flowOn(dispatcherIO)
            .onStart {
                config.update(Config(gitHubApiToken = user.oauthAccessToken.orEmpty()))
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
                config.update(config.config.copy(gitHubApiUser = name, lastSignInMethod = user.signInMethod.orEmpty()))
                val updatedUser = user.copy(
                    account = user.account.copy(
                        name = name
                    )
                )
                reduce {
                    LoginState.Authenticated(updatedUser)
                }
            }
    }

    private fun loginAnonymous() = flow {
        emit(auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.Anonymous))
    }

    private fun loginProvider(uiHandler: Any?, refresh: Boolean = false, forceSignIn: Boolean = false) = flow {
        emit(
            auth.startSignInFlow(
                uiHandler = uiHandler,
                providerType = Auth.LoginMethod.GitHub,
                refresh = refresh,
                linkWithProvider = !forceSignIn && auth.shouldLinkAccounts(Auth.LoginMethod.GitHub)
            )
        )
    }
        // TODO: obsługa null usera
        .filterNotNull()

    private fun loginEmail(uiHandler: Any?, login: String, password: String, create: Boolean = false) = flow {
        emit(
            auth.startSignInFlow(
                uiHandler = uiHandler,
                providerType = Auth.LoginMethod.Email,
                login = login,
                password = password,
                create = create
            )
        )
    }
        .filterNotNull()

    private fun linkProvider(uiHandler: Any?) = flow {
        emit(
            auth.startSignInFlow(
                uiHandler,
                providerType = Auth.LoginMethod.GitHub,
                linkWithProvider = true
            )
        )
    }
        .filterNotNull()

    private fun fetchGitHubUserFlow(user: User.Authenticated) = flow {
        emit(userRepository.fetchUser().getOrThrow())
    }
        .onStart {
            config.update(Config(gitHubApiToken = user.oauthAccessToken.orEmpty()))
        }
        .onEach {
            config.update(config.config.copy(gitHubApiUser = it, lastSignInMethod = user.signInMethod.orEmpty()))
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
