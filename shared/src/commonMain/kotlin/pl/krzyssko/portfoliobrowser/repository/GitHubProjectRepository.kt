package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.ApiResponse
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack

class GitHubRepositoryException: Exception("Fatal exception.")

class GitHubPagingState(override val paging: Paging): PagingState

class GitHubProjectRepository(private val api: Api, private val auth: Auth) : ProjectRepository {

    private var gitHubPagingState: GitHubPagingState? = null
    override val pagingState: PagingState?
        get() = gitHubPagingState

    override fun resetPagingState() {
        gitHubPagingState = null
    }

    /**
     * ProjectRepository
     */
    override fun fetchUser(): Flow<String> = flow {
        when (val response = api.getUser()) {
            is ApiResponse.Success -> emit(response.data.login)
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }
    }

    override fun fetchStack(name: String): Flow<List<Stack>> = flow {
        when (val response = api.getRepoLanguages(name)) {
            is ApiResponse.Success -> {
                val sum =
                    response.data.map { it.value }
                        .reduce { sum, lines -> sum + lines }

                emit(response.data.map {
                    Stack(
                        name = it.key,
                        percent = it.value * 100f / sum
                    )
                })
            }
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
                createdOn = 11234567890
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
                    createdOn = 11234567890
                )
            }, next = response.data.next, prev = response.data.prev, last = response.data.last))
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }

    }

    override fun nextPage(): Flow<List<Project>> = flow {
        val pageKey = gitHubPagingState?.paging?.nextPageKey.toString()
        when (val response = api.getRepos(pageKey)) {
            is ApiResponse.Success -> emit(response.data.also {
                gitHubPagingState = GitHubPagingState(
                    paging = Paging(
                        pageKey = pageKey,
                        nextPageKey = it.next,
                        prevPageKey = it.prev,
                        isLastPage = it.next == null
                    )
                )
            }.page.map {
                Project(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    image = Resource.NetworkResource("https://picsum.photos/500/500"),
                    createdBy = auth.userProfile?.id ?: "",
                    createdOn = 11234567890,
                    source = Source.GitHub
                )
            })
            is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
        }
    }

    override fun nextSearchPage(query: String): Flow<List<Project>> {
        TODO("Not yet implemented")
    }
}