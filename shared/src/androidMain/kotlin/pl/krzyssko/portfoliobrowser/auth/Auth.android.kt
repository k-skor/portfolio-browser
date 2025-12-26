package pl.krzyssko.portfoliobrowser.auth

import android.app.Activity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
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

    override suspend fun startSignInFlow(
        uiHandler: Any?,
        providerType: LoginMethod,
        create: Boolean,
        login: String?,
        password: String?,
        refresh: Boolean,
        token: String?,
        linkWithProvider: Boolean
    ): User.Authenticated? {
        try {
            return super.startSignInFlow(
                uiHandler,
                providerType,
                create,
                login,
                password,
                refresh,
                token,
                linkWithProvider
            )
        } catch (exception: FirebaseAuthUserCollisionException) {
            // cannot link the account with provider for some reason, notify user and sign in
            throw AuthAccountExistsException("Cannot link accounts because the account already exists.")
        } catch (exception: FirebaseAuthInvalidCredentialsException) {
            // to weak password
            throw AuthFailedException(Reason.PasswordTooWeak)
        } catch (exception: FirebaseAuthInvalidUserException) {
            // disabled, not found, token expired, etc.
            throw AuthFailedException(Reason.InvalidUser)
        }
    }

    override suspend fun signInAnonymous() = auth.signInAnonymously().await().toUser()

    override suspend fun signInWithGitHub(uiHandler: Any?, token: String?, refresh: Boolean): User.Authenticated? {
        token?.let {
            // Let's refresh the token once user has some previous one
            return if (isUserSignedIn) {
                val credential = GithubAuthProvider.getCredential(token)
                auth.signInWithCredential(credential).await().toUser()
            } else {
                null
            }
        }
        val provider = OAuthProvider.newBuilder("github.com")
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is missing!")
        if (refresh) {
            return if (isUserSignedIn) {
                val firebaseUser = auth.currentUser!!
                firebaseUser
                    .startActivityForReauthenticateWithProvider(activity, provider.build()).await().toUser()
            } else {
                null
            }
        }
        val pendingResultTask = auth.pendingAuthResult
        return if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask.await().toUser()
        } else {
            // There's no pending result so you need to start the sign-in flow.
            auth
                .startActivityForSignInWithProvider(activity, provider.build()).await().toUser()
        }
    }

    override suspend fun signInLinkWithGitHub(uiHandler: Any?): User.Authenticated? {
        val provider = OAuthProvider.newBuilder("github.com")
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is missing!")
        if (!isUserSignedIn) {
            throw IllegalStateException("User should be signed in before linking with GitHub.")
        }
        try {
            val firebaseUser = auth.currentUser!!
            return firebaseUser
                .startActivityForLinkWithProvider(activity, provider.build()).await().toUser()
        } catch (exception: FirebaseAuthUserCollisionException) {
            // cannot link the account with provider for some reason, notify user and sign in
            throw AuthAccountExistsException("Cannot link accounts because the account already exists.")
        }
    }

    override suspend fun linkWithProvider(login: String, password: String) =
        if (isUserSignedIn) {
            val credential = EmailAuthProvider.getCredential(login, password)
            linkWithProvider(credential)
        } else {
            null
        }

    override suspend fun linkWithProvider(token: String) =
        if (isUserSignedIn) {
            // TODO: custom way to get token is required
            val credential = GithubAuthProvider.getCredential(token)
            linkWithProvider(credential)
        } else {
            null
        }

    override suspend fun createWithEmail(uiHandler: Any?, login: String, password: String) =
        auth.createUserWithEmailAndPassword(login, password).await().toUser()

    override suspend fun signInWithEmail(uiHandler: Any?, login: String, password: String) =
        auth.signInWithEmailAndPassword(login, password).await().toUser()
            //.addOnCompleteListener(activity) {
            //    if (it.isSuccessful) {
            //        callback.onSuccess(it.result.toUser())
            //    } else {
            //        callback.onFailure(it.exception ?: Error("Unknown email sign in error."))
            //    }
            //}

    override fun signOut() {
        auth.signOut()
    }

    private suspend fun linkWithProvider(credential: AuthCredential): User.Authenticated? {
        try {
            return if (isUserSignedIn) {
                // User is either logged in with email or with GitHub.
                // Let's refresh its credentials and link with current user.
                auth.currentUser!!.linkWithCredential(credential).await().toUser()
            } else {
                null
            }
        } catch (exception: FirebaseAuthUserCollisionException) {
            throw AuthAccountExistsException("Cannot link accounts because the account already exists.")
        }
    }

    override suspend fun delete() {
        if (isUserSignedIn) {
            auth.currentUser?.delete()?.await()
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