package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toProfile
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.getOrNull

class UserProfile(
    coroutineScope: CoroutineScope,
    private val dispatcherIO: CoroutineDispatcher,
    private val firestore: Firestore,
    private val userAccount: Account
) : KoinComponent, OrbitStore<ProfileState>(coroutineScope, ProfileState.Initialized) {

    private var fetchJob: Job? = null

    init {
        fetch()
    }

    fun fetch() = intent {
        fetchJob?.cancel()
        fetchJob = coroutineScope {
            val userId = userAccount.id
            val result = runCatching {
                withContext(dispatcherIO) {
                    firestore.getProfile(userId)?.toProfile() ?: throw Exception("Profile not found.")
                }
            }
            launch {
                when {
                    result.isSuccess -> {
                        reduce {
                            ProfileState.Completed(result.getOrNull()!!)
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
        }
    }
}

fun StateFlow<ProfileState>.toProfile() =
    map {
        when (it) {
            is ProfileState.Initialized -> Response.Pending
            is ProfileState.Completed -> Response.Ok(it.profile)
            is ProfileState.Error -> Response.Error(it.reason)
        }
    }
        .map { it.getOrNull() }
