package pl.krzyssko.portfoliobrowser.auth

import android.app.Activity
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.platform.getLogging

class AndroidAuth: Auth() {
    private lateinit var auth: FirebaseAuth

    override fun initAuth() {
        auth = Firebase.auth
    }

    override val isUserSignedIn
        get() = auth.currentUser != null

    override val userProfile: Profile?
        get() = auth.currentUser?.toProfile()

    override var accessToken: String?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun signInWithGitHub(uiHandler: Any?, refresh: Boolean, callback: LoginFlowCallback) {
        val provider = OAuthProvider.newBuilder("github.com")
        val activity = (uiHandler as? Activity) ?: throw Exception("UI handler is missing!")
        if (refresh) {
            if (isUserSignedIn) {
                val firebaseUser = auth.currentUser!!
                firebaseUser
                    .startActivityForReauthenticateWithProvider(activity, provider.build())
                    .addOnSuccessListener {
                        // User is re-authenticated with fresh tokens and
                        // should be able to perform sensitive operations
                        // like account deletion and email or password
                        // update.
                        callback.onSuccess(it.toUser())
                    }
                    .addOnFailureListener {
                        // Handle failure.
                        callback.onFailure(it)
                    }
                return
            }
        }
        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                .addOnSuccessListener {
                    callback.onSuccess(it.toUser())
                }
                .addOnFailureListener(callback::onFailure)
        } else {
            // There's no pending result so you need to start the sign-in flow.
            auth
                .startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener {
                    // User is signed in.
                    // IdP data available in
                    // authResult.getAdditionalUserInfo().getProfile().
                    // The OAuth access token can also be retrieved:
                    // ((OAuthCredential)authResult.getCredential()).getAccessToken().
                    // The OAuth secret can be retrieved by calling:
                    // ((OAuthCredential)authResult.getCredential()).getSecret().
                    callback.onSuccess(it.toUser())
                }
                .addOnFailureListener {
                    // Handle failure.
                    callback.onFailure(it)
                }
        }
    }

    override fun signInLinkWithGitHub(uiHandler: Any?, callback: LoginFlowCallback) {
        val provider = OAuthProvider.newBuilder("github.com")
        val activity = (uiHandler as? Activity) ?: throw Exception("UI handler is missing!")
        if (isUserSignedIn) {
            val firebaseUser = auth.currentUser!!
            firebaseUser
                .startActivityForLinkWithProvider(activity, provider.build())
                .addOnSuccessListener {
                    // Provider credential is linked to the current user.
                    // IdP data available in
                    // authResult.getAdditionalUserInfo().getProfile().
                    // The OAuth access token can also be retrieved:
                    // authResult.getCredential().getAccessToken().
                    // The OAuth secret can be retrieved by calling:
                    // authResult.getCredential().getSecret().
                    callback.onSuccess(it.toUser())
                }
                .addOnFailureListener {
                    // Handle failure.
                    callback.onFailure(it)
                }
            return
        }
        callback.onFailure(Error("User should be signed in before linking with GitHub."))
    }

    override fun linkWithProvider(token: String, callback: LoginFlowCallback) {
        if (isUserSignedIn) {
            val credential = GithubAuthProvider.getCredential(token)
            auth.currentUser!!.linkWithCredential(credential).addOnSuccessListener {
                callback.onSuccess(it.toUser())
            }.addOnFailureListener {
                callback.onFailure(it)
            }
        }
    }

    override fun createWithEmail(uiHandler: Any?, login: String, password: String, callback: LoginFlowCallback) {
        val activity = (uiHandler as? Activity) ?: throw Exception("UI handler is missing!")
        auth.createUserWithEmailAndPassword(login, password)
            .addOnCompleteListener(activity) {
                if (it.isSuccessful) {
                    callback.onSuccess(it.result.toUser())
                } else {
                    callback.onFailure(it.exception ?: Error("Unknown email sign up error."))
                }
            }
    }

    override fun signInWithEmail(uiHandler: Any?, login: String, password: String, callback: LoginFlowCallback) {
        val activity = (uiHandler as? Activity) ?: throw Exception("UI handler is missing!")
        auth.signInWithEmailAndPassword(login, password)
            .addOnCompleteListener(activity) {
                if (it.isSuccessful) {
                    callback.onSuccess(it.result.toUser())
                } else {
                    callback.onFailure(it.exception ?: Error("Unknown email sign in error."))
                }
            }
    }

    override fun signOut() {
        auth.signOut()
    }
}

fun FirebaseUser.toProfile(): Profile = Profile(this.uid, this.displayName ?: "no name", this.email ?: "no email", this.photoUrl.toString(), this.isEmailVerified)

fun AuthResult.toUser(): User? {
    // User is signed in.
    // IdP data available in
    // authResult.getAdditionalUserInfo().getProfile().
    // The OAuth access token can also be retrieved:
    // ((OAuthCredential)authResult.getCredential()).getAccessToken().
    // The OAuth secret can be retrieved by calling:
    // ((OAuthCredential)authResult.getCredential()).getSecret().
    val profile = this.user?.toProfile()
    val additionalData = this.additionalUserInfo?.profile
    val accessToken = (this.credential as? OAuthCredential)?.accessToken
    return profile?.let {
        val user = User(profile, additionalData, accessToken)
        getLogging().debug("Auth result user=${user}")
        user
    }
}

actual fun getPlatformAuth(): Auth = AndroidAuth()