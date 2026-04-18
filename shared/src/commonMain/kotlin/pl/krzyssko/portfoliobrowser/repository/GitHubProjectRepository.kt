package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack

class GitHubRepositoryException(message: String? = null, throwable: Throwable? = null): Exception(message, throwable)

class GitHubPagingState(override val pageSize: Int = 10, override val paging: Paging = Paging()): PagingState

class GitHubProjectRepository(private val api: Api, private val auth: Auth) : ProjectRepository,
    SearchRepository, UserRepository, CategoriesRepository {

    private var gitHubPagingState = GitHubPagingState()
    override val pagingState: PagingState
        get() = gitHubPagingState

    private var gitHubSearchPagingState = GitHubPagingState()
    override val searchPagingState: PagingState
        get() = gitHubSearchPagingState

    override fun resetPagingState() {
        gitHubPagingState = GitHubPagingState()
    }

    override fun resetSearchPagingState() {
        gitHubSearchPagingState = GitHubPagingState()
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

    override suspend fun fetchCategory(name: String): Result<List<Stack>> {
        return Result.success(emptyList())
    }

    override suspend fun fetchProjectDetails(uid: String, id: String): Result<Project> {
        return runCatching {
            api.getRepoBy(id)?.let {
                Project(
                    id = it.id.toString(),
                    name = it.name,
                    description = it.description,
                    categories = emptyList(),
                    image = Resource.NetworkResource("https://picsum.photos/500/500"),
                    createdBy = auth.userAccount?.id ?: "",
                    createdByName = auth.userAccount?.name ?: "",
                    createdOn = 11234567890,
                    public = !it.private
                )
            } ?: throw GitHubRepositoryException("Project not found.")
        }
    }

    override suspend fun nextPage(nextPageKey: Any?, category: String?): Result<List<Project>> {
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
                    categories = emptyList(),
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

    override suspend fun nextSearchPage(query: String, nextPageKey: Any?): Result<List<Project>> {
        return runCatching {
            val response = api.searchRepos(query, nextPageKey?.toString())
            gitHubSearchPagingState = GitHubPagingState(
                paging = Paging(
                    pageKey = nextPageKey,
                    nextPageKey = response.next,
                    prevPageKey = response.prev,
                    isLastPage = response.next == null
                )
            )
            response.page.projects.map {
                Project(
                    id = it.id.toString(),
                    name = it.name,
                    description = it.description,
                    categories = emptyList(),
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

    override suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>> =
        Result.failure(GitHubRepositoryException(throwable = NotImplementedError()))
}
