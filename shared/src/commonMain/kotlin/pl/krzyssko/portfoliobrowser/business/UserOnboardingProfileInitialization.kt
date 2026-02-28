package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.ProfileRole
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserOnboardingProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class UserOnboardingProfileInitialization(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher,
    private val user: User.Authenticated,
    private val firestore: Firestore
) : KoinComponent, OrbitStore<UserOnboardingProfileState>(coroutineScope, dispatcherIO, UserOnboardingProfileState.NotCreated) {

    private var checkJob: Job? = null

    fun check() = intent {
        checkJob?.cancel()
        checkJob = coroutineScope.launch {
            val userId = user.account.id
            val hasUser = profileExists(userId)
            if (!hasUser) {
                reduce {
                    UserOnboardingProfileState.FirstTimeCreation
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.PrepareProfile))
            } else {
                reduce {
                    UserOnboardingProfileState.AlreadyCreated
                }
            }
        }
    }

    fun create(
        firstName: String = "Krzysztof",
        lastName: String = "Skorcz",
        title: String = "apps for Android",
        role: List<ProfileRole> = listOf(ProfileRole.Developer),
        about: String = "I'm a developer...",
        experience: Int = 10,
        location: String = "Poznań, Poland",
    ) = intent {
        val userId = user.account.id
        val alias = user.identityProviders.firstNotNullOfOrNull { it.name }
        val avatarUrl = user.identityProviders.firstNotNullOfOrNull { it.photoUrl }
        val hasUser = profileExists(userId)
        if (!hasUser) {
            val profile = Profile(
                firstName = firstName,
                lastName = lastName,
                alias = alias,
                title = title,
                role = role,
                avatarUrl = avatarUrl,
                about = about,
                experience = experience,
                location = location,
            )
            val result = runCatching {
                withContext(dispatcherIO) {
                    firestore.createProfile(user.account.id, profile.toDto())
                }
            }
            when {
                result.isSuccess -> {
                    reduce {
                        UserOnboardingProfileState.NewlyCreated(userName = profile.alias ?: "${profile.firstName} ${profile.lastName}")
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