package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
import pl.krzyssko.portfoliobrowser.store.authenticateAnonymous
import pl.krzyssko.portfoliobrowser.store.authenticateWithEmail
import pl.krzyssko.portfoliobrowser.store.authenticateWithGitHub
import pl.krzyssko.portfoliobrowser.store.createAccount
import pl.krzyssko.portfoliobrowser.store.deleteAccount
import pl.krzyssko.portfoliobrowser.store.fetchUserProfile
import pl.krzyssko.portfoliobrowser.store.initAuth
import pl.krzyssko.portfoliobrowser.store.linkWithGitHub
import pl.krzyssko.portfoliobrowser.store.profile
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

    val projectsImportOnboardingStep by lazy {
        UserOnboardingProjectsImport(viewModelScope, Dispatchers.IO, auth, firestore)
    }
    val profileCreationOnboardingStep by lazy {
        UserOnboardingProfileInitialization(viewModelScope, Dispatchers.IO, firestore)
    }

    val sideEffectsFlow = merge(loginStore.sideEffectFlow, projectsImportOnboardingStep.sideEffectFlow)

    val profileState: StateFlow<Response<Profile>> = profileStore.stateFlow
        .map {
            when (it) {
                is ProfileState.ProfileCreated -> Response.Ok(it.profile)
                is ProfileState.Error -> Response.Error(it.reason)
                else -> null
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Response.Pending)

    val userState: StateFlow<Response<User>> = loginStore.stateFlow
        .map {
            when (it) {
                is LoginState.Authenticated -> Response.Ok(it.user)
                is LoginState.Error -> Response.Error(it.reason)
                else -> null
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Response.Pending)

    //val errorFlow: Flow<Throwable?> = stateFlow
    //    .map {
    //        when (it) {
    //            is ProfileState.Error -> it.reason?.let { reason -> throw reason }
    //            else -> null
    //        }
    //    }

    init {
        // TODO: move to store initializer block
        loginStore.profile {
            initAuth(auth, config, firestore)
        }
        viewModelScope.launch {
            loginStore.stateFlow.collect {
                when (it) {
                    is LoginState.Authenticated -> {
                        (it.user as? User.Authenticated)?.account?.let { account ->
                            profileCreationOnboardingStep.start(account)
                            profileStore.fetchUserProfile(account, firestore)
                        }
                    }
                    is LoginState.ProfileCreated -> {
                        projectsImportOnboardingStep.start()
                    }
                    else -> {}
                }
            }
        }
    }

    fun createUser(activity: Context, login: String, password: String) {
        loginStore.profile {
            createAccount(activity, auth, login, password, firestore, config)
        }
    }

    fun authenticateGuest() {
        loginStore.profile {
            authenticateAnonymous(auth, firestore, config)
        }
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false) {
        loginStore.profile {
            authenticateWithGitHub(activity, auth, config, repository, firestore, projectsImportOnboardingStep, refreshOnly, forceSignIn)
        }
    }

    fun authenticateUser(activity: Context, login: String, password: String) {
        loginStore.profile {
            authenticateWithEmail(activity, auth, login, password, firestore, config)
        }
    }

    fun linkUser(activity: Context) {
        loginStore.profile {
            linkWithGitHub(activity, auth, config, repository)
        }
    }

    fun resetAuthentication() {
        loginStore.profile {
            resetAuth(auth, config)
        }
    }

    fun deleteAccount() {
        loginStore.profile {
            deleteAccount(auth, config)
        }
    }

    fun openImportPage() {
        projectsImportOnboardingStep.confirm()
    }

    fun startImportFromSource(activity: Context, source: Source) {
        projectsImportOnboardingStep.import(activity, source)
    }

    fun cancelImport() {
        projectsImportOnboardingStep.cancel()
    }

    fun createProfile() {
        ((userState.value as? Response.Ok)?.data as? User.Authenticated)?.let {
            profileCreationOnboardingStep.create(it.account)
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
