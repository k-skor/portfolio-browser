package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Config
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_LIST
import pl.krzyssko.portfoliobrowser.di.NAMED_PROFILE
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
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

    init {
        //viewModelScope.launch {
        //    stateFlow.filter { it is ProfileState.Authenticated }.map { it as ProfileState.Authenticated }.collectLatest {
        //        logging.debug("AUTH user=${it.user.profile.id}, token=${it.user.token}")
        //        val user = it.user.profile.id
        //        val token = it.user.token
        //        if (user.isNotEmpty()) {
        //            config.config = Config(user, token.orEmpty())
        //            firestore.createUser(user, it.user.profile)
        //        }
        //    }
        //}
        // TODO: move to store initializer block
        initAuthentication()
    }

    fun initAuthentication() {
        store.profile {
            initAuth(auth)
        }
    }

    fun createUser(activity: Context, login: String, password: String) {
        store.profile {
            createAccount(activity, auth, login, password)
        }
    }

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false) {
        store.profile {
            authenticateWithGitHub(activity, auth, repository, config, refreshOnly)
        }
    }

    fun authenticateUser(activity: Context, login: String, password: String) {
        store.profile {
            authenticateWithEmail(activity, auth, login, password)
        }
    }

    fun linkUser(activity: Context) {
        store.profile {
            linkWithGitHub(activity, auth, repository, config, firestore)
        }
    }

    fun resetAuthentication() {
        store.profile {
            resetAuth(auth)
        }
    }
}