package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class Auth {

    lateinit var requestedLoginMethod: LoginMethod

    enum class LoginMethod {
        GitHub,
        Email
    }

    suspend fun startSignInFlow(uiHandler: Any?, providerType: LoginMethod, create: Boolean = false, login: String? = null, password: String? = null, refresh: Boolean = false, token: String? = null, linkWithProvider: Boolean = false) = suspendCoroutine { continuation ->
        val callback = createCallbackWithContinuation(
            onSuccess = continuation::resume,
            onFailure = continuation::resumeWithException
        )
        //if (linkWithProvider) {
        //    if (isUserSignedIn) {
        //        if (providerType == AccountType.GitHub && login != null && password != null) {
        //            linkWithProvider(login, password, callback)
        //            return@suspendCoroutine
        //        }
        //        callback.onSuccess(null)
        //        return@suspendCoroutine
        //    }
        //}
        if (providerType == LoginMethod.Email && login != null && password != null) {
            if (create) {
                createWithEmail(uiHandler, login, password, callback)
            } else {
                signInWithEmail(uiHandler, login, password, callback)
            }
            requestedLoginMethod = LoginMethod.Email
        } else if (providerType == LoginMethod.GitHub) {
            if (linkWithProvider) {
                signInLinkWithGitHub(uiHandler, callback)
            } else {
                signInWithGitHub(uiHandler, token, refresh, callback)
            }
            requestedLoginMethod = LoginMethod.GitHub
        } else {
            callback.onFailure(Error("Invalid arguments to sign in flow"))
        }
    }

    interface LoginFlowCallback {
        fun onSuccess(profile: User.Authenticated?)
        fun onFailure(error: Throwable)
    }

    private fun createCallbackWithContinuation(
        onSuccess: (User.Authenticated?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) = object : LoginFlowCallback {
        // Resume the coroutine with the result
        override fun onSuccess(profile: User.Authenticated?) = onSuccess(profile)

        // Resume the coroutine with an exception
        override fun onFailure(error: Throwable) = onFailure(error)
    }

    abstract val isUserSignedIn: Boolean
    abstract val userProfile: Account?
    abstract val providerData: List<pl.krzyssko.portfoliobrowser.data.Provider>?
    abstract var accessToken: String?
    abstract val hasGitHubProvider: Boolean

    abstract fun initAuth()
    abstract fun signInWithGitHub(uiHandler: Any?, token: String?, refresh: Boolean = false, callback: LoginFlowCallback)
    abstract fun signInLinkWithGitHub(uiHandler: Any?, callback: LoginFlowCallback)
    abstract fun createWithEmail(uiHandler: Any?, login: String, password: String, callback: LoginFlowCallback)
    abstract fun signInWithEmail(uiHandler: Any?, login: String, password: String, callback: LoginFlowCallback)
    abstract fun linkWithProvider(token: String, callback: LoginFlowCallback)
    abstract fun linkWithProvider(login: String, password: String, callback: LoginFlowCallback)
    abstract fun signOut()
}

expect fun String.toLoginMethod(): Auth.LoginMethod

expect fun getPlatformAuth(): Auth