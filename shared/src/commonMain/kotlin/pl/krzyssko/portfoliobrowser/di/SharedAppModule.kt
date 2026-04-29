package pl.krzyssko.portfoliobrowser.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.ApiRequestException
import pl.krzyssko.portfoliobrowser.api.AzureApi
import pl.krzyssko.portfoliobrowser.api.AzureSearchApi
import pl.krzyssko.portfoliobrowser.api.AzureTokenProvider
import pl.krzyssko.portfoliobrowser.api.GitHubApi
import pl.krzyssko.portfoliobrowser.api.getAzureTokenProvider
import pl.krzyssko.portfoliobrowser.auth.getPlatformAuth
import pl.krzyssko.portfoliobrowser.business.Login
import pl.krzyssko.portfoliobrowser.business.Onboarding
import pl.krzyssko.portfoliobrowser.business.ProjectDetails
import pl.krzyssko.portfoliobrowser.business.ProjectsList
import pl.krzyssko.portfoliobrowser.business.UserProfile
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.getFirestore
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.AzureSearchRepository
import pl.krzyssko.portfoliobrowser.repository.CategoriesRepository
import pl.krzyssko.portfoliobrowser.repository.FirestoreProjectRepository
import pl.krzyssko.portfoliobrowser.repository.GitHubProjectRepository
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.SearchRepository
import pl.krzyssko.portfoliobrowser.repository.UserRepository
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import pl.krzyssko.portfoliobrowser.auth.Auth as PlatformAuth

val NAMED_GITHUB = named("github")
val NAMED_FIRESTORE = named("firestore")
val NAMED_AZURE = named("azure")

fun sharedAppModule() = module {

    single<Api> { GitHubApi(get(qualifier = NAMED_GITHUB), get()) }
    single<AzureApi> { AzureSearchApi(get(qualifier = NAMED_AZURE), get()) }

    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind ProjectRepository::class
    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind SearchRepository::class
    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind UserRepository::class
    factory(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) } bind CategoriesRepository::class

    factory { GitHubProjectRepository(get(), get()) } bind UserRepository::class
    
    factory(NAMED_FIRESTORE) { FirestoreProjectRepository(get(), get()) } bind ProjectRepository::class
    factory(NAMED_FIRESTORE) { FirestoreProjectRepository(get(), get()) } bind CategoriesRepository::class
    factory<SearchRepository>(NAMED_AZURE) { AzureSearchRepository(get(), get(qualifier = NAMED_FIRESTORE)) }

    single<AzureTokenProvider> {
        val authClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        getAzureTokenProvider(authClient, get())
    }
    
    single(NAMED_AZURE) {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val provider = get<AzureTokenProvider>()
                        //provider.initialize()
                        BearerTokens(provider.token, provider.refreshToken)
                    }
                    refreshTokens {
                        withContext(Dispatchers.IO) {
                            val provider = get<AzureTokenProvider>()
                            provider.refreshToken(oldTokens) {
                                markAsRefreshTokenRequest()
                            }
                        }
                    }
                }
            }
        }
    }
    
    single(NAMED_GITHUB) {
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
    single<PlatformAuth> { getPlatformAuth(get()) }
    single<Firestore> { getFirestore() }
    single<InfiniteColorPicker> { (colorMap: StackColorMap?) -> InfiniteColorPicker(colorMap ?: emptyMap()) }
    factory<ProjectsList> { (coroutineScope: CoroutineScope) ->
        ProjectsList(
            coroutineScope,
            get()
        )
    }
    factory<ProjectDetails> { (coroutineScope: CoroutineScope) ->
        ProjectDetails(
            coroutineScope,
            Dispatchers.IO,
            get(qualifier = NAMED_FIRESTORE),
            get(),
            get()
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
