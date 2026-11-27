package pl.krzyssko.portfoliobrowser.business

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.test
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.AuthAccountExistsException
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.di.FakeAuth
import pl.krzyssko.portfoliobrowser.di.InMemoryConfigurationSaver
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.repository.UserRepository
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginTest {

    private lateinit var config: InMemoryConfigurationSaver

    @BeforeTest
    fun setUp() {
        config = InMemoryConfigurationSaver()
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createLogin(
        auth: Auth,
        userRepository: UserRepository = mock(MockMode.autoUnit)
    ) = Login(
        dispatcherIO = UnconfinedTestDispatcher(),
        auth = auth,
        config = config,
        userRepository = userRepository
    )

    @Test
    fun `test initial state`() = runTest(UnconfinedTestDispatcher()) {
        val login = createLogin(FakeAuth())
        login.test(this) {
            expectInitialState()
        }
    }

    @Test
    fun `test initialize when not signed in`() = runTest(UnconfinedTestDispatcher()) {
        val login = createLogin(FakeAuth())
        login.test(this) {
            expectInitialState()
            containerHost.initialize()
            // No state change expected as it's already Initialized
        }
    }

    @Test
    fun `test initialize when signed in anonymously`() = runTest(UnconfinedTestDispatcher()) {
        val anonymousAccount = Account("anon-uid", null, "", null, isEmailVerified = false, anonymous = true)
        val login = createLogin(FakeAuth(initialAccount = anonymousAccount))
        login.test(this) {
            expectInitialState()
            containerHost.initialize()
            expectState { LoginState.Authenticated(User.Guest) }
        }
    }

    @Test
    fun `test login anonymous success`() = runTest(UnconfinedTestDispatcher()) {
        val login = createLogin(FakeAuth())
        login.test(this) {
            expectInitialState()
            containerHost.login()
            expectState { LoginState.Authenticated(User.Guest) }
            val effect = awaitSideEffect()
            assertTrue(effect is UserSideEffects.NavigateTo && effect.route is Route.HomeRoot)
        }
    }

    @Test
    fun `test github login success`() = runTest(UnconfinedTestDispatcher()) {
        val account = Account("uid123", "initialName", "user@github.com", null, true, false)
        val result = User.Authenticated(account, oauthAccessToken = "token123")
        val auth = FakeAuth().apply {
            onSignIn = { _, _ -> result }
        }
        val userRepository = mock<UserRepository> {
            everySuspend { fetchUser() } returns Result.success("gitHubUser")
        }
        val login = createLogin(auth, userRepository)

        login.test(this) {
            expectInitialState()
            containerHost.login(null)

            expectState {
                LoginState.Authenticated(
                    result.copy(
                        account = account.copy(name = "gitHubUser")
                    )
                )
            }
            val effect = awaitSideEffect()
            assertTrue(effect is UserSideEffects.NavigateTo && effect.route is Route.HomeRoot)
        }
    }

    @Test
    fun `test github login account exists exception`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuth().apply {
            onSignIn = { _, _ -> throw AuthAccountExistsException(Exception()) }
        }
        val login = createLogin(auth)

        login.test(this) {
            expectInitialState()
            containerHost.login(null)

            val effect = awaitSideEffect()
            assertTrue(
                effect is UserSideEffects.NavigateTo && effect.route is Route.AccountsMerge,
                "Expected AccountsMerge navigation but was $effect"
            )
            expectState(LoginState.LinkInProgress)
        }
    }

    @Test
    fun `test email login success`() = runTest(UnconfinedTestDispatcher()) {
        val account = Account("uid123", "emailUser", "user@email.com", null, true, false)
        val result = User.Authenticated(account)
        val auth = FakeAuth().apply {
            onSignIn = { _, _ -> result }
        }
        val login = createLogin(auth)

        login.test(this) {
            expectInitialState()
            containerHost.login(null, "user@email.com", "password123")

            expectState { LoginState.Authenticated(User.Authenticated(account)) }
            val effect = awaitSideEffect()
            assertTrue(effect is UserSideEffects.NavigateTo && effect.route is Route.HomeRoot)
        }
    }

    @Test
    fun `test logout`() = runTest(UnconfinedTestDispatcher()) {
        val account = Account("uid123", "user", "user@email.com", null, true, false)
        val auth = FakeAuth(initialAccount = account)
        val login = createLogin(auth)

        login.test(this) {
            expectInitialState()
            containerHost.initialize()
            expectState { LoginState.Authenticated(User.Authenticated(account)) }

            containerHost.reset()

            expectState(LoginState.Initialized)
            val effect = awaitSideEffect()
            assertTrue(effect is UserSideEffects.NavigateTo && effect.route is Route.Welcome)
            assertEquals(false, auth.isUserSignedIn, "Expected user to be signed out")
        }
    }

    @Test
    fun `test initialize when signed in authenticated`() = runTest(UnconfinedTestDispatcher()) {
        val account = Account("uid123", "user", "user@email.com", null, true, false)
        val login = createLogin(FakeAuth(initialAccount = account))

        login.test(this) {
            expectInitialState()
            containerHost.initialize()
            expectState { LoginState.Authenticated(User.Authenticated(account)) }
        }
    }

    @Test
    fun `test link success`() = runTest(UnconfinedTestDispatcher()) {
        val account = Account("uid123", "initialName", "user@github.com", null, true, false)
        val result = User.Authenticated(account, oauthAccessToken = "token123")
        val auth = FakeAuth(initialAccount = account).apply {
            onSignIn = { _, _ -> result }
        }
        val userRepository = mock<UserRepository> {
            everySuspend { fetchUser() } returns Result.success("gitHubUser")
        }
        val login = createLogin(auth, userRepository)

        login.test(this) {
            expectInitialState()
            containerHost.initialize()
            expectState { LoginState.Authenticated(User.Authenticated(account)) }

            containerHost.link(null)

            val toast = awaitSideEffect()
            assertTrue(toast is UserSideEffects.Toast)

            expectState {
                LoginState.Authenticated(
                    result.copy(
                        account = account.copy(name = "gitHubUser")
                    )
                )
            }
        }
    }

    @Test
    fun `test delete account`() = runTest(UnconfinedTestDispatcher()) {
        val account = Account("uid123", "user", "user@email.com", null, true, false)
        val auth = FakeAuth(initialAccount = account)
        val login = createLogin(auth)

        login.test(this) {
            expectInitialState()
            containerHost.initialize()
            expectState { LoginState.Authenticated(User.Authenticated(account)) }

            containerHost.delete()

            expectState(LoginState.Initialized)
            val effect = awaitSideEffect()
            assertTrue(effect is UserSideEffects.NavigateTo && effect.route is Route.Welcome)
            assertEquals(false, auth.isUserSignedIn, "Expected user to be signed out")
        }
    }
}
