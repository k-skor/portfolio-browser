package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.platform.Configuration

actual val FirebaseProviderId: String = "firestore"
actual val GitHubProviderId: String = "github"
actual val EmailProviderId: String = "email"

class IosAuth(configuration: Configuration): Auth(configuration) {
    override fun initAuth() {
        TODO("Not yet implemented")
    }

    override fun isUserSignedIn(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getUserProfile(): Account? {
        TODO("Not yet implemented")
    }

    override fun getAccessToken(): String {
        TODO("Not yet implemented")
    }

    override fun signInWithGitHub(uiHandler: Any?, refresh: Boolean, callback: LoginFlowCallback) {
        TODO("Not yet implemented")
    }

    override fun signOut() {
        TODO("Not yet implemented")
    }
}

//actual fun String.toLoginMethod(): Auth.LoginMethod = Auth.LoginMethod.Email
actual fun Auth.LoginMethod.toProviderId(): String {
    TODO("Not yet implemented")
}

actual fun getPlatformAuth(configuration: Configuration): Auth = IosAuth(configuration)