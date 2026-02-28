package pl.krzyssko.portfoliobrowser.auth

import android.app.Activity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.platform.isEmulator

actual val FirebaseProviderId: String = FirebaseAuthProvider.PROVIDER_ID
actual val GitHubProviderId: String = GithubAuthProvider.PROVIDER_ID
actual val EmailProviderId: String = EmailAuthProvider.PROVIDER_ID

class AndroidAuth(configuration: Configuration): Auth(configuration) {
    private val auth: FirebaseAuth

    init {
        if (isEmulator) {
            Firebase.auth.useEmulator("10.0.2.2", 9099)
        }
        auth = Firebase.auth
    }

    override val isUserSignedIn
        get() = auth.currentUser != null

    override val userAccount: Account?
        get() = auth.currentUser?.toAccount()

    override val providerData: List<Provider>?
        get() = auth.currentUser?.toProviderData()

    override val hasGitHubProvider: Boolean
        get() = providerData?.any {
            it.providerId == GithubAuthProvider.PROVIDER_ID
        } ?: false

    override val hasEmailProvider: Boolean
        get() = providerData?.any {
            it.providerId == EmailAuthProvider.PROVIDER_ID
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
            throw AuthAccountExistsException(exception)
        } catch (exception: FirebaseAuthInvalidCredentialsException) {
            // to weak password
            throw AuthPasswordTooWeakException(exception)
        } catch (exception: FirebaseAuthInvalidUserException) {
            // disabled, not found, token expired, etc.
            throw AuthInvalidUserException(exception)
        }
    }

    override suspend fun signInAnonymous() = auth.signInAnonymously().await().toUser()

    override suspend fun signInWithGitHub(uiHandler: Any?, token: String?, refresh: Boolean): User.Authenticated? {
        val user = token?.let {
            // Let's refresh the token once user has some previous one
            if (isUserSignedIn) {
                val credential = GithubAuthProvider.getCredential(token)
                auth.signInWithCredential(credential).await().toUser()
            } else {
                throw IllegalStateException("User must be signed in before refreshing token.")
            }
        }
        if (user != null) {
            return user
        }
        val provider = OAuthProvider.newBuilder("github.com")
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is missing!")
        if (refresh) {
            return if (isUserSignedIn) {
                val firebaseUser = auth.currentUser!!
                firebaseUser
                    .startActivityForReauthenticateWithProvider(activity, provider.build()).await().toUser()
            } else {
                throw IllegalStateException("User must be signed in before refreshing login.")
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
        val activity = (uiHandler as? Activity) ?: throw IllegalArgumentException("UI handler is not an activity or is null.")
        if (!isUserSignedIn) {
            throw IllegalStateException("User must be signed in before linking with GitHub.")
        }
        try {
            val firebaseUser = auth.currentUser!!
            return firebaseUser
                .startActivityForLinkWithProvider(activity, provider.build()).await().toUser()
        } catch (exception: FirebaseAuthUserCollisionException) {
            // cannot link the account with provider for some reason, notify user and sign in
            throw AuthAccountExistsException(exception)
        }
    }

    override suspend fun linkWithProvider(login: String, password: String) =
        if (isUserSignedIn) {
            val credential = EmailAuthProvider.getCredential(login, password)
            linkWithProvider(credential)
        } else {
            throw IllegalStateException("User must be signed in before linking with Email.")
        }

    override suspend fun linkWithProvider(token: String) =
        if (!isUserSignedIn) {
            val credential = GithubAuthProvider.getCredential(token)
            linkWithProvider(credential)
        } else {
            throw IllegalStateException("User must be signed in before linking with GitHub.")
        }

    override suspend fun createWithEmail(uiHandler: Any?, login: String, password: String) =
        auth.createUserWithEmailAndPassword(login, password).await().toUser()

    override suspend fun signInWithEmail(uiHandler: Any?, login: String, password: String) =
        auth.signInWithEmailAndPassword(login, password).await().toUser()

    override fun signOut() {
        auth.signOut()
    }

    private suspend fun linkWithProvider(credential: AuthCredential): User.Authenticated? {
        try {
            // User is either logged in with email or with GitHub.
            // Let's refresh its credentials and link with current user.
            return auth.currentUser!!.linkWithCredential(credential).await().toUser()
        } catch (exception: FirebaseAuthUserCollisionException) {
            throw AuthAccountExistsException(exception)
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
    this.photoUrl?.toString(),
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
            it.photoUrl?.toString()
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
    val providers = this.user?.toProviderData() ?: emptyList()
    return account?.let {
        val user = User.Authenticated(account, additionalData, accessToken, signInMethod, providers)
        getLogging().debug("Auth result user=${user}")
        user
    }
}

actual fun Auth.LoginMethod.toProviderId(): String = when (this) {
    Auth.LoginMethod.GitHub -> GithubAuthProvider.PROVIDER_ID
    Auth.LoginMethod.Email -> EmailAuthProvider.PROVIDER_ID
    Auth.LoginMethod.Anonymous -> FirebaseAuthProvider.PROVIDER_ID
}

actual fun getPlatformAuth(configuration: Configuration): Auth = AndroidAuth(configuration)