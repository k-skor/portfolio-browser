package pl.krzyssko.portfoliobrowser.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.ApiRequestException
import pl.krzyssko.portfoliobrowser.api.AzureApi
import pl.krzyssko.portfoliobrowser.api.AzureSearchApi
import pl.krzyssko.portfoliobrowser.api.GitHubApi
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.getPlatformAuth
import pl.krzyssko.portfoliobrowser.business.Login
import pl.krzyssko.portfoliobrowser.business.Onboarding
import pl.krzyssko.portfoliobrowser.business.ProjectEdition
import pl.krzyssko.portfoliobrowser.business.ProjectsListInteraction
import pl.krzyssko.portfoliobrowser.business.UserProfile
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.getFirestore
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.CategoriesRepository
import pl.krzyssko.portfoliobrowser.repository.FirestoreProjectRepository
import pl.krzyssko.portfoliobrowser.repository.GitHubProjectRepository
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.SearchRepository
import pl.krzyssko.portfoliobrowser.repository.UserRepository
import pl.krzyssko.portfoliobrowser.store.StackColorMap

val NAMED_GITHUB = named("github")
val NAMED_FIRESTORE = named("firestore")

fun sharedAppModule() = module {

    single<Api> { GitHubApi(get(), get()) }
    single<AzureApi> { AzureSearchApi(get(), get()) }

    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind ProjectRepository::class
    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind SearchRepository::class
    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind UserRepository::class
    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind CategoriesRepository::class

    factory { GitHubProjectRepository(get(), get()) } bind UserRepository::class
    
    factory(NAMED_FIRESTORE) { FirestoreProjectRepository(get(), get()) } bind ProjectRepository::class
    factory(NAMED_FIRESTORE) { FirestoreProjectRepository(get(), get()) } bind CategoriesRepository::class
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            expectSuccess = true
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, _ ->
                    throw ApiRequestException(cause)
                }
            }
        }
    }
    factory<Logging> { getLogging() }
    single<Auth> { getPlatformAuth(get()) }
    single<Firestore> { getFirestore() }
    single<InfiniteColorPicker> { (colorMap: StackColorMap?) -> InfiniteColorPicker(colorMap ?: emptyMap()) }
    factory<ProjectsListInteraction> { (coroutineScope: CoroutineScope) ->
        ProjectsListInteraction(
            coroutineScope
        )
    }
    factory<ProjectEdition> { (coroutineScope: CoroutineScope) ->
        ProjectEdition(
            coroutineScope,
            Dispatchers.IO
        )
    }
    single<Onboarding> { (coroutineScope: CoroutineScope) ->
        Onboarding(
            coroutineScope,
            Dispatchers.IO,
            get(),
            get()
        )
    }
    single<Login> { (coroutineScope: CoroutineScope) ->
        Login(
            coroutineScope,
            Dispatchers.IO,
            get(),
            get(),
            get()
        )
    }
    factory<UserProfile> { (coroutineScope: CoroutineScope) ->
        UserProfile(
            coroutineScope,
            Dispatchers.IO,
            get(),
            get()
        )
    }
}
