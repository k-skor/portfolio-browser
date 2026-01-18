package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.util.Response

data class Paging(
    val pageKey: Any? = null,
    val nextPageKey: Any? = null,
    val prevPageKey: Any? = null,
    val isLastPage: Boolean = true
)

interface PagingState {
    val paging: Paging
    val pageSize: Int
}

interface ProjectRepository {
    val pagingState: PagingState

    fun resetPagingState()
    fun fetchUser(): Flow<Result<String>>
    suspend fun nextPage(nextPageKey: Any?): Result<List<Project>>
    fun fetchStack(name: String): Flow<Result<List<Stack>>>
    fun fetchProjectDetails(uid: String, id: String): Flow<Result<Project>>
    fun searchProjects(query: String, queryParams: String?): Flow<Result<PagedResponse<Project>>>
    suspend fun nextSearchPage(query: String, nextPageKey: Any?): Result<List<Project>>
    suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>>
}