package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.platform.Config
import pl.krzyssko.portfoliobrowser.platform.Configuration

class AuthInvalidUserException(throwable: Throwable?) : Exception("Invalid user.", throwable)
class AuthPasswordTooWeakException(throwable: Throwable?) : Exception("Password is too weak.", throwable)
class AuthAccountExistsException(throwable: Throwable) : Exception("Cannot link accounts because the account already exists.", throwable)

abstract class Auth(protected val config: Configuration) {

    var requestedLoginMethod: LoginMethod? = null
        protected set

    enum class LoginMethod {
        Anonymous,
        GitHub,
        Email
    }

    protected val oauthToken: String?
        get() = config.config.gitHubApiToken

    //fun shouldLinkAccounts(providerType: LoginMethod) = isUserSignedIn && providerData?.map { it.providerId.toLoginMethod() }?.contains(providerType) ?: true
    fun shouldLinkAccounts(providerType: LoginMethod) = isUserSignedIn && providerData?.any { it.providerId == providerType.toProviderId() } == false

    open suspend fun startSignInFlow(
        uiHandler: Any?,
        providerType: LoginMethod,
        create: Boolean = false,
        login: String? = null,
        password: String? = null,
        refresh: Boolean = false,
        token: String? = oauthToken,
        //linkWithProvider: Boolean = shouldLinkAccounts(providerType)
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

        return if (linkWithProvider) {
            when {
                providerType == LoginMethod.Email && login != null && password != null && !hasEmailProvider -> linkWithProvider(login, password)
                providerType == LoginMethod.GitHub && !hasGitHubProvider -> signInLinkWithGitHub(uiHandler)
                else -> null
            }
        } else {
            when {
                providerType == LoginMethod.Anonymous -> signInAnonymous()
                providerType == LoginMethod.Email && login != null && password != null ->
                    if (create) createWithEmail(uiHandler, login, password)
                    else signInWithEmail(uiHandler, login, password)
                providerType == LoginMethod.GitHub -> signInWithGitHub(uiHandler, token, refresh)
                else -> null
            }
        } as? User.Authenticated

        //return (if (providerType == LoginMethod.Anonymous) {
        //    signInAnonymous()
        //    //requestedLoginMethod = LoginMethod.Anonymous
        //} else if (providerType == LoginMethod.Email && login != null && password != null) {
        //    if (linkWithProvider) {
        //        linkWithProvider(login, password)
        //    } else if (create) {
        //        createWithEmail(uiHandler, login, password)
        //    } else {
        //        signInWithEmail(uiHandler, login, password)
        //    }
        //    //requestedLoginMethod = LoginMethod.Email
        //} else if (providerType == LoginMethod.GitHub) {
        //    if (linkWithProvider) {
        //        signInLinkWithGitHub(uiHandler)
        //    } else {
        //        signInWithGitHub(uiHandler, token, refresh)
        //    }
        //    //requestedLoginMethod = LoginMethod.GitHub
        //} else {
        //    null
        //}) as? User.Authenticated
    }

    abstract val isUserSignedIn: Boolean
    abstract val userAccount: Account?
    abstract val providerData: List<pl.krzyssko.portfoliobrowser.data.Provider>?
    //abstract var accessToken: String?
    abstract val hasGitHubProvider: Boolean
    abstract val hasEmailProvider: Boolean

    abstract fun initAuth()
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

//expect fun String.toLoginMethod(): Auth.LoginMethod
expect fun Auth.LoginMethod.toProviderId(): String

expect fun getPlatformAuth(configuration: Configuration): Auth