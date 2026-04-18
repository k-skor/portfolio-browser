package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    //private val repository: ProjectRepository,
    //private val auth: Auth,
    //private val firestore: Firestore,
    //private val config: Configuration,
    //private val logging: Logging
) : ViewModel(), KoinComponent {

    //lateinit var projectsImportOnboarding: UserOnboardingProjectsImport
    //lateinit var profileCreationOnboarding: UserOnboardingProfileStubCreation

    //val accountLink: UserLoginAccountLink by inject {
    //    parametersOf(viewModelScope)
    //}
    //val userLogin: UserLogin by inject {
    //    parametersOf(viewModelScope)
    //}
    val login: Login by inject {
        parametersOf(viewModelScope)
    }
    val onboarding: Onboarding by inject {
        parametersOf(viewModelScope)
    }
    val profile: UserProfile by inject {
        parametersOf(viewModelScope)
    }

    //private val _sideEffectsFlow = MutableSharedFlow<UserSideEffects>()
    //val sideEffectsFlow: SharedFlow<UserSideEffects> = _sideEffectsFlow
    //val sideEffectsFlow: Flow<UserSideEffects>
    //    get() = login.sideEffectFlow
    //val onboardingSideEffects: Flow<UserSideEffects>
    //    get() = onboarding.sideEffectFlow
    //val profileSideEffects: Flow<UserSideEffects>
    //    get() = profile.sideEffectFlow

    //val loginState: StateFlow<LoginState>
    //    get() = login.stateFlow
    //val onboardingState: StateFlow<OnboardingState>
    //    get() = onboarding.stateFlow
    //val userProfileState: StateFlow<ProfileState>
    //    get() = profile.stateFlow

    val userState: StateFlow<User> = login.userState.stateIn(viewModelScope, SharingStarted.Lazily, User.Guest)
    //= login.userState
    //    .stateIn(viewModelScope, SharingStarted.Lazily, User.Guest)

    val profileState: StateFlow<Profile> = profile.profileState.stateIn(viewModelScope, SharingStarted.Lazily, Profile.Stub)
    //= profile.profileState
    //    .stateIn(viewModelScope, SharingStarted.Lazily, Profile.Stub)

    init {
        //handleLoginState()
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
                        profile.fetch()
                    }
            }
            .launchIn(viewModelScope)
    }

    fun createUser(activity: Context, user: String, password: String) {
        //userLogin.login(activity, login, password, create = true)
        login.login(activity, user, password, create = true)
    }

    fun authenticateGuest() {
        //userLogin.login()
        login.login()
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false) {
        //userLogin.login(activity, refreshOnly, forceSignIn)
        login.login(activity, refreshOnly, forceSignIn)
    }

    fun authenticateUser(activity: Context, user: String, password: String) {
        //userLogin.login(activity, login, password)
        login.login(activity, user, password)
    }

    fun linkUser(activity: Context) {
        //accountLink.link(activity)
        login.link(activity)
    }

    fun resetAuthentication() {
        //userLogin.reset()
        login.reset()
    }

    fun deleteAccount() {
        //userLogin.delete()
        login.delete()
    }

    fun openImportPage() {
        //projectsImportOnboarding.confirm()
        onboarding.confirm()
    }

    fun startImportFromSource(activity: Context, source: Source) {
        //projectsImportOnboarding.import(activity, source)
        onboarding.import(activity, source)
    }

    fun cancelImport() {
        //projectsImportOnboarding.cancel()
        onboarding.cancel()
    }

    //private fun handleLoginState() {
    //    userLogin.stateFlow.onEach {
    //        when (it) {
    //            is LoginState.Authenticated -> {
    //                if (it.user is User.Authenticated) {
    //                    profileCreationOnboarding = UserOnboardingProfileStubCreation(
    //                        viewModelScope,
    //                        Dispatchers.IO,
    //                        it.user as User.Authenticated,
    //                        firestore
    //                    )
    //                    handleProfileCreationOnboarding()
    //                    handleProfileState()
    //                }
    //            }

    //            is LoginState.Error -> {
    //                if (it.reason is AuthAccountExistsException) {
    //                    handleAccountLink()
    //                }
    //            }

    //            else -> {}
    //        }
    //        nextOnboardingAction()
    //    }
    //        .launchIn(viewModelScope)

    //    publishSideEffects(userLogin.sideEffectFlow)
    //}

    //private fun handleProfileState() {
    //    profileEdition.stateFlow.onEach {
    //        when (it) {
    //            is ProfileState.Completed -> {
    //                projectsImportOnboarding = UserOnboardingProjectsImport(
    //                    viewModelScope,
    //                    Dispatchers.IO,
    //                    auth,
    //                    firestore
    //                )
    //                publishSideEffects(projectsImportOnboarding.sideEffectFlow)
    //            }
    //            else -> {}
    //        }
    //        nextOnboardingAction()
    //    }
    //        .launchIn(viewModelScope)

    //    publishSideEffects(profileEdition.sideEffectFlow)
    //}

    //private fun handleProfileCreationOnboarding() {
    //    profileCreationOnboarding.stateFlow.onEach {
    //        nextOnboardingAction()
    //    }
    //        .launchIn(viewModelScope)

    //    publishSideEffects(profileCreationOnboarding.sideEffectFlow)
    //}

    //private fun handleAccountLink() {
    //    accountLink.stateFlow.onEach {
    //        if (it is AccountMergeState.Success) {
    //            userLogin.completeLogin(it.user)
    //        }
    //    }.launchIn(viewModelScope)

    //    publishSideEffects(accountLink.sideEffectFlow)
    //}

    //val onboardingFlow = combine(
    //    userLogin.stateFlow,
    //    profileEdition.stateFlow,
    //) { user, profile ->

    //}.map {

    //}
    //    .onStart {  }

    //val onboarding = flow<Unit> {
    //    userLogin.stateFlow.collect {
    //        when (it) {
    //            is LoginState.Authenticated -> {
    //                if (it.user is User.Authenticated) {
    //                    profileCreationOnboarding = UserOnboardingProfileStubCreation(
    //                        viewModelScope,
    //                        Dispatchers.IO,
    //                        it.user as User.Authenticated,
    //                        firestore
    //                    )
    //                    //handleProfileCreationOnboarding()
    //                    profileCreationOnboarding.stateFlow.collect {
    //                        emit(Unit)
    //                    }
    //                    handleProfileState()
    //                }
    //            }

    //            is LoginState.Error -> {
    //                if (it.reason is AuthAccountExistsException) {
    //                    handleAccountLink()
    //                }
    //            }

    //            else -> {}
    //        }
    //    }
    //}

    //fun nextOnboardingAction() {
    //    if (userState.value !is User.Authenticated) {
    //        return
    //    }
    //    when {
    //        profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.NotCreated -> {
    //            profileCreationOnboarding.check()
    //        }
    //        profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.FirstTimeCreation -> {
    //            profileCreationOnboarding.create()
    //        }
    //        //profileEdition.stateFlow.value is ProfileState.Completed -> {
    //        projectsImportOnboarding.stateFlow.value is UserOnboardingImportState.Initialized -> {
    //            projectsImportOnboarding.start()
    //        }
    //        profileCreationOnboarding.stateFlow.value is UserOnboardingProfileState.AlreadyCreated -> {
    //            profileEdition.fetch(auth.userAccount!!)
    //        }
    //    }
    //}

    //fun profileCreated() {
    //    profileEdition.fetch()
    //}

    //private fun publishSideEffects(publishedEffectsFlow: Flow<UserSideEffects>) {
    //    viewModelScope.launch {
    //        _sideEffectsFlow.emitAll(publishedEffectsFlow)
    //    }
    //}

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
