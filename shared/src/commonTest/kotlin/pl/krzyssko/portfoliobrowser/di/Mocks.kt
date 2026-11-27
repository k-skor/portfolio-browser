package pl.krzyssko.portfoliobrowser.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.EmailProviderId
import pl.krzyssko.portfoliobrowser.auth.GitHubProviderId
import pl.krzyssko.portfoliobrowser.auth.toProviderId
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.data.UserInfo
import pl.krzyssko.portfoliobrowser.platform.ConfigurationData
import pl.krzyssko.portfoliobrowser.platform.ConfigurationSaver

class InMemoryConfigurationSaver(
    initialData: ConfigurationData = ConfigurationData()
) : ConfigurationSaver {
    private var storedData: ConfigurationData = initialData

    override val data: ConfigurationData
        get() = restore()

    override fun save(data: ConfigurationData) {
        storedData = data
    }

    override fun restore(defaults: ConfigurationData?): ConfigurationData = ConfigurationData(
        gitHubApiUser = storedData.gitHubApiUser.takeIf { !it.isNullOrBlank() } ?: defaults?.gitHubApiUser,
        gitHubApiToken = storedData.gitHubApiToken.takeIf { !it.isNullOrBlank() } ?: defaults?.gitHubApiToken,
        lastSignInMethod = storedData.lastSignInMethod.takeIf { !it.isNullOrBlank() } ?: defaults?.lastSignInMethod,
        azureSearchEndpoint = storedData.azureSearchEndpoint.takeIf { !it.isNullOrBlank() } ?: defaults?.azureSearchEndpoint,
        azureSearchIndex = storedData.azureSearchIndex.takeIf { !it.isNullOrBlank() } ?: defaults?.azureSearchIndex,
        azureTenantId = storedData.azureTenantId.takeIf { !it.isNullOrBlank() } ?: defaults?.azureTenantId,
        azureClientId = storedData.azureClientId.takeIf { !it.isNullOrBlank() } ?: defaults?.azureClientId,
        azureClientSecret = storedData.azureClientSecret.takeIf { !it.isNullOrBlank() } ?: defaults?.azureClientSecret,
        azureApiToken = storedData.azureApiToken.takeIf { !it.isNullOrBlank() } ?: defaults?.azureApiToken,
        azureApiRefreshToken = storedData.azureApiRefreshToken.takeIf { !it.isNullOrBlank() } ?: defaults?.azureApiRefreshToken,
        azureScope = storedData.azureScope.takeIf { !it.isNullOrBlank() } ?: defaults?.azureScope
    )

    override fun clear() {
        storedData = ConfigurationData()
    }

    override fun update(data: ConfigurationData) {
        save(data)
    }
}

/**
 * Stateful in-memory [Auth] fake designed for testing reactive flows (e.g. [userState] -> Onboarding)
 * as well as sign-in / sign-out / delete state transitions without constructor parameter explosion.
 */
class FakeAuth(
    initialAccount: Account? = null,
    initialProviders: List<Provider> = emptyList()
) : Auth {
    private val _userState = MutableStateFlow(initialAccount?.toUserInfo())
    override val userState: StateFlow<UserInfo?> = _userState.asStateFlow()

    override var requestedLoginMethod: Auth.LoginMethod? = null
        private set

    var currentAccount: Account? = initialAccount
        private set

    var providers: List<Provider> = initialProviders

    /**
     * Optional hook to script [startSignInFlow] outcomes (success or exception) in a specific test.
     * When null, defaults to signing in with [currentAccount] if present.
     */
    var onSignIn: (suspend (Auth.LoginMethod, Boolean) -> User.Authenticated?)? = null

    override val isUserSignedIn: Boolean
        get() = currentAccount != null

    override val userAccount: Account?
        get() = currentAccount

    override val userInfo: UserInfo?
        get() = _userState.value

    override val providerData: List<Provider>
        get() = providers

    override val hasGitHubProvider: Boolean
        get() = providers.any { it.providerId == GitHubProviderId }

    override val hasEmailProvider: Boolean
        get() = providers.any { it.providerId == EmailProviderId }

    override fun shouldLinkAccounts(providerType: Auth.LoginMethod): Boolean =
        isUserSignedIn && providers.none { it.providerId == providerType.toProviderId() }

    fun setSignedIn(account: Account, providers: List<Provider> = this.providers) {
        this.currentAccount = account
        this.providers = providers
        _userState.value = account.toUserInfo()
    }

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
        requestedLoginMethod = providerType
        val result = onSignIn?.invoke(providerType, linkWithProvider)
            ?: currentAccount?.let { User.Authenticated(it) }
        if (result != null) {
            setSignedIn(result.account)
        } else if (providerType == Auth.LoginMethod.Anonymous) {
            setSignedIn(Account("anon-uid", null, "", null, isEmailVerified = false, anonymous = true))
        }
        return result
    }

    override fun signOut() {
        currentAccount = null
        providers = emptyList()
        _userState.value = null
    }

    override suspend fun delete() {
        signOut()
    }

    private fun Account.toUserInfo(): UserInfo = UserInfo(id, anonymous)
}
