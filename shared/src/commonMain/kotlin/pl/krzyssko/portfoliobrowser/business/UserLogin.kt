package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthAccountExistsException
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.platform.Config
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.UserRepository
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserFetchException
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.Response

class UserLogin(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher,
    private val auth: Auth,
    private val config: Configuration,
    private val repository: ProjectRepository,
    private val userRepository: UserRepository
) : KoinComponent, OrbitStore<LoginState>(coroutineScope, dispatcherIO, LoginState.Initialized) {

    val userState: SharedFlow<Response<User>> = stateFlow
        .map {
            when (it) {
                is LoginState.Initialized -> Response.Pending
                is LoginState.Authenticated -> Response.Ok(it.user)
                is LoginState.Error -> Response.Error(it.reason)
            }
        }
        .shareIn(coroutineScope, SharingStarted.Eagerly)

    init {
        initialize()
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
            postSideEffect(UserSideEffects.NavigateTo(Route.Welcome))
        }
    }

    fun login() = intent {
        val result = runCatching {
            withContext(dispatcherIO) {
                auth.startSignInFlow(uiHandler = null, providerType = Auth.LoginMethod.Anonymous)
            }
        }
        when {
            result.isFailure -> {
                handleException(result.exceptionOrNull())
                resetAuthSub()
            }

            else -> {
                reduce {
                    LoginState.Authenticated(user = User.Guest)
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            }
        }
    }

    fun login(uiHandler: Any?, refresh: Boolean = false, forceSignIn: Boolean = false) = intent {
        val result = runCatching {
            withContext(dispatcherIO) {
                auth.startSignInFlow(
                    uiHandler = uiHandler,
                    providerType = Auth.LoginMethod.GitHub,
                    refresh = refresh,
                    linkWithProvider = !forceSignIn && auth.shouldLinkAccounts(Auth.LoginMethod.GitHub)
                )
            }
        }
        val user = when {
            result.isFailure -> {
                val exception = result.exceptionOrNull()
                handleException(exception)
                if (exception !is AuthAccountExistsException) {
                    resetAuthSub()
                }
                return@intent
            }

            else -> result.getOrNull()!!
        }

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
                reduce {
                    LoginState.Authenticated(
                        user = user.copy(
                            account = user.account.copy(
                                name = name
                            )
                        )
                    )
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.Home))
            }
    }

    fun login(uiHandler: Any?, login: String, password: String, create: Boolean = false) = intent {
        val result = runCatching {
            withContext(dispatcherIO) {
                auth.startSignInFlow(
                    uiHandler = uiHandler,
                    providerType = Auth.LoginMethod.Email,
                    login = login,
                    password = password,
                    create = create
                )
            }
        }
        when {
            result.isFailure -> {
                val exception = result.exceptionOrNull()
                handleException(exception)
                if (exception !is AuthAccountExistsException) {
                    resetAuthSub()
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
        reduce {
            LoginState.Error(exception)
        }
        postSideEffect(UserSideEffects.Error(exception))
        if (exception !is AuthAccountExistsException) {
            postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
        }
    }
}
