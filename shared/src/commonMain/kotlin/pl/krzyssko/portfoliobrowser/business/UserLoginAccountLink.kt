package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.platform.Config
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.repository.UserRepository
import pl.krzyssko.portfoliobrowser.store.AccountMergeState
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserFetchException
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class UserLoginAccountLink(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher,
    private val auth: Auth,
    private val config: Configuration,
    private val repository: UserRepository
) : KoinComponent, OrbitStore<AccountMergeState>(coroutineScope, dispatcherIO, AccountMergeState.Idle) {

    init {
        start()
    }

    fun link(uiHandler: Any?) = intent {
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
        fetchGitHubUser(user)
    }

    private fun start() = intent {
        postSideEffect(UserSideEffects.NavigateTo(Route.AccountsMerge))
        reduce {
            AccountMergeState.InProgress
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun fetchGitHubUser(user: User.Authenticated) = subIntent {
        flow {
            emit(repository.fetchUser())
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
                    AccountMergeState.Success(updatedUser)
                }
            }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun handleException(exception: Throwable?) = subIntent {
        reduce {
            AccountMergeState.Error(exception)
        }
        postSideEffect(UserSideEffects.Error(exception))
        postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
    }
}
