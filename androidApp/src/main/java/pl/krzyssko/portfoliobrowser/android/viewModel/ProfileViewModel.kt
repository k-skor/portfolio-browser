package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.business.Login
import pl.krzyssko.portfoliobrowser.business.Onboarding
import pl.krzyssko.portfoliobrowser.business.UserProfile
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.OnboardingState
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel : ViewModel(), KoinComponent {

    private val login: Login by inject {
        parametersOf(viewModelScope)
    }
    private val onboarding: Onboarding by inject {
        parametersOf(viewModelScope)
    }
    private val userProfile: UserProfile by inject {
        parametersOf(viewModelScope)
    }
    val loginState: StateFlow<LoginState>
        get() = login.stateFlow
    val loginSideEffects: Flow<UserSideEffects>
        get() = login.sideEffectFlow
    val isSignedIn: Flow<Boolean>
        get() = login.user.mapLatest { it is User.Authenticated }

    val onboardingState: StateFlow<OnboardingState>
        get() = onboarding.stateFlow
    val onboardingSideEffects: Flow<UserSideEffects>
        get() = onboarding.sideEffectFlow

    val profileState: StateFlow<ProfileState>
        get() = userProfile.stateFlow
    val profileSideEffects: Flow<UserSideEffects>
        get() = userProfile.sideEffectFlow
    val profile: Flow<Profile>
        get() = userProfile.profile

    init {
        login.stateFlow
            .filter {
                it is LoginState.Authenticated && it.user is User.Authenticated
            }
            .onEach {
                onboarding.initialize()
            }
            .flatMapLatest {
                onboarding.stateFlow
                    .filter {
                        it is OnboardingState.OnboardingCompleted
                    }
                    .onEach {
                        userProfile.fetch()
                    }
            }
            .launchIn(viewModelScope)
    }

    fun welcomeUser() {
        login.welcome()
    }

    fun createUser(activity: Context, user: String, password: String) {
        login.login(activity, user, password, create = true)
    }

    fun authenticateGuest() {
        login.login()
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false) {
        login.login(activity, refreshOnly, forceSignIn)
    }

    fun authenticateUser(activity: Context, user: String, password: String) {
        login.login(activity, user, password)
    }

    fun linkUser(activity: Context) {
        login.link(activity)
    }

    fun resetAuthentication() {
        login.reset()
    }

    fun deleteAccount() {
        login.delete()
    }

    fun openImportPage() {
        onboarding.confirm()
    }

    fun startImportFromSource(activity: Context, source: Source) {
        onboarding.import(activity, source)
    }

    fun cancelImport() {
        onboarding.cancel()
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
