package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.data.Project

class GitHubProjectRepository(val api: Api): ProjectRepository {
    override fun fetchProjects(): List<Project> {
        return api.fetchProjects()
    }
}