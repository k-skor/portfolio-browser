package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toProfile
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.Response

class ProfileEdition(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher,
    private val firestore: Firestore
) : KoinComponent, OrbitStore<ProfileState>(coroutineScope, dispatcherIO, ProfileState.Initialized) {

    val profileState: SharedFlow<Response<Profile>> = stateFlow
        .map {
            when (it) {
                is ProfileState.Initialized -> Response.Pending
                is ProfileState.Loaded -> Response.Ok(it.profile)
                is ProfileState.Error -> Response.Error(it.reason)
            }
        }
        .shareIn(coroutineScope, SharingStarted.Eagerly)

    fun fetch(account: Account) = intent {
        val result = runCatching {
            withContext(dispatcherIO) {
                firestore.getProfile(account.id)?.toProfile() ?: throw Exception("Profile not found.")
            }
        }
        when {
            result.isSuccess -> {
                reduce {
                    ProfileState.Loaded(result.getOrNull()!!)
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