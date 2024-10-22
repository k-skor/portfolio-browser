package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Project

data class ProjectState(val project: Project)

data class ProjectsListState(val projects: List<Project>, val canFetchMore: Boolean, val stackFilter: List<String>)