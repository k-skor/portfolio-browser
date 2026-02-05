package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.ProfileRole
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserOnboardingProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class UserOnboardingProfileInitialization(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher,
    private val firestore: Firestore
) : KoinComponent, OrbitStore<UserOnboardingProfileState>(coroutineScope, dispatcherIO, UserOnboardingProfileState.NotCreated) {

    fun start(account: Account) = intent {
        val userId = account.id
        val hasUser = profileExists(userId)
        if (!hasUser) {
            reduce {
                UserOnboardingProfileState.FirstTimeCreation
            }
            postSideEffect(UserSideEffects.Toast("Preparing user account."))
        }
    }
    
    fun create(
        account: Account,
        firstName: String = "Krzysztof",
        lastName: String = "Skorcz",
        title: String = "apps for Android",
        role: List<ProfileRole> = listOf(ProfileRole.Developer),
        about: String = "I'm a developer...",
        experience: Int = 10,
        location: String = "Poznań, Poland",
    ) = intent {
        val userId = account.id
        val hasUser = profileExists(userId)
        if (!hasUser) {
            val profile = Profile(
                firstName = firstName,
                lastName = lastName,
                alias = account.name,
                title = title,
                role = role,
                avatarUrl = account.avatarUrl,
                about = about,
                experience = experience,
                location = location,
            )
            val result = runCatching {
                withContext(dispatcherIO) {
                    firestore.createProfile(account.id, profile.toDto())
                }
            }
            when {
                result.isSuccess -> {
                    reduce {
                        UserOnboardingProfileState.Created(userName = profile.alias ?: "${profile.firstName} ${profile.lastName}")
                    }
                }
                else -> {
                    reduce {
                        UserOnboardingProfileState.Error(result.exceptionOrNull())
                    }
                }
            }
        }
    }

    private suspend fun profileExists(userId: String): Boolean {
        return withContext(dispatcherIO) {
            firestore.hasUser(userId)
        }
    }
}