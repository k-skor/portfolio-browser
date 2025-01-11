package pl.krzyssko.portfoliobrowser.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.GitHubApi
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.auth.getPlatformAuth
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.getFirestore
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.FirestoreProjectRepository
import pl.krzyssko.portfoliobrowser.repository.GitHubProjectRepository
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.StackColorMap

val NAMED_LIST = named("list")
val NAMED_DETAILS = named("details")
val NAMED_PROFILE = named("profile")
val NAMED_GITHUB = named("github")
val NAMED_FIRESTORE = named("firestore")

fun sharedAppModule() = module {

    single<Api> { GitHubApi(get(), get()) }
    factory<ProjectRepository>(NAMED_GITHUB) { GitHubProjectRepository(get(), get()) }
    factory<ProjectRepository>(NAMED_FIRESTORE) { FirestoreProjectRepository(get(), get()) }
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            expectSuccess = true
        }
    }
    factory<Logging> { getLogging() }
    single<Auth> { getPlatformAuth() }
    single<Firestore> { getFirestore() }
    single<InfiniteColorPicker> { (colorMap: StackColorMap?) -> InfiniteColorPicker(colorMap ?: emptyMap()) }
    factory<OrbitStore<ProjectsListState>>(NAMED_LIST) { (coroutineScope: CoroutineScope, initialState: ProjectsListState) ->
        OrbitStore(
            coroutineScope,
            initialState
        )
    }
    factory<OrbitStore<ProjectState>>(NAMED_DETAILS) { (coroutineScope: CoroutineScope, initialState: ProjectState) ->
        OrbitStore(
            coroutineScope,
            initialState
        )
    }
    factory<OrbitStore<ProfileState>>(NAMED_PROFILE) { (coroutineScope: CoroutineScope, initialState: ProfileState) ->
        OrbitStore(
            coroutineScope,
            initialState
        )
    }
}
