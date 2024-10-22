package pl.krzyssko.portfoliobrowser.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.GitHubApi
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.GitHubProjectRepository

fun sharedAppModule() = module {
    single<Api> { GitHubApi(get()) }
    single<ProjectRepository> { GitHubProjectRepository(get()) }
    single<HttpClient> { HttpClient(CIO)}
}