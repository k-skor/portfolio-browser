package pl.krzyssko.portfoliobrowser.business

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.test
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.FakeAuth
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.repository.CategoriesRepository
import pl.krzyssko.portfoliobrowser.repository.Paging
import pl.krzyssko.portfoliobrowser.repository.PagingState
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OnboardingState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingTest {

    private val testAccount = Account(
        id = "uid123",
        name = "user",
        email = "user@email.com",
        avatarUrl = null,
        isEmailVerified = false,
        anonymous = false
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createOnboarding(
        scope: CoroutineScope,
        auth: Auth,
        firestore: Firestore,
        projectRepository: ProjectRepository = mock(MockMode.autoUnit),
        categoriesRepository: CategoriesRepository = mock(MockMode.autoUnit)
    ) = Onboarding(
        coroutineScope = scope,
        dispatcherIO = StandardTestDispatcher(),
        auth = auth,
        firestore = firestore,
        repository = projectRepository,
        categoriesRepository = categoriesRepository
    )

    @Test
    fun `test onboarding flow for returning user`() = runTest {
        val auth = FakeAuth(initialAccount = testAccount)
        val firestore = mock<Firestore>(MockMode.autoUnit) {
            everySuspend { hasUser("uid123") } returns true
            everySuspend { getLastSyncTimestampForSource("uid123", Source.GitHub) } returns 123L
        }
        val onboarding = createOnboarding(backgroundScope, auth, firestore)

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.ProfileExists }
            expectState { OnboardingState.OnboardingCompleted }
            val effect = awaitSideEffect()
            assertTrue { effect is UserSideEffects.NavigateTo && effect.route is Route.List }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `test onboarding flow for new user requiring profile creation`() = runTest {
        val auth = FakeAuth(initialAccount = testAccount)
        val firestore = mock<Firestore>(MockMode.autoUnit) {
            everySuspend { hasUser("uid123") } returns false
            everySuspend { getLastSyncTimestampForSource("uid123", Source.GitHub) } returns null
        }
        val onboarding = createOnboarding(backgroundScope, auth, firestore)

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.FirstTimeSignUp }
            val effect = awaitSideEffect()
            assertTrue { effect is UserSideEffects.NavigateTo && effect.route is Route.Onboarding }

            val state = awaitState()
            assertTrue(state is OnboardingState.FirstTimeSignUpCompleted, "Expected FirstTimeSignUpCompleted state")
            cancelAndIgnoreRemainingItems()
        }

        verifySuspend { firestore.createProfile("uid123", any()) }
    }

    @Test
    fun `test onboarding flow for user needing import`() = runTest {
        val auth = FakeAuth(initialAccount = testAccount)
        val firestore = mock<Firestore>(MockMode.autoUnit) {
            everySuspend { hasUser("uid123") } returns true
            everySuspend { getLastSyncTimestampForSource("uid123", Source.GitHub) } returns null
        }
        val onboarding = createOnboarding(backgroundScope, auth, firestore)

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.ProfileExists }
            expectState { OnboardingState.ImportSourceAvailable }
            val effect = awaitSideEffect()
            assertTrue { effect is UserSideEffects.NavigateTo && effect.route is Route.ProviderImport }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `test import flow failure`() = runTest {
        val auth = FakeAuth(initialAccount = testAccount)
        val firestore = mock<Firestore>(MockMode.autoUnit) {
            everySuspend { hasUser("uid123") } returns true
            everySuspend { getLastSyncTimestampForSource("uid123", Source.GitHub) } returns null
            everySuspend { syncProjects(any(), any(), any()) } throws RuntimeException("Sync failed")
        }
        val mockPagingState = mock<PagingState> {
            every { paging } returns Paging(isLastPage = true)
        }
        val projectRepository = mock<ProjectRepository>(MockMode.autoUnit) {
            every { pagingState } returns mockPagingState
            everySuspend { fetchTotalProjectsSize() } returns Result.success(0)
            everySuspend { nextPage(any(), any()) } returns Result.success(emptyList())
        }
        val onboarding = createOnboarding(backgroundScope, auth, firestore, projectRepository)

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.ProfileExists }
            expectState { OnboardingState.ImportSourceAvailable }
            val effect = awaitSideEffect()
            assertTrue { effect is UserSideEffects.NavigateTo && effect.route is Route.ProviderImport }

            containerHost.import(null, Source.GitHub)
            expectState { OnboardingState.ImportStarted }

            val progressState = awaitState()
            assertTrue(progressState is OnboardingState.ImportProgress, "Expected ImportProgress state")

            val errorState = awaitState()
            assertTrue(errorState is OnboardingState.Error, "Expected Error state")

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `test onboarding resets to initialized when user signs out`() = runTest {
        val auth = FakeAuth(initialAccount = testAccount)
        val firestore = mock<Firestore>(MockMode.autoUnit) {
            everySuspend { hasUser("uid123") } returns true
            everySuspend { getLastSyncTimestampForSource("uid123", Source.GitHub) } returns 123L
        }
        val onboarding = createOnboarding(backgroundScope, auth, firestore)

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.ProfileExists }
            expectState { OnboardingState.OnboardingCompleted }
            awaitSideEffect()

            // Reactive Auth.userState transition -> Onboarding resets automatically
            auth.signOut()
            expectState { OnboardingState.Initialized }

            cancelAndIgnoreRemainingItems()
        }
    }
}
