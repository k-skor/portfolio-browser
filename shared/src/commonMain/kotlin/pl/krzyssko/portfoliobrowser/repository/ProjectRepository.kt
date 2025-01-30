package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack

interface PagingState {
    val paging: Paging
}

interface ProjectRepository {
    val pagingState: PagingState?

    fun resetPagingState()
    fun fetchUser(): Flow<String>
    fun nextPage(): Flow<List<Project>>
    fun fetchStack(name: String): Flow<List<Stack>>
    fun fetchProjectDetails(uid: String, id: String): Flow<Project>
    fun searchProjects(query: String, queryParams: String?): Flow<PagedResponse<Project>>
    fun nextSearchPage(query: String): Flow<List<Project>>
}