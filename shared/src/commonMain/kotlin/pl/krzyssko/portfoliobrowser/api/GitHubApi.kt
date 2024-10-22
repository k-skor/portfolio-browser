package pl.krzyssko.portfoliobrowser.api

import io.ktor.client.HttpClient
import pl.krzyssko.portfoliobrowser.data.Project

class GitHubApi(val httpClient: HttpClient): Api {
    override fun fetchProjects(page: Number): List<Project> {
        TODO("Not yet implemented")
    }

    override fun fetchProjectDetails(project: Project): Project {
        TODO("Not yet implemented")
    }
}