package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject
import pl.krzyssko.portfoliobrowser.data.Project

class GitHubProjectRepository(private val api: Api) : ProjectRepository {

    // TODO: caching?
    //private var cache = mutableMapOf<String, Project>()

    /**
     * ProjectRepository
     */
    override suspend fun fetchProjects(query: String?): PagedResponse<GitHubProject> {
        val response = api.getRepos(query)
        return response
    }

    override suspend fun fetchStack(name: String): Flow<GitHubLanguage> = flow {
        val response = api.getRepoLanguages(name)
        emit(response)
    }

    override suspend fun fetchProjectDetails(name: String): Flow<GitHubProject> = flow {
        emit(api.getRepoBy(name))
    }
}