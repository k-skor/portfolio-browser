package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack

interface ProjectRepository {
    fun fetchProjects(queryParams: String?): Flow<PagedResponse<Project>>
    fun fetchStack(name: String): Flow<List<Stack>>
    fun fetchProjectDetails(name: String): Flow<Project>
    fun searchProjects(query: String, queryParams: String?): Flow<PagedResponse<Project>>
}