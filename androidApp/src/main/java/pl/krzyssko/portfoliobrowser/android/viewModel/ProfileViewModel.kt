package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.business.Login
import pl.krzyssko.portfoliobrowser.business.Onboarding
import pl.krzyssko.portfoliobrowser.business.UserProfile
import pl.krzyssko.portfoliobrowser.business.toProfile
import pl.krzyssko.portfoliobrowser.business.toUser
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.OnboardingState

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel : ViewModel(), KoinComponent {

    val login: Login by inject {
        parametersOf(viewModelScope)
    }
    val onboarding: StateFlow<Onboarding?> by lazy {
        login.stateFlow
            .map { loginState ->
                if (loginState is LoginState.Authenticated) {
                    val onboarding = get<Onboarding> { parametersOf(viewModelScope) }
                    onboarding
                } else {
                    null
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }
    val userProfile: StateFlow<UserProfile?> by lazy {
        combine(
            login.stateFlow.toUser().mapLatest { it as? User.Authenticated },
            onboarding.flatMapLatest { it?.stateFlow ?: flowOf(null) }
        ) { user, onboardingState ->
            if (onboardingState is OnboardingState.OnboardingCompleted && user != null) {
                val profile = get<UserProfile> { parametersOf(viewModelScope, user.account) }
                profile
            } else {
                null
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    val isSignedIn: Flow<Boolean>
        get() = login.stateFlow.toUser().mapLatest { it is User.Authenticated }
    val profile: Flow<Profile?>
        get() = userProfile.flatMapLatest { it?.stateFlow?.toProfile() ?: flowOf(null) }

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
        onboarding.value?.confirm()
    }

    fun startImportFromSource(activity: Context, source: Source) {
        onboarding.value?.import(activity, source)
    }

    fun cancelImport() {
        onboarding.value?.cancel()
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
