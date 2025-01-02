package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack

interface ProjectRepository {
    fun fetchUser(): Flow<String>
    fun fetchProjects(queryParams: String?): Flow<PagedResponse<Project>>
    fun fetchStack(name: String): Flow<List<Stack>>
    fun fetchProjectDetails(name: String): Flow<Project>
    fun searchProjects(query: String, queryParams: String?): Flow<PagedResponse<Project>>
}

interface ProjectDbRepository {
    suspend fun uploadProject(docId: String, project: Project)
    suspend fun uploadProjects(docId: String, projects: List<Project>)
    suspend fun uploadProjects(projectsFlow: Flow<List<Project>>)
}