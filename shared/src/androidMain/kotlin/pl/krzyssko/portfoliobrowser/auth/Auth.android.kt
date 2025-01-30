package pl.krzyssko.portfoliobrowser.auth

import android.app.Activity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Config
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.platform.getLogging

class AndroidAuth: Auth() {
    private lateinit var auth: FirebaseAuth

    override fun initAuth(config: Config?) {
        config?.let {
            if (!it.lastSignInMethod.isNullOrEmpty()) {
                requestedLoginMethod = LoginMethod.valueOf(it.lastSignInMethod)
            }
        }
        auth = Firebase.auth
    }

    override val isUserSignedIn
        get() = auth.currentUser != null

    override val userProfile: Account?
        get() = auth.currentUser?.toAccount()

    override val providerData: List<Provider>?
        get() = auth.currentUser?.toProviderData()

    override var accessToken: String?
        get() = TODO("Not yet implemented")
        set(value) {}

    override val hasGitHubProvider: Boolean
        get() = providerData?.let {
            for (provider in it) {
                if (provider.providerId == GithubAuthProvider.PROVIDER_ID) {
                    return@let true
                }
            }
            false
        } ?: false

    override fun signInAnonymous(callback: LoginFlowCallback) {
        auth.signInAnonymously()
            .addOnSuccessListener {
                callback.onSuccess(it.toUser())
            }
            .addOnFailureListener {
                callback.onFailure(it)
            }
    }

    override fun signInWithGitHub(uiHandler: Any?, token: String?, refresh: Boolean, callback: LoginFlowCallback) {
        token?.let {
            // Let's refresh the token once user has some previous one
            if (isUserSignedIn) {
                val credential = GithubAuthProvider.getCredential(token)
                auth.signInWithCredential(credential).addOnSuccessListener {
                    callback.onSuccess(it.toUser())
                }
                    .addOnFailureListener {
                        callback.onFailure(it)
                    }
            }
            return
        }
        val provider = OAuthProvider.newBuilder("github.com")
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is missing!")
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
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is missing!")
        if (!isUserSignedIn) {
            throw IllegalStateException("User should be signed in before linking with GitHub.")
        }
        try {
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
        } catch (exception: FirebaseAuthUserCollisionException) {
            // cannot link the account with provider for some reason, notify user and sign in
            throw AuthLinkFailedException("User already linked with GitHub.")
        }
    }

    override fun linkWithProvider(login: String, password: String, callback: LoginFlowCallback) {
        if (isUserSignedIn) {
            val credential = EmailAuthProvider.getCredential(login, password)
            linkWithProvider(credential, callback)
        }
    }

    override fun linkWithProvider(token: String, callback: LoginFlowCallback) {
        if (isUserSignedIn) {
            // TODO: custom way to get token is required
            val credential = GithubAuthProvider.getCredential(token)
            linkWithProvider(credential, callback)
        }
    }

    override fun createWithEmail(uiHandler: Any?, login: String, password: String, callback: LoginFlowCallback) {
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is missing!")
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
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is missing!")
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

    private fun linkWithProvider(credential: AuthCredential, callback: LoginFlowCallback) {
        if (isUserSignedIn) {
            // User is either logged in with email or with GitHub.
            // Let's refresh its credentials and link with current user.
            auth.currentUser!!.linkWithCredential(credential).addOnSuccessListener {
                callback.onSuccess(it.toUser())
            }.addOnFailureListener {
                callback.onFailure(it)
            }
        }
    }
}

fun FirebaseUser.toAccount(): Account = Account(
    this.uid,
    this.displayName,
    this.email,
    this.photoUrl.toString(),
    this.isEmailVerified,
    this.isAnonymous
)

fun FirebaseUser.toProviderData(): List<Provider> =
    this.providerData.map {
        Provider(
            it.providerId,
            it.uid,
            it.displayName,
            it.email,
            it.photoUrl.toString()
        )
    }

fun AuthResult.toUser(): User.Authenticated? {
    // User is signed in.
    // IdP data available in
    // authResult.getAdditionalUserInfo().getProfile().
    // The OAuth access token can also be retrieved:
    // ((OAuthCredential)authResult.getCredential()).getAccessToken().
    // The OAuth secret can be retrieved by calling:
    // ((OAuthCredential)authResult.getCredential()).getSecret().
    val account = this.user?.toAccount()
    val additionalData = this.additionalUserInfo?.profile
    val accessToken = (this.credential as? OAuthCredential)?.accessToken
    val signInMethod = this.credential?.signInMethod
    return account?.let {
        val user = User.Authenticated(account, additionalData, accessToken, signInMethod)
        getLogging().debug("Auth result user=${user}")
        user
    }
}

actual fun String.toLoginMethod(): Auth.LoginMethod = if (this == GithubAuthProvider.PROVIDER_ID) Auth.LoginMethod.GitHub else Auth.LoginMethod.Email

actual fun getPlatformAuth(): Auth = AndroidAuth()