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
import pl.krzyssko.portfoliobrowser.business.UserOnboardingProfileInitialization
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
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.UserOnboardingProfileState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.store.authenticateAnonymous
import pl.krzyssko.portfoliobrowser.store.authenticateWithEmail
import pl.krzyssko.portfoliobrowser.store.authenticateWithGitHub
import pl.krzyssko.portfoliobrowser.store.createAccount
import pl.krzyssko.portfoliobrowser.store.deleteAccount
import pl.krzyssko.portfoliobrowser.store.fetchUserProfile
import pl.krzyssko.portfoliobrowser.store.initAuth
import pl.krzyssko.portfoliobrowser.store.linkWithGitHub
import pl.krzyssko.portfoliobrowser.store.resetAuth
import pl.krzyssko.portfoliobrowser.util.Response

class ProfileViewModel(
    private val repository: ProjectRepository,
    private val auth: Auth,
    private val firestore: Firestore,
    private val config: Configuration,
    private val logging: Logging
) : ViewModel(), KoinComponent {

    val loginStore: OrbitStore<LoginState> by inject(NAMED_LOGIN) {
        parametersOf(
            viewModelScope,
            LoginState.Initialized
        )
    }

    val profileStore: OrbitStore<ProfileState> by inject(NAMED_PROFILE) {
        parametersOf(
            viewModelScope,
            ProfileState.Initialized
        )
    }

    lateinit var projectsImportOnboarding: UserOnboardingProjectsImport
    lateinit var profileCreationOnboarding: UserOnboardingProfileInitialization

    private val _sideEffectsFlow = MutableSharedFlow<UserSideEffects>()
    val sideEffectsFlow: SharedFlow<UserSideEffects> = _sideEffectsFlow

    val profileState: StateFlow<Response<Profile>> = profileStore.stateFlow
        .map {
            when (it) {
                is ProfileState.Initialized -> Response.Pending
                is ProfileState.ProfileCreated -> Response.Ok(it.profile)
                is ProfileState.Error -> Response.Error(it.reason)
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Response.Pending)

    val userState: StateFlow<Response<User>> = loginStore.stateFlow
        .map {
            when (it) {
                is LoginState.Initialized -> Response.Pending
                is LoginState.Authenticated -> Response.Ok(it.user)
                is LoginState.Error -> Response.Error(it.reason)
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Response.Pending)

    init {
        // TODO: move to store initializer block
        loginStore.initAuth(auth)

        handleLoginOnboarding()
        handleProfileOnboarding()
    }

    fun createUser(activity: Context, login: String, password: String) {
        loginStore.createAccount(activity, auth, login, password, config)
    }

    fun authenticateGuest() {
        loginStore.authenticateAnonymous(auth, config)
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false) {
        loginStore.authenticateWithGitHub(activity, auth, config, repository, refreshOnly, forceSignIn)

    }

    fun authenticateUser(activity: Context, login: String, password: String) {
        loginStore.authenticateWithEmail(activity, auth, login, password, config)

    }

    fun linkUser(activity: Context) {
        loginStore.linkWithGitHub(activity, auth, config, repository)

    }

    fun resetAuthentication() {
        loginStore.resetAuth(auth, config)

    }

    fun deleteAccount() {
        loginStore.deleteAccount(auth, config)

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

    private fun handleLoginOnboarding() {
        loginStore.stateFlow.onEach {
            when {
                it is LoginState.Authenticated -> {
                    profileCreationOnboarding = UserOnboardingProfileInitialization(
                        viewModelScope,
                        Dispatchers.IO,
                        it.user as User.Authenticated,
                        firestore
                    )
                    publishSideEffects(profileCreationOnboarding.sideEffectFlow)
                    handleProfileCreationOnboarding()
                }
            }
            nextOnboardingAction()
        }
            .launchIn(viewModelScope)

        publishSideEffects(loginStore.sideEffectFlow)
    }

    private fun handleProfileOnboarding() {
        profileStore.stateFlow.onEach {
            when (it) {
                is ProfileState.ProfileCreated -> {
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

        publishSideEffects(profileStore.sideEffectFlow)
    }

    private fun handleProfileCreationOnboarding() {
        profileCreationOnboarding.stateFlow.onEach {
            nextOnboardingAction()
        }
            .launchIn(viewModelScope)

    }

    fun nextOnboardingAction() {
        if (loginStore.stateFlow.value !is LoginState.Authenticated) {
            return
        }
        when {
            profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.NotCreated -> {
                profileCreationOnboarding.check()
            }
            profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.FirstTimeCreation -> {
                profileCreationOnboarding.create()
            }
            profileStore.stateFlow.value is ProfileState.ProfileCreated -> {
                projectsImportOnboarding.start()
            }
            profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.AlreadyCreated -> {
                profileStore.fetchUserProfile(auth.userAccount!!, firestore)
            }
        }
    }

    fun profileCreated() {
        profileStore.fetchUserProfile(auth.userAccount!!, firestore)
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
