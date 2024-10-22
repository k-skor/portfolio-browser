package pl.krzyssko.portfoliobrowser.store

import pl.krzyssko.portfoliobrowser.data.Project

sealed class ProjectListSideEffects {
    class Block()
    class ShowNext(val projects: List<Project>) : ProjectListSideEffects()
    class Filter(val stackFilter: List<String>) : ProjectListSideEffects()
}