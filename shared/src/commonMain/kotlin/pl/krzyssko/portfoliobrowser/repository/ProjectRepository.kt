package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack

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
    suspend fun fetchStack(name: String): Result<List<Stack>>
    suspend fun nextPage(nextPageKey: Any?, category: String? = null): Result<List<Project>>
    suspend fun fetchProjectDetails(uid: String, id: String): Result<Project>
    suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>>
}