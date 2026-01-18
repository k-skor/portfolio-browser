package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack

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
    suspend fun fetchUser(): Result<String>
    suspend fun nextPage(nextPageKey: Any?): Result<List<Project>>
    suspend fun fetchStack(name: String): Result<List<Stack>>
    suspend fun fetchProjectDetails(uid: String, id: String): Result<Project>
    suspend fun searchProjects(query: String, queryParams: String?): Result<PagedResponse<Project>>
    suspend fun nextSearchPage(query: String, nextPageKey: Any?): Result<List<Project>>
    suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>>
}