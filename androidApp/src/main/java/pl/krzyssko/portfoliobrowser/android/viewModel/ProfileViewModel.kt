package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.auth.Auth
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
import pl.krzyssko.portfoliobrowser.store.initAuth
import pl.krzyssko.portfoliobrowser.store.linkWithGitHub
import pl.krzyssko.portfoliobrowser.store.profile
import pl.krzyssko.portfoliobrowser.store.resetAuth

class ProfileViewModel(
    private val repository: ProjectRepository,
    private val auth: Auth,
    private val firestore: Firestore,
    private val config: Configuration,
    private val logging: Logging
) : ViewModel(), KoinComponent {

    private val store: OrbitStore<ProfileState> by inject(NAMED_PROFILE) {
        parametersOf(
            viewModelScope,
            ProfileState.Created
        )
    }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    val userState = stateFlow.onEach { logging.debug("USER STATE=${it}") }.map {
        when(it) {
            is ProfileState.Authenticated -> it.user
            is ProfileState.Initialized -> User.Guest
            else -> null
        }
    }.filterNotNull().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), User.Guest)

    val profileState = stateFlow.map {
        when(it) {
            is ProfileState.ProfileCreated -> it.profile
            else -> null
        }
    }.filterNotNull().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Profile())

    val isSourceAvailable = stateFlow.onEach {
        logging.debug("source state=$it")
    }.map {
        it is ProfileState.SourceAvailable
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // TODO: move to store initializer block
        //initAuthentication()
        with(store) {
            profile {
                initAuth(auth, config, firestore)
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
            authenticateWithGitHub(activity, auth, config, repository, firestore, refreshOnly)
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
}