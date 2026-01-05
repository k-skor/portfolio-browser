package pl.krzyssko.portfoliobrowser.api

import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject
import pl.krzyssko.portfoliobrowser.api.dto.GitHubSearch
import pl.krzyssko.portfoliobrowser.api.dto.GitHubUser

class ApiRequestException(throwable: Throwable? = null): Exception("Failed to get data from network.", throwable)

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

//sealed class ApiResponse<out T> {
//    data class Success<T>(val data: T): ApiResponse<T>()
//    data class Failure(val throwable: Throwable?): ApiResponse<Nothing>()
//}

interface Api {
    suspend fun getUser(): GitHubUser?
    suspend fun getRepos(): List<GitHubProject>
    suspend fun getRepos(queryParams: String?): PagedResponse<GitHubProject>
    suspend fun getRepoLanguages(repoName: String): GitHubLanguage
    suspend fun getRepoBy(name: String): GitHubProject?
    suspend fun searchRepos(searchPhrase: String, queryParams: String?): PagedSearchResult<GitHubSearch>
}
