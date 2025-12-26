package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Config
import pl.krzyssko.portfoliobrowser.data.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

enum class Reason {
    PasswordTooWeak,
    InvalidUser
}

class AuthFailedException(reason: Reason) : Exception()
class AuthAccountExistsException(message: String) : Exception(message)

abstract class Auth {

    var requestedLoginMethod: LoginMethod? = null
        protected set

    enum class LoginMethod {
        Anonymous,
        GitHub,
        Email
    }

    fun shouldLinkAccounts(providerType: LoginMethod) = isUserSignedIn && providerData?.map { it.providerId.toLoginMethod() }?.contains(providerType) ?: true

    open suspend fun startSignInFlow(
        uiHandler: Any?,
        providerType: LoginMethod,
        create: Boolean = false,
        login: String? = null,
        password: String? = null,
        refresh: Boolean = false,
        token: String? = null,
        linkWithProvider: Boolean = false
    ): User.Authenticated? {
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
        return (if (providerType == LoginMethod.Anonymous) {
            signInAnonymous()
            //requestedLoginMethod = LoginMethod.Anonymous
        } else if (providerType == LoginMethod.Email && login != null && password != null) {
            if (linkWithProvider) {
                linkWithProvider(login, password)
            } else if (create) {
                createWithEmail(uiHandler, login, password)
            } else {
                signInWithEmail(uiHandler, login, password)
            }
            //requestedLoginMethod = LoginMethod.Email
        } else if (providerType == LoginMethod.GitHub) {
            if (linkWithProvider) {
                signInLinkWithGitHub(uiHandler)
            } else {
                signInWithGitHub(uiHandler, token, refresh)
            }
            //requestedLoginMethod = LoginMethod.GitHub
        } else {
            null
        }) as? User.Authenticated
    }

    abstract val isUserSignedIn: Boolean
    abstract val userProfile: Account?
    abstract val providerData: List<pl.krzyssko.portfoliobrowser.data.Provider>?
    abstract var accessToken: String?
    abstract val hasGitHubProvider: Boolean

    abstract fun initAuth(config: Config? = null)
    protected abstract suspend fun signInAnonymous(): User?
    protected abstract suspend fun signInWithGitHub(uiHandler: Any?, token: String?, refresh: Boolean = false): User?
    protected abstract suspend fun signInLinkWithGitHub(uiHandler: Any?): User?
    protected abstract suspend fun createWithEmail(uiHandler: Any?, login: String, password: String): User?
    protected abstract suspend fun signInWithEmail(uiHandler: Any?, login: String, password: String): User?
    protected abstract suspend fun linkWithProvider(token: String): User?
    protected abstract suspend fun linkWithProvider(login: String, password: String): User?
    abstract fun signOut()
    abstract suspend fun delete()
}

expect fun String.toLoginMethod(): Auth.LoginMethod

expect fun getPlatformAuth(): Auth