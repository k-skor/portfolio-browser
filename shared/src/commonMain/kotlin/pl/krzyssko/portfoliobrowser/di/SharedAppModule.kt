package pl.krzyssko.portfoliobrowser.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.GitHubApi
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getConfiguration
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.GitHubProjectRepository
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.SharedState

val NAMED_LIST = named("list")
val NAMED_DETAILS = named("details")
val NAMED_SHARED = named("shared")

fun sharedAppModule() = module {

    single<Api> { GitHubApi(get(), get()) }
    single<ProjectRepository> { GitHubProjectRepository(get()) }
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
    factory<Configuration> { getConfiguration() }
    factory<Logging> { getLogging() }
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
    single<OrbitStore<SharedState>>(NAMED_SHARED) { (initialState: SharedState?) ->
        OrbitStore(
            CoroutineScope(Dispatchers.Default),
            initialState ?: SharedState()
        )
    }
}
