package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.User
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
        linkWithProvider: Boolean = false
    ): User.Authenticated? {

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

expect fun Auth.LoginMethod.toProviderId(): String

expect fun getPlatformAuth(configuration: Configuration): Auth