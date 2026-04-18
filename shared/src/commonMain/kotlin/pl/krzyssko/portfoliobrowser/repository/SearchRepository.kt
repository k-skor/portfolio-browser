package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.data.Project

interface SearchRepository {
    val searchPagingState: PagingState
    fun resetSearchPagingState()
    suspend fun nextSearchPage(query: String, nextPageKey: Any?): Result<List<Project>>
}
