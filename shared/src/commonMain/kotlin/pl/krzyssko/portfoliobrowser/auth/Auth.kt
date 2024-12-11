package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class Auth {

    suspend fun startSignInFlow(uiHandler: Any?, refresh: Boolean = false) = suspendCoroutine { continuation ->
        val callback = createCallbackWithContinuation(
            onSuccess = continuation::resume,
            onFailure = continuation::resumeWithException
        )
        signInWithGitHub(uiHandler, refresh, callback)
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

    abstract fun initAuth()
    abstract fun isUserSignedIn(): Boolean
    abstract fun getUserProfile(): Profile?
    abstract fun getAccessToken(): String
    abstract fun signInWithGitHub(uiHandler: Any?, refresh: Boolean = false, callback: LoginFlowCallback)
    abstract fun signOut()
}

expect fun getPlatformAuth(): Auth