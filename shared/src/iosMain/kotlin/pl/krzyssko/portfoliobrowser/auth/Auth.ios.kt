package pl.krzyssko.portfoliobrowser.auth

import pl.krzyssko.portfoliobrowser.data.Profile

class IosAuth: Auth() {
    override fun initAuth() {
        TODO("Not yet implemented")
    }

    override fun isUserSignedIn(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getUserProfile(): Profile? {
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

actual fun getPlatformAuth(): Auth = IosAuth()