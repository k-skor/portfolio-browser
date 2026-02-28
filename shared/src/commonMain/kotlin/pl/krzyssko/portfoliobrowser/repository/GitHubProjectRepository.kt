package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack

class GitHubRepositoryException(message: String? = null, throwable: Throwable? = null): Exception(message, throwable)

class GitHubPagingState(override val pageSize: Int = 10, override val paging: Paging): PagingState

class GitHubProjectRepository(private val api: Api, private val auth: Auth) : ProjectRepository {

    private var gitHubPagingState = GitHubPagingState(paging = Paging())
    override val pagingState: GitHubPagingState
        get() = gitHubPagingState

    override fun resetPagingState() {
        gitHubPagingState = GitHubPagingState(paging = Paging())
    }

    /**
     * ProjectRepository
     */
    override suspend fun fetchUser(): Result<String> {
        return runCatching {
            api.getUser()?.login ?: throw GitHubRepositoryException("User not found.")
        }
    }

    override suspend fun fetchTotalProjectsSize(): Result<Int> {
        return runCatching {
            val user = api.getUser()
            user?.let {
                it.totalPublicRepos + it.totalPrivateRepos
            } ?: throw GitHubRepositoryException("User not found.")
        }
    }

    override suspend fun fetchStack(name: String): Result<List<Stack>> {
        return runCatching {
            val response = api.getRepoLanguages(name)
            val sum =
                response.map { it.value }
                    .takeIf { it.isNotEmpty() }
                    ?.reduce { sum, lines -> sum + lines } ?: 0

            response.map {
                Stack(
                    name = it.key, percent = it.value * 100f / sum
                )
            }
        }
    }

    override suspend fun fetchProjectDetails(uid: String, id: String): Result<Project> {
        return runCatching {
            api.getRepoBy(id)?.let {
                Project(
                    id = it.id.toString(),
                    name = it.name,
                    description = it.description,
                    image = Resource.NetworkResource("https://picsum.photos/500/500"),
                    createdBy = auth.userAccount?.id ?: "",
                    createdByName = auth.userAccount?.name ?: "",
                    createdOn = 11234567890,
                    public = !it.private
                )
            } ?: throw GitHubRepositoryException("Project not found.")
        }
    }

    override suspend fun searchProjects(query: String, queryParams: String?): Result<PagedResponse<Project>> {
        return runCatching {
            val response = api.searchRepos(query, queryParams)
            PagedResponse(page = response.page.projects.map {
                Project(
                    id = it.id.toString(),
                    name = it.name,
                    description = it.description,
                    image = Resource.NetworkResource("https://picsum.photos/500/500"),
                    createdBy = auth.userAccount?.id ?: "",
                    createdByName = auth.userAccount?.name ?: "",
                    createdOn = 11234567890,
                    public = !it.private
                )
            }, next = response.next, prev = response.prev, last = response.last)
        }
    }

    override suspend fun nextPage(nextPageKey: Any?): Result<List<Project>> {
        return runCatching {
            val response = api.getRepos(nextPageKey?.toString())
            response.also {
                gitHubPagingState = GitHubPagingState(
                    paging = Paging(
                        pageKey = nextPageKey,
                        nextPageKey = it.next,
                        prevPageKey = it.prev,
                        isLastPage = it.next == null
                    )
                )
            }.page.map {
                Project(
                    id = it.id.toString(),
                    name = it.name,
                    description = it.description,
                    image = Resource.NetworkResource("https://picsum.photos/500/500"),
                    createdBy = auth.userAccount?.id ?: "",
                    createdByName = auth.userAccount?.name ?: "",
                    createdOn = 11234567890,
                    public = !it.private,
                    source = Source.GitHub
                )
            }
        }
    }

    override suspend fun nextSearchPage(query: String, nextPageKey: Any?): Result<List<Project>> =
        Result.failure(GitHubRepositoryException(throwable = NotImplementedError()))

    override suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>> =
        Result.failure(GitHubRepositoryException(throwable = NotImplementedError()))
}