package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack

class GitHubRepositoryException(message: String? = null, throwable: Throwable? = null): Exception(message, throwable)

class GitHubPagingState(override val paging: Paging): PagingState

class GitHubProjectRepository(private val api: Api, private val auth: Auth) : ProjectRepository {

    private var gitHubPagingState = GitHubPagingState(Paging())
    override val pagingState: PagingState
        get() = gitHubPagingState

    override fun resetPagingState() {
        gitHubPagingState = GitHubPagingState(Paging())
    }

    /**
     * ProjectRepository
     */
    override fun fetchUser(): Flow<Result<String>> = flow {
        emit(runCatching {
            //when (val response = api.getUser()) {
            //    is ApiResponse.Success -> response.data.login
            //    is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
            //}
            api.getUser()?.login ?: throw GitHubRepositoryException("User not found.")
        })
    }

    override fun fetchStack(name: String): Flow<Result<List<Stack>>> = flow {
        emit(runCatching {
            //when (val response = api.getRepoLanguages(name)) {
            //    is ApiResponse.Success -> {
            //        val sum =
            //            response.data.map { it.value }
            //                .reduce { sum, lines -> sum + lines }

            //        (response.data.map {
            //            Stack(
            //                name = it.key, percent = it.value * 100f / sum
            //            )
            //        })
            //    }

            //    is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
            //}
            val response = api.getRepoLanguages(name)
            val sum =
                response.map { it.value }
                    .reduce { sum, lines -> sum + lines }

            response.map {
                Stack(
                    name = it.key, percent = it.value * 100f / sum
                )
            }
        })
    }

    override fun fetchProjectDetails(uid: String, id: String): Flow<Result<Project>> = flow {
        emit(runCatching {
            //when (val response = api.getRepoBy(id)) {
            //    is ApiResponse.Success -> Project(
            //        id = response.data.id.toString(),
            //        name = response.data.name,
            //        description = response.data.description,
            //        image = Resource.NetworkResource("https://picsum.photos/500/500"),
            //        createdBy = auth.userAccount?.id ?: "",
            //        createdByName = auth.userAccount?.name ?: "",
            //        createdOn = 11234567890,
            //        public = !response.data.private
            //    )
            //    is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
            //}
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
        })
    }

    override fun searchProjects(query: String, queryParams: String?): Flow<Result<PagedResponse<Project>>> = flow {
        emit(runCatching {
            //when (val response = api.searchRepos(query, queryParams)) {
            //    is ApiResponse.Success -> PagedResponse(page = response.data.page.projects.map {
            //        Project(
            //            id = it.id.toString(),
            //            name = it.name,
            //            description = it.description,
            //            image = Resource.NetworkResource("https://picsum.photos/500/500"),
            //            createdBy = auth.userAccount?.id ?: "",
            //            createdByName = auth.userAccount?.name ?: "",
            //            createdOn = 11234567890,
            //            public = !it.private
            //        )
            //    }, next = response.data.next, prev = response.data.prev, last = response.data.last)
            //    is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
            //}
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
        })
    }

    override fun nextPage(): Flow<Result<List<Project>>> = flow {
        emit(runCatching {
            val pageKey = gitHubPagingState.paging.nextPageKey.toString()
            //when (val response = api.getRepos(pageKey)) {
            //    is ApiResponse.Success -> response.data.also {
            //        gitHubPagingState = GitHubPagingState(
            //            paging = Paging(
            //                pageKey = pageKey,
            //                nextPageKey = it.next,
            //                prevPageKey = it.prev,
            //                isLastPage = it.next == null
            //            )
            //        )
            //    }.page.map {
            //        Project(
            //            id = it.id.toString(),
            //            name = it.name,
            //            description = it.description,
            //            image = Resource.NetworkResource("https://picsum.photos/500/500"),
            //            createdBy = auth.userAccount?.id ?: "",
            //            createdByName = auth.userAccount?.name ?: "",
            //            createdOn = 11234567890,
            //            public = !it.private,
            //            source = Source.GitHub
            //        )
            //    }
            //    is ApiResponse.Failure -> throw response.throwable ?: GitHubRepositoryException()
            //}
            val response = api.getRepos(pageKey)
            response.also {
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
        })
    }

    override fun nextSearchPage(query: String): Flow<Result<List<Project>>> {
        TODO("Not yet implemented")
    }
}