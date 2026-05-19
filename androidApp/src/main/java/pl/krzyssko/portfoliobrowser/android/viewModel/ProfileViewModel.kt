package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import org.koin.core.component.KoinComponent
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel : ViewModel(), KoinComponent {

    val login: Login by inject {
        parametersOf(viewModelScope)
    }
    val onboarding: Onboarding by inject {
        parametersOf(viewModelScope)
    }

    val userProfile: UserProfile by inject {
        parametersOf(viewModelScope)
    }

    val isSignedIn: Flow<Boolean>
        get() = login.stateFlow.toUser().mapLatest { it is User.Authenticated }
    val profile: Flow<Profile?>
        get() = userProfile.stateFlow.toProfile()

    fun welcome() {
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
