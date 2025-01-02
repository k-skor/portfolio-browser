package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.ApiResponse
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack

class GitHubRepositoryException: Exception("Unknown exception.")

class GitHubProjectRepository(private val api: Api, private val auth: Auth) : ProjectRepository {

    // TODO: caching?
    //private var cache = mutableMapOf<String, Project>()

    /**
     * ProjectRepository
     */
    override fun fetchUser(): Flow<String> = flow {
        when (val response = api.getUser()) {
            is ApiResponse.Success -> emit(response.data.login)
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }
    }

    override fun fetchProjects(queryParams: String?): Flow<PagedResponse<Project>> = flow {
        when (val response = api.getRepos(queryParams)) {
            is ApiResponse.Success -> emit(PagedResponse(page = response.data.page.map {
                Project(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    image = Resource.NetworkResource("https://picsum.photos/500/500"),
                    createdBy = auth.userProfile?.id ?: "",
                    createdOn = "2024-01-01T00:00:00Z"
                )
            }, next = response.data.next, prev = response.data.prev, last = response.data.last))
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }
    }

    override fun fetchStack(name: String): Flow<List<Stack>> = flow {
        when (val response = api.getRepoLanguages(name)) {
            is ApiResponse.Success -> emit(response.data.map {
                Stack(
                    name = it.key,
                    lines = it.value
                )
            })
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }
    }

    override fun fetchProjectDetails(name: String): Flow<Project> = flow {
        when (val response = api.getRepoBy(name)) {
            is ApiResponse.Success -> emit(Project(
                id = response.data.id,
                name = response.data.name,
                description = response.data.description,
                image = Resource.NetworkResource("https://picsum.photos/500/500"),
                createdBy = auth.userProfile?.id ?: "",
                createdOn = "2024-01-01T00:00:00Z"
            ))
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }
    }

    override fun searchProjects(query: String, queryParams: String?): Flow<PagedResponse<Project>> = flow {
        when (val response = api.searchRepos(query, queryParams)) {
            is ApiResponse.Success -> emit(PagedResponse(page = response.data.page.projects.map {
                Project(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    image = Resource.NetworkResource("https://picsum.photos/500/500"),
                    createdBy = auth.userProfile?.id ?: "",
                    createdOn = "2024-01-01T00:00:00Z"
                )
            }, next = response.data.next, prev = response.data.prev, last = response.data.last))
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }

    }
}