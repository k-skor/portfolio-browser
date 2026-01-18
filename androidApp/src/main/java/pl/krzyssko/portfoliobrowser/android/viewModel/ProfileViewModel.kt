package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import pl.krzyssko.portfoliobrowser.business.UserOnboardingImportFromExternalSource
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_PROFILE
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.authenticateAnonymous
import pl.krzyssko.portfoliobrowser.store.authenticateWithEmail
import pl.krzyssko.portfoliobrowser.store.authenticateWithGitHub
import pl.krzyssko.portfoliobrowser.store.createAccount
import pl.krzyssko.portfoliobrowser.store.deleteAccount
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

    val store: OrbitStore<ProfileState> by inject(NAMED_PROFILE) {
        parametersOf(
            viewModelScope,
            ProfileState.Initialized
        )
    }

    val stateFlow = store.stateFlow
    val userOnboarding = UserOnboardingImportFromExternalSource(viewModelScope, stateFlow, auth, firestore)
    val sideEffectsFlow = merge(store.sideEffectFlow, userOnboarding.sideEffectsFlow)


    val profileState: StateFlow<Response<Profile>> = stateFlow
        .map {
            when (it) {
                is ProfileState.ProfileCreated -> Response.Ok(it.profile)
                is ProfileState.Error -> Response.Error(it.reason)
                else -> null
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Response.Pending)

    val userState: StateFlow<Response<User>> = stateFlow
        .map {
            when (it) {
                is ProfileState.Authenticated -> Response.Ok(it.user)
                is ProfileState.Error -> Response.Error(it.reason)
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
        store.profile {
            initAuth(auth, config, firestore, userOnboarding)
        }
        viewModelScope.launch {
            userOnboarding.stateFlow.collect {
                Log.d(TAG, "collect: has state=$it")
            }
        }
    }

    fun createUser(activity: Context, login: String, password: String) {
        store.profile {
            createAccount(activity, auth, login, password, firestore, config)
        }
    }

    fun authenticateGuest() {
        store.profile {
            authenticateAnonymous(auth, firestore, config)
        }
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false) {
        store.profile {
            authenticateWithGitHub(activity, auth, config, repository, firestore, userOnboarding, refreshOnly, forceSignIn)
        }
    }

    fun authenticateUser(activity: Context, login: String, password: String) {
        store.profile {
            authenticateWithEmail(activity, auth, login, password, firestore, config)
        }
    }

    fun linkUser(activity: Context) {
        store.profile {
            linkWithGitHub(activity, auth, config, repository)
        }
    }

    fun resetAuthentication() {
        store.profile {
            resetAuth(auth, config)
        }
    }

    fun openImportPage() {
        userOnboarding.openImport()
    }

    fun startImportFromSource(activity: Context) {
        userOnboarding.startImportFromSource(activity)
    }

    fun deleteAccount() {
        store.profile {
            deleteAccount(auth, config)
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
