package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.data.Project

interface ProjectRepository {
    fun fetchProjects(): List<Project>
}