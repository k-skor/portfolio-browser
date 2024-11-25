package pl.krzyssko.portfoliobrowser.api

import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject

data class PagedResponse<out T>(val data: List<T>, val prev: String?, val next: String?, val last: String?)

interface Api {
    suspend fun getRepos(page: Number = 1): List<GitHubProject>
    suspend fun getRepos(query: String?): PagedResponse<GitHubProject>
    suspend fun getRepoLanguages(repoName: String): GitHubLanguage
    suspend fun getRepoBy(name: String): GitHubProject
}