package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject

interface ProjectRepository {
    suspend fun fetchProjects(query: String?): PagedResponse<GitHubProject>
    suspend fun fetchStack(name: String): Flow<GitHubLanguage>
    suspend fun fetchProjectDetails(name: String): Flow<GitHubProject>
}