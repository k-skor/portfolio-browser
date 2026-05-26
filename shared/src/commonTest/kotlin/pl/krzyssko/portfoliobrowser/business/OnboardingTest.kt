package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.get
import org.orbitmvi.orbit.test.test
import pl.krzyssko.portfoliobrowser.OnboardingUseCase
import pl.krzyssko.portfoliobrowser.di.sharedAppModule
import pl.krzyssko.portfoliobrowser.di.testModule
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.store.OnboardingState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingTest : KoinTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        startKoin {
            modules(sharedAppModule() + testModule)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `test onboarding flow for returning user`() = runTest {
        val onboarding = get<Onboarding> {
            parametersOf(backgroundScope, OnboardingUseCase.ProfileIsCreatedWhenUserIsSignedIn)
        }

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.ProfileExists }
            expectState { OnboardingState.OnboardingCompleted }
            //expectSideEffect(UserSideEffects.NavigateTo(Route.List))
            val effect = awaitSideEffect()
            assertTrue { effect is UserSideEffects.NavigateTo }
            println("state=${onboarding.stateFlow.value}")
            println("effect=${(effect as UserSideEffects.NavigateTo).route::class}")
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `test onboarding flow for new user requiring profile creation`() = runTest {
        val onboarding = get<Onboarding> {
            parametersOf(backgroundScope, OnboardingUseCase.NewUserRequiresProfileCreation)
        }

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.FirstTimeSignUp }
            expectSideEffect(UserSideEffects.NavigateTo(Route.Onboarding))
            
            // Trigger profile creation
            containerHost.create()
            val state = awaitState()
            assertTrue(state is OnboardingState.FirstTimeSignUpCompleted, "Expected FirstTimeSignUpCompleted state")
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `test onboarding flow for user needing import`() = runTest {
        val onboarding = get<Onboarding> {
            parametersOf(backgroundScope, OnboardingUseCase.UserExistsButNeedsImport)
        }

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.ProfileExists }
            expectState { OnboardingState.ImportSourceAvailable }
            expectSideEffect(UserSideEffects.NavigateTo(Route.ProviderImport))
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `test import flow failure`() = runTest {
        val onboarding = get<Onboarding> {
            parametersOf(backgroundScope, OnboardingUseCase.SyncFailureScenario)
        }

        onboarding.test(this) {
            expectInitialState()
            runOnCreate()
            expectState { OnboardingState.ProfileExists }
            expectState { OnboardingState.ImportSourceAvailable }
            expectSideEffect(UserSideEffects.NavigateTo(Route.ProviderImport))

            // Start import which will eventually fail on save
            containerHost.import(null, pl.krzyssko.portfoliobrowser.data.Source.GitHub)
            expectState { OnboardingState.ImportStarted }
            
            val progressState = awaitState()
            assertTrue(progressState is OnboardingState.ImportProgress, "Expected ImportProgress state")
            
            val errorState = awaitState()
            assertTrue(errorState is OnboardingState.Error, "Expected Error state")
            
            cancelAndIgnoreRemainingItems()
        }
    }
}
