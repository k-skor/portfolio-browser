package pl.krzyssko.portfoliobrowser.auth

import android.app.Activity
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.User

class AndroidAuth: Auth() {
    private lateinit var auth: FirebaseAuth

    override fun initAuth() {
        auth = Firebase.auth
    }

    override fun isUserSignedIn() = auth.currentUser != null

    override fun getUserProfile(): Profile? = auth.currentUser?.toProfile()

    override fun getAccessToken(): String = ""

    override fun signInWithGitHub(uiHandler: Any?, refresh: Boolean, callback: LoginFlowCallback) {
        val provider = OAuthProvider.newBuilder("github.com")
        val activity = (uiHandler as? Activity) ?: throw Exception("UI handler is missing!")
        if (refresh) {
            if (isUserSignedIn()) {
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
            }
            return
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
    val credential = (this.credential as OAuthCredential).accessToken
    return if (profile != null && credential != null) User(profile, additionalData, credential) else null
}

actual fun getPlatformAuth(): Auth = AndroidAuth()