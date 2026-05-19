package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toProfile
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.getOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfile(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher,
    auth: Auth,
    firestore: Firestore
) : KoinComponent, OrbitStore<ProfileState>(coroutineScope, ProfileState.Initialized) {

    private var fetchJob: Job? = null

    init {
        doOnCreate {
            repeatOnSubscription {
                auth.userState
                    .flatMapLatest { user ->
                        if (user != null && !user.isAnonymous) {
                            firestore.observeProfile(user.uid)
                                .flowOn(dispatcherIO)
                                .map { it?.toProfile() }
                                .map { user to it }
                                .catch { emit(user to null) }
                        } else {
                            flowOf(null to null)
                        }
                    }
                    .collect { (user, profile) ->
                        if (user != null) {
                            if (profile != null) {
                                reduce {
                                    ProfileState.Completed(profile)
                                }
                            } else {
                                val exception = Exception("Profile not found.")
                                reduce {
                                    ProfileState.Error(exception)
                                }
                                postSideEffect(UserSideEffects.Error(exception))
                                postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
                            }
                        } else {
                            reduce {
                                ProfileState.Initialized
                            }
                        }
                    }
            }
        }
    }

    //fun fetch(userId: String) = intent {
    //    fetchJob?.cancel()
    //    fetchJob = coroutineScope {
    //        val result = runCatching {
    //            withContext(dispatcherIO) {
    //                firestore.getProfile(userId)?.toProfile() ?: throw Exception("Profile not found.")
    //            }
    //        }
    //        launch {
    //            when {
    //                result.isSuccess -> {
    //                    reduce {
    //                        ProfileState.Completed(result.getOrNull()!!)
    //                    }
    //                }

    //                else -> {
    //                    val exception = result.exceptionOrNull()
    //                    reduce {
    //                        ProfileState.Error(exception)
    //                    }
    //                    postSideEffect(UserSideEffects.Error(exception))
    //                    postSideEffect(UserSideEffects.NavigateTo(exception.toRoute()))
    //                }
    //            }
    //        }
    //    }
    //}

    //fun reset() = intent {
    //    reduce {
    //        ProfileState.Initialized
    //    }
    //}
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
