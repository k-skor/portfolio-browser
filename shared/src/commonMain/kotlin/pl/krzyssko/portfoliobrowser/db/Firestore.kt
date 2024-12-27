package pl.krzyssko.portfoliobrowser.db

import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project


interface Firestore {
    suspend fun createUser(uid: String, profile: Profile)
    suspend fun syncProjects(projectsList: List<Project>)
}

expect fun getFirestore(): Firestore
