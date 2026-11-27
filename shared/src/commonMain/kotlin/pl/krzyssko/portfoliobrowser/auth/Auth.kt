package pl.krzyssko.portfoliobrowser.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.data.UserInfo
import pl.krzyssko.portfoliobrowser.platform.Configuration

class AuthInvalidUserException(throwable: Throwable?) : Exception("Invalid user.", throwable)
class AuthPasswordTooWeakException(throwable: Throwable?) : Exception("Password is too weak.", throwable)
class AuthAccountExistsException(throwable: Throwable) : Exception("Cannot link accounts because the account already exists.", throwable)

expect val FirebaseProviderId: String
expect val GitHubProviderId: String
expect val EmailProviderId: String

interface Auth {
    val requestedLoginMethod: LoginMethod?

    enum class LoginMethod {
        Anonymous,
        GitHub,
        Email
    }

    val isUserSignedIn: Boolean
    val userAccount: Account?
    val userInfo: UserInfo?
    val providerData: List<Provider>?
    val hasGitHubProvider: Boolean
    val hasEmailProvider: Boolean
    val userState: StateFlow<UserInfo?>

    fun shouldLinkAccounts(providerType: LoginMethod): Boolean

    suspend fun startSignInFlow(
        uiHandler: Any?,
        providerType: LoginMethod,
        create: Boolean = false,
        login: String? = null,
        password: String? = null,
        refresh: Boolean = false,
        token: String? = null,
        linkWithProvider: Boolean = false
    ): User.Authenticated?

    fun signOut()
    suspend fun delete()
}

abstract class BaseAuth(protected val config: Configuration) : Auth {

    override var requestedLoginMethod: Auth.LoginMethod? = null
        protected set

    protected val oauthToken: String?
        get() = config.data.gitHubApiToken

    override fun shouldLinkAccounts(providerType: Auth.LoginMethod) =
        isUserSignedIn && providerData?.any { it.providerId == providerType.toProviderId() } == false

    override suspend fun startSignInFlow(
        uiHandler: Any?,
        providerType: Auth.LoginMethod,
        create: Boolean,
        login: String?,
        password: String?,
        refresh: Boolean,
        token: String?,
        linkWithProvider: Boolean
    ): User.Authenticated? {
        val resolvedToken = token ?: oauthToken
        return if (linkWithProvider) {
            when {
                providerType == Auth.LoginMethod.Email && login != null && password != null && !hasEmailProvider -> linkWithProvider(login, password)
                providerType == Auth.LoginMethod.GitHub && !hasGitHubProvider -> signInLinkWithGitHub(uiHandler)
                else -> null
            }
        } else {
            when {
                providerType == Auth.LoginMethod.Anonymous -> signInAnonymous()
                providerType == Auth.LoginMethod.Email && login != null && password != null ->
                    if (create) createWithEmail(uiHandler, login, password)
                    else signInWithEmail(uiHandler, login, password)
                providerType == Auth.LoginMethod.GitHub -> signInWithGitHub(uiHandler, resolvedToken, refresh)
                else -> null
            }
        } as? User.Authenticated
    }

    protected abstract fun userFlow(): Flow<UserInfo?>
    protected abstract suspend fun signInAnonymous(): User?
    protected abstract suspend fun signInWithGitHub(uiHandler: Any?, token: String?, refresh: Boolean = false): User?
    protected abstract suspend fun signInLinkWithGitHub(uiHandler: Any?): User?
    protected abstract suspend fun createWithEmail(uiHandler: Any?, login: String, password: String): User?
    protected abstract suspend fun signInWithEmail(uiHandler: Any?, login: String, password: String): User?
    protected abstract suspend fun linkWithProvider(token: String): User?
    protected abstract suspend fun linkWithProvider(login: String, password: String): User?
}

expect fun Auth.LoginMethod.toProviderId(): String

expect fun getPlatformAuth(configuration: Configuration): Auth