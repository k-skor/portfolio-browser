package pl.krzyssko.portfoliobrowser.api

import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject
import pl.krzyssko.portfoliobrowser.api.dto.GitHubSearch
import pl.krzyssko.portfoliobrowser.api.dto.GitHubUser

data class PagedResponse<out T>(
    val page: List<T> = emptyList(),
    val prev: String? = null,
    val next: String? = null,
    val last: String? = null
)

data class PagedSearchResult<out T>(
    val page: T,
    val prev: String? = null,
    val next: String? = null,
    val last: String? = null
)

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T): ApiResponse<T>()
    data class Failure(val throwable: Throwable?): ApiResponse<Nothing>()
}

interface Api {
    suspend fun getUser(): ApiResponse<GitHubUser>
    suspend fun getRepos(): ApiResponse<List<GitHubProject>>
    suspend fun getRepos(queryParams: String?): ApiResponse<PagedResponse<GitHubProject>>
    suspend fun getRepoLanguages(repoName: String): ApiResponse<GitHubLanguage>
    suspend fun getRepoBy(name: String): ApiResponse<GitHubProject>
    suspend fun searchRepos(query: String, queryParams: String?): ApiResponse<PagedSearchResult<GitHubSearch>>
}
