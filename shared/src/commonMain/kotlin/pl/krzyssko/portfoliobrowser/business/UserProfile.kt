package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Profile
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
    private val auth: Auth
) : KoinComponent, OrbitStore<ProfileState>(coroutineScope, ProfileState.Initialized) {

    private var fetchJob: Job? = null

    val profileState: Flow<Profile> = stateFlow
        .map {
            when (it) {
                is ProfileState.Initialized -> Response.Pending
                is ProfileState.Completed -> Response.Ok(it.profile)
                is ProfileState.Error -> Response.Error(it.reason)
            }
        }
        .map { it.getOrNull() }
        .filterNotNull()
        //.stateIn(coroutineScope, SharingStarted.Eagerly, Profile.Stub)

    fun fetch() = intent {
        fetchJob?.cancel()
        fetchJob = coroutineScope {
            val userId = auth.userAccount?.id.orEmpty()
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
