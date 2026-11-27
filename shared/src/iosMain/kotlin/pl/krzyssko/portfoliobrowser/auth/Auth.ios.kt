package pl.krzyssko.portfoliobrowser.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.data.UserInfo
import pl.krzyssko.portfoliobrowser.platform.Configuration

actual val FirebaseProviderId: String = "firestore"
actual val GitHubProviderId: String = "github"
actual val EmailProviderId: String = "email"

class IosAuth(configuration: Configuration) : BaseAuth(configuration) {
    override val isUserSignedIn: Boolean
        get() = TODO("Not yet implemented")
    override val userAccount: Account?
        get() = TODO("Not yet implemented")
    override val userInfo: UserInfo?
        get() = TODO("Not yet implemented")
    override val providerData: List<Provider>?
        get() = TODO("Not yet implemented")
    override val hasGitHubProvider: Boolean
        get() = TODO("Not yet implemented")
    override val hasEmailProvider: Boolean
        get() = TODO("Not yet implemented")
    override val userState: StateFlow<UserInfo?>
        get() = TODO("Not yet implemented")

    override fun userFlow(): Flow<UserInfo?> {
        TODO("Not yet implemented")
    }

    override suspend fun signInAnonymous(): User? {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithGitHub(uiHandler: Any?, token: String?, refresh: Boolean): User? {
        TODO("Not yet implemented")
    }

    override suspend fun signInLinkWithGitHub(uiHandler: Any?): User? {
        TODO("Not yet implemented")
    }

    override suspend fun createWithEmail(uiHandler: Any?, login: String, password: String): User? {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithEmail(uiHandler: Any?, login: String, password: String): User? {
        TODO("Not yet implemented")
    }

    override suspend fun linkWithProvider(token: String): User? {
        TODO("Not yet implemented")
    }

    override suspend fun linkWithProvider(login: String, password: String): User? {
        TODO("Not yet implemented")
    }

    override fun signOut() {
        TODO("Not yet implemented")
    }

    override suspend fun delete() {
        TODO("Not yet implemented")
    }
}

//actual fun String.toLoginMethod(): Auth.LoginMethod = Auth.LoginMethod.Email
actual fun Auth.LoginMethod.toProviderId(): String = when (this) {
    Auth.LoginMethod.GitHub -> GitHubProviderId
    Auth.LoginMethod.Email -> EmailProviderId
    Auth.LoginMethod.Anonymous -> FirebaseProviderId
}

actual fun getPlatformAuth(configuration: Configuration): Auth = IosAuth(configuration)