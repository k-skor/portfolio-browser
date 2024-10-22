package pl.krzyssko.portfoliobrowser.api

import pl.krzyssko.portfoliobrowser.data.Project

interface Api {
    fun fetchProjects(page: Number = 0): List<Project>
    fun fetchProjectDetails(project: Project): Project
}