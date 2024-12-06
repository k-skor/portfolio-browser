package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack

class GitHubProjectRepository(private val api: Api) : ProjectRepository {

    // TODO: caching?
    //private var cache = mutableMapOf<String, Project>()

    /**
     * ProjectRepository
     */
    override fun fetchProjects(queryParams: String?): Flow<PagedResponse<Project>> = flow {
        val response = api.getRepos(queryParams)
        emit(PagedResponse(data = response.data.map {
            Project(
                id = it.id,
                name = it.name,
                description = it.description,
                icon = Resource.NetworkResource("https://picsum.photos/500/500"),
            )
        }, next = response.next, prev = response.prev, last = response.last))
    }

    override fun fetchStack(name: String): Flow<List<Stack>> = flow {
        val response = api.getRepoLanguages(name)
        emit(response.map {
            Stack(
                name = it.key,
                lines = it.value
            )
        })
    }

    override fun fetchProjectDetails(name: String): Flow<Project> = flow {
        val response = api.getRepoBy(name)
        emit(Project(
            id = response.id,
            name = response.name,
            description = response.description,
            icon = Resource.NetworkResource("https://picsum.photos/500/500"),
        ))
    }

    override fun searchProjects(query: String, queryParams: String?): Flow<PagedResponse<Project>> = flow {
        val response = api.searchRepos(query, queryParams)

        emit(PagedResponse(data = response.data.projects.map {
            Project(
                id = it.id,
                name = it.name,
                description = it.description,
                icon = Resource.NetworkResource("https://picsum.photos/500/500"),
            )
        }, next = response.next, prev = response.prev, last = response.last))
    }
}