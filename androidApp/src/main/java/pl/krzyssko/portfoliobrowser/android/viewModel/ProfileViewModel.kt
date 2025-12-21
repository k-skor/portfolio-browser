package pl.krzyssko.portfoliobrowser.android.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
import pl.krzyssko.portfoliobrowser.store.openImport
import pl.krzyssko.portfoliobrowser.store.profile
import pl.krzyssko.portfoliobrowser.store.resetAuth
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.exceptionAsResponse

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
            ProfileState.Created
        )
    }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    //val userState = stateFlow
    //    .onEach { logging.debug("USER STATE=${it}") }
    //    .map {
    //        when (it) {
    //            is ProfileState.Authenticated -> it.user
    //            is ProfileState.Initialized -> User.Guest
    //            else -> null
    //        }
    //    }
    //    .filterNotNull()
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), User.Guest)

    val profileState = stateFlow
        .map {
            when (it) {
                is ProfileState.Initialized -> Profile()
                is ProfileState.ProfileCreated -> it.profile
                else -> null
            }
        }
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Profile())

    //val isSourceAvailable = stateFlow
    //    .onEach { logging.debug("source state=$it") }
    //    .map { it is ProfileState.SourceAvailable }
    //    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    //val isSignedIn = userState.map { it is User.Authenticated }.stateIn(viewModelScope,
    //    SharingStarted.WhileSubscribed(5_000), false)


    //private fun Flow<ProfileState>.createProfileFlow(): Flow<ProfileState> =
    //    onEach {
    //        Log.d(TAG, "createProfileFlow")
    //        when (it) {
    //            is ProfileState.Authenticated -> {
    //                val user = it.user as? User.Authenticated ?: return@onEach
    //                store.getOrCreateProfile(user, firestore)
    //            }
    //            else -> return@onEach
    //        }
    //    }

    //private fun Flow<ProfileState>.checkImportFlow(): Flow<ProfileState> =
    //    onEach {
    //        Log.d(TAG, "checkImportFlow")
    //        when (it) {
    //            is ProfileState.Created -> {
    //                val user = userState.value as? User.Authenticated ?: return@onEach
    //                store.checkImport(user.account.id, firestore)
    //            }
    //            else -> return@onEach
    //        }
    //    }

    init {
        // TODO: move to store initializer block
        //initAuthentication()
        viewModelScope.launch {
            store.profile {
                initAuth(auth, config, firestore)
            }
        }
        with(store) {
            //initAuth(auth, config, firestore)

            // Background logic
            //viewModelScope.launch {
            //    stateFlow.collect {
            //        when (it) {
            //            is ProfileState.Authenticated -> getOrCreateProfile(it.user as? User.Authenticated ?: return@collect, firestore)
            //            else -> return@collect
            //        }
            //    }
            //}

            //viewModelScope.launch {
            //    stateFlow.collect {
            //        when (it) {
            //            is ProfileState.ProfileCreated -> checkImport((userState.value as? User.Authenticated)?.account?.id ?: return@collect, firestore)
            //            else -> return@collect
            //        }
            //    }
            //}
        }
    }

    fun createUser(activity: Context, login: String, password: String) {
        store.profile {
            createAccount(activity, auth, login, password, firestore, config)
        }
    }

    //fun createUserFlow(activity: Context, login: String, password: String): Flow<Response<User.Authenticated>> {
    //    return stateFlow
    //        .onSubscription {
    //            store.profile {
    //                createAccount(activity, auth, login, password, firestore, config)
    //            }
    //        }
    //        //.createProfileFlow()
    //        .map {
    //            when (it) {
    //                is ProfileState.Authenticated -> Response.Ok(it.user as User.Authenticated)
    //                is ProfileState.Error -> throw Error(it.reason)
    //                else -> null
    //            }
    //        }
    //        .filterNotNull()
    //        .catch {
    //            resetAuthentication()
    //            throw it
    //        }
    //        .errorAsResponse()
    //}

    fun authenticateGuest() {
        store.profile {
            authenticateAnonymous(auth, firestore, config)
        }
    }

    private val guestUserFlow: Flow<Response<User.Guest?>> = stateFlow
        .map {
            when (it) {
                //is ProfileState.Initialized -> Response.Ok(null)
                is ProfileState.Authenticated -> Response.Ok(it.user as? User.Guest)
                is ProfileState.Error -> Response.Error(it.reason)
                //is ProfileState.Error -> throw Exception(it.reason)
                else -> null
            }
        }
        //.filterNotNull()
        //.catch {
        //    resetAuthentication()
        //    throw it
        //}
        //.exceptionAsResponse()
        .filterNotNull()
        .exceptionAsResponse()
        .onStart {
            Log.d(TAG, "onStart: guest flow started!")
        }
        .onCompletion {
            Log.d(TAG, "onCompletion: guest flow completed!")
        }
        //.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Response.Ok(null))
        //.shareIn(viewModelScope, SharingStarted.Eagerly)

    //fun authenticateGuestFlow(): Flow<Response<User.Guest>> {
    //    return stateFlow
    //        .onSubscription {
    //            store.profile {
    //                authenticateAnonymous(auth, firestore, config)
    //            }
    //        }
    //        .map {
    //            when (it) {
    //                is ProfileState.Authenticated -> Response.Ok(it.user as User.Guest)
    //                is ProfileState.Error -> throw Error(it.reason)
    //                else -> null
    //            }
    //        }
    //        .filterNotNull()
    //        .catch {
    //            resetAuthentication()
    //            throw it
    //        }
    //        .errorAsResponse()
    //}

    fun authenticateUser(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false) {
        store.profile {
            authenticateWithGitHub(activity, auth, config, repository, firestore, refreshOnly)
        }
    }
    private val noneUserFlow: Flow<Response<User.None?>> = stateFlow
        .map {
            when (it) {
                is ProfileState.Initialized -> Response.Ok(User.None)
                is ProfileState.Error -> Response.Error(it.reason)
                else -> null
            }
        }
        .filterNotNull()
        .exceptionAsResponse()

    private val authenticatedUserFlow: Flow<Response<User.Authenticated?>> = stateFlow
        .map {
            when (it) {
                //is ProfileState.Initialized -> Response.Ok(null)
                is ProfileState.Authenticated -> Response.Ok(it.user as? User.Authenticated)
                //is ProfileState.Error -> Response.Error(it.reason)
                else -> null
            }
        }
        //.filterNotNull()
        //.catch {
        //    resetAuthentication()
        //    throw it
        //}
        .filterNotNull()
        .exceptionAsResponse()
        .onStart {
            Log.d(TAG, "onStart: auth flow started!")
        }
        .onCompletion {
            Log.d(TAG, "onCompletion: auth flow completed!")
        }
        //.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Response.Ok(null))
        //.shareIn(viewModelScope, SharingStarted.Eagerly)

    val isSignedIn: StateFlow<Boolean> = authenticatedUserFlow
        .map { it is Response.Ok }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val loginFlow: Flow<Response<User?>> = merge(
        authenticatedUserFlow,
        guestUserFlow,
        noneUserFlow
    )

    val userState: StateFlow<User> = loginFlow
        .map {
            when (it) {
                //is Response.Pending, is Response.Error -> User.None
                is Response.Ok -> it.data
                is Response.Error -> User.None
            }
        }
        //.onEach {
        //    when (it) {
        //        is User.None -> resetAuthentication()
        //        else -> {}
        //    }
        //}
        .filterNotNull()
        //.map { it ?: User.None }
        //.catch {
        //    Log.d(TAG, "catch: caught master exception")
        //    resetAuthentication()
        //    emit(User.None)
        //}
        .onStart {
            Log.d(TAG, "onStart: user flow started!")
        }
        .onCompletion {
            Log.d(TAG, "onCompletion: user flow completed!")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), User.None)

    val errorFlow: Flow<Throwable?> = loginFlow
        .map {
            when (it) {
                is Response.Error -> throw it.throwable ?: Error()
                else -> null
            }
        }

    val errorState: StateFlow<Throwable?> = stateFlow
        .map {
            when (it) {
                is ProfileState.Error -> it.reason
                else -> null
            }
        }
        //.onEach {
        //    Log.d(TAG, "onEach: error=$it")
        //    it?.let {
        //        resetAuthentication()
        //    }
        //}
        .onStart {
            Log.d(TAG, "onStart: error flow started!")
        }
        .onCompletion {
            Log.d(TAG, "onCompletion: error flow completed!")
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    //suspend fun authenticateUserFlow(activity: Context, refreshOnly: Boolean = false, forceSignIn: Boolean = false): StateFlow<Response<User.Authenticated>> {
    //    return stateFlow
    //        .onSubscription {
    //            Log.d(TAG, "authenticateUserFlow: on subscription!")
    //            store.profile {
    //                authenticateWithGitHub(activity, auth, config, repository, firestore, refreshOnly)
    //            }
    //        }
    //        //.createProfileFlow()
    //        //.checkImportFlow()
    //        .map {
    //            when (it) {
    //                is ProfileState.Authenticated -> Response.Ok(it.user as User.Authenticated)
    //                is ProfileState.Error -> throw Error(it.reason)
    //                else -> null
    //            }
    //        }
    //        .filterNotNull()
    //        .catch {
    //            resetAuthentication()
    //            throw it
    //        }
    //        .errorAsResponse()
    //        .stateIn(viewModelScope)
    //}

    fun authenticateUser(activity: Context, login: String, password: String) {
        store.profile {
            authenticateWithEmail(activity, auth, login, password, firestore, config)
        }
    }

    fun linkUser(activity: Context, user: User.Authenticated) {
        store.profile {
            linkWithGitHub(activity, auth, config, repository, user)
        }
    }

    fun resetAuthentication() {
        store.profile {
            resetAuth(auth, config)
        }
    }

    fun openImportPage() {
        store.profile {
            openImport()
        }
    }


    companion object {
        private const val TAG = "ProfileViewModel"
    }
}

//fun <T: ProfileState> ProfileViewModel.doOnState(block: OrbitStore<T>.() -> Unit) {
//    viewModelScope.launch {
//        stateFlow.collect {
//            when (it) {
//                is T -> 1//store.apply(block)
//                else -> return@collect
//            }
//        }
//    }
//    //store.apply(block)
//}