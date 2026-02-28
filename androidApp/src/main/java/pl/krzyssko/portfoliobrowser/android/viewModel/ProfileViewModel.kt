package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthAccountExistsException
import pl.krzyssko.portfoliobrowser.business.ProfileEdition
import pl.krzyssko.portfoliobrowser.business.UserLogin
import pl.krzyssko.portfoliobrowser.business.UserLoginAccountLink
import pl.krzyssko.portfoliobrowser.business.UserOnboardingProfileStubCreation
import pl.krzyssko.portfoliobrowser.business.UserOnboardingProjectsImport
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_LOGIN
import pl.krzyssko.portfoliobrowser.di.NAMED_PROFILE
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.AccountMergeState
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.UserOnboardingProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.getOrNull

class ProfileViewModel(
    private val repository: ProjectRepository,
    private val auth: Auth,
    private val firestore: Firestore,
    private val config: Configuration,
    private val logging: Logging
) : ViewModel(), KoinComponent {

    lateinit var projectsImportOnboarding: UserOnboardingProjectsImport
    lateinit var profileCreationOnboarding: UserOnboardingProfileStubCreation
    
    val accountLink: UserLoginAccountLink by inject {
        parametersOf(viewModelScope)
    }
    val userLogin: UserLogin by inject(NAMED_LOGIN) {
        parametersOf(viewModelScope)
    }
    val profileEdition: ProfileEdition by inject(NAMED_PROFILE) {
        parametersOf(viewModelScope)
    }

    private val _sideEffectsFlow = MutableSharedFlow<UserSideEffects>()
    val sideEffectsFlow: SharedFlow<UserSideEffects> = _sideEffectsFlow

    val userState: StateFlow<User> = userLogin.userState
        .map { it.getOrNull() }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, User.Guest)

    val profileState: StateFlow<Profile> = profileEdition.profileState
        .map { it.getOrNull() }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Profile.Stub)

    init {
        handleLoginState()
    }

    fun createUser(activity: Context, login: String, password: String) {
        userLogin.login(activity, login, password, create = true)
    }

    fun authenticateGuest() {
        userLogin.login()
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false) {
        userLogin.login(activity, refreshOnly, forceSignIn)
    }

    fun authenticateUser(activity: Context, login: String, password: String) {
        userLogin.login(activity, login, password)
    }

    fun linkUser(activity: Context) {
        accountLink.link(activity)
    }

    fun resetAuthentication() {
        userLogin.reset()
    }

    fun deleteAccount() {
        userLogin.delete()
    }

    fun openImportPage() {
        projectsImportOnboarding.confirm()
    }

    fun startImportFromSource(activity: Context, source: Source) {
        projectsImportOnboarding.import(activity, source)
    }

    fun cancelImport() {
        projectsImportOnboarding.cancel()
    }

    private fun handleLoginState() {
        userLogin.stateFlow.onEach {
            when (it) {
                is LoginState.Authenticated -> {
                    if (it.user is User.Authenticated) {
                        profileCreationOnboarding = UserOnboardingProfileStubCreation(
                            viewModelScope,
                            Dispatchers.IO,
                            it.user as User.Authenticated,
                            firestore
                        )
                        handleProfileCreationOnboarding()
                        handleProfileState()
                    }
                }

                is LoginState.Error -> {
                    if (it.reason is AuthAccountExistsException) {
                        handleAccountLink()
                    }
                }

                else -> {}
            }
            nextOnboardingAction()
        }
            .launchIn(viewModelScope)

        publishSideEffects(userLogin.sideEffectFlow)
    }

    private fun handleProfileState() {
        profileEdition.stateFlow.onEach {
            when (it) {
                is ProfileState.Loaded -> {
                    projectsImportOnboarding = UserOnboardingProjectsImport(
                        viewModelScope,
                        Dispatchers.IO,
                        auth,
                        firestore
                    )
                    publishSideEffects(projectsImportOnboarding.sideEffectFlow)
                }
                else -> {}
            }
            nextOnboardingAction()
        }
            .launchIn(viewModelScope)

        publishSideEffects(profileEdition.sideEffectFlow)
    }

    private fun handleProfileCreationOnboarding() {
        profileCreationOnboarding.stateFlow.onEach {
            nextOnboardingAction()
        }
            .launchIn(viewModelScope)

        publishSideEffects(profileCreationOnboarding.sideEffectFlow)
    }

    private fun handleAccountLink() {
        accountLink.stateFlow.onEach {
            if (it is AccountMergeState.Success) {
                userLogin.completeLogin(it.user)
            }
        }.launchIn(viewModelScope)

        publishSideEffects(accountLink.sideEffectFlow)
    }

    fun nextOnboardingAction() {
        if (userState.value !is User.Authenticated) {
            return
        }
        when {
            profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.NotCreated -> {
                profileCreationOnboarding.check()
            }
            profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.FirstTimeCreation -> {
                profileCreationOnboarding.create()
            }
            profileEdition.stateFlow.value is ProfileState.Loaded -> {
                projectsImportOnboarding.start()
            }
            profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.AlreadyCreated -> {
                profileEdition.fetch(auth.userAccount!!)
            }
        }
    }

    fun profileCreated() {
        profileEdition.fetch(auth.userAccount!!)
    }

    private fun publishSideEffects(publishedEffectsFlow: Flow<UserSideEffects>) {
        viewModelScope.launch {
            _sideEffectsFlow.emitAll(publishedEffectsFlow)
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
