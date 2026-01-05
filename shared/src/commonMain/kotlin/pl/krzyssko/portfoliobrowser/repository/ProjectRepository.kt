package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.util.Response

data class Paging(
    val pageKey: String? = null,
    val nextPageKey: String? = null,
    val prevPageKey: String? = null,
    val isLastPage: Boolean = true
)

interface PagingState {
    val paging: Paging
}

interface ProjectRepository {
    val pagingState: PagingState

    fun resetPagingState()
    fun fetchUser(): Flow<Result<String>>
    fun nextPage(): Flow<Result<List<Project>>>
    fun fetchStack(name: String): Flow<Result<List<Stack>>>
    fun fetchProjectDetails(uid: String, id: String): Flow<Result<Project>>
    fun searchProjects(query: String, queryParams: String?): Flow<Result<PagedResponse<Project>>>
    fun nextSearchPage(query: String): Flow<Result<List<Project>>>
    fun nextFavoritePage(): Flow<Result<List<Project>>>
}