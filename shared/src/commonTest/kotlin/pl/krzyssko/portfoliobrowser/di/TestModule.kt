package pl.krzyssko.portfoliobrowser.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject
import pl.krzyssko.portfoliobrowser.api.dto.GitHubUser
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.business.Onboarding
import pl.krzyssko.portfoliobrowser.UseCase
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.platform.ConfigurationData
import pl.krzyssko.portfoliobrowser.platform.ConfigurationDataSaver

val testModule = module {
    //single<VaultProvider> {
    //    return@single object : VaultProvider {
    //        override val vault: Nothing
    //            get() = throw NotImplementedError()
    //    }
    //}
    single {
        object : ConfigurationDataSaver {
            override fun save(data: ConfigurationData) { }
            override fun restore(): ConfigurationData = ConfigurationData()
        }
    } bind ConfigurationDataSaver::class
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
    factory<Api> {
        val user = GitHubUser(123, "k-skor", totalPublicRepos = 0, totalPrivateRepos = 0)
        val projects = emptyList<GitHubProject>()
        val langStack = emptyMap<String, Int>()
        FakeApi(user, projects, langStack)
    }

    factory<Onboarding> { (coroutineScope: CoroutineScope, useCase: UseCase) ->
        val (isSignedIn, hasUser) = when (useCase) {
            UseCase.ProfileIsCreatedWhenUserIsSignedIn,
            UseCase.UserExistsButNeedsImport,
            UseCase.SyncFailureScenario -> Pair(true, true)
            UseCase.NewUserRequiresProfileCreation -> Pair(true, false)
        }
        val (timestamp, shouldFailSync) = when (useCase) {
            UseCase.ProfileIsCreatedWhenUserIsSignedIn -> Pair(123L, false)
            UseCase.NewUserRequiresProfileCreation,
            UseCase.UserExistsButNeedsImport -> Pair(null, false)
            UseCase.SyncFailureScenario -> Pair(null, true)
        }

        val auth = get<Auth> { parametersOf(isSignedIn) }

        Onboarding(
            coroutineScope,
            StandardTestDispatcher(),
            auth,
            get<Firestore> {
                parametersOf(hasUser, timestamp, shouldFailSync)
            }
        )
    }
}
