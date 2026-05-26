package pl.krzyssko.portfoliobrowser.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.OnboardingUseCase
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.business.Onboarding
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.VaultProvider
import pl.krzyssko.portfoliobrowser.repository.CategoriesRepository
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

val testModule = module {
    single<VaultProvider> {
        return@single object : VaultProvider {
            override val vault: Nothing
                get() = throw NotImplementedError()
        }
    }
    single<Configuration> {
        object : Configuration(get()) { }
    }
    single<Auth> { (isSignedIn: Boolean) ->
        if (isSignedIn) {
            FakeAuth(get(), true)
        } else {
            FakeAuth(get())
        }
    }
    factory<Firestore> { (userCreated: Boolean, lastSyncTimestamp: Long?, shouldFailSync: Boolean) ->
        FakeFirestore(userCreated, lastSyncTimestamp, shouldFailSync)
    }
    factory(NAMED_GITHUB) { (projects: List<Project>) ->
        FakeProjectRepository(projects, projects.size)
    } bind ProjectRepository::class
    factory(NAMED_GITHUB) { (projects: List<Project>) ->
        FakeProjectRepository(projects, projects.size)
    } bind CategoriesRepository::class

    factory<Onboarding> { (coroutineScope: CoroutineScope, useCase: OnboardingUseCase) ->
        val (isSignedIn, hasUser) = when (useCase) {
            OnboardingUseCase.ProfileIsCreatedWhenUserIsSignedIn,
            OnboardingUseCase.UserExistsButNeedsImport,
            OnboardingUseCase.SyncFailureScenario -> Pair(true, true)
            OnboardingUseCase.NewUserRequiresProfileCreation -> Pair(true, false)
        }
        val (timestamp, shouldFailSync) = when (useCase) {
            OnboardingUseCase.ProfileIsCreatedWhenUserIsSignedIn -> Pair(123L, false)
            OnboardingUseCase.NewUserRequiresProfileCreation,
            OnboardingUseCase.UserExistsButNeedsImport -> Pair(null, false)
            OnboardingUseCase.SyncFailureScenario -> Pair(null, true)
        }

        Onboarding(
            coroutineScope,
            StandardTestDispatcher(),
            get<Auth> {
                parametersOf(isSignedIn)
            },
            get<Firestore> {
                parametersOf(hasUser, timestamp, shouldFailSync)
            }
        )
    }
}
