package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project

data class Paging(
    val pageKey: Any? = null,
    val nextPageKey: Any? = null,
    val prevPageKey: Any? = null,
    val isLastPage: Boolean = false
)

interface PagingState {
    val paging: Paging
    val pageSize: Int
}

interface ProjectRepository {
    val pagingState: PagingState

    fun resetPagingState()
    suspend fun fetchTotalProjectsSize(): Result<Int>
    suspend fun nextPage(nextPageKey: Any?): Result<List<Project>>
    suspend fun fetchProjectDetails(uid: String, id: String): Result<Project>
    suspend fun searchProjects(query: String, queryParams: String?): Result<PagedResponse<Project>>
    suspend fun nextSearchPage(query: String, nextPageKey: Any?): Result<List<Project>>
    suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>>
}