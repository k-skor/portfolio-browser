package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class Auth {

    lateinit var authenticationType: Provider

    enum class Provider {
        GitHub,
        Email
    }

    suspend fun startSignInFlow(uiHandler: Any?, provider: Provider, create: Boolean = false, login: String? = null, password: String? = null, refresh: Boolean = false, linkWithProvider: Boolean = false) = suspendCoroutine { continuation ->
        val callback = createCallbackWithContinuation(
            onSuccess = continuation::resume,
            onFailure = continuation::resumeWithException
        )
        if (provider == Provider.Email && login != null && password != null) {
            if (create) {
                createWithEmail(uiHandler, login, password, callback)
            } else {
                signInWithEmail(uiHandler, login, password, callback)
            }
        } else if (provider == Provider.GitHub) {
            if (linkWithProvider) {
                signInLinkWithGitHub(uiHandler, callback)
                //linkWithProvider("", callback)
            } else {
                signInWithGitHub(uiHandler, refresh, callback)
            }
        } else {
            callback.onFailure(Error("Invalid arguments to sign in flow"))
        }
    }

    interface LoginFlowCallback {
        fun onSuccess(profile: User?)
        fun onFailure(error: Throwable)
    }

    private fun createCallbackWithContinuation(
        onSuccess: (User?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) = object : LoginFlowCallback {
        // Resume the coroutine with the result
        override fun onSuccess(profile: User?) = onSuccess(profile)

        // Resume the coroutine with an exception
        override fun onFailure(error: Throwable) = onFailure(error)
    }

    abstract val isUserSignedIn: Boolean
    abstract val userProfile: Profile?
    abstract var accessToken: String?

    abstract fun initAuth()
    abstract fun signInWithGitHub(uiHandler: Any?, refresh: Boolean = false, callback: LoginFlowCallback)
    abstract fun signInLinkWithGitHub(uiHandler: Any?, callback: LoginFlowCallback)
    abstract fun createWithEmail(uiHandler: Any?, login: String, password: String, callback: LoginFlowCallback)
    abstract fun signInWithEmail(uiHandler: Any?, login: String, password: String, callback: LoginFlowCallback)
    abstract fun linkWithProvider(token: String, callback: LoginFlowCallback)
    abstract fun signOut()
}

expect fun getPlatformAuth(): Auth