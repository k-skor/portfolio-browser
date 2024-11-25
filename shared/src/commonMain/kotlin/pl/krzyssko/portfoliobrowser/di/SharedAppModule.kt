package pl.krzyssko.portfoliobrowser.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
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
import pl.krzyssko.portfoliobrowser.store.State

val NAMED_LIST = named("list")
val NAMED_DETAILS = named("details")

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
    factory<OrbitStore<State.ProjectsListState>>(NAMED_LIST) { (coroutineScope: CoroutineScope, initialState: State.ProjectsListState) ->
        OrbitStore(
            coroutineScope,
            initialState
        )
    }
    factory<OrbitStore<State.ProjectState>>(NAMED_DETAILS) { (coroutineScope: CoroutineScope, initialState: State.ProjectState) ->
        OrbitStore(
            coroutineScope,
            initialState
        )
    }
}